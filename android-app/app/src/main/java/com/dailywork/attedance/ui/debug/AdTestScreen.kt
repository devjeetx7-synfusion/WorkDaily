package com.dailywork.attedance.ui.debug

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button as AndroidButton
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dailywork.attedance.ads.AdsCoordinator
import com.dailywork.attedance.ads.AdsInitState
import com.dailywork.attedance.viewmodel.AdTestViewModel
import com.dailywork.attedance.utils.WebViewDiagnostics
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

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun BannerAdView(
    adUnitId: String,
    adSize: AdSize,
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    val context = LocalContext.current
    val adView = remember(adUnitId, adSize) {
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(adSize)
            this.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    onAdLoaded()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    onAdFailedToLoad(error)
                }
            }
        }
    }

    LaunchedEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
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
    val activity = context.findActivity()

    // Centralized AdsCoordinator
    val adsCoordinator = remember { AdsCoordinator.getInstance(context) }
    val adsInitState by adsCoordinator.initState.collectAsState()

    // Full screen ad coordination guard
    val isFullScreenAdShowing by adTestViewModel.isFullScreenAdShowing.collectAsState()

    // Selected ad format to render at one time (performance optimization)
    var selectedFormatTab by remember { mutableStateOf("Banner") }

    // Diagnostics list
    val diagnosticsInfo = remember { WebViewDiagnostics.getDiagnosticsInfo(context) }

    // Banner ad test states
    var bannerStatus by remember { mutableStateOf("Not Loaded") }
    var fixedBannerStatus by remember { mutableStateOf("Not Loaded") }

    // Native ad test states
    var nativeAdObj by remember { mutableStateOf<NativeAd?>(null) }
    var nativeAdStatus by remember { mutableStateOf("Not Loaded") }

    var nativeVideoAdObj by remember { mutableStateOf<NativeAd?>(null) }
    var nativeVideoAdStatus by remember { mutableStateOf("Not Loaded") }

    // Ensure we release native ads on dispose
    DisposableEffect(Unit) {
        onDispose {
            nativeAdObj?.destroy()
            nativeVideoAdObj?.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ad Test Center", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warn Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Test Mode",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "TEST ADS ACTIVE",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 16.sp
                            )
                            Text(
                                "Using Google demo publisher credentials on ${BuildConfig.BUILD_TYPE} build.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Centralized SDK Control Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mobile Ads SDK Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Consent Status: ${diagnosticsInfo["Consent Status"]}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Coordinator State: $adsInitState", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (activity != null) {
                                    adsCoordinator.initialize(activity) { success ->
                                        Toast.makeText(context, "SDK Init Successful: $success", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = adsInitState != AdsInitState.Ready && adsInitState != AdsInitState.Initializing
                        ) {
                            Text(if (adsInitState == AdsInitState.Ready) "Mobile Ads SDK Ready" else "Initialize Mobile Ads SDK")
                        }
                    }
                }
            }

            // WebView & Connection Diagnostics Block
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("WebView & Play Services Diagnostics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        diagnosticsInfo.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(key, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
                                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
                            }
                        }
                    }
                }
            }

            // Tabs to avoid rendering everything together and causing massive lag
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("Banner", "FullScreen", "Native")
                    tabs.forEach { tab ->
                        Button(
                            onClick = { selectedFormatTab = tab },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFormatTab == tab) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedFormatTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            elevation = null
                        ) {
                            Text(tab, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab-specific ad content
            when (selectedFormatTab) {
                "Banner" -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Adaptive Banner Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Status: $bannerStatus", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp, max = 100.dp)
                                        .background(Color.LightGray.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BannerAdView(
                                        adUnitId = AdConfiguration.adaptiveBannerAdUnitId,
                                        adSize = AdSize.BANNER,
                                        onAdLoaded = { bannerStatus = "Loaded successfully" },
                                        onAdFailedToLoad = { err -> bannerStatus = "Failed: ${err.message} (${err.code})" }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Fixed Banner Ad (320x50)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Status: $fixedBannerStatus", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .background(Color.LightGray.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BannerAdView(
                                        adUnitId = AdConfiguration.fixedBannerAdUnitId,
                                        adSize = AdSize.BANNER,
                                        onAdLoaded = { fixedBannerStatus = "Loaded successfully" },
                                        onAdFailedToLoad = { err -> fixedBannerStatus = "Failed: ${err.message} (${err.code})" }
                                    )
                                }
                            }
                        }
                    }
                }

                "FullScreen" -> {
                    // Interstitial
                    item {
                        val interstitialStatus by adTestViewModel.interstitialStatus.collectAsState()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Interstitial Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Status: $interstitialStatus", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            adTestViewModel.setInterstitialStatus("Loading...")
                                            InterstitialAd.load(
                                                context,
                                                AdConfiguration.interstitialAdUnitId,
                                                AdRequest.Builder().build(),
                                                object : InterstitialAdLoadCallback() {
                                                    override fun onAdLoaded(ad: InterstitialAd) {
                                                        adTestViewModel.interstitialAd = ad
                                                        adTestViewModel.setInterstitialStatus("Ready")
                                                    }
                                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                                        adTestViewModel.interstitialAd = null
                                                        adTestViewModel.setInterstitialStatus("Failed: ${error.message} (${error.code})")
                                                    }
                                                }
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Load Ad")
                                    }

                                    Button(
                                        onClick = {
                                            if (activity != null && adTestViewModel.interstitialAd != null) {
                                                adTestViewModel.setFullScreenAdShowing(true)
                                                adTestViewModel.interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                                    override fun onAdDismissedFullScreenContent() {
                                                        adTestViewModel.setFullScreenAdShowing(false)
                                                        adTestViewModel.interstitialAd = null
                                                        adTestViewModel.setInterstitialStatus("Dismissed")
                                                    }
                                                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                                        adTestViewModel.setFullScreenAdShowing(false)
                                                        adTestViewModel.interstitialAd = null
                                                        adTestViewModel.setInterstitialStatus("Failed to show: ${error.message}")
                                                    }
                                                }
                                                adTestViewModel.interstitialAd?.show(activity)
                                            } else {
                                                Toast.makeText(context, "Ad not ready", Toast.LENGTH_SHORT).show()
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
                    }

                    // Rewarded
                    item {
                        val rewardedStatus by adTestViewModel.rewardedStatus.collectAsState()
                        val rewardedResult by adTestViewModel.rewardedResult.collectAsState()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Rewarded Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Status: $rewardedStatus", fontSize = 13.sp, color = Color.Gray)
                                if (rewardedResult.isNotEmpty()) {
                                    Text("Reward: $rewardedResult", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            adTestViewModel.setRewardedStatus("Loading...")
                                            RewardedAd.load(
                                                context,
                                                AdConfiguration.rewardedAdUnitId,
                                                AdRequest.Builder().build(),
                                                object : RewardedAdLoadCallback() {
                                                    override fun onAdLoaded(ad: RewardedAd) {
                                                        adTestViewModel.rewardedAd = ad
                                                        adTestViewModel.setRewardedStatus("Ready")
                                                    }
                                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                                        adTestViewModel.rewardedAd = null
                                                        adTestViewModel.setRewardedStatus("Failed: ${error.message} (${error.code})")
                                                    }
                                                }
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Load Ad")
                                    }

                                    Button(
                                        onClick = {
                                            if (activity != null && adTestViewModel.rewardedAd != null) {
                                                adTestViewModel.setFullScreenAdShowing(true)
                                                var rewarded = false
                                                adTestViewModel.rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                                    override fun onAdDismissedFullScreenContent() {
                                                        adTestViewModel.setFullScreenAdShowing(false)
                                                        adTestViewModel.rewardedAd = null
                                                        adTestViewModel.setRewardedStatus(if (rewarded) "Completed" else "Dismissed without completion")
                                                    }
                                                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                                        adTestViewModel.setFullScreenAdShowing(false)
                                                        adTestViewModel.rewardedAd = null
                                                        adTestViewModel.setRewardedStatus("Failed to show: ${error.message}")
                                                    }
                                                }
                                                adTestViewModel.rewardedAd?.show(activity) { reward ->
                                                    rewarded = true
                                                    adTestViewModel.setRewardedResult("${reward.amount} ${reward.type}")
                                                }
                                            } else {
                                                Toast.makeText(context, "Ad not ready", Toast.LENGTH_SHORT).show()
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
                    }

                    // App Open
                    item {
                        val appOpenStatus by adTestViewModel.appOpenStatus.collectAsState()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("App Open Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Status: $appOpenStatus", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            adTestViewModel.setAppOpenStatus("Loading...")
                                            AppOpenAd.load(
                                                context,
                                                AdConfiguration.appOpenAdUnitId,
                                                AdRequest.Builder().build(),
                                                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                                                object : AppOpenAd.AppOpenAdLoadCallback() {
                                                    override fun onAdLoaded(ad: AppOpenAd) {
                                                        adTestViewModel.appOpenAd = ad
                                                        adTestViewModel.setAppOpenStatus("Ready")
                                                    }
                                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                                        adTestViewModel.appOpenAd = null
                                                        adTestViewModel.setAppOpenStatus("Failed: ${error.message} (${error.code})")
                                                    }
                                                }
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Load Ad")
                                    }

                                    Button(
                                        onClick = {
                                            if (activity != null && adTestViewModel.appOpenAd != null) {
                                                adTestViewModel.setFullScreenAdShowing(true)
                                                adTestViewModel.appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                                    override fun onAdDismissedFullScreenContent() {
                                                        adTestViewModel.setFullScreenAdShowing(false)
                                                        adTestViewModel.appOpenAd = null
                                                        adTestViewModel.setAppOpenStatus("Dismissed")
                                                    }
                                                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                                        adTestViewModel.setFullScreenAdShowing(false)
                                                        adTestViewModel.appOpenAd = null
                                                        adTestViewModel.setAppOpenStatus("Failed to show: ${error.message}")
                                                    }
                                                }
                                                adTestViewModel.appOpenAd?.show(activity)
                                            } else {
                                                Toast.makeText(context, "Ad not ready", Toast.LENGTH_SHORT).show()
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
                    }
                }

                "Native" -> {
                    // Standard Native Ad
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Native Content Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Status: $nativeAdStatus", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        nativeAdStatus = "Loading..."
                                        val adLoader = AdLoader.Builder(context, AdConfiguration.nativeAdUnitId)
                                            .forNativeAd { nativeAd ->
                                                nativeAdObj?.destroy() // release old
                                                nativeAdObj = nativeAd
                                                nativeAdStatus = "Ready"
                                            }
                                            .withAdListener(object : AdListener() {
                                                override fun onAdFailedToLoad(error: LoadAdError) {
                                                    nativeAdStatus = "Failed: ${error.message} (${error.code})"
                                                }
                                            })
                                            .build()
                                        adLoader.loadAd(AdRequest.Builder().build())
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Load Native Ad")
                                }

                                nativeAdObj?.let { ad ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    AndroidView(
                                        factory = { ctx ->
                                            val nativeAdView = NativeAdView(ctx)
                                            val container = LinearLayout(ctx).apply {
                                                orientation = LinearLayout.VERTICAL
                                                setPadding(16, 16, 16, 16)
                                                // Clean, non-obsolete background
                                                background = ctx.getDrawable(android.R.drawable.dialog_holo_dark_frame)
                                            }

                                            val headlineView = TextView(ctx).apply {
                                                textSize = 16f
                                                setTypeface(null, android.graphics.Typeface.BOLD)
                                                setTextColor(android.graphics.Color.WHITE)
                                                text = ad.headline
                                            }
                                            container.addView(headlineView)
                                            nativeAdView.headlineView = headlineView

                                            val bodyView = TextView(ctx).apply {
                                                textSize = 14f
                                                setTextColor(android.graphics.Color.LTGRAY)
                                                text = ad.body
                                            }
                                            container.addView(bodyView)
                                            nativeAdView.bodyView = bodyView

                                            nativeAdView.addView(container)
                                            nativeAdView.setNativeAd(ad)
                                            nativeAdView
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Native Video Ad
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Native Video Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Status: $nativeVideoAdStatus", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        nativeVideoAdStatus = "Loading..."
                                        val adLoader = AdLoader.Builder(context, AdConfiguration.nativeVideoAdUnitId)
                                            .forNativeAd { nativeAd ->
                                                nativeVideoAdObj?.destroy() // release old
                                                nativeVideoAdObj = nativeAd
                                                nativeVideoAdStatus = "Ready"
                                            }
                                            .withAdListener(object : AdListener() {
                                                override fun onAdFailedToLoad(error: LoadAdError) {
                                                    nativeVideoAdStatus = "Failed: ${error.message} (${error.code})"
                                                }
                                            })
                                            .build()
                                        adLoader.loadAd(AdRequest.Builder().build())
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Load Native Video Ad")
                                }

                                nativeVideoAdObj?.let { ad ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    AndroidView(
                                        factory = { ctx ->
                                            val nativeAdView = NativeAdView(ctx)
                                            val container = LinearLayout(ctx).apply {
                                                orientation = LinearLayout.VERTICAL
                                                setPadding(16, 16, 16, 16)
                                                background = ctx.getDrawable(android.R.drawable.dialog_holo_dark_frame)
                                            }

                                            val headlineView = TextView(ctx).apply {
                                                textSize = 16f
                                                setTypeface(null, android.graphics.Typeface.BOLD)
                                                setTextColor(android.graphics.Color.WHITE)
                                                text = ad.headline
                                            }
                                            container.addView(headlineView)
                                            nativeAdView.headlineView = headlineView

                                            val mediaView = MediaView(ctx).apply {
                                                layoutParams = LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    450
                                                ).apply {
                                                    topMargin = 8
                                                }
                                            }
                                            container.addView(mediaView)
                                            nativeAdView.mediaView = mediaView

                                            nativeAdView.addView(container)
                                            nativeAdView.setNativeAd(ad)
                                            nativeAdView
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
