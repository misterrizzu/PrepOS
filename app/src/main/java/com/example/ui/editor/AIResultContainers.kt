package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocElement
import com.example.ui.components.RichMathText
import com.example.ui.theme.*

// -----------------------------------------------------------------------------------------
// ELEVATED AI RESULT CONTAINER DISPATCHER
// -----------------------------------------------------------------------------------------
@Composable
fun AiResultArtifactContainer(
    artifact: AiResultArtifact,
    isDark: Boolean,
    onApprove: () -> Unit,
    onMakeChanges: () -> Unit,
    onInsert: (() -> Unit)? = null,
    onReplace: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (artifact) {
        is AiResultArtifact.WeeklyPlan -> {
            StudyPlanResultContainer(
                plan = artifact,
                isDark = isDark,
                onApprove = onApprove,
                onMakeChanges = onMakeChanges,
                modifier = modifier
            )
        }
        is AiResultArtifact.StructuredNotes -> {
            StructuredNoteResultContainer(
                notes = artifact,
                isDark = isDark,
                onSave = onApprove,
                onMakeChanges = onMakeChanges,
                onInsert = onInsert,
                onReplace = onReplace,
                onDuplicate = onDuplicate,
                modifier = modifier
            )
        }
        is AiResultArtifact.SyllabusStructure -> {
            SyllabusResultContainer(
                syllabus = artifact,
                isDark = isDark,
                onApprove = onApprove,
                onMakeChanges = onMakeChanges,
                modifier = modifier
            )
        }
        is AiResultArtifact.ExamStrategy -> {
            ExamStrategyResultContainer(
                strategy = artifact,
                isDark = isDark,
                onApprove = onApprove,
                onMakeChanges = onMakeChanges,
                modifier = modifier
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// 1. STUDY PLAN RESULT CONTAINER
// -----------------------------------------------------------------------------------------
@Composable
fun StudyPlanResultContainer(
    plan: AiResultArtifact.WeeklyPlan,
    isDark: Boolean,
    onApprove: () -> Unit,
    onMakeChanges: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    val containerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val primaryColor = if (isDark) Color(0xFF8B5CF6) else AiPrimaryDark

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = BorderStroke(1.5.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFF818CF8).copy(alpha = 0.4f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = plan.planTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else LightTextPrimary
                        )
                        Text(
                            text = "7-Day Personalized Schedule",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4F46E5) else Color(0xFFC7D2FE)),
                    modifier = Modifier.clickable {
                        val textToCopy = if (plan.rawPlanText.isNotBlank()) plan.rawPlanText else {
                            plan.days.joinToString("\n\n") { day ->
                                "${day.dayName} (${day.totalTime}):\n" +
                                        day.slots.joinToString("\n") { "  • ${it.timeSlot}: ${it.subjectName} - ${it.taskTitle}" }
                            }
                        }
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        showCopied = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = primaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCopied) "Copied" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day Selector Row
            if (plan.days.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    plan.days.forEachIndexed { idx, day ->
                        val isSelected = selectedDayIndex == idx
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) primaryColor else cardBg,
                            border = BorderStroke(1.dp, if (isSelected) primaryColor else borderColor),
                            modifier = Modifier.clickable { selectedDayIndex = idx }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.dayName.take(3),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFFE2E8F0) else LightTextPrimary)
                                )
                                Text(
                                    text = day.totalTime,
                                    fontSize = 9.5.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else (if (isDark) Color(0xFF94A3B8) else LightTextSecondary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Day Slots Display
                val activeDay = plan.days.getOrNull(selectedDayIndex)
                if (activeDay != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeDay.slots.forEach { slot ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                when (slot.taskType.uppercase()) {
                                                    "MOCK", "TEST" -> Color(0xFFEF4444)
                                                    "REVISION" -> Color(0xFFF59E0B)
                                                    "PRACTICE" -> Color(0xFF10B981)
                                                    else -> primaryColor
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = slot.subjectName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color.White else LightTextPrimary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    text = slot.timeSlot,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = slot.taskTitle,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (plan.rawPlanText.isNotBlank()) {
                // Fallback structured markdown renderer if slots array is raw
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        RichMathText(
                            text = plan.rawPlanText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp
                            ),
                            color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                            isDark = isDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Approve & Make Changes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMakeChanges,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Make Changes",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Make Changes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Approve",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Approve Plan ✓", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. STRUCTURED NOTE RESULT CONTAINER (CANONICAL MODEL)
// -----------------------------------------------------------------------------------------
@Composable
fun StructuredNoteResultContainer(
    notes: AiResultArtifact.StructuredNotes,
    isDark: Boolean,
    onSave: () -> Unit,
    onMakeChanges: () -> Unit,
    onInsert: (() -> Unit)? = null,
    onReplace: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    val containerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val primaryColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = BorderStroke(1.5.dp, if (isDark) Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0xFF38BDF8).copy(alpha = 0.4f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (notes.subjectName.isNotBlank()) "${notes.subjectName} • ${notes.chapterTitle.ifBlank { "Structured Notes" }}" else notes.chapterTitle.ifBlank { "Structured Study Notes" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else LightTextPrimary
                        )
                        Text(
                            text = if (notes.subjectName.isNotBlank()) "Target: ${notes.subjectName} → ${notes.chapterTitle} • High-Yield Format" else "Preserved Source • High Yield Structure",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF0284C7) else Color(0xFFBAE6FD)),
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(notes.rawMarkdownNotes))
                        showCopied = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = primaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCopied) "Copied" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Note Content Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    RichMathText(
                        text = notes.rawMarkdownNotes,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        ),
                        color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMakeChanges,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Make Changes",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Make Changes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Notes",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Approve / Save ✓", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 3. SYLLABUS / FOLDER STRUCTURE CONTAINER
// -----------------------------------------------------------------------------------------
@Composable
fun SyllabusResultContainer(
    syllabus: AiResultArtifact.SyllabusStructure,
    isDark: Boolean,
    onApprove: () -> Unit,
    onMakeChanges: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSubjectIndex by remember { mutableIntStateOf(0) }
    val containerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val primaryColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = BorderStroke(1.5.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFF34D399).copy(alpha = 0.4f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF059669), Color(0xFF10B981))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Syllabus Workspace Structure",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else LightTextPrimary
                    )
                    Text(
                        text = "${syllabus.examName} • ${syllabus.subjects.size} Subject Folders",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subjects & Chapters List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                syllabus.subjects.forEachIndexed { subIdx, subject ->
                    val isExpanded = expandedSubjectIndex == subIdx
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, if (isExpanded) primaryColor else borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSubjectIndex = if (isExpanded) -1 else subIdx }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subject.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${subject.chapters.size} chapters",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    subject.chapters.forEach { chapter ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                color = primaryColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = chapter.title,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                                                )
                                                if (chapter.topics.isNotEmpty()) {
                                                    Text(
                                                        text = chapter.topics.joinToString(", "),
                                                        fontSize = 11.sp,
                                                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMakeChanges,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Make Changes",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Make Changes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Create Structure",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Create Structure ✓", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. EXAM STRATEGY CONTAINER
// -----------------------------------------------------------------------------------------
@Composable
fun ExamStrategyResultContainer(
    strategy: AiResultArtifact.ExamStrategy,
    isDark: Boolean,
    onApprove: () -> Unit,
    onMakeChanges: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val primaryColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = BorderStroke(1.5.dp, if (isDark) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFFFBBF24).copy(alpha = 0.4f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Exam Strategy Roadmap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else LightTextPrimary
                    )
                    Text(
                        text = "${strategy.examName} • ${strategy.daysRemaining} Days Remaining",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Phases Column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                strategy.phases.forEachIndexed { idx, phase ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phase.phaseName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = phase.durationWeeks,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = phase.focusDescription,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary
                            )
                            if (phase.dailyAction.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚡ Daily: ${phase.dailyAction}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMakeChanges,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Make Changes",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Make Changes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Confirm Strategy ✓", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
