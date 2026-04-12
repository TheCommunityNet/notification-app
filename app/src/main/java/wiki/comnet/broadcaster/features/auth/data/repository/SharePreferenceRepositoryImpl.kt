package wiki.comnet.broadcaster.features.auth.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import wiki.comnet.broadcaster.features.auth.domain.model.AuthToken
import wiki.comnet.broadcaster.features.auth.domain.model.UserProfile
import wiki.comnet.broadcaster.features.auth.domain.repository.SharePreferenceRepository
import javax.inject.Inject

class SharePreferenceRepositoryImpl @Inject constructor(
    private val sharePreference: SharedPreferences,
) : SharePreferenceRepository {

    companion object {
        private const val AUTH_TOKEN_KEY = "auth_token"
        private const val AUTH_PROFILE_KEY = "auth_profile"
    }


    override fun getAuthToken(): AuthToken? {
        return sharePreference.getString(AUTH_TOKEN_KEY, null)?.let {
            return try {
                Gson().fromJson(it, AuthToken::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun setAuthToken(authToken: AuthToken) {
        val json = Gson().toJson(authToken)
        sharePreference.edit { putString(AUTH_TOKEN_KEY, json) }
    }

    override fun clearAuthToken() {
        sharePreference.edit { remove(AUTH_TOKEN_KEY) }
    }

    override fun getAuthProfile(): UserProfile? {
        return sharePreference.getString(AUTH_PROFILE_KEY, null)?.let {
            return try {
                Gson().fromJson(it, UserProfile::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun setAuthProfile(profile: UserProfile) {
        sharePreference.edit { putString(AUTH_PROFILE_KEY, Gson().toJson(profile)) }
    }

    override fun clearAuthProfile() {
        sharePreference.edit { remove(AUTH_PROFILE_KEY) }
    }
}