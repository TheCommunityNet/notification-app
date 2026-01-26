package wiki.comnet.broadcaster.core.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceIdRepository {
    override fun getDeviceId(): String {
        return getSettingDeviceId(context)
    }

    @SuppressLint("HardwareIds")
    private fun getSettingDeviceId(context: Context): String {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (androidId != null) {
            return androidId
        }
        return "unknown"
    }
}