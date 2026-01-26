package wiki.comnet.broadcaster.features.ble.domain.repository

import android.bluetooth.BluetoothDevice
import wiki.comnet.broadcaster.features.ble.domain.model.BleDeviceConnection

interface BleConnectionTrackerRepository {
    /**
     * Start the connection tracker
     */
    fun start()

    /**
     * Stop the connection tracker
     */
    fun stop()

    /**
     * Add a device connection
     */
    fun addDeviceConnection(deviceAddress: String, deviceConn: BleDeviceConnection)

    /**
     * Update a device connection
     */
    fun updateDeviceConnection(deviceAddress: String, deviceConn: BleDeviceConnection)

    /**
     * Get a device connection
     */
    fun getDeviceConnection(deviceAddress: String): BleDeviceConnection?

    /**
     * Get all connected devices
     */
    fun getConnectedDevices(): Map<String, BleDeviceConnection>

    fun isConnectionLimitReached(): Boolean

    fun getSubscribedDevices(): List<BluetoothDevice>

    /**
     * Add a subscribed device
     */
    fun addSubscribedDevice(device: BluetoothDevice)

    /**
     * Remove a subscribed device
     */
    fun removeSubscribedDevice(device: BluetoothDevice)

    /**
     * Check if device is already connected
     */
    fun isDeviceConnected(deviceAddress: String): Boolean

    /**
     * Check if connection attempt is allowed
     */
    fun isConnectionAttemptAllowed(deviceAddress: String): Boolean

    /**
     * Add a pending connection attempt
     */
    fun addPendingConnection(deviceAddress: String): Boolean

    /**
     *
     */
    fun removePendingConnection(deviceAddress: String)

    /**
     * Disconnect a specific device (by MAC address)
     */
    fun disconnectDevice(deviceAddress: String)

    fun cleanupDeviceConnection(deviceAddress: String)
}