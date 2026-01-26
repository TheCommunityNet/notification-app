package wiki.comnet.broadcaster.features.websocket.domain.model

data class WebsocketMatrixMessage(
    val appId: String,
    val connectorToken: String,
    val payload: Map<*, *>,
)