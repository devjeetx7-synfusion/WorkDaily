package com.dailywork.attedance.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.access.FeatureAccessController
import com.dailywork.attedance.access.RewardedFeature
import com.dailywork.attedance.ads.RewardedAdManager
import com.dailywork.attedance.ads.RewardedAdState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardedAccessBottomSheet(
    feature: RewardedFeature,
    suffix: String = "",
    onDismissRequest: () -> Unit,
    onAccessGranted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adManager = remember { RewardedAdManager.getInstance(context) }
    val accessController = remember { FeatureAccessController.getInstance(context) }

    val adState by adManager.state.collectAsState()

    val isWorkerLimit = feature == RewardedFeature.ADD_WORKER
    val title = if (isWorkerLimit) "Worker Limit Reached" else "Unlock this feature"
    val description = if (isWorkerLimit) {
        "You can currently manage up to 5 workers. Watch an ad to add one more worker."
    } else {
        "Watch a short ad to temporarily unlock this feature."
    }
    val buttonText = if (isWorkerLimit) "Watch Ad and Add Worker" else "Watch Ad to Unlock"

    var buttonLabel by remember { mutableStateOf(buttonText) }
    var isEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(adState) {
        when (adState) {
            RewardedAdState.LOADING -> {
                buttonLabel = "Loading ad..."
                isEnabled = false
            }
            RewardedAdState.READY -> {
                buttonLabel = buttonText
                isEnabled = true
            }
            RewardedAdState.SHOWING -> {
                buttonLabel = "Ad showing..."
                isEnabled = false
            }
            RewardedAdState.FAILED -> {
                buttonLabel = "Ad unavailable"
                isEnabled = true
            }
            RewardedAdState.IDLE -> {
                buttonLabel = buttonText
                isEnabled = true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (adState == RewardedAdState.FAILED) {
                        if (accessController.fallbackUnlockEnabled && feature != RewardedFeature.ADD_WORKER) {
                            coroutineScope.launch {
                                accessController.grantAccess(feature, suffix)
                                buttonLabel = "Reward received"
                                onAccessGranted()
                                onDismissRequest()
                            }
                        } else {
                            adManager.preloadAd()
                        }
                    } else if (adState == RewardedAdState.READY) {
                        val activity = context as? Activity
                        if (activity != null) {
                            adManager.showAd(
                                activity = activity,
                                onRewardEarned = {
                                    coroutineScope.launch {
                                        accessController.grantAccess(feature, suffix)
                                        buttonLabel = "Reward received"
                                        onAccessGranted()
                                        onDismissRequest()
                                    }
                                },
                                onDismissed = {
                                    onDismissRequest()
                                },
                                onFailedToShow = {
                                    if (accessController.fallbackUnlockEnabled && feature != RewardedFeature.ADD_WORKER) {
                                        coroutineScope.launch {
                                            accessController.grantAccess(feature, suffix)
                                            buttonLabel = "Reward received"
                                            onAccessGranted()
                                            onDismissRequest()
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        adManager.preloadAd()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isEnabled || adState == RewardedAdState.FAILED
            ) {
                Text(
                    text = if (adState == RewardedAdState.FAILED) "Ad unavailable (Retry)" else buttonLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Not Now",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
