package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class PaperStyle(val title: String, val description: String) {
    PLAIN("Plain", "Clean unruled notebook paper"),
    RULED("Ruled", "Standard college ruled lines"),
    GRID("Grid", "Math & science quad grid"),
    DOTTED("Dotted", "Minimalist bullet journal dots")
}

enum class NoteFont(val displayName: String) {
    SANS_SERIF("Modern Sans"),
    SERIF("Classic Serif"),
    MONOSPACE("Monospace Code"),
    HANDWRITTEN("Neat Handwritten")
}

enum class PaperTheme(val displayName: String) {
    PAPER_LIGHT("Paper White"),
    WARM_SEPIA("Warm Sepia"),
    SOFT_SLATE("Soft Slate"),
    AMOLED_DARK("Midnight Dark")
}

enum class ElementType {
    HEADING_1,
    HEADING_2,
    HEADING_3,
    HEADING_4,
    PARAGRAPH,
    BULLET_LIST,
    NUMBERED_LIST,
    IMAGE,
    TABLE,
    DIAGRAM,
    CALLOUT,
    CHART
}

enum class CalloutType(val title: String, val icon: String, val colorHex: String) {
    IMPORTANT("Important Note", "priority_high", "#EF4444"),
    DEFINITION("Key Definition", "menu_book", "#3B82F6"),
    EXAM_TIP("Exam Tip", "lightbulb", "#F59E0B"),
    FORMULA("Formula / Theorem", "functions", "#10B981")
}

enum class DiagramType(val title: String) {
    FLOWCHART("Flowchart"),
    TREE("Hierarchy Tree"),
    COMPARISON("Comparison Columns"),
    ASCII("Structured ASCII"),
    FORMULA("Step-by-Step Logic")
}

enum class NodeStyle { DEFAULT, HIGHLIGHT, MUTED }
enum class NodeShape { RECTANGLE, ROUNDED_RECT, CIRCLE, DIAMOND, HEXAGON, CAPSULE }
enum class EdgeStyle { ARROW, DOUBLE_ARROW, LINE }

data class DiagramNode(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val sublabel: String = "",
    val style: NodeStyle = NodeStyle.DEFAULT,
    val shape: NodeShape = NodeShape.ROUNDED_RECT,
    val color: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("label", label)
            put("sublabel", sublabel)
            put("style", style.name)
            put("shape", shape.name)
            put("color", color)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DiagramNode {
            val styleStr = json.optString("style", NodeStyle.DEFAULT.name)
            val nodeStyle = try { NodeStyle.valueOf(styleStr) } catch (e: Exception) { NodeStyle.DEFAULT }
            val shapeStr = json.optString("shape", NodeShape.ROUNDED_RECT.name)
            val nodeShape = try {
                NodeShape.valueOf(shapeStr.uppercase())
            } catch (e: Exception) {
                when (shapeStr.uppercase()) {
                    "ROUNDED", "ROUNDED_RECTANGLE", "BOX" -> NodeShape.ROUNDED_RECT
                    "RECT", "RECTANGLE", "SQUARE" -> NodeShape.RECTANGLE
                    "ROUND", "CIRCLE", "OVAL" -> NodeShape.CIRCLE
                    "DECISION", "DIAMOND", "RHOMBUS" -> NodeShape.DIAMOND
                    "HEX", "HEXAGON" -> NodeShape.HEXAGON
                    "PILL", "CAPSULE", "STADIUM" -> NodeShape.CAPSULE
                    else -> NodeShape.ROUNDED_RECT
                }
            }
            return DiagramNode(
                id = json.optString("id", UUID.randomUUID().toString()),
                label = json.optString("label", ""),
                sublabel = json.optString("sublabel", ""),
                style = nodeStyle,
                shape = nodeShape,
                color = json.optString("color", "")
            )
        }
    }
}

data class DiagramEdge(
    val fromId: String = "",
    val toId: String = "",
    val label: String = "",
    val style: EdgeStyle = EdgeStyle.ARROW
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("fromId", fromId)
            put("toId", toId)
            put("label", label)
            put("style", style.name)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DiagramEdge {
            val styleStr = json.optString("style", EdgeStyle.ARROW.name)
            val edgeStyle = try { EdgeStyle.valueOf(styleStr) } catch (e: Exception) { EdgeStyle.ARROW }
            return DiagramEdge(
                fromId = json.optString("fromId", ""),
                toId = json.optString("toId", ""),
                label = json.optString("label", ""),
                style = edgeStyle
            )
        }
    }
}

enum class ChartType(val title: String) {
    BAR("Bar Chart"),
    PIE("Pie Chart"),
    DONUT("Donut Chart"),
    PROGRESS_RINGS("Progress Rings")
}

