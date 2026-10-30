package com.synfusion.workdailyadmin.ui.live

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
import com.synfusion.workdailyadmin.data.models.LiveRoomItem
import com.synfusion.workdailyadmin.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveManagementScreen(
    viewModel: LiveViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEndDialog by remember { mutableStateOf(false) }
    var selectedRoomId by remember { mutableStateOf("") }
    var endReason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Streams Oversight", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadLiveRooms() },
                        enabled = uiState !is LiveUiState.Loading
                    ) {
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
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is LiveUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching active broadcast rooms...")
                    }
                }
                is LiveUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadLiveRooms() }) {
                            Text("Retry")
                        }
                    }
                }
                is LiveUiState.Success -> {
                    if (state.rooms.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No active live streams at present.", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.rooms) { room ->
                                LiveRoomCard(
                                    room = room,
                                    onEndRoom = {
                                        selectedRoomId = room.roomId
                                        showEndDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showEndDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showEndDialog = false },
                    title = { Text("Terminate Live Stream", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Provide mandatory termination reason for audit logs:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = endReason,
                                onValueChange = { endReason = it },
                                placeholder = { Text("Reason...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (endReason.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    viewModel.endRoom(selectedRoomId, endReason.trim()) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showEndDialog = false
                                            endReason = ""
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            enabled = endReason.isNotBlank() && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onError
                                )
                            } else {
                                Text("Terminate Room")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showEndDialog = false },
                            enabled = !isSubmitting
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LiveRoomCard(
    room: LiveRoomItem,
    onEndRoom: () -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.title.ifBlank { "Untitled Stream" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Host UID: ${room.hostUserId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                StatusChip(text = room.type, color = Color(0xFFEC4899))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Room ID: ${room.roomId}",
                    style = MaterialTheme.typography.labelSmall
                )
                Button(
                    onClick = onEndRoom,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("End Room")
                }
            }
        }
    }
}
