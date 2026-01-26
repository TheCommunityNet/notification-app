package wiki.comnet.broadcaster.features.websocket.domain.repository

import kotlinx.coroutines.flow.Flow
import wiki.comnet.broadcaster.features.websocket.domain.model.WebSocketMessage
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketMatrixMessage
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketNotificationTrackingMessage

interface WebSocketRepository {
    val connectionState: Flow<Boolean>
    val messages: Flow<WebSocketMessage>

    val matrixMessage: Flow<WebsocketMatrixMessage>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun setUserId(userId: String)
    suspend fun sendMessage(message: String): Result<Unit>

    suspend fun sendNotificationTracking(tracking: WebsocketNotificationTrackingMessage): Result<Unit>
}