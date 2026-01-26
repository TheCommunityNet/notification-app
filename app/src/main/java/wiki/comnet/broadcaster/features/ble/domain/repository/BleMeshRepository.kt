package wiki.comnet.broadcaster.features.ble.domain.repository

import kotlinx.coroutines.flow.Flow
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket

interface BleMeshRepository {
    val message: Flow<BlePacket?>

    fun startServices()

    fun stopServices()

    fun sendMessage(id: String, message: String)

    fun sendTrackingMessage(id: String, message: String)
}