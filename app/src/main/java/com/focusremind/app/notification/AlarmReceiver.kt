package com.focusremind.app.notification

import android.Manifest
import android.app.NotificationManager
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
import androidx.core.content.ContextCompat

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

        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val soundIndex = prefs.getInt("notification_sound_index", 0)
        val volume = prefs.getFloat("notification_volume", 0.7f)

        // Get sound URI
        val soundUri = getSoundByIndex(context, soundIndex)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build notification (amber, always-visible buttons — same style everywhere)
        val notification = ReminderNotificationBuilder.build(context, reminderId, title)
        nm.notify(reminderId.toInt(), notification)
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
