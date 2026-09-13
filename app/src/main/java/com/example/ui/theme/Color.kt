package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// LIGHT THEME TOKENS
// ==========================================
val LightBackground       = Color(0xFFF6F7FB)
val LightSurface          = Color(0xFFFFFFFF)
val LightSurfaceSecondary = Color(0xFFF1F3F8)
val LightSurfaceInput     = Color(0xFFF4F6FA)

val LightTextPrimary      = Color(0xFF172033)
val LightTextSecondary    = Color(0xFF657085)
val LightTextMuted        = Color(0xFF8E98AA)
val LightTextDisabled     = Color(0xFFB5BDCA)

val LightBorder           = Color(0xFFE3E8F0)
val LightBorderStrong     = Color(0xFFD8DEE9)

// Brand
val BrandPrimary          = Color(0xFF6C63D9)
val BrandPrimaryDark      = Color(0xFF574FC3)
val BrandPrimaryLight     = Color(0xFFEEE9FF)

// Subject Base Colors
val SubjectBlue           = Color(0xFF3D7FE8)
val SubjectBlueSoft       = Color(0xFFEAF2FF)
val SubjectPink           = Color(0xFFD94D91)
val SubjectPinkSoft       = Color(0xFFFDEAF3)
val SubjectGreen          = Color(0xFF2FA88A)
val SubjectGreenSoft      = Color(0xFFE7F7F1)
val SubjectPurple         = Color(0xFFB6579A)
val SubjectPurpleSoft     = Color(0xFFF7EAF3)
val SubjectGold           = Color(0xFFC9923E)
val SubjectGoldSoft       = Color(0xFFFFF4E5)
val SubjectGoldBorder     = Color(0xFFEAC982)

// Soft Premium Gold Accents
val GoldAccent            = Color(0xFFC9923E)
val GoldAccentDark        = Color(0xFFA5732A)
val GoldAccentLight       = Color(0xFFFFF7ED)
val GoldAccentSoft        = Color(0xFFFDF3E1)
val GoldAccentBorder      = Color(0xFFE8CFA0)
val GoldAccentBorderSubtle= Color(0xFFF4E4C6)

// AI Assistant Purple Palette
val AiPrimary             = Color(0xFF6C63D9)  // brand purple
val AiPrimaryDark         = Color(0xFF4B38B7)  // deep purple
val AiPrimaryLight        = Color(0xFFEEE9FF)  // soft purple bg
val AiPrimarySoft         = Color(0xFFF4F0FF)  // very soft purple surface
val AiPrimaryBorder       = Color(0xFF8B7FE8)  // selected border
val AiPrimaryBorderSubtle = Color(0xFFD6CEF8)  // subtle border

// Status
val StatusDanger          = Color(0xFFD95767)
val StatusDangerSoft      = Color(0xFFFDECEE)
val StatusSuccess         = Color(0xFF2FA88A)
val StatusSuccessSoft     = Color(0xFFE8F7F1)

// Subject Palette List (8 user-selectable colors)
val SubjectPaletteList = listOf(
    "#3D7FE8", // Blue
    "#D94D91", // Pink
    "#6C63D9", // Purple
    "#2FA88A", // Green
    "#249C8B", // Teal
    "#C9923E", // Orange
    "#D95767", // Red
    "#B6579A"  // Magenta
)

fun subjectColorFromHex(hex: String?): Color {
    if (hex.isNullOrBlank()) return SubjectBlue
    return try {
        val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleanHex))
    } catch (e: Exception) {
        SubjectBlue
    }
}

fun softBackground(baseColor: Color): Color = baseColor.copy(alpha = 0.10f)
fun softBorder(baseColor: Color): Color = baseColor.copy(alpha = 0.30f)

// ==========================================
// LEGACY & DARK THEME BRANDING
// ==========================================
val PrepOSPrimary = BrandPrimary
val PrepOSPrimaryDark = Color(0xFF60A5FA)
val PrepOSSecondary = Color(0xFF0D9488)
val PrepOSSecondaryDark = Color(0xFF2DD4BF)
val PrepOSTertiary = Color(0xFFD97706)

// Paper Background Colors
val PaperWhiteBg = Color(0xFFFAF9F6) // Warm off-white
val PaperWhiteLine = Color(0xFFE2E8F0)
val PaperWhiteMarginLine = Color(0xFFFCA5A5) // Soft pinkish margin

val SepiaPaperBg = Color(0xFFFBF0D9) // Classic warm reading sepia
val SepiaPaperLine = Color(0xFFE6D6B8)
val SepiaPaperMarginLine = Color(0xFFD9A075)

val SlatePaperBg = Color(0xFF1E293B) // Modern soft dark slate
val SlatePaperLine = Color(0xFF334155)
val SlatePaperMarginLine = Color(0xFF475569)

val MidnightPaperBg = Color(0xFF090D16) // Deep AMOLED Night
val MidnightPaperLine = Color(0xFF1E293B)
val MidnightPaperMarginLine = Color(0xFF253347)

// Text Colors
val TextPaperLight = Color(0xFF0F172A)
val TextPaperLightSecondary = Color(0xFF475569)
val TextSepia = Color(0xFF2C2216)
val TextSepiaSecondary = Color(0xFF6B5844)
val TextDark = Color(0xFFF1F5F9)
val TextDarkSecondary = Color(0xFF94A3B8)

// Highlight Colors
val HighlightYellow = Color(0x66FEF08A)
val HighlightGreen = Color(0x66BBF7D0)
val HighlightBlue = Color(0x66BAE6FD)
val HighlightPink = Color(0x66FBCFE8)
val HighlightPurple = Color(0x66E9D5FF)

// ==========================================
// UNIFIED APP DARK PALETTE (Deep Slate Obsidian)
// ==========================================
val AppDarkBackground        = Color(0xFF0B0F19) // Unified deep canvas for all screens
val AppDarkSurface           = Color(0xFF131B2E) // Primary card/surface container
val AppDarkSurfaceVariant    = Color(0xFF1A243B) // Elevated chip/inner card surface
val AppDarkBorder            = Color(0xFF22304C) // Crisp border outline
val AppDarkBorderSubtle      = Color(0xFF192438) // Divider/subtle border
val AppDarkTextPrimary       = Color(0xFFF1F5F9) // White-slate text
val AppDarkTextSecondary     = Color(0xFF94A3B8) // Slate secondary text
val AppDarkTextMuted         = Color(0xFF64748B) // Subtle muted text

