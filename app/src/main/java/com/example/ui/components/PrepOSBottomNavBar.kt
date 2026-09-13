package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppDarkTheme

/**
 * PrepOS Compact & Mini Independent Bottom Navigation Bar
 * Features independent floating buttons (not a single heavy card)
 * with responsive touch interactions, crisp contrast, and mini footprint.
 */
@Composable
fun PrepOSBottomNavBar(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onOpenAskAI: () -> Unit = { onSelectTab("ask_ai") },
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()

    // Subtle ambient breathing for AI center pill
    val infiniteTransition = rememberInfiniteTransition(label = "ai_idle_breathe")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_breathe"
    )

    // Transparent floating container holding independent buttons
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Independent Home Button
            MiniNavButton(
                iconSelected = Icons.Filled.Home,
                iconUnselected = Icons.Outlined.Home,
                label = "Home",
                isSelected = selectedTab == "home",
                isDark = isDark,
                onClick = { onSelectTab("home") },
                modifier = Modifier.testTag("nav_item_home")
            )

            // 2. Independent Study Button
            MiniNavButton(
                iconSelected = Icons.Filled.MenuBook,
                iconUnselected = Icons.Outlined.MenuBook,
                label = "Study",
                isSelected = selectedTab == "study",
                isDark = isDark,
                onClick = { onSelectTab("study") },
                modifier = Modifier.testTag("nav_item_study")
            )

            // 3. Independent Center Ask AI Button
            MiniAiButton(
                isActive = selectedTab == "ask_ai",
                isDark = isDark,
                breathingScale = breathingScale,
                onClick = {
                    onSelectTab("ask_ai")
                    onOpenAskAI()
                },
                modifier = Modifier.testTag("nav_item_ask_ai")
            )

            // 4. Independent Plan Button
            MiniNavButton(
                iconSelected = Icons.Filled.DateRange,
                iconUnselected = Icons.Outlined.DateRange,
                label = "Plan",
                isSelected = selectedTab == "plan",
                isDark = isDark,
                onClick = { onSelectTab("plan") },
                modifier = Modifier.testTag("nav_item_plan")
            )

            // 5. Independent Test Button
            MiniNavButton(
                iconSelected = Icons.Filled.Quiz,
                iconUnselected = Icons.Outlined.Quiz,
                label = "Test",
                isSelected = selectedTab == "practice",
                isDark = isDark,
                onClick = { onSelectTab("practice") },
                modifier = Modifier.testTag("nav_item_practice")
            )
        }
    }
}

/**
 * Independent Mini Navigation Button
 */
@Composable
fun MiniNavButton(
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "mini_nav_press"
    )

    // Independent button styling
    val bgColor = if (isSelected) {
        if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF)
    } else {
        if (isDark) Color(0xE60F172A) else Color(0xFAFFFFFF)
    }

    val borderColor = if (isSelected) {
        if (isDark) Color(0xFF6366F1).copy(alpha = 0.8f) else Color(0xFF6366F1).copy(alpha = 0.5f)
    } else {
        if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    }

    val contentColor = if (isSelected) {
        if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (isSelected) 4.dp else 2.dp,
        modifier = modifier
            .scale(scale)
            .height(42.dp)
            .sizeIn(minWidth = 44.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (isSelected) 10.dp else 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Independent Center Ask AI Mini Button
 */
@Composable
private fun MiniAiButton(
    isActive: Boolean,
    isDark: Boolean,
    breathingScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "ai_press_scale"
    )

    val currentScale = (if (isActive) 1.04f else breathingScale) * pressScale

    val gradientColors = if (isActive) {
        listOf(Color(0xFFA855F7), Color(0xFF6366F1))
    } else {
        if (isDark) {
            listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
        } else {
            listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE))
        }
    }

    val borderColor = if (isActive) {
        Color(0xFFC084FC)
    } else {
        if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFF8B5CF6).copy(alpha = 0.4f)
    }

    val iconColor = if (isActive) {
        Color.White
    } else {
        if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (isActive) 5.dp else 2.dp,
        modifier = modifier
            .scale(currentScale)
            .height(42.dp)
            .sizeIn(minWidth = 46.dp)
            .background(Brush.linearGradient(gradientColors), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Ask AI",
                tint = iconColor,
                modifier = Modifier.size(17.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "AI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = iconColor,
                    fontSize = 11.5.sp
                )
            )
        }
    }
}
