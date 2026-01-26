package wiki.comnet.broadcaster.core.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wiki.comnet.broadcaster.core.constant.AppConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharePreferenceModule {
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }
}