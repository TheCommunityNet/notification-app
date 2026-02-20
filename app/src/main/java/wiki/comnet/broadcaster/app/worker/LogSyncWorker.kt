package wiki.comnet.broadcaster.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import wiki.comnet.broadcaster.features.logging.domain.repository.LogRepository

@HiltWorker
class LogSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val logRepository: LogRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            logRepository.cleanUpOldLogs()
            val synced = logRepository.syncPendingLogs()
            if (synced) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.w(TAG, "Log sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LogSyncWorker"
        const val WORK_NAME = "LogSyncWorker"
    }
}
