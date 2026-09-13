package com.example.ui.test

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.StudyAIService
import com.example.data.entity.TestAttemptEntity
import com.example.model.QuestionItem
import com.example.ui.components.ShareQuestionSheet
import com.example.ui.theme.*
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TestResultsScreen(
    attemptId: String,
    viewModel: PrepOSViewModel,
    onBackToHub: () -> Unit,
    onRetest: (List<QuestionItem>, String, String) -> Unit,
    onOpenAskAI: (initialPrompt: String) -> Unit
) {
    val isDark = isAppDarkTheme()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences by viewModel.preferences.collectAsState()
    val allAttempts by viewModel.allTestAttempts.collectAsState()

    var attempt by remember { mutableStateOf<TestAttemptEntity?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, WRONG, SKIPPED, CORRECT

    // Diagnostic Sheet & Share Sheet
    var showDiagnosticSheet by remember { mutableStateOf(false) }
    var questionToShare by remember { mutableStateOf<QuestionItem?>(null) }

    LaunchedEffect(attemptId) {
        viewModel.getTestAttempt(attemptId) { result ->
            attempt = result
        }
    }

    val curAttempt = attempt
    if (curAttempt == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF0B1120) else LightBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = if (isDark) Color(0xFF6366F1) else GoldAccent)
        }
        return
    }

    val questions = remember(curAttempt.questionsJson) {
        TestQuestionProvider.parseQuestionsFromJson(curAttempt.questionsJson)
    }

    // Partition questions by outcome: Wrong first, then Skipped, then Correct
    val wrongQuestions = remember(questions) {
        questions.filter { it.userSelectedOptionIndex != null && it.userSelectedOptionIndex != it.correctOptionIndex }
    }
    val skippedQuestions = remember(questions) {
        questions.filter { it.userSelectedOptionIndex == null }
    }
    val correctQuestions = remember(questions) {
        questions.filter { it.userSelectedOptionIndex != null && it.userSelectedOptionIndex == it.correctOptionIndex }
    }

    // Ordered list putting WRONG answers first, then Skipped, then Correct
    val orderedQuestions = remember(wrongQuestions, skippedQuestions, correctQuestions) {
        wrongQuestions + skippedQuestions + correctQuestions
    }

    val filteredQuestions = remember(selectedFilter, orderedQuestions, wrongQuestions, skippedQuestions, correctQuestions) {
        when (selectedFilter) {
            "WRONG" -> wrongQuestions
            "SKIPPED" -> skippedQuestions
            "CORRECT" -> correctQuestions
            else -> orderedQuestions
        }
    }

    Scaffold(
        containerColor = if (isDark) AppDarkBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Test Analysis & Results",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToHub) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else LightTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val summary = "I scored ${curAttempt.scorePercentage}% in ${curAttempt.testTitle} on PrepOS! (${curAttempt.correctAnswers}/${curAttempt.totalQuestions} correct in ${curAttempt.timeTakenSeconds / 60}m)"
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, summary)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Test Score"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) AppDarkBackground else LightSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = if (isDark) Color(0xFF0F172A) else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else GoldAccentBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (wrongQuestions.isNotEmpty()) {
                        Button(
                            onClick = {
                                onRetest(
                                    wrongQuestions,
                                    "${curAttempt.testTitle} • Mistakes Practice",
                                    "PRACTICE_TEST"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("retest_mistakes_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retest Mistakes (${wrongQuestions.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onRetest(questions, curAttempt.testTitle, curAttempt.testType)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LightBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color.White else LightTextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("retest_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retest All (${questions.size})", fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = onBackToHub,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF6366F1) else GoldAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("done_results_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Done", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PRIMARY SCORE & MASTERY CARD
            item {
                val scorePct = curAttempt.scorePercentage.toFloat().coerceIn(0f, 100f)
                val scoreColor = ProgressColor.forProgress(scorePct)
                val cardBorderColor = ProgressColor.border(scorePct, if (isDark) 0.45f else 0.35f)
                val fogGradient = ProgressColor.rememberAnimatedFogBrush(scorePct, isDark)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, cardBorderColor, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131D33) else LightSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(fogGradient)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = curAttempt.testTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Circular Score Metric Ring
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { (curAttempt.scorePercentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.size(110.dp),
                                    color = scoreColor,
                                    strokeWidth = 10.dp,
                                    trackColor = if (isDark) Color(0xFF1E293B) else LightSurfaceSecondary
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${curAttempt.scorePercentage}%",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color.White else LightTextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Accuracy",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Performance Tier Badge
                            val ratingText = when {
                                curAttempt.scorePercentage >= 80 -> "Mastered • Exam Ready"
                                curAttempt.scorePercentage >= 60 -> "Proficient • Good Progress"
                                else -> "Needs Practice • Review Weak Areas"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ProgressColor.softBg(scorePct, if (isDark) 0.18f else 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ProgressColor.border(scorePct, 0.5f))
                            ) {
                                Text(
                                    text = ratingText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Stats Summary Row: Correct, Wrong, Unanswered, Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ScoreMiniStat(label = "Correct", count = "${curAttempt.correctAnswers}", color = Color(0xFF10B981))
                                ScoreMiniStat(label = "Incorrect", count = "${curAttempt.wrongAnswers}", color = Color(0xFFEF4444))
                                ScoreMiniStat(label = "Skipped", count = "${curAttempt.unanswered}", color = if (isDark) Color(0xFF64748B) else LightTextMuted)
                                ScoreMiniStat(
                                    label = "Time",
                                    count = "${curAttempt.timeTakenSeconds / 60}m ${curAttempt.timeTakenSeconds % 60}s",
                                    color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark
                                )
                            }

                            // Immediate Quick Retest Bar if there are wrong or skipped questions
                            if (wrongQuestions.isNotEmpty() || skippedQuestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (wrongQuestions.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                onRetest(
                                                    wrongQuestions,
                                                    "${curAttempt.testTitle} • Mistakes Practice",
                                                    "PRACTICE_TEST"
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retest Wrong (${wrongQuestions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onRetest(questions, curAttempt.testTitle, curAttempt.testType)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF475569) else LightBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Retest All (${questions.size})", fontSize = 12.sp, color = if (isDark) Color.White else LightTextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. COMPACT & PROFESSIONAL AI DIAGNOSTICS CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDiagnosticSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.85f) else GoldAccentSoft
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.45f) else GoldAccentBorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.25f) else GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFC084FC) else GoldAccentDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Performance & Subject Diagnostics",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary,
                                    fontSize = 13.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Status across tests • Weakest chapters & focus plan",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFD8B4FE) else GoldAccentDark,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View Details",
                            tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 3. QUESTION-BY-QUESTION REVIEW HEADER & FILTER CHIPS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question Review (${questions.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        )
                        if (wrongQuestions.isNotEmpty()) {
                            Text(
                                text = "❌ Wrong answers prioritized",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${questions.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) Color(0xFF6366F1) else GoldAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                        if (wrongQuestions.isNotEmpty()) {
                            FilterChip(
                                selected = selectedFilter == "WRONG",
                                onClick = { selectedFilter = "WRONG" },
                                label = { Text("❌ Wrong (${wrongQuestions.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEF4444),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        if (skippedQuestions.isNotEmpty()) {
                            FilterChip(
                                selected = selectedFilter == "SKIPPED",
                                onClick = { selectedFilter = "SKIPPED" },
                                label = { Text("⚠️ Skipped (${skippedQuestions.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        FilterChip(
                            selected = selectedFilter == "CORRECT",
                            onClick = { selectedFilter = "CORRECT" },
                            label = { Text("✅ Correct (${correctQuestions.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 4. QUESTIONS LIST WITH DETAILED USER SELECTION & EXPLANATION
            itemsIndexed(filteredQuestions, key = { _, q -> q.id }) { qIndex, question ->
                val userChoice = question.userSelectedOptionIndex
                val isWrong = userChoice != null && userChoice != question.correctOptionIndex
                val isSkipped = userChoice == null
                val isCorrect = userChoice != null && userChoice == question.correctOptionIndex

                val cardBorderColor = when {
                    isWrong -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.5f else 0.4f)
                    isSkipped -> Color(0xFFF59E0B).copy(alpha = if (isDark) 0.4f else 0.35f)
                    else -> Color(0xFF10B981).copy(alpha = if (isDark) 0.35f else 0.3f)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131D33) else LightSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Outcome Status Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${qIndex + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark
                                )
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    isWrong -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    isSkipped -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> Color(0xFF10B981).copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isWrong -> "❌ Incorrect"
                                        isSkipped -> "⚠️ Skipped"
                                        else -> "✅ Correct"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isWrong -> Color(0xFFEF4444)
                                            isSkipped -> Color(0xFFF59E0B)
                                            else -> Color(0xFF10B981)
                                        },
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else LightTextPrimary,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Options display with user selection feedback
                        val prefixes = listOf("A", "B", "C", "D", "E")
                        question.options.forEachIndexed { optIdx, optText ->
                            val isCorrectOpt = optIdx == question.correctOptionIndex
                            val isUserSelected = optIdx == userChoice
                            val prefix = prefixes.getOrElse(optIdx) { "${optIdx + 1}" }

                            val optionBg = when {
                                isCorrectOpt -> Color(0xFF10B981).copy(alpha = if (isDark) 0.14f else 0.10f)
                                isUserSelected && !isCorrectOpt -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.14f else 0.10f)
                                else -> if (isDark) Color(0xFF0F172A) else LightSurfaceSecondary
                            }
                            val optionBorder = when {
                                isCorrectOpt -> Color(0xFF10B981).copy(alpha = 0.7f)
                                isUserSelected && !isCorrectOpt -> Color(0xFFEF4444).copy(alpha = 0.7f)
                                else -> if (isDark) Color(0xFF1E293B) else LightBorder
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(optionBg)
                                    .border(1.dp, optionBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$prefix. ",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isCorrectOpt -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                            isUserSelected && !isCorrectOpt -> Color(0xFFEF4444)
                                            else -> if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                        }
                                    )
                                )
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = when {
                                            isCorrectOpt -> if (isDark) Color.White else Color(0xFF065F46)
                                            isUserSelected && !isCorrectOpt -> if (isDark) Color.White else Color(0xFF991B1B)
                                            else -> if (isDark) Color(0xFFCBD5E1) else LightTextPrimary
                                        },
                                        fontWeight = if (isCorrectOpt || isUserSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                if (isCorrectOpt) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isUserSelected) "Your Choice ✓" else "Correct Answer",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isUserSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Your Choice ✗",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFEF4444),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Explanation Box
                        if (question.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else LightSurfaceSecondary,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else GoldAccentBorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFF59E0B) else GoldAccentDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Solution & Explanation",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFFCD34D) else GoldAccentDark,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = question.explanation,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                            lineHeight = 17.sp,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Action Buttons: Share Poster & Explain with AI
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { questionToShare = question }
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Share Poster",
                                    color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            TextButton(
                                onClick = {
                                    val prompt = "Explain why option ${prefixes.getOrElse(question.correctOptionIndex) { "A" }} is correct for this question: '${question.questionText}'. Break down the underlying principles."
                                    onOpenAskAI(prompt)
                                }
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFC084FC) else GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Explain with AI",
                                    color = if (isDark) Color(0xFFC084FC) else GoldAccent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 1: 4:3 Graphic Poster & Caption Share Sheet
    questionToShare?.let { q ->
        ShareQuestionSheet(
            item = q,
            examName = curAttempt.testTitle,
            chapterTitle = curAttempt.testTitle,
            onDismiss = { questionToShare = null }
        )
    }

    // Modal 2: Performance Diagnostic & Subject Weakness Sheet
    if (showDiagnosticSheet) {
        PrepOSPerformanceDiagnosticSheet(
            currentAttempt = curAttempt,
            allAttempts = allAttempts,
            userApiKey = preferences?.apiKey ?: "",
            onDismiss = { showDiagnosticSheet = false },
            onRetestSubject = { subjectName, attemptList ->
                showDiagnosticSheet = false
                val allSubjectQuestions = attemptList.flatMap {
                    TestQuestionProvider.parseQuestionsFromJson(it.questionsJson)
                }.distinctBy { it.id }
                if (allSubjectQuestions.isNotEmpty()) {
                    onRetest(
                        allSubjectQuestions.take(25),
                        "$subjectName • Diagnostic Practice",
                        "SUBJECT_TEST"
                    )
                } else {
                    Toast.makeText(context, "No saved questions for this subject yet.", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenAskAI = onOpenAskAI
        )
    }
}

/**
 * Diagnostic Bottom Sheet: Analyzes tests given per subject, weakest chapters, and gives targeted recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrepOSPerformanceDiagnosticSheet(
    currentAttempt: TestAttemptEntity,
    allAttempts: List<TestAttemptEntity>,
    userApiKey: String,
    onDismiss: () -> Unit,
    onRetestSubject: (String, List<TestAttemptEntity>) -> Unit,
    onOpenAskAI: (String) -> Unit
) {
    val isDark = isAppDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    var aiStrategyTip by remember { mutableStateOf<String?>(null) }
    var isLoadingTip by remember { mutableStateOf(false) }

    // Aggregate statistics across all attempts
    val totalTestsCount = allAttempts.size
    val totalQuestionsAnswered = allAttempts.sumOf { it.totalQuestions }
    val totalCorrectCount = allAttempts.sumOf { it.correctAnswers }
    val overallAccuracy = if (totalQuestionsAnswered > 0) {
        ((totalCorrectCount.toFloat() / totalQuestionsAnswered) * 100).toInt()
    } else {
        0
    }

    // Group attempts by subject
    val subjectGroups = remember(allAttempts) {
        allAttempts.groupBy {
            if (it.subjectName.isNotBlank()) it.subjectName else "General Studies"
        }.map { (subjName, attempts) ->
            val totalQ = attempts.sumOf { it.totalQuestions }
            val correctQ = attempts.sumOf { it.correctAnswers }
            val wrongQ = attempts.sumOf { it.wrongAnswers }
            val avgAcc = if (totalQ > 0) ((correctQ.toFloat() / totalQ) * 100).toInt() else 0
            SubjectDiagnosticItem(
                subjectName = subjName,
                testsGiven = attempts.size,
                totalQuestions = totalQ,
                correctQuestions = correctQ,
                wrongQuestions = wrongQ,
                averageAccuracy = avgAcc,
                attempts = attempts
            )
        }.sortedBy { it.averageAccuracy } // Weakest subjects first
    }

    // Group attempts by chapter to find weakest chapters
    val weakChapters = remember(allAttempts) {
        allAttempts
            .filter { it.chapterTitle.isNotBlank() }
            .groupBy { it.chapterTitle }
            .map { (chapTitle, attempts) ->
                val totalQ = attempts.sumOf { it.totalQuestions }
                val correctQ = attempts.sumOf { it.correctAnswers }
                val acc = if (totalQ > 0) ((correctQ.toFloat() / totalQ) * 100).toInt() else 0
                Pair(chapTitle, acc)
            }
            .filter { it.second < 75 }
            .sortedBy { it.second }
            .take(4)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF0F172A) else LightSurface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Performance Diagnostic",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        )
                        Text(
                            text = "Comprehensive status & targeted study focus",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else LightTextPrimary)
                    }
                }
            }

            // Summary Highlights Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiagnosticStatCard(
                        label = "Tests Taken",
                        value = "$totalTestsCount",
                        color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticStatCard(
                        label = "Questions",
                        value = "$totalQuestionsAnswered",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticStatCard(
                        label = "Avg Accuracy",
                        value = "$overallAccuracy%",
                        color = if (overallAccuracy >= 70) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Subject-wise Breakdown
            item {
                Text(
                    text = "Subject Performance Status (${subjectGroups.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary
                    )
                )
            }

            itemsIndexed(subjectGroups) { _, item ->
                val statusColor = when {
                    item.averageAccuracy >= 75 -> Color(0xFF10B981)
                    item.averageAccuracy >= 55 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131D33) else LightSurfaceSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else LightBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.subjectName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${item.averageAccuracy}% Accuracy",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (item.averageAccuracy / 100f).coerceIn(0.05f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(statusColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.testsGiven} test(s) • ${item.correctQuestions} correct • ${item.wrongQuestions} wrong",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                    fontSize = 11.sp
                                )
                            )

                            if (item.averageAccuracy < 75) {
                                TextButton(
                                    onClick = { onRetestSubject(item.subjectName, item.attempts) }
                                ) {
                                    Text("Practice Subject", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark)
                                }
                            }
                        }
                    }
                }
            }

            // Weak Chapters & Recommended Focus
            if (weakChapters.isNotEmpty()) {
                item {
                    Text(
                        text = "⚠️ Recommended Focus Areas",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary
                        )
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.6f) else GoldAccentSoft),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.3f) else GoldAccentBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            weakChapters.forEach { (chapter, acc) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = chapter,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else LightTextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$acc% acc",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Focus Tip (Lightweight & Token-Safe)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1B4B) else GoldAccentSoft),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.4f) else GoldAccentBorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (isDark) Color(0xFFC084FC) else GoldAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personalized AI Study Strategy",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (aiStrategyTip != null) {
                            Text(
                                text = aiStrategyTip ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                                    lineHeight = 18.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Get a concise 3-point strategy based on your test accuracy and weakest chapters without using heavy tokens.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    isLoadingTip = true
                                    coroutineScope.launch {
                                        val weakNames = weakChapters.joinToString { "${it.first} (${it.second}%)" }
                                        val prompt = "Student test summary: Tests taken: $totalTestsCount, Overall Accuracy: $overallAccuracy%. Weak areas: $weakNames. Give 3 crisp, high-impact study points in under 60 words."
                                        val result = StudyAIService.executeRawAiPrompt(
                                            prompt = prompt,
                                            apiKey = userApiKey
                                        )
                                        aiStrategyTip = result.getOrElse {
                                            "1. Focus 60% of study time on your lowest accuracy subjects.\n2. Retest wrong MCQs regularly with the Mistake Practice button.\n3. Revise summary formulas before attempting timed full mocks."
                                        }
                                        isLoadingTip = false
                                    }
                                },
                                enabled = !isLoadingTip,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF7C3AED) else GoldAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isLoadingTip) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generating...", fontSize = 12.sp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate AI Strategy Tip", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SubjectDiagnosticItem(
    val subjectName: String,
    val testsGiven: Int,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val averageAccuracy: Int,
    val attempts: List<TestAttemptEntity>
)

@Composable
private fun DiagnosticStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = if (isDark) 0.12f else 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun ScoreMiniStat(label: String, count: String, color: Color) {
    val isDark = isAppDarkTheme()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = if (isDark) 0.12f else 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier.width(72.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}
