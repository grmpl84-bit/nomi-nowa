package com.focusremind.app.notification

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.data.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager at the exact scheduled time.
 * Shows a high-priority notification, then hands off to AlarmSoundService
 * (a foreground service) to actually play sound/vibrate — a bare
 * BroadcastReceiver has no protection from Android's background execution
 * limits, so playback started directly here would get silenced by the OS
 * after a short while on many devices.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        val title = intent.getStringExtra("reminder_title") ?: "Przypomnienie"
        val recurrence = intent.getStringExtra("reminder_recurrence")
        val triggerAt = intent.getLongExtra("reminder_trigger_at", System.currentTimeMillis())

        Log.d(TAG, "🔔 AlarmReceiver.onReceive FIRED! reminder=$reminderId, title='$title'")

        // Mark as fired IMMEDIATELY — so WorkManager backup knows not to duplicate
        context.getSharedPreferences("nomi_alarm_flags", Context.MODE_PRIVATE)
            .edit().putBoolean("fired_$reminderId", true).apply()

        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, cannot show notification")
                return
            }
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build notification (amber, always-visible buttons — same style everywhere)
        val notification = ReminderNotificationBuilder.build(context, reminderId, title)
        nm.notify(reminderId.toInt(), notification)
        Log.d(TAG, "Notification shown for reminder $reminderId")

        // Hand off sound/vibration to the foreground service — it re-posts
        // this same notification (satisfying the foreground-service
        // requirement) and keeps playing reliably in the background.
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra(AlarmSoundService.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmSoundService.EXTRA_TITLE, title)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // === RECURRING REMINDER: schedule the next occurrence right away ===
        // Done here (not only when user taps "Zrobione") so a daily/weekly
        // reminder never breaks its chain even if the user ignores this one.
        if (recurrence != null) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = FocusRemindApp.instance.database.reminderDao()
                    val current = dao.getById(reminderId)
                    val anchor = current?.anchorTime ?: triggerAt
                    // Only a genuine cycle completion (this firing IS the
                    // anchor time, give or take alarm-timing slop) advances
                    // the schedule. A snoozed re-fire has a triggerAt that no
                    // longer matches the anchor — because the anchor was
                    // already advanced the moment the real cycle fired — so
                    // it must NOT advance again; it just needs the already-
                    // correct next alarm restored (the snooze's own
                    // rescheduling overwrote it with the temporary snoozed time).
                    val isGenuineCycleFire = kotlin.math.abs(triggerAt - anchor) < 60_000L
                    val nextTrigger = if (isGenuineCycleFire) {
                        ReminderAlarmScheduler.nextTriggerTime(anchor, recurrence).also {
                            dao.advanceRecurrence(reminderId, it)
                        }
                    } else {
                        // Safety net: if a very long snooze pushed us past the
                        // restored anchor time already, keep advancing until
                        // it's actually in the future instead of scheduling
                        // an alarm in the past.
                        var restored = anchor
                        while (restored <= System.currentTimeMillis()) {
                            restored = ReminderAlarmScheduler.nextTriggerTime(restored, recurrence)
                        }
                        if (restored != anchor) dao.advanceRecurrence(reminderId, restored)
                        restored
                    }
                    // Reset the "already fired" flag so the new cycle's backup Worker works correctly
                    context.getSharedPreferences("nomi_alarm_flags", Context.MODE_PRIVATE)
                        .edit().putBoolean("fired_$reminderId", false).apply()
                    ReminderAlarmScheduler.schedule(
                        context,
                        Reminder(id = reminderId, title = title, triggerAt = nextTrigger, recurrence = recurrence, anchorTime = nextTrigger)
                    )
                    Log.d(TAG, "Recurring reminder $reminderId ($recurrence) -> next=$nextTrigger (genuineCycle=$isGenuineCycleFire)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule recurring reminder $reminderId", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
