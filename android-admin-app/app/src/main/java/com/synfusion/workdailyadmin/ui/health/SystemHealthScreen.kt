package com.synfusion.workdailyadmin.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Refresh
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
fun SystemHealthScreen(
    viewModel: HealthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Operational Health", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadHealth() }) {
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
                is HealthUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HealthUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadHealth() }) {
                            Text("Retry")
                        }
                    }
                }
                is HealthUiState.Success -> {
                    val h = state.health
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
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
                                        text = "Cloud Infrastructure Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "WorkDaily Global Backend & Firebase Managed Cluster",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                StatusChip(text = "MONITORED", color = Color(0xFF10B981))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "Firestore DB",
                                value = h.firestoreStatus,
                                icon = Icons.Default.CloudDone,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Cloud Functions",
                                value = h.firebaseFunctionsStatus,
                                icon = Icons.Default.CloudDone,
                                accentColor = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            KpiCard(
                                title = "LiveKit SFU Nodes",
                                value = h.liveKitStatus,
                                icon = Icons.Default.CloudDone,
                                accentColor = Color(0xFFEC4899),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "FCM Delivery Rate",
                                value = h.fcmDeliveryRate,
                                icon = Icons.Default.CloudDone,
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
