package wiki.comnet.broadcaster.features.ble.domain.repository

import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket

interface BleFragmentRepository {
    fun createFragments(packet: BlePacket): List<BlePacket>

    fun handleFragment(packet: BlePacket): BlePacket?
}