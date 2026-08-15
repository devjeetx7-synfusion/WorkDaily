package com.synfusion.attendance.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalNotificationManager {

    const val CHANNEL_OFFLINE_ATTENDANCE = "offline_attendance_reminders"

    // For backwards compatibility and automatic mapping of existing references
    const val CHANNEL_ATTENDANCE = CHANNEL_OFFLINE_ATTENDANCE
    const val CHANNEL_REPORTS = CHANNEL_OFFLINE_ATTENDANCE
    const val CHANNEL_EARNINGS = CHANNEL_OFFLINE_ATTENDANCE
    const val CHANNEL_FUN_MOTIVATION = CHANNEL_OFFLINE_ATTENDANCE

    private const val PREFS_HISTORY = "notification_send_history_logs"
    private const val KEY_LOGS = "logs_list"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val attendanceChannel = NotificationChannel(
                CHANNEL_OFFLINE_ATTENDANCE,
                "Offline Attendance Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily attendance pending reminders, misses, reports and earnings updates"
            }

            manager.createNotificationChannel(attendanceChannel)

            // Safely delete old legacy/unused channels to clean up system settings
            try {
                manager.deleteNotificationChannel("attendance_reminders")
                manager.deleteNotificationChannel("reports")
                manager.deleteNotificationChannel("earnings")
                manager.deleteNotificationChannel("funny_motivation")
            } catch (e: Exception) {
                // Safe fallback if deletion is blocked or fails
            }
        }
    }

    fun areNotificationsFullyEnabled(context: Context): Boolean {
        val notifManager = NotificationManagerCompat.from(context)
        if (!notifManager.areNotificationsEnabled()) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(CHANNEL_OFFLINE_ATTENDANCE)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }

        return true
    }

    fun sendNotification(
        context: Context,
        channelId: String, // Kept for signature compatibility but mapped to consolidated channel
        title: String,
        message: String,
        category: String,
        reason: String
    ): Boolean {
        // Build Intent to open MainActivity on Tap
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notificationId = System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val targetChannelId = CHANNEL_OFFLINE_ATTENDANCE

        val builder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(com.synfusion.attendance.R.mipmap.ic_launcher) // Use app launcher icon safely
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            // Check if title and message are both empty - if so, only log history and return
            if (title.isBlank() && message.isBlank()) {
                logNotificationHistory(context, category, message, reason)
                return false
            }

            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (!notificationManagerCompat.areNotificationsEnabled()) {
                logNotificationHistory(context, category, message, "Permission Denied: System Notifications Disabled")
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    // Log fail internally, do not fire
                    logNotificationHistory(context, category, message, "Permission Denied: POST_NOTIFICATIONS missing")
                    return false
                }
            }

            // Check channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                var channel = manager.getNotificationChannel(targetChannelId)
                if (channel == null) {
                    initChannels(context) // Re-init if missing
                    channel = manager.getNotificationChannel(targetChannelId)
                }

                if (channel == null) {
                    logNotificationHistory(context, category, message, "Failed: Channel $targetChannelId does not exist")
                    return false
                }

                if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                    logNotificationHistory(context, category, message, "Permission Denied: Channel disabled")
                    return false
                }
            }

            // Use the standard NotificationManager system API directly to post
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.notify(notificationId, builder.build())

            // Add to execution logs
            logNotificationHistory(context, category, message, reason)
            return true
        } catch (e: Exception) {
            logNotificationHistory(context, category, message, "Failed: ${e.localizedMessage}")
            return false
        }
    }

    fun logNotificationHistory(
        context: Context,
        category: String,
        message: String,
        reason: String
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            val existingStr = prefs.getString(KEY_LOGS, "[]") ?: "[]"
            val array = JSONArray(existingStr)

            val logObj = JSONObject().apply {
                put("time", SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()))
                put("timestamp", System.currentTimeMillis())
                put("category", category)
                put("message", message)
                put("reason", reason)
            }

            // Put newer logs first
            val newArray = JSONArray()
            newArray.put(logObj)
            for (i in 0 until minOf(array.length(), 49)) { // cap at 50 logs to save storage
                newArray.put(array.get(i))
            }

            prefs.edit().putString(KEY_LOGS, newArray.toString()).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun getNotificationLogs(context: Context): List<NotificationLogItem> {
        val list = mutableListOf<NotificationLogItem>()
        try {
            val prefs = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            val existingStr = prefs.getString(KEY_LOGS, "[]") ?: "[]"
            val array = JSONArray(existingStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NotificationLogItem(
                        time = obj.optString("time", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        category = obj.optString("category", ""),
                        message = obj.optString("message", ""),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            // safe fallback
        }
        return list
    }

    fun clearNotificationLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGS).apply()
    }
}

data class NotificationLogItem(
    val time: String,
    val timestamp: Long,
    val category: String,
    val message: String,
    val reason: String
)
