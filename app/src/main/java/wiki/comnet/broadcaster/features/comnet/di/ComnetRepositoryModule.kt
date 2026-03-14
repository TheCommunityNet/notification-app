package wiki.comnet.broadcaster.features.comnet.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.features.comnet.data.repository.ComnetRepositoryImpl
import wiki.comnet.broadcaster.features.comnet.domain.repository.ComnetRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ComnetRepositoryModule {
    @Binds
    @Singleton
    abstract fun provideComnetRepository(impl: ComnetRepositoryImpl): ComnetRepository
}