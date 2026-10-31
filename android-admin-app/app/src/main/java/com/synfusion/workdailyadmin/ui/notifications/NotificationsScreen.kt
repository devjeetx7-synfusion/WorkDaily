package com.synfusion.workdailyadmin.ui.notifications

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
import com.synfusion.workdailyadmin.ui.components.StatusChip
import com.synfusion.workdailyadmin.ui.components.TruncatedIdChip

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
                title = { Text("FCM Notification Campaigns", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadCampaigns() },
                        enabled = uiState !is NotificationsUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Campaign")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (val state = uiState) {
                is NotificationsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NotificationsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadCampaigns() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is NotificationsUiState.Success -> {
                    if (state.campaigns.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No notification campaigns found.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.campaigns, key = { it.id }) { campaign ->
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
                                                text = campaign.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            StatusChip(text = campaign.status, color = Color(0xFF10B981))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = campaign.body, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Audience: ${campaign.targetAudience}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            TruncatedIdChip(id = campaign.id, prefix = "ID: ", maxLength = 12)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showCreateDialog = false },
                    title = { Text("New Notification Campaign") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = bodyInput,
                                onValueChange = { bodyInput = it },
                                label = { Text("Message Body") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = audienceInput,
                                onValueChange = { audienceInput = it },
                                label = { Text("Target Audience (ALL)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (titleInput.isNotBlank() && bodyInput.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    viewModel.sendCampaign(titleInput, bodyInput, audienceInput) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showCreateDialog = false
                                            titleInput = ""
                                            bodyInput = ""
                                        }
                                    }
                                }
                            },
                            enabled = titleInput.isNotBlank() && bodyInput.isNotBlank() && !isSubmitting
                        ) {
                            Text("Send Push Broadcast")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showCreateDialog = false }, enabled = !isSubmitting) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
