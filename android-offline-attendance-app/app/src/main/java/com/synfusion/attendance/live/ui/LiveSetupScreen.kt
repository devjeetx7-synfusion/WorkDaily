package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSetupScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit
) {
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(LiveRoomType.VIDEO) }
    var micEnabled by remember { mutableStateOf(true) }
    var cameraEnabled by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }
    var isChatEnabled by remember { mutableStateOf(true) }
    var allowGuestRequests by remember { mutableStateOf(true) }
    var showGifts by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Immersive Simulated Video/Audio Camera Preview Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1F2937),
                            Color(0xFF111827),
                            Color(0xFF000000)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedType == LiveRoomType.VIDEO) LivePrimaryEmerald.copy(alpha = 0.2f)
                            else LivePurple.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedType == LiveRoomType.VIDEO) Icons.Default.Videocam else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (selectedType == LiveRoomType.VIDEO) LivePrimaryMint else LivePurple,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = if (selectedType == LiveRoomType.VIDEO) "Camera Preview Simulation" else "Voice Room Stage",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                LiveBadge(text = "DEMO PREVIEW", bgColor = LivePrimaryEmerald)
            }
        }

        // Top Bar Overlay (Close, Go Live Label, Settings Icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Text(
                text = "Go Live",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { showAdvancedSettings = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Bottom Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Mode Switch Pill (Video / Audio)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedType == LiveRoomType.VIDEO) LivePrimaryEmerald else Color.Transparent)
                        .clickable { selectedType = LiveRoomType.VIDEO }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Video",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selectedType == LiveRoomType.VIDEO) FontWeight.Bold else FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedType == LiveRoomType.AUDIO) LivePurple else Color.Transparent)
                        .clickable { selectedType = LiveRoomType.AUDIO }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Audio",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selectedType == LiveRoomType.AUDIO) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Mic and Camera Flip Quick Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { micEnabled = !micEnabled },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (micEnabled) Color.White.copy(alpha = 0.2f) else LiveRed)
                ) {
                    Icon(
                        imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone Toggle",
                        tint = Color.White
                    )
                }

                if (selectedType == LiveRoomType.VIDEO) {
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = Color.White
                        )
                    }
                }
            }

            // Start Live Direct CTA
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true

                    val autoTitle = if (selectedType == LiveRoomType.VIDEO) "Live Site Workday Broadcast" else "Live Site Voice Discussion"
                    val createdRoom = viewModel.startLiveStream(
                        title = autoTitle,
                        category = "Site Safety",
                        type = selectedType,
                        tags = listOf("LiveWork", "SiteSafety"),
                        description = "",
                        isPublic = isPublic,
                        allowGuestRequests = allowGuestRequests,
                        showGifts = showGifts
                    )

                    Toast.makeText(context, "You are LIVE! 🔴", Toast.LENGTH_SHORT).show()
                    onNavigateLiveRoom(createdRoom.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Podcasts, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Live", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Optional Secondary Settings Bottom Sheet
        if (showAdvancedSettings) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedSettings = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Room Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Public Stream", fontSize = 14.sp)
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Live Chat", fontSize = 14.sp)
                        Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Guest Requests", fontSize = 14.sp)
                        Switch(checked = allowGuestRequests, onCheckedChange = { allowGuestRequests = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Gifts", fontSize = 14.sp)
                        Switch(checked = showGifts, onCheckedChange = { showGifts = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
