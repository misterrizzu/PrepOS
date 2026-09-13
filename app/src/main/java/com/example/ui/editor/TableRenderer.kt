package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.model.DocElement

@Composable
fun TableCard(
    block: DocElement.TableBlock,
    isEditMode: Boolean,
    onUpdate: (DocElement.TableBlock) -> Unit,
    onDelete: () -> Unit,
    onAiEdit: (DocElement.TableBlock) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLocallyEditing by remember { mutableStateOf(false) }
    val effectiveEditMode = isEditMode || isLocallyEditing
    var title by remember(block.blockId, block.title) { mutableStateOf(block.title) }
    val horizontalScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val headerTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0F172A),
        fontSize = 13.5.sp
    )
    val cellTextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = Color(0xFF334155),
        fontSize = 13.sp
    )

    // Calculate content-driven intrinsic width for each column (longest cell in each column)
    val columnWidths = remember(block.headers, block.rows, effectiveEditMode) {
        val colCount = maxOf(block.headers.size, block.rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(1)
        (0 until colCount).map { colIdx ->
            val headerText = block.headers.getOrElse(colIdx) { "" }
            val headerWidthPx = if (block.hasHeaderRow && headerText.isNotBlank()) {
                textMeasurer.measure(
                    text = AnnotatedString(headerText),
                    style = headerTextStyle,
                    maxLines = 1
                ).size.width
            } else {
                0
            }

            var maxColWidthPx = headerWidthPx
            for (row in block.rows) {
                val cellText = row.getOrElse(colIdx) { "" }
                if (cellText.isNotBlank()) {
                    val lines = cellText.split("\n")
                    for (line in lines) {
                        val lineWidthPx = textMeasurer.measure(
                            text = AnnotatedString(line),
                            style = cellTextStyle,
                            maxLines = 1
                        ).size.width
                        if (lineWidthPx > maxColWidthPx) {
                            maxColWidthPx = lineWidthPx
                        }
                    }
                }
            }

            val paddingHorizontalDp = 24.dp // 12.dp each side
            val minWidthDp = if (effectiveEditMode) 96.dp else 48.dp
            val intrinsicWidthDp = with(density) { maxColWidthPx.toDp() } + paddingHorizontalDp
            maxOf(minWidthDp, intrinsicWidthDp)
        }
    }

    val gridBorderColor = Color(0xFFCBD5E1)
    val dividerColor = Color(0xFFE2E8F0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { isLocallyEditing = !isLocallyEditing }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, gridBorderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header / Title bar
            if (effectiveEditMode || block.title.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (effectiveEditMode) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                onUpdate(block.copy(title = it))
                            },
                            placeholder = { Text("Table Title (Optional)") },
                            textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
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
                                    .testTag("ai_table_edit_btn")
                            ) {
                                Text(
                                    text = "✨",
                                    fontSize = 16.sp
                                )
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Table", tint = Color.Gray)
                            }
                        }
                    } else {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Scrollable Table Content with natural intrinsic column widths
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentWidth(Alignment.Start)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, gridBorderColor, RoundedCornerShape(8.dp))
                ) {
                    // Header Row
                    if (block.hasHeaderRow) {
                        Row(
                            modifier = Modifier
                                .wrapContentWidth(Alignment.Start)
                                .background(Color(0xFFF1F5F9))
                                .drawBehind {
                                    // Bottom border under header
                                    drawLine(
                                        color = gridBorderColor,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            columnWidths.forEachIndexed { colIdx, colWidth ->
                                val headerText = block.headers.getOrElse(colIdx) { "" }
                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .drawBehind {
                                            if (colIdx < columnWidths.size - 1) {
                                                // Crisp vertical divider line to next column
                                                drawLine(
                                                    color = gridBorderColor,
                                                    start = Offset(size.width, 0f),
                                                    end = Offset(size.width, size.height),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (effectiveEditMode) {
                                        OutlinedTextField(
                                            value = headerText,
                                            onValueChange = { newText ->
                                                val updatedHeaders = block.headers.toMutableList()
                                                while (updatedHeaders.size <= colIdx) {
                                                    updatedHeaders.add("")
                                                }
                                                updatedHeaders[colIdx] = newText
                                                onUpdate(block.copy(headers = updatedHeaders))
                                            },
                                            textStyle = headerTextStyle,
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(
                                            text = headerText,
                                            style = headerTextStyle,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Data Rows
                    block.rows.forEachIndexed { rowIdx, rowData ->
                        Row(
                            modifier = Modifier
                                .wrapContentWidth(Alignment.Start)
                                .background(if (rowIdx % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                .drawBehind {
                                    if (rowIdx < block.rows.size - 1) {
                                        // Bottom border between data rows
                                        drawLine(
                                            color = dividerColor,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            columnWidths.forEachIndexed { colIdx, colWidth ->
                                val cellVal = rowData.getOrElse(colIdx) { "" }
                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .drawBehind {
                                            if (colIdx < columnWidths.size - 1) {
                                                // Crisp vertical divider line between columns
                                                drawLine(
                                                    color = dividerColor,
                                                    start = Offset(size.width, 0f),
                                                    end = Offset(size.width, size.height),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (effectiveEditMode) {
                                        OutlinedTextField(
                                            value = cellVal,
                                            onValueChange = { newCell ->
                                                val updatedRows = block.rows.map { it.toMutableList() }.toMutableList()
                                                while (updatedRows[rowIdx].size <= colIdx) {
                                                    updatedRows[rowIdx].add("")
                                                }
                                                updatedRows[rowIdx][colIdx] = newCell
                                                onUpdate(block.copy(rows = updatedRows))
                                            },
                                            textStyle = cellTextStyle,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(
                                            text = cellVal,
                                            style = cellTextStyle,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            if (effectiveEditMode && block.rows.size > 1) {
                                IconButton(
                                    onClick = {
                                        val updated = block.rows.toMutableList().apply { removeAt(rowIdx) }
                                        onUpdate(block.copy(rows = updated))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Row", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Edit Actions for Table (Add Row / Add Column)
            if (effectiveEditMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val colCount = maxOf(block.headers.size, block.rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(1)
                            val newRow = List(colCount) { "" }
                            val updated = block.rows.toMutableList().apply { add(newRow) }
                            onUpdate(block.copy(rows = updated))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Row")
                    }

                    TextButton(
                        onClick = {
                            val newHeaders = block.headers.toMutableList().apply { add("Column ${size + 1}") }
                            val newRows = block.rows.map { row ->
                                row.toMutableList().apply { add("") }
                            }
                            onUpdate(block.copy(headers = newHeaders, rows = newRows))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Column")
                    }
                }
            }
        }
    }
}

