package com.synfusion.attendance.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val TAG = "WorkDailyNotification"
    private const val WORK_NAME = "WorkDailyNotificationWork"

    const val ACTION_DAILY_REMINDER = "com.synfusion.attendance.ACTION_DAILY_REMINDER"
    const val ACTION_SUMMARY_REMINDER = "com.synfusion.attendance.ACTION_SUMMARY_REMINDER"
    const val ACTION_BACKUP_REMINDER = "com.synfusion.attendance.ACTION_BACKUP_REMINDER"

    private const val REQUEST_CODE_DAILY = 1001
    private const val REQUEST_CODE_SUMMARY = 1002
    private const val REQUEST_CODE_BACKUP = 1003

    fun scheduleAllSchedules(context: Context) {
        scheduleAlarms(context)
        schedulePeriodicWork(context)
    }

    fun scheduleAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            scheduleAlarmAtHour(
                context = context,
                alarmManager = alarmManager,
                action = ACTION_DAILY_REMINDER,
                requestCode = REQUEST_CODE_DAILY,
                targetHour = 19,
                targetMinute = 0
            )

            scheduleAlarmAtHour(
                context = context,
                alarmManager = alarmManager,
                action = ACTION_SUMMARY_REMINDER,
                requestCode = REQUEST_CODE_SUMMARY,
                targetHour = 21,
                targetMinute = 0
            )

            scheduleWeeklyBackupAlarm(context, alarmManager)

            Log.i(TAG, "Inexact alarms scheduled successfully via AlarmManager.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling alarms: ${e.localizedMessage}", e)
        }
    }

    // Retained for backward compatibility
    fun scheduleExactAlarms(context: Context) {
        scheduleAlarms(context)
    }

    private fun scheduleAlarmAtHour(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int,
        targetHour: Int,
        targetMinute: Int
    ) {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            this.action = action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerAtMs = calendar.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }

        LocalNotificationManager.logNotificationHistory(
            context,
            "scheduler",
            "",
            "Scheduled $action for ${calendar.time} (inexact)"
        )
    }

    private fun scheduleWeeklyBackupAlarm(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_BACKUP_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BACKUP,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun schedulePeriodicWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.HOURS
        )
        .setConstraints(constraints)
        .build()

        val workManager = WorkManager.getInstance(context)
        val workInfos = try {
            workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        } catch (e: Exception) {
            emptyList()
        }

        val hasActiveWork = workInfos.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }

        val policy = if (hasActiveWork) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            policy,
            periodicRequest
        )

        LocalNotificationManager.logNotificationHistory(
            context,
            "scheduler",
            "",
            "schedulePeriodicWork invoked (policy=$policy, hasActiveWork=$hasActiveWork)"
        )
    }

    fun cancelAllSchedules(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val actions = listOf(ACTION_DAILY_REMINDER, ACTION_SUMMARY_REMINDER, ACTION_BACKUP_REMINDER)
            val requestCodes = listOf(REQUEST_CODE_DAILY, REQUEST_CODE_SUMMARY, REQUEST_CODE_BACKUP)

            for (i in actions.indices) {
                val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                    action = actions[i]
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCodes[i],
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }

            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

            LocalNotificationManager.logNotificationHistory(
                context,
                "scheduler",
                "",
                "cancelAllSchedules: Cancelled all AlarmManager alarms and WorkManager periodic tasks."
            )
            Log.i(TAG, "Cancelled all notification schedules.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling schedules: ${e.localizedMessage}", e)
        }
    }

    // Alias for backward compatibility
    fun cancelAllScheduledWork(context: Context) {
        cancelAllSchedules(context)
    }
}
