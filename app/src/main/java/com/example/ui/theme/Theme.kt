package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isAppDarkTheme(): Boolean = LocalDarkTheme.current

private val DarkColorScheme = darkColorScheme(
    primary = PrepOSPrimaryDark,
    secondary = PrepOSSecondaryDark,
    tertiary = PrepOSTertiary,
    background = AppDarkBackground,
    surface = AppDarkSurface,
    onPrimary = Color(0xFF0B0F19),
    onSecondary = Color(0xFF0B0F19),
    onBackground = AppDarkTextPrimary,
    onSurface = AppDarkTextPrimary,
    surfaceVariant = AppDarkSurfaceVariant,
    onSurfaceVariant = AppDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = SubjectBlue,
    onSecondary = Color.White,
    secondaryContainer = SubjectBlueSoft,
    onSecondaryContainer = SubjectBlue,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldAccentLight,
    onTertiaryContainer = GoldAccentDark,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderStrong
)

@Composable
fun PrepOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent high-polish study branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
