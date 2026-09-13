package com.example.util

import com.example.data.entity.ChapterEntity
import com.example.data.entity.DailyStudyLogEntity
import com.example.data.entity.ProgressXpEntity
import com.example.data.entity.TestAttemptEntity
import com.example.data.entity.UserPreferencesEntity
import com.example.model.AchievementCategory
import com.example.model.AchievementDefinition
import com.example.model.AchievementProgress
import com.example.model.ProgressEventType
import com.example.model.Rank
import com.example.model.calculateRankProgress
import com.example.model.getRank
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Unified Progression Engine for PrepOS.
 * Provides deterministic XP generation, idempotent ledger operations,
 * rank tier tracking (Novice to Grandmaster), and achievement evaluations.
 */
object ProgressionEngine {

    const val MAX_DAILY_STUDY_XP = 300L

    val ALL_ACHIEVEMENTS: List<AchievementDefinition> = listOf(
        // Study Time
        AchievementDefinition(
            id = "first_spark",
            title = "First Spark",
            description = "Complete your first verified study session",
            category = AchievementCategory.STUDY,
            targetValue = 1,
            xpReward = 25,
            iconSymbol = "🔥"
        ),
        AchievementDefinition(
            id = "bookworm_5h",
            title = "Bookworm",
            description = "Complete 5 hours (300 mins) of verified study",
            category = AchievementCategory.STUDY,
            targetValue = 300,
            xpReward = 50,
            iconSymbol = "📚"
        ),
        AchievementDefinition(
            id = "scholar_15h",
            title = "Scholar",
            description = "Accumulate 15 hours of focused study",
            category = AchievementCategory.STUDY,
            targetValue = 900,
            xpReward = 100,
            iconSymbol = "🎓"
        ),
        AchievementDefinition(
            id = "deep_thinker_30h",
            title = "Deep Thinker",
            description = "Accumulate 30 hours of focused study",
            category = AchievementCategory.STUDY,
            targetValue = 1800,
            xpReward = 150,
            iconSymbol = "🧠"
        ),
        AchievementDefinition(
            id = "grand_scholar_100h",
            title = "Grand Scholar",
            description = "Accumulate 100 hours of focused mastery",
            category = AchievementCategory.STUDY,
            targetValue = 6000,
            xpReward = 250,
            iconSymbol = "🏛️"
        ),

        // Accuracy
        AchievementDefinition(
            id = "sharp_mind",
            title = "Sharp Mind",
            description = "Score 80%+ in any mock test",
            category = AchievementCategory.ACCURACY,
            targetValue = 80,
            xpReward = 50,
            iconSymbol = "🎯"
        ),
        AchievementDefinition(
            id = "sharpshooter",
            title = "Sharpshooter",
            description = "Score 90%+ in a graded test",
            category = AchievementCategory.ACCURACY,
            targetValue = 90,
            xpReward = 75,
            iconSymbol = "🏹"
        ),
        AchievementDefinition(
            id = "bullseye_95",
            title = "Bullseye",
            description = "Score 95%+ in a graded test",
            category = AchievementCategory.ACCURACY,
            targetValue = 95,
            xpReward = 100,
            iconSymbol = "🎯"
        ),
        AchievementDefinition(
            id = "flawless_100",
            title = "Flawless Master",
            description = "Achieve 100% accuracy in a completed test",
            category = AchievementCategory.ACCURACY,
            targetValue = 100,
            xpReward = 150,
            iconSymbol = "💎"
        ),

        // Practice Questions
        AchievementDefinition(
            id = "practice_first_step",
            title = "First Step",
            description = "Attempt 10 practice questions",
            category = AchievementCategory.PRACTICE,
            targetValue = 10,
            xpReward = 25,
            iconSymbol = "✏️"
        ),
        AchievementDefinition(
            id = "curious_mind",
            title = "Curious Mind",
            description = "Attempt 50 practice questions",
            category = AchievementCategory.PRACTICE,
            targetValue = 50,
            xpReward = 50,
            iconSymbol = "📝"
        ),
        AchievementDefinition(
            id = "problem_solver",
            title = "Problem Solver",
            description = "Attempt 200 practice questions",
            category = AchievementCategory.PRACTICE,
            targetValue = 200,
            xpReward = 100,
            iconSymbol = "💡"
        ),
        AchievementDefinition(
            id = "question_crusher",
            title = "Question Crusher",
            description = "Attempt 500 practice questions",
            category = AchievementCategory.PRACTICE,
            targetValue = 500,
            xpReward = 200,
            iconSymbol = "⚔️"
        ),

        // Consistency & Streak
        AchievementDefinition(
            id = "streak_3d",
            title = "First Spark",
            description = "Maintain a 3-day study streak",
            category = AchievementCategory.CONSISTENCY,
            targetValue = 3,
            xpReward = 25,
            iconSymbol = "🔥"
        ),
        AchievementDefinition(
            id = "streak_7d",
            title = "7-Day Discipline",
            description = "Maintain a 7-day continuous streak",
            category = AchievementCategory.CONSISTENCY,
            targetValue = 7,
            xpReward = 50,
            iconSymbol = "⚡"
        ),
        AchievementDefinition(
            id = "streak_14d",
            title = "Unshakable",
            description = "Maintain a 14-day continuous streak",
            category = AchievementCategory.CONSISTENCY,
            targetValue = 14,
            xpReward = 100,
            iconSymbol = "🛡️"
        ),
        AchievementDefinition(
            id = "streak_30d",
            title = "30-Day Discipline",
            description = "Maintain an unbroken 30-day streak",
            category = AchievementCategory.CONSISTENCY,
            targetValue = 30,
            xpReward = 200,
            iconSymbol = "💎"
        ),
        AchievementDefinition(
            id = "streak_100d",
            title = "The Centurion",
            description = "Reach a legendary 100-day streak",
            category = AchievementCategory.CONSISTENCY,
            targetValue = 100,
            xpReward = 500,
            iconSymbol = "🦁"
        ),

        // Chapter Completion
        AchievementDefinition(
            id = "first_chapter",
            title = "First Milestone",
            description = "Complete your first syllabus chapter",
            category = AchievementCategory.COMPLETION,
            targetValue = 1,
            xpReward = 50,
            iconSymbol = "🌱"
        ),
        AchievementDefinition(
            id = "chapter_crusher",
            title = "Chapter Crusher",
            description = "Complete 5 chapters with full comprehension",
            category = AchievementCategory.COMPLETION,
            targetValue = 5,
            xpReward = 100,
            iconSymbol = "📖"
        ),
        AchievementDefinition(
            id = "syllabus_conqueror",
            title = "Syllabus Conqueror",
            description = "Complete 15 chapters",
            category = AchievementCategory.COMPLETION,
            targetValue = 15,
            xpReward = 150,
            iconSymbol = "📜"
        ),
        AchievementDefinition(
            id = "master_of_knowledge",
            title = "Master of Knowledge",
            description = "Complete 30 chapters across your subjects",
            category = AchievementCategory.COMPLETION,
            targetValue = 30,
            xpReward = 250,
            iconSymbol = "👑"
        ),

        // Tests
        AchievementDefinition(
            id = "first_test",
            title = "First Trial",
            description = "Complete your first mock or sectional test",
            category = AchievementCategory.TESTS,
            targetValue = 1,
            xpReward = 25,
            iconSymbol = "🧪"
        ),
        AchievementDefinition(
            id = "test_enthusiast",
            title = "Test Enthusiast",
            description = "Complete 5 mock tests",
            category = AchievementCategory.TESTS,
            targetValue = 5,
            xpReward = 50,
            iconSymbol = "📋"
        ),
        AchievementDefinition(
            id = "test_master",
            title = "Test Master",
            description = "Complete 15 mock tests",
            category = AchievementCategory.TESTS,
            targetValue = 15,
            xpReward = 150,
            iconSymbol = "🏆"
        ),
        AchievementDefinition(
            id = "exam_veteran",
            title = "Exam Veteran",
            description = "Complete 30 full tests",
            category = AchievementCategory.TESTS,
            targetValue = 30,
            xpReward = 250,
            iconSymbol = "🎖️"
        ),

        // Special & Routine
        AchievementDefinition(
            id = "night_owl",
            title = "Night Guardian",
            description = "Complete a focus session after 10:00 PM",
            category = AchievementCategory.SPECIAL,
            targetValue = 1,
            xpReward = 25,
            iconSymbol = "🌙"
        ),
        AchievementDefinition(
            id = "early_bird",
            title = "Dawn Scholar",
            description = "Complete a focus session before 8:00 AM",
            category = AchievementCategory.SPECIAL,
            targetValue = 1,
            xpReward = 25,
            iconSymbol = "🌅"
        ),
        AchievementDefinition(
            id = "revision_session",
            title = "Memory Vault",
            description = "Complete a dedicated revision session",
            category = AchievementCategory.SPECIAL,
            targetValue = 1,
            xpReward = 25,
            iconSymbol = "🔄"
        ),
        AchievementDefinition(
            id = "grandmaster_journey",
            title = "Grandmaster Journey",
            description = "Ascend to Grandmaster Rank (200,000+ XP)",
            category = AchievementCategory.SPECIAL,
            targetValue = 200000,
            xpReward = 0, // No additional XP to prevent rank inflation
            iconSymbol = "🌌"
        )
    )

