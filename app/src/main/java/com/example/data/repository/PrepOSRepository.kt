package com.example.data.repository

import com.example.data.db.PrepOSDatabase
import com.example.data.db.SeedDataProvider
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.NoteDocumentEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.UserPreferencesEntity
import com.example.model.DocElement
import com.example.model.ElementType
import com.example.model.PrepDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BackupExportOptions(
    val exportSubjectsAndNotes: Boolean = true,
    val exportQuestionsBank: Boolean = true,
    val exportTimetableTasks: Boolean = true,
    val exportTestAttempts: Boolean = true,
    val exportProgressAndStreaks: Boolean = true,
    val exportPreferencesAndGoals: Boolean = true
)

class PrepOSRepository(private val db: PrepOSDatabase) {
    private val examDao = db.examDao()
    private val subjectDao = db.subjectDao()
    private val chapterDao = db.chapterDao()
    private val docDao = db.noteDocumentDao()
    private val prefsDao = db.userPreferencesDao()
    private val studyTaskDao = db.studyTaskDao()
    private val taskCompletionDao = db.taskCompletionDao()
    private val smartNotificationDao = db.smartNotificationDao()
    private val testAttemptDao = db.testAttemptDao()
    private val activeTestSessionDao = db.activeTestSessionDao()
    private val activeFocusSessionDao = db.activeFocusSessionDao()
    private val dailyStudyLogDao = db.dailyStudyLogDao()
    private val progressXpDao = db.progressXpDao()

    // --- Progression & XP Ledger ---
    val allXpEvents: Flow<List<com.example.data.entity.ProgressXpEntity>> = progressXpDao.observeAllEvents()
    val recentXpEvents: Flow<List<com.example.data.entity.ProgressXpEntity>> = progressXpDao.observeRecentEvents(30)
    val totalVerifiedXp: Flow<Long> = progressXpDao.observeTotalXp()

    suspend fun getTotalXpSync(): Long = withContext(Dispatchers.IO) {
        progressXpDao.getTotalXpSync()
    }

    suspend fun getTodayStudyXpSync(startOfDayEpochMs: Long): Long = withContext(Dispatchers.IO) {
        progressXpDao.getTodayStudyXpSync(startOfDayEpochMs)
    }

    suspend fun getTodayTotalXpSync(startOfDayEpochMs: Long): Long = withContext(Dispatchers.IO) {
        progressXpDao.getTodayTotalXpSync(startOfDayEpochMs)
    }

    suspend fun recordXpEvent(event: com.example.data.entity.ProgressXpEntity): Boolean = withContext(Dispatchers.IO) {
        val rowId = progressXpDao.insertEvent(event)
        rowId != -1L
    }

    suspend fun recordXpEvents(events: List<com.example.data.entity.ProgressXpEntity>): Int = withContext(Dispatchers.IO) {
        val rowIds = progressXpDao.insertEvents(events)
        rowIds.count { it != -1L }
    }

    suspend fun getAllXpEventsSync(): List<com.example.data.entity.ProgressXpEntity> = withContext(Dispatchers.IO) {
        progressXpDao.getAllEventsSync()
    }

    suspend fun hasXpEvent(id: String): Boolean = withContext(Dispatchers.IO) {
        progressXpDao.hasEvent(id)
    }

    // --- Daily Study History Logs (Persistent Per-Date Study Tracking) ---
    val allDailyStudyLogs: Flow<List<com.example.data.entity.DailyStudyLogEntity>> =
        dailyStudyLogDao.observeAllLogs()

    fun getDailyStudyLog(dateKey: String): Flow<com.example.data.entity.DailyStudyLogEntity?> =
        dailyStudyLogDao.observeLogForDate(dateKey)

    suspend fun getDailyStudyLogSync(dateKey: String): com.example.data.entity.DailyStudyLogEntity? = withContext(Dispatchers.IO) {
        dailyStudyLogDao.getLogForDate(dateKey)
    }

    suspend fun getAllDailyStudyLogsSync(): List<com.example.data.entity.DailyStudyLogEntity> = withContext(Dispatchers.IO) {
        dailyStudyLogDao.getAllLogsSync()
    }

    suspend fun saveDailyStudyLog(log: com.example.data.entity.DailyStudyLogEntity) = withContext(Dispatchers.IO) {
        dailyStudyLogDao.saveLog(log)
    }

    suspend fun recordDailyStudyMinutes(
        dateKey: String,
        minutes: Int,
        dailyTargetMinutes: Int = 45,
        isTargetMet: Boolean = false,
        subjectName: String? = null,
        deltaMinutes: Int = 0
    ) = withContext(Dispatchers.IO) {
        val existing = dailyStudyLogDao.getLogForDate(dateKey)
        val currentMins = existing?.totalFocusedMinutes ?: 0
        val newTotalMins = if (minutes > 0) maxOf(currentMins, minutes) else (currentMins + deltaMinutes).coerceAtLeast(0)
        val targetMet = isTargetMet || (newTotalMins >= dailyTargetMinutes)

        val map = try {
            val jsonStr = existing?.subjectsBreakdownJson ?: "{}"
            val jsonObj = org.json.JSONObject(jsonStr)
            val resMap = mutableMapOf<String, Int>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                resMap[k] = jsonObj.optInt(k, 0)
            }
            resMap
        } catch (e: Exception) {
            mutableMapOf<String, Int>()
        }

        if (!subjectName.isNullOrBlank()) {
            val added = if (deltaMinutes > 0) deltaMinutes else minutes
            val prev = map.getOrDefault(subjectName, 0)
            map[subjectName] = maxOf(prev, added)
        }

