package com.example.ui.screens.progression

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AchievementCategory
import com.example.model.AchievementProgress
import com.example.model.ProgressionOverview
import com.example.model.Rank
import com.example.ui.theme.isAppDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified Progression Dashboard Section on Focus Hub.
 * Replaces disconnected Streak, Badge, and Rank views with a single,
 * coherent master progression hub.
 */
@Composable
fun UnifiedProgressionSection(
    progression: ProgressionOverview,
    modifier: Modifier = Modifier,
    onTestStreak: ((Int) -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    var showDetailModal by remember { mutableStateOf(false) }
    val currentRank = progression.rankProgress.rank
    val primaryColor = currentRank.getPrimaryColor()
    val secondaryColor = currentRank.getSecondaryColor()

    val animatedProgress by animateFloatAsState(
        targetValue = progression.rankProgress.progressPercent,
        animationSpec = tween(durationMillis = 800),
        label = "rank_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unified_progression_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🏆",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YOUR PROGRESSION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDetailModal = true }
                    .testTag("open_progression_roadmap_button"),
                color = primaryColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Roadmap & Badges",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Master Unified Progression Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDetailModal = true }
                .testTag("unified_progression_main_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0E1424) else Color(0xFFF8FAFC)
            ),
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        primaryColor.copy(alpha = 0.6f),
                        secondaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.15f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Row: Insignia + Rank Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Insignia Crest
                    RankInsigniaArtwork(
                        rank = currentRank,
                        modifier = Modifier.size(76.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentRank.title.uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 19.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )

                            // Tier Pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = primaryColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Tier ${currentRank.ordinal + 1}/14",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryColor,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "\"${currentRank.slogan}\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Big Bold XP Display
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${String.format(Locale.US, "%,d", progression.totalXp)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = primaryColor,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VERIFIED XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar toward next rank
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val next = progression.rankProgress.nextRank
                        if (next != null) {
                            val remaining = (next.minXp - progression.totalXp).coerceAtLeast(0L)
                            Text(
                                text = "${String.format(Locale.US, "%,d", remaining)} XP to ${next.title}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                    fontSize = 11.5.sp
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
                        } else {
                            Text(
                                text = "Supreme Rank Achieved (Grandmaster)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    fontSize = 11.5.sp
                                )
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = primaryColor,
                        trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trio Metrics Row: Streak (Consistency) + Achievements (Milestones) + Today's XP (Progress)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Streak Card (Consistency Indicator)
                    ProgressionQuickStatCard(
                        modifier = Modifier.weight(1f),
                        iconSymbol = "🔥",
                        title = "${progression.streakCount}d Streak",
                        subtitle = "Consistency",
                        accentColor = Color(0xFFF97316),
                        isDark = isDark
                    )

                    // 2. Achievements Card (Milestones Indicator)
                    ProgressionQuickStatCard(
                        modifier = Modifier.weight(1f),
                        iconSymbol = "🎖️",
                        title = "${progression.unlockedAchievementsCount}/${progression.totalAchievementsCount}",
                        subtitle = "Milestones",
                        accentColor = Color(0xFFA855F7),
                        isDark = isDark
                    )

                    // 3. Today's XP Card
                    ProgressionQuickStatCard(
                        modifier = Modifier.weight(1f),
                        iconSymbol = "⚡",
                        title = "+${progression.todayTotalXp} XP",
                        subtitle = "Cap ${progression.todayStudyMinutesXp}/${progression.maxDailyStudyXp}m",
                        accentColor = Color(0xFF10B981),
                        isDark = isDark
                    )
                }
            }
        }
    }

    // Interactive Progression Roadmap & Badges Modal
    if (showDetailModal) {
        ProgressionDetailsModal(
            progression = progression,
            onDismiss = { showDetailModal = false }
        )
    }
}

@Composable
fun ProgressionQuickStatCard(
    iconSymbol: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF131B2E) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = iconSymbol,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    fontSize = 9.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Custom Canvas Insignia Crest Artwork for Ranks.
 */
@Composable
fun RankInsigniaArtwork(
    rank: Rank,
    modifier: Modifier = Modifier
) {
    val primary = rank.getPrimaryColor()
    val secondary = rank.getSecondaryColor()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // Outer Soft Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.35f), Color.Transparent),
                    center = center,
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = center
            )

            // Shield Geometry
            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.88f, h * 0.20f)
                lineTo(w * 0.82f, h * 0.65f)
                lineTo(w * 0.50f, h * 0.94f)
                lineTo(w * 0.18f, h * 0.65f)
                lineTo(w * 0.12f, h * 0.20f)
                close()
            }

            // Outer Shield Fill (Dark Metallic with Gradient)
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                ),
                style = Fill
            )

            // Shield Stroke Border
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    listOf(primary, secondary)
                ),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner Accent Shield
            val innerPath = Path().apply {
                moveTo(w * 0.5f, h * 0.18f)
                lineTo(w * 0.78f, h * 0.28f)
                lineTo(w * 0.74f, h * 0.62f)
                lineTo(w * 0.50f, h * 0.86f)
                lineTo(w * 0.26f, h * 0.62f)
                lineTo(w * 0.22f, h * 0.28f)
                close()
            }

            drawPath(
                path = innerPath,
                brush = Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.25f), secondary.copy(alpha = 0.05f))
                ),
                style = Fill
            )

            drawPath(
                path = innerPath,
                color = primary.copy(alpha = 0.4f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Center Rank Symbol Emoji / Crest Center
        Text(
            text = rank.symbol,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Full Interactive Modal Dialog with 3 Tabs:
 * 1. 🏆 14-Tier Master Rank Ladder
 * 2. 🎖️ Achievements Showcase (with 7 category filters)
 * 3. 📜 Verified XP Ledger History
 */
@Composable
fun ProgressionDetailsModal(
    progression: ProgressionOverview,
    onDismiss: () -> Unit
) {
    val isDark = isAppDarkTheme()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🏆 Rank Ladder", "🎖️ Achievements", "📜 XP Ledger")
    val currentRank = progression.rankProgress.rank
    val primaryColor = currentRank.getPrimaryColor()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .testTag("progression_roadmap_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF0B1020) else Color(0xFFF8FAFC),
            border = BorderStroke(1.2.dp, if (isDark) primaryColor.copy(alpha = 0.45f) else Color(0xFFCBD5E1))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Rank Emblem and Total XP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RankInsigniaArtwork(
                            rank = currentRank,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Progression & Roadmap",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "${currentRank.title.uppercase()} • ${String.format(Locale.US, "%,d", progression.totalXp)} XP",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_progression_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                // Modern Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = if (isDark) Color(0xFF080C18) else Color(0xFFF1F5F9),
                    contentColor = primaryColor,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) primaryColor else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    when (selectedTab) {
                        0 -> RankLadderTabContent(
                            currentXp = progression.totalXp,
                            isDark = isDark
                        )
                        1 -> AchievementsTabContent(
                            achievements = progression.achievements,
                            isDark = isDark
                        )
                        2 -> XpLedgerTabContent(
                            events = progression.recentEvents,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: 14-Tier Rank Ladder (Novice to Grandmaster).
 */
@Composable
fun RankLadderTabContent(
    currentXp: Long,
    isDark: Boolean
) {
    val ranks = Rank.entries
    val currentRank = com.example.model.getRank(currentXp)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rank_ladder_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        items(ranks) { rank ->
            val isCurrent = rank == currentRank
            val isUnlocked = currentXp >= rank.minXp
            val primary = rank.getPrimaryColor()

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when {
                    isCurrent -> primary.copy(alpha = if (isDark) 0.20f else 0.12f)
                    isUnlocked -> if (isDark) Color(0xFF131A2C) else Color.White
                    else -> if (isDark) Color(0xFF0D121F) else Color(0xFFF1F5F9)
                },
                border = BorderStroke(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = when {
                        isCurrent -> primary
                        isUnlocked -> primary.copy(alpha = 0.35f)
                        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Insignia Artwork
                    RankInsigniaArtwork(
                        rank = rank,
                        modifier = Modifier.size(52.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = rank.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 15.sp
                                )
                            )

                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = primary
                                ) {
                                    Text(
                                        text = "CURRENT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = rank.slogan,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // XP Range
                        val rangeText = if (rank.maxXp == Long.MAX_VALUE) {
                            "${String.format(Locale.US, "%,d", rank.minXp)}+ XP"
                        } else {
                            "${String.format(Locale.US, "%,d", rank.minXp)} – ${String.format(Locale.US, "%,d", rank.maxXp)} XP"
                        }

                        Text(
                            text = rangeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = primary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Status Indicator Icon
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Current Tier",
                            tint = primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Unlocked",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Achievements Showcase (with 7 category filter chips).
 */
@Composable
fun AchievementsTabContent(
    achievements: List<AchievementProgress>,
    isDark: Boolean
) {
    var selectedCategory by remember { mutableStateOf(AchievementCategory.ALL) }

    val filteredAchievements = remember(selectedCategory, achievements) {
        if (selectedCategory == AchievementCategory.ALL) {
            achievements
        } else {
            achievements.filter { it.definition.category == selectedCategory }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AchievementCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) {
                        Color(0xFFA855F7)
                    } else {
                        if (isDark) Color(0xFF131B2E) else Color(0xFFE2E8F0)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedCategory = category }
                ) {
                    Text(
                        text = "${category.iconSymbol} ${category.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF334155),
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Achievements List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("achievements_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredAchievements) { item ->
                AchievementCardItem(item = item, isDark = isDark)
            }
        }
    }
}

@Composable
fun AchievementCardItem(
    item: AchievementProgress,
    isDark: Boolean
) {
    val def = item.definition
    val isUnlocked = item.isUnlocked
    val accentColor = Color(0xFFA855F7)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            isUnlocked -> if (isDark) Color(0xFF14192A) else Color.White
            else -> if (isDark) Color(0xFF0E1320) else Color(0xFFF1F5F9)
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isUnlocked -> accentColor.copy(alpha = 0.45f)
                else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Bubble
            Surface(
                shape = CircleShape,
                color = if (isUnlocked) accentColor.copy(alpha = 0.18f) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = def.iconSymbol,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = def.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 13.5.sp
                        )
                    )

                    if (def.xpReward > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "+${def.xpReward} XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = def.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { item.progressPercent },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isUnlocked) Color(0xFF10B981) else accentColor,
                        trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isUnlocked) "UNLOCKED" else "${item.currentValue}/${def.targetValue}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color(0xFF10B981) else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            fontSize = 9.5.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Tab 3: Verified XP Ledger Audit History.
 */
@Composable
fun XpLedgerTabContent(
    events: List<com.example.data.entity.ProgressXpEntity>,
    isDark: Boolean
) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📜", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No XP activity recorded yet",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Complete focused study or tests to record verified XP!",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                    )
                )
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("xp_ledger_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(events) { event ->
                val typeBadge = when (event.type) {
                    "STUDY_MINUTE" -> "📚"
                    "PRACTICE_ATTEMPT" -> "✏️"
                    "PRACTICE_CORRECT" -> "🎯"
                    "TEST_COMPLETED" -> "📝"
                    "TEST_ACCURACY_BONUS" -> "⭐"
                    "CHAPTER_COMPLETED" -> "📖"
                    "DAILY_TARGET_COMPLETED" -> "🎯"
                    "REVISION_SESSION" -> "🔄"
                    "STREAK_DAY" -> "🔥"
                    "STREAK_MILESTONE" -> "⚡"
                    "ACHIEVEMENT_UNLOCKED" -> "🎖️"
                    else -> "✨"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF13192A) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = typeBadge, fontSize = 18.sp)

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.note.ifBlank { event.type.replace("_", " ") },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = dateFormat.format(Date(event.createdAt)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "+${event.xp} XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
