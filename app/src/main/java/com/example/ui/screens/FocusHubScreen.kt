package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import com.example.ui.screens.home.formatMinutesToDisplay
import com.example.ui.screens.progression.MasterRankIdentityCard
import com.example.ui.screens.progression.TasksDetailSheet
import com.example.ui.screens.progression.ProgressionDetailsModal
import com.example.util.BatteryOptimizationHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import com.example.ui.components.PrepOSBottomNavBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ChapterEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.SubjectEntity
import com.example.ui.editor.AskAIScreen
import com.example.util.FocusAudioMode
import com.example.util.PrepOSAppPinningManager
import com.example.util.PrepOSFocusController
import com.example.viewmodel.ActiveFocusSession
import com.example.viewmodel.PrepOSViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class FocusTimeScope(val label: String) {
    SELECTED_DAY("Selected Day"),
    THIS_WEEK("This Week (7 Days)"),
    THIS_MONTH("This Month"),
    ALL_TIME("Total Study")
}

// Helper function for standardized 5-level progress coloring
fun getFocusProgressColor(percentage: Int): Color = ProgressColor.forPercentage(percentage)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHubScreen(
    viewModel: PrepOSViewModel,
    onBack: () -> Unit,
    onOpenChapter: (String) -> Unit = {},
    onOpenTest: (String) -> Unit = {},
    onNavigateTab: (String) -> Unit = {},
    onOpenAskAI: () -> Unit = {}
) {
    val context = LocalContext.current
    val tasks by viewModel.studyTasks.collectAsState()
    val completions by viewModel.allTaskCompletions.collectAsState()
    val dailyStudyLogs by viewModel.dailyStudyLogs.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val progressionOverview by viewModel.progressionOverview.collectAsState()

    val selectedDateKey by viewModel.selectedDateKey.collectAsState()
    val activeFocusSession by viewModel.activeFocusSession.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val recentChapters by viewModel.recentChapters.collectAsState()
    val availableSubjectNames = remember(subjects) {
        subjects.map { it.name }.distinct().filter { it.isNotBlank() }
    }

    var showSettingsModal by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showTimerPresetDialog by remember { mutableStateOf(false) }
    var showNotificationBehaviorDialog by remember { mutableStateOf(false) }
    var showPracticeSheet by remember { mutableStateOf(false) }
    var showTasksDetailSheet by remember { mutableStateOf(false) }
    var tasksSheetInitialTab by remember { mutableStateOf(0) }
    var showStreakMilestonesDialog by remember { mutableStateOf(false) }
    var showRoadmapModal by remember { mutableStateOf(false) }
    var taskPendingDelete by remember { mutableStateOf<StudyTaskEntity?>(null) }

    // Always auto-select Today upon opening Focus Hub / Timetable
    LaunchedEffect(Unit) {
        viewModel.selectToday()
    }

    val todayKey = remember {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        } catch (e: Exception) {
            "2026-08-30"
        }
    }

    val isViewingToday = selectedDateKey == todayKey
    val isViewingPast = selectedDateKey < todayKey
    val isViewingFuture = selectedDateKey > todayKey

    // Focus Settings bindings
    val appPinning = preferences?.focusAppPinningEnabled ?: true
    val dnd = preferences?.focusDndEnabled ?: true
    val notifBehavior = preferences?.focusNotificationBehaviour ?: "SILENT"
    val timerMins = preferences?.focusTimerMinutes ?: 45
    val followTimetable = preferences?.focusFollowTimetable ?: true
    val streakCount = preferences?.currentStreak ?: 0

    // Dynamic 7-day date window around today
    val daysList = remember(todayKey) {
        val list = mutableListOf<DateItem>()
        val cal = Calendar.getInstance()
        
        // Start 2 days before today up to 4 days after
        cal.add(Calendar.DAY_OF_YEAR, -2)
        for (i in 0..6) {
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            val dayNum = SimpleDateFormat("d", Locale.US).format(cal.time)
            val dayName = viewModel.getDayOfWeekCode(key)
            val isToday = key == todayKey
            list.add(DateItem(dateKey = key, dayNumber = dayNum, dayName = dayName, isToday = isToday))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val currentDayMeta = daysList.find { it.dateKey == selectedDateKey } 
        ?: daysList.find { it.isToday } 
        ?: daysList.firstOrNull() 
        ?: DateItem(selectedDateKey, "1", "DAY", isToday = true)

    val selectedDayOfWeek = remember(selectedDateKey) {
        viewModel.getDayOfWeekCode(selectedDateKey)
    }

    val tasksForSelectedDate = remember(tasks, selectedDayOfWeek) {
        tasks.filter { task ->
            task.daysOfWeek.isBlank() || task.daysOfWeek.contains(selectedDayOfWeek, ignoreCase = true)
        }
    }

    val completedTaskIdsForDate = remember(completions, selectedDateKey) {
        completions.filter { it.dateKey == selectedDateKey && it.isCompleted }.map { it.taskId }.toSet()
    }

    val totalTaskCount = tasksForSelectedDate.size
    val completedCount = tasksForSelectedDate.count { completedTaskIdsForDate.contains(it.id) }
    val progressFraction = if (totalTaskCount > 0) (completedCount.toFloat() / totalTaskCount.toFloat()).coerceIn(0f, 1f) else 0f
    val progressPct = (progressFraction * 100).toInt()

    val yesterdayKey = remember(todayKey) {
        try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        } catch (e: Exception) { "" }
    }

    val completedTasksForSelectedDate = remember(tasksForSelectedDate, completedTaskIdsForDate) {
        tasksForSelectedDate.filter { completedTaskIdsForDate.contains(it.id) }
    }

    val studiedMinutesForDate = remember(selectedDateKey, dailyStudyLogs, preferences, tasks, completions) {
        viewModel.getStudiedMinutesForDate(selectedDateKey, dailyStudyLogs, preferences, tasks, completions)
    }

    // Weekly, Monthly, and All-Time Study calculations for filtering (Exact Rolling Days)
    val weeklyStudiedMinutes = remember(dailyStudyLogs, preferences, tasks, completions, todayKey) {
        try {
            var total = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (i in 0 until 7) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val key = sdf.format(cal.time)
                total += viewModel.getStudiedMinutesForDate(key, dailyStudyLogs, preferences, tasks, completions)
            }
            total
        } catch (e: Exception) {
            studiedMinutesForDate
        }
    }

    val monthlyStudiedMinutes = remember(dailyStudyLogs, preferences, tasks, completions, todayKey) {
        try {
            var total = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (i in 0 until 30) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val key = sdf.format(cal.time)
                total += viewModel.getStudiedMinutesForDate(key, dailyStudyLogs, preferences, tasks, completions)
            }
            total
        } catch (e: Exception) {
            weeklyStudiedMinutes
        }
    }

    val allTimeStudiedMinutes = remember(dailyStudyLogs, preferences, tasks, completions, todayKey) {
        try {
            val logMins = dailyStudyLogs.sumOf { it.totalFocusedMinutes }
            val todayMins = viewModel.getStudiedMinutesForDate(todayKey, dailyStudyLogs, preferences, tasks, completions)
            val isTodayInLogs = dailyStudyLogs.any { it.dateKey == todayKey }
            val base = if (isTodayInLogs) logMins else logMins + todayMins
            maxOf(base, monthlyStudiedMinutes)
        } catch (e: Exception) {
            monthlyStudiedMinutes
        }
    }

    val todayStudiedMinutes = remember(todayKey, dailyStudyLogs, preferences, tasks, completions) {
        viewModel.getStudiedMinutesForDate(todayKey, dailyStudyLogs, preferences, tasks, completions)
    }

    val todayDayCode = remember(todayKey) {
        viewModel.getDayOfWeekCode(todayKey)
    }

    val todayTasks = remember(tasks, todayDayCode) {
        tasks.filter { it.daysOfWeek.isBlank() || it.daysOfWeek.contains(todayDayCode, ignoreCase = true) }
    }

    val todayTargetMinutes = remember(todayTasks, preferences) {
        if (todayTasks.isNotEmpty()) todayTasks.sumOf { it.durationMinutes }
        else (preferences?.dailyTargetMinutes ?: 120).coerceAtLeast(30)
    }

    var selectedTimeScope by remember { mutableStateOf(FocusTimeScope.SELECTED_DAY) }
    var showTimeScopeDropdown by remember { mutableStateOf(false) }

    val scheduledTotalMinutes = remember(tasksForSelectedDate, preferences) {
        if (tasksForSelectedDate.isNotEmpty()) tasksForSelectedDate.sumOf { it.durationMinutes }
        else preferences?.dailyTargetMinutes ?: 45
    }

    val extraMinutesAboveGoal = remember(studiedMinutesForDate, scheduledTotalMinutes) {
        if (studiedMinutesForDate > scheduledTotalMinutes) studiedMinutesForDate - scheduledTotalMinutes else 0
    }

    val subjectsStudiedForDate = remember(completedTasksForSelectedDate, studiedMinutesForDate, dailyStudyLogs, selectedDateKey) {
        val map = mutableMapOf<String, Int>()
        // 1. Task completions
        completedTasksForSelectedDate.forEach { task ->
            val current = map.getOrDefault(task.subjectName, 0)
            map[task.subjectName] = current + task.durationMinutes
        }
        // 2. Persistent logs breakdown
        val logForDate = dailyStudyLogs.find { it.dateKey == selectedDateKey }
        if (logForDate != null) {
            try {
                val json = org.json.JSONObject(logForDate.subjectsBreakdownJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = json.optInt(k, 0)
                    if (v > 0) {
                        val existing = map.getOrDefault(k, 0)
                        map[k] = maxOf(existing, v)
                    }
                }
            } catch (e: Exception) {
                // Ignore parse fallback
            }
        }
        if (map.isEmpty() && studiedMinutesForDate > 0) {
            map["Focus Mode"] = studiedMinutesForDate
        }
        map
    }


    // Handle Active Focus Session system effects (App Pinning & Audio Mode)
    LaunchedEffect(activeFocusSession.isActive, activeFocusSession.isPaused) {
        if (activeFocusSession.isActive) {
            if (!activeFocusSession.isPaused) {
                // Apply Audio Mode
                if (dnd) {
                    val mode = if (notifBehavior == "SILENT") FocusAudioMode.SILENT else FocusAudioMode.VIBRATE
                    PrepOSFocusController.setPhoneAudioMode(context, mode)
                }
                // Apply Screen Pinning
                if (appPinning) {
                    PrepOSAppPinningManager.pinApplication(context)
                }
            } else {
                // When paused, temporarily return to normal audio
                PrepOSFocusController.setPhoneAudioMode(context, FocusAudioMode.NORMAL)
            }
        }
    }

    // Colors
    val isDark = isAppDarkTheme()
    val pageBackground = MaterialTheme.colorScheme.background
    val cardSurface = if (isDark) AppDarkSurface else LightSurface
    val cardBorder = if (isDark) AppDarkBorder else LightBorder
    val textPrimary = if (isDark) AppDarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDark) AppDarkTextSecondary else LightTextSecondary
    val accentPurple = if (isDark) Color(0xFFA855F7) else BrandPrimary
    val accentGreen = Color(0xFF10B981)

    Scaffold(
        containerColor = pageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("focus_hub_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "PrepOS Focus Hub",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = textPrimary
                                )
                            )
                            Text(
                                text = "Continuous Study Workspace",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textSecondary,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsModal = true },
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Focus Settings",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageBackground
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBackground)
            ) {
                if (activeFocusSession.isActive) {
                    LiveFocusTimerPanel(
                        session = activeFocusSession,
                        onExpand = { },
                        onPause = { viewModel.pauseFocusSession() },
                        onResume = { viewModel.resumeFocusSession() },
                        onStop = { viewModel.stopFocusSession() }
                    )
                }
                PrepOSBottomNavBar(
                    selectedTab = "plan",
                    onSelectTab = { tab ->
                        if (tab != "plan") {
                            onNavigateTab(tab)
                        }
                    },
                    onOpenAskAI = onOpenAskAI
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Top Rank & Identity Progression Card (Immediately below Header)
                item {
                    MasterRankIdentityCard(
                        progression = progressionOverview,
                        todayStudiedMinutes = todayStudiedMinutes,
                        todayTargetMinutes = todayTargetMinutes,
                        onOpenTasks = {
                            tasksSheetInitialTab = 0
                            showTasksDetailSheet = true
                        },
                        onOpenRoadmap = {
                            tasksSheetInitialTab = 1
                            showTasksDetailSheet = true
                        },
                        onStreakClick = { showStreakMilestonesDialog = true },
                        timeScope = selectedTimeScope,
                        scopeStudyMinutes = when (selectedTimeScope) {
                            FocusTimeScope.SELECTED_DAY -> studiedMinutesForDate
                            FocusTimeScope.THIS_WEEK -> weeklyStudiedMinutes
                            FocusTimeScope.THIS_MONTH -> monthlyStudiedMinutes
                            FocusTimeScope.ALL_TIME -> allTimeStudiedMinutes
                        },
                        onSelectTimeScope = { selectedTimeScope = it }
                    )
                }

                // 2. Compact Interactive Calendar Date Strip (Auto-syncs top cards on click)
                item {
                    Column {
                        val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentMonthYear,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentPurple,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.5.sp
                                )
                            )

                            if (!isViewingToday) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF312E81) else GoldAccentSoft,
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF6366F1) else GoldAccentBorderSubtle),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.selectToday()
                                            selectedTimeScope = FocusTimeScope.SELECTED_DAY
                                        }
                                        .testTag("jump_to_today_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFC084FC) else GoldAccentDark,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else GoldAccentDark,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysList.forEach { day ->
                                val isSelected = day.dateKey == selectedDateKey
                                val hasCompletedForDay = completions.any { it.dateKey == day.dateKey && it.isCompleted }
                                CalendarDayPill(
                                    day = day.copy(hasCompleted = hasCompletedForDay),
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.selectDateKey(day.dateKey)
                                        selectedTimeScope = FocusTimeScope.SELECTED_DAY
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Today's Plan & Task Progress Section
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    isViewingToday -> "TODAY'S PLAN • ${currentDayMeta.dayName}, ${currentDayMeta.dayNumber}"
                                    isViewingPast -> "PAST HISTORY • ${currentDayMeta.dayName}, ${currentDayMeta.dayNumber}"
                                    else -> "FUTURE PLAN • ${currentDayMeta.dayName}, ${currentDayMeta.dayNumber}"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isViewingToday) accentPurple else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val idx = daysList.indexOfFirst { it.dateKey == selectedDateKey }
                                        if (idx > 0) {
                                            viewModel.selectDateKey(daysList[idx - 1].dateKey)
                                            selectedTimeScope = FocusTimeScope.SELECTED_DAY
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous Day",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val idx = daysList.indexOfFirst { it.dateKey == selectedDateKey }
                                        if (idx < daysList.size - 1) {
                                            viewModel.selectDateKey(daysList[idx + 1].dateKey)
                                            selectedTimeScope = FocusTimeScope.SELECTED_DAY
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next Day",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Completion Progress Bar with Standardized 5-Level Progress Colors
                        val progressThemeColor = getFocusProgressColor(progressPct)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = if (completedCount > 0) progressThemeColor else Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (totalTaskCount > 0) {
                                        "$completedCount of $totalTaskCount tasks completed ${if (isViewingToday) "today" else "for this date"}"
                                    } else {
                                        "0 tasks scheduled"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (completedCount > 0) progressThemeColor else Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$progressPct%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = progressThemeColor,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Standardized progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(progressThemeColor.copy(alpha = 0.75f), progressThemeColor)
                                        )
                                    )
                            )
                        }

                        // Compact Subjects Covered Row (if subjects studied on this date)
                        if (subjectsStudiedForDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Subjects Covered",
                                        tint = if (isDark) accentPurple else GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Studied: ",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    subjectsStudiedForDate.forEach { (subName, mins) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0xFF1E293B) else GoldAccentLight,
                                            border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else GoldAccentBorder)
                                        ) {
                                            Text(
                                                text = "$subName (${mins}m)",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 10.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // 4. Study Tasks List
                if (tasksForSelectedDate.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardSurface),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isViewingToday) "No study tasks scheduled for Today" else "No tasks scheduled for ${currentDayMeta.dayName}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        fontSize = 13.5.sp
                                    )
                                )
                                Text(
                                    text = "Add tasks to your timetable to track habits and focus routines.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuickTaskPresetChip(
                                        label = "+ 45m Theory",
                                        onClick = {
                                            viewModel.createStudyTask(
                                                subjectName = "Study",
                                                taskTitle = "Theory & Notes Reading",
                                                taskType = "READING",
                                                subDetails = "Focus reading chapter",
                                                startTime = "7:00 PM",
                                                endTime = "7:45 PM",
                                                durationMinutes = 45,
                                                daysOfWeek = selectedDayOfWeek
                                            )
                                        }
                                    )
                                    QuickTaskPresetChip(
                                        label = "+ 30m Practice",
                                        onClick = {
                                            viewModel.createStudyTask(
                                                subjectName = "Practice",
                                                taskTitle = "Question Practice & Quiz",
                                                taskType = "QUIZ",
                                                subDetails = "Solve MCQs and problems",
                                                startTime = "8:00 PM",
                                                endTime = "8:30 PM",
                                                durationMinutes = 30,
                                                daysOfWeek = selectedDayOfWeek
                                            )
                                        }
                                    )
                                    QuickTaskPresetChip(
                                        label = "+ 20m Revision",
                                        onClick = {
                                            viewModel.createStudyTask(
                                                subjectName = "Revision",
                                                taskTitle = "Daily Formula & Notes Revision",
                                                taskType = "REVISION",
                                                subDetails = "Active recall of key points",
                                                startTime = "9:00 PM",
                                                endTime = "9:20 PM",
                                                durationMinutes = 20,
                                                daysOfWeek = selectedDayOfWeek
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(tasksForSelectedDate, key = { it.id }) { task ->
                        val isTaskDone = completedTaskIdsForDate.contains(task.id)
                        StudyTaskCard(
                            task = task,
                            isCompleted = isTaskDone,
                            isToday = isViewingToday,
                            isPast = isViewingPast,
                            isFuture = isViewingFuture,
                            onToggleComplete = { done ->
                                viewModel.toggleTaskCompletion(task.id, selectedDateKey, done)
                            },
                            onStartFocus = {
                                if (isViewingToday) {
                                    viewModel.startFocusSession(task)
                                } else {
                                    Toast.makeText(context, "Focus sessions are active on Today's scheduled plan", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                taskPendingDelete = task
                            },
                            activeFocusSession = activeFocusSession,
                            onPauseFocus = { viewModel.pauseFocusSession() },
                            onResumeFocus = { viewModel.resumeFocusSession() },
                            onStopFocus = { viewModel.stopFocusSession() }
                        )
                    }
                }

                // 5. + Add Task Button
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clickable { showAddTaskDialog = true }
                            .testTag("add_task_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else LightSurface,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Task",
                                tint = if (isDark) Color(0xFFA855F7) else GoldAccentDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Add Task to Timetable",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC084FC) else GoldAccentDark,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // Tasks Detail Sheet (from [☷ Tasks] or Rank Crest on Rank Card)
            if (showTasksDetailSheet) {
                TasksDetailSheet(
                    todayTasks = todayTasks,
                    completedTaskIds = completedTaskIdsForDate,
                    todayStudiedMinutes = todayStudiedMinutes,
                    todayTargetMinutes = todayTargetMinutes,
                    progression = progressionOverview,
                    initialTab = tasksSheetInitialTab,
                    onToggleTaskComplete = { taskId, done ->
                        viewModel.toggleTaskCompletion(taskId, todayKey, done)
                    },
                    onStartFocus = { task ->
                        viewModel.startFocusSession(task)
                    },
                    onAddTask = { showAddTaskDialog = true },
                    onOpenRoadmap = {
                        tasksSheetInitialTab = 1
                        showTasksDetailSheet = true
                    },
                    onDismiss = { showTasksDetailSheet = false }
                )
            }

            // Streak & Milestones Info Dialog (from clicking Streak on Rank Card)
            if (showStreakMilestonesDialog) {
                MilestonesDialog(
                    streak = progressionOverview.streakCount,
                    onDismiss = { showStreakMilestonesDialog = false }
                )
            }

            // Progression Roadmap & Badges Dialog
            if (showRoadmapModal) {
                ProgressionDetailsModal(
                    progression = progressionOverview,
                    onDismiss = { showRoadmapModal = false }
                )
            }

            // 7. Focus Mode Settings Popup
            if (showSettingsModal) {
                FocusModeSettingsPopup(
                    appPinning = appPinning,
                    onToggleAppPinning = {
                        viewModel.updateFocusSettings(
                            appPinning = it,
                            dnd = dnd,
                            notificationBehaviour = notifBehavior,
                            timerMinutes = timerMins,
                            followTimetable = followTimetable
                        )
                    },
                    dnd = dnd,
                    onToggleDnd = { enabled ->
                        if (enabled && !PrepOSFocusController.isDndAccessGranted(context)) {
                            PrepOSFocusController.checkAndRequestDndPermission(context)
                        }
                        viewModel.updateFocusSettings(
                            appPinning = appPinning,
                            dnd = enabled,
                            notificationBehaviour = notifBehavior,
                            timerMinutes = timerMins,
                            followTimetable = followTimetable
                        )
                    },
                    onRequestDndPermission = {
                        PrepOSFocusController.checkAndRequestDndPermission(context)
                    },
                    notificationBehaviour = notifBehavior,
                    onOpenNotificationBehavior = { showNotificationBehaviorDialog = true },
                    timerMinutes = timerMins,
                    onOpenTimerPresets = { showTimerPresetDialog = true },
                    followTimetable = followTimetable,
                    onToggleFollowTimetable = {
                        viewModel.updateFocusSettings(
                            appPinning = appPinning,
                            dnd = dnd,
                            notificationBehaviour = notifBehavior,
                            timerMinutes = timerMins,
                            followTimetable = it
                        )
                    },
                    onDismiss = { showSettingsModal = false }
                )
            }
        }
    }

    // Interrupted / Overnight Session Recovery Dialog
    if (activeFocusSession.isInterrupted) {
        val elapsedMins = (activeFocusSession.recoverableElapsedSeconds / 60).coerceAtLeast(1)
        val plannedMins = activeFocusSession.totalSeconds / 60
        AlertDialog(
            onDismissRequest = { viewModel.discardInterruptedSession() },
            containerColor = Color(0xFF1E1B4B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Session Interrupted",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus Session Interrupted",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your deep focus session for '${activeFocusSession.taskTitle}' was paused or closed.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1))
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "⏱️ Detected elapsed time: $elapsedMins min",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "🎯 Planned target: $plannedMins min",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                            )
                        }
                    }
                    Text(
                        text = "How would you like to proceed?",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resumeInterruptedSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Resume Session", color = Color.White)
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

    // Modal: Timer Preset Selector
    if (showTimerPresetDialog) {
        TimerPresetDialog(
            currentMinutes = timerMins,
            onSelect = { selectedMin ->
                viewModel.updateFocusSettings(
                    appPinning = appPinning,
                    dnd = dnd,
                    notificationBehaviour = notifBehavior,
                    timerMinutes = selectedMin,
                    followTimetable = followTimetable
                )
                showTimerPresetDialog = false
            },
            onDismiss = { showTimerPresetDialog = false }
        )
    }

    // Modal: Notification Behavior Selector
    if (showNotificationBehaviorDialog) {
        NotificationBehaviorDialog(
            currentBehavior = notifBehavior,
            onSelect = { selected ->
                viewModel.updateFocusSettings(
                    appPinning = appPinning,
                    dnd = dnd,
                    notificationBehaviour = selected,
                    timerMinutes = timerMins,
                    followTimetable = followTimetable
                )
                showNotificationBehaviorDialog = false
            },
            onDismiss = { showNotificationBehaviorDialog = false }
        )
    }

    // Modal: Delete Task Confirmation Dialog
    if (taskPendingDelete != null) {
        val taskToDelete = taskPendingDelete!!
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = {
                Text(
                    text = "Delete Study Task?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${taskToDelete.taskTitle}\" (${taskToDelete.subjectName}) from your schedule?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1))
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudyTask(taskToDelete.id)
                        taskPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal: Add New Task
    if (showAddTaskDialog) {
        AddTaskDialog(
            availableSubjects = availableSubjectNames,
            initialDay = selectedDayOfWeek,
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { subject, title, details, start, end, duration, days, repeat ->
                viewModel.createStudyTask(
                    subjectName = subject,
                    taskTitle = title,
                    taskType = "READING",
                    subDetails = details,
                    startTime = start,
                    endTime = end,
                    durationMinutes = duration,
                    daysOfWeek = days,
                    repeatWeekly = repeat
                )
                showAddTaskDialog = false
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Sub-Composables
// -------------------------------------------------------------------------------------------------

data class DateItem(
    val dateKey: String,
    val dayNumber: String,
    val dayName: String,
    val hasCompleted: Boolean = false,
    val isToday: Boolean = false
)

@Composable
fun CalendarDayPill(
    day: DateItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val pillBg = when {
        isSelected -> if (isDark) Color(0xFF6366F1) else GoldAccent
        day.isToday -> if (isDark) Color(0xFF1E1B4B) else GoldAccentSoft
        else -> if (isDark) Color(0xFF131826) else LightSurface
    }
    val pillBorder = when {
        isSelected -> if (isDark) Color(0xFF818CF8) else GoldAccentDark
        day.isToday -> if (isDark) Color(0xFFA855F7) else GoldAccentBorder
        else -> if (isDark) Color(0xFF1E293B) else LightBorder
    }

    Surface(
        modifier = Modifier
            .width(54.dp)
            .height(68.dp)
            .clickable { onClick() }
            .testTag("calendar_day_${day.dateKey}"),
        shape = RoundedCornerShape(12.dp),
        color = pillBg,
        border = BorderStroke(if (day.isToday && !isSelected) 1.5.dp else 1.dp, pillBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.dayNumber,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> Color.White
                        day.isToday -> if (isDark) Color(0xFFC084FC) else GoldAccentDark
                        else -> if (isDark) Color.White else LightTextPrimary
                    },
                    fontSize = 15.sp
                )
            )
            Text(
                text = if (day.isToday) "TODAY" else day.dayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> Color.White
                        day.isToday -> if (isDark) Color(0xFFE9D5FF) else GoldAccent
                        else -> if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                    },
                    fontSize = 8.5.sp
                )
            )
            // Status Dot
            Box(
                modifier = Modifier.size(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (day.hasCompleted) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                } else if (day.isToday) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFFF59E0B) else GoldAccent)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTaskPresetChip(
    label: String,
    onClick: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF1E1B4B) else GoldAccentLight,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else GoldAccentBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFC084FC) else GoldAccentDark,
                fontSize = 10.5.sp
            )
        )
    }
}

