package com.focusremind.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Small controller handed back by [rememberVoiceRecognizer] so a screen can
 * show a mic button and react to speech results, without re-implementing
 * permission handling + SpeechRecognizer lifecycle every time.
 */
class VoiceRecognizerController internal constructor(
    private val listeningState: () -> Boolean,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit
) {
    val isListening: Boolean get() = listeningState()
    fun start() = onStart()
    fun stop() = onStop()
}

/**
 * Reusable voice-recognition hook — same underlying SpeechRecognizer setup
 * as the main mic on HomeScreen, but generic: [onResult] receives the
 * recognized text and decides what to do with it (e.g. try
 * RecurringVoiceParser or ShoppingListParser), instead of this hook knowing
 * anything about reminders/shopping itself.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberVoiceRecognizer(onResult: (String) -> Unit): VoiceRecognizerController {
    val context = LocalContext.current
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var isListening by remember { mutableStateOf(false) }
    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    fun startListening() {
        if (!micPermission.status.isGranted) {
            micPermission.launchPermissionRequest()
            return
        }
        isListening = true
        val speechLocale = getSpeechRecognitionLocale(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                isListening = false
                onResult(text)
            }
            override fun onError(error: Int) { isListening = false }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try {
            recognizer.startListening(intent)
        } catch (_: Exception) {
            isListening = false
        }
    }

    fun stopAndProcess() {
        try { recognizer.stopListening() } catch (_: Exception) {}
    }

    return remember {
        VoiceRecognizerController(
            listeningState = { isListening },
            onStart = { startListening() },
            onStop = { stopAndProcess() }
        )
    }
}

/**
 * Mic button matching the home screen's look (gradient circle, turns red
 * while listening) — reused wherever a screen needs its own voice input,
 * respecting the same hold/tap mic-mode Settings preference.
 */
@Composable
fun VoiceMicFab(controller: VoiceRecognizerController, size: Dp = 96.dp) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val micMode = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
                            .getString("mic_mode", "hold")
                        if (micMode == "tap") {
                            if (controller.isListening) controller.stop() else controller.start()
                        } else {
                            controller.start()
                            val released = tryAwaitRelease()
                            if (released) controller.stop()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (controller.isListening) {
            Surface(
                modifier = Modifier.size(size),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, null, Modifier.size(size / 2), tint = MaterialTheme.colorScheme.onError)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF7C4DFF), Color(0xFF00BCD4)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, null, Modifier.size(size / 2), tint = Color.White)
            }
        }
    }
}
