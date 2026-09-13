package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ActiveFocusSessionDao
import com.example.data.dao.ActiveTestSessionDao
import com.example.data.dao.ChapterDao
import com.example.data.dao.DailyStudyLogDao
import com.example.data.dao.ExamDao
import com.example.data.dao.NoteDocumentDao
import com.example.data.dao.ProgressXpDao
import com.example.data.dao.SmartNotificationDao
import com.example.data.dao.StudyTaskDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TaskCompletionDao
import com.example.data.dao.TestAttemptDao
import com.example.data.dao.UserPreferencesDao
import com.example.data.entity.ActiveFocusSessionEntity
import com.example.data.entity.ActiveTestSessionEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.DailyStudyLogEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.NoteDocumentEntity
import com.example.data.entity.ProgressXpEntity
import com.example.data.entity.SmartNotificationEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.TaskCompletionEntity
import com.example.data.entity.TestAttemptEntity
import com.example.data.entity.UserPreferencesEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExamEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        NoteDocumentEntity::class,
        UserPreferencesEntity::class,
        StudyTaskEntity::class,
        TaskCompletionEntity::class,
        SmartNotificationEntity::class,
        TestAttemptEntity::class,
        ActiveTestSessionEntity::class,
        ActiveFocusSessionEntity::class,
        DailyStudyLogEntity::class,
        ProgressXpEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class PrepOSDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun noteDocumentDao(): NoteDocumentDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun smartNotificationDao(): SmartNotificationDao
    abstract fun testAttemptDao(): TestAttemptDao
    abstract fun activeTestSessionDao(): ActiveTestSessionDao
    abstract fun activeFocusSessionDao(): ActiveFocusSessionDao
    abstract fun dailyStudyLogDao(): DailyStudyLogDao
    abstract fun progressXpDao(): ProgressXpDao

    companion object {
        @Volatile
        private var INSTANCE: PrepOSDatabase? = null

        // Safe migrations to preserve all user notes, chapters, and subjects
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add any missing columns to user_preferences if upgrading from v1
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN defaultExamId TEXT")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN showExamLabelsOnHome INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add study tasks and completion tables safely
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS study_tasks (
                            id TEXT PRIMARY KEY NOT NULL,
                            subjectId TEXT,
                            subjectName TEXT NOT NULL,
                            taskTitle TEXT NOT NULL,
                            taskType TEXT NOT NULL DEFAULT 'READING',
                            subDetails TEXT NOT NULL DEFAULT '',
                            startTime TEXT NOT NULL DEFAULT '7:00 PM',
                            endTime TEXT NOT NULL DEFAULT '7:45 PM',
                            durationMinutes INTEGER NOT NULL DEFAULT 45,
                            daysOfWeek TEXT NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN',
                            repeatWeekly INTEGER NOT NULL DEFAULT 1,
                            orderIndex INTEGER NOT NULL DEFAULT 0,
                            createdAt INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS task_completions (
                            id TEXT PRIMARY KEY NOT NULL,
                            taskId TEXT NOT NULL,
                            dateKey TEXT NOT NULL,
                            isCompleted INTEGER NOT NULL DEFAULT 1,
                            completedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    // Tables might already exist
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN focusAppPinningEnabled INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN focusDndEnabled INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN focusNotificationBehaviour TEXT NOT NULL DEFAULT 'SILENT'")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN focusTimerMinutes INTEGER NOT NULL DEFAULT 45")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN focusFollowTimetable INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN currentStreak INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN bestStreak INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN lastActiveDate TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Columns might already exist
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN showExamFilters INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN selectedAiProvider TEXT NOT NULL DEFAULT 'GEMINI'")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN deepSeekApiKey TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN dailyTargetMinutes INTEGER NOT NULL DEFAULT 45")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN todayFocusedMinutes INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN lastDailyTargetDate TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Columns might already exist
                }

                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS smart_notifications (
                            id TEXT PRIMARY KEY NOT NULL,
                            type TEXT NOT NULL,
                            title TEXT NOT NULL,
                            message TEXT NOT NULL,
                            actionType TEXT NOT NULL DEFAULT 'NONE',
                            actionPayload TEXT,
                            actionLabel TEXT NOT NULL DEFAULT 'Open',
                            isRead INTEGER NOT NULL DEFAULT 0,
                            isDismissed INTEGER NOT NULL DEFAULT 0,
                            priority TEXT NOT NULL DEFAULT 'NORMAL',
                            createdAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    // Table might already exist
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN preferredUserName TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN targetExamName TEXT NOT NULL DEFAULT 'JKSSB Junior Assistant'")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN targetExamDate TEXT NOT NULL DEFAULT '2026-10-25'")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN targetDailyStudyHours REAL NOT NULL DEFAULT 2.5")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN targetScoreGoal INTEGER NOT NULL DEFAULT 85")
                } catch (e: Exception) {
                    // Columns might already exist
                }

                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS test_attempts (
                            id TEXT PRIMARY KEY NOT NULL,
                            testTitle TEXT NOT NULL,
                            testType TEXT NOT NULL DEFAULT 'FULL_MOCK',
                            subjectId TEXT,
                            subjectName TEXT NOT NULL DEFAULT 'General Studies',
                            chapterId TEXT,
                            chapterTitle TEXT NOT NULL DEFAULT '',
                            topicId TEXT,
                            topicName TEXT NOT NULL DEFAULT '',
                            totalQuestions INTEGER NOT NULL DEFAULT 0,
                            correctAnswers INTEGER NOT NULL DEFAULT 0,
                            wrongAnswers INTEGER NOT NULL DEFAULT 0,
                            unanswered INTEGER NOT NULL DEFAULT 0,
                            scorePercentage INTEGER NOT NULL DEFAULT 0,
                            accuracy REAL NOT NULL DEFAULT 0.0,
                            timeTakenSeconds INTEGER NOT NULL DEFAULT 0,
                            totalTimeLimitSeconds INTEGER NOT NULL DEFAULT 0,
                            questionsJson TEXT NOT NULL DEFAULT '[]',
                            aiAnalysisJson TEXT NOT NULL DEFAULT '',
                            startedAt INTEGER NOT NULL,
                            completedAt INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS active_test_sessions (
                            id TEXT PRIMARY KEY NOT NULL,
                            testTitle TEXT NOT NULL,
                            testType TEXT NOT NULL DEFAULT 'FULL_MOCK',
                            subjectId TEXT,
                            subjectName TEXT NOT NULL DEFAULT 'General Studies',
                            chapterId TEXT,
                            chapterTitle TEXT NOT NULL DEFAULT '',
                            topicId TEXT,
                            topicName TEXT NOT NULL DEFAULT '',
                            currentQuestionIndex INTEGER NOT NULL DEFAULT 0,
                            remainingTimeSeconds INTEGER NOT NULL DEFAULT 1200,
                            totalTimeLimitSeconds INTEGER NOT NULL DEFAULT 1200,
                            questionsJson TEXT NOT NULL DEFAULT '[]',
                            startedAt INTEGER NOT NULL,
                            lastSavedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    // Tables might already exist
                }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS active_focus_sessions (
                            id TEXT PRIMARY KEY NOT NULL,
                            sessionId TEXT NOT NULL,
                            taskTitle TEXT NOT NULL DEFAULT 'Deep Study Session',
                            subjectName TEXT NOT NULL DEFAULT 'General Studies',
                            subjectId TEXT,
                            chapterId TEXT,
                            chapterTitle TEXT NOT NULL DEFAULT '',
                            associatedTaskId TEXT,
                            plannedDurationSeconds INTEGER NOT NULL DEFAULT 2700,
                            startTimeEpochMs INTEGER NOT NULL,
                            lastPauseEpochMs INTEGER NOT NULL DEFAULT 0,
                            accumulatedPauseMs INTEGER NOT NULL DEFAULT 0,
                            status TEXT NOT NULL DEFAULT 'RUNNING',
                            lastHeartbeatEpochMs INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    // Table might already exist
                }
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN yesterdayFocusedMinutes INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN lastStudyDate TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Columns might already exist
                }
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS daily_study_logs (
                            dateKey TEXT PRIMARY KEY NOT NULL,
                            totalFocusedMinutes INTEGER NOT NULL DEFAULT 0,
                            dailyTargetMinutes INTEGER NOT NULL DEFAULT 45,
                            isTargetMet INTEGER NOT NULL DEFAULT 0,
                            subjectsBreakdownJson TEXT NOT NULL DEFAULT '{}',
                            updatedAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    // Table might already exist
                }
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_preferences ADD COLUMN appThemeMode TEXT NOT NULL DEFAULT 'SYSTEM'")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS progress_xp_events (
                            id TEXT PRIMARY KEY NOT NULL,
                            type TEXT NOT NULL,
                            xp INTEGER NOT NULL,
                            sourceId TEXT,
                            note TEXT NOT NULL DEFAULT '',
                            createdAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    // Table might already exist
                }
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): PrepOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrepOSDatabase::class.java,
                    "prepos_study.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13
                )

                 .fallbackToDestructiveMigrationOnDowngrade()
                 .addCallback(DatabaseCallback(context.applicationContext, scope))
                 .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val appContext: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        SeedDataProvider.populateInitialData(database, appContext)
                    }
                }
            }
        }
    }
}
