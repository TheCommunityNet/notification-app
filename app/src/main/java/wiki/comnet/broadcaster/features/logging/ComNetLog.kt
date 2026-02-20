package wiki.comnet.broadcaster.features.logging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.features.logging.domain.model.LogLevel
import wiki.comnet.broadcaster.features.logging.domain.repository.LogRepository

/**
 * Drop-in replacement for [android.util.Log] that persists logs to SQLite
 * for later sync to the server. Also forwards all logs to standard logcat.
 *
 * Must be initialized via [init] before use (typically in Application.onCreate).
 * Before initialization, falls back to standard logcat only.
 */
object ComNetLog {
    private var repository: LogRepository? = null
    private var scope: CoroutineScope? = null

    fun init(repository: LogRepository, scope: CoroutineScope) {
        this.repository = repository
        this.scope = scope
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.v(tag, message, throwable) else Log.v(tag, message)
        persist(LogLevel.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
        persist(LogLevel.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
        persist(LogLevel.INFO, tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
        persist(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        persist(LogLevel.ERROR, tag, message, throwable)
    }

    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.wtf(tag, message, throwable) else Log.wtf(tag, message)
        persist(LogLevel.ASSERT, tag, message, throwable)
    }

    private fun persist(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val repo = repository ?: return
        val coroutineScope = scope ?: return

        coroutineScope.launch {
            try {
                repo.log(level, tag, message, throwable)
            } catch (_: Exception) {
                // Avoid infinite recursion — don't log persistence failures through ComNetLog
            }
        }
    }
}
