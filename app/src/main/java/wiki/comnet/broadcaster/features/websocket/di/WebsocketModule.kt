package wiki.comnet.broadcaster.features.websocket.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.core.data.repository.DeviceIdRepositoryImpl
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.features.websocket.data.repository.UnifiedPushAppRepositoryImpl
import wiki.comnet.broadcaster.features.websocket.data.repository.WebSocketNotificationRepositoryImpl
import wiki.comnet.broadcaster.features.websocket.data.repository.WebSocketRepositoryImpl
import wiki.comnet.broadcaster.features.websocket.domain.repository.UnifiedPushAppRepository
import wiki.comnet.broadcaster.features.websocket.domain.repository.WebSocketNotificationRepository
import wiki.comnet.broadcaster.features.websocket.domain.repository.WebSocketRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebsocketModule {
    @Binds
    @Singleton
    abstract fun provideDeviceIdRepository(impl: DeviceIdRepositoryImpl): DeviceIdRepository

    @Binds
    @Singleton
    abstract fun provideWebSocketRepository(impl: WebSocketRepositoryImpl): WebSocketRepository

    @Binds
    @Singleton
    abstract fun provideWebSocketNotificationRepository(impl: WebSocketNotificationRepositoryImpl): WebSocketNotificationRepository

    @Binds
    @Singleton
    abstract fun provideUnifiedPushAppRepository(impl: UnifiedPushAppRepositoryImpl): UnifiedPushAppRepository
}