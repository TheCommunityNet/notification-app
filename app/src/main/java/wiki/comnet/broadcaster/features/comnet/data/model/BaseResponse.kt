package wiki.comnet.broadcaster.features.comnet.data.model

data class BaseResponse<T>(
    val success: Boolean,
    val data: T,
    val message: String?,
)
