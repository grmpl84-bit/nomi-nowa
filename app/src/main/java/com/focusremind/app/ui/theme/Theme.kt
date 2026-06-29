package com.focusremind.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5E),
    primaryContainer = Color(0xFF7AF8E2),
    secondary = Color(0xFF8B5000),
    secondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFF6750A4),
    tertiaryContainer = Color(0xFFE9DDFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5CDBC6),
    primaryContainer = Color(0xFF005047),
    secondary = Color(0xFFFFB86F),
    secondaryContainer = Color(0xFF6A3C00),
    tertiary = Color(0xFFCFBCFF),
    tertiaryContainer = Color(0xFF4F378A)
)

@Composable
fun FocusRemindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
