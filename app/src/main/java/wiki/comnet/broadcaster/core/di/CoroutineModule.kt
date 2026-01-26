package wiki.comnet.broadcaster.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    @ServiceScope
    @Provides
    @Singleton // Ensures only one instance exists for the SingletonComponent's lifetime
    fun provideServiceScope(
    ): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
}