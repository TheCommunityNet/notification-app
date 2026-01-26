package wiki.comnet.broadcaster.features.websocket.domain.model

data class WebSocketMessage(
    val id: String? = null,
    val category: String = "default",
    val title: String?,
    val content: String,
    val url: String? = null,
    val isDialog: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)