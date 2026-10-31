package com.synfusion.workdailyadmin.ui.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.synfusion.workdailyadmin.data.models.LiveRoomItem
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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
                title = {
                    Column {
                        Text("Live Streams Oversight", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Real-time broadcast supervision & moderation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadLiveRooms() },
                        enabled = uiState !is LiveUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Rooms")
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
                is LiveUiState.Loading -> {
                    AdminLoadingState(message = "Querying live broadcasting sessions...")
                }
                is LiveUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadLiveRooms() }
                    )
                }
                is LiveUiState.Success -> {
                    if (state.rooms.isEmpty()) {
                        AdminEmptyState(
                            title = "No Live Broadcasts Found",
                            description = "There are no active or recorded LiveKit broadcast rooms on the platform.",
                            icon = Icons.Default.LiveTv,
                            actionText = "Refresh",
                            onActionClick = { viewModel.loadLiveRooms() }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.rooms, key = { it.roomId }) { room ->
                                LiveRoomCard(
                                    room = room,
                                    onTerminate = {
                                        selectedRoomId = room.roomId
                                        endReason = ""
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
                    title = { Text("Terminate Live Broadcast", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Are you sure you want to forcibly terminate room \"$selectedRoomId\"?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = endReason,
                                onValueChange = { endReason = it },
                                placeholder = { Text("Violation / termination reason...") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (endReason.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.endRoom(selectedRoomId, endReason) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showEndDialog = false
                                        }
                                    }
                                }
                            },
                            enabled = endReason.isNotBlank() && !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Terminate Room")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showEndDialog = false },
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
fun LiveRoomCard(
    room: LiveRoomItem,
    onTerminate: () -> Unit
) {
    val createdDateStr = remember(room.createdAtMillis) {
        if (room.createdAtMillis > 0) {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(room.createdAtMillis))
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
                    if (room.isLive) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFEF4444), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusChip(text = "LIVE NOW", color = Color(0xFFEF4444))
                    } else {
                        StatusChip(text = "ENDED", color = Color(0xFF6B7280))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = room.type,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = createdDateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = room.title.ifBlank { "Untitled Broadcast Session" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    TruncatedIdChip(id = room.roomId, prefix = "Room: ", maxLength = 14)
                    Spacer(modifier = Modifier.height(4.dp))
                    TruncatedIdChip(id = room.hostUserId, prefix = "Host UID: ", maxLength = 14)
                }

                if (room.isLive) {
                    Button(
                        onClick = onTerminate,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terminate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (!room.endReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "End Reason: ${room.endReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
