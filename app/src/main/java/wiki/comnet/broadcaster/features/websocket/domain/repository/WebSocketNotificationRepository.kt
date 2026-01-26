package wiki.comnet.broadcaster.features.websocket.domain.repository

import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketNotification

interface WebSocketNotificationRepository {
    suspend fun getDeviceNotifications(deviceId: String): List<WebsocketNotification>
}