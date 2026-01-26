package wiki.comnet.broadcaster.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationTrackingRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanUpNotificationTrackingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationTrackingRepository: NotificationTrackingRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val expire = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            notificationTrackingRepository.deleteOldNotifications(expire) // this is suspend
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}