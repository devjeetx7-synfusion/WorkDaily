package com.dailywork.attedance.ads

import com.dailywork.attedance.BuildConfig
import android.util.Log

object AdConfiguration {
    val isTestMode: Boolean = BuildConfig.ADS_TEST_MODE

    private const val DEMO_PUBLISHER_PREFIX = "ca-app-pub-3940256099942544"

    private fun checkProductionId(id: String): String {
        if (!BuildConfig.ADS_TEST_MODE && id.startsWith(DEMO_PUBLISHER_PREFIX)) {
            Log.e("AdConfiguration", "CRITICAL CONFIGURATION ERROR: Demo ad unit ID used in production release build! ID: $id")
            return ""
        }
        return id
    }

    val appOpenAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/9257395921"
        } else {
            checkProductionId(BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID)
        }

    val adaptiveBannerAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/9214589741"
        } else {
            checkProductionId(BuildConfig.ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID)
        }

    val fixedBannerAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/6300978111"
        } else {
            checkProductionId(BuildConfig.ADMOB_FIXED_BANNER_AD_UNIT_ID)
        }

    val interstitialAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/1033173712"
        } else {
            checkProductionId(BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID)
        }

    val rewardedAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/5224354917"
        } else {
            checkProductionId(BuildConfig.ADMOB_REWARDED_AD_UNIT_ID)
        }

    val rewardedInterstitialAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/5354046379"
        } else {
            checkProductionId(BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID)
        }

    val nativeAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/2247696110"
        } else {
            checkProductionId(BuildConfig.ADMOB_NATIVE_AD_UNIT_ID)
        }

    val nativeVideoAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/1044960115"
        } else {
            checkProductionId(BuildConfig.ADMOB_NATIVE_VIDEO_AD_UNIT_ID)
        }
}
