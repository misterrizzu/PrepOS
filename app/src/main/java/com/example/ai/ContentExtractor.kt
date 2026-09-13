package com.example.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object ContentExtractor {

    /**
     * Extracts text from a Uri pointing to either a TXT file or a PDF file.
     */
    suspend fun extractTextFromUri(context: Context, uri: Uri, mimeType: String? = null): String = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val resolvedType = mimeType ?: contentResolver.getType(uri) ?: ""

            if (resolvedType.contains("pdf", ignoreCase = true) || uri.toString().endsWith(".pdf", ignoreCase = true)) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    extractTextFromPdfStream(stream)
                } ?: ""
            } else {
                // Read text / TXT / Markdown directly
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    reader.readText()
                } ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Extracts readable text strings from a PDF stream.
     * Uses a robust stream scanning algorithm to extract text objects (BT/ET blocks and literal strings)
     * without requiring heavy external binaries.
     */
    fun extractTextFromPdfStream(inputStream: InputStream): String {
        return try {
            val bytes = inputStream.readBytes()
            val textBuilder = StringBuilder()
            var inTextObject = false
            var i = 0
            val len = bytes.size

            while (i < len) {
                // Look for BT (Begin Text)
                if (!inTextObject && i + 2 < len && bytes[i] == 'B'.code.toByte() && bytes[i + 1] == 'T'.code.toByte() && isPdfDelimiter(bytes[i + 2])) {
                    inTextObject = true
                    i += 2
                    continue
                }

                // Look for ET (End Text)
                if (inTextObject && i + 2 < len && bytes[i] == 'E'.code.toByte() && bytes[i + 1] == 'T'.code.toByte() && isPdfDelimiter(bytes[i + 2])) {
                    inTextObject = false
                    textBuilder.append("\n")
                    i += 2
                    continue
                }

                if (inTextObject) {
                    // Extract literal string in parentheses: (Text here)
                    if (bytes[i] == '('.code.toByte()) {
                        i++
                        var depth = 1
                        val chunk = StringBuilder()
                        while (i < len && depth > 0) {
                            val b = bytes[i]
                            if (b == '\\'.code.toByte() && i + 1 < len) {
                                i++
                                chunk.append(bytes[i].toInt().toChar())
                            } else if (b == '('.code.toByte()) {
                                depth++
                                chunk.append('(')
                            } else if (b == ')'.code.toByte()) {
                                depth--
                                if (depth > 0) chunk.append(')')
                            } else {
                                chunk.append(b.toInt().toChar())
                            }
                            i++
                        }
                        textBuilder.append(chunk.toString()).append(" ")
                        continue
                    }
                    // Extract hex string: <48656c6c6f>
                    else if (bytes[i] == '<'.code.toByte() && i + 1 < len && bytes[i + 1] != '<'.code.toByte()) {
                        i++
                        val hexChunk = StringBuilder()
                        while (i < len && bytes[i] != '>'.code.toByte()) {
                            val c = bytes[i].toInt().toChar()
                            if (c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F') {
                                hexChunk.append(c)
                            }
                            i++
                        }
                        if (i < len && bytes[i] == '>'.code.toByte()) i++
                        val hexStr = hexChunk.toString()
                        if (hexStr.length % 2 == 0) {
                            for (h in 0 until hexStr.length step 2) {
                                try {
                                    val byteVal = hexStr.substring(h, h + 2).toInt(16)
                                    if (byteVal in 32..126 || byteVal == 10 || byteVal == 13) {
                                        textBuilder.append(byteVal.toChar())
                                    }
                                } catch (ignored: Exception) {}
                            }
                            textBuilder.append(" ")
                        }
                        continue
                    }
                }
                i++
            }

            val extracted = textBuilder.toString().trim()
            if (extracted.length > 50) {
                cleanExtractedPdfText(extracted)
            } else {
                // Fallback: extract ASCII readable string sequences from the file
                extractAsciiSequences(bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun isPdfDelimiter(b: Byte): Boolean {
        val c = b.toInt().toChar()
        return c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '/' || c == '[' || c == ']'
    }

    private fun extractAsciiSequences(bytes: ByteArray): String {
        val sb = StringBuilder()
        val currentWord = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in " .,:;!?'\"-()[]{}\n") {
                currentWord.append(c)
            } else {
                if (currentWord.length >= 4) {
                    sb.append(currentWord.toString()).append(" ")
                }
                currentWord.setLength(0)
            }
        }
        if (currentWord.length >= 4) {
            sb.append(currentWord.toString())
        }
        return cleanExtractedPdfText(sb.toString())
    }

    /**
     * Cleans up raw OCR/PDF scanner noise, multiple consecutive blank lines, and broken line breaks.
     */
    fun cleanExtractedPdfText(raw: String): String {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }
}
