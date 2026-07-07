package com.focusremind.app.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.data.Reminder

/**
 * WorkManager backup for notifications.
 * This fires even if AlarmManager is blocked by OEM battery optimization.
 * May be delayed up to 15 minutes by system, but at least it WILL fire.
 *
 * Before showing notification, checks if AlarmReceiver already showed it
 * (using NotificationManager.activeNotifications).
 */
class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
    }

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong("reminder_id", -1)
        val title = inputData.getString("reminder_title") ?: "Przypomnienie"
        val recurrence = inputData.getString("reminder_recurrence")
        val triggerAt = inputData.getLong("reminder_trigger_at", System.currentTimeMillis())

        if (reminderId == -1L) return Result.failure()

        Log.d(TAG, "WorkManager backup fired for reminder $reminderId: $title")

        // Check if AlarmReceiver already fired (using SharedPreferences flag - reliable)
        val alarmFlags = context.getSharedPreferences("nomi_alarm_flags", Context.MODE_PRIVATE)
        val alreadyFired = alarmFlags.getBoolean("fired_$reminderId", false)
        if (alreadyFired) {
            Log.d(TAG, "AlarmReceiver already fired for reminder $reminderId, skipping backup")
            // Clean up the flag
            alarmFlags.edit().remove("fired_$reminderId").apply()
            return Result.success()
        }

        // AlarmReceiver didn't fire — show notification ourselves
        Log.w(TAG, "⚠️ AlarmReceiver DID NOT fire! WorkManager backup showing notification.")
        showNotification(reminderId, title)

        // Recurring reminder: since we're the ones who actually fired (AlarmReceiver
        // didn't), we're responsible for scheduling the next occurrence too.
        if (recurrence != null) {
            try {
                val nextTrigger = ReminderAlarmScheduler.nextTriggerTime(triggerAt, recurrence)
                FocusRemindApp.instance.database.reminderDao().snooze(reminderId, nextTrigger)
                alarmFlags.edit().putBoolean("fired_$reminderId", false).apply()
                ReminderAlarmScheduler.schedule(
                    context,
                    Reminder(id = reminderId, title = title, triggerAt = nextTrigger, recurrence = recurrence)
                )
                Log.d(TAG, "Recurring reminder $reminderId ($recurrence) rescheduled for $nextTrigger (via backup)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule recurring reminder $reminderId", e)
            }
        }

        return Result.success()
    }

    private fun showNotification(reminderId: Long, title: String) {
        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No POST_NOTIFICATIONS permission")
                return
            }
        }

        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Same amber, always-visible-buttons notification as AlarmReceiver
        val notification = ReminderNotificationBuilder.build(context, reminderId, title)
        nm.notify(reminderId.toInt(), notification)
        Log.d(TAG, "Backup notification shown for reminder $reminderId")

        // Vibrate
        if (vibrationEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
            } catch (_: Exception) {}
        }
    }
}
