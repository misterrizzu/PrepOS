package com.example.ui.test

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.OfflineQuestionParser
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity
import com.example.model.QuestionItem
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.isAppDarkTheme
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ImportQuestionsDialog(
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    preselectedChapterId: String? = null,
    preselectedSubjectId: String? = null,
    viewModel: PrepOSViewModel,
    onDismiss: () -> Unit,
    onImported: ((count: Int, chapterTitle: String) -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Paste Text / File (Offline Parser), 1: Manual Single Add
    var rawInputText by remember { mutableStateOf("") }
    var selectedSubjectId by remember {
        mutableStateOf(
            preselectedSubjectId ?: preselectedChapterId?.let { cid -> chapters.find { it.id == cid }?.subjectId } ?: subjects.firstOrNull()?.id
        )
    }
    var selectedChapterId by remember {
        mutableStateOf(
            preselectedChapterId ?: chapters.firstOrNull { it.subjectId == selectedSubjectId }?.id
        )
    }

    // Parsed Questions state
    var parsedQuestions by remember { mutableStateOf<List<QuestionItem>>(emptyList()) }
    var isParsing by remember { mutableStateOf(false) }
    var replaceExisting by remember { mutableStateOf(false) }
    var showConnectApiKeyDialog by remember { mutableStateOf(false) }

    // Manual Single Add fields
    var manualQuestionText by remember { mutableStateOf("") }
    var manualOptA by remember { mutableStateOf("") }
    var manualOptB by remember { mutableStateOf("") }
    var manualOptC by remember { mutableStateOf("") }
    var manualOptD by remember { mutableStateOf("") }
    var manualCorrectIndex by remember { mutableIntStateOf(0) }
    var manualExplanation by remember { mutableStateOf("") }

    // Parser mode: "offline" (deterministic, zero data) or "online" (Gemini AI extraction)
    var parserMode by remember { mutableStateOf("offline") }

    val filteredChapters = remember(selectedSubjectId, chapters) {
        if (selectedSubjectId == null) chapters else chapters.filter { it.subjectId == selectedSubjectId }
    }

    // Auto update selectedChapterId if subject changes
    LaunchedEffect(selectedSubjectId) {
        if (selectedChapterId == null || chapters.none { it.id == selectedChapterId && it.subjectId == selectedSubjectId }) {
            selectedChapterId = filteredChapters.firstOrNull()?.id
        }
    }

    // Function to run Parser (Offline or Online AI)
    fun runParse(text: String, mode: String = parserMode) {
        if (text.isBlank()) {
            parsedQuestions = emptyList()
            return
        }

        val isOnline = (mode == "online")
        if (isOnline) {
            val prefs = viewModel.preferences.value
            val apiKey = prefs?.apiKey ?: ""
            if (!com.example.ai.StudyAIService.isGeminiKeyConfigured(apiKey)) {
                showConnectApiKeyDialog = true
                return
            }
        }

        isParsing = true
        scope.launch {
            try {
                val result = if (isOnline) {
                    withContext(Dispatchers.IO) {
                        val prefs = viewModel.preferences.value
                        val apiKey = prefs?.apiKey ?: ""
                        val model = prefs?.selectedAiModel ?: "gemini-2.5-flash"
                        com.example.ai.StudyAIService.parseQuestionsFromText(
                            rawText = text,
                            apiKey = apiKey,
                            model = model
                        )
                    }
                } else {
                    withContext(Dispatchers.Default) {
                        OfflineQuestionParser.parseLocally(text)
                    }
                }
                withContext(Dispatchers.Main) {
                    parsedQuestions = result
                    isParsing = false
                    if (result.isEmpty()) {
                        val msg = if (isOnline) {
                            "No MCQs found by AI. Ensure text has questions, or try the Offline Parser."
                        } else {
                            "No MCQs recognized offline. Switch to 'Online AI' for smart AI extraction!"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } else {
                        val label = if (isOnline) "Online AI (Gemini)" else "Offline Parser"
                        Toast.makeText(context, "✓ Parsed ${result.size} questions using $label!", Toast.LENGTH_SHORT).show()
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

    if (showConnectApiKeyDialog) {
        com.example.ui.components.ConnectApiKeyDialog(
            onDismiss = { showConnectApiKeyDialog = false },
            onKeySaved = { savedKey ->
                showConnectApiKeyDialog = false
                viewModel.saveApiKey(savedKey)
                viewModel.saveAiProvider("GEMINI")
                Toast.makeText(context, "✓ Gemini API Key connected successfully!", Toast.LENGTH_SHORT).show()
                if (rawInputText.isNotBlank()) {
                    runParse(rawInputText, "online")
                }
            },
            featureTitle = "Smart MCQ Online Extractor"
        )
    }

    // File picker for .txt files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    reader.close()
                    inputStream?.close()

                    withContext(Dispatchers.Main) {
                        rawInputText = content
                        runParse(content)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.2.dp,
                    if (isDark) Color(0xFF6366F1).copy(alpha = 0.4f) else LightBorder,
                    RoundedCornerShape(24.dp)
                ),
            color = if (isDark) Color(0xFF0F172A) else LightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.2f) else BrandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import / Add Questions",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                            Text(
                                text = "Add practice questions & MCQs to chapter",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Subject & Chapter Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "TARGET DESTINATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = if (isDark) Color(0xFFA5B4FC) else BrandPrimary,
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Subjects scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { sub ->
                            FilterChip(
                                selected = selectedSubjectId == sub.id,
                                onClick = { selectedSubjectId = sub.id },
                                label = { Text(sub.name, maxLines = 1, fontSize = 12.sp, fontWeight = if (selectedSubjectId == sub.id) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                                    labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedSubjectId == sub.id,
                                    borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    selectedBorderColor = Color.Transparent,
                                    borderWidth = 1.dp
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Chapters scroll
                    if (filteredChapters.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredChapters.forEach { chap ->
                                FilterChip(
                                    selected = selectedChapterId == chap.id,
                                    onClick = { selectedChapterId = chap.id },
                                    label = { Text(chap.title, maxLines = 1, fontSize = 12.sp, fontWeight = if (selectedChapterId == chap.id) FontWeight.Bold else FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isDark) Color(0xFF7C3AED) else Color(0xFF6D28D9),
                                        selectedLabelColor = Color.White,
                                        containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                                        labelColor = if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedChapterId == chap.id,
                                        borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        selectedBorderColor = Color.Transparent,
                                        borderWidth = 1.dp
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No chapters found. Please create a chapter first.",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEF4444)),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Row: Parser vs Manual Add
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { selectedTab = 0 },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 0) (if (isDark) Color(0xFF0F172A) else Color.White) else Color.Transparent,
                        shadowElevation = if (selectedTab == 0) 1.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color(0xFF10B981) else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Auto Parser",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (selectedTab == 0) (if (isDark) Color.White else LightTextPrimary) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { selectedTab = 1 },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 1) (if (isDark) Color(0xFF0F172A) else Color.White) else Color.Transparent,
                        shadowElevation = if (selectedTab == 1) 1.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Quiz,
                                contentDescription = null,
                                tint = if (selectedTab == 1) BrandPrimary else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Manual Single Add",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (selectedTab == 1) (if (isDark) Color.White else LightTextPrimary) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB 0: Parser (Offline or Online AI, paste text or pick file)
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Parser Engine Segmented Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { parserMode = "offline" },
                                shape = RoundedCornerShape(9.dp),
                                color = if (parserMode == "offline") (if (isDark) Color(0xFF0F172A) else Color.White) else Color.Transparent,
                                shadowElevation = if (parserMode == "offline") 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Offline Parser",
                                        fontWeight = if (parserMode == "offline") FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (parserMode == "offline") (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { parserMode = "online" },
                                shape = RoundedCornerShape(9.dp),
                                color = if (parserMode == "online") (if (isDark) Color(0xFF0F172A) else Color.White) else Color.Transparent,
                                shadowElevation = if (parserMode == "online") 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Online AI (Gemini)",
                                        fontWeight = if (parserMode == "online") FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (parserMode == "online") (if (isDark) Color.White else Color(0xFF8B5CF6)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action buttons row: Paste Clipboard, Pick .txt, Clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        rawInputText = clip
                                        runParse(clip)
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("text/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import .TXT", fontSize = 12.sp)
                            }

                            if (rawInputText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        rawInputText = ""
                                        parsedQuestions = emptyList()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text input field or Preview toggle
                        if (parsedQuestions.isEmpty()) {
                            OutlinedTextField(
                                value = rawInputText,
                                onValueChange = {
                                    rawInputText = it
                                },
                                label = { Text("Questions Text (Q1. What is...? A) ... B) ... Ans: A)") },
                                placeholder = {
                                    Text(
                                        "1. The capital of Jammu & Kashmir in summer is:\nA) Jammu\nB) Srinagar\nC) Anantnag\nD) Leh\nAns: B\nExplanation: Srinagar is the summer capital."
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else LightBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { runParse(rawInputText, parserMode) },
                                enabled = rawInputText.isNotBlank() && !isParsing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (parserMode == "online") Color(0xFF7C3AED) else BrandPrimary
                                )
                            ) {
                                if (isParsing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (parserMode == "online") "Parsing with Online AI..." else "Parsing MCQs...")
                                } else {
                                    Icon(
                                        if (parserMode == "online") Icons.Default.AutoAwesome else Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (parserMode == "online") "Parse with Online AI (Gemini)" else "Parse with Offline Parser",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Parsed Results Preview
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = (if (parserMode == "online") Color(0xFF8B5CF6) else Color(0xFF10B981)).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, (if (parserMode == "online") Color(0xFF8B5CF6) else Color(0xFF10B981)).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (parserMode == "online") Icons.Default.AutoAwesome else Icons.Default.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (parserMode == "online") Color(0xFF8B5CF6) else Color(0xFF10B981)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${parsedQuestions.size} MCQs (${if (parserMode == "online") "Online AI" else "Offline"})",
                                            fontWeight = FontWeight.Bold,
                                            color = if (parserMode == "online") (if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)) else (if (isDark) Color(0xFF34D399) else Color(0xFF059669)),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (parserMode == "offline") {
                                        TextButton(
                                            onClick = {
                                                parserMode = "online"
                                                runParse(rawInputText, "online")
                                            },
                                            enabled = !isParsing
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF8B5CF6))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Try Online AI", fontSize = 11.5.sp, color = Color(0xFF8B5CF6))
                                        }
                                    } else {
                                        TextButton(
                                            onClick = {
                                                parserMode = "offline"
                                                runParse(rawInputText, "offline")
                                            },
                                            enabled = !isParsing
                                        ) {
                                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Try Offline", fontSize = 11.5.sp, color = Color(0xFF10B981))
                                        }
                                    }

                                    TextButton(onClick = { parsedQuestions = emptyList() }) {
                                        Text("Edit Raw", fontSize = 11.5.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(parsedQuestions) { idx, q ->
                                    val (fig, cleanQ) = remember(q.questionText) {
                                        com.example.ui.components.extractFigureBlock(q.questionText)
                                    }
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
                                        ),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LightBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            if (fig != null) {
                                                com.example.ui.components.FigureQuestionCard(
                                                    figure = fig,
                                                    isDark = isDark
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }
                                            com.example.ui.components.RichMathText(
                                                text = "${idx + 1}. ${cleanQ.ifBlank { q.questionText }}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = if (isDark) Color.White else LightTextPrimary,
                                                isDark = isDark
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            q.options.forEachIndexed { optIdx, opt ->
                                                val isCorrect = optIdx == q.correctOptionIndex
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${('A' + optIdx)}) ",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCorrect) Color(0xFF10B981) else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                                                    )
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        com.example.ui.components.RichMathText(
                                                            text = opt,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontSize = 11.5.sp,
                                                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                                            ),
                                                            color = if (isCorrect) Color(0xFF10B981) else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary),
                                                            isDark = isDark
                                                        )
                                                    }
                                                }
                                            }
                                            if (q.explanation.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                com.example.ui.components.RichMathText(
                                                    text = "💡 ${q.explanation}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                    isDark = isDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Import to Chapter Button
                            Button(
                                onClick = {
                                    val targetChapId = selectedChapterId
                                    if (targetChapId == null) {
                                        Toast.makeText(context, "Please select a target chapter first!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val chapTitle = chapters.find { it.id == targetChapId }?.title ?: "Chapter"
                                    viewModel.addQuestionsBatchToChapter(
                                        chapterId = targetChapId,
                                        newQuestions = parsedQuestions,
                                        replaceExisting = replaceExisting
                                    )
                                    Toast.makeText(context, "✓ Successfully added ${parsedQuestions.size} questions to $chapTitle!", Toast.LENGTH_LONG).show()
                                    onImported?.invoke(parsedQuestions.size, chapTitle)
                                    onDismiss()
                                },
                                enabled = selectedChapterId != null && parsedQuestions.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_import_questions_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save ${parsedQuestions.size} MCQs to Chapter", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    // TAB 1: Manual Single Question Add
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = manualQuestionText,
                            onValueChange = { manualQuestionText = it },
                            label = { Text("Question Statement") },
                            placeholder = { Text("Type question here...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            "OPTIONS (TAP CIRCLE TO SELECT CORRECT)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                fontSize = 10.5.sp
                            )
                        )

                        val opts = listOf(
                            manualOptA to { t: String -> manualOptA = t },
                            manualOptB to { t: String -> manualOptB = t },
                            manualOptC to { t: String -> manualOptC = t },
                            manualOptD to { t: String -> manualOptD = t }
                        )

                        opts.forEachIndexed { idx, (text, setter) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (manualCorrectIndex == idx) Color(0xFF10B981) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                    border = BorderStroke(1.dp, if (manualCorrectIndex == idx) Color(0xFF10B981) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { manualCorrectIndex = idx }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ('A' + idx).toString(),
                                            color = if (manualCorrectIndex == idx) Color.White else (if (isDark) Color.White else LightTextPrimary),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = setter,
                                    placeholder = { Text("Option ${('A' + idx)}") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = manualExplanation,
                            onValueChange = { manualExplanation = it },
                            label = { Text("Explanation (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val targetChapId = selectedChapterId
                                if (targetChapId == null) {
                                    Toast.makeText(context, "Please select a target chapter", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val optionsList = listOfNotNull(
                                    manualOptA.ifBlank { null },
                                    manualOptB.ifBlank { null },
                                    manualOptC.ifBlank { null },
                                    manualOptD.ifBlank { null }
                                )
                                val newQ = QuestionItem(
                                    id = java.util.UUID.randomUUID().toString(),
                                    questionText = manualQuestionText.trim(),
                                    options = optionsList,
                                    correctOptionIndex = manualCorrectIndex.coerceIn(0, optionsList.size - 1),
                                    explanation = manualExplanation.trim()
                                )
                                viewModel.addQuestionToChapter(targetChapId, newQ)
                                val chapTitle = chapters.find { it.id == targetChapId }?.title ?: "Chapter"
                                Toast.makeText(context, "✓ Added question to $chapTitle!", Toast.LENGTH_SHORT).show()

                                // Reset manual form
                                manualQuestionText = ""
                                manualOptA = ""
                                manualOptB = ""
                                manualOptC = ""
                                manualOptD = ""
                                manualExplanation = ""
                                onImported?.invoke(1, chapTitle)
                            },
                            enabled = selectedChapterId != null && manualQuestionText.isNotBlank() && manualOptA.isNotBlank() && manualOptB.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Save Single Question", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
