package wiki.comnet.broadcaster.features.auth.di


//import okhttp3.logging.HttpLoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import wiki.comnet.broadcaster.features.auth.constant.AuthConfig
import wiki.comnet.broadcaster.features.auth.data.api.KeycloakApi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object KeycloakApiModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
//        val loggingInterceptor = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }

        return OkHttpClient.Builder()
//            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("${AuthConfig.KEYCLOAK_ENDPOINT}/realms/${AuthConfig.KEYCLOAK_REALM}/protocol/openid-connect/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideKeycloakApi(retrofit: Retrofit): KeycloakApi {
        return retrofit.create(KeycloakApi::class.java)
    }
}