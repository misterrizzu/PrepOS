package com.example.ui.test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.TestAttemptEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

data class TestAchievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean,
    val progressText: String
)

@Composable
fun TestAchievementsDialog(
    testCount: Int,
    bestScore: Int?,
    avgAccuracy: Float?,
    onDismiss: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val accuracy = avgAccuracy ?: 0f
    val best = bestScore ?: 0

    val achievements = remember(testCount, best, accuracy) {
        listOf(
            TestAchievement(
                id = "first_test",
                title = "First Step",
                description = "Complete your very first practice test in PrepOS",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF10B981),
                isUnlocked = testCount >= 1,
                progressText = if (testCount >= 1) "Unlocked" else "0 / 1 Completed"
            ),
            TestAchievement(
                id = "test_5",
                title = "Consistent Tester",
                description = "Complete 5 mock and practice tests",
                icon = Icons.Default.Quiz,
                color = Color(0xFF3B82F6),
                isUnlocked = testCount >= 5,
                progressText = "$testCount / 5 Completed"
            ),
            TestAchievement(
                id = "test_15",
                title = "Test Master",
                description = "Complete 15 full tests across subjects",
                icon = Icons.Default.EmojiEvents,
                color = Color(0xFFF59E0B),
                isUnlocked = testCount >= 15,
                progressText = "$testCount / 15 Completed"
            ),
            TestAchievement(
                id = "high_accuracy",
                title = "Precision Sniper",
                description = "Achieve an overall average accuracy of 80%+",
                icon = Icons.Default.Bolt,
                color = Color(0xFF8B5CF6),
                isUnlocked = testCount >= 3 && accuracy >= 80f,
                progressText = if (testCount >= 3 && accuracy >= 80f) "Unlocked" else "Current: ${accuracy.toInt()}% / 80%"
            ),
            TestAchievement(
                id = "perfectionist",
                title = "Century Score",
                description = "Score 100% in any chapter or rapid quiz",
                icon = Icons.Default.AutoAwesome,
                color = Color(0xFFEC4899),
                isUnlocked = best >= 100,
                progressText = if (best >= 100) "Unlocked (100%)" else "Best: $best% / 100%"
            ),
            TestAchievement(
                id = "deep_thinker",
                title = "Diagnostic Explorer",
                description = "Review test analysis and AI explanations",
                icon = Icons.Default.Psychology,
                color = Color(0xFF06B6D4),
                isUnlocked = testCount >= 2,
                progressText = if (testCount >= 2) "Unlocked" else "$testCount / 2 Reviewed"
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.2.dp,
                    if (isDark) Color(0xFF6366F1).copy(alpha = 0.4f) else LightBorder,
                    RoundedCornerShape(24.dp)
                ),
            color = if (isDark) Color(0xFF0F172A) else LightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = if (isDark) 0.18f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Test Achievements",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                            Text(
                                text = "${achievements.count { it.isUnlocked }} of ${achievements.size} Unlocked",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(achievements, key = { it.id }) { ach ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ach.isUnlocked) {
                                    if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
                                } else {
                                    if (isDark) Color(0xFF131D33).copy(alpha = 0.6f) else LightBackground
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (ach.isUnlocked) ach.color.copy(alpha = 0.4f) else (if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else LightBorder)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (ach.isUnlocked) ach.color.copy(alpha = if (isDark) 0.2f else 0.12f)
                                            else (if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else LightBorder)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ach.icon,
                                        contentDescription = null,
                                        tint = if (ach.isUnlocked) ach.color else (if (isDark) Color(0xFF64748B) else LightTextMuted),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ach.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (ach.isUnlocked) (if (isDark) Color.White else LightTextPrimary) else (if (isDark) Color(0xFF94A3B8) else LightTextMuted)
                                        )
                                    )
                                    Text(
                                        text = ach.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (ach.isUnlocked) Color(0xFF10B981).copy(alpha = if (isDark) 0.15f else 0.12f) else (if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else LightSurface)
                                ) {
                                    Text(
                                        text = ach.progressText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (ach.isUnlocked) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFF94A3B8) else LightTextMuted),
                                            fontSize = 10.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomTestConfigDialog(
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit,
    onStartTest: (subjectId: String?, chapterId: String?, count: Int, durationMinutes: Int) -> Unit,
    onOpenAITest: (() -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var selectedCount by remember { mutableIntStateOf(10) }
    var selectedDurationMinutes by remember { mutableIntStateOf(15) }

    val filteredChapters = remember(selectedSubjectId, chapters) {
        if (selectedSubjectId == null) chapters else chapters.filter { it.subjectId == selectedSubjectId }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.2.dp,
                    if (isDark) Color(0xFF6366F1).copy(alpha = 0.4f) else LightBorder,
                    RoundedCornerShape(20.dp)
                ),
            color = if (isDark) Color(0xFF0F172A) else LightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.2f) else BrandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Configure Custom Test",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                        )
                    }
                }

                if (onOpenAITest != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenAITest()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF1E1035) else Color(0xFFFAF5FF),
                        border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Want AI to build a 10 or 20 Q test on any topic?",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFC084FC) else Color(0xFF6B21A8)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Try AI →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subject Selection
                Text(
                    text = "Select Subject (Optional)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = {
                            selectedSubjectId = null
                            selectedChapterId = null
                        },
                        label = { Text("All Subjects", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary,
                            labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                        ),
                        border = BorderStroke(1.dp, if (selectedSubjectId == null) BrandPrimary else if (isDark) Color(0xFF334155) else LightBorder)
                    )
                    subjects.forEach { sub ->
                        FilterChip(
                            selected = selectedSubjectId == sub.id,
                            onClick = {
                                selectedSubjectId = sub.id
                                selectedChapterId = null
                            },
                            label = { Text(sub.name, maxLines = 1, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary,
                                labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                            ),
                            border = BorderStroke(1.dp, if (selectedSubjectId == sub.id) BrandPrimary else if (isDark) Color(0xFF334155) else LightBorder)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chapter Selection
                if (filteredChapters.isNotEmpty()) {
                    Text(
                        text = "Select Chapter (Optional)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedChapterId == null,
                            onClick = { selectedChapterId = null },
                            label = { Text("All Chapters", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary,
                                labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                            ),
                            border = BorderStroke(1.dp, if (selectedChapterId == null) BrandPrimary else if (isDark) Color(0xFF334155) else LightBorder)
                        )
                        filteredChapters.forEach { chap ->
                            FilterChip(
                                selected = selectedChapterId == chap.id,
                                onClick = { selectedChapterId = chap.id },
                                label = { Text(chap.title, maxLines = 1, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary,
                                    labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                ),
                                border = BorderStroke(1.dp, if (selectedChapterId == chap.id) BrandPrimary else if (isDark) Color(0xFF334155) else LightBorder)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Question Count Selection
                Text(
                    text = "Number of Questions",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 15, 20, 25).forEach { count ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCount = count
                                    selectedDurationMinutes = (count * 1.5).toInt().coerceAtLeast(5)
                                },
                            color = if (selectedCount == count) BrandPrimary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedCount == count) BrandPrimary else (if (isDark) Color(0xFF334155) else LightBorder)
                            )
                        ) {
                            Text(
                                text = "$count Qs",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCount == count) Color.White else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Time Limit Selection
                Text(
                    text = "Time Limit (Minutes)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 15, 20, 30).forEach { mins ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDurationMinutes = mins },
                            color = if (selectedDurationMinutes == mins) BrandPrimary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedDurationMinutes == mins) BrandPrimary else (if (isDark) Color(0xFF334155) else LightBorder)
                            )
                        ) {
                            Text(
                                text = "$mins m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedDurationMinutes == mins) Color.White else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onStartTest(selectedSubjectId, selectedChapterId, selectedCount, selectedDurationMinutes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_custom_test_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Start Test ($selectedCount Qs • $selectedDurationMinutes mins)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
