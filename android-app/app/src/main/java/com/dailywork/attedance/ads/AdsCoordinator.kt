package com.dailywork.attedance.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import com.dailywork.attedance.BuildConfig

enum class AdsInitState {
    NotStarted,
    GatheringConsent,
    ConsentUnavailable,
    Initializing,
    Ready,
    Failed
}

class AdsCoordinator private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(appContext)

    private val _initState = MutableStateFlow(AdsInitState.NotStarted)
    val initState: StateFlow<AdsInitState> = _initState

    private val isInitializing = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)

    fun initialize(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        if (isInitialized.get()) {
            onComplete(true)
            return
        }
        if (isInitializing.compareAndSet(false, true)) {
            _initState.value = AdsInitState.GatheringConsent
            val startTime = System.currentTimeMillis()

            val paramsBuilder = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)

            if (BuildConfig.ADS_TEST_MODE && BuildConfig.ADMOB_TEST_DEVICE_IDS.isNotBlank()) {
                val debugSettingsBuilder = ConsentDebugSettings.Builder(appContext)
                BuildConfig.ADMOB_TEST_DEVICE_IDS.split(",").forEach {
                    debugSettingsBuilder.addTestDeviceHashedId(it.trim())
                }
                paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())
            }

            val params = paramsBuilder.build()

            activity.runOnUiThread {
                try {
                    consentInformation.requestConsentInfoUpdate(
                        activity,
                        params,
                        {
                            UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                activity
                            ) { formError ->
                                if (formError != null) {
                                    Log.w("AdsCoordinator", "Consent form show failed: ${formError.message}")
                                }
                                checkAndInitMobileAds(startTime, onComplete)
                            }
                        },
                        { requestConsentError ->
                            Log.w("AdsCoordinator", "Consent info update failed: ${requestConsentError.message}")
                            // Fallback to check if we can already request ads (deadlock prevention)
                            checkAndInitMobileAds(startTime, onComplete)
                        }
                    )
                } catch (e: Exception) {
                    Log.e("AdsCoordinator", "Error during consent gathering", e)
                    checkAndInitMobileAds(startTime, onComplete)
                }
            }
        } else {
            // Already initializing, wait or invoke callback based on current flow
            onComplete(isInitialized.get())
        }
    }

    private fun checkAndInitMobileAds(startTime: Long, onComplete: (Boolean) -> Unit) {
        if (consentInformation.canRequestAds()) {
            _initState.value = AdsInitState.Initializing
            try {
                MobileAds.initialize(appContext) { status ->
                    val duration = System.currentTimeMillis() - startTime
                    isInitialized.set(true)
                    isInitializing.set(false)
                    _initState.value = AdsInitState.Ready
                    Log.d("AdsCoordinator", "Mobile Ads SDK Initialized in ${duration}ms. Test Mode: ${BuildConfig.ADS_TEST_MODE}")
                    status.adapterStatusMap.forEach { (adapter, details) ->
                        Log.d("AdsCoordinator", "Adapter: $adapter, State: ${details.initializationState}")
                    }
                    onComplete(true)
                }
            } catch (e: Exception) {
                isInitializing.set(false)
                _initState.value = AdsInitState.Failed
                Log.e("AdsCoordinator", "Failed to initialize Mobile Ads SDK", e)
                onComplete(false)
            }
        } else {
            isInitializing.set(false)
            _initState.value = AdsInitState.ConsentUnavailable
            Log.w("AdsCoordinator", "Consent not available to request ads.")
            onComplete(false)
        }
    }

    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    companion object {
        @Volatile
        private var INSTANCE: AdsCoordinator? = null

        fun getInstance(context: Context): AdsCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdsCoordinator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
