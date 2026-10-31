package com.synfusion.workdailyadmin.ui.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.synfusion.workdailyadmin.ui.components.TruncatedIdChip

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
    var isSubmitting by remember { mutableStateOf(false) }

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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Horizontally scrollable status filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
                    selected = statusFilter == "RESOLVED",
                    onClick = {
                        viewModel.statusFilter.value = "RESOLVED"
                        viewModel.loadReports()
                    },
                    label = { Text("Resolved") }
                )
                FilterChip(
                    selected = statusFilter == "IGNORED",
                    onClick = {
                        viewModel.statusFilter.value = "IGNORED"
                        viewModel.loadReports()
                    },
                    label = { Text("Dismissed") }
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadReports() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is ReportsUiState.Success -> {
                    if (state.reports.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No reports in this category.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.reports, key = { it.id }) { report ->
                                ReportItemCard(
                                    report = report,
                                    onTakeAction = {
                                        selectedReport = report
                                        showResolveDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showResolveDialog && selectedReport != null) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showResolveDialog = false },
                    title = { Text("Resolve Report") },
                    text = {
                        Column {
                            Text("Take moderation action on report #${selectedReport!!.id}:")
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = actionStatus == "RESOLVED",
                                    onClick = { actionStatus = "RESOLVED" },
                                    label = { Text("Resolve") }
                                )
                                FilterChip(
                                    selected = actionStatus == "IGNORED",
                                    onClick = { actionStatus = "IGNORED" },
                                    label = { Text("Dismiss") }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resolutionReason,
                                onValueChange = { resolutionReason = it },
                                placeholder = { Text("Action reason...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (resolutionReason.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    viewModel.updateReportStatus(selectedReport!!.id, actionStatus, resolutionReason) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showResolveDialog = false
                                            resolutionReason = ""
                                        }
                                    }
                                }
                            },
                            enabled = resolutionReason.isNotBlank() && !isSubmitting
                        ) {
                            Text("Submit Action")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showResolveDialog = false }, enabled = !isSubmitting) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ReportItemCard(
    report: ReportItem,
    onTakeAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.reason.ifBlank { "Unspecified Violation" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val statusColor = when (report.status) {
                    "PENDING" -> Color(0xFFEF4444)
                    "RESOLVED" -> Color(0xFF10B981)
                    else -> Color.Gray
                }
                StatusChip(text = report.status, color = statusColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (report.details.isNotBlank()) {
                Text(
                    text = report.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TruncatedIdChip(id = report.reporterUid, prefix = "Reporter: ", maxLength = 14)
                TruncatedIdChip(id = report.targetUid, prefix = "Target: ", maxLength = 14)
            }

            if (report.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onTakeAction,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Moderate Report")
                }
            }
        }
    }
}
