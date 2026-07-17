package com.dailywork.attedance.access

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FeatureAccessController private constructor(context: Context) {

    private val repository = FeatureAccessRepository(context)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Config defaults
    var statsUnlockMinutes = 30
    var historyUnlockMinutes = 30
    var ledgerUnlockMinutes = 30
    var reportUnlockMinutes = 10
    var fallbackUnlockEnabled = false
    var rewardedAdsEnabled = true
    var freeWorkerLimit = 5

    // Legacy entitlements (in-memory cache synced with Firestore)
    private val _legacyLifetimeAccess = MutableStateFlow(false)
    private val _legacyAccessUntil = MutableStateFlow(0L)

    init {
        setupConfigListener()
        setupUserLegacyListener()
    }

    private fun setupConfigListener() {
        db.collection("config").document("settings")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    statsUnlockMinutes = (snapshot.getLong("statsUnlockMinutes") ?: 30).toInt()
                    historyUnlockMinutes = (snapshot.getLong("historyUnlockMinutes") ?: 30).toInt()
                    ledgerUnlockMinutes = (snapshot.getLong("ledgerUnlockMinutes") ?: 30).toInt()
                    reportUnlockMinutes = (snapshot.getLong("reportUnlockMinutes") ?: 10).toInt()
                    fallbackUnlockEnabled = snapshot.getBoolean("fallbackUnlockEnabled") ?: false
                    rewardedAdsEnabled = snapshot.getBoolean("rewardedAdsEnabled") ?: true
                    freeWorkerLimit = (snapshot.getLong("freeWorkerLimit") ?: 5).toInt()
                }
            }
    }

    private fun setupUserLegacyListener() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                db.collection("users").document(user.uid)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null && snapshot.exists()) {
                            val lifetime = snapshot.getBoolean("legacyLifetimeAccess") ?: snapshot.getBoolean("isPremium") ?: false
                            val until = snapshot.getLong("legacyAccessUntil") ?: 0L
                            _legacyLifetimeAccess.value = lifetime
                            _legacyAccessUntil.value = until
                        }
                    }
            } else {
                _legacyLifetimeAccess.value = false
                _legacyAccessUntil.value = 0L
            }
        }
    }

    fun hasLegacyAccess(): Boolean {
        if (_legacyLifetimeAccess.value) return true
        if (_legacyAccessUntil.value > System.currentTimeMillis()) return true
        return false
    }

    suspend fun hasAccess(feature: RewardedFeature, suffix: String = ""): Boolean {
        if (hasLegacyAccess()) return true
        val grant = repository.getGrant(feature, suffix)
        return grant.isValid()
    }

    fun getAccessFlow(feature: RewardedFeature, suffix: String = ""): Flow<Boolean> {
        return repository.getGrantFlow(feature, suffix).map { grant ->
            if (hasLegacyAccess()) true else grant.isValid()
        }
    }

    suspend fun grantAccess(feature: RewardedFeature, suffix: String = "") {
        val policy = when (feature) {
            RewardedFeature.ADD_WORKER -> AccessPolicy.OneShot
            RewardedFeature.ADVANCED_STATS -> AccessPolicy.Timed(statsUnlockMinutes * 60 * 1000L)
            RewardedFeature.WORKER_HISTORY -> AccessPolicy.Timed(historyUnlockMinutes * 60 * 1000L)
            RewardedFeature.PERSONAL_PASSBOOK_DETAILS -> AccessPolicy.Timed(ledgerUnlockMinutes * 60 * 1000L)
            RewardedFeature.WORKER_DETAILS -> AccessPolicy.Timed(ledgerUnlockMinutes * 60 * 1000L)
            RewardedFeature.PERSONAL_REPORT_TOOLS -> AccessPolicy.Timed(reportUnlockMinutes * 60 * 1000L)
            RewardedFeature.WORKER_REPORT_TOOLS -> AccessPolicy.Timed(reportUnlockMinutes * 60 * 1000L)
        }

        val now = System.currentTimeMillis()
        val grant = when (policy) {
            is AccessPolicy.OneShot -> {
                val current = repository.getGrant(feature, suffix)
                FeatureAccessGrant(feature, suffix, oneShotTokens = current.oneShotTokens + 1)
            }
            is AccessPolicy.Timed -> {
                FeatureAccessGrant(feature, suffix, expiresAt = now + policy.durationMillis)
            }
        }
        repository.saveGrant(grant)
    }

    suspend fun consumeToken(feature: RewardedFeature, suffix: String = ""): Boolean {
        if (hasLegacyAccess()) return true
        if (feature == RewardedFeature.ADD_WORKER) {
            val current = repository.getGrant(feature, suffix)
            if (current.oneShotTokens > 0) {
                repository.saveGrant(current.copy(oneShotTokens = current.oneShotTokens - 1))
                return true
            }
            return false
        }
        return true
    }

    suspend fun clearSession() {
        repository.clearAllGrants()
    }

    companion object {
        @Volatile
        private var INSTANCE: FeatureAccessController? = null

        fun getInstance(context: Context): FeatureAccessController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeatureAccessController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
