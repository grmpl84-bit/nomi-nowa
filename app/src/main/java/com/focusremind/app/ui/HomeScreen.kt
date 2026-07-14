package com.focusremind.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.R
import com.focusremind.app.data.Reminder
import com.focusremind.app.notification.ReminderAlarmScheduler
import com.focusremind.app.notification.ReminderNotificationBuilder
import com.focusremind.app.notification.SoundPlayer
import com.focusremind.app.speech.TimeParser
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(onAddReminder: () -> Unit, onOpenSettings: () -> Unit, onOpenHistory: () -> Unit, onOpenRecurring: () -> Unit, startRecordingImmediately: Boolean = false) {
    val context = LocalContext.current
    val dao = FocusRemindApp.instance.database.reminderDao()
    val reminders by dao.getActive().collectAsState(initial = emptyList())

    // Ticks every 30s so "za X minut" / overdue styling on each card stays
    // fresh without needing an unrelated recomposition (e.g. navigating
    // away and back) to notice time has passed.
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            nowTick = System.currentTimeMillis()
        }
    }
    val scope = rememberCoroutineScope()
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Request notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notifPermission.status.isGranted) {
                notifPermission.launchPermissionRequest()
            }
        }
    }

    // Check exact alarm permission on Android 12+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Fallback without package URI
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                }
            }
        }
    }

    // CRITICAL: Request battery optimization exemption
    // Without this, the phone KILLS the app and AlarmReceiver never fires!
    LaunchedEffect(Unit) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    // XIAOMI-specific: Open Autostart settings
    // On Xiaomi/MIUI, the app MUST be in Autostart list or AlarmManager won't wake it
    var showXiaomiDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer == "xiaomi" || manufacturer == "redmi" || manufacturer == "poco") {
            val prefs2 = context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE)
            if (!prefs2.getBoolean("xiaomi_autostart_shown", false)) {
                showXiaomiDialog = true
            }
        }
    }

    if (showXiaomiDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("⚠️ Ważne dla Xiaomi!") },
            text = {
                Column {
                    Text("Twój telefon Xiaomi blokuje powiadomienia aplikacji działających w tle.")
                    Spacer(Modifier.height(12.dp))
                    Text("Musisz włączyć 3 rzeczy:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("1️⃣ Autostart → włącz dla Nomi")
                    Text("2️⃣ Bateria → Bez ograniczeń")
                    Text("3️⃣ Zablokuj aplikację w ostatnich (przytrzymaj w menu ostatnich → kłódka)")
                    Spacer(Modifier.height(12.dp))
                    Text("Bez tego przypomnienia NIE będą działać gdy zamkniesz aplikację!", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Open Xiaomi Autostart settings
                    try {
                        val intent = Intent()
                        intent.component = android.content.ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Fallback: open app settings
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("xiaomi_autostart_shown", true).apply()
                    showXiaomiDialog = false
                }) {
                    Text("Otwórz Autostart")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("xiaomi_autostart_shown", true).apply()
                    showXiaomiDialog = false
                }) {
                    Text("Później")
                }
            }
        )
    }

    // Voice recording state
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<TimeParser.Result?>(null) }
    var selectedMinutes by remember { mutableIntStateOf(0) }

    // Photo picker state - use rememberSaveable to survive process death
    var photoReminderId by rememberSaveable { mutableStateOf(-1L) }
    var photoReminder by remember { mutableStateOf<Reminder?>(null) }
    var fullScreenPhotoReminder by remember { mutableStateOf<Reminder?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Camera photo URI - persisted across process death
    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val tempCameraUri: Uri? = tempCameraUriString?.let { Uri.parse(it) }

    // Camera photo launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUriString != null && photoReminderId > 0) {
            // Capture values now — tempCameraUriString/photoReminderId get reset
            // below, and scope.launch only SCHEDULES the coroutine, it doesn't
            // run synchronously. Reading the mutable state again inside the
            // coroutine body would read the already-reset (null) value.
            val savedUri = tempCameraUriString!!
            val savedReminderId = photoReminderId
            scope.launch {
                dao.updatePhoto(savedReminderId, savedUri)
            }
        }
        photoReminderId = -1L
        tempCameraUriString = null
    }

    // Camera permission launcher — requests CAMERA permission then launches camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && tempCameraUriString != null) {
            cameraLauncher.launch(Uri.parse(tempCameraUriString!!))
        } else {
            // Permission denied — reset state
            tempCameraUriString = null
        }
    }

    // Gallery photo picker launcher (using OpenDocument for persistent access)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && photoReminderId > 0) {
            // Same capture-before-reset fix as cameraLauncher above
            val savedReminderId = photoReminderId
            // Take persistable permission so we can read it later
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            scope.launch {
                dao.updatePhoto(savedReminderId, uri.toString())
            }
        }
        photoReminderId = -1L
    }

    // Snooze dialog state
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var snoozeReminder by remember { mutableStateOf<Reminder?>(null) }

    // Edit dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var editReminder by remember { mutableStateOf<Reminder?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editMinutes by remember { mutableIntStateOf(0) }
    var editCustomDateTime by remember { mutableStateOf<Long?>(null) }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    // Flag to trigger auto-start
    var shouldAutoStart by remember { mutableStateOf(startRecordingImmediately) }

    fun startListening() {
        if (!micPermission.status.isGranted) {
            micPermission.launchPermissionRequest()
            return
        }
        isListening = true
        recognizedText = ""
        parsedResult = null
        selectedMinutes = 0
        val speechLocale = getSpeechRecognitionLocale(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Auto-stop after 1.5s of silence
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                recognizedText = text
                val parsed = TimeParser.parse(text)
                parsedResult = parsed
                editableTitle = if (parsed != null && parsed.cleanedText.isNotBlank()) {
                    parsed.cleanedText.replaceFirstChar { it.uppercase() }
                } else {
                    text.replaceFirstChar { it.uppercase() }
                }
                isListening = false

                // AUTO-SAVE: If time was parsed from voice, save immediately without review
                if (parsed != null && editableTitle.isNotBlank()) {
                    scope.launch {
                        val id = dao.insert(Reminder(title = editableTitle, triggerAt = parsed.triggerAt, isVoiceCreated = true, originalVoiceText = text))
                        ReminderAlarmScheduler.schedule(context, Reminder(id = id, title = editableTitle, triggerAt = parsed.triggerAt))
                    }
                } else {
                    // No time detected — show review screen so user can pick time
                    showResult = true
                }
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

    fun stopAndProcess() {
        recognizer.stopListening()
    }

    // Auto-start from widget
    LaunchedEffect(shouldAutoStart) {
        if (shouldAutoStart && micPermission.status.isGranted) {
            shouldAutoStart = false
            startListening()
        }
    }

    // Snooze dialog
    if (showSnoozeDialog && snoozeReminder != null) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text(stringResource(R.string.snooze_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\"${snoozeReminder!!.title}\"", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        5 to "+5 min", 10 to "+10 min", 15 to "+15 min",
                        30 to "+30 min", 60 to "+1h", 120 to "+2h",
                        1440 to "Jutro"
                    ).forEach { (min, label) ->
                        FilledTonalButton(
                            onClick = {
                                val newTime = System.currentTimeMillis() + min * 60_000L
                                // Capture now — if the user reopens this dialog for a
                                // DIFFERENT reminder before this coroutine runs,
                                // snoozeReminder would already point elsewhere.
                                val target = snoozeReminder!!
                                scope.launch {
                                    dao.snooze(target.id, newTime)
                                    ReminderAlarmScheduler.cancel(context, target.id)
                                    ReminderAlarmScheduler.schedule(context, target.copy(triggerAt = newTime))
                                }
                                showSnoozeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnoozeDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Photo options dialog
    if (showPhotoOptions && photoReminder != null) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false; photoReminder = null },
            icon = { Icon(Icons.Default.CameraAlt, null) },
            title = { Text(stringResource(R.string.add_photo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Camera button
                    FilledTonalButton(
                        onClick = {
                            showPhotoOptions = false
                            photoReminderId = photoReminder!!.id
                            // Create the pictures directory if it doesn't exist
                            val picturesDir = File(context.filesDir, "pictures")
                            if (!picturesDir.exists()) picturesDir.mkdirs()
                            val photoFile = File(picturesDir, "nomi_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            tempCameraUriString = uri.toString()
                            // Check if CAMERA permission is already granted
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraLauncher.launch(uri)
                            } else {
                                // Request permission — callback will launch camera if granted
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.take_photo))
                    }
                    // Gallery button
                    FilledTonalButton(
                        onClick = {
                            showPhotoOptions = false
                            photoReminderId = photoReminder!!.id
                            galleryLauncher.launch(arrayOf("image/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.from_gallery))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptions = false; photoReminder = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Full-screen photo preview (tap thumbnail on a reminder card)
    if (fullScreenPhotoReminder != null) {
        val target = fullScreenPhotoReminder!!
        Dialog(
            onDismissRequest = { fullScreenPhotoReminder = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)
                offsetX += panChange.x
                offsetY += panChange.y
            }
            var showDeleteConfirm by remember { mutableStateOf(false) }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = target.photoUri,
                    contentDescription = stringResource(R.string.photo_attached),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale, scaleY = scale,
                            translationX = offsetX, translationY = offsetY
                        )
                        .transformable(transformState),
                    contentScale = ContentScale.Fit
                )

                // Close button
                IconButton(
                    onClick = { fullScreenPhotoReminder = null },
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }

                // Delete photo button (only removes the photo, not the reminder)
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text(stringResource(R.string.delete_photo_title)) },
                    text = { Text(stringResource(R.string.delete_photo_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = target.id
                                scope.launch { dao.updatePhoto(id, null) }
                                showDeleteConfirm = false
                                fullScreenPhotoReminder = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text(stringResource(R.string.delete_photo_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }
    }

    // Edit dialog
    if (showEditDialog && editReminder != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_reminder)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(stringResource(R.string.reminder_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Show current trigger time
                    val currentTriggerFormatted = remember(editReminder, editMinutes, editCustomDateTime) {
                        val triggerTime = when {
                            editCustomDateTime != null -> editCustomDateTime!!
                            editMinutes > 0 -> System.currentTimeMillis() + editMinutes * 60_000L
                            else -> editReminder!!.triggerAt
                        }
                        SimpleDateFormat("EEEE, d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(triggerTime))
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("\u23F0", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.notification_fires_at), style = MaterialTheme.typography.labelSmall)
                                Text(currentTriggerFormatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Text(stringResource(R.string.when_remind), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5 to "5 min", 15 to "15 min", 30 to "30 min").forEach { (min, label) ->
                            FilterChip(
                                selected = editMinutes == min && editCustomDateTime == null,
                                onClick = { editMinutes = min; editCustomDateTime = null },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60 to "1h", 120 to "2h", 1440 to stringResource(R.string.tomorrow)).forEach { (min, label) ->
                            FilterChip(
                                selected = editMinutes == min && editCustomDateTime == null,
                                onClick = { editMinutes = min; editCustomDateTime = null },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Custom date & time picker button
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(context, { _, year, month, day ->
                                TimePickerDialog(context, { _, hour, minute ->
                                    val chosen = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                    }
                                    editCustomDateTime = chosen.timeInMillis
                                    editMinutes = 0
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pick_date_time))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTrigger = when {
                            editCustomDateTime != null -> editCustomDateTime!!
                            editMinutes > 0 -> System.currentTimeMillis() + editMinutes * 60_000L
                            else -> editReminder!!.triggerAt
                        }
                        // Capture now — same reasoning as the snooze dialog above.
                        val target = editReminder!!
                        val newTitle = editTitle
                        scope.launch {
                            dao.update(target.id, newTitle, newTrigger)
                            ReminderAlarmScheduler.cancel(context, target.id)
                            ReminderAlarmScheduler.schedule(context, target.copy(title = newTitle, triggerAt = newTrigger))
                        }
                        showEditDialog = false
                    },
                    enabled = editTitle.isNotBlank()
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Bottom sheet for voice result
    if (showResult) {
        ModalBottomSheet(onDismissRequest = { showResult = false }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 8.dp)) {
                // BUTTONS FIRST - always visible at top
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showResult = false; startListening() }, Modifier.weight(1f)) {
                        Text(stringResource(R.string.retry))
                    }
                    Button(
                        onClick = {
                            val triggerAt = when {
                                selectedMinutes > 0 -> System.currentTimeMillis() + selectedMinutes * 60_000L
                                parsedResult != null -> parsedResult!!.triggerAt
                                else -> System.currentTimeMillis() + 15 * 60_000L
                            }
                            scope.launch {
                                val id = dao.insert(Reminder(title = editableTitle, triggerAt = triggerAt, isVoiceCreated = true, originalVoiceText = recognizedText))
                                ReminderAlarmScheduler.schedule(context, Reminder(id = id, title = editableTitle, triggerAt = triggerAt))
                                showResult = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = editableTitle.isNotBlank() && (selectedMinutes > 0 || parsedResult != null)
                    ) { Text(stringResource(R.string.save)) }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = editableTitle,
                    onValueChange = { editableTitle = it },
                    label = { Text(stringResource(R.string.reminder_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (parsedResult != null) {
                    val timeFormatted = remember(parsedResult) {
                        SimpleDateFormat("EEEE, d MMM, HH:mm", Locale("pl")).format(Date(parsedResult!!.triggerAt))
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.detected_time), style = MaterialTheme.typography.labelSmall)
                                Text(timeFormatted, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text(stringResource(R.string.when_remind), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5 to "5 min", 10 to "10 min", 15 to "15 min").forEach { (min, label) ->
                        FilterChip(selected = selectedMinutes == min, onClick = { selectedMinutes = min; parsedResult = null }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 min", 60 to "1h", 120 to "2h").forEach { (min, label) ->
                        FilterChip(selected = selectedMinutes == min, onClick = { selectedMinutes = min; parsedResult = null }, label = { Text(label) })
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF9C27B0), Color(0xFF7C4DFF), Color(0xFF00BCD4))
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(R.drawable.ic_nomi_symbol),
                            contentDescription = "Nomi",
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer(scaleX = 2.2f, scaleY = 2.2f)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Just Say It. We'll Remember.",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, "Historia", tint = Color.White)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings), tint = Color.White)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* already here */ },
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text(stringResource(R.string.reminders_nav_label)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenRecurring,
                    icon = { Icon(Icons.Default.Repeat, null) },
                    label = { Text(stringResource(R.string.recurring_nav_label)) }
                )
            }
        },
        floatingActionButton = {
            val pulse = rememberInfiniteTransition(label = "p")
                .animateFloat(1f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")

            // PUSH-TO-TALK: press and hold to record, release to stop
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(if (isListening) pulse.value else 1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                startListening()
                                val released = tryAwaitRelease()
                                if (released) stopAndProcess()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isListening) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Mic,
                                stringResource(R.string.tap_mic),
                                Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF7C4DFF), Color(0xFF00BCD4)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            stringResource(R.string.tap_mic),
                            Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (reminders.isEmpty() && !isListening) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧠", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_reminders), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.hold_mic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (isListening) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎙️", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.listening), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.release_to_stop), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (recognizedText.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Card { Text(recognizedText, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                // Faded logo watermark in background
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .alpha(0.06f)
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF9C27B0),
                                        Color(0xFF7C4DFF),
                                        Color(0xFF00BCD4)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("n", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Task list on top
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        nowTick = nowTick,
                        onComplete = {
                            scope.launch {
                                dao.complete(reminder.id)
                                ReminderAlarmScheduler.cancel(context, reminder.id)
                                // Safety net: if the alarm sound was still playing
                                // (e.g. user found the task on the list instead of
                                // using the notification), stop it here too —
                                // previously only the notification's own buttons
                                // could do this.
                                SoundPlayer.stop()
                                (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                                    .cancel(reminder.id.toInt())
                            }
                        },
                        onEdit = {
                            editReminder = reminder
                            editTitle = reminder.title
                            editMinutes = 0
                            editCustomDateTime = null
                            showEditDialog = true
                        },
                        onSnooze = {
                            snoozeReminder = reminder
                            showSnoozeDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                dao.delete(reminder.id)
                                ReminderAlarmScheduler.cancel(context, reminder.id)
                                SoundPlayer.stop()
                                (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                                    .cancel(reminder.id.toInt())
                            }
                        },
                        onAddPhoto = {
                            photoReminder = reminder
                            showPhotoOptions = true
                        },
                        onLockedDeleteTap = {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.recurring_manage_hint),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onOpenPhoto = { fullScreenPhotoReminder = reminder }
                    )
                }
            }
            } // end Box with watermark
        }
    }
}

@Composable
fun ReminderCard(reminder: Reminder, nowTick: Long, onComplete: () -> Unit, onEdit: () -> Unit, onSnooze: () -> Unit, onDelete: () -> Unit, onAddPhoto: () -> Unit, onLockedDeleteTap: () -> Unit, onOpenPhoto: () -> Unit) {
    val overdueText = stringResource(R.string.overdue)
    val timeText = remember(reminder.triggerAt, nowTick) {
        val diff = reminder.triggerAt - System.currentTimeMillis()
        when {
            diff < 0 -> "\u26A0\uFE0F $overdueText"
            diff < 60_000 -> "< 1 min"
            diff < 3_600_000 -> "${diff / 60_000} min"
            diff < 86_400_000 -> "${diff / 3_600_000}h ${(diff % 3_600_000) / 60_000}m"
            else -> SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()).format(Date(reminder.triggerAt))
        }
    }

    val isOverdue = reminder.triggerAt < System.currentTimeMillis()

    Card(
        Modifier.fillMaxWidth(),
        colors = if (isOverdue) CardDefaults.cardColors(containerColor = Color(0xFFFDECEA), contentColor = Color(0xFF1A1A2E))
        else CardDefaults.cardColors(containerColor = Color.White, contentColor = Color(0xFF1A1A2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color accent dot + short dashed line — brand gradient, red for overdue
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOverdue) SolidColor(Color(0xFFB4432F))
                                else Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF00BCD4)))
                            )
                    )
                    Spacer(Modifier.height(3.dp))
                    Canvas(modifier = Modifier.width(2.dp).height(18.dp)) {
                        drawLine(
                            color = Color(0xFFE3DEF5),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(ReminderNotificationBuilder.buildNotificationText(reminder.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) Color(0xFFB4432F) else Color(0xFF8A8FA3)
                        )
                        if (reminder.recurrence != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (reminder.recurrence == "DAILY") "🔁 ${stringResource(R.string.recurrence_daily)}"
                                else "🔁 ${stringResource(R.string.recurrence_weekly)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7C4DFF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Show photo if attached
            if (!reminder.photoUri.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                var photoLoadFailed by remember(reminder.photoUri) { mutableStateOf(false) }
                Card(
                    Modifier.fillMaxWidth().height(140.dp).clickable(onClick = onOpenPhoto),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (photoLoadFailed) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "⚠️ Nie udało się wczytać zdjęcia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        AsyncImage(
                            model = reminder.photoUri,
                            contentDescription = stringResource(R.string.photo_attached),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { photoLoadFailed = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            // Responsive action buttons - use small icon buttons to fit all screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green "Wykonane" button with checkmark - compact
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.completed_btn), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
                // Edit button (icon only)
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Photo button (icon only)
                IconButton(onClick = onAddPhoto, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Delete button — disabled for recurring reminders; those can only be
                // stopped/deleted from the dedicated "Cykliczne" screen (bottom entry).
                if (reminder.recurrence == null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    IconButton(onClick = onLockedDeleteTap, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}


/**
 * Returns the appropriate speech recognition locale based on app language setting.
 * Maps app language codes to full BCP-47 locale tags for SpeechRecognizer.
 */
private fun getSpeechRecognitionLocale(context: Context): String {
    val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
    // Check app-level locale override first
    val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
    val langCode = if (!appLocales.isEmpty) {
        appLocales[0]?.language ?: ""
    } else {
        // Use system locale
        Locale.getDefault().language
    }
    return when (langCode) {
        "pl" -> "pl-PL"
        "en" -> "en-US"
        "de" -> "de-DE"
        "fr" -> "fr-FR"
        "it" -> "it-IT"
        "es" -> "es-ES"
        "ru" -> "ru-RU"
        else -> "pl-PL" // default fallback
    }
}