        val updatedJson = org.json.JSONObject(map as Map<*, *>).toString()
        val logEntity = com.example.data.entity.DailyStudyLogEntity(
            dateKey = dateKey,
            totalFocusedMinutes = newTotalMins,
            dailyTargetMinutes = dailyTargetMinutes,
            isTargetMet = targetMet,
            subjectsBreakdownJson = updatedJson,
            updatedAt = System.currentTimeMillis()
        )
        dailyStudyLogDao.saveLog(logEntity)
    }

    // --- Active Focus Sessions (Crash-Proof & Timestamp-Driven) ---

    val activeFocusSession: Flow<com.example.data.entity.ActiveFocusSessionEntity?> =
        activeFocusSessionDao.observeActiveFocusSession()

    suspend fun getActiveFocusSessionSync(): com.example.data.entity.ActiveFocusSessionEntity? = withContext(Dispatchers.IO) {
        activeFocusSessionDao.getActiveFocusSession()
    }

    suspend fun saveActiveFocusSession(session: com.example.data.entity.ActiveFocusSessionEntity) = withContext(Dispatchers.IO) {
        activeFocusSessionDao.saveActiveFocusSession(session)
    }

    suspend fun updateFocusStatus(status: String, lastPauseEpoch: Long, accumulatedPause: Long) = withContext(Dispatchers.IO) {
        activeFocusSessionDao.updateStatus(status, lastPauseEpoch, accumulatedPause)
    }

    suspend fun updateFocusHeartbeat(timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        activeFocusSessionDao.updateHeartbeat(timestamp)
    }

    suspend fun clearActiveFocusSession() = withContext(Dispatchers.IO) {
        activeFocusSessionDao.clearActiveFocusSession()
    }

    suspend fun finalizeFocusSession(
        newTodayMinutes: Int,
        dailyTargetMinutes: Int,
        targetDateKey: String,
        isTargetNowMet: Boolean,
        newStreak: Int,
        bestStreak: Int,
        lastActiveDate: String,
        associatedTaskId: String?,
        markTaskCompleted: Boolean = false,
        sessionMinutes: Int = 0
    ) = withContext(Dispatchers.IO) {
        val currentPrefs = prefsDao.getPreferences() ?: UserPreferencesEntity()
        val yesterdayKey = try {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        } catch (e: Exception) { "" }

        val yesterdayMins = when (currentPrefs.lastStudyDate) {
            yesterdayKey -> currentPrefs.todayFocusedMinutes
            targetDateKey -> currentPrefs.yesterdayFocusedMinutes
            else -> currentPrefs.yesterdayFocusedMinutes
        }

        prefsDao.savePreferences(
            currentPrefs.copy(
                dailyTargetMinutes = dailyTargetMinutes,
                todayFocusedMinutes = newTodayMinutes,
                yesterdayFocusedMinutes = yesterdayMins,
                lastStudyDate = targetDateKey,
                lastDailyTargetDate = if (isTargetNowMet) targetDateKey else currentPrefs.lastDailyTargetDate,
                currentStreak = newStreak,
                bestStreak = maxOf(currentPrefs.bestStreak, bestStreak),
                lastActiveDate = if (isTargetNowMet) targetDateKey else currentPrefs.lastActiveDate
            )
        )
        if (associatedTaskId != null && markTaskCompleted) {
            val record = com.example.data.entity.TaskCompletionEntity(
                id = "${associatedTaskId}_$targetDateKey",
                taskId = associatedTaskId,
                dateKey = targetDateKey,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
            taskCompletionDao.setCompletion(record)
        }

        // Persist into daily study log
        val taskSubject = if (associatedTaskId != null) {
            studyTaskDao.getTaskById(associatedTaskId)?.subjectName
        } else null
        recordDailyStudyMinutes(
            dateKey = targetDateKey,
            minutes = newTodayMinutes,
            dailyTargetMinutes = dailyTargetMinutes,
            isTargetMet = isTargetNowMet,
            subjectName = taskSubject ?: "Focus Mode"
        )

        // Award verified XP through Unified Progression Engine
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val currentStudyXpToday = progressXpDao.getTodayStudyXpSync(startOfDay)
        val deltaMins = if (sessionMinutes > 0) sessionMinutes else (newTodayMinutes - currentPrefs.todayFocusedMinutes).coerceAtLeast(0)

        if (deltaMins > 0) {
            val studyEvents = com.example.util.ProgressionEngine.createStudySessionEvents(
                minutes = deltaMins,
                isRevision = false,
                sessionId = "focus_${System.currentTimeMillis()}",
                currentStudyXpToday = currentStudyXpToday
            )
            progressXpDao.insertEvents(studyEvents)
            val streakDayEvent = com.example.util.ProgressionEngine.createDailyStreakDayEvent(targetDateKey)
            progressXpDao.insertEvent(streakDayEvent)
        }

        if (isTargetNowMet) {
            val targetEvent = com.example.util.ProgressionEngine.createDailyTargetEvent(targetDateKey)
            progressXpDao.insertEvent(targetEvent)
        }

        com.example.util.ProgressionEngine.createStreakMilestoneEvent(newStreak)?.let {
            progressXpDao.insertEvent(it)
        }

        activeFocusSessionDao.clearActiveFocusSession()
    }


    // --- Tests & Practice Attempts ---
    val allTestAttempts: Flow<List<com.example.data.entity.TestAttemptEntity>> = testAttemptDao.getAllAttempts()
    val testCount: Flow<Int> = testAttemptDao.observeTestCount()
    val avgScore: Flow<Float?> = testAttemptDao.observeAvgScore()
    val bestScore: Flow<Int?> = testAttemptDao.observeBestScore()
    val avgAccuracy: Flow<Float?> = testAttemptDao.observeAvgAccuracy()
    val activeTestSession: Flow<com.example.data.entity.ActiveTestSessionEntity?> = activeTestSessionDao.observeActiveSession()

    fun getRecentAttempts(limit: Int = 10): Flow<List<com.example.data.entity.TestAttemptEntity>> =
        testAttemptDao.getRecentAttempts(limit)

    suspend fun getTestAttemptById(id: String): com.example.data.entity.TestAttemptEntity? = withContext(Dispatchers.IO) {
        testAttemptDao.getAttemptById(id)
    }

    suspend fun saveTestAttempt(attempt: com.example.data.entity.TestAttemptEntity) = withContext(Dispatchers.IO) {
        testAttemptDao.insertAttempt(attempt)
        val events = com.example.util.ProgressionEngine.createTestCompletionEvents(attempt)
        progressXpDao.insertEvents(events)
    }

    suspend fun finalizeTestAttempt(
        attempt: com.example.data.entity.TestAttemptEntity,
        chapterId: String? = null,
        attemptedCount: Int = 0,
        correctCount: Int = 0
    ) = withContext(Dispatchers.IO) {
        testAttemptDao.insertAttempt(attempt)
        if (chapterId != null && attemptedCount > 0) {
            chapterDao.updateTestScore(chapterId, attemptedCount, correctCount)
        }
        activeTestSessionDao.clearActiveSession()
        val events = com.example.util.ProgressionEngine.createTestCompletionEvents(attempt)
        progressXpDao.insertEvents(events)
    }

    suspend fun deleteTestAttempt(id: String) = withContext(Dispatchers.IO) {
        testAttemptDao.deleteAttemptById(id)
    }

    suspend fun clearAllTestAttempts() = withContext(Dispatchers.IO) {
        testAttemptDao.clearAllAttempts()
    }

    suspend fun getActiveTestSessionSync(): com.example.data.entity.ActiveTestSessionEntity? = withContext(Dispatchers.IO) {
        activeTestSessionDao.getActiveSession()
    }

    suspend fun saveActiveTestSession(session: com.example.data.entity.ActiveTestSessionEntity) = withContext(Dispatchers.IO) {
        activeTestSessionDao.saveActiveSession(session)
    }

    suspend fun clearActiveTestSession() = withContext(Dispatchers.IO) {
        activeTestSessionDao.clearActiveSession()
    }

    // --- Smart In-App Notifications ---
    val allNotifications: Flow<List<com.example.data.entity.SmartNotificationEntity>> =
        smartNotificationDao.observeActiveNotifications()
    val allSmartNotifications: Flow<List<com.example.data.entity.SmartNotificationEntity>> = allNotifications
    val unreadNotificationCount: Flow<Int> =
        smartNotificationDao.observeUnreadCount()

    suspend fun addNotification(notification: com.example.data.entity.SmartNotificationEntity) = withContext(Dispatchers.IO) {
        smartNotificationDao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        smartNotificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        smartNotificationDao.markAllAsRead()
    }

    suspend fun dismissNotification(id: String) = withContext(Dispatchers.IO) {
        smartNotificationDao.dismissNotification(id)
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        smartNotificationDao.dismissNotification(id)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        smartNotificationDao.dismissAllNotifications()
    }

    suspend fun getRecentNotificationsSync(): List<com.example.data.entity.SmartNotificationEntity> = withContext(Dispatchers.IO) {
        smartNotificationDao.getActiveNotificationsSync()
    }

    // --- Study Tasks & Habit Timetable ---
    val allStudyTasks: Flow<List<com.example.data.entity.StudyTaskEntity>> = studyTaskDao.getAllTasks()
    val allTaskCompletions: Flow<List<com.example.data.entity.TaskCompletionEntity>> = taskCompletionDao.observeAllCompletions()

    suspend fun getStudyTasksSync(): List<com.example.data.entity.StudyTaskEntity> = withContext(Dispatchers.IO) {
        studyTaskDao.getAllTasksSync()
    }

    fun getCompletionsForDate(dateKey: String): Flow<List<com.example.data.entity.TaskCompletionEntity>> =
        taskCompletionDao.observeCompletionsForDate(dateKey)

    suspend fun createStudyTask(
        subjectId: String? = null,
        subjectName: String,
        taskTitle: String,
        taskType: String = "READING",
        subDetails: String = "",
        startTime: String = "7:00 PM",
        endTime: String = "7:45 PM",
        durationMinutes: Int = 45,
        daysOfWeek: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
        repeatWeekly: Boolean = true
    ): com.example.data.entity.StudyTaskEntity = withContext(Dispatchers.IO) {
        val task = com.example.data.entity.StudyTaskEntity(
            id = "task_${UUID.randomUUID().toString().take(8)}",
            subjectId = subjectId,
            subjectName = subjectName.trim(),
            taskTitle = taskTitle.trim(),
            taskType = taskType,
            subDetails = subDetails.trim(),
            startTime = startTime.trim(),
            endTime = endTime.trim(),
            durationMinutes = durationMinutes,
            daysOfWeek = daysOfWeek,
            repeatWeekly = repeatWeekly,
            orderIndex = System.currentTimeMillis().toInt()
        )
        studyTaskDao.insertTask(task)
        task
    }

    suspend fun updateStudyTask(task: com.example.data.entity.StudyTaskEntity) = withContext(Dispatchers.IO) {
        studyTaskDao.updateTask(task)
    }

    suspend fun deleteStudyTask(taskId: String) = withContext(Dispatchers.IO) {
        studyTaskDao.deleteTaskById(taskId)
    }

    suspend fun getTaskCompletionsForDate(dateKey: String): List<com.example.data.entity.TaskCompletionEntity> = withContext(Dispatchers.IO) {
        taskCompletionDao.getCompletionsForDate(dateKey)
    }

    suspend fun toggleTaskCompletion(taskId: String, dateKey: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        if (isCompleted) {
            val record = com.example.data.entity.TaskCompletionEntity(
                id = "${taskId}_$dateKey",
                taskId = taskId,
                dateKey = dateKey,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
            taskCompletionDao.setCompletion(record)
            val task = studyTaskDao.getTaskById(taskId)
            val allCompletions = taskCompletionDao.getCompletionsForDate(dateKey)
            val allTasks = studyTaskDao.getAllTasksSync()
            val totalMins = allTasks.filter { t -> allCompletions.any { it.taskId == t.id && it.isCompleted } }.sumOf { it.durationMinutes }
            recordDailyStudyMinutes(
                dateKey = dateKey,
                minutes = totalMins,
                subjectName = task?.subjectName
            )
        } else {
            taskCompletionDao.removeCompletion(taskId, dateKey)
        }
    }


    suspend fun updateFocusSettings(
        appPinning: Boolean,
        dnd: Boolean,
        notificationBehaviour: String,
        timerMinutes: Int,
        followTimetable: Boolean
    ) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(
            current.copy(
                focusAppPinningEnabled = appPinning,
                focusDndEnabled = dnd,
                focusNotificationBehaviour = notificationBehaviour,
                focusTimerMinutes = timerMinutes,
                focusFollowTimetable = followTimetable
            )
        )
    }

    suspend fun updateStreak(currentStreak: Int, bestStreak: Int, lastActiveDate: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(
            current.copy(
                currentStreak = currentStreak,
                bestStreak = maxOf(current.bestStreak, bestStreak),
                lastActiveDate = lastActiveDate
            )
        )
    }


    // --- Exams ---
    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()

    suspend fun createExam(name: String, code: String, colorHex: String = "#2563EB"): ExamEntity = withContext(Dispatchers.IO) {
        val count = examDao.getAllExams().firstOrNull()?.size ?: 0
        val exam = ExamEntity(
            id = "exam_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            code = code.trim(),
            colorHex = colorHex,
            orderIndex = count
        )
        examDao.insertExam(exam)
        exam
    }

    suspend fun updateExam(exam: ExamEntity) = withContext(Dispatchers.IO) {
        examDao.updateExam(exam)
    }

    suspend fun deleteExam(examId: String) = withContext(Dispatchers.IO) {
        examDao.deleteExamById(examId)
    }

    // --- Subjects ---
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()

    fun getSubjectsForExam(examId: String): Flow<List<SubjectEntity>> = subjectDao.getSubjectsByExam(examId)

    fun getStandaloneSubjects(): Flow<List<SubjectEntity>> = subjectDao.getStandaloneSubjects()

    suspend fun getSubject(subjectId: String): SubjectEntity? = withContext(Dispatchers.IO) {
        subjectDao.getSubjectById(subjectId)
    }

    suspend fun createSubject(
        name: String,
        examId: String? = null,
        iconName: String = "menu_book",
        colorHex: String = "#3B82F6"
    ): SubjectEntity = withContext(Dispatchers.IO) {
        val current = if (examId != null) subjectDao.getSubjectsByExam(examId).firstOrNull() ?: emptyList()
                      else subjectDao.getStandaloneSubjects().firstOrNull() ?: emptyList()
        val subject = SubjectEntity(
            id = "sub_${UUID.randomUUID().toString().take(8)}",
            examId = examId,
            name = name.trim(),
            iconName = iconName,
            colorHex = colorHex,
            orderIndex = current.size
        )
        subjectDao.insertSubject(subject)
        subject
    }

    suspend fun updateSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        subjectDao.updateSubject(subject)
    }

    suspend fun deleteSubject(subjectId: String) = withContext(Dispatchers.IO) {
        subjectDao.deleteSubjectById(subjectId)
    }

    // --- Chapters ---
    val allRecentChapters: Flow<List<ChapterEntity>> = chapterDao.getAllRecentChapters()
    val allChapters: Flow<List<ChapterEntity>> = chapterDao.getAllChapters()

    suspend fun getAllChaptersSync(): List<ChapterEntity> = withContext(Dispatchers.IO) {
        chapterDao.getAllChaptersSync()
    }

    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>> = chapterDao.getChaptersBySubject(subjectId)

    suspend fun getChapter(chapterId: String): ChapterEntity? = withContext(Dispatchers.IO) {
        chapterDao.getChapterById(chapterId)
    }

    fun observeChapter(chapterId: String): Flow<ChapterEntity?> = chapterDao.observeChapterById(chapterId)

    suspend fun createChapter(
        subjectId: String,
        title: String,
        summary: String = "",
        chapterNumber: Int = 0
    ): ChapterEntity = withContext(Dispatchers.IO) {
        val currentChapters = chapterDao.getChaptersBySubject(subjectId).firstOrNull() ?: emptyList()
        val chapterId = "chap_${UUID.randomUUID().toString().take(8)}"
        val chapter = ChapterEntity(
            id = chapterId,
            subjectId = subjectId,
            title = title.trim(),
            chapterNumber = if (chapterNumber > 0) chapterNumber else (currentChapters.size + 1),
            summary = summary.trim(),
            orderIndex = currentChapters.size,
            readingProgress = 0.0f
        )
        chapterDao.insertChapter(chapter)

        // Initialize empty document
        val initialDoc = PrepDocument.createInitialDocument(chapterId, title.trim())
        val defaultPrefs = prefsDao.getPreferences()
        docDao.insertOrUpdate(
            NoteDocumentEntity(
                chapterId = chapterId,
                contentJson = initialDoc.toJson(),
                paperStyle = defaultPrefs?.defaultPaperStyle ?: "RULED",
                fontFamily = defaultPrefs?.defaultFontFamily ?: "SANS_SERIF",
                fontSizeSp = defaultPrefs?.defaultFontSizeSp ?: 16f,
                lineSpacingMultiplier = defaultPrefs?.defaultLineSpacing ?: 1.4f,
                themeMode = defaultPrefs?.defaultThemeMode ?: "PAPER_LIGHT"
            )
        )
        chapter
    }

    suspend fun createChapterWithSanitizedContent(
        subjectId: String,
        title: String,
        summary: String,
        chapterNumber: Int,
        elements: List<com.example.model.DocElement>,
        questions: List<com.example.model.QuestionItem> = emptyList()
    ): ChapterEntity = withContext(Dispatchers.IO) {
        val currentChapters = chapterDao.getChaptersBySubject(subjectId).firstOrNull() ?: emptyList()
        val chapterId = "chap_${UUID.randomUUID().toString().take(8)}"
        
        val questionsArray = org.json.JSONArray()
        questions.forEach { q -> questionsArray.put(q.toJson()) }

        val chapter = ChapterEntity(
            id = chapterId,
            subjectId = subjectId,
            title = title.trim().ifBlank { "Untitled Chapter" },
            chapterNumber = if (chapterNumber > 0) chapterNumber else (currentChapters.size + 1),
            summary = summary.trim(),
            questionsJson = questionsArray.toString(),
            orderIndex = currentChapters.size,
            readingProgress = 0.0f
        )
        chapterDao.insertChapter(chapter)

        val doc = PrepDocument(
            chapterId = chapterId,
            title = title.trim().ifBlank { "Untitled Chapter" },
            elements = elements.toMutableList()
        )
        val defaultPrefs = prefsDao.getPreferences()
        docDao.insertOrUpdate(
            NoteDocumentEntity(
                chapterId = chapterId,
                contentJson = doc.toJson(),
                paperStyle = defaultPrefs?.defaultPaperStyle ?: "RULED",
                fontFamily = defaultPrefs?.defaultFontFamily ?: "SANS_SERIF",
                fontSizeSp = defaultPrefs?.defaultFontSizeSp ?: 16f,
                lineSpacingMultiplier = defaultPrefs?.defaultLineSpacing ?: 1.4f,
                themeMode = defaultPrefs?.defaultThemeMode ?: "PAPER_LIGHT"
            )
        )
        chapter
    }

    suspend fun updateChapter(chapter: ChapterEntity) = withContext(Dispatchers.IO) {
        chapterDao.updateChapter(chapter)
    }

    suspend fun updateTestScore(chapterId: String, attempted: Int, correct: Int) = withContext(Dispatchers.IO) {
        chapterDao.updateTestScore(chapterId, attempted, correct)
    }

    suspend fun updateReadingProgress(chapterId: String, progress: Float, scrollY: Int) = withContext(Dispatchers.IO) {
        val clamped = progress.coerceIn(0f, 1f)
        chapterDao.updateReadingProgress(chapterId, clamped, scrollY)
        if (clamped >= 0.98f) {
            val chapter = chapterDao.getChapterById(chapterId)
            val title = chapter?.title ?: "Chapter"
            val event = com.example.util.ProgressionEngine.createChapterCompletionEvent(chapterId, title)
            progressXpDao.insertEvent(event)
        }
    }

    suspend fun updateChapterQuestions(chapterId: String, questions: List<com.example.model.QuestionItem>) = withContext(Dispatchers.IO) {
        val jsonArray = org.json.JSONArray()
        questions.forEach { q ->
            val obj = org.json.JSONObject().apply {
                put("id", q.id)
                put("questionText", q.questionText)
                put("options", org.json.JSONArray(q.options))
                put("correctOptionIndex", q.correctOptionIndex)
                put("explanation", q.explanation)
                put("difficulty", q.difficulty)
            }
            jsonArray.put(obj)
        }
        chapterDao.updateChapterQuestions(chapterId, jsonArray.toString())
    }

    suspend fun deleteChapter(chapterId: String) = withContext(Dispatchers.IO) {
        chapterDao.deleteChapterById(chapterId)
        docDao.deleteDocument(chapterId)
    }

    suspend fun saveStructuredNoteToLibrary(
        subjectName: String,
        chapterTitle: String,
        markdownNotes: String,
        existingSubjectId: String? = null,
        existingChapterId: String? = null,
        existingExamId: String? = null
    ): Pair<SubjectEntity, ChapterEntity> = withContext(Dispatchers.IO) {
        val allSubjects = subjectDao.getAllSubjects().firstOrNull() ?: emptyList()
        val subjectEntity: SubjectEntity = if (!existingSubjectId.isNullOrBlank()) {
            subjectDao.getSubjectById(existingSubjectId) ?: run {
                createSubject(
                    name = subjectName.trim().ifBlank { "General Studies" },
                    examId = existingExamId ?: prefsDao.getPreferences()?.defaultExamId,
                    iconName = "book",
                    colorHex = "#3B82F6"
                )
            }
        } else {
            val matched = allSubjects.find { it.name.equals(subjectName.trim(), ignoreCase = true) }
            matched ?: createSubject(
                name = subjectName.trim().ifBlank { "General Studies" },
                examId = existingExamId ?: prefsDao.getPreferences()?.defaultExamId,
                iconName = "book",
                colorHex = "#3B82F6"
            )
        }

        val sanitizedResult = com.example.ai.OfflineChapterSanitizer.sanitizeLocally(markdownNotes, chapterTitle.trim().ifBlank { "Structured Notes" })
        val finalTitle = sanitizedResult.title.ifBlank { chapterTitle.trim().ifBlank { "Structured Notes" } }
        val parsedBlocks = sanitizedResult.elements.toMutableList()

        val chapterEntity: ChapterEntity = if (!existingChapterId.isNullOrBlank()) {
            val existingDoc = docDao.getDocument(existingChapterId)
            if (existingDoc != null) {
                val doc = PrepDocument.fromJson(existingDoc.contentJson)
                val isPlaceholderOnly = doc.elements.isEmpty() || (doc.elements.size <= 2 && doc.elements.all { el ->
                    val tb = el as? DocElement.TextBlock
                    tb != null && (tb.text.isBlank() || tb.text.equals("Untitled Chapter", ignoreCase = true) || tb.text.equals("Untitled", ignoreCase = true))
                })

                val updatedElements = if (isPlaceholderOnly) {
                    parsedBlocks
                } else {
                    doc.elements.toMutableList().apply {
                        addAll(parsedBlocks)
                    }
                }

                docDao.insertOrUpdate(
                    existingDoc.copy(
                        contentJson = doc.copy(elements = updatedElements).toJson(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                val doc = PrepDocument(
                    chapterId = existingChapterId,
                    title = finalTitle,
                    elements = parsedBlocks
                )
                val defaultPrefs = prefsDao.getPreferences()
                docDao.insertOrUpdate(
                    NoteDocumentEntity(
                        chapterId = existingChapterId,
                        contentJson = doc.toJson(),
                        paperStyle = defaultPrefs?.defaultPaperStyle ?: "RULED",
                        fontFamily = defaultPrefs?.defaultFontFamily ?: "SANS_SERIF",
                        fontSizeSp = defaultPrefs?.defaultFontSizeSp ?: 16f,
                        lineSpacingMultiplier = defaultPrefs?.defaultLineSpacing ?: 1.4f,
                        themeMode = defaultPrefs?.defaultThemeMode ?: "PAPER_LIGHT"
                    )
                )
            }
            val existingChap = chapterDao.getChapterById(existingChapterId)
            if (existingChap != null) {
                val updatedChap = existingChap.copy(
                    title = if (existingChap.title.isBlank() || existingChap.title.equals("Untitled Chapter", ignoreCase = true)) finalTitle else existingChap.title,
                    summary = if (existingChap.summary.isBlank()) {
                        sanitizedResult.summary.ifBlank { if (markdownNotes.length > 120) markdownNotes.take(120) + "..." else markdownNotes }
                    } else existingChap.summary,
                    lastModified = System.currentTimeMillis()
                )
                chapterDao.insertChapter(updatedChap)
                updatedChap
            } else {
                createChapterWithSanitizedContent(
                    subjectId = subjectEntity.id,
                    title = finalTitle,
                    summary = sanitizedResult.summary.ifBlank { if (markdownNotes.length > 120) markdownNotes.take(120) + "..." else markdownNotes },
                    chapterNumber = 0,
                    elements = parsedBlocks
                )
            }
        } else {
            createChapterWithSanitizedContent(
                subjectId = subjectEntity.id,
                title = finalTitle,
                summary = sanitizedResult.summary.ifBlank { if (markdownNotes.length > 120) markdownNotes.take(120) + "..." else markdownNotes },
                chapterNumber = 0,
                elements = parsedBlocks
            )
        }

        Pair(subjectEntity, chapterEntity)
    }

    // --- Document ---
    suspend fun getDocument(chapterId: String): NoteDocumentEntity? = withContext(Dispatchers.IO) {
        docDao.getDocument(chapterId)
    }

    fun observeDocument(chapterId: String): Flow<NoteDocumentEntity?> = docDao.observeDocument(chapterId)

    suspend fun saveDocument(docEntity: NoteDocumentEntity) = withContext(Dispatchers.IO) {
        docDao.insertOrUpdate(docEntity.copy(updatedAt = System.currentTimeMillis()))
    }

    // --- Preferences ---
    val preferences: Flow<UserPreferencesEntity?> = prefsDao.observePreferences()
    val userPreferences: Flow<UserPreferencesEntity?> = preferences

    suspend fun getPreferencesSync(): UserPreferencesEntity = withContext(Dispatchers.IO) {
        prefsDao.getPreferences() ?: UserPreferencesEntity()
    }

    suspend fun updatePreferences(prefs: UserPreferencesEntity) = withContext(Dispatchers.IO) {
        prefsDao.savePreferences(prefs)
    }

    suspend fun updateApiKey(apiKey: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(apiKey = apiKey.trim()))
    }

    suspend fun updateAiModel(model: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(selectedAiModel = model.trim()))
    }

    suspend fun updateAiProviderSettings(provider: String, deepSeekKey: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(selectedAiProvider = provider, deepSeekApiKey = deepSeekKey.trim()))
    }

    suspend fun updateAiProvider(provider: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(selectedAiProvider = provider))
    }

    suspend fun updateDeepSeekApiKey(key: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(deepSeekApiKey = key.trim()))
    }

    suspend fun updateDailyTargetMinutes(minutes: Int) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(dailyTargetMinutes = minutes))
    }

    suspend fun updateAppThemeMode(themeMode: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(appThemeMode = themeMode))
    }

    suspend fun updateTodayFocusedMinutes(todayMinutes: Int, lastTargetDate: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(todayFocusedMinutes = todayMinutes, lastDailyTargetDate = lastTargetDate))
    }

    suspend fun updateDailyTargetProgress(
        dailyTargetMinutes: Int,
        todayFocusedMinutes: Int,
        yesterdayFocusedMinutes: Int = 0,
        lastStudyDate: String = "",
        lastDailyTargetDate: String
    ) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(
            current.copy(
                dailyTargetMinutes = dailyTargetMinutes,
                todayFocusedMinutes = todayFocusedMinutes,
                yesterdayFocusedMinutes = yesterdayFocusedMinutes,
                lastStudyDate = lastStudyDate,
                lastDailyTargetDate = lastDailyTargetDate
            )
        )
        if (lastStudyDate.isNotBlank() && todayFocusedMinutes > 0) {
            recordDailyStudyMinutes(
                dateKey = lastStudyDate,
                minutes = todayFocusedMinutes,
                dailyTargetMinutes = dailyTargetMinutes,
                isTargetMet = lastDailyTargetDate == lastStudyDate
            )
        }
    }


    suspend fun updateTodayStudyTime(
        todayMinutes: Int,
        yesterdayMinutes: Int,
        lastStudyDate: String,
        lastDailyTargetDate: String
    ) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(
            current.copy(
                todayFocusedMinutes = todayMinutes,
                yesterdayFocusedMinutes = yesterdayMinutes,
                lastStudyDate = lastStudyDate,
                lastDailyTargetDate = lastDailyTargetDate
            )
        )
    }

    suspend fun updateDefaultExamId(examId: String?) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(defaultExamId = examId))
    }

    suspend fun updateShowExamLabels(show: Boolean) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(showExamLabelsOnHome = show))
    }

    suspend fun updateShowExamFilters(show: Boolean) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(showExamFilters = show))
    }

    suspend fun updateDefaultPaperStyle(style: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(defaultPaperStyle = style))
    }

    suspend fun updateDefaultThemeMode(theme: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(defaultThemeMode = theme))
    }

    suspend fun updateDefaultFontFamily(font: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(defaultFontFamily = font))
    }

    suspend fun updateDefaultFontSize(size: Float) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(defaultFontSizeSp = size))
    }

    suspend fun updateDefaultLineSpacing(spacing: Float) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(defaultLineSpacing = spacing))
    }

    suspend fun updatePreferredUserName(name: String) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(current.copy(preferredUserName = name.trim()))
    }

    suspend fun updateTargetExamProfile(
        name: String,
        targetExam: String,
        targetDate: String,
        dailyHours: Float,
        scoreGoal: Int
    ) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(
            current.copy(
                preferredUserName = name.trim(),
                targetExamName = targetExam.trim(),
                targetExamDate = targetDate.trim(),
                targetDailyStudyHours = dailyHours,
                targetScoreGoal = scoreGoal
            )
        )
    }

    suspend fun updateAllReadingDefaults(
        style: String,
        theme: String,
        font: String,
        size: Float,
        lineSpacing: Float
    ) = withContext(Dispatchers.IO) {
        val current = getPreferencesSync()
        prefsDao.savePreferences(
            current.copy(
                defaultPaperStyle = style,
                defaultThemeMode = theme,
                defaultFontFamily = font,
                defaultFontSizeSp = size,
                defaultLineSpacing = lineSpacing
            )
        )
    }

    // --- Full Backup & Restore (JSON & ZIP) with Custom Selective Options ---
    suspend fun exportBackupJson(options: BackupExportOptions = BackupExportOptions()): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        val prefs = getPreferencesSync()
        if (options.exportPreferencesAndGoals || options.exportProgressAndStreaks) {
            val prefsObj = JSONObject().apply {
                if (options.exportPreferencesAndGoals) {
                    put("defaultPaperStyle", prefs.defaultPaperStyle)
                    put("defaultFontFamily", prefs.defaultFontFamily)
                    put("defaultFontSizeSp", prefs.defaultFontSizeSp.toDouble())
                    put("defaultThemeMode", prefs.defaultThemeMode)
                    put("defaultLineSpacing", prefs.defaultLineSpacing.toDouble())
                    put("selectedAiModel", prefs.selectedAiModel)
                    put("selectedAiProvider", prefs.selectedAiProvider)
                    put("defaultExamId", prefs.defaultExamId ?: "")
                    put("showExamLabelsOnHome", prefs.showExamLabelsOnHome)
                    put("showExamFilters", prefs.showExamFilters)
                    put("dailyTargetMinutes", prefs.dailyTargetMinutes)
                    put("preferredUserName", prefs.preferredUserName)
                    put("targetExamName", prefs.targetExamName)
                    put("targetExamDate", prefs.targetExamDate)
                    put("targetDailyStudyHours", prefs.targetDailyStudyHours.toDouble())
                    put("targetScoreGoal", prefs.targetScoreGoal)
                }
                if (options.exportProgressAndStreaks) {
                    put("todayFocusedMinutes", prefs.todayFocusedMinutes)
                    put("yesterdayFocusedMinutes", prefs.yesterdayFocusedMinutes)
                    put("lastStudyDate", prefs.lastStudyDate)
                    put("lastDailyTargetDate", prefs.lastDailyTargetDate)
                    put("currentStreak", prefs.currentStreak)
                    put("bestStreak", prefs.bestStreak)
                    put("lastActiveDate", prefs.lastActiveDate)
                }
            }
            root.put("preferences", prefsObj)
        }

        if (options.exportSubjectsAndNotes) {
            val exams = examDao.getAllExams().firstOrNull() ?: emptyList()
            val examsArr = JSONArray()
            exams.forEach { e ->
                examsArr.put(JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("code", e.code)
                    put("colorHex", e.colorHex)
                    put("orderIndex", e.orderIndex)
                })
            }
            root.put("exams", examsArr)

            val subjects = subjectDao.getAllSubjects().firstOrNull() ?: emptyList()
            val subArr = JSONArray()
            subjects.forEach { s ->
                subArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("examId", s.examId ?: "")
                    put("name", s.name)
                    put("iconName", s.iconName)
                    put("colorHex", s.colorHex)
                    put("orderIndex", s.orderIndex)
                })
            }
            root.put("subjects", subArr)

            val chapters = chapterDao.getAllRecentChapters().firstOrNull() ?: emptyList()
            val chapArr = JSONArray()
            val docArr = JSONArray()

            chapters.forEach { c ->
                chapArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("subjectId", c.subjectId)
                    put("title", c.title)
                    put("chapterNumber", c.chapterNumber)
                    put("summary", c.summary)
                    put("questionsJson", if (options.exportQuestionsBank) c.questionsJson else "[]")
                    put("orderIndex", c.orderIndex)
                    put("readingProgress", if (options.exportProgressAndStreaks) c.readingProgress.toDouble() else 0.0)
                    put("lastReadScrollY", c.lastReadScrollY)
                    put("lastModified", c.lastModified)
                })

                val doc = docDao.getDocument(c.id)
                if (doc != null) {
                    docArr.put(JSONObject().apply {
                        put("chapterId", doc.chapterId)
                        put("contentJson", doc.contentJson)
                        put("paperStyle", doc.paperStyle)
                        put("fontFamily", doc.fontFamily)
                        put("fontSizeSp", doc.fontSizeSp.toDouble())
                        put("lineSpacingMultiplier", doc.lineSpacingMultiplier.toDouble())
                        put("themeMode", doc.themeMode)
                    })
                }
            }
            root.put("chapters", chapArr)
            root.put("documents", docArr)
        }

        if (options.exportTimetableTasks) {
            val tasks = studyTaskDao.getAllTasks().firstOrNull() ?: emptyList()
            val taskArr = JSONArray()
            tasks.forEach { t ->
                taskArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("subjectId", t.subjectId ?: "")
                    put("subjectName", t.subjectName)
                    put("taskTitle", t.taskTitle)
                    put("taskType", t.taskType)
                    put("subDetails", t.subDetails)
                    put("startTime", t.startTime)
                    put("endTime", t.endTime)
                    put("durationMinutes", t.durationMinutes)
                    put("daysOfWeek", t.daysOfWeek)
                    put("repeatWeekly", t.repeatWeekly)
                    put("orderIndex", t.orderIndex)
                    put("createdAt", t.createdAt)
                })
            }
            root.put("studyTasks", taskArr)

            val completions = taskCompletionDao.observeAllCompletions().firstOrNull() ?: emptyList()
            val compArr = JSONArray()
            completions.forEach { comp ->
                compArr.put(JSONObject().apply {
                    put("id", comp.id)
                    put("taskId", comp.taskId)
                    put("dateKey", comp.dateKey)
                    put("isCompleted", comp.isCompleted)
                    put("completedAt", comp.completedAt)
                })
            }
            root.put("taskCompletions", compArr)
        }

        if (options.exportTestAttempts) {
            val attempts = testAttemptDao.getAllAttempts().firstOrNull() ?: emptyList()
            val attArr = JSONArray()
            attempts.forEach { att ->
                attArr.put(JSONObject().apply {
                    put("id", att.id)
                    put("testTitle", att.testTitle)
                    put("testType", att.testType)
                    put("subjectId", att.subjectId ?: "")
                    put("subjectName", att.subjectName)
                    put("chapterId", att.chapterId ?: "")
                    put("chapterTitle", att.chapterTitle)
                    put("topicId", att.topicId ?: "")
                    put("topicName", att.topicName)
                    put("totalQuestions", att.totalQuestions)
                    put("correctAnswers", att.correctAnswers)
                    put("wrongAnswers", att.wrongAnswers)
                    put("unanswered", att.unanswered)
                    put("scorePercentage", att.scorePercentage)
                    put("accuracy", att.accuracy.toDouble())
                    put("timeTakenSeconds", att.timeTakenSeconds)
                    put("totalTimeLimitSeconds", att.totalTimeLimitSeconds)
                    put("questionsJson", att.questionsJson)
                    put("aiAnalysisJson", att.aiAnalysisJson)
                    put("startedAt", att.startedAt)
                    put("completedAt", att.completedAt)
                })
            }
            root.put("testAttempts", attArr)
        }

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            val prefsObj = root.optJSONObject("preferences")
            if (prefsObj != null) {
                val current = getPreferencesSync()
                prefsDao.savePreferences(
                    current.copy(
                        defaultPaperStyle = prefsObj.optString("defaultPaperStyle", current.defaultPaperStyle),
                        defaultFontFamily = prefsObj.optString("defaultFontFamily", current.defaultFontFamily),
                        defaultFontSizeSp = prefsObj.optDouble("defaultFontSizeSp", current.defaultFontSizeSp.toDouble()).toFloat(),
                        defaultThemeMode = prefsObj.optString("defaultThemeMode", current.defaultThemeMode),
                        defaultLineSpacing = prefsObj.optDouble("defaultLineSpacing", current.defaultLineSpacing.toDouble()).toFloat(),
                        selectedAiModel = prefsObj.optString("selectedAiModel", current.selectedAiModel),
                        selectedAiProvider = prefsObj.optString("selectedAiProvider", current.selectedAiProvider),
                        defaultExamId = prefsObj.optString("defaultExamId").takeIf { it.isNotBlank() },
                        showExamLabelsOnHome = prefsObj.optBoolean("showExamLabelsOnHome", current.showExamLabelsOnHome),
                        showExamFilters = prefsObj.optBoolean("showExamFilters", current.showExamFilters),
                        dailyTargetMinutes = prefsObj.optInt("dailyTargetMinutes", current.dailyTargetMinutes),
                        preferredUserName = prefsObj.optString("preferredUserName", current.preferredUserName),
                        targetExamName = prefsObj.optString("targetExamName", current.targetExamName),
                        targetExamDate = prefsObj.optString("targetExamDate", current.targetExamDate),
                        targetDailyStudyHours = prefsObj.optDouble("targetDailyStudyHours", current.targetDailyStudyHours.toDouble()).toFloat(),
                        targetScoreGoal = prefsObj.optInt("targetScoreGoal", current.targetScoreGoal),
                        currentStreak = prefsObj.optInt("currentStreak", current.currentStreak),
                        bestStreak = prefsObj.optInt("bestStreak", current.bestStreak),
                        todayFocusedMinutes = prefsObj.optInt("todayFocusedMinutes", current.todayFocusedMinutes),
                        yesterdayFocusedMinutes = prefsObj.optInt("yesterdayFocusedMinutes", current.yesterdayFocusedMinutes),
                        lastStudyDate = prefsObj.optString("lastStudyDate", current.lastStudyDate),
                        lastDailyTargetDate = prefsObj.optString("lastDailyTargetDate", current.lastDailyTargetDate),
                        lastActiveDate = prefsObj.optString("lastActiveDate", current.lastActiveDate)
                    )
                )
            }

            val examsArr = root.optJSONArray("exams") ?: JSONArray()
            for (i in 0 until examsArr.length()) {
                val obj = examsArr.getJSONObject(i)
                examDao.insertExam(
                    ExamEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        code = obj.optString("code", ""),
                        colorHex = obj.optString("colorHex", "#2563EB"),
                        orderIndex = obj.optInt("orderIndex", i)
                    )
                )
            }

            val subArr = root.optJSONArray("subjects") ?: JSONArray()
            for (i in 0 until subArr.length()) {
                val obj = subArr.getJSONObject(i)
                val examId = obj.optString("examId").takeIf { it.isNotBlank() }
                subjectDao.insertSubject(
                    SubjectEntity(
                        id = obj.getString("id"),
                        examId = examId,
                        name = obj.getString("name"),
                        iconName = obj.optString("iconName", "menu_book"),
                        colorHex = obj.optString("colorHex", "#3B82F6"),
                        orderIndex = obj.optInt("orderIndex", i)
                    )
                )
            }

            val chapArr = root.optJSONArray("chapters") ?: JSONArray()
            for (i in 0 until chapArr.length()) {
                val obj = chapArr.getJSONObject(i)
                chapterDao.insertChapter(
                    ChapterEntity(
                        id = obj.getString("id"),
                        subjectId = obj.getString("subjectId"),
                        title = obj.getString("title"),
                        chapterNumber = obj.optInt("chapterNumber", 0),
                        summary = obj.optString("summary", ""),
                        questionsJson = obj.optString("questionsJson", "[]"),
                        orderIndex = obj.optInt("orderIndex", i),
                        readingProgress = obj.optDouble("readingProgress", 0.0).toFloat(),
                        lastReadScrollY = obj.optInt("lastReadScrollY", 0),
                        lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                    )
                )
            }

            val docArr = root.optJSONArray("documents") ?: JSONArray()
            for (i in 0 until docArr.length()) {
                val obj = docArr.getJSONObject(i)
                docDao.insertOrUpdate(
                    NoteDocumentEntity(
                        chapterId = obj.getString("chapterId"),
                        contentJson = obj.getString("contentJson"),
                        paperStyle = obj.optString("paperStyle", "RULED"),
                        fontFamily = obj.optString("fontFamily", "SANS_SERIF"),
                        fontSizeSp = obj.optDouble("fontSizeSp", 16.0).toFloat(),
                        lineSpacingMultiplier = obj.optDouble("lineSpacingMultiplier", 1.4).toFloat(),
                        themeMode = obj.optString("themeMode", "PAPER_LIGHT")
                    )
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getSubjectCount(): Int = withContext(Dispatchers.IO) {
        subjectDao.getSubjectCount()
    }

    /**
     * Clears all existing study subjects, chapters, notes, and exams.
     * Preserves user preferences and streaks.
     */
    suspend fun clearAllStudyContent() = withContext(Dispatchers.IO) {
        docDao.clearAllDocuments()
        chapterDao.clearAllChapters()
        subjectDao.clearAllSubjects()
        examDao.clearAllExams()
    }

    /**
     * Resets or overrides study library with the master JKSSB Constable content.
     */
    suspend fun reloadJkssbContent(context: android.content.Context): Boolean = withContext(Dispatchers.IO) {
        clearAllStudyContent()
        SeedDataProvider.syncOrUpdateSyllabus(db, context, force = true)
        true
    }
}
