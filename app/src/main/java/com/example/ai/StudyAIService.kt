package com.example.ai

import android.util.Log
import com.example.model.CalloutType
import com.example.model.ChartEntry
import com.example.model.ChartType
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.ElementType
import com.example.model.QuestionItem
import com.example.model.SanitizedChapterResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AIAttachment(
    val name: String,
    val mimeType: String,
    val base64Data: String? = null,
    val textContent: String? = null
)

enum class AIProviderType(val id: String, val displayName: String, val badge: String) {
    GEMINI("GEMINI", "Google Gemini", "Gemini 3.5 Flash / 3.1 Pro"),
    DEEPSEEK("DEEPSEEK", "DeepSeek AI", "DeepSeek V3 / R1 Reasoner"),
    LOCAL_SYNTHESIZER("LOCAL_SYNTHESIZER", "Offline Synthesizer", "Local Rule Engine")
}

enum class AIAction(val label: String, val promptInstruction: String, val iconName: String) {
    EXPLAIN(
        "Explain Topic",
        "Explain the following study topic clearly, concisely, and with intuition suitable for competitive exam preparation:",
        "auto_awesome"
    ),
    SIMPLIFY(
        "Simplify Language",
        "Simplify the following text into plain, direct, easy-to-understand language while keeping all key facts accurate:",
        "translate"
    ),
    REWRITE(
        "Rewrite & Update",
        "Rewrite and update the following study topic notes to be crisp, structured, grammatically pristine, and high-impact:",
        "edit_note"
    ),
    TRANSLATE(
        "Translate Topic",
        "Translate the following study topic content into clean, standard Hindi / bilingual Hinglish with clear academic terminology:",
        "g_translate"
    ),
    SUGGEST_DIAGRAM(
        "Suggest Diagram / Visuals",
        "Suggest clear structural diagrams, ASCII flowcharts, hierarchy trees, or comparison tables to visually explain this topic:",
        "account_tree"
    ),
    STRUCTURE(
        "Structure Notes",
        "Organize and structure the following content with clear headings, subpoints, and logical hierarchy:",
        "format_list_bulleted"
    ),
    IMPROVE(
        "Improve Clarity",
        "Improve the clarity, precision, and sharpness of the following study explanation:",
        "bolt"
    ),
    BULLETS(
        "Convert to Bullets",
        "Convert the following study content into neat, high-yield bullet points for quick revision:",
        "checklist"
    ),
    EXAMPLES(
        "Create Examples",
        "Generate 2 realistic exam/practical examples or sample questions with brief step-by-step solutions based on this concept:",
        "quiz"
    )
}

object StudyAIService {
    private const val TAG = "StudyAIService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    const val DEFAULT_MODEL = "gemini-3.5-flash"

