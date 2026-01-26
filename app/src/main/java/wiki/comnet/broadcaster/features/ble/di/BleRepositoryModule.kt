package wiki.comnet.broadcaster.features.ble.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.features.ble.data.repository.BleConnectionManagerRepositoryImpl
import wiki.comnet.broadcaster.features.ble.data.repository.BleConnectionTrackerRepositoryImpl
import wiki.comnet.broadcaster.features.ble.data.repository.BleFragmentRepositoryImpl
import wiki.comnet.broadcaster.features.ble.data.repository.BleGattClientRepositoryImpl
import wiki.comnet.broadcaster.features.ble.data.repository.BleGattServerRepositoryImpl
import wiki.comnet.broadcaster.features.ble.data.repository.BleMeshRepositoryImpl
import wiki.comnet.broadcaster.features.ble.data.repository.BlePacketBroadcasterRepositoryImpl
import wiki.comnet.broadcaster.features.ble.data.repository.BlePacketProcessorRepositoryImpl
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionManagerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleConnectionTrackerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleFragmentRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleGattClientRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleGattServerRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BleMeshRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BlePacketBroadcasterRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BlePacketProcessorRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BleRepositoryModule {
    @Binds
    @Singleton
    abstract fun provideBleConnectionTrackerRepository(
        impl: BleConnectionTrackerRepositoryImpl,
    ): BleConnectionTrackerRepository

    @Binds
    @Singleton
    abstract fun provideFragmentRepository(
        impl: BleFragmentRepositoryImpl,
    ): BleFragmentRepository

    @Binds
    @Singleton
    abstract fun provideBlePacketProcessorRepository(
        impl: BlePacketProcessorRepositoryImpl,
    ): BlePacketProcessorRepository

    @Binds
    @Singleton
    abstract fun provideBlePacketBroadcasterRepository(
        impl: BlePacketBroadcasterRepositoryImpl,
    ): BlePacketBroadcasterRepository

    @Binds
    @Singleton
    abstract fun provideBleGattClientRepository(
        impl: BleGattClientRepositoryImpl,
    ): BleGattClientRepository

    @Binds
    @Singleton
    abstract fun provideBleGattServerRepository(
        impl: BleGattServerRepositoryImpl,
    ): BleGattServerRepository

    @Binds
    @Singleton
    abstract fun provideBleConnectionManagerRepository(
        impl: BleConnectionManagerRepositoryImpl,
    ): BleConnectionManagerRepository


    @Binds
    @Singleton
    abstract fun provideBleMeshRepository(
        impl: BleMeshRepositoryImpl,
    ): BleMeshRepository
}