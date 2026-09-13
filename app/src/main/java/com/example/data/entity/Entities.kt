package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val colorHex: String,
    val orderIndex: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val examId: String? = null, // Optional parent exam
    val name: String,
    val iconName: String = "menu_book",
    val colorHex: String = "#3B82F6",
    val orderIndex: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val chapterNumber: Int = 0,
    val summary: String = "",
    val questionsJson: String = "[]",
    val orderIndex: Int,
    val readingProgress: Float = 0.0f, // 0.0 to 1.0 (0% to 100%)
    val lastReadScrollY: Int = 0,
    val testAttemptedCount: Int = 0,
    val testCorrectCount: Int = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "note_documents")
data class NoteDocumentEntity(
    @PrimaryKey val chapterId: String,
    val contentJson: String,
    val paperStyle: String = "RULED", // PLAIN, RULED, GRID, DOTTED
    val fontFamily: String = "SANS_SERIF", // SANS_SERIF, SERIF, MONOSPACE, HANDWRITTEN
    val fontSizeSp: Float = 16f,
    val lineSpacingMultiplier: Float = 1.4f,
    val themeMode: String = "PAPER_LIGHT", // PAPER_LIGHT, WARM_SEPIA, SOFT_SLATE, AMOLED_DARK
    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val key: String = "global_prefs",
    val defaultPaperStyle: String = "RULED",
    val defaultFontFamily: String = "SANS_SERIF",
    val defaultFontSizeSp: Float = 16f,
    val defaultThemeMode: String = "PAPER_LIGHT",
    val defaultLineSpacing: Float = 1.4f,
    val apiKey: String = "",
    val selectedAiModel: String = "gemini-3.5-flash",
    val selectedAiProvider: String = "GEMINI", // GEMINI, DEEPSEEK, LOCAL_SYNTHESIZER
    val deepSeekApiKey: String = "",
    val defaultExamId: String? = null,
    val showExamLabelsOnHome: Boolean = true,
    val showExamFilters: Boolean = true,
    // Focus Mode Settings & Mandatory Daily Target
    val dailyTargetMinutes: Int = 45, // Mandatory daily study target in minutes
    val todayFocusedMinutes: Int = 0,
    val yesterdayFocusedMinutes: Int = 0,
    val lastStudyDate: String = "",
    val lastDailyTargetDate: String = "",
    val focusAppPinningEnabled: Boolean = true,
    val focusDndEnabled: Boolean = true,
    val focusNotificationBehaviour: String = "SILENT", // SILENT, VIBRATE
    val focusTimerMinutes: Int = 45, // 0 = off, 15, 25, 30, 45, 60, custom
    val focusFollowTimetable: Boolean = true,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String = "",
    val preferredUserName: String = "",
    val targetExamName: String = "JKSSB Junior Assistant",
    val targetExamDate: String = "2026-10-25",
    val targetDailyStudyHours: Float = 2.5f,
    val targetScoreGoal: Int = 85,
    val appThemeMode: String = "SYSTEM" // SYSTEM, LIGHT, DARK
)

