package wiki.comnet.broadcaster.features.notification.data.mapper

import wiki.comnet.broadcaster.features.notification.data.room.entity.NotificationTrackingEntity
import wiki.comnet.broadcaster.features.notification.domain.model.NotificationTracking

fun NotificationTracking.toEntity(): NotificationTrackingEntity {
    return NotificationTrackingEntity(
        id = id,
        notificationId = notificationId,
        deviceId = deviceId,
        userId = userId,
        receivedAt = receivedAt,
        isSynced = isSynced,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun NotificationTrackingEntity.toDomain(): NotificationTracking {
    return NotificationTracking(
        id = id,
        notificationId = notificationId,
        deviceId = deviceId,
        userId = userId,
        receivedAt = receivedAt,
        isSynced = isSynced,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
