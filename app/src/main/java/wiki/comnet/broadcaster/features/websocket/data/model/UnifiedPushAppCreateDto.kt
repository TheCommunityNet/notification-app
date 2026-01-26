package wiki.comnet.broadcaster.features.websocket.data.model

import com.google.gson.annotations.SerializedName

data class UnifiedPushAppCreateDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("app_id")
    val appId: String,
    @SerializedName("connector_token")
    val connectorToken: String,
)