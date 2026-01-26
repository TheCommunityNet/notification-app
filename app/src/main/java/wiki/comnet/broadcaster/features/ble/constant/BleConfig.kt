package wiki.comnet.broadcaster.features.ble.constant

import java.util.UUID

class BleConfig {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("B36A172D-A9E2-4860-87FD-B36CE89A3780")

        val CHARACTERISTIC_UUID: UUID = UUID.fromString("B32CA618-89D6-49D6-A785-94AE3C601FC3")

        val DESCRIPTOR_UUID: UUID = UUID.fromString("0DE8B6E2-E675-44AA-8C5B-2FC2DABB9F8C")

        const val CONNECTION_RETRY_DELAY = 5000L

        const val MAX_CLIENT_CONNECTION = 5

        const val MAX_CONNECTION_ATTEMPTS = 3
        const val CLEANUP_DELAY = 500L
        const val CLEANUP_INTERVAL = 30000L // 30 seconds
    }
}