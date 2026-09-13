package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.isAppDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.StatusDanger

/**
 * Top Header for PrepOS Home Screen
 * Displays Greeting, User Name, Subtitle, and Quick Action Icons (Search, Notifications, Settings)
 */
@Composable
fun HomeHeader(
    userName: String,
    unreadNotifCount: Int,
    currentThemeMode: String = "SYSTEM",
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()
    val displayName = if (userName.isNotBlank()) userName else "Alex"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Greeting & Subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Hi, $displayName",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = if (isDark) Color.White else LightTextPrimary,
                modifier = Modifier.testTag("home_greeting_text")
            )
            Text(
                text = "Let's make today count!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                modifier = Modifier.testTag("home_subtitle_text")
            )
        }

        // Right: Theme Switcher, Notification, Settings icon buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme Switcher: Cycles through Auto (System) -> Dark -> Light -> Auto
            val (themeIcon, themeDesc) = when (currentThemeMode.uppercase()) {
                "DARK" -> Pair(Icons.Outlined.DarkMode, "Dark Mode (Click for Light)")
                "LIGHT" -> Pair(Icons.Outlined.LightMode, "Light Mode (Click for Auto)")
                else -> Pair(Icons.Outlined.BrightnessAuto, "Auto Theme (Click for Dark)")
            }

            HeaderIconButton(
                icon = themeIcon,
                contentDescription = themeDesc,
                testTag = "home_btn_theme_toggle",
                isDark = isDark,
                onClick = onToggleTheme
            )

            // Notifications Icon (with badge)
            Box {
                HeaderIconButton(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    testTag = "home_btn_notifications",
                    isDark = isDark,
                    onClick = onOpenNotifications
                )
                if (unreadNotifCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .background(StatusDanger, CircleShape)
                            .border(1.5.dp, if (isDark) Color(0xFF0F172A) else LightSurface, CircleShape)
                    )
                }
            }

            // Settings Icon
            HeaderIconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings",
                testTag = "home_btn_settings",
                isDark = isDark,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else LightSurfaceSecondary)
            .border(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else LightBorder, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

