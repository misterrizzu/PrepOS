package com.example.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.isAppDarkTheme
import com.example.ui.theme.ProgressColor
import com.example.ui.components.AnimatedProgressBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextDisabled
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

/**
 * Ultra-compact Subject Progress section for PrepOS Home Screen
 * Displays strictly the last three subjects worked upon or studied.
 */
@Composable
fun HomeSubjectProgress(
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    onOpenSubject: (subjectId: String, subjectName: String) -> Unit,
    onViewAllSubjects: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()

    // Sort to show the last 3 subjects worked upon or studied
    val displaySubjects = androidx.compose.runtime.remember(subjects, chapters) {
        subjects.sortedWith(
            compareByDescending<SubjectEntity> { sub ->
                val subChaps = chapters.filter { it.subjectId == sub.id }
                val lastMod = subChaps.maxOfOrNull { it.lastModified } ?: 0L
                val hasProgress = subChaps.any { it.readingProgress > 0.02f }
                // Boost subjects with active reading progress and recent activity
                if (hasProgress) lastMod + 1_000_000_000_000L else maxOf(lastMod, sub.createdAt)
            }
        ).take(3)
    }

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
                    text = "RECENT SUBJECTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    ),
                    color = if (isDark) Color(0xFF64748B) else LightTextSecondary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Last 3 worked upon",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.sp
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                    )
                }
            }
            if (subjects.size > 3) {
                Text(
                    text = "View all (${subjects.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .clickable(onClick = onViewAllSubjects)
                        .testTag("home_subject_progress_view_all")
                )
            }
        }

        val averageProgress = remember(displaySubjects, chapters) {
            if (displaySubjects.isEmpty()) 0f
            else {
                val pcts = displaySubjects.map { subject ->
                    val subjectChapters = chapters.filter { it.subjectId == subject.id }
                    if (subjectChapters.isNotEmpty()) {
                        val allDone = subjectChapters.all { it.readingProgress >= 0.98f }
                        if (allDone) 100f
                        else {
                            val avg = subjectChapters.map { if (it.readingProgress >= 0.98f) 1.0f else it.readingProgress }.average().toFloat()
                            (avg * 100f).coerceIn(0f, 100f)
                        }
                    } else 0f
                }
                pcts.average().toFloat()
            }
        }

        val cardFogBrush = ProgressColor.rememberAnimatedFogBrush(
            percent = averageProgress,
            isDark = isDark,
            customAlphaMultiplier = 0.75f
        )
        val cardBorderColor = if (displaySubjects.isNotEmpty()) {
            ProgressColor.border(averageProgress, alpha = if (isDark) 0.35f else 0.25f)
        } else {
            if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else LightBorder
        }

        // Ultra Compact Card Container with Animated Fog Tint
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    cardBorderColor,
                    RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardFogBrush)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                if (displaySubjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No subjects added yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF64748B) else LightTextMuted
                        )
                    }
                } else {
                    displaySubjects.forEachIndexed { index, subject ->
                        val subjectChapters = chapters.filter { it.subjectId == subject.id }
                        val (progressPercent, progressFraction) = if (subjectChapters.isNotEmpty()) {
                            val allDone = subjectChapters.all { it.readingProgress >= 0.98f }
                            if (allDone) {
                                100 to 1.0f
                            } else {
                                val avg = subjectChapters.map { if (it.readingProgress >= 0.98f) 1.0f else it.readingProgress }.average().toFloat()
                                val pct = if (avg >= 0.985f) 100 else kotlin.math.round(avg * 100).toInt().coerceIn(0, 100)
                                pct to avg.coerceIn(0f, 1f)
                            }
                        } else {
                            0 to 0f
                        }

                        SubjectProgressCompactRow(
                            subject = subject,
                            chapterCount = subjectChapters.size,
                            progressPercent = progressPercent,
                            progressFraction = progressFraction,
                            isDark = isDark,
                            onClick = { onOpenSubject(subject.id, subject.name) }
                        )

                        if (index < displaySubjects.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .height(0.5.dp)
                                    .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else LightBorder)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectProgressCompactRow(
    subject: SubjectEntity,
    chapterCount: Int,
    progressPercent: Int,
    progressFraction: Float,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        label = "subject_progress"
    )

    val progressColor = ProgressColor.forProgressSmart(progressPercent.toFloat())
    val iconVector = getSubjectIcon(subject.iconName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("subject_row_${subject.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Subject Icon Badge styled with progress color
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(progressColor.copy(alpha = if (isDark) 0.15f else 0.12f))
                .border(0.8.dp, progressColor.copy(alpha = if (isDark) 0.4f else 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = subject.name,
                tint = progressColor,
                modifier = Modifier.size(15.dp)
            )
        }

        // Title and Progress Bar
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = if (isDark) Color.White else LightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = progressColor
                )
            }

            // Compact Progress Track
            AnimatedProgressBar(
                percent = progressPercent.toFloat(),
                height = 4.dp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isDark) Color(0xFF475569) else LightTextDisabled,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun getSubjectIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "science", "chemistry", "physics" -> Icons.Default.Science
        "math", "mathematics", "calculate" -> Icons.Default.Calculate
        "history", "geography", "civics", "polity" -> Icons.Default.Public
        "cs", "coding", "computer", "code" -> Icons.Default.Code
        "psychology", "reasoning", "aptitude" -> Icons.Default.Psychology
        "electronics", "tech" -> Icons.Default.Memory
        else -> Icons.Default.MenuBook
    }
}

