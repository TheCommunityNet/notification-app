package wiki.comnet.broadcaster.features.websocket.data.model

import com.google.gson.annotations.SerializedName

data class UnifiedPushAppDeleteResponse(
    val success: Boolean,
    @SerializedName("app_id")
    val appId: String?,
)