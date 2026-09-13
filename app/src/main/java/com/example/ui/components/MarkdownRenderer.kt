package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.isAppDarkTheme

/**
 * High-performance, crash-safe, aesthetic Markdown and structured response renderer for Jetpack Compose.
 * Handles headings (H1-H4), tables, lists (ordered/unordered), blockquotes/callouts,
 * horizontal dividers, code fences, and rich inline text (bold, italic, code, strikethrough).
 */

sealed class MarkdownNode {
    data class Heading(val level: Int, val text: String) : MarkdownNode()
    data class Paragraph(val text: String) : MarkdownNode()
    data class BulletItem(val text: String, val level: Int = 0) : MarkdownNode()
    data class NumberedItem(val number: String, val text: String, val level: Int = 0) : MarkdownNode()
    data class BlockQuote(val lines: List<String>) : MarkdownNode()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownNode()
    data class CodeBlock(val language: String, val code: String) : MarkdownNode()
    object Divider : MarkdownNode()
}

@Composable
fun AiMarkdownMessage(
    text: String,
    modifier: Modifier = Modifier,
    isUser: Boolean = false,
    accentColor: Color = Color(0xFFC084FC),
    baseTextColor: Color? = null
) {
    val isDark = isAppDarkTheme()
    val effectiveBaseTextColor = baseTextColor ?: if (isUser) {
        Color.White
    } else {
        if (isDark) Color(0xFFF1F5F9) else LightTextPrimary
    }
    val boldColor = if (isUser) Color.White else if (isDark) Color.White else LightTextPrimary
    val codeColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val codeBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val italicColor = if (isDark) Color(0xFFE2E8F0) else LightTextSecondary

    val nodes = remember(text) { parseMarkdown(text) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Heading -> {
                    RenderHeading(node, accentColor, isDark)
                }
                is MarkdownNode.Paragraph -> {
                    RenderParagraph(node.text, effectiveBaseTextColor, boldColor, codeColor, codeBgColor, italicColor)
                }
                is MarkdownNode.BulletItem -> {
                    RenderBulletItem(node, effectiveBaseTextColor, accentColor, boldColor, codeColor, codeBgColor, italicColor)
                }
                is MarkdownNode.NumberedItem -> {
                    RenderNumberedItem(node, effectiveBaseTextColor, accentColor, boldColor, codeColor, codeBgColor, italicColor)
                }
                is MarkdownNode.BlockQuote -> {
                    RenderBlockQuote(node, accentColor, isDark)
                }
                is MarkdownNode.Table -> {
                    RenderMarkdownTable(node, accentColor, isDark)
                }
                is MarkdownNode.CodeBlock -> {
                    RenderCodeBlock(node, isDark)
                }
                is MarkdownNode.Divider -> {
                    HorizontalDivider(
                        color = if (isDark) Color(0xFF334155).copy(alpha = 0.8f) else LightBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderHeading(node: MarkdownNode.Heading, accentColor: Color, isDark: Boolean) {
    val headingColor = when (node.level) {
        1 -> if (isDark) Color.White else LightTextPrimary
        2 -> if (isDark) Color(0xFFF8FAFC) else LightTextPrimary
        3 -> accentColor
        else -> if (isDark) Color(0xFF94A3B8) else LightTextSecondary
    }
    val fontSize = when (node.level) {
        1 -> 16.5.sp
        2 -> 15.5.sp
        3 -> 14.5.sp
        else -> 13.5.sp
    }
    val topPadding = when (node.level) {
        1 -> 8.dp
        2 -> 6.dp
        3 -> 4.dp
        else -> 3.dp
    }

    Column(modifier = Modifier.padding(top = topPadding, bottom = 2.dp)) {
        Text(
            text = buildMarkdownAnnotatedString(
                text = node.text,
                baseColor = headingColor,
                boldColor = if (isDark) Color.White else LightTextPrimary,
                codeColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                codeBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = headingColor,
            lineHeight = (fontSize.value + 6).sp
        )
    }
}

@Composable
private fun RenderParagraph(
    text: String,
    baseTextColor: Color,
    boldColor: Color,
    codeColor: Color,
    codeBgColor: Color,
    italicColor: Color
) {
    Text(
        text = buildMarkdownAnnotatedString(
            text = text,
            baseColor = baseTextColor,
            boldColor = boldColor,
            codeColor = codeColor,
            codeBgColor = codeBgColor,
            italicColor = italicColor
        ),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = baseTextColor,
            fontSize = 13.5.sp,
            lineHeight = 21.sp
        )
    )
}

@Composable
private fun RenderBulletItem(
    node: MarkdownNode.BulletItem,
    baseTextColor: Color,
    accentColor: Color,
    boldColor: Color,
    codeColor: Color,
    codeBgColor: Color,
    italicColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.level * 12).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp, end = 8.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = buildMarkdownAnnotatedString(
                text = node.text,
                baseColor = baseTextColor,
                boldColor = boldColor,
                codeColor = codeColor,
                codeBgColor = codeBgColor,
                italicColor = italicColor
            ),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = baseTextColor,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RenderNumberedItem(
    node: MarkdownNode.NumberedItem,
    baseTextColor: Color,
    accentColor: Color,
    boldColor: Color,
    codeColor: Color,
    codeBgColor: Color,
    italicColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.level * 12).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${node.number}.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            modifier = Modifier.widthIn(min = 18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = buildMarkdownAnnotatedString(
                text = node.text,
                baseColor = baseTextColor,
                boldColor = boldColor,
                codeColor = codeColor,
                codeBgColor = codeBgColor,
                italicColor = italicColor
            ),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = baseTextColor,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RenderBlockQuote(node: MarkdownNode.BlockQuote, accentColor: Color, isDark: Boolean) {
    val quoteSurface = if (isDark) Color(0xFF0F172A).copy(alpha = 0.9f) else Color(0xFFF8FAFC)
    val quoteBorder = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
    val quoteText = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val quoteBold = if (isDark) Color.White else LightTextPrimary

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = quoteSurface,
        border = BorderStroke(1.dp, quoteBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // Left vertical accent stripe
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.5.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                node.lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("1. ") || trimmed.startsWith("2. ") || trimmed.startsWith("3. ") ||
                        trimmed.startsWith("4. ") || trimmed.startsWith("5. ") || trimmed.startsWith("6. ") ||
                        trimmed.startsWith("7. ") || trimmed.startsWith("8. ") || trimmed.startsWith("9. ")
                    ) {
                        val dotIdx = trimmed.indexOf('.')
                        val num = trimmed.substring(0, dotIdx).trim()
                        val rest = trimmed.substring(dotIdx + 1).trim()
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "$num.",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                modifier = Modifier.widthIn(min = 16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = buildMarkdownAnnotatedString(rest, baseColor = quoteText, boldColor = quoteBold),
                                color = quoteText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                        val rest = trimmed.substring(2).trim()
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = buildMarkdownAnnotatedString(rest, baseColor = quoteText, boldColor = quoteBold),
                                color = quoteText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = buildMarkdownAnnotatedString(line, baseColor = quoteText, boldColor = quoteBold),
                            color = quoteText,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderMarkdownTable(table: MarkdownNode.Table, accentColor: Color, isDark: Boolean) {
    if (table.headers.isEmpty() && table.rows.isEmpty()) return

    val horizontalScrollState = rememberScrollState()
    val tableSurface = if (isDark) Color(0xFF0F172A) else LightSurface
    val tableBorder = if (isDark) Color(0xFF334155) else LightBorder
    val headerBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val headerText = accentColor
    val cellTextColor = if (isDark) Color(0xFFE2E8F0) else LightTextPrimary
    val cellBoldColor = if (isDark) Color.White else LightTextPrimary
    val dividerColor = if (isDark) Color(0xFF1E293B) else LightBorder

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tableSurface,
        border = BorderStroke(1.dp, tableBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(1.dp)
        ) {
            // Header Row
            if (table.headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .background(headerBg)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    table.headers.forEachIndexed { colIdx, text ->
                        Box(
                            modifier = Modifier
                                .widthIn(min = 120.dp, max = 220.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = buildMarkdownAnnotatedString(
                                    text,
                                    baseColor = headerText,
                                    boldColor = headerText
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = headerText,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                        if (colIdx < table.headers.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(dividerColor)
                            )
                        }
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }

            // Data Rows
            table.rows.forEachIndexed { rowIndex, row ->
                val isEven = rowIndex % 2 == 0
                val rowBg = if (isDark) {
                    if (isEven) Color(0xFF0F172A) else Color(0xFF141E33)
                } else {
                    if (isEven) LightSurface else Color(0xFFF8FAFC)
                }

                Row(
                    modifier = Modifier
                        .background(rowBg)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val columnCount = maxOf(table.headers.size, row.size)
                    for (colIdx in 0 until columnCount) {
                        val cellText = row.getOrNull(colIdx) ?: ""
                        Box(
                            modifier = Modifier
                                .widthIn(min = 120.dp, max = 220.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = buildMarkdownAnnotatedString(
                                    cellText,
                                    baseColor = cellTextColor,
                                    boldColor = cellBoldColor
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = cellTextColor,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                        if (colIdx < columnCount - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(dividerColor)
                            )
                        }
                    }
                }
                if (rowIndex < table.rows.size - 1) {
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun RenderCodeBlock(node: MarkdownNode.CodeBlock, isDark: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val codeBg = if (isDark) Color(0xFF080D1A) else Color(0xFFF8FAFC)
    val headerBg = if (isDark) Color(0xFF131D33) else Color(0xFFF1F5F9)
    val borderCol = if (isDark) Color(0xFF334155) else LightBorder
    val codeTextColor = if (isDark) Color(0xFFF1F5F9) else LightTextPrimary

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = codeBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Top Language & Copy Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = node.language.ifBlank { "CODE" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(node.code))
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isDark) Color(0xFF94A3B8) else LightTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = node.code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// MARKDOWN PARSER (BLOCK LEVEL)
// ---------------------------------------------------------------------------------------------

fun parseMarkdown(rawText: String): List<MarkdownNode> {
    if (rawText.isBlank()) return emptyList()

    val lines = rawText.lines()
    val nodes = mutableListOf<MarkdownNode>()
    var i = 0
    val total = lines.size

    while (i < total) {
        val rawLine = lines[i]
        val trimmed = rawLine.trim()

        // 1. Skip empty lines
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 2. Code Block: ```lang
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < total && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < total && lines[i].trim().startsWith("```")) {
                i++ // consume closing ```
            }
            nodes.add(MarkdownNode.CodeBlock(language = lang, code = codeLines.joinToString("\n")))
            continue
        }

        // 3. Table Detection
        if (isMarkdownTableRow(trimmed)) {
            val tableLines = mutableListOf<String>()
            while (i < total && isMarkdownTableRow(lines[i].trim())) {
                tableLines.add(lines[i].trim())
                i++
            }
            val tableNode = parseMarkdownTable(tableLines)
            if (tableNode != null) {
                nodes.add(tableNode)
                continue
            }
        }

        // 4. Blockquote: > ...
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < total && lines[i].trim().startsWith(">")) {
                val cleanLine = lines[i].trim().removePrefix(">").trim()
                quoteLines.add(cleanLine)
                i++
            }
            nodes.add(MarkdownNode.BlockQuote(quoteLines))
            continue
        }

        // 5. Horizontal Divider: ---, ***, ___
        if (trimmed == "---" || trimmed == "***" || trimmed == "___" || trimmed == "- - -" || trimmed == "* * *") {
            nodes.add(MarkdownNode.Divider)
            i++
            continue
        }

        // 6. Headings
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val headingText = trimmed.drop(level).trim()
            if (level in 1..4 && headingText.isNotEmpty()) {
                nodes.add(MarkdownNode.Heading(level = level, text = headingText))
                i++
                continue
            }
        }

        // 7. Bullet Lists: * , - , + , •
        if (isBulletLine(trimmed)) {
            val indent = rawLine.takeWhile { it.isWhitespace() }.length / 2
            val bulletText = stripBulletPrefix(trimmed)
            nodes.add(MarkdownNode.BulletItem(text = bulletText, level = indent.coerceIn(0, 3)))
            i++
            continue
        }

        // 8. Numbered Lists: 1. , 2. etc.
        val numberedMatch = Regex("""^(\d{1,3})\.\s+(.*)$""").find(trimmed)
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[1]
            val text = numberedMatch.groupValues[2]
            val indent = rawLine.takeWhile { it.isWhitespace() }.length / 2
            nodes.add(MarkdownNode.NumberedItem(number = num, text = text, level = indent.coerceIn(0, 3)))
            i++
            continue
        }

        // 9. Standard Paragraph (gather consecutive text lines)
        val paragraphLines = mutableListOf<String>()
        paragraphLines.add(rawLine.trim())
        i++

        while (i < total) {
            val nextTrimmed = lines[i].trim()
            if (nextTrimmed.isEmpty() ||
                nextTrimmed.startsWith("```") ||
                isMarkdownTableRow(nextTrimmed) ||
                nextTrimmed.startsWith(">") ||
                nextTrimmed == "---" || nextTrimmed == "***" ||
                nextTrimmed.startsWith("#") ||
                isBulletLine(nextTrimmed) ||
                Regex("""^(\d{1,3})\.\s+(.*)$""").matches(nextTrimmed)
            ) {
                break
            }
            paragraphLines.add(nextTrimmed)
            i++
        }

        nodes.add(MarkdownNode.Paragraph(text = paragraphLines.joinToString(" ")))
    }

    return nodes
}

private fun isMarkdownTableRow(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("|") && trimmed.contains("|") && trimmed.count { it == '|' } >= 2
}

private fun parseMarkdownTable(lines: List<String>): MarkdownNode.Table? {
    if (lines.isEmpty()) return null

    fun splitRow(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split("|").map { it.trim() }
    }

    fun isDividerRow(row: List<String>): Boolean {
        return row.all { cell ->
            val clean = cell.replace("-", "").replace(":", "").replace(" ", "")
            clean.isEmpty()
        }
    }

    val rows = lines.map { splitRow(it) }
    if (rows.isEmpty()) return null

    val firstRow = rows.first()
    val isSecondRowDivider = rows.size > 1 && isDividerRow(rows[1])

    val headers: List<String>
    val dataRows: List<List<String>>

    if (isSecondRowDivider) {
        headers = firstRow
        dataRows = rows.drop(2).filter { !isDividerRow(it) }
    } else if (isDividerRow(firstRow)) {
        headers = emptyList()
        dataRows = rows.drop(1).filter { !isDividerRow(it) }
    } else {
        headers = firstRow
        dataRows = rows.drop(1).filter { !isDividerRow(it) }
    }

    return MarkdownNode.Table(headers = headers, rows = dataRows)
}

private fun isBulletLine(line: String): Boolean {
    return line.startsWith("* ") || line.startsWith("- ") || line.startsWith("+ ") || line.startsWith("• ")
}

private fun stripBulletPrefix(line: String): String {
    return when {
        line.startsWith("* ") -> line.removePrefix("* ").trim()
        line.startsWith("- ") -> line.removePrefix("- ").trim()
        line.startsWith("+ ") -> line.removePrefix("+ ").trim()
        line.startsWith("• ") -> line.removePrefix("• ").trim()
        else -> line.trim()
    }
}

// ---------------------------------------------------------------------------------------------
// INLINE FORMATTING PARSER (AnnotatedString)
// ---------------------------------------------------------------------------------------------

fun buildMarkdownAnnotatedString(
    text: String,
    baseColor: Color = Color(0xFFF1F5F9),
    boldColor: Color = Color.White,
    codeColor: Color = Color(0xFF38BDF8),
    codeBgColor: Color = Color(0xFF1E293B),
    italicColor: Color = Color(0xFFE2E8F0)
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")

    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            // 1. Inline Code: `code`
            if (text[cursor] == '`') {
                val nextBacktick = text.indexOf('`', cursor + 1)
                if (nextBacktick != -1) {
                    val codeContent = text.substring(cursor + 1, nextBacktick)
                    pushStyle(
                        SpanStyle(
                            color = codeColor,
                            background = codeBgColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(" $codeContent ")
                    pop()
                    cursor = nextBacktick + 1
                    continue
                }
            }

            // 2. Bold: **text** or __text__
            if ((text.startsWith("**", cursor) || text.startsWith("__", cursor)) && cursor + 2 < length) {
                val delim = text.substring(cursor, cursor + 2)
                val nextDelim = text.indexOf(delim, cursor + 2)
                if (nextDelim != -1) {
                    val boldContent = text.substring(cursor + 2, nextDelim)
                    pushStyle(
                        SpanStyle(
                            color = boldColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    // Nested parse for bold content (if it has italic or code inside)
                    append(boldContent)
                    pop()
                    cursor = nextDelim + 2
                    continue
                }
            }

            // 3. Strikethrough: ~~text~~
            if (text.startsWith("~~", cursor) && cursor + 2 < length) {
                val nextTilde = text.indexOf("~~", cursor + 2)
                if (nextTilde != -1) {
                    val strikeContent = text.substring(cursor + 2, nextTilde)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color(0xFF94A3B8)))
                    append(strikeContent)
                    pop()
                    cursor = nextTilde + 2
                    continue
                }
            }

            // 4. Italic: *text* or _text_ (single asterisk/underscore, not double)
            if ((text[cursor] == '*' || text[cursor] == '_') && cursor + 1 < length && text[cursor + 1] != text[cursor]) {
                val delim = text[cursor]
                val nextDelim = text.indexOf(delim, cursor + 1)
                // Check that it's not part of a double delimiter and is within reasonable span
                if (nextDelim != -1 && nextDelim - cursor < 300) {
                    val italicContent = text.substring(cursor + 1, nextDelim)
                    if (italicContent.isNotBlank()) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = italicColor))
                        append(italicContent)
                        pop()
                        cursor = nextDelim + 1
                        continue
                    }
                }
            }

            // 5. Normal character
            append(text[cursor])
            cursor++
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
