package com.focusremind.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focusremind.app.FocusRemindApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-schedules all active reminders after device reboot.
 * AlarmManager alarms are lost on reboot, so we must restore them.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed - rescheduling all active reminders")

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = FocusRemindApp.instance.database.reminderDao()
                val reminders = dao.getActive().first()
                Log.d(TAG, "Found ${reminders.size} active reminders to reschedule")
                reminders.forEach { reminder ->
                    ReminderAlarmScheduler.schedule(context, reminder)
                    Log.d(TAG, "Rescheduled reminder ${reminder.id}: ${reminder.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling reminders after boot", e)
            } finally {
                pending.finish()
            }
        }
    }
}
