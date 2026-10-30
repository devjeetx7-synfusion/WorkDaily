package com.synfusion.workdailyadmin.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.ui.components.KpiCard
import com.synfusion.workdailyadmin.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateUsers: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateLive: () -> Unit,
    onNavigateRewards: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Portal Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDashboardStats() },
                        enabled = uiState !is DashboardUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Syncing platform metrics...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDashboardStats() }) {
                            Text("Retry Network Sync")
                        }
                    }
                }
                is DashboardUiState.Success -> {
                    val stats = state.stats
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Operational Status Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "System Operational Health",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Firebase Services & LiveKit Managed Nodes",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                StatusChip(
                                    text = stats.systemStatus,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Platform Core Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // KPI Cards Grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Total Users",
                                value = stats.totalUsers.toString(),
                                subtitle = "${stats.verifiedUsers} Verified Accounts",
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Active Live Streams",
                                value = stats.activeLiveRooms.toString(),
                                subtitle = "Broadcasting Now",
                                icon = Icons.Default.LiveTv,
                                accentColor = Color(0xFFEC4899),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Pending Reports",
                                value = stats.pendingReports.toString(),
                                subtitle = "Moderation Queue",
                                icon = Icons.Default.ReportProblem,
                                accentColor = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Pending Redemptions",
                                value = stats.pendingRedemptionsCount.toString(),
                                subtitle = "Rewards Queue",
                                icon = Icons.Default.CardGiftcard,
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Coins Issued",
                                value = stats.totalCoinsIssued.toString(),
                                subtitle = "Lifetime Minted",
                                icon = Icons.Default.MonetizationOn,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Banned / Suspended",
                                value = "${stats.bannedUsers} / ${stats.suspendedUsers}",
                                subtitle = "Moderated Users",
                                icon = Icons.Default.Block,
                                accentColor = Color(0xFF6B7280),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Admin Actions & Workflows",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateUsers,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("User Directory")
                            }
                            OutlinedButton(
                                onClick = onNavigateReports,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Moderation")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateLive,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Live Streams")
                            }
                            OutlinedButton(
                                onClick = onNavigateRewards,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Economy")
                            }
                        }
                    }
                }
            }
        }
    }
}
