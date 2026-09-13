package com.example.ui.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CalloutType
import com.example.model.DiagramNode
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.NodeShape
import com.example.util.MathNotationHelper

import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DonutLarge
import com.example.model.ChartEntry
import com.example.model.ChartType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertContentSheet(
    onDismiss: () -> Unit,
    onInsertElement: (DocElement) -> Unit,
    onPasteSmartContent: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableStateOf<String?>(null) } // null, "TABLE", "DIAGRAM", "CHART", "CALLOUT", "IMAGE", "PASTE"
    var pasteText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (selectedCategory) {
                        "TABLE" -> "Insert Study Table"
                        "DIAGRAM" -> "Insert Study Diagram"
                        "CHART" -> "Insert Data Chart"
                        "CALLOUT" -> "Insert Key Note / Callout"
                        "IMAGE" -> "Insert Diagram / Image"
                        else -> "Insert Into Document"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = {
                    if (selectedCategory != null) selectedCategory = null else onDismiss()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedCategory) {
                null -> {
                    // Main Category Grid
                    InsertOptionCard(
                        title = "Key Note / Concept Callout",
                        subtitle = "Highlight important formulas, exam tips, definitions, or warnings",
                        icon = Icons.Default.Lightbulb,
                        color = Color(0xFFF59E0B),
                        onClick = { selectedCategory = "CALLOUT" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InsertOptionCard(
                        title = "Structured Study Table",
                        subtitle = "Organize comparisons, formulas, definitions, and vocabulary in grid columns",
                        icon = Icons.Default.TableChart,
                        color = Color(0xFF2563EB),
                        onClick = { selectedCategory = "TABLE" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InsertOptionCard(
                        title = "Study Diagram / Flowchart",
                        subtitle = "Visual step-by-step process flows, hierarchy trees, and comparison matrices",
                        icon = Icons.Default.AccountTree,
                        color = Color(0xFF7C3AED),
                        onClick = { selectedCategory = "DIAGRAM" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InsertOptionCard(
                        title = "Data Chart / Progress Rings",
                        subtitle = "Horizontal bars, distribution donut, and syllabus circular rings",
                        icon = Icons.Default.BarChart,
                        color = Color(0xFF6C63D9),
                        onClick = { selectedCategory = "CHART" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InsertOptionCard(
                        title = "Image / Illustrated Chart",
                        subtitle = "Insert study reference graphic or textbook diagram",
                        icon = Icons.Default.Image,
                        color = Color(0xFF059669),
                        onClick = { selectedCategory = "IMAGE" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InsertOptionCard(
                        title = "Smart Paste Study Content",
                        subtitle = "Paste study material, syllabus notes, or markdown to automatically split into headings & paragraphs",
                        icon = Icons.Default.ContentPaste,
                        color = Color(0xFF0D9488),
                        onClick = { selectedCategory = "PASTE" }
                    )
                }
                "CHART" -> {
                    InsertOptionCard(
                        title = "Horizontal Bar Chart",
                        subtitle = "Compare topic scores, performance, and chapter weights",
                        icon = Icons.Default.BarChart,
                        color = Color(0xFF6C63D9),
                        onClick = {
                            onInsertElement(
                                DocElement.ChartBlock(
                                    chartType = ChartType.BAR,
                                    title = "Topic Accuracy & Breakdown",
                                    subtitle = "Score percentages across key modules",
                                    entries = listOf(
                                        ChartEntry("Grammar & Rules", 75f, "#6C63D9"),
                                        ChartEntry("Vocabulary & Idioms", 60f, "#2563EB"),
                                        ChartEntry("Comprehension", 85f, "#059669"),
                                        ChartEntry("Mock Drills", 45f, "#D97706")
                                    )
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InsertOptionCard(
                        title = "Distribution Donut Chart",
                        subtitle = "Visual distribution and marks weightage breakdown",
                        icon = Icons.Default.DonutLarge,
                        color = Color(0xFF2563EB),
                        onClick = {
                            onInsertElement(
                                DocElement.ChartBlock(
                                    chartType = ChartType.DONUT,
                                    title = "Exam Marks Weightage",
                                    subtitle = "Distribution per topic in paper",
                                    entries = listOf(
                                        ChartEntry("General Knowledge", 30f, "#6C63D9"),
                                        ChartEntry("Reasoning Ability", 25f, "#2563EB"),
                                        ChartEntry("English Comprehension", 25f, "#059669"),
                                        ChartEntry("Computer Concepts", 20f, "#DB2777")
                                    )
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InsertOptionCard(
                        title = "Syllabus Progress Rings",
                        subtitle = "Circular completion indicators for chapters",
                        icon = Icons.Default.DataUsage,
                        color = Color(0xFF059669),
                        onClick = {
                            onInsertElement(
                                DocElement.ChartBlock(
                                    chartType = ChartType.PROGRESS_RINGS,
                                    title = "Chapter Mastery Rings",
                                    subtitle = "Target vs current completion",
                                    entries = listOf(
                                        ChartEntry("Theory", 80f, "#059669"),
                                        ChartEntry("PYQs", 65f, "#6C63D9"),
                                        ChartEntry("Revision", 40f, "#2563EB")
                                    )
                                )
                            )
                            onDismiss()
                        }
                    )
                }
                "PASTE" -> {
                    Text("Paste any study notes, articles, or markdown text below. It will be smartly split into headings, bullet lists, and paragraphs automatically:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        label = { Text("Paste Text / Notes Here") },
                        minLines = 6,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (pasteText.isNotBlank()) {
                                onPasteSmartContent(pasteText)
                            }
                            onDismiss()
                        },
                        enabled = pasteText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import & Split Content")
                    }
                }
                "CALLOUT" -> {
                    // Callout Type choices
                    Text("Select Callout Style:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(10.dp))

                    CalloutChoiceRow(
                        title = "Exam Tip & Strategy",
                        type = CalloutType.EXAM_TIP,
                        icon = Icons.Default.Lightbulb,
                        color = Color(0xFFD97706),
                        onSelect = {
                            onInsertElement(
                                DocElement.CalloutBlock(
                                    calloutType = CalloutType.EXAM_TIP,
                                    title = "Exam Strategy & Tip",
                                    content = "• Focus on standard patterns\n• Avoid unnecessary lengthy calculations"
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CalloutChoiceRow(
                        title = "Important Formula / Theorem",
                        type = CalloutType.FORMULA,
                        icon = Icons.Default.Functions,
                        color = Color(0xFF059669),
                        onSelect = {
                            onInsertElement(
                                DocElement.CalloutBlock(
                                    calloutType = CalloutType.FORMULA,
                                    title = "Quadratic & Energy Formulas",
                                    content = "Formula 1: x = (-b ± √(b² - 4ac)) ÷ (2a)\nFormula 2: E = mc² | F = G(m₁m₂) ÷ r²"
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CalloutChoiceRow(
                        title = "Formal Concept Definition",
                        type = CalloutType.DEFINITION,
                        icon = Icons.Default.MenuBook,
                        color = Color(0xFF2563EB),
                        onSelect = {
                            onInsertElement(
                                DocElement.CalloutBlock(
                                    calloutType = CalloutType.DEFINITION,
                                    title = "Definition",
                                    content = "Enter concise formal definition here..."
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CalloutChoiceRow(
                        title = "Crucial Warning / Exception",
                        type = CalloutType.IMPORTANT,
                        icon = Icons.Default.PriorityHigh,
                        color = Color(0xFFDC2626),
                        onSelect = {
                            onInsertElement(
                                DocElement.CalloutBlock(
                                    calloutType = CalloutType.IMPORTANT,
                                    title = "Important Exception",
                                    content = "Note: This rule does NOT apply when initial conditions are zero."
                                )
                            )
                            onDismiss()
                        }
                    )
                }
                "TABLE" -> {
                    var tableTitle by remember { mutableStateOf("Comparison Matrix") }
                    var colCount by remember { mutableStateOf(2) }

                    OutlinedTextField(
                        value = tableTitle,
                        onValueChange = { tableTitle = it },
                        label = { Text("Table Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val headers = listOf("Property / Term", "Description / Value")
                                val rows = listOf(
                                    listOf("Sample Point 1", "Detailed explanation 1"),
                                    listOf("Sample Point 2", "Detailed explanation 2")
                                )
                                onInsertElement(
                                    DocElement.TableBlock(
                                        title = tableTitle,
                                        headers = headers,
                                        rows = rows
                                    )
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Insert 2-Column Table")
                        }

                        Button(
                            onClick = {
                                val headers = listOf("Parameter", "Condition A", "Condition B")
                                val rows = listOf(
                                    listOf("Metric 1", "Value A1", "Value B1"),
                                    listOf("Metric 2", "Value A2", "Value B2")
                                )
                                onInsertElement(
                                    DocElement.TableBlock(
                                        title = tableTitle,
                                        headers = headers,
                                        rows = rows
                                    )
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Insert 3-Column Table")
                        }
                    }
                }
                "DIAGRAM" -> {
                    InsertOptionCard(
                        title = "Process Flowchart",
                        subtitle = "Sequential step nodes connected by directional flow",
                        icon = Icons.Default.Timeline,
                        color = Color(0xFF3B82F6),
                        onClick = {
                            onInsertElement(
                                DocElement.DiagramBlock(
                                    diagramType = DiagramType.FLOWCHART,
                                    title = "Execution Sequence",
                                    nodes = listOf("Phase 1: Input Data", "Phase 2: Processing & Validation", "Phase 3: Final Output"),
                                    structuredNodes = listOf(
                                        DiagramNode(id = "fc_1", label = "Phase 1: Input Data", shape = NodeShape.CAPSULE),
                                        DiagramNode(id = "fc_2", label = "Phase 2: Processing & Validation", shape = NodeShape.DIAMOND),
                                        DiagramNode(id = "fc_3", label = "Phase 3: Final Output", shape = NodeShape.CAPSULE)
                                    )
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InsertOptionCard(
                        title = "Classification Tree",
                        subtitle = "Hierarchy categories from general to specific",
                        icon = Icons.Default.AccountTree,
                        color = Color(0xFF8B5CF6),
                        onClick = {
                            onInsertElement(
                                DocElement.DiagramBlock(
                                    diagramType = DiagramType.TREE,
                                    title = "Classification Hierarchy",
                                    nodes = listOf("Root: Primary Subject", "Level 1: Category A & B", "Level 2: Concrete Examples"),
                                    structuredNodes = listOf(
                                        DiagramNode(id = "tr_1", label = "Root: Primary Subject", shape = NodeShape.ROUNDED_RECT),
                                        DiagramNode(id = "tr_2", label = "Level 1: Category A & B", shape = NodeShape.HEXAGON),
                                        DiagramNode(id = "tr_3", label = "Level 2: Concrete Examples", shape = NodeShape.CIRCLE)
                                    )
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InsertOptionCard(
                        title = "Comparison Columns Matrix",
                        subtitle = "Side-by-side contrast between concepts or parameters",
                        icon = Icons.Default.CompareArrows,
                        color = Color(0xFF059669),
                        onClick = {
                            onInsertElement(
                                DocElement.DiagramBlock(
                                    diagramType = DiagramType.COMPARISON,
                                    title = "Concept Comparison",
                                    nodes = listOf(
                                        "Feature A: Lightweight & Fast",
                                        "Feature A: Simple Configuration",
                                        "Feature B: Extensive Feature Set",
                                        "Feature B: Multi-platform Support"
                                    )
                                )
                            )
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InsertOptionCard(
                        title = "ASCII Diagram Box",
                        subtitle = "Clean monospace diagram for architecture & formulas",
                        icon = Icons.Default.Code,
                        color = Color(0xFF0F172A),
                        onClick = {
                            onInsertElement(
                                DocElement.DiagramBlock(
                                    diagramType = DiagramType.ASCII,
                                    title = "System Architecture",
                                    rawContent = "+-------------------+\n|   Input Request   |\n+-------------------+\n          |\n          v\n+-------------------+\n|  Core Controller  |\n+-------------------+\n          |\n          v\n+-------------------+\n|  Database Result  |\n+-------------------+"
                                )
                            )
                            onDismiss()
                        }
                    )
                }
                "IMAGE" -> {
                    var caption by remember { mutableStateOf("Reference Study Diagram") }
                    var imageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=600") }

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Image / Diagram URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onInsertElement(
                                DocElement.ImageBlock(
                                    imageUri = imageUrl.ifBlank { "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600" },
                                    caption = caption
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Insert Diagram Image")
                    }
                }
            }
        }
    }
}

@Composable
private fun InsertOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalloutChoiceRow(
    title: String,
    type: CalloutType,
    icon: ImageVector,
    color: Color,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            )
        }
    }
}
