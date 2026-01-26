package wiki.comnet.broadcaster.app.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class RestartServiceWorker(private val context: Context) {
    fun refresh() {
        val workManager = WorkManager.getInstance(context)
        val startServiceRequest =
            OneTimeWorkRequest.Builder(NotificationBroadcastWatchdogWorker::class.java)
                .build()
        workManager.enqueueUniqueWork(
            WORK_NAME_ONCE,
            ExistingWorkPolicy.KEEP,
            startServiceRequest
        ) // Unique avoids races!
    }

    companion object {
        const val WORK_NAME_ONCE = "RestartServiceWorkerOnce"

        fun refresh(context: Context) {
            val manager = RestartServiceWorker(context)
            manager.refresh()
        }
    }
}