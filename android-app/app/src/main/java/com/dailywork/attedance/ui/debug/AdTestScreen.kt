package com.dailywork.attedance.ui.debug

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button as AndroidButton
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailywork.attedance.BuildConfig
import com.dailywork.attedance.ads.AdConfiguration
import com.dailywork.attedance.viewmodel.AdTestViewModel
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform

@Composable
fun BannerAdView(
    adUnitId: String,
    adSize: AdSize,
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(adSize)
            this.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    onAdLoaded()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onAdFailedToLoad(error)
                }
                override fun onAdOpened() { Log.d("BannerAdView", "Banner Ad Opened") }
                override fun onAdClosed() { Log.d("BannerAdView", "Banner Ad Closed") }
                override fun onAdImpression() { Log.d("BannerAdView", "Banner Ad Impression Recorded") }
                override fun onAdClicked() { Log.d("BannerAdView", "Banner Ad Clicked") }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> adView.resume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> adView.pause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdTestScreen(
    adTestViewModel: AdTestViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Full screen ad coordination guard
    val isFullScreenAdShowing by adTestViewModel.isFullScreenAdShowing.collectAsState()

    // Consent Tracking State
    val consentInformation = remember { UserMessagingPlatform.getConsentInformation(context) }
    var consentStatusText by remember { mutableStateOf("") }
    var canRequestAdsText by remember { mutableStateOf("") }
    var privacyOptionsText by remember { mutableStateOf("") }
    var adsInitStatusText by remember { mutableStateOf("Initializing...") }

    fun refreshConsentStatus() {
        val status = when (consentInformation.consentStatus) {
            ConsentInformation.ConsentStatus.UNKNOWN -> "Unknown"
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "Not Required"
            ConsentInformation.ConsentStatus.REQUIRED -> "Required"
            ConsentInformation.ConsentStatus.OBTAINED -> "Obtained"
            else -> "Unknown"
        }
        consentStatusText = status
        canRequestAdsText = consentInformation.canRequestAds().toString()
        privacyOptionsText = (consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED).toString()
    }

    LaunchedEffect(Unit) {
        refreshConsentStatus()
        // Check Ads initialization
        try {
            MobileAds.initialize(context) { initializationStatus ->
                val adapterStatusMap = initializationStatus.adapterStatusMap
                val statusString = adapterStatusMap.entries.joinToString { "${it.key}: ${it.value.initializationState}" }
                adsInitStatusText = if (statusString.isEmpty()) "Initialized" else statusString
                Log.d("AdTestCenter", "MobileAds initialized: $statusString")

                // Apply Optional debug test-device IDs if configured
                if (BuildConfig.ADS_TEST_MODE && BuildConfig.ADMOB_TEST_DEVICE_IDS.isNotBlank()) {
                    val devices = BuildConfig.ADMOB_TEST_DEVICE_IDS.split(",").map { it.trim() }
                    val requestConfiguration = MobileAds.getRequestConfiguration().toBuilder()
                        .setTestDeviceIds(devices)
                        .build()
                    MobileAds.setRequestConfiguration(requestConfiguration)
                    Log.d("AdTestCenter", "Applied Test Device IDs: $devices")
                }
            }
        } catch (e: Exception) {
            adsInitStatusText = "Failed: ${e.message}"
            Log.e("AdTestCenter", "MobileAds initialization failed", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ad Test Center — Debug", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (BuildConfig.ADS_TEST_MODE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TEST ADS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Build type: ${BuildConfig.BUILD_TYPE}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Google demo ad units are active",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Consent & Environment Status Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Environment Configuration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Test Mode Active: ${BuildConfig.ADS_TEST_MODE}", fontSize = 13.sp)
                    Text("AdMob App ID: ca-app-pub-3940256099942544~3347511713", fontSize = 13.sp)
                    HorizontalDivider()
                    Text("Consent Status: $consentStatusText", fontSize = 13.sp)
                    Text("Can Request Ads: $canRequestAdsText", fontSize = 13.sp)
                    Text("Privacy Options Required: $privacyOptionsText", fontSize = 13.sp)
                    Text("Mobile Ads SDK Init Status: $adsInitStatusText", fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                consentInformation.reset()
                                refreshConsentStatus()
                                Toast.makeText(context, "Consent state reset. Please restart application or reload to trigger UMP.", Toast.LENGTH_LONG).show()
                                Log.d("AdTestCenter", "Consent state reset by developer")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Consent", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                Log.d("AdTestCenter", "Opening Ad Inspector requested")
                                MobileAds.openAdInspector(context) { error ->
                                    if (error != null) {
                                        Log.e("AdTestCenter", "Ad Inspector failed to open: ${error.message} (code: ${error.code})")
                                        Toast.makeText(context, "Failed to open Ad Inspector: ${error.message}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Log.d("AdTestCenter", "Ad Inspector opened successfully")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ad Inspector", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Format 1: Anchored Adaptive Banner
            Text("1. Anchored Adaptive Banner", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Anchored Adaptive Banner — Test Ad", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val density = LocalDensity.current
                        val availableWidth = with(density) { maxWidth.value.toInt() }
                        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, availableWidth)
                        BannerAdView(
                            adUnitId = AdConfiguration.adaptiveBannerAdUnitId,
                            adSize = adSize,
                            onAdLoaded = { Log.d("AdTestCenter", "Anchored Adaptive Banner Loaded") },
                            onAdFailedToLoad = { err -> Log.e("AdTestCenter", "Anchored Adaptive Banner Failed to load: ${err.message}") }
                        )
                    }
                }
            }

            // Format 2: Inline Adaptive Banner
            Text("2. Inline Adaptive Banner", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Inline Adaptive Banner — Test Ad", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    BannerAdView(
                        adUnitId = AdConfiguration.adaptiveBannerAdUnitId,
                        adSize = AdSize.MEDIUM_RECTANGLE,
                        onAdLoaded = { Log.d("AdTestCenter", "Inline Adaptive Banner Loaded") },
                        onAdFailedToLoad = { err -> Log.e("AdTestCenter", "Inline Adaptive Banner Failed to load: ${err.message}") }
                    )
                }
            }

            // Format 3: Fixed-size Banner
            Text("3. Fixed-size Banner", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fixed Banner (320x50) — Test Ad", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    BannerAdView(
                        adUnitId = AdConfiguration.fixedBannerAdUnitId,
                        adSize = AdSize.BANNER,
                        onAdLoaded = { Log.d("AdTestCenter", "Fixed Banner Loaded") },
                        onAdFailedToLoad = { err -> Log.e("AdTestCenter", "Fixed Banner Failed to load: ${err.message}") }
                    )
                }
            }

            // Format 4: Interstitial Ad
            val interstitialStatus by adTestViewModel.interstitialStatus.collectAsState()
            Text("4. Interstitial Ad", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status: $interstitialStatus", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                adTestViewModel.setInterstitialStatus("Loading")
                                Log.d("AdTestCenter", "Requesting Interstitial Ad")
                                val adRequest = AdRequest.Builder().build()
                                InterstitialAd.load(context, AdConfiguration.interstitialAdUnitId, adRequest,
                                    object : InterstitialAdLoadCallback() {
                                        override fun onAdLoaded(ad: InterstitialAd) {
                                            adTestViewModel.interstitialAd = ad
                                            adTestViewModel.setInterstitialStatus("Ready")
                                            Log.d("AdTestCenter", "Interstitial Ad Loaded successfully")
                                        }
                                        override fun onAdFailedToLoad(error: LoadAdError) {
                                            adTestViewModel.interstitialAd = null
                                            adTestViewModel.setInterstitialStatus("Failed: ${error.message} (${error.code})")
                                            Log.e("AdTestCenter", "Failed to load Interstitial: ${error.message} (code: ${error.code})")
                                        }
                                    }
                                )
                            },
                            enabled = interstitialStatus != "Loading" && interstitialStatus != "Showing" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Ad")
                        }
                        Button(
                            onClick = {
                                val ad = adTestViewModel.interstitialAd
                                if (ad != null && !isFullScreenAdShowing) {
                                    adTestViewModel.setFullScreenAdShowing(true)
                                    adTestViewModel.setInterstitialStatus("Showing")
                                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                        override fun onAdShowedFullScreenContent() {
                                            Log.d("AdTestCenter", "Interstitial Ad Showed Full Screen")
                                        }
                                        override fun onAdFailedToShowFullScreenContent(err: AdError) {
                                            adTestViewModel.setFullScreenAdShowing(false)
                                            adTestViewModel.interstitialAd = null
                                            adTestViewModel.setInterstitialStatus("Failed to show: ${err.message}")
                                            Log.e("AdTestCenter", "Interstitial Ad failed to show: ${err.message}")
                                        }
                                        override fun onAdDismissedFullScreenContent() {
                                            adTestViewModel.setFullScreenAdShowing(false)
                                            adTestViewModel.interstitialAd = null
                                            adTestViewModel.setInterstitialStatus("Dismissed")
                                            Log.d("AdTestCenter", "Interstitial Ad Dismissed")
                                        }
                                        override fun onAdImpression() { Log.d("AdTestCenter", "Interstitial Ad Impression Recorded") }
                                        override fun onAdClicked() { Log.d("AdTestCenter", "Interstitial Ad Clicked") }
                                    }
                                    ad.show(context as Activity)
                                }
                            },
                            enabled = interstitialStatus == "Ready" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Ad")
                        }
                    }
                }
            }

            // Format 5: Rewarded Ad
            val rewardedStatus by adTestViewModel.rewardedStatus.collectAsState()
            val rewardedResult by adTestViewModel.rewardedResult.collectAsState()
            Text("5. Rewarded Ad", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status: $rewardedStatus", fontWeight = FontWeight.SemiBold)
                    if (rewardedResult.isNotEmpty()) {
                        Text(rewardedResult, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                adTestViewModel.setRewardedStatus("Loading")
                                adTestViewModel.setRewardedResult("")
                                Log.d("AdTestCenter", "Requesting Rewarded Ad")
                                val adRequest = AdRequest.Builder().build()
                                RewardedAd.load(context, AdConfiguration.rewardedAdUnitId, adRequest,
                                    object : RewardedAdLoadCallback() {
                                        override fun onAdLoaded(ad: RewardedAd) {
                                            adTestViewModel.rewardedAd = ad
                                            adTestViewModel.setRewardedStatus("Ready")
                                            Log.d("AdTestCenter", "Rewarded Ad Loaded successfully")
                                        }
                                        override fun onAdFailedToLoad(error: LoadAdError) {
                                            adTestViewModel.rewardedAd = null
                                            adTestViewModel.setRewardedStatus("Failed: ${error.message} (${error.code})")
                                            Log.e("AdTestCenter", "Failed to load Rewarded: ${error.message} (code: ${error.code})")
                                        }
                                    }
                                )
                            },
                            enabled = rewardedStatus != "Loading" && rewardedStatus != "Showing" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Ad")
                        }
                        Button(
                            onClick = {
                                val ad = adTestViewModel.rewardedAd
                                if (ad != null && !isFullScreenAdShowing) {
                                    adTestViewModel.setFullScreenAdShowing(true)
                                    adTestViewModel.setRewardedStatus("Showing")
                                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                        override fun onAdShowedFullScreenContent() {
                                            Log.d("AdTestCenter", "Rewarded Ad Showed Full Screen")
                                        }
                                        override fun onAdFailedToShowFullScreenContent(err: AdError) {
                                            adTestViewModel.setFullScreenAdShowing(false)
                                            adTestViewModel.rewardedAd = null
                                            adTestViewModel.setRewardedStatus("Failed to show: ${err.message}")
                                            Log.e("AdTestCenter", "Rewarded Ad failed to show: ${err.message}")
                                        }
                                        override fun onAdDismissedFullScreenContent() {
                                            adTestViewModel.setFullScreenAdShowing(false)
                                            adTestViewModel.rewardedAd = null
                                            adTestViewModel.setRewardedStatus("Dismissed")
                                            Log.d("AdTestCenter", "Rewarded Ad Dismissed")
                                        }
                                        override fun onAdImpression() { Log.d("AdTestCenter", "Rewarded Ad Impression Recorded") }
                                        override fun onAdClicked() { Log.d("AdTestCenter", "Rewarded Ad Clicked") }
                                    }
                                    ad.show(context as Activity) { reward ->
                                        adTestViewModel.setRewardedResult("Reward earned\nType: ${reward.type}\nAmount: ${reward.amount}")
                                        adTestViewModel.setRewardedStatus("Reward Earned")
                                        Log.d("AdTestCenter", "Rewarded Ad completed reward. Type: ${reward.type}, Amount: ${reward.amount}")
                                    }
                                }
                            },
                            enabled = rewardedStatus == "Ready" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Ad")
                        }
                    }
                }
            }

            // Format 6: Rewarded Interstitial Ad
            val rewardedInterstitialStatus by adTestViewModel.rewardedInterstitialStatus.collectAsState()
            val rewardedInterstitialResult by adTestViewModel.rewardedInterstitialResult.collectAsState()
            var showRewardedInterstitialIntro by remember { mutableStateOf(false) }
            Text("6. Rewarded Interstitial Ad", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status: $rewardedInterstitialStatus", fontWeight = FontWeight.SemiBold)
                    if (rewardedInterstitialResult.isNotEmpty()) {
                        Text(rewardedInterstitialResult, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                adTestViewModel.setRewardedInterstitialStatus("Loading")
                                adTestViewModel.setRewardedInterstitialResult("")
                                Log.d("AdTestCenter", "Requesting Rewarded Interstitial Ad")
                                val adRequest = AdRequest.Builder().build()
                                RewardedInterstitialAd.load(context, AdConfiguration.rewardedInterstitialAdUnitId, adRequest,
                                    object : RewardedInterstitialAdLoadCallback() {
                                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                            adTestViewModel.rewardedInterstitialAd = ad
                                            adTestViewModel.setRewardedInterstitialStatus("Ready")
                                            Log.d("AdTestCenter", "Rewarded Interstitial Loaded successfully")
                                        }
                                        override fun onAdFailedToLoad(error: LoadAdError) {
                                            adTestViewModel.rewardedInterstitialAd = null
                                            adTestViewModel.setRewardedInterstitialStatus("Failed: ${error.message} (${error.code})")
                                            Log.e("AdTestCenter", "Failed to load Rewarded Interstitial: ${error.message} (code: ${error.code})")
                                        }
                                    }
                                )
                            },
                            enabled = rewardedInterstitialStatus != "Loading" && rewardedInterstitialStatus != "Showing" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Ad")
                        }
                        Button(
                            onClick = {
                                showRewardedInterstitialIntro = true
                            },
                            enabled = rewardedInterstitialStatus == "Ready" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Ad")
                        }
                    }
                }
            }

            if (showRewardedInterstitialIntro) {
                AlertDialog(
                    onDismissRequest = { showRewardedInterstitialIntro = false },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    title = { Text("Rewarded Interstitial Intro") },
                    text = {
                        Text("A test rewarded interstitial will be shown.\nYou may close it or complete it to test the reward callback.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRewardedInterstitialIntro = false
                                val ad = adTestViewModel.rewardedInterstitialAd
                                if (ad != null && !isFullScreenAdShowing) {
                                    adTestViewModel.setFullScreenAdShowing(true)
                                    adTestViewModel.setRewardedInterstitialStatus("Showing")
                                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                        override fun onAdShowedFullScreenContent() {
                                            Log.d("AdTestCenter", "Rewarded Interstitial Showed Full Screen")
                                        }
                                        override fun onAdFailedToShowFullScreenContent(err: AdError) {
                                            adTestViewModel.setFullScreenAdShowing(false)
                                            adTestViewModel.rewardedInterstitialAd = null
                                            adTestViewModel.setRewardedInterstitialStatus("Failed to show: ${err.message}")
                                            Log.e("AdTestCenter", "Rewarded Interstitial failed to show: ${err.message}")
                                        }
                                        override fun onAdDismissedFullScreenContent() {
                                            adTestViewModel.setFullScreenAdShowing(false)
                                            adTestViewModel.rewardedInterstitialAd = null
                                            adTestViewModel.setRewardedInterstitialStatus("Dismissed")
                                            Log.d("AdTestCenter", "Rewarded Interstitial Dismissed")
                                        }
                                        override fun onAdImpression() { Log.d("AdTestCenter", "Rewarded Interstitial Impression Recorded") }
                                        override fun onAdClicked() { Log.d("AdTestCenter", "Rewarded Interstitial Clicked") }
                                    }
                                    ad.show(context as Activity) { reward ->
                                        adTestViewModel.setRewardedInterstitialResult("Reward earned\nType: ${reward.type}\nAmount: ${reward.amount}")
                                        adTestViewModel.setRewardedInterstitialStatus("Reward Earned")
                                        Log.d("AdTestCenter", "Rewarded Interstitial completed reward. Type: ${reward.type}, Amount: ${reward.amount}")
                                    }
                                }
                            }
                        ) {
                            Text("Show Ad")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRewardedInterstitialIntro = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Format 7: Native Ad
            var nativeAdStateText by remember { mutableStateOf("Not Loaded") }
            var nativeAdInstance by remember { mutableStateOf<NativeAd?>(null) }
            Text("7. Native Ad", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status: $nativeAdStateText", fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = {
                            nativeAdStateText = "Loading"
                            Log.d("AdTestCenter", "Requesting Native Ad")
                            val adLoader = AdLoader.Builder(context, AdConfiguration.nativeAdUnitId)
                                .forNativeAd { ad ->
                                    val old = nativeAdInstance
                                    nativeAdInstance = ad
                                    old?.destroy()
                                    nativeAdStateText = "Ready"
                                    Log.d("AdTestCenter", "Native Ad loaded successfully")
                                }
                                .withAdListener(object : AdListener() {
                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        val old = nativeAdInstance
                                        nativeAdInstance = null
                                        old?.destroy()
                                        nativeAdStateText = "Failed: ${error.message} (${error.code})"
                                        Log.e("AdTestCenter", "Failed to load Native Ad: ${error.message} (code: ${error.code})")
                                    }
                                    override fun onAdOpened() { Log.d("AdTestCenter", "Native Ad Opened") }
                                    override fun onAdClosed() { Log.d("AdTestCenter", "Native Ad Closed") }
                                    override fun onAdImpression() { Log.d("AdTestCenter", "Native Ad Impression") }
                                    override fun onAdClicked() { Log.d("AdTestCenter", "Native Ad Clicked") }
                                })
                                .withNativeAdOptions(NativeAdOptions.Builder().build())
                                .build()
                            adLoader.loadAd(AdRequest.Builder().build())
                        },
                        enabled = nativeAdStateText != "Loading"
                    ) {
                        Text("Load Native Ad")
                    }

                    DisposableEffect(nativeAdInstance) {
                        onDispose {
                            nativeAdInstance?.destroy()
                        }
                    }

                    val ad = nativeAdInstance
                    if (ad != null) {
                        AndroidView(
                            factory = { ctx ->
                                NativeAdView(ctx).apply {
                                    val container = LinearLayout(ctx).apply {
                                        orientation = LinearLayout.VERTICAL
                                        setPadding(16, 16, 16, 16)
                                        background = ctx.getDrawable(android.R.drawable.dialog_holo_light_frame)
                                    }

                                    val adAttribution = TextView(ctx).apply {
                                        text = "Ad — Native test"
                                        setTextColor(android.graphics.Color.WHITE)
                                        setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Green
                                        setPadding(8, 4, 8, 4)
                                        textSize = 10f
                                    }
                                    container.addView(adAttribution)

                                    val headlineView = TextView(ctx).apply {
                                        textSize = 16f
                                        setTypeface(null, android.graphics.Typeface.BOLD)
                                        text = ad.headline
                                    }
                                    container.addView(headlineView)
                                    this.headlineView = headlineView

                                    if (ad.body != null) {
                                        val bodyView = TextView(ctx).apply {
                                            textSize = 14f
                                            text = ad.body
                                        }
                                        container.addView(bodyView)
                                        this.bodyView = bodyView
                                    }

                                    if (ad.icon != null) {
                                        val iconView = ImageView(ctx).apply {
                                            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                                                topMargin = 8
                                                bottomMargin = 8
                                            }
                                            setImageDrawable(ad.icon?.drawable)
                                        }
                                        container.addView(iconView)
                                        this.iconView = iconView
                                    }

                                    val mediaView = MediaView(ctx).apply {
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            450
                                        ).apply {
                                            topMargin = 8
                                            bottomMargin = 8
                                        }
                                    }
                                    container.addView(mediaView)
                                    this.mediaView = mediaView

                                    if (ad.advertiser != null) {
                                        val advertiserView = TextView(ctx).apply {
                                            textSize = 12f
                                            text = ad.advertiser
                                        }
                                        container.addView(advertiserView)
                                        this.advertiserView = advertiserView
                                    }

                                    if (ad.callToAction != null) {
                                        val callToActionView = AndroidButton(ctx).apply {
                                            layoutParams = LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                            ).apply {
                                                topMargin = 8
                                            }
                                            text = ad.callToAction
                                        }
                                        container.addView(callToActionView)
                                        this.callToActionView = callToActionView
                                    }

                                    addView(container)
                                    setNativeAd(ad)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )
                    }
                }
            }

            // Format 8: Native Video
            var nativeVideoStateText by remember { mutableStateOf("Not Loaded") }
            var nativeVideoAdInstance by remember { mutableStateOf<NativeAd?>(null) }
            var videoControllerStatusText by remember { mutableStateOf("No Video Active") }
            Text("8. Native Video", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status: $nativeVideoStateText", fontWeight = FontWeight.SemiBold)
                    Text("Video Status: $videoControllerStatusText", fontSize = 13.sp)
                    Button(
                        onClick = {
                            nativeVideoStateText = "Loading"
                            videoControllerStatusText = "Loading..."
                            Log.d("AdTestCenter", "Requesting Native Video Ad")
                            val adLoader = AdLoader.Builder(context, AdConfiguration.nativeVideoAdUnitId)
                                .forNativeAd { ad ->
                                    val old = nativeVideoAdInstance
                                    nativeVideoAdInstance = ad
                                    old?.destroy()
                                    nativeVideoStateText = "Ready"
                                    Log.d("AdTestCenter", "Native Video loaded successfully")

                                    val controller = ad.mediaContent?.videoController
                                    if (controller != null && controller.hasVideoContent()) {
                                        videoControllerStatusText = "Video Loaded — Muted: ${controller.isMuted}"
                                        controller.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                                            override fun onVideoStart() { videoControllerStatusText = "Video Started" }
                                            override fun onVideoPlay() { videoControllerStatusText = "Video Playing" }
                                            override fun onVideoPause() { videoControllerStatusText = "Video Paused" }
                                            override fun onVideoEnd() { videoControllerStatusText = "Video Ended" }
                                            override fun onVideoMute(muted: Boolean) { videoControllerStatusText = "Mute state changed: $muted" }
                                        }
                                    } else {
                                        videoControllerStatusText = "No video content returned by test unit."
                                    }
                                }
                                .withAdListener(object : AdListener() {
                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        val old = nativeVideoAdInstance
                                        nativeVideoAdInstance = null
                                        old?.destroy()
                                        nativeVideoStateText = "Failed: ${error.message} (${error.code})"
                                        videoControllerStatusText = "Load failed."
                                        Log.e("AdTestCenter", "Failed to load Native Video: ${error.message} (code: ${error.code})")
                                    }
                                })
                                .withNativeAdOptions(NativeAdOptions.Builder().build())
                                .build()
                            adLoader.loadAd(AdRequest.Builder().build())
                        },
                        enabled = nativeVideoStateText != "Loading"
                    ) {
                        Text("Load Native Video")
                    }

                    DisposableEffect(nativeVideoAdInstance) {
                        onDispose {
                            nativeVideoAdInstance?.destroy()
                        }
                    }

                    val ad = nativeVideoAdInstance
                    if (ad != null) {
                        AndroidView(
                            factory = { ctx ->
                                NativeAdView(ctx).apply {
                                    val container = LinearLayout(ctx).apply {
                                        orientation = LinearLayout.VERTICAL
                                        setPadding(16, 16, 16, 16)
                                        background = ctx.getDrawable(android.R.drawable.dialog_holo_light_frame)
                                    }

                                    val headlineView = TextView(ctx).apply {
                                        textSize = 16f
                                        setTypeface(null, android.graphics.Typeface.BOLD)
                                        text = ad.headline
                                    }
                                    container.addView(headlineView)
                                    this.headlineView = headlineView

                                    val mediaView = MediaView(ctx).apply {
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            450
                                        ).apply {
                                            topMargin = 8
                                            bottomMargin = 8
                                        }
                                    }
                                    container.addView(mediaView)
                                    this.mediaView = mediaView

                                    addView(container)
                                    setNativeAd(ad)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )
                    }
                }
            }

            // Format 9: App Open Ad
            val appOpenStatus by adTestViewModel.appOpenStatus.collectAsState()
            Text("9. App Open Ad", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status: $appOpenStatus", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                adTestViewModel.setAppOpenStatus("Loading")
                                Log.d("AdTestCenter", "Requesting App Open Ad")
                                val adRequest = AdRequest.Builder().build()
                                AppOpenAd.load(context, AdConfiguration.appOpenAdUnitId, adRequest,
                                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                                    object : AppOpenAd.AppOpenAdLoadCallback() {
                                        override fun onAdLoaded(ad: AppOpenAd) {
                                            adTestViewModel.appOpenAd = ad
                                            adTestViewModel.setAppOpenStatus("Ready")
                                            Log.d("AdTestCenter", "App Open Ad Loaded successfully")
                                        }
                                        override fun onAdFailedToLoad(error: LoadAdError) {
                                            adTestViewModel.appOpenAd = null
                                            adTestViewModel.setAppOpenStatus("Failed: ${error.message} (${error.code})")
                                            Log.e("AdTestCenter", "Failed to load App Open: ${error.message} (code: ${error.code})")
                                        }
                                    }
                                )
                            },
                            enabled = appOpenStatus != "Loading" && appOpenStatus != "Showing" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Ad")
                        }
                        Button(
                            onClick = {
                                val ad = adTestViewModel.appOpenAd
                                if (ad != null && !isFullScreenAdShowing) {
                                    val act = context as? Activity
                                    if (act != null && !act.isFinishing && !act.isDestroyed) {
                                        adTestViewModel.setFullScreenAdShowing(true)
                                        adTestViewModel.setAppOpenStatus("Showing")
                                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                            override fun onAdShowedFullScreenContent() {
                                                Log.d("AdTestCenter", "App Open Ad Showed Full Screen")
                                            }
                                            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                                                adTestViewModel.setFullScreenAdShowing(false)
                                                adTestViewModel.appOpenAd = null
                                                adTestViewModel.setAppOpenStatus("Failed to show: ${err.message}")
                                                Log.e("AdTestCenter", "App Open Ad failed to show: ${err.message}")
                                            }
                                            override fun onAdDismissedFullScreenContent() {
                                                adTestViewModel.setFullScreenAdShowing(false)
                                                adTestViewModel.appOpenAd = null
                                                adTestViewModel.setAppOpenStatus("Dismissed")
                                                Log.d("AdTestCenter", "App Open Ad Dismissed")
                                            }
                                            override fun onAdImpression() { Log.d("AdTestCenter", "App Open Ad Impression Recorded") }
                                            override fun onAdClicked() { Log.d("AdTestCenter", "App Open Ad Clicked") }
                                        }
                                        ad.show(act)
                                    }
                                }
                            },
                            enabled = appOpenStatus == "Ready" && !isFullScreenAdShowing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Ad")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
