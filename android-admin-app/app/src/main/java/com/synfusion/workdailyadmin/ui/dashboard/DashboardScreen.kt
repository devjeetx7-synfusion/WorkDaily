package com.synfusion.workdailyadmin.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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
                title = {
                    Column {
                        Text(
                            text = "Executive Dashboard",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Platform metrics, activity and system status",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDashboardStats() },
                        enabled = uiState !is DashboardUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    AdminLoadingState(message = "Synchronizing platform metrics...")
                }
                is DashboardUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadDashboardStats() }
                    )
                }
                is DashboardUiState.Success -> {
                    val stats = state.stats
                    val formattedDate = remember(stats.timestamp) {
                        SimpleDateFormat("EEE, MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(stats.timestamp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Top Context Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (stats.maintenanceMode) Color(0xFFF59E0B) else Color(0xFF10B981),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (stats.maintenanceMode) "Maintenance Mode Active" else "Platform Operational",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (stats.maintenanceMode) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                            }
                            Text(
                                text = "Synced: $formattedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Responsive KPI Cards Grid
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isWide = maxWidth >= 760.dp

                            if (isWide) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        KpiCard(
                                            title = "Total Registered Users",
                                            value = stats.totalUsers.toString(),
                                            subtitle = "${stats.verifiedUsers} Verified Accounts",
                                            icon = Icons.Default.People,
                                            accentColor = MaterialTheme.colorScheme.primary,
                                            onClick = onNavigateUsers,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiCard(
                                            title = "Active Live Rooms",
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
                                            subtitle = "Awaiting Moderation",
                                            icon = Icons.Default.ReportProblem,
                                            accentColor = Color(0xFFEF4444),
                                            onClick = onNavigateReports,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        KpiCard(
                                            title = "Pending Redemptions",
                                            value = stats.pendingRedemptionsCount.toString(),
                                            subtitle = "Requires Disbursement",
                                            icon = Icons.Default.SupportAgent,
                                            accentColor = Color(0xFFF59E0B),
                                            onClick = onNavigateRewards,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiCard(
                                            title = "Total Coins Issued",
                                            value = stats.totalCoinsIssued.toString(),
                                            subtitle = "${stats.totalCoinsSpent} Coins Redeemed",
                                            icon = Icons.Default.MonetizationOn,
                                            accentColor = Color(0xFF10B981),
                                            onClick = onNavigateRewards,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiCard(
                                            title = "Moderated Accounts",
                                            value = "${stats.bannedUsers} Banned",
                                            subtitle = "${stats.suspendedUsers} Suspended Accounts",
                                            icon = Icons.Default.Block,
                                            accentColor = Color(0xFF6B7280),
                                            onClick = onNavigateUsers,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                // 2-column grid for mobile
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        KpiCard(
                                            title = "Total Users",
                                            value = stats.totalUsers.toString(),
                                            subtitle = "${stats.verifiedUsers} Verified",
                                            icon = Icons.Default.People,
                                            accentColor = MaterialTheme.colorScheme.primary,
                                            onClick = onNavigateUsers,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiCard(
                                            title = "Active Live",
                                            value = stats.activeLiveRooms.toString(),
                                            subtitle = "Broadcasting",
                                            icon = Icons.Default.LiveTv,
                                            accentColor = Color(0xFFEC4899),
                                            onClick = onNavigateLive,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        KpiCard(
                                            title = "Pending Reports",
                                            value = stats.pendingReports.toString(),
                                            subtitle = "Queue Action",
                                            icon = Icons.Default.ReportProblem,
                                            accentColor = Color(0xFFEF4444),
                                            onClick = onNavigateReports,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiCard(
                                            title = "Redemptions",
                                            value = stats.pendingRedemptionsCount.toString(),
                                            subtitle = "Pending Payout",
                                            icon = Icons.Default.SupportAgent,
                                            accentColor = Color(0xFFF59E0B),
                                            onClick = onNavigateRewards,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
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
                                            title = "Banned Users",
                                            value = stats.bannedUsers.toString(),
                                            subtitle = "${stats.suspendedUsers} Suspended",
                                            icon = Icons.Default.Block,
                                            accentColor = Color(0xFF6B7280),
                                            onClick = onNavigateUsers,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Navigation Shortcuts
                        Text(
                            text = "Administrative Workflows",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkflowActionCard(
                                title = "User Directory",
                                description = "Search, filter & moderate members",
                                icon = Icons.Default.People,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateUsers,
                                modifier = Modifier.weight(1f)
                            )
                            WorkflowActionCard(
                                title = "Moderation Queue",
                                description = "Resolve flags & safety reports",
                                icon = Icons.Default.ReportProblem,
                                color = Color(0xFFEF4444),
                                onClick = onNavigateReports,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkflowActionCard(
                                title = "Live Monitoring",
                                description = "Inspect & supervise broadcast rooms",
                                icon = Icons.Default.LiveTv,
                                color = Color(0xFFEC4899),
                                onClick = onNavigateLive,
                                modifier = Modifier.weight(1f)
                            )
                            WorkflowActionCard(
                                title = "Rewards & Economy",
                                description = "Balance adjustments & payouts",
                                icon = Icons.Default.MonetizationOn,
                                color = Color(0xFF10B981),
                                onClick = onNavigateRewards,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Version & App Status Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Client Configuration Sync",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    StatusChip(
                                        text = stats.systemStatus,
                                        color = if (stats.systemStatus == "OPERATIONAL") Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Minimum Supported Client:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "v${stats.minSupportedVersionCode}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Latest Published Release:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "v${stats.latestVersionCode}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
