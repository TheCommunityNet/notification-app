package wiki.comnet.broadcaster.features.auth.domain.usecase


import kotlinx.coroutines.flow.StateFlow
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.features.auth.domain.model.AuthState
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for observing authentication state
 */
class GetAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): StateFlow<Result<AuthState>> {
        return authRepository.authState
    }
}