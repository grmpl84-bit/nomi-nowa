package com.focusremind.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

        // Switching between the three bottom-bar tabs should behave like
        // standard bottom navigation — no back-stack pile-up, each tab
        // keeps its own scroll/state when you come back to it.
        fun navigateToTab(route: String) {
            nav.navigate(route) {
                popUpTo("home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(Modifier.fillMaxSize()) {
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        onAddReminder = { nav.navigate("voice") },
                        onOpenSettings = { nav.navigate("settings") },
                        onOpenHistory = { nav.navigate("history") },
                        onOpenRecurring = { navigateToTab("recurring") },
                        onOpenShopping = { navigateToTab("shopping") },
                        startRecordingImmediately = startWithVoice
                    )
                }
                composable("shopping") {
                    ShoppingListScreen(
                        onOpenHome = { navigateToTab("home") },
                        onOpenRecurring = { navigateToTab("recurring") }
                    )
                }
                composable("recurring") {
                    RecurringScreen(
                        onOpenHome = { navigateToTab("home") },
                        onOpenShopping = { navigateToTab("shopping") }
                    )
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

            // Flying-icon confirmation ("where did that voice command go?"),
            // rendered above every screen so it plays no matter which tab
            // was visible when the mic was used.
            FlyingIconOverlay()
        }
    }
}
