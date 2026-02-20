package wiki.comnet.broadcaster.features.refer.data.model

import com.google.gson.annotations.SerializedName

data class ReferRequest(
    @SerializedName("refer_code") val referCode: String,
    @SerializedName("device_id") val deviceId: String,
)
