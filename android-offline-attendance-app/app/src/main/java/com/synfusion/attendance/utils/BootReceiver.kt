package com.synfusion.attendance.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.synfusion.attendance.data.OfflinePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("WorkDailyNotification", "BootReceiver received system action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            LocalNotificationManager.logNotificationHistory(
                context,
                "system_boot",
                "",
                "System event ($action) - Re-scheduling notification alarms & workers"
            )

            LocalNotificationManager.initChannels(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefsRepo = OfflinePreferencesRepository(context.applicationContext)
                    val masterEnabled = prefsRepo.remindersFlow.first()
                    if (masterEnabled && LocalNotificationManager.areNotificationsFullyEnabled(context.applicationContext)) {
                        NotificationScheduler.scheduleAllSchedules(context.applicationContext)
                        Log.i("WorkDailyNotification", "BootReceiver successfully rescheduled notification alarms.")
                    } else {
                        Log.d("WorkDailyNotification", "BootReceiver: Reminders or system permissions disabled. Skipping reschedule.")
                    }
                } catch (e: Exception) {
                    Log.e("WorkDailyNotification", "BootReceiver rescheduling failed: ${e.localizedMessage}", e)
                    LocalNotificationManager.logNotificationHistory(
                        context,
                        "system_boot",
                        "",
                        "BootReceiver rescheduling failed: ${e.localizedMessage}"
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
