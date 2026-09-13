package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance, zero-bloat utility to capture the current screen,
 * append a clean, branded purple watermark footer, and open the system Share Intent immediately.
 */
object ScreenshotShareManager {

    private const val BRAND_TITLE = "PrepOS"
    private const val BRAND_SUBTITLE = "Study Workspace • Notes • Tests • Offline"

    /**
     * Finds the Activity from any Compose Context.
     */
    fun findActivity(context: Context): Activity? {
        var currentContext: Context? = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * Captures the current active screen, attaches the sleek two-tier PrepOS Study Workspace watermark,
     * and opens the Android native share intent directly.
     */
    fun captureAndShare(
        context: Context,
        title: String = BRAND_TITLE,
        subtitle: String = BRAND_SUBTITLE,
        onComplete: (() -> Unit)? = null
    ) {
        val activity = findActivity(context)
        if (activity == null) {
            Toast.makeText(context, "Cannot capture screen: Activity not found", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(activity, "Capturing MCQ screenshot...", Toast.LENGTH_SHORT).show()

                val decorView = activity.window.decorView.rootView
                val width = decorView.width.coerceAtLeast(1)
                val height = decorView.height.coerceAtLeast(1)

                // 1. Capture current screen bitmap
                val screenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val screenCanvas = Canvas(screenBitmap)
                decorView.draw(screenCanvas)

                // 2. Add bottom watermark banner in background IO thread
                val shareUri = withContext(Dispatchers.IO) {
                    val density = activity.resources.displayMetrics.density
                    val bannerHeight = (64 * density).toInt().coerceIn(120, 260)
                    val totalHeight = height + bannerHeight

                    val finalBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
                    val finalCanvas = Canvas(finalBitmap)

                    // Draw original screenshot
                    finalCanvas.drawBitmap(screenBitmap, 0f, 0f, null)
                    screenBitmap.recycle()

                    // Draw sleek dark footer background
                    val bgPaint = Paint().apply {
                        color = Color.parseColor("#090D16") // Deep premium slate
                        style = Paint.Style.FILL
                    }
                    finalCanvas.drawRect(
                        0f,
                        height.toFloat(),
                        width.toFloat(),
                        totalHeight.toFloat(),
                        bgPaint
                    )

                    // Draw vibrant purple top separator line
                    val linePaint = Paint().apply {
                        color = Color.parseColor("#7C3AED") // Purple Accent
                        strokeWidth = (2.5f * density)
                    }
                    finalCanvas.drawLine(
                        0f,
                        height.toFloat(),
                        width.toFloat(),
                        height.toFloat(),
                        linePaint
                    )

                    // Draw Brand Title: "PrepOS" (Large, Bold, White / Violet)
                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FFFFFF")
                        textSize = 16f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }

                    // Draw Subtitle: "Study Workspace • Notes • Tests • Offline"
                    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#C4B5FD") // Light Lavender
                        textSize = 11.5f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textAlign = Paint.Align.CENTER
                    }

                    val centerY = height + (bannerHeight / 2f)
                    val titleY = centerY - (4 * density)
                    val subtitleY = centerY + (16 * density)

                    finalCanvas.drawText(title, width / 2f, titleY, titlePaint)
                    finalCanvas.drawText(subtitle, width / 2f, subtitleY, subtitlePaint)

                    // Save bitmap to cache dir
                    val cacheDir = File(activity.cacheDir, "shared_mcq")
                    if (!cacheDir.exists()) cacheDir.mkdirs()

                    val screenshotFile = File(cacheDir, "PrepOS_MCQ_${System.currentTimeMillis()}.png")
                    val fos = FileOutputStream(screenshotFile)
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    fos.flush()
                    fos.close()
                    finalBitmap.recycle()

                    FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.fileprovider",
                        screenshotFile
                    )
                }

                if (shareUri != null) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        putExtra(Intent.EXTRA_SUBJECT, "PrepOS Question")
                        putExtra(Intent.EXTRA_TEXT, "Practice MCQs on PrepOS — Your All-in-One Offline Study Workspace!")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share MCQ via")
                    activity.startActivity(chooser)
                    onComplete?.invoke()
                } else {
                    Toast.makeText(activity, "Failed to prepare screenshot", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(activity, "Error sharing screenshot: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
