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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.screens.home.formatMinutesToDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top Rank & Identity Progression Card for Focus Hub.
 * 
 * Features:
 * - Consistent deep canvas (#0B1020) with dynamic rank accents (Gold, Diamond, Platinum, etc.)
 * - Rank crest, rank title, and streamlined XP (e.g. "5,473 XP · +120 today")
 * - Next rank tracker with percentage indicator
 * - Three semantic activity cards (Consistency, Activity, Goal) with dedicated subtle tints
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
    onTestStreak: ((Int) -> Unit)? = null
) {
    val currentRank = progression.rankProgress.rank
    val primaryColor = currentRank.getPrimaryColor()
    val secondaryColor = currentRank.getSecondaryColor()

    val nextRank = progression.rankProgress.nextRank
    val animatedProgress by animateFloatAsState(
        targetValue = progression.rankProgress.progressPercent,
        animationSpec = tween(durationMillis = 800),
        label = "rank_progress"
    )

    // Base consistent deep canvas
    val cardBackground = Color(0xFF0B1020)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("master_rank_identity_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    primaryColor.copy(alpha = 0.60f),
                    secondaryColor.copy(alpha = 0.35f),
                    primaryColor.copy(alpha = 0.18f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 1. Top Row: [Rank Emblem]  +  [Rank Title & XP Line]  +  [☷ Tasks Button]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Crest Emblem (tap to view Roadmap & Badges)
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
                                color = Color.White,
                                fontSize = 13.5.sp
                            )
                        )
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

                Spacer(modifier = Modifier.width(8.dp))

                // Compact [☷ Tasks] Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.45f)),
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
                                color = Color.White,
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
                            color = Color(0xFFCBD5E1),
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
                    trackColor = Color(0xFF1E2638)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Three Semantic Activity Cards: Consistency (Streak) → Activity (Study Time) → Goal (Target)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Streak (Consistency)
                val streakStatus = if (todayStudiedMinutes > 0) "Active" else if (progression.streakCount > 0) "Needs Study" else "Start Today"
                ActivityStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Whatshot,
                    title = "Streak",
                    value = "${progression.streakCount} Days",
                    subtitle = streakStatus,
                    accentColor = Color(0xFFF59E0B),
                    gradientStart = Color(0xFF451A03).copy(alpha = 0.35f),
                    testTag = "focus_stat_streak",
                    onClick = {
                        // Quick toggle test streak if supported
                        onTestStreak?.let { test ->
                            val nextTest = if (progression.streakCount >= 7) 1 else progression.streakCount + 1
                            test(nextTest)
                        }
                    }
                )

                // Card 2: Study Time (Actual Activity)
                val formattedStudyTime = if (todayStudiedMinutes > 0) formatMinutesToDisplay(todayStudiedMinutes) else "0m"
                ActivityStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    title = "Study Time",
                    value = formattedStudyTime,
                    subtitle = "Today",
                    accentColor = Color(0xFF38BDF8),
                    gradientStart = Color(0xFF0C2A4A).copy(alpha = 0.35f),
                    testTag = "focus_stat_study_time",
                    onClick = onOpenTasks
                )

                // Card 3: Target (Goal Completion)
                val effectiveTarget = todayTargetMinutes.coerceAtLeast(1)
                val isTargetAchieved = todayStudiedMinutes >= effectiveTarget && todayStudiedMinutes > 0
                val targetSubtitle = if (isTargetAchieved) "Achieved 🎯" else "Today"
                val targetColor = if (isTargetAchieved) Color(0xFF10B981) else Color(0xFFA855F7)
                val targetGradient = if (isTargetAchieved) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFF3B0764).copy(alpha = 0.35f)

                ActivityStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Adjust,
                    title = "Target",
                    value = "${todayStudiedMinutes} / ${effectiveTarget}m",
                    subtitle = targetSubtitle,
                    accentColor = targetColor,
                    gradientStart = targetGradient,
                    testTag = "focus_stat_target",
                    onClick = onOpenTasks
                )
            }
        }
    }
}

/**
 * Clean semantic activity metric card.
 * Maintains dark base + subtle tinted surface + tinted icon container + small accent text.
 */
@Composable
private fun ActivityStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    gradientStart: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1424)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(gradientStart, Color(0xFF0E1424))
                    )
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
                            .background(accentColor.copy(alpha = 0.18f)),
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
                            color = Color(0xFF94A3B8),
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
                        color = Color.White,
                        fontSize = 14.5.sp
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
