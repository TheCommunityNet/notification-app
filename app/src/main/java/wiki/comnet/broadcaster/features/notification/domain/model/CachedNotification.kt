package wiki.comnet.broadcaster.features.notification.domain.model

data class CachedNotification(
    val title: String,
    val content: String,
    val timestamp: Long,
)