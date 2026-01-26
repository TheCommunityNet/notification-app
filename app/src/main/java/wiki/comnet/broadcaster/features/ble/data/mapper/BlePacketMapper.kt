package wiki.comnet.broadcaster.features.ble.data.mapper

import wiki.comnet.broadcaster.features.ble.data.protocol.BinaryProtocol
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket

fun BlePacket.toBinaryData(): ByteArray? {
    return BinaryProtocol.encode(this)
}

fun ByteArray.toBlePacket(): BlePacket? {
    return BinaryProtocol.decode(this)
}