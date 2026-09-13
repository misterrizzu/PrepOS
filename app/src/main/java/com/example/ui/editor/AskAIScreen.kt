package com.example.ui.editor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppDarkBorder
import com.example.ui.theme.AppDarkSurface
import com.example.ui.theme.AppDarkSurfaceVariant
import com.example.ui.theme.isAppDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.ai.AIAttachment
import com.example.ai.StudyAIService
import com.example.model.ChapterTopic
import com.example.model.DocElement
import com.example.model.DiagramType
import com.example.ui.components.AiMarkdownMessage
import com.example.ui.components.PrepOSBottomNavBar
import com.example.ui.components.buildMarkdownAnnotatedString
import com.example.ui.theme.*
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.absoluteValue

// -----------------------------------------------------------------------------------------
// DATA MODELS
// -----------------------------------------------------------------------------------------
data class AttachedUiMedia(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String,
    val isImage: Boolean,
    val uri: Uri? = null,
    val bitmap: Bitmap? = null,
    val textSnippet: String? = null,
    val sizeText: String = "",
    val base64Data: String? = null
)

data class AskAIChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val actionTag: String? = null,
    val text: String,
    val attachments: List<AttachedUiMedia> = emptyList(),
    val options: List<String> = emptyList(),
    val isApprovalProposal: Boolean = false,
    val proposalType: AssistantWorkflowType? = null,
    val artifact: AiResultArtifact? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class HelpCarouselCard(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val defaultPrompt: String,
    val workflowType: AssistantWorkflowType? = null,
    val tag: String = "Conversational",
    val badges: List<String> = emptyList(),
    val actionText: String = "Start Plan"
)

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val prompt: String,
    val workflowType: AssistantWorkflowType? = null
)

data class ConversationalQuestion(
    val id: String,
    val introMessage: String? = null,
    val questionText: String,
    val options: List<String> = emptyList()
)

data class ActiveWorkflowSession(
    val type: AssistantWorkflowType,
    val questions: List<ConversationalQuestion>,
    var currentStepIndex: Int = 0,
    val answers: MutableMap<String, String> = mutableMapOf(),
    val accumulatedAttachments: MutableList<AttachedUiMedia> = mutableListOf(),
    var isProposalActive: Boolean = false,
    var isCompleted: Boolean = false
)

private var hasPromptedNameGlobally = false

enum class TargetScopeType {
    SELECTION,
    TOPIC,
    WHOLE_CHAPTER,
    DIAGRAM
}

data class ContentTransformation(
    val title: String,
    val emoji: String,
    val subtitle: String,
    val promptInstruction: String
)

