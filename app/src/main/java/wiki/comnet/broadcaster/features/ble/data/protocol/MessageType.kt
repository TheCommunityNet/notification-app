package wiki.comnet.broadcaster.features.ble.data.protocol

enum class MessageType(val value: UByte) {
    MESSAGE(0x02u),  // All user messages (private and broadcast)
    FRAGMENT(0x20u), // Fragmentation for large packets

    NOTIFICATION_TRACKING(0x21u);

    companion object {
        fun fromValue(value: UByte): MessageType? {
            return entries.find { it.value == value }
        }
    }
}