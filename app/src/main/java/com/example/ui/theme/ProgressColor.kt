package com.example.ui.theme

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Standardized 5-Color Progress & Card Tint System for PrepOS
 * 
 * Strict 5-tier progression:
 *   0–20%   = Red (0xFFEF4444)
 *   21–40%  = Orange / Red-Orange (0xFFF97316)
 *   41–60%  = Yellow / Amber (0xFFF59E0B)
 *   61–80%  = Light Green (0xFF84CC16)
 *   81–100% = Green (0xFF10B981)
 *
 * ONLY these five colors are shared across all progresses, chapter cards,
 * subject cards, test cards, and card tints throughout the app.
 */
object ProgressColor {

    // The canonical 5 colors
    val Level1Red = Color(0xFFEF4444)         // 0–20% Red
    val Level2Orange = Color(0xFFF97316)      // 21–40% Orange / Red-Orange
    val Level3Yellow = Color(0xFFF59E0B)      // 41–60% Yellow / Amber
    val Level4LightGreen = Color(0xFF84CC16)  // 61–80% Light Green
    val Level5Green = Color(0xFF10B981)       // 81–100% Green

    // 5 color stops
    val colorStops = listOf(
        20f  to Level1Red,
        40f  to Level2Orange,
        60f  to Level3Yellow,
        80f  to Level4LightGreen,
        100f to Level5Green
    )

    // Main mapping function: returns exactly one of the 5 colors
    fun forProgress(percent: Float): Color {
        val clamped = percent.coerceIn(0f, 100f)
        return when {
            clamped <= 20f -> Level1Red
            clamped <= 40f -> Level2Orange
            clamped <= 60f -> Level3Yellow
            clamped <= 80f -> Level4LightGreen
            else -> Level5Green
        }
    }

    // Returns the exact 5-level color for progress
    fun forProgressSmart(percent: Float): Color = forProgress(percent)

    // Helper for integer percentages
    fun forPercentage(percentage: Int): Color = forProgress(percentage.toFloat())

    // Soft background tint — same 5-level color at low opacity
    fun softBg(percent: Float, alpha: Float = 0.12f): Color =
        forProgress(percent).copy(alpha = alpha)

    // Border — same 5-level color at medium opacity
    fun border(percent: Float, alpha: Float = 0.35f): Color =
        forProgress(percent).copy(alpha = alpha)

    /**
     * Atmospheric "Fog" gradient tint:
     * Denser at top/start, diffusing gently through the card to create a subtle glow/mist aura.
     */
    fun fogBrush(
        percent: Float,
        isDark: Boolean,
        customAlphaMultiplier: Float = 1.0f
    ): Brush {
        val baseColor = forProgress(percent)
        val highAlpha = if (isDark) 0.16f * customAlphaMultiplier else 0.11f * customAlphaMultiplier
        val midAlpha = if (isDark) 0.06f * customAlphaMultiplier else 0.035f * customAlphaMultiplier
        val surfaceColor = if (isDark) Color(0xFF0F172A) else Color.White

        return Brush.linearGradient(
            0.0f to baseColor.copy(alpha = highAlpha),
            0.40f to baseColor.copy(alpha = midAlpha),
            1.0f to surfaceColor
        )
    }

    /**
     * Animated Fog Gradient:
     * Slowly drifts and breathes smoothly over time like shifting mist/fog.
     */
    @Composable
    fun rememberAnimatedFogBrush(
        percent: Float,
        isDark: Boolean,
        customAlphaMultiplier: Float = 1.0f,
        durationMillis: Int = 6000
    ): Brush {
        val infiniteTransition = rememberInfiniteTransition(label = "animated_fog_drift")
        val drift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fog_drift_offset"
        )

        val baseColor = forProgress(percent)
        val highAlpha = (if (isDark) 0.16f else 0.10f) * customAlphaMultiplier
        val midAlpha = (if (isDark) 0.06f else 0.035f) * customAlphaMultiplier
        val surfaceColor = if (isDark) Color(0xFF0F172A) else Color.White

        val startX = -40f + (drift * 90f)
        val startY = -30f + (drift * 50f)
        val endX = 500f + (drift * 110f)
        val endY = 800f + (drift * 90f)

        return Brush.linearGradient(
            0.0f to baseColor.copy(alpha = highAlpha),
            (0.35f + drift * 0.12f).coerceIn(0.2f, 0.65f) to baseColor.copy(alpha = midAlpha),
            1.0f to surfaceColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY)
        )
    }

    /**
     * Animated Color Fog Gradient with custom base color:
     */
    @Composable
    fun rememberAnimatedColorFogBrush(
        baseColor: Color,
        isDark: Boolean,
        customAlphaMultiplier: Float = 1.0f,
        surfaceColor: Color = if (isDark) Color(0xFF0F172A) else Color.White,
        durationMillis: Int = 6000
    ): Brush {
        val infiniteTransition = rememberInfiniteTransition(label = "animated_color_fog_drift")
        val drift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "color_fog_drift_offset"
        )

        val highAlpha = (if (isDark) 0.16f else 0.10f) * customAlphaMultiplier
        val midAlpha = (if (isDark) 0.06f else 0.035f) * customAlphaMultiplier

        val startX = -40f + (drift * 90f)
        val startY = -30f + (drift * 50f)
        val endX = 500f + (drift * 110f)
        val endY = 800f + (drift * 90f)

        return Brush.linearGradient(
            0.0f to baseColor.copy(alpha = highAlpha),
            (0.35f + drift * 0.12f).coerceIn(0.2f, 0.65f) to baseColor.copy(alpha = midAlpha),
            1.0f to surfaceColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY)
        )
    }

    /**
     * Atmospheric radial fog gradient originating from top-left corner
     */
    fun fogRadialBrush(
        percent: Float,
        isDark: Boolean,
        radius: Float = 650f
    ): Brush {
        val baseColor = forProgress(percent)
        val highAlpha = if (isDark) 0.18f else 0.12f
        val midAlpha = if (isDark) 0.05f else 0.03f
        val surfaceColor = if (isDark) Color(0xFF0F172A) else Color.White

        return Brush.radialGradient(
            listOf(
                baseColor.copy(alpha = highAlpha),
                baseColor.copy(alpha = midAlpha),
                surfaceColor
            ),
            center = Offset(0f, 0f),
            radius = radius
        )
    }
}