@Composable
fun StudyTaskCard(
    task: StudyTaskEntity,
    isCompleted: Boolean,
    isToday: Boolean,
    isPast: Boolean,
    isFuture: Boolean,
    onToggleComplete: (Boolean) -> Unit = {},
    onStartFocus: () -> Unit,
    onDelete: () -> Unit,
    activeFocusSession: com.example.viewmodel.ActiveFocusSession? = null,
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    onStopFocus: () -> Unit = {}
) {
    val isDark = isAppDarkTheme()
    val (iconColor, iconVector) = when (task.taskType) {
        "READING" -> Color(0xFF10B981) to Icons.Default.MenuBook
        "QUIZ" -> (if (isDark) Color(0xFFF59E0B) else GoldAccent) to Icons.Default.Assignment
        "TOPIC" -> (if (isDark) Color(0xFF8B5CF6) else BrandPrimary) to Icons.Default.Laptop
        "REVISION" -> Color(0xFF3B82F6) to Icons.Default.Edit
        "MOCK" -> Color(0xFFEC4899) to Icons.Default.Adjust
        else -> Color(0xFF10B981) to Icons.Default.MenuBook
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                if (isCompleted) Color(0xFF0F1420) else Color(0xFF131826)
            } else {
                if (isCompleted) LightSurfaceSecondary.copy(alpha = 0.7f) else LightSurface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) {
                if (isCompleted) Color(0xFF1A2234) else Color(0xFF1F293D)
            } else {
                if (isCompleted) LightBorder.copy(alpha = 0.6f) else GoldAccentBorderSubtle
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task Type / Completion Status Circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) {
                            if (isDark) Color(0xFF064E3B) else Color(0xFFE8F7F1)
                        } else {
                            iconColor.copy(alpha = if (isPast) 0.08f else 0.15f)
                        }
                    )
                    .border(
                        1.dp,
                        if (isCompleted) Color(0xFF10B981)
                        else iconColor.copy(alpha = if (isPast) 0.2f else 0.35f),
                        CircleShape
                    )
                    .clickable { onToggleComplete(!isCompleted) }
                    .testTag("task_toggle_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = task.taskTitle,
                        tint = if (isPast) iconColor.copy(alpha = 0.6f) else iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.taskTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) {
                            if (isDark) Color(0xFF64748B) else LightTextMuted
                        } else {
                            if (isDark) Color.White else LightTextPrimary
                        },
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.subjectName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isCompleted) {
                                if (isDark) Color(0xFF475569) else LightTextDisabled
                            } else {
                                if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDark) Color(0xFF475569) else LightBorderStrong,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = if (task.startTime == "Anytime" || task.endTime.isBlank()) "${task.durationMinutes}m" else "${task.startTime} (${task.durationMinutes}m)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isCompleted) {
                                if (isDark) Color(0xFF475569) else LightTextDisabled
                            } else if (isPast) {
                                iconColor.copy(alpha = 0.5f)
                            } else {
                                iconColor
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Focus Mode Button / State
            if (isToday) {
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFE8F7F1),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onToggleComplete(false) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                } else {
                    val isThisTaskActive = activeFocusSession?.isActive == true && activeFocusSession.associatedTaskId == task.id
                    if (isThisTaskActive && activeFocusSession != null) {
                        val displayTime = if (activeFocusSession.isTimerMode) {
                            val rem = activeFocusSession.remainingSeconds
                            String.format("%02d:%02d", rem / 60, rem % 60)
                        } else {
                            val el = activeFocusSession.elapsedSeconds
                            String.format("%02d:%02d", el / 60, el % 60)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) {
                                if (activeFocusSession.isPaused) Color(0xFF78350F) else Color(0xFF311042)
                            } else {
                                if (activeFocusSession.isPaused) GoldAccentSoft else GoldAccentLight
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isDark) {
                                    if (activeFocusSession.isPaused) Color(0xFFF59E0B) else Color(0xFFA855F7)
                                } else {
                                    GoldAccentBorder
                                }
                            ),
                            modifier = Modifier
                                .clickable {
                                    if (activeFocusSession.isPaused) onResumeFocus() else onPauseFocus()
                                }
                                .testTag("task_focus_button_${task.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (activeFocusSession.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (activeFocusSession.isPaused) "Resume" else "Pause",
                                    tint = if (isDark) {
                                        if (activeFocusSession.isPaused) Color(0xFFFBBF24) else Color(0xFFC084FC)
                                    } else {
                                        GoldAccentDark
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${if (activeFocusSession.isTimerMode) "⏳" else "⏱️"} $displayTime",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) {
                                            if (activeFocusSession.isPaused) Color(0xFFFBBF24) else Color(0xFFF3E8FF)
                                        } else {
                                            GoldAccentDark
                                        },
                                        fontSize = 10.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onStopFocus() }
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E1B4B) else GoldAccentLight,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.4f) else GoldAccentBorder),
                            modifier = Modifier
                                .clickable { onStartFocus() }
                                .testTag("task_focus_button_${task.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Adjust,
                                    contentDescription = "Focus",
                                    tint = if (isDark) Color(0xFFC084FC) else GoldAccentDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Focus",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFE0E7FF) else GoldAccentDark,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            } else if (isPast) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else LightSurfaceSecondary,
                    border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else LightBorder)
                ) {
                    Text(
                        text = if (isCompleted) "Done" else "Missed",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color(0xFF10B981) else (if (isDark) Color(0xFF64748B) else LightTextMuted),
                            fontSize = 10.sp
                        )
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.4f) else GoldAccentSoft,
                    border = BorderStroke(0.8.dp, if (isDark) Color(0xFF4338CA).copy(alpha = 0.4f) else GoldAccentBorderSubtle)
                ) {
                    Text(
                        text = "Planned",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF818CF8) else GoldAccentDark,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete Task Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = if (isDark) Color(0xFF64748B) else LightTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Your Journey • Achievements Progression System (Genuine Consistency, No Subscriptions)
// -------------------------------------------------------------------------------------------------

enum class MilestoneState {
    COMPLETED,
    NEXT_MILESTONE,
    LOCKED
}

enum class MilestoneTierTheme(
    val rankTitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val darkBgColor: Color,
    val glowColor: Color,
    val shapeType: Int // 1: Bronze Hexagon, 2: Silver Chevron Dial, 3: Gold Octagon Crest, 4: Platinum Aegis, 5: Diamond Cyber, 6: Elite Obsidian, 7: Legendary Sunburst
) {
    TIER_3_DAYS(
        rankTitle = "Bronze Scout",
        primaryColor = Color(0xFFCD7F32),
        secondaryColor = Color(0xFFFED7AA),
        darkBgColor = Color(0xFF261506),
        glowColor = Color(0xFFB45309),
        shapeType = 1
    ),
    TIER_7_DAYS(
        rankTitle = "Silver Vanguard",
        primaryColor = Color(0xFFCBD5E1),
        secondaryColor = Color(0xFFF8FAFC),
        darkBgColor = Color(0xFF0F172A),
        glowColor = Color(0xFF94A3B8),
        shapeType = 2
    ),
    TIER_14_DAYS(
        rankTitle = "Gold Sentinel",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFFEF3C7),
        darkBgColor = Color(0xFF261704),
        glowColor = Color(0xFFD97706),
        shapeType = 3
    ),
    TIER_21_DAYS(
        rankTitle = "Platinum Titan",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFFCFFAFE),
        darkBgColor = Color(0xFF082F49),
        glowColor = Color(0xFF0891B2),
        shapeType = 4
    ),
    TIER_30_DAYS(
        rankTitle = "Diamond Warden",
        primaryColor = Color(0xFF818CF8),
        secondaryColor = Color(0xFFE0E7FF),
        darkBgColor = Color(0xFF1E1B4B),
        glowColor = Color(0xFF6366F1),
        shapeType = 5
    ),
    TIER_100_DAYS(
        rankTitle = "Elite Centurion",
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFFFE4E6),
        darkBgColor = Color(0xFF3F0713),
        glowColor = Color(0xFFE11D48),
        shapeType = 6
    ),
    TIER_365_DAYS(
        rankTitle = "Legendary Monarch",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFFBCFE8),
        darkBgColor = Color(0xFF2A0845),
        glowColor = Color(0xFFD97706),
        shapeType = 7
    )
}

