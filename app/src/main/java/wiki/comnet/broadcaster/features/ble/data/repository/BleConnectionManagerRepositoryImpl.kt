package wiki.comnet.broadcaster.features.ble.data.repository

import android.bluetooth.BluetoothManager
import wiki.comnet.broadcaster.features.logging.ComNetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.di.ServiceScope
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import wiki.comnet.broadcaster.features.ble.domain.model.RoutedPacket
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionManagerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionTrackerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleGattClientRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleGattServerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BlePacketBroadcasterRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BlePacketProcessorRepository
import javax.inject.Inject

class BleConnectionManagerRepositoryImpl @Inject constructor(
    @param:ServiceScope private val connectionScope: CoroutineScope,
    private val bluetoothManager: BluetoothManager,
    private val bluetoothPermissionManagerRepository: BluetoothPermissionManagerRepository,
    private val bleConnectionTrackerRepository: BleConnectionTrackerRepository,
    private val bleGattClientRepository: BleGattClientRepository,
    private val bleGattServerRepository: BleGattServerRepository,
    private val packetBroadcasterRepository: BlePacketBroadcasterRepository,
    private val packetProcessorRepository: BlePacketProcessorRepository,
) : BleConnectionManagerRepository {
    companion object {
        private val TAG = BleConnectionManagerRepository::class.java.simpleName
    }

    override val message = packetProcessorRepository.message

    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private var isActive = false

    override fun startServices(): Boolean {
        try {
            isActive = true

            if (!bluetoothPermissionManagerRepository.hasBluetoothPermissions()) {
                ComNetLog.e(TAG, "Bluetooth permissions not granted")
                return false
            }

            if (bluetoothAdapter?.isEnabled != true) {
                ComNetLog.e(TAG, "Bluetooth is not enabled")
                return false
            }

            connectionScope.launch {
                bleConnectionTrackerRepository.start()

                if (!bleGattServerRepository.start()) {
                    this@BleConnectionManagerRepositoryImpl.isActive = false
                    return@launch
                } else {
                    ComNetLog.i(TAG, "GATT Server disabled by debug settings; not starting")
                }

                if (!bleGattClientRepository.start()) {
                    this@BleConnectionManagerRepositoryImpl.isActive = false
                    return@launch
                } else {
                    ComNetLog.i(TAG, "GATT Client disabled by debug settings; not starting")
                }

                this@BleConnectionManagerRepositoryImpl.isActive = false
            }

            connectionScope.launch {
                bleGattServerRepository.packet.collect {
                    if (it == null) return@collect
                    onPacketReceived(it)
                }
            }

            connectionScope.launch {
                bleGattClientRepository.packet.collect {
                    if (it == null) return@collect
                    onPacketReceived(it)
                }
            }

            return true
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Failed to start Bluetooth services: ${e.message}")
            isActive = false
            return false
        }
    }

    override fun stopServices() {
        ComNetLog.i(TAG, "Stopping power-optimized Bluetooth services")

        isActive = false

        connectionScope.launch {
            // Stop component managers
            bleGattClientRepository.stop()
            bleGattServerRepository.stop()


            // Stop connection tracker
            bleConnectionTrackerRepository.stop()

            // Cancel the coroutine scope
            // TODO: fix me
            // connectionScope.cancel()
            ComNetLog.i(TAG, "All Bluetooth services stopped")
        }
    }

    override fun broadcastPacket(routed: RoutedPacket) {
//        if (!isActive) return

        packetBroadcasterRepository.broadcastPacket(
            routed,
            bleGattServerRepository.getGattServer(),
            bleGattServerRepository.getCharacteristic()
        )
    }

    private fun onPacketReceived(packet: BlePacket) {
        packetProcessorRepository.processPacket(packet)
    }
}