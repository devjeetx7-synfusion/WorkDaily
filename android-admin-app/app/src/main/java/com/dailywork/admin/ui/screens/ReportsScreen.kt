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
import androidx.compose.ui.unit.sp
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

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("pending", "resolved", "ignored")
    val filteredReports = reports.filter { it.status == tabs[selectedTabIndex] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports Center", fontWeight = FontWeight.Black, fontSize = 28.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                if (filteredReports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Flag,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No ${tabs[selectedTabIndex]} reports", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredReports) { report ->
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when(report.status) {
                        "pending" -> MaterialTheme.colorScheme.errorContainer
                        "resolved" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when(report.status) {
                        "pending" -> MaterialTheme.colorScheme.onErrorContainer
                        "resolved" -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    Text(
                        report.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(formatTimestamp(report.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Reported User: ${report.reportedUserId}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("By: ${report.reportedBy}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    report.reason,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onUserClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !isPerformingAction,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Review Target")
                }

                if (report.status == "pending") {
                    FilledTonalIconButton(
                        onClick = onIgnore,
                        enabled = !isPerformingAction,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, null)
                    }
                    IconButton(
                        onClick = onResolve,
                        enabled = !isPerformingAction,
                        modifier = Modifier.size(48.dp).background(Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}
