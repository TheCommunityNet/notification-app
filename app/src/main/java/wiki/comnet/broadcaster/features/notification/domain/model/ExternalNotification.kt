package wiki.comnet.broadcaster.features.notification.domain.model

data class ExternalNotification(
    val title: String,
    val content: String,
    val category: String,
    val url: String?,
    val isDialog: Boolean,
)