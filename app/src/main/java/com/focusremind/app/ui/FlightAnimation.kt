package com.focusremind.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Tiny global event bus so a plain (non-composable) function like
 * handleUniversalVoiceInput can trigger the "flying icon" animation from
 * wherever the mic actually was — the destination tab (home/recurring/
 * shopping) is always one of the three persistent bottom-bar tabs, visible
 * on all three screens, so the flight target is the same regardless of
 * which screen the user was on when they spoke.
 */
object FlightBus {
    var destination by mutableStateOf<String?>(null)
}

/**
 * Renders a small icon flying from roughly where the mic sits to the
 * matching bottom-bar tab, with a little bounce on landing. Purely
 * decorative — it doesn't gate navigation or saving, which already
 * happened by the time this plays.
 */
@Composable
fun FlyingIconOverlay() {
    val destination = FlightBus.destination ?: return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val targetXFraction = when (destination) {
            "home" -> 1f / 6f
            "recurring" -> 3f / 6f
            "shopping" -> 5f / 6f
            else -> 0.5f
        }
        val icon = when (destination) {
            "home" -> Icons.Default.List
            "recurring" -> Icons.Default.Repeat
            "shopping" -> Icons.Default.ShoppingCart
            else -> Icons.Default.List
        }

        val progress = remember(destination) { Animatable(0f) }
        val bottomBarPx = with(density) { 64.dp.toPx() }
        val micAreaPx = with(density) { 140.dp.toPx() }

        LaunchedEffect(destination) {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = tween(550, easing = CubicBezierEasing(0.3f, -0.2f, 0.7f, 1.3f))
            )
            FlightBus.destination = null
        }

        val t = progress.value
        val startX = widthPx / 2f
        val startY = heightPx - micAreaPx
        val endX = widthPx * targetXFraction
        val endY = heightPx - bottomBarPx / 2f
        val curX = startX + (endX - startX) * t
        val curY = startY + (endY - startY) * t
        val scale = 1f - 0.45f * t

        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    translationX = curX - size.width / 2f
                    translationY = curY - size.height / 2f
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}
