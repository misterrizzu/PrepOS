package com.example.ui.editor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChartEntry
import com.example.model.ChartType
import com.example.model.DocElement

private val DEFAULT_PALETTE = listOf(
    "#6C63D9", // Purple
    "#2563EB", // Blue
    "#059669", // Green
    "#D97706", // Amber
    "#DC2626", // Red
    "#DB2777", // Pink
    "#0891B2", // Cyan
    "#7C3AED"  // Violet
)

private fun parseColor(hex: String, fallback: Color = Color(0xFF6C63D9)): Color {
    return try {
        if (hex.startsWith("#")) {
            Color(android.graphics.Color.parseColor(hex))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartCard(
    block: DocElement.ChartBlock,
    isEditMode: Boolean,
    onUpdate: (DocElement.ChartBlock) -> Unit,
    onDelete: () -> Unit,
    onAiEdit: (DocElement.ChartBlock) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var title by remember(block.blockId, block.title) { mutableStateOf(block.title) }
    var subtitle by remember(block.blockId, block.subtitle) { mutableStateOf(block.subtitle) }
    var isLocallyEditing by remember { mutableStateOf(false) }
    val effectiveEditMode = isEditMode || isLocallyEditing

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { isLocallyEditing = !isLocallyEditing }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title, Type Selector, Delete Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6C63D9).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (block.chartType) {
                                ChartType.BAR -> Icons.Default.BarChart
                                ChartType.DONUT, ChartType.PIE -> Icons.Default.DonutLarge
                                ChartType.PROGRESS_RINGS -> Icons.Default.DataUsage
                            },
                            contentDescription = null,
                            tint = Color(0xFF6C63D9),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    if (effectiveEditMode) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = {
                                    title = it
                                    onUpdate(block.copy(title = it))
                                },
                                placeholder = { Text("Chart Title") },
                                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6C63D9),
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = subtitle,
                                onValueChange = {
                                    subtitle = it
                                    onUpdate(block.copy(subtitle = it))
                                },
                                placeholder = { Text("Subtitle / Metric (Optional)") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6C63D9),
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = block.title.ifBlank { "Data Visualization" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (block.subtitle.isNotBlank()) {
                                Text(
                                    text = block.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (effectiveEditMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLocallyEditing && !isEditMode) {
                            IconButton(
                                onClick = { isLocallyEditing = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Done editing",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onAiEdit(block) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF4B38B7), Color(0xFF7B5CE7))
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .testTag("ai_chart_edit_btn")
                        ) {
                            Text(
                                text = "✨",
                                fontSize = 16.sp
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete chart",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (effectiveEditMode) {
                Spacer(modifier = Modifier.height(10.dp))
                // Chart Type Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = block.chartType == ChartType.BAR,
                        onClick = { onUpdate(block.copy(chartType = ChartType.BAR)) },
                        label = { Text("Bar Chart", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6C63D9),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = block.chartType == ChartType.DONUT || block.chartType == ChartType.PIE,
                        onClick = { onUpdate(block.copy(chartType = ChartType.DONUT)) },
                        label = { Text("Donut", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.DonutLarge, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6C63D9),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = block.chartType == ChartType.PROGRESS_RINGS,
                        onClick = { onUpdate(block.copy(chartType = ChartType.PROGRESS_RINGS)) },
                        label = { Text("Rings", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.DataUsage, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6C63D9),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Visual Display
            when (block.chartType) {
                ChartType.BAR -> BarChartView(entries = block.entries)
                ChartType.DONUT, ChartType.PIE -> DonutChartView(entries = block.entries)
                ChartType.PROGRESS_RINGS -> ProgressRingsView(entries = block.entries)
            }

            // Edit Mode Entry List Manager
            if (effectiveEditMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Data Entries",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                block.entries.forEachIndexed { index, entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Color Indicator & Cycle Button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(parseColor(entry.colorHex, Color(0xFF6C63D9)))
                                .clickable {
                                    val nextPaletteColor = DEFAULT_PALETTE[(DEFAULT_PALETTE.indexOf(entry.colorHex) + 1).coerceAtLeast(0) % DEFAULT_PALETTE.size]
                                    val updated = block.entries.toMutableList()
                                    updated[index] = entry.copy(colorHex = nextPaletteColor)
                                    onUpdate(block.copy(entries = updated))
                                }
                        )

                        // Label field
                        OutlinedTextField(
                            value = entry.label,
                            onValueChange = { newLabel ->
                                val updated = block.entries.toMutableList()
                                updated[index] = entry.copy(label = newLabel)
                                onUpdate(block.copy(entries = updated))
                            },
                            placeholder = { Text("Label") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.5f)
                        )

                        // Value field
                        OutlinedTextField(
                            value = if (entry.value % 1 == 0f) entry.value.toInt().toString() else entry.value.toString(),
                            onValueChange = { newText ->
                                val num = newText.toFloatOrNull() ?: 0f
                                val updated = block.entries.toMutableList()
                                updated[index] = entry.copy(value = num)
                                onUpdate(block.copy(entries = updated))
                            },
                            placeholder = { Text("Val") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(72.dp)
                        )

                        // Delete Entry Button
                        if (block.entries.size > 1) {
                            IconButton(
                                onClick = {
                                    val updated = block.entries.toMutableList().apply { removeAt(index) }
                                    onUpdate(block.copy(entries = updated))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete item", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val nextColor = DEFAULT_PALETTE[block.entries.size % DEFAULT_PALETTE.size]
                        val updated = block.entries.toMutableList().apply {
                            add(ChartEntry("Topic ${size + 1}", 50f, nextColor))
                        }
                        onUpdate(block.copy(entries = updated))
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Entry")
                }
            }
        }
    }
}

@Composable
fun BarChartView(entries: List<ChartEntry>) {
    val maxValue = remember(entries) {
        entries.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 100f
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        entries.forEachIndexed { index, entry ->
            val targetRatio = (entry.value / maxValue).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = if (startAnimation) targetRatio else 0f,
                animationSpec = tween(durationMillis = 600 + index * 100, easing = FastOutSlowInEasing),
                label = "bar_progress_${entry.label}"
            )
            val barColor = parseColor(entry.colorHex, Color(0xFF6C63D9))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (entry.value % 1 == 0f) "${entry.value.toInt()}%" else "${entry.value}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChartView(entries: List<ChartEntry>) {
    val total = remember(entries) {
        entries.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animationProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donut_animation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(radius * 2, radius * 2)

                // Draw background track
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                var startAngle = -90f
                for (entry in entries) {
                    val sweepAngle = (entry.value / total) * 360f * animationProgress
                    val color = parseColor(entry.colorHex, Color(0xFF6C63D9))
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += (entry.value / total) * 360f
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${entries.size}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            entries.forEach { entry ->
                val color = parseColor(entry.colorHex, Color(0xFF6C63D9))
                val percentage = ((entry.value / total) * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${entry.label} (${percentage}%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressRingsView(entries: List<ChartEntry>) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        entries.take(4).forEachIndexed { index, entry ->
            val targetRatio = (entry.value / 100f).coerceIn(0f, 1f)
            val animatedSweep by animateFloatAsState(
                targetValue = if (startAnimation) targetRatio * 360f else 0f,
                animationSpec = tween(durationMillis = 700 + index * 100, easing = FastOutSlowInEasing),
                label = "ring_progress_${entry.label}"
            )
            val ringColor = parseColor(entry.colorHex, Color(0xFF6C63D9))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    Canvas(modifier = Modifier.size(56.dp)) {
                        val stroke = 6.dp.toPx()
                        val radius = (size.minDimension - stroke) / 2
                        val topLeft = Offset(stroke / 2, stroke / 2)
                        val arcSize = Size(radius * 2, radius * 2)

                        // Track
                        drawArc(
                            color = ringColor.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )

                        // Fill
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${entry.value.toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
