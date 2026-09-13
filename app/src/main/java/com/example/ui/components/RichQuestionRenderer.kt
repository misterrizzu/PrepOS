package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuestionItem
import com.example.ui.theme.isAppDarkTheme

/**
 * Universal Rich Question Renderer for PrepOS.
 * Renders mathematical equations, vertical fractions, radicals, subscripts, superscripts,
 * visual reasoning figures, option letters, and interactive check/reveal states.
 */
@Composable
fun RichQuestionCard(
    item: QuestionItem,
    index: Int,
    modifier: Modifier = Modifier,
    isShared: Boolean = false,
    selectedOption: Int? = null,
    showSolutionImmediately: Boolean = false,
    isTestMode: Boolean = false,
    onOptionSelected: ((Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val isDark = isAppDarkTheme()
    var localSelectedOption by remember { mutableStateOf(selectedOption) }
    var showExplanation by remember { mutableStateOf(showSolutionImmediately) }

    val currentSelected = selectedOption ?: localSelectedOption
    val (figure, cleanQuestionStem) = remember(item.questionText) {
        extractFigureBlock(item.questionText)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rich_question_card_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Question Number Badge & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4338CA) else Color(0xFFC7D2FE))
                ) {
                    Text(
                        text = "Q${index + 1}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onShare != null) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("share_question_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Question",
                                tint = if (isShared) Color(0xFF10B981) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_question_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Question",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Figure Display if present
            if (figure != null) {
                FigureQuestionCard(
                    figure = figure,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Question Stem with Rich Math & Formatting
            RichMathText(
                text = cleanQuestionStem.ifBlank { item.questionText },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp,
                    lineHeight = 22.sp
                ),
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Options List
            val optionLabels = listOf("A", "B", "C", "D")
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item.options.forEachIndexed { optIndex, optText ->
                    val isOptionSelected = currentSelected == optIndex
                    val isCorrectOption = optIndex == item.correctOptionIndex
                    val revealMode = showExplanation || (!isTestMode && currentSelected != null)

                    val (containerColor, borderColor, textColor, badgeColor, badgeTextColor) = when {
                        revealMode && isCorrectOption -> {
                            // Correct Option Highlight
                            Tuple5(
                                if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7),
                                if (isDark) Color(0xFF059669) else Color(0xFF16A34A),
                                if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D),
                                if (isDark) Color(0xFF059669) else Color(0xFF16A34A),
                                Color.White
                            )
                        }
                        revealMode && isOptionSelected && !isCorrectOption -> {
                            // Wrong Selected Option Highlight
                            Tuple5(
                                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEE2E2),
                                if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444),
                                if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444),
                                Color.White
                            )
                        }
                        isOptionSelected -> {
                            // Selected in Test Mode
                            Tuple5(
                                if (isDark) Color(0xFF312E81).copy(alpha = 0.6f) else Color(0xFFEEF2FF),
                                if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5),
                                if (isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3),
                                if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5),
                                Color.White
                            )
                        }
                        else -> {
                            // Default Unselected Option
                            Tuple5(
                                if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (onOptionSelected != null) {
                                    onOptionSelected(optIndex)
                                } else {
                                    localSelectedOption = optIndex
                                    showExplanation = true
                                }
                            }
                            .testTag("question_${index}_option_$optIndex"),
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(1.2.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Option Label Badge (A, B, C, D)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (revealMode && isCorrectOption) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Correct",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (revealMode && isOptionSelected && !isCorrectOption) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Wrong",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = optionLabels.getOrElse(optIndex) { "${optIndex + 1}" },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Option text with rich math rendering
                            Box(modifier = Modifier.weight(1f)) {
                                RichMathText(
                                    text = optText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isOptionSelected || (revealMode && isCorrectOption)) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    ),
                                    color = textColor,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }

            // Explanation / Solution Panel
            val shouldShowExp = (showExplanation || (!isTestMode && currentSelected != null)) && item.explanation.isNotBlank()

            AnimatedVisibility(
                visible = shouldShowExp,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.4f) else Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Explanation & Solution",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                                        fontSize = 11.5.sp
                                    )
                                )
                            }

                            RichMathText(
                                text = item.explanation,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 19.sp,
                                    fontSize = 13.sp
                                ),
                                color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF166534),
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
