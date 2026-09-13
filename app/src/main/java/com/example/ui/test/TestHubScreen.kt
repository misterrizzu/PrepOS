package com.example.ui.test

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.isAppDarkTheme
import com.example.ui.theme.ProgressColor
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ActiveTestSessionEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.TestAttemptEntity
import com.example.model.QuestionItem
import com.example.ui.components.PrepOSBottomNavBar
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppDarkBorder
import com.example.ui.theme.AppDarkSurface
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Test Hub Screen — Practice. Analyze. Improve.
 * Complete one-stop Testing and Diagnostic Engine for PrepOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestHubScreen(
    viewModel: PrepOSViewModel,
    initialChapterId: String? = null,
    onNavigateTab: (String) -> Unit,
    onOpenAskAI: (initialPrompt: String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isAppDarkTheme()

    // Data streams from Room via ViewModel
    val testAttempts by viewModel.testAttempts.collectAsState()
    val activeSession by viewModel.activeTestSession.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    // Active screen state inside Test module
    var runningTestParams by remember { mutableStateOf<ActiveTestLaunchParams?>(null) }
    var viewingResultsAttemptId by remember { mutableStateOf<String?>(null) }

    // Auto-launch chapter test if navigated from Subject or Chapter (loads all questions in chapter)
    var hasLaunchedInitialChapter by remember { mutableStateOf(false) }
    LaunchedEffect(initialChapterId, chapters) {
        if (!hasLaunchedInitialChapter && !initialChapterId.isNullOrBlank() && initialChapterId != "{chapterId}" && chapters.isNotEmpty()) {
            val targetChapter = chapters.find { it.id == initialChapterId }
            if (targetChapter != null) {
                val sub = subjects.find { it.id == targetChapter.subjectId }
                val questions = TestQuestionProvider.buildQuestionPool(
                    chapters = chapters,
                    chapterId = targetChapter.id,
                    count = 0, // 0 = Load full questions from this chapter!
                    recentAttempts = testAttempts
                )
                if (questions.isNotEmpty()) {
                    runningTestParams = ActiveTestLaunchParams(
                        testTitle = targetChapter.title.ifBlank { "Chapter Test" },
                        testType = "CHAPTER_TEST",
                        subjectId = sub?.id,
                        subjectName = sub?.name ?: "General Studies",
                        chapterId = targetChapter.id,
                        chapterTitle = targetChapter.title,
                        questions = questions,
                        totalTimeLimitSeconds = (questions.size * 60).coerceAtLeast(180)
                    )
                }
                hasLaunchedInitialChapter = true
            }
        }
    }

    // Intercept back presses when inside an active test or results screen
    BackHandler(enabled = runningTestParams != null || viewingResultsAttemptId != null) {
        if (runningTestParams != null) {
            runningTestParams = null
        } else if (viewingResultsAttemptId != null) {
            viewingResultsAttemptId = null
        }
    }

    // Dialog states
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showCustomConfigDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showNoMistakesDialog by remember { mutableStateOf(false) }
    var showImportQuestionsDialog by remember { mutableStateOf(false) }
    var importPreselectedChapterId by remember { mutableStateOf<String?>(null) }
    var attemptToDelete by remember { mutableStateOf<TestAttemptEntity?>(null) }
    var showAITestDialog by remember { mutableStateOf(false) }
    var aiTestPreselectedChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var aiTestPreselectedSubject by remember { mutableStateOf<SubjectEntity?>(null) }

    // Filter for Subject/Chapter Test Browser
    var selectedSubjectIdFilter by remember { mutableStateOf<String?>(null) }

    // Aggregate statistics
    val totalTests = testAttempts.size
    val avgScore = if (totalTests > 0) testAttempts.map { it.scorePercentage }.average().toInt() else 0
    val bestScore = if (totalTests > 0) testAttempts.maxOfOrNull { it.scorePercentage } ?: 0 else 0
    val avgAccuracy = if (totalTests > 0) {
        val totalAcc = testAttempts.sumOf { it.accuracy.toDouble() }
        (totalAcc / totalTests).toFloat()
    } else 0f
    val totalQuestionsAttempted = testAttempts.sumOf { it.totalQuestions }
    val totalCorrectQuestions = testAttempts.sumOf { it.correctAnswers }

    // If a test is currently active/running, render ActiveTestAttemptScreen
    if (runningTestParams != null) {
        val params = runningTestParams!!
        ActiveTestAttemptScreen(
            testTitle = params.testTitle,
            testType = params.testType,
            subjectId = params.subjectId,
            subjectName = params.subjectName,
            chapterId = params.chapterId,
            chapterTitle = params.chapterTitle,
            questions = params.questions,
            totalTimeLimitSeconds = params.totalTimeLimitSeconds,
            initialRemainingSeconds = params.initialRemainingSeconds,
            viewModel = viewModel,
            onFinishTest = { finishedAttemptId ->
                runningTestParams = null
                viewingResultsAttemptId = finishedAttemptId
            },
            onCancelTest = {
                runningTestParams = null
            }
        )
        return
    }

    // If viewing results of a test attempt, render TestResultsScreen
    if (viewingResultsAttemptId != null) {
        val attemptId = viewingResultsAttemptId!!
        TestResultsScreen(
            attemptId = attemptId,
            viewModel = viewModel,
            onBackToHub = {
                viewingResultsAttemptId = null
            },
            onRetest = { questions, title, testType ->
                viewingResultsAttemptId = null
                runningTestParams = ActiveTestLaunchParams(
                    testTitle = title,
                    testType = testType,
                    questions = questions,
                    totalTimeLimitSeconds = (questions.size * 75).coerceAtLeast(180)
                )
            },
            onOpenAskAI = { prompt ->
                viewingResultsAttemptId = null
                onOpenAskAI(prompt)
            }
        )
        return
    }

    // Main Test Hub Screen Layout
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("test_hub_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            PrepOSBottomNavBar(
                selectedTab = "practice",
                onSelectTab = { tab ->
                    if (tab != "practice") {
                        onNavigateTab(tab)
                    }
                },
                onOpenAskAI = {
                    onOpenAskAI("")
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HEADER SECTION
            item {
                TestHubHeader(
                    testCount = totalTests,
                    onOpenAchievements = { showAchievementsDialog = true }
                )
            }

            // 2. ACTIVE TEST RESUME BANNER (if unfinished session exists)
            if (activeSession != null) {
                item {
                    val session = activeSession!!
                    ActiveTestResumeCard(
                        session = session,
                        onResume = {
                            val parsedQuestions = TestQuestionProvider.parseQuestionsFromJson(session.questionsJson)
                            runningTestParams = ActiveTestLaunchParams(
                                testTitle = session.testTitle,
                                testType = session.testType,
                                subjectId = session.subjectId,
                                subjectName = session.subjectName,
                                chapterId = session.chapterId,
                                chapterTitle = session.chapterTitle,
                                questions = parsedQuestions,
                                totalTimeLimitSeconds = session.totalTimeLimitSeconds,
                                initialRemainingSeconds = session.remainingTimeSeconds
                            )
                        },
                        onDiscard = {
                            viewModel.clearActiveTestSession()
                            Toast.makeText(context, "In-progress test discarded", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 3. PROGRESS OVERVIEW SECTION
            item {
                ProgressOverviewSection(
                    totalTests = totalTests,
                    avgScore = avgScore,
                    bestScore = bestScore,
                    avgAccuracy = avgAccuracy,
                    totalQuestionsAttempted = totalQuestionsAttempted,
                    totalCorrect = totalCorrectQuestions,
                    onStartFirstTest = {
                        val questions = TestQuestionProvider.buildRapidFireQuiz(chapters)
                        runningTestParams = ActiveTestLaunchParams(
                            testTitle = "Rapid Fire Starter Quiz",
                            testType = "RAPID_FIRE",
                            questions = questions,
                            totalTimeLimitSeconds = 420
                        )
                    }
                )
            }

            // 4. QUICK START SECTION (4 Diagnostic Modes)
            item {
                val mistakeStats = remember(testAttempts) {
                    TestQuestionProvider.getMistakeStats(context, testAttempts)
                }
                QuickStartSection(
                    mistakeStats = mistakeStats,
                    onStartRapidFire = {
                        val questions = TestQuestionProvider.buildRapidFireQuiz(chapters, recentAttempts = testAttempts)
                        if (questions.isEmpty()) {
                            Toast.makeText(context, "Add chapters or notes first to generate questions!", Toast.LENGTH_SHORT).show()
                        } else {
                            runningTestParams = ActiveTestLaunchParams(
                                testTitle = "Rapid Fire Recall Quiz",
                                testType = "RAPID_FIRE",
                                questions = questions,
                                totalTimeLimitSeconds = 420
                            )
                        }
                    },
                    onStartMockTest = {
                        val questions = TestQuestionProvider.buildFullMockTest(chapters, recentAttempts = testAttempts)
                        if (questions.isEmpty()) {
                            Toast.makeText(context, "Add chapters or notes first to generate mock test!", Toast.LENGTH_SHORT).show()
                        } else {
                            runningTestParams = ActiveTestLaunchParams(
                                testTitle = "Full Syllabus Mock Test",
                                testType = "MOCK_TEST",
                                questions = questions,
                                totalTimeLimitSeconds = 1500
                            )
                        }
                    },
                    onStartWeakAreaDrill = {
                        val questions = TestQuestionProvider.buildWeakAreaTest(context, testAttempts, count = 10)
                        if (questions.isEmpty()) {
                            showNoMistakesDialog = true
                        } else {
                            runningTestParams = ActiveTestLaunchParams(
                                testTitle = "Targeted Weak Area Drill",
                                testType = "WEAK_AREA",
                                questions = questions,
                                totalTimeLimitSeconds = (questions.size * 60).coerceAtLeast(300)
                            )
                        }
                    },
                    onOpenCustomConfig = {
                        showCustomConfigDialog = true
                    },
                    onOpenAITest = {
                        aiTestPreselectedChapter = null
                        aiTestPreselectedSubject = null
                        showAITestDialog = true
                    }
                )
            }

            // 5. PRACTICE BY SUBJECT / CHAPTER BROWSER
            item {
                SubjectChapterPracticeSection(
                    subjects = subjects,
                    chapters = chapters,
                    selectedSubjectId = selectedSubjectIdFilter,
                    onSelectSubjectFilter = { selectedSubjectIdFilter = it },
                    onOpenAITest = { targetChapter ->
                        aiTestPreselectedChapter = targetChapter
                        aiTestPreselectedSubject = subjects.find { it.id == targetChapter?.subjectId }
                        showAITestDialog = true
                    },
                    onImportQuestions = { targetChapter ->
                        importPreselectedChapterId = targetChapter?.id
                        showImportQuestionsDialog = true
                    },
                    onStartChapterTest = { chapter ->
                        val sub = subjects.find { it.id == chapter.subjectId }
                        val questions = TestQuestionProvider.buildQuestionPool(
                            chapters = chapters,
                            chapterId = chapter.id,
                            count = 0, // Load all questions from this chapter
                            recentAttempts = testAttempts
                        )
                        runningTestParams = ActiveTestLaunchParams(
                            testTitle = chapter.title.ifBlank { "Chapter Test" },
                            testType = "CHAPTER_TEST",
                            subjectId = sub?.id,
                            subjectName = sub?.name ?: "General Studies",
                            chapterId = chapter.id,
                            chapterTitle = chapter.title,
                            questions = questions,
                            totalTimeLimitSeconds = (questions.size * 60).coerceAtLeast(180)
                        )
                    }
                )
            }

            // 6. SMART ANALYSIS CTA (AI Insights based on actual performance)
            item {
                SmartAnalysisCtaCard(
                    totalTests = totalTests,
                    avgScore = avgScore,
                    avgAccuracy = avgAccuracy,
                    onOpenAskAI = {
                        val prompt = if (totalTests > 0) {
                            "Analyze my test performance in PrepOS: I have completed $totalTests tests with an average score of $avgScore% and $avgAccuracy% accuracy. What are the best diagnostic strategies to push my score above 90%?"
                        } else {
                            "How can I use active recall and spaced testing to prepare effectively for my competitive exams?"
                        }
                        onOpenAskAI(prompt)
                    }
                )
            }

            // 7. TEST HISTORY SECTION
            item {
                TestHistoryHeader(
                    historyCount = testAttempts.size,
                    onClearAll = {
                        if (testAttempts.isNotEmpty()) {
                            showClearHistoryConfirm = true
                        }
                    }
                )
            }

            if (testAttempts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F172A) else LightSurface
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else LightBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.15f) else BrandPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Test Attempts Yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your completed tests, mistake breakdowns, and score analyses will appear here.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            } else {
                items(testAttempts, key = { it.id }) { attempt ->
                    TestHistoryItemCard(
                        attempt = attempt,
                        onClick = {
                            viewingResultsAttemptId = attempt.id
                        },
                        onDelete = {
                            attemptToDelete = attempt
                        }
                    )
                }
            }
        }
    }

    // =============================================================================================
    // DIALOGS & OVERLAYS
    // =============================================================================================

    // Achievements Dialog
    if (showAchievementsDialog) {
        TestAchievementsDialog(
            testCount = totalTests,
            bestScore = if (totalTests > 0) bestScore else null,
            avgAccuracy = if (totalTests > 0) avgAccuracy else null,
            onDismiss = { showAchievementsDialog = false }
        )
    }

    // Custom Test Config Dialog
    if (showCustomConfigDialog) {
        CustomTestConfigDialog(
            subjects = subjects,
            chapters = chapters,
            onDismiss = { showCustomConfigDialog = false },
            onOpenAITest = {
                showCustomConfigDialog = false
                aiTestPreselectedChapter = null
                aiTestPreselectedSubject = null
                showAITestDialog = true
            },
            onStartTest = { subjectId, chapterId, count, durationMinutes ->
                showCustomConfigDialog = false
                val selectedSub = subjects.find { it.id == subjectId }
                val selectedChap = chapters.find { it.id == chapterId }

                val pool = if (selectedChap != null) {
                    TestQuestionProvider.buildQuestionPool(chapters = chapters, chapterId = selectedChap.id, count = count)
                } else if (selectedSub != null) {
                    TestQuestionProvider.buildQuestionPool(chapters = chapters, subjectId = selectedSub.id, count = count)
                } else {
                    TestQuestionProvider.buildQuestionPool(chapters = chapters, count = count)
                }

                if (pool.isEmpty()) {
                    Toast.makeText(context, "No questions found for the selected configuration!", Toast.LENGTH_SHORT).show()
                } else {
                    val testTitle = when {
                        selectedChap != null -> "${selectedChap.title} Quiz"
                        selectedSub != null -> "${selectedSub.name} Custom Test"
                        else -> "Custom Mock Test"
                    }
                    runningTestParams = ActiveTestLaunchParams(
                        testTitle = testTitle,
                        testType = "CUSTOM_TEST",
                        subjectId = selectedSub?.id,
                        subjectName = selectedSub?.name ?: "General Studies",
                        chapterId = selectedChap?.id,
                        chapterTitle = selectedChap?.title ?: "",
                        questions = pool,
                        totalTimeLimitSeconds = durationMinutes * 60
                    )
                }
            }
        )
    }

    // AI Test Generator Dialog
    if (showAITestDialog) {
        AITestGeneratorDialog(
            subjects = subjects,
            chapters = chapters,
            initialChapter = aiTestPreselectedChapter,
            initialSubject = aiTestPreselectedSubject,
            onDismiss = {
                showAITestDialog = false
                aiTestPreselectedChapter = null
                aiTestPreselectedSubject = null
            },
            onStartTest = { title, questions, timeLimitSeconds, subjectId, chapterId ->
                showAITestDialog = false
                aiTestPreselectedChapter = null
                aiTestPreselectedSubject = null
                val selectedSub = subjects.find { it.id == subjectId }
                val selectedChap = chapters.find { it.id == chapterId }
                runningTestParams = ActiveTestLaunchParams(
                    testTitle = title,
                    testType = "AI_TEST",
                    subjectId = subjectId,
                    subjectName = selectedSub?.name ?: "AI Test",
                    chapterId = chapterId,
                    chapterTitle = selectedChap?.title ?: "",
                    questions = questions,
                    totalTimeLimitSeconds = timeLimitSeconds
                )
            },
            viewModel = viewModel
        )
    }

    // No Mistakes Recorded Dialog
    if (showNoMistakesDialog) {
        AlertDialog(
            onDismissRequest = { showNoMistakesDialog = false },
            containerColor = if (isDark) Color(0xFF131D33) else LightSurface,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "No Mistakes Recorded Yet",
                    color = if (isDark) Color.White else LightTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "You haven't answered any questions incorrectly in your tests yet!\n\nTake a Mock Test, Rapid Fire, or Chapter Quiz. Any question you answer wrong will automatically be gathered into your personalized Mistake Bank for zero-repeat targeted drills.",
                    color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showNoMistakesDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF6366F1) else BrandPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Clear All History Confirmation
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            containerColor = if (isDark) Color(0xFF131D33) else LightSurface,
            title = {
                Text(
                    "Clear All Test History?",
                    color = if (isDark) Color.White else LightTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will delete all $totalTests completed test attempt records and their mistake reviews. This action cannot be undone.",
                    color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTestAttempts()
                        showClearHistoryConfirm = false
                        Toast.makeText(context, "Test history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextMuted)
                }
            }
        )
    }

    // Delete Single Attempt Confirmation
    attemptToDelete?.let { att ->
        AlertDialog(
            onDismissRequest = { attemptToDelete = null },
            containerColor = if (isDark) Color(0xFF131D33) else LightSurface,
            title = {
                Text(
                    "Delete Test Record?",
                    color = if (isDark) Color.White else LightTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete '${att.testTitle}' (${att.scorePercentage}% score)?",
                    color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTestAttempt(att.id)
                        attemptToDelete = null
                        Toast.makeText(context, "Test record deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { attemptToDelete = null }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextMuted)
                }
            }
        )
    }

    // Import Questions / Offline Parser Dialog
    if (showImportQuestionsDialog) {
        ImportQuestionsDialog(
            subjects = subjects,
            chapters = chapters,
            preselectedChapterId = importPreselectedChapterId,
            viewModel = viewModel,
            onDismiss = {
                showImportQuestionsDialog = false
                importPreselectedChapterId = null
            }
        )
    }
}

// =================================================================================================
// SUB-COMPONENTS
// =================================================================================================

data class ActiveTestLaunchParams(
    val testTitle: String,
    val testType: String,
    val subjectId: String? = null,
    val subjectName: String = "General Studies",
    val chapterId: String? = null,
    val chapterTitle: String = "",
    val questions: List<QuestionItem>,
    val totalTimeLimitSeconds: Int,
    val initialRemainingSeconds: Int? = null
)

/**
 * 1. Header with Trophy Achievements trigger
 */
@Composable
fun TestHubHeader(
    testCount: Int,
    onOpenAchievements: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Test Hub",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 24.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF6366F1).copy(alpha = 0.2f) else BrandPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF818CF8).copy(alpha = 0.5f) else BrandPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "DIAGNOSTICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFC7D2FE) else BrandPrimary,
                            fontSize = 9.sp,
                            letterSpacing = 0.6.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "Practice. Analyze. Improve.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 12.sp
                )
            )
        }

        // Trophy / Achievement Button
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1E1B4B) else Color(0xFFFEF3C7),
            border = BorderStroke(1.2.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.7f) else Color(0xFFF59E0B).copy(alpha = 0.5f)),
            modifier = Modifier
                .clickable(onClick = onOpenAchievements)
                .testTag("test_hub_trophy_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Achievements",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (testCount > 0) "$testCount Badges" else "Trophies",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E),
                        fontSize = 11.5.sp
                    )
                )
            }
        }
    }
}

