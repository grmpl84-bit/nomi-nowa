package com.focusremind.app.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(startWithVoice: Boolean = false, skipSplash: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE) }
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_done", false)) }
    // Skip splash when coming from widget (speed is key!)
    var showSplash by remember { mutableStateOf(!startWithVoice && !skipSplash) }

    if (showSplash) {
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
                    onOpenShopping = { nav.navigate("shopping") },
                    startRecordingImmediately = startWithVoice
                )
            }
            composable("shopping") {
                ShoppingListScreen(onBack = { nav.popBackStack() })
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
