package com.focusremind.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.focusremind.app.data.Reminder
import com.focusremind.app.notification.AlarmService
import com.focusremind.app.notification.ReminderAlarmScheduler
import com.focusremind.app.speech.TimeParser
import com.focusremind.app.ui.AppNavigation
import com.focusremind.app.ui.theme.FocusRemindTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only stop the alarm sound service when user opens from notification tap
        if (savedInstanceState == null && intent?.hasExtra("reminder_id") == true) {
            AlarmService.stop(this)
        }

        // Handle Google Assistant "Create Note" intent
        val reminderText = intent?.getStringExtra("reminder_text")
        if (reminderText != null && savedInstanceState == null) {
            handleAssistantReminder(reminderText)
            return // Don't show UI — just confirm and close
        }

        val startVoice = intent?.getBooleanExtra("open_voice", false) ?: false

        setContent {
            FocusRemindTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(startWithVoice = startVoice)
                }
            }
        }
    }

    /**
     * Process reminder text from Google Assistant.
     * Parses time, saves reminder, speaks confirmation, closes app.
     */
    private fun handleAssistantReminder(text: String) {
        Log.d("MainActivity", "Google Assistant reminder: $text")

        val parsed = TimeParser.parse(text)
        val title = if (parsed != null && parsed.cleanedText.isNotBlank()) {
            parsed.cleanedText.replaceFirstChar { it.uppercase() }
        } else {
            text.replaceFirstChar { it.uppercase() }
        }
        val triggerAt = parsed?.triggerAt ?: (System.currentTimeMillis() + 15 * 60_000L)

        // Save reminder
        CoroutineScope(Dispatchers.IO).launch {
            val dao = FocusRemindApp.instance.database.reminderDao()
            val id = dao.insert(Reminder(title = title, triggerAt = triggerAt, isVoiceCreated = true, originalVoiceText = text))
            ReminderAlarmScheduler.schedule(this@MainActivity, Reminder(id = id, title = title, triggerAt = triggerAt))
            Log.d("MainActivity", "Assistant reminder saved: id=$id, title=$title")
        }

        // Voice confirmation via TTS, then close
        val timeFormatted = SimpleDateFormat("HH:mm", Locale("pl")).format(Date(triggerAt))
        val confirmText = "Zapisano. Przypomnę o $timeFormatted"

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pl")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        tts?.shutdown()
                        finishAndRemoveTask()
                    }
                    override fun onError(utteranceId: String?) {
                        tts?.shutdown()
                        finishAndRemoveTask()
                    }
                })
                tts?.speak(confirmText, TextToSpeech.QUEUE_FLUSH, null, "assistant_confirm")
            } else {
                // TTS failed — just close
                finishAndRemoveTask()
            }
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
