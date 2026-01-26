package wiki.comnet.broadcaster.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.core.data.repository.WifiConnectionRepositoryImpl
import wiki.comnet.broadcaster.core.domain.repository.WifiConnectionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideWifiConnectionRepository(
        @ApplicationContext context: Context,
    ): WifiConnectionRepository {
        return WifiConnectionRepositoryImpl(context)
    }
}