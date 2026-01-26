package wiki.comnet.broadcaster.features.websocket.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import wiki.comnet.broadcaster.features.websocket.data.network.WebsocketApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebsocketNetworkModule {
//    private val interceptor: HttpLoggingInterceptor =
//        HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }

    private val client = OkHttpClient.Builder()
//        .addInterceptor(interceptor)
        // Add other interceptors or configurations
        .build()

    @Provides
    @Singleton
    fun providesAuthApi(): WebsocketApi {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(WebsocketApi.BASE_URL)
            .client(client)
            .build()
            .create(WebsocketApi::class.java)
    }
}