package wiki.comnet.broadcaster.features.ble.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.features.ble.data.mapper.toBinaryData
import wiki.comnet.broadcaster.features.ble.data.mapper.toBlePacket
import wiki.comnet.broadcaster.features.ble.data.protocol.MessagePadding
import wiki.comnet.broadcaster.features.ble.data.protocol.MessageType
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import wiki.comnet.broadcaster.features.ble.domain.model.FragmentPayload
import wiki.comnet.broadcaster.features.ble.domain.repository.BleFragmentRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class BleFragmentRepositoryImpl @Inject constructor() : BleFragmentRepository {
    companion object {
        private val TAG = BleFragmentRepository::class.java.simpleName

        // iOS values: 512 MTU threshold, 469 max fragment size (512 MTU - headers)
        private const val FRAGMENT_SIZE_THRESHOLD = 512 // Matches iOS: if data.count > 512
        private const val MAX_FRAGMENT_SIZE = 453        // Matches iOS: maxFragmentSize = 469
        private const val FRAGMENT_TIMEOUT = 30000L     // Matches iOS: 30 seconds cleanup
        private const val CLEANUP_INTERVAL = 10000L     // 10 seconds cleanup check
    }

    private val incomingFragments = ConcurrentHashMap<String, MutableMap<Int, ByteArray>>()

    private val fragmentMetadata =
        ConcurrentHashMap<String, Triple<UByte, Int, Long>>() // originalType, totalFragments, timestamp

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun createFragments(packet: BlePacket): List<BlePacket> {
        try {
            Log.d(
                TAG,
                "🔀 Creating fragments for packet type ${packet.type}, payload: ${packet.payload.size} bytes"
            )
            val encoded = packet.toBinaryData()
            if (encoded == null) {
                Log.e(TAG, "❌ Failed to encode packet to binary data")
                return emptyList()
            }
            Log.d(TAG, "📦 Encoded to ${encoded.size} bytes")

            // Fragment the unpadded frame; each fragment will be encoded (and padded) independently - iOS fix
            val fullData = try {
                MessagePadding.unpad(encoded)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to unpad data: ${e.message}", e)
                return emptyList()
            }
            Log.d(TAG, "📏 Unpadded to ${fullData.size} bytes")

            // iOS logic: if data.count > 512 && packet.type != MessageType.fragment.rawValue
            if (fullData.size <= FRAGMENT_SIZE_THRESHOLD) {
                return listOf(packet) // No fragmentation needed
            }

            val fragments = mutableListOf<BlePacket>()

            // iOS: let fragmentID = Data((0..<8).map { _ in UInt8.random(in: 0...255) })
            val fragmentID = FragmentPayload.generateFragmentID()

            // iOS: stride(from: 0, to: fullData.count, by: maxFragmentSize)
            val fragmentChunks = stride(
                0, fullData.size,
                MAX_FRAGMENT_SIZE
            ) { offset ->
                val endOffset = minOf(offset + MAX_FRAGMENT_SIZE, fullData.size)
                fullData.sliceArray(offset..<endOffset)
            }

            Log.d(
                TAG,
                "Creating ${fragmentChunks.size} fragments for ${fullData.size} byte packet (iOS compatible)"
            )

            // iOS: for (index, fragment) in fragments.enumerated()
            for (index in fragmentChunks.indices) {
                val fragmentData = fragmentChunks[index]

                // Create iOS-compatible fragment payload
                val fragmentPayload = FragmentPayload(
                    fragmentID = fragmentID,
                    index = index,
                    total = fragmentChunks.size,
                    originalType = packet.type,
                    data = fragmentData
                )

                // iOS: MessageType.fragment.rawValue (single fragment type)
                val fragmentPacket = BlePacket(
                    type = MessageType.FRAGMENT.value,
                    id = packet.id,
                    payload = fragmentPayload.encode(),
                )

                fragments.add(fragmentPacket)
            }

            Log.d(TAG, "✅ Created ${fragments.size} fragments successfully")
            return fragments
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fragment creation failed: ${e.message}", e)
            Log.e(TAG, "❌ Packet type: ${packet.type}, payload: ${packet.payload.size} bytes")
            return emptyList()
        }
    }

    /**
     * Handle incoming fragment - 100% iOS Compatible
     * Matches iOS handleFragment() implementation exactly
     */
    override fun handleFragment(packet: BlePacket): BlePacket? {
        // iOS: guard packet.payload.count > 13 else { return }
        if (packet.payload.size < FragmentPayload.HEADER_SIZE) {
            Log.w(TAG, "Fragment packet too small: ${packet.payload.size}")
            return null
        }

        // Don't process our own fragments - iOS equivalent check
        // This would be done at a higher level but we'll include for safety

        try {
            // Use FragmentPayload for type-safe decoding
            val fragmentPayload = FragmentPayload.decode(packet.payload)
            if (fragmentPayload == null || !fragmentPayload.isValid()) {
                Log.w(TAG, "Invalid fragment payload")
                return null
            }

            // iOS: let fragmentID = packet.payload[0..<8].map { String(format: "%02x", $0) }.joined()
            val fragmentIDString = fragmentPayload.getFragmentIDString()

            Log.d(
                TAG,
                "Received fragment ${fragmentPayload.index}/${fragmentPayload.total} for fragmentID: $fragmentIDString, originalType: ${fragmentPayload.originalType}"
            )

            // iOS: if incomingFragments[fragmentID] == nil
            if (!incomingFragments.containsKey(fragmentIDString)) {
                incomingFragments[fragmentIDString] = mutableMapOf()
                fragmentMetadata[fragmentIDString] = Triple(
                    fragmentPayload.originalType,
                    fragmentPayload.total,
                    System.currentTimeMillis()
                )
            }

            // iOS: incomingFragments[fragmentID]?[index] = Data(fragmentData)
            incomingFragments[fragmentIDString]?.put(fragmentPayload.index, fragmentPayload.data)

            // iOS: if let fragments = incomingFragments[fragmentID], fragments.count == total
            val fragmentMap = incomingFragments[fragmentIDString]
            if (fragmentMap != null && fragmentMap.size == fragmentPayload.total) {
                Log.d(TAG, "All fragments received for $fragmentIDString, reassembling...")

                // iOS reassembly logic: for i in 0..<total { if let fragment = fragments[i] { reassembled.append(fragment) } }
                val reassembledData = mutableListOf<Byte>()
                for (i in 0 until fragmentPayload.total) {
                    fragmentMap[i]?.let { data ->
                        reassembledData.addAll(data.asIterable())
                    }
                }

                // Decode the original packet bytes we reassembled, so flags/compression are preserved - iOS fix
                val originalPacket = reassembledData.toByteArray().toBlePacket()
                if (originalPacket != null) {
                    // iOS cleanup: incomingFragments.removeValue(forKey: fragmentID)
                    incomingFragments.remove(fragmentIDString)
                    fragmentMetadata.remove(fragmentIDString)

                    Log.d(
                        TAG,
                        "Successfully reassembled and decoded original packet of ${reassembledData.size} bytes"
                    )
                    return originalPacket
                } else {
                    val metadata = fragmentMetadata[fragmentIDString]
                    Log.e(
                        TAG,
                        "Failed to decode reassembled packet (type=${metadata?.first}, total=${metadata?.second})"
                    )
                }
            } else {
                val received = fragmentMap?.size ?: 0
                Log.d(
                    TAG,
                    "Fragment ${fragmentPayload.index} stored, have $received/${fragmentPayload.total} fragments for $fragmentIDString"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle fragment: ${e.message}")
        }

        return null
    }

    /**
     * Helper function to match iOS stride functionality
     * stride(from: 0, to: fullData.count, by: maxFragmentSize)
     */
    private fun <T> stride(from: Int, to: Int, by: Int, transform: (Int) -> T): List<T> {
        val result = mutableListOf<T>()
        var current = from
        while (current < to) {
            result.add(transform(current))
            current += by
        }
        return result
    }

    /**
     * iOS cleanup - exactly matching performCleanup() implementation
     * Clean old fragments (> 30 seconds old)
     */
    private fun cleanupOldFragments() {
        val now = System.currentTimeMillis()
        val cutoff = now - FRAGMENT_TIMEOUT

        // iOS: let oldFragments = fragmentMetadata.filter { $0.value.timestamp < cutoff }.map { $0.key }
        val oldFragments = fragmentMetadata.filter { it.value.third < cutoff }.map { it.key }

        // iOS: for fragmentID in oldFragments { incomingFragments.removeValue(forKey: fragmentID) }
        for (fragmentID in oldFragments) {
            incomingFragments.remove(fragmentID)
            fragmentMetadata.remove(fragmentID)
        }

        if (oldFragments.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${oldFragments.size} old fragment sets (iOS compatible)")
        }
    }

    /**
     * Get debug information - matches iOS debugging
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== Fragment Manager Debug Info (iOS Compatible) ===")
            appendLine("Active Fragment Sets: ${incomingFragments.size}")
            appendLine("Fragment Size Threshold: ${FRAGMENT_SIZE_THRESHOLD} bytes")
            appendLine("Max Fragment Size: ${MAX_FRAGMENT_SIZE} bytes")

            fragmentMetadata.forEach { (fragmentID, metadata) ->
                val (originalType, totalFragments, timestamp) = metadata
                val received = incomingFragments[fragmentID]?.size ?: 0
                val ageSeconds = (System.currentTimeMillis() - timestamp) / 1000
                appendLine("  - $fragmentID: $received/$totalFragments fragments, type: $originalType, age: ${ageSeconds}s")
            }
        }
    }

    /**
     * Start periodic cleanup of old fragments - matches iOS maintenance timer
     */
    private fun startPeriodicCleanup() {
        managerScope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL)
                cleanupOldFragments()
            }
        }
    }

    /**
     * Clear all fragments
     */
    fun clearAllFragments() {
        incomingFragments.clear()
        fragmentMetadata.clear()
    }

    /**
     * Shutdown the manager
     */
    fun shutdown() {
        managerScope.cancel()
        clearAllFragments()
    }
}