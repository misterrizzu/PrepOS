package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.NoteDocumentEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: String)

    @Query("DELETE FROM exams")
    suspend fun clearAllExams()
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE examId = :examId ORDER BY orderIndex ASC, createdAt ASC")
    fun getSubjectsByExam(examId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE examId IS NULL ORDER BY orderIndex ASC, createdAt ASC")
    fun getStandaloneSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY CASE WHEN chapterNumber <= 0 THEN 999999 ELSE chapterNumber END ASC, orderIndex ASC, createdAt ASC")
    fun getChaptersBySubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY lastModified DESC")
    fun getAllRecentChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters")
    suspend fun getAllChaptersSync(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun observeChapterById(id: String): Flow<ChapterEntity?>

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun getChapterCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("UPDATE chapters SET readingProgress = :progress, lastReadScrollY = :scrollY, lastModified = :lastModified WHERE id = :id")
    suspend fun updateReadingProgress(id: String, progress: Float, scrollY: Int, lastModified: Long = System.currentTimeMillis())

    @Query("UPDATE chapters SET questionsJson = :questionsJson, lastModified = :lastModified WHERE id = :id")
    suspend fun updateChapterQuestions(id: String, questionsJson: String, lastModified: Long = System.currentTimeMillis())

    @Query("UPDATE chapters SET testAttemptedCount = :attempted, testCorrectCount = :correct, lastModified = :lastModified WHERE id = :id")
    suspend fun updateTestScore(id: String, attempted: Int, correct: Int, lastModified: Long = System.currentTimeMillis())

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: String)

    @Query("DELETE FROM chapters")
    suspend fun clearAllChapters()
}

@Dao
interface NoteDocumentDao {
    @Query("SELECT * FROM note_documents WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getDocument(chapterId: String): NoteDocumentEntity?

    @Query("SELECT * FROM note_documents WHERE chapterId = :chapterId LIMIT 1")
    fun observeDocument(chapterId: String): Flow<NoteDocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(document: NoteDocumentEntity)

    @Query("DELETE FROM note_documents WHERE chapterId = :chapterId")
    suspend fun deleteDocument(chapterId: String)

    @Query("DELETE FROM note_documents")
    suspend fun clearAllDocuments()
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE `key` = 'global_prefs' LIMIT 1")
    fun observePreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE `key` = 'global_prefs' LIMIT 1")
    suspend fun getPreferences(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: UserPreferencesEntity)
}

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllTasks(): Flow<List<com.example.data.entity.StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getAllTasksSync(): List<com.example.data.entity.StudyTaskEntity>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): com.example.data.entity.StudyTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: com.example.data.entity.StudyTaskEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<com.example.data.entity.StudyTaskEntity>)

    @Update
    suspend fun updateTask(task: com.example.data.entity.StudyTaskEntity)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)
}

@Dao
interface TaskCompletionDao {
    @Query("SELECT * FROM task_completions WHERE dateKey = :dateKey")
    fun observeCompletionsForDate(dateKey: String): Flow<List<com.example.data.entity.TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE dateKey = :dateKey")
    suspend fun getCompletionsForDate(dateKey: String): List<com.example.data.entity.TaskCompletionEntity>

    @Query("SELECT * FROM task_completions")
    fun observeAllCompletions(): Flow<List<com.example.data.entity.TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND dateKey = :dateKey LIMIT 1")
    suspend fun getCompletion(taskId: String, dateKey: String): com.example.data.entity.TaskCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCompletion(completion: com.example.data.entity.TaskCompletionEntity)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND dateKey = :dateKey")
    suspend fun removeCompletion(taskId: String, dateKey: String)
}

@Dao
interface SmartNotificationDao {
    @Query("SELECT * FROM smart_notifications WHERE isDismissed = 0 ORDER BY createdAt DESC")
    fun observeActiveNotifications(): Flow<List<com.example.data.entity.SmartNotificationEntity>>

    @Query("SELECT * FROM smart_notifications WHERE isDismissed = 0 ORDER BY createdAt DESC")
    suspend fun getActiveNotificationsSync(): List<com.example.data.entity.SmartNotificationEntity>

