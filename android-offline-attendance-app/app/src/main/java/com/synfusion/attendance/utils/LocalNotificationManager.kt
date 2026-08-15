package com.synfusion.attendance.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationLogItem(
    val time: String,
    val timestamp: Long,
    val category: String,
    val message: String,
    val reason: String
)

object LocalNotificationManager {

    private const val TAG = "WorkDailyNotification"

    // Stable Channel Definitions
    const val CHANNEL_ATTENDANCE = "attendance_channel"
    const val CHANNEL_REMINDERS = "reminders_channel"
    const val CHANNEL_SUMMARY = "summary_channel"
    const val CHANNEL_EARNINGS = "earnings_channel"
    const val CHANNEL_BACKUP = "backup_channel"

    // Legacy fallback channel alias
    const val CHANNEL_OFFLINE_ATTENDANCE = CHANNEL_ATTENDANCE
    const val CHANNEL_REPORTS = CHANNEL_SUMMARY
    const val CHANNEL_FUN_MOTIVATION = CHANNEL_REMINDERS

    private const val PREFS_HISTORY = "notification_send_history_logs"
    private const val KEY_LOGS = "logs_list"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Attendance & Shifts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to mark daily attendance, check-in, check-out and missing logs"
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Daily Reminders & Productivity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General work reminders, streaks, productivity tips and motivational alerts"
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY,
                "Reports & Summaries",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily, weekly, monthly and yearly work summaries and passbook reports"
            }

            val earningsChannel = NotificationChannel(
                CHANNEL_EARNINGS,
                "Wages & Overtime",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily wage updates, overtime calculations, advance deductions and net payable alerts"
            }

            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "Backup & Storage",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reminders for data backups, PDF exports and offline data protection"
            }

            manager.createNotificationChannels(
                listOf(
                    attendanceChannel,
                    remindersChannel,
                    summaryChannel,
                    earningsChannel,
                    backupChannel
                )
            )

            try {
                manager.deleteNotificationChannel("offline_attendance_reminders")
                manager.deleteNotificationChannel("reports")
                manager.deleteNotificationChannel("funny_motivation")
            } catch (e: Exception) {
                // Ignore cleanup errors
            }

            Log.d(TAG, "Initialized standard notification channels successfully.")
        }
    }

    fun mapCategoryToChannel(category: String): String {
        return when (category) {
            "attendance_reminder", "check_in", "check_out", "late_attendance", "missing_attendance" -> CHANNEL_ATTENDANCE
            "overtime", "overtime_threshold", "earnings" -> CHANNEL_EARNINGS
            "daily_summary", "weekly_summary", "monthly_summary", "yearly_report", "weekly_report", "monthly_report", "passbook", "export" -> CHANNEL_SUMMARY
            "backup" -> CHANNEL_BACKUP
            else -> CHANNEL_REMINDERS
        }
    }

    fun areNotificationsFullyEnabled(context: Context): Boolean {
        val notifManager = NotificationManagerCompat.from(context)
        if (!notifManager.areNotificationsEnabled()) {
            Log.w(TAG, "areNotificationsFullyEnabled: App-level notifications disabled.")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "areNotificationsFullyEnabled: POST_NOTIFICATIONS permission not granted.")
                return false
            }
        }

        return true
    }

    fun generateNotificationId(category: String, dateStr: String = ""): Int {
        val todayStr = if (dateStr.isBlank()) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        } else dateStr
        val raw = "${category}_$todayStr"
        return raw.hashCode() and 0x7FFFFFFF // Ensure positive Integer
    }

    fun sendNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        category: String,
        reason: String,
        action: String = "home"
    ): Boolean {
        val targetChannelId = if (channelId.isBlank()) mapCategoryToChannel(category) else channelId

        Log.d(TAG, "Attempting sendNotification - Category: $category, Channel: $targetChannelId, Title: $title")

        if (title.isBlank() && message.isBlank()) {
            logNotificationHistory(context, category, message, reason)
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", action)
            putExtra("notification_category", category)
        }

        val notificationId = generateNotificationId(category)

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                if (targetChannelId == CHANNEL_SUMMARY || targetChannelId == CHANNEL_BACKUP)
                    NotificationCompat.PRIORITY_LOW
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (!notificationManagerCompat.areNotificationsEnabled()) {
                logNotificationHistory(context, category, message, "FAILED: System Notifications Disabled")
                Log.w(TAG, "sendNotification failed: System notifications disabled for app.")
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    logNotificationHistory(context, category, message, "FAILED: POST_NOTIFICATIONS missing")
                    Log.w(TAG, "sendNotification failed: POST_NOTIFICATIONS permission missing.")
                    return false
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                var channel = manager.getNotificationChannel(targetChannelId)
                if (channel == null) {
                    initChannels(context)
                    channel = manager.getNotificationChannel(targetChannelId)
                }

                if (channel == null) {
                    logNotificationHistory(context, category, message, "FAILED: Channel $targetChannelId not found")
                    Log.e(TAG, "sendNotification failed: Channel $targetChannelId not found after init.")
                    return false
                }

                if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                    logNotificationHistory(context, category, message, "FAILED: Channel $targetChannelId disabled")
                    Log.w(TAG, "sendNotification failed: Channel $targetChannelId is disabled by user.")
                    return false
                }
            }

            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.notify(notificationId, builder.build())

            logNotificationHistory(context, category, message, "DELIVERED: $reason")
            Log.i(TAG, "Notification delivered successfully! ID: $notificationId, Title: $title")
            return true
        } catch (e: Exception) {
            logNotificationHistory(context, category, message, "FAILED_EXCEPTION: ${e.localizedMessage}")
            Log.e(TAG, "Error posting notification: ${e.localizedMessage}", e)
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

            val newArray = JSONArray()
            newArray.put(logObj)
            for (i in 0 until minOf(array.length(), 49)) {
                newArray.put(array.get(i))
            }

            prefs.edit().putString(KEY_LOGS, newArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed logging notification history: ${e.localizedMessage}")
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