/**
 * 2. Active Test Resume Banner
 */
@Composable
fun ActiveTestResumeCard(
    session: ActiveTestSessionEntity,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_test_resume_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1B4B) else LightSurface),
        border = BorderStroke(1.2.dp, if (isDark) Color(0xFF8B5CF6) else BrandPrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.25f) else BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFC084FC) else BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IN-PROGRESS TEST",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    Text(
                        text = session.testTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val minutes = session.remainingTimeSeconds / 60
                    val seconds = session.remainingTimeSeconds % 60
                    Text(
                        text = "Time Left: ${String.format("%02d:%02d", minutes, seconds)} • ${session.subjectName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                ) {
                    Text("Discard", fontSize = 12.sp)
                }

                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1.6f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume Test", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * 3. Progress Overview Section (Streamed StateFlow with Zero Fake Statistics)
 */
@Composable
fun ProgressOverviewSection(
    totalTests: Int,
    avgScore: Int,
    bestScore: Int,
    avgAccuracy: Float,
    totalQuestionsAttempted: Int,
    totalCorrect: Int,
    onStartFirstTest: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PERFORMANCE OVERVIEW",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (totalTests == 0) {
            // Empty State Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else LightSurface),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LightBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF38BDF8).copy(alpha = 0.15f) else BrandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Measuring Your Mastery",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary,
                                    fontSize = 14.5.sp
                                )
                            )
                            Text(
                                text = "Take your first diagnostic test to generate real score curves & accuracy metrics.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onStartFirstTest,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Starter Quiz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Live Real Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Avg Score
                MetricOverviewCard(
                    title = "Avg Score",
                    value = "$avgScore%",
                    subtext = "Best: $bestScore%",
                    accentColor = if (avgScore >= 75) Color(0xFF10B981) else if (avgScore >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444),
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Accuracy
                MetricOverviewCard(
                    title = "Accuracy",
                    value = "${avgAccuracy.toInt()}%",
                    subtext = "$totalCorrect / $totalQuestionsAttempted Qs",
                    accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )

                // Metric 3: Tests Completed
                MetricOverviewCard(
                    title = "Tests Taken",
                    value = "$totalTests",
                    subtext = "Attempts",
                    accentColor = BrandPrimary,
                    icon = Icons.Default.Quiz,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricOverviewCard(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else LightSurface),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else LightBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else LightTextPrimary,
                    fontSize = 20.sp
                )
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFFCBD5E1) else LightTextMuted,
                    fontSize = 9.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 4. Quick Start Section with 4 Focused Modes
 */
@Composable
fun QuickStartSection(
    mistakeStats: Triple<Int, Int, Int> = Triple(0, 0, 1),
    onStartRapidFire: () -> Unit,
    onStartMockTest: () -> Unit,
    onStartWeakAreaDrill: () -> Unit,
    onOpenCustomConfig: () -> Unit,
    onOpenAITest: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK START MODES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFFC084FC) else BrandPrimary,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
            )
            Text(
                text = "Pre-Engineered",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hero: AI Test Generator Card (10 or 20 Questions on any topic or chapter)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAITest() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E1035) else Color(0xFFFAF5FF)
            ),
            border = BorderStroke(1.2.dp, if (isDark) Color(0xFF7C3AED).copy(alpha = 0.55f) else Color(0xFFD8B4FE))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 10.dp),
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

                Spacer(modifier = Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI Test Generator",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF4C1D95),
                                fontSize = 13.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF7C3AED).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "10 or 20 Qs",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Generate tests from your chapters or any custom topic",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid 2x2 of Launch Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Rapid Fire Quiz
                QuickModeCard(
                    title = "Rapid Fire",
                    subtitle = "10 Qs • 7 Mins",
                    description = "Fast recall speed drill",
                    badge = "SPEED",
                    badgeColor = Color(0xFFF59E0B),
                    gradientColors = listOf(Color(0xFF92400E), Color(0xFF1E1405)),
                    icon = Icons.Default.Bolt,
                    onClick = onStartRapidFire,
                    modifier = Modifier.weight(1f)
                )

                // 2. Full Mock Test
                QuickModeCard(
                    title = "Full Mock",
                    subtitle = "20 Qs • 25 Mins",
                    description = "Comprehensive exam test",
                    badge = "SIMULATION",
                    badgeColor = Color(0xFF3B82F6),
                    gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF081229)),
                    icon = Icons.Default.Quiz,
                    onClick = onStartMockTest,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val (totalMistakes, unseenCount, cycleNum) = mistakeStats
                val weakSubtitle = if (totalMistakes > 0) "$totalMistakes Missed Qs" else "10 Target Qs"
                val weakDesc = if (totalMistakes > 0) "$unseenCount unseen in cycle #$cycleNum" else "Focus on missed questions"
                val weakBadge = if (totalMistakes > 0) "$totalMistakes MISTAKES" else "BOOST"

                // 3. Weak Area Drill
                QuickModeCard(
                    title = "Weak Drill",
                    subtitle = weakSubtitle,
                    description = weakDesc,
                    badge = weakBadge,
                    badgeColor = Color(0xFFEC4899),
                    gradientColors = listOf(Color(0xFF831843), Color(0xFF1A050E)),
                    icon = Icons.Default.Psychology,
                    onClick = onStartWeakAreaDrill,
                    modifier = Modifier.weight(1f)
                )

                // 4. Custom Test Configurator
                QuickModeCard(
                    title = "Custom Test",
                    subtitle = "Configure Qs & Time",
                    description = "Pick subjects & chapters",
                    badge = "CUSTOM",
                    badgeColor = Color(0xFF10B981),
                    gradientColors = listOf(Color(0xFF064E3B), Color(0xFF021610)),
                    icon = Icons.Default.Tune,
                    onClick = onOpenCustomConfig,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickModeCard(
    title: String,
    subtitle: String,
    description: String,
    badge: String,
    badgeColor: Color,
    gradientColors: List<Color>,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("quick_mode_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0B1120) else LightSurface),
        border = BorderStroke(1.2.dp, if (isDark) badgeColor.copy(alpha = 0.45f) else LightBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) Brush.verticalGradient(gradientColors)
                    else Brush.verticalGradient(listOf(badgeColor.copy(alpha = 0.08f), LightSurface))
                )
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = if (isDark) 0.2f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = badgeColor.copy(alpha = if (isDark) 0.25f else 0.12f),
                        border = BorderStroke(0.8.dp, badgeColor)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = badgeColor,
                                fontSize = 8.sp,
                                letterSpacing = 0.4.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 13.5.sp
                    )
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 9.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 5. Practice by Subject & Chapter Browser (Compact and AI-enabled)
 */
@Composable
fun SubjectChapterPracticeSection(
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    selectedSubjectId: String?,
    onSelectSubjectFilter: (String?) -> Unit,
    onOpenAITest: (ChapterEntity?) -> Unit,
    onImportQuestions: (ChapterEntity?) -> Unit,
    onStartChapterTest: (ChapterEntity) -> Unit
) {
    val isDark = isAppDarkTheme()
    var showAllChapters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PRACTICE BY TOPIC",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = (if (isDark) Color(0xFF34D399) else Color(0xFF059669)).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${chapters.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick AI Test Action
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF2E1065) else Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onOpenAITest(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "AI Test",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Import Questions
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LightBorder),
                    modifier = Modifier.clickable { onImportQuestions(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "+ Import",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Subject Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedSubjectId == null,
                onClick = { onSelectSubjectFilter(null) },
                label = { Text("All Subjects", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = if (isDark) Color(0xFF0F172A) else LightSurface,
                    labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                ),
                border = BorderStroke(1.dp, if (selectedSubjectId == null) BrandPrimary else if (isDark) Color(0xFF1E293B) else LightBorder)
            )

            subjects.forEach { sub ->
                FilterChip(
                    selected = selectedSubjectId == sub.id,
                    onClick = { onSelectSubjectFilter(sub.id) },
                    label = { Text(sub.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = if (isDark) Color(0xFF0F172A) else LightSurface,
                        labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                    ),
                    border = BorderStroke(1.dp, if (selectedSubjectId == sub.id) BrandPrimary else if (isDark) Color(0xFF1E293B) else LightBorder)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val displayChapters = remember(selectedSubjectId, chapters) {
            if (selectedSubjectId == null) chapters else chapters.filter { it.subjectId == selectedSubjectId }
        }

        if (displayChapters.isEmpty()) {
            Text(
                text = "No chapters found for this selection.",
                style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF64748B) else LightTextMuted),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val chaptersToShow = if (showAllChapters) displayChapters else displayChapters.take(4)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chaptersToShow.forEach { chapter ->
                    val sub = subjects.find { it.id == chapter.subjectId }
                    val accentColor = try {
                        Color(android.graphics.Color.parseColor(sub?.colorHex ?: "#6366F1"))
                    } catch (e: Exception) {
                        BrandPrimary
                    }
                    val qCount = remember(chapter.questionsJson) {
                        TestQuestionProvider.parseQuestionsFromJson(chapter.questionsJson).size
                    }

                    // Ultra-Compact Chapter Practice Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartChapterTest(chapter) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else LightSurface),
                        border = BorderStroke(0.8.dp, if (isDark) Color(0xFF1E293B) else LightBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = if (isDark) 0.2f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sub?.name?.uppercase() ?: "GENERAL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 7.5.sp,
                                            letterSpacing = 0.3.sp
                                        )
                                    )
                                    Text(
                                        text = if (qCount > 0) " • $qCount Qs" else " • Practice",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                            fontSize = 7.5.sp
                                        )
                                    )
                                }
                                Text(
                                    text = chapter.title.ifBlank { "Untitled Chapter" },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else LightTextPrimary,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // AI Test Icon Action
                                IconButton(
                                    onClick = { onOpenAITest(chapter) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Test for ${chapter.title}",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Import Icon Action
                                IconButton(
                                    onClick = { onImportQuestions(chapter) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = "Import MCQs",
                                        tint = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                // Ultra-Compact Start Button
                                Button(
                                    onClick = { onStartChapterTest(chapter) },
                                    shape = RoundedCornerShape(5.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 1.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Test", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (displayChapters.size > 4) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (showAllChapters) "Show less ▲" else "Show all ${displayChapters.size} topics ▼",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showAllChapters = !showAllChapters }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6. Smart Analysis CTA Card (AI Insights)
 */
@Composable
fun SmartAnalysisCtaCard(
    totalTests: Int,
    avgScore: Int,
    avgAccuracy: Float,
    onOpenAskAI: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenAskAI)
            .testTag("test_smart_analysis_cta"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131A2E) else LightSurface),
        border = BorderStroke(1.2.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.6f) else BrandPrimary.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) Brush.radialGradient(
                        listOf(Color(0xFF6366F1).copy(alpha = 0.25f), Color.Transparent),
                        radius = 400f
                    ) else Brush.radialGradient(
                        listOf(BrandPrimary.copy(alpha = 0.08f), Color.Transparent),
                        radius = 400f
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF7C3AED).copy(alpha = 0.3f) else BrandPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFC084FC) else BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SMART AI DIAGNOSTICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFFC084FC) else BrandPrimary,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Text(
                            text = if (totalTests > 0) "Performance Deep Dive Ready" else "AI Test Readiness Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary,
                                fontSize = 14.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (totalTests > 0) {
                        "Based on your $totalTests tests ($avgScore% avg score), get custom suggestions on weak formulas, memory gaps, and speed optimization."
                    } else {
                        "Get structured advice on how to pace your practice tests, prevent negative markings, and tackle tricky questions."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onOpenAskAI,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask AI for Diagnostic Review", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 7. Test History Section Header & List Item
 */
@Composable
fun TestHistoryHeader(
    historyCount: Int,
    onClearAll: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TEST HISTORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
            ) {
                Text(
                    text = "$historyCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        if (historyCount > 0) {
            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TestHistoryItemCard(
    attempt: TestAttemptEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val scorePct = attempt.scorePercentage.toFloat().coerceIn(0f, 100f)
    val scoreColor = ProgressColor.forProgress(scorePct)
    val cardBorderColor = ProgressColor.border(scorePct, if (isDark) 0.35f else 0.25f)
    val fogGradient = ProgressColor.rememberAnimatedFogBrush(scorePct, isDark)

    val formattedDate = remember(attempt.completedAt) {
        try {
            DateFormat.format("MMM d, h:mm a", Date(attempt.completedAt)).toString()
        } catch (e: Exception) {
            "Recent"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.2.dp, cardBorderColor, RoundedCornerShape(14.dp))
            .testTag("history_item_${attempt.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else LightSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(fogGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score circle badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ProgressColor.softBg(scorePct, if (isDark) 0.22f else 0.14f))
                        .border(1.2.dp, ProgressColor.border(scorePct, 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${attempt.scorePercentage}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = scoreColor,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.testTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${attempt.correctAnswers}/${attempt.totalQuestions} Correct",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                            fontSize = 10.5.sp
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) Color(0xFF64748B) else LightTextMuted)
                    )
                    Text(
                        text = "${attempt.accuracy.toInt()}% Acc",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = scoreColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = if (isDark) Color(0xFF64748B) else LightTextMuted)
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF64748B) else LightTextMuted,
                            fontSize = 10.5.sp
                        )
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = if (isDark) Color(0xFF64748B) else LightTextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Analysis",
                tint = if (isDark) Color(0xFF818CF8) else BrandPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
}
