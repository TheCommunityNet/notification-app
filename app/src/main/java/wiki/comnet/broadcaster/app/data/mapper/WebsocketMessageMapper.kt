package wiki.comnet.broadcaster.app.data.mapper

import wiki.comnet.broadcaster.features.notification.domain.model.CachedNotification
import wiki.comnet.broadcaster.features.notification.domain.model.ExternalNotification
import wiki.comnet.broadcaster.features.websocket.domain.model.WebSocketMessage

fun WebSocketMessage.toCacheNotification(): CachedNotification {
    return CachedNotification(
        title = title ?: "",
        content = content,
        timestamp = timestamp,
    )
}

fun WebSocketMessage.toExternalNotification(): ExternalNotification {
    return ExternalNotification(
        title = title ?: "",
        content = content,
        category = category,
        url = url,
        isDialog = isDialog,
    )
}
