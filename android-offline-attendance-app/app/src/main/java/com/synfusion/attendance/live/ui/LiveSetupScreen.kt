package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

fun extractYouTubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val regex = "(?<=watch\\?v=|/videos/|embed\\/|youtu\\.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu\\.be%2F|%2Fv%2F)[^#\\&\\?\\n]*".toRegex()
    val match = regex.find(url)
    return if (match != null && match.value.length == 11) match.value else if (url.trim().length == 11) url.trim() else null
}

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
    var isFrontCamera by remember { mutableStateOf(true) }
    var youtubeUrl by remember { mutableStateOf("") }
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
        // Immersive Full-Screen Stage Canvas Surface
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
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            when (selectedType) {
                                LiveRoomType.VIDEO -> LivePrimaryEmerald.copy(alpha = 0.2f)
                                LiveRoomType.AUDIO -> LivePurple.copy(alpha = 0.2f)
                                LiveRoomType.TEXT -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFEF4444).copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedType) {
                            LiveRoomType.VIDEO -> Icons.Default.Videocam
                            LiveRoomType.AUDIO -> Icons.Default.Mic
                            LiveRoomType.TEXT -> Icons.Default.ChatBubble
                            LiveRoomType.YOUTUBE_WATCH_PARTY -> Icons.Default.OndemandVideo
                        },
                        contentDescription = null,
                        tint = when (selectedType) {
                            LiveRoomType.VIDEO -> LivePrimaryMint
                            LiveRoomType.AUDIO -> LivePurple
                            LiveRoomType.TEXT -> Color(0xFF60A5FA)
                            LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFF87171)
                        },
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = when (selectedType) {
                        LiveRoomType.VIDEO -> "Camera Preview Stage"
                        LiveRoomType.AUDIO -> "Voice Room Stage"
                        LiveRoomType.TEXT -> "Group Text Live Chat Stage"
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> "YouTube Watch Party Stage"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selectedType == LiveRoomType.YOUTUBE_WATCH_PARTY) {
                    OutlinedTextField(
                        value = youtubeUrl,
                        onValueChange = { youtubeUrl = it },
                        placeholder = { Text("Paste YouTube Video URL or Video ID", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    val videoId = extractYouTubeVideoId(youtubeUrl)
                    if (youtubeUrl.isNotBlank()) {
                        Text(
                            text = if (videoId != null) "✓ Valid Video ID: $videoId" else "⚠️ Invalid YouTube URL or ID",
                            color = if (videoId != null) LivePrimaryMint else Color(0xFFF87171),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                LiveBadge(text = "PREVIEW MODE", bgColor = LivePrimaryEmerald)
            }
        }

        // Top Bar Overlay
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Bottom Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Switcher Pill (Video, Audio, Text, YouTube)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                LiveRoomType.values().forEach { mode ->
                    val isSelected = selectedType == mode
                    val label = when (mode) {
                        LiveRoomType.VIDEO -> "Video"
                        LiveRoomType.AUDIO -> "Audio"
                        LiveRoomType.TEXT -> "Text"
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> "YouTube"
                    }
                    val bg = if (isSelected) {
                        when (mode) {
                            LiveRoomType.VIDEO -> LivePrimaryEmerald
                            LiveRoomType.AUDIO -> LivePurple
                            LiveRoomType.TEXT -> Color(0xFF2563EB)
                            LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFDC2626)
                        }
                    } else Color.Transparent

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .clickable { selectedType = mode }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Actions based on Mode
            if (selectedType == LiveRoomType.VIDEO || selectedType == LiveRoomType.AUDIO) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { micEnabled = !micEnabled },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (micEnabled) Color.White.copy(alpha = 0.25f) else LiveRed)
                    ) {
                        Icon(
                            imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Microphone Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (selectedType == LiveRoomType.VIDEO) {
                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Flip Camera",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Start Live CTA Button
            Button(
                onClick = {
                    if (isSubmitting) return@Button

                    val videoId = if (selectedType == LiveRoomType.YOUTUBE_WATCH_PARTY) extractYouTubeVideoId(youtubeUrl) else null
                    if (selectedType == LiveRoomType.YOUTUBE_WATCH_PARTY && videoId == null) {
                        Toast.makeText(context, "Please enter a valid YouTube video URL or ID", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true

                    val autoTitle = when (selectedType) {
                        LiveRoomType.VIDEO -> "Live Site Workday Broadcast"
                        LiveRoomType.AUDIO -> "Live Site Voice Discussion"
                        LiveRoomType.TEXT -> "Live Site Group Text Chat"
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> "Site Safety YouTube Watch Party"
                    }

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
                    .height(50.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        LiveRoomType.VIDEO -> LivePrimaryEmerald
                        LiveRoomType.AUDIO -> LivePurple
                        LiveRoomType.TEXT -> Color(0xFF2563EB)
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFDC2626)
                    },
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Live", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Room Settings Modal Bottom Sheet
        if (showAdvancedSettings) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedSettings = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
