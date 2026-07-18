package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdTestViewModel : ViewModel() {
    // Flags
    private val _isFullScreenAdShowing = MutableStateFlow(false)
    val isFullScreenAdShowing: StateFlow<Boolean> = _isFullScreenAdShowing

    fun setFullScreenAdShowing(showing: Boolean) {
        _isFullScreenAdShowing.value = showing
    }

    // Interstitial
    var interstitialAd: InterstitialAd? = null
    private val _interstitialStatus = MutableStateFlow("Not Loaded")
    val interstitialStatus: StateFlow<String> = _interstitialStatus

    fun setInterstitialStatus(status: String) {
        _interstitialStatus.value = status
    }

    // Rewarded
    var rewardedAd: RewardedAd? = null
    private val _rewardedStatus = MutableStateFlow("Not Loaded")
    val rewardedStatus: StateFlow<String> = _rewardedStatus
    private val _rewardedResult = MutableStateFlow("")
    val rewardedResult: StateFlow<String> = _rewardedResult

    fun setRewardedStatus(status: String) {
        _rewardedStatus.value = status
    }

    fun setRewardedResult(result: String) {
        _rewardedResult.value = result
    }

    // Rewarded Interstitial
    var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private val _rewardedInterstitialStatus = MutableStateFlow("Not Loaded")
    val rewardedInterstitialStatus: StateFlow<String> = _rewardedInterstitialStatus
    private val _rewardedInterstitialResult = MutableStateFlow("")
    val rewardedInterstitialResult: StateFlow<String> = _rewardedInterstitialResult

    fun setRewardedInterstitialStatus(status: String) {
        _rewardedInterstitialStatus.value = status
    }

    fun setRewardedInterstitialResult(result: String) {
        _rewardedInterstitialResult.value = result
    }

    // App Open
    var appOpenAd: AppOpenAd? = null
    private val _appOpenStatus = MutableStateFlow("Not Loaded")
    val appOpenStatus: StateFlow<String> = _appOpenStatus

    fun setAppOpenStatus(status: String) {
        _appOpenStatus.value = status
    }

    override fun onCleared() {
        super.onCleared()
        interstitialAd = null
        rewardedAd = null
        rewardedInterstitialAd = null
        appOpenAd = null
    }
}
