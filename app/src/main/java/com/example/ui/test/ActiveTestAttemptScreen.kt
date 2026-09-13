package com.example.ui.test

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.isAppDarkTheme
import androidx.compose.foundation.layout.Arrangement
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.components.FigureQuestionCard
import com.example.ui.components.RichMathText
import com.example.ui.components.extractFigureBlock
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ActiveTestSessionEntity
import com.example.data.entity.TestAttemptEntity
import com.example.model.QuestionItem
import com.example.ui.components.ShareQuestionSheet
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppDarkBorder
import com.example.ui.theme.AppDarkSurface
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActiveTestAttemptScreen(
    testTitle: String,
    testType: String,
    subjectId: String? = null,
    subjectName: String = "General Studies",
    chapterId: String? = null,
    chapterTitle: String = "",
    questions: List<QuestionItem>,
    totalTimeLimitSeconds: Int,
    initialRemainingSeconds: Int? = null,
    viewModel: PrepOSViewModel,
    onFinishTest: (attemptId: String) -> Unit,
    onCancelTest: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val activeQuestions = remember(questions) { mutableStateListOf<QuestionItem>().apply { addAll(questions) } }

    if (activeQuestions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF0B1120) else LightBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No questions available for this test", color = if (isDark) Color.White else LightTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCancelTest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else GoldAccent
                    )
                ) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var remainingSeconds by remember { mutableIntStateOf(initialRemainingSeconds ?: totalTimeLimitSeconds) }
    // User selected option index (0..3)
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    // Flagged questions indices
    val flaggedIndices = remember { mutableStateListOf<Int>() }

    var showPaletteSheet by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteQuestionDialog by remember { mutableStateOf(false) }

    var questionToShare by remember { mutableStateOf<QuestionItem?>(null) }
    var sharedQuestionIds by remember { mutableStateOf(setOf<String>()) }

    val coroutineScope = rememberCoroutineScope()

    // Track active test practice & problem-solving study time (every second counts)
    DisposableEffect(Unit) {
        val timerJob = coroutineScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000L)
                viewModel.recordActiveStudySeconds(1, subjectName = subjectName)
            }
        }
        onDispose {
            timerJob.cancel()
            viewModel.flushActiveStudySeconds()
        }
    }

    // Intercept back button to confirm exit
    BackHandler {
        showExitConfirmDialog = true
    }

    // Countdown Timer Loop & Autosave
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1

            // Save active session to Room every 5 seconds
            if (remainingSeconds % 5 == 0 && activeQuestions.isNotEmpty()) {
                val questionsJson = TestQuestionProvider.questionsToJson(activeQuestions.toList())
                viewModel.saveActiveTestSession(
                    ActiveTestSessionEntity(
                        testTitle = testTitle,
                        testType = testType,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        chapterId = chapterId,
                        chapterTitle = chapterTitle,
                        currentQuestionIndex = currentIndex,
                        remainingTimeSeconds = remainingSeconds,
                        totalTimeLimitSeconds = totalTimeLimitSeconds,
                        questionsJson = questionsJson,
                        startedAt = startTime,
                        lastSavedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // Auto submit when time runs out
        if (remainingSeconds <= 0 && activeQuestions.isNotEmpty()) {
            submitTest(
                testTitle = testTitle,
                testType = testType,
                subjectId = subjectId,
                subjectName = subjectName,
                chapterId = chapterId,
                chapterTitle = chapterTitle,
                questions = activeQuestions.toList(),
                userAnswers = userAnswers,
                totalTimeLimitSeconds = totalTimeLimitSeconds,
                remainingSeconds = 0,
                viewModel = viewModel,
                onFinishTest = onFinishTest
            )
        }
    }

    val currentQuestion = activeQuestions.getOrNull(currentIndex) ?: activeQuestions[0]
    val selectedOptionIndex = userAnswers[currentIndex]
    val isRevealed = selectedOptionIndex != null
    val isCurrentFlagged = flaggedIndices.contains(currentIndex)
    val isCurrentShared = currentQuestion.id in sharedQuestionIds || currentQuestion.questionText in sharedQuestionIds

    Scaffold(
        containerColor = if (isDark) AppDarkBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = testTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Question ${currentIndex + 1} of ${activeQuestions.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showExitConfirmDialog = true },
                        modifier = Modifier.testTag("test_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Test",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                        )
                    }
                },
                actions = {
                    // Isolated Timer Badge (Prevents screen-wide recomposition)
                    TestTimerBadge(remainingSeconds = remainingSeconds)

                    Spacer(modifier = Modifier.width(6.dp))

                    // Question Palette Trigger Button
                    IconButton(
                        onClick = { showPaletteSheet = true },
                        modifier = Modifier.testTag("open_palette_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Question Palette",
                            tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark
                        )
                    }

                    // Submit Test Button
                    Button(
                        onClick = { showSubmitConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("top_submit_test_button")
                    ) {
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF0B1120) else LightSurface
                )
            )
        },
        bottomBar = {
            // Distraction-free Bottom Action Navigation Bar
            Surface(
                color = if (isDark) Color(0xFF0F172A) else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else GoldAccentBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    OutlinedButton(
                        onClick = {
                            if (currentIndex > 0) currentIndex -= 1
                        },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LightBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                        ),
                        modifier = Modifier.testTag("prev_question_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    // Flag / Bookmark Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrentFlagged) {
                            if (isDark) Color(0xFFF59E0B).copy(alpha = 0.2f) else GoldAccentSoft
                        } else {
                            if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrentFlagged) {
                                if (isDark) Color(0xFFF59E0B) else GoldAccent
                            } else {
                                if (isDark) Color(0xFF334155) else LightBorder
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (isCurrentFlagged) {
                                    flaggedIndices.remove(currentIndex)
                                } else {
                                    flaggedIndices.add(currentIndex)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCurrentFlagged) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Flag for review",
                                tint = if (isCurrentFlagged) (if (isDark) Color(0xFFF59E0B) else GoldAccentDark) else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCurrentFlagged) "Flagged" else "Flag",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isCurrentFlagged) (if (isDark) Color(0xFFF59E0B) else GoldAccentDark) else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Next or Finish Button
                    if (currentIndex < activeQuestions.size - 1) {
                        Button(
                            onClick = { currentIndex += 1 },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF6366F1) else GoldAccent
                            ),
                            modifier = Modifier.testTag("next_question_button")
                        ) {
                            Text("Next", color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { showSubmitConfirmDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.testTag("bottom_submit_button")
                        ) {
                            Text("Finish", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Question Header info: Difficulty, Tags, Share & Delete action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val diffColor = when (currentQuestion.difficulty.uppercase()) {
                        "EASY" -> Color(0xFF10B981)
                        "HARD" -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = diffColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, diffColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = currentQuestion.difficulty.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = diffColor,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (currentQuestion.examSource.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
                        ) {
                            Text(
                                text = currentQuestion.examSource,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (isCurrentFlagged) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.18f) else GoldAccentSoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFF59E0B) else GoldAccentDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Flagged",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFFF59E0B) else GoldAccentDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Question Action Buttons: Share & Delete (Unified across all tests)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            questionToShare = currentQuestion
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("share_question_btn")
                    ) {
                        Icon(
                            imageVector = if (isCurrentShared) Icons.Default.CheckCircle else Icons.Default.Share,
                            contentDescription = if (isCurrentShared) "Question Shared" else "Share Question",
                            tint = if (isCurrentShared) Color(0xFF10B981) else (if (isDark) Color(0xFFA5B4FC) else GoldAccentDark),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { showDeleteQuestionDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_question_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Question",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val (figure, cleanQuestionStem) = remember(currentQuestion.questionText) {
                extractFigureBlock(currentQuestion.questionText)
            }

            // Figure Display if present
            if (figure != null) {
                FigureQuestionCard(
                    figure = figure,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Question Text Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF131D33) else LightSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF1E293B) else GoldAccentBorderSubtle
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    RichMathText(
                        text = "Q${currentIndex + 1}. ${cleanQuestionStem.ifBlank { currentQuestion.questionText }}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 24.sp
                        ),
                        color = if (isDark) Color.White else LightTextPrimary,
                        isDark = isDark
                    )

                    if (currentQuestion.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentQuestion.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else GoldAccentSoft
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF818CF8) else GoldAccentDark,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Options List (A, B, C, D) with Instant Correct / Wrong Feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRevealed) "Evaluation & Result:" else "Choose the correct option:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRevealed) (if (isDark) Color(0xFFA5B4FC) else GoldAccentDark) else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                    )
                )

                if (isRevealed) {
                    val isCorrectSelection = selectedOptionIndex == currentQuestion.correctOptionIndex
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCorrectSelection) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isCorrectSelection) "✓ Correct (+1.0)" else "✗ Incorrect (0.0)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrectSelection) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val optionPrefixes = listOf("A", "B", "C", "D", "E", "F")
            currentQuestion.options.forEachIndexed { optIndex, optionText ->
                val isSelected = selectedOptionIndex == optIndex
                val isCorrect = optIndex == currentQuestion.correctOptionIndex
                val prefix = optionPrefixes.getOrElse(optIndex) { "${optIndex + 1}" }

                // Dynamic styling based on instant feedback
                val optionBgColor = when {
                    !isRevealed -> if (isSelected) (if (isDark) Color(0xFF1E1B4B) else GoldAccentSoft) else (if (isDark) Color(0xFF131D33) else LightSurface)
                    isCorrect -> Color(0xFF10B981).copy(alpha = if (isDark) 0.20f else 0.12f)
                    isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.20f else 0.12f)
                    else -> if (isDark) Color(0xFF131D33).copy(alpha = 0.5f) else LightSurfaceSecondary.copy(alpha = 0.6f)
                }

                val optionBorderColor = when {
                    !isRevealed -> if (isSelected) (if (isDark) Color(0xFF6366F1) else GoldAccent) else (if (isDark) Color(0xFF1E293B) else GoldAccentBorderSubtle)
                    isCorrect -> Color(0xFF10B981)
                    isSelected && !isCorrect -> Color(0xFFEF4444)
                    else -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else LightBorder
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isRevealed) {
                            userAnswers[currentIndex] = optIndex
                        }
                        .testTag("option_card_$optIndex"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = optionBgColor),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isRevealed && (isCorrect || isSelected)) 1.8.dp else 1.2.dp,
                        optionBorderColor
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Option Badge: Letter or Check/Close Icon
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isRevealed && isCorrect -> Color(0xFF10B981)
                                        isRevealed && isSelected && !isCorrect -> Color(0xFFEF4444)
                                        isSelected -> if (isDark) Color(0xFF6366F1) else GoldAccent
                                        else -> if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRevealed && isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Correct",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isRevealed && isSelected && !isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Wrong",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = prefix,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            RichMathText(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected || (isRevealed && isCorrect)) FontWeight.SemiBold else FontWeight.Normal,
                                    lineHeight = 20.sp
                                ),
                                color = when {
                                    isRevealed && isCorrect -> if (isDark) Color.White else Color(0xFF065F46)
                                    isRevealed && isSelected && !isCorrect -> if (isDark) Color.White else Color(0xFF991B1B)
                                    isRevealed -> if (isDark) Color(0xFFCBD5E1).copy(alpha = 0.7f) else LightTextSecondary
                                    isSelected -> if (isDark) Color.White else LightTextPrimary
                                    else -> if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                                },
                                isDark = isDark
                            )
                        }

                        // Visual tag for correct answer or choice
                        if (isRevealed && isCorrect) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "✓ Correct",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        fontSize = 10.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isRevealed && isSelected && !isCorrect) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "✗ Your Choice",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF87171),
                                        fontSize = 10.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Instant Explanation & Solution Card (revealed immediately upon option selection)
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else LightSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFBBF24) else GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Instant Explanation",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFCD34D) else GoldAccentDark
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                val isSelectionCorrect = selectedOptionIndex == currentQuestion.correctOptionIndex
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelectionCorrect) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isSelectionCorrect) "100% Accuracy" else "Review Key Point",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelectionCorrect) Color(0xFF34D399) else Color(0xFFF87171),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Correct Answer highlight banner
                            val correctPrefix = optionPrefixes.getOrElse(currentQuestion.correctOptionIndex) { "A" }
                            val correctText = currentQuestion.options.getOrElse(currentQuestion.correctOptionIndex) { "" }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Correct Answer: ",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                        )
                                    )
                                    Text(
                                        text = "($correctPrefix) $correctText",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color.White else LightTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val explanationText = if (currentQuestion.explanation.isNotBlank()) {
                                currentQuestion.explanation
                            } else {
                                "Option ($correctPrefix) is correct as it satisfies the fundamental concept tested in this question. Review related concepts in your chapter notes."
                            }

                            RichMathText(
                                text = explanationText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 20.sp,
                                    fontSize = 13.sp
                                ),
                                color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                isDark = isDark
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { questionToShare = currentQuestion }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Share Question Card",
                                        color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Question Palette Bottom Sheet
    if (showPaletteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaletteSheet = false },
            containerColor = if (isDark) Color(0xFF0F172A) else LightSurface,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp),
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF334155) else LightBorder
                ) {}
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Palette",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary
                        )
                    )

                    IconButton(onClick = { showPaletteSheet = false }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                        )
                    }
                }

                // Legend row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    PaletteLegendItem(color = Color(0xFF10B981), label = "Answered (${userAnswers.size})")
                    PaletteLegendItem(color = if (isDark) Color(0xFFF59E0B) else GoldAccentDark, label = "Flagged (${flaggedIndices.size})")
                    PaletteLegendItem(color = if (isDark) Color(0xFF334155) else LightSurfaceSecondary, label = "Unvisited (${activeQuestions.size - userAnswers.size})")
                }

                HorizontalDivider(color = if (isDark) Color(0xFF1E293B) else LightBorder)

                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(activeQuestions) { qIdx, _ ->
                        val isAnswered = userAnswers.containsKey(qIdx)
                        val isFlagged = flaggedIndices.contains(qIdx)
                        val isCurrent = currentIndex == qIdx

                        val btnColor = when {
                            isFlagged -> if (isDark) Color(0xFFF59E0B) else GoldAccent
                            isAnswered -> Color(0xFF10B981)
                            else -> if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
                        }

                        val textColor = when {
                            isFlagged || isAnswered -> Color.White
                            else -> if (isDark) Color.White else LightTextPrimary
                        }

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    currentIndex = qIdx
                                    showPaletteSheet = false
                                },
                            color = btnColor,
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, if (isDark) Color.White else GoldAccentDark) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${qIdx + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showPaletteSheet = false
                        showSubmitConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Test Now", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal: 4:3 Graphic Poster & Rich Caption Share Sheet
    questionToShare?.let { q ->
        ShareQuestionSheet(
            item = q,
            examName = testTitle,
            chapterTitle = testTitle,
            onDismiss = { questionToShare = null },
            onQuestionShared = { sharedQ ->
                sharedQuestionIds = sharedQuestionIds + sharedQ.id + sharedQ.questionText
            }
        )
    }

    // Dialog: Delete Question Confirmation
    if (showDeleteQuestionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteQuestionDialog = false },
            containerColor = if (isDark) Color(0xFF131D33) else LightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Question?", color = if (isDark) Color.White else LightTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this question from your current test session? The test question count will be adjusted.",
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteQuestionDialog = false
                        val removedIdx = currentIndex
                        userAnswers.remove(removedIdx)
                        flaggedIndices.remove(removedIdx)
                        activeQuestions.removeAt(removedIdx)

                        // Remap subsequent user answers and flags
                        val updatedAnswers = mutableMapOf<Int, Int>()
                        userAnswers.forEach { (k, v) ->
                            when {
                                k < removedIdx -> updatedAnswers[k] = v
                                k > removedIdx -> updatedAnswers[k - 1] = v
                            }
                        }
                        userAnswers.clear()
                        userAnswers.putAll(updatedAnswers)

                        val updatedFlags = flaggedIndices.mapNotNull { flagIdx ->
                            when {
                                flagIdx < removedIdx -> flagIdx
                                flagIdx > removedIdx -> flagIdx - 1
                                else -> null
                            }
                        }
                        flaggedIndices.clear()
                        flaggedIndices.addAll(updatedFlags)

                        if (activeQuestions.isEmpty()) {
                            onCancelTest()
                        } else if (currentIndex >= activeQuestions.size) {
                            currentIndex = (activeQuestions.size - 1).coerceAtLeast(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteQuestionDialog = false }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                }
            }
        )
    }

    // Dialog: Submit Confirmation
    if (showSubmitConfirmDialog) {
        val answeredCount = userAnswers.size
        val unansweredCount = activeQuestions.size - answeredCount
        val flaggedCount = flaggedIndices.size

        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            containerColor = if (isDark) Color(0xFF131D33) else LightSurface,
            title = {
                Text("Ready to Submit?", color = if (isDark) Color.White else LightTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Review your test progress before submitting:",
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryStatBox(title = "Answered", count = "$answeredCount", color = Color(0xFF10B981))
                        SummaryStatBox(title = "Unanswered", count = "$unansweredCount", color = Color(0xFFEF4444))
                        SummaryStatBox(title = "Flagged", count = "$flaggedCount", color = if (isDark) Color(0xFFF59E0B) else GoldAccentDark)
                    }

                    if (unansweredCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚠️ You still have $unansweredCount unanswered questions.",
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmDialog = false
                        submitTest(
                            testTitle = testTitle,
                            testType = testType,
                            subjectId = subjectId,
                            subjectName = subjectName,
                            chapterId = chapterId,
                            chapterTitle = chapterTitle,
                            questions = activeQuestions.toList(),
                            userAnswers = userAnswers,
                            totalTimeLimitSeconds = totalTimeLimitSeconds,
                            remainingSeconds = remainingSeconds,
                            viewModel = viewModel,
                            onFinishTest = onFinishTest
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Yes, Submit", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmDialog = false }) {
                    Text("Continue Test", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                }
            }
        )
    }

    // Dialog: Exit Confirmation
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            containerColor = if (isDark) Color(0xFF131D33) else LightSurface,
            title = {
                Text("Exit Test?", color = if (isDark) Color.White else LightTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Your progress has been auto-saved locally. You can resume this test anytime from Test Hub.",
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onCancelTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Exit", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Keep Practicing", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PaletteLegendItem(color: Color, label: String) {
    val isDark = isAppDarkTheme()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun SummaryStatBox(title: String, count: String, color: Color) {
    val isDark = isAppDarkTheme()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = if (isDark) 0.15f else 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

private fun submitTest(
    testTitle: String,
    testType: String,
    subjectId: String?,
    subjectName: String,
    chapterId: String?,
    chapterTitle: String,
    questions: List<QuestionItem>,
    userAnswers: Map<Int, Int>,
    totalTimeLimitSeconds: Int,
    remainingSeconds: Int,
    viewModel: PrepOSViewModel,
    onFinishTest: (attemptId: String) -> Unit
) {
    var correctCount = 0
    var wrongCount = 0
    var unansweredCount = 0

    questions.forEachIndexed { index, q ->
        val selected = userAnswers[index]
        if (selected == null) {
            unansweredCount += 1
        } else if (selected == q.correctOptionIndex) {
            correctCount += 1
        } else {
            wrongCount += 1
        }
    }

    val total = questions.size
    val accuracy = if (total > 0) (correctCount.toFloat() / total) * 100f else 0f
    val scorePercentage = accuracy.toInt()
    val timeTakenSeconds = (totalTimeLimitSeconds - remainingSeconds).coerceAtLeast(0)

    val attemptId = "attempt_${UUID.randomUUID().toString().take(8)}"
    val questionsWithAnswers = questions.mapIndexed { index, q ->
        q.copy(userSelectedOptionIndex = userAnswers[index])
    }
    val questionsJson = TestQuestionProvider.questionsToJson(questionsWithAnswers)

    val attempt = TestAttemptEntity(
        id = attemptId,
        testTitle = testTitle,
        testType = testType,
        subjectId = subjectId,
        subjectName = subjectName,
        chapterId = chapterId,
        chapterTitle = chapterTitle,
        totalQuestions = total,
        correctAnswers = correctCount,
        wrongAnswers = wrongCount,
        unanswered = unansweredCount,
        scorePercentage = scorePercentage,
        accuracy = accuracy,
        timeTakenSeconds = timeTakenSeconds,
        totalTimeLimitSeconds = totalTimeLimitSeconds,
        questionsJson = questionsJson,
        startedAt = System.currentTimeMillis() - (timeTakenSeconds * 1000L),
        completedAt = System.currentTimeMillis()
    )

    // Save attempt atomically, update chapter stats, and clear active test session
    viewModel.finalizeTestAttempt(
        attempt = attempt,
        chapterId = chapterId,
        attemptedCount = total,
        correctCount = correctCount
    )

    onFinishTest(attemptId)
}

@Composable
private fun TestTimerBadge(remainingSeconds: Int) {
    val isDark = isAppDarkTheme()
    val timerMinutes = remainingSeconds / 60
    val timerSecs = remainingSeconds % 60
    val isLowTime = remainingSeconds < 120

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isLowTime) {
            Color(0xFFEF4444).copy(alpha = if (isDark) 0.2f else 0.12f)
        } else {
            if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLowTime) Color(0xFFEF4444) else (if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else GoldAccentBorderSubtle)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = if (isLowTime) Color(0xFFEF4444) else (if (isDark) Color(0xFFA5B4FC) else GoldAccentDark),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%02d:%02d", timerMinutes, timerSecs),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isLowTime) Color(0xFFEF4444) else (if (isDark) Color.White else LightTextPrimary)
                )
            )
        }
    }
}
