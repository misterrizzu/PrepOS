package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.isAppDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.viewmodel.ActiveFocusSession

/**
 * Compact Quick Actions section for PrepOS Home Screen
 */
@Composable
fun HomeQuickActions(
    onStartFocus: () -> Unit,
    onTakeTest: () -> Unit,
    onAskAI: () -> Unit,
    onAddSubject: () -> Unit,
    activeFocusSession: ActiveFocusSession? = null,
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()
    val isFocusActive = activeFocusSession?.isActive == true
    val focusCountdownLabel = if (isFocusActive && activeFocusSession != null) {
        if (activeFocusSession.isTimerMode) {
            val remSec = activeFocusSession.remainingSeconds
            String.format("%02d:%02d", remSec / 60, remSec % 60)
        } else {
            val elSec = activeFocusSession.elapsedSeconds
            String.format("%02d:%02d", elSec / 60, elSec % 60)
        }
    } else "Focus Mode"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                fontSize = 11.sp
            ),
            color = if (isDark) Color(0xFF64748B) else LightTextSecondary,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionItem(
                icon = if (isFocusActive) {
                    if (activeFocusSession?.isPaused == true) Icons.Filled.PlayArrow else Icons.Filled.Pause
                } else Icons.Filled.Timer,
                label = if (isFocusActive) "${if (activeFocusSession?.isTimerMode == true) "⏳" else "⏱️"} $focusCountdownLabel" else "Focus Mode",
                iconTint = if (isFocusActive) {
                    if (activeFocusSession?.isPaused == true) Color(0xFFF59E0B) else (if (isDark) Color(0xFFC084FC) else BrandPrimary)
                } else (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)),
                testTag = "home_action_focus",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isFocusActive) {
                        if (activeFocusSession?.isPaused == true) onResumeFocus() else onPauseFocus()
                    } else {
                        onStartFocus()
                    }
                }
            )

            QuickActionItem(
                icon = Icons.Filled.Quiz,
                label = "Mock Test",
                iconTint = if (isDark) Color(0xFFA78BFA) else BrandPrimary,
                testTag = "home_action_test",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = onTakeTest
            )

            QuickActionItem(
                icon = Icons.Filled.AutoAwesome,
                label = "Ask AI",
                iconTint = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                testTag = "home_action_ai",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = onAskAI
            )

            QuickActionItem(
                icon = Icons.Filled.Add,
                label = "New Subject",
                iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                testTag = "home_action_add_subject",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = onAddSubject
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    testTag: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardBorder = if (isDark) {
        BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
    } else {
        BorderStroke(1.dp, iconTint.copy(alpha = 0.20f))
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else LightSurface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            listOf(iconTint.copy(alpha = 0.05f), Color(0xFF0F172A))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(iconTint.copy(alpha = 0.07f), LightSurface)
                        )
                    }
                )
                .padding(vertical = 8.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = if (isDark) 0.14f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    ),
                    color = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