    @Query("SELECT COUNT(*) FROM smart_notifications WHERE isDismissed = 0 AND isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: com.example.data.entity.SmartNotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<com.example.data.entity.SmartNotificationEntity>)

    @Query("UPDATE smart_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE smart_notifications SET isRead = 1 WHERE isDismissed = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE smart_notifications SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissNotification(id: String)

    @Query("UPDATE smart_notifications SET isDismissed = 1")
    suspend fun dismissAllNotifications()

    @Query("DELETE FROM smart_notifications WHERE isDismissed = 1 OR createdAt < :olderThanTimestamp")
    suspend fun cleanOldNotifications(olderThanTimestamp: Long)
}

@Dao
interface TestAttemptDao {
    @Query("SELECT * FROM test_attempts ORDER BY completedAt DESC")
    fun getAllAttempts(): Flow<List<com.example.data.entity.TestAttemptEntity>>

    @Query("SELECT * FROM test_attempts ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentAttempts(limit: Int = 10): Flow<List<com.example.data.entity.TestAttemptEntity>>

    @Query("SELECT * FROM test_attempts WHERE id = :id LIMIT 1")
    suspend fun getAttemptById(id: String): com.example.data.entity.TestAttemptEntity?

    @Query("SELECT * FROM test_attempts WHERE id = :id LIMIT 1")
    fun observeAttemptById(id: String): Flow<com.example.data.entity.TestAttemptEntity?>

    @Query("SELECT COUNT(*) FROM test_attempts")
    fun observeTestCount(): Flow<Int>

    @Query("SELECT AVG(scorePercentage) FROM test_attempts")
    fun observeAvgScore(): Flow<Float?>

    @Query("SELECT MAX(scorePercentage) FROM test_attempts")
    fun observeBestScore(): Flow<Int?>

    @Query("SELECT AVG(accuracy) FROM test_attempts")
    fun observeAvgAccuracy(): Flow<Float?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: com.example.data.entity.TestAttemptEntity)

    @Update
    suspend fun updateAttempt(attempt: com.example.data.entity.TestAttemptEntity)

    @Query("DELETE FROM test_attempts WHERE id = :id")
    suspend fun deleteAttemptById(id: String)

    @Query("DELETE FROM test_attempts")
    suspend fun clearAllAttempts()
}

@Dao
interface ActiveTestSessionDao {
    @Query("SELECT * FROM active_test_sessions WHERE id = 'current_active_session' LIMIT 1")
    fun observeActiveSession(): Flow<com.example.data.entity.ActiveTestSessionEntity?>

    @Query("SELECT * FROM active_test_sessions WHERE id = 'current_active_session' LIMIT 1")
    suspend fun getActiveSession(): com.example.data.entity.ActiveTestSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveSession(session: com.example.data.entity.ActiveTestSessionEntity)

    @Query("DELETE FROM active_test_sessions WHERE id = 'current_active_session'")
    suspend fun clearActiveSession()
}

@Dao
interface ActiveFocusSessionDao {
    @Query("SELECT * FROM active_focus_sessions WHERE id = 'current_active_focus_session' LIMIT 1")
    fun observeActiveFocusSession(): Flow<com.example.data.entity.ActiveFocusSessionEntity?>

    @Query("SELECT * FROM active_focus_sessions WHERE id = 'current_active_focus_session' LIMIT 1")
    suspend fun getActiveFocusSession(): com.example.data.entity.ActiveFocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveFocusSession(session: com.example.data.entity.ActiveFocusSessionEntity)

    @Query("UPDATE active_focus_sessions SET lastHeartbeatEpochMs = :timestamp WHERE id = 'current_active_focus_session'")
    suspend fun updateHeartbeat(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE active_focus_sessions SET status = :status, lastPauseEpochMs = :lastPauseEpoch, accumulatedPauseMs = :accumulatedPause WHERE id = 'current_active_focus_session'")
    suspend fun updateStatus(status: String, lastPauseEpoch: Long, accumulatedPause: Long)

    @Query("DELETE FROM active_focus_sessions WHERE id = 'current_active_focus_session'")
    suspend fun clearActiveFocusSession()
}

@Dao
interface DailyStudyLogDao {
    @Query("SELECT * FROM daily_study_logs ORDER BY dateKey DESC")
    fun observeAllLogs(): Flow<List<com.example.data.entity.DailyStudyLogEntity>>

    @Query("SELECT * FROM daily_study_logs WHERE dateKey = :dateKey LIMIT 1")
    fun observeLogForDate(dateKey: String): Flow<com.example.data.entity.DailyStudyLogEntity?>

    @Query("SELECT * FROM daily_study_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getLogForDate(dateKey: String): com.example.data.entity.DailyStudyLogEntity?

    @Query("SELECT * FROM daily_study_logs")
    suspend fun getAllLogsSync(): List<com.example.data.entity.DailyStudyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLog(log: com.example.data.entity.DailyStudyLogEntity)

    @Query("DELETE FROM daily_study_logs WHERE dateKey = :dateKey")
    suspend fun deleteLogForDate(dateKey: String)
}

@Dao
interface ProgressXpDao {
    @Query("SELECT * FROM progress_xp_events ORDER BY createdAt DESC")
    fun observeAllEvents(): Flow<List<com.example.data.entity.ProgressXpEntity>>

    @Query("SELECT * FROM progress_xp_events ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 20): Flow<List<com.example.data.entity.ProgressXpEntity>>

    @Query("SELECT * FROM progress_xp_events ORDER BY createdAt DESC")
    suspend fun getAllEventsSync(): List<com.example.data.entity.ProgressXpEntity>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM progress_xp_events")
    fun observeTotalXp(): Flow<Long>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM progress_xp_events")
    suspend fun getTotalXpSync(): Long

    @Query("SELECT COALESCE(SUM(xp), 0) FROM progress_xp_events WHERE type IN ('STUDY_MINUTE', 'REVISION_SESSION') AND createdAt >= :startOfDayEpochMs")
    suspend fun getTodayStudyXpSync(startOfDayEpochMs: Long): Long

    @Query("SELECT COALESCE(SUM(xp), 0) FROM progress_xp_events WHERE createdAt >= :startOfDayEpochMs")
    suspend fun getTodayTotalXpSync(startOfDayEpochMs: Long): Long

    @Query("SELECT EXISTS(SELECT 1 FROM progress_xp_events WHERE id = :id)")
    suspend fun hasEvent(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: com.example.data.entity.ProgressXpEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<com.example.data.entity.ProgressXpEntity>): List<Long>

    @Query("DELETE FROM progress_xp_events")
    suspend fun clearAllEvents()
}


