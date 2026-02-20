package wiki.comnet.broadcaster.features.logging.data.model

data class LogSyncRequest(
    val logs: List<LogEntry>,
)

data class LogEntry(
    val level: String,
    val tag: String,
    val message: String,
    val throwable: String?,
    val deviceId: String,
    val timestamp: Long,
)
