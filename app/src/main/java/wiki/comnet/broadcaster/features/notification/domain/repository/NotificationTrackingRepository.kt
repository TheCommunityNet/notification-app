package wiki.comnet.broadcaster.features.notification.domain.repository

import wiki.comnet.broadcaster.features.notification.domain.model.NotificationTracking

interface NotificationTrackingRepository {

    suspend fun start()

    suspend fun stop()

    fun addCacheKey(notificationId: String)

    suspend fun addTracking(tracking: NotificationTracking)

    suspend fun isReceived(notificationId: String): Boolean

    suspend fun getNotSyncedTracking(): NotificationTracking?

    suspend fun deleteTracking(tracking: NotificationTracking)

    suspend fun deleteOldNotifications(timestamp: Long)

}