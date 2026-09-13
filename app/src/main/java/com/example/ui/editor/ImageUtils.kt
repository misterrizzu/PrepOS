package com.example.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

data class ImageProcessResult(
    val bitmap: Bitmap,
    val base64Data: String,
    val sizeBytes: Int,
    val sizeText: String,
    val file: File? = null,
    val uri: Uri? = null
)

object ImageUtils {

    fun processImageUriForAi(context: Context, uri: Uri, maxDimension: Int = 1280): ImageProcessResult? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            var rotation = 0
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotation = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (_: Exception) {}

            processBitmap(original, rotation, maxDimension).copy(uri = uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun processImageFileForAi(file: File, maxDimension: Int = 1280): ImageProcessResult? {
        return try {
            if (!file.exists() || file.length() == 0L) return null
            val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null

            var rotation = 0
            try {
                val exif = ExifInterface(file.absolutePath)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (_: Exception) {}

            processBitmap(original, rotation, maxDimension).copy(file = file, uri = Uri.fromFile(file))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processBitmap(src: Bitmap, rotation: Int, maxDimension: Int): ImageProcessResult {
        var bmp = src
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            bmp = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }

        val width = bmp.width
        val height = bmp.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val targetWidth: Int
            val targetHeight: Int
            if (ratio > 1f) {
                targetWidth = maxDimension
                targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                targetHeight = maxDimension
                targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
        } else {
            bmp
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val compressedBytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

        return ImageProcessResult(
            bitmap = scaledBitmap,
            base64Data = base64,
            sizeBytes = compressedBytes.size,
            sizeText = "${(compressedBytes.size / 1024).coerceAtLeast(1)} KB"
        )
    }
}
