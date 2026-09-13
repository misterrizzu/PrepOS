package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.db.PrepOSDatabase
import com.example.data.entity.SmartNotificationEntity
import com.example.util.PrepOSAlarmScheduler
import com.example.util.PrepOSFocusNotificationManager
import com.example.viewmodel.PrepOSViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class PrepOSNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrepOSNotifReceiver"

        const val ACTION_STUDY_TASK_REMINDER = "com.example.prepos.ACTION_STUDY_TASK_REMINDER"
        const val ACTION_DAILY_MORNING_BRIEFING = "com.example.prepos.ACTION_DAILY_MORNING_BRIEFING"
        const val ACTION_EVENING_STREAK_CHECK = "com.example.prepos.ACTION_EVENING_STREAK_CHECK"
        const val ACTION_FOCUS_TIMER_FINISHED = "com.example.prepos.ACTION_FOCUS_TIMER_FINISHED"
        const val ACTION_TEST_ALARM = "com.example.prepos.ACTION_TEST_ALARM"

        // Interactive Focus Notification Action Buttons
        const val ACTION_FOCUS_PAUSE = "com.example.prepos.ACTION_FOCUS_PAUSE"
        const val ACTION_FOCUS_RESUME = "com.example.prepos.ACTION_FOCUS_RESUME"
        const val ACTION_FOCUS_STOP = "com.example.prepos.ACTION_FOCUS_STOP"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_HEADS_UP_MINS = "extra_heads_up_mins"
        const val EXTRA_TOTAL_SECONDS = "extra_total_seconds"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received alarm action: $action")

        // Acquire a temporary wake lock (3s max) to guarantee the CPU doesn't sleep while posting
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "prepos:notif_wakelock")
        wakeLock?.acquire(3000L)

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                when (action) {
                    ACTION_STUDY_TASK_REMINDER -> {
                        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: ""
                        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Study Plan"
                        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Study Session"
                        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                        val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""
                        val headsUp = intent.getIntExtra(EXTRA_HEADS_UP_MINS, 0)

                        val title = if (headsUp > 0) {
                            "⏰ Starting in $headsUp min: $subjectName"
                        } else {
                            "⏰ Time to Study: $subjectName"
                        }

                        val message = "$taskTitle ($startTime - $endTime) is scheduled now. Tap to start your focus session!"

                        // 1. Post real system notification with interactive buttons
                        PrepOSFocusNotificationManager.showSystemAlertNotification(
                            context = context,
                            title = title,
                            message = message,
                            type = "REMINDER",
                            actionType = "START_FOCUS",
                            actionPayload = subjectName
                        )

                        // 2. Persist in database
                        try {
                            val db = PrepOSDatabase.getDatabase(context, scope)
                            val notif = SmartNotificationEntity(
                                id = "notif_${UUID.randomUUID().toString().take(8)}",
                                title = title,
                                message = message,
                                type = "REMINDER",
                                actionType = "START_FOCUS",
                                actionPayload = subjectName,
                                actionLabel = "Start Focus",
                                priority = "HIGH",
                                createdAt = System.currentTimeMillis()
                            )
                            db.smartNotificationDao().insertNotification(notif)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving task reminder to db", e)
                        }
                    }

                    ACTION_DAILY_MORNING_BRIEFING -> {
                        try {
                            val db = PrepOSDatabase.getDatabase(context, scope)
                            val prefs = db.userPreferencesDao().getPreferences()
                            val tasks = db.studyTaskDao().getAllTasksSync()
                            val targetMins = prefs?.dailyTargetMinutes ?: 45
                            val targetHours = String.format("%.1f", targetMins / 60f)

                            val title = "☀️ Good Morning! Today's Study Plan"
                            val message = if (tasks.isNotEmpty()) {
                                "You have ${tasks.size} tasks planned today. Daily Target: ${targetMins}m ($targetHours hrs). Let's make today count!"
                            } else {
                                "Daily Target: ${targetMins}m. Set up your timetable in Focus Hub to achieve maximum focus today!"
                            }

                            PrepOSFocusNotificationManager.showSystemAlertNotification(
                                context = context,
                                title = title,
                                message = message,
                                type = "TARGET",
                                actionType = "VIEW_PLAN"
                            )

                            // Save to database
                            val notif = SmartNotificationEntity(
                                id = "notif_${UUID.randomUUID().toString().take(8)}",
                                title = title,
                                message = message,
                                type = "TARGET",
                                actionType = "VIEW_PLAN",
                                actionLabel = "View Plan",
                                priority = "NORMAL",
                                createdAt = System.currentTimeMillis()
                            )
                            db.smartNotificationDao().insertNotification(notif)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error generating morning briefing", e)
                        }

                        // Reschedule for next day
                        val prefs = PrepOSAlarmScheduler.getPrefs(context)
                        val morningTime = prefs.getString(PrepOSAlarmScheduler.PREF_MORNING_BRIEFING_TIME, "08:30") ?: "08:30"
                        PrepOSAlarmScheduler.scheduleDailyMorningBriefing(context, morningTime)
                    }

                    ACTION_EVENING_STREAK_CHECK -> {
                        try {
                            val db = PrepOSDatabase.getDatabase(context, scope)
                            val prefs = db.userPreferencesDao().getPreferences()
                            val streak = prefs?.currentStreak ?: 0
                            val targetMins = prefs?.dailyTargetMinutes ?: 45
                            val todayMins = prefs?.todayFocusedMinutes ?: 0

                            if (streak > 0 && todayMins < targetMins) {
                                val remainingMins = (targetMins - todayMins).coerceAtLeast(1)
                                val title = "⚠️ Keep Your $streak-Day Streak Alive!"
                                val message = "You only have $remainingMins mins left to reach today's target. Study before midnight to keep your streak!"

                                PrepOSFocusNotificationManager.showSystemAlertNotification(
                                    context = context,
                                    title = title,
                                    message = message,
                                    type = "STREAK_WARNING",
                                    actionType = "START_FOCUS"
                                )

                                val notif = SmartNotificationEntity(
                                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                                    title = title,
                                    message = message,
                                    type = "STREAK_WARNING",
                                    actionType = "START_FOCUS",
                                    actionLabel = "Start Focus",
                                    priority = "HIGH",
                                    createdAt = System.currentTimeMillis()
                                )
                                db.smartNotificationDao().insertNotification(notif)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking evening streak", e)
                        }

                        // Reschedule for next day
                        val prefs = PrepOSAlarmScheduler.getPrefs(context)
                        val eveningTime = prefs.getString(PrepOSAlarmScheduler.PREF_EVENING_STREAK_TIME, "20:00") ?: "20:00"
                        PrepOSAlarmScheduler.scheduleEveningStreakCheck(context, eveningTime)
                    }

                    ACTION_FOCUS_TIMER_FINISHED -> {
                        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Focus Session"
                        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Study"

                        PrepOSFocusNotificationManager.showSystemAlertNotification(
                            context = context,
                            title = "🎉 Focus Session Completed!",
                            message = "Outstanding work! You completed your focus session for $taskTitle ($subjectName).",
                            type = "ACHIEVEMENT",
                            actionType = "VIEW_PLAN"
                        )

                        // If ViewModel is active, tell it to conclude session
                        PrepOSViewModel.currentInstance?.finishFocusSession()
                    }

                    ACTION_TEST_ALARM -> {
                        val delay = intent.getIntExtra("delay_seconds", 10)
                        PrepOSFocusNotificationManager.showSystemAlertNotification(
                            context = context,
                            title = "✓ Background Alarm Verified ($delay s)",
                            message = "PrepOS successfully triggered an exact background alarm while your device was idle!",
                            type = "TARGET",
                            actionType = "VIEW_PLAN"
                        )
                    }

                    ACTION_FOCUS_PAUSE -> {
                        PrepOSViewModel.currentInstance?.pauseFocusSession()
                    }

                    ACTION_FOCUS_RESUME -> {
                        PrepOSViewModel.currentInstance?.resumeFocusSession()
                    }

                    ACTION_FOCUS_STOP -> {
                        PrepOSViewModel.currentInstance?.stopFocusSession()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in broadcast receiver", e)
            } finally {
                wakeLock?.release()
                pendingResult.finish()
            }
        }
    }
}
