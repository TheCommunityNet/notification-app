package wiki.comnet.broadcaster.features.logging.data.repository

import android.util.Log
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.features.logging.data.model.LogEntry
import wiki.comnet.broadcaster.features.logging.data.model.LogSyncRequest
import wiki.comnet.broadcaster.features.logging.data.network.LogApi
import wiki.comnet.broadcaster.features.logging.data.room.dao.LogEntryDao
import wiki.comnet.broadcaster.features.logging.data.room.entity.LogEntryEntity
import wiki.comnet.broadcaster.features.logging.domain.model.LogLevel
import wiki.comnet.broadcaster.features.logging.domain.repository.LogRepository
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LogRepositoryImpl @Inject constructor(
    private val logEntryDao: LogEntryDao,
    private val logApi: LogApi,
    private val deviceIdRepository: DeviceIdRepository,
) : LogRepository {

    override suspend fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entity = LogEntryEntity(
            level = level.priority,
            tag = tag,
            message = message,
            throwable = stackTrace,
        )

        try {
            logEntryDao.insert(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist log entry", e)
        }
    }

    override suspend fun syncPendingLogs(): Boolean {
        return try {
            val deviceId = deviceIdRepository.getDeviceId()
            var hasMore = true

            while (hasMore) {
                val unsyncedLogs = logEntryDao.getUnsyncedLogs(SYNC_BATCH_SIZE)
                if (unsyncedLogs.isEmpty()) {
                    hasMore = false
                    continue
                }

                val request = LogSyncRequest(
                    logs = unsyncedLogs.map { entity ->
                        LogEntry(
                            level = LogLevel.fromPriority(entity.level).name,
                            tag = entity.tag,
                            message = entity.message,
                            throwable = entity.throwable,
                            deviceId = deviceId,
                            timestamp = entity.createdAt,
                        )
                    }
                )

                logApi.syncLogs(request)
                logEntryDao.markAsSynced(unsyncedLogs.map { it.id })

                if (unsyncedLogs.size < SYNC_BATCH_SIZE) {
                    hasMore = false
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync logs to server", e)
            false
        }
    }

    override suspend fun cleanUpOldLogs(retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        logEntryDao.deleteSyncedLogsBefore(cutoff)

        val harderCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays * 2L)
        logEntryDao.deleteLogsBefore(harderCutoff)
    }

    override suspend fun getUnsyncedCount(): Int {
        return logEntryDao.getUnsyncedCount()
    }

    companion object {
        private const val TAG = "LogRepository"
        private const val SYNC_BATCH_SIZE = 100
    }
}
