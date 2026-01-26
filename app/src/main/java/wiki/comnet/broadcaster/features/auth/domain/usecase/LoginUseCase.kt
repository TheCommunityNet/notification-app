package wiki.comnet.broadcaster.features.auth.domain.usecase

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for handling user login
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(launcher: ActivityResultLauncher<Intent>) {
        authRepository.login(launcher)
    }
}