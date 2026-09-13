package com.example.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.ProgressColor
import com.example.ui.theme.isAppDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.viewmodel.ActiveFocusSession

/**
 * Ultra-Compact Resume Study / Reading Card for PrepOS Home Screen
 */
@Composable
fun HomeResumeCard(
    recentChapters: List<ChapterEntity>,
    subjects: List<SubjectEntity>,
    onOpenChapter: (chapterId: String) -> Unit,
    onStartFocus: (chapterId: String, subjectName: String, chapterTitle: String) -> Unit,
    onSeeAll: () -> Unit,
    activeFocusSession: ActiveFocusSession? = null,
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()
    val activeChapter = recentChapters.firstOrNull()
    val parentSubject = subjects.find { it.id == activeChapter?.subjectId }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RESUME STUDY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 11.sp
                ),
                color = if (isDark) Color(0xFF64748B) else LightTextSecondary
            )
            if (recentChapters.size > 1) {
                Text(
                    text = "All notes",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF38BDF8) else BrandPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .clickable(onClick = onSeeAll)
                        .testTag("home_resume_see_all")
                )
            }
        }

        if (activeChapter == null) {
            // Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else LightBorder,
                        RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else LightSurface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a subject to begin studying",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF64748B) else LightTextMuted
                    )
                }
            }
        } else {
            val progressPercent = if (activeChapter.readingProgress >= 0.98f) 100 else kotlin.math.round(activeChapter.readingProgress * 100).toInt().coerceIn(0, 100)
            val progressFloat = progressPercent.toFloat()
            val progressThemeColor = ProgressColor.forProgress(progressFloat)
            val animatedProgress by animateFloatAsState(
                targetValue = activeChapter.readingProgress.coerceIn(0f, 1f),
                label = "resume_progress"
            )
            val subjectColor = try {
                Color(android.graphics.Color.parseColor(parentSubject?.colorHex ?: "#3B82F6"))
            } catch (e: Exception) {
                BrandPrimary
            }

            val resumeCardBorder = BorderStroke(1.2.dp, ProgressColor.border(progressFloat, if (isDark) 0.35f else 0.25f))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                border = resumeCardBorder,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ProgressColor.rememberAnimatedFogBrush(progressFloat, isDark))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    // Row 1: Subject Tag + Progress %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ProgressColor.softBg(progressFloat, if (isDark) 0.20f else 0.12f))
                                .border(
                                    0.8.dp,
                                    ProgressColor.border(progressFloat, if (isDark) 0.40f else 0.30f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = parentSubject?.name ?: "Study Notes",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = progressThemeColor
                            )
                        }

                        Text(
                            text = "$progressPercent% read",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = progressThemeColor
                        )
                    }

                    // Row 2: Title & Progress Bar
                    Text(
                        text = activeChapter.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = if (isDark) Color.White else LightTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Slim Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEDF0F5))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = animatedProgress.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(progressThemeColor)
                        )
                    }

                    // Row 3: Compact Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Compact Continue Reading Button
                        Button(
                            onClick = { onOpenChapter(activeChapter.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("home_resume_btn_continue"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF2563EB) else BrandPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Continue",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = Color.White
                            )
                        }

                        // Compact Focus Button / Live Countdown Display
                        val isFocusActive = activeFocusSession?.isActive == true
                        val focusLabel = if (isFocusActive && activeFocusSession != null) {
                            if (activeFocusSession.isTimerMode) {
                                val rem = activeFocusSession.remainingSeconds
                                String.format("%02d:%02d", rem / 60, rem % 60)
                            } else {
                                val el = activeFocusSession.elapsedSeconds
                                String.format("%02d:%02d", el / 60, el % 60)
                            }
                        } else "Focus"

                        OutlinedButton(
                            onClick = {
                                if (isFocusActive) {
                                    if (activeFocusSession?.isPaused == true) onResumeFocus() else onPauseFocus()
                                } else {
                                    onStartFocus(
                                        activeChapter.id,
                                        parentSubject?.name ?: "General Studies",
                                        activeChapter.title
                                    )
                                }
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("home_resume_btn_focus"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDark) {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFF78350F) else Color(0xFF2E1065)
                                    } else Color.Transparent
                                } else {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFFFF4E5) else Color(0xFFEEE9FF)
                                    } else LightSurfaceSecondary
                                },
                                contentColor = if (isDark) {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFFBBF24) else Color(0xFFC084FC)
                                    } else Color(0xFF38BDF8)
                                } else {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFC9923E) else BrandPrimary
                                    } else BrandPrimary
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFF59E0B) else Color(0xFFA855F7)
                                    } else Color(0xFF38BDF8).copy(alpha = 0.5f)
                                } else {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFC9923E) else BrandPrimary
                                    } else LightBorder
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isFocusActive) {
                                    if (activeFocusSession?.isPaused == true) Icons.Filled.PlayArrow else Icons.Filled.Pause
                                } else Icons.Filled.Timer,
                                contentDescription = null,
                                tint = if (isDark) {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFFBBF24) else Color(0xFFC084FC)
                                    } else Color(0xFF38BDF8)
                                } else {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFC9923E) else BrandPrimary
                                    } else BrandPrimary
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFocusActive) "${if (activeFocusSession?.isTimerMode == true) "⏳" else "⏱️"} $focusLabel" else "Focus",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = if (isDark) {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFFDE68A) else Color(0xFFE9D5FF)
                                    } else Color(0xFF38BDF8)
                                } else {
                                    if (isFocusActive) {
                                        if (activeFocusSession?.isPaused == true) Color(0xFFC9923E) else BrandPrimary
                                    } else BrandPrimary
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

