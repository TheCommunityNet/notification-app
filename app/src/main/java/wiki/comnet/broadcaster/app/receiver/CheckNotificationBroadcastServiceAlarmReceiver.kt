package wiki.comnet.broadcaster.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import wiki.comnet.broadcaster.app.data.model.ServiceAction
import wiki.comnet.broadcaster.app.data.model.ServiceState
import wiki.comnet.broadcaster.app.service.NotificationBroadcastService

class CheckNotificationBroadcastServiceAlarmReceiver : BroadcastReceiver() {
    private val TAG = "CheckNotificationBroadcastServiceAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: called")
        val serviceState = NotificationBroadcastService.readServiceState(context)
        if (serviceState == ServiceState.STOPPED) {
            Intent(context, NotificationBroadcastService::class.java).also {
                it.action = ServiceAction.START.name
                ContextCompat.startForegroundService(context, it)

            }
        }

        // Re-schedule next check
        scheduleNextCheck(context)
    }

    private fun scheduleNextCheck(context: Context) {
        val intent = Intent(context, CheckNotificationBroadcastServiceAlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 20 * 60 * 1000, // 20 min
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 20 * 60 * 1000, // 20 min
                pendingIntent
            )
        }
    }
}