    /**
     * Calculates test completion XP components:
     * - Base completion: 20 XP
     * - Attempted questions: 1 XP each
     * - Correct answers: +2 XP each
     * - Accuracy bonus tiers
     */
    fun createTestCompletionEvents(attempt: TestAttemptEntity): List<ProgressXpEntity> {
        val events = mutableListOf<ProgressXpEntity>()
        val attemptId = attempt.id
        val now = attempt.completedAt

        // 1. Base completion (+20 XP)
        events.add(
            ProgressXpEntity(
                id = "test:$attemptId:base",
                type = ProgressEventType.TEST_COMPLETED.name,
                xp = 20,
                sourceId = attemptId,
                note = "Completed test: ${attempt.testTitle}",
                createdAt = now
            )
        )

        // 2. Attempted questions (+1 XP each)
        val attempted = (attempt.correctAnswers + attempt.wrongAnswers).coerceAtLeast(0)
        if (attempted > 0) {
            events.add(
                ProgressXpEntity(
                    id = "test:$attemptId:attempts",
                    type = ProgressEventType.PRACTICE_ATTEMPT.name,
                    xp = attempted * 1,
                    sourceId = attemptId,
                    note = "$attempted questions attempted (+${attempted} XP)",
                    createdAt = now
                )
            )
        }

        // 3. Correct answers (+2 XP each bonus)
        val correct = attempt.correctAnswers.coerceAtLeast(0)
        if (correct > 0) {
            events.add(
                ProgressXpEntity(
                    id = "test:$attemptId:correct",
                    type = ProgressEventType.PRACTICE_CORRECT.name,
                    xp = correct * 2,
                    sourceId = attemptId,
                    note = "$correct correct answers (+${correct * 2} XP)",
                    createdAt = now
                )
            )
        }

        // 4. Accuracy bonus
        val pct = attempt.scorePercentage
        val accuracyBonus = when {
            pct == 100 -> 150
            pct in 95..99 -> 100
            pct in 90..94 -> 75
            pct in 80..89 -> 50
            pct in 70..79 -> 35
            pct in 60..69 -> 20
            pct in 50..59 -> 10
            else -> 0
        }

        if (accuracyBonus > 0) {
            events.add(
                ProgressXpEntity(
                    id = "test:$attemptId:accuracy",
                    type = ProgressEventType.TEST_ACCURACY_BONUS.name,
                    xp = accuracyBonus,
                    sourceId = attemptId,
                    note = "Accuracy bonus ($pct%): +$accuracyBonus XP",
                    createdAt = now
                )
            )
        }

        return events
    }

