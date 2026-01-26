package wiki.comnet.broadcaster.features.ble.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.di.ServiceScope
import wiki.comnet.broadcaster.features.ble.data.mapper.toBinaryData
import wiki.comnet.broadcaster.features.ble.data.protocol.MessageType
import wiki.comnet.broadcaster.features.ble.domain.model.BleDeviceConnection
import wiki.comnet.broadcaster.features.ble.domain.model.RoutedPacket
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionTrackerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleFragmentRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BlePacketBroadcasterRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BlePacketBroadcasterRepositoryImpl @Inject constructor(
    @ServiceScope private val connectionScope: CoroutineScope,
    private val fragmentRepository: BleFragmentRepository,
    private val connectionTrackerRepository: BleConnectionTrackerRepository,
) : BlePacketBroadcasterRepository {

    companion object {
        private val TAG = BlePacketBroadcasterRepository::class.java.simpleName

        private const val CLEANUP_DELAY = 500L
    }

    private data class BroadcastRequest(
        val routed: RoutedPacket,
        val gattServer: BluetoothGattServer?,
        val characteristic: BluetoothGattCharacteristic?,
    )

    private val broadcasterScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val transferJobs = ConcurrentHashMap<String, Job>()

    @OptIn(kotlinx.coroutines.ObsoleteCoroutinesApi::class)
    private val broadcasterActor = broadcasterScope.actor<BroadcastRequest>(
        capacity = Channel.UNLIMITED
    ) {
        Log.d(TAG, "🎭 Created packet broadcaster actor")
        try {
            for (request in channel) {
                broadcastSinglePacketInternal(
                    request.routed,
                    request.gattServer,
                    request.characteristic
                )
            }
        } finally {
            Log.d(TAG, "🎭 Packet broadcaster actor terminated")
        }
    }

    override fun broadcastPacket(
        routed: RoutedPacket,
        gattServer: BluetoothGattServer?,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        val packet = routed.packet
        val transferId = routed.transferId

        val fragments = try {
            fragmentRepository.createFragments(packet)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fragment creation failed: ${e.message}", e)
            return
        }

        if (fragments.size > 1) {
            Log.d(TAG, "Fragmenting packet into ${fragments.size} fragments")
            // TODO: fix me
            // if (transferId != null) {
            // TransferProgressManager.start(transferId, fragments.size)
            // }
            connectionScope.launch {
                var sent = 0
                fragments.forEachIndexed { index, fragment ->
                    if (!isActive) return@launch
                    // If cancelled, stop sending remaining fragments
                    Log.d(TAG, "Transfer ${index + 1}/${fragments.size}")
                    if (transferId != null && transferJobs[transferId]?.isCancelled == true) return@launch
                    broadcastSinglePacket(
                        RoutedPacket(fragment, transferId = transferId),
                        gattServer,
                        characteristic
                    )
                    // 20ms delay between fragments
                    delay(300)
                    if (transferId != null) {
                        sent += 1
                        // TransferProgressManager.progress(transferId, sent, fragments.size)
                        // if (sent == fragments.size) TransferProgressManager.complete(transferId, fragments.size)
                    }
                }
            }
//            if (transferId != null) {
//                transferJobs[transferId] = job
//                job.invokeOnCompletion { transferJobs.remove(transferId) }
//            }
            return
        }

        broadcastSinglePacket(routed, gattServer, characteristic)
    }

    fun broadcastSinglePacket(
        routed: RoutedPacket,
        gattServer: BluetoothGattServer?,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        // Submit broadcast request to actor for serialized processing
        broadcasterScope.launch {
            try {
                broadcasterActor.send(BroadcastRequest(routed, gattServer, characteristic))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send broadcast request to actor: ${e.message}")
                // Fallback to direct processing if actor fails
                broadcastSinglePacketInternal(routed, gattServer, characteristic)
            }
        }
    }

    private fun broadcastSinglePacketInternal(
        routed: RoutedPacket,
        gattServer: BluetoothGattServer?,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        val packet = routed.packet
        val data = packet.toBinaryData() ?: return
        MessageType.fromValue(packet.type)?.name ?: packet.type.toString()


        // Else, continue with broadcasting to all devices
        val subscribedDevices = connectionTrackerRepository.getSubscribedDevices()
        val connectedDevices = connectionTrackerRepository.getConnectedDevices()

        Log.i(
            TAG,
            "Broadcasting packet type ${packet.type} to ${subscribedDevices.size} server + ${connectedDevices.size} client connections"
        )


        // Send to server connections (devices connected to our GATT server)
        subscribedDevices.forEach { device ->
            if (device.address == routed.relayAddress) {
                Log.d(TAG, "Skipping broadcast to client back to relayer: ${device.address}")
                return@forEach
            }
            // FIXME: fix me
            // if (connectionTrackerRepository.addressPeerMap[device.address] == senderID) {
            //     Log.d(TAG, "Skipping broadcast to client back to sender: ${device.address}")
            //     return@forEach
            // }
            notifyDevice(device, data, gattServer, characteristic)
        }

        // Send to client connections (GATT servers we are connected to)
        connectedDevices.values.forEach { deviceConn ->
            if (deviceConn.isClient && deviceConn.gatt != null && deviceConn.characteristic != null) {
                if (deviceConn.device.address == routed.relayAddress) {
                    Log.d(
                        TAG,
                        "Skipping broadcast to server back to relayer: ${deviceConn.device.address}"
                    )
                    return@forEach
                }
                // if (connectionTrackerRepository.addressPeerMap[deviceConn.device.address] == senderID) {
                //     Log.d(TAG, "Skipping broadcast to server back to sender: ${deviceConn.device.address}")
                //     return@forEach
                // }
                writeToDeviceConn(deviceConn, data)
            }
        }
    }

    /**
     * Send data to a single device (server->client)
     */
    private fun notifyDevice(
        device: BluetoothDevice,
        data: ByteArray,
        gattServer: BluetoothGattServer?,
        characteristic: BluetoothGattCharacteristic?,
    ): Boolean {
        return try {
            characteristic?.let { char ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val result = gattServer?.notifyCharacteristicChanged(device, char, true, data)
                    return result == BluetoothStatusCodes.SUCCESS
                }
                @Suppress("DEPRECATION")
                char.value = data
                @Suppress("DEPRECATION")
                val result = gattServer?.notifyCharacteristicChanged(device, char, false) ?: false
                result
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error sending to server connection ${device.address}: ${e.message}")
            connectionScope.launch {
                delay(CLEANUP_DELAY)
                connectionTrackerRepository.removeSubscribedDevice(device)
                // connectionTrackerRepository.addressPeerMap.remove(device.address)
            }
            false
        }
    }

    private fun writeToDeviceConn(
        deviceConn: BleDeviceConnection,
        data: ByteArray,
    ): Boolean {
        return try {
            deviceConn.characteristic?.let { char ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val result = deviceConn.gatt?.writeCharacteristic(
                        char,
                        data,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                    return result == BluetoothStatusCodes.SUCCESS
                }
                @Suppress("DEPRECATION")
                char.value = data
                @Suppress("DEPRECATION")
                val result = deviceConn.gatt?.writeCharacteristic(char) ?: false
                result
            } ?: false
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Error sending to client connection ${deviceConn.device.address}: ${e.message} ${data.size}"
            )
            connectionScope.launch {
                delay(CLEANUP_DELAY)
                connectionTrackerRepository.cleanupDeviceConnection(deviceConn.device.address)
            }
            false
        }
    }
}