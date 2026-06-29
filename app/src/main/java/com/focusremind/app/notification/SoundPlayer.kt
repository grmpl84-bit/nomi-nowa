package com.focusremind.app.notification

import android.media.MediaPlayer
import android.os.Vibrator
import android.util.Log

/**
 * Holds reference to currently playing alarm sound and vibrator.
 * This allows NotificationActionReceiver to stop them when user
 * taps "Gotowe" or "+5 min".
 */
object SoundPlayer {
    var currentPlayer: MediaPlayer? = null
    var currentVibrator: Vibrator? = null

    fun stop() {
        try {
            currentPlayer?.stop()
            currentPlayer?.release()
        } catch (_: Exception) {}
        currentPlayer = null

        try {
            currentVibrator?.cancel()
        } catch (_: Exception) {}
        currentVibrator = null

        Log.d("SoundPlayer", "Sound and vibration stopped")
    }
}
