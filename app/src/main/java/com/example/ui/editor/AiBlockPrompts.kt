package com.example.ui.editor

import com.example.model.ChartType
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.ElementType
import org.json.JSONArray
import org.json.JSONObject

object AiBlockPrompts {

    // TABLE → DIAGRAM
    fun tableToDiagram(tableJson: String): String = """
        Convert this table data into a hierarchy or flow diagram.
        Return ONLY valid JSON in this exact format, nothing else (no markdown wrappers or explanation):
        {
          "type": "TREE",
          "title": "Diagram Title",
          "nodes": [
            {"id": "n1", "label": "Label 1", "sublabel": "optional description", "style": "DEFAULT", "shape": "ROUNDED_RECT"},
            {"id": "n2", "label": "Label 2", "sublabel": "", "style": "HIGHLIGHT", "shape": "DIAMOND"}
          ],
          "edges": [
            {"fromId": "n1", "toId": "n2", "label": "relates to", "style": "ARROW"}
          ]
        }
        Node styles: DEFAULT, HIGHLIGHT, MUTED
        Node shapes: ROUNDED_RECT, RECTANGLE, CIRCLE, DIAMOND, HEXAGON, CAPSULE
        Types: FLOWCHART, TREE, COMPARISON, ASCII
        Note: Use mathematical symbols where appropriate (e.g., √ for root, x², x³, xⁿ for powers, ÷ for divide, ±, ≤, ≥, ≠, ≈, π, θ).
        
        TABLE JSON: $tableJson
    """.trimIndent()

    // TABLE → BULLETS
    fun tableToBullets(tableJson: String): String = """
        Convert this table data into a clean bullet list.
        Format each row as: • [Column1]: [Column2]
        Return ONLY the bullet list, no explanation.
        
        TABLE JSON: $tableJson
    """.trimIndent()

    // TABLE → Add Row
    fun tableAddRow(tableJson: String): String = """
        Look at this table and suggest ONE new relevant row that fits the pattern and subject.
        Return ONLY the complete updated table JSON with the new row added.
        Keep exact same JSON format with headers and rows:
        {
          "title": "Table Title",
          "headers": ["Header 1", "Header 2"],
          "rows": [["Cell 1", "Cell 2"], ["New Cell 1", "New Cell 2"]],
          "hasHeaderRow": true
        }
        
        TABLE JSON: $tableJson
    """.trimIndent()

    // TABLE → Simplify
    fun tableSimplify(tableJson: String): String = """
        Rewrite all cell content in this table to be shorter, cleaner, and simpler for quick revision.
        Keep the same structure (same headers, same number of rows).
        Return ONLY the updated table JSON in this exact format:
        {
          "title": "Table Title",
          "headers": ["Header 1", "Header 2"],
          "rows": [["Cell 1", "Cell 2"]],
          "hasHeaderRow": true
        }
        
        TABLE JSON: $tableJson
    """.trimIndent()

    // TABLE → Translate
    fun tableTranslate(tableJson: String, targetLang: String = "Hindi"): String = """
        Translate all text in this table to $targetLang.
        Keep standard technical, exam, and scientific terms in English if helpful.
        Return ONLY the updated table JSON in this exact format:
        {
          "title": "Table Title",
          "headers": ["Header 1", "Header 2"],
          "rows": [["Cell 1", "Cell 2"]],
          "hasHeaderRow": true
        }
        
        TABLE JSON: $tableJson
    """.trimIndent()

    // DIAGRAM → TABLE
    fun diagramToTable(diagramJson: String): String = """
        Convert this diagram into a clean 2-column or 3-column table.
        Column 1: Concept/Node label. Column 2: Details/Relationships.
        Return ONLY valid JSON in this exact format:
        {
          "title": "Converted Table",
          "headers": ["Concept", "Relationship / Details"],
          "rows": [["Node A", "Description A"], ["Node B", "Description B"]],
          "hasHeaderRow": true
        }
        
        DIAGRAM JSON: $diagramJson
    """.trimIndent()

    // DIAGRAM → BULLETS
    fun diagramToBullets(diagramJson: String): String = """
        Convert this diagram into a structured bullet list.
        Show hierarchy with indentation using spaces.
        Format: • Parent\n  • Child\n  • Child\n• Next Parent
        Return ONLY the bullet list, no explanation.
        
        DIAGRAM JSON: $diagramJson
    """.trimIndent()