data class MilestoneItem(
    val days: Int,
    val rankTitle: String,
    val tierTheme: MilestoneTierTheme,
    val state: MilestoneState,
    val daysLeft: Int,
    val progressFraction: Float
)

@Composable
fun JourneyAchievementsSection(
    streakCount: Int,
    onTestStreak: (Int) -> Unit = {}
) {
    val milestoneDefinitions = listOf(
        Pair(3, MilestoneTierTheme.TIER_3_DAYS),
        Pair(7, MilestoneTierTheme.TIER_7_DAYS),
        Pair(14, MilestoneTierTheme.TIER_14_DAYS),
        Pair(21, MilestoneTierTheme.TIER_21_DAYS),
        Pair(30, MilestoneTierTheme.TIER_30_DAYS),
        Pair(100, MilestoneTierTheme.TIER_100_DAYS),
        Pair(365, MilestoneTierTheme.TIER_365_DAYS)
    )

    val nextMilestoneDay = milestoneDefinitions.firstOrNull { streakCount < it.first }?.first

    val milestones = milestoneDefinitions.mapIndexed { index, (days, tierTheme) ->
        val state = when {
            streakCount >= days -> MilestoneState.COMPLETED
            days == nextMilestoneDay -> MilestoneState.NEXT_MILESTONE
            else -> MilestoneState.LOCKED
        }

        val prevDays = if (index > 0) milestoneDefinitions[index - 1].first else 0
        val daysLeft = (days - streakCount).coerceAtLeast(1)
        val progress = if (state == MilestoneState.COMPLETED) 1f
        else if (state == MilestoneState.NEXT_MILESTONE) {
            ((streakCount - prevDays).toFloat() / (days - prevDays).toFloat()).coerceIn(0f, 1f)
        } else 0f

        MilestoneItem(
            days = days,
            rankTitle = tierTheme.rankTitle,
            tierTheme = tierTheme,
            state = state,
            daysLeft = daysLeft,
            progressFraction = progress
        )
    }

    val isDark = isAppDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        // Section Header: Clean "YOUR JOURNEY"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (isDark) Color(0xFFC084FC) else GoldAccentDark,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "YOUR JOURNEY",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else LightTextPrimary,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Milestone Progression Track (Left to Right) - Boundary-Free Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            milestones.forEachIndexed { index, milestone ->
                MilestoneBadgeItem(milestone = milestone)

                // Connector path between badges
                if (index < milestones.lastIndex) {
                    val isPathActive = milestone.state == MilestoneState.COMPLETED
                    MilestoneTrackConnector(isActive = isPathActive)
                }
            }
        }
    }
}

