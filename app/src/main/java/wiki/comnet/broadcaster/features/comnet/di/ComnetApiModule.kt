package wiki.comnet.broadcaster.features.comnet.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import wiki.comnet.broadcaster.features.comnet.data.network.ComnetApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ComnetApiModule {

    @Provides
    @Singleton
    fun provideCommunityNetApi(authRepository: AuthRepository): ComnetApi {
        val authInterceptor = Interceptor { chain ->
            val token = authRepository.authState.value.getOrNull()?.token?.accessToken
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                .apply {
                    token?.let { addHeader("Authorization", "Bearer $it") }
                }
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(ComnetApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ComnetApi::class.java)
    }
}
