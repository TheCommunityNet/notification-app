package wiki.comnet.broadcaster.features.auth.data.model

import com.google.gson.annotations.SerializedName

data class UserInfoResponse(
    val sub: String,
    val name: String?,
    val email: String?,
    @SerializedName("preferred_username")
    val preferredUsername: String?,
)
