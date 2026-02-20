package wiki.comnet.broadcaster.features.logging.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import wiki.comnet.broadcaster.features.logging.data.room.entity.LogEntryEntity

@Dao
interface LogEntryDao {
    @Insert
    suspend fun insert(logEntry: LogEntryEntity)

    @Insert
    suspend fun insertAll(logEntries: List<LogEntryEntity>)

    @Query("SELECT * FROM log_entries WHERE is_synced = 0 ORDER BY created_at ASC LIMIT :limit")
    suspend fun getUnsyncedLogs(limit: Int = 100): List<LogEntryEntity>

    @Query("UPDATE log_entries SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM log_entries WHERE is_synced = 1 AND created_at < :timestamp")
    suspend fun deleteSyncedLogsBefore(timestamp: Long)

    @Query("DELETE FROM log_entries WHERE created_at < :timestamp")
    suspend fun deleteLogsBefore(timestamp: Long)

    @Query("SELECT COUNT(*) FROM log_entries WHERE is_synced = 0")
    suspend fun getUnsyncedCount(): Int
}