@Entity(tableName = "test_attempts")
data class TestAttemptEntity(
    @PrimaryKey val id: String,
    val testTitle: String,
    val testType: String = "FULL_MOCK", // FULL_MOCK, SUBJECT_TEST, TOPIC_TEST, PREVIOUS_PAPER, CUSTOM_TEST, PRACTICE_TEST
    val subjectId: String? = null,
    val subjectName: String = "General Studies",
    val chapterId: String? = null,
    val chapterTitle: String = "",
    val topicId: String? = null,
    val topicName: String = "",
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val unanswered: Int = 0,
    val scorePercentage: Int = 0,
    val accuracy: Float = 0f,
    val timeTakenSeconds: Int = 0,
    val totalTimeLimitSeconds: Int = 0,
    val questionsJson: String = "[]",
    val aiAnalysisJson: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_test_sessions")
data class ActiveTestSessionEntity(
    @PrimaryKey val id: String = "current_active_session",
    val testTitle: String,
    val testType: String = "FULL_MOCK",
    val subjectId: String? = null,
    val subjectName: String = "General Studies",
    val chapterId: String? = null,
    val chapterTitle: String = "",
    val topicId: String? = null,
    val topicName: String = "",
    val currentQuestionIndex: Int = 0,
    val remainingTimeSeconds: Int = 1200,
    val totalTimeLimitSeconds: Int = 1200,
    val questionsJson: String = "[]",
    val startedAt: Long = System.currentTimeMillis(),
    val lastSavedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "smart_notifications")
data class SmartNotificationEntity(
    @PrimaryKey val id: String,
    val type: String, // "STUDY_REMINDER", "STREAK_PROGRESS", "TARGET_COMPLETED", "STREAK_WARNING", "ACHIEVEMENT", "MILESTONE"
    val title: String,
    val message: String,
    val actionType: String = "NONE", // "START_FOCUS", "OPEN_SUBJECT", "OPEN_CHAPTER", "VIEW_PLAN"
    val actionPayload: String? = null,
    val actionLabel: String = "Open",
    val isRead: Boolean = false,
    val isDismissed: Boolean = false,
    val priority: String = "NORMAL", // "HIGH", "NORMAL"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey val id: String,
    val subjectId: String? = null,
    val subjectName: String,
    val taskTitle: String,
    val taskType: String = "READING", // READING, QUIZ, TOPIC, REVISION, MOCK
    val subDetails: String = "",
    val startTime: String = "7:00 PM",
    val endTime: String = "7:45 PM",
    val durationMinutes: Int = 45,
    val daysOfWeek: String = "MON,TUE,WED,THU,FRI,SAT,SUN", // Days when task is active
    val repeatWeekly: Boolean = true,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_completions")
data class TaskCompletionEntity(
    @PrimaryKey val id: String, // e.g. "task1_2026-08-18"
    val taskId: String,
    val dateKey: String, // e.g. "2026-08-18"
    val isCompleted: Boolean = true,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_focus_sessions")
data class ActiveFocusSessionEntity(
    @PrimaryKey val id: String = "current_active_focus_session",
    val sessionId: String,
    val taskTitle: String = "Deep Study Session",
    val subjectName: String = "General Studies",
    val subjectId: String? = null,
    val chapterId: String? = null,
    val chapterTitle: String = "",
    val associatedTaskId: String? = null,
    val plannedDurationSeconds: Int = 45 * 60,
    val startTimeEpochMs: Long = System.currentTimeMillis(),
    val lastPauseEpochMs: Long = 0L,
    val accumulatedPauseMs: Long = 0L,
    val status: String = "RUNNING", // "RUNNING", "PAUSED", "COMPLETED", "INTERRUPTED", "DISCARDED"
    val lastHeartbeatEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_study_logs")
data class DailyStudyLogEntity(
    @PrimaryKey val dateKey: String, // e.g. "2026-09-03"
    val totalFocusedMinutes: Int = 0,
    val dailyTargetMinutes: Int = 45,
    val isTargetMet: Boolean = false,
    val subjectsBreakdownJson: String = "{}", // e.g. {"History": 60, "Math": 45}
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress_xp_events")
data class ProgressXpEntity(
    @PrimaryKey val id: String, // Deterministic ID preventing duplicate awards
    val type: String, // STUDY_MINUTE, PRACTICE_ATTEMPT, PRACTICE_CORRECT, TEST_COMPLETED, TEST_ACCURACY_BONUS, CHAPTER_COMPLETED, DAILY_TARGET_COMPLETED, REVISION_SESSION, STREAK_DAY, STREAK_MILESTONE, ACHIEVEMENT_UNLOCKED
    val xp: Int,
    val sourceId: String? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

