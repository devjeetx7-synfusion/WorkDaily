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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Resolves the room from the live session (real, persisted room) or from the demo discovery cache,
 * and renders explicit loading / unavailable / ended states instead of fabricating a room.
 */
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
    val sessionState by viewModel.roomSessionState.collectAsState()
    val liveRooms by viewModel.liveRooms.collectAsState()

    val activeState = sessionState as? LiveRoomSessionState.Active
    val activeRoom = activeState?.room?.takeIf { it.id == roomId }
    val demoRoom = liveRooms.firstOrNull { it.id == roomId }?.takeIf { it.isDemoRoom }
    // Ended rooms are rendered as an explicit "Live ended" state, never as a playable room.
    val room = activeRoom ?: demoRoom

    if (room == null) {
        when (val state = sessionState) {
            is LiveRoomSessionState.Loading -> LiveRoomStateScreen(
                title = "Connecting to live room…",
                message = "Joining the room and loading its messages.",
                isLoading = true,
                onBack = onNavigateBack
            )

            is LiveRoomSessionState.Unavailable -> LiveRoomStateScreen(
                title = "Live room unavailable",
                message = state.message,
                onBack = onNavigateBack
            )

            is LiveRoomSessionState.Ended -> LiveRoomStateScreen(
                title = "Live ended",
                message = "This live room has ended. The recording is not available.",
                onBack = onNavigateBack
            )

            is LiveRoomSessionState.Error -> LiveRoomStateScreen(
                title = "Could not open this live room",
                message = state.message,
                onBack = onNavigateBack,
                onRetry = { viewModel.retryActiveRoom() }
            )

            else -> LiveRoomStateScreen(
                title = "Connecting to live room…",
                message = "Joining the room and loading its messages.",
                isLoading = true,
                onBack = onNavigateBack
            )
        }
        return
    }

    LiveRoomScreenContent(
        room = room,
        isSessionRoom = activeRoom != null,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onNavigateWallet = onNavigateWallet,
        onNavigateSummary = onNavigateSummary,
        onNavigatePrivateChat = onNavigatePrivateChat,
        onNavigateFullProfile = onNavigateFullProfile,
        onPagingLockChange = onPagingLockChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveRoomScreenContent(
    room: LiveRoom,
    isSessionRoom: Boolean,
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateSummary: (String) -> Unit = {},
    onNavigatePrivateChat: (String) -> Unit = {},
    onNavigateFullProfile: (String) -> Unit = {},
    onPagingLockChange: (Boolean) -> Unit = {}
) {
    val chatMessagesMap by viewModel.chatMessagesMap.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()
    val audioSeatsMap by viewModel.audioSeatsMap.collectAsState()

    val sessionState by viewModel.roomSessionState.collectAsState()
    val sessionMessages by viewModel.sessionMessages.collectAsState()
    val sessionMembers by viewModel.sessionMembers.collectAsState()
    val sessionIsChatLoading by viewModel.sessionIsChatLoading.collectAsState()
    val sessionHasMoreHistory by viewModel.sessionHasMoreHistory.collectAsState()
    val sessionWatchParty by viewModel.sessionWatchParty.collectAsState()
    val sessionPlaylist by viewModel.sessionPlaylist.collectAsState()
    val sessionIsPlaylistLoading by viewModel.sessionIsPlaylistLoading.collectAsState()
    val sessionNotice by viewModel.sessionNotice.collectAsState()
    val liveHint by viewModel.liveHint.collectAsState()
    val liveIdentity by viewModel.liveIdentity.collectAsState()
    val followingUids by viewModel.followingUids.collectAsState()
    val followBusyUids by viewModel.followBusyUids.collectAsState()

    val chatMessages = if (isSessionRoom) sessionMessages else (chatMessagesMap[room.id] ?: emptyList())
    val giftOverlayEvents by viewModel.giftOverlayEvents.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val isFollowing = if (liveIdentity != null) {
        followingUids.contains(room.creatorId)
    } else {
        followedCreatorIds.contains(room.creatorId)
    }
    val isHost = if (isSessionRoom) {
        (sessionState as? LiveRoomSessionState.Active)?.isHost == true
    } else {
        room.isHost || (liveIdentity?.uid != null && room.creatorId == liveIdentity?.uid)
    }
    val viewerCount = if (isSessionRoom) {
        (sessionState as? LiveRoomSessionState.Active)?.viewerCount ?: 0
    } else {
        room.viewerCount
    }
    val audioSeats = audioSeatsMap[room.id] ?: emptyList()
    var locallyMuted by remember(room.id) { mutableStateOf(false) }

    // Real live duration, derived from the persisted room start time (never a fake counter).
    var liveDurationSeconds by remember(room.id, room.startTimeMillis) {
        mutableLongStateOf(liveElapsedSeconds(room.startTimeMillis, System.currentTimeMillis()))
    }
    LaunchedEffect(room.id, room.startTimeMillis, room.isLive) {
        while (room.isLive) {
            liveDurationSeconds = liveElapsedSeconds(room.startTimeMillis, System.currentTimeMillis())
            delay(1_000L)
        }
    }


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

    // Surface session/host notices (send failures, playback conflicts, auth problems).
    val activeNotice = sessionNotice ?: liveHint
    LaunchedEffect(activeNotice?.id) {
        val notice = activeNotice ?: return@LaunchedEffect
        Toast.makeText(context, notice.message, Toast.LENGTH_SHORT).show()
        if (sessionNotice != null) viewModel.consumeSessionNotice(notice.id) else viewModel.consumeLiveHint()
    }

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
        if (!isSessionRoom) {
            // Demo/offline room: keep the local join marker. Persisted rooms rely on real presence.
            viewModel.onUserJoinedRoom(room.id, "You")
        }
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
                    LiveRoomType.YOUTUBE_WATCH_PARTY -> WatchPartyStage(
                        roomId = room.id,
                        watchPartyState = sessionWatchParty ?: room.watchPartyState,
                        playlist = if (isSessionRoom) sessionPlaylist else room.watchPartyState?.playlist.orEmpty(),
                        roomYoutubeVideoId = room.youtubeVideoId,
                        roomYoutubeVideoUrl = room.youtubeVideoUrl,
                        roomYoutubePlaylistId = room.youtubePlaylistId,
                        isHost = isHost,
                        isSessionBacked = isSessionRoom,
                        isLive = room.isLive,
                        playlistLoading = if (isSessionRoom) sessionIsPlaylistLoading else false,
                        locallyMuted = locallyMuted,
                        onToggleLocalMute = { locallyMuted = !locallyMuted },
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
                                enabled = !followBusyUids.contains(room.creatorId),
                                onToggle = {
                                    if (liveIdentity != null) {
                                        viewModel.toggleFollowUser(room.creatorId)
                                    } else {
                                        viewModel.toggleFollowCreator(room.creatorId, room.id)
                                    }
                                }
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
                    // Live duration badge (real elapsed time since the room started).
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = formatLiveDuration(liveDurationSeconds),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    ViewerCountChip(
                        count = viewerCount,
                        onClick = { showViewerListSheet = true }
                    )

                    IconButton(
                        onClick = {
                            if (!isSessionRoom) {
                                // Demo rooms keep the local system-chat event; real rooms rely on the
                                // shared link only (attribution comes from the signed-in account).
                                viewModel.onShareLiveStream(room.id, liveIdentity?.displayName ?: "A viewer")
                            }
                            val shareText = com.synfusion.attendance.live.watchparty.LiveShareLink.buildShareText(
                                roomId = room.id,
                                title = room.title,
                                hostName = room.creatorName,
                                isWatchParty = room.type == LiveRoomType.YOUTUBE_WATCH_PARTY
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    com.synfusion.attendance.live.watchparty.LiveShareLink.buildShareSubject(room.title)
                                )
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Live"))
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
                        if (isSessionRoom && sessionIsChatLoading && chatMessages.isEmpty()) {
                            item(key = "chat_loading") {
                                Text(
                                    "Loading chat…",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                            }
                        }
                        if (isSessionRoom && sessionHasMoreHistory) {
                            item(key = "load_older") {
                                TextButton(
                                    onClick = { viewModel.loadOlderRoomMessages() },
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text("Load earlier messages", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                                }
                            }
                        }
                        items(
                            items = chatMessages,
                            key = { it.id }
                        ) { msg ->
                            val isMsgSenderFollowed = if (liveIdentity != null) {
                                followingUids.contains(msg.senderId)
                            } else {
                                followedCreatorIds.contains(msg.senderId)
                            }
                            LiveCommentItem(
                                message = msg,
                                onUserClick = { selectedProfileUserId = it },
                                onFollowClick = { senderId ->
                                    if (liveIdentity != null) viewModel.toggleFollowUser(senderId)
                                    else viewModel.toggleFollowCreator(senderId, room.id)
                                },
                                isFollowingUser = isMsgSenderFollowed,
                                currentUserId = liveIdentity?.uid ?: "current_user"
                            )
                            // Optimistic send state for persisted room chat (retry on failure).
                            if (isSessionRoom && (msg.isPending || msg.isFailed)) {
                                Text(
                                    text = if (msg.isFailed) "Not sent — tap to retry" else "Sending…",
                                    color = if (msg.isFailed) Color(0xFFF87171) else Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .padding(start = 8.dp, bottom = 2.dp)
                                        .then(
                                            if (msg.isFailed) {
                                                Modifier.clickable {
                                                    msg.clientMessageId?.let { viewModel.retryRoomMessage(it) }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
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
                                    viewModel.sendRoomMessage(room.id, chatInput.trim())
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
                        if (isSessionRoom) viewModel.endActiveLiveRoom() else viewModel.endLiveStream(room.id)
                        onNavigateSummary(room.id)
                    }
                )
            }

            if (showViewerListSheet) {
                if (isSessionRoom) {
                    LiveRoomMembersSheet(
                        members = sessionMembers,
                        isHost = isHost,
                        currentUid = liveIdentity?.uid,
                        followingUids = followingUids,
                        followBusyUids = followBusyUids,
                        onFollowToggle = { targetUid -> viewModel.toggleFollowUser(targetUid) },
                        onOpenProfile = { targetUid ->
                            showViewerListSheet = false
                            selectedProfileUserId = targetUid
                        },
                        onRemoveMember = { targetUid -> viewModel.removeRoomParticipant(targetUid) },
                        onDismiss = { showViewerListSheet = false }
                    )
                } else {
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
            }

            selectedProfileUserId?.let { targetId ->
                val realMember = if (isSessionRoom) sessionMembers.firstOrNull { it.uid == targetId } else null
                if (isSessionRoom && realMember != null) {
                    LiveMemberProfileSheet(
                        member = realMember,
                        isSelf = liveIdentity?.uid == targetId,
                        isFollowing = followingUids.contains(targetId),
                        followBusy = followBusyUids.contains(targetId),
                        onToggleFollow = { viewModel.toggleFollowUser(targetId) },
                        onOpenFullProfile = {
                            selectedProfileUserId = null
                            onNavigateFullProfile(targetId)
                        },
                        onDismiss = { selectedProfileUserId = null }
                    )
                } else {
                    LiveProfileBottomSheet(
                        userId = targetId,
                        viewModel = viewModel,
                        onDismiss = { selectedProfileUserId = null },
                        onNavigatePrivateChat = onNavigatePrivateChat,
                        onNavigateFullProfile = onNavigateFullProfile
                    )
                }
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
                                if (isSessionRoom) viewModel.endActiveLiveRoom() else viewModel.endLiveStream(room.id)
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
