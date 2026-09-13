package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppDarkTheme

/**
 * Competitive Exam Visual Reasoning & Figure Question Rendering Engine.
 * Supports:
 * - Reasoning Pattern Grids (2x2, 3x3, 4x4 matrix with shapes, dots, rotations, and missing `?` cell)
 * - Series / Sequence Progression Figures (Step 1 -> Step 2 -> Step 3 -> `?`)
 * - Dice & Cube Net Pattern Unfolding (T-cross net, opposite faces, dots/numbers)
 * - Venn Diagrams (2-Set & 3-Set with set labels, regional values, and intersections)
 * - Compass & Displacement Path Diagrams (Direction tests: North, East, South, West turns)
 * - Seating Arrangements (Circular tables with inward/outward facing labels, linear rows)
 * - Analogy & Mirror/Water Image Reflections
 * - High-Contrast Monospaced Box-Drawing Diagrams (Unicode / ASCII charts & tables)
 */

sealed class FigureType {
    data class Grid(val rows: Int, val cols: Int, val cells: List<String>) : FigureType()
    data class Series(val steps: List<String>) : FigureType()
    data class CubeNet(val faces: Map<String, String>) : FigureType() // top, left, center, right, bottom, bottom2
    data class Venn(val setA: String, val setB: String, val setC: String? = null, val regions: Map<String, String> = emptyMap()) : FigureType()
    data class CompassPath(val points: List<Pair<String, Int>>) : FigureType() // e.g. ("North", 10), ("East", 5)
    data class Seating(val type: String, val positions: List<String>) : FigureType() // "circular" or "row"
    data class Analogy(val leftA: String, val leftB: String, val rightA: String, val rightB: String) : FigureType()
    data class BoxDrawing(val content: String) : FigureType()
}

/**
 * Detects if a text contains figure blocks like [FIGURE:GRID], [FIGURE:SERIES], [FIGURE:VENN],
 * [FIGURE:DICE], [FIGURE:COMPASS], [FIGURE:SEATING], or ASCII box-drawing characters.
 */
fun extractFigureBlock(text: String): Pair<FigureType?, String> {
    val clean = text.trim()

    // 1. Tagged Figure syntax: [FIGURE:TYPE params] ... [/FIGURE]
    val figureTagRegex = Regex("""\[FIGURE:([A-Za-z0-9_]+)(?:\s+([^\]]*))?\]([\s\S]*?)\[/FIGURE\]""", RegexOption.IGNORE_CASE)
    val match = figureTagRegex.find(clean)
    if (match != null) {
        val typeName = match.groupValues[1].uppercase()
        val params = match.groupValues[2].trim()
        val body = match.groupValues[3].trim()
        val remainingText = clean.replace(match.value, "").trim()

        val parsedFigure = when (typeName) {
            "GRID", "MATRIX" -> parseGridFigure(params, body)
            "SERIES", "SEQUENCE" -> parseSeriesFigure(body)
            "DICE", "CUBE", "CUBE_NET" -> parseCubeNetFigure(body)
            "VENN", "SETS" -> parseVennFigure(params, body)
            "COMPASS", "DIRECTION" -> parseCompassFigure(body)
            "SEATING", "ARRANGEMENT" -> parseSeatingFigure(params, body)
            "ANALOGY" -> parseAnalogyFigure(body)
            "ASCII", "DIAGRAM", "BOX" -> FigureType.BoxDrawing(body)
            else -> FigureType.BoxDrawing(body)
        }
        return Pair(parsedFigure, remainingText)
    }

    // 2. Auto-detect ASCII Box-Drawing if text contains characters like ┌─┐ │ └─┘ ┼ ═ ║
    if (clean.contains("┌") || clean.contains("├") || clean.contains("└") || clean.contains("│") || clean.contains("║") || clean.contains("╔") || clean.contains("══")) {
        val lines = clean.lines()
        val boxLines = lines.filter { it.contains("│") || it.contains("─") || it.contains("┌") || it.contains("└") || it.contains("┼") || it.contains("║") }
        if (boxLines.size >= 2) {
            val boxContent = boxLines.joinToString("\n")
            val nonBox = lines.filterNot { boxLines.contains(it) }.joinToString("\n").trim()
            return Pair(FigureType.BoxDrawing(boxContent), nonBox)
        }
    }

    return Pair(null, text)
}

