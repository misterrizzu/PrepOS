package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ProgressColor
import com.example.ui.theme.isAppDarkTheme
import kotlinx.coroutines.delay

/**
 * Reusable Unified Animated Progress Bar
 * Features:
 * - Starts at 0 and animates to target percentage with smooth deceleration
 * - Dynamically updates color in sync with bar width using ProgressColor
 * - Adapts track color for Light (0xFFEDF0F5) and Dark (0xFF1E2040) themes
 */
@Composable
fun AnimatedProgressBar(
    percent: Float,                          // 0f to 100f — actual target value
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,                       // default 6dp — thicker, readable
    animationDuration: Int = 1000,           // 1 second total
    animationDelay: Int = 150,               // slight delay before starting
    cornerRadius: Dp = 3.dp
) {
    val isDark = isAppDarkTheme()
    val clampedPercent = percent.coerceIn(0f, 100f)

    // Animated value — starts at 0, animates to actual percent
    val animatedPercent = remember { Animatable(0f) }

    // Animate color separately — also follows the interpolated stop scale
    val animatedColor by animateColorAsState(
        targetValue = ProgressColor.forProgressSmart(animatedPercent.value),
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "progressColor"
    )

    // Trigger animation when composable enters composition or percent changes
    LaunchedEffect(key1 = clampedPercent) {
        animatedPercent.snapTo(0f)           // start from 0
        if (animationDelay > 0) {
            delay(animationDelay.toLong())   // wait for delay
        }
        animatedPercent.animateTo(
            targetValue = clampedPercent,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing  // smooth deceleration
            )
        )
    }

    val trackColor = if (isDark) Color(0xFF1E2040) else Color(0xFFEDF0F5)

    // Track + fill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor)
    ) {
        // Animated fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = (animatedPercent.value / 100f).coerceIn(0f, 1f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(animatedColor)
        )
    }
}

/**
 * Animated Circular Test Score / Accuracy Component
 * Draws an arc starting at -90deg (top) with synchronized score color and text.
 */
@Composable
fun TestScoreCircle(
    score: Float,                            // 0f to 100f
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    strokeWidth: Dp = 5.dp
) {
    val isDark = isAppDarkTheme()
    val clampedScore = score.coerceIn(0f, 100f)
    val animatedScore = remember { Animatable(0f) }
    val animatedColor by animateColorAsState(
        targetValue = ProgressColor.forProgressSmart(animatedScore.value),
        animationSpec = tween(100),
        label = "scoreColor"
    )

    LaunchedEffect(key1 = clampedScore) {
        animatedScore.snapTo(0f)
        delay(100L)
        animatedScore.animateTo(
            targetValue = clampedScore,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    val trackColor = if (isDark) Color(0xFF1E2040) else Color(0xFFEDF0F5)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Background circle track + animated arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth.toPx())
            )
            drawArc(
                color = animatedColor,
                startAngle = -90f,
                sweepAngle = (animatedScore.value / 100f) * 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
        // Score text
        Text(
            text = "${animatedScore.value.toInt()}%",
            fontSize = if (size <= 44.dp) 10.5.sp else 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = animatedColor
        )
    }
}
