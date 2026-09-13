package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ui.theme.isAppDarkTheme
import com.example.util.MathNotationHelper

/**
 * High-performance, crash-safe, native Jetpack Compose Mathematical Expression Renderer.
 * Supports:
 * - Display Math ($$...$$, \[...\], \begin{aligned}, \begin{matrix})
 * - Vertical Stacked Fractions (\frac{a}{b}, \dfrac{a}{b})
 * - Radical Roots (\sqrt{x}, \sqrt[3]{x}, \sqrt[n]{x})
 * - Exponents / Superscripts (x^2, e^{-x}, 10^5)
 * - Subscripts / Indices (a_1, x_n, H_2O)
 * - Matrices & Determinants (\begin{matrix}, \begin{pmatrix}, \begin{bmatrix}, \begin{vmatrix})
 * - Greek Letters (α, β, γ, θ, π, λ, μ, σ, Ω, Δ, etc.)
 * - Standard Academic & Competitive Exam Math Operators (±, ×, ÷, ≤, ≥, ≠, ≈, ∈, ⊂, ∪, ∩, ∫, ∑, ∏, lim, →, ⇒, ⇔, ∴, ∵)
 */

sealed class MathToken {
    data class PlainText(val text: String) : MathToken()
    data class Fraction(val numerator: String, val denominator: String) : MathToken()
    data class Root(val index: String?, val radicand: String) : MathToken()
    data class Power(val base: String, val exponent: String) : MathToken()
    data class Subscript(val base: String, val index: String) : MathToken()
    data class Matrix(val rows: List<List<String>>, val bracketType: String = "round") : MathToken()
    data class AlignedEquation(val lines: List<String>) : MathToken()
}

/**
 * Main rich math composable for rendering any string that may contain mathematical notation,
 * display formulas, inline LaTeX, or plain academic text.
 */
@Composable
fun RichMathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    isDark: Boolean = isAppDarkTheme()
) {
    if (text.isBlank()) return

    // Check if contains display math blocks ($$...$$, \[...\], or \begin{...})
    val hasDisplayMath = text.contains("$$") || text.contains("\\[") || text.contains("\\begin{aligned}") || text.contains("\\begin{matrix}") || text.contains("\\begin{bmatrix}")

    if (hasDisplayMath) {
        DisplayMathBlockContainer(
            rawText = text,
            modifier = modifier,
            style = style,
            textColor = if (color != Color.Unspecified) color else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)),
            isDark = isDark
        )
    } else {
        InlineMathContent(
            rawText = text,
            modifier = modifier,
            style = style,
            color = if (color != Color.Unspecified) color else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)),
            fontWeight = fontWeight,
            isDark = isDark
        )
    }
}

