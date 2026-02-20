package wiki.comnet.broadcaster.features.logging.domain.repository

import wiki.comnet.broadcaster.features.logging.domain.model.LogLevel

interface LogRepository {
    suspend fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
    suspend fun syncPendingLogs(): Boolean
    suspend fun cleanUpOldLogs(retentionDays: Int = 7)
    suspend fun getUnsyncedCount(): Int
}
