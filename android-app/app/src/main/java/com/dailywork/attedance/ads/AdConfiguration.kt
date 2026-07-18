package com.dailywork.attedance.ads

import com.dailywork.attedance.BuildConfig

object AdConfiguration {
    val isTestMode: Boolean = BuildConfig.DEBUG || BuildConfig.ADS_TEST_MODE

    val appOpenAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/9257395921"
        } else {
            BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID
        }

    val adaptiveBannerAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/9214589741"
        } else {
            BuildConfig.ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID
        }

    val fixedBannerAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/6300978111"
        } else {
            BuildConfig.ADMOB_FIXED_BANNER_AD_UNIT_ID
        }

    val interstitialAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/1033173712"
        } else {
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
        }

    val rewardedAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/5224354917"
        } else {
            BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
        }

    val rewardedInterstitialAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/5354046379"
        } else {
            BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID
        }

    val nativeAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/2247696110"
        } else {
            BuildConfig.ADMOB_NATIVE_AD_UNIT_ID
        }

    val nativeVideoAdUnitId: String
        get() = if (isTestMode) {
            "ca-app-pub-3940256099942544/1044960115"
        } else {
            BuildConfig.ADMOB_NATIVE_VIDEO_AD_UNIT_ID
        }
}
