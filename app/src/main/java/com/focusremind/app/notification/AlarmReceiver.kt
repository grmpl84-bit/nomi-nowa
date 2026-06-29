package com.focusremind.app.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.MainActivity

/**
 * Fired by AlarmManager at the exact scheduled time.
 * Shows a high-priority notification + plays sound via MediaPlayer + vibrates.
 *
 * IMPORTANT: On Android 8+ notification channel controls sound.
 * Since we want user-configurable sounds, we set channel sound=null
 * and play sound DIRECTLY via MediaPlayer (not through notification system).
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        val title = intent.getStringExtra("reminder_title") ?: "Przypomnienie"

        Log.d(TAG, "🔔 AlarmReceiver.onReceive FIRED! reminder=$reminderId, title='$title'")

        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, cannot show notification")
                return
            }
        }

        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val soundIndex = prefs.getInt("notification_sound_index", 0)
        val volume = prefs.getFloat("notification_volume", 0.7f)

        // Get sound URI
        val soundUri = getSoundByIndex(context, soundIndex)

        // Grammar: natural Polish
        val notificationText = buildNotificationText(title)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap intent - opens the app
        val tapIntent = PendingIntent.getActivity(
            context, reminderId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Done
        val doneIntent = PendingIntent.getBroadcast(
            context, (reminderId * 10).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "DONE"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze +5 min
        val snooze5Intent = PendingIntent.getBroadcast(
            context, (reminderId * 10 + 1).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_5"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze +15 min
        val snooze15Intent = PendingIntent.getBroadcast(
            context, (reminderId * 10 + 2).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_15"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification (NO sound on notification - we play it ourselves via MediaPlayer)
        val builder = NotificationCompat.Builder(context, FocusRemindApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Nomi")
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
            .addAction(android.R.drawable.ic_menu_recent_history, "+15 min", snooze15Intent)
            // Need setVibrate to trigger heads-up display (channel has no sound)
            .setVibrate(longArrayOf(0))

        nm.notify(reminderId.toInt(), builder.build())
        Log.d(TAG, "Notification shown for reminder $reminderId")

        // === PLAY SOUND via MediaPlayer (user's chosen sound) - LOOPING until dismissed ===
        if (soundUri != null) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    setDataSource(context, soundUri)
                    setVolume(volume, volume)
                    isLooping = true // LOOP until user dismisses!
                    prepare()
                    start()
                }
                // Save player reference so NotificationActionReceiver can stop it
                SoundPlayer.currentPlayer = player
                Log.d(TAG, "Playing sound (looping): $soundUri at volume $volume")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play custom sound, trying default", e)
                try {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val player = MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                        setDataSource(context, defaultUri)
                        setVolume(volume, volume)
                        isLooping = true
                        prepare()
                        start()
                    }
                    SoundPlayer.currentPlayer = player
                } catch (e2: Exception) {
                    Log.e(TAG, "Default sound also failed", e2)
                }
            }
        }

        // === VIBRATE (repeating until dismissed) ===
        if (vibrationEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                // Repeating vibration: 800ms on, 400ms off, loops from index 0
                val pattern = longArrayOf(0, 800, 400, 800, 400, 800)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat from start
                SoundPlayer.currentVibrator = vibrator
                Log.d(TAG, "Vibration started (repeating)")
            } catch (e: Exception) {
                Log.w(TAG, "Vibration failed", e)
            }
        }
    }

    private fun buildNotificationText(title: String): String {
        var cleanTitle = title
        cleanTitle = cleanTitle.replace(Regex("^przypomnij\\s*(mi\\s*)?", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.replace(Regex("^przypomnienie\\s*", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.trim()
        return if (cleanTitle.startsWith("o ") || cleanTitle.startsWith("O ")) {
            "Przypominam $cleanTitle"
        } else {
            "Przypominam: $cleanTitle"
        }
    }

    private fun getSoundByIndex(context: Context, index: Int): Uri? {
        if (index < 0) return null
        return try {
            val rm = RingtoneManager(context)
            rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
            val cursor = rm.cursor
            if (cursor.count == 0) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else rm.getRingtoneUri(index % cursor.count)
        } catch (_: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }
}