/**
 * Renders display math equations with dedicated equation container, copy button, and high-contrast styling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplayMathBlockContainer(
    rawText: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    textColor: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val segments = remember(rawText) { splitDisplayMathSegments(rawText) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        segments.forEach { segment ->
            when (segment) {
                is DisplaySegment.Text -> {
                    InlineMathContent(
                        rawText = segment.content,
                        style = style,
                        color = textColor,
                        isDark = isDark
                    )
                }
                is DisplaySegment.Equation -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Functions,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Formula / Equation",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(segment.formula))
                                        Toast.makeText(context, "Formula copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Formula",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Render mathematical formula content
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                RenderMathematicalFormula(
                                    formula = segment.formula,
                                    textColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class DisplaySegment {
    data class Text(val content: String) : DisplaySegment()
    data class Equation(val formula: String) : DisplaySegment()
}

private fun splitDisplayMathSegments(rawText: String): List<DisplaySegment> {
    val segments = mutableListOf<DisplaySegment>()
    val pattern = Regex("""\$\$([^\$]+)\$\$|\\\[(.*?)\\\]|\\begin\{aligned\}(.*?)\\end\{aligned\}|\\begin\{matrix\}(.*?)\\end\{matrix\}|\\begin\{pmatrix\}(.*?)\\end\{pmatrix\}|\\begin\{bmatrix\}(.*?)\\end\{bmatrix\}""", RegexOption.DOT_MATCHES_ALL)
    
    var lastIndex = 0
    val matches = pattern.findAll(rawText).toList()

    for (match in matches) {
        if (match.range.first > lastIndex) {
            val before = rawText.substring(lastIndex, match.range.first).trim()
            if (before.isNotBlank()) {
                segments.add(DisplaySegment.Text(before))
            }
        }
        val formula = (match.groupValues[1].ifBlank { null }
            ?: match.groupValues[2].ifBlank { null }
            ?: match.groupValues[3].ifBlank { null }
            ?: match.groupValues[4].ifBlank { null }
            ?: match.groupValues[5].ifBlank { null }
            ?: match.groupValues[6].ifBlank { null }
            ?: match.value).trim()

        segments.add(DisplaySegment.Equation(formula))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < rawText.length) {
        val remaining = rawText.substring(lastIndex).trim()
        if (remaining.isNotBlank()) {
            segments.add(DisplaySegment.Text(remaining))
        }
    }

    if (segments.isEmpty()) {
        segments.add(DisplaySegment.Text(rawText))
    }

    return segments
}

/**
 * Renders inline text with math expressions, fractions, roots, and exponents embedded.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineMathContent(
    rawText: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
    isDark: Boolean
) {
    val tokens = remember(rawText) { parseInlineMathTokens(rawText) }

    // If text only contains plain tokens, render as a single fast Text
    val hasComplexMath = tokens.any { it !is MathToken.PlainText }

    if (!hasComplexMath) {
        val formatted = remember(rawText) { MathNotationHelper.formatMathNotation(rawText) }
        val annotated = remember(formatted, color, fontWeight) {
            buildAnnotatedMathString(formatted, color, fontWeight)
        }
        Text(
            text = annotated,
            style = style,
            modifier = modifier
        )
    } else {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center
        ) {
            tokens.forEach { token ->
                when (token) {
                    is MathToken.PlainText -> {
                        val formatted = remember(token.text) { MathNotationHelper.formatMathNotation(token.text) }
                        val annotated = remember(formatted, color, fontWeight) {
                            buildAnnotatedMathString(formatted, color, fontWeight)
                        }
                        Text(
                            text = annotated,
                            style = style,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    is MathToken.Fraction -> {
                        VerticalFraction(
                            numerator = token.numerator,
                            denominator = token.denominator,
                            textColor = color,
                            isDark = isDark,
                            fontSize = style.fontSize
                        )
                    }
                    is MathToken.Root -> {
                        RadicalRoot(
                            index = token.index,
                            radicand = token.radicand,
                            textColor = color,
                            isDark = isDark,
                            fontSize = style.fontSize
                        )
                    }
                    is MathToken.Power -> {
                        PowerExponent(
                            base = token.base,
                            exponent = token.exponent,
                            textColor = color,
                            fontSize = style.fontSize
                        )
                    }
                    is MathToken.Subscript -> {
                        SubscriptIndex(
                            base = token.base,
                            index = token.index,
                            textColor = color,
                            fontSize = style.fontSize
                        )
                    }
                    is MathToken.Matrix -> {
                        MatrixView(
                            rows = token.rows,
                            bracketType = token.bracketType,
                            textColor = color,
                            isDark = isDark
                        )
                    }
                    is MathToken.AlignedEquation -> {
                        AlignedEquationView(
                            lines = token.lines,
                            textColor = color,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vertical fraction layout (Numerator / Denominator separated by a horizontal fraction bar).
 */
