package wiki.comnet.broadcaster.features.notification.domain.repository

import android.app.Notification
import wiki.comnet.broadcaster.features.notification.domain.model.CachedNotification
import wiki.comnet.broadcaster.features.notification.domain.model.ExternalNotification

interface NotificationRepository {
    fun start()

    fun getWebsocketNotificationId(): Int

    fun createForegroundWebsocketNotification(): Notification

    fun createForegroundWebsocketNotification(message: String?): Notification

    fun updateForegroundWebsocketNotification(message: String?)

    fun buildExternalNotification(notification: ExternalNotification): Notification

    fun showExternalNotification(notification: Notification)

    fun addCachedNotification(notification: CachedNotification)

    fun getCachedNotifications(): List<CachedNotification>

    fun clearCachedNotifications()
}