package wiki.comnet.broadcaster.features.ble.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
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
import wiki.comnet.broadcaster.features.ble.domain.repository.BleGattClientRepository
import wiki.comnet.broadcaster.features.logging.ComNetLog
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BleGattClientRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ServiceScope private val connectionScope: CoroutineScope,
    bluetoothManager: BluetoothManager,
    private val bleConnectionTrackerRepository: BleConnectionTrackerRepository,
) : BleGattClientRepository {

    companion object {
        private val TAG = BleGattClientRepository::class.java.simpleName
    }

    private var _packet = MutableStateFlow<BlePacket?>(null)

    override val packet = _packet.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var scanCallback: ScanCallback? = null

    // Scan rate limiting to prevent "scanning too frequently" errors
    private var lastScanStartTime = 0L
    private var lastScanStopTime = 0L
    private var isCurrentlyScanning = false
    private val scanRateLimit = 5000L // Minimum 5 seconds between scan start attempts

    // State management
    private var isActive = false

    /**
     * Start client manager
     */
    override fun start(): Boolean {
        if (isActive) {
            ComNetLog.d(TAG, "GATT client already active; start is a no-op")
            return true
        }

        if (bluetoothAdapter?.isEnabled != true) {
            ComNetLog.e(TAG, "Bluetooth is not enabled")
            return false
        }

        if (bleScanner == null) {
            ComNetLog.e(TAG, "BLE scanner not available")
            return false
        }

        isActive = true

        connectionScope.launch {
            startScanning()
        }

        return true
    }

    /**
     * Stop client manager
     */
    override fun stop() {
        if (!isActive) {
            // Idempotent stop
            stopScanning()

            ComNetLog.i(TAG, "GATT client manager stopped (already inactive)")
            return
        }

        isActive = false

        connectionScope.launch {
            // Disconnect all client connections decisively
            try {
                val connections =
                    bleConnectionTrackerRepository.getConnectedDevices().values.filter { it.isClient && it.gatt != null }
                connections.forEach { dc ->
                    try {
                        dc.gatt?.disconnect()
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }

            stopScanning()
            ComNetLog.i(TAG, "GATT client manager stopped")
        }
    }


    private fun startScanning() {
        val currentTime = System.currentTimeMillis()
        if (isCurrentlyScanning) {
            ComNetLog.d(TAG, "Scan already in progress, skipping start request")
            return
        }

        val timeSinceLastStart = currentTime - lastScanStartTime
        if (timeSinceLastStart < scanRateLimit) {
            val remainingWait = scanRateLimit - timeSinceLastStart
            ComNetLog.w(
                TAG,
                "Scan rate limited: need to wait ${remainingWait}ms before starting scan"
            )

            // Schedule delayed scan start
            connectionScope.launch {
                delay(remainingWait)
                if (isActive && !isCurrentlyScanning) {
                    startScanning()
                }
            }
            return
        }

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConfig.SERVICE_UUID))
            .build()

        val scanFilters = listOf(scanFilter)

        ComNetLog.d(TAG, "Starting BLE scan with target service UUID: ${BleConfig.SERVICE_UUID}")

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                ComNetLog.d(TAG, "Scan result received: ${result.device.address}")
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                ComNetLog.d(TAG, "Batch scan results received: ${results.size} devices")
                results.forEach { result ->
                    handleScanResult(result)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                ComNetLog.e(TAG, "Scan failed: $errorCode")
                isCurrentlyScanning = false
                lastScanStopTime = System.currentTimeMillis()

                when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> ComNetLog.e(TAG, "SCAN_FAILED_ALREADY_STARTED")
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> ComNetLog.e(
                        TAG,
                        "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
                    )

                    SCAN_FAILED_INTERNAL_ERROR -> ComNetLog.e(TAG, "SCAN_FAILED_INTERNAL_ERROR")
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> ComNetLog.e(
                        TAG,
                        "SCAN_FAILED_FEATURE_UNSUPPORTED"
                    )

                    SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> ComNetLog.e(
                        TAG,
                        "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES"
                    )

                    SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> {
                        ComNetLog.e(TAG, "SCAN_FAILED_SCANNING_TOO_FREQUENTLY")
                        ComNetLog.w(
                            TAG,
                            "Scan failed due to rate limiting - will retry after delay"
                        )
                        connectionScope.launch {
                            delay(10000) // Wait 10 seconds before retrying
                            if (isActive) {
                                startScanning()
                            }
                        }
                    }

                    else -> ComNetLog.e(TAG, "Unknown scan failure code: $errorCode")
                }
            }
        }

        val settings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0)
            .build()

        try {
            lastScanStartTime = currentTime
            isCurrentlyScanning = true

            bleScanner?.startScan(scanFilters, settings, scanCallback)
            ComNetLog.d(TAG, "BLE scan started successfully")
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Exception starting scan: ${e.message}")
            isCurrentlyScanning = false
        }
    }

    private fun stopScanning() {
        if (isCurrentlyScanning) {
            try {
                scanCallback?.let {
                    bleScanner?.stopScan(it)
                    ComNetLog.d(TAG, "BLE scan stopped successfully")
                }
            } catch (e: Exception) {
                ComNetLog.w(TAG, "Error stopping scan: ${e.message}")
            }

            isCurrentlyScanning = false
            lastScanStopTime = System.currentTimeMillis()
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val deviceAddress = device.address
        val scanRecord = result.scanRecord

        val hasOurService =
            scanRecord?.serviceUuids?.any { it.uuid == BleConfig.SERVICE_UUID } == true

        if (!hasOurService) {
            return
        }

        if (bleConnectionTrackerRepository.isDeviceConnected(deviceAddress)) {
            return
        }

        if (!bleConnectionTrackerRepository.isConnectionAttemptAllowed(deviceAddress)) {
            ComNetLog.d(TAG, "Connection to $deviceAddress not allowed due to recent attempts")
            return
        }


        if (bleConnectionTrackerRepository.isConnectionLimitReached()) {
            ComNetLog.d(TAG, "Connection limit reached)")
            return
        }

        if (bleConnectionTrackerRepository.addPendingConnection(deviceAddress)) {
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {

        val deviceAddress = device.address
        ComNetLog.i(TAG, "Connecting to bitchat device: $deviceAddress")

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                ComNetLog.d(
                    TAG,
                    "Client: Connection state change - Device: $deviceAddress, Status: $status, NewState: $newState"
                )

                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    ComNetLog.i(
                        TAG,
                        "Client: Successfully connected to $deviceAddress. Requesting MTU..."
                    )
                    // Request a larger MTU. Must be done before any data transfer.
                    connectionScope.launch {
                        delay(200) // A small delay can improve reliability of MTU request.
                        gatt.requestMtu(512)
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        ComNetLog.w(
                            TAG,
                            "Client: Disconnected from $deviceAddress with error status $status"
                        )
                        if (status == 147) {
                            ComNetLog.e(
                                TAG,
                                "Client: Connection establishment failed (status 147) for $deviceAddress"
                            )
                        }
                    } else {
                        ComNetLog.d(TAG, "Client: Cleanly disconnected from $deviceAddress")
                        bleConnectionTrackerRepository.cleanupDeviceConnection(deviceAddress)
                    }

                    // Notify higher layers about device disconnection to update direct flags
                    // TODO: fix
                    // delegate?.onDeviceDisconnected(gatt.device)

                    connectionScope.launch {
                        delay(500) // CLEANUP_DELAY
                        try {
                            gatt.close()
                        } catch (e: Exception) {
                            ComNetLog.w(TAG, "Error closing GATT: ${e.message}")
                        }
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                val deviceAddress = gatt.device.address
                ComNetLog.i(
                    TAG,
                    "Client: MTU changed for $deviceAddress to $mtu with status $status"
                )

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    ComNetLog.i(
                        TAG,
                        "MTU successfully negotiated for $deviceAddress. Discovering services."
                    )

                    // Now that MTU is set, connection is fully ready.
                    val deviceConn = BleDeviceConnection(
                        device = gatt.device,
                        gatt = gatt,
                        isClient = true
                    )
                    bleConnectionTrackerRepository.addDeviceConnection(deviceAddress, deviceConn)

                    // Start service discovery only AFTER MTU is set.
                    gatt.discoverServices()
                } else {
                    ComNetLog.w(
                        TAG,
                        "MTU negotiation failed for $deviceAddress with status: $status. Disconnecting."
                    )
                    bleConnectionTrackerRepository.removePendingConnection(deviceAddress)
                    gatt.disconnect()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(BleConfig.SERVICE_UUID)
                    val characteristic =
                        service?.getCharacteristic(BleConfig.CHARACTERISTIC_UUID)
                    if (service == null) {
                        ComNetLog.e(TAG, "Client: Required service not found for $deviceAddress")
                        gatt.disconnect()
                        return
                    }
                    if (characteristic == null) {
                        ComNetLog.e(
                            TAG,
                            "Client: Required characteristic not found for $deviceAddress"
                        )
                        gatt.disconnect()
                        return
                    }

                    bleConnectionTrackerRepository.getDeviceConnection(deviceAddress)
                        ?.let { deviceConn ->
                            val updatedConn =
                                deviceConn.copy(characteristic = characteristic)
                            bleConnectionTrackerRepository.updateDeviceConnection(
                                deviceAddress,
                                updatedConn
                            )
                            ComNetLog.d(
                                TAG,
                                "Client: Updated device connection with characteristic for $deviceAddress"
                            )
                        }

                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(BleConfig.DESCRIPTOR_UUID)
                    if (descriptor != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(
                                descriptor,
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }

                        connectionScope.launch {
                            delay(200)
                            ComNetLog.i(
                                TAG,
                                "Client: Connection setup complete for $deviceAddress"
                            )
                        }
                    } else {
                        ComNetLog.e(TAG, "Client: CCCD descriptor not found for $deviceAddress")
                        gatt.disconnect()
                    }


                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {

                @Suppress("DEPRECATION")
                val value = characteristic.value

                ComNetLog.i(
                    TAG,
                    "Client: Received packet from ${gatt.device.address}, size: ${value.size} bytes"
                )

                val packet = value.toBlePacket()
                if (packet == null) {
                    ComNetLog.w(
                        TAG,
                        "Client: Failed to parse packet from ${gatt.device.address}, size: ${value.size} bytes"
                    )
                    ComNetLog.w(
                        TAG,
                        "Client: Packet data: ${value.joinToString(" ") { "%02x".format(it) }}"
                    )
                    return
                }

                _packet.value = packet
            }

//            override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
//                val deviceAddress = gatt.device.address
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    ComNetLog.d(TAG, "Client: RSSI updated for $deviceAddress: $rssi dBm")
//
//                    // Update the connection tracker with new RSSI value
//                    bleConnectionTrackerRepository.getDeviceConnection(deviceAddress)?.let { deviceConn ->
//                        val updatedConn = deviceConn.copy(rssi = rssi)
//                        connectionTracker.updateDeviceConnection(deviceAddress, updatedConn)
//                    }
//                } else {
//                    ComNetLog.w(com.bitchat.android.mesh.BluetoothGattClientManager.Companion.TAG, "Client: Failed to read RSSI for $deviceAddress, status: $status")
//                }
//            }
        }

        try {
            ComNetLog.d(
                TAG,
                "Client: Attempting GATT connection to $deviceAddress with autoConnect=false"
            )
            val gatt =
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) {
                ComNetLog.e(TAG, "connectGatt returned null for $deviceAddress")
                // keep the pending connection so we can avoid too many reconnections attempts, TODO: needs testing
                bleConnectionTrackerRepository.removePendingConnection(deviceAddress)
            } else {
                ComNetLog.d(
                    TAG,
                    "Client: GATT connection initiated successfully for $deviceAddress"
                )
            }
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Client: Exception connecting to $deviceAddress: ${e.message}")
            // keep the pending connection so we can avoid too many reconnections attempts, TODO: needs testing
            bleConnectionTrackerRepository.removePendingConnection(deviceAddress)
        }
    }
}