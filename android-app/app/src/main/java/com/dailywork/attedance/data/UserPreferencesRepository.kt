package com.dailywork.attedance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val ROLE_KEY = stringPreferencesKey("user_role")
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val THEME_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("dark_mode")
        val REMINDERS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("reminders_enabled")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")

        // Premium Keys
        val IS_PREMIUM_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_premium")
        val PREMIUM_TYPE_KEY = stringPreferencesKey("premium_type")
        val PURCHASE_TOKEN_KEY = stringPreferencesKey("purchase_token")
        val EXPIRY_DATE_KEY = stringPreferencesKey("expiry_date")
        val LAST_VERIFIED_KEY = androidx.datastore.preferences.core.longPreferencesKey("last_verified")
        val ONBOARDING_COMPLETED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")
        val SPLASH_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("splash_enabled")
    }

    val userRoleFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ROLE_KEY]
    }

    val authTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val languageFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY]
    }

    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY]
    }

    val remindersFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REMINDERS_KEY] ?: false
    }

    val isPremiumFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PREMIUM_KEY] ?: false
    }

    val premiumTypeFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PREMIUM_TYPE_KEY]
    }

    val purchaseTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PURCHASE_TOKEN_KEY]
    }

    val expiryDateFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[EXPIRY_DATE_KEY]
    }

    val lastVerifiedFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[LAST_VERIFIED_KEY]
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    val splashEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SPLASH_ENABLED_KEY] ?: true
    }

    suspend fun savePremiumStatus(
        isPremium: Boolean,
        type: String? = null,
        token: String? = null,
        expiry: String? = null,
        lastVerified: Long? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[IS_PREMIUM_KEY] = isPremium
            if (type != null) preferences[PREMIUM_TYPE_KEY] = type else preferences.remove(PREMIUM_TYPE_KEY)
            if (token != null) preferences[PURCHASE_TOKEN_KEY] = token else preferences.remove(PURCHASE_TOKEN_KEY)
            if (expiry != null) preferences[EXPIRY_DATE_KEY] = expiry else preferences.remove(EXPIRY_DATE_KEY)
            if (lastVerified != null) preferences[LAST_VERIFIED_KEY] = lastVerified else preferences.remove(LAST_VERIFIED_KEY)
        }
    }

    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[ROLE_KEY] = role
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun saveThemePreference(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = isDarkMode
        }
    }

    suspend fun saveRemindersPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMINDERS_KEY] = enabled
        }
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = lang
        }
    }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun saveSplashEnabledPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPLASH_ENABLED_KEY] = enabled
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
