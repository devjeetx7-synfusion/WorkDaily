package com.dailywork.attedance.access

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.dailywork.attedance.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FeatureAccessRepository(private val context: Context) {

    fun getPrefKey(feature: RewardedFeature, suffix: String): String {
        return when (feature) {
            RewardedFeature.ADD_WORKER -> "reward_add_worker_tokens"
            RewardedFeature.ADVANCED_STATS -> "reward_stats_expires_at"
            RewardedFeature.WORKER_HISTORY -> "reward_history_expires_at"
            RewardedFeature.PERSONAL_PASSBOOK_DETAILS -> "reward_personal_passbook_expires_at"
            RewardedFeature.WORKER_DETAILS -> "reward_worker_${suffix}_expires_at"
            RewardedFeature.PERSONAL_REPORT_TOOLS -> "reward_personal_report_${suffix}_expires_at"
            RewardedFeature.WORKER_REPORT_TOOLS -> "reward_worker_report_${suffix}_expires_at"
        }
    }

    suspend fun getGrant(feature: RewardedFeature, suffix: String = ""): FeatureAccessGrant {
        val keyName = getPrefKey(feature, suffix)
        val prefs = context.dataStore.data.first()
        return if (feature == RewardedFeature.ADD_WORKER) {
            val tokens = prefs[intPreferencesKey(keyName)] ?: 0
            FeatureAccessGrant(feature, suffix, oneShotTokens = tokens)
        } else {
            val expires = prefs[longPreferencesKey(keyName)] ?: 0L
            FeatureAccessGrant(feature, suffix, expiresAt = expires)
        }
    }

    fun getGrantFlow(feature: RewardedFeature, suffix: String = ""): Flow<FeatureAccessGrant> {
        val keyName = getPrefKey(feature, suffix)
        return context.dataStore.data.map { prefs ->
            if (feature == RewardedFeature.ADD_WORKER) {
                val tokens = prefs[intPreferencesKey(keyName)] ?: 0
                FeatureAccessGrant(feature, suffix, oneShotTokens = tokens)
            } else {
                val expires = prefs[longPreferencesKey(keyName)] ?: 0L
                FeatureAccessGrant(feature, suffix, expiresAt = expires)
            }
        }
    }

    suspend fun saveGrant(grant: FeatureAccessGrant) {
        val keyName = getPrefKey(grant.feature, grant.keySuffix)
        context.dataStore.edit { prefs ->
            if (grant.feature == RewardedFeature.ADD_WORKER) {
                prefs[intPreferencesKey(keyName)] = grant.oneShotTokens
            } else {
                prefs[longPreferencesKey(keyName)] = grant.expiresAt
            }
        }
    }

    suspend fun clearAllGrants() {
        context.dataStore.edit { prefs ->
            val keysToClear = prefs.asMap().keys.filter { it.name.startsWith("reward_") }
            keysToClear.forEach { key ->
                prefs.remove(key)
            }
        }
    }
}
