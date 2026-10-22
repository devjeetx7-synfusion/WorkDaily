package com.synfusion.attendance.notifications.engine

import android.content.Context
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.notifications.data.NotificationEntity
import com.synfusion.attendance.notifications.data.NotificationPreferencesRepository
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkDailyNotificationCoordinator(
    private val context: Context,
    private val preferencesRepository: NotificationPreferencesRepository = NotificationPreferencesRepository(context)
) {
    private val database = AttendanceDatabase.getInstance(context)
    private val notificationDao = database.notificationDao()
    private val deduplicationManager = NotificationDeduplicationManager()
    private val validator = NotificationEligibilityValidator(preferencesRepository)
    private val localHelper = LocalNotificationHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun postNotification(event: NotificationEvent, triggerSystemAlert: Boolean = true) {
        scope.launch {
            if (deduplicationManager.isDuplicate(event.id)) {
                return@launch
            }

            val prefs = preferencesRepository.preferencesFlow.first()
            if (!validator.isCategoryEnabled(event.category, prefs)) {
                return@launch
            }

            // Save to local database inbox
            notificationDao.insertNotification(NotificationEntity.fromDomain(event))

            // Trigger system notification if permitted and not in quiet hours
            if (triggerSystemAlert && !validator.isInQuietHours(prefs)) {
                localHelper.sendSystemNotification(event)
            }
        }
    }

    suspend fun clearDemoNotifications() {
        notificationDao.clearDemoNotifications()
    }
}
