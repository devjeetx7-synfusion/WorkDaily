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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards & Economy Management", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadRewardsOverview() }) {
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
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is RewardsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is RewardsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadRewardsOverview() }) { Text("Retry") }
                    }
                }
                is RewardsUiState.Success -> {
                    val stats = state.stats
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Overview Cards
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
                                subtitle = "Lifetime Redeemed",
                                icon = Icons.Default.ShoppingBag,
                                accentColor = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Manual Coin Adjustment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Direct balance adjustment requires mandatory target UID, amount (+/-), reason, and generates an immutable audit reference.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showAdjustDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Adjust User Balance")
                                }
                            }
                        }
                    }
                }
            }

            if (showAdjustDialog) {
                AlertDialog(
                    onDismissRequest = { showAdjustDialog = false },
                    title = { Text("Manual Balance Adjustment") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = targetUidInput,
                                onValueChange = { targetUidInput = it },
                                label = { Text("Target User UID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = coinAmountInput,
                                onValueChange = { coinAmountInput = it },
                                label = { Text("Coin Amount (+ / -)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adjustReasonInput,
                                onValueChange = { adjustReasonInput = it },
                                label = { Text("Mandatory Audit Reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = coinAmountInput.toIntOrNull() ?: 0
                                if (targetUidInput.isNotBlank() && amount != 0 && adjustReasonInput.isNotBlank()) {
                                    showAdjustDialog = false
                                    viewModel.adjustUserCoins(targetUidInput.trim(), amount, adjustReasonInput.trim()) { _, _ -> }
                                    targetUidInput = ""
                                    coinAmountInput = ""
                                    adjustReasonInput = ""
                                }
                            },
                            enabled = targetUidInput.isNotBlank() && coinAmountInput.toIntOrNull() != null && adjustReasonInput.isNotBlank()
                        ) {
                            Text("Execute Adjustment")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showAdjustDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
