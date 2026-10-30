package com.synfusion.workdailyadmin.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardStats() }) {
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
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadDashboardStats() }) {
                            Text("Retry")
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
                        // System Health Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                                        text = "System Health Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Firebase & LiveKit operational",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                StatusChip(
                                    text = stats.systemStatus,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Platform Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // KPI Cards Grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Total Users",
                                value = stats.totalUsers.toString(),
                                subtitle = "${stats.verifiedUsers} Verified",
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Active Live Streams",
                                value = stats.activeLiveRooms.toString(),
                                subtitle = "Live Now",
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
                                subtitle = "Action Required",
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
                                subtitle = "Lifetime Earned",
                                icon = Icons.Default.MonetizationOn,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Banned / Suspended",
                                value = "${stats.bannedUsers} / ${stats.suspendedUsers}",
                                subtitle = "Moderated Accounts",
                                icon = Icons.Default.Block,
                                accentColor = Color(0xFF6B7280),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Quick Management Shortcuts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateUsers,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Users")
                            }
                            OutlinedButton(
                                onClick = onNavigateReports,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reports")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateLive,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Rooms")
                            }
                            OutlinedButton(
                                onClick = onNavigateRewards,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rewards")
                            }
                        }
                    }
                }
            }
        }
    }
}
