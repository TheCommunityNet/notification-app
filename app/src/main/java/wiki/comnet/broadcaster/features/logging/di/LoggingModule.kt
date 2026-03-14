package wiki.comnet.broadcaster.features.logging.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.features.logging.data.network.LogApi
import wiki.comnet.broadcaster.features.logging.data.repository.LogRepositoryImpl
import wiki.comnet.broadcaster.features.logging.data.room.dao.LogEntryDao
import wiki.comnet.broadcaster.features.logging.domain.repository.LogRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {

    @Provides
    @Singleton
    fun provideLogApi(): LogApi {
        val acceptApplicationJson = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
            )
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(acceptApplicationJson)
            .build()

        return Retrofit.Builder()
            .baseUrl(LogApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LogApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLogRepository(
        logEntryDao: LogEntryDao,
        logApi: LogApi,
        deviceIdRepository: DeviceIdRepository,
    ): LogRepository {
        return LogRepositoryImpl(logEntryDao, logApi, deviceIdRepository)
    }
}
