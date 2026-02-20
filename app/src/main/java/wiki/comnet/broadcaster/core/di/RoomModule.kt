package wiki.comnet.broadcaster.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.core.data.AppDatabase
import javax.inject.Singleton

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `log_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `level` INTEGER NOT NULL,
                `tag` TEXT NOT NULL,
                `message` TEXT NOT NULL,
                `throwable` TEXT,
                `is_synced` INTEGER NOT NULL DEFAULT 0,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_log_entries_is_synced` ON `log_entries` (`is_synced`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_log_entries_created_at` ON `log_entries` (`created_at`)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "community_net.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideNotificationTrackingDao(appDatabase: AppDatabase) =
        appDatabase.notificationTrackingDao()

    @Provides
    fun provideLogEntryDao(appDatabase: AppDatabase) =
        appDatabase.logEntryDao()
}