package com.example.ui.editor

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.DocElement
import java.util.UUID

enum class AssistantWorkflowType(
    val id: String,
    val title: String,
    val subtitle: String
) {
    WEEKLY_PLAN("weekly_plan", "Create My Weekly Plan", "Custom 7-day timetable aligned with your exam targets"),
    CREATE_NOTES_FROM_SOURCE("notes_from_source", "Create Notes from Source", "Turn book photos, PDFs, or source text into structured study notes"),
    SETUP_SYLLABUS("syllabus", "Set Up My Syllabus", "Extract or organize exam syllabus into subjects & chapters"),
    EXAM_STRATEGY("exam_strategy", "Plan My Exam Strategy", "Comprehensive roadmap based on remaining time & prep level"),
    FREE_TIME_SCHEDULE("free_time", "Plan Around My Free Time", "Build a realistic routine around your available daily slots"),
    FOCUS_NEXT("focus_next", "What Should I Focus On Next?", "Single highest-priority topic recommendation based on progress"),
    HOW_IT_WORKS("how_prepos_works", "How Does PrepOS Work?", "Short, actionable guide to master notes, recall, & planning")
}

// Sealed model for elevated, interactive AI Result Artifacts
sealed class AiResultArtifact {
    data class WeeklyPlan(
        val planTitle: String = "Weekly Study Plan",
        val summary: String = "",
        val days: List<DayScheduleItem> = emptyList(),
        val rawPlanText: String = ""
    ) : AiResultArtifact()

    data class StructuredNotes(
        val chapterTitle: String,
        val subjectName: String = "",
        val summary: String = "",
        val rawMarkdownNotes: String,
        val elements: List<DocElement> = emptyList()
    ) : AiResultArtifact()

    data class SyllabusStructure(
        val examName: String,
        val subjects: List<ExtractedSubjectItem> = emptyList()
    ) : AiResultArtifact()

    data class ExamStrategy(
        val examName: String,
        val daysRemaining: Int = 60,
        val summary: String = "",
        val phases: List<StrategyPhaseItem> = emptyList(),
        val keyAdvice: List<String> = emptyList()
    ) : AiResultArtifact()
}

data class DayScheduleSlot(
    val timeSlot: String,
    val subjectName: String,
    val taskTitle: String,
    val taskType: String = "READING", // READING, PRACTICE, REVISION, MOCK
    val durationMinutes: Int = 45
)

data class DayScheduleItem(
    val dayName: String, // "Monday", "Tuesday", etc.
    val totalTime: String = "2 Hours",
    val slots: List<DayScheduleSlot> = emptyList()
)

// Data classes for Workflow 1: Weekly Plan
data class WeeklyPlanTaskItem(
    val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val timeSlot: String = "07:00 PM - 07:45 PM",
    val subjectName: String,
    val taskTitle: String,
    val taskType: String = "READING", // READING, QUIZ, REVISION, MOCK
    val durationMinutes: Int = 45
)

// Data classes for Workflow 2: Syllabus
data class ExtractedTopicItem(
    val title: String
)

data class ExtractedChapterItem(
    val title: String,
    val chapterNumber: Int = 1,
    val topics: List<String> = emptyList()
)

data class ExtractedSubjectItem(
    val name: String,
    val colorHex: String = "#3B82F6",
    val chapters: List<ExtractedChapterItem> = emptyList()
)

data class ExtractedSyllabusResult(
    val examName: String,
    val subjects: List<ExtractedSubjectItem> = emptyList()
)

// Data classes for Workflow 3: Strategy
data class StrategyPhaseItem(
    val phaseName: String,
    val durationWeeks: String,
    val focusDescription: String,
    val dailyAction: String
)

data class ExamStrategyResult(
    val examName: String,
    val daysRemaining: Int,
    val summary: String,
    val phases: List<StrategyPhaseItem> = emptyList(),
    val keyAdvice: List<String> = emptyList()
)

// Data classes for Workflow 5: Focus Next
data class FocusNextResult(
    val recommendedTopic: String,
    val subjectName: String,
    val shortReason: String,
    val immediateNextAction: String
)

// Data classes for Workflow 6: Explanation
data class HowItWorksExplanation(
    val topicTitle: String,
    val points: List<String>
)
