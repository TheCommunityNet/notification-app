package wiki.comnet.broadcaster.features.auth.domain.repository

import wiki.comnet.broadcaster.features.auth.domain.model.AuthToken

interface SharePreferenceRepository {
    fun getAuthToken(): AuthToken?

    fun setAuthToken(authToken: AuthToken)

    fun clearAuthToken()
}