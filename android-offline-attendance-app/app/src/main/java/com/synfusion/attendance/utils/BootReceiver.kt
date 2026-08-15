package com.synfusion.attendance.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.synfusion.attendance.data.OfflinePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            LocalNotificationManager.logNotificationHistory(
                context,
                "system_boot",
                "",
                "Boot/Package replacement received ($action) - Re-scheduling notification work"
            )

            LocalNotificationManager.initChannels(context)

            // Asynchronously check user settings and re-enforce background work
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefsRepo = OfflinePreferencesRepository(context.applicationContext)
                    val masterEnabled = prefsRepo.remindersFlow.first()
                    if (masterEnabled && LocalNotificationManager.areNotificationsFullyEnabled(context.applicationContext)) {
                        NotificationScheduler.schedulePeriodicWork(context.applicationContext)
                    }
                } catch (e: Exception) {
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
