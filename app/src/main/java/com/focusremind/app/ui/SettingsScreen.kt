package com.focusremind.app.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.focusremind.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenSoundPicker: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(getCurrentLanguage()) }
    var baseVolume by remember { mutableFloatStateOf(prefs.getFloat("notification_volume", 0.7f)) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var bypassDnd by remember { mutableStateOf(prefs.getBoolean("bypass_dnd", false)) }

    // Language dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            icon = { Icon(Icons.Default.Language, null) },
            title = { Text(stringResource(R.string.choose_language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LanguageOption("🇵🇱", stringResource(R.string.lang_polish), currentLanguage == "pl") {
                        setLanguage("pl"); currentLanguage = "pl"; showLanguageDialog = false
                    }
                    LanguageOption("🇬🇧", stringResource(R.string.lang_english), currentLanguage == "en") {
                        setLanguage("en"); currentLanguage = "en"; showLanguageDialog = false
                    }
                    LanguageOption("🇩🇪", stringResource(R.string.lang_german), currentLanguage == "de") {
                        setLanguage("de"); currentLanguage = "de"; showLanguageDialog = false
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LanguageOption("📱", stringResource(R.string.lang_system), currentLanguage == "") {
                        setLanguage(""); currentLanguage = ""; showLanguageDialog = false
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // === NOTIFICATIONS ===
            Text("🔔 ${stringResource(R.string.notifications_section)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            // Sound picker - shows selected sound name from system
            Card(onClick = { onOpenSoundPicker() }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.sound_label), style = MaterialTheme.typography.titleSmall)
                        val soundIndex = prefs.getInt("notification_sound_index", 0)
                        val soundName = remember(soundIndex) { getSelectedSoundName(context, soundIndex) }
                        Text(soundName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Volume slider with preview
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.volume_label), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔈")
                        Slider(
                            value = baseVolume,
                            onValueChange = { baseVolume = it },
                            onValueChangeFinished = { prefs.edit().putFloat("notification_volume", baseVolume).apply() },
                            modifier = Modifier.weight(1f)
                        )
                        Text("🔊")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${(baseVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { playPreview(context, baseVolume) }) {
                            Icon(Icons.Default.VolumeUp, "Odsłuchaj", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Vibration
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.vibration_label), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.vibration_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it; prefs.edit().putBoolean("vibration_enabled", it).apply() })
                }
            }

            // Bypass DND
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DoNotDisturb, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.bypass_dnd_label), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.bypass_dnd_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = bypassDnd, onCheckedChange = { bypassDnd = it; prefs.edit().putBoolean("bypass_dnd", it).apply() })
                }
            }

            HorizontalDivider()

            // === LANGUAGE ===
            Text("🌍 ${stringResource(R.string.language)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Card(onClick = { showLanguageDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
                        Text(
                            when (currentLanguage) { "pl" -> "🇵🇱 Polski"; "en" -> "🇬🇧 English"; "de" -> "🇩🇪 Deutsch"; else -> "📱 System" },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // === ABOUT ===
            Text("ℹ️ ${stringResource(R.string.about)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Nomi v1.0.0", style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.made_for), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LanguageOption(flag: String, name: String, isSelected: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.filledTonalButtonColors()
    ) {
        Text("$flag  $name", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) { Spacer(Modifier.weight(1f)); Text("✓") }
    }
}

private fun getCurrentLanguage(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (locales.isEmpty) "" else locales[0]?.language ?: ""
}

private fun setLanguage(languageCode: String) {
    AppCompatDelegate.setApplicationLocales(
        if (languageCode.isEmpty()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(languageCode)
    )
}

private fun getSelectedSoundName(context: Context, index: Int): String {
    if (index < 0) return "🔇 Cisza (tylko wibracja)"
    return try {
        val rm = RingtoneManager(context)
        rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
        val cursor = rm.cursor
        if (cursor.count == 0) return "Domyślny"
        cursor.moveToPosition(index % cursor.count)
        cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
    } catch (_: Exception) { "Domyślny" }
}

private fun playPreview(context: Context, volume: Float) {
    try {
        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val soundIndex = prefs.getInt("notification_sound_index", 0)
        if (soundIndex < 0) return

        val rm = RingtoneManager(context)
        rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
        val cursor = rm.cursor
        if (cursor.count == 0) return
        val uri = rm.getRingtoneUri(soundIndex % cursor.count)

        val player = MediaPlayer()
        player.setDataSource(context, uri)
        player.setVolume(volume, volume)
        player.isLooping = false
        player.setOnCompletionListener { it.release() }
        player.prepare()
        player.start()
    } catch (_: Exception) {}
}
