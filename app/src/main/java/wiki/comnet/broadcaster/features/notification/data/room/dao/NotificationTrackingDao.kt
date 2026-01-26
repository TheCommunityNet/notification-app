package wiki.comnet.broadcaster.features.notification.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import wiki.comnet.broadcaster.features.notification.data.room.entity.NotificationTrackingEntity

@Dao
interface NotificationTrackingDao {
    @Insert
    suspend fun insert(notificationTracking: NotificationTrackingEntity)

    @Update
    suspend fun update(notificationTracking: NotificationTrackingEntity)

    @Delete
    suspend fun delete(notificationTracking: NotificationTrackingEntity)


    @Query("SELECT * FROM notification_tracking WHERE notification_id = :notificationId")
    suspend fun getNotificationTrackingByNotificationId(notificationId: String): NotificationTrackingEntity?

    @Query("SELECT * FROM notification_tracking WHERE is_synced is 0 ORDER BY id asc LIMIT 1")
    suspend fun getUnsyncedNotificationTracking(): NotificationTrackingEntity?

    @Query("DELETE FROM notification_tracking WHERE created_at < :timestamp")
    suspend fun deleteOldNotifications(timestamp: Long)
}