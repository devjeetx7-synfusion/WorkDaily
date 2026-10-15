package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.RedemptionOption
import com.synfusion.attendance.rewards.data.RedemptionRecord
import com.synfusion.attendance.rewards.data.RedemptionStatus
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemRewardsScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateRedemptionHistory: () -> Unit
) {
    val walletSummary by viewModel.walletSummary.collectAsState()
    val redemptionOptions by viewModel.redemptionOptions.collectAsState()

    var selectedOptionForRedeem by remember { mutableStateOf<RedemptionOption?>(null) }
    var showConfirmationBottomSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf<RedemptionRecord?>(null) }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Redeem Rewards",
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = onNavigateRedemptionHistory) {
                        Text("History", color = RoyalPurple, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = RewardsTheme.screenBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RewardsPrimaryGradient)
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("AVAILABLE BALANCE", fontSize = 11.sp, color = HighlightLavender, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                RewardCoinBalance(amount = walletSummary.availableCoins, fontSize = 26, textColor = Color.White, coinSize = 28.dp)
                            }
                            Image(
                                painter = painterResource(id = R.drawable.redeem_rewards),
                                contentDescription = "Redeem Vouchers",
                                modifier = Modifier.size(75.dp)
                            )
                        }
                    }
                }
            }

            item {
                RewardsSectionHeader(title = "Select Reward Voucher")
            }

            items(redemptionOptions, key = { it.id }) { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedOptionForRedeem = option
                            showConfirmationBottomSheet = true
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(RoyalPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.redeem_rewards),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(option.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
                                Text(option.description, fontSize = 12.sp, color = RewardsTheme.textSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RewardPrimaryButton(
                            text = "🪙${option.coinsCost}",
                            onClick = {
                                selectedOptionForRedeem = option
                                showConfirmationBottomSheet = true
                            },
                            containerColor = RoyalPurple,
                            contentColor = Color.White,
                            modifier = Modifier.height(38.dp)
                        )
                    }
                }
            }
        }

        // Confirmation BottomSheet
        if (showConfirmationBottomSheet && selectedOptionForRedeem != null) {
            val option = selectedOptionForRedeem!!
            ModalBottomSheet(
                onDismissRequest = { showConfirmationBottomSheet = false },
                containerColor = RewardsTheme.cardBackground,
                windowInsets = WindowInsets(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(40.dp))
                    Text("Confirm Redemption (Demo)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)

                    Text(
                        text = "You are redeeming '${option.title}' for 🪙${option.coinsCost}.",
                        fontSize = 14.sp,
                        color = RewardsTheme.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusOrange.copy(alpha = 0.1f))
                            .border(1.dp, StatusOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Demo Mode — no real money or coins will be transferred.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusOrange,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RewardSecondaryButton(
                            text = "Cancel",
                            onClick = { showConfirmationBottomSheet = false },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        RewardPrimaryButton(
                            text = "Confirm Redeem",
                            onClick = {
                                showConfirmationBottomSheet = false
                                val record = viewModel.submitRedemption(option)
                                showSuccessDialog = record
                            },
                            containerColor = RoyalPurple,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }
                }
            }
        }

        // Success Dialog using reward_success asset
        if (showSuccessDialog != null) {
            val rec = showSuccessDialog!!
            AlertDialog(
                onDismissRequest = { showSuccessDialog = null },
                containerColor = RewardsTheme.cardBackground,
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.reward_success),
                        contentDescription = "Success",
                        modifier = Modifier.size(64.dp)
                    )
                },
                title = { Text("Redemption Submitted!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = RewardsTheme.textPrimary) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Voucher: ${rec.optionTitle}", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = RewardsTheme.textPrimary)
                        RewardCoinBalance(amount = rec.coinsSpent, fontSize = 16, textColor = RoyalPurple)
                        Text("Reference: ${rec.referenceCode}", fontSize = 12.sp, color = RewardsTheme.textSecondary)
                        RewardStatusChip(statusText = rec.status.name, statusColor = StatusOrange)
                    }
                },
                confirmButton = {
                    RewardPrimaryButton(
                        text = "OK",
                        onClick = { showSuccessDialog = null },
                        containerColor = RoyalPurple,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@Composable
fun RedemptionHistoryScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val redemptionHistory by viewModel.redemptionHistory.collectAsState()

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Redemption History",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = RewardsTheme.screenBackground
    ) { paddingValues ->
        if (redemptionHistory.isEmpty()) {
            RewardEmptyState(
                title = "No Redemption History",
                description = "Your submitted voucher redemptions will appear here."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(redemptionHistory, key = { it.id }) { rec ->
                    val statusColor = when (rec.status) {
                        RedemptionStatus.COMPLETED -> RewardsSuccessGreen
                        RedemptionStatus.PENDING, RedemptionStatus.PROCESSING -> StatusOrange
                        RedemptionStatus.FAILED, RedemptionStatus.REVERSED -> StatusRed
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rec.optionTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
                                Text(rec.timestamp, fontSize = 12.sp, color = RewardsTheme.textSecondary)
                                Text("Ref: ${rec.referenceCode}", fontSize = 11.sp, color = RewardsTheme.textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                RewardCoinBalance(amount = rec.coinsSpent, fontSize = 14, textColor = RoyalPurple, coinSize = 16.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                RewardStatusChip(statusText = rec.status.name, statusColor = statusColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
