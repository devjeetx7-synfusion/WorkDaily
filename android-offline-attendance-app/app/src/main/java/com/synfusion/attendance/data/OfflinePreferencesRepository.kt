package com.synfusion.attendance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.offlineDataStore: DataStore<Preferences> by preferencesDataStore(name = "synfusion_user_prefs")

class OfflinePreferencesRepository(private val context: Context) {

    companion object {
        val ROLE_KEY = stringPreferencesKey("user_role")
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val THEME_KEY = booleanPreferencesKey("dark_mode")
        val REMINDERS_KEY = booleanPreferencesKey("reminders_enabled")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")

        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val SPLASH_ENABLED_KEY = booleanPreferencesKey("splash_enabled")

        // Notification Settings Keys
        val NOTIFICATIONS_MASTER_KEY = booleanPreferencesKey("notifications_master_enabled")
        val NOTIFICATIONS_ATTENDANCE_KEY = booleanPreferencesKey("notifications_attendance_reminders")
        val NOTIFICATIONS_WEEKLY_KEY = booleanPreferencesKey("notifications_weekly_reports")
        val NOTIFICATIONS_MONTHLY_KEY = booleanPreferencesKey("notifications_monthly_reports")
        val NOTIFICATIONS_EARNINGS_KEY = booleanPreferencesKey("notifications_earnings_notifications")
        val NOTIFICATIONS_FUNNY_KEY = booleanPreferencesKey("notifications_funny_notifications")
        val NOTIFICATION_STYLE_KEY = stringPreferencesKey("notification_style_preference")
    }

    val userRoleFlow: Flow<String?> = context.offlineDataStore.data.map { preferences ->
        preferences[ROLE_KEY]
    }

    val authTokenFlow: Flow<String?> = context.offlineDataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val languageFlow: Flow<String?> = context.offlineDataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY]
    }

    val darkModeFlow: Flow<Boolean?> = context.offlineDataStore.data.map { preferences ->
        preferences[THEME_KEY]
    }

    val remindersFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[REMINDERS_KEY] ?: false
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    val splashEnabledFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[SPLASH_ENABLED_KEY] ?: true
    }

    // Notification Settings Flows
    val notificationsMasterFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_MASTER_KEY] ?: true
    }

    val notificationsAttendanceFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ATTENDANCE_KEY] ?: true
    }

    val notificationsWeeklyFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_WEEKLY_KEY] ?: true
    }

    val notificationsMonthlyFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_MONTHLY_KEY] ?: true
    }

    val notificationsEarningsFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_EARNINGS_KEY] ?: true
    }

    val notificationsFunnyFlow: Flow<Boolean> = context.offlineDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_FUNNY_KEY] ?: true
    }

    val notificationStyleFlow: Flow<String> = context.offlineDataStore.data.map { preferences ->
        preferences[NOTIFICATION_STYLE_KEY] ?: "Friendly"
    }

    suspend fun saveUserRole(role: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[ROLE_KEY] = role
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun saveThemePreference(isDarkMode: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[THEME_KEY] = isDarkMode
        }
    }

    suspend fun saveRemindersPreference(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[REMINDERS_KEY] = enabled
        }
    }

    suspend fun saveLanguage(lang: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = lang
        }
    }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun saveSplashEnabledPreference(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[SPLASH_ENABLED_KEY] = enabled
        }
    }

    // Notification Saving Functions
    suspend fun saveNotificationsMaster(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_MASTER_KEY] = enabled
        }
    }

    suspend fun saveNotificationsAttendance(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ATTENDANCE_KEY] = enabled
        }
    }

    suspend fun saveNotificationsWeekly(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_WEEKLY_KEY] = enabled
        }
    }

    suspend fun saveNotificationsMonthly(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_MONTHLY_KEY] = enabled
        }
    }

    suspend fun saveNotificationsEarnings(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_EARNINGS_KEY] = enabled
        }
    }

    suspend fun saveNotificationsFunny(enabled: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_FUNNY_KEY] = enabled
        }
    }

    suspend fun saveNotificationStyle(style: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[NOTIFICATION_STYLE_KEY] = style
        }
    }

    suspend fun clearSession() {
        context.offlineDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
