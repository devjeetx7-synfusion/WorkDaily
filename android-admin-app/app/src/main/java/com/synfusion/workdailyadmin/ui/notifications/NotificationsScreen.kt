package com.synfusion.workdailyadmin.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.NotificationCampaign
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var audienceInput by remember { mutableStateOf("ALL") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FCM Notification Campaigns", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Targeted push broadcasts & user engagement alerts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadCampaigns() },
                        enabled = uiState !is NotificationsUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Campaigns")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Campaign")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (val state = uiState) {
                is NotificationsUiState.Loading -> {
                    AdminLoadingState(message = "Loading notification campaigns history...")
                }
                is NotificationsUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadCampaigns() }
                    )
                }
                is NotificationsUiState.Success -> {
                    if (state.campaigns.isEmpty()) {
                        AdminEmptyState(
                            title = "No Notification Campaigns",
                            description = "No broadcast campaigns have been launched yet. Tap the + button to dispatch your first FCM push broadcast.",
                            icon = Icons.Default.Campaign,
                            actionText = "Create Campaign",
                            onActionClick = { showCreateDialog = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.campaigns, key = { it.id }) { campaign ->
                                NotificationCampaignCard(campaign = campaign)
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showCreateDialog = false },
                    title = { Text("Broadcast FCM Campaign", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Dispatch a cloud notification to all active devices on WorkDaily:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Campaign Title") },
                                placeholder = { Text("e.g. Daily Attendance Reminder") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = bodyInput,
                                onValueChange = { bodyInput = it },
                                label = { Text("Message Body") },
                                placeholder = { Text("e.g. Don't forget to mark your daily attendance today!") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Target Audience:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = audienceInput == "ALL",
                                    onClick = { audienceInput = "ALL" },
                                    label = { Text("All Users") }
                                )
                                FilterChip(
                                    selected = audienceInput == "ACTIVE",
                                    onClick = { audienceInput = "ACTIVE" },
                                    label = { Text("Active") }
                                )
                                FilterChip(
                                    selected = audienceInput == "INACTIVE",
                                    onClick = { audienceInput = "INACTIVE" },
                                    label = { Text("Inactive") }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (titleInput.isNotBlank() && bodyInput.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.sendCampaign(titleInput.trim(), bodyInput.trim(), audienceInput) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showCreateDialog = false
                                            titleInput = ""
                                            bodyInput = ""
                                            audienceInput = "ALL"
                                        }
                                    }
                                }
                            },
                            enabled = titleInput.isNotBlank() && bodyInput.isNotBlank() && !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Send Push Broadcast")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showCreateDialog = false },
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
fun NotificationCampaignCard(
    campaign: NotificationCampaign
) {
    val dateStr = remember(campaign.createdAt) {
        if (campaign.createdAt > 0) {
            SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(campaign.createdAt))
        } else "Recent"
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
                    StatusChip(
                        text = campaign.status,
                        color = if (campaign.status == "COMPLETED") Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Audience: ${campaign.targetAudience}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = campaign.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = campaign.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            TruncatedIdChip(id = campaign.id, prefix = "Campaign ID: ")
        }
    }
}
