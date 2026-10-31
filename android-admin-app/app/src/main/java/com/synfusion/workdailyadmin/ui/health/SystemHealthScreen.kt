package com.synfusion.workdailyadmin.ui.health

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.SystemHealthData
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemHealthScreen(
    viewModel: HealthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("System Operational Health", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Cloud infrastructure, database availability & push metrics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadHealth() },
                        enabled = uiState !is HealthUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Health")
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
                is HealthUiState.Loading -> {
                    AdminLoadingState(message = "Executing real-time infrastructure diagnostics...")
                }
                is HealthUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadHealth() }
                    )
                }
                is HealthUiState.Success -> {
                    val h = state.health
                    val checkTimeStr = remember(h.timestamp) {
                        SimpleDateFormat("EEE, MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(h.timestamp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Diagnostic Hero Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "All Core Services Monitored",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    StatusChip(text = "LIVE", color = Color(0xFF10B981))
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "WorkDaily Global Backend, Firebase Managed Cluster & LiveKit SFU Nodes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Last Diagnostic Run:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = checkTimeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Cloud Microservices & Connectivity",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Responsive Grid for Microservices
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ServiceHealthCard(
                                name = "Firestore Database",
                                status = h.firestoreStatus,
                                description = "Primary document store & real-time sync",
                                icon = Icons.Default.Storage,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            ServiceHealthCard(
                                name = "Cloud Functions",
                                status = h.firebaseFunctionsStatus,
                                description = "Serverless administrative RPC backend",
                                icon = Icons.Default.Code,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            ServiceHealthCard(
                                name = "LiveKit SFU Nodes",
                                status = h.liveKitStatus,
                                description = "Real-time WebRTC audio/video broadcasting",
                                icon = Icons.Default.LiveTv,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            ServiceHealthCard(
                                name = "FCM Delivery Rate",
                                status = h.fcmDeliveryRate,
                                description = "Push broadcast reliability metric",
                                icon = Icons.Default.NotificationsActive,
                                isPercentage = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (h.errorTrends24h.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "24h Error Incident Trends",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    h.errorTrends24h.forEach { (category, count) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = category,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            StatusChip(
                                                text = "$count events",
                                                color = if (count > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceHealthCard(
    name: String,
    status: String,
    description: String,
    icon: ImageVector,
    isPercentage: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isHealthy = status.equals("HEALTHY", ignoreCase = true) || isPercentage
    val statusColor = if (isHealthy) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                StatusChip(text = status, color = statusColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}
