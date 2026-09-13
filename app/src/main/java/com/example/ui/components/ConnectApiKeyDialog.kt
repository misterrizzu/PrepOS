package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.StudyAIService
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentDark
import com.example.ui.theme.isAppDarkTheme

/**
 * Universal Dialog prompting the user to connect their free Google Gemini API Key.
 * Provides 1-click access to Google AI Studio, offline step-by-step guide, and instant auto-validation.
 */
@Composable
fun ConnectApiKeyDialog(
    featureName: String = "AI Features",
    featureTitle: String = featureName,
    onDismiss: () -> Unit,
    onKeySaved: (String) -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val resolvedFeatureName = if (featureTitle != "AI Features") featureTitle else featureName
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = isAppDarkTheme()

    var keyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStepByStepGuide by remember { mutableStateOf(false) }
    var hasClickedGetApiKey by remember { mutableStateOf(false) }

    fun openAiStudioBrowser() {
        val url = "https://aistudio.google.com/app/apikey"
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        hasClickedGetApiKey = true
    }

    fun handleSaveKey(rawText: String) {
        val extractedKey = StudyAIService.extractGeminiApiKey(rawText) ?: rawText.trim()
        if (extractedKey.isBlank()) {
            errorMessage = "Please enter or paste your Gemini API key."
            return
        }
        if (!StudyAIService.isValidGeminiApiKeyFormat(extractedKey)) {
            errorMessage = "Key format seems unusual (usually starts with AIzaSy...). Please verify your key."
        }
        // Save key
        onKeySaved(extractedKey)
        Toast.makeText(context, "✓ Gemini API Key connected successfully!", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isDark) Color(0xFF131D33) else MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.25f) else GoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Connect Gemini AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "To use $resolvedFeatureName",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Free Quota Callout Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "1,500 Requests/Day 100% Free",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                )
                            )
                            Text(
                                text = "Google AI Studio gives free API access without any credit card or payment.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                // Primary Action Button: Get Free API Key
                Button(
                    onClick = { openAiStudioBrowser() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF6366F1) else GoldAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_get_free_api_key_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get Free API Key (Google AI Studio)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                // Toggleable Step-by-Step Guide
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showStepByStepGuide = !showStepByStepGuide }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "How to get free API Key?",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                    Text(
                        text = if (showStepByStepGuide) "Hide ▲" else "View Steps ▼",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                AnimatedVisibility(visible = showStepByStepGuide) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "1️⃣ Click 'Get Free API Key' above to open Google AI Studio in browser.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
                        )
                        Text(
                            text = "2️⃣ Sign in with your Google account (No payment needed).",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
                        )
                        Text(
                            text = "3️⃣ Click 'Create API key' and copy the generated key (AIzaSy...).",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
                        )
                        Text(
                            text = "4️⃣ Paste the key below and tap 'Save & Connect'.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
                        )
                    }
                }

                // If user clicked Get API Key or already has key
                Text(
                    text = if (hasClickedGetApiKey) "Have you got your key? Paste it here:" else "Already have a key? Paste below:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("AIzaSy...", fontSize = 12.5.sp) },
                    singleLine = true,
                    isError = errorMessage != null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (keyInput.isBlank()) {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank()) {
                                            val extracted = StudyAIService.extractGeminiApiKey(clip) ?: clip
                                            keyInput = extracted
                                            errorMessage = null
                                            Toast.makeText(context, "Key pasted from clipboard!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = if (isDark) Color(0xFFA5B4FC) else GoldAccentDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(onClick = { keyInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_api_key_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { handleSaveKey(keyInput) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_save_and_connect_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save & Connect", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onOpenSettings != null) {
                    TextButton(onClick = {
                        onDismiss()
                        onOpenSettings()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settings", fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Use Offline", fontSize = 12.sp)
                }
            }
        }
    )
}
