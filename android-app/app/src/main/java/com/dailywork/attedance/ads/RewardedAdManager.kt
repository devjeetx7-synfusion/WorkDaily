package com.dailywork.attedance.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dailywork.attedance.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class RewardedAdState {
    IDLE, LOADING, READY, SHOWING, FAILED
}

class RewardedAdManager private constructor(private val context: Context) {

    private val _state = MutableStateFlow(RewardedAdState.IDLE)
    val state: StateFlow<RewardedAdState> = _state

    private var rewardedAd: RewardedAd? = null
    private val isInitializing = AtomicBoolean(false)
    private val isMobileAdsInitialized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val adUnitId: String
        get() {
            val unitId = AdConfiguration.rewardedAdUnitId
            if (unitId.isBlank()) {
                Log.e("RewardedAdManager", "Missing AdMob Rewarded Ad Unit ID!")
            }
            return unitId
        }

    fun initialize(onComplete: () -> Unit = {}) {
        if (isMobileAdsInitialized.get()) {
            onComplete()
            return
        }
        if (isInitializing.compareAndSet(false, true)) {
            mainHandler.post {
                try {
                    MobileAds.initialize(context) {
                        isMobileAdsInitialized.set(true)
                        isInitializing.set(false)
                        Log.d("RewardedAdManager", "Mobile Ads SDK Initialized")
                        preloadAd()
                        onComplete()
                    }
                } catch (e: Exception) {
                    isInitializing.set(false)
                    Log.e("RewardedAdManager", "Failed to initialize Mobile Ads", e)
                    onComplete()
                }
            }
        }
    }

    fun preloadAd() {
        if (!isMobileAdsInitialized.get()) {
            Log.w("RewardedAdManager", "Cannot preload, Mobile Ads not initialized yet")
            return
        }
        if (_state.value == RewardedAdState.LOADING || _state.value == RewardedAdState.SHOWING) {
            return
        }

        val adId = adUnitId
        if (adId.isBlank()) {
            _state.value = RewardedAdState.FAILED
            Log.e("RewardedAdManager", "Empty Ad Unit ID. Disabling ads.")
            return
        }

        mainHandler.post {
            _state.value = RewardedAdState.LOADING
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(context, adId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _state.value = RewardedAdState.READY
                    Log.d("RewardedAdManager", "Rewarded Ad Loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _state.value = RewardedAdState.FAILED
                    Log.e("RewardedAdManager", "Failed to load rewarded ad: ${error.message} (code: ${error.code})")
                }
            })
        }
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit, onDismissed: () -> Unit, onFailedToShow: (String) -> Unit) {
        if (_state.value != RewardedAdState.READY || rewardedAd == null) {
            onFailedToShow("Ad not ready yet")
            preloadAd()
            return
        }
        if (_state.value == RewardedAdState.SHOWING) {
            onFailedToShow("An ad is already showing")
            return
        }

        mainHandler.post {
            _state.value = RewardedAdState.SHOWING
            val ad = rewardedAd
            rewardedAd = null // Clear used ad instance

            var rewardGranted = false

            ad?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d("RewardedAdManager", "Ad showed full screen content")
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    _state.value = RewardedAdState.FAILED
                    Log.e("RewardedAdManager", "Ad failed to show: ${error.message}")
                    onFailedToShow(error.message)
                    preloadAd()
                }

                override fun onAdDismissedFullScreenContent() {
                    _state.value = RewardedAdState.IDLE
                    Log.d("RewardedAdManager", "Ad dismissed full screen content")
                    if (rewardGranted) {
                        onRewardEarned()
                    } else {
                        onDismissed()
                    }
                    preloadAd()
                }
            }

            ad?.show(activity) { rewardItem ->
                rewardGranted = true
                Log.d("RewardedAdManager", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RewardedAdManager? = null

        fun getInstance(context: Context): RewardedAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RewardedAdManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
