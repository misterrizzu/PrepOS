package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.components.RichMathText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.ElementType
import com.example.model.InlineSpan
import com.example.model.NoteFont
import com.example.model.PaperStyle
import com.example.model.PaperTheme
import com.example.model.PrepDocument
import com.example.model.Quadruple
import com.example.ui.paper.PaperSurface
import com.example.ui.paper.getPaperColors
import com.example.util.MathNotationHelper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import java.util.UUID

@Composable
fun ContinuousDocumentView(
    document: PrepDocument,
    isEditMode: Boolean,
    paperStyle: PaperStyle,
    paperTheme: PaperTheme,
    fontFamily: NoteFont,
    fontSizeSp: Float,
    lineSpacing: Float,
    initialScrollY: Int = 0,
    targetScrollBlockId: String? = null,
    onScrollToBlockHandled: () -> Unit = {},
    onScrollProgressChanged: (progress: Float, scrollY: Int) -> Unit,
    onDocumentChanged: (PrepDocument) -> Unit,
    onSelectionChanged: (selectedText: String, activeBlockId: String?, startOffset: Int, endOffset: Int) -> Unit,
    onRequestEditMode: () -> Unit,
    onAskAiForDiagram: ((DocElement.DiagramBlock) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val paperColors = getPaperColors(paperTheme)

    // Focus & Cursor coordination across blocks
    var targetFocusBlockId by remember { mutableStateOf<String?>(null) }
    var targetCursorPosition by remember { mutableStateOf<Int?>(null) }
    var highlightedBlockId by remember { mutableStateOf<String?>(null) }
    var aiEditBlock by remember { mutableStateOf<DocElement?>(null) }

    // Map of block ID to relative Y position within the document column
    val blockYPositions = remember { mutableMapOf<String, Int>() }

    // Jump to topic / block smoothly
    LaunchedEffect(targetScrollBlockId) {
        if (targetScrollBlockId != null) {
            val yPos = blockYPositions[targetScrollBlockId]
            if (yPos != null) {
                highlightedBlockId = targetScrollBlockId
                scrollState.animateScrollTo((yPos - 40).coerceAtLeast(0))
                onScrollToBlockHandled()
                kotlinx.coroutines.delay(1800)
                highlightedBlockId = null
            }
        }
    }

    // Restore reading scroll position on first load (wait until layout height is measured)
    var hasRestoredInitialScroll by remember(document.chapterId) {
        mutableStateOf(false)
    }

    // Attempt to restore scroll position once layout measurement has completed
    LaunchedEffect(document.chapterId, scrollState.maxValue) {
        if (!hasRestoredInitialScroll) {
            val max = scrollState.maxValue
            if (max > 0 && max < Int.MAX_VALUE) {
                if (initialScrollY > 0) {
                    scrollState.scrollTo(initialScrollY.coerceAtMost(max))
                }
                hasRestoredInitialScroll = true
            } else if (max == 0 && document.elements.isEmpty()) {
                hasRestoredInitialScroll = true
            }
        }
    }

    // User gesture takes precedence over programmatic initial restoration
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            hasRestoredInitialScroll = true
        }
    }

    // Reading Progress calculation: listen to scrollState changes
    LaunchedEffect(scrollState, hasRestoredInitialScroll) {
        if (!hasRestoredInitialScroll) return@LaunchedEffect

        snapshotFlow {
            val maxScroll = scrollState.maxValue
            val currentScroll = scrollState.value
            if (maxScroll > 0 && maxScroll < Int.MAX_VALUE) {
                val rawRatio = (currentScroll.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
                val effectiveRatio = if (rawRatio >= 0.95f || currentScroll >= maxScroll - 60) {
                    1.0f
                } else {
                    rawRatio
                }
                effectiveRatio to currentScroll
            } else if (maxScroll == 0) {
                1.0f to 0
            } else {
                null
            }
        }
        .filterNotNull()
        .distinctUntilChanged { old, new ->
            val oldPct = (old.first * 100).toInt()
            val newPct = (new.first * 100).toInt()
            oldPct == newPct && kotlin.math.abs(old.second - new.second) < 15
        }
        .collect { (progress, scroll) ->
            onScrollProgressChanged(progress, scroll)
        }
    }

    val composeFontFamily = when (fontFamily) {
        NoteFont.SANS_SERIF -> FontFamily.SansSerif
        NoteFont.SERIF -> FontFamily.Serif
        NoteFont.MONOSPACE -> FontFamily.Monospace
        NoteFont.HANDWRITTEN -> FontFamily.Cursive
    }

    PaperSurface(
        modifier = modifier
            .fillMaxSize()
            .testTag("document_surface"),
        paperStyle = paperStyle,
        paperTheme = paperTheme,
        lineSpacing = (fontSizeSp * lineSpacing * 1.5f).dp
    ) {
        val content = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pointerInput(isEditMode) {
                        if (!isEditMode) {
                            detectTapGestures(
                                onDoubleTap = { onRequestEditMode() }
                            )
                        }
                    }
                    .padding(start = 28.dp, end = 12.dp, top = 12.dp, bottom = if (isEditMode) 140.dp else 60.dp)
            ) {
                document.elements.forEachIndexed { index, element ->
                    val isHighlighted = highlightedBlockId == element.id
                    val elementModifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val positionInParent = coordinates.positionInParent()
                            blockYPositions[element.id] = positionInParent.y.toInt()
                        }
                        .then(
                            if (isHighlighted) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                            } else Modifier
                        )

                    when (element) {
                        is DocElement.TextBlock -> {
                            val isFocused = targetFocusBlockId == element.blockId
                            val listNumber = if (element.blockType == ElementType.NUMBERED_LIST) {
                                var count = 1
                                var p = index - 1
                                while (p >= 0 && document.elements[p] is DocElement.TextBlock && (document.elements[p] as DocElement.TextBlock).blockType == ElementType.NUMBERED_LIST) {
                                    count++
                                    p--
                                }
                                count
                            } else 1

                            SingleTextBlockEditor(
                                block = element,
                                isEditMode = isEditMode,
                                isTargetFocused = isFocused,
                                initialCursorPosition = if (isFocused) targetCursorPosition else null,
                                fontFamily = composeFontFamily,
                                baseFontSizeSp = fontSizeSp,
                                lineSpacingMultiplier = lineSpacing,
                                defaultTextColor = paperColors.textColor,
                                listNumber = listNumber,
                                modifier = elementModifier,
                                onUpdate = { updatedBlock ->
                                    val elements = document.elements.toMutableList()
                                    elements[index] = updatedBlock
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onPasteBlocks = { newBlocks ->
                                    val elements = document.elements.toMutableList()
                                    elements.removeAt(index)
                                    elements.addAll(index, newBlocks)
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onDelete = {
                                    if (document.elements.size > 1) {
                                        val elements = document.elements.toMutableList().apply { removeAt(index) }
                                        onDocumentChanged(document.copy(elements = elements))
                                    }
                                },
                                onSelectionChanged = { text, start, end ->
                                    onSelectionChanged(text, element.blockId, start, end)
                                },
                                onEnterSplit = { beforeText, afterText ->
                                    val elements = document.elements.toMutableList()
                                    elements[index] = element.copy(text = beforeText).adjustSpansForTextChange(beforeText)
                                    val newBlockId = UUID.randomUUID().toString()
                                    val newBlock = DocElement.TextBlock(
                                        blockId = newBlockId,
                                        blockType = ElementType.PARAGRAPH,
                                        text = afterText,
                                        alignment = element.alignment
                                    )
                                    elements.add(index + 1, newBlock)
                                    targetFocusBlockId = newBlockId
                                    targetCursorPosition = 0
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onMergeWithPrevious = {
                                    if (index > 0) {
                                        val prevElement = document.elements[index - 1]
                                        if (prevElement is DocElement.TextBlock) {
                                            val oldPrevLen = prevElement.text.length
                                            val mergedText = prevElement.text + element.text
                                            val elements = document.elements.toMutableList()
                                            elements[index - 1] = prevElement.copy(text = mergedText)
                                            elements.removeAt(index)
                                            targetFocusBlockId = prevElement.blockId
                                            targetCursorPosition = oldPrevLen
                                            onDocumentChanged(document.copy(elements = elements))
                                        } else {
                                            // Previous is a non-text block, if current is empty, delete current
                                            if (element.text.isEmpty() && document.elements.size > 1) {
                                                val elements = document.elements.toMutableList().apply { removeAt(index) }
                                                onDocumentChanged(document.copy(elements = elements))
                                            }
                                        }
                                    }
                                },
                                onMergeWithNext = {
                                    if (index + 1 < document.elements.size) {
                                        val nextElement = document.elements[index + 1]
                                        if (nextElement is DocElement.TextBlock) {
                                            val curLen = element.text.length
                                            val mergedText = element.text + nextElement.text
                                            val elements = document.elements.toMutableList()
                                            elements[index] = element.copy(text = mergedText)
                                            elements.removeAt(index + 1)
                                            targetFocusBlockId = element.blockId
                                            targetCursorPosition = curLen
                                            onDocumentChanged(document.copy(elements = elements))
                                        }
                                    }
                                },
                                onFocusHandled = {
                                    if (targetFocusBlockId == element.blockId) {
                                        targetFocusBlockId = null
                                        targetCursorPosition = null
                                    }
                                }
                            )
                        }
                        is DocElement.CalloutBlock -> {
                            CalloutCard(
                                block = element,
                                isEditMode = isEditMode,
                                onUpdate = { updated ->
                                    val elements = document.elements.toMutableList()
                                    elements[index] = updated
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onDelete = {
                                    val elements = document.elements.toMutableList().apply { removeAt(index) }
                                    onDocumentChanged(document.copy(elements = elements))
                                }
                            )
                        }
                        is DocElement.DiagramBlock -> {
                            DiagramCard(
                                block = element,
                                isEditMode = isEditMode,
                                onUpdate = { updated ->
                                    val elements = document.elements.toMutableList()
                                    elements[index] = updated
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onDelete = {
                                    val elements = document.elements.toMutableList().apply { removeAt(index) }
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onAiEdit = {
                                    if (onAskAiForDiagram != null) {
                                        onAskAiForDiagram(it)
                                    } else {
                                        aiEditBlock = it
                                    }
                                }
                            )
                        }
                        is DocElement.TableBlock -> {
                            TableCard(
                                block = element,
                                isEditMode = isEditMode,
                                onUpdate = { updated ->
                                    val elements = document.elements.toMutableList()
                                    elements[index] = updated
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onDelete = {
                                    val elements = document.elements.toMutableList().apply { removeAt(index) }
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onAiEdit = { tableBlock ->
                                    if (onAskAiForDiagram != null) {
                                        val diagramRepresentation = DocElement.DiagramBlock(
                                            blockId = tableBlock.blockId,
                                            title = tableBlock.title.ifBlank { "Table Data" },
                                            diagramType = DiagramType.COMPARISON,
                                            nodes = if (tableBlock.headers.isNotEmpty()) {
                                                listOf("Columns: " + tableBlock.headers.joinToString(" | ")) +
                                                    tableBlock.rows.map { it.joinToString(" | ") }
                                            } else {
                                                tableBlock.rows.map { it.joinToString(" | ") }
                                            }
                                        )
                                        onAskAiForDiagram(diagramRepresentation)
                                    } else {
                                        aiEditBlock = tableBlock
                                    }
                                }
                            )
                        }
                        is DocElement.ChartBlock -> {
                            ChartCard(
                                block = element,
                                isEditMode = isEditMode,
                                onUpdate = { updated ->
                                    val elements = document.elements.toMutableList()
                                    elements[index] = updated
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onDelete = {
                                    val elements = document.elements.toMutableList().apply { removeAt(index) }
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onAiEdit = { chartBlock ->
                                    if (onAskAiForDiagram != null) {
                                        val diagramRepresentation = DocElement.DiagramBlock(
                                            blockId = chartBlock.blockId,
                                            title = chartBlock.title.ifBlank { "Chart: ${chartBlock.chartType.name}" },
                                            diagramType = DiagramType.FLOWCHART,
                                            nodes = chartBlock.entries.map { "${it.label}: ${it.value}" }
                                        )
                                        onAskAiForDiagram(diagramRepresentation)
                                    } else {
                                        aiEditBlock = chartBlock
                                    }
                                }
                            )
                        }
                        is DocElement.ImageBlock -> {
                            ImageCard(
                                block = element,
                                isEditMode = isEditMode,
                                onUpdate = { updated ->
                                    val elements = document.elements.toMutableList()
                                    elements[index] = updated
                                    onDocumentChanged(document.copy(elements = elements))
                                },
                                onDelete = {
                                    val elements = document.elements.toMutableList().apply { removeAt(index) }
                                    onDocumentChanged(document.copy(elements = elements))
                                }
                            )
                        }
                    }
                }

                // Quick add block at bottom when editing
                if (isEditMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                val newId = UUID.randomUUID().toString()
                                val elements = document.elements.toMutableList().apply {
                                    add(DocElement.TextBlock(blockId = newId, blockType = ElementType.PARAGRAPH, text = ""))
                                }
                                targetFocusBlockId = newId
                                targetCursorPosition = 0
                                onDocumentChanged(document.copy(elements = elements))
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Tap to continue writing...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        if (!isEditMode) {
            // Independent cross-block text selection container for reading mode
            SelectionContainer {
                content()
            }
        } else {
            content()
        }

        if (aiEditBlock != null) {
            AiBlockEditSheet(
                block = aiEditBlock!!,
                onApply = { newBlock ->
                    val elements = document.elements.toMutableList()
                    val targetId = aiEditBlock?.id
                    val index = elements.indexOfFirst { it.id == targetId }
                    if (index >= 0) {
                        elements[index] = newBlock
                        onDocumentChanged(document.copy(elements = elements))
                    }
                    aiEditBlock = null
                },
                onDismiss = {
                    aiEditBlock = null
                }
            )
        }
    }
}

/**
 * Builds an AnnotatedString from a TextBlock's raw text and its list of InlineSpan formatting.
 * Automatically parses inline Markdown (bold, italic, highlight, underline, strikethrough) if spans are not yet computed.
 */
fun buildAnnotatedStringFromBlock(
    rawText: String,
    spans: List<InlineSpan>,
    defaultColor: Color
): AnnotatedString {
    val (cleanText, effectiveSpans) = if (spans.isEmpty() && (rawText.contains("*") || rawText.contains("_") || rawText.contains("==") || rawText.contains("~~") || rawText.contains("<mark>") || rawText.contains("<u>"))) {
        PasteProcessor.parseInlineFormatting(rawText)
    } else {
        Pair(rawText, spans)
    }

    val builder = AnnotatedString.Builder(cleanText)
    effectiveSpans.forEach { span ->
        val start = span.start.coerceIn(0, cleanText.length)
        val end = span.end.coerceIn(start, cleanText.length)
        if (start < end) {
            val color = span.textColorHex?.let {
                try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
            }
            val bgColor = span.highlightColorHex?.let {
                try { Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.4f) } catch (e: Exception) { null }
            }
            val textDeco = when {
                span.isUnderline && span.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                span.isUnderline -> TextDecoration.Underline
                span.isStrikethrough -> TextDecoration.LineThrough
                else -> null
            }
            val spanStyle = SpanStyle(
                fontWeight = if (span.isBold) FontWeight.Bold else null,
                fontStyle = if (span.isItalic) FontStyle.Italic else null,
                color = color ?: Color.Unspecified,
                background = bgColor ?: Color.Transparent,
                textDecoration = textDeco
            )
            builder.addStyle(spanStyle, start, end)
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun SingleTextBlockEditor(
    block: DocElement.TextBlock,
    isEditMode: Boolean,
    isTargetFocused: Boolean = false,
    initialCursorPosition: Int? = null,
    fontFamily: FontFamily,
    baseFontSizeSp: Float,
    lineSpacingMultiplier: Float,
    defaultTextColor: Color,
    listNumber: Int = 1,
    onUpdate: (DocElement.TextBlock) -> Unit,
    onPasteBlocks: (List<DocElement>) -> Unit,
    onDelete: () -> Unit,
    onSelectionChanged: (selectedText: String, startOffset: Int, endOffset: Int) -> Unit,
    onEnterSplit: (before: String, after: String) -> Unit,
    onMergeWithPrevious: () -> Unit,
    onMergeWithNext: () -> Unit = {},
    onFocusHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember(block.blockId, block.text) {
        val curPos = initialCursorPosition ?: block.text.length
        mutableStateOf(
            TextFieldValue(
                text = block.text,
                selection = TextRange(curPos.coerceIn(0, block.text.length))
            )
        )
    }

    LaunchedEffect(isTargetFocused, initialCursorPosition) {
        if (isTargetFocused && isEditMode) {
            val curPos = initialCursorPosition ?: block.text.length
            textFieldValue = textFieldValue.copy(
                selection = TextRange(curPos.coerceIn(0, textFieldValue.text.length))
            )
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus requester might not be attached yet
            }
            onFocusHandled()
        }
    }

    // Precise Heading Spacing & Visual System:
    // H1: Warm Gold / Amber #D97706, 26sp (1.6x), Bold, top = 20.dp, bottom = 6.dp
    // H2: Royal Blue #2563EB, 21sp (1.35x), Bold, top = 14.dp, bottom = 5.dp
    // H3: Emerald Green #16A34A, 18sp (1.18x), SemiBold, top = 10.dp, bottom = 4.dp
    // H4: Slate Teal #0D9488, 16sp (1.05x), SemiBold, top = 8.dp, bottom = 3.dp
    // Paragraph: readable text color, base font size, Normal, top = 3.dp, bottom = 3.dp
    // Lists: compact top = 2.dp, bottom = 2.dp
    val (fontSize, fontWeight, topPadding, bottomPadding) = when (block.blockType) {
        ElementType.HEADING_1 -> Quadruple((baseFontSizeSp * 1.6f).sp, FontWeight.Bold, 20.dp, 6.dp)
        ElementType.HEADING_2 -> Quadruple((baseFontSizeSp * 1.35f).sp, FontWeight.Bold, 14.dp, 5.dp)
        ElementType.HEADING_3 -> Quadruple((baseFontSizeSp * 1.18f).sp, FontWeight.SemiBold, 10.dp, 4.dp)
        ElementType.HEADING_4 -> Quadruple((baseFontSizeSp * 1.05f).sp, FontWeight.SemiBold, 8.dp, 3.dp)
        ElementType.PARAGRAPH -> Quadruple(baseFontSizeSp.sp, if (block.isBold) FontWeight.Bold else FontWeight.Normal, 3.dp, 3.dp)
        ElementType.BULLET_LIST,
        ElementType.NUMBERED_LIST -> Quadruple(baseFontSizeSp.sp, if (block.isBold) FontWeight.Bold else FontWeight.Normal, 2.dp, 2.dp)
        else -> Quadruple(baseFontSizeSp.sp, FontWeight.Normal, 3.dp, 3.dp)
    }

    val textAlign = when (block.alignment) {
        "CENTER" -> TextAlign.Center
        "RIGHT" -> TextAlign.Right
        else -> TextAlign.Left
    }

    val headingColor = when (block.blockType) {
        ElementType.HEADING_1 -> Color(0xFFD97706) // Warm Gold / Amber
        ElementType.HEADING_2 -> Color(0xFF2563EB) // Royal Blue
        ElementType.HEADING_3 -> Color(0xFF16A34A) // Emerald Green
        ElementType.HEADING_4 -> Color(0xFF0D9488) // Slate Teal (No purple)
        else -> null
    }

    val customTextColor = block.textColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            null
        }
    } ?: headingColor ?: defaultTextColor

    val customHighlightColor = block.highlightColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.4f)
        } catch (e: Exception) {
            Color.Transparent
        }
    } ?: Color.Transparent

    val textDecoration = when {
        block.isUnderline && block.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        block.isUnderline -> TextDecoration.Underline
        block.isStrikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }

    val baseStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
        textAlign = textAlign,
        color = customTextColor,
        background = customHighlightColor,
        textDecoration = textDecoration,
        lineHeight = (fontSize.value * lineSpacingMultiplier).sp
    )

    val visualTransformation = remember(block.text, block.spans, defaultTextColor) {
        VisualTransformation { text ->
            val annotated = buildAnnotatedStringFromBlock(text.text, block.spans, defaultTextColor)
            TransformedText(annotated, OffsetMapping.Identity)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Bullet / Number prefix
            if (block.blockType == ElementType.BULLET_LIST) {
                Text(
                    text = "• ",
                    style = baseStyle.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(end = 4.dp)
                )
            } else if (block.blockType == ElementType.NUMBERED_LIST) {
                Text(
                    text = "$listNumber. ",
                    style = baseStyle.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            if (isEditMode) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val oldText = textFieldValue.text
                        val newText = newValue.text

                        // Track user selection for AI & range formatting
                        if (newValue.selection.length > 0) {
                            val selStart = newValue.selection.min
                            val selEnd = newValue.selection.max
                            val selected = newText.substring(selStart, selEnd)
                            onSelectionChanged(selected, selStart, selEnd)
                        } else {
                            val cursor = newValue.selection.start.coerceIn(0, newText.length)
                            onSelectionChanged("", cursor, cursor)
                        }

                        // Smart Paste Detection: if multiple lines or markdown blocks were pasted
                        if (newText.length - oldText.length > 10 && (newText.contains("\n") || newText.contains("|") || newText.contains("###"))) {
                            val parsedBlocks = PasteProcessor.parseRichTextToBlocks(newText)
                            if (parsedBlocks.size > 1) {
                                onPasteBlocks(parsedBlocks)
                                return@BasicTextField
                            }
                        }

                        // Check if user pressed Enter inside text -> split into new paragraph block
                        if (newText.length > oldText.length && newText.contains("\n")) {
                            val newlineIdx = newText.indexOf('\n')
                            if (newlineIdx != -1) {
                                val before = newText.substring(0, newlineIdx)
                                val after = newText.substring(newlineIdx + 1)
                                onEnterSplit(before, after)
                                return@BasicTextField
                            }
                        }

                        // Check Backspace on empty block to merge into previous
                        if (oldText.isEmpty() && newText.isEmpty()) {
                            onMergeWithPrevious()
                            return@BasicTextField
                        }

                        textFieldValue = newValue
                        onUpdate(block.adjustSpansForTextChange(newText))
                    },
                    textStyle = baseStyle,
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                val cursor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                                onSelectionChanged("", cursor, cursor)
                            }
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Backspace && textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0) {
                                onMergeWithPrevious()
                                true
                            } else if (keyEvent.key == Key.Delete && textFieldValue.selection.start == textFieldValue.text.length && textFieldValue.selection.end == textFieldValue.text.length) {
                                onMergeWithNext()
                                true
                            } else {
                                false
                            }
                        }
                        .testTag("text_editor_field_${block.blockId.take(4)}")
                )
            } else {
                val cleanDisplayText = remember(block.text, block.blockType) {
                    var t = block.text
                    if (block.blockType == ElementType.HEADING_1 || block.blockType == ElementType.HEADING_2 ||
                        block.blockType == ElementType.HEADING_3 || block.blockType == ElementType.HEADING_4) {
                        t = t.replace(Regex("""^#{1,6}\s*"""), "")
                    } else if (block.blockType == ElementType.BULLET_LIST) {
                        t = t.replace(Regex("""^(\*|\-|•|o|\+|\u2022)\s*"""), "")
                    } else if (block.blockType == ElementType.NUMBERED_LIST) {
                        t = t.replace(Regex("""^(\d+[\.\)]|\(\d+\))\s*"""), "")
                    }
                    t.ifBlank { if (isEditMode) "" else " " }
                }
                val formattedText = MathNotationHelper.formatMathNotation(cleanDisplayText)
                val hasMath = formattedText.contains("$") || formattedText.contains("\\frac") || formattedText.contains("\\sqrt") ||
                        formattedText.contains("$$") || formattedText.contains("\\begin") || formattedText.contains("^{") || formattedText.contains("_{")

                if (hasMath) {
                    RichMathText(
                        text = formattedText,
                        style = baseStyle,
                        color = customTextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    Text(
                        text = buildAnnotatedStringFromBlock(
                            formattedText,
                            block.spans,
                            defaultTextColor
                        ),
                        style = baseStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ImageCard(
    block: DocElement.ImageBlock,
    isEditMode: Boolean,
    onUpdate: (DocElement.ImageBlock) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(block.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = block.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isEditMode) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Image", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (block.caption.isNotBlank() || isEditMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = block.caption.ifBlank { "Study Diagram Reference" },
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
