package com.example.ui.editor

import androidx.compose.runtime.Composable
import com.example.ai.StudyAIService
import com.example.model.ChapterTopic
import com.example.model.DocElement

/**
 * AskAIDialog now delegates directly to the dedicated full-screen [AskAIScreen].
 */
@Composable
fun AskAIDialog(
    selectedText: String,
    fullChapterText: String = "",
    chapterTitle: String,
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
    viewModel: com.example.viewmodel.PrepOSViewModel? = null,
    onDismiss: () -> Unit,
    onReplaceSelection: (String) -> Unit,
    onInsertBelow: (String) -> Unit,
    onUpdateTopic: (topicId: String, newContent: String) -> Unit = { _, _ -> },
    onInsertIntoTopic: (topicId: String, newContent: String) -> Unit = { _, _ -> },
    onDuplicateTopic: (topicId: String, newContent: String) -> Unit = { _, _ -> }
) {
    AskAIScreen(
        selectedText = selectedText,
        fullChapterText = fullChapterText,
        chapterTitle = chapterTitle,
        subjectName = subjectName,
        chapterId = chapterId,
        topics = topics,
        initialSelectedTopicId = initialSelectedTopicId,
        selectedDiagram = selectedDiagram,
        onUpdateDiagram = onUpdateDiagram,
        viewModel = viewModel,
        apiKey = apiKey,
        model = model,
        provider = provider,
        deepSeekApiKey = deepSeekApiKey,
        onDismiss = onDismiss,
        onReplaceSelection = onReplaceSelection,
        onInsertBelow = onInsertBelow,
        onUpdateTopic = onUpdateTopic,
        onInsertIntoTopic = onInsertIntoTopic,
        onDuplicateTopic = onDuplicateTopic
    )
}
