package com.example.ui.editor

import com.example.model.CalloutType
import com.example.model.ChartEntry
import com.example.model.ChartType
import com.example.model.DiagramEdge
import com.example.model.DiagramNode
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.ElementType
import com.example.model.InlineSpan
import com.example.model.NodeShape
import com.example.util.MathNotationHelper
import java.util.UUID

/**
 * Intelligent, crash-safe semantic parser that splits pasted study notes, chapters,
 * markdown, tables, callouts, diagrams, and lists into structured DocElements.
 *
 * Guarantees:
 * 1. Zero UI thread blocking (runs on background dispatcher).
 * 2. Never crashes or throws on malformed, oversized, or complex inputs.
 * 3. Conservative hierarchy inference (never turns arbitrary plain sentences into false headings).
 */
object PasteProcessor {

    private const val MAX_PASTE_LENGTH = 300_000

    /**
     * Parse raw pasted text into a list of structured document blocks safely.
     */
    fun parseRichTextToBlocks(rawText: String?): List<DocElement> {
        if (rawText.isNullOrBlank()) {
            return listOf(DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = ""))
        }

        return try {
            val safeInput = if (rawText.length > MAX_PASTE_LENGTH) {
                rawText.substring(0, MAX_PASTE_LENGTH)
            } else {
                rawText
            }
            parseInternal(safeInput)
        } catch (e: Throwable) {
            // Safety Fallback: Split raw text by double newlines into simple paragraphs
            fallbackParagraphs(rawText)
        }
    }

    private fun fallbackParagraphs(rawText: String): List<DocElement> {
        val lines = rawText.split("\n\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return listOf(DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = rawText.trim()))
        }
        return lines.map { paragraph ->
            DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = ElementType.PARAGRAPH,
                text = paragraph.trim()
            )
        }
    }

    private fun parseInternal(rawText: String): List<DocElement> {
        val lines = rawText.lines()
        val blocks = mutableListOf<DocElement>()
        var lineIndex = 0
        val totalLines = lines.size

        while (lineIndex < totalLines) {
            val line = lines[lineIndex].trimEnd()
            val trimmed = line.trim()

            // Skip consecutive empty lines
            if (trimmed.isEmpty()) {
                lineIndex++
                continue
            }

            // 0. Check for Horizontal Rule / Divider (---, ***, ___)
            if (isDividerStart(trimmed)) {
                lineIndex++
                continue
            }

            // 1. Check for Markdown / ASCII Table
            if (isTableStart(line, lines, lineIndex)) {
                try {
                    val (tableBlock, nextIdx) = parseTableBlock(lines, lineIndex)
                    blocks.add(tableBlock)
                    lineIndex = nextIdx
                    continue
                } catch (e: Exception) {
                    // fallback to standard line parsing
                }
            }

            // 2. Check for Recognizable ASCII / Flowchart Diagrams
            if (isDiagramStart(line, lines, lineIndex)) {
                try {
                    val (diagramBlock, nextIdx) = parseDiagramBlock(lines, lineIndex)
                    blocks.add(diagramBlock)
                    lineIndex = nextIdx
                    continue
                } catch (e: Exception) {
                    // fallback to standard line parsing
                }
            }

            // 3. Check for Callouts / Study Tips
            if (isCalloutStart(trimmed)) {
                try {
                    val (calloutBlock, nextIdx) = parseCalloutBlock(lines, lineIndex)
                    blocks.add(calloutBlock)
                    lineIndex = nextIdx
                    continue
                } catch (e: Exception) {
                    // fallback line
                }
            }

            // 4. Check for Chart / Progress Rings
            val chartBlock = tryParseChart(trimmed)
            if (chartBlock != null) {
                blocks.add(chartBlock)
                lineIndex++
                continue
            }

            // 5. Check for Markdown Image: ![caption](url) or standalone image URLs
            val imageMatch = parseImage(trimmed)
            if (imageMatch != null) {
                blocks.add(imageMatch)
                lineIndex++
                continue
            }

            // 6. Check for Explicit Headings & Numbered Hierarchy (H1, H2, H3, H4)
            val headingMatch = parseHeading(trimmed)
            if (headingMatch != null) {
                blocks.add(headingMatch)
                lineIndex++
                continue
            }

            // 7. Check for Bullet Points
            val bulletMatch = parseBullet(trimmed)
            if (bulletMatch != null) {
                blocks.add(bulletMatch)
                lineIndex++
                continue
            }

            // 8. Check for Numbered List
            val numberedMatch = parseNumbered(trimmed)
            if (numberedMatch != null) {
                blocks.add(numberedMatch)
                lineIndex++
                continue
            }

            // 9. Regular Paragraph - collect multi-line paragraph until empty line or special token
            val paragraphLines = mutableListOf<String>()
            paragraphLines.add(line.trim())
            lineIndex++

            while (lineIndex < totalLines) {
                val nextLine = lines[lineIndex].trim()
                if (nextLine.isEmpty() ||
                    isDividerStart(nextLine) ||
                    isHeadingStart(nextLine) ||
                    isBulletStart(nextLine) ||
                    isNumberedStart(nextLine) ||
                    isTableStart(lines[lineIndex], lines, lineIndex) ||
                    isDiagramStart(lines[lineIndex], lines, lineIndex) ||
                    isCalloutStart(nextLine) ||
                    tryParseChart(nextLine) != null ||
                    parseImage(nextLine) != null
                ) {
                    break
                }
                paragraphLines.add(nextLine)
                lineIndex++
            }

            val fullParagraphText = paragraphLines.joinToString(" ")
            val (cleanText, spans) = parseInlineFormatting(fullParagraphText)
            blocks.add(
                DocElement.TextBlock(
                    blockId = UUID.randomUUID().toString(),
                    blockType = ElementType.PARAGRAPH,
                    text = cleanText,
                    spans = spans
                )
            )
        }

        return if (blocks.isEmpty()) {
            listOf(DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = ""))
        } else {
            blocks
        }
    }

    /**
     * Determines if a line is the start of a Markdown / ASCII table safely.
     */
    private fun isTableStart(line: String, allLines: List<String>, currentIndex: Int): Boolean {
        val trimmed = line.trim()
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
            return true
        }
        if (currentIndex + 1 < allLines.size) {
            val nextTrimmed = allLines[currentIndex + 1].trim()
            if (nextTrimmed.startsWith("|") && nextTrimmed.contains("-")) {
                return true
            }
        }
        return false
    }

    private fun parseTableBlock(lines: List<String>, startIndex: Int): Pair<DocElement.TableBlock, Int> {
        val tableLines = mutableListOf<String>()
        var idx = startIndex
        while (idx < lines.size && lines[idx].trim().contains("|") && tableLines.size < 100) {
            tableLines.add(lines[idx].trim())
            idx++
        }

        var headers = listOf("Column 1", "Column 2")
        val rows = mutableListOf<List<String>>()
        var hasHeader = true

        val parsedRows = tableLines.filter { !it.matches(Regex("""^\|?[\s\-:|]+\|?$""")) }.map { rawRow ->
            rawRow.trim().removeSurrounding("|", "|")
                .split("|")
                .map { it.trim() }
        }

        if (parsedRows.isNotEmpty()) {
            headers = parsedRows[0]
            rows.addAll(if (parsedRows.size > 1) parsedRows.drop(1) else listOf(List(headers.size) { "" }))
        } else {
            hasHeader = false
            rows.add(listOf("Data 1", "Data 2"))
        }

        return Pair(
            DocElement.TableBlock(
                blockId = UUID.randomUUID().toString(),
                title = "Study Reference Table",
                headers = headers,
                rows = rows,
                hasHeaderRow = hasHeader
            ),
            idx
        )
    }

    /**
     * Detects recognizable ASCII diagram, flowchart arrows, or box-drawing characters.
     */
    private fun isDiagramStart(line: String, allLines: List<String>, currentIndex: Int): Boolean {
        val trimmed = line.trim()
        // Check for ASCII box borders or flowchart sequences
        val hasAsciiArt = trimmed.contains("+---+") || trimmed.contains("+===+") ||
                trimmed.contains("+---+") || trimmed.contains("┌───") ||
                trimmed.contains("╭───") || (trimmed.contains("|") && trimmed.contains("-->")) ||
                trimmed.contains("--->") || trimmed.contains("===>")
        if (hasAsciiArt) return true

        // Sequence of flowchart steps: [Step 1] -> [Step 2] -> [Step 3]
        if (trimmed.contains(" -> ") && (trimmed.contains("[") || trimmed.contains("("))) {
            return true
        }

        return false
    }

    private fun parseDiagramBlock(lines: List<String>, startIndex: Int): Pair<DocElement.DiagramBlock, Int> {
        val diagramLines = mutableListOf<String>()
        var idx = startIndex
        while (idx < lines.size && diagramLines.size < 40) {
            val line = lines[idx]
            if (line.isBlank() && diagramLines.size > 2) {
                break
            }
            if (line.isNotBlank()) {
                diagramLines.add(line)
            }
            idx++
        }

        val rawAscii = diagramLines.joinToString("\n")

        // If it's a simple step flowchart e.g. "Input -> Process -> Output"
        val nodes = mutableListOf<String>()
        val structuredNodes = mutableListOf<DiagramNode>()
        if (rawAscii.contains("->")) {
            val rawTokens = rawAscii.split(Regex("""->|-->|--->|==>"""))
            val steps = rawTokens.map { it.trim() }.filter { it.isNotBlank() }
            if (steps.size >= 2) {
                steps.forEachIndexed { i, rawStep ->
                    val cleanLabel = MathNotationHelper.formatMathNotation(
                        rawStep.removeSurrounding("[", "]")
                            .removeSurrounding("(", ")")
                            .removeSurrounding("{", "}")
                            .removeSurrounding("<", ">")
                    )
                    nodes.add(cleanLabel)

                    // Infer node shape from brackets or keywords
                    val shape = when {
                        rawStep.startsWith("{") && rawStep.endsWith("}") -> NodeShape.DIAMOND
                        rawStep.startsWith("<") && rawStep.endsWith(">") -> NodeShape.DIAMOND
                        rawStep.startsWith("([") && rawStep.endsWith("])") -> NodeShape.CAPSULE
                        rawStep.startsWith("((") && rawStep.endsWith("))") -> NodeShape.CIRCLE
                        rawStep.startsWith("(") && rawStep.endsWith(")") -> NodeShape.CIRCLE
                        rawStep.startsWith("[[") && rawStep.endsWith("]]") -> NodeShape.HEXAGON
                        rawStep.startsWith("[") && rawStep.endsWith("]") -> NodeShape.ROUNDED_RECT
                        cleanLabel.contains("?") || cleanLabel.lowercase().startsWith("if ") || cleanLabel.lowercase().startsWith("check ") -> NodeShape.DIAMOND
                        i == 0 -> NodeShape.CAPSULE
                        i == steps.size - 1 -> NodeShape.CAPSULE
                        else -> NodeShape.ROUNDED_RECT
                    }
                    structuredNodes.add(
                        DiagramNode(
                            id = "p_node_$i",
                            label = cleanLabel,
                            shape = shape
                        )
                    )
                }
            }
        }

        val diagramType = if (nodes.size >= 2) DiagramType.FLOWCHART else DiagramType.ASCII
        val title = if (diagramType == DiagramType.FLOWCHART) "Process Flowchart" else "Structural Diagram"

        return Pair(
            DocElement.DiagramBlock(
                blockId = UUID.randomUUID().toString(),
                diagramType = diagramType,
                title = title,
                nodes = if (nodes.isNotEmpty()) nodes else listOf("Step 1: Input", "Step 2: Processing", "Step 3: Output"),
                structuredNodes = structuredNodes,
                rawContent = rawAscii
            ),
            idx
        )
    }

    private fun isCalloutStart(line: String): Boolean {
        val trimmed = line.trim()
        val lower = trimmed.lowercase()

        // 1. GitHub / Obsidian style: > [!NOTE], > [!IMPORTANT], > [!TIP], etc.
        if (lower.startsWith("> [!") || lower.startsWith(">[!")) return true

        // 2. Blockquote with callout prefix: > **Definition:**, > Note:, etc.
        if (trimmed.startsWith(">")) {
            val inner = trimmed.removePrefix(">").trim().removePrefix("**").trim().lowercase()
            if (inner.startsWith("note") || inner.startsWith("important") || inner.startsWith("tip") ||
                inner.startsWith("exam tip") || inner.startsWith("definition") || inner.startsWith("formula") ||
                inner.startsWith("key formula") || inner.startsWith("theorem") || inner.startsWith("warning") ||
                inner.startsWith("key concept") || inner.startsWith("key takeaway")
            ) return true
        }

        // 3. Bold Markdown prefixes commonly emitted by LLMs: **Definition:**, **Important:**, **Key Formula:**, etc.
        if (trimmed.startsWith("**") && (trimmed.contains(":**") || trimmed.contains("**:") || trimmed.contains("**:"))) {
            val inner = trimmed.removePrefix("**").trim().lowercase()
            if (inner.startsWith("definition") || inner.startsWith("important") || inner.startsWith("note") ||
                inner.startsWith("exam tip") || inner.startsWith("tip") || inner.startsWith("formula") ||
                inner.startsWith("key formula") || inner.startsWith("theorem") || inner.startsWith("warning") ||
                inner.startsWith("key concept") || inner.startsWith("key takeaway") || inner.startsWith("example") ||
                inner.startsWith("solution")
            ) return true
        }

        // 4. Plain text prefixes
        return lower.startsWith("note:") ||
                lower.startsWith("important:") ||
                lower.startsWith("tip:") ||
                lower.startsWith("exam tip:") ||
                lower.startsWith("definition:") ||
                lower.startsWith("formula:") ||
                lower.startsWith("theorem:") ||
                lower.startsWith("key formula:") ||
                lower.startsWith("key concept:") ||
                lower.startsWith("key takeaway:") ||
                lower.startsWith("warning:")
    }

    private fun parseCalloutBlock(lines: List<String>, startIndex: Int): Pair<DocElement.CalloutBlock, Int> {
        val firstLine = lines[startIndex].trim()
        val lower = firstLine.lowercase()

        val calloutType = when {
            lower.contains("definition") -> CalloutType.DEFINITION
            lower.contains("formula") || lower.contains("theorem") || lower.contains("equation") -> CalloutType.FORMULA
            lower.contains("exam") || lower.contains("tip") || lower.contains("strategy") -> CalloutType.EXAM_TIP
            else -> CalloutType.IMPORTANT
        }

        var title = calloutType.title
        var content = firstLine

        // Extract customized title if provided (e.g. **Definition (Newton's First Law):** or **Formula (Kinematics):**)
        val customMatch = Regex(
            """^(?:>\s*)?(?:\*\*)?(Note|Important|Tip|Exam Tip|Definition|Formula|Key Formula|Theorem|Key Concept|Warning|Example|Solution)(?:\s*\((.*?)\))?(?:\*\*)?\s*[:–-]\s*(.*)$""",
            RegexOption.IGNORE_CASE
        ).find(firstLine)

        if (customMatch != null) {
            val tag = customMatch.groupValues[1].trim()
            val subLabel = customMatch.groupValues[2].trim()
            val rest = customMatch.groupValues[3].trim().removePrefix("**").trim()
            title = if (subLabel.isNotBlank()) {
                "$tag: $subLabel"
            } else when (calloutType) {
                CalloutType.DEFINITION -> if (tag.contains("definition", ignoreCase = true)) "Definition" else tag
                CalloutType.FORMULA -> "Key Formula"
                CalloutType.EXAM_TIP -> "Exam Strategy & Tip"
                CalloutType.IMPORTANT -> if (tag.contains("warning", ignoreCase = true)) "Important Warning" else "Important Concept"
            }
            content = rest
        } else {
            content = firstLine
                .replace(Regex("""^>\s*\[!.*?\]\s*""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^(?:>\s*)?(?:\*\*)?(Note|Important|Tip|Exam Tip|Definition|Formula|Key Formula|Theorem|Key Concept|Warning|Example|Solution)(?:\*\*)?\s*[:–-]\s*""", RegexOption.IGNORE_CASE), "")
                .trim()
        }

        var idx = startIndex + 1
        val extraLines = mutableListOf<String>()
        while (idx < lines.size && extraLines.size < 50) {
            val nextLine = lines[idx].trim()
            if (nextLine.isEmpty() || isHeadingStart(nextLine) || isCalloutStart(nextLine) ||
                isTableStart(lines[idx], lines, idx) || isDiagramStart(lines[idx], lines, idx) ||
                isBulletStart(nextLine) || isNumberedStart(nextLine)
            ) {
                break
            }
            if (nextLine.startsWith(">")) {
                extraLines.add(nextLine.removePrefix(">").trim())
            } else {
                extraLines.add(nextLine)
            }
            idx++
        }

        if (extraLines.isNotEmpty()) {
            content = (if (content.isNotBlank()) content + " " else "") + extraLines.joinToString(" ")
        }

        val (cleanContent, spans) = parseInlineFormatting(content)

        return Pair(
            DocElement.CalloutBlock(
                blockId = UUID.randomUUID().toString(),
                calloutType = calloutType,
                title = title,
                content = cleanContent
            ),
            idx
        )
    }

    private fun parseImage(line: String): DocElement.ImageBlock? {
        if (!line.contains("http") && !line.contains("![")) return null

        val mdImageRegex = Regex("""!\[(.*?)\]\((https?://.*?|data:image/.*?)\)""")
        val match = mdImageRegex.find(line)
        if (match != null) {
            val caption = match.groupValues[1]
            val url = match.groupValues[2]
            return DocElement.ImageBlock(
                blockId = UUID.randomUUID().toString(),
                imageUri = url,
                caption = caption
            )
        }

        val urlRegex = Regex("""^(https?://[^\s]+\.(?:png|jpg|jpeg|webp|gif))(\s+.*)?$""", RegexOption.IGNORE_CASE)
        val urlMatch = urlRegex.find(line)
        if (urlMatch != null) {
            val url = urlMatch.groupValues[1]
            val caption = urlMatch.groupValues[2].trim()
            return DocElement.ImageBlock(
                blockId = UUID.randomUUID().toString(),
                imageUri = url,
                caption = caption
            )
        }

        return null
    }

    private fun tryParseChart(line: String): DocElement.ChartBlock? {
        val lower = line.lowercase()
        // Format A: [Progress: 80%] or Progress: 80% or Readiness: 75%
        val singleMetric = Regex("""(?:\[?\s*(Progress|Weightage|Readiness|Completion|Accuracy)\s*[:=]\s*(\d{1,3}(?:\.\d+)?)\s*%\s*\]?)""", RegexOption.IGNORE_CASE)
        val matchSingle = singleMetric.find(line)
        if (matchSingle != null) {
            val label = matchSingle.groupValues[1]
            val value = matchSingle.groupValues[2].toFloatOrNull() ?: 50f
            return DocElement.ChartBlock(
                blockId = UUID.randomUUID().toString(),
                chartType = ChartType.PROGRESS_RINGS,
                title = "$label Overview",
                entries = listOf(
                    ChartEntry(label, value.coerceIn(0f, 100f), "#6C63D9")
                )
            )
        }

        // Format B: [Chart: Label1: 40%, Label2: 60%] or [Progress: A: 50%, B: 80%] or [Chart_Bars: A: 10, B: 20]
        if (lower.startsWith("[chart:") || lower.startsWith("[progress:") || lower.startsWith("[chart_bars:")) {
            val inner = line.removePrefix("[").removeSuffix("]").substringAfter(":")
            val parts = inner.split(",")
            val entries = mutableListOf<ChartEntry>()
            val colors = listOf("#6C63D9", "#2563EB", "#059669", "#D97706", "#DC2626", "#7C3AED", "#DB2777")
            for (part in parts) {
                val pair = part.split(":")
                if (pair.size == 2) {
                    val pLabel = pair[0].trim()
                    val pVal = pair[1].replace("%", "").trim().toFloatOrNull() ?: 50f
                    entries.add(ChartEntry(pLabel, pVal, colors[entries.size % colors.size]))
                }
            }
            if (entries.isNotEmpty()) {
                val chartType = if (lower.contains("bar")) ChartType.BAR else ChartType.PROGRESS_RINGS
                return DocElement.ChartBlock(
                    blockId = UUID.randomUUID().toString(),
                    chartType = chartType,
                    title = "Topic Breakdown & Metrics",
                    entries = entries
                )
            }
        }
        return null
    }

    private fun isDividerStart(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.matches(Regex("""^(\-{3,}|\*{3,}|_{3,}|(\-\s*){3,}|(\*\s*){3,})$"""))
    }

    private fun isHeadingStart(line: String): Boolean {
        return parseHeading(line) != null
    }

    /**
     * Checks explicit heading markers and clear numbered section hierarchies.
     * Conservative: does not classify random plain sentences as headings.
     */
    private fun parseHeading(line: String): DocElement.TextBlock? {
        // 1. Explicit Markdown headings: # (H1), ## (H2), ### (H3), #### (H4), etc.
        val mdMatch = Regex("""^(#{1,6})\s+(.+)$""").find(line)
        if (mdMatch != null) {
            val level = mdMatch.groupValues[1].length
            val rawText = mdMatch.groupValues[2].trim()
            val (cleanText, spans) = parseInlineFormatting(rawText)
            val type = when (level) {
                1 -> ElementType.HEADING_1
                2 -> ElementType.HEADING_2
                3 -> ElementType.HEADING_3
                else -> ElementType.HEADING_4
            }
            return DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = type,
                text = cleanText,
                spans = spans
            )
        }

        // 2. Clear Chapter / Unit / Topic labels: "Chapter 1: ...", "Unit 2: ...", "Topic 1.0: ..."
        val chapterMatch = Regex("""^(Chapter\s+\d+|Unit\s+\d+|Part\s+\d+)\s*[:\-–]\s*(.+)$""", RegexOption.IGNORE_CASE).find(line)
        if (chapterMatch != null) {
            val (cleanText, spans) = parseInlineFormatting(line)
            return DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = ElementType.HEADING_1,
                text = cleanText,
                spans = spans
            )
        }

        val sectionMatch = Regex("""^(Section\s+\d+|Topic\s+\d+|Summary|Overview|Key Concepts)\s*[:\-–]?\s*(.*)$""", RegexOption.IGNORE_CASE).find(line)
        if (sectionMatch != null && line.length < 80) {
            val (cleanText, spans) = parseInlineFormatting(line)
            return DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = ElementType.HEADING_2,
                text = cleanText,
                spans = spans
            )
        }

        // 3. Clear Numbered Section Hierarchies:
        // "1.0 Main Topic" -> H1 / H2
        // "1.1 Sub Topic" -> H3
        // "1.1.1 Deeper Sub Topic" -> H4
        val numH3 = Regex("""^(\d+\.\d+)\s+([A-Z][^.?!]{2,70})$""").find(line)
        if (numH3 != null) {
            val (cleanText, spans) = parseInlineFormatting(line)
            return DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = ElementType.HEADING_3,
                text = cleanText,
                spans = spans
            )
        }

        val numH4 = Regex("""^(\d+\.\d+\.\d+)\s+([A-Z][^.?!]{2,70})$""").find(line)
        if (numH4 != null) {
            val (cleanText, spans) = parseInlineFormatting(line)
            return DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = ElementType.HEADING_4,
                text = cleanText,
                spans = spans
            )
        }

        // 4. Standalone Bold Headings emitted by LLMs (e.g. **Overview**, **1. Key Principles**, **Active Recall Questions**)
        val boldMatch = Regex("""^\*\*([A-Z0-9][\w\s\-–:,.()&/]{2,70})\*\*$""").find(line)
        if (boldMatch != null && !isCalloutStart(line)) {
            val rawText = boldMatch.groupValues[1].trim()
            val (cleanText, spans) = parseInlineFormatting(rawText)
            return DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = ElementType.HEADING_2,
                text = cleanText,
                spans = spans
            )
        }

        return null
    }

    private fun isBulletStart(line: String): Boolean {
        return line.matches(Regex("""^(\*|\-|•|o|\+|\u2022)\s+.+$"""))
    }

    private fun parseBullet(line: String): DocElement.TextBlock? {
        val match = Regex("""^(\*|\-|•|o|\+|\u2022)\s+(.+)$""").find(line) ?: return null
        val rawText = match.groupValues[2].trim()
        val (cleanText, spans) = parseInlineFormatting(rawText)
        return DocElement.TextBlock(
            blockId = UUID.randomUUID().toString(),
            blockType = ElementType.BULLET_LIST,
            text = cleanText,
            spans = spans
        )
    }

    private fun isNumberedStart(line: String): Boolean {
        return line.matches(Regex("""^(\d+[\.\)]|\(\d+\)|[a-z]\)|\([a-z]\))\s+.+$""", RegexOption.IGNORE_CASE))
    }

    private fun parseNumbered(line: String): DocElement.TextBlock? {
        val match = Regex("""^(\d+[\.\)]|\(\d+\)|[a-z]\)|\([a-z]\))\s+(.+)$""", RegexOption.IGNORE_CASE).find(line) ?: return null
        val rawText = match.groupValues[2].trim()
        val (cleanText, spans) = parseInlineFormatting(rawText)
        return DocElement.TextBlock(
            blockId = UUID.randomUUID().toString(),
            blockType = ElementType.NUMBERED_LIST,
            text = cleanText,
            spans = spans
        )
    }

    /**
     * Safely and non-recursively parses markdown inline formatting (**bold**, *italic*, ==highlight==, etc.)
     * without risk of infinite loops or regex catastrophic backtracking.
     */
    fun parseInlineFormatting(rawInput: String): Pair<String, List<InlineSpan>> {
        if (rawInput.isEmpty() || (!rawInput.contains("*") && !rawInput.contains("_") && !rawInput.contains("~") && !rawInput.contains("=") && !rawInput.contains("<"))) {
            return Pair(rawInput, emptyList())
        }

        try {
            val spans = mutableListOf<InlineSpan>()
            var text = rawInput

            // 1. Bold: **text** or __text__ (max 50 matches for safety)
            val boldRegex = Regex("""\*\*(.*?)\*\*|__(.*?)__""")
            var boldCount = 0
            while (boldCount < 50) {
                val match = boldRegex.find(text) ?: break
                val content = match.groupValues[1].ifEmpty { match.groupValues[2] }
                val start = match.range.first
                val end = start + content.length
                text = text.replaceRange(match.range, content)
                if (content.isNotEmpty()) {
                    spans.add(InlineSpan(start = start, end = end, isBold = true))
                }
                boldCount++
            }

            // 2. Highlight: ==text== or <mark>text</mark>
            val highlightRegex = Regex("""==(.*?)==|<mark>(.*?)</mark>""")
            var hlCount = 0
            while (hlCount < 50) {
                val match = highlightRegex.find(text) ?: break
                val content = match.groupValues[1].ifEmpty { match.groupValues[2] }
                val start = match.range.first
                val end = start + content.length
                text = text.replaceRange(match.range, content)
                if (content.isNotEmpty()) {
                    spans.add(InlineSpan(start = start, end = end, highlightColorHex = "#FEF08A"))
                }
                hlCount++
            }

            // 3. Strikethrough: ~~text~~
            val strikeRegex = Regex("""~~(.*?)~~""")
            var strikeCount = 0
            while (strikeCount < 50) {
                val match = strikeRegex.find(text) ?: break
                val content = match.groupValues[1]
                val start = match.range.first
                val end = start + content.length
                text = text.replaceRange(match.range, content)
                if (content.isNotEmpty()) {
                    spans.add(InlineSpan(start = start, end = end, isStrikethrough = true))
                }
                strikeCount++
            }

            // 4. Underline: <u>text</u>
            val underlineRegex = Regex("""<u>(.*?)</u>""")
            var uCount = 0
            while (uCount < 50) {
                val match = underlineRegex.find(text) ?: break
                val content = match.groupValues[1]
                val start = match.range.first
                val end = start + content.length
                text = text.replaceRange(match.range, content)
                if (content.isNotEmpty()) {
                    spans.add(InlineSpan(start = start, end = end, isUnderline = true))
                }
                uCount++
            }

            // 5. Italic: *text* or _text_
            val italicRegex = Regex("""\*(.*?)\*|_(.*?)_""")
            var itCount = 0
            while (itCount < 50) {
                val match = italicRegex.find(text) ?: break
                val content = match.groupValues[1].ifEmpty { match.groupValues[2] }
                val start = match.range.first
                val end = start + content.length
                text = text.replaceRange(match.range, content)
                if (content.isNotEmpty()) {
                    spans.add(InlineSpan(start = start, end = end, isItalic = true))
                }
                itCount++
            }

            return Pair(text, spans)
        } catch (e: Throwable) {
            return Pair(rawInput, emptyList())
        }
    }
}
