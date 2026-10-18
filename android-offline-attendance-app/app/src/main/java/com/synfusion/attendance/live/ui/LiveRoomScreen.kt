package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    onNavigateWallet: () -> Unit,
    onNavigateSummary: (String) -> Unit = {}
) {
    val liveRooms by viewModel.liveRooms.collectAsState()
    val chatMessagesMap by viewModel.chatMessagesMap.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val audioSeatsMap by viewModel.audioSeatsMap.collectAsState()

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
    val isHost = room.isHost || room.creatorId == "creator_me"
    val audioSeats = audioSeatsMap[room.id] ?: emptyList()

    var showGiftSheet by remember { mutableStateOf(false) }
    var showHostControlsSheet by remember { mutableStateOf(false) }
    var showViewerListSheet by remember { mutableStateOf(false) }

    var chatInput by remember { mutableStateOf("") }
    var triggerHeartSignal by remember { mutableLongStateOf(0L) }
    var showGiftSentDialog by remember { mutableStateOf<Pair<LiveGift, Int>?>(null) }

    val context = LocalContext.current
    val density = LocalDensity.current

    // IME / Keyboard Visibility Detection
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    val chatListState = rememberLazyListState()

    LaunchedEffect(room.id) {
        viewModel.setActiveRoom(room.id)
    }

    // Auto-scroll chat feed to latest message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = LiveDarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Simulated Stream Background / Stage Canvas with Double-Tap Reaction Gesture
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                triggerHeartSignal = System.currentTimeMillis()
                            }
                        )
                    }
            ) {
                if (room.type == LiveRoomType.VIDEO) {
                    VideoStageCanvas(room = room)
                } else {
                    AudioStageCanvas(
                        room = room,
                        seats = audioSeats,
                        onSeatClick = { seatIndex ->
                            if (isHost) {
                                viewModel.toggleAudioSeatMute(room.id, seatIndex)
                            } else {
                                viewModel.occupyAudioSeat(room.id, seatIndex, "user_me", "You")
                                Toast.makeText(context, "Joined Audio Seat #${seatIndex + 1}!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Floating Heart Particle Overlay
            FloatingHeartWaveOverlay(
                triggerHeartSignal = triggerHeartSignal,
                onSendHearts = { count ->
                    viewModel.sendHearts(room.id, count)
                }
            )

            // TOP OVERLAY BAR: Compact Header (Avatar, Ellipsized Name, Follow/Host, Viewer Count, Exit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pill: Creator Profile + Ellipsized Name + Follow
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryMint.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PulsingLiveAvatar(
                            name = room.creatorName,
                            size = 30.dp,
                            isLive = room.isLive
                        )

                        Column(modifier = Modifier.widthIn(max = 110.dp)) {
                            Text(
                                text = room.creatorName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(9.dp))
                                Text("${room.diamondEarnings}", fontSize = 9.sp, color = LiveDiamondColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isHost) {
                            FollowButton(
                                isFollowing = isFollowing,
                                onToggle = { viewModel.toggleFollowCreator(room.creatorId) }
                            )
                        } else {
                            LiveBadge(text = "HOST", bgColor = LivePrimaryEmerald)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right Controls: Viewer Count, Diamond Balance, Host / Exit Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ViewerCountChip(
                        count = room.viewerCount,
                        onClick = { showViewerListSheet = true }
                    )

                    DiamondBalanceChip(
                        balance = wallet.diamondBalance,
                        onClick = onNavigateWallet,
                        fontSize = 11
                    )

                    if (isHost) {
                        IconButton(
                            onClick = { showHostControlsSheet = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LivePrimaryEmerald)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Host Controls", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.setActiveRoom(null)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Leave Stream", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // RIGHT SIDE ACTION BUTTONS: Hide completely when keyboard is visible to prevent layout jumps & overlaps
            AnimatedVisibility(
                visible = !isKeyboardVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Heart Wave Trigger Button
                    IconButton(
                        onClick = {
                            triggerHeartSignal = System.currentTimeMillis()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LiveRed)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Send Heart", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Gift Store Trigger Button
                    IconButton(
                        onClick = { showGiftSheet = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LiveGold)
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = "Gift Store", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }

                    // Raise Hand (Audio) / Request Guest Button for Viewers
                    if (!isHost) {
                        IconButton(
                            onClick = {
                                viewModel.requestToJoinGuest(room.id)
                                Toast.makeText(context, "Guest join request sent to host!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, LivePrimaryMint, CircleShape)
                        ) {
                            Icon(Icons.Default.PanTool, contentDescription = "Raise Hand", tint = LivePrimaryMint, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Share Button
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Stream link copied to clipboard! 🔗", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // BOTTOM OVERLAY: Transparent Floating Chat Feed + Composer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()

                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pinned Comment Banner if present
                room.pinnedMessage?.let { pinnedMsg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = LiveGold.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LiveGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = LiveGold, modifier = Modifier.size(14.dp))
                            Text("PINNED: ", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LiveGold)
                            Text("${pinnedMsg.senderName}: ${pinnedMsg.message}", fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Chat Feed overlay with smooth auto-scrolling
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .fillMaxWidth(if (isKeyboardVisible) 1.0f else 0.82f)
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chatMessages) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }

                // Input Bar: Single clean row docked above keyboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Say something...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.65f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.65f),
                            focusedBorderColor = LivePrimaryMint,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.35f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        trailingIcon = {
                            if (chatInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        viewModel.sendChatMessage(room.id, chatInput)
                                        chatInput = ""
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = LivePrimaryMint, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true
                    )

                    // Secondary Gift Button in bottom row - visible ONLY when keyboard is CLOSED to avoid duplication
                    if (!isKeyboardVisible) {
                        Button(
                            onClick = { showGiftSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gift", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Gift Store Bottom Sheet
            if (showGiftSheet) {
                GiftStoreBottomSheet(
                    roomId = room.id,
                    viewModel = viewModel,
                    onDismiss = { showGiftSheet = false },
                    onNavigateWallet = onNavigateWallet,
                    onGiftSent = { gift, qty ->
                        showGiftSentDialog = Pair(gift, qty)
                    }
                )
            }

            // Host Controls Bottom Sheet
            if (showHostControlsSheet) {
                HostControlsSheet(
                    room = room,
                    viewModel = viewModel,
                    onDismiss = { showHostControlsSheet = false },
                    onEndLiveConfirmed = {
                        val summary = viewModel.endLiveStream(room.id)
                        onNavigateBack()
                    }
                )
            }

            // Viewer List Bottom Sheet
            if (showViewerListSheet) {
                ViewerListSheet(
                    room = room,
                    viewModel = viewModel,
                    onDismiss = { showViewerListSheet = false }
                )
            }

            // Gift Celebration Dialog
            showGiftSentDialog?.let { (gift, qty) ->
                GiftCongratulationsDialog(
                    gift = gift,
                    quantity = qty,
                    recipientName = room.creatorName,
                    totalDiamondsSpent = gift.diamondCost * qty,
                    onDismiss = { showGiftSentDialog = null }
                )
            }
        }
    }
}

@Composable
fun VideoStageCanvas(room: LiveRoom) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(LivePrimaryEmerald.copy(alpha = 0.2f))
                    .border(2.dp, LivePrimaryMint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = LivePrimaryMint,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = room.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            RoomCategoryChip(category = room.category)

            Text(
                text = "DEMO LIVE • Simulated Site Broadcast",
                fontSize = 11.sp,
                color = Color.LightGray.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AudioStageCanvas(
    room: LiveRoom,
    seats: List<AudioSeat>,
    onSeatClick: (Int) -> Unit
) {
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
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = LivePurple, modifier = Modifier.size(24.dp))
                Text("Audio Stage Discussion", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LivePurple)
            }

            Text(
                text = room.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 6-Seat Speaker Grid
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                seats.chunked(3).forEach { rowSeats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowSeats.forEach { seat ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable { onSeatClick(seat.seatIndex) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (seat.userName != null) {
                                        PulsingLiveAvatar(
                                            name = seat.userName,
                                            size = 48.dp,
                                            isLive = seat.isSpeaking
                                        )
                                        if (seat.isMuted) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(LiveRed),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.MicOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.1f))
                                                .border(1.dp, LivePurple.copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Empty Seat", tint = LivePurple, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = seat.userName?.split(" ")?.firstOrNull() ?: "Seat ${seat.seatIndex + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (seat.userName != null) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerListSheet(
    room: LiveRoom,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit
) {
    val creators by viewModel.creators.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LiveDarkBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Room Viewers (${room.viewerCount})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(creators) { creator ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiveSurfaceDark)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PulsingLiveAvatar(name = creator.name, size = 32.dp, isLive = false)
                            Column {
                                Text(creator.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(creator.profession, fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        LiveBadge(text = "VIEWER", bgColor = LivePrimaryEmerald)
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
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = LiveGold, modifier = Modifier.size(14.dp))
                Text(
                    text = "${message.senderName} ${message.message}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LiveGold
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
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