@Composable
fun MilestoneTrackConnector(isActive: Boolean) {
    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(
                    if (isActive) Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFFA855F7)))
                    else Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                )
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFFA855F7) else Color(0xFF475569))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(
                    if (isActive) Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED)))
                    else Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF334155)))
                )
        )
    }
}

@Composable
fun MilestoneBadgeItem(milestone: MilestoneItem) {
    val isDark = isAppDarkTheme()
    Column(
        modifier = Modifier
            .width(115.dp)
            .testTag("milestone_badge_${milestone.days}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Center Artwork Shield / Crest (Boundary-Free)
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            when (milestone.state) {
                MilestoneState.COMPLETED -> {
                    CompletedRankShieldBadge(days = milestone.days, theme = milestone.tierTheme)
                }
                MilestoneState.NEXT_MILESTONE -> {
                    NextMilestoneCyberDialBadge(days = milestone.days, theme = milestone.tierTheme)
                }
                MilestoneState.LOCKED -> {
                    LockedTierShieldBadge(days = milestone.days, theme = milestone.tierTheme)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Rank Name
        Text(
            text = milestone.rankTitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = when (milestone.state) {
                    MilestoneState.COMPLETED -> if (isDark) Color.White else LightTextPrimary
                    MilestoneState.NEXT_MILESTONE -> if (isDark) Color(0xFFF1F5F9) else LightTextPrimary
                    MilestoneState.LOCKED -> if (isDark) Color(0xFF64748B) else LightTextMuted
                },
                fontSize = 11.5.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 3. Days Subtitle
        Text(
            text = "${milestone.days} Days Streak",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = when (milestone.state) {
                    MilestoneState.COMPLETED -> milestone.tierTheme.secondaryColor
                    MilestoneState.NEXT_MILESTONE -> Color(0xFFC084FC)
                    MilestoneState.LOCKED -> Color(0xFF475569)
                },
                fontSize = 9.5.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Status Indicator Pill
        when (milestone.state) {
            MilestoneState.COMPLETED -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = milestone.tierTheme.primaryColor.copy(alpha = 0.2f),
                    border = BorderStroke(0.8.dp, milestone.tierTheme.primaryColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = milestone.tierTheme.secondaryColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "UNLOCKED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = milestone.tierTheme.secondaryColor,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
            MilestoneState.NEXT_MILESTONE -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF7C3AED).copy(alpha = 0.25f),
                    border = BorderStroke(0.8.dp, Color(0xFFA855F7))
                ) {
                    Text(
                        text = "${milestone.daysLeft}D LEFT",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE9D5FF),
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
            MilestoneState.LOCKED -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.4f),
                    border = BorderStroke(0.8.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "LOCKED",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Specialized Vector & Canvas Shield Badges
// -------------------------------------------------------------------------------------------------

@Composable
fun CompletedRankShieldBadge(days: Int, theme: MilestoneTierTheme) {
    Box(
        modifier = Modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Ambient sparkling particles
            val particles = listOf(
                Offset(w * 0.12f, h * 0.20f),
                Offset(w * 0.88f, h * 0.18f),
                Offset(w * 0.08f, h * 0.62f),
                Offset(w * 0.92f, h * 0.58f),
                Offset(w * 0.22f, h * 0.86f),
                Offset(w * 0.78f, h * 0.84f)
            )
            particles.forEach { pos ->
                drawCircle(
                    color = theme.secondaryColor,
                    radius = 1.8.dp.toPx(),
                    center = pos
                )
            }

            // 2. Distinctive authentic shape geometries per tier
            when (theme.shapeType) {
                // 1: BRONZE (Round Bronze Medallion with Outer Rivets)
                1 -> {
                    val center = Offset(w * 0.5f, h * 0.5f)
                    val r = w * 0.38f

                    // Outer Bronze Coin Rim
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFCD7F32), Color(0xFF78350F), Color(0xFF261506)),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFED7AA), Color(0xFFB45309))),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Inner Concentric Ring
                    drawCircle(
                        color = Color(0xFFFED7AA).copy(alpha = 0.4f),
                        radius = r * 0.78f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    // 6 Bronze Perimeter Rivet Studs
                    for (i in 0 until 6) {
                        val angle = Math.toRadians(i * 60.0)
                        val rivetR = r * 0.88f
                        val rivetPos = Offset(
                            center.x + (rivetR * Math.cos(angle)).toFloat(),
                            center.y + (rivetR * Math.sin(angle)).toFloat()
                        )
                        drawCircle(color = Color(0xFFFED7AA), radius = 1.6.dp.toPx(), center = rivetPos)
                    }
                }

                // 2: SILVER (V-Winged Knight's Shield with Flared Chevrons)
                2 -> {
                    val shieldPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.88f, h * 0.16f)
                        lineTo(w * 0.84f, h * 0.60f)
                        lineTo(w * 0.5f, h * 0.94f)
                        lineTo(w * 0.16f, h * 0.60f)
                        lineTo(w * 0.12f, h * 0.16f)
                        close()
                    }
                    drawPath(
                        path = shieldPath,
                        brush = Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF0F172A))),
                        style = Fill
                    )
                    drawPath(
                        path = shieldPath,
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF64748B))),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Silver Inner Chevron V-Trim
                    val chevron = Path().apply {
                        moveTo(w * 0.26f, h * 0.30f)
                        lineTo(w * 0.5f, h * 0.46f)
                        lineTo(w * 0.74f, h * 0.30f)
                    }
                    drawPath(
                        path = chevron,
                        color = Color(0xFFF8FAFC).copy(alpha = 0.6f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // 3: GOLD (Royal Octagonal Gold Fortress Crest)
                3 -> {
                    val path = Path().apply {
                        moveTo(w * 0.34f, h * 0.10f)
                        lineTo(w * 0.66f, h * 0.10f)
                        lineTo(w * 0.88f, h * 0.32f)
                        lineTo(w * 0.88f, h * 0.68f)
                        lineTo(w * 0.66f, h * 0.90f)
                        lineTo(w * 0.34f, h * 0.90f)
                        lineTo(w * 0.12f, h * 0.68f)
                        lineTo(w * 0.12f, h * 0.32f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF92400E), Color(0xFF261704))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // 4 Golden Corner Studs
                    listOf(
                        Offset(w * 0.34f, h * 0.10f),
                        Offset(w * 0.66f, h * 0.10f),
                        Offset(w * 0.66f, h * 0.90f),
                        Offset(w * 0.34f, h * 0.90f)
                    ).forEach { pt ->
                        drawCircle(color = Color(0xFFFEF3C7), radius = 1.8.dp.toPx(), center = pt)
                    }
                }

                // 4: PLATINUM (Triple-Towered Aegis Crown Shield)
                4 -> {
                    val path = Path().apply {
                        moveTo(w * 0.16f, h * 0.16f)
                        lineTo(w * 0.34f, h * 0.22f)
                        lineTo(w * 0.50f, h * 0.10f)
                        lineTo(w * 0.66f, h * 0.22f)
                        lineTo(w * 0.84f, h * 0.16f)
                        lineTo(w * 0.86f, h * 0.60f)
                        lineTo(w * 0.50f, h * 0.92f)
                        lineTo(w * 0.14f, h * 0.60f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF0E7490), Color(0xFF082F49))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFFCFFAFE), Color(0xFF06B6D4))),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // 5: DIAMOND (Faceted Brilliant Cut Diamond Gem)
                5 -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.88f, h * 0.36f)
                        lineTo(w * 0.74f, h * 0.84f)
                        lineTo(w * 0.5f, h * 0.94f)
                        lineTo(w * 0.26f, h * 0.84f)
                        lineTo(w * 0.12f, h * 0.36f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF3730A3), Color(0xFF1E1B4B))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFFE0E7FF), Color(0xFF818CF8), Color(0xFF38BDF8))),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Facet Lines
                    drawLine(
                        color = Color(0xFFE0E7FF).copy(alpha = 0.5f),
                        start = Offset(w * 0.12f, h * 0.36f),
                        end = Offset(w * 0.88f, h * 0.36f),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 6: ELITE (Winged Obsidian & Ruby Shield)
                6 -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.90f, h * 0.20f)
                        lineTo(w * 0.80f, h * 0.65f)
                        lineTo(w * 0.5f, h * 0.92f)
                        lineTo(w * 0.20f, h * 0.65f)
                        lineTo(w * 0.10f, h * 0.20f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF881337), Color(0xFF3F0713))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFE4E6), Color(0xFFF43F5E), Color(0xFFE11D48))),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // 7: LEGENDARY (12-Ray Cosmic Celestial Corona Sunburst)
                else -> {
                    val center = Offset(w * 0.5f, h * 0.5f)
                    val radius = w * 0.36f

                    for (i in 0 until 12) {
                        val angle = Math.toRadians(i * 30.0)
                        val r1 = radius * 0.85f
                        val r2 = radius * 1.08f
                        val p1 = Offset(center.x + (r1 * Math.cos(angle)).toFloat(), center.y + (r1 * Math.sin(angle)).toFloat())
                        val p2 = Offset(center.x + (r2 * Math.cos(angle)).toFloat(), center.y + (r2 * Math.sin(angle)).toFloat())
                        drawLine(
                            color = Color(0xFFF59E0B),
                            start = p1,
                            end = p2,
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    drawCircle(
                        brush = Brush.verticalGradient(listOf(Color(0xFF4A044E), Color(0xFF2A0845))),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEC4899))),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            // Bottom Laurel Wreath
            val leftLaurel = Path().apply {
                moveTo(w * 0.20f, h * 0.68f)
                cubicTo(w * 0.15f, h * 0.80f, w * 0.35f, h * 0.94f, w * 0.5f, h * 0.95f)
            }
            val rightLaurel = Path().apply {
                moveTo(w * 0.80f, h * 0.68f)
                cubicTo(w * 0.85f, h * 0.80f, w * 0.65f, h * 0.94f, w * 0.5f, h * 0.95f)
            }
            drawPath(leftLaurel, color = theme.primaryColor, style = Stroke(width = 1.8.dp.toPx()))
            drawPath(rightLaurel, color = theme.primaryColor, style = Stroke(width = 1.8.dp.toPx()))

            listOf(
                Offset(w * 0.18f, h * 0.72f), Offset(w * 0.24f, h * 0.82f), Offset(w * 0.35f, h * 0.90f),
                Offset(w * 0.82f, h * 0.72f), Offset(w * 0.76f, h * 0.82f), Offset(w * 0.65f, h * 0.90f)
            ).forEach { pt ->
                drawCircle(color = theme.secondaryColor, radius = 2.dp.toPx(), center = pt)
            }
        }

        // Top Star & Days Center Typography
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = theme.secondaryColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$days",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 19.sp
                )
            )
            Text(
                text = "DAYS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = theme.secondaryColor,
                    fontSize = 8.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
fun NextMilestoneCyberDialBadge(days: Int, theme: MilestoneTierTheme) {
    Box(
        modifier = Modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.5f)
            val radius = w * 0.36f

            // Inner dark glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF4C1D95).copy(alpha = 0.7f), Color(0xFF1E1035).copy(alpha = 0.2f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer segmented dial
            drawCircle(
                color = Color(0xFF3B0764),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Radiant Active Arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFFC084FC), Color(0xFF7C3AED)),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Segmented Dial Ticks
            val tickCount = 8
            for (i in 0 until tickCount) {
                val angle = Math.toRadians((i * (360.0 / tickCount) - 90.0))
                val innerR = radius - 3.dp.toPx()
                val outerR = radius + 2.dp.toPx()
                val p1 = Offset(center.x + (innerR * Math.cos(angle)).toFloat(), center.y + (innerR * Math.sin(angle)).toFloat())
                val p2 = Offset(center.x + (outerR * Math.cos(angle)).toFloat(), center.y + (outerR * Math.sin(angle)).toFloat())
                drawLine(
                    color = Color(0xFFE9D5FF),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.2.dp.toPx()
                )
            }
        }

        // Top Hourglass & Center Typography
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFFC084FC),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$days",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 19.sp
                )
            )
            Text(
                text = "DAYS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE9D5FF),
                    fontSize = 8.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
fun LockedTierShieldBadge(days: Int, theme: MilestoneTierTheme) {
    Box(
        modifier = Modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Render muted locked silhouette matching the specific tier geometry
            when (theme.shapeType) {
                // 1: BRONZE (Round Medallion)
                1 -> {
                    val center = Offset(w * 0.5f, h * 0.5f)
                    val r = w * 0.36f
                    drawCircle(
                        brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0B1120))),
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF475569),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // 2: SILVER (V-Winged Knight's Shield)
                2 -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.88f, h * 0.16f)
                        lineTo(w * 0.84f, h * 0.60f)
                        lineTo(w * 0.5f, h * 0.94f)
                        lineTo(w * 0.16f, h * 0.60f)
                        lineTo(w * 0.12f, h * 0.16f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFF475569),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // 3: GOLD (Octagonal Crest)
                3 -> {
                    val path = Path().apply {
                        moveTo(w * 0.34f, h * 0.10f)
                        lineTo(w * 0.66f, h * 0.10f)
                        lineTo(w * 0.88f, h * 0.32f)
                        lineTo(w * 0.88f, h * 0.68f)
                        lineTo(w * 0.66f, h * 0.90f)
                        lineTo(w * 0.34f, h * 0.90f)
                        lineTo(w * 0.12f, h * 0.68f)
                        lineTo(w * 0.12f, h * 0.32f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFF475569),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // 4: PLATINUM (Crown Aegis)
                4 -> {
                    val path = Path().apply {
                        moveTo(w * 0.16f, h * 0.16f)
                        lineTo(w * 0.34f, h * 0.22f)
                        lineTo(w * 0.50f, h * 0.10f)
                        lineTo(w * 0.66f, h * 0.22f)
                        lineTo(w * 0.84f, h * 0.16f)
                        lineTo(w * 0.86f, h * 0.60f)
                        lineTo(w * 0.50f, h * 0.92f)
                        lineTo(w * 0.14f, h * 0.60f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFF475569),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // 5: DIAMOND (Diamond Gem)
                5 -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.88f, h * 0.36f)
                        lineTo(w * 0.74f, h * 0.84f)
                        lineTo(w * 0.5f, h * 0.94f)
                        lineTo(w * 0.26f, h * 0.84f)
                        lineTo(w * 0.12f, h * 0.36f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFF475569),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // 6: ELITE (Winged Crest)
                6 -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.90f, h * 0.20f)
                        lineTo(w * 0.80f, h * 0.65f)
                        lineTo(w * 0.5f, h * 0.92f)
                        lineTo(w * 0.20f, h * 0.65f)
                        lineTo(w * 0.10f, h * 0.20f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFF475569),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // 7: LEGENDARY (Sunburst)
                else -> {
                    val center = Offset(w * 0.5f, h * 0.5f)
                    val radius = w * 0.36f
                    drawCircle(
                        brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF475569),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
            }
        }

        // Center Prominent Lock & Typography
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$days",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp
                )
            )
            Text(
                text = "DAYS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    fontSize = 7.5.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Focus Mode Settings Popup with Direct DND & Audio Permission Integration
// -------------------------------------------------------------------------------------------------

@Composable
fun FocusModeSettingsPopup(
    appPinning: Boolean,
    onToggleAppPinning: (Boolean) -> Unit,
    dnd: Boolean,
    onToggleDnd: (Boolean) -> Unit,
    onRequestDndPermission: () -> Unit,
    notificationBehaviour: String,
    onOpenNotificationBehavior: () -> Unit,
    timerMinutes: Int,
    onOpenTimerPresets: () -> Unit,
    followTimetable: Boolean,
    onToggleFollowTimetable: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDndGranted = PrepOSFocusController.isDndAccessGranted(context)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() }
                .padding(horizontal = 16.dp, vertical = 28.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clickable(enabled = false) {}
                    .testTag("focus_mode_settings_popup"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
                border = BorderStroke(1.2.dp, Color(0xFF3B2F5F))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header: Title and Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FOCUS MODE SETTINGS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC084FC),
                                letterSpacing = 0.5.sp,
                                fontSize = 12.sp
                            )
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DND Permission Access Banner (Crucial for Samsung/Xiaomi name visibility)
                    if (!isDndGranted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2E1065).copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFF7C3AED)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRequestDndPermission() }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable PrepOS DND Access",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE9D5FF),
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Tap to grant notification policy permission in system settings",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 1. App Pinning (Android)
                    FocusSettingToggleRow(
                        icon = Icons.Default.ScreenLockPortrait,
                        iconColor = Color(0xFF8B5CF6),
                        title = "App Pinning (Android)",
                        description = "Locks study workspace screen to prevent distractions",
                        isChecked = appPinning,
                        onCheckedChange = onToggleAppPinning
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Do Not Disturb (DND)
                    FocusSettingToggleRow(
                        icon = Icons.Default.DoNotDisturb,
                        iconColor = Color(0xFFA855F7),
                        title = "Do Not Disturb (DND)",
                        description = "Silence ringer and notifications during session",
                        isChecked = dnd,
                        onCheckedChange = onToggleDnd
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Notification Behaviour
                    FocusSettingActionRow(
                        icon = Icons.Default.Notifications,
                        iconColor = Color(0xFFC084FC),
                        title = "Notification Behaviour",
                        description = "Audio mode applied when session starts",
                        value = if (notificationBehaviour == "SILENT") "Silent" else "Vibrate",
                        onClick = onOpenNotificationBehavior
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Timer
                    FocusSettingActionRow(
                        icon = Icons.Default.Timer,
                        iconColor = Color(0xFFC084FC),
                        title = "Timer Preset",
                        description = "Target duration for focus session",
                        value = if (timerMinutes <= 0) "Off" else "$timerMinutes min",
                        onClick = onOpenTimerPresets
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Follow Timetable (Optional)
                    FocusSettingToggleRow(
                        icon = Icons.Default.DateRange,
                        iconColor = Color(0xFF8B5CF6),
                        title = "Follow Timetable",
                        description = "Sync focus timer with scheduled task times",
                        isChecked = followTimetable,
                        onCheckedChange = onToggleFollowTimetable
                    )

                    // 6. Background Reliability & Battery Optimization
                    val isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                    val canScheduleExact = BatteryOptimizationHelper.canScheduleExactAlarms(context)

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isBatteryOptimized) Color(0xFF451A03).copy(alpha = 0.6f) else Color(0xFF064E3B).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isBatteryOptimized) Color(0xFFD97706) else Color(0xFF059669)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isBatteryOptimized) {
                                    BatteryOptimizationHelper.requestDisableBatteryOptimization(context)
                                } else if (!canScheduleExact) {
                                    BatteryOptimizationHelper.requestExactAlarmPermission(context)
                                } else {
                                    BatteryOptimizationHelper.openNotificationSettings(context)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBatteryOptimized) "⚠️" else "⚡",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBatteryOptimized) "Allow Unrestricted Background Running" else "Exact Background Alarms Active",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBatteryOptimized) Color(0xFFFDE68A) else Color(0xFFA7F3D0),
                                        fontSize = 11.5.sp
                                    )
                                )
                                Text(
                                    text = if (isBatteryOptimized)
                                        "Tap to disable battery restrictions so timetable alarms and live study notifications fire exactly on time."
                                    else
                                        "PrepOS is exempt from battery saver delays. Timers and notifications fire on the exact second.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 9.5.sp
                                    )
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
fun FocusSettingToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.5.sp
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF7C3AED),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
fun FocusSettingActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.5.sp
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC084FC),
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Select",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Dialogs: Timer Presets, Notification Behavior & Add Task
// -------------------------------------------------------------------------------------------------

