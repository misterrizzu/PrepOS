package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.StudyAIService
import com.example.model.ChartEntry
import com.example.model.ChartType
import com.example.model.DiagramEdge
import com.example.model.DiagramNode
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.ElementType
import com.example.model.NodeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBlockEditSheet(
    block: DocElement, // TableBlock, DiagramBlock, or ChartBlock
    subjectColor: Color = Color(0xFF6C63D9),
    apiKey: String = "",
    model: String = StudyAIService.DEFAULT_MODEL,
    onApply: (DocElement) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Quick actions per block type
    val quickActions = remember(block) {
        when (block) {
            is DocElement.TableBlock -> listOf(
                "→ Diagram",
                "→ Bullet List",
                "Add Row",
                "Simplify",
                "Translate"
            )
            is DocElement.DiagramBlock -> listOf(
                "→ Table",
                "→ Bullet List",
                "Add Nodes",
                "Simplify",
                "Expand"
            )
            is DocElement.ChartBlock -> listOf(
                "→ Bar",
                "→ Donut",
                "→ Rings",
                "Update Labels",
                "Translate"
            )
            else -> emptyList()
        }
    }

    var selectedAction by remember { mutableStateOf<String?>(quickActions.firstOrNull()) }
    var customInstruction by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generatedResultBlock by remember { mutableStateOf<DocElement?>(null) }
    var currentApiKey by remember { mutableStateOf(apiKey) }
    var showConnectApiKeyDialog by remember { mutableStateOf(false) }

    val blockTypeTitle = when (block) {
        is DocElement.TableBlock -> "Table"
        is DocElement.DiagramBlock -> "Diagram"
        is DocElement.ChartBlock -> "Chart"
        else -> "Block"
    }

    fun executeAiEdit() {
        val action = selectedAction
        val instruction = customInstruction.trim()

        if (action == null && instruction.isBlank()) return

        if (!com.example.ai.StudyAIService.isGeminiKeyConfigured(currentApiKey)) {
            showConnectApiKeyDialog = true
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val blockJson = block.toJson().toString()
                val prompt = when {
                    instruction.isNotBlank() && action == null -> {
                        AiBlockPrompts.custom(blockJson, blockTypeTitle, instruction)
                    }
                    block is DocElement.TableBlock -> {
                        when (action) {
                            "→ Diagram" -> AiBlockPrompts.tableToDiagram(blockJson)
                            "→ Bullet List" -> AiBlockPrompts.tableToBullets(blockJson)
                            "Add Row" -> AiBlockPrompts.tableAddRow(blockJson)
                            "Simplify" -> AiBlockPrompts.tableSimplify(blockJson)
                            "Translate" -> AiBlockPrompts.tableTranslate(blockJson)
                            else -> AiBlockPrompts.custom(blockJson, "TABLE", instruction.ifBlank { action ?: "" })
                        }
                    }
                    block is DocElement.DiagramBlock -> {
                        when (action) {
                            "→ Table" -> AiBlockPrompts.diagramToTable(blockJson)
                            "→ Bullet List" -> AiBlockPrompts.diagramToBullets(blockJson)
                            "Add Nodes" -> AiBlockPrompts.diagramAddNodes(blockJson, instruction)
                            "Simplify" -> AiBlockPrompts.diagramSimplify(blockJson)
                            "Expand" -> AiBlockPrompts.diagramExpand(blockJson)
                            else -> AiBlockPrompts.custom(blockJson, "DIAGRAM", instruction.ifBlank { action ?: "" })
                        }
                    }
                    block is DocElement.ChartBlock -> {
                        when (action) {
                            "→ Bar" -> AiBlockPrompts.chartChangeType(blockJson, "BAR")
                            "→ Donut" -> AiBlockPrompts.chartChangeType(blockJson, "DONUT")
                            "→ Rings" -> AiBlockPrompts.chartChangeType(blockJson, "PROGRESS_RINGS")
                            "Update Labels" -> AiBlockPrompts.chartUpdateLabels(blockJson)
                            "Translate" -> AiBlockPrompts.chartTranslate(blockJson)
                            else -> AiBlockPrompts.custom(blockJson, "CHART", instruction.ifBlank { action ?: "" })
                        }
                    }
                    else -> AiBlockPrompts.custom(blockJson, blockTypeTitle, instruction.ifBlank { action ?: "" })
                }

                val aiResult = withContext(Dispatchers.IO) {
                    StudyAIService.executeRawAiPrompt(
                        prompt = prompt,
                        apiKey = currentApiKey,
                        model = model
                    )
                }

                aiResult.onSuccess { rawOutput ->
                    val parsed = parseAiResponse(rawOutput, block, action ?: "")
                    if (parsed != null) {
                        generatedResultBlock = parsed
                    } else {
                        errorMessage = "AI returned an unrecognized format. Tap Retry to try again."
                    }
                }.onFailure { err ->
                    errorMessage = err.message ?: "Failed to generate AI block edit. Check your API key in Settings."
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An error occurred while editing block with AI."
            } finally {
                isLoading = false
            }
        }
    }

    if (showConnectApiKeyDialog) {
        com.example.ui.components.ConnectApiKeyDialog(
            onDismiss = { showConnectApiKeyDialog = false },
            onKeySaved = { savedKey ->
                showConnectApiKeyDialog = false
                currentApiKey = savedKey
                executeAiEdit()
            },
            featureTitle = "AI $blockTypeTitle Transformation"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: ✨ AI Edit + Block Type Chip
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
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF4B38B7), Color(0xFF7B5CE7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI Edit",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 16.sp
                        )
                    )
                }

                // Block Type Chip
                Surface(
                    color = subjectColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, subjectColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = blockTypeTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = subjectColor
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // If no result yet, show prompt actions and input
            if (generatedResultBlock == null) {
                // Quick Action Chips
                if (quickActions.isNotEmpty()) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickActions.forEach { action ->
                            val isSelected = selectedAction == action
                            val chipBg = if (isSelected) {
                                Color(0xFF6C63D9).copy(alpha = 0.20f)
                            } else {
                                if (isDark) Color(0xFF1A1B2E) else Color(0xFFF1F3F8)
                            }
                            val borderStroke = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6C63D9))
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedAction = if (isSelected) null else action
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = chipBg,
                                border = borderStroke
                            ) {
                                Text(
                                    text = action,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isSelected) Color(0xFF6C63D9) else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Custom Instruction TextField
                OutlinedTextField(
                    value = customInstruction,
                    onValueChange = { customInstruction = it },
                    placeholder = {
                        Text(
                            text = "Tell AI what to do...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        )
                    },
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF13152B) else Color(0xFFF4F6FA),
                        unfocusedContainerColor = if (isDark) Color(0xFF13152B) else Color(0xFFF4F6FA),
                        focusedBorderColor = Color(0xFF6C63D9),
                        unfocusedBorderColor = if (isDark) Color(0xFF272F45) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error message banner if any
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons: Cancel + Go
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Cancel",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val canSubmit = (selectedAction != null || customInstruction.isNotBlank()) && !isLoading

                    Button(
                        onClick = { executeAiEdit() },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63D9),
                            disabledContainerColor = Color(0xFF6C63D9).copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("ai_block_go_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...", color = Color.White)
                        } else {
                            Text(
                                text = "✨ Go →",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            } else {
                // Preview Before Apply Section
                val resultBlock = generatedResultBlock!!

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Original Label & Preview
                    Text(
                        text = "── ORIGINAL ──",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(0.40f)
                    ) {
                        RenderBlockPreview(block)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Result Label & Preview
                    Text(
                        text = "── AI RESULT ──",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF6C63D9),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF6C63D9).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    ) {
                        RenderBlockPreview(resultBlock)
                    }

                    // Error in case user wants to retry
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons: [↩ Retry] + [✅ Apply]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                generatedResultBlock = null
                                executeAiEdit()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }

                        Button(
                            onClick = {
                                onApply(resultBlock)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63D9)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("ai_block_apply_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderBlockPreview(block: DocElement) {
    when (block) {
        is DocElement.TableBlock -> {
            TableCard(
                block = block,
                isEditMode = false,
                onUpdate = {},
                onDelete = {}
            )
        }
        is DocElement.DiagramBlock -> {
            DiagramCard(
                block = block,
                isEditMode = false,
                onUpdate = {},
                onDelete = {}
            )
        }
        is DocElement.ChartBlock -> {
            ChartCard(
                block = block,
                isEditMode = false,
                onUpdate = {},
                onDelete = {}
            )
        }
        is DocElement.TextBlock -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        else -> {
            Text(
                text = "Preview not available for this block",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun cleanMarkdownJson(raw: String): String {
    var text = raw.trim()
    if (text.startsWith("```json", ignoreCase = true)) {
        text = text.substring(7)
    } else if (text.startsWith("```")) {
        text = text.substring(3)
    }
    if (text.endsWith("```")) {
        text = text.substring(0, text.length - 3)
    }
    return text.trim()
}

fun parseAiResponse(
    rawResponse: String,
    originalBlock: DocElement,
    selectedAction: String
): DocElement? {
    val cleaned = cleanMarkdownJson(rawResponse)

    // Check if bullet list conversion
    if (selectedAction == "→ Bullet List" || cleaned.startsWith("•") || cleaned.startsWith("-")) {
        return DocElement.TextBlock(
            blockId = originalBlock.id,
            blockType = ElementType.BULLET_LIST,
            text = cleaned
        )
    }

    // Try to extract JSON
    val jsonObj = try {
        JSONObject(cleaned)
    } catch (e: Exception) {
        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        if (startIdx >= 0 && endIdx > startIdx) {
            try {
                JSONObject(cleaned.substring(startIdx, endIdx + 1))
            } catch (e2: Exception) { null }
        } else null
    }

    if (jsonObj != null) {
        // 1. Table check
        if (jsonObj.has("headers") || jsonObj.has("rows")) {
            val headersArray = jsonObj.optJSONArray("headers") ?: JSONArray()
            val headers = mutableListOf<String>()
            for (i in 0 until headersArray.length()) {
                headers.add(headersArray.optString(i))
            }
            val rowsArray = jsonObj.optJSONArray("rows") ?: JSONArray()
            val rows = mutableListOf<List<String>>()
            for (i in 0 until rowsArray.length()) {
                val rowArray = rowsArray.optJSONArray(i) ?: JSONArray()
                val row = mutableListOf<String>()
                for (j in 0 until rowArray.length()) {
                    row.add(rowArray.optString(j))
                }
                rows.add(row)
            }
            return DocElement.TableBlock(
                blockId = originalBlock.id,
                title = jsonObj.optString("title", if (originalBlock is DocElement.TableBlock) originalBlock.title else "Converted Table"),
                headers = if (headers.isEmpty()) listOf("Item", "Detail") else headers,
                rows = if (rows.isEmpty()) listOf(listOf("Row 1", "Detail 1")) else rows,
                hasHeaderRow = jsonObj.optBoolean("hasHeaderRow", true)
            )
        }

        // 2. Diagram check
        if (jsonObj.has("nodes") || jsonObj.has("edges") || jsonObj.has("structuredNodes") || jsonObj.has("diagramType") || jsonObj.has("type")) {
            val diagTypeStr = jsonObj.optString("diagramType", jsonObj.optString("type", DiagramType.FLOWCHART.name))
            val diagType = try {
                DiagramType.valueOf(diagTypeStr.uppercase())
            } catch (e: Exception) {
                when (diagTypeStr.uppercase()) {
                    "TREE", "HIERARCHY" -> DiagramType.TREE
                    "COMPARISON" -> DiagramType.COMPARISON
                    "ASCII" -> DiagramType.ASCII
                    "FORMULA" -> DiagramType.FORMULA
                    else -> DiagramType.FLOWCHART
                }
            }

            val nodes = mutableListOf<String>()
            val structuredNodes = mutableListOf<DiagramNode>()
            val nArr = jsonObj.optJSONArray("nodes")
            if (nArr != null) {
                for (i in 0 until nArr.length()) {
                    val item = nArr.opt(i)
                    if (item is JSONObject) {
                        val node = DiagramNode.fromJson(item)
                        structuredNodes.add(node)
                        nodes.add(node.label)
                    } else if (item is String) {
                        nodes.add(item)
                        structuredNodes.add(DiagramNode(label = item))
                    }
                }
            }
            val sArr = jsonObj.optJSONArray("structuredNodes")
            if (sArr != null) {
                for (i in 0 until sArr.length()) {
                    val sObj = sArr.optJSONObject(i)
                    if (sObj != null) structuredNodes.add(DiagramNode.fromJson(sObj))
                }
            }
            val edges = mutableListOf<DiagramEdge>()
            val eArr = jsonObj.optJSONArray("edges")
            if (eArr != null) {
                for (i in 0 until eArr.length()) {
                    val eObj = eArr.optJSONObject(i)
                    if (eObj != null) edges.add(DiagramEdge.fromJson(eObj))
                }
            }

            return DocElement.DiagramBlock(
                blockId = originalBlock.id,
                diagramType = diagType,
                title = jsonObj.optString("title", if (originalBlock is DocElement.DiagramBlock) originalBlock.title else "Diagram"),
                nodes = if (nodes.isEmpty()) listOf("Node 1", "Node 2") else nodes,
                structuredNodes = structuredNodes,
                edges = edges,
                rawContent = jsonObj.optString("rawContent", "")
            )
        }

        // 3. Chart check
        if (jsonObj.has("entries") || jsonObj.has("chartType")) {
            val chartTypeStr = jsonObj.optString("chartType", jsonObj.optString("type", ChartType.BAR.name))
            val chartType = try {
                ChartType.valueOf(chartTypeStr.uppercase())
            } catch (e: Exception) {
                when (chartTypeStr.uppercase()) {
                    "DONUT" -> ChartType.DONUT
                    "PIE" -> ChartType.PIE
                    "PROGRESS_RINGS", "RINGS" -> ChartType.PROGRESS_RINGS
                    else -> ChartType.BAR
                }
            }
            val entries = mutableListOf<ChartEntry>()
            val entArr = jsonObj.optJSONArray("entries")
            if (entArr != null) {
                for (i in 0 until entArr.length()) {
                    val eObj = entArr.optJSONObject(i)
                    if (eObj != null) entries.add(ChartEntry.fromJson(eObj))
                }
            }

            return DocElement.ChartBlock(
                blockId = originalBlock.id,
                chartType = chartType,
                title = jsonObj.optString("title", if (originalBlock is DocElement.ChartBlock) originalBlock.title else "Chart"),
                subtitle = jsonObj.optString("subtitle", ""),
                entries = if (entries.isEmpty()) listOf(ChartEntry("Item 1", 50f, "#6C63D9")) else entries
            )
        }
    }

    // Fallback: If original was a known block, return it with updated title or raw text
    return when (originalBlock) {
        is DocElement.TableBlock -> originalBlock
        is DocElement.DiagramBlock -> originalBlock.copy(rawContent = cleaned)
        is DocElement.ChartBlock -> originalBlock
        else -> null
    }
}
