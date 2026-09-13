package com.example.ui.screens.progression

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Locale

/**
 * Compact, high-craft Tasks Detail Modal.
 * Opens when tapping [☷ Tasks] on the master Rank Card.
 * 
 * Displays:
 * - Today's Target progress (e.g. 90 / 120 min)
 * - Focus Sessions logged & XP Earned Today
 * - Interactive task checklist with completion and focus launch
 * - Direct link to the Unified Progression Roadmap & Badges
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
    onDismiss: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val rank = progression.rankProgress.rank
    val primaryColor = rank.getPrimaryColor()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 24.dp)
                .testTag("tasks_detail_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF0B1020) else Color(0xFFF8FAFC),
            border = BorderStroke(1.2.dp, if (isDark) primaryColor.copy(alpha = 0.35f) else Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row
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
                                text = "Today's Tasks & Activity",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 17.sp
                                )
                            )
                        }
                        Text(
                            text = "Focus Hub Study Breakdown",
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

                Spacer(modifier = Modifier.height(12.dp))

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
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                )
                            )
                            Text(
                                text = "$todayStudiedMinutes / ${effectiveTarget} min",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = primaryColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { targetFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = primaryColor,
                            trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid (Focus Sessions + XP Earned Today)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sessions Count
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF0F1524) else Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⏱️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        val sessionEstimate = if (todayStudiedMinutes > 0) ((todayStudiedMinutes + 24) / 25) else 0
                                        Text(
                                            text = "$sessionEstimate Sessions",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = "Logged today",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF64748B),
                                                fontSize = 9.5.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // XP Earned Today
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF0F1524) else Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "+${progression.todayTotalXp} XP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = "Earned today",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF64748B),
                                                fontSize = 9.5.sp
                                            )
                                        )
                                    }
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primaryColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        onDismiss()
                                        onAddTask()
                                    }
                                ) {
                                    Text(
                                        text = "+ Add Task to Timetable",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = primaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(todayTasks, key = { it.id }) { task ->
                            val isDone = completedTaskIds.contains(task.id)
                            TaskItemRow(
                                task = task,
                                isDone = isDone,
                                isDark = isDark,
                                primaryColor = primaryColor,
                                onToggleDone = { onToggleTaskComplete(task.id, !isDone) },
                                onFocus = {
                                    onDismiss()
                                    onStartFocus(task)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Actions: + Add Task & Roadmap Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // + Add Task
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
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
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Task",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    // View Roadmap & Badges Button
                    Surface(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(40.dp)
                            .clickable {
                                onDismiss()
                                onOpenRoadmap()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🏆", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Roadmap & Badges →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemRow(
    task: StudyTaskEntity,
    isDone: Boolean,
    isDark: Boolean,
    primaryColor: Color,
    onToggleDone: () -> Unit,
    onFocus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isDone -> if (isDark) Color(0xFF0D1424).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
            else -> if (isDark) Color(0xFF131A2D) else Color.White
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isDone) Color(0xFF10B981).copy(alpha = 0.4f)
            else if (isDark) Color(0xFF1E2A42) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDone) Color(0xFF10B981)
                        else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                    )
                    .clickable { onToggleDone() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.taskTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (isDone) Color(0xFF64748B) else if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 13.sp,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.subjectName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${task.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Focus Action (if not done)
            if (!isDone) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onFocus() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Focus",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Focus",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
