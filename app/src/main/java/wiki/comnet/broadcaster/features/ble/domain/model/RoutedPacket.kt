package wiki.comnet.broadcaster.features.ble.domain.model

data class RoutedPacket(
    val packet: BlePacket,
    val transferId: String? = null,
    val relayAddress: String? = null,
)