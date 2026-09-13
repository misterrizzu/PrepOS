package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipBackupHelper {
    private const val BACKUP_ENTRY_NAME = "prepos_backup.json"

    /**
     * Writes backup JSON string directly to an output stream (e.g. user selected file URI) as a valid ZIP.
     */
    suspend fun writeBackupZipToStream(outputStream: OutputStream, backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                val entry = ZipEntry(BACKUP_ENTRY_NAME)
                zipOut.putNextEntry(entry)
                zipOut.write(backupJson.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
                zipOut.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads a ZIP backup file or stream from a Uri and extracts the JSON backup payload.
     * Also handles uncompressed JSON files gracefully as a fallback.
     */
    suspend fun readBackupJsonFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                readBackupJsonFromStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts backup JSON payload from an input stream.
     */
    fun readBackupJsonFromStream(inputStream: InputStream): String? {
        val buffered = BufferedInputStream(inputStream)
        buffered.mark(1024 * 1024)

        // Try reading as ZIP
        try {
            ZipInputStream(buffered).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && (entry.name.endsWith(".json") || entry.name == BACKUP_ENTRY_NAME)) {
                        val jsonBytes = zipIn.readBytes()
                        return String(jsonBytes, Charsets.UTF_8)
                    }
                    entry = zipIn.nextEntry
                }
            }
        } catch (e: Exception) {
            // Not a zip or error reading zip
        }

        // Fallback: Try reading directly as plain text JSON
        try {
            buffered.reset()
            return buffered.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Creates a temporary ZIP file in cache directory for sharing.
     */
    suspend fun createTempBackupZip(context: Context, backupJson: String): File? = withContext(Dispatchers.IO) {
        try {
            val fileName = "PrepOS_Backup_${System.currentTimeMillis()}.zip"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                writeBackupZipToStream(fos, backupJson)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
