package com.focusremind.app.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.MainActivity

/**
 * Foreground service that plays alarm sound in a LOOP and vibrates
 * until user taps "Gotowe" or "Odłóż". This is the persistent alarm.
 * Uses the unified "nomi_reminders" channel.
 */
class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra("reminder_id", -1) ?: -1
        val title = intent?.getStringExtra("reminder_title") ?: "Przypomnienie"

        Log.d(TAG, "AlarmService started for reminder $reminderId")

        val prefs = getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val volume = prefs.getFloat("notification_volume", 0.7f)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val soundIndex = prefs.getInt("notification_sound_index", 0)

        // Full screen intent
        val fullIntent = PendingIntent.getActivity(
            this, reminderId.toInt(),
            Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Done
        val doneIntent = PendingIntent.getBroadcast(
            this, (reminderId * 10).toInt(),
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = "DONE"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze +5min
        val snooze5Intent = PendingIntent.getBroadcast(
            this, (reminderId * 10 + 1).toInt(),
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_5"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze +15min
        val snooze15Intent = PendingIntent.getBroadcast(
            this, (reminderId * 10 + 2).toInt(),
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_15"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Grammar: natural Polish
        val notificationText = buildNotificationText(title)

        val notification = NotificationCompat.Builder(this, FocusRemindApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Nomi")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullIntent, true)
            .setContentIntent(fullIntent)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Gotowe", doneIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+5 min", snooze5Intent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+15 min", snooze15Intent)
            .build()

        startForeground(reminderId.toInt().coerceAtLeast(1), notification)

        // Start LOOPING sound
        startSound(soundIndex, volume)

        // Start LOOPING vibration
        if (vibrationEnabled) startVibration()

        return START_NOT_STICKY
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

    private fun startSound(soundIndex: Int, volume: Float) {
        try {
            val soundUri = getSoundByIndex(soundIndex)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                setDataSource(applicationContext, soundUri)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary sound failed, trying fallback", e)
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    setDataSource(applicationContext, fallbackUri)
                    isLooping = true
                    setVolume(volume, volume)
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback sound also failed", e2)
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 300, 500, 300, 800, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat from index 0
    }

    private fun getSoundByIndex(index: Int): Uri? {
        if (index < 0) return null
        return try {
            val rm = RingtoneManager(applicationContext)
            rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
            val cursor = rm.cursor
            if (cursor.count == 0) null
            else rm.getRingtoneUri(index % cursor.count)
        } catch (_: Exception) { null }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        Log.d(TAG, "AlarmService destroyed")
    }
}
