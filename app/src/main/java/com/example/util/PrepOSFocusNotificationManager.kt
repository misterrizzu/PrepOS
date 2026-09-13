package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.viewmodel.ActiveFocusSession

object PrepOSFocusNotificationManager {

    private const val CHANNEL_FOCUS_ID = "prepos_focus_channel"
    private const val CHANNEL_FOCUS_NAME = "PrepOS Live Focus Session"

    private const val CHANNEL_ALERTS_ID = "prepos_alerts_channel"
    private const val CHANNEL_ALERTS_NAME = "PrepOS Study & Target Alerts"

    private const val FOCUS_NOTIFICATION_ID = 1001

    const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
    const val EXTRA_ACTION_TYPE = "extra_action_type"
    const val EXTRA_ACTION_PAYLOAD = "extra_action_payload"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Live Focus Channel (Silent, sticky)
            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_ID,
                CHANNEL_FOCUS_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live study timer countdown and task details during active focus mode."
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(focusChannel)

            // 2. High-Priority Study Alerts Channel (With sound, vibration, badges)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                CHANNEL_ALERTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends daily targets, streak milestones, revision reminders, and test summaries."
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    /**
     * Dispatches a real Android Push / System Notification with rich text and clickable action buttons.
     */
    fun showSystemAlertNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "REMINDER",
        actionType: String? = null,
        actionPayload: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Target navigation route calculation based on action type
        val targetDestination = when {
            actionType == "START_FOCUS" || actionType == "VIEW_PLAN" || type == "STREAK_WARNING" -> "plan"
            actionType == "TAKE_TEST" || type == "PRACTICE" || type == "TEST_RESULT" -> "practice"
            actionType == "OPEN_CHAPTER" -> "editor"
            type == "STREAK" || type == "TARGET" -> "home"
            else -> "home"
        }

        // Primary Click Intent (Tapping notification card)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, targetDestination)
            if (actionType != null) putExtra(EXTRA_ACTION_TYPE, actionType)
            if (actionPayload != null) putExtra(EXTRA_ACTION_PAYLOAD, actionPayload)
        }

        val notifReqCode = (System.currentTimeMillis() % 100000).toInt()

        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notifReqCode,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notifBuilder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
                    .setSummaryText(getSummaryTag(type))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)

        // Add Quick Action Button 1
        when {
            actionType == "START_FOCUS" || type == "REMINDER" || type == "STREAK_WARNING" -> {
                val focusIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_NAVIGATE_TO, "plan")
                    putExtra(EXTRA_ACTION_TYPE, "START_FOCUS")
                    if (actionPayload != null) putExtra(EXTRA_ACTION_PAYLOAD, actionPayload)
                }
                val focusPendingIntent = PendingIntent.getActivity(
                    context,
                    notifReqCode + 1,
                    focusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                notifBuilder.addAction(
                    android.R.drawable.ic_media_play,
                    "🎯 Start Focus",
                    focusPendingIntent
                )
            }
            actionType == "TAKE_TEST" || type == "PRACTICE" || type == "TEST_RESULT" -> {
                val testIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_NAVIGATE_TO, "practice")
                }
                val testPendingIntent = PendingIntent.getActivity(
                    context,
                    notifReqCode + 2,
                    testIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                notifBuilder.addAction(
                    android.R.drawable.ic_menu_agenda,
                    "📝 Practice Now",
                    testPendingIntent
                )
            }
            actionType == "OPEN_CHAPTER" && !actionPayload.isNullOrBlank() -> {
                val editIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_NAVIGATE_TO, "editor")
                    putExtra(EXTRA_ACTION_PAYLOAD, actionPayload)
                }
                val editPendingIntent = PendingIntent.getActivity(
                    context,
                    notifReqCode + 3,
                    editIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                notifBuilder.addAction(
                    android.R.drawable.ic_menu_edit,
                    "📖 Open Notes",
                    editPendingIntent
                )
            }
        }

        try {
            val alertNotifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            notificationManager.notify(alertNotifId, notifBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSummaryTag(type: String): String {
        return when (type) {
            "STREAK", "STREAK_MILESTONE" -> "PrepOS Streak Alert 🔥"
            "TARGET", "ACHIEVEMENT" -> "PrepOS Daily Target 🎯"
            "STREAK_WARNING" -> "PrepOS Streak Saver ⚠️"
            "REVISION" -> "PrepOS Revision Reminder 🧠"
            "PRACTICE", "TEST_RESULT" -> "PrepOS Practice Hub 📝"
            else -> "PrepOS Smart Study"
        }
    }

    fun updateFocusNotification(context: Context, session: ActiveFocusSession) {
        if (!session.isActive) {
            cancelNotification(context)
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val remSec = session.remainingSeconds
        val minutes = remSec / 60
        val seconds = remSec % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val totalSec = if (session.totalSeconds > 0) session.totalSeconds else 1
        val progress = ((totalSec - remSec).coerceAtLeast(0) * 100) / totalSec

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, "plan")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (session.isPaused) "⏸ Focus Paused ($timeFormatted left)" else "🎯 Focus Mode Active: $timeFormatted remaining"
        val subtitle = "${session.taskTitle} • ${session.subjectName}"
        val expandedDetails = buildString {
            append("Target: ${session.taskTitle}\n")
            append("Subject: ${session.subjectName}\n")
            append("Status: ${if (session.isPaused) "Paused" else "Running"}\n")
            append("Time Remaining: $timeFormatted (${session.remainingSeconds / 60}m ${session.remainingSeconds % 60}s)")
        }

        val targetEndTime = System.currentTimeMillis() + (remSec * 1000L)

        val notifBuilder = NotificationCompat.Builder(context, CHANNEL_FOCUS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedDetails)
                    .setBigContentTitle(title)
                    .setSummaryText("PrepOS Live Focus")
            )
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Native 60fps system countdown on lock screen and status bar
        if (!session.isPaused && remSec > 0) {
            notifBuilder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetEndTime)
        } else {
            notifBuilder.setUsesChronometer(false)
        }

        // Add Interactive Action Buttons on the Notification
        if (session.isPaused) {
            // Resume Action
            val resumeIntent = Intent(context, com.example.receiver.PrepOSNotificationReceiver::class.java).apply {
                action = com.example.receiver.PrepOSNotificationReceiver.ACTION_FOCUS_RESUME
            }
            val resumePending = PendingIntent.getBroadcast(
                context,
                1101,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            notifBuilder.addAction(android.R.drawable.ic_media_play, "▶ Resume", resumePending)
        } else {
            // Pause Action
            val pauseIntent = Intent(context, com.example.receiver.PrepOSNotificationReceiver::class.java).apply {
                action = com.example.receiver.PrepOSNotificationReceiver.ACTION_FOCUS_PAUSE
            }
            val pausePending = PendingIntent.getBroadcast(
                context,
                1102,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            notifBuilder.addAction(android.R.drawable.ic_media_pause, "⏸ Pause", pausePending)
        }

        // Stop Action
        val stopIntent = Intent(context, com.example.receiver.PrepOSNotificationReceiver::class.java).apply {
            action = com.example.receiver.PrepOSNotificationReceiver.ACTION_FOCUS_STOP
        }
        val stopPending = PendingIntent.getBroadcast(
            context,
            1103,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        notifBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "⏹ Finish", stopPending)

        try {
            notificationManager.notify(FOCUS_NOTIFICATION_ID, notifBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(FOCUS_NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
