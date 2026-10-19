package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.*
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomScreen(
    roomId: String,
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateSummary: (String) -> Unit = {},
    onNavigatePrivateChat: (String) -> Unit = {}
) {
    val liveRooms by viewModel.liveRooms.collectAsState()
    val chatMessagesMap by viewModel.chatMessagesMap.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()
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
    var selectedProfileUserId by remember { mutableStateOf<String?>(null) }

    var chatInput by remember { mutableStateOf("") }
    var triggerHeartSignal by remember { mutableLongStateOf(0L) }
    var showGiftSentDialog by remember { mutableStateOf<Pair<LiveGift, Int>?>(null) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Keyboard Visibility Detection
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    // Clear focus on back press when keyboard is open
    BackHandler(enabled = isKeyboardVisible) {
        focusManager.clearFocus()
    }

    val chatListState = rememberLazyListState()

    // Intelligent auto-scroll derived state
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = chatListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) true
            else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 2
            }
        }
    }

    var userScrolledUp by remember { mutableStateOf(false) }

    LaunchedEffect(room.id) {
        viewModel.setActiveRoom(room.id)
    }

    // Detect user manual scroll upward
    LaunchedEffect(chatListState.isScrollInProgress) {
        if (chatListState.isScrollInProgress) {
            userScrolledUp = !isAtBottom
        }
    }

    // Intelligent Auto-scroll to latest comments
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty() && !userScrolledUp) {
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
            // Layer 1: Edge-to-Edge Video / Audio Broadcast Stage
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

            // Layer 2: Top Readability Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Layer 3: Minimal Floating Creator Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Floating Creator Header Surface
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { selectedProfileUserId = room.creatorId }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val initials = remember(room.creatorName) {
                            val parts = room.creatorName.trim().split(" ")
                            if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                            else if (room.creatorName.isNotBlank()) room.creatorName.take(2).uppercase()
                            else "WD"
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(LivePrimaryEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.widthIn(max = 110.dp)
                        ) {
                            Text(
                                text = room.creatorName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                        }

                        if (!isHost) {
                            FollowButton(
                                isFollowing = isFollowing,
                                onToggle = { viewModel.toggleFollowCreator(room.creatorId) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right Actions: Viewer Count Chip + Host Controls + Minimal Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ViewerCountChip(
                        count = room.viewerCount,
                        onClick = { showViewerListSheet = true }
                    )

                    if (isHost) {
                        IconButton(
                            onClick = { showHostControlsSheet = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Host Controls", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.setActiveRoom(null)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Stream", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Layer 4: Floating Hearts Overlay Canvas
            FloatingHeartWaveOverlay(
                triggerHeartSignal = triggerHeartSignal,
                onSendHearts = { count ->
                    viewModel.sendHearts(room.id, count)
                }
            )

            // Layer 5: Floating Live Chat Feed & Compact Pill-Shaped Comment Composer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Floating Comments Feed overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 210.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = chatMessages,
                            key = { it.id }
                        ) { msg ->
                            LiveCommentItem(
                                message = msg,
                                onUserClick = { selectedProfileUserId = it }
                            )
                        }
                    }

                    // "New Messages ↓" indicator
                    if (userScrolledUp && !isAtBottom) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LivePrimaryEmerald,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                                .clickable {
                                    userScrolledUp = false
                                    coroutineScope.launch {
                                        if (chatMessages.isNotEmpty()) {
                                            chatListState.animateScrollToItem(chatMessages.size - 1)
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("New Messages", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Bottom Composer: Compact Semi-Transparent Pill Input (~46dp) + Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Fully pill-shaped translucent input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(23.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (chatInput.isEmpty()) {
                            Text("Comment...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                        BasicTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(LivePrimaryMint)
                        )
                    }

                    // Send Action when typing or active
                    if (isKeyboardVisible || chatInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    viewModel.sendChatMessage(room.id, chatInput.trim())
                                    chatInput = ""
                                    userScrolledUp = false
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = chatInput.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (chatInput.isNotBlank()) LiveRed else Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Comment",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Secondary Compact Action Triggers [Gift, Heart] when idle
                    if (!isKeyboardVisible && chatInput.isBlank()) {
                        IconButton(
                            onClick = { showGiftSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = "Gift Store", tint = LiveGold, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { triggerHeartSignal = System.currentTimeMillis() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Send Heart", tint = LiveRed, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Layer 6: Gift Store Bottom Sheet
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
                        viewModel.endLiveStream(room.id)
                        onNavigateBack()
                    }
                )
            }

            // Live Viewers Bottom Sheet
            if (showViewerListSheet) {
                LiveViewerListBottomSheet(
                    room = room,
                    viewModel = viewModel,
                    onDismiss = { showViewerListSheet = false },
                    onSelectViewerProfile = { viewerId ->
                        showViewerListSheet = false
                        selectedProfileUserId = viewerId
                    }
                )
            }

            // Reusable User Profile Bottom Sheet
            selectedProfileUserId?.let { targetId ->
                LiveProfileBottomSheet(
                    userId = targetId,
                    viewModel = viewModel,
                    onDismiss = { selectedProfileUserId = null },
                    onNavigatePrivateChat = onNavigatePrivateChat
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
                        Color(0xFF0F172A)
                    )
                )
            )
    )
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
