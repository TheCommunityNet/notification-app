package wiki.comnet.broadcaster.features.auth.domain.usecase

import android.content.Intent
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for handling authentication result from OAuth flow
 */
class HandleAuthResultUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(data: Intent) {
        authRepository.handleAuthorizationResponse(data)
    }
}