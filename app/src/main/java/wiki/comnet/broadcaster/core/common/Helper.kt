package wiki.comnet.broadcaster.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun CoroutineScope.startRepeatingTask(
    repeatInterval: Long = 5000L,
    action: suspend () -> Unit,
) = launch {
    while (isActive) {
        val start = System.currentTimeMillis()

        action()

        val elapsed = System.currentTimeMillis() - start

        delay(maxOf(0, repeatInterval - elapsed))
    }
}