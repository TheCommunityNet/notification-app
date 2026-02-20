package wiki.comnet.broadcaster.app.worker

import android.app.Application
import android.content.Context
import android.content.Intent
import wiki.comnet.broadcaster.features.logging.ComNetLog
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import wiki.comnet.broadcaster.app.data.model.ServiceAction
import wiki.comnet.broadcaster.app.service.NotificationBroadcastService

class NotificationBroadcastWatchdogWorker(private val context: Context, params: WorkerParameters) :
    Worker(context, params) {

    companion object {
        private const val TAG = "NotificationBroadcastWatchdogWorker"
    }

    override fun doWork(): Result {
        val id = this.id
        if (context.applicationContext !is Application) {
            ComNetLog.d(TAG, "ServiceStartWorker: Failed, no application found (work ID: ${id})")
            return Result.failure()
        }
        val serviceState = NotificationBroadcastService.readServiceState(applicationContext)

        ComNetLog.d(
            TAG,
            "ServiceStartWorker: Starting foreground service with state $serviceState (work ID: ${id})"
        )

        Intent(context, NotificationBroadcastService::class.java).also {
            it.action = ServiceAction.START.name
            ContextCompat.startForegroundService(applicationContext, it)
        }

        return Result.success()
    }
}