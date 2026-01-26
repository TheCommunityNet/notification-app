package wiki.comnet.broadcaster.features.ble.domain.repository

import kotlinx.coroutines.flow.Flow
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import wiki.comnet.broadcaster.features.ble.domain.model.RoutedPacket

interface BleConnectionManagerRepository {
    val message: Flow<BlePacket?>

    fun startServices(): Boolean

    fun stopServices()

    fun broadcastPacket(routed: RoutedPacket)
}