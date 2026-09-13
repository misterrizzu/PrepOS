package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.QuestionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object McqPosterGenerator {

    /**
     * Extracts dynamic topic name, video number tag, and formatted first caption line.
     * E.g. contextTitle "Video 02 Articles" -> firstLine "#video02 articles enn👇"
     */
    fun extractTopicAndVideoTags(contextTitle: String, questionText: String = ""): Pair<String, List<String>> {
        val cleanTitle = contextTitle.trim()

        // Check for video/lecture/part pattern: e.g. "Video 02", "Lecture 02", "Part 2", "V02", "02 - Articles"
        val videoMatch = Regex("""(?i)\b(?:video|lecture|part|class|v)\s*[-_.:#]?\s*(\d+)\b""").find(cleanTitle)
            ?: Regex("""^(\d{1,2})\s*[-_.:]""").find(cleanTitle)

        val videoTag = if (videoMatch != null) {
            val num = videoMatch.groupValues[1].padStart(2, '0')
            "#video$num"
        } else {
            ""
        }

        // Extract clean topic name
        val stripped = cleanTitle
            .replace(Regex("""(?i)\b(?:video|lecture|part|class|v)\s*[-_.:#]?\s*\d+\b"""), "")
            .replace(Regex("""^(\d{1,2})\s*[-_.:]"""), "")
            .replace(Regex("""[^a-zA-Z0-9\s]"""), " ")
            .trim()

        val topicWords = stripped.split(Regex("""\s+"""))
            .filter { it.length >= 2 && !it.equals("test", ignoreCase = true) && !it.equals("chapter", ignoreCase = true) && !it.equals("prepos", ignoreCase = true) && !it.equals("exam", ignoreCase = true) }

        val topicName = if (topicWords.isNotEmpty()) {
            topicWords.take(3).joinToString(" ") { it.lowercase() }
        } else {
            "daily mcq"
        }

        // First caption line format: e.g. "#video02 articles enn👇" or "articles enn👇"
        val firstLine = buildString {
            if (videoTag.isNotBlank()) append("$videoTag ")
            if (topicName.isNotBlank()) append("$topicName ")
            append("enn👇")
        }.trim()

        val additionalTags = mutableListOf<String>()
        if (videoTag.isNotBlank()) additionalTags.add(videoTag.removePrefix("#").lowercase())
        if (topicName.isNotBlank()) {
            val singleTag = topicName.replace(" ", "")
            if (singleTag.length in 3..25) additionalTags.add(singleTag)
        }

        return Pair(firstLine, additionalTags)
    }

    /**
     * Builds clean caption text for clipboard and share text containing answer, explanation, and rich 5-tag metadata.
     * Guaranteed un-spoiled question format, correct answer, explanation, and first line hashtag with enn👇.
     */
    fun buildClipboardCaption(
        item: QuestionItem,
        examName: String = "Exam Prep",
        chapterTitle: String = ""
    ): String {
        val optLabels = listOf("A", "B", "C", "D", "E")
        val correctLetter = optLabels.getOrElse(item.correctOptionIndex) { "A" }
        val correctText = item.options.getOrElse(item.correctOptionIndex) { "" }

        val effectiveTitle = if (chapterTitle.isNotBlank()) chapterTitle else examName
        val (firstLine, topicTags) = extractTopicAndVideoTags(effectiveTitle, item.questionText)

        return buildString {
            // 1. First line hashtag / topic tag with enn👇
            appendLine(firstLine)
            appendLine()

            // 2. Question Prompt
            appendLine("❓ Question:")
            appendLine(item.questionText.trim())
            appendLine()

            // 3. Options
            item.options.forEachIndexed { idx, opt ->
                val label = optLabels.getOrElse(idx) { "${idx + 1}" }
                appendLine("$label) $opt")
            }
            appendLine()

            // 4. Correct Answer
            appendLine("✅ Correct Answer: Option $correctLetter – $correctText")

            // 5. Explanation
            if (item.explanation.isNotBlank()) {
                appendLine()
                appendLine("💡 Explanation:")
                appendLine(item.explanation.trim())
            }

            // 6. App Intro & CTA
            appendLine()
            appendLine("🚀 PrepOS – Your Personal AI Study & MCQ Companion with Smart Notes, Flashcards & Daily Practice.")

            // 7. Guaranteed 5+ metadata tags
            val allTags = linkedSetOf("prepos", "exampreperation", "dailytest", "mcqchallenge", "examready")
            topicTags.forEach { allTags.add(it) }
            val cleanExamTag = examName.filter { it.isLetterOrDigit() }.lowercase()
            if (cleanExamTag.isNotBlank() && cleanExamTag.length in 3..20) {
                allTags.add(cleanExamTag)
            }
            appendLine(allTags.joinToString(" ") { "#$it" })
        }.trim()
    }

    /**
     * Copies caption text to clipboard.
     */
    fun copyCaptionToClipboard(context: Context, caption: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("PrepOS MCQ Caption", caption)
            clipboard?.setPrimaryClip(clip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Measures height needed for static layout.
     */
    private fun createStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(alignment)
                .setLineSpacing(6f, 1.15f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, alignment, 1.15f, 6f, true)
        }
    }

    /**
     * Renders a beautiful high-resolution 3:4 aspect ratio poster bitmap of the MCQ.
     * Dimensions: 1080 x 1440 (Exact 3:4 Ratio), vibrant modern dark palette, clear question,
     * options, and bottom answer & explanation box.
     */
    fun generatePosterBitmap(
        item: QuestionItem,
        examName: String = "Exam Prep"
    ): Bitmap {
        // Strict 3:4 Aspect Ratio (1080 x 1440)
        val width = 1080
        val height = 1440
        val horizontalPadding = 56
        val contentWidth = width - (horizontalPadding * 2)
        val optLabels = listOf("A", "B", "C", "D", "E")

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background Gradient (Deep Rich Sapphire to Midnight Violet)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#0A0E1A"), // Midnight Sapphire
                    Color.parseColor("#151833"), // Deep Indigo Slate
                    Color.parseColor("#1F1538"), // Rich Violet
                    Color.parseColor("#0C0F1D")  // Deep Base
                ),
                floatArrayOf(0f, 0.35f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Vibrant ambient glowing accents
        val glowPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                width.toFloat() - 300f, 0f, width.toFloat(), 400f,
                Color.argb(70, 139, 92, 246), // Purple glow
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width.toFloat() - 80f, 120f, 360f, glowPaint1)

        val glowPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, height.toFloat() - 360f, 360f, height.toFloat(),
                Color.argb(55, 59, 130, 246), // Blue glow
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(100f, height.toFloat() - 100f, 340f, glowPaint2)

        // Glowing Outer Decorative Border
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4338CA")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(RectF(18f, 18f, width - 18f, height - 18f), 32f, 32f, outerBorderPaint)

        // Paints for Typography
        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A5B4FC")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E7FF")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val questionBadgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8") // Sky blue
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Adaptive Question Text Paint based on text length to fill space optimally
        val qTextLen = item.questionText.length
        val qTextSize = when {
            qTextLen < 90 -> 34f
            qTextLen < 180 -> 29f
            qTextLen < 280 -> 25f
            else -> 22f
        }
        val questionTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = qTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val optionLetterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val optionTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Layout measurements
        val questionLayout = createStaticLayout(item.questionText, questionTextPaint, contentWidth - 48)

        val optionLayouts = item.options.map { opt ->
            val optWidth = contentWidth - 110
            createStaticLayout(opt, optionTextPaint, optWidth)
        }

        // 2. Draw Top Header Bar
        val headerY = 48f
        val logoBoxSize = 58f
        val logoBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                horizontalPadding.toFloat(), headerY,
                horizontalPadding.toFloat() + logoBoxSize, headerY + logoBoxSize,
                Color.parseColor("#8B5CF6"),
                Color.parseColor("#3B82F6"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(
            RectF(
                horizontalPadding.toFloat(),
                headerY,
                horizontalPadding.toFloat() + logoBoxSize,
                headerY + logoBoxSize
            ),
            16f, 16f, logoBoxPaint
        )

        val logoTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("P", horizontalPadding + (logoBoxSize / 2f), headerY + 41f, logoTextPaint)

        canvas.drawText("PrepOS", horizontalPadding + logoBoxSize + 16f, headerY + 34f, headerTitlePaint)
        canvas.drawText("AI Study & MCQ Master", horizontalPadding + logoBoxSize + 16f, headerY + 58f, headerSubPaint)

        // Exam badge on top-right
        val cleanExam = examName.ifBlank { "Daily Challenge" }
        val displayExam = if (cleanExam.length > 22) cleanExam.take(20) + "..." else cleanExam
        val examBadgeWidth = badgeTextPaint.measureText("🎯 $displayExam") + 32f
        val examBadgeX = (width - horizontalPadding - examBadgeWidth).toFloat()

        val examBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#312E81")
            style = Paint.Style.FILL
        }
        val examBadgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1")
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }

        val examBadgeRect = RectF(examBadgeX, headerY + 6f, (width - horizontalPadding).toFloat(), headerY + 52f)
        canvas.drawRoundRect(examBadgeRect, 20f, 20f, examBadgeBgPaint)
        canvas.drawRoundRect(examBadgeRect, 20f, 20f, examBadgeBorderPaint)
        canvas.drawText("🎯 $displayExam", examBadgeX + 16f, headerY + 36f, badgeTextPaint)

        // Divider below header
        val headerDividerY = headerY + logoBoxSize + 18f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            strokeWidth = 1.5f
        }
        canvas.drawLine(horizontalPadding.toFloat(), headerDividerY, (width - horizontalPadding).toFloat(), headerDividerY, dividerPaint)

        // 3. Bottom Answer & Explanation Card
        val correctLetter = optLabels.getOrElse(item.correctOptionIndex) { "A" }
        val correctText = item.options.getOrElse(item.correctOptionIndex) { "" }
        val hasExplanation = item.explanation.isNotBlank()

        val explSnippet = if (hasExplanation) {
            val cleanExpl = item.explanation.trim()
            if (cleanExpl.length <= 150) cleanExpl else cleanExpl.take(142) + "... (See caption 👇)"
        } else {
            "Check caption below for detailed concepts & solution breakdown 👇"
        }

        val explPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val explLayout = createStaticLayout(
            "💡 $explSnippet",
            explPaint,
            contentWidth - 40
        )

        val footerHeight = (80f + explLayout.height + 36f).coerceIn(160f, 230f)
        val footerY = height - 44f - footerHeight
        val footerRect = RectF(
            horizontalPadding.toFloat(),
            footerY,
            (width - horizontalPadding).toFloat(),
            footerY + footerHeight
        )

        val footerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                horizontalPadding.toFloat(), footerY,
                (width - horizontalPadding).toFloat(), footerY + footerHeight,
                Color.parseColor("#1E1B4B"),
                Color.parseColor("#0F172A"),
                Shader.TileMode.CLAMP
            )
        }
        val footerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1")
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }

        canvas.drawRoundRect(footerRect, 18f, 18f, footerBgPaint)
        canvas.drawRoundRect(footerRect, 18f, 18f, footerBorderPaint)

        // Answer Title
        val answerHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34D399") // Emerald green
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val answerText = "✅ Correct Answer: Option $correctLetter – $correctText"
        val displayAnswer = if (answerText.length > 55) answerText.take(52) + "..." else answerText
        canvas.drawText(displayAnswer, horizontalPadding + 20f, footerY + 34f, answerHeaderPaint)

        // Explanation snippet or Intro
        canvas.save()
        canvas.translate(horizontalPadding + 20f, footerY + 48f)
        explLayout.draw(canvas)
        canvas.restore()

        // App CTA Subtext
        val footerCtaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("🚀 PrepOS AI Study Companion", (width - horizontalPadding - 20).toFloat(), footerY + footerHeight - 14f, footerCtaPaint)

        // 4. Middle Content Calculation
        val availableMiddleTop = headerDividerY + 16f
        val availableMiddleBottom = footerY - 16f

        // Question Card Height
        val questionCardPaddingVertical = 16f
        val questionCardHeight = (questionLayout.height + 36f + (questionCardPaddingVertical * 2)).coerceAtLeast(100f)

        val questionCardRect = RectF(
            horizontalPadding.toFloat(),
            availableMiddleTop,
            (width - horizontalPadding).toFloat(),
            availableMiddleTop + questionCardHeight
        )

        val questionCardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141E33")
            style = Paint.Style.FILL
        }
        val questionCardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8").let { Color.argb(140, Color.red(it), Color.green(it), Color.blue(it)) }
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        canvas.drawRoundRect(questionCardRect, 18f, 18f, questionCardBgPaint)
        canvas.drawRoundRect(questionCardRect, 18f, 18f, questionCardBorderPaint)

        canvas.drawText("❓ QUESTION CHALLENGE", horizontalPadding + 20f, availableMiddleTop + 30f, questionBadgePaint)

        canvas.save()
        canvas.translate(horizontalPadding + 20f, availableMiddleTop + 48f)
        questionLayout.draw(canvas)
        canvas.restore()

        // 5. Options Section
        val optionsSectionTop = availableMiddleTop + questionCardHeight + 14f
        val availableOptionsHeight = availableMiddleBottom - optionsSectionTop
        val numOptions = item.options.size.coerceAtLeast(1)

        val optionGap = 10f
        val totalGapHeight = optionGap * (numOptions - 1)
        val singleOptionHeight = ((availableOptionsHeight - totalGapHeight) / numOptions).coerceIn(64f, 96f)

        val optionColors = listOf(
            Pair("#1E3A8A", "#3B82F6"), // Blue
            Pair("#4C1D95", "#8B5CF6"), // Purple
            Pair("#831843", "#EC4899"), // Pink
            Pair("#064E3B", "#10B981"), // Emerald
            Pair("#78350F", "#F59E0B")  // Amber
        )

        var curOptY = optionsSectionTop
        item.options.forEachIndexed { idx, opt ->
            val label = optLabels.getOrElse(idx) { "${idx + 1}" }
            val optLayout = optionLayouts[idx]

            val cardRect = RectF(
                horizontalPadding.toFloat(),
                curOptY,
                (width - horizontalPadding).toFloat(),
                curOptY + singleOptionHeight
            )

            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1A2238")
                style = Paint.Style.FILL
            }
            val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
            }

            canvas.drawRoundRect(cardRect, 14f, 14f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 14f, 14f, cardStrokePaint)

            // Option Letter Circle Badge
            val circleCenterX = horizontalPadding + 38f
            val circleCenterY = curOptY + (singleOptionHeight / 2f)
            val circleRadius = 20f

            val optColorPair = optionColors.getOrElse(idx) { optionColors[0] }
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(optColorPair.first)
                style = Paint.Style.FILL
            }
            val circleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(optColorPair.second)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, circlePaint)
            canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, circleBorderPaint)

            optionLetterPaint.apply {
                color = Color.parseColor("#F8FAFC")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(label, circleCenterX, circleCenterY + 8f, optionLetterPaint)

            // Option Text Layout centered vertically
            val textLeft = horizontalPadding + 76f
            val textTop = curOptY + ((singleOptionHeight - optLayout.height) / 2f).coerceAtLeast(6f)

            canvas.save()
            canvas.translate(textLeft, textTop)
            optLayout.draw(canvas)
            canvas.restore()

            curOptY += singleOptionHeight + optionGap
        }

        return bitmap
    }

    /**
     * Saves bitmap to app cache directory and returns file Uri via FileProvider.
     */
    suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "shared_mcq").apply { if (!exists()) mkdirs() }
            // Clean older images to save storage
            cacheDir.listFiles()?.forEach { file ->
                if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                    file.delete()
                }
            }

            val imageFile = File(cacheDir, "prepos_mcq_${System.currentTimeMillis()}.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Complete One-Tap Workflow:
     * 1. Copies full caption + answer + explanation + CTA to Clipboard
     * 2. Generates beautiful MCQ poster image in background
     * 3. Opens Android Share Sheet with image + text
     */
    suspend fun generateAndShareMcq(
        context: Context,
        item: QuestionItem,
        examName: String = "Exam Prep",
        chapterTitle: String = "",
        onDone: () -> Unit = {}
    ) = withContext(Dispatchers.Main) {
        val effectiveTitle = if (chapterTitle.isNotBlank()) chapterTitle else examName
        // Step 1: Copy caption to clipboard immediately
        val caption = buildClipboardCaption(item, examName, chapterTitle)
        copyCaptionToClipboard(context, caption)
        Toast.makeText(context, "✓ Poster generated & caption copied to clipboard!", Toast.LENGTH_SHORT).show()

        // Step 2: Generate bitmap and save to cache
        val uri = withContext(Dispatchers.Default) {
            val bmp = generatePosterBitmap(item, effectiveTitle)
            saveBitmapToCache(context, bmp)
        }

        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra(Intent.EXTRA_TITLE, "PrepOS MCQ Challenge - $effectiveTitle")
                putExtra(Intent.EXTRA_SUBJECT, "PrepOS MCQ Challenge - $effectiveTitle")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share MCQ Poster via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } else {
            // Fallback to text share if image generation had issue
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            context.startActivity(Intent.createChooser(textIntent, "Share MCQ"))
        }

        onDone()
    }
}
