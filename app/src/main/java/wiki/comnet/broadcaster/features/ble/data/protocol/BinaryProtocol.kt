package wiki.comnet.broadcaster.features.ble.data.protocol

import wiki.comnet.broadcaster.features.logging.ComNetLog
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary packet format - 100% backward compatible with iOS version
 *
 * Header (13 bytes for v1, 15 bytes for v2):
 * - Version: 1 byte
 * - Type: 1 byte
 * - IdLength: 4 bytes (big-endian)
 * - PayloadLength: 4 bytes (big-endian)
 *
 * Variable sections:
 * - Id: Variable length
 * - Payload: Variable length
 */
object BinaryProtocol {
    private const val HEADER_SIZE = 10

    fun encode(packet: BlePacket): ByteArray? {
        try {
            val payload = packet.payload

            val idBytes = packet.id.size
            val payloadBytes = payload.size

            val capacity = HEADER_SIZE + idBytes + payloadBytes + 16

            val buffer = ByteBuffer.allocate(capacity.coerceAtLeast(512))
                .apply { order(ByteOrder.BIG_ENDIAN) }

            buffer.put(packet.version.toByte())
            buffer.put(packet.type.toByte())
            buffer.putInt(idBytes)
            buffer.putInt(payloadBytes)
            buffer.put(packet.id)
            buffer.put(payload)

            val result = ByteArray(buffer.position())

            buffer.rewind()
            buffer.get(result)

            val optimalSize = MessagePadding.optimalBlockSize(result.size)
            val paddedData = MessagePadding.pad(result, optimalSize)

            return paddedData
        } catch (e: Exception) {
            return null
        }
    }

    fun decode(data: ByteArray): BlePacket? {
        decodeCore(data)?.let { return it }

        val unpadded = MessagePadding.unpad(data)
        if (unpadded.contentEquals(data)) return null

        return decodeCore(unpadded)
    }

    private fun decodeCore(raw: ByteArray): BlePacket? {
        try {
            if (raw.size < HEADER_SIZE) return null

            val buffer = ByteBuffer.wrap(raw).apply { order(ByteOrder.BIG_ENDIAN) }

            // Header
            val version = buffer.get().toUByte()
            val type = buffer.get().toUByte()
            val idLength = buffer.getInt().toUInt()
            val payloadLength = buffer.getInt().toUInt()

            val expectedSize = HEADER_SIZE + idLength.toInt() + payloadLength.toInt()

            if (raw.size < expectedSize) return null

            // SenderID
            val id = ByteArray(idLength.toInt())
            buffer.get(id)

            val payloadBytes = ByteArray(payloadLength.toInt())
            buffer.get(payloadBytes)
            val payload = payloadBytes


            return BlePacket(
                version = version,
                type = type,
                id = id,
                payload = payload
            )
        } catch (e: Exception) {
            ComNetLog.e("BinaryProtocol", "Error decoding packet: ${e.message}")
            return null
        }
    }
}