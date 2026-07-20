package com.focusremind.app.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Foreground service that actually keeps the alarm sound/vibration alive.
 *
 * Before this existed, sound/vibration were started directly from
 * AlarmReceiver (a plain BroadcastReceiver) — which has NO protection from
 * Android's background execution limits once onReceive() returns. On many
 * devices (especially aggressive OEM battery managers — Xiaomi, Samsung,
 * Huawei) the sound got silenced after roughly 20-30 seconds even though the
 * code asked for indefinite looping, because nothing told the system "this
 * is an active alarm, don't kill it". A foreground service with its own
 * notification is the standard, Google-recommended way to get that
 * protection (the same pattern real alarm-clock apps use).
 */
class AlarmSoundService : Service() {

    companion object {
        private const val TAG = "AlarmSoundService"
        const val ACTION_STOP = "com.focusremind.app.ALARM_SOUND_STOP"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(EXTRA_REMINDER_ID, -1) ?: -1

        if (intent?.action == ACTION_STOP || reminderId == -1L) {
            if (reminderId != -1L) SoundPlayer.stop(reminderId)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        // Re-post the SAME rich alarm notification here too — this satisfies
        // the "a foreground service must show a notification" requirement
        // without creating a second, redundant one; it IS the alarm UI.
        val notification = ReminderNotificationBuilder.build(this, reminderId, title)
        startForeground(reminderId.toInt(), notification)

        val prefs = getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val soundIndex = prefs.getInt("notification_sound_index", 0)
        val volume = prefs.getFloat("notification_volume", 0.7f)
        val soundUri = getSoundByIndex(soundIndex)

        if (soundUri != null) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmSoundService, soundUri)
                    setVolume(volume, volume)
                    isLooping = true
                    prepare()
                    start()
                }
                SoundPlayer.register(reminderId, player = player)
                Log.d(TAG, "Playing sound (looping): $soundUri at volume $volume")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play custom sound, trying default", e)
                try {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(this@AlarmSoundService, defaultUri)
                        setVolume(volume, volume)
                        isLooping = true
                        prepare()
                        start()
                    }
                    SoundPlayer.register(reminderId, player = player)
                } catch (e2: Exception) {
                    Log.e(TAG, "Default sound also failed", e2)
                }
            }
        }

        if (vibrationEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                val pattern = longArrayOf(0, 800, 400, 800, 400, 800)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                SoundPlayer.register(reminderId, vibrator = vibrator)

                // Safety net: on some devices, the very first vibrate() call
                // right after waking from deep sleep can silently fail to
                // actually start. Retry once, shortly after — but ONLY if
                // this reminder hasn't already been stopped in the meantime,
                // so tapping "Done"/snooze during that brief window can
                // never make a vibration come back after being turned off.
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (SoundPlayer.isActive(reminderId)) {
                        try {
                            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                        } catch (_: Exception) {}
                    }
                }, 1500)
                Log.d(TAG, "Vibration started (repeating)")
            } catch (e: Exception) {
                Log.w(TAG, "Vibration failed", e)
            }
        }

        return START_NOT_STICKY
    }

    private fun getSoundByIndex(index: Int): Uri? {
        if (index == -1) return null
        if (index == -2) {
            val prefs = getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
            val uriString = prefs.getString("notification_sound_custom_uri", null)
            return if (uriString != null) {
                try { Uri.parse(uriString) } catch (_: Exception) { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        }
        if (index < 0) return null
        return try {
            val rm = RingtoneManager(this)
            rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
            val cursor = rm.cursor
            if (cursor.count == 0) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else rm.getRingtoneUri(index % cursor.count)
        } catch (_: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }
}