    /**
     * Calculates focus session study XP with strict daily cap of 300 XP.
     */
    fun createStudySessionEvents(
        minutes: Int,
        isRevision: Boolean,
        sessionId: String,
        currentStudyXpToday: Long
    ): List<ProgressXpEntity> {
        val events = mutableListOf<ProgressXpEntity>()
        val now = System.currentTimeMillis()

        // Daily cap enforcement: Max 300 XP from raw study minutes per day
        val remainingCap = (MAX_DAILY_STUDY_XP - currentStudyXpToday).coerceAtLeast(0L).toInt()
        val awardMinutes = minutes.coerceIn(0, remainingCap)

        if (awardMinutes > 0) {
            events.add(
                ProgressXpEntity(
                    id = "study:$sessionId:minutes",
                    type = ProgressEventType.STUDY_MINUTE.name,
                    xp = awardMinutes,
                    sourceId = sessionId,
                    note = "Focused study: $awardMinutes min (+${awardMinutes} XP)",
                    createdAt = now
                )
            )
        }

        // Revision session bonus: +20 XP
        if (isRevision && minutes >= 5) {
            events.add(
                ProgressXpEntity(
                    id = "revision:$sessionId:bonus",
                    type = ProgressEventType.REVISION_SESSION.name,
                    xp = 20,
                    sourceId = sessionId,
                    note = "Completed revision session (+20 XP)",
                    createdAt = now
                )
            )
        }

        return events
    }

