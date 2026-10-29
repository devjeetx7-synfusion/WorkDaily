package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.synfusion.attendance.live.model.*
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostControlsSheet(
    room: LiveRoom,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onEndLiveConfirmed: suspend () -> LiveOperationResult<Unit>
) {
    val guestRequestsMap by viewModel.guestRequestsMap.collectAsState()
    val guestRequests = guestRequestsMap[room.id] ?: emptyList()
    val pendingRequests = guestRequests.filter { it.status == GuestRequestStatus.PENDING }

    var selectedTab by remember { mutableStateOf("Controls") }
    var micEnabled by remember { mutableStateOf(true) }
    var cameraEnabled by remember { mutableStateOf(true) }
    var showEndConfirmDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Controls", "Guests (${pendingRequests.size})", "Moderation")
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = LivePrimaryEmerald)
                    Text("Host Stream Controls", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                tabs.forEach { tab ->
                    val tabName = tab.split(" ").first()
                    val isSelected = selectedTab.startsWith(tabName)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) LivePrimaryEmerald else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedTab.startsWith("Controls")) {
                // Media & Stream Toggles
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (room.type == LiveRoomType.VIDEO || room.type == LiveRoomType.AUDIO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { micEnabled = !micEnabled },
                                color = if (micEnabled) LivePrimaryEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (micEnabled) LivePrimaryEmerald else Color.Gray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff, contentDescription = null, tint = LivePrimaryEmerald)
                                    Text(if (micEnabled) "Mic On" else "Mic Muted", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (room.type == LiveRoomType.VIDEO) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { cameraEnabled = !cameraEnabled },
                                    color = if (cameraEnabled) LivePrimaryEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (cameraEnabled) LivePrimaryEmerald else Color.Gray)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, contentDescription = null, tint = LivePrimaryEmerald)
                                        Text(if (cameraEnabled) "Cam On" else "Cam Off", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Chat Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = LivePrimaryEmerald)
                            Text("Allow Viewers Chat", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = room.isChatEnabled,
                            onCheckedChange = { viewModel.toggleChatEnabled(room.id) }
                        )
                    }

                    // End Stream CTA Button
                    Button(
                        onClick = { showEndConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End Live Stream", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (selectedTab.startsWith("Guests")) {
                // Pending Guest Requests List
                if (pendingRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No pending guest join requests", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pendingRequests) { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(req.userName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(req.userProfession, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.approveGuestRequest(room.id, req.id)
                                                Toast.makeText(context, "Approved ${req.userName}!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.rejectGuestRequest(room.id, req.id)
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Decline", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Moderation Options
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chat & Room Moderation Actions", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Button(
                        onClick = {
                            viewModel.toggleChatEnabled(room.id)
                            Toast.makeText(context, if (room.isChatEnabled) "Chat Disabled" else "Chat Enabled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (room.isChatEnabled) Icons.Default.SpeakerNotesOff else Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (room.isChatEnabled) "Disable Chat For Viewers" else "Enable Chat For Viewers", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.unpinChatMessage(room.id)
                            Toast.makeText(context, "Unpinned active comment", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Pinned Comment", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showEndConfirmDialog) {
            val coroutineScope = rememberCoroutineScope()
            var isEnding by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isEnding) showEndConfirmDialog = false },
                title = { Text("End Live Stream?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to end your live stream? A summary of views and earnings will be generated.") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isEnding) {
                                isEnding = true
                                coroutineScope.launch {
                                    val result = onEndLiveConfirmed()
                                    isEnding = false
                                    if (result is LiveOperationResult.Success) {
                                        showEndConfirmDialog = false
                                        onDismiss()
                                    } else if (result is LiveOperationResult.Error) {
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isEnding,
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                    ) {
                        Text(if (isEnding) "Ending..." else "End Stream Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEndConfirmDialog = false },
                        enabled = !isEnding
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
