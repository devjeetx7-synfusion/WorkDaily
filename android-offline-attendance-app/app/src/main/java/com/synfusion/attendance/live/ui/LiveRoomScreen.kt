package com.synfusion.attendance.live.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
    onNavigatePrivateChat: (String) -> Unit = {},
    onNavigateFullProfile: (String) -> Unit = {},
    onPagingLockChange: (Boolean) -> Unit = {}
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
    val giftOverlayEvents by viewModel.giftOverlayEvents.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val isFollowing = followedCreatorIds.contains(room.creatorId)
    val isHost = room.isHost || room.creatorId == "creator_me" || room.creatorId == "user_me"
    val audioSeats = audioSeatsMap[room.id] ?: emptyList()

    val roomGiftEvents = remember(giftOverlayEvents, room.id) {
        giftOverlayEvents.filter { it.roomId == room.id }
    }

    var showGiftSheet by remember { mutableStateOf(false) }
    var showInlineGiftTray by remember { mutableStateOf(false) }
    var showHostControlsSheet by remember { mutableStateOf(false) }
    var showViewerListSheet by remember { mutableStateOf(false) }
    var showEndLiveConfirmationDialog by remember { mutableStateOf(false) }
    var selectedProfileUserId by remember { mutableStateOf<String?>(null) }

    var chatInput by remember { mutableStateOf("") }
    var triggerHeartSignal by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Keyboard Visibility Detection
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    // Auto-dismiss inline gift tray when typing starts
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            showInlineGiftTray = false
        }
    }

    // Report lock state to parent VerticalPager container whenever overlays/sheets/inputs are active
    val isInteractionActive = isKeyboardVisible ||
            showInlineGiftTray ||
            showGiftSheet ||
            showHostControlsSheet ||
            showViewerListSheet ||
            selectedProfileUserId != null ||
            showEndLiveConfirmationDialog

    LaunchedEffect(isInteractionActive) {
        onPagingLockChange(isInteractionActive)
    }

    // Back press handling: clear focus if keyboard is open, or request confirmation if host
    BackHandler(enabled = isKeyboardVisible || isHost) {
        if (isKeyboardVisible) {
            focusManager.clearFocus()
        } else if (isHost) {
            showEndLiveConfirmationDialog = true
        } else {
            viewModel.setActiveRoom(null)
            onNavigateBack()
        }
    }

    val chatListState = rememberLazyListState()

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
        viewModel.onUserJoinedRoom(room.id, "You")
    }

    LaunchedEffect(chatListState.isScrollInProgress) {
        if (chatListState.isScrollInProgress) {
            userScrolledUp = !isAtBottom
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty() && !userScrolledUp) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = LiveDarkBg
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val totalScreenHeight = maxHeight
            val maxChatHeightNormal = totalScreenHeight * 0.55f
            val maxChatHeightTray = (totalScreenHeight - 160.dp) * 0.42f
            val dynamicMaxChatHeight = if (showInlineGiftTray || showGiftSheet) {
                maxChatHeightTray.coerceAtLeast(180.dp)
            } else {
                maxChatHeightNormal.coerceAtLeast(280.dp)
            }
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
                when (room.type) {
                    LiveRoomType.VIDEO -> VideoStageCanvas(room = room)
                    LiveRoomType.AUDIO -> AudioStageCanvas(
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
                    LiveRoomType.TEXT -> TextStageCanvas(room = room)
                    LiveRoomType.YOUTUBE_WATCH_PARTY -> YouTubeWatchPartyStageCanvas(
                        room = room,
                        watchPartyState = room.watchPartyState,
                        isHost = isHost,
                        viewModel = viewModel
                    )
                }
            }

            // Layer 2: Top Gradient Scrim
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

            // Layer 3: Floating Creator Header Bar (Clean, Compact, Dynamic First-Name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Compact Host Profile Chip [Avatar] [First Name] [Follow]
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { selectedProfileUserId = room.creatorId }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorkDailyLiveAvatar(
                            name = room.creatorName,
                            avatarUrl = room.creatorAvatar,
                            size = 32.dp,
                            isLive = room.isLive,
                            showBadge = false
                        )

                        val firstName = remember(room.creatorName) {
                            extractFirstName(room.creatorName)
                        }

                        Text(
                            text = firstName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 95.dp)
                        )

                        if (!isHost) {
                            FollowButton(
                                isFollowing = isFollowing,
                                onToggle = { viewModel.toggleFollowCreator(room.creatorId, room.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right Actions: Viewer Count + Share + Host Controls + Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ViewerCountChip(
                        count = room.viewerCount,
                        onClick = { showViewerListSheet = true }
                    )

                    IconButton(
                        onClick = {
                            viewModel.onShareLiveStream(room.id, "You")
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Watch ${room.creatorName} Live on WorkDaily")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join ${room.creatorName}'s live stream '${room.title}' on WorkDaily!\nhttps://workdaily.app/live/${room.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Live Stream"))
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Stream", tint = Color.White, modifier = Modifier.size(15.dp))
                    }

                    if (isHost) {
                        IconButton(
                            onClick = { showHostControlsSheet = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Host Controls", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isHost) {
                                showEndLiveConfirmationDialog = true
                            } else {
                                viewModel.setActiveRoom(null)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Stream", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }

            // Layer 4: Transparent Animated Gift Overlay Engine
            LiveGiftAnimationHost(
                events = roomGiftEvents,
                currentUserId = "current_user",
                soundEnabled = soundEnabled,
                onEventFinished = { eventId ->
                    viewModel.removeGiftOverlayEvent(eventId)
                }
            )

            // Layer 5: Floating Hearts Wave Canvas
            FloatingHeartWaveOverlay(
                triggerHeartSignal = triggerHeartSignal,
                onSendHearts = { count ->
                    viewModel.sendHearts(room.id, count)
                }
            )

            // Layer 5: Floating Live Chat Feed, Inline Gift Tray & Pill Comment Composer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Comments Feed Overlay with Extended Viewport and Continuous Top Alpha Dissolve Mask V3
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(max = dynamicMaxChatHeight)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        0.12f to Color.Black.copy(alpha = 0.10f),
                                        0.28f to Color.Black.copy(alpha = 0.30f),
                                        0.45f to Color.Black.copy(alpha = 0.65f),
                                        0.65f to Color.Black.copy(alpha = 0.90f),
                                        0.78f to Color.Black,
                                        1.00f to Color.Black
                                    )
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
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
                            val isMsgSenderFollowed = followedCreatorIds.contains(msg.senderId)
                            LiveCommentItem(
                                message = msg,
                                onUserClick = { selectedProfileUserId = it },
                                onFollowClick = { senderId -> viewModel.toggleFollowCreator(senderId, room.id) },
                                isFollowingUser = isMsgSenderFollowed,
                                currentUserId = "current_user"
                            )
                        }
                    }

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

                // Expandable Frosted-Glass Gift Panel directly above comment composer
                if ((showInlineGiftTray || showGiftSheet) && !isKeyboardVisible) {
                    ExpandableGiftPanel(
                        roomId = room.id,
                        viewModel = viewModel,
                        onClose = {
                            showInlineGiftTray = false
                            showGiftSheet = false
                        },
                        onNavigateWallet = onNavigateWallet,
                        onGiftSent = { _, _ ->
                            showInlineGiftTray = false
                            showGiftSheet = false
                        }
                    )
                }

                // Unclipped Pill-Shaped Comment Composer (~46dp) + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
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
                                .background(if (chatInput.isNotBlank()) LivePrimaryEmerald else Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Comment",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!isKeyboardVisible && chatInput.isBlank()) {
                        IconButton(
                            onClick = { showInlineGiftTray = !showInlineGiftTray },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (showInlineGiftTray) LiveGold.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = "Gift Store", tint = LiveGold, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { triggerHeartSignal = System.currentTimeMillis() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Send Heart", tint = LiveRed, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Layer 6: Sheets & Dialogs
            if (showGiftSheet) {
                GiftStoreBottomSheet(
                    roomId = room.id,
                    viewModel = viewModel,
                    onDismiss = { showGiftSheet = false },
                    onNavigateWallet = onNavigateWallet,
                    onGiftSent = { _, _ ->
                        showGiftSheet = false
                    }
                )
            }

            if (showHostControlsSheet) {
                HostControlsSheet(
                    room = room,
                    viewModel = viewModel,
                    onDismiss = { showHostControlsSheet = false },
                    onEndLiveConfirmed = {
                        showHostControlsSheet = false
                        viewModel.endLiveStream(room.id)
                        onNavigateSummary(room.id)
                    }
                )
            }

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

            selectedProfileUserId?.let { targetId ->
                LiveProfileBottomSheet(
                    userId = targetId,
                    viewModel = viewModel,
                    onDismiss = { selectedProfileUserId = null },
                    onNavigatePrivateChat = onNavigatePrivateChat,
                    onNavigateFullProfile = onNavigateFullProfile
                )
            }

            // Broadcaster End Live Confirmation AlertDialog
            if (showEndLiveConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showEndLiveConfirmationDialog = false },
                    title = { Text("End Live Broadcast?", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to end this live stream session? Viewers will be notified.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEndLiveConfirmationDialog = false
                                viewModel.endLiveStream(room.id)
                                onNavigateSummary(room.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                        ) {
                            Text("End Live", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndLiveConfirmationDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPartyPlaylistBottomSheet(
    roomId: String,
    watchPartyState: WatchPartyState?,
    isHost: Boolean,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputUrl by remember { mutableStateOf("") }
    var inputTitle by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    val playlist = watchPartyState?.playlist ?: emptyList()
    val activeItemId = watchPartyState?.activePlaylistItemId
    val isMuted = watchPartyState?.isMuted == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        windowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Watch Party Playlist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Mute Toggle
                    IconButton(onClick = { viewModel.toggleMuteWatchParty(roomId) }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Turn Off Watch Party (Host only)
                    if (isHost) {
                        TextButton(
                            onClick = {
                                viewModel.turnOffWatchParty(roomId)
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
                        ) {
                            Text("Turn Off", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Add Video Input Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add YouTube Video to Playlist",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = {
                                inputUrl = it
                                inputError = null
                            },
                            placeholder = { Text("Paste YouTube URL or Video ID", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val parsedId = extractYouTubeVideoId(inputUrl)
                                if (parsedId == null) {
                                    inputError = "Invalid YouTube video URL or ID"
                                    return@Button
                                }
                                val success = viewModel.addVideoToPlaylist(roomId, inputUrl.trim(), inputTitle)
                                if (success) {
                                    inputUrl = ""
                                    inputTitle = ""
                                    inputError = null
                                    Toast.makeText(context, "Added to playlist! 🎵", Toast.LENGTH_SHORT).show()
                                } else {
                                    inputError = "Video is already in the playlist"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    inputError?.let { err ->
                        Text(text = err, color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Text(
                text = "Playlist Items (${playlist.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Scrollable Playlist List
            if (playlist.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Text("No videos in playlist yet", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = playlist,
                        key = { it.id }
                    ) { item ->
                        val isActive = item.id == activeItemId

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) Color(0xFFDC2626).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isActive) Color(0xFFEF4444) else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black,
                                    modifier = Modifier.size(width = 64.dp, height = 42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Added by ${item.addedByUserName}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    if (isActive) {
                                        Text(
                                            text = "▶ PLAYING NOW",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LivePrimaryMint
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isActive) {
                                        IconButton(
                                            onClick = { viewModel.selectPlaylistItem(roomId, item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = LivePrimaryMint)
                                        }
                                    }

                                    if (isHost || item.addedByUserId == "creator_me") {
                                        IconButton(
                                            onClick = { viewModel.removeVideoFromPlaylist(roomId, item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Transparent Inline Gift Tray composable directly above the comment composer.
 */
@Composable
fun InlineGiftTray(
    roomId: String,
    viewModel: LiveViewModel,
    onClose: () -> Unit,
    onNavigateWallet: () -> Unit,
    onGiftSent: (LiveGift, Int) -> Unit
) {
    val gifts by viewModel.gifts.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color.Black.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = LiveGold, modifier = Modifier.size(16.dp))
                    Text("Gifts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DiamondBalanceChip(
                        balance = wallet.diamondBalance,
                        onClick = onNavigateWallet,
                        fontSize = 10,
                        iconSize = 12.dp
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Tray", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(gifts) { gift ->
                    val totalCost = gift.diamondCost
                    val canAfford = wallet.diamondBalance >= totalCost

                    Surface(
                        modifier = Modifier
                            .width(80.dp)
                            .height(72.dp)
                            .clickable {
                                if (canAfford) {
                                    val success = viewModel.sendGift(roomId, gift, 1)
                                    if (success) {
                                        onGiftSent(gift, 1)
                                    } else {
                                        Toast.makeText(context, "Insufficient diamonds", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Insufficient diamonds. Tap balance to top up.", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = LiveSurfaceDark.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (canAfford) LiveGold.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Icon(
                                imageVector = getGiftIcon(gift.name, gift.id),
                                contentDescription = gift.name,
                                tint = if (canAfford) LiveGold else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = gift.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (canAfford) Color.White else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(9.dp))
                                Text("${gift.diamondCost}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LiveDiamondColor)
                            }
                        }
                    }
                }
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
fun TextStageCanvas(room: LiveRoom) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LiveDarkBg,
                        Color(0xFF1E293B),
                        LiveDarkBg
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(32.dp))
            }
            Text("Group Text Chat Room", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(room.title, fontSize = 14.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun YouTubeWatchPartyStageCanvas(
    room: LiveRoom,
    watchPartyState: WatchPartyState?,
    isHost: Boolean,
    viewModel: LiveViewModel
) {
    val videoId = remember(watchPartyState?.videoId, room.youtubeVideoId, room.youtubeVideoUrl) {
        watchPartyState?.videoId.takeIf { !it.isNullOrBlank() }
            ?: room.youtubeVideoId
            ?: room.youtubeVideoUrl?.let { extractYouTubeVideoId(it) }
    }
    val playlistId = watchPartyState?.playlistId ?: room.youtubePlaylistId

    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var isApplyingRemoteCommand by remember { mutableStateOf(false) }
    var lastAppliedRevision by remember { mutableLongStateOf(0L) }

    var isHidden by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    // Touch Dragging State with Clamping Offset
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // On dispose, clean up WebView
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewRef = null
        }
    }

    // Handle remote WatchPartyState updates
    LaunchedEffect(watchPartyState, isReady) {
        val state = watchPartyState ?: return@LaunchedEffect
        val webView = webViewRef ?: return@LaunchedEffect
        if (!isReady) return@LaunchedEffect

        if (state.revision > lastAppliedRevision) {
            lastAppliedRevision = state.revision
            isApplyingRemoteCommand = true

            val now = System.currentTimeMillis()
            val elapsedSeconds = if (state.status == WatchPartyPlaybackStatus.PLAYING) {
                ((now - state.updatedAtMillis).coerceAtLeast(0L)) / 1000f
            } else 0f

            val expectedPosition = state.positionSeconds + elapsedSeconds

            when (state.status) {
                WatchPartyPlaybackStatus.PLAYING -> {
                    webView.evaluateJavascript("seekTo($expectedPosition); playVideo();", null)
                }
                WatchPartyPlaybackStatus.PAUSED -> {
                    webView.evaluateJavascript("seekTo(${state.positionSeconds}); pauseVideo();", null)
                }
                WatchPartyPlaybackStatus.ENDED -> {
                    webView.evaluateJavascript("pauseVideo();", null)
                }
                else -> {}
            }

            isApplyingRemoteCommand = false
        }
    }

    // Handle Mute state change
    LaunchedEffect(watchPartyState?.isMuted) {
        val isMuted = watchPartyState?.isMuted == true
        webViewRef?.evaluateJavascript(if (isMuted) "muteVideo();" else "unMuteVideo();", null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        val isTurnedOff = watchPartyState?.isTurnedOff == true

        if (isTurnedOff) {
            // Turned Off State Container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 100.dp, start = 24.dp, end = 24.dp)
            ) {
                Icon(Icons.Default.TvOff, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(52.dp))
                Text("Watch Party Turned Off", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("The host turned off synchronized video playback.", color = Color.LightGray, fontSize = 13.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showPlaylistSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                    ) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Playlist", fontSize = 13.sp)
                    }

                    if (isHost) {
                        Button(
                            onClick = { viewModel.addVideoToPlaylist(room.id, room.youtubeVideoUrl ?: "https://www.youtube.com/watch?v=dQw4w9WgXcQ") },
                            colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-enable Watch Party", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else if (isHidden) {
            // Hidden Compact Floating Pill
            Surface(
                onClick = { isHidden = false },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFDC2626),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 70.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Reopen Watch Party 📺", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (videoId == null && playlistId == null) {
            // Missing Video or Playlist Selection State
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 100.dp, start = 24.dp, end = 24.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(48.dp))
                Text("No YouTube Video Selected", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showPlaylistSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Playlist to Add Video")
                }
            }
        } else {
            // Real Draggable Rounded Floating Player Container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF111827),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 58.dp)
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .fillMaxWidth(0.92f)
                    .height(230.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar with Drag Handle and Action Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1F2937))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, _, _ ->
                                    offsetX = (offsetX + pan.x / 2.5f).coerceIn(-120f, 120f)
                                    offsetY = (offsetY + pan.y / 2.5f).coerceIn(-10f, 280f)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DragHandle, contentDescription = "Drag Player", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Text("YouTube Watch Party", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Playlist Action Button
                            IconButton(onClick = { showPlaylistSheet = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.QueueMusic, contentDescription = "Playlist", tint = Color.White, modifier = Modifier.size(16.dp))
                            }

                            // Mute Action Button
                            IconButton(
                                onClick = { viewModel.toggleMuteWatchParty(room.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (watchPartyState?.isMuted == true) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute Toggle",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Hide Action Button
                            IconButton(onClick = { isHidden = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Player", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // WebView Official YouTube Player Frame
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                    }
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: android.webkit.WebView?,
                                            request: android.webkit.WebResourceRequest?
                                        ): Boolean {
                                            val uri = request?.url ?: return false
                                            val host = uri.host?.lowercase() ?: ""
                                            return !host.contains("youtube") && !host.contains("googlevideo")
                                        }
                                    }

                                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

                                    class YouTubeJSInterface {
                                        @android.webkit.JavascriptInterface
                                        fun onPlayerReady() {
                                            mainHandler.post {
                                                isReady = true
                                            }
                                        }

                                        @android.webkit.JavascriptInterface
                                        fun onPlayerStateChange(stateCode: Int, currentTime: Float) {
                                            mainHandler.post {
                                                val status = when (stateCode) {
                                                    1 -> WatchPartyPlaybackStatus.PLAYING
                                                    2 -> WatchPartyPlaybackStatus.PAUSED
                                                    3 -> WatchPartyPlaybackStatus.BUFFERING
                                                    0 -> WatchPartyPlaybackStatus.ENDED
                                                    else -> WatchPartyPlaybackStatus.UNSTARTED
                                                }
                                                if (isHost && !isApplyingRemoteCommand) {
                                                    viewModel.updateWatchPartyState(room.id, status, currentTime)
                                                }
                                            }
                                        }

                                        @android.webkit.JavascriptInterface
                                        fun onPlayerError(errorCode: Int) {
                                            mainHandler.post {
                                                errorMessage = when (errorCode) {
                                                    2 -> "Invalid YouTube video parameter or ID."
                                                    5 -> "HTML5 video playback error."
                                                    100 -> "Video not found or removed."
                                                    101, 150 -> "Video embedding restricted by owner."
                                                    else -> "Playback error occurred (Code $errorCode)."
                                                }
                                            }
                                        }
                                    }

                                    addJavascriptInterface(YouTubeJSInterface(), "AndroidBridge")

                                    val listParam = if (!playlistId.isNullOrBlank()) "'listType': 'playlist', 'list': '$playlistId'," else ""
                                    val videoParam = videoId ?: ""

                                    val htmlContent = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                            <style>
                                                body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; }
                                                #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; }
                                            </style>
                                        </head>
                                        <body>
                                            <div id="player"></div>
                                            <script>
                                                var tag = document.createElement('script');
                                                tag.src = "https://www.youtube.com/iframe_api";
                                                var firstScriptTag = document.getElementsByTagName('script')[0];
                                                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                                                var player;
                                                function onYouTubeIframeAPIReady() {
                                                    player = new YT.Player('player', {
                                                        height: '100%',
                                                        width: '100%',
                                                        videoId: '$videoParam',
                                                        playerVars: {
                                                            'playsinline': 1,
                                                            'autoplay': 1,
                                                            'controls': 1,
                                                            'rel': 0,
                                                            'enablejsapi': 1,
                                                            'origin': 'https://www.youtube.com',
                                                            $listParam
                                                        },
                                                        events: {
                                                            'onReady': function(e) { if (window.AndroidBridge) window.AndroidBridge.onPlayerReady(); },
                                                            'onStateChange': function(e) { if (window.AndroidBridge) window.AndroidBridge.onPlayerStateChange(e.data, player ? player.getCurrentTime() : 0); },
                                                            'onError': function(e) { if (window.AndroidBridge) window.AndroidBridge.onPlayerError(e.data); }
                                                        }
                                                    });
                                                }

                                                function playVideo() { if (player && player.playVideo) player.playVideo(); }
                                                function pauseVideo() { if (player && player.pauseVideo) player.pauseVideo(); }
                                                function muteVideo() { if (player && player.mute) player.mute(); }
                                                function unMuteVideo() { if (player && player.unMute) player.unMute(); }
                                                function seekTo(seconds) { if (player && player.seekTo) player.seekTo(seconds, true); }
                                                function loadVideoById(vId, startSec) { if (player && player.loadVideoById) player.loadVideoById({'videoId': vId, 'startSeconds': startSec || 0}); }
                                            </script>
                                        </body>
                                        </html>
                                    """.trimIndent()

                                    loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
                                    webViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Error Overlay
                        errorMessage?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(36.dp))
                                    Text("Playback Error", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(err, color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playlist Bottom Sheet
        if (showPlaylistSheet) {
            WatchPartyPlaylistBottomSheet(
                roomId = room.id,
                watchPartyState = watchPartyState,
                isHost = isHost,
                viewModel = viewModel,
                onDismiss = { showPlaylistSheet = false }
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
                                        WorkDailyLiveAvatar(
                                            name = seat.userName,
                                            size = 48.dp,
                                            isLive = seat.isSpeaking,
                                            showBadge = false
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
