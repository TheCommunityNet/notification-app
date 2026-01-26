package wiki.comnet.broadcaster.features.ble.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.di.ServiceScope
import wiki.comnet.broadcaster.features.ble.data.protocol.MessageType
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import wiki.comnet.broadcaster.features.ble.domain.model.RoutedPacket
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionManagerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleMeshRepository
import javax.inject.Inject

class BleMeshRepositoryImpl @Inject constructor(
    @param:ServiceScope private val serviceScope: CoroutineScope,
    private val bleConnectionManagerRepository: BleConnectionManagerRepository,
) : BleMeshRepository {
    companion object {
        private val TAG = BleMeshRepository::class.java.simpleName
    }

    override val message = bleConnectionManagerRepository.message

    private var isActive = false

    override fun startServices() {
        if (isActive) {
            Log.w(TAG, "Mesh service already active, ignoring duplicate start request")
            return
        }

        if (bleConnectionManagerRepository.startServices()) {
            isActive = true

        } else {
            Log.e(TAG, "Failed to start Bluetooth services")
        }
    }

    override fun stopServices() {
        if (!isActive) {
            Log.w(TAG, "Mesh service not active, ignoring stop request")
            return
        }

        Log.i(TAG, "Stopping Bluetooth mesh service")

        isActive = false

        serviceScope.launch {
            delay(200) // Give leave message time to send
            bleConnectionManagerRepository.stopServices()
        }
    }

    override fun sendMessage(id: String, message: String) {
        bleConnectionManagerRepository.broadcastPacket(
            RoutedPacket(
                packet = BlePacket(
                    type = MessageType.MESSAGE.value,
                    id = id,
                    payload = message.toByteArray()
                )
            )
        )
    }

    override fun sendTrackingMessage(id: String, message: String) {
        bleConnectionManagerRepository.broadcastPacket(
            RoutedPacket(
                packet = BlePacket(
                    type = MessageType.NOTIFICATION_TRACKING.value,
                    id = id,
                    payload = message.toByteArray()
                )
            )
        )
    }
}