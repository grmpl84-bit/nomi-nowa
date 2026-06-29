package com.focusremind.app.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.focusremind.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE) }
    var selectedIndex by remember { mutableIntStateOf(prefs.getInt("notification_sound_index", 0)) }
    var playingIndex by remember { mutableIntStateOf(-1) }

    val sounds = remember { getSystemSounds(context) }

    // MediaPlayer for previews (plays once, stops automatically)
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    // Stop player when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            player?.stop()
            player?.release()
            player = null
        }
    }

    fun stopPreview() {
        player?.stop()
        player?.release()
        player = null
        playingIndex = -1
    }

    fun playPreview(index: Int, uri: Uri) {
        stopPreview()
        try {
            player = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = false
                val vol = prefs.getFloat("notification_volume", 0.7f)
                setVolume(vol, vol)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    player = null
                    playingIndex = -1
                }
            }
            playingIndex = index
        } catch (_: Exception) {
            playingIndex = -1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_sound)) },
                navigationIcon = {
                    IconButton(onClick = { stopPreview(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(8.dp)
        ) {
            // Silent option
            item {
                SoundRow(
                    name = "🔇 Cisza (tylko wibracja)",
                    isSelected = selectedIndex == -1,
                    isPlaying = false,
                    onSelect = {
                        stopPreview()
                        selectedIndex = -1
                        prefs.edit().putInt("notification_sound_index", -1).apply()
                    },
                    onPlay = { /* no preview for silence */ }
                )
            }

            itemsIndexed(sounds) { index, (name, uri) ->
                SoundRow(
                    name = name,
                    isSelected = selectedIndex == index,
                    isPlaying = playingIndex == index,
                    onSelect = {
                        selectedIndex = index
                        prefs.edit().putInt("notification_sound_index", index).apply()
                    },
                    onPlay = {
                        if (playingIndex == index) {
                            stopPreview()
                        } else {
                            playPreview(index, uri)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SoundRow(
    name: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Text(
                name,
                Modifier.weight(1f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            // Play/stop button
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    "Odtwórz",
                    tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun getSystemSounds(context: Context): List<Pair<String, Uri>> {
    val sounds = mutableListOf<Pair<String, Uri>>()
    val rm = RingtoneManager(context)
    rm.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
    val cursor = rm.cursor
    if (cursor.moveToFirst()) {
        do {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = rm.getRingtoneUri(cursor.position)
            sounds.add(Pair(title, uri))
        } while (cursor.moveToNext())
    }
    return sounds
}
