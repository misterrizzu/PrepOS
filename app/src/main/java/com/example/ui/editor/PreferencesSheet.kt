package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteFont
import com.example.model.PaperStyle
import com.example.model.PaperTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesSheet(
    currentPaperStyle: PaperStyle,
    defaultPaperStyle: PaperStyle,
    currentTheme: PaperTheme,
    defaultTheme: PaperTheme,
    currentFont: NoteFont,
    defaultFont: NoteFont,
    fontSizeSp: Float,
    defaultFontSizeSp: Float,
    lineSpacing: Float,
    defaultLineSpacing: Float,
    onDismiss: () -> Unit,
    onPaperStyleChange: (PaperStyle) -> Unit,
    onSetDefaultPaperStyle: (PaperStyle) -> Unit,
    onThemeChange: (PaperTheme) -> Unit,
    onSetDefaultTheme: (PaperTheme) -> Unit,
    onFontChange: (NoteFont) -> Unit,
    onSetDefaultFont: (NoteFont) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSetDefaultFontSize: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onSetDefaultLineSpacing: (Float) -> Unit,
    onSaveAllAsDefault: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var defaultFeedbackMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Reading & Paper Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Tap to select. Tap ♥ on active option to set as default.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Optional transient feedback pill
            AnimatedVisibility(visible = defaultFeedbackMsg != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = defaultFeedbackMsg ?: "",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Paper Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAPER STYLE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                if (currentPaperStyle == defaultPaperStyle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaperStyle.values().forEach { style ->
                    val isSelected = style == currentPaperStyle
                    val isDefault = style == defaultPaperStyle

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(style) {
                                detectTapGestures(
                                    onTap = {
                                        onPaperStyleChange(style)
                                    },
                                    onDoubleTap = {
                                        onPaperStyleChange(style)
                                        onSetDefaultPaperStyle(style)
                                        defaultFeedbackMsg = "${style.title} set as default paper style"
                                    }
                                )
                            }
                            .testTag("paper_style_${style.name.lowercase()}")
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = style.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Heart Icon for Defaulting
                                if (isSelected || isDefault) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDefault) Color(0xFFFEE2E2)
                                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                            )
                                            .clickable {
                                                onSetDefaultPaperStyle(style)
                                                defaultFeedbackMsg = "${style.title} set as default paper style"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isDefault) "Default Paper" else "Set as Default",
                                            tint = if (isDefault) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(22.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Paper / Reading Theme
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAPER THEME",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                if (currentTheme == defaultTheme) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaperTheme.values().forEach { theme ->
                    val isSelected = theme == currentTheme
                    val isDefault = theme == defaultTheme

                    val previewBg = when (theme) {
                        PaperTheme.PAPER_LIGHT -> Color(0xFFFAF9F6)
                        PaperTheme.WARM_SEPIA -> Color(0xFFFBF0D9)
                        PaperTheme.SOFT_SLATE -> Color(0xFF1E293B)
                        PaperTheme.AMOLED_DARK -> Color(0xFF090D16)
                    }
                    val previewText = when (theme) {
                        PaperTheme.PAPER_LIGHT -> Color(0xFF0F172A)
                        PaperTheme.WARM_SEPIA -> Color(0xFF2C2216)
                        PaperTheme.SOFT_SLATE, PaperTheme.AMOLED_DARK -> Color(0xFFF1F5F9)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = previewBg,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(theme) {
                                detectTapGestures(
                                    onTap = {
                                        onThemeChange(theme)
                                    },
                                    onDoubleTap = {
                                        onThemeChange(theme)
                                        onSetDefaultTheme(theme)
                                        defaultFeedbackMsg = "${theme.displayName} set as default theme"
                                    }
                                )
                            }
                            .testTag("paper_theme_${theme.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = theme.displayName.split(" ").first(),
                                color = previewText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            if (isSelected || isDefault) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDefault) Color(0xFFFEE2E2)
                                            else previewText.copy(alpha = 0.15f)
                                        )
                                        .clickable {
                                            onSetDefaultTheme(theme)
                                            defaultFeedbackMsg = "${theme.displayName} set as default theme"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isDefault) "Default Theme" else "Set as Default",
                                        tint = if (isDefault) Color(0xFFEF4444) else (if (theme == PaperTheme.AMOLED_DARK || theme == PaperTheme.SOFT_SLATE) Color.White else MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(22.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Font Family
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STUDY FONT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                if (currentFont == defaultFont) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NoteFont.values().forEach { font ->
                    val isSelected = font == currentFont
                    val isDefault = font == defaultFont

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(font) {
                                detectTapGestures(
                                    onTap = {
                                        onFontChange(font)
                                    },
                                    onDoubleTap = {
                                        onFontChange(font)
                                        onSetDefaultFont(font)
                                        defaultFeedbackMsg = "${font.displayName} set as default font"
                                    }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = font.displayName,
                                    fontFamily = when (font) {
                                        NoteFont.SANS_SERIF -> FontFamily.SansSerif
                                        NoteFont.SERIF -> FontFamily.Serif
                                        NoteFont.MONOSPACE -> FontFamily.Monospace
                                        NoteFont.HANDWRITTEN -> FontFamily.Cursive
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                if (isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = "Default",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFEF4444),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    // Heart button to toggle default
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDefault) Color(0xFFFEE2E2)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                onSetDefaultFont(font)
                                                defaultFeedbackMsg = "${font.displayName} set as default font"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isDefault) "Default font" else "Set as default",
                                            tint = if (isDefault) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Font Size", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${fontSizeSp.toInt()} sp", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (fontSizeSp.toInt() == defaultFontSizeSp.toInt()) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            onSetDefaultFontSize(fontSizeSp)
                            defaultFeedbackMsg = "${fontSizeSp.toInt()} sp set as default font size"
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (fontSizeSp.toInt() == defaultFontSizeSp.toInt()) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Set default font size",
                            tint = if (fontSizeSp.toInt() == defaultFontSizeSp.toInt()) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (fontSizeSp.toInt() == defaultFontSizeSp.toInt()) "Default" else "Make Default",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fontSizeSp.toInt() == defaultFontSizeSp.toInt()) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
            Slider(
                value = fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = 13f..24f,
                steps = 10
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Line Spacing Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Line Spacing", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(String.format("%.1fx", lineSpacing), style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (Math.abs(lineSpacing - defaultLineSpacing) < 0.05f) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            onSetDefaultLineSpacing(lineSpacing)
                            defaultFeedbackMsg = String.format("%.1fx set as default line spacing", lineSpacing)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (Math.abs(lineSpacing - defaultLineSpacing) < 0.05f) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Set default spacing",
                            tint = if (Math.abs(lineSpacing - defaultLineSpacing) < 0.05f) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (Math.abs(lineSpacing - defaultLineSpacing) < 0.05f) "Default" else "Make Default",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (Math.abs(lineSpacing - defaultLineSpacing) < 0.05f) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
            Slider(
                value = lineSpacing,
                onValueChange = onLineSpacingChange,
                valueRange = 1.2f..2.0f,
                steps = 7
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Global Batch Set Default Button
            Button(
                onClick = {
                    onSaveAllAsDefault()
                    defaultFeedbackMsg = "All current reading settings saved as default for all chapters!"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("save_all_as_default_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save All as Default for All Chapters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
