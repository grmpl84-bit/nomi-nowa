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
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.data.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                // Save player reference (keyed by this reminder's ID) so
                // NotificationActionReceiver can stop THIS one specifically,
                // without risk of clobbering/being clobbered by another alarm.
                SoundPlayer.register(reminderId, player = player)
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
                    SoundPlayer.register(reminderId, player = player)
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
                SoundPlayer.register(reminderId, vibrator = vibrator)
                Log.d(TAG, "Vibration started (repeating)")
            } catch (e: Exception) {
                Log.w(TAG, "Vibration failed", e)
            }
        }

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

    private fun getSoundByIndex(context: Context, index: Int): Uri? {
        if (index == -1) return null // silence
        if (index == -2) {
            // Custom user-picked sound (see SoundPickerScreen.CUSTOM_SOUND_INDEX)
            val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
            val uriString = prefs.getString("notification_sound_custom_uri", null)
            return if (uriString != null) {
                try { Uri.parse(uriString) } catch (_: Exception) { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        }
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
