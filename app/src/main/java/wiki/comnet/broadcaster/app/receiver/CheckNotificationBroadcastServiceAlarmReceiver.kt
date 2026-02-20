package wiki.comnet.broadcaster.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import wiki.comnet.broadcaster.features.logging.ComNetLog
import androidx.core.content.ContextCompat
import wiki.comnet.broadcaster.app.data.model.ServiceAction
import wiki.comnet.broadcaster.app.data.model.ServiceState
import wiki.comnet.broadcaster.app.service.NotificationBroadcastService

class CheckNotificationBroadcastServiceAlarmReceiver : BroadcastReceiver() {
    private val TAG = "CheckNotificationBroadcastServiceAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        ComNetLog.d(TAG, "onReceive: called")
        val serviceState = NotificationBroadcastService.readServiceState(context)
        if (serviceState == ServiceState.STOPPED) {
            Intent(context, NotificationBroadcastService::class.java).also {
                it.action = ServiceAction.START.name
                ContextCompat.startForegroundService(context, it)
            }
        }

        AlarmScheduler.scheduleServiceCheck(context)
    }
}