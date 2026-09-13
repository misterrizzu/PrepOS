package com.example.util

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast

enum class FocusAudioMode { SILENT, VIBRATE, NORMAL }

object PrepOSFocusController {

    /**
     * Check whether ACCESS_NOTIFICATION_POLICY is granted (DND Access)
     */
    fun isDndAccessGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    /**
     * Step A: Check & Request Permission (Shows PrepOS explicitly in DND settings list)
     */
    fun checkAndRequestDndPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please enable PrepOS in the list for Focus Mode!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "DND & Audio Access is already granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Step B: App Ke Andar Se Direct Mute / Silent / Normal Mode Control Karna
     */
    fun setPhoneAudioMode(context: Context, mode: FocusAudioMode) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || notificationManager?.isNotificationPolicyAccessGranted == true) {
            try {
                when (mode) {
                    FocusAudioMode.SILENT -> {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                    FocusAudioMode.VIBRATE -> {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                    FocusAudioMode.NORMAL -> {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Agar permission nahi hai toh gracefully ignore ya toast
        }
    }
}

object PrepOSAppPinningManager {

    /**
     * Start Lock Mode: Application ko screen par pin kar deta hai
     */
    fun pinApplication(context: Context) {
        val activity = context as? Activity ?: return
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!activityManager.isInLockTaskMode) {
                    activity.startLockTask()
                    Toast.makeText(context, "Study Workspace Pinned! Stay Focused.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Pinning note: Enable App Pinning in phone Security settings.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Stop Lock Mode: App ko unpin kar deta hai jab target poora ho jaye ya session end ho
     */
    fun unpinApplication(context: Context) {
        val activity = context as? Activity ?: return
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (activityManager?.isInLockTaskMode == true) {
                    activity.stopLockTask()
                    Toast.makeText(context, "Workspace Unpinned!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
