package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.ProgressColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ai.StudyAIService
import com.example.model.DocElement
import com.example.model.NoteFont
import com.example.model.PaperStyle
import com.example.model.PaperTheme
import com.example.ui.editor.AskAIDialog
import com.example.ui.editor.ChapterQuestionsSheet
import com.example.ui.editor.ContinuousDocumentView
import com.example.ui.editor.FormattingToolbar
import com.example.ui.editor.InsertContentSheet
import com.example.ui.editor.PreferencesSheet
import com.example.ui.editor.TopicNavigationSheet
import com.example.ui.paper.getPaperColors
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    chapterId: String,
    viewModel: PrepOSViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Hide status bar in full-screen reading/editing experience
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    LaunchedEffect(chapterId) {
        viewModel.openChapter(chapterId)
    }

    val state by viewModel.editorState.collectAsState()
    val preferences by viewModel.preferences.collectAsState(initial = null)
    val chapterEntity by viewModel.observeChapter(chapterId).collectAsState(initial = null)

    val coroutineScope = rememberCoroutineScope()

    // Track active note reading & study time (every second counts)
    DisposableEffect(chapterId, chapterEntity?.title) {
        val timerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000L)
                viewModel.recordActiveStudySeconds(1, chapterTitle = chapterEntity?.title)
            }
        }
        onDispose {
            timerJob.cancel()
            viewModel.flushActiveStudySeconds()
        }
    }
    val questions = remember(chapterEntity?.questionsJson) {
        viewModel.parseQuestionsFromJson(chapterEntity?.questionsJson ?: "")
    }

    val topics = remember(state.document) {
        state.document.extractTopics()
    }

    var showPreferencesSheet by remember { mutableStateOf(false) }
    var showInsertSheet by remember { mutableStateOf(false) }
    var showAskAIDialog by remember { mutableStateOf(false) }
    var showQuestionsSheet by remember { mutableStateOf(false) }
    var showTopicNavigationSheet by remember { mutableStateOf(false) }
    var targetScrollBlockId by remember { mutableStateOf<String?>(null) }
    var aiPreselectedTopicId by remember { mutableStateOf<String?>(null) }
    var selectedDiagramForAi by remember { mutableStateOf<DocElement.DiagramBlock?>(null) }

    var localReadingProgress by remember(state.chapterId) {
        mutableStateOf(state.readingProgress)
    }

    LaunchedEffect(state.readingProgress) {
        localReadingProgress = state.readingProgress
    }

    val currentReadingProgress = localReadingProgress
    val progressPct = if (currentReadingProgress >= 0.98f) 100 else kotlin.math.round(currentReadingProgress * 100).toInt().coerceIn(0, 100)
    val paperColors = getPaperColors(state.paperTheme)

    Scaffold(
        topBar = {
            // Ultra-Compact Top Bar matching the active reading paper & theme colors
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = paperColors.backgroundColor,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(paperColors.backgroundColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(29.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = paperColors.textColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Chapter Title & Reading Progress
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = state.chapterTitle.ifBlank { "Study Notes" },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = paperColors.textColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = ProgressColor.softBg(progressPct.toFloat(), 0.18f),
                                modifier = Modifier.clickable {
                                    val newCompleted = progressPct < 100
                                    viewModel.markChapterCompleted(state.chapterId, newCompleted)
                                }
                            ) {
                                Text(
                                    text = if (progressPct >= 100) "100% ✓" else "$progressPct%",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = ProgressColor.forProgress(progressPct.toFloat())
                                    )
                                )
                            }

                            if (progressPct >= 100) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = ProgressColor.Level3Yellow.copy(alpha = 0.2f),
                                    modifier = Modifier.clickable {
                                        viewModel.markChapterCompleted(state.chapterId, false)
                                    }
                                ) {
                                    Text(
                                        text = "Restart ↺",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = ProgressColor.Level3Yellow
                                        )
                                    )
                                }
                            }
                        }

                        // Topic List / Outline Navigation Button
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = paperColors.patternColor.copy(alpha = 0.3f),
                            modifier = Modifier
                                .clickable { showTopicNavigationSheet = true }
                                .testTag("editor_topics_outline_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Topic Navigation",
                                    tint = paperColors.textColor.copy(alpha = 0.9f),
                                    modifier = Modifier.size(11.dp)
                                )
                                if (topics.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${topics.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            color = paperColors.textColor
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Settings Button (Font / Paper)
                        IconButton(
                            onClick = { showPreferencesSheet = true },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("editor_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = paperColors.textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Compact Done / Edit Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (state.isEditMode) MaterialTheme.colorScheme.primary else paperColors.patternColor.copy(alpha = 0.35f),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { viewModel.toggleEditMode() }
                                .testTag("toggle_edit_mode_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = if (state.isEditMode) "Done" else "Edit",
                                    tint = if (state.isEditMode) Color.White else paperColors.textColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (state.isEditMode) "Done" else "Edit",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        color = if (state.isEditMode) Color.White else paperColors.textColor
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!state.isEditMode) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = {
                        aiPreselectedTopicId = null
                        showAskAIDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Ask AI",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    containerColor = Color(0xFF7C3AED),
                    modifier = Modifier.testTag("reading_mode_ask_ai_fab")
                )
            }
        },
        bottomBar = {
            if (state.isEditMode) {
                FormattingToolbar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    formattingState = state.formattingState,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onHeadingChange = { viewModel.applyHeadingStyle(it) },
                    onToggleBold = { viewModel.toggleInlineBold() },
                    onToggleItalic = { viewModel.toggleInlineItalic() },
                    onToggleUnderline = { viewModel.toggleInlineUnderline() },
                    onToggleStrikethrough = { viewModel.toggleInlineStrikethrough() },
                    onTextColorSelected = { viewModel.setTextColor(it) },
                    onHighlightColorSelected = { viewModel.setHighlightColor(it) },
                    onToggleBulletList = { viewModel.toggleBulletList() },
                    onToggleNumberedList = { viewModel.toggleNumberedList() },
                    onAlignmentChange = { viewModel.setAlignment(it) },
                    onOpenInsertSheet = { showInsertSheet = true },
                    onOpenAskAI = {
                        aiPreselectedTopicId = null
                        showAskAIDialog = true
                    },
                    hasSelection = state.activeSelectedText.isNotBlank()
                )
            }
        }
    ) { innerPadding ->
        if (state.chapterId != chapterId) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(paperColors.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = paperColors.textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )
            }
        } else {
            ContinuousDocumentView(
                document = state.document,
                isEditMode = state.isEditMode,
                paperStyle = state.paperStyle,
                paperTheme = state.paperTheme,
                fontFamily = state.fontFamily,
                fontSizeSp = state.fontSizeSp,
                lineSpacing = state.lineSpacingMultiplier,
                initialScrollY = state.lastReadScrollY,
                targetScrollBlockId = targetScrollBlockId,
                onScrollToBlockHandled = { targetScrollBlockId = null },
                onScrollProgressChanged = { progress, scrollY ->
                    localReadingProgress = progress
                    viewModel.updateReadingScroll(progress, scrollY)
                },
                onDocumentChanged = { updatedDoc ->
                    viewModel.onDocumentUpdated(updatedDoc)
                },
                onSelectionChanged = { selectedText, blockId, startOffset, endOffset ->
                    viewModel.updateSelection(selectedText, blockId, startOffset, endOffset)
                },
                onRequestEditMode = {
                    viewModel.toggleEditMode(true)
                },
                onAskAiForDiagram = { diagram ->
                    selectedDiagramForAi = diagram
                    showAskAIDialog = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    // Modal: Chapter Topics Outline Navigation Sheet
    if (showTopicNavigationSheet) {
        TopicNavigationSheet(
            chapterTitle = state.chapterTitle,
            topics = topics,
            onDismiss = { showTopicNavigationSheet = false },
            onTopicSelected = { selectedTopic ->
                showTopicNavigationSheet = false
                targetScrollBlockId = selectedTopic.headingElementId
            },
            onAskAIForTopic = { selectedTopic ->
                showTopicNavigationSheet = false
                aiPreselectedTopicId = selectedTopic.topicId
                showAskAIDialog = true
            }
        )
    }

    // Modal: Chapter Questions & Quiz Sheet
    if (showQuestionsSheet) {
        ChapterQuestionsSheet(
            questions = questions,
            chapterTitle = state.chapterTitle,
            onDismiss = { showQuestionsSheet = false },
            onAddQuestion = { newQ ->
                viewModel.addQuestionToChapter(chapterId, newQ)
            },
            onDeleteQuestion = { qId ->
                viewModel.deleteQuestionFromChapter(chapterId, qId)
            },
            onDeleteAllQuestions = {
                viewModel.deleteAllQuestionsForChapter(chapterId)
            }
        )
    }

    // Modal: Preferences & Paper Settings Sheet
    if (showPreferencesSheet) {
        val defaultPaperStyle = remember(preferences?.defaultPaperStyle) {
            try { PaperStyle.valueOf(preferences?.defaultPaperStyle ?: "RULED") } catch (e: Exception) { PaperStyle.RULED }
        }
        val defaultTheme = remember(preferences?.defaultThemeMode) {
            try { PaperTheme.valueOf(preferences?.defaultThemeMode ?: "PAPER_LIGHT") } catch (e: Exception) { PaperTheme.PAPER_LIGHT }
        }
        val defaultFont = remember(preferences?.defaultFontFamily) {
            try { NoteFont.valueOf(preferences?.defaultFontFamily ?: "SANS_SERIF") } catch (e: Exception) { NoteFont.SANS_SERIF }
        }
        val defaultFontSize = preferences?.defaultFontSizeSp ?: 16f
        val defaultLineSpacing = preferences?.defaultLineSpacing ?: 1.4f

        PreferencesSheet(
            currentPaperStyle = state.paperStyle,
            defaultPaperStyle = defaultPaperStyle,
            currentTheme = state.paperTheme,
            defaultTheme = defaultTheme,
            currentFont = state.fontFamily,
            defaultFont = defaultFont,
            fontSizeSp = state.fontSizeSp,
            defaultFontSizeSp = defaultFontSize,
            lineSpacing = state.lineSpacingMultiplier,
            defaultLineSpacing = defaultLineSpacing,
            onDismiss = { showPreferencesSheet = false },
            onPaperStyleChange = { viewModel.setPaperStyle(it) },
            onSetDefaultPaperStyle = { viewModel.setDefaultPaperStyle(it) },
            onThemeChange = { viewModel.setPaperTheme(it) },
            onSetDefaultTheme = { viewModel.setDefaultPaperTheme(it) },
            onFontChange = { viewModel.setFont(it) },
            onSetDefaultFont = { viewModel.setDefaultFont(it) },
            onFontSizeChange = { viewModel.setFontSize(it) },
            onSetDefaultFontSize = { viewModel.setDefaultFontSize(it) },
            onLineSpacingChange = { viewModel.setLineSpacing(it) },
            onSetDefaultLineSpacing = { viewModel.setDefaultLineSpacing(it) },
            onSaveAllAsDefault = { viewModel.saveAllCurrentSettingsAsDefault() }
        )
    }

    // Modal: Insert Content Sheet (Images, Tables, Diagrams, Callouts)
    if (showInsertSheet) {
        InsertContentSheet(
            onDismiss = { showInsertSheet = false },
            onInsertElement = { element ->
                viewModel.insertDocElement(element)
            },
            onPasteSmartContent = { rawText ->
                viewModel.pasteAndSplitContent(rawText)
            }
        )
    }

    // Modal: Ask AI Selection Assistant
    if (showAskAIDialog) {
        val allSubjects by viewModel.subjects.collectAsState(initial = emptyList())
        val currentSubject = allSubjects.find { it.id == chapterEntity?.subjectId }
        val subjectName = currentSubject?.name ?: ""

        AskAIDialog(
            selectedText = state.activeSelectedText,
            fullChapterText = state.document.toPlainText(),
            chapterTitle = state.chapterTitle,
            subjectName = subjectName,
            chapterId = chapterId,
            topics = topics,
            initialSelectedTopicId = aiPreselectedTopicId,
            selectedDiagram = selectedDiagramForAi,
            onUpdateDiagram = { updated ->
                viewModel.updateDiagramBlock(updated)
                showAskAIDialog = false
                selectedDiagramForAi = null
            },
            apiKey = preferences?.apiKey ?: "",
            model = preferences?.selectedAiModel ?: StudyAIService.DEFAULT_MODEL,
            provider = preferences?.selectedAiProvider ?: "GEMINI",
            deepSeekApiKey = preferences?.deepSeekApiKey ?: "",
            viewModel = viewModel,
            onDismiss = {
                showAskAIDialog = false
                aiPreselectedTopicId = null
                selectedDiagramForAi = null
            },
            onReplaceSelection = { replacement ->
                viewModel.replaceSelectionWithAI(replacement)
            },
            onInsertBelow = { inserted ->
                viewModel.insertAIBelow(inserted)
            },
            onUpdateTopic = { topicId, updatedContent ->
                viewModel.updateTopicContent(chapterId, topicId, updatedContent)
            },
            onInsertIntoTopic = { topicId, additionalContent ->
                viewModel.insertContentIntoTopic(chapterId, topicId, additionalContent)
            },
            onDuplicateTopic = { topicId, duplicatedContent ->
                viewModel.duplicateTopicContent(chapterId, topicId, duplicatedContent)
            }
        )
    }
}
