package com.focusremind.app.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        emoji = "👋",
                        title = "Witaj w Nomi!",
                        description = "Twój drugi mózg.\nMów — Nomi zapamiętuje.",
                        buttonText = null,
                        onButtonClick = {}
                    )
                    1 -> OnboardingPage(
                        emoji = "🎙️",
                        title = "Mowa offline",
                        description = "Aby Nomi rozumiał Cię bez internetu, pobierz pakiet języka polskiego:\n\nUstawienia → Google → Rozpoznawanie mowy → Offline → Polski",
                        buttonText = "Otwórz ustawienia mowy",
                        onButtonClick = {
                            try {
                                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_MAIN).apply {
                                        setClassName("com.google.android.googlequicksearchbox",
                                            "com.google.android.voicesearch.greco3.languagepack.InstallActivity")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                    2 -> OnboardingPage(
                        emoji = "🔋",
                        title = "Powiadomienia w tle",
                        description = "Żeby Nomi mógł przypominać nawet gdy aplikacja jest zamknięta, wyłącz optymalizację baterii:\n\nUstawienia → Aplikacje → Nomi → Bateria → Bez ograniczeń",
                        buttonText = "Wyłącz optymalizację baterii",
                        onButtonClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (_: Exception) {}
                            }
                        }
                    )
                    3 -> OnboardingPage(
                        emoji = "🚀",
                        title = "Gotowe!",
                        description = "Przytrzymaj mikrofon → mów → puść.\nNomi zapamięta i przypomni.\n\nTo takie proste.",
                        buttonText = "Zaczynamy!",
                        showSettingsIcon = false,
                        onButtonClick = {
                            context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("onboarding_done", true).apply()
                            onFinished()
                        }
                    )
                }
            }

            // Page indicators + navigation
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { index ->
                        Box(
                            Modifier
                                .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        Color(0xFF7C4DFF)
                                    else
                                        Color(0xFFE0E0E0)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Next/Skip buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage < 3) {
                        TextButton(onClick = {
                            context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("onboarding_done", true).apply()
                            onFinished()
                        }) {
                            Text("Pomiń")
                        }

                        Button(onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }) {
                            Text("Dalej")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    emoji: String,
    title: String,
    description: String,
    buttonText: String?,
    showSettingsIcon: Boolean = true,
    onButtonClick: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 64.sp)

        Spacer(Modifier.height(24.dp))

        Text(
            title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1A1A2E)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF666666),
            lineHeight = 24.sp
        )

        if (buttonText != null) {
            Spacer(Modifier.height(24.dp))

            FilledTonalButton(
                onClick = onButtonClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f)
                )
            ) {
                if (showSettingsIcon) {
                    Icon(Icons.Default.Settings, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(buttonText)
            }
        }
    }
}
