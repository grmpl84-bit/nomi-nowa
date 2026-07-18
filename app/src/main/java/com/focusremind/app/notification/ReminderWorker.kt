package com.focusremind.app.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
                val dao = FocusRemindApp.instance.database.reminderDao()
                val current = dao.getById(reminderId)
                val anchor = current?.anchorTime ?: triggerAt
                val isGenuineCycleFire = kotlin.math.abs(triggerAt - anchor) < 60_000L
                val nextTrigger = if (isGenuineCycleFire) {
                    ReminderAlarmScheduler.nextTriggerTime(anchor, recurrence).also {
                        dao.advanceRecurrence(reminderId, it)
                    }
                } else {
                    var restored = anchor
                    while (restored <= System.currentTimeMillis()) {
                        restored = ReminderAlarmScheduler.nextTriggerTime(restored, recurrence)
                    }
                    if (restored != anchor) dao.advanceRecurrence(reminderId, restored)
                    restored
                }
                alarmFlags.edit().putBoolean("fired_$reminderId", false).apply()
                ReminderAlarmScheduler.schedule(
                    context,
                    Reminder(id = reminderId, title = title, triggerAt = nextTrigger, recurrence = recurrence, anchorTime = nextTrigger)
                )
                Log.d(TAG, "Recurring reminder $reminderId ($recurrence) -> next=$nextTrigger (via backup, genuineCycle=$isGenuineCycleFire)")
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

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Same amber, always-visible-buttons notification as AlarmReceiver
        val notification = ReminderNotificationBuilder.build(context, reminderId, title)
        nm.notify(reminderId.toInt(), notification)
        Log.d(TAG, "Backup notification shown for reminder $reminderId")

        // Hand off to the same foreground service AlarmReceiver uses — this
        // path previously played only a short one-shot vibration, with no
        // sound at all, and no protection from background execution limits.
        val serviceIntent = android.content.Intent(context, AlarmSoundService::class.java).apply {
            putExtra(AlarmSoundService.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmSoundService.EXTRA_TITLE, title)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
