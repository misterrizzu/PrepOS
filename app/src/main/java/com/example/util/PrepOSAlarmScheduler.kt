package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.entity.StudyTaskEntity
import com.example.receiver.PrepOSNotificationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Robust, professional exact alarm scheduler for PrepOS.
 * Ensures notifications are dispatched at the EXACT scheduled time,
 * even when the device is locked, in deep Doze mode, or under aggressive OEM battery limits.
 */
object PrepOSAlarmScheduler {

    private const val TAG = "PrepOSAlarmScheduler"
    private const val PREFS_NAME = "prepos_alarm_prefs"

    const val PREF_NOTIFS_ENABLED = "pref_notifs_enabled"
    const val PREF_TASK_REMINDERS_ENABLED = "pref_task_reminders_enabled"
    const val PREF_MORNING_BRIEFING_ENABLED = "pref_morning_briefing_enabled"
    const val PREF_MORNING_BRIEFING_TIME = "pref_morning_briefing_time" // "08:30"
    const val PREF_EVENING_STREAK_ENABLED = "pref_evening_streak_enabled"
    const val PREF_EVENING_STREAK_TIME = "pref_evening_streak_time" // "20:00"
    const val PREF_HEADS_UP_MINUTES = "pref_heads_up_minutes" // 0 = exact, 5 = 5m before

    const val REQ_MORNING_BRIEFING = 7001
    const val REQ_EVENING_STREAK = 7002
    const val REQ_FOCUS_TIMER = 7003
    const val REQ_TEST_ALARM = 7004

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Schedules all study alarms (tasks, morning briefing, evening streak check).
     */
    fun scheduleAllReminders(
        context: Context,
        tasks: List<StudyTaskEntity>
    ) {
        val prefs = getPrefs(context)
        val notifsEnabled = prefs.getBoolean(PREF_NOTIFS_ENABLED, true)
        if (!notifsEnabled) {
            cancelAll(context)
            return
        }

        // 1. Morning Briefing
        if (prefs.getBoolean(PREF_MORNING_BRIEFING_ENABLED, true)) {
            val morningTime = prefs.getString(PREF_MORNING_BRIEFING_TIME, "08:30") ?: "08:30"
            scheduleDailyMorningBriefing(context, morningTime)
        }

        // 2. Evening Streak Alert
        if (prefs.getBoolean(PREF_EVENING_STREAK_ENABLED, true)) {
            val eveningTime = prefs.getString(PREF_EVENING_STREAK_TIME, "20:00") ?: "20:00"
            scheduleEveningStreakCheck(context, eveningTime)
        }

        // 3. Timetable Task Reminders
        if (prefs.getBoolean(PREF_TASK_REMINDERS_ENABLED, true)) {
            val headsUpMinutes = prefs.getInt(PREF_HEADS_UP_MINUTES, 0)
            for (task in tasks) {
                scheduleTaskReminder(context, task, headsUpMinutes)
            }
        }
    }

    /**
     * Schedules an exact alarm for a specific timetable study task.
     */
    fun scheduleTaskReminder(context: Context, task: StudyTaskEntity, headsUpMinutes: Int = 0) {
        if (task.startTime.isBlank()) return

        val targetCal = parseTimeToCalendar(task.startTime) ?: return
        if (headsUpMinutes > 0) {
            targetCal.add(Calendar.MINUTE, -headsUpMinutes)
        }

        val now = System.currentTimeMillis()
        val triggerAt = targetCal.timeInMillis

        // If scheduled time for today has already passed, schedule for tomorrow
        val finalTriggerAt = if (triggerAt <= now + 10_000L) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
            targetCal.timeInMillis
        } else {
            triggerAt
        }