@Composable
fun VerticalFraction(
    numerator: String,
    denominator: String,
    textColor: Color,
    isDark: Boolean,
    fontSize: TextUnit = 14.sp
) {
    val cleanNum = remember(numerator) { MathNotationHelper.formatMathNotation(numerator.trim()) }
    val cleanDen = remember(denominator) { MathNotationHelper.formatMathNotation(denominator.trim()) }

    val numFontSize = (fontSize.value * 0.88f).sp
    val denFontSize = (fontSize.value * 0.88f).sp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(min = 16.dp)
        ) {
            Text(
                text = cleanNum,
                fontSize = numFontSize,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = (numFontSize.value * 1.1f).sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.5.dp),
                thickness = 1.2.dp,
                color = textColor.copy(alpha = 0.85f)
            )
            Text(
                text = cleanDen,
                fontSize = denFontSize,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = (denFontSize.value * 1.1f).sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

/**
 * Radical Root layout (\sqrt[n]{x} or \sqrt{x}).
 */
@Composable
fun RadicalRoot(
    index: String?,
    radicand: String,
    textColor: Color,
    isDark: Boolean,
    fontSize: TextUnit = 14.sp
) {
    val cleanRadicand = remember(radicand) { MathNotationHelper.formatMathNotation(radicand.trim()) }
    val cleanIndex = index?.let { MathNotationHelper.toSuperscript(it.trim()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        if (!cleanIndex.isNullOrBlank()) {
            Text(
                text = cleanIndex,
                fontSize = (fontSize.value * 0.75f).sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(
            text = "√",
            fontSize = (fontSize.value * 1.25f).sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 1.dp),
                thickness = 1.2.dp,
                color = textColor.copy(alpha = 0.85f)
            )
            Text(
                text = cleanRadicand,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

/**
 * Exponent / Power layout (base^exp).
 */
@Composable
fun PowerExponent(
    base: String,
    exponent: String,
    textColor: Color,
    fontSize: TextUnit = 14.sp
) {
    val cleanBase = remember(base) { MathNotationHelper.formatMathNotation(base.trim()) }
    val superExp = remember(exponent) { MathNotationHelper.toSuperscript(exponent.trim()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 1.dp)
    ) {
        Text(
            text = cleanBase,
            fontSize = fontSize,
            color = textColor,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = superExp,
            fontSize = (fontSize.value * 0.85f).sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

/**
 * Subscript layout (base_index).
 */
@Composable
fun SubscriptIndex(
    base: String,
    index: String,
    textColor: Color,
    fontSize: TextUnit = 14.sp
) {
    val cleanBase = remember(base) { MathNotationHelper.formatMathNotation(base.trim()) }
    val subIdx = remember(index) { MathNotationHelper.toSubscript(index.trim()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 1.dp)
    ) {
        Text(
            text = cleanBase,
            fontSize = fontSize,
            color = textColor,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = subIdx,
            fontSize = (fontSize.value * 0.85f).sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Matrix / Determinant rendering in Jetpack Compose.
 */
@Composable
fun MatrixView(
    rows: List<List<String>>,
    bracketType: String = "round", // round, square, bar
    textColor: Color,
    isDark: Boolean
) {
    val (leftBracket, rightBracket) = when (bracketType) {
        "square" -> Pair("[", "]")
        "bar" -> Pair("|", "|")
        "double_bar" -> Pair("‖", "‖")
        else -> Pair("(", ")")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = leftBracket,
            fontSize = (rows.size * 18).coerceIn(20, 48).sp,
            fontWeight = FontWeight.Light,
            color = textColor
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { cell ->
                        Text(
                            text = MathNotationHelper.formatMathNotation(cell.trim()),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Text(
            text = rightBracket,
            fontSize = (rows.size * 18).coerceIn(20, 48).sp,
            fontWeight = FontWeight.Light,
            color = textColor
        )
    }
}

/**
 * Aligned multi-line equation view.
 */
@Composable
fun AlignedEquationView(
    lines: List<String>,
    textColor: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { line ->
            val parts = line.split("=", "&=", "\\Rightarrow", "->")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = MathNotationHelper.formatMathNotation(line),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Formula block renderer that parses LaTeX math strings into tokens.
 */
@Composable
fun RenderMathematicalFormula(
    formula: String,
    textColor: Color,
    isDark: Boolean
) {
    val cleanFormula = formula.trim()

    // Check if matrix
    if (cleanFormula.contains("\\begin{matrix}") || cleanFormula.contains("\\begin{pmatrix}") || cleanFormula.contains("\\begin{bmatrix}") || cleanFormula.contains("\\begin{vmatrix}")) {
        val bracketType = when {
            cleanFormula.contains("pmatrix") -> "round"
            cleanFormula.contains("bmatrix") -> "square"
            cleanFormula.contains("vmatrix") -> "bar"
            else -> "round"
        }
        val inner = cleanFormula
            .replace(Regex("""\\begin\{(?:matrix|pmatrix|bmatrix|vmatrix)\}"""), "")
            .replace(Regex("""\\end\{(?:matrix|pmatrix|bmatrix|vmatrix)\}"""), "")
            .trim()
        val rowLines = inner.split("\\\\")
        val matrixRows = rowLines.mapNotNull { row ->
            val cells = row.split("&").map { it.trim() }.filter { it.isNotBlank() }
            if (cells.isNotEmpty()) cells else null
        }
        if (matrixRows.isNotEmpty()) {
            MatrixView(rows = matrixRows, bracketType = bracketType, textColor = textColor, isDark = isDark)
            return
        }
    }

    // Default inline math
    InlineMathContent(
        rawText = cleanFormula,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp
        ),
        color = textColor,
        isDark = isDark
    )
}

/**
 * Parser for mixed text and inline math strings.
 */
fun parseInlineMathTokens(rawText: String): List<MathToken> {
    if (rawText.isBlank()) return emptyList()

    val tokens = mutableListOf<MathToken>()

    // Tokenize fractions: \frac{a}{b} or \dfrac{a}{b}
    val fractionRegex = Regex("""\\(?:frac|dfrac|tfrac)\{([^{}]+)\}\{([^{}]+)\}""")
    val rootRegex = Regex("""\\sqrt(?:\[([^\[\]]+)\])?\{([^{}]+)\}""")
    val powerRegex = Regex("""([a-zA-Z0-9\(\)]+)\^\{([^{}]+)\}""")
    val subscriptRegex = Regex("""([a-zA-Z0-9\(\)]+)\_\{([^{}]+)\}""")

    var currentIndex = 0
    val fullText = rawText

    // Combined pattern
    val combinedRegex = Regex("""(\\frac\{[^{}]+\}\{[^{}]+\}|\\dfrac\{[^{}]+\}\{[^{}]+\}|\\sqrt(?:\[[^\[\]]+\])?\{[^{}]+\}|[a-zA-Z0-9\(\)]+\^\{[^{}]+\}|[a-zA-Z0-9\(\)]+\_\{[^{}]+\})""")
    val matches = combinedRegex.findAll(fullText).toList()

    if (matches.isEmpty()) {
        tokens.add(MathToken.PlainText(rawText))
        return tokens
    }

    for (match in matches) {
        if (match.range.first > currentIndex) {
            val plain = fullText.substring(currentIndex, match.range.first)
            if (plain.isNotEmpty()) {
                tokens.add(MathToken.PlainText(plain))
            }
        }

        val matchValue = match.value
        val fracMatch = fractionRegex.matchEntire(matchValue)
        val rootMatch = rootRegex.matchEntire(matchValue)
        val powerMatch = powerRegex.matchEntire(matchValue)
        val subMatch = subscriptRegex.matchEntire(matchValue)

        if (fracMatch != null) {
            tokens.add(MathToken.Fraction(fracMatch.groupValues[1], fracMatch.groupValues[2]))
        } else if (rootMatch != null) {
            val idx = rootMatch.groupValues.getOrNull(1)?.ifBlank { null }
            val rad = rootMatch.groupValues[2]
            tokens.add(MathToken.Root(idx, rad))
        } else if (powerMatch != null) {
            tokens.add(MathToken.Power(powerMatch.groupValues[1], powerMatch.groupValues[2]))
        } else if (subMatch != null) {
            tokens.add(MathToken.Subscript(subMatch.groupValues[1], subMatch.groupValues[2]))
        } else {
            tokens.add(MathToken.PlainText(matchValue))
        }

        currentIndex = match.range.last + 1
    }

    if (currentIndex < fullText.length) {
        val remaining = fullText.substring(currentIndex)
        if (remaining.isNotEmpty()) {
            tokens.add(MathToken.PlainText(remaining))
        }
    }

    return tokens
}

/**
 * Converts formatted math string into AnnotatedString with syntax highlighting for math variables and operators.
 */
fun buildAnnotatedMathString(
    text: String,
    baseColor: Color,
    baseWeight: FontWeight? = null
): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    val mathSymbols = setOf('=', '+', '-', '×', '÷', '±', '∓', '·', '≤', '≥', '≠', '≈', '≡', '∝', '∞', '√', '∛', '∠', '⊥', '∥', '∈', '∉', '⊂', '∪', '∩', '∫', '∂', '∑', '∏', '→', '←', '⇒', '⇔', '∴', '∵', '°')

    for (i in text.indices) {
        val ch = text[i]
        if (ch in mathSymbols) {
            builder.addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = baseColor
                ),
                i,
                i + 1
            )
        }
    }

    return builder.toAnnotatedString()
}
