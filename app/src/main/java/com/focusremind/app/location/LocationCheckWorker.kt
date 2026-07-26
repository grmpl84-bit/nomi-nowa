package com.focusremind.app.location

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.notification.AlarmSoundService
import com.focusremind.app.notification.ReminderNotificationBuilder
import androidx.core.content.ContextCompat

/**
 * Runs after the debounce delay (30s for Bluetooth/car, 3 min for WiFi/home
 * /work by default) — if it actually gets to run at all, that means the
 * connection held for the whole debounce window (a disconnect during that
 * window cancels this same unique work before it ever executes, via
 * WorkManager's enqueueUniqueWork REPLACE policy). Fires every reminder
 * tagged with the matching location trigger.
 */
class LocationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TRIGGER = "trigger"
        const val TAG = "LocationCheckWorker"
    }

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER) ?: return Result.failure()
        val dao = FocusRemindApp.instance.database.reminderDao()
        val matching = dao.getByLocationTrigger(trigger)

        Log.d(TAG, "Location trigger '$trigger' confirmed after debounce — ${matching.size} reminder(s) match")

        for (reminder in matching) {
            val notification = ReminderNotificationBuilder.build(applicationContext, reminder.id, reminder.title)
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(reminder.id.toInt(), notification)

            val serviceIntent = android.content.Intent(applicationContext, AlarmSoundService::class.java).apply {
                putExtra(AlarmSoundService.EXTRA_REMINDER_ID, reminder.id)
                putExtra(AlarmSoundService.EXTRA_TITLE, reminder.title)
            }
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        }

        return Result.success()
    }
}
