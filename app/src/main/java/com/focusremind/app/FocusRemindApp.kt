package com.focusremind.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.room.Room
import com.focusremind.app.data.FocusRemindDatabase
import java.util.Locale

class FocusRemindApp : Application() {

    lateinit var database: FocusRemindDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, FocusRemindDatabase::class.java, "focusremind.db")
            .fallbackToDestructiveMigration()
            .build()
        createNotificationChannel()
        autoDetectLanguage()
    }

    /**
     * Auto-detect system language on first launch.
     * If the system language matches one of our supported languages,
     * set the app to use that language automatically.
     * On Android 13+, the system handles per-app language via localeConfig,
     * so we only do this on older Android versions.
     */
    private fun autoDetectLanguage() {
        // On Android 13+, the system handles locales via localeConfig - don't interfere
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

        val prefs = getSharedPreferences("nomi_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("language_auto_detected", false)) return

        // Only auto-detect AFTER onboarding is done — otherwise we'd skip the language
        // selection screen (getApplicationLocales() would be non-empty on first launch)
        if (!prefs.getBoolean("onboarding_done", false)) return

        // Only auto-detect if user hasn't manually set a language
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) return

        val systemLang = Locale.getDefault().language
        val supportedLanguages = listOf("pl", "en", "de", "fr", "it", "es", "ru")

        if (systemLang in supportedLanguages) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(systemLang)
            )
            Log.d("FocusRemindApp", "Auto-detected system language: $systemLang")
        }

        prefs.edit().putBoolean("language_auto_detected", true).apply()
    }

    /**
     * Creates a unified notification channel.
     * Sound is set to NULL on the channel — we play the user's chosen sound
     * directly in the notification builder (AlarmReceiver). This allows users
     * to change sounds in-app without needing to recreate the channel.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Clean up ALL old channels
            listOf(
                "reminders", "nomi_alarm_v2", "nomi_v3", "nomi_alarm_service",
                "nomi_reminders", "nomi_reminders_v2", "nomi_reminders_v3", "nomi_reminders_v4"
            ).forEach { oldId ->
                nm.deleteNotificationChannel(oldId)
            }

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Nomi - Przypomnienia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o przypomnieniach z Nomi"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                enableLights(true)
                lightColor = android.graphics.Color.MAGENTA
                // Sound = null on channel. User's chosen sound is set per-notification.
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
            Log.d("FocusRemindApp", "Channel '$NOTIFICATION_CHANNEL_ID' created (sound=per-notification)")
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "nomi_reminders_v5"

        lateinit var instance: FocusRemindApp
            private set
    }
}
