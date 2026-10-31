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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.data.AdminErrorType
import com.synfusion.workdailyadmin.ui.components.KpiCard
import com.synfusion.workdailyadmin.ui.components.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        onClick = { viewModel.loadDashboardStats(force = true) },
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
                    val icon = when (state.errorType) {
                        AdminErrorType.NETWORK_UNAVAILABLE -> Icons.Default.WifiOff
                        AdminErrorType.AUTHENTICATION_EXPIRED -> Icons.Default.Lock
                        AdminErrorType.PERMISSION_DENIED -> Icons.Default.Security
                        AdminErrorType.BACKEND_FUNCTION_NOT_FOUND -> Icons.Default.CloudQueue
                        AdminErrorType.RESOURCE_NOT_FOUND -> Icons.Default.SearchOff
                        else -> Icons.Default.CloudOff
                    }

                    val title = when (state.errorType) {
                        AdminErrorType.NETWORK_UNAVAILABLE -> "Connection Unavailable"
                        AdminErrorType.AUTHENTICATION_EXPIRED -> "Session Expired"
                        AdminErrorType.PERMISSION_DENIED -> "Access Restricted"
                        AdminErrorType.BACKEND_FUNCTION_NOT_FOUND -> "Backend Service Unavailable"
                        AdminErrorType.RESOURCE_NOT_FOUND -> "Resource Not Found"
                        else -> "Sync Failure"
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (state.canRetry) {
                            Button(
                                onClick = { viewModel.loadDashboardStats(force = true) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Network Sync")
                            }
                        }
                    }
                }
                is DashboardUiState.Success -> {
                    val stats = state.stats
                    val timeFormat = remember { SimpleDateFormat("HH:mm:ss dd MMM", Locale.getDefault()) }
                    val formattedTime = remember(stats.timestamp) { timeFormat.format(Date(stats.timestamp)) }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val availableWidth = maxWidth
                        val isWide = availableWidth >= 700.dp

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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "System Operational Health",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        val subText = if (state.isFallback) {
                                            "Connected via Direct Firestore Resilient Sync"
                                        } else {
                                            "Firebase Services & LiveKit Managed Nodes"
                                        }
                                        Text(
                                            text = subText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Last synced: $formattedTime",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                        )
                                    }
                                    StatusChip(
                                        text = stats.systemStatus,
                                        color = if (state.isFallback) Color(0xFF3B82F6) else Color(0xFF10B981)
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

                            if (isWide) {
                                // 4-column responsive grid on wide screens
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    KpiCard(
                                        title = "Total Users",
                                        value = stats.totalUsers.toString(),
                                        subtitle = "${stats.verifiedUsers} Verified",
                                        icon = Icons.Default.People,
                                        onClick = onNavigateUsers,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Active Live",
                                        value = stats.activeLiveRooms.toString(),
                                        subtitle = "Broadcasting Now",
                                        icon = Icons.Default.LiveTv,
                                        accentColor = Color(0xFFEC4899),
                                        onClick = onNavigateLive,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Pending Reports",
                                        value = stats.pendingReports.toString(),
                                        subtitle = "Moderation Queue",
                                        icon = Icons.Default.ReportProblem,
                                        accentColor = Color(0xFFEF4444),
                                        onClick = onNavigateReports,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Support / Redemptions",
                                        value = stats.pendingRedemptionsCount.toString(),
                                        subtitle = "Awaiting Action",
                                        icon = Icons.Default.SupportAgent,
                                        accentColor = Color(0xFFF59E0B),
                                        onClick = onNavigateRewards,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                // 2-column responsive layout on mobile screens
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    KpiCard(
                                        title = "Total Users",
                                        value = stats.totalUsers.toString(),
                                        subtitle = "${stats.verifiedUsers} Verified",
                                        icon = Icons.Default.People,
                                        onClick = onNavigateUsers,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Active Live",
                                        value = stats.activeLiveRooms.toString(),
                                        subtitle = "Broadcasting Now",
                                        icon = Icons.Default.LiveTv,
                                        accentColor = Color(0xFFEC4899),
                                        onClick = onNavigateLive,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    KpiCard(
                                        title = "Pending Reports",
                                        value = stats.pendingReports.toString(),
                                        subtitle = "Moderation Queue",
                                        icon = Icons.Default.ReportProblem,
                                        accentColor = Color(0xFFEF4444),
                                        onClick = onNavigateReports,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiCard(
                                        title = "Support / Redemptions",
                                        value = stats.pendingRedemptionsCount.toString(),
                                        subtitle = "Awaiting Action",
                                        icon = Icons.Default.SupportAgent,
                                        accentColor = Color(0xFFF59E0B),
                                        onClick = onNavigateRewards,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                KpiCard(
                                    title = "Coins Issued",
                                    value = stats.totalCoinsIssued.toString(),
                                    subtitle = "Lifetime Minted",
                                    icon = Icons.Default.MonetizationOn,
                                    accentColor = Color(0xFF10B981),
                                    onClick = onNavigateRewards,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiCard(
                                    title = "Moderated Accounts",
                                    value = "${stats.bannedUsers} / ${stats.suspendedUsers}",
                                    subtitle = "Banned / Suspended",
                                    icon = Icons.Default.Block,
                                    accentColor = Color(0xFF6B7280),
                                    onClick = onNavigateUsers,
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
                                    Text("Users")
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
                                    Text("Reports")
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
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rewards")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