data class ChartEntry(
    val label: String = "",
    val value: Float = 0f,
    val colorHex: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("label", label)
            put("value", value.toDouble())
            put("colorHex", colorHex)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ChartEntry {
            return ChartEntry(
                label = json.optString("label", ""),
                value = json.optDouble("value", 0.0).toFloat(),
                colorHex = json.optString("colorHex", "")
            )
        }
    }
}

data class InlineSpan(
    val start: Int,
    val end: Int,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val textColorHex: String? = null,
    val highlightColorHex: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("start", start)
            put("end", end)
            put("isBold", isBold)
            put("isItalic", isItalic)
            put("isUnderline", isUnderline)
            put("isStrikethrough", isStrikethrough)
            put("textColorHex", textColorHex ?: "")
            put("highlightColorHex", highlightColorHex ?: "")
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InlineSpan {
            return InlineSpan(
                start = json.optInt("start", 0),
                end = json.optInt("end", 0),
                isBold = json.optBoolean("isBold", false),
                isItalic = json.optBoolean("isItalic", false),
                isUnderline = json.optBoolean("isUnderline", false),
                isStrikethrough = json.optBoolean("isStrikethrough", false),
                textColorHex = json.optString("textColorHex").takeIf { it.isNotBlank() },
                highlightColorHex = json.optString("highlightColorHex").takeIf { it.isNotBlank() }
            )
        }
    }
}

