package wiki.comnet.broadcaster.features.websocket.data.model

import com.google.gson.annotations.SerializedName

data class DeviceNotification(
    val id: String,
    val category: String,
    val data: Map<*, *>,
    @SerializedName("is_dialog")
    val isDialog: Boolean,
)

data class DeviceNotificationResponse(
    val data: List<DeviceNotification>,
)

