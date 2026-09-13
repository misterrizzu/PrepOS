package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.PrepOSDatabase
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.NoteDocumentEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.UserPreferencesEntity
import com.example.data.repository.PrepOSRepository
import com.example.model.DocElement
import com.example.model.ElementType
import com.example.model.NoteFont
import com.example.model.PaperStyle
import com.example.model.PaperTheme
import com.example.model.PrepDocument
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class ActiveFormattingState(
    val isBold: Boolean = false,
    val isBoldMixed: Boolean = false,
    val isItalic: Boolean = false,
    val isItalicMixed: Boolean = false,
    val isUnderline: Boolean = false,
    val isUnderlineMixed: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isStrikethroughMixed: Boolean = false,
    val activeHeading: ElementType = ElementType.PARAGRAPH,
    val activeTextColorHex: String? = null,
    val activeHighlightColorHex: String? = null,
    val alignment: String = "LEFT"
)

data class ActiveEditorState(
    val chapterId: String = "",
    val chapterTitle: String = "",
    val document: PrepDocument = PrepDocument.createInitialDocument("", "Untitled"),
    val paperStyle: PaperStyle = PaperStyle.RULED,
    val paperTheme: PaperTheme = PaperTheme.PAPER_LIGHT,
    val fontFamily: NoteFont = NoteFont.SANS_SERIF,
    val fontSizeSp: Float = 16f,
    val lineSpacingMultiplier: Float = 1.4f,
    val readingProgress: Float = 0.0f,
    val lastReadScrollY: Int = 0,
    val isEditMode: Boolean = false,
    val activeSelectedText: String = "",
    val activeSelectedBlockId: String? = null,
    val activeSelectionStart: Int = 0,
    val activeSelectionEnd: Int = 0,
    val selectedBlockIds: Set<String> = emptySet(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val formattingState: ActiveFormattingState = ActiveFormattingState()
)

class PrepOSViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PrepOSRepository

    companion object {
        @Volatile
        var currentInstance: PrepOSViewModel? = null
    }

    init {
        currentInstance = this
        val database = PrepOSDatabase.getDatabase(application, viewModelScope)
        repository = PrepOSRepository(database)
        com.example.util.PrepOSFocusNotificationManager.createNotificationChannel(application)
        viewModelScope.launch(Dispatchers.IO) {
            // Guarantee JKSSB content and syllabus updates are synced safely
            com.example.data.db.SeedDataProvider.syncOrUpdateSyllabus(database, application)
            // Schedule all background exact alarms
            val tasks = repository.getStudyTasksSync()
            com.example.util.PrepOSAlarmScheduler.scheduleAllReminders(application, tasks)
        }
        checkAndGenerateDailyReminders()
        checkAndRecoverActiveFocusSession()
        checkDailyStreakOnAppLaunch()
        syncDailyStudyLogs()
        syncUnifiedProgression()
    }

    private fun syncUnifiedProgression() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalXp = repository.getTotalXpSync()
                if (totalXp == 0L) {
                    val prefs = repository.getPreferencesSync()
                    val tests: List<com.example.data.entity.TestAttemptEntity> = repository.allTestAttempts.firstOrNull() ?: emptyList()
                    val chapters: List<ChapterEntity> = repository.getAllChaptersSync()
                    val completedChapters = chapters.filter { it.readingProgress >= 0.98f }
                    val totalHistoricalStudyMinutes = repository.getAllDailyStudyLogsSync().sumOf { it.totalFocusedMinutes }
                    val seedEvents = com.example.util.ProgressionEngine.generateLegacySeedEvents(
                        testAttempts = tests,
                        completedChapters = completedChapters,
                        preferences = prefs,
                        totalHistoricalStudyMinutes = maxOf(totalHistoricalStudyMinutes, prefs.todayFocusedMinutes + prefs.yesterdayFocusedMinutes)
                    )
                    if (seedEvents.isNotEmpty()) {
                        repository.recordXpEvents(seedEvents)
                    }
                }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    private fun syncDailyStudyLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = repository.getPreferencesSync()
                val todayKey = getTodayDateKey()
                val yesterdayKey = getYesterdayDateKey()

                if (prefs.todayFocusedMinutes > 0 && prefs.lastStudyDate.isNotBlank()) {
                    repository.recordDailyStudyMinutes(
                        dateKey = prefs.lastStudyDate,
                        minutes = prefs.todayFocusedMinutes,
                        dailyTargetMinutes = prefs.dailyTargetMinutes,
                        isTargetMet = prefs.lastDailyTargetDate == prefs.lastStudyDate
                    )
                }

                if (prefs.yesterdayFocusedMinutes > 0) {
                    repository.recordDailyStudyMinutes(
                        dateKey = yesterdayKey,
                        minutes = prefs.yesterdayFocusedMinutes,
                        dailyTargetMinutes = prefs.dailyTargetMinutes,
                        isTargetMet = prefs.yesterdayFocusedMinutes >= prefs.dailyTargetMinutes
                    )
                }

                val allTasks = repository.getStudyTasksSync()
                val allCompletions = repository.getTaskCompletionsForDate(todayKey)
                val todayTaskMins = allTasks.filter { t -> allCompletions.any { it.taskId == t.id && it.isCompleted } }.sumOf { it.durationMinutes }
                if (todayTaskMins > 0) {
                    repository.recordDailyStudyMinutes(
                        dateKey = todayKey,
                        minutes = todayTaskMins,
                        dailyTargetMinutes = prefs.dailyTargetMinutes
                    )
                }
            } catch (e: Exception) {
                // Non-blocking
            }
        }
    }

    private fun checkDailyStreakOnAppLaunch() {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = repository.getPreferencesSync()
                val todayKey = getTodayDateKey()
                val lastActive = prefs.lastActiveDate
                val lastStudyDate = prefs.lastStudyDate
                val daysDiff = getDaysBetween(lastActive, todayKey)

                if (lastStudyDate == todayKey && prefs.todayFocusedMinutes > 0 && lastActive != todayKey) {
                    val currentStreak = prefs.currentStreak
                    val newStreak = when {
                        lastActive.isBlank() -> maxOf(currentStreak, 1)
                        daysDiff in 1..2 -> currentStreak + 1
                        else -> 1
                    }
                    repository.updateStreak(
                        currentStreak = newStreak,
                        bestStreak = maxOf(prefs.bestStreak, newStreak),
                        lastActiveDate = todayKey
                    )
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    // --- State Streams ---
    val exams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChapters: StateFlow<List<ChapterEntity>> = repository.allRecentChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chapters: StateFlow<List<ChapterEntity>> = repository.allRecentChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<UserPreferencesEntity?> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val smartNotifications: StateFlow<List<com.example.data.entity.SmartNotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allTestAttempts: StateFlow<List<com.example.data.entity.TestAttemptEntity>> = repository.allTestAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val testAttempts: StateFlow<List<com.example.data.entity.TestAttemptEntity>> = repository.allTestAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTestAttempts: StateFlow<List<com.example.data.entity.TestAttemptEntity>> = repository.getRecentAttempts(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val testCount: StateFlow<Int> = repository.testCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val avgScore: StateFlow<Float?> = repository.avgScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bestScore: StateFlow<Int?> = repository.bestScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val avgAccuracy: StateFlow<Float?> = repository.avgAccuracy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Master Unified Progression State ---
    val totalVerifiedXp: StateFlow<Long> = repository.totalVerifiedXp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val recentXpEvents: StateFlow<List<com.example.data.entity.ProgressXpEntity>> = repository.recentXpEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressionOverview: StateFlow<com.example.model.ProgressionOverview> = combine(
        repository.totalVerifiedXp,
        repository.recentXpEvents,
        repository.preferences,
        repository.allTestAttempts,
        repository.allChapters
    ) { totalXp: Long, recentEvents: List<com.example.data.entity.ProgressXpEntity>, prefs: UserPreferencesEntity?, testAttempts: List<com.example.data.entity.TestAttemptEntity>, chapters: List<ChapterEntity> ->
        val safeTotalXp = totalXp.coerceAtLeast(0L)
        val rankProg = com.example.model.calculateRankProgress(safeTotalXp)
        val streak = prefs?.currentStreak ?: 0
        val longestStreak = prefs?.bestStreak ?: 0
        val completedChaptersCount = chapters.count { it.readingProgress >= 0.98f }
        val totalStudyMinutes = prefs?.todayFocusedMinutes ?: 0

        val unlockedIds = recentEvents
            .filter { it.type == com.example.model.ProgressEventType.ACHIEVEMENT_UNLOCKED.name }
            .mapNotNull { it.sourceId }
            .toSet()

        val (achievements, newlyUnlockedEvents) = com.example.util.ProgressionEngine.evaluateAchievements(
            totalStudyMinutes = totalStudyMinutes,
            testAttempts = testAttempts,
            completedChaptersCount = completedChaptersCount,
            streakCount = streak,
            totalPracticeAttempted = testAttempts.sumOf { it.correctAnswers + it.wrongAnswers },
            totalXp = safeTotalXp,
            existingUnlockedIds = unlockedIds
        )

        // Automatically persist any newly unlocked achievement XP events into ledger
        if (newlyUnlockedEvents.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.recordXpEvents(newlyUnlockedEvents)
            }
        }

        val unlockedCount = achievements.count { it.isUnlocked }

        com.example.model.ProgressionOverview(
            totalXp = safeTotalXp,
            rankProgress = rankProg,
            streakCount = streak,
            longestStreak = longestStreak,
            unlockedAchievementsCount = unlockedCount,
            totalAchievementsCount = com.example.util.ProgressionEngine.ALL_ACHIEVEMENTS.size,
            todayTotalXp = safeTotalXp,
            todayStudyMinutesXp = (prefs?.todayFocusedMinutes ?: 0).coerceAtMost(300).toLong(),
            maxDailyStudyXp = com.example.util.ProgressionEngine.MAX_DAILY_STUDY_XP,
            recentEvents = recentEvents,
            achievements = achievements
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.model.ProgressionOverview()
    )

    fun recordPracticeAnswer(sessionId: String, questionId: String, isCorrect: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val events = com.example.util.ProgressionEngine.createPracticeAnswerEvents(sessionId, questionId, isCorrect)
            repository.recordXpEvents(events)
        }
    }

    fun recordManualXpEvent(event: com.example.data.entity.ProgressXpEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordXpEvent(event)
        }
    }

    // Current selected tab on Home Screen ("home" or "study")
    private val _currentHomeTab = MutableStateFlow("home")
    val currentHomeTab: StateFlow<String> = _currentHomeTab.asStateFlow()

    fun setCurrentHomeTab(tab: String) {
        _currentHomeTab.value = if (tab == "study") "study" else "home"
    }

    val activeTestSession: StateFlow<com.example.data.entity.ActiveTestSessionEntity?> = repository.activeTestSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filter & Search state for Home Screen
    private val _selectedExamIdFilter = MutableStateFlow<String?>(null)
    val selectedExamIdFilter: StateFlow<String?> = _selectedExamIdFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Editor State
    private val _editorState = MutableStateFlow(ActiveEditorState())
    val editorState: StateFlow<ActiveEditorState> = _editorState.asStateFlow()

    // Undo / Redo history stacks (isolated to current chapter)
    private val undoStack = mutableListOf<String>() // JSON snapshots
    private val redoStack = mutableListOf<String>()
    private var autosaveJob: Job? = null

    fun selectExamFilter(examId: String?) {
        _selectedExamIdFilter.value = examId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Exam CRUD ---
    fun createExam(name: String, code: String, colorHex: String) {
        viewModelScope.launch {
            repository.createExam(name, code, colorHex)
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            repository.deleteExam(examId)
        }
    }

    // --- Subject CRUD ---
    fun createSubject(name: String, examId: String?, iconName: String, colorHex: String) {
        viewModelScope.launch {
            repository.createSubject(name, examId, iconName, colorHex)
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    // --- Chapter CRUD ---
    fun createChapter(subjectId: String, title: String, summary: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val chapter = repository.createChapter(subjectId, title, summary)
            onCreated(chapter.id)
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            repository.deleteChapter(chapterId)
        }
    }

    fun updateChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            repository.updateChapter(chapter)
        }
    }

    fun recordTestResult(chapterId: String, attempted: Int, correct: Int) {
        viewModelScope.launch {
            repository.updateTestScore(chapterId, attempted, correct)
        }
    }

    fun updateChapterQuestions(chapterId: String, questions: List<com.example.model.QuestionItem>) {
        viewModelScope.launch {
            repository.updateChapterQuestions(chapterId, questions)
        }
    }

    fun parseQuestionsFromRawText(
        rawText: String,
        useAI: Boolean = true,
        onStart: () -> Unit = {},
        onComplete: (List<com.example.model.QuestionItem>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                onStart()
                val parsed = if (useAI) {
                    val prefs = repository.getPreferencesSync()
                    val apiKey = prefs.apiKey
                    val model = prefs.selectedAiModel
                    com.example.ai.StudyAIService.parseQuestionsFromText(
                        rawText = rawText,
                        apiKey = apiKey,
                        model = model
                    )
                } else {
                    com.example.ai.OfflineQuestionParser.parseLocally(rawText)
                }
                onComplete(parsed)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Failed to parse questions")
            }
        }
    }

    // --- Settings & Preferences Updates ---
    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            repository.updateApiKey(apiKey)
        }
    }

    fun saveAiModel(model: String) {
        viewModelScope.launch {
            repository.updateAiModel(model)
        }
    }

    fun saveDefaultExamId(examId: String?) {
        viewModelScope.launch {
            repository.updateDefaultExamId(examId)
            _selectedExamIdFilter.value = examId
        }
    }

    fun setShowExamLabels(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowExamLabels(show)
        }
    }

    fun setShowExamFilters(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowExamFilters(show)
        }
    }

    /**
     * AI Sanitization & Chapter Structuring workflow.
     * Takes raw text (from paste, txt file, or pdf file), runs the 3-step pipeline,
     * structures H1-H4, standardizes revision section, and creates chapter + document.
     */
    fun sanitizeAndCreateChapter(
        subjectId: String,
        title: String,
        chapterNumber: Int,
        rawContent: String,
        useAI: Boolean = true,
        onStart: () -> Unit = {},
        onComplete: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                onStart()
                val prefs = repository.getPreferencesSync()
                val apiKey = if (useAI) prefs.apiKey else ""
                val model = prefs.selectedAiModel

                val result = com.example.ai.StudyAIService.sanitizeAndStructureChapter(
                    rawContent = rawContent,
                    chapterTitle = title,
                    chapterNumber = chapterNumber,
                    apiKey = apiKey,
                    model = model,
                    useAI = useAI
                )

                val created = repository.createChapterWithSanitizedContent(
                    subjectId = subjectId,
                    title = result.title.ifBlank { title.ifBlank { "Untitled Chapter" } },
                    summary = result.summary,
                    chapterNumber = chapterNumber,
                    elements = result.elements,
                    questions = result.questions
                )
                onComplete(created.id)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Failed to sanitize and create chapter.")
            }
        }
    }

    fun getChaptersForSubject(subjectId: String) = repository.getChaptersForSubject(subjectId)

    fun observeChapter(chapterId: String) = repository.observeChapter(chapterId)

    fun addQuestionToChapter(chapterId: String, question: com.example.model.QuestionItem) {
        viewModelScope.launch {
            val chapter = repository.getChapter(chapterId) ?: return@launch
            val existing = parseQuestionsFromJson(chapter.questionsJson).toMutableList()
            existing.add(question)
            repository.updateChapterQuestions(chapterId, existing)
        }
    }

    fun addQuestionsBatchToChapter(chapterId: String, newQuestions: List<com.example.model.QuestionItem>, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            val chapter = repository.getChapter(chapterId) ?: return@launch
            val finalQuestions = if (replaceExisting) {
                newQuestions
            } else {
                val existing = parseQuestionsFromJson(chapter.questionsJson).toMutableList()
                existing.addAll(newQuestions)
                existing
            }
            repository.updateChapterQuestions(chapterId, finalQuestions)
        }
    }

    fun deleteQuestionFromChapter(chapterId: String, questionId: String) {
        viewModelScope.launch {
            val chapter = repository.getChapter(chapterId) ?: return@launch
            val existing = parseQuestionsFromJson(chapter.questionsJson).filter { it.id != questionId }
            repository.updateChapterQuestions(chapterId, existing)
        }
    }

    fun deleteAllQuestionsForChapter(chapterId: String) {
        viewModelScope.launch {
            repository.updateChapterQuestions(chapterId, emptyList())
        }
    }

    /**
     * Generates a 10 or 20 question AI test on a chosen topic or chapter,
     * and optionally saves the questions directly to a chapter or creates a new chapter.
     */
    fun generateAiTest(
        topicOrTitle: String,
        questionCount: Int, // 10 or 20
        targetChapterId: String? = null,
        createAsNewChapter: Boolean = false,
        subjectIdForNewChapter: String? = null,
        onProgress: (String) -> Unit = {},
        onSuccess: (List<com.example.model.QuestionItem>, savedChapterTitle: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onProgress("Connecting to AI Question Engine...")
                val prefs = repository.getPreferencesSync()
                val apiKey = prefs.apiKey
                val model = prefs.selectedAiModel
                val provider = prefs.selectedAiProvider
                val deepSeekKey = prefs.deepSeekApiKey

                var chapterContextText = ""
                var chapterTitle = ""
                if (!targetChapterId.isNullOrBlank()) {
                    val chap = repository.getChapter(targetChapterId)
                    if (chap != null) {
                        chapterTitle = chap.title
                        val doc = repository.getDocument(targetChapterId)
                        if (doc != null && doc.contentJson.isNotBlank()) {
                            try {
                                val prepDoc = com.example.model.PrepDocument.fromJson(doc.contentJson, chap.id, chap.title)
                                chapterContextText = prepDoc.toPlainText().take(3000)
                            } catch (e: Exception) {
                                chapterContextText = chap.summary
                            }
                        } else {
                            chapterContextText = chap.summary
                        }
                    }
                }

                onProgress("Crafting $questionCount high-yield questions for $topicOrTitle...")
                val examName = prefs.targetExamName

                val result = com.example.ai.StudyAIService.generateAiTestQuestions(
                    topicOrTitle = topicOrTitle,
                    questionCount = questionCount,
                    chapterContextText = chapterContextText,
                    examContext = examName,
                    apiKey = apiKey,
                    model = model,
                    provider = provider,
                    deepSeekApiKey = deepSeekKey
                )

                result.fold(
                    onSuccess = { questions ->
                        if (questions.isEmpty()) {
                            onError("No questions could be generated. Please try again.")
                            return@fold
                        }

                        var savedTitle: String? = null
                        if (!targetChapterId.isNullOrBlank()) {
                            addQuestionsBatchToChapter(targetChapterId, questions, replaceExisting = false)
                            val chap = repository.getChapter(targetChapterId)
                            savedTitle = chap?.title ?: chapterTitle.ifBlank { "Chapter" }
                        } else if (createAsNewChapter) {
                            val subId = subjectIdForNewChapter ?: subjects.value.firstOrNull()?.id
                            if (subId != null) {
                                val newTitle = "$topicOrTitle (AI Test)"
                                val createdChapter = repository.createChapter(
                                    subjectId = subId,
                                    title = newTitle,
                                    summary = "AI-generated practice test on $topicOrTitle ($questionCount Questions)"
                                )
                                repository.updateChapterQuestions(createdChapter.id, questions)
                                savedTitle = newTitle
                            }
                        }

                        onSuccess(questions, savedTitle)
                    },
                    onFailure = { ex ->
                        onError(ex.message ?: "Failed to generate AI test questions.")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun parseQuestionsFromJson(jsonStr: String): List<com.example.model.QuestionItem> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<com.example.model.QuestionItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val opts = mutableListOf<String>()
                val optsArray = obj.optJSONArray("options")
                if (optsArray != null) {
                    for (j in 0 until optsArray.length()) {
                        opts.add(optsArray.getString(j))
                    }
                }
                list.add(
                    com.example.model.QuestionItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        questionText = obj.optString("questionText", obj.optString("question", "")),
                        options = opts,
                        correctOptionIndex = obj.optInt("correctOptionIndex", 0),
                        explanation = obj.optString("explanation", ""),
                        difficulty = obj.optString("difficulty", "MEDIUM")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Active Document Management ---
    fun openChapter(chapterId: String) {
        viewModelScope.launch {
            val chapter = repository.getChapter(chapterId)
            val docEntity = repository.getDocument(chapterId)
            val globalPrefs = repository.getPreferencesSync()

            val title = chapter?.title ?: "Study Notes"
            val rawDoc = if (docEntity != null && docEntity.contentJson.isNotBlank()) {
                PrepDocument.fromJson(docEntity.contentJson, chapterId, title)
            } else {
                PrepDocument.createInitialDocument(chapterId, title)
            }
            val doc = rawDoc.normalizeIfContainsRawMarkdown()
            if (docEntity != null && doc.elements.size != rawDoc.elements.size) {
                try {
                    repository.saveDocument(
                        docEntity.copy(
                            contentJson = doc.toJson(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    // ignore safe background persistence error
                }
            }

            val pStyle = try {
                PaperStyle.valueOf(docEntity?.paperStyle ?: globalPrefs.defaultPaperStyle)
            } catch (e: Exception) {
                PaperStyle.RULED
            }

            val pTheme = try {
                PaperTheme.valueOf(docEntity?.themeMode ?: globalPrefs.defaultThemeMode)
            } catch (e: Exception) {
                PaperTheme.PAPER_LIGHT
            }

            val pFont = try {
                NoteFont.valueOf(docEntity?.fontFamily ?: globalPrefs.defaultFontFamily)
            } catch (e: Exception) {
                NoteFont.SANS_SERIF
            }

            undoStack.clear()
            redoStack.clear()
            undoStack.add(doc.toJson())

            _editorState.value = ActiveEditorState(
                chapterId = chapterId,
                chapterTitle = title,
                document = doc,
                paperStyle = pStyle,
                paperTheme = pTheme,
                fontFamily = pFont,
                fontSizeSp = docEntity?.fontSizeSp ?: globalPrefs.defaultFontSizeSp,
                lineSpacingMultiplier = docEntity?.lineSpacingMultiplier ?: globalPrefs.defaultLineSpacing,
                readingProgress = chapter?.readingProgress ?: 0.0f,
                lastReadScrollY = chapter?.lastReadScrollY ?: 0,
                isEditMode = false,
                canUndo = false,
                canRedo = false
            )
        }
    }

    fun toggleEditMode(enable: Boolean? = null) {
        val next = enable ?: !_editorState.value.isEditMode
        _editorState.value = _editorState.value.copy(isEditMode = next)
    }

    fun updateSelection(
        selectedText: String,
        blockId: String?,
        startOffset: Int = 0,
        endOffset: Int = 0,
        multiBlockIds: Set<String> = emptySet()
    ) {
        val currentDoc = _editorState.value.document
        val formatting = computeFormattingState(currentDoc, blockId, startOffset, endOffset)
        _editorState.value = _editorState.value.copy(
            activeSelectedText = selectedText,
            activeSelectedBlockId = blockId,
            activeSelectionStart = startOffset,
            activeSelectionEnd = endOffset,
            selectedBlockIds = if (multiBlockIds.isNotEmpty()) multiBlockIds else (blockId?.let { setOf(it) } ?: emptySet()),
            formattingState = formatting
        )
    }

    fun onDocumentUpdated(updatedDoc: PrepDocument, pushUndo: Boolean = true) {
        val currentJson = _editorState.value.document.toJson()
        if (pushUndo) {
            undoStack.add(currentJson)
            if (undoStack.size > 30) undoStack.removeAt(0)
            redoStack.clear()
        }

        val formatting = computeFormattingState(
            updatedDoc,
            _editorState.value.activeSelectedBlockId,
            _editorState.value.activeSelectionStart,
            _editorState.value.activeSelectionEnd
        )

        _editorState.value = _editorState.value.copy(
            document = updatedDoc,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            formattingState = formatting
        )

        scheduleDebouncedAutosave()
    }

    private fun computeFormattingState(
        doc: PrepDocument,
        blockId: String?,
        startOffset: Int,
        endOffset: Int
    ): ActiveFormattingState {
        val block = doc.elements.filterIsInstance<DocElement.TextBlock>()
            .find { it.id == blockId }
            ?: doc.elements.filterIsInstance<DocElement.TextBlock>().lastOrNull()
            ?: return ActiveFormattingState()

        val (boldActive, boldMixed) = block.queryRangeFormat(startOffset, endOffset, "BOLD")
        val (italicActive, italicMixed) = block.queryRangeFormat(startOffset, endOffset, "ITALIC")
        val (underlineActive, underlineMixed) = block.queryRangeFormat(startOffset, endOffset, "UNDERLINE")
        val (strikeActive, strikeMixed) = block.queryRangeFormat(startOffset, endOffset, "STRIKETHROUGH")

        return ActiveFormattingState(
            isBold = boldActive,
            isBoldMixed = boldMixed,
            isItalic = italicActive,
            isItalicMixed = italicMixed,
            isUnderline = underlineActive,
            isUnderlineMixed = underlineMixed,
            isStrikethrough = strikeActive,
            isStrikethroughMixed = strikeMixed,
            activeHeading = block.blockType,
            activeTextColorHex = block.textColorHex,
            activeHighlightColorHex = block.highlightColorHex,
            alignment = block.alignment
        )
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previousJson = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_editorState.value.document.toJson())
            val restored = PrepDocument.fromJson(previousJson, _editorState.value.chapterId, _editorState.value.chapterTitle)
            _editorState.value = _editorState.value.copy(
                document = restored,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
            scheduleDebouncedAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextJson = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_editorState.value.document.toJson())
            val restored = PrepDocument.fromJson(nextJson, _editorState.value.chapterId, _editorState.value.chapterTitle)
            _editorState.value = _editorState.value.copy(
                document = restored,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
            scheduleDebouncedAutosave()
        }
    }

    fun updateReadingScroll(progress: Float, scrollY: Int) {
        val state = _editorState.value
        if (state.chapterId.isBlank()) return

        // If progress is >= 0.96f, snap directly to 100% (1.0f)
        val normalizedProgress = if (progress >= 0.96f) 1.0f else progress.coerceIn(0f, 1f)

        _editorState.value = state.copy(readingProgress = normalizedProgress, lastReadScrollY = scrollY)
        viewModelScope.launch {
            repository.updateReadingProgress(state.chapterId, normalizedProgress, scrollY)
        }
    }

    fun markChapterCompleted(chapterId: String, isCompleted: Boolean) {
        val progress = if (isCompleted) 1.0f else 0.0f
        val scrollY = 0
        if (_editorState.value.chapterId == chapterId) {
            _editorState.value = _editorState.value.copy(readingProgress = progress, lastReadScrollY = scrollY)
        }
        viewModelScope.launch {
            repository.updateReadingProgress(chapterId, progress, scrollY)
        }
    }

    // --- Preferences Modifications ---
    fun setPaperStyle(style: PaperStyle) {
        _editorState.value = _editorState.value.copy(paperStyle = style)
        scheduleDebouncedAutosave()
    }

    fun setDefaultPaperStyle(style: PaperStyle) {
        _editorState.value = _editorState.value.copy(paperStyle = style)
        scheduleDebouncedAutosave()
        viewModelScope.launch {
            repository.updateDefaultPaperStyle(style.name)
        }
    }

    fun setPaperTheme(theme: PaperTheme) {
        _editorState.value = _editorState.value.copy(paperTheme = theme)
        scheduleDebouncedAutosave()
    }

    fun setDefaultPaperTheme(theme: PaperTheme) {
        _editorState.value = _editorState.value.copy(paperTheme = theme)
        scheduleDebouncedAutosave()
        viewModelScope.launch {
            repository.updateDefaultThemeMode(theme.name)
        }
    }

    fun setFont(font: NoteFont) {
        _editorState.value = _editorState.value.copy(fontFamily = font)
        scheduleDebouncedAutosave()
    }

    fun setDefaultFont(font: NoteFont) {
        _editorState.value = _editorState.value.copy(fontFamily = font)
        scheduleDebouncedAutosave()
        viewModelScope.launch {
            repository.updateDefaultFontFamily(font.name)
        }
    }

    fun setFontSize(fontSizeSp: Float) {
        _editorState.value = _editorState.value.copy(fontSizeSp = fontSizeSp)
        scheduleDebouncedAutosave()
    }

    fun setDefaultFontSize(fontSizeSp: Float) {
        _editorState.value = _editorState.value.copy(fontSizeSp = fontSizeSp)
        scheduleDebouncedAutosave()
        viewModelScope.launch {
            repository.updateDefaultFontSize(fontSizeSp)
        }
    }

    fun setLineSpacing(lineSpacing: Float) {
        _editorState.value = _editorState.value.copy(lineSpacingMultiplier = lineSpacing)
        scheduleDebouncedAutosave()
    }

    fun setDefaultLineSpacing(lineSpacing: Float) {
        _editorState.value = _editorState.value.copy(lineSpacingMultiplier = lineSpacing)
        scheduleDebouncedAutosave()
        viewModelScope.launch {
            repository.updateDefaultLineSpacing(lineSpacing)
        }
    }

    fun saveAllCurrentSettingsAsDefault() {
        val state = _editorState.value
        viewModelScope.launch {
            repository.updateAllReadingDefaults(
                style = state.paperStyle.name,
                theme = state.paperTheme.name,
                font = state.fontFamily.name,
                size = state.fontSizeSp,
                lineSpacing = state.lineSpacingMultiplier
            )
        }
    }

    fun updatePreferredUserName(name: String) {
        viewModelScope.launch {
            repository.updatePreferredUserName(name)
        }
    }

    fun updateTargetExamProfile(
        name: String,
        targetExam: String,
        targetDate: String,
        dailyHours: Float,
        scoreGoal: Int
    ) {
        viewModelScope.launch {
            repository.updateTargetExamProfile(name, targetExam, targetDate, dailyHours, scoreGoal)
        }
    }

    // --- Test Hub & Attempt Operations ---
    fun finalizeTestAttempt(
        attempt: com.example.data.entity.TestAttemptEntity,
        chapterId: String? = null,
        attemptedCount: Int = 0,
        correctCount: Int = 0
    ) {
        viewModelScope.launch {
            repository.finalizeTestAttempt(attempt, chapterId, attemptedCount, correctCount)
            addNotification(
                title = "📝 Test Completed: ${attempt.testTitle}",
                message = "Score: ${attempt.correctAnswers}/${attempt.totalQuestions} (${attempt.scorePercentage}% score). Tap to review weak areas and test analysis!",
                type = "TEST_RESULT",
                actionType = "TAKE_TEST"
            )
        }
    }

    fun saveTestAttempt(attempt: com.example.data.entity.TestAttemptEntity) {
        viewModelScope.launch {
            repository.saveTestAttempt(attempt)
            // Clear active test session once saved
            repository.clearActiveTestSession()
        }
    }

    fun deleteTestAttempt(id: String) {
        viewModelScope.launch {
            repository.deleteTestAttempt(id)
        }
    }

    fun clearAllTestAttempts() {
        viewModelScope.launch {
            repository.clearAllTestAttempts()
        }
    }

    fun saveActiveTestSession(session: com.example.data.entity.ActiveTestSessionEntity) {
        viewModelScope.launch {
            repository.saveActiveTestSession(session)
        }
    }

    fun clearActiveTestSession() {
        viewModelScope.launch {
            repository.clearActiveTestSession()
        }
    }

    suspend fun getTestAttemptById(id: String): com.example.data.entity.TestAttemptEntity? {
        return repository.getTestAttemptById(id)
    }

    // --- Rich Element Insertion ---
    fun insertDocElement(element: DocElement) {
        val currentElements = _editorState.value.document.elements.toMutableList()
        val blockId = _editorState.value.activeSelectedBlockId
        val targetIndex = if (blockId != null) {
            val idx = currentElements.indexOfFirst { it.id == blockId }
            if (idx >= 0) idx + 1 else currentElements.size
        } else {
            currentElements.size
        }
        currentElements.add(targetIndex, element)
        // Add a follow-up paragraph after elements for easy continued typing
        if (element !is DocElement.TextBlock) {
            currentElements.add(targetIndex + 1, DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = ""))
        }
        onDocumentUpdated(_editorState.value.document.copy(elements = currentElements))
    }

    // --- Exact Formatting Actions on Active/Selected Block (and multi-block ranges) ---
    fun applyHeadingStyle(newType: ElementType) {
        val selectedIds = _editorState.value.selectedBlockIds
        val blockId = _editorState.value.activeSelectedBlockId
        val start = _editorState.value.activeSelectionStart
        val end = _editorState.value.activeSelectionEnd
        val elements = _editorState.value.document.elements.toMutableList()

        if (selectedIds.size > 1) {
            // Multi-block heading change: apply heading to all selected text blocks
            elements.forEachIndexed { i, elem ->
                if (elem.id in selectedIds && elem is DocElement.TextBlock) {
                    val targetType = if (elem.blockType == newType) ElementType.PARAGRAPH else newType
                    elements[i] = elem.copy(blockType = targetType)
                }
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            return
        }

        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock

            // If user selected partial text inside a paragraph (e.g. selected a sub-clause to turn into heading)
            if (start < end && (start > 0 || end < target.text.length)) {
                val beforeText = target.text.substring(0, start).trimEnd()
                val selectedText = target.text.substring(start, end).trim()
                val afterText = target.text.substring(end).trimStart()

                val newBlocks = mutableListOf<DocElement>()
                if (beforeText.isNotEmpty()) {
                    newBlocks.add(target.copy(blockId = UUID.randomUUID().toString(), text = beforeText).adjustSpansForTextChange(beforeText))
                }
                val headingType = if (target.blockType == newType) ElementType.PARAGRAPH else newType
                newBlocks.add(DocElement.TextBlock(
                    blockId = UUID.randomUUID().toString(),
                    blockType = headingType,
                    text = selectedText
                ))
                if (afterText.isNotEmpty()) {
                    newBlocks.add(DocElement.TextBlock(
                        blockId = UUID.randomUUID().toString(),
                        blockType = ElementType.PARAGRAPH,
                        text = afterText
                    ))
                }
                elements.removeAt(index)
                elements.addAll(index, newBlocks)
            } else {
                // Whole block selected or cursor placed in block: toggle heading (if already same, revert to PARAGRAPH)
                val targetType = if (target.blockType == newType) ElementType.PARAGRAPH else newType
                elements[index] = target.copy(blockType = targetType)
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun toggleInlineBold() {
        val selectedIds = _editorState.value.selectedBlockIds
        val blockId = _editorState.value.activeSelectedBlockId
        val start = _editorState.value.activeSelectionStart
        val end = _editorState.value.activeSelectionEnd
        val elements = _editorState.value.document.elements.toMutableList()

        if (selectedIds.size > 1) {
            elements.forEachIndexed { i, elem ->
                if (elem.id in selectedIds && elem is DocElement.TextBlock) {
                    elements[i] = elem.copy(isBold = !elem.isBold)
                }
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            return
        }

        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            elements[index] = target.toggleFormatOnRange(start, end, "BOLD")
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun toggleInlineItalic() {
        val selectedIds = _editorState.value.selectedBlockIds
        val blockId = _editorState.value.activeSelectedBlockId
        val start = _editorState.value.activeSelectionStart
        val end = _editorState.value.activeSelectionEnd
        val elements = _editorState.value.document.elements.toMutableList()

        if (selectedIds.size > 1) {
            elements.forEachIndexed { i, elem ->
                if (elem.id in selectedIds && elem is DocElement.TextBlock) {
                    elements[i] = elem.copy(isItalic = !elem.isItalic)
                }
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            return
        }

        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            elements[index] = target.toggleFormatOnRange(start, end, "ITALIC")
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun toggleInlineUnderline() {
        val selectedIds = _editorState.value.selectedBlockIds
        val blockId = _editorState.value.activeSelectedBlockId
        val start = _editorState.value.activeSelectionStart
        val end = _editorState.value.activeSelectionEnd
        val elements = _editorState.value.document.elements.toMutableList()

        if (selectedIds.size > 1) {
            elements.forEachIndexed { i, elem ->
                if (elem.id in selectedIds && elem is DocElement.TextBlock) {
                    elements[i] = elem.copy(isUnderline = !elem.isUnderline)
                }
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            return
        }

        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            elements[index] = target.toggleFormatOnRange(start, end, "UNDERLINE")
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun toggleInlineStrikethrough() {
        val selectedIds = _editorState.value.selectedBlockIds
        val blockId = _editorState.value.activeSelectedBlockId
        val start = _editorState.value.activeSelectionStart
        val end = _editorState.value.activeSelectionEnd
        val elements = _editorState.value.document.elements.toMutableList()

        if (selectedIds.size > 1) {
            elements.forEachIndexed { i, elem ->
                if (elem.id in selectedIds && elem is DocElement.TextBlock) {
                    elements[i] = elem.copy(isStrikethrough = !elem.isStrikethrough)
                }
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            return
        }

        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            elements[index] = target.toggleFormatOnRange(start, end, "STRIKETHROUGH")
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun setTextColor(colorHex: String?) {
        val selectedIds = _editorState.value.selectedBlockIds
        val blockId = _editorState.value.activeSelectedBlockId
        val start = _editorState.value.activeSelectionStart
        val end = _editorState.value.activeSelectionEnd
        val elements = _editorState.value.document.elements.toMutableList()

        if (selectedIds.size > 1) {
            elements.forEachIndexed { i, elem ->
                if (elem.id in selectedIds && elem is DocElement.TextBlock) {
                    elements[i] = elem.copy(textColorHex = colorHex)
                }
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            return
        }

        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            if (start < end && end <= target.text.length) {
                elements[index] = target.applyFormattingToRange(start, end, textColor = colorHex, clearColors = colorHex == null)
            } else {
                elements[index] = target.copy(textColorHex = colorHex)
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun setHighlightColor(colorHex: String?) {
        val selectedIds = _editorState.value.selectedBlockIds
        val blockId = _editorState.value.activeSelectedBlockId
        val start = _editorState.value.activeSelectionStart
        val end = _editorState.value.activeSelectionEnd
        val elements = _editorState.value.document.elements.toMutableList()

        if (selectedIds.size > 1) {
            elements.forEachIndexed { i, elem ->
                if (elem.id in selectedIds && elem is DocElement.TextBlock) {
                    elements[i] = elem.copy(highlightColorHex = colorHex)
                }
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            return
        }

        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            if (start < end && end <= target.text.length) {
                elements[index] = target.applyFormattingToRange(start, end, highlightColor = colorHex, clearColors = colorHex == null)
            } else {
                elements[index] = target.copy(highlightColorHex = colorHex)
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    /**
     * Intelligently and safely splits and parses pasted chapter text on background thread.
     */
    fun pasteAndSplitContent(rawText: String) {
        if (rawText.isBlank()) return

        viewModelScope.launch {
            val parsedBlocks = withContext(Dispatchers.Default) {
                com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(rawText)
            }
            if (parsedBlocks.isEmpty()) return@launch

            val currentElements = _editorState.value.document.elements.toMutableList()
            val blockId = _editorState.value.activeSelectedBlockId
            val targetIndex = if (blockId != null) {
                val idx = currentElements.indexOfFirst { it.id == blockId }
                if (idx >= 0) idx else currentElements.size
            } else {
                currentElements.size
            }

            // If active block is empty paragraph, replace it; otherwise insert after it
            if (targetIndex in currentElements.indices &&
                currentElements[targetIndex] is DocElement.TextBlock &&
                (currentElements[targetIndex] as DocElement.TextBlock).text.isBlank()
            ) {
                currentElements.removeAt(targetIndex)
                currentElements.addAll(targetIndex, parsedBlocks)
            } else {
                val insertPos = if (targetIndex in currentElements.indices) targetIndex + 1 else currentElements.size
                currentElements.addAll(insertPos, parsedBlocks)
            }

            onDocumentUpdated(_editorState.value.document.copy(elements = currentElements))
        }
    }

    fun setAlignment(align: String) {
        val blockId = _editorState.value.activeSelectedBlockId
        val elements = _editorState.value.document.elements.toMutableList()
        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            elements[index] = target.copy(alignment = align)
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun toggleBulletList() {
        val blockId = _editorState.value.activeSelectedBlockId
        val elements = _editorState.value.document.elements.toMutableList()
        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            val newType = if (target.blockType == ElementType.BULLET_LIST) ElementType.PARAGRAPH else ElementType.BULLET_LIST
            elements[index] = target.copy(blockType = newType)
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun toggleNumberedList() {
        val blockId = _editorState.value.activeSelectedBlockId
        val elements = _editorState.value.document.elements.toMutableList()
        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        if (index in elements.indices && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            val newType = if (target.blockType == ElementType.NUMBERED_LIST) ElementType.PARAGRAPH else ElementType.NUMBERED_LIST
            elements[index] = target.copy(blockType = newType)
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    // --- AI Replacement & Below Insertion ---
    fun replaceSelectionWithAI(newText: String) {
        val selected = _editorState.value.activeSelectedText
        val blockId = _editorState.value.activeSelectedBlockId
        val elements = _editorState.value.document.elements.toMutableList()
        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else -1

        if (index >= 0 && elements[index] is DocElement.TextBlock) {
            val target = elements[index] as DocElement.TextBlock
            // If inline sub-string was selected and newText is a single line, replace inline
            if (selected.isNotBlank() && target.text.contains(selected) && !newText.contains("\n")) {
                val updatedText = target.text.replace(selected, newText)
                elements[index] = target.copy(text = updatedText)
                onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            } else {
                // Multi-line or full-block replacement with structured blocks
                val parsedBlocks = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(newText)
                if (parsedBlocks.isNotEmpty()) {
                    elements.removeAt(index)
                    elements.addAll(index, parsedBlocks)
                } else {
                    elements[index] = target.copy(text = newText)
                }
                onDocumentUpdated(_editorState.value.document.copy(elements = elements))
            }
        } else {
            val parsedBlocks = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(newText)
            if (parsedBlocks.isNotEmpty()) {
                elements.addAll(parsedBlocks)
            } else {
                elements.add(DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = newText))
            }
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    fun insertAIBelow(newText: String) {
        val blockId = _editorState.value.activeSelectedBlockId
        val elements = _editorState.value.document.elements.toMutableList()
        val index = if (blockId != null) elements.indexOfFirst { it.id == blockId } else elements.size - 1
        val targetIdx = (index + 1).coerceIn(0, elements.size)
        val parsedBlocks = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(newText)
        if (parsedBlocks.isNotEmpty()) {
            elements.addAll(targetIdx, parsedBlocks)
        } else {
            elements.add(targetIdx, DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = newText))
        }
        onDocumentUpdated(_editorState.value.document.copy(elements = elements))
    }

    /**
     * Updates specific topic content directly (chapterId -> topicId -> content)
     * without rewriting the rest of the chapter document.
     */
    fun updateTopicContent(chapterId: String, topicId: String, newContent: String) {
        val currentDoc = _editorState.value.document
        val parsedBlocks = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(newContent)
        val updatedDoc = currentDoc.replaceTopicContent(topicId, parsedBlocks)
        onDocumentUpdated(updatedDoc)
    }

    /**
     * Updates a diagram block directly in the document.
     */
    fun updateDiagramBlock(updatedBlock: DocElement.DiagramBlock) {
        val elements = _editorState.value.document.elements.toMutableList()
        val index = elements.indexOfFirst { it.id == updatedBlock.id }
        if (index >= 0) {
            elements[index] = updatedBlock
            onDocumentUpdated(_editorState.value.document.copy(elements = elements))
        }
    }

    /**
     * Appends/inserts content directly into a specific topic before the next H2.
     */
    fun insertContentIntoTopic(chapterId: String, topicId: String, newContent: String) {
        val currentDoc = _editorState.value.document
        val parsedBlocks = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(newContent)
        val updatedDoc = currentDoc.insertIntoTopic(topicId, parsedBlocks)
        onDocumentUpdated(updatedDoc)
    }

    /**
     * Duplicates the topic content as a new section "[Topic Title] (AI Enhanced)"
     * leaving the original topic completely untouched.
     */
    fun duplicateTopicContent(chapterId: String, topicId: String, newContent: String) {
        val currentDoc = _editorState.value.document
        val parsedBlocks = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(newContent)
        val updatedDoc = currentDoc.duplicateTopicContent(topicId, parsedBlocks)
        onDocumentUpdated(updatedDoc)
    }

    // --- Debounced Autosave ---
    private fun scheduleDebouncedAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(500)
            val state = _editorState.value
            if (state.chapterId.isNotBlank()) {
                val docEntity = NoteDocumentEntity(
                    chapterId = state.chapterId,
                    contentJson = state.document.toJson(),
                    paperStyle = state.paperStyle.name,
                    fontFamily = state.fontFamily.name,
                    fontSizeSp = state.fontSizeSp,
                    lineSpacingMultiplier = state.lineSpacingMultiplier,
                    themeMode = state.paperTheme.name,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveDocument(docEntity)
            }
        }
    }

    // --- Backup & Restore (JSON & ZIP) ---
    suspend fun exportBackupJson(options: com.example.data.repository.BackupExportOptions = com.example.data.repository.BackupExportOptions()): String {
        return repository.exportBackupJson(options)
    }

    suspend fun importBackupJson(json: String): Boolean {
        return repository.importBackupJson(json)
    }

    suspend fun exportZipBackupToStream(outputStream: java.io.OutputStream, options: com.example.data.repository.BackupExportOptions = com.example.data.repository.BackupExportOptions()): Boolean {
        val backupJson = repository.exportBackupJson(options)
        return com.example.data.ZipBackupHelper.writeBackupZipToStream(outputStream, backupJson)
    }

    suspend fun importZipBackupFromUri(context: android.content.Context, uri: android.net.Uri): Boolean {
        val jsonPayload = com.example.data.ZipBackupHelper.readBackupJsonFromUri(context, uri)
        return if (!jsonPayload.isNullOrBlank()) {
            repository.importBackupJson(jsonPayload)
        } else {
            false
        }
    }

    suspend fun createTempBackupZip(context: android.content.Context, options: com.example.data.repository.BackupExportOptions = com.example.data.repository.BackupExportOptions()): java.io.File? {
        val backupJson = repository.exportBackupJson(options)
        return com.example.data.ZipBackupHelper.createTempBackupZip(context, backupJson)
    }

    suspend fun reloadJkssbDefaultContent(context: android.content.Context): Boolean {
        return repository.reloadJkssbContent(context)
    }

    suspend fun clearAllStudyContent() {
        repository.clearAllStudyContent()
    }

    // ==========================================
    // --- FOCUS MODE, HABIT & TIMETABLE HUB ---
    // ==========================================

    val dailyStudyLogs: StateFlow<List<com.example.data.entity.DailyStudyLogEntity>> = repository.allDailyStudyLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val studyTasks: StateFlow<List<com.example.data.entity.StudyTaskEntity>> = repository.allStudyTasks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTaskCompletions: StateFlow<List<com.example.data.entity.TaskCompletionEntity>> = repository.allTaskCompletions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getStudiedMinutesForDate(
        dateKey: String,
        logs: List<com.example.data.entity.DailyStudyLogEntity> = dailyStudyLogs.value,
        prefs: com.example.data.entity.UserPreferencesEntity? = preferences.value,
        tasks: List<com.example.data.entity.StudyTaskEntity> = studyTasks.value,
        completions: List<com.example.data.entity.TaskCompletionEntity> = allTaskCompletions.value
    ): Int {
        val todayKey = getTodayDateKey()
        val yesterdayKey = getYesterdayDateKey()
        val logMins = logs.find { it.dateKey == dateKey }?.totalFocusedMinutes ?: 0
        val prefsMins = when {
            dateKey == todayKey && (prefs?.lastStudyDate == todayKey || prefs?.lastDailyTargetDate == todayKey) -> prefs.todayFocusedMinutes
            dateKey == yesterdayKey -> prefs?.yesterdayFocusedMinutes ?: 0
            else -> 0
        }
        return maxOf(logMins, prefsMins)
    }

    fun getTodayDateKey(): String {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        } catch (e: Exception) {
            "2026-08-30"
        }
    }

    fun getYesterdayDateKey(): String {
        return try {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        } catch (e: Exception) {
            "2026-08-29"
        }
    }

    fun getDayOfWeekCode(dateKey: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val d = sdf.parse(dateKey) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = d
            when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.SUNDAY -> "SUN"
                java.util.Calendar.MONDAY -> "MON"
                java.util.Calendar.TUESDAY -> "TUE"
                java.util.Calendar.WEDNESDAY -> "WED"
                java.util.Calendar.THURSDAY -> "THU"
                java.util.Calendar.FRIDAY -> "FRI"
                java.util.Calendar.SATURDAY -> "SAT"
                else -> "MON"
            }
        } catch (e: Exception) {
            "MON"
        }
    }

    fun getDaysBetween(fromKey: String, toKey: String): Long {
        if (fromKey.isBlank() || toKey.isBlank()) return 1
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val d1 = sdf.parse(fromKey) ?: return 1
            val d2 = sdf.parse(toKey) ?: return 1
            val diffMillis = d2.time - d1.time
            java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
        } catch (e: Exception) {
            1
        }
    }

    fun manualAdjustStreak(newStreak: Int) {
        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            val validStreak = newStreak.coerceAtLeast(0)
            val todayKey = getTodayDateKey()
            repository.updateStreak(
                currentStreak = validStreak,
                bestStreak = maxOf(prefs.bestStreak, validStreak),
                lastActiveDate = if (validStreak > 0) todayKey else prefs.lastActiveDate
            )
        }
    }

    fun advanceStreakToday() {
        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            val todayKey = getTodayDateKey()
            val newStreak = (prefs.currentStreak + 1).coerceAtLeast(1)
            repository.updateStreak(
                currentStreak = newStreak,
                bestStreak = maxOf(prefs.bestStreak, newStreak),
                lastActiveDate = todayKey
            )
        }
    }

    private val _selectedDateKey = MutableStateFlow(
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        } catch (e: Exception) {
            "2026-08-30"
        }
    )
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _activeFocusSession = MutableStateFlow(ActiveFocusSession())
    val activeFocusSession: StateFlow<ActiveFocusSession> = _activeFocusSession.asStateFlow()

    private var focusTimerJob: Job? = null

    fun selectToday() {
        _selectedDateKey.value = getTodayDateKey()
    }

    fun selectDateKey(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun toggleTaskCompletion(taskId: String, dateKey: String, isCompleted: Boolean) {
        val todayKey = getTodayDateKey()
        // Strict Today-only rule: Only tasks scheduled for Today can be checked or completed
        if (dateKey != todayKey) {
            return
        }

        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId, dateKey, isCompleted)
            // Manual checkbox marking does NOT inflate streak.
            // Streak is officially updated when actual scheduled study/focus target time is completed.
        }
    }

    private var accumulatedStudySeconds = 0

    /**
     * Records active continuous study time in seconds (from AskAI, reading notes, practice quizzes).
     * Batches into full minutes and immediately persists to Room database.
     */
    fun recordActiveStudySeconds(seconds: Int, subjectName: String? = null, chapterTitle: String? = null) {
        if (seconds <= 0) return
        accumulatedStudySeconds += seconds
        if (accumulatedStudySeconds >= 60) {
            val mins = accumulatedStudySeconds / 60
            accumulatedStudySeconds %= 60
            addFocusedStudyTime(mins)
        }
    }

    /**
     * Flushes any remaining accumulated seconds (>= 10s counts as 1 full minute).
     */
    fun flushActiveStudySeconds() {
        if (accumulatedStudySeconds >= 10) {
            addFocusedStudyTime(1)
        }
        accumulatedStudySeconds = 0
    }

    /**
     * Adds focused study time and verifies whether the mandatory daily study target is met.
     * When the target is completed:
     * - Daily target is officially marked complete.
     * - Streak is officially incremented and saved.
     * - Today's scheduled tasks are automatically checked off based on completed minutes.
     * - Smart notifications & XP milestones are triggered.
     */
    fun addFocusedStudyTime(minutes: Int) {
        if (minutes <= 0) return
        viewModelScope.launch {
            val todayKey = getTodayDateKey()
            val yesterdayKey = getYesterdayDateKey()
            val prefs = repository.getPreferencesSync()
            val dailyTarget = prefs.dailyTargetMinutes.coerceAtLeast(1)

            val lastStudyDate = prefs.lastStudyDate
            val yesterdayMins = when (lastStudyDate) {
                yesterdayKey -> prefs.todayFocusedMinutes
                todayKey -> prefs.yesterdayFocusedMinutes
                else -> if (prefs.todayFocusedMinutes > 0 && lastStudyDate.isNotBlank()) 0 else prefs.yesterdayFocusedMinutes
            }

            val todayTaskMins = try {
                val tasks = repository.getStudyTasksSync()
                val completions = repository.getTaskCompletionsForDate(todayKey)
                tasks.filter { t -> completions.any { it.taskId == t.id && it.isCompleted } }.sumOf { it.durationMinutes }
            } catch (e: Exception) { 0 }

            val currentPrefsMins = if (lastStudyDate == todayKey || prefs.lastDailyTargetDate == todayKey) prefs.todayFocusedMinutes else 0
            val currentTodayMins = maxOf(currentPrefsMins, todayTaskMins)
            val newTodayMins = currentTodayMins + minutes

            val wasTargetMetBefore = prefs.lastDailyTargetDate == todayKey && currentTodayMins >= dailyTarget
            val isTargetNowMet = newTodayMins >= dailyTarget

            repository.updateDailyTargetProgress(
                dailyTargetMinutes = dailyTarget,
                todayFocusedMinutes = newTodayMins,
                yesterdayFocusedMinutes = yesterdayMins,
                lastStudyDate = todayKey,
                lastDailyTargetDate = if (isTargetNowMet) todayKey else prefs.lastDailyTargetDate
            )

            // Automatically complete today's scheduled tasks based on cumulative verified study minutes
            try {
                val currentDayOfWeek = java.time.LocalDate.now().dayOfWeek.name
                val currentDayShort = currentDayOfWeek.take(3)
                val allTasks = repository.getStudyTasksSync()
                val todayTasks = allTasks.filter {
                    it.daysOfWeek.contains(currentDayShort, ignoreCase = true) ||
                    it.daysOfWeek.contains("DAILY", ignoreCase = true) ||
                    it.daysOfWeek.contains("EVERYDAY", ignoreCase = true)
                }.sortedBy { it.orderIndex }

                var remainingMins = newTodayMins
                for (task in todayTasks) {
                    if (remainingMins >= task.durationMinutes) {
                        repository.toggleTaskCompletion(task.id, todayKey, true)
                        remainingMins -= task.durationMinutes
                    }
                }
            } catch (e: Exception) {
                // Ignore fallback
            }

            // Check streak advancement when user studies today
            val currentStreak = prefs.currentStreak
            val bestStreak = prefs.bestStreak
            val lastActive = prefs.lastActiveDate
            val daysDiff = getDaysBetween(lastActive, todayKey)

            if (lastActive != todayKey) {
                val newStreak = when {
                    lastActive.isBlank() -> maxOf(currentStreak, 1)
                    daysDiff in 1..2 -> currentStreak + 1
                    else -> 1
                }
                repository.updateStreak(
                    currentStreak = newStreak,
                    bestStreak = maxOf(bestStreak, newStreak),
                    lastActiveDate = todayKey
                )
            }

            if (!wasTargetMetBefore && isTargetNowMet) {
                val activePrefs = repository.getPreferencesSync()
                val currentActiveStreak = activePrefs.currentStreak
                // Trigger Achievement / Celebratory Notification
                addNotification(
                    title = "🎉 Daily target completed!",
                    message = "Awesome! You completed your ${dailyTarget}m study target. Streak extended to $currentActiveStreak days!",
                    type = "TARGET_COMPLETED",
                    actionType = "VIEW_PLAN"
                )

                if (currentActiveStreak in listOf(3, 7, 10, 14, 21, 30, 50, 100)) {
                    addNotification(
                        title = "🔥 $currentActiveStreak-Day Streak Milestone!",
                        message = "Incredible dedication! You have sustained a continuous $currentActiveStreak-day study streak.",
                        type = "STREAK_MILESTONE",
                        actionType = "VIEW_PLAN"
                    )
                }
            }
        }
    }

    fun updateDailyTargetMinutes(targetMinutes: Int) {
        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            repository.updateDailyTargetProgress(
                dailyTargetMinutes = targetMinutes.coerceAtLeast(5),
                todayFocusedMinutes = prefs.todayFocusedMinutes,
                lastDailyTargetDate = prefs.lastDailyTargetDate
            )
        }
    }

    fun updateAppThemeMode(themeMode: String) {
        viewModelScope.launch {
            repository.updateAppThemeMode(themeMode)
        }
    }

    fun saveAiProvider(provider: String) {
        viewModelScope.launch {
            repository.updateAiProvider(provider)
        }
    }

    fun saveDeepSeekApiKey(key: String) {
        viewModelScope.launch {
            repository.updateDeepSeekApiKey(key)
        }
    }

    // --- Smart Notifications Management ---
    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun addNotification(
        title: String,
        message: String,
        type: String = "REMINDER",
        actionType: String? = null,
        actionPayload: String? = null
    ) {
        viewModelScope.launch {
            val notification = com.example.data.entity.SmartNotificationEntity(
                id = "notif_${java.util.UUID.randomUUID().toString().take(8)}",
                title = title,
                message = message,
                type = type,
                actionType = actionType ?: "NONE",
                actionPayload = actionPayload
            )
            repository.addNotification(notification)

            // Dispatch real Android System Notification with interactive actions to device tray
            com.example.util.PrepOSFocusNotificationManager.showSystemAlertNotification(
                context = getApplication(),
                title = title,
                message = message,
                type = type,
                actionType = actionType,
                actionPayload = actionPayload
            )
        }
    }

    fun checkAndGenerateDailyReminders() {
        viewModelScope.launch {
            val todayKey = getTodayDateKey()
            val prefs = repository.getPreferencesSync()
            val tasks = repository.getStudyTasksSync()
            val todayDayOfWeek = getDayOfWeekCode(todayKey)

            val todayTasks = tasks.filter {
                it.daysOfWeek.isBlank() || it.daysOfWeek.contains(todayDayOfWeek, ignoreCase = true)
            }

            if (todayTasks.isNotEmpty()) {
                val firstTask = todayTasks.first()
                val existing = repository.getRecentNotificationsSync()
                val hasReminderToday = existing.any {
                    it.createdAt >= System.currentTimeMillis() - (12 * 3600 * 1000) &&
                    it.title.contains("scheduled", ignoreCase = true)
                }
                if (!hasReminderToday) {
                    addNotification(
                        title = "${firstTask.subjectName} chapter scheduled",
                        message = "${firstTask.taskTitle} is scheduled for today (${firstTask.startTime} - ${firstTask.endTime}). Start now to hit your target!",
                        type = "REMINDER",
                        actionType = "START_FOCUS",
                        actionPayload = firstTask.subjectName
                    )
                }
            }

            // Streak warning check
            val currentStreak = prefs.currentStreak
            if (currentStreak > 0 && prefs.lastDailyTargetDate != todayKey) {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                if (hour >= 18) { // After 6 PM
                    val existing = repository.getRecentNotificationsSync()
                    val hasWarningToday = existing.any {
                        it.type == "STREAK_WARNING" && it.createdAt >= System.currentTimeMillis() - (8 * 3600 * 1000)
                    }
                    if (!hasWarningToday) {
                        addNotification(
                            title = "⚠️ Your streak may break today",
                            message = "You have an active $currentStreak-day streak! Complete your ${prefs.dailyTargetMinutes}m focus target before midnight to keep it alive.",
                            type = "STREAK_WARNING",
                            actionType = "START_FOCUS"
                        )
                    }
                }
            }
        }
    }

    private fun checkAndUpdateStreak(dateKey: String) {
        viewModelScope.launch {
            val todayKey = getTodayDateKey()
            if (dateKey != todayKey) return@launch

            val prefs = preferences.value
            val currentStreak = prefs?.currentStreak ?: 0
            val bestStreak = prefs?.bestStreak ?: 0
            val lastDate = prefs?.lastActiveDate ?: ""
            val yesterdayKey = getYesterdayDateKey()

            if (lastDate == todayKey) {
                // Already recorded streak progress for today
                return@launch
            }

            val newStreak = if (lastDate == yesterdayKey) {
                // Continuous consecutive streak
                currentStreak + 1
            } else {
                // First day or streak restarted
                1
            }

            repository.updateStreak(
                currentStreak = newStreak,
                bestStreak = maxOf(bestStreak, newStreak),
                lastActiveDate = todayKey
            )
        }
    }

    fun setStreakForTesting(newStreak: Int) {
        viewModelScope.launch {
            val prefs = preferences.value
            val bestStreak = maxOf(prefs?.bestStreak ?: 0, newStreak)
            val todayKey = getTodayDateKey()
            repository.updateStreak(
                currentStreak = newStreak,
                bestStreak = bestStreak,
                lastActiveDate = todayKey
            )
        }
    }

    fun createStudyTask(
        subjectName: String,
        taskTitle: String,
        taskType: String = "READING",
        subDetails: String = "",
        startTime: String = "7:00 PM",
        endTime: String = "7:45 PM",
        durationMinutes: Int = 45,
        daysOfWeek: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
        repeatWeekly: Boolean = true
    ) {
        viewModelScope.launch {
            repository.createStudyTask(
                subjectName = subjectName,
                taskTitle = taskTitle,
                taskType = taskType,
                subDetails = subDetails,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                daysOfWeek = daysOfWeek,
                repeatWeekly = repeatWeekly
            )
            val updatedTasks = repository.getStudyTasksSync()
            com.example.util.PrepOSAlarmScheduler.scheduleAllReminders(getApplication(), updatedTasks)
        }
    }

    fun getTestAttempt(id: String, onResult: (com.example.data.entity.TestAttemptEntity?) -> Unit) {
        viewModelScope.launch {
            val attempt = repository.getTestAttemptById(id)
            onResult(attempt)
        }
    }

    fun deleteStudyTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteStudyTask(taskId)
            com.example.util.PrepOSAlarmScheduler.cancelTaskReminder(getApplication(), taskId)
        }
    }

    fun rescheduleAllAlarms() {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = repository.getStudyTasksSync()
            com.example.util.PrepOSAlarmScheduler.scheduleAllReminders(getApplication(), tasks)
        }
    }

    fun updateFocusSettings(
        appPinning: Boolean,
        dnd: Boolean,
        notificationBehaviour: String,
        timerMinutes: Int,
        followTimetable: Boolean
    ) {
        viewModelScope.launch {
            repository.updateFocusSettings(
                appPinning = appPinning,
                dnd = dnd,
                notificationBehaviour = notificationBehaviour,
                timerMinutes = timerMinutes,
                followTimetable = followTimetable
            )
        }
    }

    private val isFinalizingFocus = java.util.concurrent.atomic.AtomicBoolean(false)

    fun checkAndRecoverActiveFocusSession() {
        viewModelScope.launch {
            try {
                val persisted = repository.getActiveFocusSessionSync()
                if (persisted != null && (persisted.status == "RUNNING" || persisted.status == "PAUSED")) {
                    val now = System.currentTimeMillis()
                    val (rawElapsedSec, remainingSec) = calculateTimerState(persisted, now)
                    val cappedElapsedSec = rawElapsedSec.coerceAtMost(persisted.plannedDurationSeconds)
                    val timeSinceHeartbeat = now - persisted.lastHeartbeatEpochMs
                    val isOld = timeSinceHeartbeat > 3 * 60 * 1000L || (now - persisted.startTimeEpochMs > 12 * 3600 * 1000L)

                    if (isOld) {
                        // Crash / killed / overnight interruption detected
                        _activeFocusSession.value = ActiveFocusSession(
                            isActive = true,
                            sessionId = persisted.sessionId,
                            taskTitle = persisted.taskTitle,
                            subjectName = persisted.subjectName,
                            subjectId = persisted.subjectId,
                            chapterId = persisted.chapterId,
                            chapterTitle = persisted.chapterTitle,
                            totalSeconds = persisted.plannedDurationSeconds,
                            remainingSeconds = remainingSec,
                            elapsedSeconds = cappedElapsedSec,
                            isPaused = true,
                            associatedTaskId = persisted.associatedTaskId,
                            startTimeEpochMs = persisted.startTimeEpochMs,
                            lastPauseEpochMs = persisted.lastPauseEpochMs,
                            accumulatedPauseMs = persisted.accumulatedPauseMs,
                            status = "INTERRUPTED",
                            isInterrupted = true,
                            recoverableElapsedSeconds = cappedElapsedSec
                        )
                    } else {
                        // Fast process restart / recent activity -> restore session
                        _activeFocusSession.value = ActiveFocusSession(
                            isActive = true,
                            sessionId = persisted.sessionId,
                            taskTitle = persisted.taskTitle,
                            subjectName = persisted.subjectName,
                            subjectId = persisted.subjectId,
                            chapterId = persisted.chapterId,
                            chapterTitle = persisted.chapterTitle,
                            totalSeconds = persisted.plannedDurationSeconds,
                            remainingSeconds = remainingSec,
                            elapsedSeconds = rawElapsedSec,
                            isPaused = persisted.status == "PAUSED",
                            associatedTaskId = persisted.associatedTaskId,
                            startTimeEpochMs = persisted.startTimeEpochMs,
                            lastPauseEpochMs = persisted.lastPauseEpochMs,
                            accumulatedPauseMs = persisted.accumulatedPauseMs,
                            status = persisted.status,
                            isInterrupted = false,
                            recoverableElapsedSeconds = rawElapsedSec
                        )
                        if (persisted.status == "RUNNING") {
                            startFocusTimerLoop()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateTimerState(session: com.example.data.entity.ActiveFocusSessionEntity, nowEpochMs: Long): Pair<Int, Int> {
        val currentPauseDelta = if (session.status == "PAUSED" && session.lastPauseEpochMs > 0) {
            (nowEpochMs - session.lastPauseEpochMs).coerceAtLeast(0)
        } else {
            0L
        }
        val totalPauseMs = session.accumulatedPauseMs + currentPauseDelta
        val rawElapsedMs = (nowEpochMs - session.startTimeEpochMs - totalPauseMs).coerceAtLeast(0)
        val elapsedSeconds = (rawElapsedMs / 1000).toInt()
        val remainingSeconds = (session.plannedDurationSeconds - elapsedSeconds).coerceAtLeast(0)
        return Pair(elapsedSeconds, remainingSeconds)
    }

    fun startFocusSession(
        task: com.example.data.entity.StudyTaskEntity? = null,
        customMinutes: Int? = null,
        subjectId: String? = null,
        subjectName: String? = null,
        chapterId: String? = null,
        chapterTitle: String? = null
    ) {
        focusTimerJob?.cancel()
        val prefs = preferences.value
        val durationMins = customMinutes ?: (if (task != null) task.durationMinutes else (prefs?.focusTimerMinutes ?: 45))
        val targetSeconds = if (durationMins <= 0) 45 * 60 else durationMins * 60
        val now = System.currentTimeMillis()
        val sessionId = "focus_${UUID.randomUUID().toString().take(8)}"
        val taskTitle = task?.taskTitle ?: "Deep Study Session"
        val subName = subjectName ?: (task?.subjectName ?: "PrepOS Focus")

        val entity = com.example.data.entity.ActiveFocusSessionEntity(
            id = "current_active_focus_session",
            sessionId = sessionId,
            taskTitle = taskTitle,
            subjectName = subName,
            subjectId = subjectId ?: task?.subjectId,
            chapterId = chapterId,
            chapterTitle = chapterTitle ?: "",
            associatedTaskId = task?.id,
            plannedDurationSeconds = targetSeconds,
            startTimeEpochMs = now,
            lastPauseEpochMs = 0L,
            accumulatedPauseMs = 0L,
            status = "RUNNING",
            lastHeartbeatEpochMs = now
        )

        viewModelScope.launch {
            repository.saveActiveFocusSession(entity)
        }

        val newSession = ActiveFocusSession(
            isActive = true,
            sessionId = sessionId,
            taskTitle = taskTitle,
            subjectName = subName,
            subjectId = entity.subjectId,
            chapterId = entity.chapterId,
            chapterTitle = entity.chapterTitle,
            totalSeconds = targetSeconds,
            remainingSeconds = targetSeconds,
            elapsedSeconds = 0,
            isPaused = false,
            associatedTaskId = task?.id,
            startTimeEpochMs = now,
            lastPauseEpochMs = 0L,
            accumulatedPauseMs = 0L,
            status = "RUNNING",
            isInterrupted = false,
            recoverableElapsedSeconds = 0
        )
        _activeFocusSession.value = newSession
        com.example.util.PrepOSFocusNotificationManager.updateFocusNotification(getApplication(), newSession)

        if (newSession.isTimerMode && newSession.totalSeconds > 0) {
            com.example.util.PrepOSAlarmScheduler.scheduleFocusTimerCompletion(
                getApplication(),
                newSession.remainingSeconds,
                newSession.taskTitle,
                newSession.subjectName
            )
        }

        startFocusTimerLoop()
    }

    private fun startFocusTimerLoop() {
        focusTimerJob?.cancel()
        focusTimerJob = viewModelScope.launch {
            var ticks = 0
            while (true) {
                delay(1000)
                val current = _activeFocusSession.value
                if (current.isActive && current.status == "RUNNING" && !current.isPaused) {
                    val now = System.currentTimeMillis()
                    val rawElapsedMs = (now - current.startTimeEpochMs - current.accumulatedPauseMs).coerceAtLeast(0)
                    val elapsedSeconds = (rawElapsedMs / 1000).toInt()
                    val remainingSeconds = (current.totalSeconds - elapsedSeconds).coerceAtLeast(0)

                    val updated = current.copy(
                        elapsedSeconds = elapsedSeconds,
                        remainingSeconds = remainingSeconds
                    )
                    _activeFocusSession.value = updated
                    com.example.util.PrepOSFocusNotificationManager.updateFocusNotification(getApplication(), updated)

                    ticks++
                    if (ticks % 10 == 0) {
                        repository.updateFocusHeartbeat(now)
                    }

                    if (current.isTimerMode && current.totalSeconds > 0 && remainingSeconds <= 0) {
                        finishFocusSession()
                        break
                    }
                }
            }
        }
    }

    fun pauseFocusSession() {
        val current = _activeFocusSession.value
        if (!current.isActive || current.isPaused || current.status != "RUNNING") return
        val now = System.currentTimeMillis()
        val updated = current.copy(
            isPaused = true,
            status = "PAUSED",
            lastPauseEpochMs = now
        )
        _activeFocusSession.value = updated
        viewModelScope.launch {
            repository.updateFocusStatus("PAUSED", now, current.accumulatedPauseMs)
        }
        com.example.util.PrepOSAlarmScheduler.cancelFocusTimerAlarm(getApplication())
        com.example.util.PrepOSFocusNotificationManager.updateFocusNotification(getApplication(), updated)
    }

    fun resumeFocusSession() {
        val current = _activeFocusSession.value
        if (!current.isActive || !current.isPaused) return
        val now = System.currentTimeMillis()
        val pauseDelta = if (current.lastPauseEpochMs > 0) (now - current.lastPauseEpochMs).coerceAtLeast(0) else 0L
        val newAccumulated = current.accumulatedPauseMs + pauseDelta

        val updated = current.copy(
            isPaused = false,
            status = "RUNNING",
            lastPauseEpochMs = 0L,
            accumulatedPauseMs = newAccumulated,
            isInterrupted = false
        )
        _activeFocusSession.value = updated
        viewModelScope.launch {
            repository.updateFocusStatus("RUNNING", 0L, newAccumulated)
        }
        if (updated.isTimerMode && updated.remainingSeconds > 0) {
            com.example.util.PrepOSAlarmScheduler.scheduleFocusTimerCompletion(
                getApplication(),
                updated.remainingSeconds,
                updated.taskTitle,
                updated.subjectName
            )
        }
        com.example.util.PrepOSFocusNotificationManager.updateFocusNotification(getApplication(), updated)
        startFocusTimerLoop()
    }

    fun resumeInterruptedSession() {
        resumeFocusSession()
    }

    fun saveInterruptedSession() {
        val current = _activeFocusSession.value
        val elapsedMins = (current.recoverableElapsedSeconds / 60).coerceAtLeast(1)
        val isFull = current.recoverableElapsedSeconds >= current.totalSeconds && current.totalSeconds > 0
        finishFocusSessionWithMinutes(elapsedMins, markTaskCompleted = isFull)
    }

    fun discardInterruptedSession() {
        focusTimerJob?.cancel()
        _activeFocusSession.value = ActiveFocusSession()
        viewModelScope.launch {
            repository.clearActiveFocusSession()
        }
        com.example.util.PrepOSAlarmScheduler.cancelFocusTimerAlarm(getApplication())
        com.example.util.PrepOSFocusNotificationManager.cancelNotification(getApplication())
    }

    fun stopFocusSession() {
        focusTimerJob?.cancel()
        val current = _activeFocusSession.value
        val now = System.currentTimeMillis()
        val totalPause = current.accumulatedPauseMs + (if (current.isPaused && current.lastPauseEpochMs > 0) now - current.lastPauseEpochMs else 0L)
        val rawElapsedMs = (now - current.startTimeEpochMs - totalPause).coerceAtLeast(0)
        val maxCap = if (current.isTimerMode && current.totalSeconds > 0) current.totalSeconds else Int.MAX_VALUE
        val elapsedSeconds = (rawElapsedMs / 1000).toInt().coerceAtMost(maxCap)
        val elapsedMinutes = elapsedSeconds / 60
        val isFullCompleted = current.isTimerMode && current.totalSeconds > 0 && elapsedSeconds >= current.totalSeconds
        if (elapsedMinutes >= 1) {
            finishFocusSessionWithMinutes(elapsedMinutes, markTaskCompleted = isFullCompleted)
        } else {
            discardInterruptedSession()
        }
    }

    fun finishFocusSession() {
        val current = _activeFocusSession.value
        val now = System.currentTimeMillis()
        val totalPause = current.accumulatedPauseMs + (if (current.isPaused && current.lastPauseEpochMs > 0) now - current.lastPauseEpochMs else 0L)
        val rawElapsedMs = (now - current.startTimeEpochMs - totalPause).coerceAtLeast(0)
        val maxCap = if (current.isTimerMode && current.totalSeconds > 0) current.totalSeconds else Int.MAX_VALUE
        val elapsedSeconds = (rawElapsedMs / 1000).toInt().coerceAtMost(maxCap)
        val elapsedMinutes = if (current.isTimerMode && current.totalSeconds > 0 && elapsedSeconds >= current.totalSeconds) {
            current.totalSeconds / 60
        } else {
            (elapsedSeconds / 60).coerceAtLeast(1)
        }
        val isFullCompleted = current.isTimerMode && current.totalSeconds > 0 && elapsedSeconds >= current.totalSeconds
        finishFocusSessionWithMinutes(elapsedMinutes, markTaskCompleted = isFullCompleted)
    }

    fun finishFocusSessionWithMinutes(minutes: Int, markTaskCompleted: Boolean = false) {
        if (!isFinalizingFocus.compareAndSet(false, true)) {
            return
        }
        focusTimerJob?.cancel()
        val session = _activeFocusSession.value
        viewModelScope.launch {
            try {
                val todayKey = getTodayDateKey()
                val prefs = repository.getPreferencesSync()
                val dailyTarget = prefs.dailyTargetMinutes.coerceAtLeast(1)
                val currentPrefsMins = if (prefs.lastStudyDate == todayKey || prefs.lastDailyTargetDate == todayKey) prefs.todayFocusedMinutes else 0
                val newTodayMins = currentPrefsMins + minutes

                val wasTargetMetBefore = prefs.lastDailyTargetDate == todayKey && currentPrefsMins >= dailyTarget
                val isTargetNowMet = newTodayMins >= dailyTarget

                val yesterdayKey = getYesterdayDateKey()
                val currentStreak = prefs.currentStreak
                val bestStreak = prefs.bestStreak
                val lastActive = prefs.lastActiveDate
                val daysDiff = getDaysBetween(lastActive, todayKey)

                val newStreak = if (lastActive != todayKey) {
                    when {
                        lastActive.isBlank() -> maxOf(currentStreak, 1)
                        daysDiff in 1..2 -> currentStreak + 1
                        else -> 1
                    }
                } else {
                    currentStreak
                }

                repository.finalizeFocusSession(
                    newTodayMinutes = newTodayMins,
                    dailyTargetMinutes = dailyTarget,
                    targetDateKey = todayKey,
                    isTargetNowMet = isTargetNowMet,
                    newStreak = newStreak,
                    bestStreak = maxOf(bestStreak, newStreak),
                    lastActiveDate = todayKey,
                    associatedTaskId = session.associatedTaskId,
                    markTaskCompleted = markTaskCompleted,
                    sessionMinutes = minutes
                )

                if (!wasTargetMetBefore && isTargetNowMet) {
                    addNotification(
                        title = "🎉 Daily target completed!",
                        message = "Awesome! You completed your ${dailyTarget}m study target. Streak extended to $newStreak days!",
                        type = "TARGET_COMPLETED",
                        actionType = "VIEW_PLAN"
                    )

                    if (newStreak in listOf(3, 7, 10, 14, 21, 30, 50, 100)) {
                        addNotification(
                            title = "🔥 $newStreak-Day Streak Milestone!",
                            message = "Incredible dedication! You have sustained a continuous $newStreak-day study streak.",
                            type = "STREAK_MILESTONE",
                            actionType = "VIEW_PLAN"
                        )
                    }
                }

                _activeFocusSession.value = ActiveFocusSession()
                com.example.util.PrepOSAlarmScheduler.cancelFocusTimerAlarm(getApplication())
                com.example.util.PrepOSFocusNotificationManager.cancelNotification(getApplication())
            } finally {
                isFinalizingFocus.set(false)
            }
        }
    }

    // =========================================================================
    // --- 6-CARD WORKFLOW BATCH PERSISTENCE ---
    // =========================================================================

    /**
     * Persists an entire structured syllabus (Exam, Subjects, Chapters) to Room DB
     */
    fun saveWorkflowSyllabus(
        syllabus: com.example.ui.editor.ExtractedSyllabusResult,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // 1. Create or find Exam
                val examCode = syllabus.examName.take(6).uppercase().replace(" ", "")
                val examEntity = repository.createExam(
                    name = syllabus.examName.ifBlank { "Target Exam" },
                    code = examCode.ifBlank { "EXAM" },
                    colorHex = "#3B82F6"
                )

                // 2. Insert Subjects & Chapters
                syllabus.subjects.forEachIndexed { subIndex, sub ->
                    val subjectEntity = repository.createSubject(
                        name = sub.name,
                        examId = examEntity.id,
                        iconName = when (subIndex % 4) {
                            0 -> "book"
                            1 -> "calculate"
                            2 -> "psychology"
                            else -> "language"
                        },
                        colorHex = sub.colorHex
                    )

                    sub.chapters.forEachIndexed { chapIndex, chap ->
                        val initialElements = mutableListOf<com.example.model.DocElement>()
                        initialElements.add(
                            com.example.model.DocElement.TextBlock(
                                blockType = com.example.model.ElementType.HEADING_1,
                                text = "${chap.chapterNumber}. ${chap.title}"
                            )
                        )
                        if (chap.topics.isNotEmpty()) {
                            initialElements.add(
                                com.example.model.DocElement.TextBlock(
                                    blockType = com.example.model.ElementType.HEADING_2,
                                    text = "Core Topics & High-Yield Syllabus"
                                )
                            )
                            chap.topics.forEach { topic ->
                                initialElements.add(
                                    com.example.model.DocElement.TextBlock(
                                        blockType = com.example.model.ElementType.BULLET_LIST,
                                        text = topic
                                    )
                                )
                            }
                        }
                        initialElements.add(
                            com.example.model.DocElement.TextBlock(
                                blockType = com.example.model.ElementType.HEADING_2,
                                text = "Concept Notes & Detailed Facts"
                            )
                        )
                        initialElements.add(
                            com.example.model.DocElement.TextBlock(
                                blockType = com.example.model.ElementType.PARAGRAPH,
                                text = "Start active recall study notes for ${chap.title} here."
                            )
                        )

                        repository.createChapterWithSanitizedContent(
                            subjectId = subjectEntity.id,
                            title = chap.title,
                            summary = if (chap.topics.isNotEmpty()) chap.topics.joinToString(", ") else "Syllabus chapter",
                            chapterNumber = chap.chapterNumber,
                            elements = initialElements,
                            questions = emptyList()
                        )
                    }
                }

                // Update default exam
                repository.updateDefaultExamId(examEntity.id)
                _selectedExamIdFilter.value = examEntity.id

                addNotification(
                    title = "📚 Syllabus created: ${syllabus.examName}",
                    message = "Added ${syllabus.subjects.size} subjects and ${syllabus.subjects.sumOf { it.chapters.size }} chapters to your study workspace.",
                    type = "SYLLABUS_READY",
                    actionType = "VIEW_SYLLABUS"
                )

                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    /**
     * Persists weekly generated study tasks to Room DB
     */
    fun saveWorkflowWeeklyPlan(
        tasks: List<com.example.ui.editor.WeeklyPlanTaskItem>,
        dailyTargetMinutes: Int = 45,
        replaceExisting: Boolean = false,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (replaceExisting) {
                    val existing = repository.getStudyTasksSync()
                    existing.forEach { repository.deleteStudyTask(it.id) }
                }

                tasks.forEach { task ->
                    val dayCode = when (task.dayOfWeek.take(3).uppercase()) {
                        "MON" -> "MON"
                        "TUE" -> "TUE"
                        "WED" -> "WED"
                        "THU" -> "THU"
                        "FRI" -> "FRI"
                        "SAT" -> "SAT"
                        "SUN" -> "SUN"
                        else -> "DAILY"
                    }
                    val parts = task.timeSlot.split("-").map { it.trim() }
                    val sTime = parts.getOrNull(0) ?: "07:00 PM"
                    val eTime = parts.getOrNull(1) ?: "07:45 PM"

                    repository.createStudyTask(
                        subjectName = task.subjectName,
                        taskTitle = task.taskTitle,
                        taskType = task.taskType,
                        subDetails = "${task.dayOfWeek} Focus Plan",
                        startTime = sTime,
                        endTime = eTime,
                        durationMinutes = task.durationMinutes,
                        daysOfWeek = dayCode,
                        repeatWeekly = true
                    )
                }

                updateDailyTargetMinutes(dailyTargetMinutes)

                addNotification(
                    title = "📅 Weekly study plan activated",
                    message = "Created ${tasks.size} scheduled tasks for your week with a ${dailyTargetMinutes}m daily target.",
                    type = "PLAN_UPDATED",
                    actionType = "VIEW_PLAN"
                )

                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    /**
     * Persists exam strategy settings and notifications to Room DB
     */
    fun saveWorkflowExamStrategy(
        strategy: com.example.ui.editor.ExamStrategyResult,
        dailyTargetMinutes: Int,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                updateDailyTargetMinutes(dailyTargetMinutes)
                addNotification(
                    title = "🎯 Strategy activated for ${strategy.examName}",
                    message = strategy.summary,
                    type = "STRATEGY_SET",
                    actionType = "VIEW_PLAN"
                )
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    /**
     * Persists structured notes to Room DB (creates Subject/Chapter if needed, or appends to existing)
     */
    fun saveStructuredNoteToLibrary(
        subjectName: String,
        chapterTitle: String,
        markdownNotes: String,
        existingSubjectId: String? = null,
        existingChapterId: String? = null,
        existingExamId: String? = null,
        onComplete: (subject: SubjectEntity, chapter: ChapterEntity) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val result = repository.saveStructuredNoteToLibrary(
                    subjectName = subjectName,
                    chapterTitle = chapterTitle,
                    markdownNotes = markdownNotes,
                    existingSubjectId = existingSubjectId,
                    existingChapterId = existingChapterId,
                    existingExamId = existingExamId
                )
                addNotification(
                    title = "📝 Notes Saved: ${result.second.title}",
                    message = "Saved to ${result.first.name} in your study library.",
                    type = "NOTE_SAVED",
                    actionType = "VIEW_NOTE"
                )
                onComplete(result.first, result.second)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (currentInstance == this) {
            currentInstance = null
        }
    }
}

data class ActiveFocusSession(
    val isActive: Boolean = false,
    val sessionId: String = "",
    val taskTitle: String = "Focus Session",
    val subjectName: String = "PrepOS Focus",
    val subjectId: String? = null,
    val chapterId: String? = null,
    val chapterTitle: String = "",
    val totalSeconds: Int = 45 * 60,
    val remainingSeconds: Int = 45 * 60,
    val elapsedSeconds: Int = 0,
    val isPaused: Boolean = false,
    val associatedTaskId: String? = null,
    val startTimeEpochMs: Long = 0L,
    val lastPauseEpochMs: Long = 0L,
    val accumulatedPauseMs: Long = 0L,
    val status: String = "IDLE", // IDLE, RUNNING, PAUSED, INTERRUPTED, COMPLETED
    val isInterrupted: Boolean = false,
    val recoverableElapsedSeconds: Int = 0
) {
    val isTimerMode: Boolean get() = totalSeconds > 0
}

