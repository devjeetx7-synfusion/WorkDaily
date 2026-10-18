package com.synfusion.attendance.live.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.*
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomScreen(
    roomId: String,
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit
) {
    val liveRooms by viewModel.liveRooms.collectAsState()
    val chatMessagesMap by viewModel.chatMessagesMap.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()
    val wallet by viewModel.wallet.collectAsState()

    val room = liveRooms.find { it.id == roomId } ?: liveRooms.firstOrNull() ?: LiveRoom(
        id = roomId,
        title = "Live Site Stream",
        creatorId = "creator_1",
        creatorName = "Rajesh Sharma",
        creatorAvatar = "",
        category = "Site Safety",
        type = LiveRoomType.VIDEO,
        viewerCount = 240
    )

    val chatMessages = chatMessagesMap[room.id] ?: emptyList()
    val isFollowing = followedCreatorIds.contains(room.creatorId)

    var showGiftSheet by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var likeCount by remember { mutableStateOf(128) }

    LaunchedEffect(room.id) {
        viewModel.setActiveRoom(room.id)
    }

    Scaffold(
        containerColor = LiveDarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Simulated Stream Background / Stage Canvas
            if (room.type == LiveRoomType.VIDEO) {
                VideoStageCanvas(room = room)
            } else {
                AudioStageCanvas(room = room)
            }

            // Top Overlay Bar: Creator Header, Viewer Count, Diamond Balance, Leave Room
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Creator Profile Header + Follow Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryMint.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PulsingLiveAvatar(
                            name = room.creatorName,
                            size = 32.dp,
                            isLive = room.isLive
                        )

                        Column {
                            Text(
                                text = room.creatorName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(10.dp))
                                Text("${room.diamondEarnings}", fontSize = 10.sp, color = LiveDiamondColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        FollowButton(
                            isFollowing = isFollowing,
                            onToggle = { viewModel.toggleFollowCreator(room.creatorId) }
                        )
                    }
                }

                // Right Controls: Viewer Count & Exit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewerCountChip(count = room.viewerCount)

                    DiamondBalanceChip(
                        balance = wallet.diamondBalance,
                        onClick = onNavigateWallet,
                        fontSize = 11
                    )

                    IconButton(
                        onClick = {
                            viewModel.setActiveRoom(null)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Leave Stream", tint = Color.White)
                    }
                }
            }

            // Bottom Chat Messages & Controls Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chat Messages Feed
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chatMessages.takeLast(10)) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }

                // Chat Input & Floating Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Chat Input Box
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Comment in live stream...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                            focusedBorderColor = LivePrimaryMint,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        trailingIcon = {
                            if (chatInput.isNotBlank()) {
                                IconButton(onClick = {
                                    viewModel.sendChatMessage(room.id, chatInput)
                                    chatInput = ""
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = "Send Chat", tint = LivePrimaryMint)
                                }
                            }
                        },
                        singleLine = true
                    )

                    // Like Floating Heart Button
                    IconButton(
                        onClick = { likeCount++ },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LiveRed.copy(alpha = 0.85f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Gift Store Button
                    Button(
                        onClick = { showGiftSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = "Gift Store", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gift", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Gift Store Modal Bottom Sheet
            if (showGiftSheet) {
                GiftStoreBottomSheet(
                    roomId = room.id,
                    viewModel = viewModel,
                    onDismiss = { showGiftSheet = false },
                    onNavigateWallet = onNavigateWallet
                )
            }
        }
    }
}

@Composable
fun VideoStageCanvas(room: LiveRoom) {
    val infiniteTransition = rememberInfiniteTransition(label = "videoAnim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "gradientRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LiveDarkBg,
                        Color(0xFF1E1B4B),
                        Color(0xFF064E3B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(LivePrimaryEmerald.copy(alpha = 0.2f))
                    .border(2.dp, LivePrimaryMint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = LivePrimaryMint,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = room.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            RoomCategoryChip(category = room.category)

            Text(
                text = "Simulated Live Site Feed • 1080p 60fps",
                fontSize = 11.sp,
                color = Color.LightGray.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AudioStageCanvas(room: LiveRoom) {
    val speakers = listOf(
        Pair(room.creatorName, true),
        Pair("Amit Engineer", false),
        Pair("Suresh Contractor", false),
        Pair("Pooja Safety", true)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LiveDarkBg,
                        Color(0xFF2E1065),
                        LiveDarkBg
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = LivePurple, modifier = Modifier.size(24.dp))
                Text("Audio Stage Discussion", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LivePurple)
            }

            Text(
                text = room.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Speaker Stage Grid (4 Seats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                speakers.forEach { (speakerName, isSpeaking) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PulsingLiveAvatar(
                                name = speakerName,
                                size = 52.dp,
                                isLive = isSpeaking
                            )
                            if (isSpeaking) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(LivePrimaryMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Text(
                            text = speakerName.split(" ").firstOrNull() ?: speakerName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    if (message.isGiftAnnouncement) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(LiveGold.copy(alpha = 0.2f))
                .border(1.dp, LiveGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = LiveGold, modifier = Modifier.size(14.dp))
                Text(
                    text = "${message.senderName} ${message.message}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LiveGold
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LivePrimaryMint
                )
                Text(
                    text = message.message,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}
