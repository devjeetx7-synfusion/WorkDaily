package com.synfusion.attendance.notifications.engine

import android.content.Context
import com.synfusion.attendance.notifications.data.NotificationPreferencesRepository
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationEvent
import com.synfusion.attendance.notifications.model.NotificationPreferences
import java.util.Calendar

class NotificationDeduplicationManager {
    private val sentEventIds = mutableSetOf<String>()

    fun isDuplicate(eventId: String): Boolean {
        if (sentEventIds.contains(eventId)) {
            return true
        }
        sentEventIds.add(eventId)
        return false
    }

    fun clear() {
        sentEventIds.clear()
    }
}

class NotificationEligibilityValidator(private val prefsRepository: NotificationPreferencesRepository) {

    fun isCategoryEnabled(category: NotificationCategory, prefs: NotificationPreferences): Boolean {
        return when (category) {
            NotificationCategory.CHECK_IN -> prefs.checkInEnabled
            NotificationCategory.SPIN -> prefs.spinEnabled
            NotificationCategory.STREAK -> prefs.streakEnabled
            NotificationCategory.ATTENDANCE -> prefs.attendanceEnabled
            NotificationCategory.SALARY -> prefs.salaryEnabled
            NotificationCategory.PASSBOOK -> prefs.passbookEnabled
            NotificationCategory.BACKUP -> prefs.backupEnabled
            NotificationCategory.CHAT -> prefs.chatEnabled
            NotificationCategory.SOCIAL -> prefs.socialEnabled
            NotificationCategory.GIFTS -> prefs.giftsEnabled
            NotificationCategory.REDEEM -> prefs.redeemEnabled
            NotificationCategory.LIVE -> prefs.liveEnabled
            NotificationCategory.SYSTEM -> prefs.announcementsEnabled
        }
    }

    fun isInQuietHours(prefs: NotificationPreferences): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = prefs.quietStartHour
        val end = prefs.quietEndHour

        return if (start > end) {
            currentHour >= start || currentHour < end
        } else {
            currentHour in start until end
        }
    }
}
