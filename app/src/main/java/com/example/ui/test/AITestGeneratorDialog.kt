package com.example.ui.test

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity
import com.example.model.QuestionItem
import com.example.ui.theme.*
import com.example.viewmodel.PrepOSViewModel

/**
 * Dialog for generating a customized 10 or 20 question test using AI.
 * Supports choosing from existing chapters or entering any custom topic,
 * and saving the generated test to the library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITestGeneratorDialog(
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    initialChapter: ChapterEntity? = null,
    initialSubject: SubjectEntity? = null,
    onDismiss: () -> Unit,
    onStartTest: (
        title: String,
        questions: List<QuestionItem>,
        timeLimitSeconds: Int,
        subjectId: String?,
        chapterId: String?
    ) -> Unit,
    viewModel: PrepOSViewModel
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.run { (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f }

    // Mode: "CUSTOM_TOPIC" or "FROM_CHAPTERS"
    var mode by remember {
        mutableStateOf(if (initialChapter != null) "FROM_CHAPTERS" else "CUSTOM_TOPIC")
    }

    var customTopicText by remember {
        mutableStateOf(initialChapter?.title ?: "")
    }

    var selectedSubjectId by remember {
        mutableStateOf(initialChapter?.subjectId ?: initialSubject?.id ?: subjects.firstOrNull()?.id)
    }

    val availableChaptersForSubject = remember(selectedSubjectId, chapters) {
        if (selectedSubjectId == null) chapters else chapters.filter { it.subjectId == selectedSubjectId }
    }

    var selectedChapterId by remember {
        mutableStateOf(
            initialChapter?.id ?: availableChaptersForSubject.firstOrNull()?.id
        )
    }

    // Keep chapter selection in sync with subject
    LaunchedEffect(selectedSubjectId) {
        if (availableChaptersForSubject.none { it.id == selectedChapterId }) {
            selectedChapterId = availableChaptersForSubject.firstOrNull()?.id
        }
    }

    // Question Count: 10 or 20
    var questionCount by remember { mutableIntStateOf(10) }

    // Save Options
    var saveToLibrary by remember { mutableStateOf(true) }
    var createAsNewChapter by remember { mutableStateOf(mode == "CUSTOM_TOPIC") }

    // Generation states
    var isGenerating by remember { mutableStateOf(false) }
    var generationStatus by remember { mutableStateOf("Connecting to AI Question Engine...") }
    var generatedResult by remember { mutableStateOf<Pair<List<QuestionItem>, String?>?>(null) }

    val quickTopics = remember {
        listOf(
            "Indian Constitution & Fundamental Rights",
            "Modern Indian History & INA",
            "General Science & Physics",
            "Indian Economy & Budgeting",
            "Cell Biology & Genetics",
            "Quantitative Aptitude & Percentages",
            "Logical Reasoning & Syllogism",
            "Geography & Climate Systems"
        )
    }

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LightBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Test Generator",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF7C3AED).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "10 / 20 Qs",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "Create exam-grade MCQs & save to your library",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF1E293B) else LightBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // If questions have been successfully generated, show the preview & save state
                    if (generatedResult != null) {
                        val (questions, savedTitle) = generatedResult!!
                        GeneratedSuccessCard(
                            questions = questions,
                            savedChapterTitle = savedTitle,
                            isDark = isDark,
                            onStartNow = {
                                val targetTopic = if (mode == "CUSTOM_TOPIC") customTopicText.trim() else (chapters.find { it.id == selectedChapterId }?.title ?: "AI Test")
                                onStartTest(
                                    "AI: $targetTopic",
                                    questions,
                                    questionCount * 60,
                                    selectedSubjectId,
                                    selectedChapterId
                                )
                                onDismiss()
                            }
                        )
                    } else if (isGenerating) {
                        // Loading Animation Box
                        GeneratingIndicatorBox(
                            statusText = generationStatus,
                            targetCount = questionCount,
                            isDark = isDark
                        )
                    } else {
                        // --- 1. Mode Selection: Custom Topic vs From Chapters ---
                        Text(
                            text = "1. CHOOSE TEST SOURCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.5.sp
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = mode == "CUSTOM_TOPIC",
                                onClick = {
                                    mode = "CUSTOM_TOPIC"
                                    createAsNewChapter = true
                                },
                                label = { Text("Custom Topic", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = mode == "FROM_CHAPTERS",
                                onClick = {
                                    mode = "FROM_CHAPTERS"
                                    createAsNewChapter = false
                                },
                                label = { Text("From My Chapters", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // --- 2. Input Based on Selected Mode ---
                        if (mode == "CUSTOM_TOPIC") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = customTopicText,
                                    onValueChange = { customTopicText = it },
                                    label = { Text("Topic Name or Focus Area") },
                                    placeholder = { Text("e.g., Indian Constitution Writs, Thermodynamics...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Text(
                                    text = "Quick Topics:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                        fontSize = 10.sp
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quickTopics.forEach { topic ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else LightBorder),
                                            modifier = Modifier.clickable { customTopicText = topic }
                                        ) {
                                            Text(
                                                text = topic,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.5.sp,
                                                    color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // "FROM_CHAPTERS" Selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Select Subject:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    subjects.forEach { sub ->
                                        FilterChip(
                                            selected = selectedSubjectId == sub.id,
                                            onClick = { selectedSubjectId = sub.id },
                                            label = { Text(sub.name, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Text(
                                    text = "Select Target Chapter:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                                if (availableChaptersForSubject.isEmpty()) {
                                    Text(
                                        text = "No chapters found in this subject. Choose 'Custom Topic' instead.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                                            fontSize = 11.sp
                                        )
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        availableChaptersForSubject.take(8).forEach { chap ->
                                            val isSelected = selectedChapterId == chap.id
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedChapterId = chap.id },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) {
                                                    Color(0xFF7C3AED).copy(alpha = if (isDark) 0.25f else 0.12f)
                                                } else {
                                                    if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF7C3AED) else if (isDark) Color(0xFF334155) else LightBorder
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color(0xFF7C3AED) else Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = chap.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isDark) Color.White else LightTextPrimary
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- 3. Question Count Selection: 10 or 20 Questions ---
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "2. SELECT QUESTION COUNT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.5.sp
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuestionCountCard(
                                count = 10,
                                durationLabel = "10 Mins",
                                subtitle = "Quick Recall Drill",
                                isSelected = questionCount == 10,
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                                onClick = { questionCount = 10 }
                            )

                            QuestionCountCard(
                                count = 20,
                                durationLabel = "20 Mins",
                                subtitle = "Full Practice Test",
                                isSelected = questionCount == 20,
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                                onClick = { questionCount = 20 }
                            )
                        }

                        // --- 4. Save Test Options ---
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3. SAVE TEST IN LIBRARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.5.sp
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LightBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = saveToLibrary,
                                        onCheckedChange = { saveToLibrary = it },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Save generated test to chapter library",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = if (isDark) Color.White else LightTextPrimary
                                            )
                                        )
                                        Text(
                                            text = "Enables practicing this test again anytime from the Test Hub",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                            )
                                        )
                                    }
                                }

                                if (saveToLibrary && mode == "CUSTOM_TOPIC") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = createAsNewChapter,
                                            onCheckedChange = { createAsNewChapter = it },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Create as a new Chapter in selected subject",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                if (generatedResult == null && !isGenerating) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val targetTopic = if (mode == "CUSTOM_TOPIC") customTopicText.trim() else (chapters.find { it.id == selectedChapterId }?.title ?: "")
                                if (targetTopic.isBlank()) {
                                    Toast.makeText(context, "Please enter a topic or select a chapter", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                isGenerating = true
                                viewModel.generateAiTest(
                                    topicOrTitle = targetTopic,
                                    questionCount = questionCount,
                                    targetChapterId = if (saveToLibrary && !createAsNewChapter) selectedChapterId else null,
                                    createAsNewChapter = saveToLibrary && createAsNewChapter,
                                    subjectIdForNewChapter = selectedSubjectId,
                                    onProgress = { generationStatus = it },
                                    onSuccess = { qList, savedTitle ->
                                        isGenerating = false
                                        generatedResult = Pair(qList, savedTitle)
                                        Toast.makeText(context, "✓ Generated and saved ${qList.size} questions!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        isGenerating = false
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Chapter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val targetTopic = if (mode == "CUSTOM_TOPIC") customTopicText.trim() else (chapters.find { it.id == selectedChapterId }?.title ?: "")
                                if (targetTopic.isBlank()) {
                                    Toast.makeText(context, "Please enter a topic or select a chapter", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGenerating = true
                                viewModel.generateAiTest(
                                    topicOrTitle = targetTopic,
                                    questionCount = questionCount,
                                    targetChapterId = if (saveToLibrary && !createAsNewChapter) selectedChapterId else null,
                                    createAsNewChapter = saveToLibrary && createAsNewChapter,
                                    subjectIdForNewChapter = selectedSubjectId,
                                    onProgress = { generationStatus = it },
                                    onSuccess = { qList, savedTitle ->
                                        isGenerating = false
                                        Toast.makeText(context, "✓ Test Ready! Starting test...", Toast.LENGTH_SHORT).show()
                                        onStartTest(
                                            "AI: $targetTopic",
                                            qList,
                                            questionCount * 60,
                                            selectedSubjectId,
                                            selectedChapterId
                                        )
                                        onDismiss()
                                    },
                                    onError = { err ->
                                        isGenerating = false
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Test 🚀", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCountCard(
    count: Int,
    durationLabel: String,
    subtitle: String,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            Color(0xFF7C3AED).copy(alpha = if (isDark) 0.25f else 0.12f)
        } else {
            if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
        },
        border = BorderStroke(
            1.5.dp,
            if (isSelected) Color(0xFF7C3AED) else if (isDark) Color(0xFF334155) else LightBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count Questions",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) Color(0xFF7C3AED) else (if (isDark) Color.White else LightTextPrimary)
                    )
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)
            ) {
                Text(
                    text = durationLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GeneratingIndicatorBox(
    statusText: String,
    targetCount: Int,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "generating")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFFFAF5FF)
        ),
        border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C3AED).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF7C3AED),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Crafting $targetCount MCQs with AI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF4C1D95)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFC084FC) else Color(0xFF6B21A8)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF7C3AED),
                trackColor = Color(0xFF7C3AED).copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun GeneratedSuccessCard(
    questions: List<QuestionItem>,
    savedChapterTitle: String?,
    isDark: Boolean,
    onStartNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFA7F3D0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "✓ ${questions.size} Questions Generated!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF065F46),
                            fontSize = 15.sp
                        )
                    )
                    if (!savedChapterTitle.isNullOrBlank()) {
                        Text(
                            text = "Saved to: $savedChapterTitle",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Preview sample question
            val sample = questions.firstOrNull()
            if (sample != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFD1FAE5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Sample Question (Q1):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sample.questionText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = if (isDark) Color.White else Color(0xFF064E3B)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Correct Answer: ${sample.options.getOrNull(sample.correctOptionIndex) ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Button(
                onClick = onStartNow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF059669) else Color(0xFF059669)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start This AI Test Now 🚀", fontWeight = FontWeight.Bold)
            }
        }
    }
}
