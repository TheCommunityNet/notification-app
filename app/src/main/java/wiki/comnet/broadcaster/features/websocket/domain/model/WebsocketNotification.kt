package wiki.comnet.broadcaster.features.websocket.domain.model

sealed class WebsocketNotification {
    data class MatrixNotification(
        val appId: String,
        val connectorToken: String,
        val payload: Map<*, *>,
    ) : WebsocketNotification()

    data class BasicNotification(
        val id: String? = null,
        val category: String = "default",
        val title: String?,
        val content: String,
        val url: String? = null,
        val isDialog: Boolean = false,
        val timestamp: Long = System.currentTimeMillis(),
    ) : WebsocketNotification()
}