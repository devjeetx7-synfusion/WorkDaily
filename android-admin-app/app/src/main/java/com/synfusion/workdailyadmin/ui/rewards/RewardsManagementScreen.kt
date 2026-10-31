package com.synfusion.workdailyadmin.ui.rewards

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
import com.synfusion.workdailyadmin.ui.components.KpiCard

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
                title = { Text("Rewards & Economy Governance", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadRewardsOverview() },
                        enabled = uiState !is RewardsUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (val state = uiState) {
                is RewardsUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading economy stats...")
                    }
                }
                is RewardsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadRewardsOverview() }) {
                            Text("Retry")
                        }
                    }
                }
                is RewardsUiState.Success -> {
                    val s = state.stats
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                    ) {
                        Text(
                            text = "WorkDaily Platform Economy",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Coins Issued",
                                value = s.totalCoinsIssued.toString(),
                                subtitle = "Lifetime Minted",
                                icon = Icons.Default.MonetizationOn,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Coins Redeemed",
                                value = s.totalCoinsSpent.toString(),
                                subtitle = "Burned / Redeemed",
                                icon = Icons.Default.LocalFireDepartment,
                                accentColor = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Pending Redemptions",
                                value = s.pendingRedemptionsCount.toString(),
                                subtitle = "Action Required",
                                icon = Icons.Default.CardGiftcard,
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Circulating Ratio",
                                value = if (s.totalCoinsIssued > 0) "${((s.totalCoinsIssued - s.totalCoinsSpent) * 100 / s.totalCoinsIssued)}%" else "100%",
                                subtitle = "Active Supply",
                                icon = Icons.Default.PieChart,
                                accentColor = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Admin Coins Control Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Administrative Coin Adjustment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Grant or deduct coins from user wallets with mandatory audit trail.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAdjustDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Adjust User Coins")
                                }
                            }
                        }
                    }
                }
            }

            if (showAdjustDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showAdjustDialog = false },
                    title = { Text("Manual Coin Adjustment") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = targetUidInput,
                                onValueChange = { targetUidInput = it },
                                label = { Text("Target User UID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = coinAmountInput,
                                onValueChange = { coinAmountInput = it },
                                label = { Text("Amount (e.g. 50 or -50)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adjustReasonInput,
                                onValueChange = { adjustReasonInput = it },
                                label = { Text("Mandatory Reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = coinAmountInput.toIntOrNull()
                                if (targetUidInput.isNotBlank() && amount != null && adjustReasonInput.isNotBlank() && !isSubmitting) {
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
                            enabled = targetUidInput.isNotBlank() && coinAmountInput.toIntOrNull() != null && adjustReasonInput.isNotBlank() && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Apply Adjustment")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showAdjustDialog = false }, enabled = !isSubmitting) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
