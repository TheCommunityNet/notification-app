package wiki.comnet.broadcaster.features.logging.domain.model

enum class LogLevel(val priority: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6),
    ASSERT(7);

    companion object {
        fun fromPriority(priority: Int): LogLevel =
            entries.firstOrNull { it.priority == priority } ?: DEBUG
    }
}
