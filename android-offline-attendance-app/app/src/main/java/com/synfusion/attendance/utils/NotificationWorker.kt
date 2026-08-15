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

            if (!LocalNotificationManager.areNotificationsFullyEnabled(applicationContext)) {
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

                if (!isDailyAttendanceType || isEligibleWindow) {
                    val posted = LocalNotificationManager.sendNotification(
                        context = applicationContext,
                        channelId = evalResult.channelId,
                        title = evalResult.title,
                        message = evalResult.message,
                        category = evalResult.category,
                        reason = evalResult.reason
                    )
                    if (posted) {
                        NotificationRuleEngine.markRuleStatePersisted(applicationContext, evalResult)
                    }
                } else {
                    LocalNotificationManager.logNotificationHistory(
                        applicationContext,
                        evalResult.category,
                        "",
                        "SKIPPED_OUTSIDE_ELIGIBLE_WINDOW (Hour: $hour, window: 18..22)"
                    )
                }
            }

            // Evaluate Friendly Notifications
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val timeInMinutes = currentHour * 60 + currentMinute

            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val userId = activeUser.id

            val friendlyCategory = when {
                timeInMinutes in 390..570 -> "morning"    // 6:30 AM - 9:30 AM (inclusive)
                timeInMinutes in 1050..1200 -> "evening"  // 5:30 PM - 8:00 PM (inclusive)
                timeInMinutes in 1260..1350 -> "night"    // 9:00 PM - 10:30 PM (inclusive)
                else -> null
            }

            if (friendlyCategory != null) {
                val prefSpam = applicationContext.getSharedPreferences("notification_spam_prevention", Context.MODE_PRIVATE)
                val evalKey = "friendly_eval_${friendlyCategory}_${todayStr}_$userId"
                val sentKey = "friendly_sent_${friendlyCategory}_${todayStr}_$userId"

                if (!prefSpam.contains(evalKey)) {
                    // Not evaluated today yet! Roll a 35% probability.
                    val roll = kotlin.random.Random.nextFloat()
                    val shouldSend = roll < 0.35f

                    prefSpam.edit()
                        .putBoolean(evalKey, true)
                        .putBoolean(sentKey, shouldSend)
                        .apply()

                    if (shouldSend) {
                        val placeholders = NotificationRuleEngine.buildPlaceholders(
                            activeUser,
                            repository.getPersonalAttendance(),
                            todayStr
                        )
                        val finalPlaceholders = placeholders.toMutableMap()
                        if (finalPlaceholders["userName"].isNullOrBlank()) {
                            finalPlaceholders["userName"] = "bhai"
                        }

                        val pool = when (friendlyCategory) {
                            "morning" -> NotificationMessagePool.goodMorningMessages
                            "evening" -> NotificationMessagePool.goodEveningMessages
                            "night" -> NotificationMessagePool.goodNightMessages
                            else -> emptyList()
                        }

                        val message = NotificationMessageSelector.selectMessage(
                            applicationContext,
                            "friendly_$friendlyCategory",
                            pool,
                            finalPlaceholders
                        )

                        LocalNotificationManager.sendNotification(
                            context = applicationContext,
                            channelId = LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE,
                            title = "WorkDaily 😊",
                            message = message,
                            category = "friendly_$friendlyCategory",
                            reason = "Seeded/randomized friendly $friendlyCategory reminder"
                        )
                    }
                }
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

        val workManager = WorkManager.getInstance(context)
        val workInfos = try {
            workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        } catch (e: Exception) {
            emptyList()
        }

        // Re-enqueue using UPDATE if work is active, or KEEP if none exists/completed
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

    fun cancelAllScheduledWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        LocalNotificationManager.logNotificationHistory(
            context,
            "scheduler",
            "",
            "cancelAllScheduledWork invoked - Cancelled $WORK_NAME"
        )
    }
}
