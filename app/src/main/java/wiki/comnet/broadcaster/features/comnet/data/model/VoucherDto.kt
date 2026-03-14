package wiki.comnet.broadcaster.features.comnet.data.model

import com.google.gson.annotations.SerializedName

data class VoucherDto(
    val id: String,
    val name: String,
    @SerializedName("expired_in")
    val expiredIn: Int,
    @SerializedName("expired_at")
    val expiredAt: String,
)