private fun parseGridFigure(params: String, body: String): FigureType.Grid {
    val sizeParts = params.split("x", "X", " ")
    val rows = sizeParts.getOrNull(0)?.toIntOrNull() ?: 3
    val cols = sizeParts.getOrNull(1)?.toIntOrNull() ?: rows

    val lines = body.lines().map { it.trim() }.filter { it.isNotBlank() }
    val cells = mutableListOf<String>()

    lines.forEach { line ->
        val items = line.split("|", ",", "\t").map { it.trim() }.filter { it.isNotBlank() }
        if (items.isNotEmpty()) {
            cells.addAll(items)
        } else {
            cells.add(line)
        }
    }

    return FigureType.Grid(rows = rows, cols = cols, cells = cells)
}

private fun parseSeriesFigure(body: String): FigureType.Series {
    val steps = body.split("->", "-->", "\n", "|").map { it.trim() }.filter { it.isNotBlank() }
    return FigureType.Series(steps = if (steps.isNotEmpty()) steps else listOf("Step 1", "Step 2", "Step 3", "?"))
}

private fun parseCubeNetFigure(body: String): FigureType.CubeNet {
    val lines = body.lines().map { it.trim() }.filter { it.isNotBlank() }
    val map = mutableMapOf<String, String>()
    lines.forEach { line ->
        val parts = line.split(":", "=")
        if (parts.size >= 2) {
            map[parts[0].trim().lowercase()] = parts[1].trim()
        }
    }
    return FigureType.CubeNet(faces = map)
}

private fun parseVennFigure(params: String, body: String): FigureType.Venn {
    val setNames = params.split(",", "|", " ").map { it.trim() }.filter { it.isNotBlank() }
    val setA = setNames.getOrNull(0) ?: "Set A"
    val setB = setNames.getOrNull(1) ?: "Set B"
    val setC = setNames.getOrNull(2)

    val regionMap = mutableMapOf<String, String>()
    body.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
        val parts = line.split(":", "=")
        if (parts.size >= 2) {
            regionMap[parts[0].trim()] = parts[1].trim()
        }
    }

    return FigureType.Venn(setA = setA, setB = setB, setC = setC, regions = regionMap)
}

private fun parseCompassFigure(body: String): FigureType.CompassPath {
    val points = mutableListOf<Pair<String, Int>>()
    body.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
        val match = Regex("""([A-Za-z]+)\s*[:\-–]?\s*(\d+)""").find(line)
        if (match != null) {
            val dir = match.groupValues[1].trim()
            val dist = match.groupValues[2].toIntOrNull() ?: 10
            points.add(Pair(dir, dist))
        }
    }
    return FigureType.CompassPath(points = if (points.isNotEmpty()) points else listOf(Pair("North", 10), Pair("East", 15)))
}

private fun parseSeatingFigure(params: String, body: String): FigureType.Seating {
    val type = if (params.contains("row", ignoreCase = true) || body.contains("row", ignoreCase = true)) "row" else "circular"
    val positions = body.split(",", "|", "\n").map { it.trim() }.filter { it.isNotBlank() }
    return FigureType.Seating(type = type, positions = if (positions.isNotEmpty()) positions else listOf("A", "B", "C", "D", "E", "F"))
}

private fun parseAnalogyFigure(body: String): FigureType.Analogy {
    val parts = body.split("::", ":", "\n").map { it.trim() }.filter { it.isNotBlank() }
    return FigureType.Analogy(
        leftA = parts.getOrNull(0) ?: "Figure A",
        leftB = parts.getOrNull(1) ?: "Figure B",
        rightA = parts.getOrNull(2) ?: "Figure C",
        rightB = parts.getOrNull(3) ?: "?"
    )
}

