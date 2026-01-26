package wiki.comnet.broadcaster.features.auth.domain.model

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)