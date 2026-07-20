package com.focusremind.app.notification

import android.media.MediaPlayer
import android.os.Vibrator
import android.util.Log

/**
 * Holds references to currently playing alarm sounds/vibrations — keyed by
 * reminder ID, NOT a single shared slot. A single global slot meant that if
 * a second alarm fired (a different reminder, or the same one's next
 * recurring cycle) while a first one was still ringing, the second one's
 * sound/vibration would silently overwrite the first one's reference —
 * orphaning it with literally no way to stop it from the app anymore (the
 * "Done" button on the FIRST notification would stop the SECOND alarm's
 * sound instead, since that's all the singleton remembered).
 */
object SoundPlayer {
    private val players = mutableMapOf<Long, MediaPlayer>()
    private val vibrators = mutableMapOf<Long, Vibrator>()

    fun register(reminderId: Long, player: MediaPlayer? = null, vibrator: Vibrator? = null) {
        player?.let { players[reminderId] = it }
        vibrator?.let { vibrators[reminderId] = it }
    }

    /** True if this reminder's sound/vibration hasn't been stopped yet — used
     * to make sure a delayed retry never revives something the user already
     * silenced in the meantime. */
    fun isActive(reminderId: Long): Boolean =
        players.containsKey(reminderId) || vibrators.containsKey(reminderId)

    fun stop(reminderId: Long) {
        try {
            players[reminderId]?.let {
                it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        players.remove(reminderId)

        try {
            vibrators[reminderId]?.cancel()
        } catch (_: Exception) {}
        vibrators.remove(reminderId)

        Log.d("SoundPlayer", "Sound and vibration stopped for reminder $reminderId")
    }

    /** Stops every currently-tracked alarm — used only as a last-resort cleanup. */
    fun stopAll() {
        val ids = (players.keys + vibrators.keys).toSet()
        ids.forEach { stop(it) }
    }
}