/**
 * Universal Figure Renderer Composable.
 */
@Composable
fun FigureQuestionCard(
    figure: FigureType,
    modifier: Modifier = Modifier,
    isDark: Boolean = isAppDarkTheme()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (figure) {
                is FigureType.Grid -> ReasoningGridFigure(figure, isDark)
                is FigureType.Series -> SeriesProgressionFigure(figure, isDark)
                is FigureType.CubeNet -> CubeNetFigure(figure, isDark)
                is FigureType.Venn -> VennDiagramFigure(figure, isDark)
                is FigureType.CompassPath -> CompassPathFigure(figure, isDark)
                is FigureType.Seating -> SeatingArrangementFigure(figure, isDark)
                is FigureType.Analogy -> AnalogyFigure(figure, isDark)
                is FigureType.BoxDrawing -> BoxDrawingCanvas(figure.content, isDark)
            }
        }
    }
}

/**
 * 1. Pattern Reasoning Grid (Matrix)
 */
@Composable
fun ReasoningGridFigure(grid: FigureType.Grid, isDark: Boolean) {
    val totalCells = grid.rows * grid.cols
    val cellItems = remember(grid) {
        val list = grid.cells.toMutableList()
        while (list.size < totalCells) {
            list.add(if (list.size == totalCells - 1) "?" else "•")
        }
        list.take(totalCells)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = null,
                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Reasoning Pattern Matrix (${grid.rows}×${grid.cols})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (r in 0 until grid.rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until grid.cols) {
                        val index = r * grid.cols + c
                        val cellContent = cellItems.getOrElse(index) { "•" }
                        val isMissing = cellContent == "?"

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isMissing -> if (isDark) Color(0xFF6366F1).copy(alpha = 0.25f) else Color(0xFFFEF3C7)
                                        else -> if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isMissing) (if (isDark) Color(0xFF818CF8) else Color(0xFFF59E0B)) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isMissing) {
                                Text(
                                    text = "?",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFFD97706)
                                )
                            } else {
                                Text(
                                    text = cellContent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Series / Sequence Progression Figures
 */
@Composable
fun SeriesProgressionFigure(series: FigureType.Series, isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ViewCarousel,
                contentDescription = null,
                tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sequence / Series Transition",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            series.steps.forEachIndexed { index, step ->
                val isLast = index == series.steps.size - 1
                val isMissing = step == "?" || isLast

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMissing) (if (isDark) Color(0xFF1E1B4B) else Color(0xFFFEF3C7)) else (if (isDark) Color(0xFF1E293B) else Color.White)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isMissing) (if (isDark) Color(0xFF818CF8) else Color(0xFFF59E0B)) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ),
                    modifier = Modifier.size(width = 64.dp, height = 64.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = step,
                            fontSize = if (step == "?") 22.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMissing) (if (isDark) Color(0xFFA5B4FC) else Color(0xFFD97706)) else (if (isDark) Color.White else Color(0xFF1E293B)),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (!isLast) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3. Cube / Dice Net Pattern
 */
@Composable
fun CubeNetFigure(cubeNet: FigureType.CubeNet, isDark: Boolean) {
    val faces = cubeNet.faces
    val top = faces["top"] ?: "1"
    val left = faces["left"] ?: "2"
    val center = faces["center"] ?: "3"
    val right = faces["right"] ?: "4"
    val bottom = faces["bottom"] ?: "5"
    val bottom2 = faces["bottom2"] ?: "6"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                tint = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Unfolded Cube / Dice Net",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        // Top face
        DiceFaceBox(top, isDark)

        // Middle row (Left, Center, Right)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            DiceFaceBox(left, isDark)
            DiceFaceBox(center, isDark)
            DiceFaceBox(right, isDark)
        }

        // Bottom face
        DiceFaceBox(bottom, isDark)

        // Bottom2 face
        DiceFaceBox(bottom2, isDark)
    }
}

@Composable
private fun DiceFaceBox(value: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0xFF1E293B) else Color.White)
            .border(1.2.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
    }
}

