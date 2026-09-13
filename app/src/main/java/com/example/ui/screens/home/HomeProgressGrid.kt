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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.ProgressColor

/**
 * Compact 2x2 Progress Grid for PrepOS Home Screen with subtle matching tint and enhanced layout
 */
@Composable
fun HomeProgressGrid(
    todayFocusedMinutes: Int,
    yesterdayFocusedMinutes: Int = 0,
    streakDays: Int,
    weeklyStudyHours: Float,
    accuracyPercent: Int,
    onCardClick: () -> Unit = {},
    onStreakClick: (() -> Unit)? = null,
    onAccuracyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YOUR PROGRESS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 11.sp
                ),
                color = if (isDark) Color(0xFF64748B) else LightTextSecondary
            )
        }

        // Row 1: Study Time & Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val (studyTimeDisplay, studySubtitle) = if (todayFocusedMinutes > 0) {
                Pair(formatMinutesToDisplay(todayFocusedMinutes), "Today")
            } else if (yesterdayFocusedMinutes > 0) {
                Pair(formatMinutesToDisplay(yesterdayFocusedMinutes), "Yesterday")
            } else {
                Pair("0m", "Today")
            }

            // Card 1: Study Time (Blue)
            ProgressCard(
                title = "Study Time",
                value = studyTimeDisplay,
                subtitle = studySubtitle,
                icon = Icons.Filled.Timer,
                iconColor = Color(0xFF38BDF8),
                darkGradient = listOf(Color(0xFF0C2A4A), Color(0xFF071424)),
                testTag = "home_progress_study_time",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = onCardClick
            )

            val streakDisplay = if (streakDays > 0) "$streakDays Days" else "0 Days"
            // Card 2: Streak (Amber/Orange)
            ProgressCard(
                title = "Streak",
                value = streakDisplay,
                subtitle = "Current",
                icon = Icons.Filled.Whatshot,
                iconColor = Color(0xFFF59E0B),
                darkGradient = listOf(Color(0xFF451A03), Color(0xFF1B0B02)),
                testTag = "home_progress_streak",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = onStreakClick ?: onCardClick
            )
        }

        // Row 2: Weekly Study Time & Accuracy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val weeklyDisplay = if (weeklyStudyHours > 0) {
                if (weeklyStudyHours >= 1.0f) "${String.format("%.1f", weeklyStudyHours)}h" else "${(weeklyStudyHours * 60).toInt()}m"
            } else "0h"

            // Card 3: Weekly Study (Purple)
            ProgressCard(
                title = "Weekly Study",
                value = weeklyDisplay,
                subtitle = "Last 7 Days",
                icon = Icons.Filled.Timeline,
                iconColor = Color(0xFFA855F7),
                darkGradient = listOf(Color(0xFF3B0764), Color(0xFF140324)),
                testTag = "home_progress_weekly",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = onCardClick
            )

            val accuracyDisplay = if (accuracyPercent > 0) "$accuracyPercent%" else "--"
            // Card 4: Accuracy (Emerald/Green)
            ProgressCard(
                title = "Accuracy",
                value = accuracyDisplay,
                subtitle = "Test Average",
                icon = Icons.Filled.Speed,
                iconColor = Color(0xFF10B981),
                darkGradient = listOf(Color(0xFF064E3B), Color(0xFF021610)),
                testTag = "home_progress_accuracy",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = onAccuracyClick ?: onCardClick
            )
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    darkGradient: List<Color>,
    testTag: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val cardBorder = if (isDark) {
        BorderStroke(1.2.dp, iconColor.copy(alpha = 0.45f))
    } else {
        BorderStroke(1.2.dp, iconColor.copy(alpha = 0.22f))
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0B1120) else LightSurface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(darkGradient)
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                iconColor.copy(alpha = 0.08f),
                                LightSurface
                            )
                        )
                    }
                )
                .padding(horizontal = 11.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Text Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.1.sp
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = if (isDark) Color.White else LightTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = iconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Icon Box on Right
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(iconColor.copy(alpha = if (isDark) 0.20f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

fun formatMinutesToDisplay(minutes: Int): String {
    return when {
        minutes <= 0 -> "0m"
        minutes < 60 -> "${minutes}m"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m > 0) "${h}h ${m}m" else "${h}h"
        }
    }
}

