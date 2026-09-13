package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.isAppDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.TaskCompletionEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryLight
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightBorderStrong
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSoft
import com.example.viewmodel.ActiveFocusSession

/**
 * Today's Plan & Schedule section for PrepOS Home Screen
 * Displays real daily target tasks with live countdown of remaining completion time and dynamic focus buttons.
 */
@Composable
fun HomeTodayPlanCard(
    tasks: List<StudyTaskEntity>,
    completions: List<TaskCompletionEntity>,
    todayKey: String,
    onToggleTask: (taskId: String, isCompleted: Boolean) -> Unit,
    onViewAllPlan: () -> Unit,
    onAddTask: () -> Unit,
    onStartTaskFocus: (task: StudyTaskEntity) -> Unit = {},
    activeFocusSession: ActiveFocusSession? = null,
    dailyTargetMinutes: Int = 45,
    todayFocusedMinutes: Int = 0,
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    onStopFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()

    val completedCount = tasks.count { task ->
        completions.any { it.taskId == task.id && it.dateKey == todayKey && it.isCompleted }
    }
    val totalCount = tasks.size

    val totalPlannedMinutesToday = if (tasks.isNotEmpty()) {
        tasks.sumOf { it.durationMinutes }
    } else {
        dailyTargetMinutes
    }

    val completedMinutesToday = maxOf(
        todayFocusedMinutes,
        tasks.filter { task ->
            completions.any { it.taskId == task.id && it.dateKey == todayKey && it.isCompleted }
        }.sumOf { it.durationMinutes }
    )


    val baseRemainingMinutes = maxOf(0, totalPlannedMinutesToday - completedMinutesToday)

    val isSessionActive = activeFocusSession?.isActive == true
    val sessionElapsedSec = if (isSessionActive && activeFocusSession?.isPaused == false) {
        if (activeFocusSession.isTimerMode) {
            (activeFocusSession.totalSeconds - activeFocusSession.remainingSeconds).coerceAtLeast(0)
        } else {
            activeFocusSession.elapsedSeconds
        }
    } else 0

    val liveCompletedTotalSec = (completedMinutesToday * 60) + sessionElapsedSec
    val livePlannedTotalSec = totalPlannedMinutesToday * 60

    val isGoalExceeded = liveCompletedTotalSec > livePlannedTotalSec
    val extraSecAbove = if (isGoalExceeded) liveCompletedTotalSec - livePlannedTotalSec else 0
    val remSecToGoal = if (!isGoalExceeded) (livePlannedTotalSec - liveCompletedTotalSec).coerceAtLeast(0) else 0

    val remMinutes = remSecToGoal / 60
    val remSeconds = remSecToGoal % 60

    val extraMinutes = extraSecAbove / 60
    val extraSeconds = extraSecAbove % 60

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TODAY'S SCHEDULE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    ),
                    color = if (isDark) Color(0xFF64748B) else LightTextSecondary
                )
                if (totalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF334155) else LightSurfaceSecondary)
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "$completedCount/$totalCount done",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = if (completedCount == totalCount && totalCount > 0) {
                                StatusSuccess
                            } else {
                                if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                            }
                        )
                    }
                }
            }

            Text(
                text = "Full Plan",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                modifier = Modifier
                    .clickable(onClick = onViewAllPlan)
                    .testTag("home_today_plan_view_all")
            )
        }

        // Plan Card Container
        val planCardBorder = if (isDark) {
            BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
        } else {
            BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.18f))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            border = planCardBorder,
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
                                listOf(Color(0xFF6366F1).copy(alpha = 0.04f), Color(0xFF0F172A))
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(Color(0xFF6366F1).copy(alpha = 0.05f), LightSurface)
                            )
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                // Live Countdown / Remaining / Over Target Banner
                if (totalCount > 0 || isSessionActive) {
                    val isPaused = isSessionActive && activeFocusSession?.isPaused == true
                    
                    val bannerBg = when {
                        isPaused -> if (isDark) Color(0xFF451A03).copy(alpha = 0.7f) else Color(0xFFFEF3C7)
                        isSessionActive -> if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.9f) else Color(0xFFEEF2FF)
                        isGoalExceeded -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.7f) else Color(0xFFECFDF5)
                        remSecToGoal == 0 -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.7f) else Color(0xFFECFDF5)
                        else -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color(0xFFF1F5F9)
                    }

                    val bannerBorder = when {
                        isPaused -> if (isDark) Color(0xFFD97706) else Color(0xFFFBBF24)
                        isSessionActive -> if (isDark) Color(0xFF6366F1) else Color(0xFFA5B4FC)
                        isGoalExceeded -> if (isDark) Color(0xFF059669) else Color(0xFF6EE7B7)
                        remSecToGoal == 0 -> if (isDark) Color(0xFF059669) else Color(0xFF6EE7B7)
                        else -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    }

                    val indicatorColor = when {
                        isPaused -> Color(0xFFF59E0B)
                        isSessionActive -> if (isDark) Color(0xFF818CF8) else BrandPrimary
                        isGoalExceeded -> Color(0xFF10B981)
                        remSecToGoal == 0 -> Color(0xFF10B981)
                        else -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bannerBg,
                        border = BorderStroke(1.dp, bannerBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .clip(CircleShape)
                                        .background(indicatorColor)
                                )
                                Text(
                                    text = when {
                                        isPaused -> "Focus Paused"
                                        isSessionActive -> "Focusing Now"
                                        isGoalExceeded -> "Goal Exceeded"
                                        remSecToGoal == 0 -> "Goal Reached"
                                        else -> "Remaining Time"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = when {
                                            isPaused -> if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                            isSessionActive -> if (isDark) Color(0xFFE0E7FF) else Color(0xFF3730A3)
                                            isGoalExceeded || remSecToGoal == 0 -> if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                            else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                        }
                                    )
                                )
                            }

                            // Right Value (Time Left / Above Goal)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.9f),
                                border = BorderStroke(0.6.dp, bannerBorder.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formatScheduleTimeDisplay(
                                            isGoalExceeded = isGoalExceeded,
                                            remSecToGoal = remSecToGoal,
                                            extraSecAbove = extraSecAbove,
                                            isSessionActive = isSessionActive
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            color = when {
                                                isPaused -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                                isSessionActive -> if (isDark) Color(0xFF38BDF8) else BrandPrimary
                                                isGoalExceeded || remSecToGoal == 0 -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                                else -> if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    EmptyScheduleState(isDark = isDark, onAddTask = onAddTask)
                } else {
                    val displayTasks = tasks.take(3)
                    displayTasks.forEachIndexed { index, task ->
                        val isCompleted = completions.any {
                            it.taskId == task.id && it.dateKey == todayKey && it.isCompleted
                        }

                        TaskAutoCheckRowItem(
                            task = task,
                            isCompleted = isCompleted,
                            isDark = isDark,
                            activeFocusSession = activeFocusSession,
                            onStartFocus = { onStartTaskFocus(task) },
                            onPauseFocus = onPauseFocus,
                            onResumeFocus = onResumeFocus,
                            onStopFocus = onStopFocus
                        )

                        if (index < displayTasks.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                                    .height(0.5.dp)
                                    .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else LightBorder)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun TaskAutoCheckRowItem(
    task: StudyTaskEntity,
    isCompleted: Boolean,
    isDark: Boolean,
    activeFocusSession: ActiveFocusSession? = null,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    onStopFocus: () -> Unit = {}
) {
    val isThisTaskActive = activeFocusSession?.isActive == true && activeFocusSession.associatedTaskId == task.id
    val isAnySessionActive = activeFocusSession?.isActive == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .testTag("task_row_${task.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status Indicator Icon (Auto checked on time completion)
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF10B981).copy(alpha = 0.2f) else StatusSuccessSoft)
                    .border(1.dp, StatusSuccess, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed on time",
                    tint = StatusSuccess,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            if (isThisTaskActive) Color(0xFF311042) else Color(0xFF1E293B)
                        } else {
                            if (isThisTaskActive) BrandPrimaryLight else LightSurfaceSecondary
                        }
                    )
                    .border(
                        1.dp,
                        if (isDark) {
                            if (isThisTaskActive) Color(0xFFC084FC) else Color(0xFF475569)
                        } else {
                            if (isThisTaskActive) BrandPrimary else LightBorderStrong
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Pending Timer Completion",
                    tint = if (isDark) {
                        if (isThisTaskActive) Color(0xFFC084FC) else Color(0xFF94A3B8)
                    } else {
                        if (isThisTaskActive) BrandPrimary else LightTextSecondary
                    },
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Title & Subject/Time
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = task.taskTitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isCompleted) {
                    if (isDark) Color(0xFF64748B) else LightTextMuted
                } else {
                    if (isDark) Color.White else LightTextPrimary
                },
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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isDark) Color(0xFF38BDF8) else BrandPrimary
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF475569) else LightBorderStrong
                    )
                )
                Text(
                    text = "${task.durationMinutes}m duration",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                    )
                )
            }
        }

        // Right side: Completed Badge OR Dynamic Countdown Focus Button
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else StatusSuccessSoft)
                    .border(0.8.dp, StatusSuccess.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = StatusSuccess
                    )
                )
            }
        } else if (isThisTaskActive && activeFocusSession != null) {
            // DYNAMIC COUNTDOWN BUTTON for this active task
            val displayTime = if (activeFocusSession.isTimerMode) {
                val rem = activeFocusSession.remainingSeconds
                String.format("%02d:%02d", rem / 60, rem % 60)
            } else {
                val el = activeFocusSession.elapsedSeconds
                String.format("%02d:%02d", el / 60, el % 60)
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isDark) {
                    if (activeFocusSession.isPaused) Color(0xFF78350F) else Color(0xFF2E1065)
                } else {
                    if (activeFocusSession.isPaused) Color(0xFFFFF4E5) else BrandPrimaryLight
                },
                border = BorderStroke(
                    1.dp,
                    if (isDark) {
                        if (activeFocusSession.isPaused) Color(0xFFF59E0B) else Color(0xFFA855F7)
                    } else {
                        if (activeFocusSession.isPaused) Color(0xFFC9923E) else BrandPrimary
                    }
                ),
                modifier = Modifier
                    .height(28.dp)
                    .clickable {
                        if (activeFocusSession.isPaused) onResumeFocus() else onPauseFocus()
                    }
                    .testTag("task_focus_btn_${task.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (activeFocusSession.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = if (isDark) {
                            if (activeFocusSession.isPaused) Color(0xFFFBBF24) else Color(0xFFC084FC)
                        } else {
                            if (activeFocusSession.isPaused) Color(0xFFC9923E) else BrandPrimary
                        },
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "${if (activeFocusSession.isTimerMode) "⏳" else "⏱️"} $displayTime",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = if (isDark) {
                                if (activeFocusSession.isPaused) Color(0xFFFDE68A) else Color(0xFFE9D5FF)
                            } else {
                                if (activeFocusSession.isPaused) Color(0xFFC9923E) else BrandPrimary
                            }
                        )
                    )
                }
            }
        } else {
            Button(
                onClick = onStartFocus,
                modifier = Modifier
                    .height(28.dp)
                    .testTag("task_focus_btn_${task.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) {
                        if (isAnySessionActive) Color(0xFF334155) else Color(0xFF0284C7)
                    } else {
                        if (isAnySessionActive) LightSurfaceSecondary else BrandPrimary
                    },
                    contentColor = if (!isDark && isAnySessionActive) LightTextSecondary else Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (!isDark && isAnySessionActive) LightTextSecondary else Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Focus",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (!isDark && isAnySessionActive) LightTextSecondary else Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyScheduleState(
    isDark: Boolean,
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "No study tasks scheduled for today",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
        )

        OutlinedButton(
            onClick = onAddTask,
            modifier = Modifier
                .height(30.dp)
                .testTag("home_schedule_add_task_btn"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isDark) Color(0xFF38BDF8) else BrandPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF38BDF8).copy(alpha = 0.5f) else BrandPrimary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Add Study Target",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

private fun formatScheduleTimeDisplay(
    isGoalExceeded: Boolean,
    remSecToGoal: Int,
    extraSecAbove: Int,
    isSessionActive: Boolean
): String {
    return if (isGoalExceeded) {
        val extraMinutes = extraSecAbove / 60
        val extraSeconds = extraSecAbove % 60
        val timeStr = when {
            extraMinutes >= 60 -> {
                val h = extraMinutes / 60
                val m = extraMinutes % 60
                if (isSessionActive) "${h}h ${m}m ${String.format("%02d", extraSeconds)}s"
                else if (m > 0) "${h}h ${m}m" else "${h}h"
            }
            else -> {
                if (isSessionActive) "${extraMinutes}m ${String.format("%02d", extraSeconds)}s"
                else "${extraMinutes}m"
            }
        }
        "+$timeStr above"
    } else if (remSecToGoal == 0) {
        "Goal met (100%)"
    } else {
        val remMinutes = remSecToGoal / 60
        val remSeconds = remSecToGoal % 60
        val timeStr = when {
            remMinutes >= 60 -> {
                val h = remMinutes / 60
                val m = remMinutes % 60
                if (isSessionActive) "${h}h ${m}m ${String.format("%02d", remSeconds)}s"
                else if (m > 0) "${h}h ${m}m" else "${h}h"
            }
            else -> {
                if (isSessionActive) "${remMinutes}m ${String.format("%02d", remSeconds)}s"
                else "${remMinutes}m"
            }
        }
        "$timeStr left"
    }
}

