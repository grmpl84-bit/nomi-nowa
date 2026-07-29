package com.focusremind.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focusremind.app.FocusRemindApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles notification action buttons: Done, Snooze +5, Snooze +15.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        if (reminderId == -1L) return

        Log.d(TAG, "Action ${intent.action} for reminder $reminderId")

        // Tell AlarmSoundService to stop THIS reminder's sound/vibration and
        // remove itself from the foreground — harmless if the service isn't
        // actually running for this reminder (e.g. it fired via the backup
        // Worker path instead), Android just starts-and-immediately-stops it.
        //
        // CRITICAL: wrapped in try-catch. This call was previously
        // unprotected — if it threw (e.g. a background-service-start
        // restriction on some device/timing), the WHOLE onReceive() crashed
        // right here, before EVER reaching the notification cancel or the
        // database update below. The notification could still appear to
        // disappear from Android's own default dismiss-on-action-tap
        // behavior, creating the illusion that the button worked when
        // nothing in our code actually ran — exactly matching a report of
        // 'notification went away but nothing else happened'.
        try {
            val stopIntent = Intent(context, AlarmSoundService::class.java).apply {
                action = AlarmSoundService.ACTION_STOP
                putExtra(AlarmSoundService.EXTRA_REMINDER_ID, reminderId)
            }
            context.startService(stopIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop AlarmSoundService for $reminderId", e)
        }

        // Dismiss notification — safety net in case the service wasn't
        // running for this reminder (backup Worker path posts directly).
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(reminderId.toInt())

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = FocusRemindApp.instance.database.reminderDao()
                when (intent.action) {
                    "DONE" -> {
                        dao.complete(reminderId)
                        Log.d(TAG, "Reminder $reminderId marked as done")
                    }
                    "SNOOZE_5" -> {
                        val newTime = System.currentTimeMillis() + 5 * 60_000
                        dao.snooze(reminderId, newTime)
                        val reminder = dao.getById(reminderId)
                        if (reminder != null) {
                            ReminderAlarmScheduler.schedule(context, reminder.copy(triggerAt = newTime))
                            Log.d(TAG, "Reminder $reminderId snoozed +5 min")
                        }
                    }
                    "SNOOZE_15" -> {
                        val newTime = System.currentTimeMillis() + 15 * 60_000
                        dao.snooze(reminderId, newTime)
                        val reminder = dao.getById(reminderId)
                        if (reminder != null) {
                            ReminderAlarmScheduler.schedule(context, reminder.copy(triggerAt = newTime))
                            Log.d(TAG, "Reminder $reminderId snoozed +15 min")
                        }
                    }
                    "SNOOZE_30" -> {
                        val newTime = System.currentTimeMillis() + 30 * 60_000
                        dao.snooze(reminderId, newTime)
                        val reminder = dao.getById(reminderId)
                        if (reminder != null) {
                            ReminderAlarmScheduler.schedule(context, reminder.copy(triggerAt = newTime))
                            Log.d(TAG, "Reminder $reminderId snoozed +30 min")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling action ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }
}
