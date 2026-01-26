package wiki.comnet.broadcaster.features.notification.data.repository

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import wiki.comnet.broadcaster.R
import wiki.comnet.broadcaster.features.notification.domain.model.CachedNotification
import wiki.comnet.broadcaster.features.notification.domain.model.ExternalNotification
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationRepository
import wiki.comnet.broadcaster.features.notification.presentation.NotificationDialogActivity
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
) : NotificationRepository {
    companion object {
        private const val TAG = "NotificationRepository"

        private const val WEBSOCKET_CHANNEL_ID = "WebSocketServiceChannel"

        private const val WEBSOCKET_NOTIFICATION_ID = 1001

        const val EXTERNAL_CHANNEL_ID = "ExternalChannel"
    }

    private val cachedNotifications = mutableListOf<CachedNotification>()

    override fun start() {
        setupChannel()
    }

    override fun getWebsocketNotificationId(): Int {
        return WEBSOCKET_NOTIFICATION_ID
    }

    private fun setupChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                WEBSOCKET_CHANNEL_ID,
                "WebSocket Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            val externalChannel = NotificationChannel(
                EXTERNAL_CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(externalChannel)
        }
    }

    override fun createForegroundWebsocketNotification(): Notification {
        return createForegroundWebsocketNotification(null)
    }

    override fun createForegroundWebsocketNotification(message: String?): Notification {
        return NotificationCompat.Builder(context, WEBSOCKET_CHANNEL_ID)
            .setContentTitle("WebSocket Service")
            .setContentText(message ?: "Listening for messages...")
            .setSmallIcon(R.drawable.ic_logo)
            .setOngoing(true)
            .build()
    }

    override fun updateForegroundWebsocketNotification(message: String?) {
        val notification = NotificationCompat.Builder(context, WEBSOCKET_CHANNEL_ID)
            .setContentTitle("WebSocket Service")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_logo)
            .setOngoing(true)
            .build()

        notificationManager.notify(WEBSOCKET_NOTIFICATION_ID, notification)
    }

    @SuppressLint("FullScreenIntentPolicy")
    override fun buildExternalNotification(notification: ExternalNotification): Notification {
        val icon = when (notification.category) {
            "emergency" -> android.R.drawable.ic_dialog_alert
            else -> android.R.drawable.ic_dialog_info
        }
        val builder = NotificationCompat.Builder(
            context,
            EXTERNAL_CHANNEL_ID
        )
            .setContentTitle(notification.title)
            .setContentText(notification.content)
            .setSmallIcon(icon)
            .setAutoCancel(true)

        if (notification.isDialog) {
            val intent = Intent(context, NotificationDialogActivity::class.java)
            builder.setFullScreenIntent(
                PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                ), true
            )
        } else if (notification.url != null) {
            val intent = Intent(Intent.ACTION_VIEW, notification.url.toUri())
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        return builder.build()
    }

    override fun showExternalNotification(notification: Notification) {
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun addCachedNotification(notification: CachedNotification) {
        cachedNotifications.add(notification)
    }

    override fun getCachedNotifications(): List<CachedNotification> {
        return cachedNotifications
    }

    override fun clearCachedNotifications() {
        cachedNotifications.clear()
    }
}