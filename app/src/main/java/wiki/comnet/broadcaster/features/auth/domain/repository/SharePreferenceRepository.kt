package wiki.comnet.broadcaster.features.auth.domain.repository

import wiki.comnet.broadcaster.features.auth.domain.model.AuthToken
import wiki.comnet.broadcaster.features.auth.domain.model.UserProfile

interface SharePreferenceRepository {
    fun getAuthToken(): AuthToken?

    fun setAuthToken(authToken: AuthToken)

    fun clearAuthToken()

    fun getAuthProfile(): UserProfile?

    fun setAuthProfile(profile: UserProfile)

    fun clearAuthProfile()
}