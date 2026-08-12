package com.synfusion.attendance.utils

import android.content.Context
import androidx.work.*
import com.synfusion.attendance.data.LocalAttendanceRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val repository = LocalAttendanceRepository(applicationContext)

            // Abort if no active user profile exists in database (User Isolation)
            val activeUser = repository.getUser() ?: return Result.success()

            val prefsRepo = com.synfusion.attendance.data.OfflinePreferencesRepository(applicationContext)

            // Check system permissions first
            // Let's check master toggles before running evaluation
            val masterEnabled = prefsRepo.remindersFlow.first()
            if (!masterEnabled) {
                return Result.success() // Reminders OFF
            }

            val notifManager = androidx.core.app.NotificationManagerCompat.from(applicationContext)
            if (!notifManager.areNotificationsEnabled()) {
                LocalNotificationManager.sendNotification(
                    context = applicationContext,
                    channelId = "",
                    title = "",
                    message = "",
                    category = "worker",
                    reason = "SKIPPED_NOTIFICATION_PERMISSION_DENIED"
                )
                return Result.success()
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        applicationContext,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    LocalNotificationManager.sendNotification(
                        context = applicationContext,
                        channelId = "",
                        title = "",
                        message = "",
                        category = "worker",
                        reason = "SKIPPED_NOTIFICATION_PERMISSION_DENIED"
                    )
                    return Result.success()
                }
            }


            // Check if active hours check is met
            // Daily Attendance reminder eligibility is 6:00 PM (18) to 10:00 PM (22)
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isEligibleWindow = hour in 18..22

            // Run evaluation with style setting
            val evalResult = NotificationRuleEngine.evaluateAllRules(applicationContext, repository, "Friendly")

            if (evalResult.triggerNotification) {
                // Determine category filters enabled
                val isDailyAttendanceType = evalResult.category in listOf(
                    "daily_attendance", "missed_two_days", "missed_three_days",
                    "missed_seven_plus", "missed_ten_days", "long_absence"
                )

                if (isDailyAttendanceType && !isEligibleWindow) {
                    return Result.success() // Wait for eligible window
                }

                LocalNotificationManager.sendNotification(
                    context = applicationContext,
                    channelId = evalResult.channelId,
                    title = evalResult.title,
                    message = evalResult.message,
                    category = evalResult.category,
                    reason = evalResult.reason
                )
            }
        } catch (e: Exception) {
            // Never crash, safely return success (could try retry if important, but background reminders can skip)
        }
        return Result.success()
    }
}

object NotificationScheduler {

    private const val WORK_NAME = "WorkDailyNotificationWork"

    fun schedulePeriodicWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.HOURS // Run once every hour to check rules
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing to maintain scheduled timing
            periodicRequest
        )
    }

    fun cancelAllScheduledWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
