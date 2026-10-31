package com.synfusion.workdailyadmin.ui.rewards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsManagementScreen(
    viewModel: RewardsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAdjustDialog by remember { mutableStateOf(false) }
    var targetUidInput by remember { mutableStateOf("") }
    var coinAmountInput by remember { mutableStateOf("") }
    var adjustReasonInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rewards & Economy Governance", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Virtual coin currency, balance adjustments & payout queue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadRewardsOverview() },
                        enabled = uiState !is RewardsUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Rewards")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is RewardsUiState.Loading -> {
                    AdminLoadingState(message = "Calculating economy circulating statistics...")
                }
                is RewardsUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadRewardsOverview() }
                    )
                }
                is RewardsUiState.Success -> {
                    val stats = state.stats
                    val netCirculation = stats.totalCoinsIssued - stats.totalCoinsSpent

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Action Banner: Manual Balance Adjustment
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Direct Balance Adjustment",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Credit or debit coin balances for user dispute resolutions with immutable audit logging.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { showAdjustDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Adjust Balance")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Economy Overview KPI Cards
                        Text(
                            text = "Platform Economy Metrics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Total Coins Issued",
                                value = stats.totalCoinsIssued.toString(),
                                subtitle = "Lifetime Minted",
                                icon = Icons.Default.MonetizationOn,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Total Coins Spent",
                                value = stats.totalCoinsSpent.toString(),
                                subtitle = "Redeemed / Burned",
                                icon = Icons.Default.LocalFireDepartment,
                                accentColor = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Net Active Circulation",
                                value = netCirculation.toString(),
                                subtitle = "Held in User Wallets",
                                icon = Icons.Default.AccountBalanceWallet,
                                accentColor = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Pending Redemptions",
                                value = stats.pendingRedemptionsCount.toString(),
                                subtitle = "Disbursement Queue",
                                icon = Icons.Default.PendingActions,
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Pending Redemptions / Payout Queue
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Redemption Queue",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    StatusChip(
                                        text = "${state.pendingRedemptions.size} Pending",
                                        color = Color(0xFFF59E0B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (state.pendingRedemptions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.CheckCircleOutline,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "No pending redemptions awaiting review",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        state.pendingRedemptions.forEach { redemption ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = redemption.optionTitle,
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                        Text(
                                                            text = "Value: ${redemption.monetaryValue} • Spent: ${redemption.coinsSpent} Coins",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        TruncatedIdChip(id = redemption.targetUid, prefix = "UID: ")
                                                    }
                                                    StatusChip(text = redemption.status, color = Color(0xFFF59E0B))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Adjust Coins Modal Dialog
            if (showAdjustDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showAdjustDialog = false },
                    title = { Text("Adjust User Coin Balance", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Enter positive amount to grant coins, or negative to deduct coins from the user's wallet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = targetUidInput,
                                onValueChange = { targetUidInput = it },
                                label = { Text("Target User UID") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = coinAmountInput,
                                onValueChange = { coinAmountInput = it },
                                label = { Text("Coin Amount (+ / -)") },
                                placeholder = { Text("e.g. 50 or -20") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = adjustReasonInput,
                                onValueChange = { adjustReasonInput = it },
                                label = { Text("Mandatory Audit Reason") },
                                placeholder = { Text("e.g. Compensation for failed check-in") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = coinAmountInput.toIntOrNull()
                                if (targetUidInput.isNotBlank() && amount != null && adjustReasonInput.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.adjustUserCoins(targetUidInput.trim(), amount, adjustReasonInput.trim()) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showAdjustDialog = false
                                            targetUidInput = ""
                                            coinAmountInput = ""
                                            adjustReasonInput = ""
                                        }
                                    }
                                }
                            },
                            enabled = targetUidInput.isNotBlank() && coinAmountInput.toIntOrNull() != null && adjustReasonInput.isNotBlank() && !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Execute Adjustment")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showAdjustDialog = false },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
