package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.example.ui.components.AnimatedProgressBar
import com.example.ui.theme.ProgressColor
import com.example.ui.theme.isAppDarkTheme
import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.ContentExtractor
import com.example.data.entity.ChapterEntity
import com.example.ui.test.ImportQuestionsDialog
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: String,
    subjectName: String,
    viewModel: PrepOSViewModel,
    onBack: () -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    onOpenTest: (chapterId: String) -> Unit
) {
    val chapters by viewModel.getChaptersForSubject(subjectId).collectAsState(initial = emptyList())
    val subjects by viewModel.subjects.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    val currentSubject = subjects.find { it.id == subjectId }
    val parentExam = exams.find { it.id == currentSubject?.examId }

    var showNewChapterDialog by remember { mutableStateOf(false) }
    var chapterToEdit by remember { mutableStateOf<ChapterEntity?>(null) }
    var chapterToUploadQuestions by remember { mutableStateOf<ChapterEntity?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Track active study time while reviewing subject & chapters
    androidx.compose.runtime.DisposableEffect(subjectId) {
        val timerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000L)
                viewModel.recordActiveStudySeconds(1, subjectName = subjectName)
            }
        }
        onDispose {
            timerJob.cancel()
            viewModel.flushActiveStudySeconds()
        }
    }

    // Sort chapters strictly by ascending chapterNumber (1, 2, 3...), unnumbered at end
    val sortedChapters = remember(chapters) {
        chapters.sortedWith(
            compareBy<ChapterEntity> { if (it.chapterNumber <= 0) Int.MAX_VALUE else it.chapterNumber }
                .thenBy { it.orderIndex }
                .thenBy { it.createdAt }
        )
    }

    // Filter chapters dynamically by search query (title, summary, chapter number)
    val displayedChapters = remember(sortedChapters, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedChapters
        } else {
            val q = searchQuery.trim().lowercase()
            sortedChapters.filter { chap ->
                chap.title.lowercase().contains(q) ||
                chap.summary.lowercase().contains(q) ||
                "chapter ${chap.chapterNumber}".contains(q) ||
                "ch ${chap.chapterNumber}".contains(q) ||
                "${chap.chapterNumber}".contains(q)
            }
        }
    }

    val isDark = isAppDarkTheme()
    val accentColor = try {
        Color(android.graphics.Color.parseColor(currentSubject?.colorHex ?: "#3B82F6"))
    } catch (e: Exception) {
        if (isDark) MaterialTheme.colorScheme.primary else GoldAccent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subject_search_field"),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search chapters, notes...",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 15.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        Column {
                            if (parentExam != null) {
                                Text(
                                    text = parentExam.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Text(
                                text = subjectName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSearchActive) "Close Search" else "Back"
                        )
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("subject_btn_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Chapters"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChapterDialog = true },
                containerColor = accentColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_chapter")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Chapter")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Chapter", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 4.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Subject Overall Progress Header Card
            if (sortedChapters.isNotEmpty()) {
                item {
                    val completedCount = sortedChapters.count { it.readingProgress >= 0.98f }
                    val totalCount = sortedChapters.size
                    val overallPct = if (completedCount == totalCount) 100 else {
                        val avg = sortedChapters.map { if (it.readingProgress >= 0.98f) 1.0f else it.readingProgress }.average().toFloat()
                        if (avg >= 0.985f) 100 else kotlin.math.round(avg * 100).toInt().coerceIn(0, 100)
                    }
                    val isAllComplete = completedCount == totalCount

                    val headerBorder = if (isAllComplete) {
                        androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = if (isDark) 0.50f else 0.35f))
                    } else {
                        androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = if (isDark) 0.38f else 0.25f))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.2.dp, ProgressColor.border(overallPct.toFloat(), if (isDark) 0.35f else 0.25f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F172A) else Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ProgressColor.rememberAnimatedFogBrush(overallPct.toFloat(), isDark))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ProgressColor.softBg(overallPct.toFloat(), if (isDark) 0.22f else 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isAllComplete) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = ProgressColor.forProgressSmart(overallPct.toFloat()),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isAllComplete) "Subject 100% Completed!" else "Subject Syllabus Progress",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurface else LightTextPrimary,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = "$overallPct%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ProgressColor.forProgressSmart(overallPct.toFloat()),
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                AnimatedProgressBar(
                                    percent = overallPct.toFloat(),
                                    height = 6.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$completedCount of $totalCount chapters completed",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Search query feedback bar
            if (searchQuery.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = if (isDark) 0.18f else 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${displayedChapters.size} of ${sortedChapters.size} chapters found",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                ),
                                modifier = Modifier
                                    .clickable {
                                        searchQuery = ""
                                        isSearchActive = false
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Chapters List (Sorted 1 -> 2 -> 3...)
            if (sortedChapters.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No chapters yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add notes manually or paste raw text/PDF to automatically structure with AI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (displayedChapters.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F172A) else LightSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF1E293B) else LightBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF64748B) else LightTextSecondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No chapters match \"$searchQuery\"",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching with a different term, chapter number, or key topic.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Clear Search", color = Color.White)
                            }
                        }
                    }
                }
            } else {
                items(displayedChapters, key = { it.id }) { chapter ->
                    ChapterCard(
                        chapter = chapter,
                        accentColor = accentColor,
                        viewModel = viewModel,
                        onClick = { onOpenChapter(chapter.id) },
                        onEdit = { chapterToEdit = chapter },
                        onOpenTest = { onOpenTest(chapter.id) },
                        onDelete = { viewModel.deleteChapter(chapter.id) },
                        onUploadQuestions = { chapterToUploadQuestions = chapter }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Dialog: Edit Chapter (Number & Title)
    if (chapterToEdit != null) {
        val editingChapter = chapterToEdit!!
        EditChapterDialog(
            initialTitle = editingChapter.title,
            initialNumber = if (editingChapter.chapterNumber > 0) editingChapter.chapterNumber.toString() else "",
            onDismiss = { chapterToEdit = null },
            onSave = { updatedTitle, updatedNumber ->
                viewModel.updateChapter(
                    editingChapter.copy(
                        title = updatedTitle,
                        chapterNumber = updatedNumber
                    )
                )
                chapterToEdit = null
            }
        )
    }

    // Dialog: Upload / Import Questions for Chapter (Stays on Subject Detail screen, NO redirect to test)
    if (chapterToUploadQuestions != null) {
        val targetChap = chapterToUploadQuestions!!
        ImportQuestionsDialog(
            subjects = subjects,
            chapters = chapters,
            preselectedChapterId = targetChap.id,
            preselectedSubjectId = targetChap.subjectId,
            viewModel = viewModel,
            onDismiss = { chapterToUploadQuestions = null },
            onImported = { count, chapterTitle ->
                // User uploaded questions successfully:
                // Stay right here on the Subject Detail screen. Do NOT redirect to Test screen!
                chapterToUploadQuestions = null
            }
        )
    }

    // Dialog: Create Chapter with AI Sanitization & File Uploads
    if (showNewChapterDialog) {
        CreateChapterDialog(
            subjectName = subjectName,
            subjectId = subjectId,
            viewModel = viewModel,
            isAiKeyAvailable = preferences?.apiKey?.isNotBlank() == true && preferences?.apiKey != "MY_GEMINI_API_KEY",
            onDismiss = { showNewChapterDialog = false },
            onChapterCreated = { chapterId ->
                showNewChapterDialog = false
                onOpenChapter(chapterId)
            }
        )
    }
}

@Composable
fun ChapterCard(
    chapter: ChapterEntity,
    accentColor: Color,
    viewModel: PrepOSViewModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onOpenTest: () -> Unit,
    onDelete: () -> Unit,
    onDeleteQuestions: (() -> Unit)? = null,
    onUploadQuestions: (() -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteQuestionsDialog by remember { mutableStateOf(false) }
    var showDeleteChapterDialog by remember { mutableStateOf(false) }
    val progressPct = if (chapter.readingProgress >= 0.98f) 100 else kotlin.math.round(chapter.readingProgress * 100).toInt().coerceIn(0, 100)
    val progressFloat = progressPct.toFloat()
    val dynamicColor = ProgressColor.forProgressSmart(progressFloat)
    val cardBorderColor = ProgressColor.border(progressFloat, if (isDark) 0.35f else 0.25f)
    val fogGradient = ProgressColor.rememberAnimatedFogBrush(progressFloat, isDark)

    // Test performance metrics
    val hasAttemptedTest = chapter.testAttemptedCount > 0
    val accuracyPct = if (hasAttemptedTest) {
        ((chapter.testCorrectCount.toFloat() / chapter.testAttemptedCount) * 100).toInt()
    } else {
        0
    }
    val scoreColor = if (hasAttemptedTest) ProgressColor.forProgress(accuracyPct.toFloat()) else dynamicColor

    val questionsList = remember(chapter.questionsJson) {
        viewModel.parseQuestionsFromJson(chapter.questionsJson)
    }
    val questionsCount = questionsList.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.2.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .testTag("chapter_card_${chapter.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(fogGradient)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (chapter.chapterNumber > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ProgressColor.softBg(progressFloat, if (isDark) 0.20f else 0.10f))
                                    .border(
                                        0.8.dp,
                                        ProgressColor.border(progressFloat, if (isDark) 0.40f else 0.25f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "CHAPTER ${chapter.chapterNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = dynamicColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                        Text(
                            text = chapter.title.ifBlank { "Untitled Chapter" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = if (isDark) Color(0xFF94A3B8) else Color.Gray)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (progressPct >= 100) Icons.Default.Refresh else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (progressPct >= 100) ProgressColor.Level3Yellow else ProgressColor.Level5Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (progressPct >= 100) "Mark as Unread (0%)" else "Mark as 100% Completed")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.markChapterCompleted(chapter.id, progressPct < 100)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Edit Chapter")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Upload Questions")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onUploadQuestions?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Quiz,
                                            contentDescription = null,
                                            tint = if (questionsCount > 0) MaterialTheme.colorScheme.error else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (questionsCount > 0) "Delete Questions ($questionsCount)" else "Delete Questions",
                                            color = if (questionsCount > 0) MaterialTheme.colorScheme.error else Color.Gray
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    if (questionsCount > 0) {
                                        showDeleteQuestionsDialog = true
                                    } else {
                                        Toast.makeText(context, "No questions found for this chapter", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delete Chapter", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteChapterDialog = true
                                }
                            )
                        }
                    }
                }

                if (chapter.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chapter.summary,
                        style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF94A3B8) else Color.Gray),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dedicated Test System Button & Score Badge (Requirement 5 & 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ProgressColor.softBg(if (hasAttemptedTest) accuracyPct.toFloat() else progressFloat, if (isDark) 0.16f else 0.10f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            ProgressColor.border(if (hasAttemptedTest) accuracyPct.toFloat() else progressFloat, if (isDark) 0.40f else 0.28f)
                        ),
                        modifier = Modifier.clickable { onOpenTest() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = "Chapter Test",
                                tint = scoreColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (hasAttemptedTest) {
                                Text(
                                    text = "Score: $accuracyPct% (${chapter.testCorrectCount}/${chapter.testAttemptedCount})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor,
                                        fontSize = 11.sp
                                    )
                                )
                            } else {
                                Text(
                                    text = if (questionsCount > 0) "Practice Quiz ($questionsCount Qs)" else "Take Quiz",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = dynamicColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "Tap to open →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = dynamicColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reading Progress Indicator (With dedicated Restart button when 100%)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            viewModel.markChapterCompleted(chapter.id, progressPct < 100)
                        }
                    ) {
                        if (progressPct >= 100) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = ProgressColor.Level5Green,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "100% Completed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ProgressColor.Level5Green,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                        } else {
                            Text(
                                text = if (progressPct > 0) "$progressPct% read" else "Not started",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (progressPct > 0) dynamicColor else (if (isDark) Color(0xFF94A3B8) else LightTextMuted),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (progressPct >= 100) {
                        // Explicit Restart Badge Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                viewModel.markChapterCompleted(chapter.id, false)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart Progress",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Restart",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B),
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                AnimatedProgressBar(
                    percent = progressFloat,
                    height = 4.dp
                )
            }
        }
    }

    // Dialog: Confirm Deleting All Questions for this Chapter
    if (showDeleteQuestionsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteQuestionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Questions?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all $questionsCount question${if (questionsCount != 1) "s" else ""} for \"${chapter.title}\"? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteQuestionsDialog = false
                        if (onDeleteQuestions != null) {
                            onDeleteQuestions()
                        } else {
                            viewModel.deleteAllQuestionsForChapter(chapter.id)
                        }
                        Toast.makeText(
                            context,
                            "Deleted $questionsCount questions from \"${chapter.title}\"",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Questions", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteQuestionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Confirm Deleting Chapter
    if (showDeleteChapterDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChapterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Chapter?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${chapter.title}\"? All its notes and questions will be permanently removed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteChapterDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChapterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditChapterDialog(
    initialTitle: String,
    initialNumber: String,
    onDismiss: () -> Unit,
    onSave: (title: String, chapterNumber: Int) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var numberText by remember { mutableStateOf(initialNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Chapter", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = numberText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) numberText = it },
                    label = { Text("Chapter Number") },
                    placeholder = { Text("e.g. 1, 2, 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chapter Title") },
                    placeholder = { Text("Chapter name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = numberText.toIntOrNull() ?: 0
                    val finalTitle = title.ifBlank { "Untitled Chapter" }
                    onSave(finalTitle, num)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChapterDialog(
    subjectName: String,
    subjectId: String,
    viewModel: PrepOSViewModel,
    isAiKeyAvailable: Boolean,
    onDismiss: () -> Unit,
    onChapterCreated: (chapterId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var chapterTitle by remember { mutableStateOf("") }
    var chapterNumberText by remember { mutableStateOf("") }
    var rawContentText by remember { mutableStateOf("") }
    var selectedInputTab by remember { mutableStateOf(0) } // 0: Paste, 1: TXT, 2: PDF, 3: Blank
    var isProcessing by remember { mutableStateOf(false) }
    var processingStatusMessage by remember { mutableStateOf("Structuring notes...") }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var aiFailureErrorMessage by remember { mutableStateOf<String?>(null) }
    var showConnectApiKeyDialog by remember { mutableStateOf(false) }

    if (showConnectApiKeyDialog) {
        com.example.ui.components.ConnectApiKeyDialog(
            onDismiss = { showConnectApiKeyDialog = false },
            onKeySaved = { savedKey ->
                showConnectApiKeyDialog = false
                viewModel.saveApiKey(savedKey)
                viewModel.saveAiProvider("GEMINI")
                Toast.makeText(context, "✓ Gemini API Key connected successfully!", Toast.LENGTH_SHORT).show()
            },
            featureTitle = "AI Online Sanitizer"
        )
    }

    // Retry / Offline Fallback Dialog on AI failure
    if (aiFailureErrorMessage != null) {
        val chapNum = chapterNumberText.toIntOrNull() ?: 0
        val titleToUse = chapterTitle.ifBlank { "Untitled Chapter" }

        AlertDialog(
            onDismissRequest = { aiFailureErrorMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Online AI Parsing Failed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = aiFailureErrorMessage ?: "An unexpected error occurred during online AI parsing.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Would you like to retry the online AI parser or switch to the quick offline parser to build your chapter immediately?",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val errorToClear = aiFailureErrorMessage
                        aiFailureErrorMessage = null
                        isProcessing = true
                        processingStatusMessage = "Retrying Online AI (Gemini)..."
                        viewModel.sanitizeAndCreateChapter(
                            subjectId = subjectId,
                            title = titleToUse,
                            chapterNumber = chapNum,
                            rawContent = rawContentText,
                            useAI = true,
                            onComplete = { newChapterId ->
                                isProcessing = false
                                onChapterCreated(newChapterId)
                            },
                            onError = { err ->
                                isProcessing = false
                                aiFailureErrorMessage = err
                            }
                        )
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Try Again")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { aiFailureErrorMessage = null }) {
                        Text("Cancel")
                    }
                    OutlinedButton(
                        onClick = {
                            aiFailureErrorMessage = null
                            isProcessing = true
                            processingStatusMessage = "Structuring with Local Offline Cleaner..."
                            viewModel.sanitizeAndCreateChapter(
                                subjectId = subjectId,
                                title = titleToUse,
                                chapterNumber = chapNum,
                                rawContent = rawContentText,
                                useAI = false,
                                onComplete = { newChapterId ->
                                    isProcessing = false
                                    onChapterCreated(newChapterId)
                                },
                                onError = { err ->
                                    isProcessing = false
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Offline Parser")
                    }
                }
            }
        )
    }

    // File picker for TXT files
    val txtFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingStatusMessage = "Extracting text file..."
                val extracted = ContentExtractor.extractTextFromUri(context, uri, "text/plain")
                isProcessing = false
                if (extracted.isNotBlank()) {
                    rawContentText = extracted
                    uploadedFileName = uri.lastPathSegment ?: "Text document"
                    if (chapterTitle.isBlank()) {
                        chapterTitle = uploadedFileName?.substringBeforeLast(".") ?: ""
                    }
                    Toast.makeText(context, "Text extracted successfully (${extracted.length} chars)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not extract text from file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // File picker for PDF files
    val pdfFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingStatusMessage = "Extracting text from PDF..."
                val extracted = ContentExtractor.extractTextFromUri(context, uri, "application/pdf")
                isProcessing = false
                if (extracted.isNotBlank()) {
                    rawContentText = extracted
                    uploadedFileName = uri.lastPathSegment ?: "PDF Document"
                    if (chapterTitle.isBlank()) {
                        chapterTitle = uploadedFileName?.substringBeforeLast(".") ?: ""
                    }
                    Toast.makeText(context, "PDF extracted successfully (${extracted.length} chars)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not extract readable text from PDF.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isDark = isAppDarkTheme()

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else GoldAccentBorderSubtle
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Header with icon and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Chapter to $subjectName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isDark) Color.White else LightTextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    if (!isProcessing) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(42.dp), strokeWidth = 3.5.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = processingStatusMessage,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cleaning markdown artifacts and establishing H1-H4 hierarchy...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Chapter Title & Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chapterTitle,
                            onValueChange = { chapterTitle = it },
                            label = { Text("Chapter Title") },
                            placeholder = { Text("e.g. Fundamentals of Logic") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chapter_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = chapterNumberText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) chapterNumberText = it },
                            label = { Text("Ch #") },
                            placeholder = { Text("1") },
                            singleLine = true,
                            modifier = Modifier
                                .width(76.dp)
                                .testTag("chapter_number_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Multi-Input Selection Tabs
                    Text(
                        text = "Content Source",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf("Paste Text", "Upload TXT", "Upload PDF", "Blank Note")
                        tabs.forEachIndexed { index, tabTitle ->
                            val isSelected = selectedInputTab == index
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle)
                                ),
                                modifier = Modifier.clickable { selectedInputTab = index }
                            ) {
                                Text(
                                    text = tabTitle,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else LightTextPrimary),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    // Content Input depending on tab
                    when (selectedInputTab) {
                        0 -> { // Paste Text
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${rawContentText.length} characters",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                                if (pasted.isNotBlank()) {
                                                    rawContentText = pasted
                                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste Clipboard", fontSize = 12.sp)
                                    }
                                }

                                OutlinedTextField(
                                    value = rawContentText,
                                    onValueChange = { rawContentText = it },
                                    placeholder = { Text("Paste raw notes, syllabus, markdown text, or OCR extracts here...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .testTag("paste_content_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 8
                                )
                            }
                        }
                        1 -> { // Upload TXT
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else LightSurfaceSecondary)
                                    .border(1.dp, if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uploadedFileName ?: "Select a .txt or .text note file",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { txtFileLauncher.launch("text/*") },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose TXT File")
                                }
                            }
                        }
                        2 -> { // Upload PDF
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else LightSurfaceSecondary)
                                    .border(1.dp, if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uploadedFileName ?: "Select a study material PDF",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { pdfFileLauncher.launch("application/pdf") },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose PDF Document")
                                }
                            }
                        }
                        3 -> { // Blank Note
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else LightSurfaceSecondary)
                                    .border(1.dp, if (isDark) Color(0xFF334155) else GoldAccentBorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Creates a fresh blank continuous note with ruled paper ready for your handwriting or typing.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons Column
                    val chapNum = chapterNumberText.toIntOrNull() ?: 0
                    val titleToUse = chapterTitle.ifBlank { "Untitled Chapter" }

                    if (selectedInputTab != 3 && rawContentText.isNotBlank()) {
                        // Option 1: AI Online Sanitizer (Gemini API with deep restructuring)
                        Button(
                            onClick = {
                                if (!isAiKeyAvailable) {
                                    showConnectApiKeyDialog = true
                                } else {
                                    isProcessing = true
                                    processingStatusMessage = "Sanitizing with Online AI (Gemini)..."
                                    viewModel.sanitizeAndCreateChapter(
                                        subjectId = subjectId,
                                        title = titleToUse,
                                        chapterNumber = chapNum,
                                        rawContent = rawContentText,
                                        useAI = true,
                                        onComplete = { newChapterId ->
                                            isProcessing = false
                                            onChapterCreated(newChapterId)
                                        },
                                        onError = { err ->
                                            isProcessing = false
                                            aiFailureErrorMessage = err
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("ai_online_sanitizer_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Online Sanitizer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Option 2: Quick Local Cleaner (Offline deterministic parsing)
                        OutlinedButton(
                            onClick = {
                                isProcessing = true
                                processingStatusMessage = "Structuring with Local Cleaner..."
                                viewModel.sanitizeAndCreateChapter(
                                    subjectId = subjectId,
                                    title = titleToUse,
                                    chapterNumber = chapNum,
                                    rawContent = rawContentText,
                                    useAI = false,
                                    onComplete = { newChapterId ->
                                        isProcessing = false
                                        onChapterCreated(newChapterId)
                                    },
                                    onError = { err ->
                                        isProcessing = false
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("quick_local_cleaner_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Local Cleaner (Offline)", color = if (isDark) Color.White else LightTextPrimary, fontSize = 14.sp)
                        }
                    } else {
                        // Blank / Standard creation
                        Button(
                            onClick = {
                                viewModel.createChapter(
                                    subjectId = subjectId,
                                    title = titleToUse,
                                    summary = ""
                                ) { newId ->
                                    onChapterCreated(newId)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("create_blank_chapter_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Chapter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Cancel button cleanly below
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel",
                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
}

