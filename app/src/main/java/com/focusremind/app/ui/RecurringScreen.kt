package com.focusremind.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.R
import com.focusremind.app.data.Reminder
import com.focusremind.app.notification.ReminderAlarmScheduler
import com.focusremind.app.notification.ReminderNotificationBuilder
import com.focusremind.app.speech.RecurringVoiceParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Dedicated screen for recurring reminders — a completely separate category
 * from the main one-time reminder list. This is the ONLY place a recurring
 * reminder's series can be deleted or have its frequency changed; the main
 * list only shows them (read-only badge) for awareness.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(onOpenHome: () -> Unit, onOpenShopping: () -> Unit) {
    val context = LocalContext.current
    val dao = (context.applicationContext as FocusRemindApp).database.reminderDao()
    val shoppingDao = FocusRemindApp.instance.database.shoppingDao()
    val scope = rememberCoroutineScope()
    val reminders by dao.getRecurring().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDateTime by remember { mutableStateOf<Long?>(null) }
    var newFrequency by remember { mutableStateOf("DAILY") }

    val recognizer = rememberVoiceRecognizer { text ->
        handleUniversalVoiceInput(text, context, dao, shoppingDao, scope)
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
                    .height(110.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.recurring_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        },
        bottomBar = {
            AppBottomBar(
                current = "recurring",
                onOpenHome = onOpenHome,
                onOpenRecurring = { /* already here */ },
                onOpenShopping = onOpenShopping
            )
        },
        floatingActionButton = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(onClick = {
                    newTitle = ""
                    newDateTime = null
                    newFrequency = "DAILY"
                    showAddDialog = true
                }) {
                    Icon(Icons.Default.Add, null)
                }
                VoiceMicFab(recognizer)
            }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.no_recurring_reminders),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    RecurringCard(
                        reminder = reminder,
                        onDelete = {
                            scope.launch {
                                dao.delete(reminder.id)
                                ReminderAlarmScheduler.cancel(context, reminder.id)
                            }
                        },
                        onChangeFrequency = { freq ->
                            scope.launch {
                                dao.updateRecurrence(reminder.id, freq)
                                ReminderAlarmScheduler.schedule(context, reminder.copy(recurrence = freq))
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.new_recurring_reminder)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(stringResource(R.string.reminder_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(stringResource(R.string.recurring_frequency_label), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newFrequency == "DAILY",
                            onClick = { newFrequency = "DAILY"; newDateTime = null },
                            label = { Text(stringResource(R.string.recurrence_daily)) }
                        )
                        FilterChip(
                            selected = newFrequency == "WEEKLY",
                            onClick = { newFrequency = "WEEKLY"; newDateTime = null },
                            label = { Text(stringResource(R.string.recurrence_weekly)) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newFrequency == "BIWEEKLY",
                            onClick = { newFrequency = "BIWEEKLY"; newDateTime = null },
                            label = { Text(stringResource(R.string.recurrence_biweekly)) }
                        )
                        FilterChip(
                            selected = newFrequency == "MONTHLY",
                            onClick = { newFrequency = "MONTHLY"; newDateTime = null },
                            label = { Text(stringResource(R.string.recurrence_monthly)) }
                        )
                    }

                    val formatted = remember(newDateTime, newFrequency) {
                        newDateTime?.let {
                            if (newFrequency == "DAILY")
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                            else
                                SimpleDateFormat("EEEE, d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            if (newFrequency == "DAILY") {
                                // Daily repeats forever starting today — only the
                                // TIME matters, picking a date makes no sense here.
                                TimePickerDialog(context, { _, hour, minute ->
                                    val chosen = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                    newDateTime = chosen.timeInMillis
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            } else {
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
                                        newDateTime = chosen.timeInMillis
                                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatted ?: if (newFrequency == "DAILY") "Wybierz godzinę..." else stringResource(R.string.pick_date_time)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trigger = newDateTime ?: return@Button
                        scope.launch {
                            val id = dao.insert(
                                Reminder(title = newTitle, triggerAt = trigger, recurrence = newFrequency, anchorTime = trigger)
                            )
                            ReminderAlarmScheduler.schedule(
                                context,
                                Reminder(id = id, title = newTitle, triggerAt = trigger, recurrence = newFrequency, anchorTime = trigger)
                            )
                        }
                        showAddDialog = false
                    },
                    enabled = newTitle.isNotBlank() && newDateTime != null
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun RecurringCard(reminder: Reminder, onDelete: () -> Unit, onChangeFrequency: (String) -> Unit) {
    var showFreqMenu by remember { mutableStateOf(false) }
    val timeFormatted = remember(reminder.triggerAt) {
        SimpleDateFormat("EEEE, d MMM, HH:mm", Locale.getDefault()).format(Date(reminder.triggerAt))
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(ReminderNotificationBuilder.buildNotificationText(reminder.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${stringResource(R.string.next_occurrence_label)} $timeFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { showFreqMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Repeat, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (reminder.recurrence) {
                                "DAILY" -> stringResource(R.string.recurrence_daily)
                                "WEEKLY" -> stringResource(R.string.recurrence_weekly)
                                "BIWEEKLY" -> stringResource(R.string.recurrence_biweekly)
                                "MONTHLY" -> stringResource(R.string.recurrence_monthly)
                                else -> stringResource(R.string.recurrence_weekly)
                            }
                        )
                    }
                    DropdownMenu(expanded = showFreqMenu, onDismissRequest = { showFreqMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_daily)) },
                            onClick = { onChangeFrequency("DAILY"); showFreqMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_weekly)) },
                            onClick = { onChangeFrequency("WEEKLY"); showFreqMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_biweekly)) },
                            onClick = { onChangeFrequency("BIWEEKLY"); showFreqMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_monthly)) },
                            onClick = { onChangeFrequency("MONTHLY"); showFreqMenu = false }
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
