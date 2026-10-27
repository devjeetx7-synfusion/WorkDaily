package com.synfusion.attendance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.offlineDataStore: DataStore<Preferences> by preferencesDataStore(name = "synfusion_user_prefs")

open class OfflinePreferencesRepository(val context: Context) {

    companion object {
        val ROLE_KEY = stringPreferencesKey("user_role")
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val THEME_KEY = booleanPreferencesKey("dark_mode")
        val REMINDERS_KEY = booleanPreferencesKey("reminders_enabled")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        val COUNTRY_KEY = stringPreferencesKey("user_country")
        val CURRENCY_KEY = stringPreferencesKey("user_currency")

        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val ONLINE_ACCOUNT_SETUP_COMPLETED_KEY = booleanPreferencesKey("online_account_setup_completed")
        val LINKED_FIREBASE_UID_KEY = stringPreferencesKey("linked_firebase_uid")
        val GOOGLE_PHOTO_URL_KEY = stringPreferencesKey("google_photo_url")
        val SPLASH_ENABLED_KEY = booleanPreferencesKey("splash_enabled")
        val PROFILE_PHOTO_PATH_KEY = stringPreferencesKey("profile_photo_path")
        val LAST_BACKUP_TIME_KEY = longPreferencesKey("last_backup_time")
        val LAST_BACKUP_SIZE_KEY = longPreferencesKey("last_backup_size")
        val LAST_BACKUP_LOC_KEY = stringPreferencesKey("last_backup_loc")

        val NAV_LEFT_1_KEY = stringPreferencesKey("nav_left_1")
        val NAV_LEFT_2_KEY = stringPreferencesKey("nav_left_2")
        val NAV_RIGHT_KEY = stringPreferencesKey("nav_right")
        val STARTUP_SCREEN_KEY = stringPreferencesKey("startup_screen")
        val REMEMBER_LAST_SCREEN_KEY = booleanPreferencesKey("remember_last_screen")
        val LAST_ACTIVE_SCREEN_KEY = stringPreferencesKey("last_active_screen")
    }

    open val userRoleFlow: Flow<String?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[ROLE_KEY]
        }

    val countryFlow: Flow<String>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[COUNTRY_KEY] ?: "IN"
        }

    val currencyFlow: Flow<String>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[CURRENCY_KEY] ?: "INR"
        }

    val lastBackupTimeFlow: Flow<Long?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[LAST_BACKUP_TIME_KEY]
        }

    val lastBackupSizeFlow: Flow<Long?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[LAST_BACKUP_SIZE_KEY]
        }

    val lastBackupLocFlow: Flow<String?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[LAST_BACKUP_LOC_KEY]
        }

    open val profilePhotoPathFlow: Flow<String?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[PROFILE_PHOTO_PATH_KEY]
        }

    open val googlePhotoUrlFlow: Flow<String?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[GOOGLE_PHOTO_URL_KEY]
        }

    open val authTokenFlow: Flow<String?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }

    val languageFlow: Flow<String?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY]
        }

    val darkModeFlow: Flow<Boolean?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[THEME_KEY]
        }

    val remindersFlow: Flow<Boolean>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[REMINDERS_KEY] ?: false
        }

    open val isOnboardingCompletedFlow: Flow<Boolean>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false
        }

    open val isOnlineAccountSetupCompletedFlow: Flow<Boolean>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[ONLINE_ACCOUNT_SETUP_COMPLETED_KEY] ?: false
        }

    open val linkedFirebaseUidFlow: Flow<String?>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[LINKED_FIREBASE_UID_KEY]
        }

    val splashEnabledFlow: Flow<Boolean>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[SPLASH_ENABLED_KEY] ?: true
        }

    val navLeft1Flow: Flow<String>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[NAV_LEFT_1_KEY] ?: "dashboard"
        }

    val navLeft2Flow: Flow<String>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[NAV_LEFT_2_KEY] ?: "calendar"
        }

    val navRightFlow: Flow<String>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[NAV_RIGHT_KEY] ?: "settings"
        }

    val startupScreenFlow: Flow<String>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[STARTUP_SCREEN_KEY] ?: "dashboard"
        }

    val rememberLastScreenFlow: Flow<Boolean>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[REMEMBER_LAST_SCREEN_KEY] ?: false
        }

    val lastActiveScreenFlow: Flow<String>
        get() = context.offlineDataStore.data.map { preferences ->
            preferences[LAST_ACTIVE_SCREEN_KEY] ?: "dashboard"
        }

    open suspend fun saveCountryAndCurrency(countryCode: String, currencyCode: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[COUNTRY_KEY] = countryCode
            preferences[CURRENCY_KEY] = currencyCode
        }
    }

    open suspend fun saveUserRole(role: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[ROLE_KEY] = role
        }
    }

    open suspend fun saveAuthToken(token: String) {
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

    open suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    open suspend fun saveOnlineAccountSetupCompleted(completed: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[ONLINE_ACCOUNT_SETUP_COMPLETED_KEY] = completed
        }
    }

    open suspend fun saveLinkedFirebaseUid(uid: String?) {
        context.offlineDataStore.edit { preferences ->
            if (uid != null) {
                preferences[LINKED_FIREBASE_UID_KEY] = uid
            } else {
                preferences.remove(LINKED_FIREBASE_UID_KEY)
            }
        }
    }

    open suspend fun saveGooglePhotoUrl(url: String?) {
        context.offlineDataStore.edit { preferences ->
            if (url != null) {
                preferences[GOOGLE_PHOTO_URL_KEY] = url
            } else {
                preferences.remove(GOOGLE_PHOTO_URL_KEY)
            }
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

    suspend fun saveNavigationConfig(left1: String, left2: String, right: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[NAV_LEFT_1_KEY] = left1
            preferences[NAV_LEFT_2_KEY] = left2
            preferences[NAV_RIGHT_KEY] = right
        }
    }

    suspend fun saveStartupScreen(screen: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[STARTUP_SCREEN_KEY] = screen
        }
    }

    suspend fun saveRememberLastScreen(remember: Boolean) {
        context.offlineDataStore.edit { preferences ->
            preferences[REMEMBER_LAST_SCREEN_KEY] = remember
        }
    }

    suspend fun saveLastActiveScreen(screen: String) {
        context.offlineDataStore.edit { preferences ->
            preferences[LAST_ACTIVE_SCREEN_KEY] = screen
        }
    }

    /**
     * Clears active session tokens and online account bindings during sign-out.
     * Preserves UserEntity, attendance records, summaries, and local backup files,
     * while allowing account switching for Google Sign-In.
     */
    open suspend fun clearAuthSession() {
        context.offlineDataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(ROLE_KEY)
            preferences.remove(LINKED_FIREBASE_UID_KEY)
            preferences.remove(ONLINE_ACCOUNT_SETUP_COMPLETED_KEY)
            preferences.remove(GOOGLE_PHOTO_URL_KEY)
        }
    }

    suspend fun clearSession() {
        clearAuthSession()
    }
}