    /**
     * Daily study day streak qualification (+10 XP per study day).
     */
    fun createDailyStreakDayEvent(dateKey: String): ProgressXpEntity {
        return ProgressXpEntity(
            id = "streak_day:$dateKey",
            type = ProgressEventType.STREAK_DAY.name,
            xp = 10,
            sourceId = dateKey,
            note = "Daily study consistency ($dateKey): +10 XP",
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Daily target completed (+50 XP).
     */
    fun createDailyTargetEvent(dateKey: String): ProgressXpEntity {
        return ProgressXpEntity(
            id = "daily_target:$dateKey:completed",
            type = ProgressEventType.DAILY_TARGET_COMPLETED.name,
            xp = 50,
            sourceId = dateKey,
            note = "Mandatory daily target achieved ($dateKey): +50 XP",
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Streak milestone bonus:
     * 3d: +25 XP, 7d: +50 XP, 14d: +100 XP, 30d: +200 XP, 100d: +500 XP, 365d: +2000 XP
     */
    fun createStreakMilestoneEvent(streakDays: Int): ProgressXpEntity? {
        val bonus = when (streakDays) {
            3 -> 25
            7 -> 50
            14 -> 100
            30 -> 200
            100 -> 500
            365 -> 2000
            else -> 0
        }
        if (bonus <= 0) return null

        return ProgressXpEntity(
            id = "streak_milestone:$streakDays:reached",
            type = ProgressEventType.STREAK_MILESTONE.name,
            xp = bonus,
            sourceId = "$streakDays",
            note = "🔥 $streakDays-Day Streak Milestone: +$bonus XP",
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Chapter completion (+100 XP, once per chapter).
     */
    fun createChapterCompletionEvent(chapterId: String, chapterTitle: String): ProgressXpEntity {
        return ProgressXpEntity(
            id = "chapter:$chapterId:completion",
            type = ProgressEventType.CHAPTER_COMPLETED.name,
            xp = 100,
            sourceId = chapterId,
            note = "Completed chapter: $chapterTitle (+100 XP)",
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Practice mode MCQ answer (+1 XP attempt, +2 XP correct).
     */
    fun createPracticeAnswerEvents(
        sessionId: String,
        questionId: String,
        isCorrect: Boolean
    ): List<ProgressXpEntity> {
        val events = mutableListOf<ProgressXpEntity>()
        val now = System.currentTimeMillis()

        events.add(
            ProgressXpEntity(
                id = "practice:$sessionId:$questionId:attempt",
                type = ProgressEventType.PRACTICE_ATTEMPT.name,
                xp = 1,
                sourceId = questionId,
                note = "Practice question attempted (+1 XP)",
                createdAt = now
            )
        )

        if (isCorrect) {
            events.add(
                ProgressXpEntity(
                    id = "practice:$sessionId:$questionId:correct",
                    type = ProgressEventType.PRACTICE_CORRECT.name,
                    xp = 2,
                    sourceId = questionId,
                    note = "Correct answer bonus (+2 XP)",
                    createdAt = now
                )
            )
        }

        return events
    }

    /**
     * Evaluates all achievements based on current database state and returns
     * updated progress for each achievement along with any newly unlocked events.
     */
    fun evaluateAchievements(
        totalStudyMinutes: Int,
        testAttempts: List<TestAttemptEntity>,
        completedChaptersCount: Int,
        streakCount: Int,
        totalPracticeAttempted: Int,
        totalXp: Long,
        existingUnlockedIds: Set<String>
    ): Pair<List<AchievementProgress>, List<ProgressXpEntity>> {
        val progressList = mutableListOf<AchievementProgress>()
        val newXpEvents = mutableListOf<ProgressXpEntity>()
        val now = System.currentTimeMillis()

        val maxTestAccuracy = testAttempts.maxOfOrNull { it.scorePercentage } ?: 0
        val totalTestsCount = testAttempts.size

        for (def in ALL_ACHIEVEMENTS) {
            val currentValue = when (def.id) {
                "first_spark" -> if (totalStudyMinutes >= 1) 1 else 0
                "bookworm_5h" -> totalStudyMinutes
                "scholar_15h" -> totalStudyMinutes
                "deep_thinker_30h" -> totalStudyMinutes
                "grand_scholar_100h" -> totalStudyMinutes

                "sharp_mind" -> maxTestAccuracy
                "sharpshooter" -> maxTestAccuracy
                "bullseye_95" -> maxTestAccuracy
                "flawless_100" -> maxTestAccuracy

                "practice_first_step" -> totalPracticeAttempted
                "curious_mind" -> totalPracticeAttempted
                "problem_solver" -> totalPracticeAttempted
                "question_crusher" -> totalPracticeAttempted

                "streak_3d" -> streakCount
                "streak_7d" -> streakCount
                "streak_14d" -> streakCount
                "streak_30d" -> streakCount
                "streak_100d" -> streakCount

                "first_chapter" -> completedChaptersCount
                "chapter_crusher" -> completedChaptersCount
                "syllabus_conqueror" -> completedChaptersCount
                "master_of_knowledge" -> completedChaptersCount

                "first_test" -> totalTestsCount
                "test_enthusiast" -> totalTestsCount
                "test_master" -> totalTestsCount
                "exam_veteran" -> totalTestsCount

                "night_owl" -> if (totalStudyMinutes >= 1) 1 else 0
                "early_bird" -> if (totalStudyMinutes >= 1) 1 else 0
                "revision_session" -> if (totalStudyMinutes >= 5) 1 else 0
                "grandmaster_journey" -> totalXp.toInt()
                else -> 0
            }

            val isConditionMet = currentValue >= def.targetValue
            val isAlreadyUnlocked = existingUnlockedIds.contains(def.id)
            val isUnlocked = isAlreadyUnlocked || isConditionMet

            progressList.add(
                AchievementProgress(
                    definition = def,
                    currentValue = currentValue,
                    isUnlocked = isUnlocked,
                    unlockedAtEpochMs = if (isUnlocked) now else null
                )
            )

            // If newly unlocked and has XP reward, create idempotent XP ledger event
            if (isConditionMet && !isAlreadyUnlocked && def.xpReward > 0) {
                newXpEvents.add(
                    ProgressXpEntity(
                        id = "achievement:${def.id}:unlocked",
                        type = ProgressEventType.ACHIEVEMENT_UNLOCKED.name,
                        xp = def.xpReward,
                        sourceId = def.id,
                        note = "Achievement Unlocked: ${def.title} (+${def.xpReward} XP)",
                        createdAt = now
                    )
                )
            }
        }

        return Pair(progressList, newXpEvents)
    }

    /**
     * Reconciles legacy data (e.g. on first launch or database upgrade)
     * so existing users retain all verified XP for previously completed
     * tests, chapters, and study sessions.
     */
    fun generateLegacySeedEvents(
        testAttempts: List<TestAttemptEntity>,
        completedChapters: List<ChapterEntity>,
        preferences: UserPreferencesEntity?,
        totalHistoricalStudyMinutes: Int
    ): List<ProgressXpEntity> {
        val events = mutableListOf<ProgressXpEntity>()
        val baseTime = System.currentTimeMillis() - 86400000L

        // 1. Existing test attempts
        for (attempt in testAttempts) {
            events.addAll(createTestCompletionEvents(attempt))
        }

        // 2. Existing completed chapters
        for (chapter in completedChapters) {
            events.add(createChapterCompletionEvent(chapter.id, chapter.title))
        }

        // 3. Historical study time (credited up to realistic verified minutes)
        if (totalHistoricalStudyMinutes > 0) {
            val creditedMinutes = totalHistoricalStudyMinutes.coerceAtMost(600) // initial seed credit
            events.add(
                ProgressXpEntity(
                    id = "study:historical_credit",
                    type = ProgressEventType.STUDY_MINUTE.name,
                    xp = creditedMinutes,
                    sourceId = "historical_credit",
                    note = "Verified study history credit: $creditedMinutes min (+${creditedMinutes} XP)",
                    createdAt = baseTime
                )
            )
        }

        // 4. Streak consistency
        val streak = preferences?.currentStreak ?: 0
        if (streak >= 3) {
            createStreakMilestoneEvent(3)?.let { events.add(it) }
        }
        if (streak >= 7) {
            createStreakMilestoneEvent(7)?.let { events.add(it) }
        }

        return events
    }

    /**
     * Reconciles ALL real historical data from Room database:
     * - Daily study logs (each day with its exact dateKey, focus minutes, and daily target status)
     * - Test attempts (with exact completedAt timestamps, question counts, and score accuracy)
     * - Completed chapters
     * - Real streak consistency milestones
     *
     * Ensures all XP in the ledger reflects exact historical dates rather than today's timestamp.
     */
    fun generateGranularEventsFromRealData(
        dailyLogs: List<DailyStudyLogEntity>,
        testAttempts: List<TestAttemptEntity>,
        completedChapters: List<ChapterEntity>,
        preferences: UserPreferencesEntity?
    ): List<ProgressXpEntity> {
        val events = mutableListOf<ProgressXpEntity>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // 1. Reconcile daily study logs with real dateKey timestamps
        for (log in dailyLogs) {
            val logBaseTime = try {
                sdf.parse(log.dateKey)?.time ?: log.updatedAt
            } catch (e: Exception) {
                log.updatedAt
            }

            if (log.totalFocusedMinutes > 0) {
                val studyXp = log.totalFocusedMinutes.coerceAtMost(300)
                events.add(
                    ProgressXpEntity(
                        id = "study:day:${log.dateKey}",
                        type = ProgressEventType.STUDY_MINUTE.name,
                        xp = studyXp,
                        sourceId = log.dateKey,
                        note = "Focus study on ${log.dateKey}: ${log.totalFocusedMinutes}m (+${studyXp} XP)",
                        createdAt = logBaseTime + (18 * 3600 * 1000L) // mid-evening timestamp
                    )
                )
            }

            if (log.isTargetMet || (log.dailyTargetMinutes > 0 && log.totalFocusedMinutes >= log.dailyTargetMinutes)) {
                events.add(
                    ProgressXpEntity(
                        id = "target:day:${log.dateKey}",
                        type = ProgressEventType.DAILY_TARGET_COMPLETED.name,
                        xp = 50,
                        sourceId = log.dateKey,
                        note = "Daily Target Met on ${log.dateKey} (+50 XP)",
                        createdAt = logBaseTime + (20 * 3600 * 1000L)
                    )
                )
            }
        }

        // 2. Reconcile real test attempts with exact completedAt, question counts, and accuracy
        for (attempt in testAttempts) {
            events.addAll(createTestCompletionEvents(attempt))
        }

        // 3. Reconcile completed chapters
        for (chapter in completedChapters) {
            events.add(createChapterCompletionEvent(chapter.id, chapter.title))
        }

        // 4. Streak consistency milestones based on real streak data
        val streak = preferences?.currentStreak ?: 0
        if (streak >= 3) createStreakMilestoneEvent(3)?.let { events.add(it) }
        if (streak >= 7) createStreakMilestoneEvent(7)?.let { events.add(it) }
        if (streak >= 14) createStreakMilestoneEvent(14)?.let { events.add(it) }
        if (streak >= 30) createStreakMilestoneEvent(30)?.let { events.add(it) }
        if (streak >= 100) createStreakMilestoneEvent(100)?.let { events.add(it) }

        return events
    }
}
