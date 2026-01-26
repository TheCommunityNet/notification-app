package wiki.comnet.broadcaster.features.ble.domain.repository

import kotlinx.coroutines.flow.StateFlow
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket

interface BleGattClientRepository {
    val packet: StateFlow<BlePacket?>

    fun start(): Boolean

    fun stop()
}