package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import com.example.util.BatteryOptimizationHelper
import com.example.util.PrepOSAlarmScheduler
import com.example.util.PrepOSFocusNotificationManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import com.example.ui.components.MadeWithLoveFooter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.StudyAIService
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PrepOSViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val preferences by viewModel.preferences.collectAsState()
    val exams by viewModel.exams.collectAsState()

    var apiKeyInput by remember {
        mutableStateOf(preferences?.apiKey ?: "")
    }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isUserEditingKey by remember { mutableStateOf(false) }

    var preferredNameInput by remember(preferences?.preferredUserName) {
        mutableStateOf(preferences?.preferredUserName ?: "")
    }

    var targetExamNameInput by remember(preferences?.targetExamName) {
        mutableStateOf(preferences?.targetExamName ?: "")
    }

    var targetExamDateInput by remember(preferences?.targetExamDate) {
        mutableStateOf(preferences?.targetExamDate ?: "")
    }

    var targetDailyHoursInput by remember(preferences?.targetDailyStudyHours) {
        mutableStateOf(preferences?.targetDailyStudyHours?.toString() ?: "3.0")
    }

    var targetScoreGoalInput by remember(preferences?.targetScoreGoal) {
        mutableStateOf(preferences?.targetScoreGoal?.toString() ?: "85")
    }

    LaunchedEffect(preferences?.apiKey) {
        val savedKey = preferences?.apiKey ?: ""
        if (!isUserEditingKey) {
            apiKeyInput = savedKey
        }
    }

    var selectedModel by remember(preferences?.selectedAiModel) {
        val currentModel = preferences?.selectedAiModel ?: StudyAIService.DEFAULT_MODEL
        val isValid = StudyAIService.AVAILABLE_MODELS.any { it.first == currentModel }
        mutableStateOf(if (isValid) currentModel else StudyAIService.DEFAULT_MODEL)
    }
    var isModelDropdownOpen by remember { mutableStateOf(false) }

    // Test API connection state
    var isTestingConnection by remember { mutableStateOf(false) }
    var testConnectionResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Selective Export Dialog & Options State
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showResetJkssbConfirmDialog by remember { mutableStateOf(false) }
    var exportSubjects by remember { mutableStateOf(true) }
    var exportQuestions by remember { mutableStateOf(true) }
    var exportTasks by remember { mutableStateOf(true) }
    var exportTestAttempts by remember { mutableStateOf(true) }
    var exportProgress by remember { mutableStateOf(true) }
    var exportPreferences by remember { mutableStateOf(true) }

    // Background Reliability & Exact Alarms State
    val alarmPrefs = remember { PrepOSAlarmScheduler.getPrefs(context) }
    var isBatteryOptIgnored by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var canScheduleExact by remember {
        mutableStateOf(BatteryOptimizationHelper.canScheduleExactAlarms(context))
    }
    var isNotificationAllowed by remember {
        mutableStateOf(BatteryOptimizationHelper.areNotificationsEnabled(context))
    }

    var notifsMasterEnabled by remember {
        mutableStateOf(alarmPrefs.getBoolean(PrepOSAlarmScheduler.PREF_NOTIFS_ENABLED, true))
    }
    var taskRemindersEnabled by remember {
        mutableStateOf(alarmPrefs.getBoolean(PrepOSAlarmScheduler.PREF_TASK_REMINDERS_ENABLED, true))
    }
    var taskHeadsUpMins by remember {
        mutableStateOf(alarmPrefs.getInt(PrepOSAlarmScheduler.PREF_HEADS_UP_MINUTES, 0))
    }
    var morningBriefingEnabled by remember {
        mutableStateOf(alarmPrefs.getBoolean(PrepOSAlarmScheduler.PREF_MORNING_BRIEFING_ENABLED, true))
    }
    var morningBriefingTime by remember {
        mutableStateOf(alarmPrefs.getString(PrepOSAlarmScheduler.PREF_MORNING_BRIEFING_TIME, "08:30") ?: "08:30")
    }
    var eveningStreakEnabled by remember {
        mutableStateOf(alarmPrefs.getBoolean(PrepOSAlarmScheduler.PREF_EVENING_STREAK_ENABLED, true))
    }
    var eveningStreakTime by remember {
        mutableStateOf(alarmPrefs.getString(PrepOSAlarmScheduler.PREF_EVENING_STREAK_TIME, "20:00") ?: "20:00")
    }

    fun refreshPermissionStatuses() {
        isBatteryOptIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        canScheduleExact = BatteryOptimizationHelper.canScheduleExactAlarms(context)
        isNotificationAllowed = BatteryOptimizationHelper.areNotificationsEnabled(context)
    }

    // ZIP Export launcher
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val chosenOptions = com.example.data.repository.BackupExportOptions(
                exportSubjectsAndNotes = exportSubjects,
                exportQuestionsBank = exportQuestions,
                exportTimetableTasks = exportTasks,
                exportTestAttempts = exportTestAttempts,
                exportProgressAndStreaks = exportProgress,
                exportPreferencesAndGoals = exportPreferences
            )
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val success = viewModel.exportZipBackupToStream(outputStream, chosenOptions)
                        if (success) {
                            Toast.makeText(context, "Selected Backup exported successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to export ZIP backup.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ZIP Restore launcher
    val restoreZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = viewModel.importZipBackupFromUri(context, uri)
                if (success) {
                    Toast.makeText(context, "Study Library restored successfully from ZIP!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to restore ZIP. Ensure the file is a valid PrepOS backup.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & AI Engine",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 0. THEME & APPEARANCE CUSTOMIZATION
            SectionCard(
                title = "App Theme & Appearance",
                icon = Icons.Default.Palette,
                iconTint = Color(0xFF6366F1)
            ) {
                val currentThemeMode = preferences?.appThemeMode ?: "SYSTEM"

                Text(
                    text = "Interface Color Mode",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "By default, PrepOS adapts dynamically to your Android system theme (Light or Dark). You can also lock it to Light or Dark mode.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themeOptions = listOf(
                        Triple("SYSTEM", "System Default", Icons.Default.BrightnessAuto),
                        Triple("LIGHT", "Light Mode", Icons.Default.LightMode),
                        Triple("DARK", "Dark Mode", Icons.Default.DarkMode)
                    )

                    themeOptions.forEach { (modeKey, label, icon) ->
                        val isSelected = currentThemeMode == modeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateAppThemeMode(modeKey)
                                    val msg = when (modeKey) {
                                        "LIGHT" -> "✓ Light Mode enabled"
                                        "DARK" -> "✓ Dark Mode enabled"
                                        else -> "✓ Following System Theme"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                                .testTag("theme_option_${modeKey.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF6366F1) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (isSelected) Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (modeKey == "SYSTEM") "Auto Match" else if (modeKey == "LIGHT") "Clean White" else "AMOLED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 1. PERSONALIZATION & STUDENT PROFILE
            SectionCard(
                title = "Student Profile & Target Exam",
                icon = Icons.Default.School,
                iconTint = Color(0xFF8B5CF6)
            ) {
                Text(
                    text = "Profile & Target Goals",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "PrepOS AI & Test Hub use these target details to calibrate your practice tests, time targets, and countdowns.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Preferred Name
                Text(
                    text = "Your Preferred Name",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = preferredNameInput,
                    onValueChange = { preferredNameInput = it },
                    placeholder = { Text("e.g. Alex, Maya, Aryan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preferred_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Target Exam
                Text(
                    text = "Target Exam Name",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetExamNameInput,
                    onValueChange = { targetExamNameInput = it },
                    placeholder = { Text("e.g. UPSC CSE, JEE Advanced, GATE, CAT, USMLE") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_exam_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Target Exam Date & Daily Hours in a 2-column row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Target Exam Date",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetExamDateInput,
                            onValueChange = { targetExamDateInput = it },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_exam_date_input")
                        )
                    }

                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Daily Hours",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetDailyHoursInput,
                            onValueChange = { targetDailyHoursInput = it },
                            placeholder = { Text("3.0") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_daily_hours_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Target Score Goal
                Text(
                    text = "Target Score Goal (%)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetScoreGoalInput,
                    onValueChange = { targetScoreGoalInput = it },
                    placeholder = { Text("e.g. 85") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_score_goal_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val trimmedName = preferredNameInput.trim()
                        val trimmedExam = targetExamNameInput.trim()
                        val trimmedDate = targetExamDateInput.trim()
                        val hours = targetDailyHoursInput.toFloatOrNull() ?: 3.0f
                        val score = targetScoreGoalInput.toIntOrNull() ?: 85

                        viewModel.updateTargetExamProfile(
                            name = trimmedName,
                            targetExam = trimmedExam,
                            targetDate = trimmedDate,
                            dailyHours = hours,
                            scoreGoal = score
                        )
                        Toast.makeText(context, "✓ Student Profile & Goals updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_preferred_name_button")
                ) {
                    Text("Save Profile & Exam Targets", fontWeight = FontWeight.Bold)
                }
            }

            // 1. AI ENGINE & API KEY CONFIGURATION (GOOGLE GEMINI)
            SectionCard(
                title = "AI Intelligence & Gemini Key",
                icon = Icons.Default.AutoAwesome,
                iconTint = Color(0xFF6366F1) // Indigo/Gemini color
            ) {
                // Status Badge
                val isAiConfigured = preferences?.apiKey?.isNotBlank() == true && preferences?.apiKey != "MY_GEMINI_API_KEY"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isAiConfigured) Color(0xFF10B981).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAiConfigured) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isAiConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAiConfigured) "Gemini AI Active & Connected" else "Offline Mode (Local Sanitizer Active)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isAiConfigured) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isAiConfigured) "Ask AI Tutor, instant doubt solver, chapter sanitizing & AI Mock test generators active."
                            else "All 2,500+ syllabus questions, offline notes, and mistake practice work 100% offline.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GET FREE API KEY CALL-TO-ACTION CARD
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Get Free Gemini API Key",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "1,500 Requests/Day Free • No Credit Card Needed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Google AI Studio par jakar 1-click mein bilkul free API key generate karein aur yahan paste karein:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // How to get steps
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "1️⃣ Neeche diye button par tap karke Google AI Studio kholein.",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "2️⃣ 'Create API Key' par click karke key copy karein.",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "3️⃣ Neeche box mein paste karein aur 'Save Key' dabayein.",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val url = "https://aistudio.google.com/app/apikey"
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("get_free_gemini_api_key_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Get Free API Key (Google AI Studio)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini API Key Input Field
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        isUserEditingKey = true
                        testConnectionResult = null
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (apiKeyInput.isBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipData = clipboardManager.getText()?.text?.trim()
                                        if (!clipData.isNullOrBlank()) {
                                            apiKeyInput = clipData
                                            isUserEditingKey = true
                                            Toast.makeText(context, "Key pasted from clipboard!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isApiKeyVisible) "Hide Key" else "Show Key"
                                )
                            }
                        }
                    },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                if (isAiConfigured && !isApiKeyVisible) {
                    val savedKey = preferences?.apiKey ?: ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✓ Key saved: ••••••••${savedKey.takeLast(4)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Tap eye icon to view",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Model Selection
                Text(
                    text = "Gemini Model",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isModelDropdownOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_selector_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val modelLabel = StudyAIService.AVAILABLE_MODELS.firstOrNull { it.first == selectedModel }?.second
                                ?: selectedModel
                            Text(text = modelLabel, style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = isModelDropdownOpen,
                        onDismissRequest = { isModelDropdownOpen = false }
                    ) {
                        StudyAIService.AVAILABLE_MODELS.forEach { (modelId, modelName) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(modelName, fontWeight = if (selectedModel == modelId) FontWeight.Bold else FontWeight.Normal)
                                        Text(modelId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedModel = modelId
                                    viewModel.saveAiModel(modelId)
                                    isModelDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Save Key, Clear Key, Test Connection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedGemini = apiKeyInput.trim()
                            viewModel.saveApiKey(trimmedGemini)
                            viewModel.saveAiProvider("GEMINI")
                            viewModel.saveAiModel(selectedModel)
                            isUserEditingKey = false
                            Toast.makeText(context, "✓ Gemini API Key saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_api_key_button")
                    ) {
                        Text("Save Key")
                    }

                    if (apiKeyInput.isNotBlank() || preferences?.apiKey?.isNotBlank() == true) {
                        OutlinedButton(
                            onClick = {
                                apiKeyInput = ""
                                isUserEditingKey = false
                                viewModel.saveApiKey("")
                                testConnectionResult = null
                                Toast.makeText(context, "API Key cleared. Switched to offline mode.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test API Connection Button
                OutlinedButton(
                    onClick = {
                        val keyToTest = apiKeyInput.trim().ifBlank { preferences?.apiKey ?: "" }
                        if (keyToTest.isBlank()) {
                            testConnectionResult = Pair(false, "Please enter your Gemini API key first.")
                            return@OutlinedButton
                        }

                        isTestingConnection = true
                        testConnectionResult = null

                        scope.launch {
                            try {
                                val res = StudyAIService.testApiConnection(
                                    apiKey = keyToTest,
                                    model = selectedModel,
                                    provider = "GEMINI"
                                )
                                isTestingConnection = false
                                if (res.isSuccess) {
                                    testConnectionResult = Pair(true, "Gemini API connection verified! Ready to assist your studies.")
                                } else {
                                    testConnectionResult = Pair(false, res.exceptionOrNull()?.localizedMessage ?: "Connection failed.")
                                }
                            } catch (e: Exception) {
                                isTestingConnection = false
                                testConnectionResult = Pair(false, "Connection failed: ${e.localizedMessage ?: "Unknown error"}")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_api_connection_button"),
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing Gemini Connection...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test API Connection")
                    }
                }

                // Live Connection Feedback
                AnimatedVisibility(visible = testConnectionResult != null) {
                    testConnectionResult?.let { (success, message) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (success) Color(0xFF10B981).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (success) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (success) Color(0xFF047857) else MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                }
            }

            // 2. MANDATORY DAILY STUDY TARGET & STREAK SYSTEM
            SectionCard(
                title = "Study Target & Streak System",
                icon = Icons.Default.AutoAwesome,
                iconTint = Color(0xFF7C3AED) // Purple
            ) {
                val dailyTargetMins = preferences?.dailyTargetMinutes ?: 45
                val currentStreak = preferences?.currentStreak ?: 0
                val bestStreak = preferences?.bestStreak ?: 0

                Text(
                    text = "Daily Target: Mandatory Focus Rule",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Streaks cannot be inflated by manually checking tasks. Streak officially increments only when your scheduled study/focus target time is completed.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Streak Stats Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF7C3AED).copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 $currentStreak Days", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED)))
                            Text("Current Streak", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFF7C3AED).copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆 $bestStreak Days", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD97706)))
                            Text("Best Streak", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFF7C3AED).copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯 ${dailyTargetMins}m", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981)))
                            Text("Daily Goal", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Daily Target (Minutes)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        val isSelected = dailyTargetMins == mins
                        OutlinedButton(
                            onClick = { viewModel.updateDailyTargetMinutes(mins) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF7C3AED).copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF7C3AED) else MaterialTheme.colorScheme.outline)
                        ) {
                            Text("${mins}m", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. EXAM PREFERENCES & HOME SCREEN CUSTOMIZATION
            SectionCard(
                title = "Exam & Home Screen",
                icon = Icons.Default.School,
                iconTint = Color(0xFF2563EB) // Royal Blue
            ) {
                // Default Exam Selection
                Text(
                    text = "Default Exam Filter on Launch",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automatically filters your subjects and chapters to this exam when PrepOS opens.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                var isExamDropdownOpen by remember { mutableStateOf(false) }
                val currentDefaultExam = exams.firstOrNull { it.id == preferences?.defaultExamId }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isExamDropdownOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentDefaultExam?.name ?: "All Exams (No default filter)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = isExamDropdownOpen,
                        onDismissRequest = { isExamDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Exams (Show Everything)") },
                            onClick = {
                                viewModel.saveDefaultExamId(null)
                                isExamDropdownOpen = false
                            }
                        )
                        HorizontalDivider()
                        exams.forEach { exam ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    try { Color(android.graphics.Color.parseColor(exam.colorHex)) }
                                                    catch (e: Exception) { Color(0xFF2563EB) }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(exam.name)
                                    }
                                },
                                onClick = {
                                    viewModel.saveDefaultExamId(exam.id)
                                    isExamDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Home Screen Exam Filter Bar Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Exam Filters",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Show filter chips below search to quickly switch between exams.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = preferences?.showExamFilters ?: true,
                        onCheckedChange = { viewModel.setShowExamFilters(it) },
                        modifier = Modifier.testTag("toggle_exam_filters")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Home Screen Exam Labels Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Exam Labels on Subject Cards",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Displays the associated exam badge on cards on the Home screen.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = preferences?.showExamLabelsOnHome ?: true,
                        onCheckedChange = { viewModel.setShowExamLabels(it) },
                        modifier = Modifier.testTag("toggle_exam_labels")
                    )
                }
            }

            // 3. BACKGROUND RELIABILITY & EXACT NOTIFICATIONS
            SectionCard(
                title = "Background Reliability & Exact Timers",
                icon = Icons.Default.NotificationsActive,
                iconTint = Color(0xFFF59E0B) // Amber / Gold
            ) {
                Text(
                    text = "Android battery optimizations (Doze Mode) and vendor app-killers pause background execution when your screen is locked. To guarantee your timetable study tasks, morning briefings, and live focus countdowns alert you at the exact minute, configure these system permissions.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // SYSTEM HEALTH / PERMISSION TILES
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Push Notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isNotificationAllowed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isNotificationAllowed) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "System Notifications",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (isNotificationAllowed) "Allowed • Status bar alerts active" else "Blocked • Tap to enable notifications",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isNotificationAllowed) Color(0xFF059669) else Color(0xFFDC2626)
                                    )
                                )
                            }
                        }
                        if (!isNotificationAllowed) {
                            OutlinedButton(
                                onClick = { BatteryOptimizationHelper.openNotificationSettings(context) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Enable", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Row 2: Battery Optimization (Doze Exemption)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isBatteryOptIgnored) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = if (isBatteryOptIgnored) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Battery Restrictions",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (isBatteryOptIgnored) "Unrestricted • Wakes up in sleep mode" else "Restricted • May delay alerts during sleep",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isBatteryOptIgnored) Color(0xFF059669) else Color(0xFFD97706)
                                    )
                                )
                            }
                        }
                        if (!isBatteryOptIgnored) {
                            Button(
                                onClick = { BatteryOptimizationHelper.requestDisableBatteryOptimization(context) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Allow Background", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Row 3: Exact Alarms (Android 12+)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (canScheduleExact) Icons.Default.CheckCircle else Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (canScheduleExact) Color(0xFF10B981) else Color(0xFF3B82F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Exact Alarm Permission",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (canScheduleExact) "Allowed • Down-to-the-second timing" else "Restricted • System may batch alarms",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (canScheduleExact) Color(0xFF059669) else Color(0xFF2563EB)
                                    )
                                )
                            }
                        }
                        if (!canScheduleExact) {
                            OutlinedButton(
                                onClick = { BatteryOptimizationHelper.requestExactAlarmPermission(context) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Allow Exact", fontSize = 12.sp)
                            }
                        }
                    }

                    // Refresh Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { refreshPermissionStatuses() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh Status", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // SCHEDULE CONFIGURATION
                Text(
                    text = "Automated Study Reminders",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Study Notifications",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Main switch for all timetable reminders and daily check-ins.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = notifsMasterEnabled,
                        onCheckedChange = { isChecked ->
                            notifsMasterEnabled = isChecked
                            alarmPrefs.edit().putBoolean(PrepOSAlarmScheduler.PREF_NOTIFS_ENABLED, isChecked).apply()
                            viewModel.rescheduleAllAlarms()
                        }
                    )
                }

                if (notifsMasterEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Timetable Task Reminders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Timetable Task Alerts",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Notify when scheduled study tasks from your Focus Hub timetable begin.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = taskRemindersEnabled,
                            onCheckedChange = { isChecked ->
                                taskRemindersEnabled = isChecked
                                alarmPrefs.edit().putBoolean(PrepOSAlarmScheduler.PREF_TASK_REMINDERS_ENABLED, isChecked).apply()
                                viewModel.rescheduleAllAlarms()
                            }
                        )
                    }

                    if (taskRemindersEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Heads-up Lead Time:",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Exact", 5 to "5m before", 10 to "10m before", 15 to "15m before").forEach { (mins, label) ->
                                val isSel = taskHeadsUpMins == mins
                                OutlinedButton(
                                    onClick = {
                                        taskHeadsUpMins = mins
                                        alarmPrefs.edit().putInt(PrepOSAlarmScheduler.PREF_HEADS_UP_MINUTES, mins).apply()
                                        viewModel.rescheduleAllAlarms()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = if (isSel) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Morning Briefing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Morning Briefing",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Start your day with an overview of your study schedule & targets.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = morningBriefingEnabled,
                            onCheckedChange = { isChecked ->
                                morningBriefingEnabled = isChecked
                                alarmPrefs.edit().putBoolean(PrepOSAlarmScheduler.PREF_MORNING_BRIEFING_ENABLED, isChecked).apply()
                                viewModel.rescheduleAllAlarms()
                            }
                        )
                    }

                    if (morningBriefingEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Briefing Time:",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("07:30", "08:00", "08:30", "09:00").forEach { time ->
                                val isSel = morningBriefingTime == time
                                val displayTime = when (time) {
                                    "07:30" -> "7:30 AM"
                                    "08:00" -> "8:00 AM"
                                    "08:30" -> "8:30 AM"
                                    "09:00" -> "9:00 AM"
                                    else -> time
                                }
                                OutlinedButton(
                                    onClick = {
                                        morningBriefingTime = time
                                        alarmPrefs.edit().putString(PrepOSAlarmScheduler.PREF_MORNING_BRIEFING_TIME, time).apply()
                                        viewModel.rescheduleAllAlarms()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = if (isSel) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                ) {
                                    Text(displayTime, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Evening Streak Guardian
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Evening Streak Guardian",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Alert before midnight if your daily study goal is not yet achieved.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = eveningStreakEnabled,
                            onCheckedChange = { isChecked ->
                                eveningStreakEnabled = isChecked
                                alarmPrefs.edit().putBoolean(PrepOSAlarmScheduler.PREF_EVENING_STREAK_ENABLED, isChecked).apply()
                                viewModel.rescheduleAllAlarms()
                            }
                        )
                    }

                    if (eveningStreakEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Streak Warning Time:",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("19:00", "20:00", "21:00", "22:00").forEach { time ->
                                val isSel = eveningStreakTime == time
                                val displayTime = when (time) {
                                    "19:00" -> "7:00 PM"
                                    "20:00" -> "8:00 PM"
                                    "21:00" -> "9:00 PM"
                                    "22:00" -> "10:00 PM"
                                    else -> time
                                }
                                OutlinedButton(
                                    onClick = {
                                        eveningStreakTime = time
                                        alarmPrefs.edit().putString(PrepOSAlarmScheduler.PREF_EVENING_STREAK_TIME, time).apply()
                                        viewModel.rescheduleAllAlarms()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = if (isSel) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                ) {
                                    Text(displayTime, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // VERIFICATION & TESTING BUTTONS
                Text(
                    text = "Background Delivery Diagnostics",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verify that notifications and exact background wakeup are working on your specific device.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            PrepOSFocusNotificationManager.showSystemAlertNotification(
                                context = context,
                                title = "🔔 PrepOS Push Test",
                                message = "Real system notification delivered successfully with action buttons!",
                                type = "REMINDER",
                                actionType = "START_FOCUS"
                            )
                            Toast.makeText(context, "Immediate test notification sent!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test Push", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            PrepOSAlarmScheduler.scheduleTestAlarm(context, delaySeconds = 10)
                            Toast.makeText(context, "Exact alarm set! Lock screen now — it will wake up in 10 seconds.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("10s Sleep Test", fontSize = 12.sp)
                    }
                }
            }

            // 4. FULL APP ZIP BACKUP & RESTORE
            SectionCard(
                title = "Backup & Restore (ZIP)",
                icon = Icons.Default.FolderZip,
                iconTint = Color(0xFF16A34A) // Emerald Green
            ) {
                Text(
                    text = "Backup your entire study library, all subjects, continuous block documents, formatting, and reading progress into a single compressed ZIP archive.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export ZIP
                    Button(
                        onClick = {
                            showExportOptionsDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_zip_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Export")
                    }

                    // Restore ZIP
                    OutlinedButton(
                        onClick = {
                            restoreZipLauncher.launch("*/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_zip_button")
                        ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore ZIP")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Restore / Reload JKSSB Default Master Content
                OutlinedButton(
                    onClick = {
                        showResetJkssbConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reload Built-in JKSSB Constable Content")
                }
            }

            // Reload JKSSB Master Content Confirmation Dialog
            if (showResetJkssbConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showResetJkssbConfirmDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reload Built-in Content?", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(
                            text = "This will reload the official JKSSB Constable study content (English, Computer, GK J&K, Reasoning Ability, Numerical Ability, and General Knowledge with all 59 chapters and 2,500+ practice questions) directly from the package. Any custom subjects will be reset. Are you sure?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResetJkssbConfirmDialog = false
                                scope.launch {
                                    val success = viewModel.reloadJkssbDefaultContent(context)
                                    if (success) {
                                        Toast.makeText(context, "Official JKSSB Constable content reloaded successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Could not reload built-in content.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("Reload Content")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetJkssbConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Selective Export Options Dialog
            if (showExportOptionsDialog) {
                AlertDialog(
                    onDismissRequest = { showExportOptionsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select What to Export", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Choose the study data modules you want to include in this backup ZIP file:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Select All / Deselect All Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        val target = !(exportSubjects && exportQuestions && exportTasks && exportTestAttempts && exportProgress && exportPreferences)
                                        exportSubjects = target
                                        exportQuestions = target
                                        exportTasks = target
                                        exportTestAttempts = target
                                        exportProgress = target
                                        exportPreferences = target
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (exportSubjects && exportQuestions && exportTasks && exportTestAttempts && exportProgress && exportPreferences) "Deselect All" else "Select All",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            ExportOptionRow(
                                title = "Subjects, Notes & Documents",
                                subtitle = "Exams, subjects, and all continuous rich note blocks",
                                checked = exportSubjects,
                                onCheckedChange = { exportSubjects = it }
                            )

                            ExportOptionRow(
                                title = "Flashcards & Question Bank",
                                subtitle = "AI-generated questions and MCQ review decks",
                                checked = exportQuestions,
                                onCheckedChange = { exportQuestions = it }
                            )

                            ExportOptionRow(
                                title = "Study Timetable & Daily Tasks",
                                subtitle = "Scheduled study plan, daily targets and task completion logs",
                                checked = exportTasks,
                                onCheckedChange = { exportTasks = it }
                            )

                            ExportOptionRow(
                                title = "Mock Tests & Attempt Analytics",
                                subtitle = "Full mock tests, scores, timers, and question answers",
                                checked = exportTestAttempts,
                                onCheckedChange = { exportTestAttempts = it }
                            )

                            ExportOptionRow(
                                title = "Streaks & Reading Progress",
                                subtitle = "Daily focus streaks, best streaks and chapter scroll progress",
                                checked = exportProgress,
                                onCheckedChange = { exportProgress = it }
                            )

                            ExportOptionRow(
                                title = "App Settings & Exam Goals",
                                subtitle = "Target exam, daily goals, AI preferences & styles",
                                checked = exportPreferences,
                                onCheckedChange = { exportPreferences = it }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExportOptionsDialog = false
                                val timeStamp = System.currentTimeMillis()
                                exportZipLauncher.launch("PrepOS_Backup_$timeStamp.zip")
                            },
                            enabled = exportSubjects || exportQuestions || exportTasks || exportTestAttempts || exportProgress || exportPreferences
                        ) {
                            Text("Export Backup")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportOptionsDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // App info footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PrepOS Continuous Study Engine v2.0 • Offline-First Architecture",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                )
            }

            MadeWithLoveFooter(bottomPadding = 20)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            content()
        }
    }
}

@Composable
private fun ExportOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