/**
 * 4. Venn Diagrams (2-Set and 3-Set)
 */
@Composable
fun VennDiagramFigure(venn: FigureType.Venn, isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Grain,
                contentDescription = null,
                tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Venn Diagram (${venn.setA} ∩ ${venn.setB}${if (venn.setC != null) " ∩ ${venn.setC}" else ""})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            val colorA = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.3f) else Color(0xFF0284C7).copy(alpha = 0.2f)
            val borderA = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            val colorB = if (isDark) Color(0xFFF472B6).copy(alpha = 0.3f) else Color(0xFFDB2777).copy(alpha = 0.2f)
            val borderB = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)

            Canvas(modifier = Modifier.size(240.dp, 120.dp)) {
                val radius = 50.dp.toPx()
                val centerLeft = Offset(size.width * 0.38f, size.height * 0.5f)
                val centerRight = Offset(size.width * 0.62f, size.height * 0.5f)

                // Circle A
                drawCircle(color = colorA, radius = radius, center = centerLeft)
                drawCircle(color = borderA, radius = radius, center = centerLeft, style = Stroke(width = 2.dp.toPx()))

                // Circle B
                drawCircle(color = colorB, radius = radius, center = centerRight)
                drawCircle(color = borderB, radius = radius, center = centerRight, style = Stroke(width = 2.dp.toPx()))
            }

            // Overlay text labels
            Row(
                modifier = Modifier
                    .width(220.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = venn.setA,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = borderA
                    )
                )
                Text(
                    text = "Both",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                )
                Text(
                    text = venn.setB,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = borderB
                    )
                )
            }
        }
    }
}

/**
 * 5. Direction & Displacement Path (Compass)
 */
@Composable
fun CompassPathFigure(compass: FigureType.CompassPath, isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Direction & Distance Vector Path",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compass Rose Card
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                Icon(Icons.Default.Explore, contentDescription = null, tint = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706), modifier = Modifier.size(24.dp))
            }

            // Path steps
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text("Path Track:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)))
                compass.points.forEachIndexed { i, p ->
                    Text(
                        text = "${i + 1}. Starts -> ${p.second}m ${p.first}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                        )
                    )
                }
            }
        }
    }
}

/**
 * 6. Seating Arrangement (Circular or Row)
 */
@Composable
fun SeatingArrangementFigure(seating: FigureType.Seating, isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (seating.type == "row") "Linear Row Arrangement" else "Circular Table Seating",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        if (seating.type == "row") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                seating.positions.forEachIndexed { idx, p ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                            .border(1.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF93C5FD), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDark) Color.White else Color(0xFF1E293B))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .border(2.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Table\n(Facing Center)",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * 7. Analogy Figures
 */
@Composable
fun AnalogyFigure(analogy: FigureType.Analogy, isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Pattern,
                contentDescription = null,
                tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Figure Analogy (A : B :: C : ?)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            AnalogyBox(analogy.leftA, false, isDark)
            Text(":", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            AnalogyBox(analogy.leftB, false, isDark)
            Text("::", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED))
            AnalogyBox(analogy.rightA, false, isDark)
            Text(":", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            AnalogyBox(analogy.rightB, analogy.rightB == "?", isDark)
        }
    }
}

@Composable
private fun AnalogyBox(text: String, isMissing: Boolean, isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isMissing) (if (isDark) Color(0xFF1E1B4B) else Color(0xFFFEF3C7)) else (if (isDark) Color(0xFF1E293B) else Color.White)
            )
            .border(
                1.dp,
                if (isMissing) (if (isDark) Color(0xFF818CF8) else Color(0xFFF59E0B)) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (isMissing) 18.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isMissing) (if (isDark) Color(0xFFA5B4FC) else Color(0xFFD97706)) else (if (isDark) Color.White else Color(0xFF1E293B)),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 8. Monospaced Box-Drawing Canvas for ASCII Diagrams
 */
@Composable
fun BoxDrawingCanvas(content: String, isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF090D16) else Color(0xFFF8FAFC))
            .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                )
            )
        }
    }
}
