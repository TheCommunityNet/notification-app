package wiki.comnet.broadcaster.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import wiki.comnet.broadcaster.app.worker.CleanUpNotificationTrackingWorker
import wiki.comnet.broadcaster.app.worker.NotificationBroadcastWatchdogWorker
import wiki.comnet.broadcaster.app.worker.PollWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CommunityNetApp() : Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        val pollRequest = PeriodicWorkRequestBuilder<PollWorker>(
            15,
            TimeUnit.MINUTES,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PollWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            pollRequest,
        )

        val request = PeriodicWorkRequestBuilder<NotificationBroadcastWatchdogWorker>(
            15,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NotificationBroadcastWatchdogWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        val cleanUpNotificationRequest =
            PeriodicWorkRequestBuilder<CleanUpNotificationTrackingWorker>(
                7,
                TimeUnit.DAYS
            ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CleanUpNotificationTrackingWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanUpNotificationRequest
        )
    }
}