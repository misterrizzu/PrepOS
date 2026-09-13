package com.example.ui.paper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PaperStyle
import com.example.model.PaperTheme
import com.example.ui.theme.MidnightPaperBg
import com.example.ui.theme.MidnightPaperLine
import com.example.ui.theme.MidnightPaperMarginLine
import com.example.ui.theme.PaperWhiteBg
import com.example.ui.theme.PaperWhiteLine
import com.example.ui.theme.PaperWhiteMarginLine
import com.example.ui.theme.SepiaPaperBg
import com.example.ui.theme.SepiaPaperLine
import com.example.ui.theme.SepiaPaperMarginLine
import com.example.ui.theme.SlatePaperBg
import com.example.ui.theme.SlatePaperLine
import com.example.ui.theme.SlatePaperMarginLine
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextPaperLight
import com.example.ui.theme.TextSepia

data class PaperThemeColors(
    val backgroundColor: Color,
    val patternColor: Color,
    val marginLineColor: Color,
    val textColor: Color
)

fun getPaperColors(theme: PaperTheme): PaperThemeColors {
    return when (theme) {
        PaperTheme.PAPER_LIGHT -> PaperThemeColors(
            backgroundColor = PaperWhiteBg,
            patternColor = PaperWhiteLine,
            marginLineColor = PaperWhiteMarginLine,
            textColor = TextPaperLight
        )
        PaperTheme.WARM_SEPIA -> PaperThemeColors(
            backgroundColor = SepiaPaperBg,
            patternColor = SepiaPaperLine,
            marginLineColor = SepiaPaperMarginLine,
            textColor = TextSepia
        )
        PaperTheme.SOFT_SLATE -> PaperThemeColors(
            backgroundColor = SlatePaperBg,
            patternColor = SlatePaperLine,
            marginLineColor = SlatePaperMarginLine,
            textColor = TextDark
        )
        PaperTheme.AMOLED_DARK -> PaperThemeColors(
            backgroundColor = MidnightPaperBg,
            patternColor = MidnightPaperLine,
            marginLineColor = MidnightPaperMarginLine,
            textColor = TextDark
        )
    }
}

@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    paperStyle: PaperStyle = PaperStyle.RULED,
    paperTheme: PaperTheme = PaperTheme.PAPER_LIGHT,
    lineSpacing: Dp = 28.dp,
    showMarginLine: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = getPaperColors(paperTheme)
    val density = LocalDensity.current
    val spacingPx = with(density) { lineSpacing.toPx() }
    val marginOffsetPx = with(density) { 16.dp.toPx() }
    val dotRadiusPx = with(density) { 1.0.dp.toPx() }
    val strokeWidthPx = with(density) { 0.75.dp.toPx() }
    val marginStrokeWidthPx = with(density) { 1.2.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw solid paper background
            drawRect(color = colors.backgroundColor)

            val width = size.width
            val height = size.height

            // 2. Draw Paper Style Patterns
            when (paperStyle) {
                PaperStyle.PLAIN -> {
                    // Clean plain background with subtle left margin
                    if (showMarginLine) {
                        drawLine(
                            color = colors.marginLineColor.copy(alpha = 0.35f),
                            start = Offset(marginOffsetPx, 0f),
                            end = Offset(marginOffsetPx, height),
                            strokeWidth = marginStrokeWidthPx
                        )
                    }
                }
                PaperStyle.RULED -> {
                    // Subtle horizontal notebook rules
                    var currentY = spacingPx
                    while (currentY < height) {
                        drawLine(
                            color = colors.patternColor.copy(alpha = 0.55f),
                            start = Offset(0f, currentY),
                            end = Offset(width, currentY),
                            strokeWidth = strokeWidthPx
                        )
                        currentY += spacingPx
                    }
                    // Left notebook margin guideline (placed safely to the left of text)
                    if (showMarginLine) {
                        drawLine(
                            color = colors.marginLineColor.copy(alpha = 0.5f),
                            start = Offset(marginOffsetPx, 0f),
                            end = Offset(marginOffsetPx, height),
                            strokeWidth = marginStrokeWidthPx
                        )
                    }
                }
                PaperStyle.GRID -> {
                    // Vertical grid lines
                    var currentX = spacingPx
                    while (currentX < width) {
                        drawLine(
                            color = colors.patternColor.copy(alpha = 0.35f),
                            start = Offset(currentX, 0f),
                            end = Offset(currentX, height),
                            strokeWidth = strokeWidthPx
                        )
                        currentX += spacingPx
                    }
                    // Horizontal grid lines
                    var currentY = spacingPx
                    while (currentY < height) {
                        drawLine(
                            color = colors.patternColor.copy(alpha = 0.35f),
                            start = Offset(0f, currentY),
                            end = Offset(width, currentY),
                            strokeWidth = strokeWidthPx
                        )
                        currentY += spacingPx
                    }
                }
                PaperStyle.DOTTED -> {
                    // Dot matrix
                    var currentX = spacingPx
                    while (currentX < width) {
                        var currentY = spacingPx
                        while (currentY < height) {
                            drawCircle(
                                color = colors.patternColor.copy(alpha = 0.45f),
                                radius = dotRadiusPx,
                                center = Offset(currentX, currentY)
                            )
                            currentY += spacingPx
                        }
                        currentX += spacingPx
                    }
                }
            }
        }

        // Render content over the paper surface
        content()
    }
}
