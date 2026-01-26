package wiki.comnet.broadcaster.features.auth.domain.usecase

import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for loading stored authentication state
 */
class LoadAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.loadAuthState()
    }
}