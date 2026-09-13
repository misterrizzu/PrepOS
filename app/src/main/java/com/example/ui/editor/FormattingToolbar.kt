package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ElementType
import com.example.viewmodel.ActiveFormattingState

@Composable
fun FormattingToolbar(
    modifier: Modifier = Modifier,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    formattingState: ActiveFormattingState = ActiveFormattingState(),
    onHeadingChange: (ElementType) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onTextColorSelected: (String?) -> Unit,
    onHighlightColorSelected: (String?) -> Unit,
    onToggleBulletList: () -> Unit,
    onToggleNumberedList: () -> Unit,
    onAlignmentChange: (String) -> Unit,
    onOpenInsertSheet: () -> Unit,
    onOpenAskAI: () -> Unit,
    hasSelection: Boolean = false
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showHighlightPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column {
            // Color Pickers Expansion Row
            if (showColorPicker) {
                ColorPaletteRow(
                    title = "Text Color",
                    colors = listOf(
                        null to MaterialTheme.colorScheme.onSurface,
                        "#000000" to Color.Black,
                        "#1E40AF" to Color(0xFF1E40AF),
                        "#DC2626" to Color(0xFFDC2626),
                        "#059669" to Color(0xFF059669),
                        "#D97706" to Color(0xFFD97706),
                        "#7C3AED" to Color(0xFF7C3AED)
                    ),
                    selectedHex = formattingState.activeTextColorHex,
                    onSelect = {
                        onTextColorSelected(it)
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false }
                )
            }

            if (showHighlightPicker) {
                ColorPaletteRow(
                    title = "Highlighter",
                    colors = listOf(
                        null to Color.Transparent, // Clear highlight
                        "#FEF08A" to Color(0xFFFEF08A),
                        "#BBF7D0" to Color(0xFFBBF7D0),
                        "#BAE6FD" to Color(0xFFBAE6FD),
                        "#FBCFE8" to Color(0xFFFBCFE8),
                        "#E9D5FF" to Color(0xFFE9D5FF)
                    ),
                    selectedHex = formattingState.activeHighlightColorHex,
                    onSelect = {
                        onHighlightColorSelected(it)
                        showHighlightPicker = false
                    },
                    onDismiss = { showHighlightPicker = false }
                )
            }

            // Main Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Undo / Redo
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                ToolbarDivider()

                // Headings Hierarchy with Visual Color System (H1 Gold, H2 Blue, H3 Green, H4 Teal)
                HeadingButton(
                    text = "H1",
                    textColor = Color(0xFFD97706),
                    fontSize = 15,
                    isActive = formattingState.activeHeading == ElementType.HEADING_1,
                    onClick = { onHeadingChange(ElementType.HEADING_1) }
                )
                HeadingButton(
                    text = "H2",
                    textColor = Color(0xFF2563EB),
                    fontSize = 14,
                    isActive = formattingState.activeHeading == ElementType.HEADING_2,
                    onClick = { onHeadingChange(ElementType.HEADING_2) }
                )
                HeadingButton(
                    text = "H3",
                    textColor = Color(0xFF16A34A),
                    fontSize = 13,
                    isActive = formattingState.activeHeading == ElementType.HEADING_3,
                    onClick = { onHeadingChange(ElementType.HEADING_3) }
                )
                HeadingButton(
                    text = "H4",
                    textColor = Color(0xFF0D9488),
                    fontSize = 12,
                    isActive = formattingState.activeHeading == ElementType.HEADING_4,
                    onClick = { onHeadingChange(ElementType.HEADING_4) }
                )
                HeadingButton(
                    text = "¶",
                    textColor = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14,
                    isActive = formattingState.activeHeading == ElementType.PARAGRAPH,
                    onClick = { onHeadingChange(ElementType.PARAGRAPH) }
                )

                ToolbarDivider()

                // Inline Formats with Active/Mixed State Tracking
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatBold,
                    desc = "Bold",
                    isActive = formattingState.isBold,
                    isMixed = formattingState.isBoldMixed,
                    onClick = onToggleBold
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatItalic,
                    desc = "Italic",
                    isActive = formattingState.isItalic,
                    isMixed = formattingState.isItalicMixed,
                    onClick = onToggleItalic
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatUnderlined,
                    desc = "Underline",
                    isActive = formattingState.isUnderline,
                    isMixed = formattingState.isUnderlineMixed,
                    onClick = onToggleUnderline
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatStrikethrough,
                    desc = "Strikethrough",
                    isActive = formattingState.isStrikethrough,
                    isMixed = formattingState.isStrikethroughMixed,
                    onClick = onToggleStrikethrough
                )

                ToolbarDivider()

                // Colors
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatColorText,
                    desc = "Text Color",
                    isActive = showColorPicker || formattingState.activeTextColorHex != null,
                    onClick = {
                        showColorPicker = !showColorPicker
                        showHighlightPicker = false
                    }
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatColorFill,
                    desc = "Highlight",
                    isActive = showHighlightPicker || formattingState.activeHighlightColorHex != null,
                    onClick = {
                        showHighlightPicker = !showHighlightPicker
                        showColorPicker = false
                    }
                )

                ToolbarDivider()

                // Lists & Alignment
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatListBulleted,
                    desc = "Bullet List",
                    isActive = formattingState.activeHeading == ElementType.BULLET_LIST,
                    onClick = onToggleBulletList
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatListNumbered,
                    desc = "Numbered List",
                    isActive = formattingState.activeHeading == ElementType.NUMBERED_LIST,
                    onClick = onToggleNumberedList
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatAlignLeft,
                    desc = "Align Left",
                    isActive = formattingState.alignment == "LEFT",
                    onClick = { onAlignmentChange("LEFT") }
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatAlignCenter,
                    desc = "Align Center",
                    isActive = formattingState.alignment == "CENTER",
                    onClick = { onAlignmentChange("CENTER") }
                )
                StatefulToolbarIconButton(
                    icon = Icons.Default.FormatAlignRight,
                    desc = "Align Right",
                    isActive = formattingState.alignment == "RIGHT",
                    onClick = { onAlignmentChange("RIGHT") }
                )

                ToolbarDivider()

                // Insert Element Button (+ Insert)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF6C63D9).copy(alpha = 0.12f))
                        .clickable(onClick = onOpenInsertSheet)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Insert",
                        tint = Color(0xFF6C63D9),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Insert",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C63D9)
                        )
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // Ask AI Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF7C3AED))
                        .clickable(onClick = onOpenAskAI)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask AI",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasSelection) "Ask AI ✦" else "AI Assist ✦",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadingButton(
    text: String,
    textColor: Color,
    fontSize: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) textColor.copy(alpha = 0.18f) else Color.Transparent
    val borderModifier = if (isActive) Modifier.border(1.5.dp, textColor, RoundedCornerShape(7.dp)) else Modifier

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .then(borderModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize.sp
            ),
            color = if (isActive) textColor else textColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun StatefulToolbarIconButton(
    icon: ImageVector,
    desc: String,
    isActive: Boolean = false,
    isMixed: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isMixed -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val iconTint = when {
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderModifier = when {
        isActive -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp))
        isMixed -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(7.dp))
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .then(borderModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
        if (isMixed) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun ColorPaletteRow(
    title: String,
    colors: List<Pair<String?, Color>>,
    selectedHex: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        colors.forEach { (hex, col) ->
            val isSelected = hex == selectedHex
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(col)
                    .border(
                        if (isSelected) 2.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else if (hex == null) Color.Gray else Color.White,
                        CircleShape
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (hex == null) {
                    Text("✕", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Done",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.clickable { onDismiss() }
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .height(20.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
