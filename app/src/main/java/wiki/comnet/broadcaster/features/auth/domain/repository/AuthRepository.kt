package wiki.comnet.broadcaster.features.auth.domain.repository

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.features.auth.domain.model.AuthState
import wiki.comnet.broadcaster.features.auth.domain.model.UserProfile

interface AuthRepository {
    val authState: StateFlow<Result<AuthState>>

    suspend fun loadAuthState()

    fun login(launcher: ActivityResultLauncher<Intent>)

    suspend fun handleAuthorizationResponse(data: Intent)

    suspend fun refreshToken()

    suspend fun getUserInfo(accessToken: String): Flow<Result<UserProfile>>

    suspend fun logout()

    suspend fun checkAndRefreshAccessTokenJob(): Job

    fun dispose()
}