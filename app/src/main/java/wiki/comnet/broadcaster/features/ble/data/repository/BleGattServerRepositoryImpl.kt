package wiki.comnet.broadcaster.features.ble.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.di.ServiceScope
import wiki.comnet.broadcaster.features.ble.constant.BleConfig
import wiki.comnet.broadcaster.features.ble.data.mapper.toBlePacket
import wiki.comnet.broadcaster.features.ble.domain.model.BleDeviceConnection
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionTrackerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleGattServerRepository
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BleGattServerRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ServiceScope private val connectionScope: CoroutineScope,
    private val bluetoothManager: BluetoothManager,
    private val bleConnectionTrackerRepository: BleConnectionTrackerRepository,
) : BleGattServerRepository {

    companion object {
        private val TAG = BleGattServerRepository::class.java.simpleName
    }

    private var _packet = MutableStateFlow<BlePacket?>(null)

    override val packet = _packet.asStateFlow()


    private val bluetoothAdapter = bluetoothManager.adapter

    private val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

    private var gattServer: BluetoothGattServer? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var advertiseCallback: AdvertiseCallback? = null

    private var isActive = false

    override fun start(): Boolean {
        if (isActive) {
            return true
        }

        isActive = true

        connectionScope.launch {
            setupGattServer()
            delay(300)
            startAdvertising()
        }

        return true
    }

    override fun stop() {
        if (!isActive) {
            stopAdvertising()
            gattServer?.close()
            gattServer = null
            Log.i(TAG, "GATT server stopped (already inactive)")
            return
        }

        isActive = false

        connectionScope.launch {
            stopAdvertising()

            // Try to cancel any active connections explicitly before closing
            try {
                val devices = bleConnectionTrackerRepository.getSubscribedDevices()
                devices.forEach { d ->
                    try {
                        gattServer?.cancelConnection(d)
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }

            // Close GATT server
            gattServer?.close()
            gattServer = null

            Log.i(TAG, "GATT server stopped")
        }
    }

    override fun getGattServer(): BluetoothGattServer? = gattServer

    /**
     * Get characteristic instance
     */
    override fun getCharacteristic(): BluetoothGattCharacteristic? = characteristic

    private fun setupGattServer() {

        val serverCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int,
            ) {
                // Guard against callbacks after service shutdown
                if (!isActive) {
                    Log.d(TAG, "Server: Ignoring connection state change after shutdown")
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Server: Device connected ${device.address} ${device.name}")

                        val deviceConn = BleDeviceConnection(
                            device = device,
                            isClient = false
                        )
                        bleConnectionTrackerRepository.addDeviceConnection(
                            device.address,
                            deviceConn
                        )
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Server: Device disconnected ${device.address}")
                        bleConnectionTrackerRepository.cleanupDeviceConnection(device.address)
                    }
                }
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                // Guard against callbacks after service shutdown
                if (!isActive) {
                    Log.d(TAG, "Server: Ignoring service added callback after shutdown")
                    return
                }

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Server: Service added successfully: ${service.uuid}")
                } else {
                    Log.e(TAG, "Server: Failed to add service: ${service.uuid}, status: $status")
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                // Guard against callbacks after service shutdown
                if (!isActive) {
                    Log.d(TAG, "Server: Ignoring characteristic write after shutdown")
                    return
                }

                if (characteristic.uuid == BleConfig.CHARACTERISTIC_UUID) {
                    Log.i(
                        TAG,
                        "Server: Received packet from ${device.address}, size: ${value.size} bytes"
                    )

                    val packet = value.toBlePacket()
                    if (packet == null) {
                        Log.w(
                            TAG,
                            "Server: Failed to parse packet from ${device.address}, size: ${value.size} bytes"
                        )
                        Log.w(
                            TAG,
                            "Server: Packet data: ${value.joinToString(" ") { "%02x".format(it) }}"
                        )
                        return
                    }

                    _packet.value = packet

                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null
                        )
                    }
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                // Guard against callbacks after service shutdown
                if (!isActive) {
                    Log.d(TAG, "Server: Ignoring descriptor write after shutdown")
                    return
                }

                if (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.contentEquals(value)) {
                    bleConnectionTrackerRepository.addSubscribedDevice(device)
                    Log.d(TAG, "Server: Connection setup complete for ${device.address}")
                }

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
        }

        // Proper cleanup sequencing to prevent race conditions
        gattServer?.let { server ->
            Log.d(TAG, "Cleaning up existing GATT server")
            try {
                server.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing existing GATT server: ${e.message}")
            }
        }

        // Small delay to ensure cleanup is complete
        Thread.sleep(100)

        if (!isActive) {
            Log.d(TAG, "Service inactive, skipping GATT server creation")
            return
        }

        // Create new server
        gattServer = bluetoothManager.openGattServer(context, serverCallback)

        // Create characteristic with notification support
        characteristic = BluetoothGattCharacteristic(
            BleConfig.CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val descriptor = BluetoothGattDescriptor(
            BleConfig.DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        characteristic?.addDescriptor(descriptor)

        val service =
            BluetoothGattService(BleConfig.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        service.addCharacteristic(characteristic)

        gattServer?.addService(service)

        Log.i(TAG, "GATT server setup complete")
    }

    private fun startAdvertising() {
        if (!isActive) {
            Log.d(TAG, "Not starting advertising: manager not active")
            return
        }

        if (bluetoothLeAdvertiser == null) {
            Log.w(TAG, "Not starting advertising: BLE advertiser not available on this device")
            return
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.w(
                TAG,
                "Not starting advertising: multiple advertisement not supported on this device"
            )
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConfig.SERVICE_UUID))
            .setIncludeTxPowerLevel(false)
            .setIncludeDeviceName(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i(TAG, "Advertising started")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Advertising failed: $errorCode")
            }
        }

        try {
            bluetoothLeAdvertiser.startAdvertising(
                settings,
                data,
                advertiseCallback
            )
        } catch (se: SecurityException) {
            Log.e(
                TAG,
                "SecurityException starting advertising (missing permission?): ${se.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting advertising: ${e.message}")
        }
    }

    private fun stopAdvertising() {
        if (bluetoothLeAdvertiser == null) {
            return
        }
        try {
            advertiseCallback?.let { cb -> bluetoothLeAdvertiser.stopAdvertising(cb) }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping advertising: ${e.message}")
        }
    }
}