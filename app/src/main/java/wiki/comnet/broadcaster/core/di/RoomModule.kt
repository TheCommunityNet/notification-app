package wiki.comnet.broadcaster.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.core.data.AppDatabase

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "community_net.db"
        ).build()
    }

    @Provides
    fun provideNotificationTrackingDao(appDatabase: AppDatabase) =
        appDatabase.notificationTrackingDao()
}