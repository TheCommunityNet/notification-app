package wiki.comnet.broadcaster.features.auth.domain.model

data class UserProfile(
    val sub: String,
    val name: String?,
    val email: String?,
    val preferredUsername: String?,
)