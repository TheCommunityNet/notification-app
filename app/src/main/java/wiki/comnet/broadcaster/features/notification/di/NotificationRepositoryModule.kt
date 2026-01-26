package wiki.comnet.broadcaster.features.notification.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.features.notification.data.repository.NotificationRepositoryImpl
import wiki.comnet.broadcaster.features.notification.data.repository.NotificationTrackingRepositoryImpl
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationRepository
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationTrackingRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationTrackingRepository(
        impl: NotificationTrackingRepositoryImpl,
    ): NotificationTrackingRepository
}