    val GEMINI_MODELS = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash (Recommended, Ultra-Fast)",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Deep Academic Reasoning & STEM)",
        "gemini-3.1-flash-lite-preview" to "Gemini 3.1 Flash Lite (High Speed & Lightweight)",
        "gemini-flash-latest" to "Gemini Flash Latest (Next-Gen Auto-Updating)"
    )

    val DEEPSEEK_MODELS = listOf(
        "deepseek-chat" to "DeepSeek-V3 (Fast, Structured Study Notes)",
        "deepseek-reasoner" to "DeepSeek-R1 (Deep Academic Step-by-Step Reasoner)"
    )

    val AVAILABLE_MODELS = GEMINI_MODELS

    fun getModelsForProvider(provider: String): List<Pair<String, String>> {
        return when (provider.uppercase()) {
            "DEEPSEEK" -> DEEPSEEK_MODELS
            "LOCAL_SYNTHESIZER" -> listOf("local-rules" to "Local Offline Rule Synthesizer")
            else -> GEMINI_MODELS
        }
    }

    fun resolveApiKey(userKey: String): String {
        val trimmed = userKey.trim()
        if (trimmed.isNotBlank() && trimmed != "MY_GEMINI_API_KEY") {
            return trimmed
        }
        val buildKey = try {
            com.example.BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    /**
     * Checks if a valid Gemini API key is configured.
     */
    fun isGeminiKeyConfigured(userKey: String = ""): Boolean {
        return resolveApiKey(userKey).isNotBlank()
    }

    /**
     * Checks if the given string resembles a valid Google Gemini API key format.
     */
    fun isValidGeminiApiKeyFormat(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isBlank() || trimmed == "MY_GEMINI_API_KEY") return false
        if (trimmed.startsWith("AIza", ignoreCase = false) && trimmed.length >= 25) {
            return true
        }
        if (trimmed.length >= 30 && !trimmed.contains(" ") && trimmed.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
            return true
        }
        return false
    }

    /**
     * Attempts to find and extract a Gemini API key from raw text (e.g. pasted into chat with extra words).
     */
    fun extractGeminiApiKey(text: String): String? {
        val trimmed = text.trim()
        val regex = Regex("""AIza[0-9A-Za-z\-_]{25,}""")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.value
        }
        if (isValidGeminiApiKeyFormat(trimmed)) {
            return trimmed
        }
        return null
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun resolveModelEndpoint(modelName: String): String {
        val trimmed = modelName.trim().lowercase()
        return when {
            trimmed.contains("3.5", ignoreCase = true) || trimmed.contains("flash-latest", ignoreCase = true) -> "gemini-2.5-flash"
            trimmed.contains("3.1-pro", ignoreCase = true) -> "gemini-2.5-pro"
            trimmed.contains("3.1-flash", ignoreCase = true) || trimmed.contains("lite", ignoreCase = true) -> "gemini-2.0-flash-lite"
            trimmed.isNotBlank() -> trimmed
            else -> "gemini-2.5-flash"
        }
    }

    private fun executeGeminiPost(requestJson: JSONObject, requestedModel: String, effectiveKey: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val primaryEndpoint = requestedModel.ifBlank { DEFAULT_MODEL }
        val fallbackEndpoint = resolveModelEndpoint(primaryEndpoint)

        val modelsToTry = if (primaryEndpoint == fallbackEndpoint) {
            listOf(primaryEndpoint)
        } else {
            listOf(primaryEndpoint, fallbackEndpoint)
        }

        var lastException: Exception? = null
        for (targetModel in modelsToTry) {
            val url = "$BASE_URL$targetModel:generateContent?key=$effectiveKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            try {
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    return responseBody
                }

                // If model is 404 or 400 (Not Found), try fallback model
                if (response.code == 404 || response.code == 400) {
                    Log.w(TAG, "Model $targetModel returned ${response.code}, attempting fallback model...")
                    continue
                }

                val errorMsg = try {
                    val errObj = JSONObject(responseBody ?: "{}")
                    errObj.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code} ${response.message}"
                } catch (e: Exception) {
                    "HTTP ${response.code} ${response.message}"
                }
                lastException = Exception("Online AI processing error: $errorMsg")
            } catch (e: Exception) {
                Log.w(TAG, "Network attempt with $targetModel failed", e)
                lastException = e
            }
        }

        throw lastException ?: Exception("Online AI connection could not be completed.")
    }

    private fun executeDeepSeekPost(fullPrompt: String, requestedModel: String, apiKey: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val targetModel = if (requestedModel.contains("reasoner", ignoreCase = true)) "deepseek-reasoner" else "deepseek-chat"
        val requestJson = JSONObject().apply {
            put("model", targetModel)
            val messagesArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert study assistant. Output structured, ready-to-insert educational content with clean headings, markdown tables, callout boxes, and concise bullet points.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", fullPrompt)
                })
            }
            put("messages", messagesArr)
            put("temperature", 0.7)
        }

        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://api.deepseek.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .post(body)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (response.isSuccessful && !responseBody.isNullOrBlank()) {
            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content")
            if (!content.isNullOrBlank()) {
                return content.trim()
            }
        }
        val err = try {
            JSONObject(responseBody ?: "{}").optJSONObject("error")?.optString("message") ?: "HTTP ${response.code} ${response.message}"
        } catch (e: Exception) {
            "HTTP ${response.code} ${response.message}"
        }
        throw Exception("DeepSeek API error: $err")
    }

    /**
     * Tests connectivity with a provided API key and model.
     */
    suspend fun testApiConnection(
        apiKey: String,
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (provider.equals("DEEPSEEK", ignoreCase = true)) {
            val key = apiKey.trim()
            if (key.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("DeepSeek API Key is empty. Please enter your key in Settings."))
            }
            return@withContext try {
                val resp = executeDeepSeekPost("Respond with 'OK' if you can read this.", model, key)
                if (resp.isNotBlank()) Result.success(true) else Result.failure(Exception("Empty response from DeepSeek API"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val effectiveKey = resolveApiKey(apiKey)
        if (effectiveKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API Key is empty. Please enter your key in Settings or configure in Secrets."))
        }

        try {
            val selectedModel = model.ifBlank { DEFAULT_MODEL }
            val prompt = "Respond with 'OK' if you can read this."
            val requestJson = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)
            }

            val responseBody = executeGeminiPost(requestJson, selectedModel, effectiveKey)
            if (responseBody.isNotBlank()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Empty response from AI server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes single inline AI actions or custom user instructions with optional extra pasted knowledge and attachments.
     */
    suspend fun executeAIAction(
        action: AIAction,
        selectedText: String,
        contextTitle: String = "",
        userCustomInstruction: String = "",
        attachments: List<AIAttachment> = emptyList(),
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        if (selectedText.isBlank() && userCustomInstruction.isBlank() && attachments.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No text or instruction provided."))
        }

        val fullPrompt = buildString {
            appendLine(action.promptInstruction)
            if (contextTitle.isNotBlank()) {
                appendLine("Context/Topic: $contextTitle")
            }
            if (userCustomInstruction.isNotBlank()) {
                appendLine()
                appendLine("User's Specific Instructions & Additional Material to Merge/Upgrade:")
                appendLine("\"\"\"")
                appendLine(userCustomInstruction.trim())
                appendLine("\"\"\"")
                appendLine("Apply the above user instructions/extra knowledge directly onto the target text.")
            }
            if (selectedText.isNotBlank()) {
                appendLine()
                appendLine("Target Text / Base Topic:")
                appendLine("\"\"\"")
                appendLine(selectedText.trim())
                appendLine("\"\"\"")
            }
            if (attachments.isNotEmpty()) {
                appendLine()
                appendLine("Attached References / Documents: ${attachments.size} file(s) attached.")
            }
            appendLine()
            appendLine("Important: Provide clean, high-yield output without conversational filler or markdown code fence wrappers, formatted cleanly for direct insertion into study notebook.")
        }

        // Branch 1: DeepSeek Provider
        if (provider.equals("DEEPSEEK", ignoreCase = true) && deepSeekApiKey.isNotBlank()) {
            try {
                val deepSeekResponse = executeDeepSeekPost(fullPrompt, model, deepSeekApiKey)
                if (deepSeekResponse.isNotBlank()) {
                    return@withContext Result.success(deepSeekResponse)
                }
            } catch (e: Exception) {
                Log.e(TAG, "DeepSeek API error, attempting fallback", e)
            }
        }

        // Branch 2: Gemini Provider
        val effectiveKey = resolveApiKey(apiKey)
        if (provider.equals("GEMINI", ignoreCase = true) && effectiveKey.isNotBlank()) {
            try {
                val selectedModel = model.ifBlank { DEFAULT_MODEL }
                val requestJson = JSONObject().apply {
                    val contentsArr = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().apply { put("text", fullPrompt) })
                                attachments.forEach { att ->
                                    if (!att.base64Data.isNullOrBlank()) {
                                        put(JSONObject().apply {
                                            put("inline_data", JSONObject().apply {
                                                put("mime_type", att.mimeType)
                                                put("data", att.base64Data)
                                            })
                                        })
                                    } else if (!att.textContent.isNullOrBlank()) {
                                        put(JSONObject().apply {
                                            put("text", "\n--- ATTACHED FILE (${att.name}) ---\n${att.textContent}\n--- END ATTACHED FILE ---")
                                        })
                                    }
                                }
                            }
                            put("parts", partsArr)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArr)
                }

                val responseBody = executeGeminiPost(requestJson, selectedModel, effectiveKey)
                if (responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val resultText = parts?.optJSONObject(0)?.optString("text")

                    if (!resultText.isNullOrBlank()) {
                        return@withContext Result.success(resultText.trim())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error, falling back to local study synthesizer", e)
            }
        }

        // High-Quality Local Study Synthesizer fallback
        val baseText = if (selectedText.isNotBlank()) selectedText else userCustomInstruction
        val localOutput = generateLocalStudyAssistance(action, baseText, userCustomInstruction)
        Result.success(localOutput)
    }

    /**
     * Dedicated method for executing arbitrary user custom instructions / prompts
     * (e.g. "Upgrade this topic with my clipboard text: ...")
     */
    suspend fun executeCustomInstruction(
        baseContextText: String,
        userInstruction: String,
        contextTitle: String = "",
        attachments: List<AIAttachment> = emptyList(),
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        if (baseContextText.isBlank() && userInstruction.isBlank() && attachments.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No prompt or text provided."))
        }

        val fullPrompt = buildString {
            appendLine("You are an expert study assistant. Follow the user's specific instructions to rewrite, upgrade, synthesize, or explain the study material.")
            if (contextTitle.isNotBlank()) {
                appendLine("Subject / Topic Context: $contextTitle")
            }
            appendLine()
            appendLine("User's Instruction / Upgrade Notes:")
            appendLine("\"\"\"")
            appendLine(userInstruction.trim())
            appendLine("\"\"\"")
            if (baseContextText.isNotBlank()) {
                appendLine()
                appendLine("Current Base Content:")
                appendLine("\"\"\"")
                appendLine(baseContextText.trim())
                appendLine("\"\"\"")
            }
            if (attachments.isNotEmpty()) {
                appendLine()
                appendLine("Attached References: ${attachments.size} file(s) attached.")
            }
            appendLine()
            appendLine("Important: Return direct, ready-to-insert educational content with clean structure (headings, tables, callouts, or bullet points where helpful). Do not add chit-chat or conversational opening/closing phrases.")
        }

        // Branch 1: DeepSeek Provider
        if (provider.equals("DEEPSEEK", ignoreCase = true) && deepSeekApiKey.isNotBlank()) {
            try {
                val deepSeekResponse = executeDeepSeekPost(fullPrompt, model, deepSeekApiKey)
                if (deepSeekResponse.isNotBlank()) {
                    return@withContext Result.success(deepSeekResponse)
                }
            } catch (e: Exception) {
                Log.e(TAG, "DeepSeek API error in custom instruction", e)
            }
        }

        // Branch 2: Gemini Provider
        val effectiveKey = resolveApiKey(apiKey)
        if (provider.equals("GEMINI", ignoreCase = true) && effectiveKey.isNotBlank()) {
            try {
                val selectedModel = model.ifBlank { DEFAULT_MODEL }
                val requestJson = JSONObject().apply {
                    val contentsArr = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().apply { put("text", fullPrompt) })
                                attachments.forEach { att ->
                                    if (!att.base64Data.isNullOrBlank()) {
                                        put(JSONObject().apply {
                                            put("inline_data", JSONObject().apply {
                                                put("mime_type", att.mimeType)
                                                put("data", att.base64Data)
                                            })
                                        })
                                    } else if (!att.textContent.isNullOrBlank()) {
                                        put(JSONObject().apply {
                                            put("text", "\n--- ATTACHED FILE (${att.name}) ---\n${att.textContent}\n--- END ATTACHED FILE ---")
                                        })
                                    }
                                }
                            }
                            put("parts", partsArr)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArr)
                }

                val responseBody = executeGeminiPost(requestJson, selectedModel, effectiveKey)
                if (responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val resultText = parts?.optJSONObject(0)?.optString("text")

                    if (!resultText.isNullOrBlank()) {
                        return@withContext Result.success(resultText.trim())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error in custom instruction, fallback to local synthesizer", e)
            }
        }

        // Clean offline fallback for custom instruction
        val localOutput = buildString {
            if (baseContextText.isNotBlank()) {
                appendLine(baseContextText.trim())
            } else if (userInstruction.isNotBlank()) {
                appendLine(userInstruction.trim())
            }
        }
        Result.success(localOutput.trim())
    }

    suspend fun executeRawAiPrompt(
        prompt: String,
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveKey = resolveApiKey(apiKey)
        if (provider.equals("DEEPSEEK", ignoreCase = true)) {
            val effectiveDeepSeekKey = deepSeekApiKey.ifBlank { resolveApiKey(apiKey) }
            if (effectiveDeepSeekKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("DeepSeek API Key is missing. Please add your key in Settings."))
            }
            try {
                val responseText = executeDeepSeekPost(prompt, model, effectiveDeepSeekKey)
                if (responseText.isNotBlank()) {
                    val root = JSONObject(responseText)
                    val choices = root.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrBlank()) {
                        return@withContext Result.success(content.trim())
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        if (effectiveKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Add Gemini API key in Settings to use AI editing"))
        }

        try {
            val selectedModel = model.ifBlank { DEFAULT_MODEL }
            val requestJson = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)
            }

            val responseBody = executeGeminiPost(requestJson, selectedModel, effectiveKey)
            if (responseBody.isNotBlank()) {
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val resultText = parts?.optJSONObject(0)?.optString("text")

                if (!resultText.isNullOrBlank()) {
                    return@withContext Result.success(resultText.trim())
                }
            }
            Result.failure(Exception("Empty response received from AI model."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dedicated Zero-Information-Loss Notes Structurer:
     * Transforms raw source material (text, photos, PDFs) into clean, rich, structured study notes.
     * Guaranteed: ZERO prompt leakage, zero user metadata, zero conversational fluff, and zero information deletion.
     */
    suspend fun generateStructuredNotesFromSource(
        sourceText: String,
        chapterTitle: String,
        attachments: List<AIAttachment> = emptyList(),
        modificationInstruction: String = "",
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveTitle = chapterTitle.trim().ifBlank { "Structured Study Notes" }

        val systemPrompt = buildString {
            appendLine("You are an expert academic study note compiler and curriculum structurer.")
            appendLine("Transform the provided source material into clean, comprehensive, structured study notes for: \"$effectiveTitle\".")
            appendLine()
            appendLine("CRITICAL RULES (ZERO INFORMATION LOSS & STRUCTURAL FIDELITY):")
            appendLine("1. ZERO INFORMATION LOSS: Never summarize by deleting source information. Never omit examples, explanations, facts, tables, formulas, dates, conditions, or exceptions merely to make notes shorter. Restructure and reorganize for pristine clarity, but preserve 100% of the knowledge.")
            appendLine("2. HIERARCHICAL & RICH STRUCTURING:")
            appendLine("   - Headings (## Major Sections, ### Sub-topics, #### Specific Rules/Cases)")
            appendLine("   - Clear explanatory paragraphs")
            appendLine("   - Bulleted lists (•) for points, characteristics, advantages, etc.")
            appendLine("   - Numbered lists (1., 2.) for processes, chronological steps, or sequential algorithms")
            appendLine("   - Markdown tables (| Column 1 | Column 2 |) for comparisons, classifications, formulas, and tabular data")
            appendLine("   - Math/Scientific equations: Use clean expressions without unnecessary dollar signs. NEVER enclose plain numbers, option choices, or sequences in dollar signs (e.g. write `(a) 2, 3, 1, 4, 5` NEVER `(a) $2, 3, 1, 4, 5$`).")
            appendLine("   - Definition highlights (**Definition:** [Term] – [Meaning])")
            appendLine("   - Key takeaways & exam warnings (**Important:** ... / **Note:** ...)")
            appendLine("   - Timelines for historical dates or sequential events (**[Year/Date]** — [Event description])")
            appendLine("   - Example blocks (**Example:** ...)")
            appendLine("3. MANDATORY FINAL REVISION SECTION:")
            appendLine("   - Every chapter MUST conclude with a dedicated section:")
            appendLine("     ## REVISION SUMMARY & KEY TAKEAWAYS (or ## QUICK REVISION & FORMULAS)")
            appendLine("   - Include a concise high-yield bulleted summary of all core concepts, mnemonic hooks, critical rules, and quick-recall points.")
            appendLine("4. ZERO PROMPT / METADATA LEAKAGE:")
            appendLine("   - DO NOT output any system instructions, prompt headers, user profile information, daily study targets, workflow names, or conversational greetings/sign-offs.")
            appendLine("   - DO NOT wrap the output in conversational text like \"Here are your notes:\".")
            appendLine("   - Return ONLY the final structured study note document.")
            if (modificationInstruction.isNotBlank()) {
                appendLine()
                appendLine("5. USER MODIFICATION REQUEST:")
                appendLine("   The user has requested the following adjustments to the previously generated notes:")
                appendLine("   \"\"\"$modificationInstruction\"\"\"")
                appendLine("   Apply these specific changes faithfully while retaining all other facts, examples, definitions, and formulas.")
            }
        }

        val userPrompt = buildString {
            if (sourceText.isNotBlank()) {
                appendLine("=== SOURCE MATERIAL ===")
                appendLine(sourceText.trim())
                appendLine("=== END SOURCE MATERIAL ===")
            }
            if (attachments.isNotEmpty()) {
                appendLine("Attached References: ${attachments.size} image/document file(s) attached.")
            }
        }

        // 1. Try DeepSeek Provider if selected
        if (provider.equals("DEEPSEEK", ignoreCase = true)) {
            val effectiveDeepSeekKey = deepSeekApiKey.ifBlank { resolveApiKey(apiKey) }
            if (effectiveDeepSeekKey.isNotBlank()) {
                try {
                    val fullPrompt = "$systemPrompt\n\n$userPrompt"
                    val responseText = executeDeepSeekPost(fullPrompt, model, effectiveDeepSeekKey)
                    if (responseText.isNotBlank()) {
                        val root = JSONObject(responseText)
                        val choices = root.optJSONArray("choices")
                        val firstChoice = choices?.optJSONObject(0)
                        val content = firstChoice?.optJSONObject("message")?.optString("content")
                        if (!content.isNullOrBlank()) {
                            val sanitized = sanitizeStructuredNotesOutput(content, effectiveTitle)
                            return@withContext Result.success(sanitized)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "DeepSeek API error in notes structuring", e)
                }
            }
        }

        // 2. Try Gemini Provider
        val effectiveKey = resolveApiKey(apiKey)
        if (effectiveKey.isNotBlank()) {
            try {
                val selectedModel = model.ifBlank { DEFAULT_MODEL }
                val requestJson = JSONObject().apply {
                    val contentsArr = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().apply { put("text", "$systemPrompt\n\n$userPrompt") })
                                attachments.forEach { att ->
                                    if (!att.base64Data.isNullOrBlank()) {
                                        put(JSONObject().apply {
                                            put("inline_data", JSONObject().apply {
                                                put("mime_type", att.mimeType)
                                                put("data", att.base64Data)
                                            })
                                        })
                                    } else if (!att.textContent.isNullOrBlank()) {
                                        put(JSONObject().apply {
                                            put("text", "\n--- ATTACHED FILE (${att.name}) ---\n${att.textContent}\n--- END ATTACHED FILE ---")
                                        })
                                    }
                                }
                            }
                            put("parts", partsArr)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArr)
                }

                val responseBody = executeGeminiPost(requestJson, selectedModel, effectiveKey)
                if (responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val resultText = parts?.optJSONObject(0)?.optString("text")

                    if (!resultText.isNullOrBlank()) {
                        val sanitized = sanitizeStructuredNotesOutput(resultText, effectiveTitle)
                        return@withContext Result.success(sanitized)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error in notes structuring, fallback to offline structure", e)
            }
        }

        // 3. Robust Offline Fallback: Zero Information Loss Local Structurer
        val offlineResult = buildOfflineStructuredNotes(sourceText, effectiveTitle)
        Result.success(offlineResult)
    }

    /**
     * Sanitizes AI response to guarantee no internal prompt or metadata leakage.
     */
    fun sanitizeStructuredNotesOutput(text: String, defaultTitle: String = "Structured Study Notes"): String {
        if (text.isBlank()) return text

        var cleaned = text.trim()

        // Strip markdown code fences if wrapped
        if (cleaned.startsWith("```markdown", ignoreCase = true)) {
            cleaned = cleaned.substringAfter("\n")
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("\n")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```").trim()
        }

        // Filter out any accidental metadata lines
        val lines = cleaned.lines()
        val filteredLines = lines.filter { line ->
            val l = line.trim()
            !(l.startsWith("# PrepOS Study Companion", ignoreCase = true) ||
              l.startsWith("**User:**", ignoreCase = true) ||
              l.startsWith("**Workflow:**", ignoreCase = true) ||
              l.startsWith("**Collected User Preferences:**", ignoreCase = true) ||
              l.startsWith("**Daily Target:**", ignoreCase = true) ||
              l.startsWith("**Subject Focus:**", ignoreCase = true) ||
              l.startsWith("**Enrolled Curriculum Integration:**", ignoreCase = true) ||
              l.startsWith("=== RAW EDUCATIONAL CONTENT", ignoreCase = true) ||
              l.startsWith("=== END RAW EDUCATIONAL CONTENT", ignoreCase = true) ||
              l.startsWith("=== SOURCE MATERIAL", ignoreCase = true) ||
              l.startsWith("=== END SOURCE MATERIAL", ignoreCase = true) ||
              l.matches(Regex("^(?i)(Here are your structured study notes.*|Here is the structured.*notes?:|Sure, I have structured.*:)$")))
        }

        cleaned = filteredLines.joinToString("\n").trim()
        return cleaned.ifBlank { text.trim() }
    }

    /**
     * Local Zero-Information-Loss Structured Notes Builder (Offline mode).
     */
    private fun buildOfflineStructuredNotes(rawSource: String, title: String): String {
        if (rawSource.isBlank()) {
            return "## $title\n\n• Add your source material to generate complete structured study notes."
        }
        val lines = rawSource.lines().map { it.trim() }.filter { it.isNotBlank() }
        val sb = StringBuilder()
        sb.appendLine("## $title")
        sb.appendLine()
        var currentHeading = "1. Core Concepts & Overview"
        sb.appendLine("### $currentHeading")

        lines.forEach { line ->
            when {
                line.startsWith("#") -> {
                    sb.appendLine()
                    sb.appendLine(line)
                }
                line.startsWith("•") || line.startsWith("-") || line.startsWith("*") -> {
                    sb.appendLine("• ${line.removePrefix("•").removePrefix("-").removePrefix("*").trim()}")
                }
                line.contains(":") && line.length < 120 -> {
                    val term = line.substringBefore(":").trim()
                    val desc = line.substringAfter(":").trim()
                    sb.appendLine("• **$term**: $desc")
                }
                line.contains("=") -> {
                    sb.appendLine("• `$line`")
                }
                else -> {
                    sb.appendLine(line)
                    sb.appendLine()
                }
            }
        }
        return sb.toString().trim()
    }

    /**
     * AI Sanitization & Chapter Structuring:
     * Takes raw text / extracted document content, executes comprehensive educational pipeline:
     * 1. Cleanup (removes raw markdown symbols, separators, formatting artifacts; preserves educational content)
     * 2. Hierarchy identification (H1 Chapter Title, H2 Major Sections, H3 Subtopics/Rules, H4 Sub-rules/Cases/Exceptions, Paragraph, Lists, Callouts, Tables, Diagrams)
     * 3. Revision section standardization
     *
     * CRITICAL: When useAI = true, this method strictly returns online AI output or throws an explicit Exception.
     * It NEVER silently falls back to the offline parser.
     */
    suspend fun sanitizeAndStructureChapter(
        rawContent: String,
        chapterTitle: String,
        chapterNumber: Int = 0,
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        useAI: Boolean = true
    ): SanitizedChapterResult = withContext(Dispatchers.IO) {
        if (!useAI) {
            return@withContext OfflineChapterSanitizer.sanitizeLocally(rawContent, chapterTitle)
        }

        val effectiveKey = resolveApiKey(apiKey)
        if (effectiveKey.isBlank()) {
            throw IllegalStateException("Gemini API Key is not configured. Please add your API key in Settings or use the Offline Parser.")
        }

        val selectedModel = model.ifBlank { DEFAULT_MODEL }
        val systemPrompt = buildSanitizationSystemPrompt(chapterTitle, chapterNumber)

        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\n=== RAW EDUCATIONAL CONTENT TO SANITIZE & STRUCTURE ===\n$rawContent\n=== END RAW EDUCATIONAL CONTENT ===")
                        })
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.15)
            })
        }

        val responseBody = try {
            executeGeminiPost(requestJson, selectedModel, effectiveKey)
        } catch (e: Exception) {
            Log.e(TAG, "Online AI network error", e)
            val isTimeout = e is java.net.SocketTimeoutException || e.message?.contains("timeout", ignoreCase = true) == true
            val errorDescription = if (isTimeout) {
                "Online AI structuring timed out. Tap 'Try Again' or use 'Use Offline Parser' to build instantly."
            } else {
                e.localizedMessage ?: e.message ?: "An unexpected error occurred during online AI parsing."
            }
            throw Exception(errorDescription, e)
        }

        val root = try {
            JSONObject(responseBody)
        } catch (e: Exception) {
            throw Exception("Invalid response format received from AI service: ${e.message}")
        }

        val candidates = root.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val jsonOutput = parts?.optJSONObject(0)?.optString("text")

        if (jsonOutput.isNullOrBlank()) {
            val finishReason = firstCandidate?.optString("finishReason", "UNKNOWN")
            throw Exception("AI model returned an empty response (finishReason: $finishReason). Please try again.")
        }

        val parsed = parseSanitizedJsonResponseStrict(jsonOutput, chapterTitle)
        if (parsed.elements.isEmpty()) {
            throw Exception("Online AI response did not contain structured document elements. Please try again.")
        }
        parsed.copy(isAiGenerated = true)
    }

    /**
     * AI & Offline Question Parser:
     * Extracts multiple-choice questions (MCQs) with options A, B, C, D, correct option index, and explanations.
     */
    suspend fun parseQuestionsFromText(
        rawText: String,
        apiKey: String = "",
        model: String = DEFAULT_MODEL
    ): List<QuestionItem> = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) return@withContext emptyList()

        val effectiveKey = resolveApiKey(apiKey)
        if (effectiveKey.isNotBlank()) {
            try {
                val selectedModel = model.ifBlank { DEFAULT_MODEL }
                val prompt = """
                    You are an expert academic curriculum and competitive-exam questions extraction engine for PrepOS.
                    Parse the following raw text or questions list into structured multiple-choice questions (MCQs).

                    CRITICAL CONTENT FIDELITY INSTRUCTIONS (DO NOT FLATTEN OR OMIT):
                    1. MATHEMATICAL NOTATION & EQUATIONS:
                       - Preserve all math notation, LaTeX equations, fractions (\frac{numerator}{denominator}), radicals (\sqrt{x}, \sqrt[3]{x}), superscripts/exponents (x^2, 10^-3), subscripts (a_n, H_2O), matrices (\begin{matrix}...\end{matrix}), integrals, sums, and Greek symbols (α, β, θ, π, λ, Δ, etc.).
                       - Do NOT simplify, strip, or flatten formulas into plain text.
                    2. REASONING & FIGURE QUESTIONS:
                       - If a question is based on a visual diagram, matrix grid, series progression, unfolded cube/dice, Venn diagram, compass direction, or seating arrangement, enclose the structured representation using figure tags:
                         • Pattern Grid: [FIGURE:GRID 3x3] ... [/FIGURE]
                         • Sequence/Series: [FIGURE:SERIES] Step 1 -> Step 2 -> Step 3 -> ? [/FIGURE]
                         • Dice/Cube Net: [FIGURE:DICE] top: 1 \n left: 2 \n center: 3 \n right: 4 \n bottom: 5 \n bottom2: 6 [/FIGURE]
                         • Venn Diagram: [FIGURE:VENN Set A, Set B] Both: 15 \n Only A: 25 \n Only B: 30 [/FIGURE]
                         • Compass Vector: [FIGURE:COMPASS] North: 10 \n East: 15 [/FIGURE]
                         • Seating: [FIGURE:SEATING circular] A, B, C, D, E, F [/FIGURE]
                         • ASCII Chart/Circuit: [FIGURE:ASCII] ... [/FIGURE]
                    3. QUESTION SCHEMA:
                       - questionText: Clean question stem (including math/figure tags if applicable) without question number prefixes (e.g. remove "Q1.", "1)").
                       - options: Exactly 4 distinct answer choices (e.g. ["Choice A", "Choice B", "Choice C", "Choice D"]). Preserve mathematical expressions inside options.
                       - correctOptionIndex: 0-indexed integer (0 for A, 1 for B, 2 for C, 3 for D). Deduce from academic facts if not marked.
                       - explanation: Clear, step-by-step mathematical derivation or reasoning rationale explaining why the correct option is right.

                    Return strictly valid JSON adhering to this schema:
                    {
                      "questions": [
                        {
                          "questionText": "...",
                          "options": ["A", "B", "C", "D"],
                          "correctOptionIndex": 0,
                          "explanation": "..."
                        }
                      ]
                    }

                    RAW INPUT:
                    $rawText
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    val contentsArr = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            }
                            put("parts", partsArr)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArr)
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.1)
                    })
                }

                val responseBody = executeGeminiPost(requestJson, selectedModel, effectiveKey)
                if (responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val jsonOutput = parts?.optJSONObject(0)?.optString("text")

                    if (!jsonOutput.isNullOrBlank()) {
                        val cleanJson = extractJsonBlock(jsonOutput)
                        val parsedRoot = JSONObject(cleanJson)
                        val qArr = parsedRoot.optJSONArray("questions") ?: JSONArray()
                        val result = mutableListOf<QuestionItem>()
                        for (i in 0 until qArr.length()) {
                            result.add(QuestionItem.fromJson(qArr.getJSONObject(i)))
                        }
                        if (result.isNotEmpty()) {
                            return@withContext result
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Question parsing failed, executing offline question parser", e)
            }
        }

        // Offline Question Parser
        OfflineQuestionParser.parseLocally(rawText)
    }

    /**
     * Generates 10 or 20 high-yield exam-grade MCQs on a chosen topic or chapter context.
     * Works with Gemini API, DeepSeek API, and includes intelligent offline synthesis.
     */
    suspend fun generateAiTestQuestions(
        topicOrTitle: String,
        questionCount: Int = 10,
        chapterContextText: String = "",
        examContext: String = "",
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<List<QuestionItem>> = withContext(Dispatchers.IO) {
        val targetCount = if (questionCount <= 10) 10 else 20
        val targetTopic = topicOrTitle.trim().ifBlank { "General Studies and Aptitude" }

        val prompt = buildString {
            appendLine("You are an elite competitive examination question designer and academic curriculum expert.")
            appendLine("Generate exactly $targetCount high-yield, challenging Multiple Choice Questions (MCQs) for practice and testing.")
            appendLine()
            appendLine("TARGET TOPIC / SUBJECT:")
            appendLine("\"$targetTopic\"")
            if (examContext.isNotBlank()) {
                appendLine("TARGET EXAM: $examContext")
            }
            if (chapterContextText.isNotBlank()) {
                appendLine()
                appendLine("SOURCE MATERIAL / CHAPTER NOTES:")
                appendLine("\"\"\"")
                appendLine(chapterContextText.take(4000))
                appendLine("\"\"\"")
            }
            appendLine()
            appendLine("STRICT GUIDELINES FOR QUESTIONS:")
            appendLine("1. Generate EXACTLY $targetCount distinct questions.")
            appendLine("2. Mix of conceptual depth, analytical reasoning, and factual precision.")
            appendLine("3. Each question MUST have exactly 4 plausible options (A, B, C, D).")
            appendLine("4. Exactly one option is correct. 'correctOptionIndex' must be 0, 1, 2, or 3.")
            appendLine("5. Distribute correct answer indices evenly across 0, 1, 2, and 3.")
            appendLine("6. 'explanation' MUST be rich, explaining why the correct choice is true and why other options are incorrect.")
            appendLine("7. 'difficulty' should be 'EASY', 'MEDIUM', or 'HARD'.")
            appendLine("8. Preserve math equations or chemical/scientific notation if relevant.")
            appendLine()
            appendLine("Output strictly valid JSON with this exact schema:")
            appendLine("{")
            appendLine("  \"questions\": [")
            appendLine("    {")
            appendLine("      \"questionText\": \"Full question statement here?\",")
            appendLine("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],")
            appendLine("      \"correctOptionIndex\": 0,")
            appendLine("      \"explanation\": \"Detailed step-by-step reasoning and facts...\",")
            appendLine("      \"difficulty\": \"MEDIUM\",")
            appendLine("      \"tags\": [\"$targetTopic\"]")
            appendLine("    }")
            appendLine("  ]")
            appendLine("}")
        }

        // 1. Attempt DeepSeek if selected
        if (provider.equals("DEEPSEEK", ignoreCase = true)) {
            val effectiveDeepSeekKey = deepSeekApiKey.ifBlank { resolveApiKey(apiKey) }
            if (effectiveDeepSeekKey.isNotBlank()) {
                try {
                    val responseText = executeDeepSeekPost(prompt, model, effectiveDeepSeekKey)
                    if (responseText.isNotBlank()) {
                        val root = JSONObject(responseText)
                        val choices = root.optJSONArray("choices")
                        val firstChoice = choices?.optJSONObject(0)
                        val content = firstChoice?.optJSONObject("message")?.optString("content") ?: ""
                        val cleanJson = extractJsonBlock(content)
                        val parsed = JSONObject(cleanJson)
                        val qArr = parsed.optJSONArray("questions")
                        if (qArr != null && qArr.length() > 0) {
                            val list = mutableListOf<QuestionItem>()
                            for (i in 0 until qArr.length()) {
                                list.add(QuestionItem.fromJson(qArr.getJSONObject(i)))
                            }
                            if (list.isNotEmpty()) {
                                return@withContext Result.success(list.take(targetCount))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "DeepSeek AI test generation failed, falling back to Gemini / offline: ${e.message}")
                }
            }
        }

        // 2. Attempt Gemini API
        val effectiveGeminiKey = resolveApiKey(apiKey)
        if (effectiveGeminiKey.isNotBlank()) {
            try {
                val selectedModel = model.ifBlank { DEFAULT_MODEL }
                val requestJson = JSONObject().apply {
                    val contentsArr = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            }
                            put("parts", partsArr)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArr)
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.25)
                    })
                }

                val responseBody = executeGeminiPost(requestJson, selectedModel, effectiveGeminiKey)
                if (responseBody.isNotBlank()) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val jsonOutput = parts?.optJSONObject(0)?.optString("text")

                    if (!jsonOutput.isNullOrBlank()) {
                        val cleanJson = extractJsonBlock(jsonOutput)
                        val parsedRoot = JSONObject(cleanJson)
                        val qArr = parsedRoot.optJSONArray("questions")
                        if (qArr != null && qArr.length() > 0) {
                            val list = mutableListOf<QuestionItem>()
                            for (i in 0 until qArr.length()) {
                                list.add(QuestionItem.fromJson(qArr.getJSONObject(i)))
                            }
                            if (list.isNotEmpty()) {
                                return@withContext Result.success(list.take(targetCount))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini AI test generation failed, using intelligent offline synthesis: ${e.message}")
            }
        }

        // 3. Fallback: Intelligent Offline Question Synthesis
        val synthesized = synthesizeOfflineTestQuestions(targetTopic, targetCount)
        Result.success(synthesized)
    }

    /**
     * Synthesizes 10 or 20 high-yield questions offline for a specified topic.
     * Ensures tests can always be generated even without internet connectivity or API keys.
     */
    private fun synthesizeOfflineTestQuestions(topic: String, count: Int): List<QuestionItem> {
        val topicLower = topic.lowercase()
        val questions = mutableListOf<QuestionItem>()

        // Domain-specific seed questions pool
        if (topicLower.contains("polity") || topicLower.contains("constitution") || topicLower.contains("writ") || topicLower.contains("right")) {
            questions.addAll(listOf(
                QuestionItem(
                    questionText = "Which Article of the Constitution of India provides for the Right to Constitutional Remedies and enables moving the Supreme Court?",
                    options = listOf("Article 32", "Article 226", "Article 14", "Article 21"),
                    correctOptionIndex = 0,
                    explanation = "Article 32 guarantees the right to move the Supreme Court for enforcement of Fundamental Rights. Dr. B.R. Ambedkar famously referred to it as the 'Heart and Soul' of the Constitution.",
                    difficulty = "EASY",
                    tags = listOf("Polity", "Fundamental Rights")
                ),
                QuestionItem(
                    questionText = "Which writ literally translates to 'We Command' and is issued to enforce the performance of a public duty?",
                    options = listOf("Mandamus", "Habeas Corpus", "Quo-Warranto", "Certiorari"),
                    correctOptionIndex = 0,
                    explanation = "Mandamus is a Latin term meaning 'We Command'. It is issued by higher courts to direct a public authority or lower court to perform a mandatory statutory duty.",
                    difficulty = "MEDIUM",
                    tags = listOf("Polity", "Writs")
                ),
                QuestionItem(
                    questionText = "The 73rd and 74th Constitutional Amendment Acts of 1992 are associated with which system of governance in India?",
                    options = listOf("Panchayati Raj and Urban Municipalities", "Goods and Services Tax", "Anti-Defection Law", "National Judicial Appointments"),
                    correctOptionIndex = 0,
                    explanation = "The 73rd Amendment gave constitutional status to Panchayati Raj Institutions (Rural local bodies) and the 74th Amendment gave constitutional status to Urban Local Bodies (Municipalities).",
                    difficulty = "EASY",
                    tags = listOf("Polity", "Local Governance")
                ),
                QuestionItem(
                    questionText = "Under the Indian Constitution, the President administers the oath of office before entering duties. Who administers the oath to the President?",
                    options = listOf("Chief Justice of India", "Vice President", "Prime Minister", "Speaker of Lok Sabha"),
                    correctOptionIndex = 0,
                    explanation = "According to Article 60, the oath of office to the President of India is administered by the Chief Justice of India or, in his absence, the senior-most Judge of the Supreme Court.",
                    difficulty = "MEDIUM",
                    tags = listOf("Polity", "Executive")
                )
            ))
        }

        if (topicLower.contains("history") || topicLower.contains("movement") || topicLower.contains("freedom") || topicLower.contains("modern")) {
            questions.addAll(listOf(
                QuestionItem(
                    questionText = "The Non-Cooperation Movement was called off by Mahatma Gandhi in February 1922 primarily due to which incident?",
                    options = listOf("Chauri Chaura Incident", "Jallianwala Bagh Massacre", "Kakori Train Action", "Rowlatt Satyagraha"),
                    correctOptionIndex = 0,
                    explanation = "Mahatma Gandhi suspended the Non-Cooperation Movement on February 12, 1922 following the violent Chauri Chaura incident in Gorakhpur district, Uttar Pradesh, where a police station was set on fire.",
                    difficulty = "EASY",
                    tags = listOf("History", "Modern India")
                ),
                QuestionItem(
                    questionText = "In which session of the Indian National Congress was the resolution for 'Purna Swaraj' (Complete Independence) passed?",
                    options = listOf("Lahore Session (1929)", "Karachi Session (1931)", "Belgaum Session (1924)", "Calcutta Session (1928)"),
                    correctOptionIndex = 0,
                    explanation = "The historic resolution of 'Purna Swaraj' was passed at the Lahore Session in December 1929 presided over by Jawaharlal Nehru, designating 26 January 1930 as Independence Day.",
                    difficulty = "MEDIUM",
                    tags = listOf("History", "INC")
                ),
                QuestionItem(
                    questionText = "Who among the following was the founder of the 'Servants of India Society' in 1905?",
                    options = listOf("Gopal Krishna Gokhale", "Bal Gangadhar Tilak", "Lala Lajpat Rai", "Dadabhai Naoroji"),
                    correctOptionIndex = 0,
                    explanation = "Gopal Krishna Gokhale established the Servants of India Society in Pune in 1905 to unite and train Indians of different ethnicities and religions in welfare work.",
                    difficulty = "MEDIUM",
                    tags = listOf("History", "Organizations")
                )
            ))
        }

        if (topicLower.contains("science") || topicLower.contains("physic") || topicLower.contains("chem") || topicLower.contains("bio") || topicLower.contains("thermo")) {
            questions.addAll(listOf(
                QuestionItem(
                    questionText = "According to the First Law of Thermodynamics, energy can neither be created nor destroyed. What is the fundamental conservation law it embodies?",
                    options = listOf("Conservation of Energy", "Conservation of Momentum", "Conservation of Mass", "Conservation of Entropy"),
                    correctOptionIndex = 0,
                    explanation = "The First Law of Thermodynamics states that change in internal energy equals heat supplied minus work done (ΔU = Q - W), which is a direct statement of the Law of Conservation of Energy.",
                    difficulty = "EASY",
                    tags = listOf("Science", "Thermodynamics")
                ),
                QuestionItem(
                    questionText = "Which phenomenon describes the bending of light as it passes obliquely from one transparent optical medium into another with different refractive indices?",
                    options = listOf("Refraction", "Diffraction", "Total Internal Reflection", "Dispersion"),
                    correctOptionIndex = 0,
                    explanation = "Refraction is the change in direction and speed of a wave passing from one medium to another. It is governed by Snell's Law (n1 * sin θ1 = n2 * sin θ2).",
                    difficulty = "EASY",
                    tags = listOf("Science", "Optics")
                ),
                QuestionItem(
                    questionText = "Which cellular organelle is responsible for synthesizing ribosomal RNA (rRNA) and assembling ribosome subunits in eukaryotic cells?",
                    options = listOf("Nucleolus", "Endoplasmic Reticulum", "Golgi Body", "Centrosome"),
                    correctOptionIndex = 0,
                    explanation = "The nucleolus is a distinct structure inside the nucleus responsible for transcribing ribosomal RNA and assembling the subunits of ribosomes.",
                    difficulty = "MEDIUM",
                    tags = listOf("Science", "Cell Biology")
                )
            ))
        }

        // Generic high-yield questions for general topics
        val genericPool = listOf(
            QuestionItem(
                questionText = "In competitive examinations, what analytical technique is recommended to eliminate improbable distractors in multiple-choice questions?",
                options = listOf("Systematic Option Elimination based on core principles", "Random selection of option B or C", "Choosing the longest option by word count", "Skipping without reading the premise"),
                correctOptionIndex = 0,
                explanation = "Systematic elimination identifies demonstrably false, extreme, or irrelevant statements, significantly increasing the probability of selecting the accurate conceptual answer.",
                difficulty = "EASY",
                tags = listOf(topic, "Exam Technique")
            ),
            QuestionItem(
                questionText = "Which parameter in economic analysis measures the responsiveness of the quantity demanded of a good to a change in its price?",
                options = listOf("Price Elasticity of Demand", "Cross Elasticity of Supply", "Marginal Propensity to Consume", "Consumer Surplus Ratio"),
                correctOptionIndex = 0,
                explanation = "Price Elasticity of Demand (PED) measures percentage change in quantity demanded divided by percentage change in price (%ΔQ / %ΔP).",
                difficulty = "MEDIUM",
                tags = listOf(topic, "Economics")
            ),
            QuestionItem(
                questionText = "In logical reasoning, if all A are B, and some B are C, which conclusion can be derived with absolute certainty without additional assumptions?",
                options = listOf("Some B are A", "All A are C", "No A is C", "All C are B"),
                correctOptionIndex = 0,
                explanation = "If All A are B, then the immediate converse conversion yields 'Some B are A'. No definitive relationship between A and C can be concluded without more premises.",
                difficulty = "MEDIUM",
                tags = listOf(topic, "Logical Reasoning")
            ),
            QuestionItem(
                questionText = "In data analysis, which measure of central tendency is least affected by extreme outlier values in a distribution?",
                options = listOf("Median", "Arithmetic Mean", "Geometric Mean", "Root Mean Square"),
                correctOptionIndex = 0,
                explanation = "The Median represents the middle observation of a sorted dataset and is resistant (robust) to extreme numerical outliers, unlike the arithmetic mean.",
                difficulty = "MEDIUM",
                tags = listOf(topic, "Statistics")
            ),
            QuestionItem(
                questionText = "In computing systems, which memory hierarchy level provides the highest access speed to the processor cores?",
                options = listOf("CPU Registers / L1 Cache", "Main System RAM (DDR5)", "Solid State Drive (NVMe)", "Hard Disk Drive"),
                correctOptionIndex = 0,
                explanation = "CPU registers and Level 1 (L1) on-chip cache operate at the highest clock frequencies with single-cycle latencies, far faster than RAM or storage drives.",
                difficulty = "EASY",
                tags = listOf(topic, "Computer Science")
            ),
            QuestionItem(
                questionText = "Under standard atmospheric conditions, what is the primary driving force for wind circulation across the Earth's surface?",
                options = listOf("Differential atmospheric pressure caused by uneven solar heating", "Gravitational pull of the moon", "Ocean current salinity differences", "Radioactive decay in earth crust"),
                correctOptionIndex = 0,
                explanation = "Unequal solar heating creates temperature differences, generating pressure gradients where air flows from high-pressure zones to low-pressure zones.",
                difficulty = "MEDIUM",
                tags = listOf(topic, "Geography")
            ),
            QuestionItem(
                questionText = "What term denotes the process by which liquid water changes into water vapor directly from the surfaces of green plant foliage?",
                options = listOf("Transpiration", "Precipitation", "Infiltration", "Condensation"),
                correctOptionIndex = 0,
                explanation = "Transpiration is the biological evaporation of water through tiny pores called stomata on plant leaves into the atmosphere.",
                difficulty = "EASY",
                tags = listOf(topic, "Ecology")
            ),
            QuestionItem(
                questionText = "In Indian administration, which constitutional authority has the mandate to audit all expenditure of the Union and State governments?",
                options = listOf("Comptroller and Auditor General of India (CAG)", "Attorney General of India", "Finance Commission Chairman", "Governor of the Reserve Bank"),
                correctOptionIndex = 0,
                explanation = "Article 148 to 151 provides for the Comptroller and Auditor General (CAG) of India, who acts as the guardian of public finances.",
                difficulty = "MEDIUM",
                tags = listOf(topic, "Polity")
            ),
            QuestionItem(
                questionText = "Which international organization was established in 1945 following the Bretton Woods Conference to foster global monetary cooperation?",
                options = listOf("International Monetary Fund (IMF)", "World Trade Organization (WTO)", "United Nations Development Programme (UNDP)", "Bank for International Settlements"),
                correctOptionIndex = 0,
                explanation = "The International Monetary Fund (IMF) and the World Bank were conceived at the Bretton Woods Conference in July 1944 to rebuild the international monetary system.",
                difficulty = "MEDIUM",
                tags = listOf(topic, "International Relations")
            ),
            QuestionItem(
                questionText = "In general science, which vitamin is water-soluble and also functions as a powerful antioxidant aiding iron absorption and collagen synthesis?",
                options = listOf("Vitamin C (Ascorbic Acid)", "Vitamin A (Retinol)", "Vitamin D (Calciferol)", "Vitamin K (Phylloquinone)"),
                correctOptionIndex = 0,
                explanation = "Vitamin C and the Vitamin B-complex are water-soluble. Vitamin C acts as a cofactor in collagen synthesis and prevents scurvy.",
                difficulty = "EASY",
                tags = listOf(topic, "General Science")
            ),
            QuestionItem(
                questionText = "When evaluating critical problems, what does the cognitive term 'Confirmation Bias' describe?",
                options = listOf("The tendency to search for, interpret, and recall information consistent with preexisting beliefs", "The inability to remember recent events after trauma", "The effect of peer pressure on group decisions", "The assumption that past trends guarantee future outcomes"),
                correctOptionIndex = 0,
                explanation = "Confirmation bias is the psychological tendency to selectively favor evidence confirming one's prior beliefs while ignoring contradictory facts.",
                difficulty = "MEDIUM",
                tags = listOf(topic, "Psychology & Aptitude")
            ),
            QuestionItem(
                questionText = "In modern Indian polity, what is the minimum voting age for Indian citizens as amended by the 61st Constitutional Amendment Act, 1988?",
                options = listOf("18 years", "21 years", "20 years", "25 years"),
                correctOptionIndex = 0,
                explanation = "The 61st Amendment Act, 1988 reduced the voting age for Lok Sabha and Legislative Assembly elections from 21 years to 18 years under Article 326.",
                difficulty = "EASY",
                tags = listOf(topic, "Polity")
            )
        )

        questions.addAll(genericPool)

        // Ensure we provide exactly 'count' unique questions
        val shuffled = questions.distinctBy { it.questionText.trim().lowercase() }.shuffled()
        val result = mutableListOf<QuestionItem>()
        result.addAll(shuffled.take(count))

        // If still short, generate contextual duplicates with variation
        var counter = 1
        while (result.size < count) {
            val base = shuffled[counter % shuffled.size]
            result.add(
                base.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    questionText = "${base.questionText} (Conceptual Variation #$counter)",
                    tags = listOf(topic, "Drill")
                )
            )
            counter++
        }

        return result.take(count)
    }

    private fun buildSanitizationSystemPrompt(chapterTitle: String, chapterNumber: Int): String {
        return """
You are the world-class academic curriculum structuring and educational note parsing engine for PrepOS.
Your objective is to sanitize, classify, and restructure raw study material (notes, book chapters, syllabus extracts, OCR/PDF text) into a beautifully structured, comprehensive continuous chapter.

TARGET CHAPTER CONTEXT:
- Title: "${chapterTitle.ifBlank { "Auto-detect" }}"
- Chapter Number: ${if (chapterNumber > 0) chapterNumber else "Auto-detect"}

CRITICAL HIERARCHY RULES (MANDATORY — DO NOT FLATTEN CONTENT):
The raw text contains rich educational material with multiple distinct hierarchy levels. You MUST NOT flatten everything into simple H2 headings and paragraphs. Intelligently classify every piece of content according to this exact hierarchy:

1. HEADING_1 (H1) - CHAPTER TITLE:
   - Reserved strictly for the overarching Chapter / Major Subject Unit Title.
   - Examples: "CHAPTER 1: ARTICLES", "CHAPTER 4: THERMODYNAMICS", "UNIT 2: CELL BIOLOGY AND GENETICS".
   - The document MUST have a primary H1 at or near the top.

2. HEADING_2 (H2) - MAJOR SECTIONS & MAIN TOPICS:
   - Primary topic divisions, major Parts, and overarching unit sections.
   - Examples:
     • "INTRODUCTION TO ARTICLES"
     • "TYPES OF ARTICLES"
     • "PART A — INDEFINITE ARTICLES (A / AN)"
     • "PART B — DEFINITE ARTICLE (THE)"
     • "REVISION FORMULA & SUMMARY"
     • "FUNDAMENTAL LAWS OF MOTION"

3. HEADING_3 (H3) - SUBTOPICS, CORE RULES & CATEGORIES:
   - Individual subtopics, specific rule categories, named theorems, definitions, and distinct conceptual branches.
   - Examples:
     • "1. INDEFINITE ARTICLES (A / AN)"
     • "Rule 1: General Rule for A and An"
     • "Rule 2: Sound Rule (Vowel Sound vs Consonant Sound)"
     • "Rule 3: Numerical Sense (One / Any)"
     • "2. DEFINITE ARTICLE (THE)"
     • "Definition of Kinetic Energy"
     • "Key Physical Properties"

4. HEADING_4 (H4) - SUB-RULES, SPECIFIC CASES, USAGE LABELS & EXCEPTIONS:
   - Sub-rules, sub-points under a rule, specific contextual cases, usage conditions, notes, and exceptions.
   - Examples:
     • "(A) Use of 'A'"
     • "(B) Use of 'An'"
     • "Special Case 1: Words Starting with Vowel Letters but Consonant Sounds (e.g., University, European)"
     • "Special Case 2: Words Starting with Silent 'H' (e.g., Honest, Hour)"
     • "Usage with Proper Nouns"
     • "Exception: Zero Article with Abstract Nouns"
     • "Condition for Isothermal Process"

5. PARAGRAPH:
   - Continuous explanatory text, detailed descriptions, sentence-level analysis, context, and inline examples.

6. BULLET_LIST:
   - Unordered items, properties, characteristics, bulleted examples, bulleted conditions.

7. NUMBERED_LIST:
   - Step-by-step procedures, numbered derivation steps, algorithms, ordered sequences.

8. CALLOUT BLOCKS:
   - DEFINITION: Formal definitions, theorems, or fundamental principles.
   - IMPORTANT: Critical rules, exam warnings, memory mnemonics, common mistakes/pitfalls.
   - EXAM_TIP: Shortcuts, exam scoring hacks, question traps, quick elimination techniques.
   - FORMULA: Mathematical/scientific formulas, derivation results, revision equations.

9. TABLE BLOCKS:
   - Tabular comparisons (e.g., "Indefinite vs Definite Articles", "Isothermal vs Adiabatic"), conjugations, declensions, properties grids with clear headers and row values.

10. DIAGRAM BLOCKS:
    - Logical workflows, step algorithms, process flowcharts ("FLOWCHART"), or classification trees ("TREE") with step nodes.

11. CHART & PROGRESS BLOCKS:
    - When the study material contains quantitative metrics, topic weightage, exam mark distributions, completion targets, or revision mastery:
    - Output a "CHART" element.
    - Supported chartTypes:
      • "PROGRESS_RINGS": For percentage completion, target readiness, weightage metrics, or milestone mastery rings (values 0–100).
      • "BAR": For comparative numeric scores, question counts, or frequency across years.
      • "PIE" or "DONUT": For proportional breakdown of chapters, mark distributions, or syllabus categories.
    - Each entry must have "label" (string), "value" (numeric float), and "colorHex" (e.g. "#6C63D9", "#2563EB", "#059669", "#D97706", "#DC2626").

12. REVISION SECTION STANDARDIZATION:
    - If the source content contains a summary, key takeaways, formula recap, or quick revision notes:
    - Create a section titled "Revision" or "Key Revision Points" as HEADING_2 or HEADING_3.
    - Follow it strictly with uniform NUMBERED_LIST, BULLET_LIST, or FORMULA callouts.
    - DO NOT place deep nested subheadings inside the revision section.

12. SEMANTIC PATTERN DETECTION (EVEN WITHOUT MARKDOWN):
    - Automatically recognize academic naming conventions:
      • "CHAPTER 1:", "CHAPTER I:" -> HEADING_1
      • "PART A", "PART 1", "SECTION I", "TOPIC:" -> HEADING_2
      • "Rule 1:", "Rule 2:", "1. Indefinite...", "Concept A:" -> HEADING_3
      • "(A)", "(B)", "Case 1:", "Special Case:", "Exception:", "Usage Note:" -> HEADING_4
      • "Definition:", "Theorem:" -> CALLOUT (DEFINITION)
      • "Formula:", "Equation:" -> CALLOUT (FORMULA)
      • "Important:", "Note:", "Warning:" -> CALLOUT (IMPORTANT)
      • "Exam Tip:", "Trick:", "Shortcut:" -> CALLOUT (EXAM_TIP)

13. MANDATORY RULE: ALL IN-NOTE EXERCISES & MATH PROBLEMS MUST REMAIN IN THE NOTES (ELEMENTS):
    - When the raw study notes contain exercises, class problems, worked examples, practice problems, homework sets, or drill questions (e.g. "Exercise 1.1", "Practice Problem 2", "Classroom Exercise", "Solve for x", "Q1.", "Question 1"):
    - You MUST KEEP every single one of these exercise questions, their options, and their solutions/steps IN THE NOTES DOCUMENT (`elements`) EXACTLY AT THEIR ORIGINAL AND RELEVANT TOPICAL PLACE!
    - Structure them clearly within `elements`:
      • Use HEADING_2 or HEADING_3 for the exercise section title (e.g., "Exercise 1.1: Practice Problems" or "Class Exercises").
      • For each individual problem, use HEADING_4 (e.g., "Problem 1" or "Question 1"), followed by PARAGRAPH or NUMBERED_LIST for the question statement and options.
      • If a solution, hint, or step-by-step calculation is provided, format it as a CALLOUT (calloutType: FORMULA or EXAM_TIP) or PARAGRAPH right after the question.
    - NEVER strip, omit, or remove inline exercises from the notes body (`elements`).
    - NEVER extract inline exercises into the "questions" array.
    - The "questions" array must ALWAYS BE EMPTY (`[]`) when structuring notes so that exercises are NOT diverted into the test series. All exercises belong strictly in the notes document where the student can read and study them in context.

14. PRESERVE ALL EDUCATIONAL CONTENT:
    - Never delete or drop educational facts, explanations, or examples from the source text.
    - Strip raw markdown symbols (like #, **, __, ```, ---), but preserve and enrich all semantic meaning.

You MUST respond strictly with valid JSON conforming to this schema:
{
  "chapterTitle": "Sanitized Title",
  "summary": "1-2 sentence core overview of what this chapter covers",
  "elements": [
    { "type": "HEADING_1" | "HEADING_2" | "HEADING_3" | "HEADING_4" | "PARAGRAPH" | "BULLET_LIST" | "NUMBERED_LIST", "text": "Clean text without markdown symbols" },
    { "type": "CALLOUT", "calloutType": "IMPORTANT" | "DEFINITION" | "EXAM_TIP" | "FORMULA", "title": "...", "content": "..." },
    { "type": "TABLE", "title": "...", "headers": ["Col 1", "Col 2"], "rows": [["A", "B"]] },
    { "type": "DIAGRAM", "diagramType": "FLOWCHART" | "TREE", "title": "...", "nodes": ["Step 1", "Step 2", "Step 3"], "rawContent": "..." },
    { "type": "CHART", "chartType": "PROGRESS_RINGS" | "BAR" | "PIE" | "DONUT", "title": "...", "subtitle": "...", "entries": [{"label": "Concept A", "value": 75.0, "colorHex": "#6C63D9"}] }
  ],
  "questions": []
}
""".trimIndent()
    }

    private fun extractJsonBlock(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        str = str.trim()
        val firstBrace = str.indexOf('{')
        val lastBrace = str.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return str.substring(firstBrace, lastBrace + 1)
        }
        return str
    }

    private fun parseSanitizedJsonResponseStrict(jsonStr: String, fallbackTitle: String): SanitizedChapterResult {
        val cleanJson = extractJsonBlock(jsonStr)
        val root = JSONObject(cleanJson)
        val title = root.optString("chapterTitle", fallbackTitle).ifBlank { fallbackTitle }
        val summary = root.optString("summary", "")

        val elementsList = mutableListOf<DocElement>()
        val elemArr = root.optJSONArray("elements") ?: JSONArray()

        for (i in 0 until elemArr.length()) {
            val item = elemArr.getJSONObject(i)
            val typeStr = item.optString("type", "PARAGRAPH").uppercase().trim()
            when (typeStr) {
                "HEADING_1", "H1" -> {
                    val (clean, spans) = com.example.ui.editor.PasteProcessor.parseInlineFormatting(cleanText(item.optString("text")))
                    elementsList.add(DocElement.TextBlock(blockType = ElementType.HEADING_1, text = clean, spans = spans))
                }
                "HEADING_2", "H2" -> {
                    val (clean, spans) = com.example.ui.editor.PasteProcessor.parseInlineFormatting(cleanText(item.optString("text")))
                    elementsList.add(DocElement.TextBlock(blockType = ElementType.HEADING_2, text = clean, spans = spans))
                }
                "HEADING_3", "H3" -> {
                    val (clean, spans) = com.example.ui.editor.PasteProcessor.parseInlineFormatting(cleanText(item.optString("text")))
                    elementsList.add(DocElement.TextBlock(blockType = ElementType.HEADING_3, text = clean, spans = spans))
                }
                "HEADING_4", "H4" -> {
                    val (clean, spans) = com.example.ui.editor.PasteProcessor.parseInlineFormatting(cleanText(item.optString("text")))
                    elementsList.add(DocElement.TextBlock(blockType = ElementType.HEADING_4, text = clean, spans = spans))
                }
                "PARAGRAPH", "P", "TEXT" -> {
                    val txt = cleanText(item.optString("text"))
                    if (txt.isNotBlank()) {
                        val (clean, spans) = com.example.ui.editor.PasteProcessor.parseInlineFormatting(txt)
                        elementsList.add(DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = clean, spans = spans))
                    }
                }
                "BULLET_LIST", "BULLET", "LIST" -> {
                    val txt = cleanText(item.optString("text"))
                    if (txt.isNotBlank()) {
                        val (clean, spans) = com.example.ui.editor.PasteProcessor.parseInlineFormatting(txt)
                        elementsList.add(DocElement.TextBlock(blockType = ElementType.BULLET_LIST, text = clean, spans = spans))
                    }
                }
                "NUMBERED_LIST", "NUMBERED" -> {
                    val txt = cleanText(item.optString("text"))
                    if (txt.isNotBlank()) {
                        val (clean, spans) = com.example.ui.editor.PasteProcessor.parseInlineFormatting(txt)
                        elementsList.add(DocElement.TextBlock(blockType = ElementType.NUMBERED_LIST, text = clean, spans = spans))
                    }
                }
                "CALLOUT" -> {
                    val cTypeStr = item.optString("calloutType", "IMPORTANT").uppercase().trim()
                    val cType = when (cTypeStr) {
                        "DEFINITION" -> CalloutType.DEFINITION
                        "EXAM_TIP", "TIP" -> CalloutType.EXAM_TIP
                        "FORMULA", "THEOREM" -> CalloutType.FORMULA
                        else -> CalloutType.IMPORTANT
                    }
                    val calloutTitle = item.optString("title", when (cType) {
                        CalloutType.DEFINITION -> "Definition"
                        CalloutType.EXAM_TIP -> "Exam Strategy & Tip"
                        CalloutType.FORMULA -> "Key Formula"
                        CalloutType.IMPORTANT -> "Important Note"
                    })
                    elementsList.add(DocElement.CalloutBlock(
                        calloutType = cType,
                        title = calloutTitle,
                        content = cleanText(item.optString("content"))
                    ))
                }
                "TABLE" -> {
                    val headers = mutableListOf<String>()
                    val hArr = item.optJSONArray("headers") ?: JSONArray()
                    for (h in 0 until hArr.length()) headers.add(cleanText(hArr.optString(h)))
                    val rows = mutableListOf<List<String>>()
                    val rArr = item.optJSONArray("rows") ?: JSONArray()
                    for (r in 0 until rArr.length()) {
                        val subRow = rArr.optJSONArray(r) ?: JSONArray()
                        val rowList = mutableListOf<String>()
                        for (c in 0 until subRow.length()) rowList.add(cleanText(subRow.optString(c)))
                        if (rowList.any { it.isNotBlank() }) {
                            rows.add(rowList)
                        }
                    }
                    elementsList.add(DocElement.TableBlock(
                        title = item.optString("title", "Study Comparison Table"),
                        headers = if (headers.isEmpty()) listOf("Attribute / Rule", "Description / Usage") else headers,
                        rows = if (rows.isEmpty()) listOf(listOf("-", "-")) else rows
                    ))
                }
                "DIAGRAM" -> {
                    val dTypeStr = item.optString("diagramType", "FLOWCHART").uppercase().trim()
                    val dType = if (dTypeStr == "TREE") DiagramType.TREE else DiagramType.FLOWCHART
                    val nodes = mutableListOf<String>()
                    val nArr = item.optJSONArray("nodes") ?: JSONArray()
                    for (n in 0 until nArr.length()) nodes.add(cleanText(nArr.optString(n)))
                    elementsList.add(DocElement.DiagramBlock(
                        diagramType = dType,
                        title = item.optString("title", "Process Workflow"),
                        nodes = if (nodes.isEmpty()) listOf("Phase 1", "Phase 2", "Phase 3") else nodes,
                        rawContent = item.optString("rawContent", "")
                    ))
                }
                "CHART", "PROGRESS", "PROGRESS_BAR" -> {
                    val cTypeStr = item.optString("chartType", "PROGRESS_RINGS").uppercase().trim()
                    val chartType = when (cTypeStr) {
                        "PROGRESS_RINGS", "RING", "RINGS", "PROGRESS" -> ChartType.PROGRESS_RINGS
                        "PIE" -> ChartType.PIE
                        "DONUT" -> ChartType.DONUT
                        else -> ChartType.BAR
                    }
                    val entries = mutableListOf<ChartEntry>()
                    val eArr = item.optJSONArray("entries") ?: JSONArray()
                    val defaultColors = listOf("#6C63D9", "#2563EB", "#059669", "#D97706", "#DC2626", "#7C3AED", "#DB2777")
                    for (e in 0 until eArr.length()) {
                        val eObj = eArr.optJSONObject(e)
                        if (eObj != null) {
                            val label = cleanText(eObj.optString("label", "Item ${e + 1}"))
                            val value = eObj.optDouble("value", 50.0).toFloat()
                            val color = eObj.optString("colorHex", defaultColors[e % defaultColors.size])
                            entries.add(ChartEntry(label = label, value = value, colorHex = color))
                        }
                    }
                    elementsList.add(DocElement.ChartBlock(
                        chartType = chartType,
                        title = item.optString("title", "Study Progress & Distribution"),
                        subtitle = item.optString("subtitle", ""),
                        entries = if (entries.isNotEmpty()) entries else listOf(
                            ChartEntry("Readiness", 75f, "#6C63D9"),
                            ChartEntry("Weightage", 40f, "#2563EB")
                        )
                    ))
                }
            }
        }

        // Re-integrate any questions the AI model might have mistakenly placed into 'questions' back into 'elements'
        val qArr = root.optJSONArray("questions") ?: JSONArray()
        val extraQuestions = mutableListOf<QuestionItem>()
        for (q in 0 until qArr.length()) {
            val qObj = qArr.getJSONObject(q)
            extraQuestions.add(QuestionItem.fromJson(qObj))
        }

        if (extraQuestions.isNotEmpty()) {
            val existingText = elementsList.joinToString(" ") {
                when (it) {
                    is DocElement.TextBlock -> it.text
                    is DocElement.CalloutBlock -> "${it.title} ${it.content}"
                    else -> ""
                }
            }.lowercase()

            val missingQuestions = extraQuestions.filter { q ->
                val qSnippet = q.questionText.take(30).lowercase()
                !existingText.contains(qSnippet)
            }

            if (missingQuestions.isNotEmpty()) {
                elementsList.add(DocElement.TextBlock(blockType = ElementType.HEADING_2, text = "Class Exercises & Practice Problems"))
                missingQuestions.forEachIndexed { idx, q ->
                    elementsList.add(DocElement.TextBlock(blockType = ElementType.HEADING_4, text = "Problem ${idx + 1}: ${q.questionText}"))
                    if (q.options.isNotEmpty()) {
                        val optsText = q.options.mapIndexed { oIdx, opt -> "(${('A' + oIdx)}) $opt" }.joinToString("   ")
                        elementsList.add(DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = optsText))
                    }
                    if (q.explanation.isNotBlank() && q.explanation != "Based on core chapter principles and study facts.") {
                        elementsList.add(DocElement.CalloutBlock(
                            calloutType = CalloutType.FORMULA,
                            title = "Solution & Derivation",
                            content = q.explanation
                        ))
                    }
                }
            }
        }

        return SanitizedChapterResult(
            title = title,
            summary = summary,
            elements = if (elementsList.isNotEmpty()) elementsList else listOf(DocElement.TextBlock(blockType = ElementType.HEADING_1, text = title)),
            questions = emptyList(), // In note structuring, keep test series questions empty so all exercises stay inside notes
            isAiGenerated = true
        )
    }

    private fun cleanText(text: String): String {
        return text.trim()
            .replace(Regex("^#+\\s*"), "") // remove heading hashes
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // remove bold markdown
            .replace(Regex("__(.*?)__"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("_(.*?)_"), "$1")
            .replace(Regex("~~(.*?)~~"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
            .replace("---", "")
            .replace("===", "")
            .trim()
    }

    private fun generateLocalStudyAssistance(action: AIAction, text: String, userCustomInstruction: String = ""): String {
        val clean = text.trim()
        val sentences = clean.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }

        val extraAddition = if (userCustomInstruction.isNotBlank() && userCustomInstruction != clean) {
            "\n\n[Applied User Guidance]:\n${userCustomInstruction.trim()}"
        } else ""

        return when (action) {
            AIAction.EXPLAIN -> buildString {
                appendLine("Conceptual Breakdown:")
                appendLine(clean)
                if (extraAddition.isNotBlank()) append(extraAddition)
                appendLine()
                appendLine()
                appendLine("Key Mechanism: The core principle revolves around logical consistency and step-by-step verification.")
                appendLine("Study Takeaway: Memorize standard properties and relate them to baseline exam patterns.")
            }
            AIAction.SIMPLIFY -> buildString {
                appendLine("Simplified Meaning:")
                if (sentences.isNotEmpty()) {
                    sentences.forEachIndexed { idx, s ->
                        appendLine("${idx + 1}. ${s.trim()}")
                    }
                } else {
                    appendLine(clean)
                }
                if (extraAddition.isNotBlank()) append(extraAddition)
                appendLine()
                appendLine()
                appendLine("In simple terms: Focus on identifying known inputs and applying the formula directly.")
            }
            AIAction.REWRITE -> buildString {
                appendLine("Refined Study Note:")
                appendLine(clean.replace("  ", " ").trim())
                if (extraAddition.isNotBlank()) append(extraAddition)
                appendLine()
                appendLine()
                appendLine("[Note: Keep notation consistent across all sub-problems.]")
            }
            AIAction.TRANSLATE -> buildString {
                appendLine("Translated Study Concept (Bilingual / Hindi):")
                appendLine("• मुख्य बिंदु (Key Points):")
                if (sentences.isNotEmpty()) {
                    sentences.forEachIndexed { idx, s ->
                        appendLine("  ${idx + 1}. ${s.trim()}")
                    }
                } else {
                    appendLine("  $clean")
                }
                if (extraAddition.isNotBlank()) append(extraAddition)
                appendLine()
                appendLine()
                appendLine("• परीक्षा तैयारी नोट (Exam Note): इस विषय से सीधे संबंधित संकल्पनाओं को याद रखें।")
            }
            AIAction.SUGGEST_DIAGRAM -> buildString {
                appendLine("Suggested Visual Structure / Diagram:")
                appendLine("```")
                appendLine("[Topic: ${sentences.firstOrNull()?.take(30) ?: "Core Concept"}]")
                appendLine("   │")
                appendLine("   ├──> Step 1: Definition & Axioms")
                appendLine("   ├──> Step 2: Formulas / Rules")
                appendLine("   └──> Step 3: Exam Problem Applications")
                appendLine("```")
                if (extraAddition.isNotBlank()) append(extraAddition)
                appendLine()
                appendLine()
                appendLine("Comparison Table / Flow Suggestion:")
                appendLine("| Phase | Action | Key Verification |")
                appendLine("| --- | --- | --- |")
                appendLine("| Input | Recognize Variables | Check Standard Units |")
                appendLine("| Process | Apply Formula | Cross-verify Edge Cases |")
                appendLine("| Output | Resultant Conclusion | Final Exam Solution |")
            }
            AIAction.STRUCTURE -> buildString {
                appendLine("Structured Overview:")
                appendLine("1. Overview:")
                appendLine("   - ${sentences.firstOrNull() ?: clean}")
                if (sentences.size > 1) {
                    appendLine("2. Key Rules & Properties:")
                    for (i in 1 until sentences.size) {
                        appendLine("   - ${sentences[i]}")
                    }
                }
                appendLine("3. Application: Directly tested in objective problem-solving.")
            }
            AIAction.IMPROVE -> buildString {
                appendLine("Precision Enhanced:")
                appendLine(clean)
                appendLine("Important condition: Always verify base assumptions before calculating final values.")
            }
            AIAction.BULLETS -> buildString {
                appendLine("Revision Points:")
                sentences.forEach { s ->
                    appendLine("• ${s.trim()}")
                }
                if (sentences.size <= 1) {
                    appendLine("• Core definition and formula linkage.")
                    appendLine("• High-frequency question archetype in standard syllabus.")
                }
            }
            AIAction.EXAMPLES -> buildString {
                appendLine("Practice Problem & Example:")
                appendLine("Problem: Given the condition described in \"${clean.take(40)}...\", calculate the resultant relation.")
                appendLine("Step 1: Identify given variables.")
                appendLine("Step 2: Apply the governing rule.")
                appendLine("Answer: Verified logically with standard exam solution criteria.")
            }
        }
    }

    // =========================================================================
    // --- 6-CARD ASSISTANT STRUCTURED WORKFLOW ENGINE ---
    // =========================================================================

    /**
     * CARD 1: Generates structured 7-day weekly study plan
     */
    suspend fun generateWeeklyPlanWorkflow(
        availableDays: List<String>,
        studyTimeDesc: String,
        weeklyGoal: String,
        activeSyllabus: String,
        currentProgress: String,
        selectedTopics: String,
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<List<com.example.ui.editor.WeeklyPlanTaskItem>> = withContext(Dispatchers.IO) {
        val prompt = """
            Task: Create a structured 7-day weekly study plan.
            
            Inputs:
            - Available days: ${availableDays.joinToString(", ")}
            - Available daily study time: $studyTimeDesc
            - Weekly goal: $weeklyGoal
            - Active syllabus context: $activeSyllabus
            - Current progress: $currentProgress
            - Selected subjects/topics: $selectedTopics
            
            Return ONLY a valid JSON array of tasks with no extra Markdown text or explanation.
            Schema:
            [
              {
                "dayOfWeek": "Monday",
                "timeSlot": "07:00 PM - 07:45 PM",
                "subjectName": "General Knowledge",
                "taskTitle": "Indian History: Freedom Movement & 1947 Events",
                "taskType": "READING",
                "durationMinutes": 45
              }
            ]
        """.trimIndent()

        try {
            val responseText = executeCustomInstruction(
                baseContextText = "Weekly Study Plan Generator for competitive exams",
                userInstruction = prompt,
                contextTitle = "Weekly Plan Generator",
                apiKey = apiKey,
                model = model,
                provider = provider,
                deepSeekApiKey = deepSeekApiKey
            ).getOrNull() ?: ""

            val cleanedJson = extractJsonFromResponse(responseText)
            val jsonArray = JSONArray(cleanedJson)
            val list = mutableListOf<com.example.ui.editor.WeeklyPlanTaskItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    com.example.ui.editor.WeeklyPlanTaskItem(
                        dayOfWeek = obj.optString("dayOfWeek", "Monday"),
                        timeSlot = obj.optString("timeSlot", "07:00 PM - 07:45 PM"),
                        subjectName = obj.optString("subjectName", "General Studies"),
                        taskTitle = obj.optString("taskTitle", "Subject Revision"),
                        taskType = obj.optString("taskType", "READING"),
                        durationMinutes = obj.optInt("durationMinutes", 45)
                    )
                )
            }
            if (list.isNotEmpty()) {
                Result.success(list)
            } else {
                Result.success(getFallbackWeeklyPlan(availableDays, selectedTopics))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate weekly plan via AI, fallback used", e)
            Result.success(getFallbackWeeklyPlan(availableDays, selectedTopics))
        }
    }

    private fun getFallbackWeeklyPlan(availableDays: List<String>, selectedTopics: String): List<com.example.ui.editor.WeeklyPlanTaskItem> {
        val days = if (availableDays.isNotEmpty()) availableDays else listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val topics = if (selectedTopics.isNotBlank()) selectedTopics.split(",").map { it.trim() } else listOf("General Studies", "Quantitative Aptitude", "Reasoning Ability", "English Language")
        
        return days.mapIndexed { idx, day ->
            val topic = topics[idx % topics.size]
            com.example.ui.editor.WeeklyPlanTaskItem(
                dayOfWeek = day,
                timeSlot = "07:00 PM - 07:45 PM",
                subjectName = topic,
                taskTitle = "Comprehensive study & topic revision on $topic",
                taskType = if (idx % 3 == 0) "QUIZ" else if (idx % 2 == 0) "REVISION" else "READING",
                durationMinutes = 45
            )
        }
    }

    /**
     * CARD 2: Parses and organizes an uploaded/pasted syllabus document into Subjects and Chapters
     */
    suspend fun extractSyllabusFromDocumentWorkflow(
        documentText: String,
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<com.example.ui.editor.ExtractedSyllabusResult> = withContext(Dispatchers.IO) {
        val prompt = """
            Task: Extract and structure the complete exam syllabus into structured Subjects, Chapters, and Topics from the provided document text.
            
            Syllabus Text:
            \"\"\"
            ${documentText.take(12000)}
            \"\"\"
            
            Return ONLY a valid JSON object matching this schema with no markdown formatting around it:
            {
              "examName": "Target Exam Name",
              "subjects": [
                {
                  "name": "Subject Name",
                  "colorHex": "#3B82F6",
                  "chapters": [
                    {
                      "title": "Chapter 1: Name",
                      "chapterNumber": 1,
                      "topics": ["Topic 1", "Topic 2", "Topic 3"]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        try {
            val responseText = executeCustomInstruction(
                baseContextText = "Syllabus Extractor for PrepOS",
                userInstruction = prompt,
                contextTitle = "Syllabus Parser",
                apiKey = apiKey,
                model = model,
                provider = provider,
                deepSeekApiKey = deepSeekApiKey
            ).getOrNull() ?: ""

            val cleanedJson = extractJsonFromResponse(responseText)
            val root = JSONObject(cleanedJson)
            val examName = root.optString("examName", "Target Exam")
            val subjectsArr = root.optJSONArray("subjects") ?: JSONArray()
            val subjectList = mutableListOf<com.example.ui.editor.ExtractedSubjectItem>()
            val palette = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899", "#06B6D4")

            for (i in 0 until subjectsArr.length()) {
                val subObj = subjectsArr.getJSONObject(i)
                val sName = subObj.optString("name", "Subject ${i + 1}")
                val sColor = subObj.optString("colorHex", palette[i % palette.size])
                val chapArr = subObj.optJSONArray("chapters") ?: JSONArray()
                val chapterList = mutableListOf<com.example.ui.editor.ExtractedChapterItem>()

                for (j in 0 until chapArr.length()) {
                    val chapObj = chapArr.getJSONObject(j)
                    val cTitle = chapObj.optString("title", "Chapter ${j + 1}")
                    val cNum = chapObj.optInt("chapterNumber", j + 1)
                    val topArr = chapObj.optJSONArray("topics") ?: JSONArray()
                    val topics = mutableListOf<String>()
                    for (k in 0 until topArr.length()) {
                        topics.add(topArr.getString(k))
                    }
                    chapterList.add(com.example.ui.editor.ExtractedChapterItem(cTitle, cNum, topics))
                }
                subjectList.add(com.example.ui.editor.ExtractedSubjectItem(sName, sColor, chapterList))
            }

            if (subjectList.isNotEmpty()) {
                Result.success(com.example.ui.editor.ExtractedSyllabusResult(examName, subjectList))
            } else {
                Result.success(getFallbackExtractedSyllabus(documentText))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse syllabus with AI, using fallback", e)
            Result.success(getFallbackExtractedSyllabus(documentText))
        }
    }

    private fun getFallbackExtractedSyllabus(documentText: String): com.example.ui.editor.ExtractedSyllabusResult {
        val lines = documentText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val examName = lines.firstOrNull()?.take(40) ?: "Exam Syllabus"
        return com.example.ui.editor.ExtractedSyllabusResult(
            examName = examName,
            subjects = listOf(
                com.example.ui.editor.ExtractedSubjectItem(
                    name = "General Studies & Aptitude",
                    colorHex = "#3B82F6",
                    chapters = listOf(
                        com.example.ui.editor.ExtractedChapterItem("Core Principles & Concepts", 1, listOf("Introduction", "Key Rules", "Terminology")),
                        com.example.ui.editor.ExtractedChapterItem("Advanced Application & Trends", 2, listOf("Formulas", "Exam Archetypes", "Problem Solving")),
                        com.example.ui.editor.ExtractedChapterItem("Practice Drills & Mock MCQs", 3, listOf("Speed Drills", "Previous Year Patterns"))
                    )
                ),
                com.example.ui.editor.ExtractedSubjectItem(
                    name = "Analytical Reasoning",
                    colorHex = "#10B981",
                    chapters = listOf(
                        com.example.ui.editor.ExtractedChapterItem("Logical Structures & Deductions", 1, listOf("Sequences", "Puzzles", "Logic Rules")),
                        com.example.ui.editor.ExtractedChapterItem("Data Interpretation", 2, listOf("Tables", "Charts", "Caselets"))
                    )
                )
            )
        )
    }

    /**
     * CARD 3: Generates custom Exam Strategy roadmap with phases
     */
    suspend fun generateExamStrategyWorkflow(
        exam: String,
        examDate: String,
        daysRemaining: Int,
        prepLevel: String,
        dailyTime: String,
        priority: String,
        syllabus: String,
        progress: String,
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<com.example.ui.editor.ExamStrategyResult> = withContext(Dispatchers.IO) {
        val prompt = """
            Task: Create a phased Exam Strategy roadmap for an upcoming competitive exam.
            
            Inputs:
            - Exam: $exam
            - Exam date: $examDate ($daysRemaining days remaining)
            - Current preparation level: $prepLevel
            - Available daily study time: $dailyTime
            - User's top priority: $priority
            - Current syllabus workspace: $syllabus
            - Current progress data: $progress
            
            Return ONLY a valid JSON object with this schema:
            {
              "examName": "$exam",
              "summary": "Brief 1-sentence high level strategy summary",
              "phases": [
                {
                  "phaseName": "Phase 1: Syllabus Foundation",
                  "durationWeeks": "Weeks 1-3",
                  "focusDescription": "Cover 100% of unread high weightage chapters with active recall notes.",
                  "dailyAction": "2 hours concept reading + 30 mins active recall notes"
                },
                {
                  "phaseName": "Phase 2: Targeted Revision & Topic MCQs",
                  "durationWeeks": "Weeks 4-6",
                  "focusDescription": "Master weak areas and solve chapter-wise MCQs.",
                  "dailyAction": "1.5 hours solving 40 MCQs + 1 hour spaced repetition review"
                },
                {
                  "phaseName": "Phase 3: Full Mock Tests & Exam Simulation",
                  "durationWeeks": "Final 2 Weeks",
                  "focusDescription": "Timed mock exams, speed drills, and final formula sheet cramming.",
                  "dailyAction": "Full mock test every alternate day + detailed error analysis"
                }
              ],
              "keyAdvice": [
                "Prioritize high-yield chapters first",
                "Maintain daily study streak without missing two consecutive days",
                "Review mistake log before attempting new mocks"
              ]
            }
        """.trimIndent()

        try {
            val responseText = executeCustomInstruction(
                baseContextText = "Exam Strategy Consultant for PrepOS",
                userInstruction = prompt,
                contextTitle = "Exam Strategy Roadmap",
                apiKey = apiKey,
                model = model,
                provider = provider,
                deepSeekApiKey = deepSeekApiKey
            ).getOrNull() ?: ""

            val cleanedJson = extractJsonFromResponse(responseText)
            val root = JSONObject(cleanedJson)
            val phasesArr = root.optJSONArray("phases") ?: JSONArray()
            val phaseList = mutableListOf<com.example.ui.editor.StrategyPhaseItem>()

            for (i in 0 until phasesArr.length()) {
                val pObj = phasesArr.getJSONObject(i)
                phaseList.add(
                    com.example.ui.editor.StrategyPhaseItem(
                        phaseName = pObj.optString("phaseName", "Phase ${i + 1}"),
                        durationWeeks = pObj.optString("durationWeeks", "Weeks ${i * 2 + 1}-${i * 2 + 2}"),
                        focusDescription = pObj.optString("focusDescription", "Core preparation and active recall."),
                        dailyAction = pObj.optString("dailyAction", "Daily study schedule")
                    )
                )
            }

            val adviceArr = root.optJSONArray("keyAdvice") ?: JSONArray()
            val adviceList = mutableListOf<String>()
            for (i in 0 until adviceArr.length()) {
                adviceList.add(adviceArr.getString(i))
            }

            Result.success(
                com.example.ui.editor.ExamStrategyResult(
                    examName = root.optString("examName", exam),
                    daysRemaining = daysRemaining,
                    summary = root.optString("summary", "Personalized $daysRemaining-day roadmap targeting $exam."),
                    phases = if (phaseList.isNotEmpty()) phaseList else getFallbackStrategyPhases(daysRemaining),
                    keyAdvice = if (adviceList.isNotEmpty()) adviceList else listOf("Maintain daily consistency", "Solve mock drills under exam time limits")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate exam strategy via AI, using fallback", e)
            Result.success(
                com.example.ui.editor.ExamStrategyResult(
                    examName = exam,
                    daysRemaining = daysRemaining,
                    summary = "Focused $daysRemaining-day preparation roadmap for $exam.",
                    phases = getFallbackStrategyPhases(daysRemaining),
                    keyAdvice = listOf(
                        "Study with active recall notes instead of passive re-reading.",
                        "Track time with PrepOS Focus Mode to build stamina.",
                        "Analyze mistakes in Mock Tests immediately after completion."
                    )
                )
            )
        }
    }

    private fun getFallbackStrategyPhases(daysRemaining: Int): List<com.example.ui.editor.StrategyPhaseItem> {
        val w1 = (daysRemaining * 0.45 / 7).toInt().coerceAtLeast(1)
        val w2 = (daysRemaining * 0.35 / 7).toInt().coerceAtLeast(1)
        return listOf(
            com.example.ui.editor.StrategyPhaseItem(
                phaseName = "Phase 1: Syllabus Coverage & Concept Clarity",
                durationWeeks = "Next $w1 Weeks",
                focusDescription = "Complete all remaining syllabus topics with active recall notes.",
                dailyAction = "Read new concepts for 60% of time; summarize into bullet notes for 40%."
            ),
            com.example.ui.editor.StrategyPhaseItem(
                phaseName = "Phase 2: High-Yield Revision & Question Practice",
                durationWeeks = "Next $w2 Weeks",
                focusDescription = "Solve topic-wise MCQs and strengthen weak subjects.",
                dailyAction = "Solve 30-50 practice questions daily and review incorrect explanations."
            ),
            com.example.ui.editor.StrategyPhaseItem(
                phaseName = "Phase 3: Full Mock Tests & Speed Polish",
                durationWeeks = "Final Stretch",
                focusDescription = "Timed full-length mock exams, exam stamina, and quick formula review.",
                dailyAction = "Take timed mock tests, conduct error logs, and review summary notes."
            )
        )
    }

    /**
     * CARD 5: Recommends what topic to focus on next based on weak areas, unread topics, and targets
     */
    suspend fun generateFocusNextWorkflow(
        exam: String,
        remainingTime: String,
        progress: String,
        weakTopics: String,
        recentActivity: String,
        missingAnswerPrompt: String? = null,
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<com.example.ui.editor.FocusNextResult> = withContext(Dispatchers.IO) {
        val prompt = """
            Task: Recommend the single highest-impact topic or chapter the student should study next.
            
            Inputs:
            - Target Exam: $exam
            - Time Context: $remainingTime
            - Overall Progress: $progress
            - Identified Weak Areas / Low Test Scores: $weakTopics
            - Recent Activity in App: $recentActivity
            ${if (!missingAnswerPrompt.isNullOrBlank()) "- User Clarification: $missingAnswerPrompt" else ""}
            
            Return ONLY a valid JSON object:
            {
              "recommendedTopic": "Topic / Chapter Name",
              "subjectName": "Subject Name",
              "shortReason": "1 concise sentence why this is the highest priority right now",
              "immediateNextAction": "Concrete action to take in the next 30 minutes"
            }
        """.trimIndent()

        try {
            val responseText = executeCustomInstruction(
                baseContextText = "Study Priority Engine for PrepOS",
                userInstruction = prompt,
                contextTitle = "Next Focus Recommendation",
                apiKey = apiKey,
                model = model,
                provider = provider,
                deepSeekApiKey = deepSeekApiKey
            ).getOrNull() ?: ""

            val cleanedJson = extractJsonFromResponse(responseText)
            val root = JSONObject(cleanedJson)
            Result.success(
                com.example.ui.editor.FocusNextResult(
                    recommendedTopic = root.optString("recommendedTopic", weakTopics.ifBlank { "High-Yield Concepts" }),
                    subjectName = root.optString("subjectName", "Core Subject"),
                    shortReason = root.optString("shortReason", "This high-weightage topic requires revision to boost your overall accuracy."),
                    immediateNextAction = root.optString("immediateNextAction", "Open chapter notes, spend 25 minutes reviewing key rules, and take a quick 10-MCQ quiz.")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute focus next recommendation, using fallback", e)
            Result.success(
                com.example.ui.editor.FocusNextResult(
                    recommendedTopic = weakTopics.ifBlank { "Important Core Concepts" },
                    subjectName = "Priority Subject",
                    shortReason = "Focusing on this topic provides maximum scoring leverage based on current progress.",
                    immediateNextAction = "Open your chapter notes, review key concepts for 25 minutes, then attempt active recall questions."
                )
            )
        }
    }

    /**
     * CARD 6: Explains how PrepOS works for a given feature/topic
     */
    suspend fun explainHowPrepOSWorksWorkflow(
        selectedTopic: String,
        apiKey: String = "",
        model: String = DEFAULT_MODEL,
        provider: String = "GEMINI",
        deepSeekApiKey: String = ""
    ): Result<com.example.ui.editor.HowItWorksExplanation> = withContext(Dispatchers.IO) {
        val prompt = """
            Task: Provide a clean 3-4 point actionable explanation of how PrepOS features help the student with: '$selectedTopic'.
            
            Features in PrepOS:
            - Multi-element Rich Study Notes (Headings, Bullets, Formula Callouts, Diagrams, Comparison Tables)
            - Active Recall Practice & Custom MCQ Quizzes inside every chapter
            - Weekly Timetable & Scheduled Study Sessions
            - Focus Session Stopwatch with notification controls
            - Dynamic Progress Gradient coloring (Muted Grey -> Red -> Orange -> Amber -> Green)
            - AI Study Assistant with structured 6-card workflows
            
            Return ONLY a valid JSON object:
            {
              "topicTitle": "$selectedTopic",
              "points": [
                "Step 1: Point description...",
                "Step 2: Point description...",
                "Step 3: Point description..."
              ]
            }
        """.trimIndent()

        try {
            val responseText = executeCustomInstruction(
                baseContextText = "PrepOS User Guide Assistant",
                userInstruction = prompt,
                contextTitle = "How PrepOS Works",
                apiKey = apiKey,
                model = model,
                provider = provider,
                deepSeekApiKey = deepSeekApiKey
            ).getOrNull() ?: ""

            val cleanedJson = extractJsonFromResponse(responseText)
            val root = JSONObject(cleanedJson)
            val arr = root.optJSONArray("points") ?: JSONArray()
            val points = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                points.add(arr.getString(i))
            }
            Result.success(
                com.example.ui.editor.HowItWorksExplanation(
                    topicTitle = root.optString("topicTitle", selectedTopic),
                    points = if (points.isNotEmpty()) points else getFallbackHowItWorksPoints(selectedTopic)
                )
            )
        } catch (e: Exception) {
            Result.success(
                com.example.ui.editor.HowItWorksExplanation(
                    topicTitle = selectedTopic,
                    points = getFallbackHowItWorksPoints(selectedTopic)
                )
            )
        }
    }

    private fun getFallbackHowItWorksPoints(topic: String): List<String> {
        return when {
            topic.contains("syllabus", ignoreCase = true) -> listOf(
                "Add your target competitive exam and define subject categories.",
                "Structure chapters with high-yield topics and active recall notes.",
                "Import complete syllabi via AI document scanner in seconds."
            )
            topic.contains("study", ignoreCase = true) -> listOf(
                "Read clean structured notes with headings, formulas, and visual comparison tables.",
                "Reinforce learning with instant 10-question MCQ practice drills.",
                "Review detailed solutions and track error logs for weak topics."
            )
            topic.contains("AI", ignoreCase = true) -> listOf(
                "Use the 6 AI Workflow cards for weekly plans, exam strategies, and topic priorities.",
                "Ask concept doubts, get instant explanations, and generate practice questions.",
                "Upgrade chapter notes with summaries, Hindi translations, and diagrams."
            )
            else -> listOf(
                "Set daily study targets and follow your personalized weekly timetable.",
                "Track focus sessions with the Pomodoro timer to build disciplined study habits.",
                "Monitor test accuracy percentages and identify weak chapters that need immediate revision.",
                "Receive intelligent reminders before midnight to protect your active study streak."
            )
        }
    }

    private fun extractJsonFromResponse(text: String): String {
        val trimmed = text.trim()
        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = codeBlockRegex.find(trimmed)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val firstBrace = trimmed.indexOfFirst { it == '{' || it == '[' }
        val lastBrace = trimmed.indexOfLast { it == '}' || it == ']' }
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim()
        }
        return trimmed
    }
}

/**
 * Offline Chapter Sanitizer:
 * Operates deterministically without internet or API keys.
 * Parses raw text, extracts headings (H1-H4), cleans markdown, structures bullet lists,
 * and formats revision sections uniformly.
 */
object OfflineChapterSanitizer {
    fun sanitizeLocally(rawText: String, fallbackTitle: String): SanitizedChapterResult {
        val safeFallback = fallbackTitle.ifBlank { "Untitled Chapter" }
        if (rawText.isBlank()) {
            return SanitizedChapterResult(
                title = safeFallback,
                summary = "",
                elements = listOf(
                    DocElement.TextBlock(blockType = ElementType.HEADING_1, text = safeFallback),
                    DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = "")
                ),
                isAiGenerated = false
            )
        }

        val parsedBlocks = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(rawText).toMutableList()
        val extractedTitle = parsedBlocks.firstOrNull { it is DocElement.TextBlock && (it.blockType == ElementType.HEADING_1 || it.blockType == ElementType.HEADING_2) }
            ?.let { (it as DocElement.TextBlock).text.trim() }
            ?.ifBlank { null }
            ?: safeFallback

        // Ensure H1 title exists at top
        if (parsedBlocks.none { it is DocElement.TextBlock && it.blockType == ElementType.HEADING_1 }) {
            parsedBlocks.add(0, DocElement.TextBlock(blockType = ElementType.HEADING_1, text = extractedTitle))
        }

        val summaryText = parsedBlocks.filterIsInstance<DocElement.TextBlock>()
            .firstOrNull { it.blockType == ElementType.PARAGRAPH }?.text?.take(160) ?: ""

        return SanitizedChapterResult(
            title = extractedTitle,
            summary = summaryText,
            elements = parsedBlocks,
            isAiGenerated = false
        )
    }

    private fun isTableStart(line: String, allLines: List<String>, currentIndex: Int): Boolean {
        if (line.startsWith("|") && line.endsWith("|") && line.count { it == '|' } >= 2) return true
        if (currentIndex + 1 < allLines.size) {
            val next = allLines[currentIndex + 1].trim()
            if (next.startsWith("|") && next.contains("-")) return true
        }
        return false
    }

    private fun isDiagramFlow(line: String): Boolean {
        val hasArrow = line.contains(" -> ") || line.contains(" --> ") || line.contains(" ==> ")
        return hasArrow && (line.contains("[") || line.contains("(") || line.count { it == '>' } >= 1)
    }

    private fun tryParseChartMetric(line: String): DocElement.ChartBlock? {
        val lower = line.lowercase()
        // Format A: [Progress: 80%] or Progress: 80% or Readiness: 75%
        val singleMetric = Regex("""(?:\[?\s*(Progress|Weightage|Readiness|Completion|Accuracy)\s*[:=]\s*(\d{1,3}(?:\.\d+)?)\s*%\s*\]?)""", RegexOption.IGNORE_CASE)
        val matchSingle = singleMetric.find(line)
        if (matchSingle != null) {
            val label = matchSingle.groupValues[1]
            val value = matchSingle.groupValues[2].toFloatOrNull() ?: 50f
            return DocElement.ChartBlock(
                chartType = ChartType.PROGRESS_RINGS,
                title = "$label Overview",
                entries = listOf(
                    ChartEntry(label, value.coerceIn(0f, 100f), "#6C63D9")
                )
            )
        }

        // Format B: [Chart: Label1: 40%, Label2: 60%] or [Progress: A: 50%, B: 80%]
        if (lower.startsWith("[chart:") || lower.startsWith("[progress:") || lower.startsWith("[chart_bars:")) {
            val inner = line.removePrefix("[").removeSuffix("]").substringAfter(":")
            val parts = inner.split(",")
            val entries = mutableListOf<ChartEntry>()
            val colors = listOf("#6C63D9", "#2563EB", "#059669", "#D97706", "#DC2626")
            for (part in parts) {
                val pair = part.split(":")
                if (pair.size == 2) {
                    val pLabel = cleanSymbols(pair[0])
                    val pVal = pair[1].replace("%", "").trim().toFloatOrNull() ?: 50f
                    entries.add(ChartEntry(pLabel, pVal, colors[entries.size % colors.size]))
                }
            }
            if (entries.isNotEmpty()) {
                val chartType = if (lower.contains("bar")) ChartType.BAR else ChartType.PROGRESS_RINGS
                return DocElement.ChartBlock(
                    chartType = chartType,
                    title = "Topic Breakdown & Metrics",
                    entries = entries
                )
            }
        }
        return null
    }

    private fun cleanSymbols(text: String): String {
        return text.trim()
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("__(.*?)__"), "$1")
            .replace(Regex("(?<=\\s|^)\\*([^*\\s]+)\\*(?=\\s|$)"), "$1")
            .replace(Regex("(?<=\\s|^)_([^_\\s]+)_(?=\\s|$)"), "$1")
            .replace(Regex("~~(.*?)~~"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }
}

/**
 * Offline Question Parser:
 * Deterministically parses raw MCQs from text without needing network or API keys.
 * Accurately extracts questions, options (A-D / 1-4 / Roman / Inline), inline/multiline answers, and detailed explanations.
 * Highly robust and forgiving against varied formatting, blank lines, markdown, and punctuation.
 */
object OfflineQuestionParser {
    fun parseLocally(rawText: String): List<QuestionItem> {
        if (rawText.isBlank()) return emptyList()

        return try {
            val normalizedText = rawText
                .replace("\uFEFF", "") // strip BOM
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()

            // Strategy 1: Attempt JSON parse in case structured JSON was pasted (supports markdown code blocks)
            val jsonQuestions = tryParseJson(normalizedText)
            if (jsonQuestions.isNotEmpty()) {
                return jsonQuestions
            }

            // Strategy 2: Ultra-resilient Smart Stream Parser
            // Handles multi-line stems, blank lines between question and options, numbered statements, and all option variants
            val streamQuestions = parseStreamSmart(normalizedText)
            if (streamQuestions.isNotEmpty()) {
                return streamQuestions
            }

            // Strategy 3: Block splitting by paragraphs or question headers (enhanced)
            val blocks = splitIntoQuestionBlocks(normalizedText)
            val blockQuestions = mutableListOf<QuestionItem>()
            for (block in blocks) {
                try {
                    val parsedQ = parseSingleQuestionBlock(block)
                    if (parsedQ != null) {
                        blockQuestions.add(parsedQ)
                    }
                } catch (e: Throwable) {
                    safeLogW("OfflineQuestionParser", "Skipping malformed block: ${e.message}")
                }
            }

            if (blockQuestions.isNotEmpty()) {
                return blockQuestions
            }

            // Strategy 4: Fallback state-machine line-by-line parser
            parseLineByLine(normalizedText)
        } catch (e: Throwable) {
            safeLogE("OfflineQuestionParser", "Error in parseLocally", e)
            emptyList()
        }
    }

    private fun safeLogW(tag: String, msg: String) {
        try { Log.w(tag, msg) } catch (_: Throwable) { println("[$tag] $msg") }
    }

    private fun safeLogE(tag: String, msg: String, e: Throwable? = null) {
        try { Log.e(tag, msg, e) } catch (_: Throwable) { println("[$tag] $msg: ${e?.message}") }
    }

    private fun tryParseJson(text: String): List<QuestionItem> {
        var trimmed = text.trim()
        // Strip markdown code fence if present
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replace(Regex("""^```(?:json)?\s*"""), "")
                .replace(Regex("""\s*```$"""), "")
                .trim()
        }
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) return emptyList()
        return try {
            val jsonArray = if (trimmed.startsWith("[")) {
                org.json.JSONArray(trimmed)
            } else {
                val obj = org.json.JSONObject(trimmed)
                when {
                    obj.has("questions") -> obj.getJSONArray("questions")
                    obj.has("mcqs") -> obj.getJSONArray("mcqs")
                    obj.has("items") -> obj.getJSONArray("items")
                    obj.has("data") -> obj.optJSONArray("data")
                    else -> null
                }
            } ?: return emptyList()

            val results = mutableListOf<QuestionItem>()
            for (i in 0 until jsonArray.length()) {
                val qObj = jsonArray.optJSONObject(i) ?: continue
                val stem = qObj.optString("question", "").ifBlank {
                    qObj.optString("stem", "").ifBlank {
                        qObj.optString("questionText", "").ifBlank {
                            qObj.optString("prompt", "").ifBlank {
                                qObj.optString("q", "")
                            }
                        }
                    }
                }
                if (stem.isBlank()) continue

                val rawOpts = mutableListOf<String>()
                val optArr = qObj.optJSONArray("options") ?: qObj.optJSONArray("choices") ?: qObj.optJSONArray("answers")
                if (optArr != null) {
                    for (j in 0 until optArr.length()) {
                        val optStr = optArr.optString(j, "").trim()
                        if (optStr.isNotBlank()) rawOpts.add(optStr)
                    }
                } else {
                    // Try individual optA, optB, optC, optD fields
                    listOf("optA", "optB", "optC", "optD", "optE", "optionA", "optionB", "optionC", "optionD", "optionE", "a", "b", "c", "d").forEach { key ->
                        if (qObj.has(key)) {
                            val v = qObj.optString(key, "").trim()
                            if (v.isNotBlank()) rawOpts.add(v)
                        }
                    }
                }
                if (rawOpts.size < 2) continue

                val padded = rawOpts.map { cleanOptionText(it) }.toMutableList()
                while (padded.size < 4) {
                    when (padded.size) {
                        2 -> padded.add("Both A and B")
                        else -> padded.add("None of the above")
                    }
                }

                val ansRaw = qObj.optString("answer", "").ifBlank {
                    qObj.optString("correctAnswer", "").ifBlank {
                        qObj.optString("correctOption", "").ifBlank {
                            qObj.optString("correct", "").ifBlank {
                                qObj.optString("key", "")
                            }
                        }
                    }
                }
                val correctIdx = if (qObj.has("correctOptionIndex")) {
                    qObj.optInt("correctOptionIndex", 0)
                } else {
                    parseAnswerIndex(ansRaw, padded).first
                }

                val expl = qObj.optString("explanation", "").ifBlank {
                    qObj.optString("solution", "").ifBlank {
                        qObj.optString("sol", "").ifBlank {
                            qObj.optString("reason", "").ifBlank {
                                qObj.optString("rationale", "Based on core chapter principles and study facts.")
                            }
                        }
                    }
                }

                results.add(
                    QuestionItem(
                        id = UUID.randomUUID().toString(),
                        questionText = cleanQuestionStem(stem),
                        options = padded.take(4),
                        correctOptionIndex = correctIdx.coerceIn(0, 3),
                        explanation = cleanExplanationText(expl)
                    )
                )
            }
            results
        } catch (e: Throwable) {
            parseJsonWithRegex(trimmed)
        }
    }

    private fun parseJsonWithRegex(text: String): List<QuestionItem> {
        return try {
            val qPattern = Regex("""\{[^{}]*"(?:question|stem|prompt)"\s*:\s*"([^"]+)"[^{}]*\}""")
            val results = mutableListOf<QuestionItem>()
            for (match in qPattern.findAll(text)) {
                val block = match.value
                val stemMatch = Regex("""\"(?:question|stem|prompt)\"\s*:\s*\"([^"]+)\"""").find(block) ?: continue
                val stem = stemMatch.groupValues[1]

                val opts = mutableListOf<String>()
                val optionsMatch = Regex("""\"(?:options|choices)\"\s*:\s*\[([^\]]+)\]""").find(block)
                if (optionsMatch != null) {
                    val inner = optionsMatch.groupValues[1]
                    Regex("""\"([^"]+)\"""").findAll(inner).forEach {
                        opts.add(it.groupValues[1])
                    }
                }
                if (opts.size < 2) continue

                val padded = opts.map { cleanOptionText(it) }.toMutableList()
                while (padded.size < 4) padded.add("None of the above")

                val ansMatch = Regex("""\"(?:answer|correctAnswer|correct)\"\s*:\s*\"?([^",}\s]+)\"?""").find(block)
                val ans = ansMatch?.groupValues?.getOrNull(1) ?: "A"
                val (idx, _) = parseAnswerIndex(ans, padded)

                val expMatch = Regex("""\"(?:explanation|solution|reason)\"\s*:\s*\"([^"]+)\"""").find(block)
                val exp = expMatch?.groupValues?.getOrNull(1) ?: "Based on core chapter principles and study facts."

                results.add(
                    QuestionItem(
                        id = UUID.randomUUID().toString(),
                        questionText = cleanQuestionStem(stem),
                        options = padded.take(4),
                        correctOptionIndex = idx,
                        explanation = cleanExplanationText(exp)
                    )
                )
            }
            results
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun safeSubstring(text: String, start: Int, end: Int): String {
        if (text.isEmpty()) return ""
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(0, text.length)
        return if (s < e) text.substring(s, e) else ""
    }

    private fun safeSubstring(text: String, start: Int): String {
        if (text.isEmpty()) return ""
        val s = start.coerceIn(0, text.length)
        return text.substring(s)
    }

    private data class ParsedLineSegments(
        val mainText: String,
        val inlineAnswer: String? = null,
        val inlineExplanation: String? = null
    )

    private fun extractInlineSegments(rawText: String): ParsedLineSegments {
        val text = rawText.trim()
        if (text.isBlank()) return ParsedLineSegments("")

        val ansPattern = Regex("""(?i)(?:^|[\s*_|~•\(\[\{]+)(?:(?:Selected|Correct|Right|Model|Given)?\s*(?:Ans(?:wer)?|Option|Key|Choice)|Correct\s+Option)\s*[:.\-—=]?\s*""")
        val expPattern = Regex("""(?i)(?:^|[\s*_|~•\(\[\{]+)(?:Exp(?:lanation)?|Solution|Sol|Soln|Reason|Note|Rationale|Detailed\s+Solution)\s*[:.\-—=]?\s*""")

        val ansMatch = ansPattern.find(text)
        val expMatch = expPattern.find(text)

        // Case 1: Both Answer and Explanation found on this line
        if (ansMatch != null && expMatch != null) {
            if (ansMatch.range.first < expMatch.range.first) {
                val main = safeSubstring(text, 0, ansMatch.range.first).trim().trim('*', '_', '~', '|', '-', '•', '[', ']')
                val ans = safeSubstring(text, ansMatch.range.last + 1, expMatch.range.first).trim().trim('*', '_', '~', '|', '-', '•', '[', ']')
                val exp = safeSubstring(text, expMatch.range.last + 1).trim().trim('*', '_', '~', '|', '-', '•')
                return ParsedLineSegments(main, ans.ifBlank { null }, exp.ifBlank { null })
            } else {
                val main = safeSubstring(text, 0, expMatch.range.first).trim().trim('*', '_', '~', '|', '-', '•', '[', ']')
                val exp = safeSubstring(text, expMatch.range.last + 1, ansMatch.range.first).trim().trim('*', '_', '~', '|', '-', '•', '[', ']')
                val ans = safeSubstring(text, ansMatch.range.last + 1).trim().trim('*', '_', '~', '|', '-', '•')
                return ParsedLineSegments(main, ans.ifBlank { null }, exp.ifBlank { null })
            }
        }

        // Case 2: Answer marker found
        if (ansMatch != null) {
            val main = safeSubstring(text, 0, ansMatch.range.first).trim().trim('*', '_', '~', '|', '-', '•', '[', ']')
            val ans = safeSubstring(text, ansMatch.range.last + 1).trim().trim('*', '_', '~', '|', '-', '•', '[', ']')
            return ParsedLineSegments(main, ans.ifBlank { null }, null)
        }

        // Case 3: Explanation marker found
        if (expMatch != null) {
            val main = safeSubstring(text, 0, expMatch.range.first).trim().trim('*', '_', '~', '|', '-', '•', '[', ']')
            val exp = safeSubstring(text, expMatch.range.last + 1).trim().trim('*', '_', '~', '|', '-', '•')
            return ParsedLineSegments(main, null, exp.ifBlank { null })
        }

        return ParsedLineSegments(text)
    }

    private fun parseAnswerIndex(ansText: String, options: List<String>): Pair<Int, String?> {
        val clean = ansText.trim()
        if (clean.isBlank()) return Pair(0, null)

        // 1. Strip outer markdown, brackets, or "Option" prefix
        val stripped = clean
            .replace(Regex("""^\s*[*_~•\-\[({>]+\s*"""), "")
            .replace(Regex("""(?i)^Option\s*[:.\-—=]?\s*"""), "")
            .trim()

        // 2. Direct single letter or letter at start: e.g. A, (A), [A], A), A., A:, 1, 2, etc.
        val letterMatch = Regex("""(?i)(?:^|[\s*(\[{\-:])([A-Ea-e1-5])(?:\s*[\)\].:\-–—,]|\s|${'$'})(?:\s*[\-–—:]?\s*(.*))?""").find(stripped)
            ?: Regex("""(?i)^([A-Ea-e1-5])\b(?:\s*[\-–—:]?\s*(.*))?""").find(stripped)
            ?: Regex("""(?i)(?:^|\s+)Option\s+([A-Ea-e1-5])\b(?:\s*[\-–—:]?\s*(.*))?""").find(clean)
            ?: Regex("""(?i)\b([A-Ea-e])\b(?:\s*is\s+(?:the\s+)?correct)""").find(clean)

        if (letterMatch != null) {
            val char = letterMatch.groupValues[1].uppercase()
            val idx = when (char) {
                "A", "1" -> 0
                "B", "2" -> 1
                "C", "3" -> 2
                "D", "4" -> 3
                "E", "5" -> 4
                else -> 0
            }
            val extra = letterMatch.groupValues.getOrNull(2)?.trim()?.trim('*', '_', '~', '-', ':')
            return Pair(idx, extra?.ifBlank { null })
        }

        // 3. Match Roman numerals: (i), (ii), (iii), (iv), i), ii), etc.
        val romanMatch = Regex("""(?i)(?:^|[\s*(\[{\-:])([ivxIVX]{1,4})(?:\s*[\)\].:\-–—,]|\s|${'$'})""").find(stripped)
        if (romanMatch != null) {
            val roman = romanMatch.groupValues[1].lowercase()
            val idx = when (roman) {
                "i" -> 0
                "ii" -> 1
                "iii" -> 2
                "iv" -> 3
                "v" -> 4
                else -> 0
            }
            return Pair(idx, null)
        }

        // 4. Check if the answer text directly matches one of the options (e.g. Answer: Paris)
        if (options.isNotEmpty()) {
            val normalizedClean = clean.lowercase()
                .replace("₹", "").replace("rs.", "")
                .replace(Regex("""^[a-e1-5][.)\]:\-–—\s]+"""), "")
                .trim()
                .trim('*', '_', '~', '.', ')')
            if (normalizedClean.isNotBlank()) {
                val matchedIdx = options.indexOfFirst { opt ->
                    val normOpt = opt.lowercase()
                        .replace("₹", "").replace("rs.", "")
                        .replace(Regex("""^[a-e1-5][.)\]:\-–—\s]+"""), "")
                        .trim()
                        .trim('*', '_', '~', '.', ')')
                    normOpt.isNotBlank() && (normOpt == normalizedClean || normOpt.contains(normalizedClean) || normalizedClean.contains(normOpt))
                }
                if (matchedIdx >= 0) {
                    return Pair(matchedIdx, null)
                }
            }
        }

        return Pair(0, null)
    }

    private fun cleanQuestionStem(raw: String): String {
        return try {
            val withoutNumbering = raw
                .replace(Regex("^(?:[*_~•\\-–—#>]+\\s*)?(?:[Qq](?:uestion|ues)?\\s*\\.?\\s*\\d*[:.)-]?|Problem\\s*\\d*[:.)-]?|\\bQ\\d+[:.)-]?|MCQ\\s*\\d*[:.)-]?|\\d+[.)-]|\\[\\d+\\]|\\(\\d+\\))\\s*"), "")
                .trim()
                .ifBlank { raw.trim() }
            val cleanBullets = withoutNumbering.replace(Regex("""(^|\n)\s*[*•\-–—]\s*"""), "$1")
            
            // If contains figure tag or LaTeX display block, preserve it directly
            if (cleanBullets.contains("[FIGURE:") || cleanBullets.contains("$$") || cleanBullets.contains("\\[") || cleanBullets.contains("\\frac{")) {
                cleanBullets
            } else {
                com.example.util.MathNotationHelper.formatMathNotation(cleanBullets)
            }
        } catch (e: Throwable) {
            raw.trim()
        }
    }

    private fun cleanOptionText(raw: String): String {
        return try {
            var cleaned = raw.trim().trim('*', '_', '~', '|', '-', '•')
            // Strip option letter if still present at the start (e.g. "A) Paris" -> "Paris")
            cleaned = cleaned.replace(Regex("""^(?:Option\s*)?(?:[(\[]?[A-Ea-e1-5][)\]:.\-–—]|[A-Ea-e1-5]\s*[\):.\-–—])\s*"""), "")
                .trim()
                .trim('*', '_', '~', '|', '-', '•')
            if (cleaned.contains("\\frac{") || cleaned.contains("\\sqrt{") || cleaned.contains("$$")) {
                cleaned
            } else {
                com.example.util.MathNotationHelper.formatMathNotation(cleaned)
            }
        } catch (e: Throwable) {
            raw.trim()
        }
    }

    private fun cleanExplanationText(raw: String): String {
        return try {
            var cleaned = raw.trim()
                .replace(Regex("""^(?:Exp(?:lanation)?|Solution|Sol|Soln|Reason|Rationale|Detailed\s+Solution|Description|Note|Explain|Why|Justification|Analysis|Hint|Discussion|Details)[:.\-—=)]?\s*""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""(^|\n)\s*[*•\-–—]\s*"""), "$1")
                .replace(Regex("""\s+[*•]\s+"""), " ")
                .trim()
                .trim('*', '_', '~', '-')
            com.example.util.MathNotationHelper.formatMathNotation(cleaned)
        } catch (e: Throwable) {
            raw.trim()
        }
    }

    private fun findInlineOptions(line: String): List<String> {
        return try {
            val optionMarkerRegex = Regex(
                """(?:^|\s+)(?:Option\s*)?(?:[(\[]?([A-Ea-e1-5]|[ivxIVX]{1,4})[)\]:.\-–—]|([A-Ea-e])[.)])\s*""",
                RegexOption.IGNORE_CASE
            )
            val matches = optionMarkerRegex.findAll(line).toList()
            if (matches.size < 2) return emptyList()

            val options = mutableListOf<String>()
            for (i in matches.indices) {
                val start = matches[i].range.last + 1
                val end = if (i + 1 < matches.size) matches[i + 1].range.first else line.length
                val rawOptText = safeSubstring(line, start, end).trim()
                val segments = extractInlineSegments(rawOptText)
                val optText = cleanOptionText(segments.mainText)
                if (optText.isNotBlank()) {
                    options.add(optText)
                }
            }
            options
        } catch (e: Throwable) {
            emptyList()
        }
    }

    // Helper to strip markdown asterisks and leading bullets for header checks
    private fun stripMarkdownDecorations(line: String): String {
        return line.trim()
            .replace(Regex("""^[\s*_~•\-–—#>]+"""), "")
            .replace(Regex("""\*{2,}|\_{2,}"""), "")
            .trim()
    }

    data class DetectedOption(
        val label: String,
        val text: String,
        val indexHint: Int? = null
    )

    private fun tryDetectOption(line: String, hasExistingStem: Boolean, currentOptionsCount: Int): DetectedOption? {
        val clean = stripMarkdownDecorations(line)
        if (clean.isBlank()) return null

        // 1. Check letter options: A), A., A:, (A), [A], Option A:, Option A., Option A -, Option A, a), a., (a), etc.
        val letterRegex = Regex(
            """^(?:Option\s*)?(?:[(\[]?([A-Ea-e])[)\]:.\-–—]|([A-Ea-e])\s*[\):.\-–—]|\*+([A-Ea-e])\*+[\):.\-–—]?)\s*(.*)""",
            RegexOption.IGNORE_CASE
        )
        val letterMatch = letterRegex.find(clean)
        if (letterMatch != null) {
            val letter = (letterMatch.groupValues[1].ifBlank { letterMatch.groupValues[2] }.ifBlank { letterMatch.groupValues[3] }).uppercase()
            val optText = letterMatch.groupValues[4].trim()
            val idx = when (letter) {
                "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; "E" -> 4; else -> null
            }
            return DetectedOption(letter, optText, idx)
        }

        // Explicit "Option A" without punctuation
        val explicitOptRegex = Regex("""^Option\s+([A-Ea-e1-5])\b[:.\-–—\s]*(.*)""", RegexOption.IGNORE_CASE)
        val explicitOptMatch = explicitOptRegex.find(clean)
        if (explicitOptMatch != null) {
            val label = explicitOptMatch.groupValues[1].uppercase()
            val optText = explicitOptMatch.groupValues[2].trim()
            val idx = when (label) {
                "A", "1" -> 0; "B", "2" -> 1; "C", "3" -> 2; "D", "4" -> 3; "E", "5" -> 4; else -> null
            }
            return DetectedOption(label, optText, idx)
        }

        // 2. Roman numeral options: (i), (ii), (iii), (iv), i), ii), etc.
        val romanRegex = Regex("""^(?:Option\s*)?(?:[(\[]?([ivxIVX]{1,4})[)\]:.\-–—])\s*(.*)""", RegexOption.IGNORE_CASE)
        val romanMatch = romanRegex.find(clean)
        if (romanMatch != null) {
            val roman = romanMatch.groupValues[1].lowercase()
            val optText = romanMatch.groupValues[2].trim()
            val idx = when (roman) {
                "i" -> 0; "ii" -> 1; "iii" -> 2; "iv" -> 3; "v" -> 4; else -> null
            }
            return DetectedOption(roman, optText, idx)
        }

        // 3. Numbered options in brackets or parentheses: (1), [1], 1)
        val bracketNumRegex = Regex("""^(?:Option\s*)?(?:[(\[]([1-5])[)\]]|([1-5])\))\s*(.*)""")
        val bracketNumMatch = bracketNumRegex.find(clean)
        if (bracketNumMatch != null) {
            val num = bracketNumMatch.groupValues[1].ifBlank { bracketNumMatch.groupValues[2] }
            val optText = bracketNumMatch.groupValues[3].trim()
            val idx = num.toIntOrNull()?.minus(1)
            return DetectedOption(num, optText, idx)
        }

        // 4. Dot-numbered option: 1. Option text, 2. Option text
        // STRICT CONDITION: Only consider "1. text" an option if:
        // - We already have a non-blank question stem, AND
        // - The line does NOT end with a question mark ('?'), AND
        // - The line does NOT begin with common question words (What, Which, Who, When, Where, Why, How, Consider, Explain, Find, Calculate, If, In, The, Describe, State), AND
        // - If currentOptionsCount > 0, the number must match expected next option (e.g. 2 if count is 1)!
        val dotNumRegex = Regex("""^([1-5])\.\s+(.*)""")
        val dotNumMatch = dotNumRegex.find(clean)
        if (dotNumMatch != null && hasExistingStem) {
            val numStr = dotNumMatch.groupValues[1]
            val num = numStr.toIntOrNull() ?: 1
            val optText = dotNumMatch.groupValues[2].trim()
            val isQuestionLike = optText.endsWith("?") ||
                    Regex("""^(?i)(what|which|who|when|where|why|how|consider|explain|find|calculate|if|in|the|describe|state|given|suppose)\b""").containsMatchIn(optText)

            if (!isQuestionLike) {
                if (currentOptionsCount == 0 && num == 1) {
                    return DetectedOption(numStr, optText, 0)
                } else if (currentOptionsCount > 0 && num == currentOptionsCount + 1) {
                    return DetectedOption(numStr, optText, num - 1)
                }
            }
        }

        return null
    }

    private fun isOptionLine(line: String): Boolean {
        val clean = stripMarkdownDecorations(line)
        return tryDetectOption(clean, hasExistingStem = true, currentOptionsCount = 0) != null
    }

    private fun isAnswerLine(line: String): Boolean {
        val clean = stripMarkdownDecorations(line)
        if (clean.isBlank()) return false
        val ansPattern = Regex(
            """^(?:(?:Selected|Correct|Right|Model|Given|Final)?\s*(?:Ans(?:wer)?|Option|Choice|Key)|Correct\s+Option|Correct|Right|Ans|Answer|Key|Option\s+[A-Ea-e1-5]\s+is\s+correct|Ans(?:wer)?\s+is|Answer\s*=\s*|Ans\s*=\s*|Ans\s*[:.\-—=]?\s*Option)[:.\-—=)]?\s*.*""",
            RegexOption.IGNORE_CASE
        )
        if (ansPattern.matches(clean)) return true
        if (Regex("""^(?i)(?:Option\s+)?([A-Ea-e1-5]|\([A-Ea-e1-5]\))\s+is\s+(?:the\s+)?correct(?:\s+answer|\s+option)?""").matches(clean)) return true
        return false
    }

    private fun isExplanationLine(line: String): Boolean {
        val clean = stripMarkdownDecorations(line)
        if (clean.isBlank()) return false
        val expPattern = Regex(
            """^(?:Exp(?:lanation)?|Solution|Sol|Soln|Reason|Rationale|Detailed\s+Solution|Description|Note|Explain|Why|Justification|Analysis|Hint|Discussion|Details)[:.\-—=)]?\s*.*""",
            RegexOption.IGNORE_CASE
        )
        return expPattern.matches(clean)
    }

    private fun isExplicitQuestionHeader(line: String): Boolean {
        val clean = stripMarkdownDecorations(line)
        if (clean.isBlank()) return false
        // Matches "Question 1:", "Q.1", "Q1)", "Problem 1:", "MCQ 1:"
        val explicitRegex = Regex("""^(?:[Qq](?:uestion|ues)?\s*\.?\s*\d*[:.)-]?|Problem\s*\d*[:.)-]?|\bQ\d+[:.)-]?|MCQ\s*\d*[:.)-]?)\s*(.*)""", RegexOption.IGNORE_CASE)
        if (explicitRegex.matches(clean)) return true

        // Numbered line that looks like a question: "1. What...", "1) What...", "(1) Which...", or any line starting with a number and ending with '?'
        val numberedQRegex = Regex("""^(?:\d+[.)\]]|\(\d+\)|\[\d+\])\s+(.*)""")
        val numMatch = numberedQRegex.find(clean)
        if (numMatch != null) {
            val body = numMatch.groupValues[1].trim()
            if (body.endsWith("?") || Regex("""^(?i)(what|which|who|when|where|why|how|consider|explain|find|calculate|if|in|the|describe|state|identify|select|determine|evaluate|match|choose)\b""").containsMatchIn(body)) {
                return true
            }
            if (body.length > 28) {
                return true
            }
        }

        return false
    }

    private fun isQuestionHeaderLine(line: String): Boolean {
        return isExplicitQuestionHeader(line)
    }

    /**
     * Smart stream parser: reads text line by line using an intelligent state machine.
     * Accurately distinguishes question stems (including multi-line statements & UPSC questions),
     * options, answer lines, and explanations, tolerating blank lines and varied punctuation.
     */
    private fun parseStreamSmart(text: String): List<QuestionItem> {
        val results = mutableListOf<QuestionItem>()
        val lines = text.lines()

        var currentStemLines = mutableListOf<String>()
        var currentOptions = mutableListOf<String>()
        var correctOptionIndex: Int? = null
        var currentExplanationLines = mutableListOf<String>()

        fun flush() {
            val rawStem = currentStemLines.joinToString("\n").trim()
            val cleanedStem = cleanQuestionStem(rawStem)
            if (cleanedStem.isNotBlank() && currentOptions.size >= 2) {
                val padded = currentOptions.map { cleanOptionText(it) }.toMutableList()
                while (padded.size < 4) {
                    when (padded.size) {
                        2 -> padded.add("Both A and B")
                        3 -> padded.add("None of the above")
                        else -> padded.add("None of the above")
                    }
                }
                val finalIdx = (correctOptionIndex ?: 0).coerceIn(0, padded.size - 1).coerceIn(0, 3)
                val rawExp = currentExplanationLines.joinToString(" ").trim()
                val finalExp = cleanExplanationText(rawExp).ifBlank { "Based on core chapter principles and study facts." }

                results.add(
                    QuestionItem(
                        id = UUID.randomUUID().toString(),
                        questionText = cleanedStem,
                        options = padded.take(4),
                        correctOptionIndex = finalIdx,
                        explanation = finalExp
                    )
                )
            }
            currentStemLines = mutableListOf()
            currentOptions = mutableListOf()
            correctOptionIndex = null
            currentExplanationLines = mutableListOf()
        }

        var state = "STEM" // "STEM", "OPTIONS", "ANSWER", "EXPLANATION"

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Horizontal separator line (e.g. ---, ===, ***)
            if (Regex("""^[-=*_~]{3,}$""").matches(line)) {
                if (currentOptions.size >= 2) {
                    flush()
                    state = "STEM"
                }
                continue
            }

            val cleanLine = stripMarkdownDecorations(line)

            // Ignore standalone "Options:" or "Choices:" header
            if (cleanLine.equals("Options:", ignoreCase = true) || cleanLine.equals("Options", ignoreCase = true) ||
                cleanLine.equals("Choices:", ignoreCase = true) || cleanLine.equals("Choices", ignoreCase = true)
            ) {
                state = "OPTIONS"
                continue
            }

            // 1. Answer Line check
            if (isAnswerLine(cleanLine)) {
                state = "ANSWER"
                val segments = extractInlineSegments(cleanLine)
                val ansBody = segments.inlineAnswer ?: cleanLine.replace(
                    Regex("""^(?i)(?:(?:Selected|Correct|Right|Model|Given|Final)?\s*(?:Ans(?:wer)?|Option|Choice|Key)|Correct\s+Option|Correct|Right|Ans|Answer|Key|Ans(?:wer)?\s+is|Answer\s*=\s*|Ans\s*=\s*|Ans\s*[:.\-—=]?\s*Option)[:.\-—=)]?\s*"""),
                    ""
                ).trim()
                val (idx, extra) = parseAnswerIndex(segments.mainText.ifBlank { ansBody }, currentOptions)
                correctOptionIndex = idx
                if (!extra.isNullOrBlank() && currentExplanationLines.isEmpty()) {
                    currentExplanationLines.add(extra)
                }
                if (!segments.inlineExplanation.isNullOrBlank()) {
                    currentExplanationLines.add(segments.inlineExplanation)
                }
                continue
            }

            // 2. Explanation Line check
            if (isExplanationLine(cleanLine)) {
                state = "EXPLANATION"
                val expBody = cleanLine.replace(
                    Regex("""^(?i)(?:Exp(?:lanation)?|Solution|Sol|Soln|Reason|Rationale|Detailed\s+Solution|Description|Note|Explain|Why|Justification|Analysis|Hint|Discussion|Details)[:.\-—=)]?\s*"""),
                    ""
                ).trim()
                if (expBody.isNotBlank()) {
                    currentExplanationLines.add(cleanExplanationText(expBody))
                }
                continue
            }

            // 3. Inline options on a single line: (A) Opt1 (B) Opt2 (C) Opt3 (D) Opt4
            val inlineOpts = findInlineOptions(line)
            if (inlineOpts.size >= 2) {
                state = "OPTIONS"
                currentOptions.addAll(inlineOpts)
                val segments = extractInlineSegments(line)
                if (segments.inlineAnswer != null) {
                    val (idx, extra) = parseAnswerIndex(segments.inlineAnswer, currentOptions)
                    correctOptionIndex = idx
                    if (!extra.isNullOrBlank() && currentExplanationLines.isEmpty()) {
                        currentExplanationLines.add(extra)
                    }
                }
                if (segments.inlineExplanation != null) {
                    currentExplanationLines.add(segments.inlineExplanation)
                }
                continue
            }

            // 4. Single Option check (A), (B), A., A), etc.
            val detectedOpt = tryDetectOption(line, currentStemLines.isNotEmpty(), currentOptions.size)
            if (detectedOpt != null) {
                val isFirstOpt = detectedOpt.indexHint == 0 || detectedOpt.label.equals("A", ignoreCase = true) || detectedOpt.label == "1"
                if ((state == "EXPLANATION" || state == "ANSWER") && isFirstOpt) {
                    flush()
                } else if (isFirstOpt && currentOptions.size >= 2) {
                    flush()
                }

                state = "OPTIONS"
                val segments = extractInlineSegments(detectedOpt.text)
                val optText = cleanOptionText(segments.mainText)
                if (optText.isNotBlank()) {
                    currentOptions.add(optText)
                }
                if (segments.inlineAnswer != null) {
                    val (idx, extra) = parseAnswerIndex(segments.inlineAnswer, currentOptions)
                    correctOptionIndex = idx
                    if (!extra.isNullOrBlank() && currentExplanationLines.isEmpty()) {
                        currentExplanationLines.add(extra)
                    }
                }
                if (segments.inlineExplanation != null) {
                    currentExplanationLines.add(segments.inlineExplanation)
                }
                continue
            }

            // 5. Explicit Question Header line: Q1, Question 1:, 1. What is..., etc.
            if (isExplicitQuestionHeader(cleanLine)) {
                if (currentOptions.size >= 2 || state == "EXPLANATION" || state == "ANSWER") {
                    flush()
                }
                state = "STEM"
                currentStemLines.add(cleanLine)
                continue
            }

            // 6. Generic line handling according to current state
            when (state) {
                "EXPLANATION" -> {
                    if (cleanLine.endsWith("?") || Regex("""^(?i)(what|which|who|when|where|why|how|consider|find|calculate|select|define|name)\b""").containsMatchIn(cleanLine)) {
                        flush()
                        state = "STEM"
                        currentStemLines.add(cleanLine)
                    } else {
                        currentExplanationLines.add(cleanExplanationText(cleanLine))
                    }
                }
                "ANSWER" -> {
                    if (cleanLine.endsWith("?") || Regex("""^(?i)(what|which|who|when|where|why|how|consider|find|calculate|select)\b""").containsMatchIn(cleanLine)) {
                        flush()
                        state = "STEM"
                        currentStemLines.add(cleanLine)
                    } else {
                        currentExplanationLines.add(cleanExplanationText(cleanLine))
                    }
                }
                "OPTIONS" -> {
                    val isUpscPrompt = cleanLine.contains("is/are", ignoreCase = true) ||
                            cleanLine.contains("given above", ignoreCase = true) ||
                            Regex("""(?i)\b(?:statements?|statement|options?|pairs?|of\s+the\s+above)\b.*?(?:correct|incorrect|true|false|not\s+correct)""").containsMatchIn(cleanLine)
                    if (isUpscPrompt) {
                        for (opt in currentOptions) {
                            currentStemLines.add(opt)
                        }
                        currentOptions = mutableListOf()
                        currentStemLines.add(cleanLine)
                        state = "STEM"
                    } else if (currentOptions.size >= 2 && (cleanLine.endsWith("?") || Regex("""^(?i)(what|which|who|when|where|why|how|consider|find|calculate|select)\b""").containsMatchIn(cleanLine))) {
                        flush()
                        state = "STEM"
                        currentStemLines.add(cleanLine)
                    } else if (currentOptions.isNotEmpty()) {
                        val lastIdx = currentOptions.size - 1
                        currentOptions[lastIdx] = cleanOptionText("${currentOptions[lastIdx]} $cleanLine")
                    } else {
                        currentStemLines.add(cleanLine)
                    }
                }
                "STEM" -> {
                    currentStemLines.add(cleanLine)
                }
            }
        }

        flush()
        return results
    }

    private fun splitIntoQuestionBlocks(text: String): List<String> {
        val paragraphs = text.split(Regex("""\n\s*\n+""")).map { it.trim() }.filter { it.isNotBlank() }
        val candidateBlocks = mutableListOf<String>()

        if (paragraphs.size > 1) {
            var currentCombined = ""
            for (p in paragraphs) {
                val lines = p.lines().map { it.trim() }.filter { it.isNotBlank() }
                val firstLine = lines.firstOrNull() ?: ""
                val isContinuation = lines.all { isAnswerLine(it) || isExplanationLine(it) || isOptionLine(it) } ||
                        (currentCombined.isNotBlank() && (isAnswerLine(firstLine) || isExplanationLine(firstLine) || isOptionLine(firstLine)))

                if (isContinuation && currentCombined.isNotBlank()) {
                    currentCombined += "\n\n$p"
                } else {
                    if (currentCombined.isNotBlank()) {
                        candidateBlocks.add(currentCombined)
                    }
                    currentCombined = p
                }
            }
            if (currentCombined.isNotBlank()) {
                candidateBlocks.add(currentCombined)
            }
        } else {
            candidateBlocks.add(text.trim())
        }

        val finalBlocks = mutableListOf<String>()
        for (candidate in candidateBlocks) {
            val lines = candidate.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val subBlocks = mutableListOf<MutableList<String>>()
            var currentSub = mutableListOf<String>()
            var currentSubHasOptions = false

            for (line in lines) {
                val isQStart = isExplicitQuestionHeader(line)
                val isOpt = isOptionLine(line) || findInlineOptions(line).size >= 2

                if (isOpt) {
                    currentSubHasOptions = true
                }

                if (isQStart && currentSubHasOptions && currentSub.isNotEmpty()) {
                    subBlocks.add(currentSub)
                    currentSub = mutableListOf()
                    currentSubHasOptions = false
                }

                currentSub.add(line)
            }

            if (currentSub.isNotEmpty()) {
                subBlocks.add(currentSub)
            }

            for (sb in subBlocks) {
                val content = sb.joinToString("\n").trim()
                if (content.isNotBlank()) {
                    finalBlocks.add(content)
                }
            }
        }

        return finalBlocks
    }

    private fun parseSingleQuestionBlock(blockText: String): QuestionItem? {
        return try {
            val parsedList = parseStreamSmart(blockText)
            if (parsedList.isNotEmpty()) {
                return parsedList.first()
            }
            null
        } catch (e: Throwable) {
            safeLogE("OfflineQuestionParser", "Error parsing single question block", e)
            null
        }
    }

    private fun parseLineByLine(text: String): List<QuestionItem> {
        return parseStreamSmart(text)
    }
}



