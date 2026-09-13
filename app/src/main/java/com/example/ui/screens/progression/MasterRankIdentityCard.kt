package com.example.ui.screens.progression

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProgressionOverview
import com.example.model.Rank
import com.example.ui.screens.FocusTimeScope
import com.example.ui.screens.home.formatMinutesToDisplay
import com.example.ui.theme.isAppDarkTheme
import java.util.Locale

/**
 * Master Rank & Identity Progression Card for Focus Hub.
 * 
 * Features:
 * - In Dark Mode: Current badge colour tinted aura background (#0D1222 + badge tint)
 * - In Light Mode: Pristine adaptive light background with subtle badge tint & clear slate typography
 * - Rank crest, rank title, and verified XP display (e.g. "5,473 XP · +120 today")
 * - Next rank tracker with percentage indicator
 * - Three semantic activity cards (Streak consistency, Study Time with Dropdown, Target goal)
 * - Clicking Streak opens streak & milestones info
 * - Clicking Study Time opens time scope dropdown (Today, Last 7 Days, Month, All Time)
 * - Compact [☷ Tasks] action button
 */
@Composable
fun MasterRankIdentityCard(
    progression: ProgressionOverview,
    todayStudiedMinutes: Int,
    todayTargetMinutes: Int,
    onOpenTasks: () -> Unit,
    onOpenRoadmap: () -> Unit,
    modifier: Modifier = Modifier,
    onStreakClick: (() -> Unit)? = null,
    timeScope: FocusTimeScope = FocusTimeScope.SELECTED_DAY,
    scopeStudyMinutes: Int = todayStudiedMinutes,
    onSelectTimeScope: ((FocusTimeScope) -> Unit)? = null,
    onTestStreak: ((Int) -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    val currentRank = progression.rankProgress.rank
    val primaryColor = currentRank.getPrimaryColor()
    val secondaryColor = currentRank.getSecondaryColor()

    val nextRank = progression.rankProgress.nextRank
    val animatedProgress by animateFloatAsState(
        targetValue = progression.rankProgress.progressPercent,
        animationSpec = tween(durationMillis = 800),
        label = "rank_progress"
    )

    var showScopeDropdown by remember { mutableStateOf(false) }

    // Adaptive card background:
    // Dark mode: current badge colour tinted aura
    // Light mode: pristine clean white card with subtle badge tint
    val cardBackgroundBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                primaryColor.copy(alpha = 0.22f),
                Color(0xFF0F1526),
                Color(0xFF090D1A)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                primaryColor.copy(alpha = 0.08f),
                Color(0xFFFFFFFF),
                Color(0xFFFBFBFE)
            )
        )
    }

    val cardBorder = if (isDark) {
        BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    primaryColor.copy(alpha = 0.65f),
                    secondaryColor.copy(alpha = 0.40f),
                    primaryColor.copy(alpha = 0.25f)
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.2.dp,
            color = primaryColor.copy(alpha = 0.35f)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("master_rank_identity_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Top Row: [Rank Emblem] + [Rank Title & XP Line] + [☷ Tasks Button]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Crest Emblem (tap to view Roadmap, Ladder & Badges)
                    Box(
                        modifier = Modifier
                            .clickable { onOpenRoadmap() }
                            .testTag("rank_card_crest_button")
                    ) {
                        RankInsigniaArtwork(
                            rank = currentRank,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Rank Title and Streamlined XP Display
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenRoadmap() }
                    ) {
                        Text(
                            text = currentRank.title.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = primaryColor,
                                fontSize = 17.sp,
                                letterSpacing = 0.8.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // XP Display: "5,473 XP · +120 today"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${String.format(Locale.US, "%,d", progression.totalXp)} XP",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 13.5.sp
                                )
                            )
                            if (progression.todayTotalXp > 0) {
                                Text(
                                    text = " · +${progression.todayTotalXp} today",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryColor,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Compact [☷ Tasks] Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) primaryColor.copy(alpha = 0.16f) else primaryColor.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = if (isDark) 0.50f else 0.35f)),
                        modifier = Modifier
                            .clickable { onOpenTasks() }
                            .testTag("rank_card_tasks_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "☷",
                                fontSize = 12.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tasks",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else primaryColor,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Next Rank Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val progressLabel = if (nextRank != null) {
                            val remaining = (nextRank.minXp - progression.totalXp).coerceAtLeast(0L)
                            "${String.format(Locale.US, "%,d", remaining)} XP to ${nextRank.title}"
                        } else {
                            "Grandmaster (Supreme Rank)"
                        }

                        Text(
                            text = progressLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                fontSize = 11.sp
                            )
                        )

                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryColor,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = primaryColor,
                        trackColor = if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Three Semantic Activity Cards: Consistency (Streak) → Activity (Study Time) → Goal (Target)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Streak (Consistency) - clicking opens streak info & milestones
                    val streakStatus = if (todayStudiedMinutes > 0) "Active 🔥" else if (progression.streakCount > 0) "Needs Study" else "Start Today"
                    ActivityStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Whatshot,
                        title = "Streak",
                        value = "${progression.streakCount} Days",
                        subtitle = streakStatus,
                        accentColor = Color(0xFFF59E0B),
                        gradientStart = if (isDark) Color(0xFF451A03).copy(alpha = 0.35f) else Color(0xFFFEF3C7),
                        isDark = isDark,
                        testTag = "focus_stat_streak",
                        onClick = {
                            if (onStreakClick != null) {
                                onStreakClick()
                            } else {
                                onOpenRoadmap()
                            }
                        }
                    )

                    // Card 2: Study Time (Actual Activity with Scope Dropdown: Today, 7 Days, Month, All Time)
                    val scopeTitle = when (timeScope) {
                        FocusTimeScope.SELECTED_DAY -> "Today ▼"
                        FocusTimeScope.THIS_WEEK -> "Weekly ▼"
                        FocusTimeScope.THIS_MONTH -> "Monthly ▼"
                        FocusTimeScope.ALL_TIME -> "All Time ▼"
                    }
                    val scopeSubtitle = when (timeScope) {
                        FocusTimeScope.SELECTED_DAY -> if (todayStudiedMinutes > 0) "Daily Focus" else "0m Today"
                        FocusTimeScope.THIS_WEEK -> "Last 7 Days"
                        FocusTimeScope.THIS_MONTH -> "Last 30 Days"
                        FocusTimeScope.ALL_TIME -> "Lifetime Study"
                    }
                    val displayMinutes = if (timeScope == FocusTimeScope.SELECTED_DAY) todayStudiedMinutes else scopeStudyMinutes
                    val formattedStudyTime = if (displayMinutes > 0) formatMinutesToDisplay(displayMinutes) else "0m"

                    Box(modifier = Modifier.weight(1f)) {
                        ActivityStatCard(
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Timer,
                            title = scopeTitle,
                            value = formattedStudyTime,
                            subtitle = scopeSubtitle,
                            accentColor = Color(0xFF0284C7),
                            gradientStart = if (isDark) Color(0xFF0C2A4A).copy(alpha = 0.35f) else Color(0xFFE0F2FE),
                            isDark = isDark,
                            testTag = "focus_stat_study_time",
                            onClick = { showScopeDropdown = true }
                        )

                        // Study Time Scope Dropdown Menu
                        DropdownMenu(
                            expanded = showScopeDropdown,
                            onDismissRequest = { showScopeDropdown = false }
                        ) {
                            FocusTimeScope.values().forEach { scope ->
                                val optionLabel = when (scope) {
                                    FocusTimeScope.SELECTED_DAY -> "Today (Selected Day)"
                                    FocusTimeScope.THIS_WEEK -> "Weekly (Today + Last 6 Days)"
                                    FocusTimeScope.THIS_MONTH -> "Monthly (Today + Last 30 Days)"
                                    FocusTimeScope.ALL_TIME -> "All Time (Lifetime Total)"
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = optionLabel,
                                            fontWeight = if (timeScope == scope) FontWeight.Bold else FontWeight.Normal,
                                            color = if (timeScope == scope) primaryColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSelectTimeScope?.invoke(scope)
                                        showScopeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Card 3: Target (Goal Completion) - clicking opens task checklist
                    val effectiveTarget = todayTargetMinutes.coerceAtLeast(1)
                    val isTargetAchieved = todayStudiedMinutes >= effectiveTarget && todayStudiedMinutes > 0
                    val targetSubtitle = if (isTargetAchieved) "Achieved 🎯" else "Today"
                    val targetColor = if (isTargetAchieved) Color(0xFF10B981) else Color(0xFFA855F7)
                    val targetGradient = if (isDark) {
                        if (isTargetAchieved) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFF3B0764).copy(alpha = 0.35f)
                    } else {
                        if (isTargetAchieved) Color(0xFFD1FAE5) else Color(0xFFF3E8FF)
                    }

                    ActivityStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Adjust,
                        title = "Target",
                        value = "${todayStudiedMinutes} / ${effectiveTarget}m",
                        subtitle = targetSubtitle,
                        accentColor = targetColor,
                        gradientStart = targetGradient,
                        isDark = isDark,
                        testTag = "focus_stat_target",
                        onClick = onOpenTasks
                    )
                }
            }
        }
    }
}

/**
 * Clean adaptive semantic activity metric card.
 * In Dark Mode: dark base + subtle tinted surface + tinted icon container + small accent text.
 * In Light Mode: crisp white base + subtle pastel tint + dark bold value text.
 */
@Composable
private fun ActivityStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    gradientStart: Color,
    isDark: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0E1424) else Color.White),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(listOf(gradientStart, Color(0xFF0E1424)))
                    } else {
                        Brush.verticalGradient(listOf(gradientStart.copy(alpha = 0.5f), Color.White))
                    }
                )
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
