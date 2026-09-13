package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.ui.components.RichQuestionCard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ai.OfflineQuestionParser
import com.example.ai.StudyAIService
import com.example.model.QuestionItem
import com.example.ui.components.ShareQuestionSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterQuestionsSheet(
    questions: List<QuestionItem>,
    chapterTitle: String,
    onDismiss: () -> Unit,
    onAddQuestion: (QuestionItem) -> Unit,
    onAddQuestionsBatch: ((List<QuestionItem>) -> Unit)? = null,
    onDeleteQuestion: (String) -> Unit,
    onDeleteAllQuestions: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var sharedQuestionIds by remember { mutableStateOf(setOf<String>()) }
    var questionToShare by remember { mutableStateOf<QuestionItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Chapter Questions & Quiz",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${questions.size} practice questions for $chapterTitle",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (questions.isNotEmpty() && onDeleteAllQuestions != null) {
                        IconButton(
                            onClick = { showDeleteAllConfirmDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_all_chapter_questions_btn")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete All Questions",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showPasteImportDialog = true },
                        modifier = Modifier.testTag("paste_import_questions_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Paste", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_question_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add", fontSize = 11.5.sp)
                    }
                }
            }

            if (showDeleteAllConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAllConfirmDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Questions?", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text("Are you sure you want to delete all ${questions.size} questions for '$chapterTitle'? This action cannot be undone.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteAllConfirmDialog = false
                                onDeleteAllQuestions?.invoke()
                                Toast.makeText(context, "All questions deleted for $chapterTitle", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete All (${questions.size})", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            if (questions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No questions in this chapter yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showPasteImportDialog = true }) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parse MCQs (Offline / AI)")
                            }
                            OutlinedButton(onClick = { showAddDialog = true }) {
                                Text("Add Single Question")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(questions, key = { _, item -> item.id }) { index, item ->
                        RichQuestionCard(
                            item = item,
                            index = index,
                            isShared = sharedQuestionIds.contains(item.id),
                            onDelete = { onDeleteQuestion(item.id) },
                            onShare = {
                                questionToShare = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: 4:3 Graphic Poster & Rich Caption Share Sheet
    questionToShare?.let { q ->
        ShareQuestionSheet(
            item = q,
            examName = chapterTitle,
            chapterTitle = chapterTitle,
            onDismiss = { questionToShare = null },
            onQuestionShared = { sharedQ ->
                sharedQuestionIds = sharedQuestionIds + sharedQ.id
            }
        )
    }

    if (showAddDialog) {
        AddQuestionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newQ ->
                onAddQuestion(newQ)
                showAddDialog = false
            }
        )
    }

    if (showPasteImportDialog) {
        PasteOfflineQuestionsDialog(
            chapterTitle = chapterTitle,
            onDismiss = { showPasteImportDialog = false },
            onImport = { newQuestions ->
                if (onAddQuestionsBatch != null) {
                    onAddQuestionsBatch(newQuestions)
                } else {
                    newQuestions.forEach { onAddQuestion(it) }
                }
                showPasteImportDialog = false
            }
        )
    }
}

@Composable
fun InteractiveQuestionCard(
    item: QuestionItem,
    index: Int,
    isShared: Boolean = false,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var selectedOptionIndex by remember(item.id) { mutableStateOf<Int?>(null) }
    var isRevealed by remember(item.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("interactive_question_card_$index"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Q${index + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.questionText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(28.dp).testTag("share_question_card_btn")
                    ) {
                        Icon(
                            imageVector = if (isShared) Icons.Default.CheckCircle else Icons.Default.Share,
                            contentDescription = if (isShared) "Question Shared (Checked)" else "Share question",
                            tint = if (isShared) Color(0xFF10B981) else Color(0xFF8B5CF6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete question",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.options.forEachIndexed { optIdx, optText ->
                    val isSelected = selectedOptionIndex == optIdx
                    val isCorrect = optIdx == item.correctOptionIndex

                    val optBgColor = when {
                        !isRevealed -> MaterialTheme.colorScheme.surface
                        isCorrect -> Color(0xFF10B981).copy(alpha = 0.18f)
                        isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    }

                    val optBorderColor = when {
                        !isRevealed -> MaterialTheme.colorScheme.outlineVariant
                        isCorrect -> Color(0xFF10B981)
                        isSelected && !isCorrect -> Color(0xFFEF4444)
                        else -> Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(optBgColor)
                            .border(1.dp, optBorderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isRevealed) {
                                selectedOptionIndex = optIdx
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val optLetter = ('A' + optIdx).toString()
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isRevealed && isCorrect -> Color(0xFF10B981)
                                isRevealed && isSelected -> Color(0xFFEF4444)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = optLetter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRevealed && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isRevealed && isCorrect) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRevealed && isCorrect) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Check Answer / Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRevealed) {
                    Button(
                        onClick = { isRevealed = true },
                        enabled = selectedOptionIndex != null,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Check Answer", fontSize = 12.sp)
                    }
                } else {
                    val wasCorrect = selectedOptionIndex == item.correctOptionIndex
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (wasCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (wasCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (wasCorrect) "Correct!" else "Incorrect (Correct: Option ${('A' + item.correctOptionIndex)})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (wasCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        )
                    }

                    TextButton(
                        onClick = {
                            isRevealed = false
                            selectedOptionIndex = null
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Retry", fontSize = 11.sp)
                    }
                }
            }

            // Explanation Section
            AnimatedVisibility(visible = isRevealed && !item.explanation.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Explanation",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.explanation ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddQuestionDialog(
    onDismiss: () -> Unit,
    onAdd: (QuestionItem) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIndex by remember { mutableStateOf(0) }
    var explanationText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Practice Question") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Question Statement") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Options (Tap circle to mark correct):",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )

                val optionStates = listOf(
                    Pair(optA) { v: String -> optA = v },
                    Pair(optB) { v: String -> optB = v },
                    Pair(optC) { v: String -> optC = v },
                    Pair(optD) { v: String -> optD = v }
                )

                optionStates.forEachIndexed { idx, (text, setter) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (correctIndex == idx) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { correctIndex = idx }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ('A' + idx).toString(),
                                    color = if (correctIndex == idx) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = setter,
                            placeholder = { Text("Option ${('A' + idx)}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = explanationText,
                    onValueChange = { explanationText = it },
                    label = { Text("Explanation (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (questionText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
                        val opts = listOfNotNull(
                            optA.ifBlank { null },
                            optB.ifBlank { null },
                            optC.ifBlank { null },
                            optD.ifBlank { null }
                        )
                        onAdd(
                            QuestionItem(
                                id = UUID.randomUUID().toString(),
                                questionText = questionText.trim(),
                                options = opts,
                                correctOptionIndex = correctIndex.coerceIn(0, opts.size - 1),
                                explanation = explanationText.trim()
                            )
                        )
                    }
                },
                enabled = questionText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()
            ) {
                Text("Add Question")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PasteOfflineQuestionsDialog(
    chapterTitle: String,
    onDismiss: () -> Unit,
    onImport: (List<QuestionItem>) -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var previewQuestions by remember { mutableStateOf<List<QuestionItem>>(emptyList()) }
    var parserMode by remember { mutableStateOf("offline") } // "offline" or "online"
    var isParsing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun executeParse(text: String, mode: String) {
        if (text.isBlank()) {
            previewQuestions = emptyList()
            return
        }
        isParsing = true
        scope.launch {
            try {
                val isOnline = (mode == "online")
                val result = if (isOnline) {
                    withContext(Dispatchers.IO) {
                        StudyAIService.parseQuestionsFromText(rawText = text)
                    }
                } else {
                    withContext(Dispatchers.Default) {
                        OfflineQuestionParser.parseLocally(text)
                    }
                }
                withContext(Dispatchers.Main) {
                    previewQuestions = result
                    isParsing = false
                    if (result.isEmpty()) {
                        val msg = if (isOnline) {
                            "No MCQs found by AI. Check question formatting or try Offline Parser."
                        } else {
                            "No MCQs recognized offline. Try switching to 'Online AI (Gemini)'!"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } else {
                        val label = if (isOnline) "Online AI" else "Offline Parser"
                        Toast.makeText(context, "✓ Parsed ${result.size} MCQs using $label!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isParsing = false
                    Toast.makeText(context, "Parsing error: ${e.localizedMessage ?: "Unexpected error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (parserMode == "online") Icons.Default.AutoAwesome else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (parserMode == "online") Color(0xFF8B5CF6) else Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Parse MCQs for Chapter", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Segmented Mode Selector: Offline vs Online AI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { parserMode = "offline" },
                        shape = RoundedCornerShape(8.dp),
                        color = if (parserMode == "offline") MaterialTheme.colorScheme.surface else Color.Transparent,
                        border = if (parserMode == "offline") BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "⚡ Offline",
                                fontWeight = if (parserMode == "offline") FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (parserMode == "offline") MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { parserMode = "online" },
                        shape = RoundedCornerShape(8.dp),
                        color = if (parserMode == "online") MaterialTheme.colorScheme.surface else Color.Transparent,
                        border = if (parserMode == "online") BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "✨ Online AI",
                                fontWeight = if (parserMode == "online") FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (parserMode == "online") Color(0xFF8B5CF6) else Color.Gray
                            )
                        }
                    }
                }

                Text(
                    text = if (parserMode == "offline") {
                        "⚡ Offline: Deterministic & instant. Works offline with standard A-D format."
                    } else {
                        "✨ Online AI: Uses Gemini to extract MCQs from raw or mixed study text."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.5.sp)
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                    },
                    placeholder = {
                        Text(
                            "Q1. Capital of J&K in Summer is?\nA) Jammu\nB) Srinagar\nC) Leh\nD) Anantnag\nAnswer: B\nExplanation: Srinagar is the summer capital."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 10
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                rawText = clip
                                executeParse(clip, parserMode)
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Clipboard", fontSize = 12.sp)
                    }

                    if (previewQuestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (parserMode == "online") Color(0xFF8B5CF6) else Color(0xFF10B981)).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✓ ${previewQuestions.size} MCQs Ready",
                                color = if (parserMode == "online") Color(0xFF8B5CF6) else Color(0xFF059669),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Action parse button
                Button(
                    onClick = { executeParse(rawText, parserMode) },
                    enabled = rawText.isNotBlank() && !isParsing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (parserMode == "online") Color(0xFF7C3AED) else Color(0xFF10B981)
                    )
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (parserMode == "online") "AI is Parsing MCQs..." else "Parsing Questions...")
                    } else {
                        Icon(
                            if (parserMode == "online") Icons.Default.AutoAwesome else Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (parserMode == "online") "Parse with Online AI (Gemini)" else "Parse with Offline Parser",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (previewQuestions.isNotEmpty()) {
                        onImport(previewQuestions)
                    } else if (rawText.isNotBlank()) {
                        isParsing = true
                        scope.launch {
                            val parsed = withContext(Dispatchers.Default) {
                                OfflineQuestionParser.parseLocally(rawText)
                            }
                            withContext(Dispatchers.Main) {
                                isParsing = false
                                if (parsed.isNotEmpty()) {
                                    onImport(parsed)
                                } else {
                                    Toast.makeText(context, "Please click 'Parse' first to detect questions.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                enabled = (previewQuestions.isNotEmpty() || rawText.isNotBlank()) && !isParsing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Import ${if (previewQuestions.isNotEmpty()) "(${previewQuestions.size})" else ""}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
