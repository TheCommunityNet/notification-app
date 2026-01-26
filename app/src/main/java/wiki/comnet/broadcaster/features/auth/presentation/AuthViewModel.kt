package wiki.comnet.broadcaster.features.auth.presentation

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.features.auth.domain.model.AuthState
import wiki.comnet.broadcaster.features.auth.domain.usecase.GetAuthStateUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.HandleAuthResultUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.LoadAuthStateUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.LoginUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.LogoutUseCase
import wiki.comnet.broadcaster.features.auth.domain.usecase.RefreshTokenUseCase
import javax.inject.Inject


/**
 * ViewModel for handling authentication-related UI logic
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val loadAuthStateUseCase: LoadAuthStateUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val handleAuthResultUseCase: HandleAuthResultUseCase,
) : ViewModel() {

    val authState: StateFlow<Result<AuthState>> = getAuthStateUseCase()

    init {
        loadAuthState()
    }

    fun loadAuthState() {
        viewModelScope.launch {
            loadAuthStateUseCase()
        }
    }

    fun login(launcher: ActivityResultLauncher<Intent>) {
        loginUseCase(launcher)
    }

    fun handleAuthResult(data: Intent) {
        viewModelScope.launch {
            handleAuthResultUseCase(data)
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            refreshTokenUseCase()
        }
    }
}