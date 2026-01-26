package wiki.comnet.broadcaster.features.websocket.domain.model

data class WebsocketNotificationTrackingMessage(
    val deviceId: String,
    val userId: String?,
    val notificationId: String,
    val receivedAt: Long,
    val sentAt: Long = System.currentTimeMillis(),
)