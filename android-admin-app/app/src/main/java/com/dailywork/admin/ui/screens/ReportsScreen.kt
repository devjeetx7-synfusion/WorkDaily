package com.dailywork.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailywork.admin.data.model.Report
import com.dailywork.admin.ui.utils.formatTimestamp
import com.dailywork.admin.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ReportsScreen(
    onUserClick: (String) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val reports by viewModel.reports.collectAsState()
    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(refreshing, {
        viewModel.loadReports()
        refreshing = false
    })
    val isPerformingAction by viewModel.isPerformingAction.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("User Reports", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
        if (reports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Flag,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No pending reports", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(reports) { report ->
                    ReportCard(
                        report = report,
                        isPerformingAction = isPerformingAction,
                        onUserClick = { onUserClick(report.reportedUserId) },
                        onIgnore = { viewModel.ignoreReport(report.id) },
                        onResolve = { viewModel.resolveReport(report.id) }
                    )
                }
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    isPerformingAction: Boolean,
    onUserClick: () -> Unit,
    onIgnore: () -> Unit,
    onResolve: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(
                        report.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(formatTimestamp(report.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Reported User ID: ${report.reportedUserId}", fontWeight = FontWeight.Bold)
            Text("Reported By: ${report.reportedBy}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    report.reason,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUserClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isPerformingAction,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View User")
                }

                if (report.status == "pending") {
                    FilledTonalIconButton(
                        onClick = onIgnore,
                        enabled = !isPerformingAction,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, null)
                    }
                    IconButton(
                        onClick = onResolve,
                        enabled = !isPerformingAction,
                        modifier = Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}
