package com.synfusion.workdailyadmin.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.data.models.ReportItem
import com.synfusion.workdailyadmin.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ModerationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var showResolveDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<ReportItem?>(null) }
    var actionStatus by remember { mutableStateOf("RESOLVED") }
    var resolutionReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Central Moderation Queue", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadReports() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Status Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "PENDING",
                    onClick = {
                        viewModel.statusFilter.value = "PENDING"
                        viewModel.loadReports()
                    },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = statusFilter == "REVIEWING",
                    onClick = {
                        viewModel.statusFilter.value = "REVIEWING"
                        viewModel.loadReports()
                    },
                    label = { Text("Reviewing") }
                )
                FilterChip(
                    selected = statusFilter == "RESOLVED",
                    onClick = {
                        viewModel.statusFilter.value = "RESOLVED"
                        viewModel.loadReports()
                    },
                    label = { Text("Resolved") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is ReportsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReportsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadReports() }) { Text("Retry") }
                        }
                    }
                }
                is ReportsUiState.Success -> {
                    if (state.reports.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No $statusFilter reports found in moderation queue.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.reports) { report ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Reason: ${report.reason}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val pColor = when (report.priority) {
                                                "CRITICAL" -> Color(0xFFEF4444)
                                                "HIGH" -> Color(0xFFF59E0B)
                                                else -> Color(0xFF3B82F6)
                                            }
                                            StatusChip(text = report.priority, color = pColor)
                                        }

                                        if (report.details.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Details: ${report.details}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Target UID: ${report.targetUid}", style = MaterialTheme.typography.bodySmall)
                                        Text(text = "Reporter UID: ${report.reporterUid}", style = MaterialTheme.typography.bodySmall)

                                        if (report.status == "PENDING" || report.status == "REVIEWING") {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        selectedReport = report
                                                        actionStatus = "RESOLVED"
                                                        showResolveDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Resolve")
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedReport = report
                                                        actionStatus = "IGNORED"
                                                        showResolveDialog = true
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Dismiss")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showResolveDialog && selectedReport != null) {
                val rep = selectedReport!!
                AlertDialog(
                    onDismissRequest = { showResolveDialog = false },
                    title = { Text("Report Decision: $actionStatus") },
                    text = {
                        Column {
                            Text("Resolution notes / reason:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = resolutionReason,
                                onValueChange = { resolutionReason = it },
                                placeholder = { Text("Resolution reason...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (resolutionReason.isNotBlank()) {
                                    showResolveDialog = false
                                    viewModel.updateReportStatus(rep.id, actionStatus, resolutionReason) { _, _ -> }
                                    resolutionReason = ""
                                }
                            },
                            enabled = resolutionReason.isNotBlank()
                        ) {
                            Text("Execute")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showResolveDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
