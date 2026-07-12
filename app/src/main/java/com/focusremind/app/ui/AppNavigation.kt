package com.focusremind.app.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.concurrent.TimeUnit

// Beta-testing build only: reminders/alarms keep working normally even after
// expiry (AlarmManager operates independently of the app UI) — this only
// blocks opening the app UI itself, so testers know to grab a fresh build.
private const val TRIAL_DURATION_DAYS = 30L

@Composable
fun AppNavigation(startWithVoice: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE) }

    val isExpired = remember {
        var firstLaunchAt = prefs.getLong("first_launch_at", 0L)
        if (firstLaunchAt == 0L) {
            firstLaunchAt = System.currentTimeMillis()
            prefs.edit().putLong("first_launch_at", firstLaunchAt).apply()
        }
        val trialDurationMs = TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS)
        System.currentTimeMillis() - firstLaunchAt > trialDurationMs
    }

    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_done", false)) }
    // Skip splash when coming from widget (speed is key!)
    var showSplash by remember { mutableStateOf(!startWithVoice) }

    if (isExpired) {
        TrialExpiredScreen()
    } else if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
    } else if (showOnboarding && !startWithVoice) {
        OnboardingScreen(onFinished = { showOnboarding = false })
    } else {
        val nav = rememberNavController()
        NavHost(nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onAddReminder = { nav.navigate("voice") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenHistory = { nav.navigate("history") },
                    onOpenRecurring = { nav.navigate("recurring") },
                    startRecordingImmediately = startWithVoice
                )
            }
            composable("recurring") {
                RecurringScreen(onBack = { nav.popBackStack() })
            }
            composable("voice") {
                VoiceScreen(onBack = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSoundPicker = { nav.navigate("sounds") },
                    onShowOnboarding = { showOnboarding = true }
                )
            }
            composable("sounds") {
                SoundPickerScreen(onBack = { nav.popBackStack() })
            }
            composable("history") {
                HistoryScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
