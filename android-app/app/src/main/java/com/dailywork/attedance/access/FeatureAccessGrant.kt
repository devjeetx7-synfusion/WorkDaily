package com.dailywork.attedance.access

data class FeatureAccessGrant(
    val feature: RewardedFeature,
    val keySuffix: String = "",
    val expiresAt: Long = 0L,
    val oneShotTokens: Int = 0
) {
    fun isValid(now: Long = System.currentTimeMillis()): Boolean {
        return expiresAt > now || oneShotTokens > 0
    }
}
