package wiki.comnet.broadcaster.features.refer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import wiki.comnet.broadcaster.features.refer.data.network.CommunityNetApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReferModule {

    @Provides
    @Singleton
    fun provideCommunityNetApi(): CommunityNetApi {
        val client = OkHttpClient.Builder()
            .build()

        return Retrofit.Builder()
            .baseUrl(CommunityNetApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CommunityNetApi::class.java)
    }
}
