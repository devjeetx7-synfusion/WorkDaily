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
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is RewardsUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching economy metrics...")
                    }
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Server-Authoritative Coin Adjustment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Direct balance adjustment requires target UID, numeric amount (+/-), reason, and generates an immutable audit reference code.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAdjustDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Adjust User Balance")
                                }
                            }
                        }
                    }
                }
            }

            if (showAdjustDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showAdjustDialog = false },
                    title = { Text("Manual Balance Adjustment", fontWeight = FontWeight.Bold) },
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
                                if (targetUidInput.isNotBlank() && amount != 0 && adjustReasonInput.isNotBlank() && !isSubmitting) {
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
                            enabled = targetUidInput.isNotBlank() && coinAmountInput.toIntOrNull() != null && coinAmountInput.toIntOrNull() != 0 && adjustReasonInput.isNotBlank() && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Execute Adjustment")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showAdjustDialog = false },
                            enabled = !isSubmitting
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
