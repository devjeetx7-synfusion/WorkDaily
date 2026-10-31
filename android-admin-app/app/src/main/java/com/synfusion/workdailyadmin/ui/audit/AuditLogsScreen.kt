package com.synfusion.workdailyadmin.ui.audit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.AdminAuditLog
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
    viewModel: AuditViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Immutable Audit Trail", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Chronological record of administrative operations & security events", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAuditLogs() },
                        enabled = uiState !is AuditUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Logs")
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (val state = uiState) {
                is AuditUiState.Loading -> {
                    AdminLoadingState(message = "Reading immutable audit log trail...")
                }
                is AuditUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadAuditLogs() }
                    )
                }
                is AuditUiState.Success -> {
                    if (state.logs.isEmpty()) {
                        AdminEmptyState(
                            title = "No Audit Records Logged",
                            description = "Administrative actions taken on users, rooms, reports, or economy will appear here chronologically.",
                            icon = Icons.Default.History,
                            actionText = "Refresh Trail",
                            onActionClick = { viewModel.loadAuditLogs() }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.logs, key = { it.id }) { log ->
                                AuditLogCard(log = log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(
    log: AdminAuditLog
) {
    val dateStr = remember(log.timestamp) {
        if (log.timestamp > 0) {
            SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
        } else "Recent"
    }

    val actionColor = when {
        log.action.contains("BAN", ignoreCase = true) -> Color(0xFFEF4444)
        log.action.contains("SUSPEND", ignoreCase = true) -> Color(0xFFF59E0B)
        log.action.contains("COIN", ignoreCase = true) -> Color(0xFF10B981)
        log.action.contains("ROLE", ignoreCase = true) -> Color(0xFF8B5CF6)
        log.action.contains("TERMINATE", ignoreCase = true) -> Color(0xFFEC4899)
        else -> Color(0xFF3B82F6)
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
                StatusChip(text = log.action, color = actionColor)
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Actor: ${log.adminEmail.ifBlank { log.adminUid }}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (log.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: ${log.reason}",
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
                if (log.targetId.isNotBlank()) {
                    TruncatedIdChip(id = log.targetId, prefix = "Target (${log.targetType}): ", maxLength = 16)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                TruncatedIdChip(id = log.id, prefix = "Log ID: ", maxLength = 10)
            }
        }
    }
}
