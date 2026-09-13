package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.example.ui.components.RichMathText
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalloutType
import com.example.model.DiagramNode
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.NodeShape
import com.example.model.Quadruple
import com.example.util.DiamondShape
import com.example.util.HexagonShape
import com.example.util.MathNotationHelper

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@Composable
fun CalloutCard(
    block: DocElement.CalloutBlock,
    isEditMode: Boolean,
    onUpdate: (DocElement.CalloutBlock) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, accentColor, icon) = when (block.calloutType) {
        CalloutType.IMPORTANT -> Quadruple(
            Color(0xFFFEF2F2),
            Color(0xFFFCA5A5),
            Color(0xFFDC2626),
            Icons.Default.PriorityHigh
        )
        CalloutType.DEFINITION -> Quadruple(
            Color(0xFFEFF6FF),
            Color(0xFF93C5FD),
            Color(0xFF2563EB),
            Icons.Default.MenuBook
        )
        CalloutType.EXAM_TIP -> Quadruple(
            Color(0xFFFFFBEB),
            Color(0xFFFDE68A),
            Color(0xFFD97706),
            Icons.Default.Lightbulb
        )
        CalloutType.FORMULA -> Quadruple(
            Color(0xFFECFDF5),
            Color(0xFFA7F3D0),
            Color(0xFF059669),
            Icons.Default.Functions
        )
    }

    var editingTitle by remember(block.blockId, block.title) { mutableStateOf(block.title) }
    var editingContent by remember(block.blockId, block.content) { mutableStateOf(block.content) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = editingTitle,
                            onValueChange = {
                                editingTitle = it
                                onUpdate(block.copy(title = it))
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        )
                    }
                }

                if (isEditMode) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete callout",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isEditMode) {
                OutlinedTextField(
                    value = editingContent,
                    onValueChange = {
                        editingContent = it
                        onUpdate(block.copy(content = it))
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1E293B),
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor.copy(alpha = 0.5f)
                    )
                )
            } else {
                RichMathText(
                    text = block.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1E293B),
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DiagramCard(
    block: DocElement.DiagramBlock,
    isEditMode: Boolean,
    onUpdate: (DocElement.DiagramBlock) -> Unit,
    onDelete: () -> Unit,
    onAiEdit: (DocElement.DiagramBlock) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLocallyEditing by remember { mutableStateOf(false) }
    val effectiveEditMode = isEditMode || isLocallyEditing
    var nodesList by remember(block.blockId, block.nodes) { mutableStateOf(block.nodes) }
    var title by remember(block.blockId, block.title) { mutableStateOf(block.title) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isEditMode) {
                            isLocallyEditing = !isLocallyEditing
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type Icon, Title / Edit Field, Action Buttons (AI, Done, Delete)
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (block.diagramType) {
                                DiagramType.FLOWCHART -> Icons.Default.Timeline
                                DiagramType.TREE -> Icons.Default.AccountTree
                                DiagramType.COMPARISON -> Icons.Default.CompareArrows
                                DiagramType.ASCII -> Icons.Default.Code
                                DiagramType.FORMULA -> Icons.Default.Functions
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    if (effectiveEditMode) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                onUpdate(block.copy(title = it))
                            },
                            placeholder = { Text("Diagram Title", fontSize = 14.sp) },
                            textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF6366F1).copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text(
                                text = block.title.ifBlank { "Structure & Process Diagram" },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.5.sp
                                )
                            )
                            Text(
                                text = when (block.diagramType) {
                                    DiagramType.FLOWCHART -> "Sequential Process Flow"
                                    DiagramType.TREE -> "Structure & Types Breakdown"
                                    DiagramType.COMPARISON -> "Side-by-Side Comparison"
                                    DiagramType.ASCII -> "ASCII / Technical View"
                                    DiagramType.FORMULA -> "Formula & Equation"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Header Actions: AI Sparkle Button + Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    // AI Sparkle Button (Available in both Read & Edit mode)
                    IconButton(
                        onClick = { onAiEdit(block) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF4B38B7), Color(0xFF7B5CE7))
                                )
                            )
                            .testTag("ai_diagram_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask AI to edit this diagram",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (effectiveEditMode) {
                        if (isLocallyEditing && !isEditMode) {
                            IconButton(
                                onClick = { isLocallyEditing = false },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF059669).copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done editing",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete diagram",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Diagram Type Switcher in Edit Mode
            if (effectiveEditMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = block.diagramType == DiagramType.FLOWCHART,
                        onClick = { onUpdate(block.copy(diagramType = DiagramType.FLOWCHART)) },
                        label = { Text("Flow", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = block.diagramType == DiagramType.TREE,
                        onClick = { onUpdate(block.copy(diagramType = DiagramType.TREE)) },
                        label = { Text("Hierarchy / Types", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = block.diagramType == DiagramType.COMPARISON,
                        onClick = { onUpdate(block.copy(diagramType = DiagramType.COMPARISON)) },
                        label = { Text("Comparison", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = block.diagramType == DiagramType.ASCII,
                        onClick = { onUpdate(block.copy(diagramType = DiagramType.ASCII)) },
                        label = { Text("Code / ASCII", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // DIAGRAM RENDERING WITH UNIFORM SHAPES
            // ==========================================
            when (block.diagramType) {
                DiagramType.FLOWCHART -> {
                    // 1. FLOWCHART: Clean sequential cards with uniform rounded corners + connecting arrows
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        nodesList.forEachIndexed { index, nodeText ->
                            val baseColor = if (index % 2 == 0) Color(0xFF6366F1) else Color(0xFF0EA5E9)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = baseColor.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, baseColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = if (effectiveEditMode) 6.dp else 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Step number badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(baseColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = baseColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    if (effectiveEditMode) {
                                        OutlinedTextField(
                                            value = nodeText,
                                            onValueChange = { newText ->
                                                val updated = nodesList.toMutableList()
                                                updated[index] = newText
                                                nodesList = updated
                                                onUpdate(block.copy(nodes = updated))
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = baseColor
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Text(
                                            text = MathNotationHelper.formatMathNotation(nodeText),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.5.sp
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (effectiveEditMode && nodesList.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val updated = nodesList.toMutableList().apply { removeAt(index) }
                                                nodesList = updated
                                                onUpdate(block.copy(nodes = updated))
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete step",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            // Downward Arrow between nodes
                            if (index < nodesList.size - 1) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = baseColor.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .padding(vertical = 3.dp)
                                        .size(16.dp)
                                )
                            }
                        }

                        if (effectiveEditMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF6366F1).copy(alpha = 0.12f))
                                    .clickable {
                                        val updated = nodesList.toMutableList().apply {
                                            add("Step ${size + 1}: Next Action")
                                        }
                                        nodesList = updated
                                        onUpdate(block.copy(nodes = updated))
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Step Node", color = Color(0xFF6366F1), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }

                DiagramType.TREE, DiagramType.FORMULA -> {
                    // 2. HIERARCHY / TYPES / PARTS: Top Root/Category Box -> Distinct Sub-parts Grid Cards
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Top Root Container (The Central Topic / Category)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = block.title.ifBlank { "Main Topic & Components" },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5),
                                        fontSize = 14.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Branch connector indicator
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(10.dp)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.5f))
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFF6366F1).copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Sub-parts / Types Grid Layout: Distinct, uniform rounded cards for each part
                        val colors = listOf(
                            Color(0xFF2563EB), // Blue
                            Color(0xFF059669), // Emerald
                            Color(0xFFD97706), // Amber
                            Color(0xFF7C3AED), // Purple
                            Color(0xFF0D9488), // Teal
                            Color(0xFFE11D48)  // Rose
                        )

                        // If 2 or more sub-nodes, render in responsive pairs
                        val pairs = nodesList.chunked(2)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pairs.forEachIndexed { pairIdx, pairList ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pairList.forEachIndexed { itemIdx, itemText ->
                                        val actualIdx = pairIdx * 2 + itemIdx
                                        val itemColor = colors[actualIdx % colors.size]

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = itemColor.copy(alpha = 0.08f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, itemColor.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
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
                                                                .size(7.dp)
                                                                .clip(CircleShape)
                                                                .background(itemColor)
                                                        )
                                                        val cleanLabel = if (itemText.contains(":")) {
                                                            itemText.substringBefore(":").trim().removePrefix("-").removePrefix("•").trim()
                                                        } else ""
                                                        if (cleanLabel.isNotBlank() && cleanLabel.length <= 28) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = cleanLabel,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = itemColor,
                                                                    fontSize = 11.5.sp
                                                                ),
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }

                                                    if (effectiveEditMode && nodesList.size > 1) {
                                                        IconButton(
                                                            onClick = {
                                                                val updated = nodesList.toMutableList().apply { removeAt(actualIdx) }
                                                                nodesList = updated
                                                                onUpdate(block.copy(nodes = updated))
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Delete item",
                                                                modifier = Modifier.size(14.dp),
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                if (effectiveEditMode) {
                                                    OutlinedTextField(
                                                        value = itemText,
                                                        onValueChange = { newText ->
                                                            val updated = nodesList.toMutableList()
                                                            updated[actualIdx] = newText
                                                            nodesList = updated
                                                            onUpdate(block.copy(nodes = updated))
                                                        },
                                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            unfocusedBorderColor = Color.Transparent,
                                                            focusedBorderColor = itemColor
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else {
                                                    val bodyText = if (itemText.contains(":") && itemText.substringBefore(":").trim().length <= 28) {
                                                        itemText.substringAfter(":").trim()
                                                    } else {
                                                        itemText.removePrefix("-").removePrefix("•").trim()
                                                    }
                                                    Text(
                                                        text = MathNotationHelper.formatMathNotation(bodyText.ifBlank { itemText }),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 12.5.sp,
                                                            lineHeight = 16.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // If odd number in last row, add dummy space filler so weight aligns nicely
                                    if (pairList.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (effectiveEditMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF6366F1).copy(alpha = 0.12f))
                                    .clickable {
                                        val updated = nodesList.toMutableList().apply {
                                            add("New Component / Type ${size + 1}")
                                        }
                                        nodesList = updated
                                        onUpdate(block.copy(nodes = updated))
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Sub-Part / Type", color = Color(0xFF6366F1), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }

                DiagramType.COMPARISON -> {
                    // 3. COMPARISON: Two clean distinct side-by-side columns without hardcoded/dummy Aspect labels
                    val titleParts = block.title.split(Regex("(?i)\\s+(vs\\.?|versus|and|compared to|\\/)\\s+"))
                    val leftSideTitle = if (titleParts.size >= 2) titleParts[0].trim() else "Side A"
                    val rightSideTitle = if (titleParts.size >= 2) titleParts[1].trim() else "Side B"

                    fun cleanComparisonText(raw: String): String {
                        return raw
                            .replace(Regex("^(?i)(aspect|feature|parameter|point|step|criteria|criterion)\\s*[:\\-]\\s*"), "")
                            .replace(Regex("^(?i)(side\\s*[12a-b]|col(umn)?\\s*[12])\\s*[:\\-]\\s*"), "")
                            .removePrefix("-").removePrefix("•").trim()
                    }

                    val half = (nodesList.size + 1) / 2
                    val leftCol = nodesList.take(half)
                    val rightCol = nodesList.drop(half)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Column Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF2563EB).copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF2563EB).copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = leftSideTitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8)),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                leftCol.forEachIndexed { i, item ->
                                    if (effectiveEditMode) {
                                        OutlinedTextField(
                                            value = item,
                                            onValueChange = { newText ->
                                                val updated = nodesList.toMutableList()
                                                updated[i] = newText
                                                nodesList = updated
                                                onUpdate(block.copy(nodes = updated))
                                            },
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = Color(0xFF2563EB)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        val cleaned = cleanComparisonText(item)
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                color = Color(0xFF2563EB),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = MathNotationHelper.formatMathNotation(cleaned),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.5.sp,
                                                    lineHeight = 16.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF059669).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF059669).copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = rightSideTitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF047857)),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                rightCol.forEachIndexed { i, item ->
                                    val actualIdx = half + i
                                    if (effectiveEditMode) {
                                        OutlinedTextField(
                                            value = item,
                                            onValueChange = { newText ->
                                                val updated = nodesList.toMutableList()
                                                updated[actualIdx] = newText
                                                nodesList = updated
                                                onUpdate(block.copy(nodes = updated))
                                            },
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = Color(0xFF059669)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        val cleaned = cleanComparisonText(item)
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                color = Color(0xFF059669),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = MathNotationHelper.formatMathNotation(cleaned),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.5.sp,
                                                    lineHeight = 16.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (effectiveEditMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.12f))
                                .clickable {
                                    val updated = nodesList.toMutableList().apply {
                                        add("Comparison item ${size + 1}")
                                    }
                                    nodesList = updated
                                    onUpdate(block.copy(nodes = updated))
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Comparison Item", color = Color(0xFF6366F1), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }

                DiagramType.ASCII -> {
                    // 4. ASCII / Code Technical View
                    val asciiScrollState = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(asciiScrollState)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = if (block.rawContent.isNotBlank()) block.rawContent else nodesList.joinToString("\n  ↓\n"),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF38BDF8),
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
