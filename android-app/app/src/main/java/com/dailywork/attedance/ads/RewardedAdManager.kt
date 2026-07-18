package com.dailywork.attedance.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class RewardedAdState {
    IDLE, LOADING, READY, SHOWING, FAILED, WAITING_FOR_CONSENT, INITIALIZING, TEMPORARILY_UNAVAILABLE
}

class RewardedAdManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val adsCoordinator = AdsCoordinator.getInstance(appContext)

    private val _state = MutableStateFlow(RewardedAdState.IDLE)
    val state: StateFlow<RewardedAdState> = _state

    private var rewardedAd: RewardedAd? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Bounded exponential backoff parameters
    private var retryCount = 0
    private val maxRetries = 5
    private val baseDelayMs = 2000L
    private val maxDelayMs = 30000L
    private var isRetryPending = false

    // Circuit breaker for repeated identical failures
    private var lastErrorCode: Int? = null
    private var identicalFailureCount = 0
    private val circuitBreakerThreshold = 3
    private var isCircuitBreakerTripped = false

    private val adUnitId: String
        get() {
            val unitId = AdConfiguration.rewardedAdUnitId
            if (unitId.isBlank()) {
                Log.e("RewardedAdManager", "Missing AdMob Rewarded Ad Unit ID!")
            }
            return unitId
        }

    fun initialize(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        _state.value = RewardedAdState.INITIALIZING
        adsCoordinator.initialize(activity) { success ->
            if (success) {
                _state.value = RewardedAdState.IDLE
                preloadAd()
                onComplete(true)
            } else {
                _state.value = RewardedAdState.FAILED
                onComplete(false)
            }
        }
    }

    // Retained for compatibility with code that calls initialize without an Activity
    fun initialize(onComplete: () -> Unit = {}) {
        // Since we can't show UMP without Activity, we delegate to coordinator if already ready,
        // otherwise wait until an Activity calls it.
        if (adsCoordinator.initState.value == AdsInitState.Ready) {
            preloadAd()
            onComplete()
        } else {
            // Safe fallback initialization attempt
            Log.d("RewardedAdManager", "initialize() called without Activity; waiting for active screen initialization.")
            onComplete()
        }
    }

    fun preloadAd() {
        if (isCircuitBreakerTripped) {
            Log.w("RewardedAdManager", "Circuit breaker active. Manual action or device fix required.")
            _state.value = RewardedAdState.TEMPORARILY_UNAVAILABLE
            return
        }

        if (adsCoordinator.initState.value != AdsInitState.Ready) {
            Log.w("RewardedAdManager", "Cannot preload, AdsCoordinator not ready yet.")
            _state.value = RewardedAdState.INITIALIZING
            return
        }

        if (!adsCoordinator.canRequestAds()) {
            Log.w("RewardedAdManager", "Cannot preload, consent not available to request ads.")
            _state.value = RewardedAdState.WAITING_FOR_CONSENT
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
            RewardedAd.load(appContext, adId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _state.value = RewardedAdState.READY
                    retryCount = 0
                    isRetryPending = false
                    lastErrorCode = null
                    identicalFailureCount = 0
                    isCircuitBreakerTripped = false
                    Log.d("RewardedAdManager", "Rewarded Ad Loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e("RewardedAdManager", "Failed to load rewarded ad: ${error.message} (code: ${error.code}, domain: ${error.domain})")

                    // Update identical failure tracking
                    if (lastErrorCode == error.code) {
                        identicalFailureCount++
                    } else {
                        lastErrorCode = error.code
                        identicalFailureCount = 1
                    }

                    if (identicalFailureCount >= circuitBreakerThreshold) {
                        isCircuitBreakerTripped = true
                        _state.value = RewardedAdState.TEMPORARILY_UNAVAILABLE
                        Log.e("RewardedAdManager", "Circuit breaker tripped due to $identicalFailureCount repeated identical failures.")
                        return
                    }

                    _state.value = RewardedAdState.FAILED
                    scheduleRetry()
                }
            })
        }
    }

    private fun scheduleRetry() {
        if (isRetryPending || retryCount >= maxRetries) {
            if (retryCount >= maxRetries) {
                _state.value = RewardedAdState.TEMPORARILY_UNAVAILABLE
            }
            return
        }
        isRetryPending = true
        val delay = (baseDelayMs * Math.pow(2.0, retryCount.toDouble()).toLong()).coerceAtMost(maxDelayMs)
        retryCount++
        Log.d("RewardedAdManager", "Scheduling ad preload retry #$retryCount in ${delay}ms")
        mainHandler.postDelayed({
            isRetryPending = false
            preloadAd()
        }, delay)
    }

    fun resetCircuitBreaker() {
        isCircuitBreakerTripped = false
        retryCount = 0
        identicalFailureCount = 0
        lastErrorCode = null
        _state.value = RewardedAdState.IDLE
        preloadAd()
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit, onDismissed: () -> Unit, onFailedToShow: (String) -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.e("RewardedAdManager", "Cannot show ad: Activity is finishing or destroyed.")
            onFailedToShow("Activity unavailable")
            return
        }

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

            val rewardGranted = AtomicBoolean(false)

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
                    if (rewardGranted.get()) {
                        onRewardEarned()
                    } else {
                        onDismissed()
                    }
                    preloadAd()
                }
            }

            ad?.show(activity) { rewardItem ->
                rewardGranted.set(true)
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