// -----------------------------------------------------------------------------------------
// MAIN SCREEN COMPOSABLE
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AskAIScreen(
    viewModel: PrepOSViewModel? = null,
    selectedText: String = "",
    fullChapterText: String = "",
    chapterTitle: String = "PrepOS Study Assistant",
    subjectName: String = "",
    chapterId: String = "",
    topics: List<ChapterTopic> = emptyList(),
    initialSelectedTopicId: String? = null,
    selectedDiagram: DocElement.DiagramBlock? = null,
    onUpdateDiagram: (DocElement.DiagramBlock) -> Unit = {},
    apiKey: String = "",
    model: String = StudyAIService.DEFAULT_MODEL,
    provider: String = "GEMINI",
    deepSeekApiKey: String = "",
    onDismiss: () -> Unit = {},
    onReplaceSelection: (String) -> Unit = {},
    onInsertBelow: (String) -> Unit = {},
    onUpdateTopic: (topicId: String, newContent: String) -> Unit = { _, _ -> },
    onInsertIntoTopic: (topicId: String, newContent: String) -> Unit = { _, _ -> },
    onDuplicateTopic: (topicId: String, newContent: String) -> Unit = { _, _ -> },
    onNavigateTab: (String) -> Unit = {}
) {
    val isDark = isAppDarkTheme()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    val isContextualMode by remember(selectedDiagram, selectedText, fullChapterText, chapterTitle, subjectName) {
        derivedStateOf {
            selectedDiagram != null || selectedText.isNotBlank() || (chapterTitle.isNotBlank() && chapterTitle != "PrepOS Study Assistant") || subjectName.isNotBlank()
        }
    }
    val isChapterEmpty by remember(fullChapterText) {
        derivedStateOf { fullChapterText.isBlank() }
    }

    // Preferences & ViewModel State
    val preferences by (viewModel?.preferences?.collectAsState(initial = null) ?: remember { mutableStateOf(null) })
    val exams by (viewModel?.exams?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val subjects by (viewModel?.subjects?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val allRecentChapters by (viewModel?.chapters?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    val effectiveApiKey = apiKey.ifBlank { preferences?.apiKey ?: "" }
    val effectiveDeepSeekKey = deepSeekApiKey.ifBlank { preferences?.deepSeekApiKey ?: "" }
    val effectiveModel = model.ifBlank { preferences?.selectedAiModel ?: StudyAIService.DEFAULT_MODEL }
    val effectiveProvider = provider.ifBlank { preferences?.selectedAiProvider ?: "GEMINI" }

    val activeProvider by remember(effectiveProvider) { mutableStateOf(effectiveProvider) }
    val activeModel by remember(effectiveModel) { mutableStateOf(effectiveModel) }

    // Preferred User Name (Strictly only prompt if outside note reading and not already saved/prompted)
    val savedUserName = preferences?.preferredUserName?.trim() ?: ""
    var showNamePopup by remember { mutableStateOf(false) }

    var triggerLaunchCamera by remember { mutableStateOf(false) }
    var triggerLaunchImagePicker by remember { mutableStateOf(false) }
    var triggerLaunchDocPicker by remember { mutableStateOf(false) }
    var currentCameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var showInAppCameraScanner by remember { mutableStateOf(false) }

    // Active Study Time tracking while studying with AI Tutor
    DisposableEffect(Unit) {
        val timerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000L)
                viewModel?.recordActiveStudySeconds(1, subjectName = "AI Tutor")
            }
        }
        onDispose {
            timerJob.cancel()
            viewModel?.flushActiveStudySeconds()
        }
    }

    LaunchedEffect(preferences, savedUserName, isContextualMode) {
        if (!isContextualMode && preferences != null && savedUserName.isBlank() && !hasPromptedNameGlobally) {
            showNamePopup = true
            hasPromptedNameGlobally = true
        }
    }

    // Target Scope State
    var activeScopeType by remember(selectedDiagram, selectedText, initialSelectedTopicId) {
        mutableStateOf(
            if (selectedDiagram != null) TargetScopeType.DIAGRAM
            else if (selectedText.isNotBlank()) TargetScopeType.SELECTION
            else if (initialSelectedTopicId != null && topics.any { it.topicId == initialSelectedTopicId }) TargetScopeType.TOPIC
            else if (topics.isNotEmpty()) TargetScopeType.TOPIC
            else TargetScopeType.WHOLE_CHAPTER
        )
    }

    var selectedTopicId by remember(initialSelectedTopicId, topics) {
        mutableStateOf(
            initialSelectedTopicId ?: topics.firstOrNull()?.topicId
        )
    }

    // The 5 Content Transformation Variants
    val contentTransformations = remember {
        listOf(
            ContentTransformation(
                title = "Enhance & Enrich",
                emoji = "🧠",
                subtitle = "Add useful supporting details, context, examples, definitions and connections without removing any original information.",
                promptInstruction = """
                    TRANSFORMATION: Enhance & Enrich
                    TASK: Add useful supporting details, context, real-world examples, precise definitions, and conceptual connections to the content below.
                    CRITICAL MANDATE: DO NOT remove, delete, or omit any original facts or points. Seamlessly enrich the existing content so it is more comprehensive, crystal-clear and educational.
                """.trimIndent()
            ),
            ContentTransformation(
                title = "Structure & Organize",
                emoji = "🗂️",
                subtitle = "Convert the content into a clean hierarchy with H2/H3 topics, subtopics, bullets, numbered lists, sections and logical grouping.",
                promptInstruction = """
                    TRANSFORMATION: Structure & Organize
                    TASK: Convert the content below into a clean, well-structured hierarchy.
                    Use clear Markdown H2/H3 headings, logical thematic sections, concise bullet points, numbered sequential steps, and bold key terms.
                """.trimIndent()
            ),
            ContentTransformation(
                title = "Visualize & Map",
                emoji = "📊",
                subtitle = "Identify where the content benefits from tables, comparison charts, timelines, flowcharts, trees, concept maps and other supported visuals.",
                promptInstruction = """
                    TRANSFORMATION: Visualize & Map
                    TASK: Transform the content below into structured visual representations:
                    - Use clear Markdown Comparison Tables with column headers for contrasting concepts.
                    - Use step-by-step Flowcharts or Tree breakdowns (using bullet hierarchy or ASCII flowcharts).
                    - Map out key timelines or relationships clearly.
                """.trimIndent()
            ),
            ContentTransformation(
                title = "Smart Highlight",
                emoji = "✨",
                subtitle = "Identify and emphasize key facts, important terms, dates, numbers, formulas, definitions, exam-important points and confusing distinctions without rewriting everything.",
                promptInstruction = """
                    TRANSFORMATION: Smart Highlight
                    TASK: Identify and emphasize key facts, formulas, essential terminology, dates, numbers, definitions, and exam-critical distinctions in the content below.
                    DO NOT rewrite everything unnecessarily. Keep the core text intact, but apply strategic **bolding**, `code/formula emphasis`, and callouts (> [!IMPORTANT] / > [!NOTE]) for instant visual retention.
                """.trimIndent()
            ),
            ContentTransformation(
                title = "Optimize for Revision",
                emoji = "⚡",
                subtitle = "Transform the content into a compact, high-retention revision format while preserving all essential information — quick facts, summaries, comparisons, memory cues and recall points.",
                promptInstruction = """
                    TRANSFORMATION: Optimize for Revision
                    TASK: Transform the content below into a compact, high-retention revision summary.
                    Preserve ALL essential information, but format it for rapid recall:
                    • Core Axioms, Rules & Formulas
                    • High-Yield Memory Cues & Mnemonics
                    • Rapid Recall Bullets
                    • Common Traps & Confusing Distinctions
                """.trimIndent()
            )
        )
    }

    // Chat Messages & Media State
    val chatMessages = remember { mutableStateListOf<AskAIChatMessage>() }
    val attachedMediaList = remember { mutableStateListOf<AttachedUiMedia>() }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Active Conversational Workflow State Machine
    var activeWorkflow by remember { mutableStateOf<ActiveWorkflowSession?>(null) }

    // Direct Custom input focus & dynamic dim placeholder
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var customPlaceholderHint by remember { mutableStateOf<String?>(null) }

    // Chat expansion & bottom nav behavior
    val isConversationActive by remember {
        derivedStateOf { chatMessages.isNotEmpty() || isGenerating || activeWorkflow != null }
    }

    // Intercept back press when conversation is active to minimize chat
    BackHandler(enabled = isConversationActive) {
        chatMessages.clear()
        activeWorkflow = null
        isGenerating = false
        customPlaceholderHint = null
    }

    // Menu States
    var showMoreMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    // 8 AI Action Cards with distinctive visual tags and feature highlights
    val carouselCards = remember {
        listOf(
            HelpCarouselCard(
                id = "how_prepos_works",
                title = "How does PrepOS work?",
                description = "Learn how to use Note Editor, Test Screen, Focus Hub, and AI study planning.",
                icon = Icons.Default.Explore,
                gradientColors = listOf(Color(0xFFA855F7), Color(0xFF6366F1)),
                defaultPrompt = "How does PrepOS work and how do I use it effectively?",
                workflowType = AssistantWorkflowType.HOW_IT_WORKS,
                tag = "🧭 100% OFFLINE GUIDE",
                badges = listOf("Interactive Tour", "Zero Jargon"),
                actionText = "Explore Guide"
            ),
            HelpCarouselCard(
                id = "weekly_plan",
                title = "Create my weekly plan",
                description = "I'll ask a few questions and build a 7-day timetable aligned with your exam targets.",
                icon = Icons.Default.DateRange,
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
                defaultPrompt = "Help me build an optimal 7-day study timetable.",
                workflowType = AssistantWorkflowType.WEEKLY_PLAN,
                tag = "📅 7-DAY TIMETABLE",
                badges = listOf("Daily Breakdown", "1-Tap Sync"),
                actionText = "Start Timetable Plan"
            ),
            HelpCarouselCard(
                id = "create_notes_source",
                title = "Create Notes from Source",
                description = "Attach book photos, PDF pages, or paste text to generate structured active recall notes.",
                icon = Icons.Default.Description,
                gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
                defaultPrompt = "Help me turn source material into structured study notes.",
                workflowType = AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE,
                tag = "📝 ACTIVE RECALL",
                badges = listOf("Vision & PDF Notes", "Blur Mode Ready"),
                actionText = "Create Source Notes"
            ),
            HelpCarouselCard(
                id = "free_time",
                title = "Plan around my free time",
                description = "I'll understand your daily schedule and build a high-retention routine that fits your free slots.",
                icon = Icons.Default.Schedule,
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C)),
                defaultPrompt = "Help me build a study routine tailored around my free hours.",
                workflowType = AssistantWorkflowType.FREE_TIME_SCHEDULE,
                tag = "⏱️ SMART SCHEDULER",
                badges = listOf("Flexible Slots", "Pomodoro Pacing"),
                actionText = "Schedule Free Hours"
            ),
            HelpCarouselCard(
                id = "syllabus",
                title = "Set up my syllabus",
                description = "Tell me your exam and I'll structure it into subjects, chapters, and high-yield topics.",
                icon = Icons.Default.FolderSpecial,
                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
                defaultPrompt = "Help me organize and set up my syllabus workspace.",
                workflowType = AssistantWorkflowType.SETUP_SYLLABUS,
                tag = "📚 SYLLABUS BUILDER",
                badges = listOf("Full Library Setup", "High-Yield Topics"),
                actionText = "Build Syllabus"
            ),
            HelpCarouselCard(
                id = "exam_strategy",
                title = "Plan my exam strategy",
                description = "I'll build a phased roadmap based on remaining days, prep level, and high-yield areas.",
                icon = Icons.Default.TrackChanges,
                gradientColors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
                defaultPrompt = "Design a high-yield exam preparation strategy for my target exam.",
                workflowType = AssistantWorkflowType.EXAM_STRATEGY,
                tag = "🎯 PHASED ROADMAP",
                badges = listOf("Foundation to Mocks", "Score Milestones"),
                actionText = "Create Roadmap"
            ),
            HelpCarouselCard(
                id = "focus_next",
                title = "What should I focus on next?",
                description = "I'll recommend your single highest-impact topic and concrete action items for right now.",
                icon = Icons.Default.TrendingUp,
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                defaultPrompt = "Recommend what I should study right now for maximum retention.",
                workflowType = AssistantWorkflowType.FOCUS_NEXT,
                tag = "⚡ PRIORITY TARGET",
                badges = listOf("Weak Spot Analysis", "Quick Wins"),
                actionText = "Find Next Topic"
            ),
            HelpCarouselCard(
                id = "study_question",
                title = "Ask any study question",
                description = "Clear concepts, difficult doubts, math formulas, and active recall practice.",
                icon = Icons.Default.School,
                gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
                defaultPrompt = "Explain key concepts clearly with examples.",
                tag = "✦ 24/7 AI COPILOT",
                badges = listOf("Step-by-Step Clarity", "Concept Breakdown"),
                actionText = "Ask Study Doubt"
            )
        )
    }

    // Pager State for centered snapping carousel
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { carouselCards.size }
    )

    // Smooth Auto-advance Carousel (automatically pauses when user is swiping or in conversation)
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, isConversationActive) {
        if (!isConversationActive && !pagerState.isScrollInProgress && pagerState.pageCount > 0) {
            delay(6500)
            if (!isConversationActive && !pagerState.isScrollInProgress && pagerState.pageCount > 0) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // Distinct Quick Study Prompts (Instant AI tools & active recall triggers, NOT repeating carousel workflows)
    val quickActions = remember {
        listOf(
            QuickActionItem("💡 Explain Simply", Icons.Outlined.Lightbulb, "Explain the core concepts of my current topic in very simple terms using everyday analogies."),
            QuickActionItem("🧠 Pop Recall Quiz", Icons.Default.AutoAwesome, "Give me a quick 3-question active recall pop quiz on high-yield concepts to test my memory."),
            QuickActionItem("⚡ Formula Sheet", Icons.Default.Bolt, "Summarize the most important formulas, equations, and definitions in a crisp cheat sheet for quick revision."),
            QuickActionItem("🎯 Common Pitfalls", Icons.Default.TrackChanges, "What are the common exam traps, tricky misconceptions, and frequent mistakes students make in this topic?"),
            QuickActionItem("🔍 Memory Mnemonics", Icons.Default.School, "Create memorable mnemonics and memory shortcuts to easily remember tricky sequences and classifications."),
            QuickActionItem("📝 2-MCQ Speed Drill", Icons.Default.Description, "Generate 2 challenging multiple choice practice questions with detailed conceptual explanations for each option."),
            QuickActionItem("⏱️ 15-Min Rapid Review", Icons.Default.Schedule, "Give me a high-intensity 15-minute rapid review checklist to consolidate what I learned today.")
        )
    }

    // Build Question sets for Predefined Workflows
    fun getQuestionsForWorkflow(type: AssistantWorkflowType): List<ConversationalQuestion> {
        val subjectOptions = if (subjects.isNotEmpty()) {
            subjects.map { it.name } + listOf("All Enrolled Subjects")
        } else {
            listOf("Maths & Quantitative", "Science & Tech", "General Studies", "Languages")
        }

        val rawQuestions = when (type) {
            AssistantWorkflowType.FREE_TIME_SCHEDULE -> listOf(
                ConversationalQuestion(
                    id = "exam_date",
                    introMessage = "Let's create a study plan tailored around your free time.",
                    questionText = "First, when is your exam or target completion date?",
                    options = listOf("In 1 Month", "In 3 Months", "In 6 Months", "Flexible / Ongoing")
                ),
                ConversationalQuestion(
                    id = "subject_count",
                    questionText = "Great. How many subjects do you need to prepare for?",
                    options = listOf("2 Subjects", "3-4 Subjects", "5+ Subjects", "Single Subject Deep Dive")
                ),
                ConversationalQuestion(
                    id = "daily_hours",
                    questionText = "How much study time can you dedicate each day?",
                    options = listOf("1.5 Hours", "3 Hours", "4.5 Hours", "6+ Hours")
                ),
                ConversationalQuestion(
                    id = "time_window",
                    questionText = "What time of day are you usually free to study?",
                    options = listOf("Early Morning (5-8 AM)", "Afternoon (1-4 PM)", "Evening / Night (7-11 PM)", "Split Morning & Evening")
                ),
                ConversationalQuestion(
                    id = "session_format",
                    questionText = "Do you prefer deep study blocks (50 min) or short Pomodoro bursts (25 min)?",
                    options = listOf("25 min Pomodoro", "50 min Deep Block", "90 min Intensive", "Mixed Flexible")
                )
            )

            AssistantWorkflowType.WEEKLY_PLAN -> listOf(
                ConversationalQuestion(
                    id = "target_exam",
                    introMessage = "Let's create your personalized 7-day study timetable.",
                    questionText = "Which target exam or course are you preparing for?",
                    options = listOf("Competitive / Entrance", "College Finals", "School Board Exams", "Professional Certification")
                ),
                ConversationalQuestion(
                    id = "schedule_representation",
                    questionText = "How should we structure your study time representation?",
                    options = listOf("Flexible Daily Time", "Fixed Time Slots", "Different Time Each Day")
                ),
                ConversationalQuestion(
                    id = "daily_hours",
                    questionText = "How many hours of focused study can you commit per day?",
                    options = listOf("2 Hours / day", "3 Hours / day", "4 Hours / day", "6+ Hours / day")
                ),
                ConversationalQuestion(
                    id = "subject_focus",
                    questionText = "Which subjects should we distribute across the week?",
                    options = subjectOptions
                ),
                ConversationalQuestion(
                    id = "revision_pref",
                    questionText = "Would you like dedicated weekly Mock Test & Revision days?",
                    options = listOf("Yes, Sunday Revision & Mock", "Yes, Midweek & Sunday", "Daily 30-min active recall", "No dedicated revision")
                )
            )

            AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE -> listOf(
                ConversationalQuestion(
                    id = "chapter_title",
                    introMessage = "Let's turn your source material into clean, structured study notes preserving all key definitions, formulas, examples, and tables.",
                    questionText = "What chapter or topic are these notes for?",
                    options = if (chapterTitle.isNotBlank()) listOf(chapterTitle, "Core Foundations", "High-Yield Summary") else listOf("General Chapter Notes", "Formula & Concept Sheet", "Exam High-Yield Topics")
                ),
                ConversationalQuestion(
                    id = "source_input",
                    questionText = "Please attach photos of book pages, a PDF, or paste your source text snippet below.",
                    options = listOf("Upload Photos / Files", "Paste Text Snippet", "Use Existing Chapter Content")
                )
            )

            AssistantWorkflowType.SETUP_SYLLABUS -> listOf(
                ConversationalQuestion(
                    id = "exam_curriculum",
                    introMessage = "Let's organize your syllabus into structured subjects, chapters, and high-yield topics.",
                    questionText = "What exam or curriculum are we setting up?",
                    options = listOf("Competitive / Aptitude Exam", "Engineering & CS", "Medical & Biology", "Commerce & Humanities")
                ),
                ConversationalQuestion(
                    id = "main_subjects",
                    questionText = "Which main subjects would you like to create first?",
                    options = listOf("Quantitative Aptitude & Reasoning", "General Science & History", "Computer Science & IT", "English & Verbal Ability")
                ),
                ConversationalQuestion(
                    id = "chapter_density",
                    questionText = "How should I structure the chapters?",
                    options = listOf("Auto-generate top 4 high-yield chapters per subject", "Create core foundation chapters only", "I will upload / paste syllabus notes")
                )
            )

            AssistantWorkflowType.EXAM_STRATEGY -> listOf(
                ConversationalQuestion(
                    id = "time_remaining",
                    introMessage = "Let's design a high-retention strategy for your target exam.",
                    questionText = "How much time is remaining until your exam?",
                    options = listOf("Under 30 Days (Final Sprint)", "30 to 60 Days (Consolidation)", "60 to 90 Days (Standard)", "90+ Days (Comprehensive)")
                ),
                ConversationalQuestion(
                    id = "prep_level",
                    questionText = "What is your current preparation level?",
                    options = listOf("Just starting (0-25%)", "Intermediate (25-60%)", "Revision phase (60%+)")
                ),
                ConversationalQuestion(
                    id = "biggest_challenge",
                    questionText = "What is your highest priority area to improve?",
                    options = listOf("Retention & Active Recall", "Problem Solving Speed", "Mock Test Accuracy", "Consistency & Routine")
                )
            )

            AssistantWorkflowType.FOCUS_NEXT -> listOf(
                ConversationalQuestion(
                    id = "available_time",
                    introMessage = "Let's identify your highest-impact study topic right now.",
                    questionText = "How much study time do you have available for this session?",
                    options = listOf("25 min Pomodoro", "45 min Deep Block", "1.5 Hours Intensive", "Full Afternoon / Evening")
                ),
                ConversationalQuestion(
                    id = "target_subject",
                    questionText = "Which subject or area feels most urgent or least practiced?",
                    options = subjectOptions
                ),
                ConversationalQuestion(
                    id = "session_goal",
                    questionText = "What is your primary goal for this session?",
                    options = listOf("Understand difficult concepts", "Review & active recall notes", "Solve practice MCQs & test drills")
                )
            )

            AssistantWorkflowType.HOW_IT_WORKS -> listOf(
                ConversationalQuestion(
                    id = "how_it_works_intro",
                    introMessage = "Welcome to PrepOS! 🎓\nPrepOS is your smart study operating system built around active recall, spaced repetition, and deep focus.",
                    questionText = "Which section would you like to explore?",
                    options = listOf(
                        "📝 Test Screen & Practice",
                        "📖 Note Editor & Blur Mode",
                        "⏱️ Focus Hub & Timers",
                        "✦ PrepOS AI (This Screen)",
                        "🏠 Home Screen & Tracking",
                        "🚀 How the whole cycle works"
                    )
                )
            )
        }

        return rawQuestions.map { q ->
            if (q.options.none { it.contains("Custom", ignoreCase = true) }) {
                q.copy(options = q.options + "✎ Custom")
            } else {
                q
            }
        }
    }

    // Scroll helper
    fun scrollToBottom() {
        coroutineScope.launch {
            delay(100)
            if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    // Helper to generate dynamic, mathematically consistent 7-day schedule matching user's exact daily hours and preferences
    fun buildDynamicWeeklySchedule(
        session: ActiveWorkflowSession,
        enrolledSubjectNames: List<String>,
        userName: String
    ): List<DayScheduleItem> {
        val adjustmentText = session.answers["adjustment_request"] ?: ""
        val dailyHoursRaw = session.answers["daily_hours"] ?: "4 Hours / day"

        // 1. Extract Target Hours
        val hourMatchInAdjustment = Regex("(\\d+)\\s*(hours?|hrs?|h)", RegexOption.IGNORE_CASE).find(adjustmentText)
        val targetHours = when {
            hourMatchInAdjustment != null -> hourMatchInAdjustment.groupValues[1].toIntOrNull() ?: 4
            dailyHoursRaw.contains("1") -> 1
            dailyHoursRaw.contains("2") -> 2
            dailyHoursRaw.contains("3") -> 3
            dailyHoursRaw.contains("4") -> 4
            dailyHoursRaw.contains("5") -> 5
            dailyHoursRaw.contains("6") -> 6
            else -> Regex("\\d+").find(dailyHoursRaw)?.value?.toIntOrNull() ?: 4
        }.coerceIn(1, 8)

        // 2. Determine Timing Style
        val isFlexibleDay = session.answers["schedule_representation"]?.contains("Flexible", ignoreCase = true) == true ||
                adjustmentText.contains("flexible", ignoreCase = true) ||
                adjustmentText.contains("day", ignoreCase = true) ||
                session.answers["timing_pref"]?.contains("flexible", ignoreCase = true) == true

        val isEveningOnly = adjustmentText.contains("evening", ignoreCase = true) ||
                adjustmentText.contains("night", ignoreCase = true) ||
                session.answers["timing_pref"]?.contains("evening", ignoreCase = true) == true

        val isMorningOnly = adjustmentText.contains("morning", ignoreCase = true) ||
                session.answers["timing_pref"]?.contains("morning", ignoreCase = true) == true

        // 3. Subject Names and Topics
        val rawSubjectNames = if (enrolledSubjectNames.isNotEmpty()) enrolledSubjectNames else listOf("General Studies", "Reasoning & Aptitude", "Core Subject", "Revision")
        val singleSubject = rawSubjectNames.size == 1

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        return days.mapIndexed { dayIdx, dayName ->
            val isSunday = dayName == "Sunday"
            val isSaturday = dayName == "Saturday"

            val slots: List<DayScheduleSlot> = if (isSunday) {
                // Sunday: Comprehensive Timed Mock & Error Diagnosis
                if (targetHours >= 4) {
                    listOf(
                        DayScheduleSlot("09:00 AM - 11:00 AM", "Exam Mock Simulation", "Full-length timed mock test & pacing drill", "MOCK", 120),
                        DayScheduleSlot("05:30 PM - 07:30 PM", "Error Log Diagnosis", "Deep review of missed questions & concept gaps", "REVISION", 120)
                    )
                } else if (targetHours == 3) {
                    listOf(
                        DayScheduleSlot("09:00 AM - 10:30 AM", "Exam Mock Simulation", "Sectional timed mock test", "MOCK", 90),
                        DayScheduleSlot("06:00 PM - 07:30 PM", "Error Log Diagnosis", "Review flash notes & tricky formulas", "REVISION", 90)
                    )
                } else {
                    listOf(
                        DayScheduleSlot("09:00 AM - 10:00 AM", "Exam Mock Simulation", "High-yield mock test drill", "MOCK", 60),
                        DayScheduleSlot("06:00 PM - 07:00 PM", "Error Log Diagnosis", "Weekly mistake review & recall", "REVISION", 60)
                    )
                }
            } else if (isSaturday) {
                // Saturday: High-Yield Problem Solving & Speed Drills
                val sub1 = rawSubjectNames[dayIdx % rawSubjectNames.size]
                val sub2 = rawSubjectNames[(dayIdx + 1) % rawSubjectNames.size]
                if (targetHours >= 4) {
                    if (isFlexibleDay) {
                        listOf(
                            DayScheduleSlot("10:00 AM - 11:30 AM", sub1, "Sectional Speed Mock & High-Yield PYQs", "TEST", 90),
                            DayScheduleSlot("03:00 PM - 04:30 PM", sub2, "Detailed Error Log Analysis & Correction", "REVISION", 90),
                            DayScheduleSlot("07:30 PM - 08:30 PM", sub1, "Formula Sheet Review & Active Recall", "PRACTICE", 60)
                        )
                    } else if (isEveningOnly) {
                        listOf(
                            DayScheduleSlot("05:00 PM - 06:30 PM", sub1, "Sectional Speed Mock & High-Yield PYQs", "TEST", 90),
                            DayScheduleSlot("07:00 PM - 08:30 PM", sub2, "Detailed Error Log Analysis & Correction", "REVISION", 90),
                            DayScheduleSlot("09:00 PM - 10:00 PM", sub1, "Formula Sheet Review & Active Recall", "PRACTICE", 60)
                        )
                    } else if (isMorningOnly) {
                        listOf(
                            DayScheduleSlot("06:30 AM - 08:00 AM", sub1, "Sectional Speed Mock & High-Yield PYQs", "TEST", 90),
                            DayScheduleSlot("08:30 AM - 10:00 AM", sub2, "Detailed Error Log Analysis & Correction", "REVISION", 90),
                            DayScheduleSlot("10:30 AM - 11:30 AM", sub1, "Formula Sheet Review & Active Recall", "PRACTICE", 60)
                        )
                    } else {
                        listOf(
                            DayScheduleSlot("07:00 AM - 08:30 AM", sub1, "Sectional Speed Mock & High-Yield PYQs", "TEST", 90),
                            DayScheduleSlot("05:30 PM - 07:00 PM", sub2, "Detailed Error Log Analysis & Correction", "REVISION", 90),
                            DayScheduleSlot("08:30 PM - 09:30 PM", sub1, "Formula Sheet Review & Active Recall", "PRACTICE", 60)
                        )
                    }
                } else if (targetHours == 3) {
                    listOf(
                        DayScheduleSlot("09:30 AM - 11:00 AM", sub1, "Sectional Speed Mock & PYQs", "TEST", 90),
                        DayScheduleSlot("07:30 PM - 09:00 PM", sub2, "Error Log Breakdown & Revision", "REVISION", 90)
                    )
                } else {
                    val sub = rawSubjectNames[dayIdx % rawSubjectNames.size]
                    listOf(
                        DayScheduleSlot("09:30 AM - 10:30 AM", sub, "Sectional Speed Test", "TEST", 60),
                        DayScheduleSlot("07:30 PM - 08:30 PM", sub, "PYQ Error Analysis & Recall", "REVISION", 60)
                    )
                }
            } else {
                // Weekdays (Mon - Fri)
                val subA = rawSubjectNames[dayIdx % rawSubjectNames.size]
                val subB = rawSubjectNames[(dayIdx + 1) % rawSubjectNames.size]
                val subC = rawSubjectNames[(dayIdx + 2) % rawSubjectNames.size]

                // If single subject (e.g. English), create targeted study domain titles
                val title1 = if (singleSubject) {
                    when (dayIdx % 5) {
                        0 -> "Grammar Rules & Foundation Concepts"
                        1 -> "Sentence Correction & Syntax Rules"
                        2 -> "Critical Reading & Text Comprehension"
                        3 -> "Advanced Vocabulary & Word Roots"
                        else -> "Mixed PYQ Patterns & Rules Review"
                    }
                } else "Core Theory & Concept Notes"

                val title2 = if (singleSubject) {
                    when (dayIdx % 5) {
                        0 -> "Reading Comprehension Passages & Drills"
                        1 -> "Para Jumbles & Cloze Test Practice"
                        2 -> "Error Spotting & Timed MCQ Drills"
                        3 -> "Inference Passages & Problem Solving"
                        else -> "High-Yield Speed Test Simulation"
                    }
                } else "Active Problem Solving & PYQ Drills"

                val title3 = if (singleSubject) {
                    when (dayIdx % 5) {
                        0 -> "Vocab Flashcards & Spaced Recall"
                        1 -> "Idioms, Phrases & Rule Revision"
                        2 -> "Summary Cheat Sheet Consolidation"
                        3 -> "Active Recall & Doubt Journal"
                        else -> "Weekly Concept Reinforcement"
                    }
                } else "Active Recall & Formula Practice"

                when (targetHours) {
                    1 -> {
                        if (isEveningOnly) listOf(DayScheduleSlot("07:30 PM - 08:30 PM", subA, title1, "READING", 60))
                        else if (isMorningOnly) listOf(DayScheduleSlot("07:00 AM - 08:00 AM", subA, title1, "READING", 60))
                        else listOf(DayScheduleSlot("08:00 PM - 09:00 PM", subA, title1, "READING", 60))
                    }
                    2 -> {
                        if (isEveningOnly) listOf(
                            DayScheduleSlot("06:30 PM - 07:30 PM", subA, title1, "READING", 60),
                            DayScheduleSlot("08:30 PM - 09:30 PM", subB, title2, "PRACTICE", 60)
                        ) else if (isMorningOnly) listOf(
                            DayScheduleSlot("06:30 AM - 07:30 AM", subA, title1, "READING", 60),
                            DayScheduleSlot("08:30 AM - 09:30 AM", subB, title2, "PRACTICE", 60)
                        ) else if (isFlexibleDay) listOf(
                            DayScheduleSlot("10:00 AM - 11:00 AM", subA, title1, "READING", 60),
                            DayScheduleSlot("07:30 PM - 08:30 PM", subB, title2, "PRACTICE", 60)
                        ) else listOf(
                            DayScheduleSlot("07:00 AM - 08:00 AM", subA, title1, "READING", 60),
                            DayScheduleSlot("08:00 PM - 09:00 PM", subB, title2, "PRACTICE", 60)
                        )
                    }
                    3 -> {
                        if (isEveningOnly) listOf(
                            DayScheduleSlot("05:30 PM - 07:00 PM", subA, title1, "READING", 90),
                            DayScheduleSlot("08:00 PM - 09:30 PM", subB, title2, "PRACTICE", 90)
                        ) else if (isMorningOnly) listOf(
                            DayScheduleSlot("06:30 AM - 08:00 AM", subA, title1, "READING", 90),
                            DayScheduleSlot("09:00 AM - 10:30 AM", subB, title2, "PRACTICE", 90)
                        ) else if (isFlexibleDay) listOf(
                            DayScheduleSlot("09:30 AM - 11:00 AM", subA, title1, "READING", 90),
                            DayScheduleSlot("07:30 PM - 09:00 PM", subB, title2, "PRACTICE", 90)
                        ) else listOf(
                            DayScheduleSlot("07:00 AM - 08:30 AM", subA, title1, "READING", 90),
                            DayScheduleSlot("07:30 PM - 09:00 PM", subB, title2, "PRACTICE", 90)
                        )
                    }
                    4 -> {
                        // 4-Hour Daily Plan (exactly matching user's requested time)
                        if (isEveningOnly) listOf(
                            DayScheduleSlot("05:00 PM - 06:30 PM", subA, title1, "READING", 90),
                            DayScheduleSlot("07:00 PM - 08:30 PM", subB, title2, "PRACTICE", 90),
                            DayScheduleSlot("09:00 PM - 10:00 PM", subC, title3, "REVISION", 60)
                        ) else if (isMorningOnly) listOf(
                            DayScheduleSlot("06:30 AM - 08:00 AM", subA, title1, "READING", 90),
                            DayScheduleSlot("08:30 AM - 10:00 AM", subB, title2, "PRACTICE", 90),
                            DayScheduleSlot("10:30 AM - 11:30 AM", subC, title3, "REVISION", 60)
                        ) else if (isFlexibleDay) listOf(
                            DayScheduleSlot("09:30 AM - 11:00 AM", subA, title1, "READING", 90),
                            DayScheduleSlot("02:30 PM - 04:00 PM", subB, title2, "PRACTICE", 90),
                            DayScheduleSlot("07:30 PM - 08:30 PM", subC, title3, "REVISION", 60)
                        ) else listOf(
                            DayScheduleSlot("07:00 AM - 08:30 AM", subA, title1, "READING", 90),
                            DayScheduleSlot("05:30 PM - 07:00 PM", subB, title2, "PRACTICE", 90),
                            DayScheduleSlot("08:30 PM - 09:30 PM", subC, title3, "REVISION", 60)
                        )
                    }
                    5 -> {
                        listOf(
                            DayScheduleSlot("09:00 AM - 10:30 AM", subA, title1, "READING", 90),
                            DayScheduleSlot("11:30 AM - 01:00 PM", subB, title2, "PRACTICE", 90),
                            DayScheduleSlot("03:30 PM - 05:00 PM", subC, title2, "PRACTICE", 90),
                            DayScheduleSlot("08:00 PM - 08:30 PM", subA, title3, "REVISION", 30)
                        )
                    }
                    else -> {
                        // 6+ Hours
                        listOf(
                            DayScheduleSlot("08:30 AM - 10:30 AM", subA, title1, "READING", 120),
                            DayScheduleSlot("02:00 PM - 04:00 PM", subB, title2, "PRACTICE", 120),
                            DayScheduleSlot("07:30 PM - 09:30 PM", subC, title3, "REVISION", 120)
                        )
                    }
                }
            }

            val totalMinutes = slots.sumOf { it.durationMinutes }
            val formattedTime = if (totalMinutes % 60 == 0) {
                "${totalMinutes / 60} Hours"
            } else {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                if (mins == 30) "$hours.5 Hours" else "$hours hrs $mins min"
            }

            DayScheduleItem(
                dayName = dayName,
                totalTime = formattedTime,
                slots = slots
            )
        }
    }

    // Generate Proposal using AI with structured prompt fallback
    fun generateProposalForWorkflow(session: ActiveWorkflowSession) {
        if (session.type == AssistantWorkflowType.HOW_IT_WORKS) {
            return // Fully offline guide handled directly by handleOfflineHowItWorks
        }

        if (session.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
            isGenerating = true
            scrollToBottom()
            coroutineScope.launch {
                val startTime = System.currentTimeMillis()
                val resolvedTitle = session.answers["chapter_title"] ?: chapterTitle.ifBlank { "Structured Study Notes" }
                val sourceText = session.answers["accumulated_source"] ?: session.answers["source_input"] ?: selectedText.ifBlank { fullChapterText }

                val aiAttachments = session.accumulatedAttachments.map { att ->
                    AIAttachment(
                        name = att.name,
                        mimeType = att.mimeType,
                        base64Data = att.base64Data,
                        textContent = att.textSnippet
                    )
                }
                val modificationInstruction = session.answers["adjustment_request"] ?: ""

                val aiResult = StudyAIService.generateStructuredNotesFromSource(
                    sourceText = sourceText,
                    chapterTitle = resolvedTitle,
                    attachments = aiAttachments,
                    modificationInstruction = modificationInstruction,
                    apiKey = effectiveApiKey,
                    model = activeModel,
                    provider = activeProvider,
                    deepSeekApiKey = effectiveDeepSeekKey
                )

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000L) {
                    delay(2000L - elapsed)
                }

                isGenerating = false
                val cleanNotes = aiResult.getOrDefault("")
                session.isProposalActive = true

                val resolvedSubName = session.answers["subject_name"] ?: subjectName.ifBlank { "General Studies" }

                val notesArtifact = AiResultArtifact.StructuredNotes(
                    chapterTitle = resolvedTitle,
                    subjectName = resolvedSubName,
                    rawMarkdownNotes = cleanNotes
                )

                chatMessages.add(
                    AskAIChatMessage(
                        isUser = false,
                        text = if (modificationInstruction.isNotBlank()) {
                            "Updated the notes for **$resolvedSubName • $resolvedTitle**. Please review the revised version:"
                        } else {
                            "Your structured notes for **$resolvedSubName • $resolvedTitle** are ready for review:"
                        },
                        artifact = notesArtifact,
                        isApprovalProposal = true,
                        proposalType = session.type,
                        options = listOf("✓ Approve & Save", "✏️ Make Changes")
                    )
                )
                scrollToBottom()
            }
            return
        }

        isGenerating = true
        scrollToBottom()

        coroutineScope.launch {
            val startTime = System.currentTimeMillis()

            val userName = savedUserName.ifBlank { "Student" }
            val answersSummary = session.answers.entries.joinToString("\n") { "• ${it.key}: ${it.value}" }
            val existingSubjectsText = subjects.joinToString(", ") { it.name }.ifBlank { "General Studies" }

            val prompt = """
                You are PrepOS AI Study Assistant.
                User: $userName
                Workflow: ${session.type.title}
                Collected User Preferences:
                $answersSummary
                Enrolled Subjects in PrepOS: $existingSubjectsText

                Generate a clear, structured, beautiful proposal for this study plan.
                Keep it highly actionable, formatting with bold titles, bullet points, time slots, and daily breakdown.
            """.trimIndent()

            val aiResult = StudyAIService.executeCustomInstruction(
                baseContextText = "PrepOS AI Companion. User: $userName, Daily Target: ${preferences?.dailyTargetMinutes ?: 45}m",
                userInstruction = prompt,
                contextTitle = session.type.title,
                apiKey = effectiveApiKey,
                model = activeModel,
                provider = activeProvider,
                deepSeekApiKey = effectiveDeepSeekKey
            )

            // Ensure natural thinking transition: thinking indicator (~2.2s) followed by 3-dots, then reveal
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 2800L) {
                delay(2800L - elapsed)
            }

            isGenerating = false

            val rawOutput = aiResult.getOrDefault("")
            session.isProposalActive = true

            when (session.type) {
                AssistantWorkflowType.WEEKLY_PLAN, AssistantWorkflowType.FREE_TIME_SCHEDULE -> {
                    val subjectNames = subjects.map { it.name }
                    val dayScheduleItems = buildDynamicWeeklySchedule(
                        session = session,
                        enrolledSubjectNames = subjectNames,
                        userName = userName
                    )

                    val planArtifact = AiResultArtifact.WeeklyPlan(
                        planTitle = "7-Day Timetable for $userName",
                        summary = "Personalized schedule aligned with ${session.answers["target_exam"] ?: "Target Exam"}",
                        days = dayScheduleItems,
                        rawPlanText = rawOutput.ifBlank { "7-Day structured timetable aligned with your exam targets." }
                    )

                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = "Here is your personalized 7-day study plan aligned with your goals:",
                            artifact = planArtifact,
                            isApprovalProposal = true,
                            proposalType = session.type
                        )
                    )
                }

                AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE -> {
                    // Handled above
                }

                AssistantWorkflowType.SETUP_SYLLABUS -> {
                    val chosenSubjectName = session.answers["main_subjects"] ?: "Core Exam Subjects"
                    val syllabusArtifact = AiResultArtifact.SyllabusStructure(
                        examName = session.answers["exam_curriculum"] ?: "Target Exam",
                        subjects = listOf(
                            ExtractedSubjectItem(
                                name = chosenSubjectName,
                                colorHex = "#8B5CF6",
                                chapters = listOf(
                                    ExtractedChapterItem("Chapter 1: Fundamentals & Core Definitions", 1, listOf("Overview", "Key formulas", "Definitions")),
                                    ExtractedChapterItem("Chapter 2: In-Depth Concepts & Application", 2, listOf("Principles", "Worked examples")),
                                    ExtractedChapterItem("Chapter 3: High-Yield Practice & MCQs", 3, listOf("PYQs", "Speed drills")),
                                    ExtractedChapterItem("Chapter 4: Quick Revision & Summary", 4, listOf("Cheat sheet", "Error notes"))
                                )
                            ),
                            ExtractedSubjectItem(
                                name = "Revision & High-Yield Sheets",
                                colorHex = "#10B981",
                                chapters = listOf(
                                    ExtractedChapterItem("Chapter 1: Formula Sheet & Quick Reference", 1, listOf("Axioms", "Key equations")),
                                    ExtractedChapterItem("Chapter 2: Weak Area Diagnostics", 2, listOf("Common traps", "Diagnostics"))
                                )
                            )
                        )
                    )

                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = "Here is your proposed syllabus workspace structure:",
                            artifact = syllabusArtifact,
                            isApprovalProposal = true,
                            proposalType = session.type
                        )
                    )
                }

                AssistantWorkflowType.EXAM_STRATEGY -> {
                    val strategyArtifact = AiResultArtifact.ExamStrategy(
                        examName = session.answers["time_remaining"] ?: "Target Exam",
                        daysRemaining = 60,
                        summary = "High-retention phased roadmap",
                        phases = listOf(
                            StrategyPhaseItem("Phase 1: Foundation & Concept Mastery", "Weeks 1–3", "Cover 100% core theory with active recall notes and formula flashcards.", "45 min theory reading + 15 min recall"),
                            StrategyPhaseItem("Phase 2: Practice & PYQ Consolidation", "Weeks 4–6", "Solve chapter-wise previous year questions and maintain error log.", "45 min timed MCQ drills"),
                            StrategyPhaseItem("Phase 3: Timed Mock Simulation", "Weeks 7–8", "Full syllabus timed mock tests under strict exam conditions.", "1 Full Mock + 1 hr error analysis")
                        )
                    )

                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = "Here is your phased exam strategy roadmap:",
                            artifact = strategyArtifact,
                            isApprovalProposal = true,
                            proposalType = session.type
                        )
                    )
                }

                AssistantWorkflowType.FOCUS_NEXT -> {
                    val defaultText = """
                        ### 🚀 High-Impact Focus Recommendation

                        • **Target Focus**: ${session.answers["target_subject"] ?: "Core Subject Mastery"}
                        • **Duration**: ${session.answers["available_time"] ?: "45 min Deep Block"}
                        • **Session Goal**: ${session.answers["session_goal"] ?: "Active Recall & Problem Drills"}

                        **Immediate Action Steps:**
                        1. Open chapter notes in PrepOS Editor.
                        2. Spend first 10 minutes recalling key formulas without looking.
                        3. Solve 5 challenging practice problems.
                    """.trimIndent()

                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = rawOutput.ifBlank { defaultText },
                            options = listOf("Start Focus Mode ⏱️", "Go to Note Editor", "Ask another question ✦")
                        )
                    )
                }

                AssistantWorkflowType.HOW_IT_WORKS -> {
                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = "PrepOS helps you master any exam through active recall notes, spaced repetition timetables, and distraction-free focus modes.\n\nChoose an action above to get started!",
                            options = listOf("Create my weekly plan", "Create Notes from Source", "Set up my syllabus")
                        )
                    )
                }
            }

            scrollToBottom()
        }
    }

    // Start a conversational workflow
    fun startConversationalWorkflow(type: AssistantWorkflowType) {
        if (type == AssistantWorkflowType.HOW_IT_WORKS) {
            val session = ActiveWorkflowSession(
                type = type,
                questions = emptyList(),
                currentStepIndex = 0
            )
            activeWorkflow = session
            coroutineScope.launch {
                isGenerating = true
                delay(250)
                isGenerating = false
                chatMessages.add(
                    AskAIChatMessage(
                        isUser = false,
                        text = """
                            ### 🎓 Welcome to PrepOS!

                            PrepOS is your smart study operating system built around science-backed learning methods: **active recall**, **spaced repetition**, and **distraction-free focus**.

                            Tap any section below to learn what you can do and how to use it:
                        """.trimIndent(),
                        actionTag = "PrepOS Guide",
                        options = listOf(
                            "📝 Test Screen & Practice",
                            "📖 Note Editor & Blur Mode",
                            "⏱️ Focus Hub & Timers",
                            "✦ PrepOS AI (This Screen)",
                            "🏠 Home Screen & Tracking",
                            "🚀 How the whole cycle works"
                        )
                    )
                )
                scrollToBottom()
            }
            return
        }

        if (type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
            val session = ActiveWorkflowSession(
                type = type,
                questions = emptyList(),
                currentStepIndex = 0
            )
            activeWorkflow = session

            if (isContextualMode) {
                val resolvedSub = subjectName.ifBlank { "Subject" }
                val resolvedChap = chapterTitle.ifBlank { "Active Chapter" }
                session.answers["stage"] = "awaiting_source"
                session.answers["subject_name"] = resolvedSub
                session.answers["chapter_title"] = resolvedChap
                if (chapterId.isNotBlank()) {
                    session.answers["chapter_id"] = chapterId
                }
                session.answers["is_update_mode"] = (!isChapterEmpty).toString()

                coroutineScope.launch {
                    isGenerating = true
                    delay(250)
                    isGenerating = false

                    val messageText = if (!isChapterEmpty) {
                        "### 🔄 Update Notes • **$resolvedSub • $resolvedChap**\n\nPlease attach your book pages, PDF, images, or paste your source text."
                    } else {
                        "Please attach your book pages, PDF, images, or paste your source text."
                    }

                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = messageText,
                            actionTag = if (!isChapterEmpty) "Update Notes" else "Create Notes",
                            options = if (!isChapterEmpty) listOf("Use Chapter Context") else emptyList()
                        )
                    )
                    scrollToBottom()
                }
                return
            } else {
                // Global mode: Prompt user to choose or create an Exam or Subject first
                session.answers["stage"] = "awaiting_subject_selection"
                coroutineScope.launch {
                    isGenerating = true
                    delay(250)
                    isGenerating = false

                    val examOptions = exams.map { "🎓 " + it.name }
                    val subjectOptions = subjects.map { "📘 " + it.name }
                    val actionOptions = listOf("➕ Create New Subject") + (if (exams.isEmpty()) listOf("➕ Create New Exam") else emptyList())

                    val combinedOptions = examOptions + subjectOptions + actionOptions

                    val introPrompt = if (exams.isNotEmpty()) {
                        "### 📝 Create Notes from Source\n\nWhich **Exam** or **Subject** from your library are you creating notes for? You can select any existing subject or exam, or create a new one:"
                    } else {
                        "### 📝 Create Notes from Source\n\nWhich **Subject** from your library are you creating notes for? Select an existing subject or create a new one:"
                    }

                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = introPrompt,
                            actionTag = "Select Exam / Subject",
                            options = if (combinedOptions.isNotEmpty()) combinedOptions else listOf("➕ Create New Subject")
                        )
                    )
                    scrollToBottom()
                }
                return
            }
        }

        val questions = getQuestionsForWorkflow(type)
        val session = ActiveWorkflowSession(
            type = type,
            questions = questions,
            currentStepIndex = 0
        )
        activeWorkflow = session

        coroutineScope.launch {
            isGenerating = true
            delay(650) // Initial typing indicator

            val firstQuestion = questions.firstOrNull()
            if (firstQuestion != null) {
                val fullText = if (firstQuestion.introMessage != null) {
                    "${firstQuestion.introMessage}\n\n${firstQuestion.questionText}"
                } else {
                    firstQuestion.questionText
                }

                chatMessages.add(
                    AskAIChatMessage(
                        isUser = false,
                        text = fullText,
                        actionTag = type.title,
                        options = firstQuestion.options
                    )
                )
            }
            isGenerating = false
            scrollToBottom()
        }
    }

    // Dedicated 100% OFFLINE intelligent guide for PrepOS features
    fun handleOfflineHowItWorks(userChoiceOrQuestion: String) {
        val prompt = userChoiceOrQuestion.trim()
        val lower = prompt.lowercase()

        // 1. Navigation / Action Shortcuts from Guide options
        when {
            lower.contains("weekly plan") || lower.contains("7-day timetable") -> {
                chatMessages.add(AskAIChatMessage(isUser = true, text = prompt))
                startConversationalWorkflow(AssistantWorkflowType.WEEKLY_PLAN)
                return
            }
            lower.contains("notes from source") || lower.contains("create notes") -> {
                chatMessages.add(AskAIChatMessage(isUser = true, text = prompt))
                startConversationalWorkflow(AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE)
                return
            }
            lower.contains("free time") || lower.contains("free hours") -> {
                chatMessages.add(AskAIChatMessage(isUser = true, text = prompt))
                startConversationalWorkflow(AssistantWorkflowType.FREE_TIME_SCHEDULE)
                return
            }
            lower.contains("set up my syllabus") || (lower.contains("syllabus") && !lower.contains("explore") && !lower.contains("how")) -> {
                chatMessages.add(AskAIChatMessage(isUser = true, text = prompt))
                startConversationalWorkflow(AssistantWorkflowType.SETUP_SYLLABUS)
                return
            }
            lower.contains("exam strategy") || lower.contains("phased roadmap") -> {
                chatMessages.add(AskAIChatMessage(isUser = true, text = prompt))
                startConversationalWorkflow(AssistantWorkflowType.EXAM_STRATEGY)
                return
            }
            lower.contains("go to home") || lower.contains("home dashboard") -> {
                onNavigateTab("home")
                onDismiss()
                return
            }
            lower.contains("start focus") || lower.contains("launch timer") -> {
                onNavigateTab("plan")
                onDismiss()
                return
            }
            lower.contains("go to note editor") -> {
                onNavigateTab("notes")
                onDismiss()
                return
            }
            lower.contains("go to test screen") -> {
                onNavigateTab("test")
                onDismiss()
                return
            }
        }

        // 2. Add user question/selection to chat
        chatMessages.add(AskAIChatMessage(isUser = true, text = prompt))
        isGenerating = true
        scrollToBottom()

        coroutineScope.launch {
            delay(300) // Fast offline response feel
            isGenerating = false

            val (responseText, nextOptions) = when {
                // TEST SCREEN
                lower.contains("test") || lower.contains("quiz") || lower.contains("mock") || lower.contains("mcq") || lower.contains("practice") || lower.contains("error log") -> {
                    Pair(
                        """
                        ### 📝 What You Can Do on the Test Screen

                        The **Test Screen** is your exam simulator and active testing arena designed to find and fix weak spots before exam day:

                        • **Chapter-Wise Quizzes**: Pick any subject and chapter from your library to generate an instant self-assessment quiz on definitions, formulas, and key concepts.
                        • **Timed Mock Exams**: Practice full-length tests under timed pressure with a real-time countdown timer to build exam pacing and composure.
                        • **Instant Smart Explanations**: Immediately after submitting an answer, see why an option is right or wrong with step-by-step conceptual clarity.
                        • **Error Diagnostic Log**: PrepOS automatically saves every question you miss. You can re-test only your mistakes so you never repeat them.
                        • **Accuracy & Score Analytics**: Track your percentage score, time taken per question, and your mastery curve over time.

                        💡 **Best Way to Use It**: Read a chapter in the Note Editor, then jump straight to the Test Screen for a quick 5–10 question drill!
                        """.trimIndent(),
                        listOf(
                            "📖 Note Editor & Blur Mode",
                            "⏱️ Focus Hub & Timers",
                            "✦ PrepOS AI (This Screen)",
                            "🏠 Home Screen & Tracking",
                            "📅 Create a weekly plan"
                        )
                    )
                }

                // NOTE EDITOR & BLUR MODE
                lower.contains("note") || lower.contains("editor") || lower.contains("blur") || lower.contains("recall") || lower.contains("math") || lower.contains("latex") || lower.contains("document") -> {
                    Pair(
                        """
                        ### 📖 What You Can Do in the Note Editor

                        The **Note Editor** is designed for deep, distraction-free active studying:

                        • **Active Recall & Blur Mode**: Tap the eye icon in the top toolbar to blur answers, definitions, and key formulas. Test your memory first, then tap to reveal and verify!
                        • **Continuous Document Flow**: Browse and scroll through all your chapters smoothly in one continuous, paginated document without loading breaks.
                        • **Crisp Math & Formulas**: Type equations using standard math notation (`x²`, `√x`, `∫f(x)dx`) for clean mathematical typography.
                        • **Tables & Comparisons**: Create side-by-side comparison tables, step-by-step derivations, and bold callout boxes.
                        • **AI Copilot Toolbar**: Select any text in your notes to instantly summarize, create active recall flashcards, or simplify tricky concepts.
                        • **Attach Source Photos & PDFs**: Tap the paperclip icon to import textbook pages, diagrams, or lecture slides right alongside your notes.

                        💡 **Best Way to Use It**: Turn your notes into question–answer blocks, and use Blur Mode to test yourself without looking at the answers!
                        """.trimIndent(),
                        listOf(
                            "📝 Test Screen & Practice",
                            "⏱️ Focus Hub & Timers",
                            "✦ PrepOS AI (This Screen)",
                            "🏠 Home Screen & Tracking",
                            "📝 Create Notes from Source"
                        )
                    )
                }

                // FOCUS HUB & TIMERS
                lower.contains("focus") || lower.contains("timer") || lower.contains("pomodoro") || lower.contains("hub") || lower.contains("deep work") || lower.contains("ambient") || lower.contains("sound") -> {
                    Pair(
                        """
                        ### ⏱️ What You Can Do in the Focus Hub

                        The **Focus Hub** keeps you distraction-free and builds your daily study consistency:

                        • **Pomodoro & Deep Work Blocks**: Start 25-minute or 50-minute focused sessions with gentle rest breaks so you study intensively without burnout.
                        • **Daily Target Ring**: Set your daily study goal (e.g., 2 hours) and watch your progress ring fill up as you complete study blocks.
                        • **Ambient Background Sounds**: Toggle calming soundscapes (gentle rain, library ambience, white noise) to block out distractions.
                        • **Subject-Linked Timers**: Tap any scheduled task to run a dedicated timer linked directly to that specific subject and chapter.
                        • **Focus History & Streaks**: Every minute you focus is recorded locally and adds to your daily study streak.

                        💡 **Best Way to Use It**: Start a 25-min Pomodoro timer whenever you begin studying a chapter to stay 100% focused without checking your phone!
                        """.trimIndent(),
                        listOf(
                            "📝 Test Screen & Practice",
                            "📖 Note Editor & Blur Mode",
                            "✦ PrepOS AI (This Screen)",
                            "🏠 Home Screen & Tracking",
                            "Start Focus Mode ⏱️"
                        )
                    )
                }

                // PREPOS AI (THIS SCREEN)
                lower.contains("this screen") || lower.contains("assistant") || lower.contains("planner") || (lower.contains("ai") && !lower.contains("chair")) || lower.contains("what can you do") || lower.contains("section") -> {
                    Pair(
                        """
                        ### ✦ What You Can Do on the PrepOS AI Screen

                        This screen is your 24/7 academic advisor and study planner. Here is everything you can do right here:

                        • **📅 Create a Weekly Plan**: Answer a few quick questions about your targets and daily free hours, and get a complete 7-day timetable with daily tabs, slot breakdown, and 1-tap sync to your schedule.
                        • **📝 Create Notes from Source**: Upload book photos, lecture slides, or paste text to generate structured active-recall notes with definitions, formulas, and comparison tables.
                        • **⏱️ Plan Around Free Time**: Tell me your available slots (morning, afternoon, evening), and I'll structure a realistic study routine.
                        • **📚 Set Up My Syllabus**: Name your target exam or curriculum, and I'll build out your complete library structure with subjects, chapters, and high-yield topics.
                        • **🎯 Plan Exam Strategy**: Generate a phased preparation roadmap (Foundation → Practice & Drills → Timed Mocks) based on your remaining days.
                        • **💬 Ask Any Study Doubt**: Clear concepts, solve tricky problems, or request practice questions anytime in the chat box below!

                        💡 **Best Way to Use It**: Tap 'Create a weekly plan' to get your study schedule organized in under 60 seconds!
                        """.trimIndent(),
                        listOf(
                            "📅 Create a weekly plan",
                            "📝 Create Notes from Source",
                            "📚 Set up my syllabus",
                            "📝 Test Screen & Practice",
                            "📖 Note Editor & Blur Mode"
                        )
                    )
                }

                // HOME SCREEN & TRACKING
                lower.contains("home") || lower.contains("streak") || lower.contains("tracking") || lower.contains("last 7 days") || lower.contains("progress") || lower.contains("weekly study") -> {
                    Pair(
                        """
                        ### 🏠 What You Can Do on the Home Screen

                        The **Home Screen** is your central cockpit for daily momentum and tracking:

                        • **Today's Mission & Quick Launch**: View today's scheduled study sessions and launch right into the Note Editor or Focus Timer with a single tap.
                        • **Live Progress Grid**:
                          - **Study Time Today**: Live counter of your minutes studied today.
                          - **Daily Streak**: Number of consecutive days you've completed your daily study target.
                          - **Weekly Study (Last 7 Days)**: Rolling 7-day total study time so you always know your weekly consistency.
                          - **Accuracy Rate**: Overall score percentage from your completed tests and quizzes.
                        • **Subject Library Cards**: Tap any subject to jump straight into its chapters, notes, and formula sheets.
                        • **Upcoming Timetable**: A quick glance at what subjects and chapters are scheduled for today and tomorrow.

                        💡 **Best Way to Use It**: Check your Home Screen first thing every morning to see your daily target and protect your study streak!
                        """.trimIndent(),
                        listOf(
                            "📝 Test Screen & Practice",
                            "📖 Note Editor & Blur Mode",
                            "⏱️ Focus Hub & Timers",
                            "✦ PrepOS AI (This Screen)",
                            "Go to Home Dashboard"
                        )
                    )
                }

                // COMPLETE WORKFLOW / CYCLE
                lower.contains("cycle") || lower.contains("walkthrough") || lower.contains("how the whole") || lower.contains("all") || lower.contains("routine") || lower.contains("how does prepos work") || lower.contains("how prepos works") || lower.contains("start") -> {
                    Pair(
                        """
                        ### 🚀 The 4-Step PrepOS High-Retention Cycle

                        Here is the proven routine to prepare for any exam with PrepOS:

                        1. **Step 1: Plan with AI Assistant** 📅
                           Generate your 7-day timetable and structure your syllabus right here on the AI Screen.

                        2. **Step 2: Study Active-Recall Notes** 📖
                           Open the **Note Editor** to read chapter notes. Use **Blur Mode** to hide key definitions and test your recall.

                        3. **Step 3: Deep Work with Focus Hub** ⏱️
                           Start a 25-minute Pomodoro timer in the **Focus Hub** to stay distraction-free and build your daily streak.

                        4. **Step 4: Test & Fix Errors** 📝
                           Take a chapter quiz on the **Test Screen**. Any missed questions are saved in your **Error Diagnostic Log** for spaced review.

                        This full cycle turns passive reading into active, permanent learning!
                        """.trimIndent(),
                        listOf(
                            "📅 Create a weekly plan",
                            "📝 Test Screen & Practice",
                            "📖 Note Editor & Blur Mode",
                            "⏱️ Focus Hub & Timers",
                            "✦ PrepOS AI (This Screen)"
                        )
                    )
                }

                // GENERAL / OTHER QUESTIONS
                else -> {
                    Pair(
                        """
                        PrepOS brings together everything you need to study effectively:
                        • **Note Editor**: Active recall notes, Blur mode, and math formatting.
                        • **Test Screen**: Chapter quizzes, timed mocks, and error diagnostic logs.
                        • **Focus Hub**: Pomodoro timers, ambient sounds, and daily streak tracking.
                        • **AI Assistant**: 7-day timetable planner, notes from source, and syllabus organizer.
                        • **Home Screen**: Today's tasks, last 7 days study tracking, and subject shortcuts.

                        Which section would you like to explore in detail?
                        """.trimIndent(),
                        listOf(
                            "📝 Test Screen & Practice",
                            "📖 Note Editor & Blur Mode",
                            "⏱️ Focus Hub & Timers",
                            "✦ PrepOS AI (This Screen)",
                            "🏠 Home Screen & Tracking"
                        )
                    )
                }
            }

            chatMessages.add(
                AskAIChatMessage(
                    isUser = false,
                    text = responseText,
                    actionTag = "PrepOS Guide",
                    options = nextOptions
                )
            )
            scrollToBottom()
        }
    }

    // Handle user answering a question in active conversational flow
    fun processConversationalAnswer(answerText: String) {
        val session = activeWorkflow ?: return

        // 100% Offline PrepOS interactive feature guide
        if (session.type == AssistantWorkflowType.HOW_IT_WORKS) {
            handleOfflineHowItWorks(answerText)
            return
        }

        val cleanAnswer = answerText.trim()

        // Handle CREATE_NOTES_FROM_SOURCE workflow dedicated flow
        if (session.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
            if (session.isProposalActive) {
                when {
                    cleanAnswer.contains("Approve", ignoreCase = true) ||
                    cleanAnswer.contains("Save", ignoreCase = true) ||
                    cleanAnswer.contains("Confirm", ignoreCase = true) ||
                    cleanAnswer.contains("✓") -> {
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        isGenerating = true
                        session.isProposalActive = false
                        session.isCompleted = true

                        coroutineScope.launch {
                            delay(400)
                            val lastMessage = chatMessages.lastOrNull { it.artifact is AiResultArtifact.StructuredNotes }
                            val rawNotes = (lastMessage?.artifact as? AiResultArtifact.StructuredNotes)?.rawMarkdownNotes ?: ""
                            val resolvedSubName = session.answers["subject_name"] ?: subjectName.ifBlank { "General Studies" }
                            val resolvedTitle = session.answers["chapter_title"] ?: chapterTitle.ifBlank { "Structured Study Notes" }
                            val resolvedSubId = session.answers["subject_id"]
                            val resolvedChapId = session.answers["chapter_id"] ?: chapterId.ifBlank { null }

                            if (isContextualMode && rawNotes.isNotBlank()) {
                                onInsertBelow(rawNotes)
                                isGenerating = false
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "✓ **Notes Saved Successfully!**\n\nYour structured notes have been saved to **$resolvedTitle** ($resolvedSubName) in your PrepOS Library.",
                                        options = emptyList()
                                    )
                                )
                            } else if (rawNotes.isNotBlank()) {
                                viewModel?.saveStructuredNoteToLibrary(
                                    subjectName = resolvedSubName,
                                    chapterTitle = resolvedTitle,
                                    markdownNotes = rawNotes,
                                    existingSubjectId = resolvedSubId,
                                    existingChapterId = resolvedChapId,
                                    existingExamId = session.answers["exam_id"]
                                )
                                isGenerating = false
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "✓ **Notes Saved Successfully to Library!**\n\n" +
                                               "📚 **Subject:** **$resolvedSubName**\n" +
                                               "📖 **Chapter:** **$resolvedTitle**\n\n" +
                                               "Your structured notes are saved in your PrepOS Library with active recall elements.",
                                        options = emptyList()
                                    )
                                )
                            } else {
                                isGenerating = false
                            }
                            scrollToBottom()
                        }
                        return
                    }

                    cleanAnswer.equals("Make Changes ✎", ignoreCase = true) ||
                    cleanAnswer.equals("Make Changes", ignoreCase = true) ||
                    cleanAnswer.equals("✏️ Make Changes", ignoreCase = true) ||
                    cleanAnswer.equals("Adjust", ignoreCase = true) ||
                    cleanAnswer.equals("Modify", ignoreCase = true) -> {
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        isGenerating = true
                        coroutineScope.launch {
                            delay(350)
                            isGenerating = false
                            chatMessages.add(
                                AskAIChatMessage(
                                    isUser = false,
                                    text = "What would you like me to change in your notes?",
                                    options = listOf(
                                        "Add timeline for dates ⏱️",
                                        "Convert comparison to table 📊",
                                        "Simpler English 💡",
                                        "Add bullet points •",
                                        "Highlight formulas 📐",
                                        "✎ Custom request"
                                    )
                                )
                            )
                            scrollToBottom()
                        }
                        return
                    }

                    cleanAnswer.equals("✎ Custom request", ignoreCase = true) ||
                    cleanAnswer.equals("✎ Custom", ignoreCase = true) ||
                    cleanAnswer.equals("Custom", ignoreCase = true) -> {
                        customPlaceholderHint = "Tell me what changes you want (e.g. add timeline, simpler English)..."
                        coroutineScope.launch {
                            delay(60)
                            focusRequester.requestFocus()
                            keyboardController?.show()
                            scrollToBottom()
                        }
                        return
                    }

                    else -> {
                        // User provided specific modification instruction
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        val lastNotes = (chatMessages.lastOrNull { it.artifact is AiResultArtifact.StructuredNotes }?.artifact as? AiResultArtifact.StructuredNotes)?.rawMarkdownNotes ?: session.answers["accumulated_source"] ?: ""
                        session.answers["accumulated_source"] = lastNotes
                        session.answers["adjustment_request"] = cleanAnswer.removeSuffix("⏱️").removeSuffix("📊").removeSuffix("💡").removeSuffix("•").removeSuffix("📐").trim()
                        generateProposalForWorkflow(session)
                        return
                    }
                }
            } else {
                // Non-proposal active phase of CREATE_NOTES_FROM_SOURCE
                when (session.answers["stage"]) {
                    "awaiting_subject_selection", "awaiting_subject_under_exam" -> {
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        if (cleanAnswer.contains("Create New Exam", ignoreCase = true)) {
                            session.answers["stage"] = "awaiting_new_exam_name"
                            customPlaceholderHint = "Enter target exam name (e.g. JKSSB, UPSC, SSC)..."
                            coroutineScope.launch {
                                delay(250)
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "What is the name of your new **Target Exam**? Type below or pick a suggestion:",
                                        options = listOf("JKSSB Junior Assistant", "UPSC Civil Services", "SSC CGL", "State PSC", "GATE / CS Exam")
                                    )
                                )
                                scrollToBottom()
                                delay(200)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            return
                        } else if (cleanAnswer.contains("Create New Subject", ignoreCase = true) || cleanAnswer.startsWith("+")) {
                            session.answers["stage"] = "awaiting_new_subject_name"
                            val examContext = session.answers["exam_name"]
                            customPlaceholderHint = "Enter subject name (e.g. Physics, History)..."
                            coroutineScope.launch {
                                delay(250)
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "What is the name of your new **Subject**${if (!examContext.isNullOrBlank()) " for **$examContext**" else ""}?",
                                        options = listOf("Physics", "Chemistry", "Mathematics", "Indian Polity", "General Studies")
                                    )
                                )
                                scrollToBottom()
                                delay(200)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            return
                        } else {
                            // Check if selected an Exam
                            val cleanExamName = cleanAnswer.removePrefix("🎓").removePrefix("•").trim()
                            val matchedExam = exams.firstOrNull { it.name.equals(cleanExamName, ignoreCase = true) }
                                ?: exams.firstOrNull { cleanExamName.contains(it.name, ignoreCase = true) }

                            if (matchedExam != null && (cleanAnswer.startsWith("🎓") || session.answers["stage"] == "awaiting_subject_selection")) {
                                session.answers["exam_id"] = matchedExam.id
                                session.answers["exam_name"] = matchedExam.name
                                val examSubjects = subjects.filter { it.examId == matchedExam.id }
                                session.answers["stage"] = "awaiting_subject_under_exam"

                                coroutineScope.launch {
                                    delay(250)
                                    val subOptions = examSubjects.map { "📘 " + it.name } + listOf("➕ Create New Subject in ${matchedExam.name}")
                                    chatMessages.add(
                                        AskAIChatMessage(
                                            isUser = false,
                                            text = "### 🎓 Exam: **${matchedExam.name}**\n\n${if (examSubjects.isNotEmpty()) "Found **${examSubjects.size} subject(s)** under **${matchedExam.name}**. Which subject are you creating notes for?" else "No subjects found under **${matchedExam.name}** yet. Create your first subject below:"}",
                                            options = subOptions
                                        )
                                    )
                                    scrollToBottom()
                                }
                                return
                            }

                            // Otherwise, process as Subject
                            val cleanSubName = cleanAnswer.removePrefix("📘").removePrefix("📚").removePrefix("•").trim()
                            val matchedSub = subjects.firstOrNull { it.name.equals(cleanSubName, ignoreCase = true) }
                                ?: subjects.firstOrNull { cleanSubName.contains(it.name, ignoreCase = true) }
                                ?: subjects.firstOrNull { it.name.contains(cleanSubName, ignoreCase = true) }
                            val chosenSubName = matchedSub?.name ?: cleanSubName

                            if (matchedSub != null) {
                                session.answers["subject_id"] = matchedSub.id
                                session.answers["subject_name"] = matchedSub.name
                                if (!matchedSub.examId.isNullOrBlank()) {
                                    session.answers["exam_id"] = matchedSub.examId
                                    val examName = exams.firstOrNull { it.id == matchedSub.examId }?.name
                                    if (!examName.isNullOrBlank()) session.answers["exam_name"] = examName
                                }
                                val subjectChapters = allRecentChapters.filter { it.subjectId == matchedSub.id }
                                val chapterOptions = subjectChapters.map { "📖 " + it.title } + listOf("➕ Create New Chapter in ${matchedSub.name}")
                                session.answers["stage"] = "awaiting_chapter_choice"

                                coroutineScope.launch {
                                    delay(250)
                                    val examInfo = session.answers["exam_name"]
                                    val examBadge = if (!examInfo.isNullOrBlank()) " (🎓 $examInfo)" else ""
                                    chatMessages.add(
                                        AskAIChatMessage(
                                            isUser = false,
                                            text = "### 📚 Subject: **${matchedSub.name}**$examBadge\n\n${if (subjectChapters.isNotEmpty()) "Found **${subjectChapters.size} chapter(s)** in **${matchedSub.name}**. Select an existing chapter to add/update notes, or create a new chapter:" else "No chapters found in **${matchedSub.name}** yet. Create your first chapter below:"}",
                                            options = chapterOptions
                                        )
                                    )
                                    scrollToBottom()
                                }
                                return
                            } else {
                                session.answers["subject_name"] = cleanSubName
                                session.answers.remove("subject_id")
                                session.answers["stage"] = "awaiting_new_chapter_name"
                                customPlaceholderHint = "Enter chapter name for $cleanSubName..."

                                coroutineScope.launch {
                                    delay(250)
                                    chatMessages.add(
                                        AskAIChatMessage(
                                            isUser = false,
                                            text = "### 📚 Subject: **$cleanSubName**\n\nWhat is the **Chapter Title** for **$cleanSubName**? Type your chapter title below:",
                                            options = listOf("Chapter 1: Fundamentals", "Core Concepts & Definitions", "High-Yield Summary")
                                        )
                                    )
                                    scrollToBottom()
                                    delay(200)
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                return
                            }
                        }
                    }

                    "awaiting_new_exam_name" -> {
                        val cleanExamName = cleanAnswer.removePrefix("🎓").trim()
                        viewModel?.createExam(cleanExamName, cleanExamName.take(4).uppercase(), "#8B5CF6")
                        session.answers["exam_name"] = cleanExamName
                        session.answers["stage"] = "awaiting_new_subject_name"
                        customPlaceholderHint = "Enter subject name for $cleanExamName..."
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        coroutineScope.launch {
                            delay(250)
                            chatMessages.add(
                                AskAIChatMessage(
                                    isUser = false,
                                    text = "### 🎓 Exam: **$cleanExamName**\n\nGreat! What is the name of the first **Subject** for **$cleanExamName**?",
                                    options = listOf("Physics", "Chemistry", "Mathematics", "Indian Polity", "General Studies")
                                )
                            )
                            scrollToBottom()
                            delay(200)
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        return
                    }

                    "awaiting_new_subject_name" -> {
                        val cleanSubName = cleanAnswer.removePrefix("📘").removePrefix("📚").trim()
                        val matchedSub = subjects.firstOrNull { it.name.equals(cleanSubName, ignoreCase = true) }
                            ?: subjects.firstOrNull { cleanSubName.contains(it.name, ignoreCase = true) }

                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))

                        if (matchedSub != null) {
                            session.answers["subject_id"] = matchedSub.id
                            session.answers["subject_name"] = matchedSub.name
                            if (!matchedSub.examId.isNullOrBlank()) {
                                session.answers["exam_id"] = matchedSub.examId
                                val examName = exams.firstOrNull { it.id == matchedSub.examId }?.name
                                if (!examName.isNullOrBlank()) session.answers["exam_name"] = examName
                            }
                            val subjectChapters = allRecentChapters.filter { it.subjectId == matchedSub.id }
                            val chapterOptions = subjectChapters.map { "📖 " + it.title } + listOf("➕ Create New Chapter in ${matchedSub.name}")
                            session.answers["stage"] = "awaiting_chapter_choice"

                            coroutineScope.launch {
                                delay(250)
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "### 📚 Subject: **${matchedSub.name}**\n\n${if (subjectChapters.isNotEmpty()) "Found **${subjectChapters.size} chapter(s)** in **${matchedSub.name}**. Select an existing chapter to add/update notes, or create a new chapter:" else "No chapters found in **${matchedSub.name}** yet. Create your first chapter below:"}",
                                        options = chapterOptions
                                    )
                                )
                                scrollToBottom()
                            }
                            return
                        } else {
                            session.answers["subject_name"] = cleanSubName
                            session.answers.remove("subject_id")
                            session.answers["stage"] = "awaiting_new_chapter_name"
                            customPlaceholderHint = "Enter chapter name for $cleanSubName..."
                            coroutineScope.launch {
                                delay(250)
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "### 📚 Subject: **$cleanSubName**\n\nWhat is the **Chapter Title** for **$cleanSubName**? Type below:",
                                        options = listOf("Chapter 1: Fundamentals", "Core Concepts & Definitions", "High-Yield Summary")
                                    )
                                )
                                scrollToBottom()
                                delay(200)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            return
                        }
                    }

                    "awaiting_chapter_choice" -> {
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        if (cleanAnswer.contains("Create New Chapter", ignoreCase = true) || cleanAnswer.startsWith("+")) {
                            session.answers["stage"] = "awaiting_new_chapter_name"
                            val currentSub = session.answers["subject_name"] ?: "Subject"
                            customPlaceholderHint = "Enter chapter name for $currentSub..."
                            coroutineScope.launch {
                                delay(250)
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "### 📚 Subject: **$currentSub**\n\nWhat is the title for the new chapter in **$currentSub**? Type your chapter title below:",
                                        options = listOf("Chapter 1: Fundamentals", "In-Depth Concepts", "Formulas & Definitions", "High-Yield Summary")
                                    )
                                )
                                scrollToBottom()
                                delay(200)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            return
                        } else {
                            val cleanChap = cleanAnswer.removePrefix("📖").removePrefix("•").removePrefix("➕ Create New Chapter in ").trim()
                            val subjectId = session.answers["subject_id"]
                            val subjectChapters = if (!subjectId.isNullOrBlank()) allRecentChapters.filter { it.subjectId == subjectId } else emptyList()
                            val matchedChap = subjectChapters.firstOrNull { it.title.equals(cleanChap, ignoreCase = true) }
                                ?: subjectChapters.firstOrNull { cleanChap.contains(it.title, ignoreCase = true) }
                                ?: subjectChapters.firstOrNull { it.title.contains(cleanChap, ignoreCase = true) }

                            if (matchedChap != null) {
                                session.answers["chapter_id"] = matchedChap.id
                                session.answers["chapter_title"] = matchedChap.title
                                session.answers["is_update_mode"] = "true"
                            } else {
                                session.answers["chapter_title"] = cleanChap
                                session.answers.remove("chapter_id")
                                session.answers["is_update_mode"] = "false"
                            }

                            session.answers["stage"] = "awaiting_source"
                            val resolvedSub = session.answers["subject_name"] ?: "Subject"
                            val resolvedChap = session.answers["chapter_title"] ?: cleanChap
                            val isUpdate = session.answers["is_update_mode"] == "true"

                            coroutineScope.launch {
                                delay(250)
                                chatMessages.add(
                                    AskAIChatMessage(
                                        isUser = false,
                                        text = "### 🎯 Target: **$resolvedSub** → **$resolvedChap** ${if (isUpdate) "(Update Mode 🔄)" else ""}\n\nPlease attach your book pages, PDF, images, or paste your source text.",
                                        options = emptyList()
                                    )
                                )
                                scrollToBottom()
                            }
                            return
                        }
                    }

                    "awaiting_new_chapter_name", "awaiting_chapter_name" -> {
                        val cleanChap = cleanAnswer.removePrefix("📖").removePrefix("•").removePrefix("➕ Create New Chapter in ").trim()
                        session.answers["chapter_title"] = cleanChap
                        session.answers.remove("chapter_id")
                        session.answers["is_update_mode"] = "false"
                        session.answers["stage"] = "awaiting_source"
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        val resolvedSub = session.answers["subject_name"] ?: "Subject"

                        coroutineScope.launch {
                            delay(250)
                            chatMessages.add(
                                AskAIChatMessage(
                                    isUser = false,
                                    text = "### 🎯 Target: **$resolvedSub** → **$cleanChap**\n\nPlease attach your book pages, PDF, images, or paste your source text.",
                                    options = emptyList()
                                )
                            )
                            scrollToBottom()
                        }
                        return
                    }

                    else -> {
                        // Stage: awaiting_source (default)
                        if (cleanAnswer.contains("Take Photo", ignoreCase = true) || cleanAnswer.contains("Camera", ignoreCase = true) || cleanAnswer.contains("📷 Take", ignoreCase = true) || cleanAnswer.contains("Take Another Photo", ignoreCase = true) || (cleanAnswer.contains("📷", ignoreCase = true) && cleanAnswer.contains("Photo", ignoreCase = true))) {
                            chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                            triggerLaunchCamera = true
                            return
                        }

                        if (cleanAnswer.contains("Gallery", ignoreCase = true) || cleanAnswer.contains("Attach Photos", ignoreCase = true) || cleanAnswer.contains("Pick More from Gallery", ignoreCase = true) || cleanAnswer.contains("🖼️", ignoreCase = true) || cleanAnswer.equals("Photos", ignoreCase = true)) {
                            chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                            triggerLaunchImagePicker = true
                            return
                        }

                        if (cleanAnswer.contains("Select PDF", ignoreCase = true) || cleanAnswer.contains("📄 Select", ignoreCase = true) || cleanAnswer.equals("PDF", ignoreCase = true)) {
                            chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                            triggerLaunchDocPicker = true
                            return
                        }

                        if (cleanAnswer.contains("Paste Source Text", ignoreCase = true) || cleanAnswer.contains("📝 Paste", ignoreCase = true)) {
                            chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                            customPlaceholderHint = "Paste your source text snippet here..."
                            coroutineScope.launch {
                                delay(60)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                                scrollToBottom()
                            }
                            return
                        }

                        if (cleanAnswer.contains("Use Chapter Context", ignoreCase = true)) {
                            chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                            session.answers["accumulated_source"] = ((session.answers["accumulated_source"] ?: "") + "\n\n" + fullChapterText).trim()
                            generateProposalForWorkflow(session)
                            return
                        }

                        // User provided source text directly
                        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                        session.answers["accumulated_source"] = ((session.answers["accumulated_source"] ?: "") + "\n\n" + cleanAnswer).trim()
                        generateProposalForWorkflow(session)
                        return
                    }
                }
            }
        }

        // Direct Custom handling (no extra messages created)
        if (cleanAnswer.equals("✎ Custom", ignoreCase = true) ||
            cleanAnswer.equals("Custom", ignoreCase = true) ||
            cleanAnswer.equals("✎ Custom adjustment", ignoreCase = true)) {
            val currentQ = session.questions.getOrNull(session.currentStepIndex)
            customPlaceholderHint = when {
                session.isProposalActive -> "Enter your custom adjustment (e.g. 4.5 hrs, morning only)…"
                currentQ?.questionText?.contains("hour", ignoreCase = true) == true -> "Enter your daily study hours…"
                currentQ?.questionText?.contains("subject", ignoreCase = true) == true -> "Enter your custom subjects…"
                currentQ?.questionText?.contains("exam", ignoreCase = true) == true -> "Enter your target exam or goal…"
                currentQ?.questionText?.contains("chapter", ignoreCase = true) == true -> "Enter your custom topic or chapter…"
                else -> "Enter your custom answer…"
            }
            coroutineScope.launch {
                delay(60)
                focusRequester.requestFocus()
                keyboardController?.show()
                scrollToBottom()
            }
            return
        }

        // If in proposal phase
        if (session.isProposalActive) {
            val cleanAnswer = answerText.trim()
            when {
                cleanAnswer.contains("Approve", ignoreCase = true) ||
                cleanAnswer.contains("Save", ignoreCase = true) ||
                cleanAnswer.contains("Confirm", ignoreCase = true) ||
                cleanAnswer.contains("✓") -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    isGenerating = true
                    session.isProposalActive = false
                    session.isCompleted = true

                    coroutineScope.launch {
                        delay(600)

                        // Execute database action based on workflow type
                        viewModel?.let { vm ->
                            when (session.type) {
                                AssistantWorkflowType.WEEKLY_PLAN, AssistantWorkflowType.FREE_TIME_SCHEDULE -> {
                                    val lastPlan = chatMessages.lastOrNull { it.artifact is AiResultArtifact.WeeklyPlan }?.artifact as? AiResultArtifact.WeeklyPlan
                                    if (lastPlan != null && lastPlan.days.isNotEmpty()) {
                                        lastPlan.days.forEach { dayItem ->
                                            val dayCode = when (dayItem.dayName.take(3).uppercase()) {
                                                "MON" -> "MON"
                                                "TUE" -> "TUE"
                                                "WED" -> "WED"
                                                "THU" -> "THU"
                                                "FRI" -> "FRI"
                                                "SAT" -> "SAT"
                                                "SUN" -> "SUN"
                                                else -> "MON"
                                            }
                                            dayItem.slots.forEach { slot ->
                                                val timeParts = slot.timeSlot.split("-").map { it.trim() }
                                                val start = timeParts.getOrNull(0)?.ifBlank { "7:00 PM" } ?: "7:00 PM"
                                                val end = timeParts.getOrNull(1)?.ifBlank { "7:45 PM" } ?: "7:45 PM"
                                                vm.createStudyTask(
                                                    subjectName = slot.subjectName.substringBefore(":").trim(),
                                                    taskTitle = slot.taskTitle,
                                                    taskType = slot.taskType,
                                                    startTime = start,
                                                    endTime = end,
                                                    durationMinutes = slot.durationMinutes,
                                                    daysOfWeek = dayCode,
                                                    repeatWeekly = true
                                                )
                                            }
                                        }
                                        val dailyMin = lastPlan.days.firstOrNull()?.slots?.sumOf { it.durationMinutes } ?: (preferences?.dailyTargetMinutes ?: 45)
                                        vm.updateDailyTargetMinutes(dailyMin)
                                    } else {
                                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                        val subList = if (subjects.isNotEmpty()) subjects.map { it.name } else listOf("General Studies", "Aptitude", "Revision")
                                        days.forEachIndexed { idx, day ->
                                            val subName = subList[idx % subList.size]
                                            val dayCode = day.take(3).uppercase()
                                            vm.createStudyTask(
                                                subjectName = subName,
                                                taskTitle = "$subName Core Study",
                                                taskType = if (day in listOf("Sat", "Sun")) "QUIZ" else "READING",
                                                startTime = "7:00 PM",
                                                endTime = "7:45 PM",
                                                durationMinutes = preferences?.dailyTargetMinutes ?: 45,
                                                daysOfWeek = dayCode,
                                                repeatWeekly = true
                                            )
                                        }
                                        vm.updateDailyTargetMinutes(preferences?.dailyTargetMinutes ?: 45)
                                    }
                                }

                                AssistantWorkflowType.SETUP_SYLLABUS -> {
                                    val chosenSubjectName = session.answers["main_subjects"] ?: "Exam Subject"
                                    val syllabusResult = ExtractedSyllabusResult(
                                        examName = session.answers["exam_curriculum"] ?: "Target Exam",
                                        subjects = listOf(
                                            ExtractedSubjectItem(
                                                name = chosenSubjectName,
                                                colorHex = "#8B5CF6",
                                                chapters = listOf(
                                                    ExtractedChapterItem("Chapter 1: Fundamentals & Core Definitions", 1, listOf("Overview", "Key formulas", "Definitions")),
                                                    ExtractedChapterItem("Chapter 2: In-Depth Concepts & Application", 2, listOf("Principles", "Worked examples")),
                                                    ExtractedChapterItem("Chapter 3: High-Yield Practice & MCQs", 3, listOf("PYQs", "Speed drills")),
                                                    ExtractedChapterItem("Chapter 4: Quick Revision & Summary", 4, listOf("Cheat sheet", "Error notes"))
                                                )
                                            )
                                        )
                                    )
                                    vm.saveWorkflowSyllabus(syllabusResult)
                                }

                                AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE -> {
                                    val lastMessage = chatMessages.lastOrNull { it.artifact is AiResultArtifact.StructuredNotes }
                                    val rawNotes = (lastMessage?.artifact as? AiResultArtifact.StructuredNotes)?.rawMarkdownNotes ?: ""
                                    val resolvedSubName = session.answers["subject_name"] ?: subjectName.ifBlank { "General Studies" }
                                    val resolvedTitle = session.answers["chapter_title"] ?: chapterTitle.ifBlank { "Structured Study Notes" }
                                    val resolvedSubId = session.answers["subject_id"]
                                    val resolvedChapId = session.answers["chapter_id"] ?: chapterId.ifBlank { null }

                                    if (isContextualMode && rawNotes.isNotBlank()) {
                                        onInsertBelow(rawNotes)
                                    } else if (rawNotes.isNotBlank()) {
                                        vm.saveStructuredNoteToLibrary(
                                            subjectName = resolvedSubName,
                                            chapterTitle = resolvedTitle,
                                            markdownNotes = rawNotes,
                                            existingSubjectId = resolvedSubId,
                                            existingChapterId = resolvedChapId,
                                            existingExamId = session.answers["exam_id"]
                                        )
                                    }
                                }

                                else -> {}
                            }
                        }

                        isGenerating = false
                        val resolvedSubName = session.answers["subject_name"] ?: subjectName.ifBlank { "General Studies" }
                        val resolvedTitle = session.answers["chapter_title"] ?: chapterTitle.ifBlank { "Structured Study Notes" }

                        val confirmationText = when (session.type) {
                            AssistantWorkflowType.WEEKLY_PLAN, AssistantWorkflowType.FREE_TIME_SCHEDULE ->
                                "✓ **Plan Approved & Synced!**\n\nYour study sessions have been added to your PrepOS Timetable. You can view your updated schedule on the Home screen or start a focus timer right away."
                            AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE ->
                                if (isContextualMode) {
                                    "✓ **Notes Saved Successfully!**\n\nYour structured notes have been saved to **$resolvedTitle** ($resolvedSubName) in your PrepOS Library."
                                } else {
                                    "✓ **Notes Saved Successfully to Library!**\n\n" +
                                    "📚 **Subject:** **$resolvedSubName**\n" +
                                    "📖 **Chapter:** **$resolvedTitle**\n\n" +
                                    "Your structured notes are saved in your PrepOS Library."
                                }
                            AssistantWorkflowType.SETUP_SYLLABUS ->
                                "✓ **Syllabus Created Successfully!**\n\nYour new subjects and structured chapters are ready in your PrepOS Library. You can open them in the Note Editor to start taking active recall notes."
                            AssistantWorkflowType.EXAM_STRATEGY ->
                                "✓ **Exam Strategy Confirmed!**\n\nYour phased roadmap is ready. Stay consistent with your Phase 1 foundation drills this week."
                            AssistantWorkflowType.FOCUS_NEXT ->
                                "✓ **Session Ready!**\n\nTimer recommendations set. Let's make this study block count!"
                            AssistantWorkflowType.HOW_IT_WORKS ->
                                "✓ You're all set! Let me know whenever you need help organizing your study routine."
                        }

                        val confirmationOptions = if (session.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
                            emptyList()
                        } else {
                            listOf("🏠 Go to Home Dashboard", "⏱️ Start Focus Mode", "✦ Ask another question")
                        }

                        chatMessages.add(
                            AskAIChatMessage(
                                isUser = false,
                                text = confirmationText,
                                options = confirmationOptions
                            )
                        )
                        scrollToBottom()
                    }
                    return
                }

                // Generic "Make Changes" request
                cleanAnswer.equals("Make Changes ✎", ignoreCase = true) ||
                cleanAnswer.equals("Make Changes", ignoreCase = true) ||
                cleanAnswer.equals("Adjust", ignoreCase = true) ||
                cleanAnswer.equals("Modify", ignoreCase = true) ||
                cleanAnswer.equals("Change schedule", ignoreCase = true) -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    isGenerating = true
                    coroutineScope.launch {
                        delay(450)
                        isGenerating = false
                        chatMessages.add(
                            AskAIChatMessage(
                                isUser = false,
                                text = "Sure! What adjustments would you like to make to your timetable?",
                                options = listOf(
                                    "Change daily study hours",
                                    "Flexible day timing",
                                    "Shift to evening only",
                                    "Shift to morning only",
                                    "Add weekend mock test",
                                    "✎ Custom adjustment"
                                )
                            )
                        )
                        scrollToBottom()
                    }
                    return
                }

                // User tapped "Change daily study hours"
                cleanAnswer.equals("Change daily study hours", ignoreCase = true) ||
                cleanAnswer.equals("Change study hours", ignoreCase = true) ||
                cleanAnswer.equals("Adjust study hours", ignoreCase = true) -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    isGenerating = true
                    coroutineScope.launch {
                        delay(400)
                        isGenerating = false
                        chatMessages.add(
                            AskAIChatMessage(
                                isUser = false,
                                text = "How many hours of focused study would you like to schedule each day?",
                                options = listOf("1 Hour / day", "2 Hours / day", "3 Hours / day", "4 Hours / day", "5+ Hours / day", "6+ Hours / day")
                            )
                        )
                        scrollToBottom()
                    }
                    return
                }

                // User selected hours directly (e.g. "4 Hours / day", "3 Hours / day", "4 hours")
                cleanAnswer.matches(Regex("^\\d+\\s*(hours?|hrs?|h).*$", RegexOption.IGNORE_CASE)) ||
                listOf("1 Hour / day", "2 Hours / day", "3 Hours / day", "4 Hours / day", "5+ Hours / day", "6+ Hours / day").any { it.equals(cleanAnswer, ignoreCase = true) } -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    session.answers["daily_hours"] = cleanAnswer
                    session.answers["adjustment_request"] = "Set daily hours to $cleanAnswer"
                    generateProposalForWorkflow(session)
                    return
                }

                // Flexible day timing requested
                cleanAnswer.contains("flexible", ignoreCase = true) ||
                cleanAnswer.contains("some at day", ignoreCase = true) ||
                cleanAnswer.equals("Flexible day timing", ignoreCase = true) ||
                (cleanAnswer.contains("day", ignoreCase = true) && !cleanAnswer.contains("/ day", ignoreCase = true) && !cleanAnswer.contains("/day", ignoreCase = true)) -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    session.answers["timing_pref"] = "flexible_day"
                    session.answers["schedule_representation"] = "Flexible Daily Time"
                    session.answers["adjustment_request"] = cleanAnswer
                    generateProposalForWorkflow(session)
                    return
                }

                // Evening only
                cleanAnswer.contains("evening", ignoreCase = true) || cleanAnswer.contains("night", ignoreCase = true) -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    session.answers["timing_pref"] = "evening"
                    session.answers["adjustment_request"] = "Shift to evening slots"
                    generateProposalForWorkflow(session)
                    return
                }

                // Morning only
                cleanAnswer.contains("morning", ignoreCase = true) -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    session.answers["timing_pref"] = "morning"
                    session.answers["adjustment_request"] = "Shift to morning slots"
                    generateProposalForWorkflow(session)
                    return
                }

                // Weekend mock test
                cleanAnswer.contains("mock", ignoreCase = true) || cleanAnswer.contains("weekend", ignoreCase = true) -> {
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    session.answers["revision_pref"] = "Yes, Sunday Revision & Mock"
                    session.answers["adjustment_request"] = "Add weekend mock test & error diagnosis"
                    generateProposalForWorkflow(session)
                    return
                }

                // Custom adjustment selected
                cleanAnswer.equals("✎ Custom adjustment", ignoreCase = true) ||
                cleanAnswer.equals("✎ Custom", ignoreCase = true) ||
                cleanAnswer.equals("Custom adjustment", ignoreCase = true) ||
                cleanAnswer.equals("Custom", ignoreCase = true) -> {
                    customPlaceholderHint = "Enter your custom adjustment (e.g. 4.5 hrs, morning only)…"
                    coroutineScope.launch {
                        delay(60)
                        focusRequester.requestFocus()
                        keyboardController?.show()
                        scrollToBottom()
                    }
                    return
                }

                else -> {
                    // User provided specific custom adjustment request
                    chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
                    val hourMatch = Regex("(\\d+)\\s*(hours?|hrs?|h)", RegexOption.IGNORE_CASE).find(cleanAnswer)
                    if (hourMatch != null) {
                        session.answers["daily_hours"] = "${hourMatch.groupValues[1]} Hours / day"
                    }
                    if (cleanAnswer.contains("flexible", ignoreCase = true) || cleanAnswer.contains("day", ignoreCase = true)) {
                        session.answers["timing_pref"] = "flexible_day"
                        session.answers["schedule_representation"] = "Flexible Daily Time"
                    }
                    session.answers["adjustment_request"] = cleanAnswer
                    generateProposalForWorkflow(session)
                    return
                }
            }
        }

        // Standard Step Question processing
        val currentQuestion = session.questions.getOrNull(session.currentStepIndex)
        if (currentQuestion != null) {
            session.answers[currentQuestion.id] = answerText
        }

        chatMessages.add(AskAIChatMessage(isUser = true, text = answerText))
        session.currentStepIndex++

        if (session.currentStepIndex < session.questions.size) {
            val nextQuestion = session.questions[session.currentStepIndex]
            isGenerating = true
            scrollToBottom()

            coroutineScope.launch {
                delay(650) // Smooth natural message cadence
                isGenerating = false
                val nextText = if (nextQuestion.introMessage != null) {
                    "${nextQuestion.introMessage}\n\n${nextQuestion.questionText}"
                } else {
                    nextQuestion.questionText
                }
                chatMessages.add(
                    AskAIChatMessage(
                        isUser = false,
                        text = nextText,
                        options = nextQuestion.options
                    )
                )
                scrollToBottom()
            }
        } else {
            // All questions answered -> generate structured AI proposal
            generateProposalForWorkflow(session)
        }
    }

    // Submit Free-form User Prompt
    fun submitPrompt(customPrompt: String = "", actionTag: String? = null) {
        val effectivePrompt = customPrompt.ifBlank { inputText }.trim()
        val currentAttachments = attachedMediaList.toList()
        if (effectivePrompt.isBlank() && currentAttachments.isEmpty()) return

        // Check if user clicked a quick suggestion that matches a workflow
        when {
            effectivePrompt.contains("How does PrepOS work", ignoreCase = true) ||
            effectivePrompt.contains("how prepos works", ignoreCase = true) ||
            effectivePrompt.contains("how to use prepos", ignoreCase = true) ||
            effectivePrompt.contains("what can i do on test screen", ignoreCase = true) ||
            effectivePrompt.contains("what can i do on this screen", ignoreCase = true) -> {
                startConversationalWorkflow(AssistantWorkflowType.HOW_IT_WORKS)
                if (effectivePrompt.contains("test screen", ignoreCase = true) ||
                    effectivePrompt.contains("this screen", ignoreCase = true) ||
                    effectivePrompt.contains("note editor", ignoreCase = true) ||
                    effectivePrompt.contains("focus hub", ignoreCase = true) ||
                    effectivePrompt.contains("home screen", ignoreCase = true)) {
                    handleOfflineHowItWorks(effectivePrompt)
                }
                inputText = ""
                return
            }
            effectivePrompt.contains("weekly plan", ignoreCase = true) -> {
                startConversationalWorkflow(AssistantWorkflowType.WEEKLY_PLAN)
                inputText = ""
                return
            }
            effectivePrompt.contains("create notes", ignoreCase = true) || effectivePrompt.contains("notes from source", ignoreCase = true) -> {
                startConversationalWorkflow(AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE)
                inputText = ""
                return
            }
            effectivePrompt.contains("free time", ignoreCase = true) || effectivePrompt.contains("daily plan", ignoreCase = true) -> {
                startConversationalWorkflow(AssistantWorkflowType.FREE_TIME_SCHEDULE)
                inputText = ""
                return
            }
            effectivePrompt.contains("syllabus", ignoreCase = true) -> {
                startConversationalWorkflow(AssistantWorkflowType.SETUP_SYLLABUS)
                inputText = ""
                return
            }
            effectivePrompt.contains("exam strategy", ignoreCase = true) -> {
                startConversationalWorkflow(AssistantWorkflowType.EXAM_STRATEGY)
                inputText = ""
                return
            }
            effectivePrompt.contains("focus next", ignoreCase = true) || effectivePrompt.contains("what should I study", ignoreCase = true) -> {
                startConversationalWorkflow(AssistantWorkflowType.FOCUS_NEXT)
                inputText = ""
                return
            }
            effectivePrompt.contains("Go to Home Dashboard", ignoreCase = true) || effectivePrompt.contains("Go to Home Screen", ignoreCase = true) -> {
                onNavigateTab("home")
                onDismiss()
                return
            }
            effectivePrompt.contains("Start Focus Mode", ignoreCase = true) || effectivePrompt.contains("Start Focus Session Now", ignoreCase = true) -> {
                onNavigateTab("plan")
                onDismiss()
                return
            }
        }

        // Check if user submitted or pasted an API Key (or key format)
        val detectedKey = StudyAIService.extractGeminiApiKey(effectivePrompt)
        if (detectedKey != null) {
            val masked = "AIzaSy••••••••${detectedKey.takeLast(4)}"
            chatMessages.add(
                AskAIChatMessage(
                    isUser = true,
                    actionTag = actionTag,
                    text = "🔑 $masked"
                )
            )
            inputText = ""
            attachedMediaList.clear()

            viewModel?.saveApiKey(detectedKey)
            viewModel?.saveAiProvider("GEMINI")

            chatMessages.add(
                AskAIChatMessage(
                    isUser = false,
                    text = "✅ **Gemini API Key Connected Successfully!**\n\nYour free key has been automatically detected and securely saved on your device (`$masked`).\n\nYou can now use all AI features including Ask AI Doubt Solver, Mock Test Generator, and Smart Note Sanitizer!\n\nWhat would you like to study or ask today?",
                    options = listOf("Ask a Doubt 💡", "Create Study Plan 📅", "Generate Mock Test 📝")
                )
            )
            Toast.makeText(context, "✓ Gemini API Key saved successfully!", Toast.LENGTH_SHORT).show()
            scrollToBottom()
            return
        }

        // Check if user is asking how to get an API key
        val isAskingAboutKey = (effectivePrompt.contains("api key", ignoreCase = true) || effectivePrompt.contains("apikey", ignoreCase = true)) &&
                (effectivePrompt.contains("how", ignoreCase = true) || effectivePrompt.contains("get", ignoreCase = true) || effectivePrompt.contains("free", ignoreCase = true) || effectivePrompt.contains("where", ignoreCase = true))
        if (isAskingAboutKey) {
            chatMessages.add(
                AskAIChatMessage(
                    isUser = true,
                    actionTag = actionTag,
                    text = effectivePrompt
                )
            )
            inputText = ""
            attachedMediaList.clear()

            chatMessages.add(
                AskAIChatMessage(
                    isUser = false,
                    text = """
                        ### 🚀 3 Easy Steps to Get Your Free Gemini API Key:

                        1. **Open Google AI Studio:** Click **"🔑 Get Free API Key"** below or visit [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey).
                        2. **Sign in with Google:** Sign in with your standard Gmail account (100% free, no credit card or payment required).
                        3. **Create & Copy Key:** Click **"Create API key"**, select or create a project, and copy your key (starts with `AIzaSy...`).
                        4. **Paste & Connect:** Paste your key right here in this chat or tap **"📋 Paste from Clipboard"**.

                        ✨ **You get 1,500 requests every day completely free!**
                    """.trimIndent(),
                    options = listOf("🔑 Get Free API Key", "📋 Paste from Clipboard", "⚙️ Open Settings")
                )
            )
            scrollToBottom()
            return
        }

        // If an active workflow is waiting for input
        if (activeWorkflow != null && !activeWorkflow!!.isCompleted) {
            val session = activeWorkflow!!
            customPlaceholderHint = null

            // Special composer-first handling for CREATE_NOTES_FROM_SOURCE
            if (session.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
                if (session.isProposalActive) {
                    // User is making adjustments to generated notes
                    val userText = effectivePrompt.ifBlank { "Apply requested changes" }
                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = true,
                            text = userText,
                            attachments = currentAttachments
                        )
                    )
                    session.accumulatedAttachments.addAll(currentAttachments)
                    session.answers["adjustment_request"] = effectivePrompt
                    inputText = ""
                    attachedMediaList.clear()
                    generateProposalForWorkflow(session)
                    return
                }

                val stage = session.answers["stage"]
                if (stage == "awaiting_source" || stage == null) {
                    val userDisplayText = if (effectivePrompt.isNotBlank()) {
                        effectivePrompt
                    } else if (currentAttachments.isNotEmpty()) {
                        "📝 Create structured notes from source (${currentAttachments.size} attachment${if (currentAttachments.size > 1) "s" else ""})"
                    } else {
                        "📝 Create structured study notes"
                    }

                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = true,
                            text = userDisplayText,
                            attachments = currentAttachments
                        )
                    )

                    session.accumulatedAttachments.addAll(currentAttachments)
                    if (effectivePrompt.isNotBlank()) {
                        session.answers["accumulated_source"] = ((session.answers["accumulated_source"] ?: "") + "\n\n" + effectivePrompt).trim()
                    }

                    inputText = ""
                    attachedMediaList.clear()
                    generateProposalForWorkflow(session)
                    return
                }
            }

            activeWorkflow?.accumulatedAttachments?.addAll(currentAttachments)
            val textToProcess = if (effectivePrompt.isNotBlank()) effectivePrompt else if (currentAttachments.isNotEmpty()) "Attached ${currentAttachments.size} media file(s)" else ""
            processConversationalAnswer(textToProcess)
            inputText = ""
            attachedMediaList.clear()
            return
        }

        // Check if API Key is configured before making external AI query
        val isKeyReady = StudyAIService.isGeminiKeyConfigured(effectiveApiKey)
        if (!isKeyReady) {
            chatMessages.add(
                AskAIChatMessage(
                    isUser = true,
                    actionTag = actionTag,
                    text = effectivePrompt,
                    attachments = currentAttachments
                )
            )
            inputText = ""
            attachedMediaList.clear()

            chatMessages.add(
                AskAIChatMessage(
                    isUser = false,
                    text = "To use AI features and answer **\"${effectivePrompt.take(50)}${if (effectivePrompt.length > 50) "..." else ""}\"**, please connect your Gemini API Key.\n\nGoogle AI Studio provides a **100% free API key** with **1,500 requests per day** and no credit card required!\n\nWould you like to get a free key now or paste an existing key?",
                    options = listOf("🔑 Get Free API Key", "📖 How to get free API key?", "📋 Paste from Clipboard", "⚙️ Go to Settings")
                )
            )
            scrollToBottom()
            return
        }

        // Standard conversational chat message
        customPlaceholderHint = null
        val userMessage = AskAIChatMessage(
            isUser = true,
            actionTag = actionTag,
            text = effectivePrompt,
            attachments = currentAttachments
        )
        chatMessages.add(userMessage)

        inputText = ""
        attachedMediaList.clear()
        isGenerating = true
        scrollToBottom()

        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            val userName = savedUserName.ifBlank { "Student" }
            val subjectNames = subjects.joinToString(", ") { it.name }.ifBlank { "General Studies" }
            val streak = preferences?.currentStreak ?: 0
            val targetMin = preferences?.dailyTargetMinutes ?: 45
            val focusedToday = preferences?.todayFocusedMinutes ?: 0

            val diagramContext = if (selectedDiagram != null) {
                """
                Target Diagram to Modify / Study:
                - Type: ${selectedDiagram.diagramType.name}
                - Title: ${selectedDiagram.title}
                - Nodes/Parts: ${selectedDiagram.nodes.joinToString(" | ")}
                """.trimIndent()
            } else ""

            val appContext = """
                User Name: $userName
                Current Subjects: $subjectNames
                Daily Target: $targetMin minutes (Studied Today: $focusedToday minutes)
                Current Streak: $streak days
                Active Editor Chapter: ${if (chapterTitle.isNotBlank()) chapterTitle else "None"}
                Chapter Content Context: ${if (fullChapterText.isNotBlank()) fullChapterText.take(1200) else "None"}
                $diagramContext
            """.trimIndent()

            val aiAttachments = currentAttachments.map { att ->
                AIAttachment(
                    name = att.name,
                    mimeType = att.mimeType,
                    base64Data = att.base64Data,
                    textContent = att.textSnippet
                )
            }

            val result = StudyAIService.executeCustomInstruction(
                baseContextText = appContext,
                userInstruction = effectivePrompt,
                contextTitle = "PrepOS AI Companion for $userName",
                attachments = aiAttachments,
                apiKey = effectiveApiKey,
                model = activeModel,
                provider = activeProvider,
                deepSeekApiKey = effectiveDeepSeekKey
            )

            // Ensure natural thinking transition: thinking indicator (~2.2s) followed by 3-dots, then reveal
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 2600L) {
                delay(2600L - elapsed)
            }

            isGenerating = false
            val aiResponseText = result.getOrDefault(
                "I'm here to assist your study routine! How else can I help you achieve your goals today?"
            )

            chatMessages.add(
                AskAIChatMessage(
                    isUser = false,
                    text = aiResponseText,
                    options = listOf("Ask follow-up doubt ✦", "Create study plan 📅", "Explain with example 💡")
                )
            )
            scrollToBottom()
        }
    }

    // Handle option click: if user selects Custom, directly open bottom composer with dim hint without creating extra messages
    fun handleOptionClick(chosenOption: String) {
        val cleanOption = chosenOption.trim()
        val isCustom = cleanOption.contains("Custom", ignoreCase = true) ||
                cleanOption.equals("✎ Custom", ignoreCase = true) ||
                cleanOption.equals("✎ Custom adjustment", ignoreCase = true)

        if (isCustom) {
            val contextualHint = if (activeWorkflow != null) {
                val session = activeWorkflow!!
                if (session.isProposalActive) {
                    "Enter your custom adjustment (e.g. 4.5 hrs, morning only)…"
                } else {
                    val currentQ = session.questions.getOrNull(session.currentStepIndex)
                    when {
                        currentQ?.questionText?.contains("hour", ignoreCase = true) == true -> "Enter your daily study hours…"
                        currentQ?.questionText?.contains("subject", ignoreCase = true) == true -> "Enter your custom subjects…"
                        currentQ?.questionText?.contains("exam", ignoreCase = true) == true -> "Enter your target exam or goal…"
                        currentQ?.questionText?.contains("chapter", ignoreCase = true) == true -> "Enter your custom topic or chapter…"
                        else -> "Enter your custom answer…"
                    }
                }
            } else {
                "Enter your custom response…"
            }

            customPlaceholderHint = contextualHint
            coroutineScope.launch {
                delay(60)
                focusRequester.requestFocus()
                keyboardController?.show()
                scrollToBottom()
            }
            return
        }

        // Handle Interactive API Key Onboarding Options
        when {
            cleanOption.contains("Get Free API Key", ignoreCase = true) || cleanOption.equals("🔑 Get Free API Key", ignoreCase = true) || cleanOption.equals("Get API Key", ignoreCase = true) -> {
                val url = "https://aistudio.google.com/app/apikey"
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                chatMessages.add(
                    AskAIChatMessage(
                        isUser = false,
                        text = "Opening Google AI Studio in your browser...\n\n**Have you got your free API Key from Google AI Studio?**",
                        options = listOf("✅ Yes, I have it", "📋 Paste from Clipboard", "📖 How to get free API key?", "⚙️ Open Settings")
                    )
                )
                scrollToBottom()
                return
            }

            cleanOption.equals("✅ Yes, I have it", ignoreCase = true) || cleanOption.contains("I have it", ignoreCase = true) -> {
                chatMessages.add(
                    AskAIChatMessage(
                        isUser = false,
                        text = "Awesome! 🎉 Please paste your API key in the chat message box below and tap send, or click **📋 Paste from Clipboard** to connect instantly.",
                        options = listOf("📋 Paste from Clipboard", "⚙️ Open Settings")
                    )
                )
                coroutineScope.launch {
                    delay(80)
                    focusRequester.requestFocus()
                    keyboardController?.show()
                    scrollToBottom()
                }
                return
            }

            cleanOption.contains("How to get", ignoreCase = true) || cleanOption.contains("How to get free API key", ignoreCase = true) -> {
                chatMessages.add(
                    AskAIChatMessage(
                        isUser = false,
                        text = """
                            ### 🚀 3 Easy Steps to Get Your Free Gemini API Key:

                            1. **Open Google AI Studio:** Click **"🔑 Get Free API Key"** below or visit [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey).
                            2. **Sign in with Google:** Sign in with your standard Gmail account (100% free, no credit card or payment required).
                            3. **Create & Copy Key:** Click **"Create API key"**, select or create a project, and copy your key (starts with `AIzaSy...`).
                            4. **Paste & Connect:** Paste your key right here in this chat or tap **"📋 Paste from Clipboard"**.

                            ✨ **You get 1,500 requests every day completely free!**
                        """.trimIndent(),
                        options = listOf("🔑 Get Free API Key", "📋 Paste from Clipboard", "⚙️ Open Settings")
                    )
                )
                scrollToBottom()
                return
            }

            cleanOption.contains("Paste from Clipboard", ignoreCase = true) || cleanOption.contains("Paste API Key", ignoreCase = true) -> {
                val clipText = clipboardManager.getText()?.text?.trim() ?: ""
                val extractedKey = StudyAIService.extractGeminiApiKey(clipText) ?: if (StudyAIService.isValidGeminiApiKeyFormat(clipText)) clipText else null
                if (!extractedKey.isNullOrBlank()) {
                    viewModel?.saveApiKey(extractedKey)
                    viewModel?.saveAiProvider("GEMINI")
                    val masked = "AIzaSy••••••••${extractedKey.takeLast(4)}"
                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = "✅ **Gemini API Key Connected Successfully!**\n\nYour key is securely saved on your device (`$masked`). You can now use all AI features including Ask AI Tutor, Mock Test Generator, and Smart Note Sanitizer.\n\nWhat would you like to study or ask today?",
                            options = listOf("Ask a Doubt 💡", "Create Study Plan 📅", "Generate Mock Test 📝")
                        )
                    )
                    Toast.makeText(context, "✓ Gemini API Key saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    chatMessages.add(
                        AskAIChatMessage(
                            isUser = false,
                            text = "Clipboard doesn't contain a valid Gemini API key yet. Please copy your key from Google AI Studio and try again, or paste it directly into the chat input below.",
                            options = listOf("🔑 Get Free API Key", "📖 How to get free API key?", "⚙️ Open Settings")
                        )
                    )
                }
                scrollToBottom()
                return
            }

            cleanOption.contains("Go to Settings", ignoreCase = true) || cleanOption.contains("Open Settings", ignoreCase = true) || cleanOption.equals("Settings", ignoreCase = true) -> {
                onNavigateTab("settings")
                return
            }
        }

        customPlaceholderHint = null
        if (activeWorkflow != null && !activeWorkflow!!.isCompleted) {
            processConversationalAnswer(chosenOption)
        } else {
            submitPrompt(chosenOption)
        }
    }

    fun getActiveTargetContent(): String {
        return when (activeScopeType) {
            TargetScopeType.SELECTION -> selectedText.ifBlank { fullChapterText }
            TargetScopeType.TOPIC -> {
                val topic = topics.find { it.topicId == selectedTopicId } ?: topics.firstOrNull()
                if (topic != null) {
                    val plain = topic.toPlainText().trim()
                    if (plain.startsWith(topic.topicTitle, ignoreCase = true)) {
                        plain
                    } else {
                        "${topic.topicTitle}\n\n$plain".trim()
                    }
                } else {
                    fullChapterText
                }
            }
            TargetScopeType.WHOLE_CHAPTER -> fullChapterText.ifBlank { "Chapter: $chapterTitle" }
            TargetScopeType.DIAGRAM -> {
                selectedDiagram?.let {
                    "Diagram: ${it.title} (${it.diagramType.name})\nNodes:\n${it.nodes.joinToString("\n• ")}"
                } ?: fullChapterText
            }
        }
    }

    fun getActiveTargetName(): String {
        return when (activeScopeType) {
            TargetScopeType.SELECTION -> "Selection (${selectedText.split(Regex("\\s+")).filter { it.isNotBlank() }.size} words)"
            TargetScopeType.TOPIC -> {
                val topic = topics.find { it.topicId == selectedTopicId } ?: topics.firstOrNull()
                topic?.topicTitle ?: "Current Topic"
            }
            TargetScopeType.WHOLE_CHAPTER -> "Whole Chapter ($chapterTitle)"
            TargetScopeType.DIAGRAM -> selectedDiagram?.title?.ifBlank { "Diagram" } ?: "Diagram"
        }
    }

    fun applyTransformation(transformation: ContentTransformation) {
        val targetContent = getActiveTargetContent()
        val targetName = getActiveTargetName()
        val prompt = """
            ${transformation.promptInstruction}

            ---
            TARGET CONTENT ($targetName):
            $targetContent
        """.trimIndent()

        submitPrompt(
            customPrompt = prompt,
            actionTag = "${transformation.emoji} ${transformation.title}"
        )
    }

    // Camera Launcher & Permission
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentCameraTempUri != null) {
            val uri = currentCameraTempUri!!
            try {
                val processed = ImageUtils.processImageUriForAi(context, uri, maxDimension = 1280)
                if (processed != null) {
                    val timeTag = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "BookPage_${timeTag}.jpg"
                    val media = AttachedUiMedia(
                        name = fileName,
                        mimeType = "image/jpeg",
                        isImage = true,
                        uri = uri,
                        bitmap = processed.bitmap,
                        sizeText = processed.sizeText,
                        base64Data = processed.base64Data
                    )
                    attachedMediaList.add(media)
                    if (activeWorkflow != null && activeWorkflow?.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
                        activeWorkflow?.accumulatedAttachments?.add(media)
                    }
                    Toast.makeText(context, "📷 Photo added to composer", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to capture photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showInAppCameraScanner = true
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos of notes & books", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            showInAppCameraScanner = true
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    // Photo and File Pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newMedias = mutableListOf<AttachedUiMedia>()
            uris.forEach { uri ->
                try {
                    val processed = ImageUtils.processImageUriForAi(context, uri, maxDimension = 1280)
                    if (processed != null) {
                        val fileName = queryFileName(context, uri) ?: "Photo_${System.currentTimeMillis() % 1000}.jpg"
                        val media = AttachedUiMedia(
                            name = fileName,
                            mimeType = "image/jpeg",
                            isImage = true,
                            uri = uri,
                            bitmap = processed.bitmap,
                            sizeText = processed.sizeText,
                            base64Data = processed.base64Data
                        )
                        attachedMediaList.add(media)
                        newMedias.add(media)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            if (newMedias.isNotEmpty() && activeWorkflow != null && activeWorkflow?.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
                activeWorkflow?.accumulatedAttachments?.addAll(newMedias)
            }
            Toast.makeText(context, "🖼️ ${newMedias.size} photo(s) added to composer", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val fileName = queryFileName(context, uri) ?: "document.txt"
                val mime = context.contentResolver.getType(uri) ?: "text/plain"
                val isTxt = mime.startsWith("text") || fileName.endsWith(".txt") || fileName.endsWith(".md")

                if (isTxt) {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    val media = AttachedUiMedia(
                        name = fileName,
                        mimeType = mime,
                        isImage = false,
                        uri = uri,
                        textSnippet = text,
                        sizeText = "${text.length} chars",
                        base64Data = null
                    )
                    attachedMediaList.add(media)
                    if (activeWorkflow != null && activeWorkflow?.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
                        activeWorkflow?.accumulatedAttachments?.add(media)
                    }
                } else {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    val base64 = if (bytes != null) Base64.encodeToString(bytes, Base64.NO_WRAP) else null
                    val media = AttachedUiMedia(
                        name = fileName,
                        mimeType = mime,
                        isImage = false,
                        uri = uri,
                        sizeText = if (bytes != null) "${bytes.size / 1024} KB" else "Document",
                        base64Data = base64
                    )
                    attachedMediaList.add(media)
                    if (activeWorkflow != null && activeWorkflow?.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
                        activeWorkflow?.accumulatedAttachments?.add(media)
                    }
                }
                Toast.makeText(context, "📄 File added to composer", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(triggerLaunchCamera) {
        if (triggerLaunchCamera) {
            triggerLaunchCamera = false
            launchCamera()
        }
    }

    LaunchedEffect(triggerLaunchImagePicker) {
        if (triggerLaunchImagePicker) {
            triggerLaunchImagePicker = false
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    LaunchedEffect(triggerLaunchDocPicker) {
        if (triggerLaunchDocPicker) {
            triggerLaunchDocPicker = false
            filePickerLauncher.launch(
                arrayOf("application/pdf", "text/plain", "*/*")
            )
        }
    }

    // MAIN SURFACE
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ask_ai_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // 1. TOP HEADER BAR
            TopHeaderBar(
                title = if (selectedDiagram != null) "Diagram Assistant" else if (selectedText.isNotBlank()) "Selection Assistant" else if (chapterTitle != "PrepOS Study Assistant") chapterTitle else "PrepOS AI Assistant",
                subtitle = if (selectedDiagram != null) "AI edit & structure tools" else if (isConversationActive) "Conversational Study Tutor" else "Your personal study companion",
                isConversationActive = isConversationActive,
                isContextualMode = isContextualMode,
                onBack = {
                    if (isConversationActive && !isContextualMode) {
                        chatMessages.clear()
                        activeWorkflow = null
                        isGenerating = false
                    } else {
                        onDismiss()
                    }
                },
                onClose = onDismiss,
                onOpenHistory = { showHistoryDialog = true },
                onOpenMore = { showMoreMenu = true }
            )

            HorizontalDivider(color = if (isDark) Color(0xFF1E293B) else AiPrimaryBorderSubtle, thickness = 0.8.dp)

            // 2. MAIN SCROLLABLE CONTENT (CAROUSEL WHEN EMPTY, CHAT STREAM WHEN ACTIVE)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // If NO chat active, show Welcome Greeting, Snapped 3D Carousel & Quick Actions
                if (chatMessages.isEmpty() && !isGenerating && activeWorkflow == null) {
                    if (isContextualMode) {
                        item(key = "contextual_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                // 1. Target Scope Selection Bar
                                Text(
                                    text = "TRANSFORMATION TARGET",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Horizontal scrollable filter chips for scope
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Whole Chapter
                                    FilterChip(
                                        selected = activeScopeType == TargetScopeType.WHOLE_CHAPTER,
                                        onClick = { activeScopeType = TargetScopeType.WHOLE_CHAPTER },
                                        label = { Text("📘 Whole Chapter") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isDark) Color(0xFF4F46E5) else AiPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )

                                    // Topics
                                    if (topics.isNotEmpty()) {
                                        topics.forEach { topic ->
                                            val isThisTopicSelected = activeScopeType == TargetScopeType.TOPIC && selectedTopicId == topic.topicId
                                            FilterChip(
                                                selected = isThisTopicSelected,
                                                onClick = {
                                                    activeScopeType = TargetScopeType.TOPIC
                                                    selectedTopicId = topic.topicId
                                                },
                                                label = { Text("📑 ${topic.topicTitle.take(20)}") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = if (isDark) Color(0xFF7C3AED) else AiPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    // Selection
                                    if (selectedText.isNotBlank()) {
                                        FilterChip(
                                            selected = activeScopeType == TargetScopeType.SELECTION,
                                            onClick = { activeScopeType = TargetScopeType.SELECTION },
                                            label = { Text("✂️ Selection") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (isDark) Color(0xFF2563EB) else AiPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }

                                    // Diagram
                                    if (selectedDiagram != null) {
                                        FilterChip(
                                            selected = activeScopeType == TargetScopeType.DIAGRAM,
                                            onClick = { activeScopeType = TargetScopeType.DIAGRAM },
                                            label = { Text("📊 Diagram") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (isDark) Color(0xFF059669) else AiPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Target Content Preview Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF131B2E) else LightSurfaceSecondary,
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF23314E) else AiPrimaryBorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🎯 Target: ${getActiveTargetName()}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                            Text(
                                                text = "Ready",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) Color(0xFF38BDF8) else SubjectBlue,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val fullTargetSnippet = getActiveTargetContent().trim()
                                        val previewSnippet = if (fullTargetSnippet.startsWith(getActiveTargetName(), ignoreCase = true)) {
                                            fullTargetSnippet.removePrefix(getActiveTargetName()).trim()
                                        } else {
                                            fullTargetSnippet
                                        }
                                        val displaySnippet = previewSnippet.ifBlank { fullTargetSnippet }
                                        Text(
                                            text = if (displaySnippet.length > 180) displaySnippet.take(180) + "..." else displaySnippet,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            ),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 2. Conditional Update Notes vs Generate Notes Action Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF1E293B) else LightSurface,
                                    border = BorderStroke(1.2.dp, if (isDark) Color(0xFF38BDF8) else AiPrimary),
                                    shadowElevation = if (isDark) 2.dp else 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            startConversationalWorkflow(AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE)
                                        }
                                        .testTag(if (!isChapterEmpty) "action_update_notes" else "action_generate_notes")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isDark) Color(0xFF0369A1) else Color(0xFFE0F2FE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (!isChapterEmpty) "🔄" else "📝",
                                                fontSize = 20.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (!isChapterEmpty) "Update Notes" else "Generate Notes",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color.White else LightTextPrimary,
                                                        fontSize = 14.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isDark) Color(0xFF0C4A6E) else Color(0xFFBAE6FD)
                                                ) {
                                                    Text(
                                                        text = if (!isChapterEmpty) "Active Chapter" else "Empty Chapter",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (!isChapterEmpty) {
                                                    "Update or merge new book pages, PDF, or concepts into $chapterTitle."
                                                } else {
                                                    "Attach book photos, PDF pages, or enter text to generate structured notes for $chapterTitle."
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 15.sp
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF38BDF8) else AiPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 3. Consolidated Single Card for 5 AI Transformations
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF1E293B) else LightSurface,
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                                    shadowElevation = if (isDark) 2.dp else 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_transformations_card")
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Header of the card
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "AI Transformations",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color(0xFFF1F5F9) else LightTextPrimary,
                                                        fontSize = 14.sp
                                                    )
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isDark) Color(0xFF312E81) else AiPrimarySoft
                                            ) {
                                                Text(
                                                    text = "5 Modes",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isDark) Color(0xFFA5B4FC) else AiPrimaryDark,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(
                                            color = if (isDark) Color(0xFF334155) else LightBorder,
                                            thickness = 1.dp
                                        )

                                        // 5 Transformation Options inside the single card
                                        contentTransformations.forEachIndexed { index, transformation ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        applyTransformation(transformation)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                                                    .testTag("transform_${transformation.title.lowercase().replace(" ", "_").replace("&", "and")}"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Emoji Badge
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(if (isDark) Color(0xFF0F172A) else AiPrimarySoft),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = transformation.emoji,
                                                        fontSize = 18.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = transformation.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (isDark) Color.White else LightTextPrimary,
                                                            fontSize = 13.5.sp
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = transformation.subtitle,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                                            fontSize = 11.5.sp,
                                                            lineHeight = 15.sp
                                                        ),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF64748B) else LightTextMuted,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            if (index < contentTransformations.lastIndex) {
                                                HorizontalDivider(
                                                    color = if (isDark) Color(0xFF243048) else Color(0xFFF1F5F9),
                                                    thickness = 0.8.dp,
                                                    modifier = Modifier.padding(start = 60.dp, end = 14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "carousel_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "How can I help you today?",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF1F5F9) else LightTextPrimary,
                                        fontSize = 17.sp,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Tap any action to start an instant conversational plan",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Centered Snapped Carousel with Fluid Card Deck Animation
                        item(key = "ai_help_carousel") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(324.dp),
                                    contentPadding = PaddingValues(horizontal = 34.dp),
                                    pageSpacing = 12.dp,
                                    beyondViewportPageCount = 1
                                ) { page ->
                                    val card = carouselCards[page]
                                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                    val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

                                    val scale = 1f - (absOffset * 0.08f)
                                    val alpha = 1f - (absOffset * 0.22f)
                                    val rotationZ = (pageOffset * 2.5f).coerceIn(-3f, 3f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                translationY = absOffset * 10.dp.toPx()
                                                this.alpha = alpha
                                                this.rotationZ = rotationZ
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        HelpCarouselDeckCard(
                                            card = card,
                                            pageIndex = page,
                                            totalPages = carouselCards.size,
                                            isFocused = page == pagerState.currentPage,
                                            onCardClick = {
                                                if (page != pagerState.currentPage) {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(page)
                                                    }
                                                } else {
                                                    if (card.workflowType != null) {
                                                        startConversationalWorkflow(card.workflowType)
                                                    } else {
                                                        submitPrompt(
                                                            customPrompt = card.defaultPrompt,
                                                            actionTag = card.title
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Carousel Interactive Controls (Prev arrow + Page dots + Next arrow)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (pagerState.currentPage > 0) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Previous slide",
                                            tint = if (pagerState.currentPage > 0) {
                                                if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                            } else {
                                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Indicator dots with tap-to-jump
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        carouselCards.indices.forEach { index ->
                                            val isSelected = pagerState.currentPage == index
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .height(6.dp)
                                                    .width(if (isSelected) 22.dp else 6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) {
                                                            Brush.horizontalGradient(
                                                                if (isDark) listOf(Color(0xFFC084FC), Color(0xFF8B5CF6))
                                                                else listOf(AiPrimary, Color(0xFF8B5CF6))
                                                            )
                                                        } else {
                                                            SolidColor(if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle)
                                                        }
                                                    )
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(index)
                                                        }
                                                    }
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            if (pagerState.currentPage < carouselCards.size - 1) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage < carouselCards.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Next slide",
                                            tint = if (pagerState.currentPage < carouselCards.size - 1) {
                                                if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                            } else {
                                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Actions Section (Distinct Instant Study Tools)
                        item(key = "quick_actions_section") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Quick study prompts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Instant 1-Tap AI",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(quickActions) { action ->
                                        QuickActionChip(
                                            item = action,
                                            onClick = {
                                                if (action.workflowType != null) {
                                                    startConversationalWorkflow(action.workflowType)
                                                } else {
                                                    submitPrompt(
                                                        customPrompt = action.prompt,
                                                        actionTag = action.title
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Messages (Conversational Stream)
                if (chatMessages.isNotEmpty()) {
                    items(chatMessages, key = { it.id }) { msg ->
                        val isLatestMessage = chatMessages.lastOrNull()?.id == msg.id
                        val canShowContextualEditorActions = isContextualMode && !msg.isUser && activeWorkflow == null && msg.artifact == null && msg.options.isEmpty()
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ChatMessageItem(
                                message = msg,
                                isLatest = isLatestMessage,
                                isGenerating = isGenerating,
                                selectedDiagram = selectedDiagram,
                                onUpdateDiagram = onUpdateDiagram,
                                onOptionSelected = { chosenOption ->
                                    handleOptionClick(chosenOption)
                                },
                                onCopyText = {
                                    clipboardManager.setText(AnnotatedString(msg.text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onReplace = if (canShowContextualEditorActions) {
                                    {
                                        when (activeScopeType) {
                                            TargetScopeType.SELECTION -> {
                                                onReplaceSelection(msg.text)
                                                Toast.makeText(context, "Replaced selection", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                            TargetScopeType.TOPIC -> {
                                                val tId = selectedTopicId ?: topics.firstOrNull()?.topicId
                                                if (tId != null) {
                                                    onUpdateTopic(tId, msg.text)
                                                    Toast.makeText(context, "Replaced topic content", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                } else {
                                                    onReplaceSelection(msg.text)
                                                    onDismiss()
                                                }
                                            }
                                            TargetScopeType.WHOLE_CHAPTER -> {
                                                onReplaceSelection(msg.text)
                                                Toast.makeText(context, "Replaced chapter content", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                            TargetScopeType.DIAGRAM -> {
                                                val parsed = parseNodesFromAiText(msg.text)
                                                if (selectedDiagram != null && parsed.isNotEmpty()) {
                                                    onUpdateDiagram(selectedDiagram.copy(nodes = parsed))
                                                    Toast.makeText(context, "Updated diagram", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    }
                                } else null,
                                onInsert = if (canShowContextualEditorActions) {
                                    {
                                        when (activeScopeType) {
                                            TargetScopeType.TOPIC -> {
                                                val tId = selectedTopicId ?: topics.firstOrNull()?.topicId
                                                if (tId != null) {
                                                    onInsertIntoTopic(tId, msg.text)
                                                    Toast.makeText(context, "Inserted into topic", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                } else {
                                                    onInsertBelow(msg.text)
                                                    onDismiss()
                                                }
                                            }
                                            else -> {
                                                onInsertBelow(msg.text)
                                                Toast.makeText(context, "Inserted below", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        }
                                    }
                                } else null,
                                onDuplicate = if (canShowContextualEditorActions) {
                                    {
                                        when (activeScopeType) {
                                            TargetScopeType.TOPIC -> {
                                                val tId = selectedTopicId ?: topics.firstOrNull()?.topicId
                                                if (tId != null) {
                                                    onDuplicateTopic(tId, msg.text)
                                                    Toast.makeText(context, "Created duplicate topic variant", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                } else {
                                                    onInsertBelow("\n\n---\n### AI Variant\n" + msg.text)
                                                    onDismiss()
                                                }
                                            }
                                            else -> {
                                                onInsertBelow("\n\n---\n### AI Variant\n" + msg.text)
                                                Toast.makeText(context, "Added as new variant", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }

                // Minimal 3-Dot Typing Indicator (● ● ●)
                if (isGenerating) {
                    item(key = "generating_typing_indicator") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            AiTypingIndicator()
                        }
                    }
                }
            }

            // 3. CHAT COMPOSER BAR (Source Collection Area)
            ChatComposerBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = { submitPrompt() },
                onTakePhoto = { launchCamera() },
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickDocument = {
                    filePickerLauncher.launch(
                        arrayOf("text/plain", "application/pdf", "application/msword", "*/*")
                    )
                },
                attachedMediaList = attachedMediaList,
                onRemoveMedia = { attachedMediaList.remove(it) },
                onClearAllMedia = { attachedMediaList.clear() },
                isGenerating = isGenerating,
                focusRequester = focusRequester,
                placeholderHint = customPlaceholderHint ?: "Ask question, paste source text, or attach files…"
            )

            // 4. PERSISTENT BOTTOM NAVIGATION (Hides smoothly during active AI conversation or contextual mode)
            AnimatedVisibility(
                visible = !isConversationActive && !isContextualMode,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it }
            ) {
                PrepOSBottomNavBar(
                    selectedTab = "ask_ai",
                    onSelectTab = { tab ->
                        if (tab != "ask_ai") {
                            onNavigateTab(tab)
                        }
                    },
                    onOpenAskAI = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                )
            }
        }
    }

    // MORE MENU
    if (showMoreMenu) {
        DropdownMenu(
            expanded = showMoreMenu,
            onDismissRequest = { showMoreMenu = false },
            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else LightSurface)
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Change preferred name", color = if (isDark) Color.White else LightTextPrimary)
                    }
                },
                onClick = {
                    showMoreMenu = false
                    showEditNameDialog = true
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Clear conversation", color = Color(0xFFF87171))
                    }
                },
                onClick = {
                    showMoreMenu = false
                    showClearConfirmDialog = true
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Copy entire chat", color = if (isDark) Color.White else LightTextPrimary)
                    }
                },
                onClick = {
                    showMoreMenu = false
                    val allText = chatMessages.joinToString("\n\n") { (if (it.isUser) "You: " else "PrepOS AI: ") + it.text }
                    if (allText.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(allText))
                        Toast.makeText(context, "Full chat copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // ONE-TIME NAME PROMPT
    if (showNamePopup && savedUserName.isBlank()) {
        var tempNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                hasPromptedNameGlobally = true
                showNamePopup = false
            },
            containerColor = if (isDark) Color(0xFF131D33) else LightSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Welcome to PrepOS AI",
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "What should PrepOS AI call you? Enter your name for personalized study plans.",
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = tempNameInput,
                        onValueChange = { tempNameInput = it },
                        singleLine = true,
                        placeholder = { Text("Enter your name (e.g. Alex)", color = if (isDark) Color(0xFF64748B) else LightTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                            focusedBorderColor = if (isDark) Color(0xFF8B5CF6) else AiPrimary,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle,
                            cursorColor = if (isDark) Color(0xFFC084FC) else AiPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempNameInput.isNotBlank()) {
                            viewModel?.updatePreferredUserName(tempNameInput.trim())
                            Toast.makeText(context, "Nice to meet you, ${tempNameInput.trim()}!", Toast.LENGTH_SHORT).show()
                        }
                        hasPromptedNameGlobally = true
                        showNamePopup = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF7C3AED) else AiPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hasPromptedNameGlobally = true
                        showNamePopup = false
                    }
                ) {
                    Text("Skip", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                }
            }
        )
    }

    // EDIT NAME DIALOG
    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(savedUserName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = if (isDark) Color(0xFF1E293B) else LightSurface,
            title = {
                Text("Your Preferred Name", color = if (isDark) Color.White else LightTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("How would you like PrepOS AI to address you?", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        placeholder = { Text("Enter your name", color = if (isDark) Color(0xFF64748B) else LightTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                            focusedBorderColor = if (isDark) Color(0xFF8B5CF6) else AiPrimary,
                            unfocusedBorderColor = if (isDark) Color(0xFF475569) else AiPrimaryBorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel?.updatePreferredUserName(tempName.trim())
                            showEditNameDialog = false
                            Toast.makeText(context, "Updated name to $tempName", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF7C3AED) else AiPrimary)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                }
            }
        )
    }

    // CLEAR CHAT DIALOG
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = if (isDark) Color(0xFF1E293B) else LightSurface,
            title = {
                Text("Clear Conversation?", color = if (isDark) Color.White else LightTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will clear all messages in this conversation session and return to the main dashboard.",
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        chatMessages.clear()
                        attachedMediaList.clear()
                        activeWorkflow = null
                        isGenerating = false
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Conversation cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                }
            }
        )
    }

    // CHAT HISTORY DIALOG
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            containerColor = if (isDark) Color(0xFF1E293B) else LightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Session Messages (${chatMessages.size})", color = if (isDark) Color.White else LightTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                if (chatMessages.isEmpty()) {
                    Text("No messages in current session yet.", color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatMessages) { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (msg.isUser) {
                                    if (isDark) Color(0xFF312E81) else AiPrimarySoft
                                } else {
                                    if (isDark) Color(0xFF0F172A) else LightSurfaceSecondary
                                },
                                border = BorderStroke(1.dp, if (isDark) Color.Transparent else AiPrimaryBorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (msg.isUser) "You" else "PrepOS AI",
                                        fontWeight = FontWeight.Bold,
                                        color = if (msg.isUser) (if (isDark) Color(0xFFC084FC) else AiPrimaryDark) else (if (isDark) Color(0xFF38BDF8) else SubjectBlue),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.text,
                                        color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                        fontSize = 12.5.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = if (isDark) Color(0xFF8B5CF6) else AiPrimaryDark)
                }
            }
        )
    }

    if (showInAppCameraScanner) {
        InAppCameraScannerDialog(
            onDismiss = { showInAppCameraScanner = false },
            onPagesCaptured = { processedPages ->
                processedPages.forEachIndexed { idx, pageResult ->
                    val timeTag = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "Scan_${timeTag}_p${idx + 1}.jpg"
                    val media = AttachedUiMedia(
                        name = fileName,
                        mimeType = "image/jpeg",
                        isImage = true,
                        uri = pageResult.uri,
                        bitmap = pageResult.bitmap,
                        sizeText = pageResult.sizeText,
                        base64Data = pageResult.base64Data
                    )
                    attachedMediaList.add(media)
                    if (activeWorkflow != null && activeWorkflow?.type == AssistantWorkflowType.CREATE_NOTES_FROM_SOURCE) {
                        activeWorkflow?.accumulatedAttachments?.add(media)
                    }
                }
                Toast.makeText(context, "📸 ${processedPages.size} page(s) added to composer", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// TOP HEADER BAR COMPONENT
// -----------------------------------------------------------------------------------------
@Composable
private fun TopHeaderBar(
    title: String,
    subtitle: String,
    isConversationActive: Boolean,
    isContextualMode: Boolean = false,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    onOpenHistory: () -> Unit,
    onOpenMore: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isConversationActive && !isContextualMode) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1E293B) else LightSurface,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                            tint = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (isDark) listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF3B82F6))
                            else listOf(AiPrimaryDark, Color(0xFF7B5CE7), AiPrimary)
                        )
                    )
                    .border(1.5.dp, if (isDark) Color(0xFFC084FC).copy(alpha = 0.6f) else AiPrimaryBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary,
                        fontSize = 15.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isContextualMode) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1E293B) else LightSurface,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                    modifier = Modifier.size(34.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close AI Sheet",
                            tint = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1E293B) else LightSurface,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                    modifier = Modifier.size(34.dp)
                ) {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Chat History",
                            tint = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1E293B) else LightSurface,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                    modifier = Modifier.size(34.dp)
                ) {
                    IconButton(
                        onClick = onOpenMore,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// CENTERED FLUID DECK HELP CAROUSEL CARD
// -----------------------------------------------------------------------------------------
@Composable
private fun HelpCarouselDeckCard(
    card: HelpCarouselCard,
    pageIndex: Int,
    totalPages: Int,
    isFocused: Boolean,
    onCardClick: () -> Unit
) {
    val isDark = isAppDarkTheme()
    val baseCardBg = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val accentColors = if (isDark) card.gradientColors else listOf(card.gradientColors.first(), card.gradientColors.last().copy(alpha = 0.9f))

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = baseCardBg,
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            brush = if (isFocused) {
                Brush.linearGradient(accentColors)
            } else {
                SolidColor(if (isDark) Color(0xFF22314E) else Color(0xFFE2E8F0))
            }
        ),
        shadowElevation = if (isFocused) (if (isDark) 10.dp else 4.dp) else (if (isDark) 2.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(315.dp)
            .clickable(onClick = onCardClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle ambient glow across top header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColors.first().copy(alpha = if (isDark) 0.12f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Glowing top accent indicator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(Brush.horizontalGradient(accentColors))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Header Row: Elevated Icon Squircle + Tag Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Elevated squircle icon container
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            shadowElevation = if (isDark) 6.dp else 3.dp,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(accentColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = card.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Tag Badge Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isFocused) accentColors.first().copy(alpha = 0.45f)
                                else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColors.first())
                                )
                                Text(
                                    text = card.tag,
                                    color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary,
                            fontSize = 17.5.sp,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Description
                    Text(
                        text = card.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 17.5.sp
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Feature highlights / Badges
                    if (card.badges.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            card.badges.take(2).forEach { badge ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF192237) else Color(0xFFF8FAFC),
                                    border = BorderStroke(0.8.dp, if (isDark) Color(0xFF2E3E60) else Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "•",
                                            color = accentColors.first(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = badge,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Row: Step count + Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page indicator pill: "1 of 8"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "${pageIndex + 1} of $totalPages",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Action pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E293B) else AiPrimaryLight,
                        border = BorderStroke(
                            1.dp,
                            if (isFocused) Brush.horizontalGradient(accentColors)
                            else SolidColor(if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle)
                        ),
                        modifier = Modifier.clickable(onClick = onCardClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = card.actionText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                    fontSize = 12.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// QUICK ACTION CHIP (Distinct Instant Study Tools)
// -----------------------------------------------------------------------------------------
@Composable
private fun QuickActionChip(
    item: QuickActionItem,
    onClick: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF151F33) else LightSurface,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2B3A5A) else AiPrimaryBorderSubtle),
        shadowElevation = if (isDark) 2.dp else 1.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF1E293B) else AiPrimaryLight
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFF1F5F9) else LightTextPrimary,
                    fontSize = 12.sp
                )
            )
        }
    }
}

private fun parseNodesFromAiText(text: String): List<String> {
    val lines = text.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            line.removePrefix("- ")
                .removePrefix("* ")
                .removePrefix("• ")
                .replace(Regex("^\\d+\\.\\s*"), "")
                .trim()
        }
        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("Note:") && !it.startsWith("Here") && it.length < 80 }
    return if (lines.isNotEmpty()) lines.take(8) else listOf(text.take(60))
}

// -----------------------------------------------------------------------------------------
// CHAT MESSAGE ITEM & EMBEDDED ANSWER OPTIONS
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageItem(
    message: AskAIChatMessage,
    isLatest: Boolean,
    isGenerating: Boolean,
    selectedDiagram: DocElement.DiagramBlock? = null,
    onUpdateDiagram: (DocElement.DiagramBlock) -> Unit = {},
    onOptionSelected: (String) -> Unit,
    onCopyText: () -> Unit,
    onReplace: (() -> Unit)? = null,
    onInsert: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    val context = LocalContext.current

    if (message.artifact != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            if (message.text.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isDark) listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                    else listOf(AiPrimaryDark, Color(0xFF7B5CE7))
                                )
                            )
                            .border(1.dp, if (isDark) Color(0xFFC084FC) else AiPrimaryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "PrepOS AI",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF131B2E) else LightSurface,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF23314E) else AiPrimaryBorderSubtle)
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDark) Color(0xFFF1F5F9) else LightTextPrimary,
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            AiResultArtifactContainer(
                artifact = message.artifact,
                isDark = isDark,
                onApprove = { onOptionSelected("Approve Plan ✓") },
                onMakeChanges = { onOptionSelected("Make Changes ✎") },
                onInsert = onInsert,
                onReplace = onReplace,
                onDuplicate = onDuplicate,
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isDark) listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                else listOf(AiPrimaryDark, Color(0xFF7B5CE7))
                            )
                        )
                        .border(1.dp, if (isDark) Color(0xFFC084FC) else AiPrimaryBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "PrepOS AI",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.widthIn(max = 320.dp)) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 18.dp
                    ),
                    color = if (message.isUser) {
                        if (isDark) Color(0xFF7C3AED) else AiPrimary
                    } else {
                        if (isDark) Color(0xFF131B2E) else LightSurface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (message.isUser) {
                            if (isDark) Color(0xFF8B5CF6) else AiPrimary
                        } else {
                            if (isDark) Color(0xFF23314E) else AiPrimaryBorderSubtle
                        }
                    ),
                    shadowElevation = if (isDark) 2.dp else 1.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        if (message.actionTag != null) {
                            Text(
                                text = "✦ ${message.actionTag}",
                                color = if (message.isUser) Color.White.copy(alpha = 0.85f) else (if (isDark) Color(0xFFC084FC) else AiPrimaryDark),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (message.isUser) {
                            Text(
                                text = buildMarkdownAnnotatedString(message.text, baseColor = Color.White),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            )
                        } else {
                            AiMarkdownMessage(
                                text = message.text,
                                isUser = false,
                                accentColor = if (isDark) Color(0xFFC084FC) else AiPrimaryDark
                            )
                        }

                        if (!message.isUser) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedDiagram != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF312E81) else AiPrimarySoft,
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF4F46E5) else AiPrimaryBorderSubtle),
                                        modifier = Modifier.clickable {
                                            val parsed = parseNodesFromAiText(message.text)
                                            if (parsed.isNotEmpty()) {
                                                onUpdateDiagram(selectedDiagram.copy(nodes = parsed))
                                                Toast.makeText(context, "Diagram updated with ${parsed.size} nodes!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Apply",
                                                color = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (onReplace != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                                        modifier = Modifier.clickable { onReplace() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Replace",
                                                tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Replace",
                                                color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (onInsert != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                                        modifier = Modifier.clickable { onInsert() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Insert",
                                                tint = if (isDark) Color(0xFF38BDF8) else SubjectBlue,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Insert",
                                                color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (onDuplicate != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle),
                                        modifier = Modifier.clickable { onDuplicate() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Duplicate",
                                                tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Duplicate",
                                                color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = onCopyText,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy text",
                                        tint = if (isDark) Color(0xFF64748B) else LightTextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline Clickable Options / Buttons (for current step or proposal)
                if (message.options.isNotEmpty() && isLatest && !isGenerating) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        message.options.forEach { option ->
                            val isApproval = option.contains("Approve", ignoreCase = true) || option.contains("✓")
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isApproval) {
                                    if (isDark) Color(0xFF7C3AED) else AiPrimary
                                } else {
                                    if (isDark) Color(0xFF1E293B) else LightSurface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isApproval) {
                                        if (isDark) Color(0xFFC084FC) else AiPrimaryDark
                                    } else {
                                        if (isDark) Color(0xFF334155) else AiPrimaryBorderSubtle
                                    }
                                ),
                                shadowElevation = if (isApproval) 3.dp else 0.5.dp,
                                modifier = Modifier.clickable { onOptionSelected(option) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isApproval) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isApproval) Color.White else (if (isDark) Color(0xFFE2E8F0) else LightTextPrimary),
                                            fontWeight = if (isApproval) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
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
}

// -----------------------------------------------------------------------------------------
// ENHANCED AI THINKING & 3-DOT TYPING INDICATOR (Thinking... -> ● ● ●)
// -----------------------------------------------------------------------------------------
@Composable
private fun AiTypingIndicator() {
    val isDark = isAppDarkTheme()
    var elapsedMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            delay(50)
            elapsedMillis = System.currentTimeMillis() - start
        }
    }

    // Thinking state shown for the first ~2.2 seconds, then smoothly transitions to the 3 blinking dots
    val isThinkingPhase = elapsedMillis < 2200L

    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, delayMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, delayMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(if (isThinkingPhase) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (isDark) listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                        else listOf(AiPrimaryDark, Color(0xFF7B5CE7))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isThinkingPhase) Icons.Default.Psychology else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF131B2E) else LightSurface,
            border = BorderStroke(
                1.dp,
                if (isThinkingPhase) {
                    Brush.horizontalGradient(
                        listOf(
                            (if (isDark) Color(0xFF8B5CF6) else AiPrimaryDark).copy(alpha = glowAlpha),
                            (if (isDark) Color(0xFF3B82F6) else Color(0xFF818CF8)).copy(alpha = 0.35f)
                        )
                    )
                } else {
                    SolidColor(if (isDark) Color(0xFF23314E) else AiPrimaryBorderSubtle)
                }
            ),
            shadowElevation = if (isThinkingPhase) 2.dp else 1.dp
        ) {
            AnimatedContent(
                targetState = isThinkingPhase,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "thinking_to_dots"
            ) { thinking ->
                if (thinking) {
                    // PHASE 1: Thinking display
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (isDark) Color(0xFFC084FC) else AiPrimaryDark).copy(alpha = glowAlpha)
                                )
                        )

                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                                fontSize = 12.5.sp
                            )
                        )

                        Text(
                            text = if (elapsedMillis < 1100L) "Analyzing requirements..." else "Structuring response...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                } else {
                    // PHASE 2: Classic 3-Dot animation (● ● ●)
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(lerp(0.8f, 1.25f, dotAlpha1))
                                .clip(CircleShape)
                                .background((if (isDark) Color(0xFFC084FC) else AiPrimaryDark).copy(alpha = dotAlpha1))
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(lerp(0.8f, 1.25f, dotAlpha2))
                                .clip(CircleShape)
                                .background((if (isDark) Color(0xFFC084FC) else AiPrimaryDark).copy(alpha = dotAlpha2))
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(lerp(0.8f, 1.25f, dotAlpha3))
                                .clip(CircleShape)
                                .background((if (isDark) Color(0xFFC084FC) else AiPrimaryDark).copy(alpha = dotAlpha3))
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// ATTACHED MEDIA PREVIEW (Professional Square Card Layout)
// -----------------------------------------------------------------------------------------
@Composable
private fun AttachedMediaPreviewItem(
    media: AttachedUiMedia,
    onRemove: () -> Unit
) {
    val isDark = isAppDarkTheme()
    Box(
        modifier = Modifier
            .size(68.dp)
            .padding(top = 4.dp, end = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.6f) else Color(0xFF93C5FD)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (media.bitmap != null) {
                Image(
                    bitmap = media.bitmap.asImageBitmap(),
                    contentDescription = media.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else AiPrimaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = media.name.takeLast(7),
                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Bottom file size badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = media.sizeText,
                color = Color.White,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Floating Delete (✕) Badge on Top-Right Corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.75f))
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove attachment",
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// CHAT COMPOSER BAR (Source Upload + Message + Attachment Manager)
// -----------------------------------------------------------------------------------------
@Composable
private fun ChatComposerBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onPickDocument: () -> Unit,
    attachedMediaList: List<AttachedUiMedia> = emptyList(),
    onRemoveMedia: (AttachedUiMedia) -> Unit = {},
    onClearAllMedia: () -> Unit = {},
    isGenerating: Boolean,
    focusRequester: FocusRequester? = null,
    placeholderHint: String = "Ask question, paste source text, or attach files…"
) {
    val isDark = isAppDarkTheme()
    val canSend = (inputText.isNotBlank() || attachedMediaList.isNotEmpty()) && !isGenerating

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) Color(0xFF080D1A) else LightBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF131D33) else LightSurface,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3756) else AiPrimaryBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pending Attachments Bar inside Composer
                if (attachedMediaList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF0B1120) else Color(0xFFF8FAFC))
                            .padding(top = 8.dp, bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📎 ${attachedMediaList.size} source item${if (attachedMediaList.size > 1) "s" else ""} ready to process",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFC084FC) else AiPrimary
                            )
                            Text(
                                text = "Clear all",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(onClick = onClearAllMedia)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            attachedMediaList.forEach { media ->
                                AttachedMediaPreviewItem(
                                    media = media,
                                    onRemove = { onRemoveMedia(media) }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Input & Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 5.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = onTakePhoto,
                        modifier = Modifier
                            .size(34.dp)
                            .padding(bottom = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take Photo with Camera",
                            tint = if (isDark) Color(0xFFC084FC) else AiPrimaryDark,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onPickImage,
                        modifier = Modifier
                            .size(34.dp)
                            .padding(bottom = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Attach Gallery Photos (Multi-select)",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onPickDocument,
                        modifier = Modifier
                            .size(34.dp)
                            .padding(bottom = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach Document / PDF",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(
                            color = if (isDark) Color.White else LightTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        minLines = 1,
                        maxLines = 4,
                        cursorBrush = SolidColor(if (isDark) Color(0xFFC084FC) else AiPrimary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (canSend) {
                                    onSend()
                                }
                            }
                        ),
                        modifier = Modifier
                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                            .weight(1f)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = placeholderHint,
                                        color = if (isDark) Color(0xFF64748B) else LightTextMuted,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (canSend) {
                            if (isDark) Color(0xFF7C3AED) else AiPrimary
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        },
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSend, onClick = onSend)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (canSend) {
                                        Brush.linearGradient(
                                            if (isDark) listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                            else listOf(AiPrimary, Color(0xFF8B5CF6))
                                        )
                                    } else {
                                        SolidColor(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send prompt",
                                tint = if (canSend) Color.White else (if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (isDark) Color(0xFF64748B) else LightTextMuted,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Conversational Study Tutor • All answers saved locally",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF64748B) else LightTextMuted,
                    fontSize = 10.sp
                )
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// HELPER: Query File Name from Uri
// -----------------------------------------------------------------------------------------
private fun queryFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
