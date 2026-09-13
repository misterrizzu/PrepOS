package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Master 14-Tier Rank Hierarchy for PrepOS.
 * Provides a single master progression ladder driven by verified XP.
 */
enum class Rank(
    val title: String,
    val minXp: Long,
    val maxXp: Long,
    val slogan: String,
    val symbol: String,
    val primaryColorHex: String,
    val secondaryColorHex: String
) {
    NOVICE(
        title = "Novice",
        minXp = 0L,
        maxXp = 299L,
        slogan = "Every big journey starts here",
        symbol = "🌱",
        primaryColorHex = "#22C55E",
        secondaryColorHex = "#15803D"
    ),
    APPRENTICE(
        title = "Apprentice",
        minXp = 300L,
        maxXp = 999L,
        slogan = "Stepping into knowledge",
        symbol = "📖",
        primaryColorHex = "#06B6D4",
        secondaryColorHex = "#0E7490"
    ),
    BRONZE(
        title = "Bronze",
        minXp = 1_000L,
        maxXp = 2_499L,
        slogan = "Building good habits",
        symbol = "🟤",
        primaryColorHex = "#D97706",
        secondaryColorHex = "#92400E"
    ),
    SILVER(
        title = "Silver",
        minXp = 2_500L,
        maxXp = 4_999L,
        slogan = "Consistency creates progress",
        symbol = "⚪",
        primaryColorHex = "#94A3B8",
        secondaryColorHex = "#475569"
    ),
    GOLD(
        title = "Gold",
        minXp = 5_000L,
        maxXp = 8_999L,
        slogan = "Knowledge takes you further",
        symbol = "🟡",
        primaryColorHex = "#F59E0B",
        secondaryColorHex = "#B45309"
    ),
    PLATINUM(
        title = "Platinum",
        minXp = 9_000L,
        maxXp = 14_999L,
        slogan = "Discipline sets you apart",
        symbol = "🔷",
        primaryColorHex = "#38BDF8",
        secondaryColorHex = "#0284C7"
    ),
    DIAMOND(
        title = "Diamond",
        minXp = 15_000L,
        maxXp = 23_999L,
        slogan = "Excellence is a habit",
        symbol = "💎",
        primaryColorHex = "#3B82F6",
        secondaryColorHex = "#1D4ED8"
    ),
    ELITE(
        title = "Elite",
        minXp = 24_000L,
        maxXp = 34_999L,
        slogan = "Among the top learners",
        symbol = "🟣",
        primaryColorHex = "#A855F7",
        secondaryColorHex = "#7E22CE"
    ),
    CHAMPION(
        title = "Champion",
        minXp = 35_000L,
        maxXp = 49_999L,
        slogan = "A true performer",
        symbol = "🦁",
        primaryColorHex = "#EF4444",
        secondaryColorHex = "#B91C1C"
    ),
    LEGEND(
        title = "Legend",
        minXp = 50_000L,
        maxXp = 69_999L,
        slogan = "Beyond limits",
        symbol = "⚔️",
        primaryColorHex = "#EAB308",
        secondaryColorHex = "#CA8A04"
    ),
    MYTHIC(
        title = "Mythic",
        minXp = 70_000L,
        maxXp = 99_999L,
        slogan = "Master of comprehension",
        symbol = "🔮",
        primaryColorHex = "#8B5CF6",
        secondaryColorHex = "#6D28D9"
    ),
    IMMORTAL(
        title = "Immortal",
        minXp = 100_000L,
        maxXp = 139_999L,
        slogan = "Unyielding dedication",
        symbol = "👑",
        primaryColorHex = "#EC4899",
        secondaryColorHex = "#BE185D"
    ),
    SOVEREIGN(
        title = "Sovereign",
        minXp = 140_000L,
        maxXp = 199_999L,
        slogan = "Commanding excellence",
        symbol = "🔥",
        primaryColorHex = "#F97316",
        secondaryColorHex = "#C2410C"
    ),
    GRANDMASTER(
        title = "Grandmaster",
        minXp = 200_000L,
        maxXp = Long.MAX_VALUE,
        slogan = "The highest level of dedication",
        symbol = "🌌",
        primaryColorHex = "#6366F1",
        secondaryColorHex = "#4338CA"
    );

    fun getPrimaryColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(primaryColorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }

    fun getSecondaryColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(secondaryColorHex))
        } catch (e: Exception) {
            Color(0xFF1D4ED8)
        }
    }
}

