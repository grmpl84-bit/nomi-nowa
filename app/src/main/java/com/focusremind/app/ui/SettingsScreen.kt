package com.focusremind.app.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.focusremind.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenSoundPicker: () -> Unit, onShowOnboarding: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(getCurrentLanguage()) }
    var baseVolume by remember { mutableFloatStateOf(prefs.getFloat("notification_volume", 0.7f)) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var bypassDnd by remember { mutableStateOf(prefs.getBoolean("bypass_dnd", false)) }

    // Language dialog - names always in native form with flags
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            icon = { Icon(Icons.Default.Language, null) },
            title = { Text(stringResource(R.string.choose_language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LanguageOption("\uD83C\uDDF5\uD83C\uDDF1", "Polski", currentLanguage == "pl") {
                        setLanguage("pl"); currentLanguage = "pl"; showLanguageDialog = false
                    }
                    LanguageOption("\uD83C\uDDEC\uD83C\uDDE7", "English", currentLanguage == "en") {
                        setLanguage("en"); currentLanguage = "en"; showLanguageDialog = false
                    }
                    LanguageOption("\uD83C\uDDE9\uD83C\uDDEA", "Deutsch", currentLanguage == "de") {
                        setLanguage("de"); currentLanguage = "de"; showLanguageDialog = false
                    }
                    LanguageOption("\uD83C\uDDEB\uD83C\uDDF7", "Fran\u00E7ais", currentLanguage == "fr") {
                        setLanguage("fr"); currentLanguage = "fr"; showLanguageDialog = false
                    }
                    LanguageOption("\uD83C\uDDEE\uD83C\uDDF9", "Italiano", currentLanguage == "it") {
                        setLanguage("it"); currentLanguage = "it"; showLanguageDialog = false
                    }
                    LanguageOption("\uD83C\uDDEA\uD83C\uDDF8", "Espa\u00F1ol", currentLanguage == "es") {
                        setLanguage("es"); currentLanguage = "es"; showLanguageDialog = false
                    }
                    LanguageOption("\uD83C\uDDF7\uD83C\uDDFA", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439", currentLanguage == "ru") {
                        setLanguage("ru"); currentLanguage = "ru"; showLanguageDialog = false
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LanguageOption("\uD83D\uDCF1", stringResource(R.string.lang_system), currentLanguage == "") {
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
            // === ABOUT (moved to top per user request) ===
            Text("\u2139\uFE0F ${stringResource(R.string.about)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Nomi v${com.focusremind.app.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.made_for), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.donation_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "https://buymeacoffee.com/ifyp",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/ifyp"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            HorizontalDivider()

            // === NOTIFICATIONS ===
            Text("\uD83D\uDD14 ${stringResource(R.string.notifications_section)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

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

            // Volume slider with preview - NOW PLAYS THE SELECTED SOUND
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.volume_label), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDD08")
                        Slider(
                            value = baseVolume,
                            onValueChange = { baseVolume = it },
                            onValueChangeFinished = {
                                prefs.edit().putFloat("notification_volume", baseVolume).apply()
                                // Play preview of selected sound at new volume
                                playSelectedSoundPreview(context, baseVolume)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Text("\uD83D\uDD0A")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${(baseVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { playSelectedSoundPreview(context, baseVolume) }) {
                            Icon(Icons.Default.VolumeUp, stringResource(R.string.preview_sound), tint = MaterialTheme.colorScheme.primary)
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
            Text("\uD83C\uDF0D ${stringResource(R.string.language)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Card(onClick = { showLanguageDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
                        Text(
                            getLanguageDisplayName(currentLanguage),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // === SETUP GUIDE (re-show onboarding) ===
            Card(onClick = {
                context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("onboarding_done", false).apply()
                onShowOnboarding()
            }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.show_setup_guide), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.show_setup_guide_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (isSelected) { Spacer(Modifier.weight(1f)); Text("\u2713") }
    }
}

/** Always show language name in native form with flag, regardless of app language */
private fun getLanguageDisplayName(code: String): String {
    return when (code) {
        "pl" -> "\uD83C\uDDF5\uD83C\uDDF1 Polski"
        "en" -> "\uD83C\uDDEC\uD83C\uDDE7 English"
        "de" -> "\uD83C\uDDE9\uD83C\uDDEA Deutsch"
        "fr" -> "\uD83C\uDDEB\uD83C\uDDF7 Fran\u00E7ais"
        "it" -> "\uD83C\uDDEE\uD83C\uDDF9 Italiano"
        "es" -> "\uD83C\uDDEA\uD83C\uDDF8 Espa\u00F1ol"
        "ru" -> "\uD83C\uDDF7\uD83C\uDDFA \u0420\u0443\u0441\u0441\u043A\u0438\u0439"
        else -> "\uD83D\uDCF1 System"
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
    if (index == CUSTOM_SOUND_INDEX) {
        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val uriString = prefs.getString("notification_sound_custom_uri", null)
        return "🎵 ${getCustomSoundDisplayName(context, uriString)}"
    }
    if (index < 0) return "\uD83D\uDD07 Cisza (tylko wibracja)"
    return try {
        val rm = RingtoneManager(context)
        rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
        val cursor = rm.cursor
        if (cursor.count == 0) return "Domy\u015Blny"
        cursor.moveToPosition(index % cursor.count)
        cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
    } catch (_: Exception) { "Domy\u015Blny" }
}

private fun getCustomSoundDisplayName(context: Context, uriString: String?): String {
    if (uriString == null) return "Własny dźwięk"
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment ?: "Własny dźwięk"
    } catch (_: Exception) {
        "Własny dźwięk"
    }
}

/**
 * Plays the SELECTED notification sound at the given volume.
 * This fixes the bug where the preview didn't play the chosen sound.
 */
private fun playSelectedSoundPreview(context: Context, volume: Float) {
    try {
        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val soundIndex = prefs.getInt("notification_sound_index", 0)
        if (soundIndex == -1) return // silence mode - nothing to play

        val uri = if (soundIndex == CUSTOM_SOUND_INDEX) {
            val uriString = prefs.getString("notification_sound_custom_uri", null) ?: return
            Uri.parse(uriString)
        } else {
            val rm = RingtoneManager(context)
            rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
            val cursor = rm.cursor
            if (cursor.count == 0) return
            rm.getRingtoneUri(soundIndex % cursor.count)
        }

        val player = MediaPlayer()
        player.setDataSource(context, uri)
        player.setVolume(volume, volume)
        player.isLooping = false
        player.setOnCompletionListener { it.release() }
        player.prepare()
        player.start()
    } catch (_: Exception) {}
}
