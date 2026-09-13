package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import com.example.ui.components.AnimatedProgressBar
import com.example.ui.theme.ProgressColor
import com.example.ui.theme.isAppDarkTheme
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.SubjectEntity
import com.example.ui.components.PrepOSBottomNavBar
import com.example.ui.editor.AskAIScreen
import com.example.ui.screens.home.HomeHeader
import com.example.ui.screens.home.HomeProgressGrid
import com.example.ui.screens.home.HomeQuickActions
import com.example.ui.screens.home.HomeResumeCard
import com.example.ui.screens.home.HomeSubjectProgress
import com.example.ui.screens.home.HomeTodayPlanCard
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PendingNewSubject(
    val name: String,
    val examId: String?,
    val icon: String,
    val color: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PrepOSViewModel,
    onOpenSubject: (subjectId: String, subjectName: String) -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFocusHub: () -> Unit,
    onOpenTest: (chapterId: String) -> Unit = {},
    onOpenTestHub: () -> Unit = {},
    onOpenAskAI: () -> Unit = {},
    initialTab: String = "home"
) {
    val exams by viewModel.exams.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val recentChapters by viewModel.recentChapters.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val selectedExamFilter by viewModel.selectedExamIdFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val studyTasks by viewModel.studyTasks.collectAsState()
    val completions by viewModel.allTaskCompletions.collectAsState()
    val dailyStudyLogs by viewModel.dailyStudyLogs.collectAsState()
    val activeFocusSession by viewModel.activeFocusSession.collectAsState()

    val smartNotifications by viewModel.smartNotifications.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val avgAccuracy by viewModel.avgAccuracy.collectAsState()
    val testAttempts by viewModel.allTestAttempts.collectAsState()

    var showNotificationCenter by remember { mutableStateOf(false) }
    val currentBottomNavTab by viewModel.currentHomeTab.collectAsState()

    androidx.compose.runtime.LaunchedEffect(initialTab) {
        if (initialTab.isNotBlank() && initialTab != "home") {
            viewModel.setCurrentHomeTab(initialTab)
        }
    }

    BackHandler(enabled = currentBottomNavTab != "home") {
        viewModel.setCurrentHomeTab("home")
    }

    val isDark = isAppDarkTheme()
    val context = LocalContext.current

    var showNewExamDialog by remember { mutableStateOf(false) }
    var showNewSubjectDialog by remember { mutableStateOf(false) }
    var showQuickFocusDialog by remember { mutableStateOf(false) }
    var showMilestonesDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    // Dialog state for asking user about retaining or clearing inbuilt sample content when creating custom subject
    var showInbuiltDataPromptDialog by remember { mutableStateOf(false) }
    var pendingSubjectData by remember { mutableStateOf<PendingNewSubject?>(null) }

    val streakCount = preferences?.currentStreak ?: 0
    val todayKey = remember { viewModel.getTodayDateKey() }
    val todayDayOfWeek = remember(todayKey) {
        viewModel.getDayOfWeekCode(todayKey)
    }

    val todayStudyTasks = remember(studyTasks, todayDayOfWeek) {
        studyTasks.filter { task ->
            task.daysOfWeek.isBlank() || task.daysOfWeek.contains(todayDayOfWeek, ignoreCase = true)
        }
    }

    val completedTasksCount = todayStudyTasks.count { task ->
        completions.any { it.taskId == task.id && it.dateKey == todayKey && it.isCompleted }
    }
    val totalTasksCount = todayStudyTasks.size

    val totalPlannedMinutes = todayStudyTasks.sumOf { it.durationMinutes }.coerceAtLeast(preferences?.dailyTargetMinutes ?: 45)
    val completedMinutes = todayStudyTasks
        .filter { task -> completions.any { it.taskId == task.id && it.dateKey == todayKey && it.isCompleted } }
        .sumOf { it.durationMinutes }
        .coerceAtLeast(if (preferences?.lastDailyTargetDate == todayKey) preferences?.todayFocusedMinutes ?: 0 else 0)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-initialize default exam filter if set in preferences and not manually selected yet
    var hasAppliedDefaultExam by remember { mutableStateOf(false) }
    LaunchedEffect(preferences?.defaultExamId, exams) {
        if (!hasAppliedDefaultExam && preferences?.defaultExamId != null && exams.isNotEmpty()) {
            if (exams.any { it.id == preferences?.defaultExamId }) {
                viewModel.selectExamFilter(preferences?.defaultExamId)
                hasAppliedDefaultExam = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                if (activeFocusSession.isActive) {
                    LiveFocusTimerPanel(
                        session = activeFocusSession,
                        onExpand = { onOpenFocusHub() },
                        onPause = { viewModel.pauseFocusSession() },
                        onResume = { viewModel.resumeFocusSession() },
                        onStop = { viewModel.stopFocusSession() }
                    )
                }
                PrepOSBottomNavBar(
                    selectedTab = currentBottomNavTab,
                    onSelectTab = { tab ->
                        when (tab) {
                            "home" -> {
                                viewModel.setCurrentHomeTab("home")
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            }
                            "study" -> {
                                viewModel.setCurrentHomeTab("study")
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            }
                            "plan" -> onOpenFocusHub()
                            "practice" -> onOpenTestHub()
                            "ask_ai" -> onOpenAskAI()
                        }
                    },
                    onOpenAskAI = onOpenAskAI
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            if (currentBottomNavTab == "home") {
                // ==========================================
                // --- HOME DASHBOARD (PREPOS REDESIGN) ---
                // ==========================================

                // 1. Home Header (Greeting, Subtitle, Theme Switcher/Notif/Settings)
                item {
                    val currentThemeMode = preferences?.appThemeMode ?: "SYSTEM"
                    HomeHeader(
                        userName = preferences?.preferredUserName ?: "",
                        unreadNotifCount = unreadNotificationCount,
                        currentThemeMode = currentThemeMode,
                        onToggleTheme = {
                            val nextMode = when (currentThemeMode.uppercase()) {
                                "SYSTEM" -> "DARK"
                                "DARK" -> "LIGHT"
                                else -> "SYSTEM"
                            }
                            viewModel.updateAppThemeMode(nextMode)
                            val toastMsg = when (nextMode) {
                                "DARK" -> "Theme: Dark Mode"
                                "LIGHT" -> "Theme: Light Mode"
                                else -> "Theme: Auto (System)"
                            }
                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                        },
                        onOpenNotifications = { showNotificationCenter = true },
                        onOpenSettings = onOpenSettings
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2. YOUR PROGRESS (2x2 Equal Grid: Study Time Today, Streak Days, Weekly Time, Accuracy)
                item {
                    val actualTodayStudyMinutes = viewModel.getStudiedMinutesForDate(
                        todayKey,
                        dailyStudyLogs,
                        preferences,
                        studyTasks,
                        completions
                    )
                    val yesterdayKey = viewModel.getYesterdayDateKey()
                    val yesterdayMins = viewModel.getStudiedMinutesForDate(
                        yesterdayKey,
                        dailyStudyLogs,
                        preferences,
                        studyTasks,
                        completions
                    )
                    val streak = preferences?.currentStreak ?: 0
                    val weeklyHours = try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        var totalMinutes = 0
                        for (i in 0 until 7) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -i)
                            val dateKey = sdf.format(cal.time)
                            val dayMins = viewModel.getStudiedMinutesForDate(
                                dateKey,
                                dailyStudyLogs,
                                preferences,
                                studyTasks,
                                completions
                            )
                            totalMinutes += dayMins
                        }
                        (totalMinutes / 60f).coerceAtLeast(actualTodayStudyMinutes / 60f)
                    } catch (e: Exception) {
                        (actualTodayStudyMinutes / 60f)
                    }
                    val accuracyPct = (avgAccuracy ?: 0f).toInt()

                    HomeProgressGrid(
                        todayFocusedMinutes = actualTodayStudyMinutes,
                        yesterdayFocusedMinutes = yesterdayMins,
                        streakDays = streak,
                        weeklyStudyHours = weeklyHours,
                        accuracyPercent = accuracyPct,
                        onCardClick = { onOpenFocusHub() },
                        onStreakClick = { showMilestonesDialog = true },
                        onAccuracyClick = { onOpenTestHub() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. SUBJECT PROGRESS (Compact unified card with icons, progress bars, percentages)
                item {
                    HomeSubjectProgress(
                        subjects = subjects,
                        chapters = recentChapters,
                        onOpenSubject = onOpenSubject,
                        onViewAllSubjects = { viewModel.setCurrentHomeTab("study") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 4. TODAY'S SCHEDULE (Today's Next Task / Daily Schedule with real Room tasks)
                item {
                    val actualTodayStudyMinutes = viewModel.getStudiedMinutesForDate(
                        todayKey,
                        dailyStudyLogs,
                        preferences,
                        studyTasks,
                        completions
                    )
                    HomeTodayPlanCard(
                        tasks = todayStudyTasks,
                        completions = completions,
                        todayKey = todayKey,
                        onToggleTask = { taskId, isCompleted ->
                            viewModel.toggleTaskCompletion(taskId, todayKey, isCompleted)
                        },
                        onViewAllPlan = { onOpenFocusHub() },
                        onAddTask = { showCreateTaskDialog = true },
                        onStartTaskFocus = { task ->
                            viewModel.startFocusSession(
                                task = task,
                                customMinutes = task.durationMinutes,
                                subjectName = task.subjectName
                            )
                        },
                        activeFocusSession = activeFocusSession,
                        dailyTargetMinutes = preferences?.dailyTargetMinutes ?: 45,
                        todayFocusedMinutes = actualTodayStudyMinutes,
                        onPauseFocus = { viewModel.pauseFocusSession() },
                        onResumeFocus = { viewModel.resumeFocusSession() },
                        onStopFocus = { viewModel.stopFocusSession() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }


                // 5. RESUME STUDY (Resume Reading Card from actual last active chapter)
                item {
                    HomeResumeCard(
                        recentChapters = recentChapters,
                        subjects = subjects,
                        onOpenChapter = onOpenChapter,
                        onStartFocus = { chapterId, subjectName, chapterTitle ->
                            viewModel.startFocusSession(
                                subjectName = subjectName,
                                chapterId = chapterId,
                                chapterTitle = chapterTitle
                            )
                        },
                        onSeeAll = { viewModel.setCurrentHomeTab("study") },
                        activeFocusSession = activeFocusSession,
                        onPauseFocus = { viewModel.pauseFocusSession() },
                        onResumeFocus = { viewModel.resumeFocusSession() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // ==========================================
                // --- STUDY WORKSPACE (NO TOP FOCUS BANNER) ---
                // ==========================================

                // 1. Study Header
                item {
                    StudyWorkspaceHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onOpenSettings = onOpenSettings
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 2. Exam Filter Chips
                item {
                    ExamFiltersRow(
                        exams = exams,
                        selectedExamFilter = selectedExamFilter,
                        onSelectExam = { viewModel.selectExamFilter(it) },
                        onAddExam = { showNewExamDialog = true }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3. Chapters Row (Matching chapters when searching, or Continue Reading when idle)
                val filteredRecentChapters = recentChapters.filter { chap ->
                    if (searchQuery.isBlank()) {
                        true
                    } else {
                        val q = searchQuery.trim().lowercase()
                        chap.title.lowercase().contains(q) ||
                            chap.summary.lowercase().contains(q) ||
                            "chapter ${chap.chapterNumber}".contains(q) ||
                            "ch ${chap.chapterNumber}".contains(q) ||
                            "${chap.chapterNumber}".contains(q)
                    }
                }

                if (searchQuery.isNotBlank()) {
                    if (filteredRecentChapters.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Matching Chapters",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                )
                                Text(
                                    text = "${filteredRecentChapters.size} found",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(filteredRecentChapters, key = { it.id }) { chapter ->
                                    val subject = subjects.find { it.id == chapter.subjectId }
                                    RecentChapterCard(
                                        chapter = chapter,
                                        subject = subject,
                                        onClick = { onOpenChapter(chapter.id) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else if (filteredRecentChapters.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue Reading",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                            Text(
                                text = "${filteredRecentChapters.size} recent",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color.Gray else LightTextSecondary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(filteredRecentChapters.take(6), key = { it.id }) { chapter ->
                                val subject = subjects.find { it.id == chapter.subjectId }
                                RecentChapterCard(
                                    chapter = chapter,
                                    subject = subject,
                                    onClick = { onOpenChapter(chapter.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 4. All Subjects Section Header
                val filteredSubjects = subjects.filter { sub ->
                    val matchesExam = selectedExamFilter == null || sub.examId == selectedExamFilter
                    val matchesSearch = searchQuery.isBlank() ||
                        sub.name.contains(searchQuery, ignoreCase = true) ||
                        recentChapters.any {
                            it.subjectId == sub.id && (
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.summary.contains(searchQuery, ignoreCase = true)
                            )
                        }
                    matchesExam && matchesSearch
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Matching Subjects" else "Your Subjects",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        )
                        Text(
                            text = "${filteredSubjects.size} subject${if (filteredSubjects.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color.Gray else LightTextSecondary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (filteredSubjects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF0F172A) else LightSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF1E293B) else GoldAccentBorderSubtle
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF64748B) else GoldAccentDark,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No subjects match '$searchQuery'" else "No subjects added yet",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try searching with a different term or clear search." else "Create your first subject to organize notes and start learning.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                if (searchQuery.isNotEmpty()) {
                                    Button(
                                        onClick = { viewModel.setSearchQuery("") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Color(0xFF6366F1) else GoldAccent
                                        )
                                    ) {
                                        Text("Clear Search", color = Color.White)
                                    }
                                } else {
                                    Button(
                                        onClick = { showNewSubjectDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Color(0xFF6366F1) else GoldAccent
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Subject", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredSubjects, key = { it.id }) { subject ->
                        val exam = exams.find { it.id == subject.examId }
                        val chaptersInSubject = recentChapters.filter { it.subjectId == subject.id }
                        val avgProgress = if (chaptersInSubject.isNotEmpty()) {
                            val allDone = chaptersInSubject.all { it.readingProgress >= 0.98f }
                            if (allDone) {
                                100
                            } else {
                                val avg = chaptersInSubject.map { if (it.readingProgress >= 0.98f) 1.0f else it.readingProgress }.average().toFloat()
                                if (avg >= 0.985f) 100 else kotlin.math.round(avg * 100).toInt().coerceIn(0, 100)
                            }
                        } else {
                            0
                        }

                        SubjectCard(
                            subject = subject,
                            exam = exam,
                            chapterCount = chaptersInSubject.size,
                            progressPct = avgProgress,
                            showExamLabel = true,
                            onClick = { onOpenSubject(subject.id, subject.name) },
                            onDelete = { viewModel.deleteSubject(subject.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 6. Add Subject Card Action
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    AddNewSubjectCard(onClick = { showNewSubjectDialog = true })
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal: Create Study Task Dialog
    if (showCreateTaskDialog) {
        CreateStudyTaskDialog(
            subjects = subjects,
            onDismiss = { showCreateTaskDialog = false },
            onCreate = { subjectName, taskTitle, minutes ->
                viewModel.createStudyTask(
                    subjectName = subjectName,
                    taskTitle = taskTitle,
                    durationMinutes = minutes
                )
                showCreateTaskDialog = false
            }
        )
    }

    // Modal: Milestones Dialog
    if (showMilestonesDialog) {
        MilestonesDialog(
            streak = streakCount,
            onDismiss = { showMilestonesDialog = false }
        )
    }

    // Modal: Quick Focus Dialog
    if (showQuickFocusDialog) {
        QuickFocusDialog(
            subjects = subjects,
            onDismiss = { showQuickFocusDialog = false },
            onStartFocus = { subjectName, durationMinutes ->
                viewModel.startFocusSession(
                    subjectName = subjectName,
                    customMinutes = durationMinutes
                )
                showQuickFocusDialog = false
            }
        )
    }

    // Modal: Create Exam Dialog
    if (showNewExamDialog) {
        CreateExamDialog(
            onDismiss = { showNewExamDialog = false },
            onCreate = { name, code, color ->
                viewModel.createExam(name, code, color)
                showNewExamDialog = false
            }
        )
    }

    // Modal: Create Subject Dialog
    if (showNewSubjectDialog) {
        CreateSubjectDialog(
            exams = exams,
            selectedExamId = selectedExamFilter,
            onDismiss = { showNewSubjectDialog = false },
            onCreate = { name, examId, icon, color ->
                showNewSubjectDialog = false
                val hasInbuiltExamsOrSubjects = exams.any { it.name.contains("JKSSB", ignoreCase = true) || it.name.contains("SSC", ignoreCase = true) }
                if (hasInbuiltExamsOrSubjects && subjects.isNotEmpty()) {
                    pendingSubjectData = PendingNewSubject(name, examId, icon, color)
                    showInbuiltDataPromptDialog = true
                } else {
                    viewModel.createSubject(name, examId, icon, color)
                }
            }
        )
    }

    // Modal: Inbuilt Data Retain vs Clean Prompt Dialog
    if (showInbuiltDataPromptDialog && pendingSubjectData != null) {
        val pending = pendingSubjectData!!
        AlertDialog(
            onDismissRequest = {
                // Default: just create the subject and keep existing data
                viewModel.createSubject(pending.name, pending.examId, pending.icon, pending.color)
                showInbuiltDataPromptDialog = false
                pendingSubjectData = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Study Library", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You are creating your own subject \"${pending.name}\".",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Would you like to keep the built-in JKSSB Constable content alongside your custom subjects, or clear the built-in content and start completely fresh with your own curriculum?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Keep inbuilt content & create new subject
                        viewModel.createSubject(pending.name, pending.examId, pending.icon, pending.color)
                        showInbuiltDataPromptDialog = false
                        pendingSubjectData = null
                    }
                ) {
                    Text("Keep Built-in & Add")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        // Clear inbuilt content & create custom subject
                        coroutineScope.launch {
                            viewModel.clearAllStudyContent()
                            viewModel.createSubject(pending.name, pending.examId, pending.icon, pending.color)
                            showInbuiltDataPromptDialog = false
                            pendingSubjectData = null
                        }
                    }
                ) {
                    Text("Clear Built-in (Fresh)")
                }
            }
        )
    }

    // Modal: Notification Center Sheet
    if (showNotificationCenter) {
        com.example.ui.components.NotificationCenterSheet(
            notifications = smartNotifications,
            onDismiss = { showNotificationCenter = false },
            onMarkAsRead = { id -> viewModel.markNotificationAsRead(id) },
            onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
            onDeleteNotification = { id -> viewModel.deleteNotification(id) },
            onClearAll = { viewModel.clearAllNotifications() },
            onStartFocus = {
                showNotificationCenter = false
                viewModel.startFocusSession()
            },
            onOpenSubject = { subId, subName ->
                showNotificationCenter = false
                onOpenSubject(subId, subName)
            },
            onOpenChapter = { chapId ->
                showNotificationCenter = false
                onOpenChapter(chapId)
            },
            onViewPlan = {
                showNotificationCenter = false
                onOpenFocusHub()
            }
        )
    }

    // Interrupted Session Recovery Dialog
    if (activeFocusSession.isInterrupted) {
        val elapsedMins = (activeFocusSession.recoverableElapsedSeconds / 60).coerceAtLeast(1)
        val plannedMins = activeFocusSession.totalSeconds / 60
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️ Focus Session Interrupted", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your previous focus session was interrupted.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1B4B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Task: ${activeFocusSession.taskTitle}", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Detected Elapsed: ${elapsedMins} min", color = Color(0xFF34D399), fontSize = 13.sp)
                            Text("Planned Duration: ${plannedMins} min", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        }
                    }
                    Text(
                        "Choose whether to resume from where you left off or save the verified elapsed time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resumeInterruptedSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Resume")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = { viewModel.discardInterruptedSession() }
                    ) {
                        Text("Discard", color = Color(0xFFEF4444))
                    }
                    Button(
                        onClick = { viewModel.saveInterruptedSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save ${elapsedMins}m", color = Color.White)
                    }
                }
            }
        )
    }
}

// ==========================================
// --- HOME DASHBOARD COMPONENTS ---
// ==========================================

@Composable
fun HomeGreetingHeader(
    unreadNotifCount: Int,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good Morning, Ali 👋"
            in 12..16 -> "Good Afternoon, Ali 👋"
            else -> "Good Evening, Ali 👋"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Let's make today productive!",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Search Icon Button
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                modifier = Modifier
                    .size(38.dp)
                    .clickable(onClick = onOpenSearch)
                    .testTag("home_search_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Notification Bell with Badge
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(onClick = onOpenNotifications)
                        .testTag("home_notification_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                if (unreadNotifCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp, end = 1.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadNotifCount > 9) "9+" else unreadNotifCount.toString(),
                            style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Settings Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                modifier = Modifier
                    .size(38.dp)
                    .clickable(onClick = onOpenSettings)
                    .testTag("home_settings_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DayStreakDashboardCard(
    streak: Int,
    todayKey: String,
    onMilestonesClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMilestonesClick)
            .testTag("home_streak_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF1E293B))
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
                // Flame icon in circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF97316).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (streak > 0) "$streak Day Streak" else "Start Your Streak",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = if (streak > 0) "Keep it up! You're on fire! 🔥" else "Complete today's study target to begin!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7-Day Calendar Row
            val weekDays = remember {
                val cal = Calendar.getInstance()
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val days = mutableListOf<Triple<String, Int, Boolean>>()
                val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

                val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                for (name in dayNames) {
                    val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                    val isToday = dayNum == todayDay
                    days.add(Triple(name, dayNum, isToday))
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
                days
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { (name, dayNum, isToday) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isToday) Color(0xFFC084FC) else Color(0xFF64748B),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isToday) Color(0xFF7C3AED) else Color(0xFF1E293B)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = TextStyle(
                                    color = if (isToday) Color.White else Color(0xFFCBD5E1),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        // Completion dot
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (streak > 0) Color(0xFF10B981) else Color(0xFF334155)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodayPlanScheduleCard(
    tasks: List<StudyTaskEntity>,
    completions: List<com.example.data.entity.TaskCompletionEntity>,
    todayKey: String,
    completedCount: Int,
    totalCount: Int,
    completedMinutes: Int,
    totalMinutes: Int,
    onToggleTask: (taskId: String, isCompleted: Boolean) -> Unit,
    onViewAllPlan: () -> Unit,
    onAddTask: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_today_plan_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Today's Plan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Active",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = TextStyle(
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Text(
                    text = "View All >",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.clickable(onClick = onViewAllPlan)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (tasks.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No study targets scheduled for today",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                        Button(
                            onClick = onAddTask,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Study Target", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Task List Items
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tasks.take(3).forEach { task ->
                        val isDone = completions.any { it.taskId == task.id && it.dateKey == todayKey && it.isCompleted }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDone) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFF1E293B))
                                .clickable { onToggleTask(task.id, !isDone) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox circle
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isDone) Color(0xFF10B981) else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isDone) Color(0xFF10B981) else Color(0xFF64748B),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${task.subjectName} • ${task.taskTitle}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDone) Color(0xFF94A3B8) else Color.White,
                                        fontSize = 13.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (task.subDetails.isNotBlank()) task.subDetails else "Study for ${task.durationMinutes} minutes",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF64748B),
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${task.durationMinutes} min",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDone) Color(0xFF34D399) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Time Summary & Progress Bar
            val progressFraction = if (totalMinutes > 0) (completedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f) else 0f
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedMinutes minutes of $totalMinutes minutes completed",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "$completedCount/$totalCount Tasks",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFC084FC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFFC084FC), Color(0xFF38BDF8))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ResumeStudyCard(
    recentChapters: List<ChapterEntity>,
    subjects: List<SubjectEntity>,
    onOpenChapter: (chapterId: String) -> Unit,
    onStartFocus: (chapterId: String, subjectName: String, chapterTitle: String) -> Unit,
    onSeeAll: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val lastChapter = recentChapters.firstOrNull()
    val subject = if (lastChapter != null) subjects.find { it.id == lastChapter.subjectId } else null
    val subjectName = subject?.name ?: "Study"
    val chapterTitle = lastChapter?.title ?: "No recent chapters"
    val progressPct = if (lastChapter != null) {
        if (lastChapter.readingProgress >= 0.98f) 100 else kotlin.math.round(lastChapter.readingProgress * 100).toInt().coerceIn(0, 100)
    } else 0
    val progressFloat = progressPct.toFloat()
    val chapterNum = if (lastChapter != null && lastChapter.chapterNumber > 0) "Chapter ${lastChapter.chapterNumber}" else "Chapter 1"

    val dynamicColor = ProgressColor.forProgressSmart(progressFloat)
    val cardBorderColor = ProgressColor.border(progressFloat, if (isDark) 0.35f else 0.28f)
    val fogGradient = ProgressColor.rememberAnimatedFogBrush(progressFloat, isDark)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = lastChapter != null) {
                if (lastChapter != null) onOpenChapter(lastChapter.id)
            }
            .border(
                width = 1.5.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("home_resume_study_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(fogGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resume Study",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "See All >",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = dynamicColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.clickable(onClick = onSeeAll)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (lastChapter == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else LightSurfaceSecondary)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No chapters started yet. Pick a subject to begin reading!",
                            style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ProgressColor.softBg(progressFloat, if (isDark) 0.22f else 0.15f))
                                .border(0.8.dp, ProgressColor.border(progressFloat, if (isDark) 0.40f else 0.30f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = dynamicColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Subject chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ProgressColor.softBg(progressFloat, if (isDark) 0.22f else 0.15f))
                                    .border(0.8.dp, ProgressColor.border(progressFloat, if (isDark) 0.40f else 0.30f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = subjectName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = dynamicColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = chapterTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$chapterNum • $progressPct% Completed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Animated Progress Bar (6dp thick)
                    AnimatedProgressBar(
                        percent = progressFloat,
                        height = 6.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actions Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onStartFocus(lastChapter.id, subjectName, chapterTitle)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Focus", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onOpenChapter(lastChapter.id) },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ProgressColor.border(progressFloat, 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "→ Continue",
                                color = dynamicColor,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YourProgressDashboardSection(
    preferences: com.example.data.entity.UserPreferencesEntity?,
    avgAccuracy: Float?,
    subjects: List<SubjectEntity>,
    onViewAnalytics: () -> Unit,
    onOpenSubject: (subjectId: String, subjectName: String) -> Unit,
    onViewAllSubjects: () -> Unit,
    onOpenTestHub: () -> Unit = {}
) {
    val isDark = isAppDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_your_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else LightSurface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isDark) Color(0xFF1E293B) else GoldAccentBorderSubtle)
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
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = "View Analytics >",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF818CF8) else GoldAccentDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.clickable(onClick = onViewAnalytics)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Stat Tiles Grid (2x2)
            val todayFocusedMins = preferences?.todayFocusedMinutes ?: 0
            val hours = todayFocusedMins / 60
            val mins = todayFocusedMins % 60
            val studyTimeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            val accuracyStr = if (avgAccuracy != null && avgAccuracy > 0) "${avgAccuracy.toInt()}%" else "75%"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Study Time
                ProgressMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    iconTint = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                    title = "Study Time",
                    value = studyTimeStr,
                    subtext = "Today"
                )

                // Stat 2: Focus Sessions
                ProgressMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Adjust,
                    iconTint = Color(0xFF10B981),
                    title = "Focus Sessions",
                    value = if (todayFocusedMins > 0) "${(todayFocusedMins / 25).coerceAtLeast(1)}" else "0",
                    subtext = "Today"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 3: Weekly Goal
                ProgressMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TrendingUp,
                    iconTint = if (isDark) Color(0xFFFBBF24) else GoldAccent,
                    title = "Weekly Goal",
                    value = "68%",
                    subtext = "5h / 7.5h"
                )

                // Stat 4: Accuracy
                ProgressMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    iconTint = Color(0xFFEC4899),
                    title = "Accuracy",
                    value = accuracyStr,
                    subtext = "This Week",
                    onClick = onOpenTestHub
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Subject Progress Sub-Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject Progress",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "View All >",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF818CF8) else GoldAccentDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    modifier = Modifier.clickable(onClick = onViewAllSubjects)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (subjects.isEmpty()) {
                Text(
                    text = "No subjects added yet.",
                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF64748B) else LightTextMuted)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    subjects.take(3).forEachIndexed { index, sub ->
                        val samplePct = when (index) {
                            0 -> 65
                            1 -> 48
                            else -> 32
                        }
                        val accentColor = try {
                            Color(android.graphics.Color.parseColor(sub.colorHex))
                        } catch (e: Exception) {
                            if (isDark) Color(0xFF8B5CF6) else GoldAccent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSubject(sub.id, sub.name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isDark) Color.White else LightTextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.width(100.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = samplePct / 100f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(accentColor)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "$samplePct%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF64748B) else LightTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressMetricTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtext: String,
    onClick: (() -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else GoldAccentLight.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else GoldAccentBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary,
                    fontSize = 16.sp
                )
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF64748B) else LightTextMuted,
                    fontSize = 10.5.sp
                )
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onStartFocus: () -> Unit,
    onTakeTest: () -> Unit,
    onAddNote: () -> Unit,
    onViewPlan: () -> Unit,
    onAchievements: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else LightTextPrimary,
                fontSize = 16.sp
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                label = "Start Focus",
                icon = Icons.Default.Adjust,
                accentColor = if (isDark) Color(0xFF8B5CF6) else GoldAccent,
                onClick = onStartFocus
            )
            QuickActionButton(
                label = "Take a Test",
                icon = Icons.Default.Quiz,
                accentColor = Color(0xFFEC4899),
                onClick = onTakeTest
            )
            QuickActionButton(
                label = "Add Note",
                icon = Icons.Default.MenuBook,
                accentColor = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                onClick = onAddNote
            )
            QuickActionButton(
                label = "View Plan",
                icon = Icons.Default.DateRange,
                accentColor = Color(0xFF10B981),
                onClick = onViewPlan
            )
            QuickActionButton(
                label = "Achievements",
                icon = Icons.Default.EmojiEvents,
                accentColor = if (isDark) Color(0xFFF59E0B) else GoldAccentDark,
                onClick = onAchievements
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF0F172A) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else GoldAccentBorderSubtle),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("home_quick_action_${label.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.2f else 0.15f)),
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
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else LightTextPrimary,
                    fontSize = 12.5.sp
                )
            )
        }
    }
}

// ==========================================
// --- STUDY WORKSPACE COMPONENTS ---
// ==========================================

@Composable
fun StudyWorkspaceHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Study & Subjects",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "Organized learning workspace",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        CompactTypewriterSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )
    }
}

@Composable
fun ExamFiltersRow(
    exams: List<ExamEntity>,
    selectedExamFilter: String?,
    onSelectExam: (String?) -> Unit,
    onAddExam: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExamChip(
            title = "All Subjects",
            isSelected = selectedExamFilter == null,
            colorHex = "#6366F1",
            onClick = { onSelectExam(null) }
        )

        exams.forEach { exam ->
            ExamChip(
                title = exam.name,
                code = exam.code,
                isSelected = selectedExamFilter == exam.id,
                colorHex = exam.colorHex,
                onClick = { onSelectExam(exam.id) }
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle),
            modifier = Modifier.clickable(onClick = onAddExam)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Add Exam",
                    style = TextStyle(
                        color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun CompactTypewriterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val placeholderPhrases = remember {
        listOf(
            "Search subjects...",
            "Search chapters...",
            "Search revision notes...",
            "Find formulas & rules...",
            "Search exam topics..."
        )
    }

    var animatedPlaceholder by remember { mutableStateOf("Search subjects...") }

    LaunchedEffect(query.isEmpty()) {
        if (query.isNotEmpty()) return@LaunchedEffect

        var phraseIndex = 0
        while (true) {
            val currentPhrase = placeholderPhrases[phraseIndex % placeholderPhrases.size]

            for (i in 1..currentPhrase.length) {
                animatedPlaceholder = currentPhrase.take(i)
                delay(70)
            }
            delay(1600)

            for (i in currentPhrase.length downTo 0) {
                animatedPlaceholder = currentPhrase.take(i)
                delay(35)
            }
            delay(300)

            phraseIndex++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = animatedPlaceholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_field"),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExamChip(
    title: String,
    code: String = "",
    isSelected: Boolean,
    colorHex: String,
    onClick: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val accentColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        if (isDark) MaterialTheme.colorScheme.primary else GoldAccent
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) accentColor else (if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else LightSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor else (if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else LightTextPrimary)
            )
            if (code.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else (if (isDark) Color.Gray.copy(alpha = 0.2f) else GoldAccentLight)
                ) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else GoldAccentDark),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentChapterCard(
    chapter: ChapterEntity,
    subject: SubjectEntity?,
    onClick: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val progressPct = if (chapter.readingProgress >= 0.98f) 100 else kotlin.math.round(chapter.readingProgress * 100).toInt().coerceIn(0, 100)
    val progressFloat = progressPct.toFloat()
    val dynamicColor = ProgressColor.forProgressSmart(progressFloat)
    val cardBorderColor = ProgressColor.border(progressFloat, if (isDark) 0.35f else 0.28f)
    val fogGradient = ProgressColor.rememberAnimatedFogBrush(progressFloat, isDark)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0F172A) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, cardBorderColor),
        modifier = Modifier
            .width(165.dp)
            .clickable(onClick = onClick)
            .testTag("home_recent_chapter_${chapter.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fogGradient)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subject?.name ?: "Subject",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = dynamicColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$progressPct%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = dynamicColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedProgressBar(
                    percent = progressFloat,
                    height = 4.dp
                )
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: SubjectEntity,
    exam: ExamEntity?,
    chapterCount: Int,
    progressPct: Int = 0,
    showExamLabel: Boolean = true,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val progressFloat = progressPct.toFloat()
    val progressColor = ProgressColor.forProgressSmart(progressFloat)
    val cardBorderColor = ProgressColor.border(progressFloat, if (isDark) 0.38f else 0.30f)
    val fogGradient = ProgressColor.rememberAnimatedFogBrush(progressFloat, isDark)

    val iconVector: ImageVector = when (subject.iconName) {
        "psychology" -> Icons.Default.Psychology
        "calculate" -> Icons.Default.Calculate
        "memory" -> Icons.Default.Memory
        "science" -> Icons.Default.Science
        else -> Icons.Default.MenuBook
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.5.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("subject_card_${subject.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(fogGradient)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ProgressColor.softBg(progressFloat, if (isDark) 0.22f else 0.15f))
                        .border(
                            0.8.dp,
                            ProgressColor.border(progressFloat, if (isDark) 0.45f else 0.35f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = progressColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (showExamLabel && exam != null) {
                        Text(
                            text = exam.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = progressColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$chapterCount chapter${if (chapterCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedProgressBar(
                        percent = progressFloat,
                        height = 4.dp,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (progressPct > 0) {
                        Text(
                            text = "$progressPct%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = progressColor,
                                fontSize = 12.sp
                            )
                        )
                        if (progressPct >= 100) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF6C63D9),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Subject",
                            tint = if (isDark) Color(0xFF64748B) else LightTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF64748B) else LightTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Subject?") },
            text = { Text("Are you sure you want to delete '${subject.name}' and all of its notes?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddNewSubjectCard(onClick: () -> Unit) {
    val isDark = isAppDarkTheme()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isDark) Color(0xFF6366F1).copy(alpha = 0.35f) else GoldAccentBorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("home_add_subject_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF312E81).copy(alpha = 0.6f) else GoldAccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Subject",
                    tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add New Subject",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 14.5.sp
                    )
                )
                Text(
                    text = "Create your own customized learning path",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 11.5.sp
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isDark) Color(0xFF818CF8) else LightTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LiveFocusTimerPanel(
    session: com.example.viewmodel.ActiveFocusSession,
    onExpand: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val remSec = session.remainingSeconds
    val minutes = remSec / 60
    val seconds = remSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val totalSec = if (session.totalSeconds > 0) session.totalSeconds else 1
    val sessionProgress = ((totalSec - remSec).toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

    Surface(
        color = Color(0xFF1E1B4B),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (session.isPaused) Color(0xFFF59E0B) else Color(0xFF8B5CF6)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .testTag("home_live_focus_panel")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (session.isPaused) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (session.isPaused) Icons.Default.Pause else Icons.Default.Adjust,
                    contentDescription = null,
                    tint = if (session.isPaused) Color(0xFFF59E0B) else Color(0xFFC084FC),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (session.isPaused) "Paused • " else "Focusing • "}${session.subjectName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (session.isPaused) Color(0xFFF59E0B) else Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = session.taskTitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = timeFormatted,
                style = TextStyle(
                    color = if (session.isPaused) Color(0xFFFBBF24) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { if (session.isPaused) onResume() else onPause() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==========================================
// --- DIALOGS ---
// ==========================================

@Composable
fun CreateStudyTaskDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onCreate: (subjectName: String, taskTitle: String, durationMinutes: Int) -> Unit
) {
    val isDark = isAppDarkTheme()
    var taskTitle by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "Study") }
    var durationMinutes by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = { Text("Add Study Target", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Topic or Chapter Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (subjects.isNotEmpty()) {
                    Text("Subject", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { sub ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedSubject == sub.name) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                                modifier = Modifier.clickable { selectedSubject = sub.name }
                            ) {
                                Text(
                                    sub.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = if (selectedSubject == sub.name) Color.White else (if (isDark) Color.White else LightTextPrimary),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Text("Target Duration: $durationMinutes min", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 30, 45, 60).forEach { mins ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (durationMinutes == mins) Color(0xFF6366F1) else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                            modifier = Modifier.clickable { durationMinutes = mins }
                        ) {
                            Text(
                                "${mins}m",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (durationMinutes == mins) Color.White else (if (isDark) Color(0xFFCBD5E1) else LightTextSecondary),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskTitle.isNotBlank()) {
                        onCreate(selectedSubject, taskTitle, durationMinutes)
                    }
                },
                enabled = taskTitle.isNotBlank()
            ) {
                Text("Add Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary) }
        }
    )
}

@Composable
fun QuickFocusDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onStartFocus: (subjectName: String, durationMinutes: Int) -> Unit
) {
    val isDark = isAppDarkTheme()
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "PrepOS Focus") }
    var durationMinutes by remember { mutableIntStateOf(25) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = { Text("Start Focus Timer", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Subject", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedSubject == "PrepOS Focus") MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                        modifier = Modifier.clickable { selectedSubject = "PrepOS Focus" }
                    ) {
                        Text(
                            "General",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = if (selectedSubject == "PrepOS Focus") Color.White else (if (isDark) Color.White else LightTextPrimary),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    subjects.forEach { sub ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedSubject == sub.name) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                            modifier = Modifier.clickable { selectedSubject = sub.name }
                        ) {
                            Text(
                                sub.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (selectedSubject == sub.name) Color.White else (if (isDark) Color.White else LightTextPrimary),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text("Duration", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 30, 45, 60).forEach { mins ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (durationMinutes == mins) Color(0xFF7C3AED) else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                            modifier = Modifier.clickable { durationMinutes = mins }
                        ) {
                            Text(
                                "${mins} min",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (durationMinutes == mins) Color.White else (if (isDark) Color(0xFFCBD5E1) else LightTextSecondary),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartFocus(selectedSubject, durationMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("Start Focus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary) }
        }
    )
}

@Composable
fun MilestonesDialog(
    streak: Int,
    onDismiss: () -> Unit
) {
    val isDark = isAppDarkTheme()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = { Text("Streak & Achievements", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E1B4B) else Color(0xFFF5F3FF),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF312E81) else Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$streak Day Streak", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary, fontSize = 16.sp)
                            Text("Earned automatically by completing study time & tasks", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Text("Milestones", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary)
                listOf(
                    Triple(3, "Bronze Scout", streak >= 3),
                    Triple(7, "Silver Vanguard", streak >= 7),
                    Triple(14, "Gold Sentinel", streak >= 14),
                    Triple(30, "Diamond Warden", streak >= 30),
                    Triple(100, "Elite Centurion", streak >= 100)
                ).forEach { (targetDays, title, isUnlocked) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isUnlocked) "🏆" else "🔒", fontSize = 16.sp)
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isUnlocked) (if (isDark) Color.White else LightTextPrimary) else (if (isDark) Color.Gray else LightTextMuted))
                                Text("$targetDays days target", fontSize = 11.sp, color = if (isDark) Color.Gray else LightTextMuted)
                            }
                        }
                        Text(
                            if (isUnlocked) "UNLOCKED" else "${targetDays - streak}d left",
                            style = TextStyle(
                                color = if (isUnlocked) Color(0xFF34D399) else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = MaterialTheme.colorScheme.primary) }
        }
    )
}

@Composable
fun CreateExamDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, code: String, color: String) -> Unit
) {
    val isDark = isAppDarkTheme()
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#2563EB") }

    val colors = listOf("#2563EB", "#7C3AED", "#DB2777", "#D97706", "#059669", "#DC2626")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = { Text("Add Exam Goal", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exam Name (e.g. SSC CGL, UPSC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Short Code (e.g. CGL, CSE)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Theme Color", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                    color = if (selectedColor == colorHex) (if (isDark) Color.White else LightTextPrimary) else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onCreate(name, code, selectedColor)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create Exam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
            }
        }
    )
}

@Composable
fun CreateSubjectDialog(
    exams: List<ExamEntity>,
    selectedExamId: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, examId: String?, icon: String, color: String) -> Unit
) {
    val isDark = isAppDarkTheme()
    var name by remember { mutableStateOf("") }
    var chosenExamId by remember { mutableStateOf(selectedExamId) }
    var selectedIcon by remember { mutableStateOf("menu_book") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }

    val icons = listOf(
        "menu_book" to Icons.Default.MenuBook,
        "psychology" to Icons.Default.Psychology,
        "calculate" to Icons.Default.Calculate,
        "memory" to Icons.Default.Memory,
        "science" to Icons.Default.Science
    )

    val colors = listOf("#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#10B981", "#EF4444")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = { Text("New Study Subject", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. Quantitative Aptitude)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (exams.isNotEmpty()) {
                    Text("Associated Exam", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (chosenExamId == null) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                            modifier = Modifier.clickable { chosenExamId = null }
                        ) {
                            Text(
                                "None",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (chosenExamId == null) Color.White else (if (isDark) Color.White else LightTextPrimary),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        exams.forEach { exam ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (chosenExamId == exam.id) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                                modifier = Modifier.clickable { chosenExamId = exam.id }
                            ) {
                                Text(
                                    exam.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = if (chosenExamId == exam.id) Color.White else (if (isDark) Color.White else LightTextPrimary),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Text("Icon", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { (iconName, vector) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                            modifier = Modifier.clickable { selectedIcon = iconName }
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = if (selectedIcon == iconName) Color.White else (if (isDark) Color.White else LightTextPrimary)
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium, color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                    color = if (selectedColor == colorHex) (if (isDark) Color.White else LightTextPrimary) else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onCreate(name, chosenExamId, selectedIcon, selectedColor)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
            }
        }
    )
}
