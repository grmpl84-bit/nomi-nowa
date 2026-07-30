package com.focusremind.app.location

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.notification.AlarmSoundService
import com.focusremind.app.notification.ReminderNotificationBuilder

/**
 * Shared "actually fire the location-tagged reminders" logic, used by both
 * the live debounce path (LocationCheckWorker) and the periodic AlarmManager
 * backup check (LocationBackupCheckReceiver) — so there's exactly one place
 * that does this, not two slightly-different copies.
 */
object LocationFirer {
    private const val TAG = "LocationFirer"

    fun presentKey(trigger: String) = "currently_at_$trigger"

    /**
     * Fires every reminder tagged with [trigger], but ONLY if we weren't
     * already marked as "present" there — without this, the periodic backup
     * check (every ~15 min) would re-fire the same reminders over and over
     * for as long as you stay connected, instead of once per arrival.
     */
    suspend fun fireIfNewArrival(context: Context, trigger: String) {
        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val key = presentKey(trigger)
        if (prefs.getBoolean(key, false)) {
            // Already marked present — this is a continued stay, not a new
            // arrival, so don't re-fire the same reminders again.
            return
        }
        prefs.edit().putBoolean(key, true).apply()
        fire(context, trigger)
    }

    /** Marks [trigger] as "not present" — called on disconnect/leaving, so the NEXT arrival can fire again. */
    fun markAbsent(context: Context, trigger: String) {
        context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
            .edit().putBoolean(presentKey(trigger), false).apply()
    }

    private suspend fun fire(context: Context, trigger: String) {
        val dao = FocusRemindApp.instance.database.reminderDao()
        val matching = dao.getByLocationTrigger(trigger)
        Log.d(TAG, "Location trigger '$trigger' confirmed — ${matching.size} reminder(s) match")

        for (reminder in matching) {
            val notification = ReminderNotificationBuilder.build(context, reminder.id, reminder.title)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(reminder.id.toInt(), notification)

            val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
                putExtra(AlarmSoundService.EXTRA_REMINDER_ID, reminder.id)
                putExtra(AlarmSoundService.EXTRA_TITLE, reminder.title)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
