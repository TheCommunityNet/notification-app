package wiki.comnet.broadcaster.features.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.features.auth.data.repository.AuthRepositoryImpl
import wiki.comnet.broadcaster.features.auth.data.repository.SharePreferenceRepositoryImpl
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import wiki.comnet.broadcaster.features.auth.domain.repository.SharePreferenceRepository
import javax.inject.Singleton


/**
 * Dagger Hilt module for binding repository interfaces to their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSharePreferenceRepository(
        impl: SharePreferenceRepositoryImpl,
    ): SharePreferenceRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository
}