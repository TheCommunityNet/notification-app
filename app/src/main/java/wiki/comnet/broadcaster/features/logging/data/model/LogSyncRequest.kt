package wiki.comnet.broadcaster.features.logging.data.model

import com.google.gson.annotations.SerializedName

data class LogSyncRequest(
    val logs: List<LogEntry>,
)

data class LogEntry(
    val level: Int,
    val tag: String,
    val message: String,
    val throwable: String?,
    @SerializedName("device_id")
    val deviceId: String,
    val timestamp: Long,
)