sealed class DocElement(
    val id: String = UUID.randomUUID().toString(),
    val type: ElementType
) {
    abstract fun toJson(): JSONObject

    data class TextBlock(
        val blockId: String = UUID.randomUUID().toString(),
        val blockType: ElementType = ElementType.PARAGRAPH,
        var text: String = "",
        var isBold: Boolean = false,
        var isItalic: Boolean = false,
        var isUnderline: Boolean = false,
        var isStrikethrough: Boolean = false,
        var textColorHex: String? = null,
        var highlightColorHex: String? = null,
        var alignment: String = "LEFT", // LEFT, CENTER, RIGHT
        var spans: List<InlineSpan> = emptyList()
    ) : DocElement(blockId, blockType) {
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", blockId)
                put("type", blockType.name)
                put("text", text)
                put("isBold", isBold)
                put("isItalic", isItalic)
                put("isUnderline", isUnderline)
                put("isStrikethrough", isStrikethrough)
                put("textColorHex", textColorHex ?: "")
                put("highlightColorHex", highlightColorHex ?: "")
                put("alignment", alignment)
                if (spans.isNotEmpty()) {
                    val spansArray = JSONArray()
                    spans.forEach { span ->
                        spansArray.put(span.toJson())
                    }
                    put("spans", spansArray)
                }
            }
        }

        /**
         * Checks formatting status for a range [startOffset, endOffset].
         * Returns (isFullyActive, isMixedActive)
         */
        fun queryRangeFormat(
            startOffset: Int,
            endOffset: Int,
            format: String // "BOLD", "ITALIC", "UNDERLINE", "STRIKETHROUGH"
        ): Pair<Boolean, Boolean> {
            val clampedStart = startOffset.coerceIn(0, text.length)
            val clampedEnd = endOffset.coerceIn(clampedStart, text.length)
            if (clampedStart == clampedEnd) {
                // Point/cursor check
                val isPointActive = when (format) {
                    "BOLD" -> isBold || spans.any { it.isBold && clampedStart in it.start..it.end }
                    "ITALIC" -> isItalic || spans.any { it.isItalic && clampedStart in it.start..it.end }
                    "UNDERLINE" -> isUnderline || spans.any { it.isUnderline && clampedStart in it.start..it.end }
                    "STRIKETHROUGH" -> isStrikethrough || spans.any { it.isStrikethrough && clampedStart in it.start..it.end }
                    else -> false
                }
                return Pair(isPointActive, false)
            }

            // Check how many characters in [clampedStart, clampedEnd] have the format
            val charActiveCount = (clampedStart until clampedEnd).count { charIndex ->
                when (format) {
                    "BOLD" -> isBold || spans.any { it.isBold && charIndex in it.start until it.end }
                    "ITALIC" -> isItalic || spans.any { it.isItalic && charIndex in it.start until it.end }
                    "UNDERLINE" -> isUnderline || spans.any { it.isUnderline && charIndex in it.start until it.end }
                    "STRIKETHROUGH" -> isStrikethrough || spans.any { it.isStrikethrough && charIndex in it.start until it.end }
                    else -> false
                }
            }

            val total = clampedEnd - clampedStart
            val allActive = charActiveCount == total
            val mixedActive = charActiveCount > 0 && charActiveCount < total
            return Pair(allActive, mixedActive)
        }

        /**
         * Toggles formatting on a specific range [startOffset, endOffset].
         */
        fun toggleFormatOnRange(
            startOffset: Int,
            endOffset: Int,
            format: String // "BOLD", "ITALIC", "UNDERLINE", "STRIKETHROUGH"
        ): TextBlock {
            val clampedStart = startOffset.coerceIn(0, text.length)
            val clampedEnd = endOffset.coerceIn(clampedStart, text.length)

            if (clampedStart == clampedEnd) {
                // Cursor mode: toggle block-level flag
                return when (format) {
                    "BOLD" -> copy(isBold = !isBold)
                    "ITALIC" -> copy(isItalic = !isItalic)
                    "UNDERLINE" -> copy(isUnderline = !isUnderline)
                    "STRIKETHROUGH" -> copy(isStrikethrough = !isStrikethrough)
                    else -> this
                }
            }

            val (isFullyActive, _) = queryRangeFormat(clampedStart, clampedEnd, format)
            val targetNewState = !isFullyActive

            // Apply or remove span
            val existingSpans = spans.filter { it.start < text.length && it.end <= text.length }.toMutableList()
            val matchIdx = existingSpans.indexOfFirst { it.start == clampedStart && it.end == clampedEnd }
            if (matchIdx >= 0) {
                val current = existingSpans[matchIdx]
                val updated = when (format) {
                    "BOLD" -> current.copy(isBold = targetNewState)
                    "ITALIC" -> current.copy(isItalic = targetNewState)
                    "UNDERLINE" -> current.copy(isUnderline = targetNewState)
                    "STRIKETHROUGH" -> current.copy(isStrikethrough = targetNewState)
                    else -> current
                }
                existingSpans[matchIdx] = updated
            } else {
                val newSpan = InlineSpan(
                    start = clampedStart,
                    end = clampedEnd,
                    isBold = if (format == "BOLD") targetNewState else false,
                    isItalic = if (format == "ITALIC") targetNewState else false,
                    isUnderline = if (format == "UNDERLINE") targetNewState else false,
                    isStrikethrough = if (format == "STRIKETHROUGH") targetNewState else false
                )
                existingSpans.add(newSpan)
            }

            return this.copy(spans = existingSpans)
        }

        /**
         * Applies formatting to a specific substring range [startOffset, endOffset].
         * If the range already has the style, it toggles it off.
         */
        fun applyFormattingToRange(
            startOffset: Int,
            endOffset: Int,
            bold: Boolean? = null,
            italic: Boolean? = null,
            underline: Boolean? = null,
            strikethrough: Boolean? = null,
            textColor: String? = null,
            highlightColor: String? = null,
            clearColors: Boolean = false
        ): TextBlock {
            val clampedStart = startOffset.coerceIn(0, text.length)
            val clampedEnd = endOffset.coerceIn(clampedStart, text.length)
            if (clampedStart >= clampedEnd) return this

            val existingSpans = spans.filter { it.start < text.length && it.end <= text.length }.toMutableList()
            
            // Check if there is an overlapping span on this exact range
            val matchIdx = existingSpans.indexOfFirst { it.start == clampedStart && it.end == clampedEnd }
            if (matchIdx >= 0) {
                val current = existingSpans[matchIdx]
                val updated = current.copy(
                    isBold = bold ?: current.isBold,
                    isItalic = italic ?: current.isItalic,
                    isUnderline = underline ?: current.isUnderline,
                    isStrikethrough = strikethrough ?: current.isStrikethrough,
                    textColorHex = if (clearColors) null else (textColor ?: current.textColorHex),
                    highlightColorHex = if (clearColors) null else (highlightColor ?: current.highlightColorHex)
                )
                existingSpans[matchIdx] = updated
            } else {
                val newSpan = InlineSpan(
                    start = clampedStart,
                    end = clampedEnd,
                    isBold = bold ?: false,
                    isItalic = italic ?: false,
                    isUnderline = underline ?: false,
                    isStrikethrough = strikethrough ?: false,
                    textColorHex = textColor,
                    highlightColorHex = highlightColor
                )
                existingSpans.add(newSpan)
            }

            return this.copy(spans = existingSpans)
        }

        /**
         * Adjusts span offsets when text is typed or deleted inside the block.
         */
        fun adjustSpansForTextChange(newText: String): TextBlock {
            val validSpans = spans.mapNotNull { span ->
                if (span.start >= newText.length) null
                else span.copy(
                    start = span.start.coerceIn(0, newText.length),
                    end = span.end.coerceIn(span.start, newText.length)
                )
            }.filter { it.start < it.end }
            return this.copy(text = newText, spans = validSpans)
        }
    }

    data class ImageBlock(
        val blockId: String = UUID.randomUUID().toString(),
        var imageUri: String = "",
        var caption: String = "",
        var widthRatio: Float = 1.0f
    ) : DocElement(blockId, ElementType.IMAGE) {
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", blockId)
                put("type", ElementType.IMAGE.name)
                put("imageUri", imageUri)
                put("caption", caption)
                put("widthRatio", widthRatio.toDouble())
            }
        }
    }

    data class TableBlock(
        val blockId: String = UUID.randomUUID().toString(),
        var title: String = "",
        var headers: List<String> = listOf("Column 1", "Column 2"),
        var rows: List<List<String>> = listOf(listOf("Data 1", "Data 2")),
        var hasHeaderRow: Boolean = true
    ) : DocElement(blockId, ElementType.TABLE) {
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", blockId)
                put("type", ElementType.TABLE.name)
                put("title", title)
                put("headers", JSONArray(headers))
                val rowsArray = JSONArray()
                rows.forEach { row ->
                    rowsArray.put(JSONArray(row))
                }
                put("rows", rowsArray)
                put("hasHeaderRow", hasHeaderRow)
            }
        }
    }

    data class DiagramBlock(
        val blockId: String = UUID.randomUUID().toString(),
        var diagramType: DiagramType = DiagramType.FLOWCHART,
        var title: String = "Process Diagram",
        var nodes: List<String> = listOf("Input", "Processing", "Output"),
        var structuredNodes: List<DiagramNode> = emptyList(),
        var edges: List<DiagramEdge> = emptyList(),
        var rawContent: String = ""
    ) : DocElement(blockId, ElementType.DIAGRAM) {
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", blockId)
                put("type", ElementType.DIAGRAM.name)
                put("diagramType", diagramType.name)
                put("title", title)
                put("nodes", JSONArray(nodes))
                if (structuredNodes.isNotEmpty()) {
                    val sNodesArray = JSONArray()
                    structuredNodes.forEach { sNodesArray.put(it.toJson()) }
                    put("structuredNodes", sNodesArray)
                }
                if (edges.isNotEmpty()) {
                    val edgesArray = JSONArray()
                    edges.forEach { edgesArray.put(it.toJson()) }
                    put("edges", edgesArray)
                }
                put("rawContent", rawContent)
            }
        }
    }

    data class ChartBlock(
        val blockId: String = UUID.randomUUID().toString(),
        var chartType: ChartType = ChartType.BAR,
        var title: String = "Study Data Chart",
        var subtitle: String = "",
        var entries: List<ChartEntry> = listOf(
            ChartEntry("Topic A", 45f, "#6C63D9"),
            ChartEntry("Topic B", 75f, "#2563EB"),
            ChartEntry("Topic C", 60f, "#059669")
        )
    ) : DocElement(blockId, ElementType.CHART) {
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", blockId)
                put("type", ElementType.CHART.name)
                put("chartType", chartType.name)
                put("title", title)
                put("subtitle", subtitle)
                val entriesArray = JSONArray()
                entries.forEach { entriesArray.put(it.toJson()) }
                put("entries", entriesArray)
            }
        }
    }

    data class CalloutBlock(
        val blockId: String = UUID.randomUUID().toString(),
        var calloutType: CalloutType = CalloutType.IMPORTANT,
        var title: String = "Important",
        var content: String = ""
    ) : DocElement(blockId, ElementType.CALLOUT) {
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", blockId)
                put("type", ElementType.CALLOUT.name)
                put("calloutType", calloutType.name)
                put("title", title)
                put("content", content)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DocElement? {
            val typeStr = json.optString("type")
            val id = json.optString("id", UUID.randomUUID().toString())
            return when (typeStr) {
                ElementType.HEADING_1.name,
                ElementType.HEADING_2.name,
                ElementType.HEADING_3.name,
                ElementType.HEADING_4.name,
                ElementType.PARAGRAPH.name,
                ElementType.BULLET_LIST.name,
                ElementType.NUMBERED_LIST.name -> {
                    val elementEnum = try {
                        ElementType.valueOf(typeStr)
                    } catch (e: Exception) {
                        ElementType.PARAGRAPH
                    }
                    val textColor = json.optString("textColorHex").takeIf { it.isNotBlank() }
                    val highlightColor = json.optString("highlightColorHex").takeIf { it.isNotBlank() }
                    val spansArray = json.optJSONArray("spans")
                    val spansList = mutableListOf<InlineSpan>()
                    if (spansArray != null) {
                        for (s in 0 until spansArray.length()) {
                            val sObj = spansArray.optJSONObject(s)
                            if (sObj != null) {
                                spansList.add(InlineSpan.fromJson(sObj))
                            }
                        }
                    }
                    TextBlock(
                        blockId = id,
                        blockType = elementEnum,
                        text = json.optString("text", ""),
                        isBold = json.optBoolean("isBold", false),
                        isItalic = json.optBoolean("isItalic", false),
                        isUnderline = json.optBoolean("isUnderline", false),
                        isStrikethrough = json.optBoolean("isStrikethrough", false),
                        textColorHex = textColor,
                        highlightColorHex = highlightColor,
                        alignment = json.optString("alignment", "LEFT"),
                        spans = spansList
                    )
                }
                ElementType.IMAGE.name -> {
                    ImageBlock(
                        blockId = id,
                        imageUri = json.optString("imageUri", ""),
                        caption = json.optString("caption", ""),
                        widthRatio = json.optDouble("widthRatio", 1.0).toFloat()
                    )
                }
                ElementType.TABLE.name -> {
                    val headersArray = json.optJSONArray("headers") ?: JSONArray()
                    val headers = mutableListOf<String>()
                    for (i in 0 until headersArray.length()) {
                        headers.add(headersArray.optString(i))
                    }
                    val rowsArray = json.optJSONArray("rows") ?: JSONArray()
                    val rows = mutableListOf<List<String>>()
                    for (i in 0 until rowsArray.length()) {
                        val rowArray = rowsArray.optJSONArray(i) ?: JSONArray()
                        val row = mutableListOf<String>()
                        for (j in 0 until rowArray.length()) {
                            row.add(rowArray.optString(j))
                        }
                        rows.add(row)
                    }
                    TableBlock(
                        blockId = id,
                        title = json.optString("title", ""),
                        headers = if (headers.isEmpty()) listOf("Col 1", "Col 2") else headers,
                        rows = if (rows.isEmpty()) listOf(listOf("Val 1", "Val 2")) else rows,
                        hasHeaderRow = json.optBoolean("hasHeaderRow", true)
                    )
                }
                ElementType.DIAGRAM.name -> {
                    val diagTypeStr = json.optString("diagramType", DiagramType.FLOWCHART.name)
                    val diagType = try {
                        DiagramType.valueOf(diagTypeStr)
                    } catch (e: Exception) {
                        DiagramType.FLOWCHART
                    }
                    val nodesArray = json.optJSONArray("nodes") ?: JSONArray()
                    val nodes = mutableListOf<String>()
                    for (i in 0 until nodesArray.length()) {
                        nodes.add(nodesArray.optString(i))
                    }
                    val sNodesArray = json.optJSONArray("structuredNodes")
                    val sNodes = mutableListOf<DiagramNode>()
                    if (sNodesArray != null) {
                        for (i in 0 until sNodesArray.length()) {
                            val nObj = sNodesArray.optJSONObject(i)
                            if (nObj != null) sNodes.add(DiagramNode.fromJson(nObj))
                        }
                    }
                    val edgesArray = json.optJSONArray("edges")
                    val edges = mutableListOf<DiagramEdge>()
                    if (edgesArray != null) {
                        for (i in 0 until edgesArray.length()) {
                            val eObj = edgesArray.optJSONObject(i)
                            if (eObj != null) edges.add(DiagramEdge.fromJson(eObj))
                        }
                    }
                    DiagramBlock(
                        blockId = id,
                        diagramType = diagType,
                        title = json.optString("title", "Process Diagram"),
                        nodes = if (nodes.isEmpty()) listOf("Input", "Processing", "Output") else nodes,
                        structuredNodes = sNodes,
                        edges = edges,
                        rawContent = json.optString("rawContent", "")
                    )
                }
                ElementType.CHART.name -> {
                    val chartTypeStr = json.optString("chartType", ChartType.BAR.name)
                    val chartType = try {
                        ChartType.valueOf(chartTypeStr)
                    } catch (e: Exception) {
                        ChartType.BAR
                    }
                    val entriesArray = json.optJSONArray("entries")
                    val entries = mutableListOf<ChartEntry>()
                    if (entriesArray != null) {
                        for (i in 0 until entriesArray.length()) {
                            val eObj = entriesArray.optJSONObject(i)
                            if (eObj != null) entries.add(ChartEntry.fromJson(eObj))
                        }
                    }
                    ChartBlock(
                        blockId = id,
                        chartType = chartType,
                        title = json.optString("title", "Study Data Chart"),
                        subtitle = json.optString("subtitle", ""),
                        entries = if (entries.isEmpty()) listOf(
                            ChartEntry("Topic A", 45f, "#6C63D9"),
                            ChartEntry("Topic B", 75f, "#2563EB"),
                            ChartEntry("Topic C", 60f, "#059669")
                        ) else entries
                    )
                }
                ElementType.CALLOUT.name -> {
                    val calloutStr = json.optString("calloutType", CalloutType.IMPORTANT.name)
                    val calloutType = try {
                        CalloutType.valueOf(calloutStr)
                    } catch (e: Exception) {
                        CalloutType.IMPORTANT
                    }
                    CalloutBlock(
                        blockId = id,
                        calloutType = calloutType,
                        title = json.optString("title", "Important"),
                        content = json.optString("content", "")
                    )
                }
                else -> null
            }
        }
    }
}

