package wiki.comnet.broadcaster.features.auth.domain.usecase

import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for refreshing authentication token
 */
class RefreshTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.refreshToken()
    }
}