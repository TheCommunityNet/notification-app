package wiki.comnet.broadcaster.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import wiki.comnet.broadcaster.features.logging.ComNetLog

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    private const val SERVICE_CHECK_INTERVAL_MS = 20 * 60 * 1000L // 20 minutes
    private const val SERVICE_CHECK_REQUEST_CODE = 0

    fun scheduleServiceCheck(context: Context, delayMs: Long = SERVICE_CHECK_INTERVAL_MS) {
        val intent = Intent(context, CheckNotificationBroadcastServiceAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SERVICE_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() + delayMs

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            ComNetLog.w(TAG, "Exact alarm permission not granted, using inexact alarm")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            return
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } catch (e: SecurityException) {
            ComNetLog.w(TAG, "SecurityException scheduling exact alarm, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }
}
