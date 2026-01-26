package wiki.comnet.broadcaster.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import wiki.comnet.broadcaster.features.notification.data.room.dao.NotificationTrackingDao
import wiki.comnet.broadcaster.features.notification.data.room.entity.NotificationTrackingEntity

@Database(
    entities = [NotificationTrackingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationTrackingDao(): NotificationTrackingDao
}