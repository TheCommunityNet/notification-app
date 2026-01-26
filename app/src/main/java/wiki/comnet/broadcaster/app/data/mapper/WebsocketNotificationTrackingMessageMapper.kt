package wiki.comnet.broadcaster.app.data.mapper

import wiki.comnet.broadcaster.features.notification.domain.model.NotificationTracking
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketNotificationTrackingMessage


fun NotificationTracking.toWebsocketNotificationTrackingMessage(): WebsocketNotificationTrackingMessage {
    return WebsocketNotificationTrackingMessage(
        deviceId = deviceId,
        userId = userId,
        notificationId = notificationId,
        receivedAt = receivedAt,
    )
}