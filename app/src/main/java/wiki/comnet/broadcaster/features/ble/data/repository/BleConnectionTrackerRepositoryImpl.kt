package wiki.comnet.broadcaster.features.ble.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.di.ServiceScope
import wiki.comnet.broadcaster.features.ble.constant.BleConfig
import wiki.comnet.broadcaster.features.ble.domain.model.BleConnectionAttempt
import wiki.comnet.broadcaster.features.ble.domain.model.BleDeviceConnection
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionTrackerRepository
import wiki.comnet.broadcaster.features.logging.ComNetLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BleConnectionTrackerRepositoryImpl @Inject constructor(
    @param:ServiceScope private val connectionScope: CoroutineScope,
) : BleConnectionTrackerRepository {

    companion object {
        private val TAG = BleConnectionTrackerRepository::class.java.simpleName
    }

    private val connectedDevices = ConcurrentHashMap<String, BleDeviceConnection>()
    private val subscribedDevices = CopyOnWriteArrayList<BluetoothDevice>()

    private val pendingConnections = ConcurrentHashMap<String, BleConnectionAttempt>()

    private var isActive = false

    override fun start() {
        isActive = true
        startPeriodicCleanup()
    }

    override fun stop() {
        isActive = false
        cleanupAllConnections()
        clearAllConnections()
    }

    override fun addDeviceConnection(
        deviceAddress: String,
        deviceConn: BleDeviceConnection,
    ) {
        connectedDevices[deviceAddress] = deviceConn
        pendingConnections.remove(deviceAddress)
    }

    override fun updateDeviceConnection(deviceAddress: String, deviceConn: BleDeviceConnection) {
        connectedDevices[deviceAddress] = deviceConn
    }

    /**
     * Get a device connection
     */
    override fun getDeviceConnection(deviceAddress: String): BleDeviceConnection? {
        return connectedDevices[deviceAddress]
    }

    /**
     * Get all connected devices
     */
    override fun getConnectedDevices(): Map<String, BleDeviceConnection> {
        return connectedDevices.toMap()
    }

    override fun isConnectionLimitReached(): Boolean {
        return connectedDevices.size >= BleConfig.MAX_CLIENT_CONNECTION
    }

    override fun getSubscribedDevices(): List<BluetoothDevice> {
        return subscribedDevices.toList()
    }

    /**
     * Add a subscribed device
     */
    override fun addSubscribedDevice(device: BluetoothDevice) {
        subscribedDevices.add(device)
    }

    /**
     * Remove a subscribed device
     */
    override fun removeSubscribedDevice(device: BluetoothDevice) {
        subscribedDevices.remove(device)
    }

    /**
     * Check if device is already connected
     */
    override fun isDeviceConnected(deviceAddress: String): Boolean {
        return connectedDevices.containsKey(deviceAddress)
    }

    /**
     * Check if connection attempt is allowed
     */
    override fun isConnectionAttemptAllowed(deviceAddress: String): Boolean {
        val existingAttempt = pendingConnections[deviceAddress]
        return existingAttempt?.let {
            it.isExpired() || it.shouldRetry()
        } ?: true
    }

    /**
     * Add a pending connection attempt
     */
    override fun addPendingConnection(deviceAddress: String): Boolean {
        synchronized(pendingConnections) {
            // Double-check inside synchronized block
            val currentAttempt = pendingConnections[deviceAddress]
            if (currentAttempt != null && !currentAttempt.isExpired() && !currentAttempt.shouldRetry()) {
                ComNetLog.d(
                    TAG,
                    "Tracker: Connection attempt already in progress for $deviceAddress"
                )
                return false
            }
            if (currentAttempt != null) {
                ComNetLog.d(TAG, "Tracker: current attempt: $currentAttempt")
            }

            // Update connection attempt atomically
            val attempts = (currentAttempt?.attempts ?: 0) + 1
            pendingConnections[deviceAddress] = BleConnectionAttempt(attempts)
            ComNetLog.d(
                TAG,
                "Tracker: Added pending connection for $deviceAddress (attempts: $attempts)"
            )
            return true
        }
    }

    override fun removePendingConnection(deviceAddress: String) {
        pendingConnections.remove(deviceAddress)
    }

    /**
     * Disconnect a specific device (by MAC address)
     */
    override fun disconnectDevice(deviceAddress: String) {
        connectedDevices[deviceAddress]?.gatt?.let {
            try {
                it.disconnect()
            } catch (_: Exception) {
            }
        }
        cleanupDeviceConnection(deviceAddress)
    }

    override fun cleanupDeviceConnection(deviceAddress: String) {
        connectedDevices.remove(deviceAddress)?.let { deviceConn ->
            subscribedDevices.removeAll { it.address == deviceAddress }
//            addressPeerMap.remove(deviceAddress)
        }
    }

    private fun cleanupAllConnections() {
        connectedDevices.values.forEach { deviceConn ->
            deviceConn.gatt?.disconnect()
        }

        connectionScope.launch {
            delay(BleConfig.CLEANUP_DELAY)

            connectedDevices.values.forEach { deviceConn ->
                try {
                    deviceConn.gatt?.close()
                } catch (e: Exception) {
                    ComNetLog.w(TAG, "Error closing GATT during cleanup: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear all connection tracking
     */
    private fun clearAllConnections() {
        connectedDevices.clear()
        subscribedDevices.clear()
//        addressPeerMap.clear()
        pendingConnections.clear()
    }

    /**
     * Start periodic cleanup of expired connections
     */
    private fun startPeriodicCleanup() {
        connectionScope.launch {
            while (isActive) {
                delay(BleConfig.CLEANUP_INTERVAL)

                if (!isActive) break

                try {
                    // Clean up expired pending connections
                    val expiredConnections = pendingConnections.filter { it.value.isExpired() }
                    expiredConnections.keys.forEach { pendingConnections.remove(it) }

                    // Log cleanup if any
                    if (expiredConnections.isNotEmpty()) {
                        ComNetLog.d(
                            TAG,
                            "Cleaned up ${expiredConnections.size} expired connection attempts"
                        )
                    }

                    // Log current state
                    ComNetLog.d(
                        TAG,
                        "Periodic cleanup: ${connectedDevices.size} connections, ${pendingConnections.size} pending"
                    )

                } catch (e: Exception) {
                    ComNetLog.w(TAG, "Error in periodic cleanup: ${e.message}")
                }
            }
        }
    }
}