/**
 * Determines current rank from total accumulated verified XP.
 */
fun getRank(xp: Long): Rank {
    val safeXp = xp.coerceAtLeast(0L)
    return Rank.entries.lastOrNull { safeXp >= it.minXp } ?: Rank.NOVICE
}

data class RankProgress(
    val rank: Rank,
    val xp: Long,
    val xpIntoRank: Long,
    val xpRequiredForNextRank: Long,
    val progressPercent: Float,
    val nextRank: Rank?
)

/**
 * Calculates user's progress within their current rank toward the next tier.
 */
fun calculateRankProgress(xp: Long): RankProgress {
    val safeXp = xp.coerceAtLeast(0L)
    val rank = getRank(safeXp)
    val next = Rank.entries.getOrNull(rank.ordinal + 1)

    if (next == null) {
        return RankProgress(
            rank = rank,
            xp = safeXp,
            xpIntoRank = safeXp - rank.minXp,
            xpRequiredForNextRank = 0L,
            progressPercent = 1f,
            nextRank = null
        )
    }

    val range = next.minXp - rank.minXp
    val xpIntoRank = safeXp - rank.minXp
    val progress = if (range > 0) xpIntoRank.toFloat() / range.toFloat() else 1f

    return RankProgress(
        rank = rank,
        xp = safeXp,
        xpIntoRank = xpIntoRank,
        xpRequiredForNextRank = range,
        progressPercent = progress.coerceIn(0f, 1f),
        nextRank = next
    )
}

enum class ProgressEventType(val displayName: String, val badge: String) {
    STUDY_MINUTE("Focused Study", "📚"),
    PRACTICE_ATTEMPT("Practice Question Attempt", "✏️"),
    PRACTICE_CORRECT("Practice Correct Answer", "🎯"),
    TEST_COMPLETED("Test Completed", "📝"),
    TEST_ACCURACY_BONUS("Test Accuracy Bonus", "⭐"),
    CHAPTER_COMPLETED("Chapter Completed", "📖"),
    DAILY_TARGET_COMPLETED("Daily Target Achieved", "🎯"),
    REVISION_SESSION("Revision Session", "🔄"),
    STREAK_DAY("Daily Study Streak", "🔥"),
    STREAK_MILESTONE("Streak Milestone Bonus", "⚡"),
    ACHIEVEMENT_UNLOCKED("Achievement Unlocked", "🎖️")
}

enum class AchievementCategory(val label: String, val iconSymbol: String) {
    ALL("All", "✨"),
    STUDY("Study", "📚"),
    ACCURACY("Accuracy", "🎯"),
    PRACTICE("Practice", "📝"),
    CONSISTENCY("Consistency", "🔥"),
    COMPLETION("Completion", "📖"),
    TESTS("Tests", "🏆"),
    SPECIAL("Special", "⚡")
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val targetValue: Int,
    val xpReward: Int,
    val iconSymbol: String
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val unlockedAtEpochMs: Long? = null
) {
    val progressPercent: Float
        get() = if (definition.targetValue > 0) {
            (currentValue.toFloat() / definition.targetValue.toFloat()).coerceIn(0f, 1f)
        } else if (isUnlocked) 1f else 0f
}

data class ProgressionOverview(
    val totalXp: Long = 0L,
    val rankProgress: RankProgress = calculateRankProgress(0L),
    val streakCount: Int = 0,
    val longestStreak: Int = 0,
    val unlockedAchievementsCount: Int = 0,
    val totalAchievementsCount: Int = 0,
    val todayTotalXp: Long = 0L,
    val todayStudyMinutesXp: Long = 0L,
    val maxDailyStudyXp: Long = 300L,
    val recentEvents: List<com.example.data.entity.ProgressXpEntity> = emptyList(),
    val achievements: List<AchievementProgress> = emptyList()
)
