package com.synfusion.attendance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.offlineDataStore: DataStore<Preferences> by preferencesDataStore(name = "synfusion_user_prefs")

class OfflinePreferencesRepository(val context: Context) {

    companion object {
        val ROLE_KEY = stringPreferencesKey("user_role")
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val THEME_KEY = booleanPreferencesKey("dark_mode")
        val REMINDERS_KEY = booleanPreferencesKey("reminders_enabled")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")

        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val SPLASH_ENABLED_KEY = booleanPreferencesKey("splash_enabled")
        val PROFILE_PHOTO_PATH_KEY = stringPreferencesKey("profile_photo_path")
        val LAST_BACKUP_TIME_KEY = longPreferencesKey("last_backup_time")
        val LAST_BACKUP_SIZE_KEY = longPreferencesKey("last_backup_size")
        val LAST_BACKUP_LOC_KEY = stringPreferencesKey("last_backup_loc")
    }

    val userRoleFlow: Flow<String?> = context.offlineDataStore.data.map { preferences ->
        preferences[ROLE_KEY]
    }

    val lastBackupTimeFlow: Flow<Long?> = context.offlineDataStore.data.map { preferences ->
        preferences[LAST_BACKUP_TIME_KEY]
    }

    val lastBackupSizeFlow: Flow<Long?> = context.offlineDataStore.data.map { preferences ->
        preferences[LAST_BACKUP_SIZE_KEY]
    }

    val lastBackupLocFlow: Flow<String?> = context.offlineDataStore.data.map { preferences ->
        preferences[LAST_BACKUP_LOC_KEY]
    }

    val profilePhotoPathFlow: Flow<String?> = context.offlineDataStore.data.map { preferences ->
        preferences[PROFILE_PHOTO_PATH_KEY]
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

    suspend fun saveProfilePhotoPath(path: String?) {
        context.offlineDataStore.edit { preferences ->
            if (path != null) {
                preferences[PROFILE_PHOTO_PATH_KEY] = path
            } else {
                preferences.remove(PROFILE_PHOTO_PATH_KEY)
            }
        }
    }

    suspend fun saveBackupMetadata(timestamp: Long, size: Long, location: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[LAST_BACKUP_TIME_KEY] = timestamp
            preferences[LAST_BACKUP_SIZE_KEY] = size
            preferences[LAST_BACKUP_LOC_KEY] = location
        }
    }

    suspend fun clearSession() {
        context.offlineDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
