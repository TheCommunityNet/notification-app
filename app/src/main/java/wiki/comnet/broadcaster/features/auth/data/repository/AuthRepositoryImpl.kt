package wiki.comnet.broadcaster.features.auth.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
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
    private var _authorizationRequest: AuthorizationRequest? = null

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
            Log.d(TAG, "Auth state loaded successfully")
        } catch (e: Exception) {
            _authState.value = Result.Initial
            Log.e(TAG, "Failed to load auth state", e)
        }
    }

    override fun login(launcher: ActivityResultLauncher<Intent>) {
        _authState.value = Result.Loading

        val serviceConfig = AuthorizationServiceConfiguration(
            AuthConfig.AUTHORIZATION_ENDPOINT.toUri(),
            AuthConfig.TOKEN_ENDPOINT.toUri(),
        )

        _authorizationRequest = AuthorizationRequest.Builder(
            serviceConfig,
            AuthConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            AuthConfig.REDIRECT_URI.toUri()
        )
            .setScope(AuthConfig.SCOPE)
            .build()

        val authIntent = _authService.getAuthorizationRequestIntent(_authorizationRequest!!)
        launcher.launch(authIntent)
        Log.d(TAG, "Login initiated")
    }

    override suspend fun handleAuthorizationResponse(data: Intent) {
        val response = AuthorizationResponse.fromIntent(data)
        val exception = AuthorizationException.fromIntent(data)

        if (exception != null) {
            val error = Exception(exception.message ?: "Authorization failed")
            _authState.value = Result.Error(error)
            Log.e(TAG, "Authorization response error", error)
            return
        }

        if (response != null) {
            exchangeCodeForToken(response)
        }
    }

    override suspend fun refreshToken() {
        val currentAuthState = _authState.value.getOrNull()

        if (currentAuthState == null) {
            Log.e(TAG, "No auth state available for token refresh")
            return
        }

        val currentRefreshToken = currentAuthState.token.refreshToken

        if (currentRefreshToken.isEmpty()) {
            Log.e(TAG, "No refresh token available")
            logout()
            return
        }

        val serviceConfig = AuthorizationServiceConfiguration(
            AuthConfig.AUTHORIZATION_ENDPOINT.toUri(),
            AuthConfig.TOKEN_ENDPOINT.toUri(),
        )

        val refreshRequest = TokenRequest.Builder(serviceConfig, AuthConfig.CLIENT_ID)
            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .setRefreshToken(currentRefreshToken)
            .build()

        _authService.performTokenRequest(refreshRequest) { tokenResponse, exception ->
            if (exception != null || tokenResponse == null) {
                Log.e(TAG, "Token refresh failed", exception)
                CoroutineScope(Dispatchers.IO).launch {
                    logout()
                }
                return@performTokenRequest
            }

            val authToken = AuthToken(
                accessToken = tokenResponse.accessToken!!,
                refreshToken = tokenResponse.refreshToken!!,
                expiresIn = tokenResponse.accessTokenExpirationTime!!,
            )

            _authState.value = Result.Success(
                AuthState(
                    token = authToken,
                    profile = currentAuthState.profile
                )
            )
            Log.d(TAG, "Token refreshed successfully")
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
                Log.d(TAG, "User info retrieved successfully")

            } catch (e: Exception) {
                emit(Result.Error(e))
                Log.e(TAG, "Failed to get user info", e)
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
        Log.d(TAG, "User logged out")
    }

    override fun dispose() {
        _authService.dispose()
    }

    private fun exchangeCodeForToken(response: AuthorizationResponse) {
        val tokenRequest = response.createTokenExchangeRequest()

        _authService.performTokenRequest(tokenRequest) { tokenResponse, exception ->
            if (exception != null) {
                val error = Exception(exception.message ?: "Token exchange failed")
                _authState.value = Result.Error(error)
                Log.e(TAG, "Token exchange error", error)
                return@performTokenRequest
            }

            if (
                tokenResponse == null
                || tokenResponse.accessToken == null
                || tokenResponse.refreshToken == null
                || tokenResponse.accessTokenExpirationTime == null
            ) {
                val error = Exception("Token exchange failed - invalid response")
                _authState.value = Result.Error(error)
                Log.e(TAG, "Token exchange failed", error)
                return@performTokenRequest
            }

            val authToken = AuthToken(
                accessToken = tokenResponse.accessToken!!,
                refreshToken = tokenResponse.refreshToken!!,
                expiresIn = tokenResponse.accessTokenExpirationTime!!,
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userInfo = keycloakApi.getUserInfo("Bearer ${authToken.accessToken}")

                    val profile = UserProfile(
                        sub = userInfo.sub,
                        name = userInfo.name,
                        email = userInfo.email,
                        preferredUsername = userInfo.preferredUsername
                    )

                    _authState.value = Result.Success(
                        AuthState(
                            token = authToken,
                            profile = profile
                        )
                    )

                    sharePreferenceRepository.setAuthToken(authToken)
                    Log.d(TAG, "Token exchange completed successfully")
                } catch (e: Exception) {
                    _authState.value = Result.Error(e)
                    Log.e(TAG, "Failed to get user info after token exchange", e)
                }
            }
        }
    }
}