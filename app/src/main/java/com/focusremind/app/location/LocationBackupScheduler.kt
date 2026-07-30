package com.focusremind.app.location

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules the periodic WiFi backup check via AlarmManager (the same
 * privileged, reliable mechanism regular reminders use) instead of
 * WorkManager — WorkManager's periodic jobs aren't given priority by the
 * system and can be delayed arbitrarily under Doze/battery optimization,
 * exactly the kind of unreliability this backup exists to avoid.
 *
 * This is a SAFETY NET for when the live WifiConnectionObserver missed the
 * actual connection event (most likely because the app process had been
 * killed) — it does not replace the live, fast path, which still fires
 * immediately (well, after the configured debounce) when the process is
 * alive to see the event.
 */
object LocationBackupScheduler {
    private const val REQUEST_CODE = 918_273
    private const val INTERVAL_MS = 15 * 60 * 1000L // 15 minutes — a reasonable balance of promptness vs battery

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LocationBackupCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + INTERVAL_MS

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }
}
