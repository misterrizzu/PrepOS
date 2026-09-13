package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val INSTAGRAM_URL = "https://www.instagram.com/tgcrizzu/"

/**
 * Modern footer displayed at the bottom of screens:
 * "Made with ❤️ by Rizzu"
 * Tapping "Rizzu" opens the creator's Instagram profile.
 */
@Composable
fun MadeWithLoveFooter(
    modifier: Modifier = Modifier,
    bottomPadding: Int = 24
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val openInstagramProfile = {
        try {
            // First try launching standard view intent
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to Compose UriHandler
            try {
                uriHandler.openUri(INSTAGRAM_URL)
            } catch (_: Exception) {
                // Silently ignore if no browser available
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = bottomPadding.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = androidx.compose.foundation.BorderStroke(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier.testTag("app_footer_container")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Made with",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.width(5.dp))

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Love",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = "by",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.width(5.dp))

                // Clickable Rizzu Link
                Text(
                    text = "Rizzu",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE1306C), // Vibrant Instagram accent
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp),
                            onClick = openInstagramProfile
                        )
                        .testTag("app_footer_rizzu_link")
                )
            }
        }
    }
}