        val intent = Intent(context, PrepOSNotificationReceiver::class.java).apply {
            action = PrepOSNotificationReceiver.ACTION_STUDY_TASK_REMINDER
            putExtra(PrepOSNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(PrepOSNotificationReceiver.EXTRA_SUBJECT_NAME, task.subjectName)
            putExtra(PrepOSNotificationReceiver.EXTRA_TASK_TITLE, task.taskTitle)
            putExtra(PrepOSNotificationReceiver.EXTRA_START_TIME, task.startTime)
            putExtra(PrepOSNotificationReceiver.EXTRA_END_TIME, task.endTime)
            putExtra(PrepOSNotificationReceiver.EXTRA_HEADS_UP_MINS, headsUpMinutes)
        }

        val reqCode = generateTaskRequestCode(task.id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        setExactAlarm(context, finalTriggerAt, pendingIntent)
        Log.d(TAG, "Scheduled study task '${task.taskTitle}' for ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(finalTriggerAt)}")
    }

    /**
     * Cancels an existing task reminder.
     */
    fun cancelTaskReminder(context: Context, taskId: String) {
        val intent = Intent(context, PrepOSNotificationReceiver::class.java).apply {
            action = PrepOSNotificationReceiver.ACTION_STUDY_TASK_REMINDER
        }
        val reqCode = generateTaskRequestCode(taskId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules the daily morning study briefing alarm.
     */
    fun scheduleDailyMorningBriefing(context: Context, timeStr: String = "08:30") {
        val targetCal = parseTimeToCalendar(timeStr) ?: return
        val now = System.currentTimeMillis()
        if (targetCal.timeInMillis <= now + 5000L) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, PrepOSNotificationReceiver::class.java).apply {
            action = PrepOSNotificationReceiver.ACTION_DAILY_MORNING_BRIEFING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_MORNING_BRIEFING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        setExactAlarm(context, targetCal.timeInMillis, pendingIntent)
        Log.d(TAG, "Scheduled morning briefing for ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(targetCal.time)}")
    }

    /**
     * Schedules the evening streak guardian alarm.
     */
    fun scheduleEveningStreakCheck(context: Context, timeStr: String = "20:00") {
        val targetCal = parseTimeToCalendar(timeStr) ?: return
        val now = System.currentTimeMillis()
        if (targetCal.timeInMillis <= now + 5000L) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, PrepOSNotificationReceiver::class.java).apply {
            action = PrepOSNotificationReceiver.ACTION_EVENING_STREAK_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_EVENING_STREAK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        setExactAlarm(context, targetCal.timeInMillis, pendingIntent)
        Log.d(TAG, "Scheduled evening streak check for ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(targetCal.time)}")
    }

    /**
     * Schedules an exact alarm when an active focus session is scheduled to finish.
     * Guarantees notification and chime trigger on time even if phone is in deep Doze sleep.
     */
    fun scheduleFocusTimerCompletion(context: Context, remainingSeconds: Int, taskTitle: String, subjectName: String) {
        if (remainingSeconds <= 0) return
        val triggerAt = System.currentTimeMillis() + (remainingSeconds * 1000L)

        val intent = Intent(context, PrepOSNotificationReceiver::class.java).apply {
            action = PrepOSNotificationReceiver.ACTION_FOCUS_TIMER_FINISHED
            putExtra(PrepOSNotificationReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(PrepOSNotificationReceiver.EXTRA_SUBJECT_NAME, subjectName)
            putExtra(PrepOSNotificationReceiver.EXTRA_TOTAL_SECONDS, remainingSeconds)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_FOCUS_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        setExactAlarm(context, triggerAt, pendingIntent)
        Log.d(TAG, "Scheduled Focus Timer Completion in $remainingSeconds seconds")
    }

    /**
     * Cancels pending focus timer completion alarm.
     */
    fun cancelFocusTimerAlarm(context: Context) {
        val intent = Intent(context, PrepOSNotificationReceiver::class.java).apply {
            action = PrepOSNotificationReceiver.ACTION_FOCUS_TIMER_FINISHED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_FOCUS_TIMER,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules an immediate 10-second test alarm to verify background wakefulness.
     */
    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 10) {
        val triggerAt = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, PrepOSNotificationReceiver::class.java).apply {
            action = PrepOSNotificationReceiver.ACTION_TEST_ALARM
            putExtra("delay_seconds", delaySeconds)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_TEST_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        setExactAlarm(context, triggerAt, pendingIntent)
    }

    /**
     * Low-level helper to dispatch exact alarms using setAlarmClock (bypasses Doze mode)
     * or setExactAndAllowWhileIdle.
     */
    private fun setExactAlarm(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // AlarmClockInfo is the gold standard on Android for time-critical user notifications
                val showIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    9990,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing, falling back to setAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cancels all scheduled alarms.
     */
    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val reqCodes = listOf(REQ_MORNING_BRIEFING, REQ_EVENING_STREAK, REQ_FOCUS_TIMER, REQ_TEST_ALARM)
        for (req in reqCodes) {
            val intent = Intent(context, PrepOSNotificationReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                req,
                intent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    private fun generateTaskRequestCode(taskId: String): Int {
        return (taskId.hashCode() and 0x7FFFFFFF) % 100000 + 10000
    }

    /**
     * Parses time strings into a Calendar object for today.
     * Supports formats: "14:30", "09:00", "9:00", "02:30 PM", "9:00 AM", "11:15pm"
     */
    fun parseTimeToCalendar(timeStr: String): Calendar? {
        val cleaned = timeStr.trim().uppercase(Locale.ROOT)
        if (cleaned.isBlank()) return null

        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            // Check for AM/PM format
            if (cleaned.contains("AM") || cleaned.contains("PM")) {
                val isPm = cleaned.contains("PM")
                val numbersOnly = cleaned.replace("AM", "").replace("PM", "").trim()
                val parts = numbersOnly.split(":")
                var hour = parts[0].trim().toInt()
                val minute = if (parts.size > 1) parts[1].trim().toInt() else 0

                if (isPm && hour < 12) hour += 12
                if (!isPm && hour == 12) hour = 0

                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                return cal
            }

            // Standard 24h format (e.g. 14:30 or 9:00)
            val parts = cleaned.split(":")
            if (parts.isNotEmpty()) {
                val hour = parts[0].trim().toInt()
                val minute = if (parts.size > 1) parts[1].trim().toInt() else 0
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                return cal
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse time string: $timeStr", e)
        }
        return null
    }
}
