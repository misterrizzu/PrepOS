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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.StudyTaskEntity
import com.example.model.ProgressionOverview
import com.example.ui.theme.isAppDarkTheme

/**
 * Comprehensive Tasks & Progression Modal.
 * 
 * Houses all options under a unified view:
 * - Tab 0: 📋 Tasks & Target (Daily checklist, target progress, add task, focus launch)
 * - Tab 1: 🏆 Rank Ladder (14-tier rank progression and unlock status)
 * - Tab 2: 🎖️ Achievements (Milestone badges, streak badges, test badges)
 * - Tab 3: 📜 XP Ledger (Verifiable chronological event history)
 */
@Composable
fun TasksDetailSheet(
    todayTasks: List<StudyTaskEntity>,
    completedTaskIds: Set<String>,
    todayStudiedMinutes: Int,
    todayTargetMinutes: Int,
    progression: ProgressionOverview,
    onToggleTaskComplete: (taskId: String, isDone: Boolean) -> Unit,
    onStartFocus: (StudyTaskEntity) -> Unit,
    onAddTask: () -> Unit,
    onOpenRoadmap: () -> Unit,
    onDismiss: () -> Unit,
    initialTab: Int = 0
) {
    val isDark = isAppDarkTheme()
    val rank = progression.rankProgress.rank
    val primaryColor = rank.getPrimaryColor()

    var selectedTab by remember { mutableStateOf(initialTab.coerceIn(0, 3)) }

    val tabTitles = listOf(
        "📋 Tasks",
        "🏆 Rank Ladder",
        "🎖️ Badges",
        "📜 XP Ledger"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .testTag("tasks_detail_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF0B1020) else Color(0xFFF8FAFC),
            border = BorderStroke(
                1.2.dp,
                if (isDark) primaryColor.copy(alpha = 0.40f) else Color(0xFFCBD5E1)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "☷",
                                fontSize = 16.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Progression & Tasks Hub",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 17.sp
                                )
                            )
                        }
                        Text(
                            text = "Focus Hub · Rank Tier: ${rank.title}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_tasks_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unified Navigation Tab Row (Tasks, Rank Ladder, Badges, XP Ledger)
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = primaryColor,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = primaryColor
                        )
                    },
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) primaryColor else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    )
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Switcher
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> TasksTabContent(
                            todayTasks = todayTasks,
                            completedTaskIds = completedTaskIds,
                            todayStudiedMinutes = todayStudiedMinutes,
                            todayTargetMinutes = todayTargetMinutes,
                            progression = progression,
                            primaryColor = primaryColor,
                            isDark = isDark,
                            onToggleTaskComplete = onToggleTaskComplete,
                            onStartFocus = onStartFocus,
                            onAddTask = onAddTask,
                            onDismiss = onDismiss
                        )
                        1 -> RankLadderTabContent(
                            currentXp = progression.totalXp,
                            isDark = isDark
                        )
                        2 -> AchievementsTabContent(
                            achievements = progression.achievements,
                            isDark = isDark
                        )
                        3 -> XpLedgerTabContent(
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
 * Tab 0 Content: Daily Study Target Progress & Task Checklist.
 */
@Composable
private fun TasksTabContent(
    todayTasks: List<StudyTaskEntity>,
    completedTaskIds: Set<String>,
    todayStudiedMinutes: Int,
    todayTargetMinutes: Int,
    progression: ProgressionOverview,
    primaryColor: Color,
    isDark: Boolean,
    onToggleTaskComplete: (taskId: String, isDone: Boolean) -> Unit,
    onStartFocus: (StudyTaskEntity) -> Unit,
    onAddTask: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Activity & Target Summary Card
        val effectiveTarget = todayTargetMinutes.coerceAtLeast(1)
        val targetFraction = (todayStudiedMinutes.toFloat() / effectiveTarget.toFloat()).coerceIn(0f, 1f)

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF131828) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Target",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        )
                    )
                    Text(
                        text = "$todayStudiedMinutes / ${effectiveTarget} min",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (todayStudiedMinutes >= effectiveTarget) Color(0xFF10B981) else primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val animatedTargetProgress by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = tween(600),
                    label = "target_progress"
                )

                LinearProgressIndicator(
                    progress = { animatedTargetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (todayStudiedMinutes >= effectiveTarget) Color(0xFF10B981) else primaryColor,
                    trackColor = if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Chips in Target Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip 1: Completion %
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF0F1424) else Color(0xFFF1F5F9)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(targetFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 12.5.sp
                                )
                            )
                            Text(
                                text = "Goal Progress",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF64748B),
                                    fontSize = 9.5.sp
                                )
                            )
                        }
                    }

                    // Chip 2: Focus Minutes Left
                    val remainingMins = (effectiveTarget - todayStudiedMinutes).coerceAtLeast(0)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF0F1424) else Color(0xFFF1F5F9)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (remainingMins == 0) "Met! 🎯" else "${remainingMins}m",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingMins == 0) Color(0xFF10B981) else (if (isDark) Color.White else Color(0xFF0F172A)),
                                    fontSize = 12.5.sp
                                )
                            )
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF64748B),
                                    fontSize = 9.5.sp
                                )
                            )
                        }
                    }

                    // Chip 3: Today's XP
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF0F1424) else Color(0xFFF1F5F9)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "+${progression.todayTotalXp} XP",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    fontSize = 12.5.sp
                                )
                            )
                            Text(
                                text = "Earned Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF64748B),
                                    fontSize = 9.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tasks Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YOUR TASKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )

            val completedCount = todayTasks.count { completedTaskIds.contains(it.id) }
            Text(
                text = "$completedCount / ${todayTasks.size} Completed",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Tasks List
        if (todayTasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF101524) else Color(0xFFF1F5F9),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks planned for today yet",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Task' below to organize your day",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tasks_detail_checklist"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todayTasks, key = { it.id }) { task ->
                    val isDone = completedTaskIds.contains(task.id)
                    TaskItemRow(
                        task = task,
                        isDone = isDone,
                        primaryColor = primaryColor,
                        isDark = isDark,
                        onToggle = { done -> onToggleTaskComplete(task.id, done) },
                        onFocus = {
                            onDismiss()
                            onStartFocus(task)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Action: + Add Task
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clickable {
                    onDismiss()
                    onAddTask()
                },
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF131828) else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Task",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

/**
 * Task item row inside the dialog checklist.
 */
@Composable
private fun TaskItemRow(
    task: StudyTaskEntity,
    isDone: Boolean,
    primaryColor: Color,
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    onFocus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = when {
            isDone -> if (isDark) Color(0xFF0D1220) else Color(0xFFF8FAFC)
            else -> if (isDark) Color(0xFF121727) else Color.White
        },
        border = BorderStroke(
            1.dp,
            when {
                isDone -> if (isDark) Color(0xFF1A2234) else Color(0xFFE2E8F0)
                else -> if (isDark) Color(0xFF1E283E) else Color(0xFFE2E8F0)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isDone) primaryColor else (if (isDark) Color(0xFF1A2234) else Color(0xFFE2E8F0))
                    )
                    .clickable { onToggle(!isDone) },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle(!isDone) }
            ) {
                Text(
                    text = task.taskTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                        color = when {
                            isDone -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            else -> if (isDark) Color.White else Color(0xFF0F172A)
                        },
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (task.subjectName.isNotBlank()) {
                        Text(
                            text = task.subjectName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        )
                    }

                    Text(
                        text = "${task.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Focus button (if task not done)
            if (!isDone) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.40f)),
                    modifier = Modifier.clickable { onFocus() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Focus",
                            tint = primaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Focus",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
