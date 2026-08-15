package com.synfusion.attendance.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("WorkDailyNotification", "NotificationAlarmReceiver received action: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val prefsRepo = OfflinePreferencesRepository(appContext)

                val remindersEnabled = prefsRepo.remindersFlow.first()
                if (!remindersEnabled) {
                    Log.d("WorkDailyNotification", "NotificationAlarmReceiver: Reminders disabled by user preference.")
                    LocalNotificationManager.logNotificationHistory(
                        appContext,
                        "alarm_receiver",
                        "",
                        "SKIPPED: Reminders disabled in preferences"
                    )
                    return@launch
                }

                if (!LocalNotificationManager.areNotificationsFullyEnabled(appContext)) {
                    Log.w("WorkDailyNotification", "NotificationAlarmReceiver: System notifications or permissions disabled.")
                    LocalNotificationManager.logNotificationHistory(
                        appContext,
                        "alarm_receiver",
                        "",
                        "SKIPPED: System notifications or permissions disabled"
                    )
                    return@launch
                }

                val repository = LocalAttendanceRepository(appContext)
                val activeUser = repository.getUser()
                if (activeUser == null) {
                    Log.d("WorkDailyNotification", "NotificationAlarmReceiver: No active user profile.")
                    return@launch
                }

                when (action) {
                    NotificationScheduler.ACTION_DAILY_REMINDER -> {
                        val evalResult = NotificationRuleEngine.evaluateAllRules(appContext, repository, "Friendly")
                        if (evalResult.triggerNotification) {
                            val posted = LocalNotificationManager.sendNotification(
                                context = appContext,
                                channelId = evalResult.channelId,
                                title = evalResult.title,
                                message = evalResult.message,
                                category = evalResult.category,
                                reason = evalResult.reason,
                                action = "home"
                            )
                            if (posted) {
                                NotificationRuleEngine.markRuleStatePersisted(appContext, evalResult)
                            }
                        } else {
                            Log.d("WorkDailyNotification", "NotificationAlarmReceiver: Daily rules evaluated, no notification required.")
                        }
                    }
                    NotificationScheduler.ACTION_SUMMARY_REMINDER -> {
                        val evalResult = NotificationRuleEngine.evaluateAllRules(appContext, repository, "Friendly")
                        if (evalResult.triggerNotification && (evalResult.category.contains("summary") || evalResult.category.contains("report"))) {
                            val posted = LocalNotificationManager.sendNotification(
                                context = appContext,
                                channelId = evalResult.channelId,
                                title = evalResult.title,
                                message = evalResult.message,
                                category = evalResult.category,
                                reason = evalResult.reason,
                                action = "passbook"
                            )
                            if (posted) {
                                NotificationRuleEngine.markRuleStatePersisted(appContext, evalResult)
                            }
                        }
                    }
                    NotificationScheduler.ACTION_BACKUP_REMINDER -> {
                        val templatePair = NotificationTemplateRepository.selectTemplate(appContext, "backup")
                        if (templatePair != null) {
                            val (template, formattedMsg) = templatePair
                            LocalNotificationManager.sendNotification(
                                context = appContext,
                                channelId = LocalNotificationManager.CHANNEL_BACKUP,
                                title = template.title,
                                message = formattedMsg,
                                category = "backup",
                                reason = "Scheduled backup reminder alarm triggered",
                                action = "settings"
                            )
                        }
                    }
                    else -> {
                        Log.d("WorkDailyNotification", "NotificationAlarmReceiver: Unhandled action $action")
                    }
                }

                // Reschedule next alarms cleanly for future triggers
                NotificationScheduler.scheduleExactAlarms(appContext)
            } catch (e: Exception) {
                Log.e("WorkDailyNotification", "Error in NotificationAlarmReceiver: ${e.localizedMessage}", e)
                LocalNotificationManager.logNotificationHistory(
                    context,
                    "alarm_receiver",
                    "",
                    "ERROR: ${e.localizedMessage}"
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
