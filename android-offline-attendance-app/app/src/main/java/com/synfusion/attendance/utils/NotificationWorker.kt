package com.synfusion.attendance.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.synfusion.attendance.data.LocalAttendanceRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val repository = LocalAttendanceRepository(applicationContext)

            val activeUser = repository.getUser() ?: return Result.success()

            val prefsRepo = com.synfusion.attendance.data.OfflinePreferencesRepository(applicationContext)

            val masterEnabled = prefsRepo.remindersFlow.first()
            if (!masterEnabled) {
                return Result.success()
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

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isEligibleWindow = hour in 18..22

            val evalResult = NotificationRuleEngine.evaluateAllRules(applicationContext, repository, "Friendly")

            if (evalResult.triggerNotification) {
                val isDailyAttendanceType = evalResult.category in listOf(
                    "attendance_reminder", "missing_attendance"
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

            // Evaluate General Productivity & Motivational Templates
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val timeInMinutes = currentHour * 60 + currentMinute

            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val userId = activeUser.id

            val friendlyCategory = when {
                timeInMinutes in 390..570 -> listOf("good_morning", "going_to_work", "attendance_reminder", "general_productivity").random()  // Morning window
                timeInMinutes in 1050..1200 -> listOf("daily_work", "hard_work", "wage", "streak", "random_funny").random()              // Evening window
                timeInMinutes in 1260..1350 -> listOf("good_night", "weekend", "adult_humor", "random_funny").random()                    // Night window
                else -> null
            }

            if (friendlyCategory != null) {
                val prefSpam = applicationContext.getSharedPreferences("notification_spam_prevention", Context.MODE_PRIVATE)
                val evalKey = "friendly_eval_${friendlyCategory}_${todayStr}_$userId"
                val sentKey = "friendly_sent_${friendlyCategory}_${todayStr}_$userId"

                if (!prefSpam.contains(evalKey)) {
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

                        val templatePair = NotificationTemplateRepository.selectTemplate(
                            context = applicationContext,
                            category = friendlyCategory,
                            placeholders = placeholders
                        )

                        if (templatePair != null) {
                            val (template, formattedMsg) = templatePair
                            LocalNotificationManager.sendNotification(
                                context = applicationContext,
                                channelId = LocalNotificationManager.mapCategoryToChannel(friendlyCategory),
                                title = template.title,
                                message = formattedMsg,
                                category = friendlyCategory,
                                reason = "Seeded/randomized template notification for $friendlyCategory",
                                action = template.action
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WorkDailyNotification", "Error in NotificationWorker: ${e.localizedMessage}", e)
        }
        return Result.success()
    }
}
