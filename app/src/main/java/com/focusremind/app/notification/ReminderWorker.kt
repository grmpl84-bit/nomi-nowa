package com.focusremind.app.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.MainActivity

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

        // Grammar
        var cleanTitle = title
        cleanTitle = cleanTitle.replace(Regex("^przypomnij\\s*(mi\\s*)?", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.replace(Regex("^przypomnienie\\s*", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.trim()
        val notificationText = if (cleanTitle.startsWith("o ") || cleanTitle.startsWith("O ")) {
            "Przypominam $cleanTitle"
        } else {
            "Przypominam: $cleanTitle"
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = PendingIntent.getActivity(
            context, reminderId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = PendingIntent.getBroadcast(
            context, (reminderId * 10).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "DONE"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze5Intent = PendingIntent.getBroadcast(
            context, (reminderId * 10 + 1).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_5"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FocusRemindApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Nomi (backup)")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .setFullScreenIntent(tapIntent, true)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Gotowe", doneIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+5 min", snooze5Intent)
            .build()

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
