package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.PrepOSDatabase
import com.example.util.PrepOSAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Automatically re-schedules all study alarms, daily briefings, and streak checks
 * when the Android device restarts or when the app is updated.
 */
class PrepOSBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("PrepOSBootReceiver", "Boot or package update detected. Rescheduling all alarms...")
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val db = PrepOSDatabase.getDatabase(context, scope)
                    val tasks = db.studyTaskDao().getAllTasksSync()
                    PrepOSAlarmScheduler.scheduleAllReminders(context, tasks)
                    Log.d("PrepOSBootReceiver", "Successfully rescheduled ${tasks.size} task alarms after boot.")
                } catch (e: Exception) {
                    Log.e("PrepOSBootReceiver", "Failed to reschedule alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
