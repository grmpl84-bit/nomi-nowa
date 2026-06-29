package com.focusremind.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animation states
    var startAnimation by remember { mutableStateOf(false) }

    val iconScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "icon_scale"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(600),
        label = "icon_alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(800, delayMillis = 400),
        label = "text_alpha"
    )

    val sloganAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(800, delayMillis = 800),
        label = "slogan_alpha"
    )

    // Pulsating sound bars
    val infiniteTransition = rememberInfiniteTransition(label = "bars")
    val bar1 = infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b1")
    val bar2 = infiniteTransition.animateFloat(0.6f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "b2")
    val bar3 = infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b3")

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(4000) // Show splash for 4 seconds
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Logo: Speech bubble with "n" + sound bars (matching app icon exactly)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF9C27B0), // purple
                                Color(0xFF7C4DFF), // deep purple
                                Color(0xFF00BCD4)  // cyan
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Letter "n" as bold text (matching Nomi style)
                    Text(
                        "n",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(2.dp))
                    // Animated sound bars
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Small dot
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                        )
                        SoundBar(heightFraction = bar1.value, maxHeight = 20.dp)
                        SoundBar(heightFraction = bar2.value, maxHeight = 28.dp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // App name
            Text(
                "nomi",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E),
                modifier = Modifier.alpha(textAlpha),
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            // Slogan
            Text(
                "YOU SPEAK. NOMI REMEMBERS.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF7C4DFF),
                modifier = Modifier.alpha(sloganAlpha),
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun SoundBar(heightFraction: Float, maxHeight: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(5.dp)
            .height(maxHeight * heightFraction)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White)
    )
}