@Composable
fun TimerPresetDialog(
    currentMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val presets = listOf(
        0 to "Timer Off",
        15 to "15 minutes",
        25 to "25 minutes (Pomodoro)",
        30 to "30 minutes",
        45 to "45 minutes (Standard)",
        60 to "60 minutes (Deep Dive)",
        90 to "90 minutes (Ultra Focus)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = { Text("Select Focus Session Timer", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary) },
        text = {
            Column {
                presets.forEach { (mins, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mins) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (currentMinutes == mins) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentMinutes == mins) MaterialTheme.colorScheme.primary else (if (isDark) Color.White else LightTextPrimary)
                            )
                        )
                        if (currentMinutes == mins) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary) }
        }
    )
}

@Composable
fun NotificationBehaviorDialog(
    currentBehavior: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val options = listOf(
        "SILENT" to "Silent (Completely mute ringer & alerts)",
        "VIBRATE" to "Vibrate only (Haptic feedback for urgent alerts)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = { Text("Notification Behaviour", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (currentBehavior == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentBehavior == key) MaterialTheme.colorScheme.primary else (if (isDark) Color.White else LightTextPrimary)
                            )
                        )
                        if (currentBehavior == key) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary) }
        }
    )
}

@Composable
fun AddTaskDialog(
    availableSubjects: List<String> = emptyList(),
    initialDay: String = "MON",
    onDismiss: () -> Unit,
    onAddTask: (
        subject: String,
        title: String,
        details: String,
        start: String,
        end: String,
        duration: Int,
        days: String,
        repeatWeekly: Boolean
    ) -> Unit
) {
    val isDark = isAppDarkTheme()
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isFlexibleMode by remember { mutableStateOf(true) }
    var targetDurationMinutes by remember { mutableIntStateOf(50) }
    var customMinutesInput by remember { mutableStateOf("50") }
    var startTime by remember { mutableStateOf("7:00 PM") }
    var endTime by remember { mutableStateOf("7:45 PM") }
    var repeatWeekly by remember { mutableStateOf(true) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember { mutableStateOf(setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) }

    val filteredSubjects = remember(subject, availableSubjects) {
        if (subject.isBlank()) availableSubjects
        else availableSubjects.filter { it.contains(subject.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF111726) else LightSurface,
        title = {
            Text("Add Study Task / Target", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title (e.g. English – Tenses)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Reasoning, Quant, OS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Subject Autocomplete / Suggested Chips
                if (filteredSubjects.isNotEmpty()) {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = if (subject.isBlank()) "Choose available subject:" else "Matching subjects:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredSubjects.take(6).forEach { subjName ->
                                val isSelected = subject.equals(subjName, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) (if (isDark) MaterialTheme.colorScheme.primary else GoldAccent) else (if (isDark) Color(0xFF1E293B) else GoldAccentLight),
                                    border = BorderStroke(1.dp, if (isSelected) (if (isDark) MaterialTheme.colorScheme.primary else GoldAccentDark) else (if (isDark) Color(0xFF334155) else GoldAccentBorder)),
                                    modifier = Modifier.clickable {
                                        subject = subjName
                                    }
                                ) {
                                    Text(
                                        text = subjName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFE2E8F0) else GoldAccentDark),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Activity / Topic (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Mode Selector: Flexible Duration vs Fixed Time Slot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isFlexibleMode = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFlexibleMode) (if (isDark) MaterialTheme.colorScheme.primary else GoldAccent) else Color.Transparent
                    ) {
                        Text(
                            text = "Flexible Target (Mins)",
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFlexibleMode) Color.White else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary),
                                fontSize = 11.sp
                            )
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isFlexibleMode = false },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isFlexibleMode) (if (isDark) MaterialTheme.colorScheme.primary else GoldAccent) else Color.Transparent
                    ) {
                        Text(
                            text = "Fixed Time Slot",
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!isFlexibleMode) Color.White else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (isFlexibleMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Study Target: $targetDurationMinutes mins",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC084FC) else GoldAccentDark,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "Anytime today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(20, 30, 45, 50, 60, 90).forEach { mins ->
                                val isSelected = targetDurationMinutes == mins
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) (if (isDark) Color(0xFF6366F1) else GoldAccent) else (if (isDark) Color(0xFF131826) else GoldAccentLight),
                                    border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFF818CF8) else GoldAccentDark) else (if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle)),
                                    modifier = Modifier.clickable {
                                        targetDurationMinutes = mins
                                        customMinutesInput = mins.toString()
                                    }
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else LightTextPrimary),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                Text("Repeat on Days:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEach { day ->
                        val isDaySelected = selectedDays.contains(day)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDaySelected) (if (isDark) MaterialTheme.colorScheme.primary else GoldAccent) else (if (isDark) Color(0xFF1E293B) else GoldAccentLight),
                            border = BorderStroke(1.dp, if (isDaySelected) (if (isDark) MaterialTheme.colorScheme.primary else GoldAccentDark) else (if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle)),
                            modifier = Modifier.clickable {
                                selectedDays = if (isDaySelected) {
                                    if (selectedDays.size > 1) selectedDays - day else selectedDays
                                } else {
                                    selectedDays + day
                                }
                            }
                        ) {
                            Text(
                                text = day,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaySelected) Color.White else (if (isDark) Color(0xFF94A3B8) else GoldAccentDark),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val daysStr = selectedDays.joinToString(",") { it.uppercase() }
                        val finalSubject = if (subject.isNotBlank()) subject.trim() else "General"
                        val finalStart = if (isFlexibleMode) "Anytime" else startTime
                        val finalEnd = if (isFlexibleMode) "" else endTime
                        val finalDuration = if (isFlexibleMode) targetDurationMinutes else 45
                        onAddTask(finalSubject, title, details, finalStart, finalEnd, finalDuration, daysStr, repeatWeekly)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) BrandPrimary else GoldAccent
                )
            ) {
                Text("Save Task", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// Full Immersion Active Focus Session Overlay
// -------------------------------------------------------------------------------------------------

@Composable
fun ActiveFocusSessionOverlay(
    session: com.example.viewmodel.ActiveFocusSession,
    onDismiss: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onFinish: () -> Unit
) {
    val remSec = session.remainingSeconds
    val minutes = remSec / 60
    val seconds = remSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Top Minimize / Back button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Minimize Immersion",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1B4B),
                    border = BorderStroke(1.dp, Color(0xFF6366F1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "DEEP FOCUS IMMERSION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE0E7FF),
                                letterSpacing = 0.8.sp,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = session.taskTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.subjectName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Circular Progress countdown clock
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF6B21A8).copy(alpha = 0.35f), Color(0xFF1E1B4B).copy(alpha = 0.2f))
                            )
                        )
                        .border(3.dp, Color(0xFF7C3AED), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 40.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = if (session.isPaused) "PAUSED" else "STAY FOCUSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (session.isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (session.isPaused) onResume() else onPause() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (session.isPaused) "Resume" else "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Button(
                        onClick = onFinish,
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Complete", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveFocusTimerPanel(
    session: ActiveFocusSession,
    onExpand: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remSec = session.remainingSeconds
    val minutes = remSec / 60
    val seconds = remSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("live_focus_timer_panel"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1B4B),
        border = BorderStroke(
            1.2.dp,
            if (session.isPaused) Color(0xFFF59E0B) else Color(0xFF8B5CF6)
        ),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExpand() }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (session.isPaused) Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else Color(0xFF8B5CF6).copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (session.isPaused) Icons.Default.Pause else Icons.Default.Adjust,
                        contentDescription = null,
                        tint = if (session.isPaused) Color(0xFFF59E0B) else Color(0xFFC084FC),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (session.isPaused) Color(0xFFFBBF24) else Color.White,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = if (session.isPaused) "Paused • Tap to expand" else "Deep Focus • Tap to expand",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Button
                Surface(
                    shape = CircleShape,
                    color = if (session.isPaused) Color(0xFF7C3AED) else Color(0xFF312E81),
                    border = BorderStroke(1.dp, Color(0xFF818CF8)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { if (session.isPaused) onResume() else onPause() }
                        .testTag("live_panel_pause_resume_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (session.isPaused) "Resume" else "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Stop Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onStop() }
                        .testTag("live_panel_stop_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
