package wiki.comnet.broadcaster.features.websocket.data.repository

import wiki.comnet.broadcaster.features.websocket.data.network.WebsocketApi
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketNotification
import wiki.comnet.broadcaster.features.websocket.domain.repository.WebSocketNotificationRepository
import javax.inject.Inject

class WebSocketNotificationRepositoryImpl @Inject constructor(
    private val websocketApi: WebsocketApi,
) : WebSocketNotificationRepository {
    override suspend fun getDeviceNotifications(deviceId: String): List<WebsocketNotification> {
        val deviceNotifications = websocketApi.getDeviceNotifications(deviceId)

        val websocketNotification: List<WebsocketNotification?> = deviceNotifications.data.map {
            if (it.category == "matrix") {
                val appId = it.data["app_id"] as? String ?: return@map null
                val connectorToken = it.data["connector_token"] as? String ?: return@map null
                val payload = it.data["payload"] as? Map<*, *> ?: return@map null
                return@map WebsocketNotification.MatrixNotification(
                    appId = appId,
                    connectorToken = connectorToken,
                    payload = payload,
                )
            }
            return@map WebsocketNotification.BasicNotification(
                id = it.id,
                category = it.category,
                title = it.data["title"] as? String,
                content = it.data["content"] as? String ?: return@map null,
                url = it.data["url"] as? String,
                isDialog = it.isDialog,
                timestamp = it.data["timestamp"] as? Long ?: System.currentTimeMillis()
            )
        }

        return websocketNotification.filterNotNull()
    }
}