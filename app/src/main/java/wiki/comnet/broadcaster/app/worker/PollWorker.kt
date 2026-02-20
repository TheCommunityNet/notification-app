package wiki.comnet.broadcaster.app.worker

import android.content.Context
import wiki.comnet.broadcaster.features.logging.ComNetLog
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import wiki.comnet.broadcaster.app.up.Distributor
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.features.notification.domain.model.ExternalNotification
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationRepository
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketNotification
import wiki.comnet.broadcaster.features.websocket.domain.repository.WebSocketNotificationRepository

@HiltWorker
class PollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deviceIdRepository: DeviceIdRepository,
    private val notificationRepository: NotificationRepository,
    private val webSocketNotificationRepository: WebSocketNotificationRepository,
) : CoroutineWorker(context, params) {

    private val distributor by lazy {
        Distributor(applicationContext)
    }

    override suspend fun doWork(): Result {
        return try {
            val deviceId = deviceIdRepository.getDeviceId()
            webSocketNotificationRepository.getDeviceNotifications(deviceId)
                .forEach { deviceNotification ->
                    when (deviceNotification) {
                        is WebsocketNotification.MatrixNotification -> {
                            distributor.sendMessage(
                                deviceNotification.appId,
                                deviceNotification.connectorToken,
                                Gson().toJson(deviceNotification.payload).toByteArray(),
                            )
                        }

                        is WebsocketNotification.BasicNotification -> {
                            notificationRepository.buildExternalNotification(
                                ExternalNotification(
                                    title = deviceNotification.title ?: "",
                                    content = deviceNotification.content,
                                    category = deviceNotification.category,
                                    url = deviceNotification.category,
                                    isDialog = false
                                )
                            )
                        }
                    }
                }
            Result.success()
        } catch (e: Exception) {
            ComNetLog.d(TAG, e.message ?: "fail to back fill notification")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PollWorker"
        const val VERSION = 1
    }
}