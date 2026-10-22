package com.synfusion.attendance.notifications.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.synfusion.attendance.notifications.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "wd_notification_preferences")

class NotificationPreferencesRepository(private val context: Context) {

    private object Keys {
        val CHECK_IN = booleanPreferencesKey("notif_check_in")
        val SPIN = booleanPreferencesKey("notif_spin")
        val STREAK = booleanPreferencesKey("notif_streak")
        val ATTENDANCE = booleanPreferencesKey("notif_attendance")
        val SALARY = booleanPreferencesKey("notif_salary")
        val PASSBOOK = booleanPreferencesKey("notif_passbook")
        val BACKUP = booleanPreferencesKey("notif_backup")
        val FESTIVALS = booleanPreferencesKey("notif_festivals")
        val LIVE = booleanPreferencesKey("notif_live")
        val CHAT = booleanPreferencesKey("notif_chat")
        val SOCIAL = booleanPreferencesKey("notif_social")
        val GIFTS = booleanPreferencesKey("notif_gifts")
        val REDEEM = booleanPreferencesKey("notif_redeem")
        val ANNOUNCEMENTS = booleanPreferencesKey("notif_announcements")

        val QUIET_HOURS = booleanPreferencesKey("notif_quiet_hours")
        val QUIET_START = intPreferencesKey("notif_quiet_start")
        val QUIET_END = intPreferencesKey("notif_quiet_end")
        val SOUND = booleanPreferencesKey("notif_sound")
        val VIBRATION = booleanPreferencesKey("notif_vibration")
        val ATTENDANCE_TIME = stringPreferencesKey("notif_attendance_time")
    }

    val preferencesFlow: Flow<NotificationPreferences> = context.notificationDataStore.data.map { prefs ->
        NotificationPreferences(
            checkInEnabled = prefs[Keys.CHECK_IN] ?: true,
            spinEnabled = prefs[Keys.SPIN] ?: true,
            streakEnabled = prefs[Keys.STREAK] ?: true,
            attendanceEnabled = prefs[Keys.ATTENDANCE] ?: true,
            salaryEnabled = prefs[Keys.SALARY] ?: true,
            passbookEnabled = prefs[Keys.PASSBOOK] ?: true,
            backupEnabled = prefs[Keys.BACKUP] ?: true,
            festivalsEnabled = prefs[Keys.FESTIVALS] ?: true,
            liveEnabled = prefs[Keys.LIVE] ?: true,
            chatEnabled = prefs[Keys.CHAT] ?: true,
            socialEnabled = prefs[Keys.SOCIAL] ?: true,
            giftsEnabled = prefs[Keys.GIFTS] ?: true,
            redeemEnabled = prefs[Keys.REDEEM] ?: true,
            announcementsEnabled = prefs[Keys.ANNOUNCEMENTS] ?: true,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS] ?: false,
            quietStartHour = prefs[Keys.QUIET_START] ?: 22,
            quietEndHour = prefs[Keys.QUIET_END] ?: 7,
            soundEnabled = prefs[Keys.SOUND] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
            attendanceReminderTime = prefs[Keys.ATTENDANCE_TIME] ?: "08:00"
        )
    }

    suspend fun updateCategoryEnabled(categoryKey: String, enabled: Boolean) {
        context.notificationDataStore.edit { prefs ->
            when (categoryKey) {
                "check_in" -> prefs[Keys.CHECK_IN] = enabled
                "spin" -> prefs[Keys.SPIN] = enabled
                "streak" -> prefs[Keys.STREAK] = enabled
                "attendance" -> prefs[Keys.ATTENDANCE] = enabled
                "salary" -> prefs[Keys.SALARY] = enabled
                "passbook" -> prefs[Keys.PASSBOOK] = enabled
                "backup" -> prefs[Keys.BACKUP] = enabled
                "festivals" -> prefs[Keys.FESTIVALS] = enabled
                "live" -> prefs[Keys.LIVE] = enabled
                "chat" -> prefs[Keys.CHAT] = enabled
                "social" -> prefs[Keys.SOCIAL] = enabled
                "gifts" -> prefs[Keys.GIFTS] = enabled
                "redeem" -> prefs[Keys.REDEEM] = enabled
                "announcements" -> prefs[Keys.ANNOUNCEMENTS] = enabled
            }
        }
    }

    suspend fun updateQuietHours(enabled: Boolean, startHour: Int = 22, endHour: Int = 7) {
        context.notificationDataStore.edit { prefs ->
            prefs[Keys.QUIET_HOURS] = enabled
            prefs[Keys.QUIET_START] = startHour
            prefs[Keys.QUIET_END] = endHour
        }
    }

    suspend fun updateSoundAndVibration(sound: Boolean, vibration: Boolean) {
        context.notificationDataStore.edit { prefs ->
            prefs[Keys.SOUND] = sound
            prefs[Keys.VIBRATION] = vibration
        }
    }

    suspend fun updateAttendanceReminderTime(time: String) {
        context.notificationDataStore.edit { prefs ->
            prefs[Keys.ATTENDANCE_TIME] = time
        }
    }
}
