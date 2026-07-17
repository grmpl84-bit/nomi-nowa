package com.focusremind.app.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.R
import com.focusremind.app.data.Reminder
import com.focusremind.app.notification.ReminderAlarmScheduler
import com.focusremind.app.speech.RecurringVoiceParser
import com.focusremind.app.speech.ShoppingListParser
import com.focusremind.app.speech.TimeParser
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val scope = rememberCoroutineScope()
    val dao = FocusRemindApp.instance.database.reminderDao()
    val shoppingDao = FocusRemindApp.instance.database.shoppingDao()

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var editableTitle by remember { mutableStateOf("") }
    var showReview by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableIntStateOf(0) }
    var parsedResult by remember { mutableStateOf<TimeParser.Result?>(null) }
    var customDateTime by remember { mutableStateOf<Long?>(null) }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    fun startListening() {
        isListening = true
        recognizedText = ""
        parsedResult = null
        selectedMinutes = 0
        customDateTime = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                recognizedText = text
                isListening = false

                // Shopping list commands have no time/date at all — check
                // for them FIRST, before TimeParser even runs, and skip the
                // whole time-review screen entirely if matched.
                val shoppingItemName = ShoppingListParser.parse(text)
                if (shoppingItemName != null) {
                    scope.launch {
                        val existing = shoppingDao.findByName(shoppingItemName)
                        if (existing != null) {
                            android.widget.Toast.makeText(
                                context, context.getString(R.string.shopping_duplicate_toast, shoppingItemName), android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            shoppingDao.insert(com.focusremind.app.data.ShoppingItem(name = shoppingItemName))
                            android.widget.Toast.makeText(
                                context, context.getString(R.string.shopping_added_toast, shoppingItemName), android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        onBack()
                    }
                    return
                }

                // Recurring reminder commands — same dedicated flow as in
                // HomeScreen's main mic, saved directly with recurrence set.
                val recurringResult = RecurringVoiceParser.parse(text)
                if (recurringResult != null) {
                    scope.launch {
                        val id = dao.insert(
                            Reminder(
                                title = recurringResult.cleanedText,
                                triggerAt = recurringResult.triggerAt,
                                isVoiceCreated = true,
                                originalVoiceText = text,
                                recurrence = recurringResult.recurrence
                            )
                        )
                        ReminderAlarmScheduler.schedule(
                            context,
                            Reminder(id = id, title = recurringResult.cleanedText, triggerAt = recurringResult.triggerAt, recurrence = recurringResult.recurrence)
                        )
                        android.widget.Toast.makeText(
                            context, context.getString(R.string.recurring_added_toast, recurringResult.cleanedText), android.widget.Toast.LENGTH_SHORT
                        ).show()
                        onBack()
                    }
                    return
                }

                // Try to parse time from speech
                val parsed = TimeParser.parse(text)
                parsedResult = parsed
                editableTitle = if (parsed != null && parsed.cleanedText.isNotBlank()) {
                    parsed.cleanedText.replaceFirstChar { it.uppercase() }
                } else {
                    text.replaceFirstChar { it.uppercase() }
                }
                showReview = true
            }
            override fun onPartialResults(partial: Bundle?) {
                recognizedText = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            }
            override fun onError(error: Int) { isListening = false }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Close") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!micPermission.status.isGranted) {
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.mic_needed))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { micPermission.launchPermissionRequest() }) { Text(stringResource(R.string.allow_mic)) }
                Spacer(Modifier.weight(1f))
            } else if (!showReview) {
                // Listening mode
                Spacer(Modifier.weight(1f))
                val pulse = rememberInfiniteTransition(label = "p")
                    .animateFloat(1f, 1.2f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s")
                FloatingActionButton(
                    onClick = { if (isListening) recognizer.stopListening() else startListening() },
                    modifier = Modifier.size(96.dp).scale(if (isListening) pulse.value else 1f),
                    shape = CircleShape,
                    containerColor = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        "Record", Modifier.size(42.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    if (isListening) stringResource(R.string.listening) else stringResource(R.string.tap_to_speak),
                    style = MaterialTheme.typography.titleMedium
                )
                if (recognizedText.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Card { Text(recognizedText, Modifier.padding(16.dp)) }
                }
                Spacer(Modifier.weight(1f))
            } else {
                // Review mode
                OutlinedTextField(
                    value = editableTitle,
                    onValueChange = { editableTitle = it },
                    label = { Text(stringResource(R.string.reminder_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Show detected time if parsed
                if (parsedResult != null) {
                    val timeFormatted = remember(parsedResult) {
                        SimpleDateFormat("EEEE, d MMM, HH:mm", Locale("pl")).format(Date(parsedResult!!.triggerAt))
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Rozpoznano czas:", style = MaterialTheme.typography.labelSmall)
                                Text(timeFormatted, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.when_remind), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                // Quick time buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5 to "5 min", 10 to "10 min", 15 to "15 min").forEach { (min, label) ->
                        FilterChip(
                            selected = selectedMinutes == min,
                            onClick = { selectedMinutes = min; parsedResult = null; customDateTime = null },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 min", 60 to "1h", 120 to "2h").forEach { (min, label) ->
                        FilterChip(
                            selected = selectedMinutes == min,
                            onClick = { selectedMinutes = min; parsedResult = null; customDateTime = null },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Manual date/time picker — fallback for anything the quick
                // intervals above can't express (e.g. "tomorrow", a specific
                // future date), in case voice recognition didn't catch a date.
                if (customDateTime != null) {
                    val customFormatted = remember(customDateTime) {
                        SimpleDateFormat("EEEE, d MMM, HH:mm", Locale("pl")).format(Date(customDateTime!!))
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Wybrano ręcznie:", style = MaterialTheme.typography.labelSmall)
                                Text(customFormatted, style = MaterialTheme.typography.bodyLarge)
                            }
                            TextButton(onClick = { customDateTime = null }) { Text(stringResource(R.string.cancel)) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.DatePickerDialog(context, { _, year, month, day ->
                                android.app.TimePickerDialog(context, { _, hour, minute ->
                                    val chosen = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
                                    }
                                    customDateTime = chosen.timeInMillis
                                    selectedMinutes = 0
                                    parsedResult = null
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Wybierz datę i godzinę...")
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showReview = false; startListening() }, Modifier.weight(1f)) {
                        Text(stringResource(R.string.retry))
                    }
                    Button(
                        onClick = {
                            val triggerAt = when {
                                customDateTime != null -> customDateTime!!
                                selectedMinutes > 0 -> System.currentTimeMillis() + selectedMinutes * 60_000L
                                parsedResult != null -> parsedResult!!.triggerAt
                                else -> System.currentTimeMillis() + 15 * 60_000L
                            }
                            scope.launch {
                                val id = dao.insert(Reminder(
                                    title = editableTitle,
                                    triggerAt = triggerAt,
                                    isVoiceCreated = true,
                                    originalVoiceText = recognizedText
                                ))
                                // CRITICAL: Schedule the alarm! Without this, notification will never fire.
                                ReminderAlarmScheduler.schedule(context, Reminder(id = id, title = editableTitle, triggerAt = triggerAt))
                                onBack()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = editableTitle.isNotBlank() && (selectedMinutes > 0 || parsedResult != null || customDateTime != null)
                    ) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}
