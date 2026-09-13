package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.isAppDarkTheme
import kotlinx.coroutines.delay

private const val INSTAGRAM_URL = "https://www.instagram.com/tgcrizzu/"

/**
 * Premium App Opening / Splash Screen
 * Features:
 * - App icon with beautifully rounded corners (26.dp) & subtle glow border
 * - Scale & Spring entry animation
 * - "PrepOS" branding
 * - Animated "Made with ❤️ by Rizzu" footer with pulsing heart and shimmer
 */
@Composable
fun AppSplashScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDarkTheme()
    var startAnimation by remember { mutableStateOf(false) }

    // Trigger animations on launch
    LaunchedEffect(Unit) {
        startAnimation = true
        // Auto-dismiss after 1800ms
        delay(1800L)
        onDismiss()
    }

    // Spring scale and alpha for the icon
    val iconScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.6f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "icon_scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    // Infinite heartbeat pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_heart_pulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    // Shimmer glow pulse for brand highlight
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF090D16),
                Color(0xFF05070B)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC),
                Color(0xFFEEF2F6)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Main Center Content: Icon + App Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(iconScale)
                .alpha(contentAlpha)
                .padding(horizontal = 24.dp)
        ) {
            // App Icon Container with Rounded Corners & Glow
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .shadow(
                        elevation = if (isDark) 24.dp else 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = BrandPrimary.copy(alpha = glowAlpha)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.8.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                BrandPrimary.copy(alpha = glowAlpha),
                                GoldAccent.copy(alpha = glowAlpha * 0.8f)
                            )
                        )
                    ),
                    modifier = Modifier.size(116.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_prepos_icon),
                            contentDescription = "PrepOS Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // App Name
            Text(
                text = "PrepOS",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = if (isDark) Color.White else LightTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Tagline
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else LightSurfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                )
            ) {
                Text(
                    text = "Intelligent Study Companion",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Bottom Animated Footer: "Made with ❤️ by Rizzu"
        AnimatedVisibility(
            visible = startAnimation,
            enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                initialOffsetY = { it / 2 }
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            SplashAnimatedFooter(
                isDark = isDark,
                heartScale = heartScale
            )
        }
    }
}

/**
 * Animated "Made with ❤️ by Rizzu" Footer with pulsing heart and interactive link
 */
@Composable
private fun SplashAnimatedFooter(
    isDark: Boolean,
    heartScale: Float
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val openInstagramProfile = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                uriHandler.openUri(INSTAGRAM_URL)
            } catch (_: Exception) {
                // Ignore fallback
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isDark) 6.dp else 4.dp,
        modifier = Modifier.testTag("splash_footer_container")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Made with",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Pulsing Heart Icon
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Love",
                tint = Color(0xFFEF4444),
                modifier = Modifier
                    .size(14.dp)
                    .scale(heartScale)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "by",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color(0xFFCBD5E1) else LightTextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.width(5.dp))

            // Glowing / Clickable Rizzu Link
            Text(
                text = "Rizzu",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFE1306C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp),
                        onClick = openInstagramProfile
                    )
                    .testTag("splash_footer_rizzu_link")
            )
        }
    }
}
