package wiki.comnet.broadcaster.features.comnet.data.model

import com.google.gson.annotations.SerializedName

data class ReferralRequest(
    @SerializedName("username") val username: String,
    @SerializedName("device_id") val deviceId: String,
)
