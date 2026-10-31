package com.synfusion.workdailyadmin.ui.reports

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.ReportItem
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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
                title = {
                    Column {
                        Text("Central Moderation Queue", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Abuse reports, user disputes & policy enforcement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadReports() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Reports")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Horizontal Status Filter Chips
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
                    label = { Text("Pending Review") },
                    leadingIcon = if (statusFilter == "PENDING") {
                        { Icon(Icons.Default.Pending, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = statusFilter == "RESOLVED",
                    onClick = {
                        viewModel.statusFilter.value = "RESOLVED"
                        viewModel.loadReports()
                    },
                    label = { Text("Resolved") },
                    leadingIcon = if (statusFilter == "RESOLVED") {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = statusFilter == "DISMISSED",
                    onClick = {
                        viewModel.statusFilter.value = "DISMISSED"
                        viewModel.loadReports()
                    },
                    label = { Text("Dismissed") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (val state = uiState) {
                is ReportsUiState.Loading -> {
                    AdminLoadingState(message = "Fetching moderation queue records...")
                }
                is ReportsUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadReports() }
                    )
                }
                is ReportsUiState.Success -> {
                    if (state.reports.isEmpty()) {
                        AdminEmptyState(
                            title = "Moderation Queue Clear",
                            description = "No reports found with status: $statusFilter.",
                            icon = Icons.Default.Shield,
                            actionText = "Refresh Queue",
                            onActionClick = { viewModel.loadReports() }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.reports, key = { it.id }) { report ->
                                ReportItemCard(
                                    report = report,
                                    onAction = { status ->
                                        selectedReport = report
                                        actionStatus = status
                                        resolutionReason = ""
                                        showResolveDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showResolveDialog && selectedReport != null) {
                val report = selectedReport!!

                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showResolveDialog = false },
                    title = {
                        Text(
                            text = if (actionStatus == "RESOLVED") "Mark Report as Resolved" else "Dismiss Report",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Please enter resolution notes and administrative disposition for report #${report.id.take(8)}:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = resolutionReason,
                                onValueChange = { resolutionReason = it },
                                placeholder = { Text("Resolution reason & audit notes...") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (resolutionReason.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.updateReportStatus(
                                        reportId = report.id,
                                        status = actionStatus,
                                        resolutionReason = resolutionReason.trim()
                                    ) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showResolveDialog = false
                                        }
                                    }
                                }
                            },
                            enabled = resolutionReason.isNotBlank() && !isSubmitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (actionStatus == "RESOLVED") Color(0xFF10B981) else Color(0xFF6B7280)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text(if (actionStatus == "RESOLVED") "Resolve Report" else "Dismiss Report")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showResolveDialog = false },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun ReportItemCard(
    report: ReportItem,
    onAction: (String) -> Unit
) {
    val dateStr = remember(report.timestamp) {
        if (report.timestamp > 0) {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        } else "Unknown"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (report.status.uppercase()) {
                        "PENDING" -> Color(0xFFEF4444)
                        "RESOLVED" -> Color(0xFF10B981)
                        else -> Color(0xFF6B7280)
                    }
                    StatusChip(text = report.status, color = statusColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(text = "Priority: ${report.priority}", color = Color(0xFF3B82F6))
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reason: ${report.reason.ifBlank { "Unspecified violation" }}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (report.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    TruncatedIdChip(id = report.targetUid, prefix = "Reported UID: ")
                    Spacer(modifier = Modifier.height(4.dp))
                    TruncatedIdChip(id = report.reporterUid, prefix = "Reporter UID: ")
                }

                if (report.status == "PENDING") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onAction("DISMISSED") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { onAction("RESOLVED") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Resolve", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (report.resolutionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Resolution Notes: ${report.resolutionReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
