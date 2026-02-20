package wiki.comnet.broadcaster.features.auth.data.model

import com.google.gson.annotations.SerializedName

data class KeycloakTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
)