data class ChapterTopic(
    val topicId: String,
    val chapterId: String,
    val topicTitle: String,
    val topicOrder: Int,
    val headingElementId: String,
    val elements: List<DocElement> = emptyList()
) {
    fun toPlainText(): String {
        return elements.joinToString("\n\n") { element ->
            when (element) {
                is DocElement.TextBlock -> element.text
                is DocElement.CalloutBlock -> "[${element.calloutType.title}: ${element.title}]\n${element.content}"
                is DocElement.TableBlock -> "${element.title}\n" + (if (element.hasHeaderRow) element.headers.joinToString(" | ") + "\n" else "") + element.rows.joinToString("\n") { it.joinToString(" | ") }
                is DocElement.DiagramBlock -> "[${element.diagramType.title}: ${element.title}]\n" + element.rawContent.ifBlank { element.nodes.joinToString(" -> ") }
                is DocElement.ChartBlock -> "[Chart (${element.chartType.title}): ${element.title}]\n" + element.entries.joinToString("\n") { "${it.label}: ${it.value}" }
                is DocElement.ImageBlock -> "[Image: ${element.caption}]"
            }
        }.trim()
    }
}

data class PrepDocument(
    val chapterId: String,
    val title: String,
    val elements: MutableList<DocElement> = mutableListOf()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("chapterId", chapterId)
        root.put("title", title)
        val elementsArray = JSONArray()
        elements.forEach { elem ->
            elementsArray.put(elem.toJson())
        }
        root.put("elements", elementsArray)
        return root.toString(2)
    }

    fun toPlainText(): String {
        return elements.joinToString("\n\n") { element ->
            when (element) {
                is DocElement.TextBlock -> element.text
                is DocElement.CalloutBlock -> "[${element.calloutType.title}: ${element.title}]\n${element.content}"
                is DocElement.TableBlock -> "${element.title}\n" + (if (element.hasHeaderRow) element.headers.joinToString(" | ") + "\n" else "") + element.rows.joinToString("\n") { it.joinToString(" | ") }
                is DocElement.DiagramBlock -> "[${element.diagramType.title}: ${element.title}]\n" + element.rawContent.ifBlank { element.nodes.joinToString(" -> ") }
                is DocElement.ChartBlock -> "[Chart (${element.chartType.title}): ${element.title}]\n" + element.entries.joinToString("\n") { "${it.label}: ${it.value}" }
                is DocElement.ImageBlock -> "[Image: ${element.caption}]"
            }
        }.trim()
    }

    /**
     * Scans and extracts all H2 headings as independent first-class topics.
     * Elements following each H2 up to the next H2 belong exclusively to that topic.
     */
    fun extractTopics(): List<ChapterTopic> {
        val topics = mutableListOf<ChapterTopic>()
        var currentTopicElements = mutableListOf<DocElement>()
        var currentHeadingBlock: DocElement.TextBlock? = null
        var topicIndex = 0

        // Check if there are introductory elements before the first H2
        val firstH2Index = elements.indexOfFirst { it is DocElement.TextBlock && it.blockType == ElementType.HEADING_2 }

        if (firstH2Index > 0) {
            val introElements = elements.subList(0, firstH2Index)
            val firstH1 = introElements.filterIsInstance<DocElement.TextBlock>().firstOrNull { it.blockType == ElementType.HEADING_1 }
            val introTitle = firstH1?.text?.ifBlank { "Overview & Introduction" } ?: "Overview & Introduction"
            topics.add(
                ChapterTopic(
                    topicId = "topic_000",
                    chapterId = chapterId,
                    topicTitle = introTitle,
                    topicOrder = 0,
                    headingElementId = firstH1?.blockId ?: introElements.first().id,
                    elements = introElements.toList()
                )
            )
        }

        for (element in elements) {
            if (element is DocElement.TextBlock && element.blockType == ElementType.HEADING_2) {
                if (currentHeadingBlock != null) {
                    topicIndex++
                    topics.add(
                        ChapterTopic(
                            topicId = String.format("topic_%03d", topicIndex),
                            chapterId = chapterId,
                            topicTitle = cleanTopicHeading(currentHeadingBlock.text),
                            topicOrder = topicIndex,
                            headingElementId = currentHeadingBlock.blockId,
                            elements = currentTopicElements.toList()
                        )
                    )
                    currentTopicElements = mutableListOf()
                }
                currentHeadingBlock = element
                currentTopicElements.add(element)
            } else {
                if (currentHeadingBlock != null) {
                    currentTopicElements.add(element)
                }
            }
        }

        if (currentHeadingBlock != null) {
            topicIndex++
            topics.add(
                ChapterTopic(
                    topicId = String.format("topic_%03d", topicIndex),
                    chapterId = chapterId,
                    topicTitle = cleanTopicHeading(currentHeadingBlock.text),
                    topicOrder = topicIndex,
                    headingElementId = currentHeadingBlock.blockId,
                    elements = currentTopicElements.toList()
                )
            )
        }

        // Fallback for documents with no H2 headings
        if (topics.isEmpty() && elements.isNotEmpty()) {
            val h1 = elements.filterIsInstance<DocElement.TextBlock>().firstOrNull { it.blockType == ElementType.HEADING_1 }
            val titleText = h1?.text?.ifBlank { title } ?: title
            topics.add(
                ChapterTopic(
                    topicId = "topic_001",
                    chapterId = chapterId,
                    topicTitle = titleText,
                    topicOrder = 1,
                    headingElementId = h1?.blockId ?: elements.first().id,
                    elements = elements.toList()
                )
            )
        }

        return topics
    }

    /**
     * Updates specific topic content without rewriting the whole chapter document.
     */
    fun replaceTopicContent(topicId: String, newContentBlocks: List<DocElement>): PrepDocument {
        val topics = extractTopics()
        val targetTopic = topics.find { it.topicId == topicId } ?: return this
        val headingId = targetTopic.headingElementId

        val headingIndex = elements.indexOfFirst { it.id == headingId }
        if (headingIndex < 0) return this

        // Find the next H2 heading index or end of document
        var nextHeadingIndex = elements.size
        for (i in (headingIndex + 1) until elements.size) {
            val el = elements[i]
            if (el is DocElement.TextBlock && el.blockType == ElementType.HEADING_2) {
                nextHeadingIndex = i
                break
            }
        }

        val newElements = elements.toMutableList()
        val containsH2 = newContentBlocks.any { it is DocElement.TextBlock && it.blockType == ElementType.HEADING_2 }
        val removeStart = if (containsH2) headingIndex else headingIndex + 1
        val removeCount = (nextHeadingIndex - removeStart).coerceAtLeast(0)

        repeat(removeCount) {
            if (removeStart < newElements.size) {
                newElements.removeAt(removeStart)
            }
        }

        newElements.addAll(removeStart, newContentBlocks)
        return copy(elements = newElements)
    }

    /**
     * Inserts new content blocks at the end of a specific topic before the next H2.
     */
    fun insertIntoTopic(topicId: String, newContentBlocks: List<DocElement>): PrepDocument {
        val topics = extractTopics()
        val targetTopic = topics.find { it.topicId == topicId } ?: return this
        val headingId = targetTopic.headingElementId

        val headingIndex = elements.indexOfFirst { it.id == headingId }
        if (headingIndex < 0) return this

        var nextHeadingIndex = elements.size
        for (i in (headingIndex + 1) until elements.size) {
            val el = elements[i]
            if (el is DocElement.TextBlock && el.blockType == ElementType.HEADING_2) {
                nextHeadingIndex = i
                break
            }
        }

        val newElements = elements.toMutableList()
        newElements.addAll(nextHeadingIndex, newContentBlocks)
        return copy(elements = newElements)
    }

    /**
     * Duplicates the topic content as a new section "[Topic Title] (AI Enhanced)"
     * leaving the original topic completely untouched.
     */
    fun duplicateTopicContent(topicId: String, newContentBlocks: List<DocElement>): PrepDocument {
        val topics = extractTopics()
        val targetTopic = topics.find { it.topicId == topicId } ?: return this
        val headingId = targetTopic.headingElementId

        val headingIndex = elements.indexOfFirst { it.id == headingId }
        if (headingIndex < 0) return this

        var nextHeadingIndex = elements.size
        for (i in (headingIndex + 1) until elements.size) {
            val el = elements[i]
            if (el is DocElement.TextBlock && el.blockType == ElementType.HEADING_2) {
                nextHeadingIndex = i
                break
            }
        }

        val hasH2 = newContentBlocks.any { it is DocElement.TextBlock && it.blockType == ElementType.HEADING_2 }
        val blocksToInsert = if (hasH2) {
            newContentBlocks
        } else {
            val duplicateHeader = DocElement.TextBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = ElementType.HEADING_2,
                text = "${targetTopic.topicTitle} (AI Enhanced)"
            )
            listOf(duplicateHeader) + newContentBlocks
        }

        val newElements = elements.toMutableList()
        newElements.addAll(nextHeadingIndex, blocksToInsert)
        return copy(elements = newElements)
    }

    fun normalizeIfContainsRawMarkdown(): PrepDocument {
        val needsParsing = elements.any { el ->
            if (el is DocElement.TextBlock) {
                val t = el.text.trim()
                t.startsWith("#") ||
                t.startsWith("---") ||
                t.startsWith("***") ||
                t.startsWith("> [!") ||
                t.startsWith("! [!") ||
                (el.blockType == ElementType.PARAGRAPH && (t.startsWith("* ") || t.startsWith("- ") || t.startsWith("• ") || t.matches(Regex("""^\d+[\.\)]\s+.*""")) || t.contains("\n#") || t.contains("\n-") || t.contains("\n*") || t.contains("\n")))
            } else false
        }

        if (!needsParsing) return this

        val newElements = mutableListOf<DocElement>()
        for (el in elements) {
            if (el is DocElement.TextBlock) {
                val t = el.text
                val isRawMarkdown = t.startsWith("#") || t.contains("\n#") || t.startsWith("---") || t.contains("\n") || t.startsWith("* ") || t.startsWith("- ") || t.startsWith("> [!") || t.startsWith("! [!")
                if (isRawMarkdown) {
                    val parsed = com.example.ui.editor.PasteProcessor.parseRichTextToBlocks(t)
                    if (parsed.isNotEmpty()) {
                        newElements.addAll(parsed)
                    } else {
                        newElements.add(el)
                    }
                } else {
                    val (clean, spans) = if (el.spans.isEmpty() && (t.contains("**") || t.contains("==") || t.contains("~~") || t.contains("<mark>"))) {
                        com.example.ui.editor.PasteProcessor.parseInlineFormatting(t)
                    } else {
                        Pair(t, el.spans)
                    }
                    newElements.add(el.copy(text = clean, spans = spans))
                }
            } else {
                newElements.add(el)
            }
        }
        return this.copy(elements = newElements)
    }

    companion object {
        fun cleanTopicHeading(raw: String): String {
            return raw
                .replace(Regex("^(?:[Hh]2[:.-]?|#{1,6}\\s*|Topic\\s*\\d*[:.-]?)\\s*"), "")
                .trim()
                .ifBlank { raw.trim() }
        }

        fun fromJson(jsonStr: String, defaultChapterId: String = "", defaultTitle: String = "Untitled Chapter"): PrepDocument {
            if (jsonStr.isBlank()) {
                return createInitialDocument(defaultChapterId, defaultTitle)
            }
            return try {
                val root = JSONObject(jsonStr)
                val chapterId = root.optString("chapterId", defaultChapterId)
                val title = root.optString("title", defaultTitle)
                val elementsArray = root.optJSONArray("elements") ?: JSONArray()
                val list = mutableListOf<DocElement>()
                for (i in 0 until elementsArray.length()) {
                    val itemObj = elementsArray.optJSONObject(i)
                    if (itemObj != null) {
                        val elem = DocElement.fromJson(itemObj)
                        if (elem != null) {
                            list.add(elem)
                        }
                    }
                }
                if (list.isEmpty()) {
                    list.add(DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = ""))
                }
                val rawDoc = PrepDocument(chapterId, title, list)
                rawDoc.normalizeIfContainsRawMarkdown()
            } catch (e: Exception) {
                createInitialDocument(defaultChapterId, defaultTitle)
            }
        }

        fun createInitialDocument(chapterId: String, title: String): PrepDocument {
            return PrepDocument(
                chapterId = chapterId,
                title = title,
                elements = mutableListOf(
                    DocElement.TextBlock(blockType = ElementType.PARAGRAPH, text = "")
                )
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class QuestionItem(
    val id: String = UUID.randomUUID().toString(),
    val questionText: String,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = -1,
    val explanation: String = "",
    val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val tags: List<String> = emptyList(),
    val examSource: String = "",
    val userSelectedOptionIndex: Int? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("questionText", questionText)
            put("options", JSONArray(options))
            put("correctOptionIndex", correctOptionIndex)
            put("explanation", explanation)
            put("difficulty", difficulty)
            put("tags", JSONArray(tags))
            put("examSource", examSource)
            if (userSelectedOptionIndex != null) {
                put("userSelectedOptionIndex", userSelectedOptionIndex)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): QuestionItem {
            val opts = mutableListOf<String>()
            val optsArr = json.optJSONArray("options")
            if (optsArr != null) {
                for (i in 0 until optsArr.length()) {
                    opts.add(optsArr.optString(i))
                }
            }
            val tagsList = mutableListOf<String>()
            val tagsArr = json.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    tagsList.add(tagsArr.optString(i))
                }
            }
            val userSelected = if (json.has("userSelectedOptionIndex") && !json.isNull("userSelectedOptionIndex")) {
                json.optInt("userSelectedOptionIndex")
            } else null

            return QuestionItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                questionText = json.optString("questionText", ""),
                options = opts,
                correctOptionIndex = json.optInt("correctOptionIndex", -1),
                explanation = json.optString("explanation", ""),
                difficulty = json.optString("difficulty", "MEDIUM"),
                tags = tagsList,
                examSource = json.optString("examSource", ""),
                userSelectedOptionIndex = userSelected
            )
        }
    }
}

data class SanitizedChapterResult(
    val title: String,
    val summary: String,
    val elements: List<DocElement>,
    val questions: List<QuestionItem> = emptyList(),
    val isAiGenerated: Boolean = false
)
