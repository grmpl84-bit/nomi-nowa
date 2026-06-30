package com.focusremind.app.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.focusremind.app.R
import kotlinx.coroutines.launch

private const val TOTAL_PAGES = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE) }
    // If language was already chosen (Activity restarted after locale change), skip to page 1
    val languageAlreadyChosen = remember { prefs.getBoolean("onboarding_language_chosen", false) }
    val startPage = if (languageAlreadyChosen) 1 else 0
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { TOTAL_PAGES })
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
                    // === PAGE 0: LANGUAGE SELECTION (no translations needed - flags speak for themselves) ===
                    0 -> LanguageSelectionPage(
                        onLanguageSelected = { langCode ->
                            // Save flag BEFORE locale change (Activity will restart!)
                            prefs.edit().putBoolean("onboarding_language_chosen", true).apply()
                            // Apply language immediately so next pages are translated
                            if (langCode.isNotEmpty()) {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(langCode)
                                )
                            }
                            // Note: setApplicationLocales() restarts Activity.
                            // After restart, languageAlreadyChosen=true → pager starts at page 1.
                        }
                    )

                    // === PAGE 1: WELCOME ===
                    1 -> OnboardingPage(
                        emoji = "\uD83D\uDC4B",
                        title = stringResource(R.string.onboarding_welcome_title),
                        description = stringResource(R.string.onboarding_welcome_desc),
                        buttonText = null,
                        onButtonClick = {}
                    )

                    // === PAGE 2: SPEECH OFFLINE ===
                    2 -> OnboardingPage(
                        emoji = "\uD83C\uDF99\uFE0F",
                        title = stringResource(R.string.onboarding_speech_title),
                        description = stringResource(R.string.onboarding_speech_desc),
                        buttonText = stringResource(R.string.onboarding_speech_button),
                        onButtonClick = {
                            try {
                                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_MAIN).apply {
                                        setClassName(
                                            "com.google.android.googlequicksearchbox",
                                            "com.google.android.voicesearch.greco3.languagepack.InstallActivity"
                                        )
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )

                    // === PAGE 3: BATTERY ===
                    3 -> OnboardingPage(
                        emoji = "\uD83D\uDD0B",
                        title = stringResource(R.string.onboarding_battery_title),
                        description = stringResource(R.string.onboarding_battery_desc),
                        buttonText = stringResource(R.string.onboarding_battery_button),
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

                    // === PAGE 4: DONE ===
                    4 -> OnboardingPage(
                        emoji = "\uD83D\uDE80",
                        title = stringResource(R.string.onboarding_done_title),
                        description = stringResource(R.string.onboarding_done_desc),
                        buttonText = stringResource(R.string.onboarding_done_button),
                        showSettingsIcon = false,
                        onButtonClick = {
                            prefs.edit()
                                .putBoolean("onboarding_done", true)
                                .putBoolean("onboarding_language_chosen", false)
                                .apply()
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
                    repeat(TOTAL_PAGES) { index ->
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

                // Next/Skip buttons (not shown on language page or last page)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage in 1 until TOTAL_PAGES - 1) {
                        TextButton(onClick = {
                            context.getSharedPreferences("nomi_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("onboarding_done", true).apply()
                            onFinished()
                        }) {
                            Text(stringResource(R.string.skip))
                        }

                        Button(onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }) {
                            Text(stringResource(R.string.next))
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                        }
                    } else if (pagerState.currentPage == 0) {
                        // On language page — no skip, no next (user must pick a language)
                        Spacer(Modifier.weight(1f))
                    } else {
                        // Last page — button is inside the page content
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Language selection page — first screen of onboarding.
 * Uses flags and native language names so it's understandable
 * regardless of the user's current language.
 * No translated text needed — the visual design is universal.
 */
@Composable
private fun LanguageSelectionPage(onLanguageSelected: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Globe icon — universally understood as "language"
        Text("\uD83C\uDF0D", fontSize = 56.sp)

        Spacer(Modifier.height(16.dp))

        // Title in multiple languages stacked — user sees their language
        Text(
            "Choose language / Wybierz j\u0119zyk",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1A1A2E)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Sprache w\u00E4hlen / Choisir la langue",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF888888)
        )

        Spacer(Modifier.height(28.dp))

        // Language buttons — flag + native name, large and clear
        val languages = listOf(
            "\uD83C\uDDF5\uD83C\uDDF1" to ("Polski" to "pl"),
            "\uD83C\uDDEC\uD83C\uDDE7" to ("English" to "en"),
            "\uD83C\uDDE9\uD83C\uDDEA" to ("Deutsch" to "de"),
            "\uD83C\uDDEB\uD83C\uDDF7" to ("Fran\u00E7ais" to "fr"),
            "\uD83C\uDDEE\uD83C\uDDF9" to ("Italiano" to "it"),
            "\uD83C\uDDEA\uD83C\uDDF8" to ("Espa\u00F1ol" to "es"),
            "\uD83C\uDDF7\uD83C\uDDFA" to ("\u0420\u0443\u0441\u0441\u043A\u0438\u0439" to "ru"),
        )

        languages.forEach { (flag, nameAndCode) ->
            val (name, code) = nameAndCode
            FilledTonalButton(
                onClick = { onLanguageSelected(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF7C4DFF).copy(alpha = 0.08f)
                )
            ) {
                Text(flag, fontSize = 22.sp)
                Spacer(Modifier.width(16.dp))
                Text(
                    name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A2E)
                )
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
