package wiki.comnet.broadcaster.features.ble.domain.repository

import kotlinx.coroutines.flow.StateFlow
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket

interface BlePacketProcessorRepository {
    val message: StateFlow<BlePacket?>

    fun processPacket(packet: BlePacket) {}
}