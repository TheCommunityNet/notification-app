package wiki.comnet.broadcaster.features.notification.domain.model

data class NotificationTracking(
    val id: Int = 0,
    val notificationId: String,
    val deviceId: String,
    val userId: String?,
    val receivedAt: Long,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)