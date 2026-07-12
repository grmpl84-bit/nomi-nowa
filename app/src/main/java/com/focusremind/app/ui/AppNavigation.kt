package com.focusremind.app.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.Calendar

// Beta-testing build only: reminders/alarms keep working normally even after
// expiry (AlarmManager operates independently of the app UI) — this only
// blocks opening the app UI itself, so testers know to grab a fresh build.
//
// Fixed calendar date (not "N days from install") — deliberately chosen so
// reinstalling the app can't reset/extend the trial period, since testers
// are expected to reinstall multiple times during testing.
private fun trialExpiresAt(): Long = Calendar.getInstance().apply {
    set(Calendar.YEAR, 2026)
    set(Calendar.MONTH, Calendar.SEPTEMBER) // expires at the very start of September = end of August
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun AppNavigation(startWithVoice: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE) }

    val isExpired = remember { System.currentTimeMillis() >= trialExpiresAt() }

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
