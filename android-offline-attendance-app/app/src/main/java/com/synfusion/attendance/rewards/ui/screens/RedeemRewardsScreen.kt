package com.synfusion.attendance.rewards.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redeem Rewards", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateRedemptionHistory) {
                        Text("History", color = RewardsPurple, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsPurpleContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Available Balance", fontSize = 12.sp, color = RewardsPurple, fontWeight = FontWeight.SemiBold)
                            CoinAmount(amount = walletSummary.availableCoins, fontSize = 22, textColor = Color(0xFF1F1047))
                        }
                        RewardStatusChip(statusText = "DEMO MODE", statusColor = StatusOrange)
                    }
                }
            }

            item {
                RewardsSectionHeader(title = "Select Reward Voucher")
            }

            items(redemptionOptions) { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedOptionForRedeem = option
                            showConfirmationBottomSheet = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(RewardsPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎟️", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(option.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(option.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(
                            onClick = {
                                selectedOptionForRedeem = option
                                showConfirmationBottomSheet = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RewardsPurple)
                        ) {
                            CoinAmount(amount = option.coinsCost, fontSize = 13, textColor = Color.White)
                        }
                    }
                }
            }
        }

        // Confirmation BottomSheet
        if (showConfirmationBottomSheet && selectedOptionForRedeem != null) {
            val option = selectedOptionForRedeem!!
            ModalBottomSheet(
                onDismissRequest = { showConfirmationBottomSheet = false },
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
                    Text("Confirm Redemption (Demo)", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Text(
                        text = "You are redeeming '${option.title}' for 🪙${option.coinsCost}.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmationBottomSheet = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                showConfirmationBottomSheet = false
                                val record = viewModel.submitRedemption(option)
                                showSuccessDialog = record
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RewardsPurple)
                        ) {
                            Text("Confirm Demo Redeem", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Preview Success Dialog
        if (showSuccessDialog != null) {
            val rec = showSuccessDialog!!
            AlertDialog(
                onDismissRequest = { showSuccessDialog = null },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(48.dp)) },
                title = { Text("Redemption Submitted!", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Voucher: ${rec.optionTitle}", fontWeight = FontWeight.Bold)
                        Text("Coins Spent: 🪙${rec.coinsSpent}")
                        Text("Reference: ${rec.referenceCode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Status: ${rec.status.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = RewardsPurple)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionHistoryScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val redemptionHistory by viewModel.redemptionHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redemption History", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(redemptionHistory) { rec ->
                val statusColor = when (rec.status) {
                    RedemptionStatus.COMPLETED -> StatusGreen
                    RedemptionStatus.PENDING, RedemptionStatus.PROCESSING -> StatusOrange
                    RedemptionStatus.FAILED, RedemptionStatus.REVERSED -> StatusRed
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(rec.optionTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(rec.timestamp, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Ref: ${rec.referenceCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🪙${rec.coinsSpent}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                            Spacer(modifier = Modifier.height(2.dp))
                            RewardStatusChip(statusText = rec.status.name, statusColor = statusColor)
                        }
                    }
                }
            }
        }
    }
}
