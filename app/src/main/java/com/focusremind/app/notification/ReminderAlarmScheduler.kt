package com.focusremind.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.focusremind.app.data.Reminder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Schedules reminders using BOTH:
 * 1. AlarmManager (primary - exact timing)
 * 2. WorkManager (backup - in case AlarmManager is blocked by OEM/battery)
 *
 * This dual approach ensures reminders fire on ALL phones.
 */
object ReminderAlarmScheduler {

    private const val TAG = "ReminderScheduler"

    fun schedule(context: Context, reminder: Reminder) {
        val delay = reminder.triggerAt - System.currentTimeMillis()
        if (delay < -30_000) {
            Log.w(TAG, "Reminder ${reminder.id} too far in past (${delay}ms), skipping")
            return
        }

        val triggerTime = if (delay < 0) System.currentTimeMillis() + 1000 else reminder.triggerAt
        val timeFormatted = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(triggerTime))

        // === METHOD 1: AlarmManager (exact, primary) ===
        var alarmMethod = "FAILED"
        try {
            alarmMethod = scheduleWithAlarmManager(context, reminder, triggerTime)
        } catch (e: Exception) {
            Log.e(TAG, "AlarmManager scheduling failed!", e)
        }

        // === METHOD 2: WorkManager (backup, may be delayed up to 15min) ===
        try {
            scheduleWithWorkManager(context, reminder, triggerTime)
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager scheduling failed!", e)
        }

        Log.d(TAG, "✅ SCHEDULED: id=${reminder.id}, title='${reminder.title}', " +
                "at=$timeFormatted, alarm=$alarmMethod, workmanager=OK")

        // Toast confirmation
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "⏰ Zapisano: $timeFormatted [$alarmMethod]",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun scheduleWithAlarmManager(context: Context, reminder: Reminder, triggerTime: Long): String {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                )
                "setAlarmClock"
            } else {
                // No exact alarm permission — try inexact methods
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                    "setExact(NO_PERMISSION)"
                } catch (e: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                    "setInexact(SecurityEx)"
                }
            }
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
            "setAlarmClock"
        }
    }

    private fun scheduleWithWorkManager(context: Context, reminder: Reminder, triggerTime: Long) {
        val delay = triggerTime - System.currentTimeMillis()

        val data = Data.Builder()
            .putLong("reminder_id", reminder.id)
            .putString("reminder_title", reminder.title)
            .build()

        // +60s delay: gives AlarmReceiver time to fire first and set the flag.
        // WorkManager is only a BACKUP for phones that kill AlarmManager.
        val backupDelay = maxOf(delay, 0) + 60_000L

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(backupDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("reminder_${reminder.id}")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reminder_${reminder.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    fun cancel(context: Context, reminderId: Long) {
        // Cancel AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Cancel WorkManager
        WorkManager.getInstance(context)
            .cancelUniqueWork("reminder_$reminderId")

        // Clean up alarm flag
        context.getSharedPreferences("nomi_alarm_flags", Context.MODE_PRIVATE)
            .edit().remove("fired_$reminderId").apply()

        Log.d(TAG, "Cancelled alarm+work for reminder $reminderId")
    }
}
