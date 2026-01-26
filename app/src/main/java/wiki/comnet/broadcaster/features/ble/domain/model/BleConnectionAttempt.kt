package wiki.comnet.broadcaster.features.ble.domain.model

import wiki.comnet.broadcaster.features.ble.constant.BleConfig

data class BleConnectionAttempt(
    val attempts: Int,
    val lastAttempt: Long = System.currentTimeMillis(),
) {
    fun isExpired(): Boolean =
        System.currentTimeMillis() - lastAttempt > BleConfig.CONNECTION_RETRY_DELAY * 2

    fun shouldRetry(): Boolean =
        attempts < BleConfig.MAX_CONNECTION_ATTEMPTS &&
                System.currentTimeMillis() - lastAttempt > BleConfig.CONNECTION_RETRY_DELAY
}