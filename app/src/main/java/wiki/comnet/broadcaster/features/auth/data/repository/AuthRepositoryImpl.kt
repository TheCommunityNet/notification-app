package wiki.comnet.broadcaster.features.auth.data.repository

import android.content.Context
import android.content.Intent
import wiki.comnet.broadcaster.features.logging.ComNetLog
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import retrofit2.HttpException
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.core.common.startRepeatingTask
import wiki.comnet.broadcaster.features.auth.constant.AuthConfig
import wiki.comnet.broadcaster.features.auth.data.api.KeycloakApi
import wiki.comnet.broadcaster.features.auth.domain.model.AuthState
import wiki.comnet.broadcaster.features.auth.domain.model.AuthToken
import wiki.comnet.broadcaster.features.auth.domain.model.UserProfile
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import wiki.comnet.broadcaster.features.auth.domain.repository.SharePreferenceRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharePreferenceRepository: SharePreferenceRepository,
    private val keycloakApi: KeycloakApi,
) : AuthRepository {

    companion object {
        private val TAG = AuthRepository::class.java.simpleName
    }

    private val _authService = AuthorizationService(context)

    private val _authState = MutableStateFlow<Result<AuthState>>(Result.Initial)
    override val authState: StateFlow<Result<AuthState>> = _authState.asStateFlow()

    override suspend fun loadAuthState() {
        val oldAuthToken = sharePreferenceRepository.getAuthToken() ?: return
        _authState.value = Result.Loading

        try {
            val userInfo = keycloakApi.getUserInfo("Bearer ${oldAuthToken.accessToken}")

            val profile = UserProfile(
                sub = userInfo.sub,
                name = userInfo.name,
                email = userInfo.email,
                preferredUsername = userInfo.preferredUsername
            )

            _authState.value = Result.Success(
                AuthState(
                    token = oldAuthToken,
                    profile = profile
                )
            )
            ComNetLog.d(TAG, "Auth state loaded successfully")
        } catch (e: Exception) {
            ComNetLog.d(TAG, "Access token invalid, attempting refresh", e)
            tryRefreshOnLoad(oldAuthToken.refreshToken)
        }
    }

    private suspend fun tryRefreshOnLoad(refreshToken: String) {
        try {
            val response = keycloakApi.refreshToken(
                clientId = AuthConfig.CLIENT_ID,
                refreshToken = refreshToken,
            )

            val newAuthToken = AuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresIn = System.currentTimeMillis() + response.expiresIn * 1000,
            )

            val userInfo = keycloakApi.getUserInfo("Bearer ${newAuthToken.accessToken}")
            val profile = UserProfile(
                sub = userInfo.sub,
                name = userInfo.name,
                email = userInfo.email,
                preferredUsername = userInfo.preferredUsername
            )

            _authState.value = Result.Success(
                AuthState(token = newAuthToken, profile = profile)
            )
            sharePreferenceRepository.setAuthToken(newAuthToken)
            ComNetLog.d(TAG, "Auth state loaded after token refresh")
        } catch (e: Exception) {
            _authState.value = Result.Initial
            sharePreferenceRepository.clearAuthToken()
            ComNetLog.e(TAG, "Failed to refresh token on load", e)
        }
    }

    override fun login(launcher: ActivityResultLauncher<Intent>) {
        _authState.value = Result.Loading

        val serviceConfig = AuthorizationServiceConfiguration(
            AuthConfig.AUTHORIZATION_ENDPOINT.toUri(),
            AuthConfig.TOKEN_ENDPOINT.toUri(),
        )

        val authorizationRequest = AuthorizationRequest.Builder(
            serviceConfig,
            AuthConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            AuthConfig.REDIRECT_URI.toUri()
        )
            .setScope(AuthConfig.SCOPE)
            .build()

        val authIntent = _authService.getAuthorizationRequestIntent(authorizationRequest)
        launcher.launch(authIntent)
        ComNetLog.d(TAG, "Login initiated")
    }

    override suspend fun handleAuthorizationResponse(data: Intent) {
        val response = AuthorizationResponse.fromIntent(data)
        val exception = AuthorizationException.fromIntent(data)

        if (exception != null) {
            val error = Exception(exception.message ?: "Authorization failed")
            _authState.value = Result.Error(error)
            ComNetLog.e(TAG, "Authorization response error", error)
            return
        }

        if (response != null) {
            exchangeCodeForToken(response)
        }
    }

    override suspend fun refreshToken() {
        val currentAuthState = _authState.value.getOrNull()

        if (currentAuthState == null) {
            ComNetLog.e(TAG, "No auth state available for token refresh")
            return
        }

        val currentRefreshToken = currentAuthState.token.refreshToken

        if (currentRefreshToken.isEmpty()) {
            ComNetLog.e(TAG, "No refresh token available")
            logout()
            return
        }

        try {
            val response = keycloakApi.refreshToken(
                clientId = AuthConfig.CLIENT_ID,
                refreshToken = currentRefreshToken,
            )

            val authToken = AuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresIn = System.currentTimeMillis() + response.expiresIn * 1000,
            )

            _authState.value = Result.Success(
                AuthState(
                    token = authToken,
                    profile = currentAuthState.profile
                )
            )
            sharePreferenceRepository.setAuthToken(authToken)
            ComNetLog.d(TAG, "Token refreshed successfully")
        } catch (e: HttpException) {
            ComNetLog.e(TAG, "Token refresh failed with HTTP ${e.code()}", e)
            logout()
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Token refresh failed (transient)", e)
        }
    }

    override suspend fun getUserInfo(accessToken: String): Flow<Result<UserProfile>> {
        return flow {
            emit(Result.Loading)

            try {
                val profile = keycloakApi.getUserInfo("Bearer $accessToken")

                emit(
                    Result.Success(
                        UserProfile(
                            sub = profile.sub,
                            name = profile.name,
                            email = profile.email,
                            preferredUsername = profile.preferredUsername
                        )
                    )
                )
                ComNetLog.d(TAG, "User info retrieved successfully")

            } catch (e: Exception) {
                emit(Result.Error(e))
                ComNetLog.e(TAG, "Failed to get user info", e)
            }
        }
    }

    override suspend fun checkAndRefreshAccessTokenJob() = supervisorScope {
        startRepeatingTask(repeatInterval = AuthConfig.TOKEN_REFRESH_INTERVAL_MS) {
            authState.value.getOrNull()?.let { authState ->
                val currentTime = System.currentTimeMillis()

                if (authState.token.expiresIn - currentTime < AuthConfig.TOKEN_REFRESH_THRESHOLD_MS) {
                    try {
                        refreshToken()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    override suspend fun logout() {
        sharePreferenceRepository.clearAuthToken()
        _authState.value = Result.Initial
        ComNetLog.d(TAG, "User logged out")
    }

    override fun dispose() {
        _authService.dispose()
    }

    private suspend fun exchangeCodeForToken(response: AuthorizationResponse) {
        val code = response.authorizationCode
        val codeVerifier = response.request.codeVerifier

        if (code == null || codeVerifier == null) {
            _authState.value = Result.Error(Exception("Missing authorization code or code verifier"))
            return
        }

        try {
            val tokenResponse = keycloakApi.exchangeCode(
                clientId = AuthConfig.CLIENT_ID,
                code = code,
                redirectUri = AuthConfig.REDIRECT_URI,
                codeVerifier = codeVerifier,
            )

            val authToken = AuthToken(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresIn = System.currentTimeMillis() + tokenResponse.expiresIn * 1000,
            )

            val userInfo = keycloakApi.getUserInfo("Bearer ${authToken.accessToken}")
            val profile = UserProfile(
                sub = userInfo.sub,
                name = userInfo.name,
                email = userInfo.email,
                preferredUsername = userInfo.preferredUsername
            )

            _authState.value = Result.Success(
                AuthState(token = authToken, profile = profile)
            )
            sharePreferenceRepository.setAuthToken(authToken)
            ComNetLog.d(TAG, "Token exchange completed successfully")
        } catch (e: Exception) {
            _authState.value = Result.Error(e)
            ComNetLog.e(TAG, "Token exchange failed", e)
        }
    }
}
