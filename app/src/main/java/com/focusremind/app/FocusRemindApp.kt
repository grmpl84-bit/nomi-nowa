package com.focusremind.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.focusremind.app.data.FocusRemindDatabase

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