    // DIAGRAM → Add Nodes
    fun diagramAddNodes(diagramJson: String, instruction: String = ""): String = """
        Add more relevant nodes and connecting edges to this diagram. $instruction
        Keep existing nodes and edges. Add new ones with unique IDs (e.g. n10, n11).
        Nodes can have shapes: ROUNDED_RECT, RECTANGLE, CIRCLE, DIAMOND, HEXAGON, CAPSULE.
        For math/science concepts, render mathematical symbols like √, powers (x², y³), ÷, ±, ≤, ≥, ≠, ≈, π, θ.
        Return ONLY the complete updated diagram JSON:
        {
          "diagramType": "FLOWCHART",
          "title": "Updated Diagram",
          "nodes": ["Node 1", "Node 2", "Node 3"],
          "structuredNodes": [
            {"id": "n1", "label": "Node 1", "sublabel": "", "style": "DEFAULT", "shape": "ROUNDED_RECT"},
            {"id": "n2", "label": "Node 2", "sublabel": "", "style": "HIGHLIGHT", "shape": "DIAMOND"}
          ],
          "edges": [
            {"fromId": "n1", "toId": "n2", "label": "leads to", "style": "ARROW"}
          ]
        }
        
        DIAGRAM JSON: $diagramJson
    """.trimIndent()

    // DIAGRAM → Simplify
    fun diagramSimplify(diagramJson: String): String = """
        Simplify this diagram. Merge similar nodes and remove redundant details.
        Keep the core structure and most high-yield concepts.
        Return ONLY the updated diagram JSON:
        {
          "diagramType": "FLOWCHART",
          "title": "Simplified Diagram",
          "nodes": ["Node 1", "Node 2"],
          "structuredNodes": [
            {"id": "n1", "label": "Node 1", "sublabel": "", "style": "DEFAULT"}
          ],
          "edges": []
        }
        
        DIAGRAM JSON: $diagramJson
    """.trimIndent()

    // DIAGRAM → Expand
    fun diagramExpand(diagramJson: String): String = """
        Expand this diagram by adding deeper academic explanation and sublabels.
        Add sublabels to nodes that don't have them and add secondary child nodes where appropriate.
        Return ONLY the updated diagram JSON:
        {
          "diagramType": "TREE",
          "title": "Expanded Concept Map",
          "nodes": ["Node 1", "Node 2"],
          "structuredNodes": [
            {"id": "n1", "label": "Node 1", "sublabel": "Detailed definition", "style": "DEFAULT"}
          ],
          "edges": []
        }
        
        DIAGRAM JSON: $diagramJson
    """.trimIndent()

    // CHART → Change Type
    fun chartChangeType(chartJson: String, newType: String): String = """
        Change this chart's type to $newType.
        Keep all entries (label, value, colorHex) exactly the same.
        Return ONLY the updated chart JSON in this exact format:
        {
          "chartType": "$newType",
          "title": "Chart Title",
          "subtitle": "Subtitle",
          "entries": [
            {"label": "Topic A", "value": 45.0, "colorHex": "#6C63D9"}
          ]
        }
        Valid types: BAR, PIE, DONUT, PROGRESS_RINGS
        
        CHART JSON: $chartJson
    """.trimIndent()

    // CHART → Update Labels
    fun chartUpdateLabels(chartJson: String): String = """
        Rewrite the chart entry labels to be clearer, more concise, and highly descriptive for exam study.
        Keep values and colorHex exactly the same.
        Return ONLY the updated chart JSON:
        {
          "chartType": "BAR",
          "title": "Chart Title",
          "subtitle": "Subtitle",
          "entries": [
            {"label": "Updated Label A", "value": 45.0, "colorHex": "#6C63D9"}
          ]
        }
        
        CHART JSON: $chartJson
    """.trimIndent()

    // CHART → Translate Labels
    fun chartTranslate(chartJson: String, targetLang: String = "Hindi"): String = """
        Translate chart labels to $targetLang. Keep values and colorHex same.
        Return ONLY the updated chart JSON:
        {
          "chartType": "BAR",
          "title": "Translated Title",
          "subtitle": "Translated Subtitle",
          "entries": [
            {"label": "Translated Label", "value": 45.0, "colorHex": "#6C63D9"}
          ]
        }
        
        CHART JSON: $chartJson
    """.trimIndent()

    // Custom instruction — figure out what to return
    fun custom(blockJson: String, blockType: String, instruction: String): String = """
        You are editing a $blockType block in a student study notes app.
        User instruction: $instruction
        
        If the result should be a TABLE: return {"headers":[],"rows":[[]], "title": ""} JSON
        If the result should be a DIAGRAM: return {"diagramType":"FLOWCHART","title":"","nodes":[],"structuredNodes":[{"id":"n1","label":"","shape":"ROUNDED_RECT","style":"DEFAULT"}],"edges":[]} JSON
        (Shapes allowed: ROUNDED_RECT, RECTANGLE, CIRCLE, DIAMOND, HEXAGON, CAPSULE. Math markings allowed: √, x², x³, ÷, ±, ≤, ≥, ≠, ≈, π, θ)
        If the result should be a CHART: return {"chartType":"BAR","title":"","subtitle":"","entries":[]} JSON
        If the result should be a BULLET LIST: return plain text starting with bullet points (•)
        Return ONLY the result (no markdown code blocks, no chat explanations).
        
        CURRENT BLOCK JSON: $blockJson
    """.trimIndent()
}
