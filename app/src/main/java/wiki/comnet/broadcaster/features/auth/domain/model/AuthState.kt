package wiki.comnet.broadcaster.features.auth.domain.model

data class AuthState(
    val token: AuthToken,
    val profile: UserProfile,
)