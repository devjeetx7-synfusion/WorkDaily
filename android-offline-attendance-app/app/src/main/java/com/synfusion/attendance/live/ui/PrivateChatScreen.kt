package com.synfusion.attendance.live.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.ChatMessagesFeedState
import com.synfusion.attendance.online.FirestoreChatMessage
import com.synfusion.attendance.online.MessageStatus
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.SocialSessionCache
import com.synfusion.attendance.online.rememberCurrentFirebaseUid
import com.synfusion.attendance.ui.components.ChatMessagesSkeleton
import com.synfusion.attendance.ui.components.SafeExternalLinkHandler
import com.synfusion.attendance.ui.components.WorkDailyErrorRetryState
import com.synfusion.attendance.ui.components.WorkDailySubtleRefreshBar
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

enum class MessageGroupPosition {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST
}

private data class OutgoingQueueItem(
    val localId: String,
    val text: String,
    val createdAt: Long,
    val isSending: Boolean,
    val errorMessage: String? = null
)

private sealed interface ChatTimelineItem {
    val stableKey: String

    data class DateHeader(
        val dayBucket: Long,
        val label: String
    ) : ChatTimelineItem {
        override val stableKey: String = "date_$dayBucket"
    }

    data class MessageRow(
        val message: FirestoreChatMessage,
        val groupPosition: MessageGroupPosition,
        val showMetaFooter: Boolean,
        val tightTopSpacing: Boolean,
        val localError: String? = null,
        val isLocalQueueItem: Boolean = false
    ) : ChatTimelineItem {
        override val stableKey: String = message.id
    }
}

private const val GROUP_WINDOW_MS = 5 * 60 * 1000L
private const val INITIAL_CHAT_PAGE_SIZE = 40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    friendId: String,
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val currentUserId = rememberCurrentFirebaseUid().value
    val socialSnapshot by SocialSessionCache.manager.snapshot.collectAsStateWithLifecycle(
        initialValue = SocialSessionCache.manager.snapshot.value
    )

    val followRepo = SocialSessionCache.followRepository
    val blockRepo = SocialSessionCache.blockRepository
    val chatRepo = SocialSessionCache.chatRepository

    val activeViewerUid: String? = currentUserId?.takeIf { it.isNotBlank() }

    val conversationId = remember(activeViewerUid, friendId) {
        if (activeViewerUid != null && friendId.isNotBlank()) {
            chatRepo.getConversationId(activeViewerUid, friendId)
        } else {
            ""
        }
    }

    val cachedFriendProfile: PublicProfile? = remember(socialSnapshot, friendId, activeViewerUid) {
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.profiles[friendId]?.value
        } else {
            null
        }
    }

    val cachedMessages: List<FirestoreChatMessage>? = remember(socialSnapshot, conversationId, activeViewerUid) {
        if (activeViewerUid != null && conversationId.isNotBlank() && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.chatMessages[conversationId]
        } else {
            null
        }
    }

    val cachedKnownMutual: Boolean? = remember(socialSnapshot, friendId, activeViewerUid) {
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            val cachedPeer = socialSnapshot.chatRelationships[friendId]?.value
            if (cachedPeer != null) {
                !cachedPeer.isBlockedByMe && !cachedPeer.isBlockedByPeer && cachedPeer.isMutual
            } else {
                val following = socialSnapshot.myFollowedUids?.value
                val followers = socialSnapshot.myFollowerUids?.value
                val blocked = socialSnapshot.myBlockedUids?.value
                if (following != null && followers != null && blocked?.contains(friendId) != true) {
                    following.contains(friendId) && followers.contains(friendId)
                } else {
                    null
                }
            }
        } else {
            null
        }
    }

    var isCheckingMutual by remember(friendId) {
        mutableStateOf(cachedKnownMutual == null && cachedMessages == null)
    }
    var isRefreshingHeader by remember(friendId) { mutableStateOf(false) }
    var isMutualFriend by remember(friendId) {
        mutableStateOf(cachedKnownMutual ?: (cachedMessages != null))
    }
    var isBlocked by remember(friendId, activeViewerUid) {
        mutableStateOf(
            activeViewerUid != null &&
                socialSnapshot.ownerUid == activeViewerUid &&
                (
                    socialSnapshot.myBlockedUids?.value?.contains(friendId) == true ||
                        socialSnapshot.chatRelationships[friendId]?.value?.let { it.isBlockedByMe || it.isBlockedByPeer } == true
                    )
        )
    }
    var isBlockedByMe by remember(friendId, activeViewerUid) {
        mutableStateOf(
            activeViewerUid != null &&
                socialSnapshot.ownerUid == activeViewerUid &&
                (
                    socialSnapshot.myBlockedUids?.value?.contains(friendId) == true ||
                        socialSnapshot.chatRelationships[friendId]?.value?.isBlockedByMe == true
                    )
        )
    }
    var relationshipError by remember(friendId) { mutableStateOf<String?>(null) }
    var isBlockActionBusy by remember(friendId) { mutableStateOf(false) }
    var showBlockConfirmDialog by remember(friendId) { mutableStateOf(false) }
    var showMenu by remember(friendId) { mutableStateOf(false) }
    var friendProfile by remember(friendId) { mutableStateOf<PublicProfile?>(cachedFriendProfile) }

    LaunchedEffect(cachedFriendProfile) {
        if (cachedFriendProfile != null) {
            friendProfile = cachedFriendProfile
        }
    }

    var messageLimit by rememberSaveable(conversationId) { mutableIntStateOf(INITIAL_CHAT_PAGE_SIZE) }
    var remoteMessages by remember(conversationId) {
        mutableStateOf<List<FirestoreChatMessage>>(cachedMessages ?: emptyList())
    }
    var isFeedInitialLoading by remember(conversationId) {
        mutableStateOf(cachedMessages == null)
    }
    var feedErrorMessage by remember(conversationId) {
        mutableStateOf<String?>(null)
    }
    var hasMoreOlderMessages by remember(conversationId) {
        mutableStateOf((cachedMessages?.size ?: 0) >= INITIAL_CHAT_PAGE_SIZE)
    }
    var isLoadingOlder by remember(conversationId) { mutableStateOf(false) }
    var outgoingQueue by remember(conversationId) { mutableStateOf<List<OutgoingQueueItem>>(emptyList()) }
    var messageInput by rememberSaveable(friendId) { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }

    suspend fun refreshChatRelationshipState(forceRefresh: Boolean) {
        val viewerUid = currentUserId?.takeIf { it.isNotBlank() }
        if (viewerUid == null || friendId.isBlank()) {
            isCheckingMutual = false
            isRefreshingHeader = false
            return
        }

        val hasWarmState = friendProfile != null || cachedMessages != null || cachedKnownMutual != null
        if (!hasWarmState) {
            isCheckingMutual = true
        } else {
            isRefreshingHeader = true
        }
        relationshipError = null

        try {
            coroutineScope {
                val profileDeferred = async<ProfileOperationResult<PublicProfile?>> {
                    SocialSessionCache.manager.getPublicProfile(
                        ownerUid = viewerUid,
                        targetUid = friendId,
                        forceRefresh = forceRefresh
                    )
                }
                val blockedByCurrentDeferred = async<Boolean> { blockRepo.isBlocked(viewerUid, friendId) }
                val blockedByOtherDeferred = async<Boolean> { blockRepo.isBlocked(friendId, viewerUid) }
                val mutualDeferred = async<Boolean> { followRepo.isMutual(viewerUid, friendId) }

                val blockedByCurrent = blockedByCurrentDeferred.await()
                val blockedByOther = blockedByOtherDeferred.await()
                val blockedEither = blockedByCurrent || blockedByOther
                isBlockedByMe = blockedByCurrent
                isBlocked = blockedEither

                val mutual = if (!blockedEither) mutualDeferred.await() else false
                isMutualFriend = mutual

                val profRes: ProfileOperationResult<PublicProfile?> = profileDeferred.await()
                when (profRes) {
                    is ProfileOperationResult.Success -> {
                        if (profRes.data != null) {
                            friendProfile = profRes.data
                        }
                    }
                    is ProfileOperationResult.Error -> {
                        if (friendProfile == null) {
                            relationshipError = profRes.message
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (friendProfile == null) {
                relationshipError = e.message ?: "Unable to verify conversation status."
            }
        } finally {
            isCheckingMutual = false
            isRefreshingHeader = false
        }
    }

    LaunchedEffect(currentUserId, friendId) {
        refreshChatRelationshipState(forceRefresh = false)
    }

    DisposableEffect(lifecycleOwner, currentUserId, friendId) {
        var handledInitialResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!handledInitialResume) {
                    handledInitialResume = true
                } else {
                    coroutineScope.launch {
                        refreshChatRelationshipState(forceRefresh = false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Single deduplicated message listener scoped to active conversationId + messageLimit
    var listenerRetryNonce by remember(conversationId) { mutableIntStateOf(0) }
    LaunchedEffect(conversationId, messageLimit, isMutualFriend, isBlocked, listenerRetryNonce) {
        val viewerUid = currentUserId?.takeIf { it.isNotBlank() }
        if (viewerUid == null || conversationId.isBlank() || isBlocked || (!isMutualFriend && !isCheckingMutual)) {
            isLoadingOlder = false
            return@LaunchedEffect
        }
        if (isCheckingMutual && cachedMessages == null && cachedKnownMutual != true) {
            return@LaunchedEffect
        }

        chatRepo.observeMessagesFeed(conversationId = conversationId, limit = messageLimit).collect { nextState ->
            when (nextState) {
                is ChatMessagesFeedState.Loading -> {
                    if (remoteMessages.isEmpty()) {
                        isFeedInitialLoading = true
                    }
                }
                is ChatMessagesFeedState.Loaded -> {
                    remoteMessages = nextState.messages
                    hasMoreOlderMessages = nextState.hasMoreOlder
                    isFeedInitialLoading = false
                    feedErrorMessage = null
                    isLoadingOlder = false
                    SocialSessionCache.manager.cacheConversationMessages(
                        ownerUid = viewerUid,
                        conversationId = conversationId,
                        messages = nextState.messages
                    )
                }
                is ChatMessagesFeedState.Error -> {
                    val isPermissionOnUninitialized = remoteMessages.isEmpty() &&
                        isMutualFriend &&
                        !isBlocked &&
                        nextState.message.contains("PERMISSION_DENIED", ignoreCase = true)
                    isFeedInitialLoading = false
                    isLoadingOlder = false
                    if (!isPermissionOnUninitialized && remoteMessages.isEmpty()) {
                        feedErrorMessage = nextState.message
                    } else if (isPermissionOnUninitialized) {
                        feedErrorMessage = null
                    }
                }
            }
        }
    }

    // Mark messages as read whenever entering an active conversation or when new unread messages arrive
    var hasMarkedInitialRead by remember(conversationId) { mutableStateOf(false) }
    var lastMarkedUnreadId by remember(conversationId) { mutableStateOf<String?>(null) }
    LaunchedEffect(remoteMessages, currentUserId, conversationId, isMutualFriend, isBlocked) {
        val viewerUid = currentUserId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (conversationId.isBlank() || !isMutualFriend || isBlocked) return@LaunchedEffect

        val latestUnreadIncoming = remoteMessages.lastOrNull { msg ->
            msg.senderUid != viewerUid && msg.status != MessageStatus.READ
        }

        val shouldMarkRead = !hasMarkedInitialRead || (latestUnreadIncoming != null && latestUnreadIncoming.id != lastMarkedUnreadId)
        if (shouldMarkRead) {
            when (chatRepo.markMessagesAsRead(conversationId, viewerUid)) {
                is ProfileOperationResult.Success -> {
                    hasMarkedInitialRead = true
                    if (latestUnreadIncoming != null) {
                        lastMarkedUnreadId = latestUnreadIncoming.id
                    }
                }
                is ProfileOperationResult.Error -> {
                    // Keep unconfirmed so a subsequent emission or retry can clear the badge.
                }
            }
        }
    }

    // Merge server messages with local pending/failed queue (deduplicating confirmed sends)
    val mergedTimeline = remember(remoteMessages, outgoingQueue, currentUserId) {
        buildChatTimeline(
            remoteMessages = remoteMessages,
            outgoingQueue = outgoingQueue,
            currentUserId = currentUserId
        )
    }

    val listState = rememberLazyListState()
    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) {
                true
            } else {
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= (total - 3).coerceAtLeast(0)
            }
        }
    }

    var hasScrolledInitially by remember(conversationId) { mutableStateOf(false) }
    var previousTimelineCount by remember(conversationId) { mutableIntStateOf(0) }
    var hasUnseenNewMessageWhileScrolledUp by remember(conversationId) { mutableStateOf(false) }

    LaunchedEffect(mergedTimeline.size) {
        val total = mergedTimeline.size
        if (total == 0) {
            previousTimelineCount = 0
            return@LaunchedEffect
        }
        val lastItem = mergedTimeline.lastOrNull()
        val lastIsMine = (lastItem as? ChatTimelineItem.MessageRow)?.message?.senderUid == currentUserId

        when {
            !hasScrolledInitially -> {
                listState.scrollToItem(total - 1)
                hasScrolledInitially = true
                hasUnseenNewMessageWhileScrolledUp = false
            }
            total > previousTimelineCount && !isLoadingOlder -> {
                if (isNearBottom || lastIsMine) {
                    listState.animateScrollToItem(total - 1)
                    hasUnseenNewMessageWhileScrolledUp = false
                } else {
                    hasUnseenNewMessageWhileScrolledUp = true
                }
            }
        }
        previousTimelineCount = total
    }

    LaunchedEffect(isNearBottom) {
        if (isNearBottom) {
            hasUnseenNewMessageWhileScrolledUp = false
        }
    }

    // Smoothly keep latest message visible when soft keyboard opens while user is near bottom
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    var wasImeOpen by remember { mutableStateOf(false) }
    LaunchedEffect(imeBottomPx > 0) {
        val imeOpenNow = imeBottomPx > 0
        if (imeOpenNow && !wasImeOpen && isNearBottom && mergedTimeline.isNotEmpty()) {
            listState.animateScrollToItem(mergedTimeline.lastIndex)
        }
        wasImeOpen = imeOpenNow
    }

    fun dispatchSend(textToSend: String, retryLocalId: String? = null) {
        val senderUid = currentUserId ?: return
        val cleanText = textToSend.trim()
        if (cleanText.isBlank()) return

        val localId = retryLocalId ?: "local_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        outgoingQueue = if (retryLocalId != null) {
            outgoingQueue.map { item ->
                if (item.localId == retryLocalId) {
                    item.copy(isSending = true, errorMessage = null, createdAt = now)
                } else item
            }
        } else {
            outgoingQueue + OutgoingQueueItem(
                localId = localId,
                text = cleanText,
                createdAt = now,
                isSending = true,
                errorMessage = null
            )
        }

        coroutineScope.launch {
            try {
                when (val result = chatRepo.sendMessage(senderUid, friendId, cleanText)) {
                    is ProfileOperationResult.Success -> {
                        outgoingQueue = outgoingQueue.filterNot { it.localId == localId }
                    }
                    is ProfileOperationResult.Error -> {
                        outgoingQueue = outgoingQueue.map { item ->
                            if (item.localId == localId) {
                                item.copy(isSending = false, errorMessage = result.message)
                            } else item
                        }
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                val errMsg = e.message ?: "Message could not be sent."
                outgoingQueue = outgoingQueue.map { item ->
                    if (item.localId == localId) {
                        item.copy(isSending = false, errorMessage = errMsg)
                    } else item
                }
                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val friendName = friendProfile?.displayName?.ifBlank { null } ?: "WorkDaily Member"
    val friendUsername = friendProfile?.username?.ifBlank { null }
    val friendAvatar = friendProfile?.photoUrl ?: ""
    val isVerified = friendProfile?.isVerified == true

    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBlockActionBusy) showBlockConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Block ${friendUsername?.let { "@$it" } ?: friendName}?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Blocking disables messaging and removes any follow connection between you.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeUid = currentUserId
                        if (activeUid.isNullOrBlank() || isBlockActionBusy) return@Button
                        isBlockActionBusy = true
                        coroutineScope.launch {
                            try {
                                when (val res = blockRepo.blockUser(activeUid, friendId)) {
                                    is ProfileOperationResult.Success -> {
                                        SocialSessionCache.manager.recordBlockSuccess(activeUid, friendId)
                                        isBlockedByMe = true
                                        isBlocked = true
                                        isMutualFriend = false
                                        showBlockConfirmDialog = false
                                        Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                                    }
                                    is ProfileOperationResult.Error -> {
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Failed to block user", Toast.LENGTH_SHORT).show()
                            } finally {
                                isBlockActionBusy = false
                            }
                        }
                    },
                    enabled = !isBlockActionBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isBlockActionBusy) "Blocking…" else "Block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockConfirmDialog = false },
                    enabled = !isBlockActionBusy
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Seamless full-bleed background across header, message canvas, composer, and system navigation bar
    val screenBackground = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        // 1. Clean Direct Chat Header (aligned Back button, Avatar, Name/Handle, Overflow Menu)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(screenBackground)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = WorkDailyIcons.ArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateProfile(friendId) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkDailyLiveAvatar(
                        name = friendName,
                        avatarUrl = friendAvatar,
                        size = 38.dp,
                        isLive = false,
                        showBadge = false
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = friendName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                            if (isVerified) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = LivePrimaryEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        val subtitleText = when {
                            isBlocked -> "Messaging unavailable"
                            !friendUsername.isNullOrBlank() && isMutualFriend -> "@$friendUsername • Friends"
                            !friendUsername.isNullOrBlank() -> "@$friendUsername"
                            isMutualFriend -> "Mutual Friends"
                            else -> "WorkDaily Member"
                        }

                        Text(
                            text = subtitleText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isMutualFriend && !isBlocked) {
                                LivePrimaryEmerald
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = WorkDailyIcons.MoreVertical,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateProfile(friendId)
                            }
                        )
                        if (!currentUserId.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isBlockedByMe) "Unblock User" else "Block User",
                                        color = if (isBlockedByMe) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isBlockedByMe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                    )
                                },
                                enabled = !isBlockActionBusy,
                                onClick = {
                                    showMenu = false
                                    val activeUid = currentUserId ?: return@DropdownMenuItem
                                    if (isBlockedByMe) {
                                        isBlockActionBusy = true
                                        coroutineScope.launch {
                                            try {
                                                when (val res = blockRepo.unblockUser(activeUid, friendId)) {
                                                    is ProfileOperationResult.Success -> {
                                                        SocialSessionCache.manager.recordUnblockSuccess(activeUid, friendId)
                                                        refreshChatRelationshipState(forceRefresh = true)
                                                        Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is ProfileOperationResult.Error -> {
                                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } finally {
                                                isBlockActionBusy = false
                                            }
                                        }
                                    } else {
                                        showBlockConfirmDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            WorkDailySubtleRefreshBar(visible = isRefreshingHeader || isLoadingOlder)
        }

        // Resolves drawable-nodpi/chatwall wallpaper dynamically if present
        val wallpaperResId = remember(context) {
            context.resources.getIdentifier("chatwall", "drawable", context.packageName)
        }

        // 2. Main Conversation Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (wallpaperResId != 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = wallpaperResId),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBackground.copy(alpha = 0.45f))
                )
            }

            when {
                isCheckingMutual && mergedTimeline.isEmpty() -> {
                    ChatMessagesSkeleton(modifier = Modifier.fillMaxSize())
                }

                relationshipError != null && !isMutualFriend && mergedTimeline.isEmpty() -> {
                    WorkDailyErrorRetryState(
                        title = "Couldn't open chat",
                        message = relationshipError ?: "Please check your connection and try again.",
                        onRetry = {
                            coroutineScope.launch {
                                refreshChatRelationshipState(forceRefresh = true)
                            }
                        }
                    )
                }

                isBlocked -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Messaging Unavailable",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "You cannot send or receive messages with this account due to block settings.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )
                                if (isBlockedByMe && !currentUserId.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val activeUid = currentUserId ?: return@OutlinedButton
                                            if (isBlockActionBusy) return@OutlinedButton
                                            isBlockActionBusy = true
                                            coroutineScope.launch {
                                                try {
                                                    when (val res = blockRepo.unblockUser(activeUid, friendId)) {
                                                        is ProfileOperationResult.Success -> {
                                                            SocialSessionCache.manager.recordUnblockSuccess(activeUid, friendId)
                                                            refreshChatRelationshipState(forceRefresh = true)
                                                            Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                                                        }
                                                        is ProfileOperationResult.Error -> {
                                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } finally {
                                                    isBlockActionBusy = false
                                                }
                                            }
                                        },
                                        enabled = !isBlockActionBusy,
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, LivePrimaryEmerald)
                                    ) {
                                        Text("Unblock User", color = LivePrimaryEmerald, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                !isCheckingMutual && !isMutualFriend -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(LivePrimaryEmerald.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Friends Only",
                                        tint = LivePrimaryEmerald,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Text(
                                    text = "Friends-Only Messaging",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Both you and $friendName must follow each other on WorkDaily to chat privately.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )

                                Button(
                                    onClick = { onNavigateProfile(friendId) },
                                    shape = RoundedCornerShape(22.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LivePrimaryEmerald,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Text("View Profile & Follow", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                isFeedInitialLoading && mergedTimeline.isEmpty() -> {
                    ChatMessagesSkeleton(modifier = Modifier.fillMaxSize())
                }

                feedErrorMessage != null && mergedTimeline.isEmpty() -> {
                    WorkDailyErrorRetryState(
                        title = "Couldn't load messages",
                        message = feedErrorMessage ?: "Failed to load conversation.",
                        onRetry = {
                            isFeedInitialLoading = true
                            feedErrorMessage = null
                            listenerRetryNonce += 1
                        }
                    )
                }

                mergedTimeline.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkDailyLiveAvatar(
                                name = friendName,
                                avatarUrl = friendAvatar,
                                size = 60.dp,
                                isLive = false,
                                showBadge = false
                            )
                            if (friendUsername != null) {
                                Text(
                                    text = "@$friendUsername",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LivePrimaryEmerald
                                )
                            }
                            Text(
                                text = "Chat with $friendName",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Send a direct message below to start your conversation.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (hasMoreOlderMessages) {
                            item(key = "load_older_messages") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(enabled = !isLoadingOlder) {
                                                isLoadingOlder = true
                                                messageLimit += INITIAL_CHAT_PAGE_SIZE
                                            }
                                    ) {
                                        Text(
                                            text = if (isLoadingOlder) "Loading earlier messages…" else "Load earlier messages",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LivePrimaryEmerald,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = mergedTimeline,
                            key = { it.stableKey }
                        ) { timelineItem ->
                            when (timelineItem) {
                                is ChatTimelineItem.DateHeader -> {
                                    ChatDateSeparator(label = timelineItem.label)
                                }

                                is ChatTimelineItem.MessageRow -> {
                                    Spacer(
                                        modifier = Modifier.height(
                                            if (timelineItem.tightTopSpacing) 2.dp else 9.dp
                                        )
                                    )
                                    PrivateMessageBubble(
                                        message = timelineItem.message,
                                        currentUserId = currentUserId,
                                        groupPosition = timelineItem.groupPosition,
                                        showMetaFooter = timelineItem.showMetaFooter,
                                        localError = timelineItem.localError,
                                        onRetryFailed = if (timelineItem.isLocalQueueItem && timelineItem.localError != null) {
                                            { dispatchSend(timelineItem.message.text, retryLocalId = timelineItem.message.id) }
                                        } else null,
                                        onDismissFailed = if (timelineItem.isLocalQueueItem && timelineItem.localError != null) {
                                            {
                                                outgoingQueue = outgoingQueue.filterNot {
                                                    it.localId == timelineItem.message.id
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    if (hasUnseenNewMessageWhileScrolledUp) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = LivePrimaryEmerald,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    hasUnseenNewMessageWhileScrolledUp = false
                                    coroutineScope.launch {
                                        if (mergedTimeline.isNotEmpty()) {
                                            listState.animateScrollToItem(mergedTimeline.lastIndex)
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "New message",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Seamless Bottom Message Composer Dock
        // Visible whenever conversation is active or warming up (hidden only on locked/blocked screens)
        val showComposer = !isBlocked && (isMutualFriend || isCheckingMutual)
        if (showComposer) {
            val trimmedInput = messageInput.trim()
            val canType = !currentUserId.isNullOrBlank() && !isBlocked && (isMutualFriend || isCheckingMutual)
            val canSendNow = trimmedInput.isNotEmpty() && !currentUserId.isNullOrBlank() && isMutualFriend && !isBlocked

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBackground)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val inputBorderColor = when {
                        isInputFocused -> LivePrimaryEmerald.copy(alpha = 0.7f)
                        trimmedInput.isNotEmpty() -> LivePrimaryEmerald.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                    }

                    val sharedInputTextStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp, max = 124.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, inputBorderColor, RoundedCornerShape(23.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (messageInput.isEmpty()) {
                            Text(
                                text = if (isCheckingMutual && !isMutualFriend) "Connecting chat…" else "Send a chat…",
                                style = sharedInputTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            enabled = canType,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isInputFocused = it.isFocused },
                            textStyle = sharedInputTextStyle,
                            cursorBrush = SolidColor(LivePrimaryEmerald),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            maxLines = 5
                        )
                    }

                    // Vertically aligned 46dp Send button with crisp touch target
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSendNow) {
                                    LivePrimaryEmerald
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                }
                            )
                            .clickable(enabled = canSendNow) {
                                val textToSend = trimmedInput
                                if (textToSend.isNotEmpty()) {
                                    messageInput = ""
                                    dispatchSend(textToSend)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (canSendNow) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .size(19.dp)
                                .offset(x = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatDateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/**
 * Clean, Snapchat-inspired grouped message bubble retaining WorkDaily's Emerald/Slate palette,
 * clickable URLs, discreet metadata on group endings, and honest pending/retry status.
 */
@Composable
fun PrivateMessageBubble(
    message: FirestoreChatMessage,
    currentUserId: String?,
    groupPosition: MessageGroupPosition = MessageGroupPosition.SINGLE,
    showMetaFooter: Boolean = true,
    localError: String? = null,
    onRetryFailed: (() -> Unit)? = null,
    onDismissFailed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isMe = message.senderUid == currentUserId
    val hasFailed = localError != null

    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleShape = remember(isMe, groupPosition) {
        val large = 18.dp
        val tight = 5.dp
        if (isMe) {
            when (groupPosition) {
                MessageGroupPosition.SINGLE -> RoundedCornerShape(
                    topStart = large, topEnd = large, bottomStart = large, bottomEnd = tight
                )
                MessageGroupPosition.FIRST -> RoundedCornerShape(
                    topStart = large, topEnd = large, bottomStart = large, bottomEnd = tight
                )
                MessageGroupPosition.MIDDLE -> RoundedCornerShape(
                    topStart = large, topEnd = tight, bottomStart = large, bottomEnd = tight
                )
                MessageGroupPosition.LAST -> RoundedCornerShape(
                    topStart = large, topEnd = tight, bottomStart = large, bottomEnd = large
                )
            }
        } else {
            when (groupPosition) {
                MessageGroupPosition.SINGLE -> RoundedCornerShape(
                    topStart = large, topEnd = large, bottomStart = tight, bottomEnd = large
                )
                MessageGroupPosition.FIRST -> RoundedCornerShape(
                    topStart = large, topEnd = large, bottomStart = tight, bottomEnd = large
                )
                MessageGroupPosition.MIDDLE -> RoundedCornerShape(
                    topStart = tight, topEnd = large, bottomStart = tight, bottomEnd = large
                )
                MessageGroupPosition.LAST -> RoundedCornerShape(
                    topStart = tight, topEnd = large, bottomStart = large, bottomEnd = large
                )
            }
        }
    }

    val bubbleBg = when {
        hasFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        isMe && message.status == MessageStatus.PENDING -> LivePrimaryEmerald.copy(alpha = 0.86f)
        isMe -> LivePrimaryEmerald
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        hasFailed -> MaterialTheme.colorScheme.onSurface
        isMe -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val linkColor = if (isMe && !hasFailed) Color.White else LivePrimaryEmerald

    val annotatedMessage = remember(message.text, textColor, linkColor) {
        buildLinkAnnotatedMessage(
            rawText = message.text,
            linkColor = linkColor
        )
    }
    val hasLinks = remember(annotatedMessage) {
        annotatedMessage.getStringAnnotations(tag = "URL", start = 0, end = annotatedMessage.length).isNotEmpty()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBg,
            border = when {
                hasFailed -> BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
                isMe -> null
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
            },
            modifier = Modifier.widthIn(min = 52.dp, max = 288.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                if (hasLinks) {
                    ClickableText(
                        text = annotatedMessage,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp
                        ),
                        onClick = { offset ->
                            annotatedMessage
                                .getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()
                                ?.let { annotation ->
                                    SafeExternalLinkHandler.openExternalUrl(context, annotation.item)
                                }
                        }
                    )
                } else {
                    Text(
                        text = message.text,
                        fontSize = 14.5.sp,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        if (hasFailed) {
            Row(
                modifier = Modifier.padding(top = 3.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (onRetryFailed != null) Modifier.clickable { onRetryFailed() } else Modifier)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Not delivered • Tap to retry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (onDismissFailed != null) {
                    Text(
                        text = "Dismiss",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDismissFailed() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        } else if (showMetaFooter || message.status == MessageStatus.PENDING) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (isMe && message.status == MessageStatus.PENDING) "Sending…" else formattedTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
                if (isMe) {
                    val statusIcon = when (message.status) {
                        MessageStatus.PENDING -> Icons.Default.Schedule
                        MessageStatus.SENT -> Icons.Default.Done
                        MessageStatus.DELIVERED -> Icons.Default.DoneAll
                        MessageStatus.READ -> Icons.Default.DoneAll
                    }
                    val statusTint = when (message.status) {
                        MessageStatus.READ -> LivePrimaryEmerald
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = message.status.name,
                        tint = statusTint,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private fun buildLinkAnnotatedMessage(
    rawText: String,
    linkColor: Color
) = buildAnnotatedString {
    append(rawText)
    val matcher = Patterns.WEB_URL.matcher(rawText)
    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        val rawUrl = rawText.substring(start, end)
        val normalizedUrl = if (rawUrl.startsWith("http://", ignoreCase = true) ||
            rawUrl.startsWith("https://", ignoreCase = true)
        ) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        addStyle(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium
            ),
            start = start,
            end = end
        )
        addStringAnnotation(
            tag = "URL",
            annotation = normalizedUrl,
            start = start,
            end = end
        )
    }
}

private fun buildChatTimeline(
    remoteMessages: List<FirestoreChatMessage>,
    outgoingQueue: List<OutgoingQueueItem>,
    currentUserId: String?
): List<ChatTimelineItem> {
    val unconfirmedOutgoing = outgoingQueue.filter { queued ->
        if (queued.errorMessage != null) {
            true
        } else {
            // Hide optimistic duplicate if server snapshot already arrived with matching sender+text within 30s
            remoteMessages.none { remote ->
                remote.senderUid == currentUserId &&
                    remote.text == queued.text &&
                    abs(remote.timestamp - queued.createdAt) < 30_000L
            }
        }
    }

    data class CombinedEntry(
        val message: FirestoreChatMessage,
        val localError: String?,
        val isLocalQueueItem: Boolean
    )

    val allEntries = buildList {
        remoteMessages.forEach { msg ->
            add(CombinedEntry(msg, localError = null, isLocalQueueItem = false))
        }
        if (!currentUserId.isNullOrBlank()) {
            unconfirmedOutgoing.forEach { local ->
                add(
                    CombinedEntry(
                        message = FirestoreChatMessage(
                            id = local.localId,
                            senderUid = currentUserId,
                            text = local.text,
                            timestamp = local.createdAt,
                            status = MessageStatus.PENDING
                        ),
                        localError = local.errorMessage,
                        isLocalQueueItem = true
                    )
                )
            }
        }
    }.sortedWith(compareBy<CombinedEntry> { it.message.timestamp }.thenBy { it.message.id })

    if (allEntries.isEmpty()) return emptyList()

    val nowMillis = System.currentTimeMillis()
    val items = ArrayList<ChatTimelineItem>(allEntries.size + 4)
    var currentDayBucket: Long? = null

    for (index in allEntries.indices) {
        val current = allEntries[index]
        val dayBucket = calendarDayKey(current.message.timestamp)
        val isNewDay = currentDayBucket != dayBucket
        if (isNewDay) {
            currentDayBucket = dayBucket
            items.add(
                ChatTimelineItem.DateHeader(
                    dayBucket = dayBucket,
                    label = formatChatDayLabel(current.message.timestamp, nowMillis)
                )
            )
        }

        val prev = allEntries.getOrNull(index - 1)
        val next = allEntries.getOrNull(index + 1)

        val groupedWithPrev = prev != null &&
            !isNewDay &&
            prev.localError == null &&
            current.localError == null &&
            prev.message.senderUid == current.message.senderUid &&
            (current.message.timestamp - prev.message.timestamp) in 0..GROUP_WINDOW_MS

        val groupedWithNext = next != null &&
            calendarDayKey(next.message.timestamp) == dayBucket &&
            next.localError == null &&
            current.localError == null &&
            next.message.senderUid == current.message.senderUid &&
            (next.message.timestamp - current.message.timestamp) in 0..GROUP_WINDOW_MS

        val position = when {
            groupedWithPrev && groupedWithNext -> MessageGroupPosition.MIDDLE
            !groupedWithPrev && groupedWithNext -> MessageGroupPosition.FIRST
            groupedWithPrev && !groupedWithNext -> MessageGroupPosition.LAST
            else -> MessageGroupPosition.SINGLE
        }

        items.add(
            ChatTimelineItem.MessageRow(
                message = current.message,
                groupPosition = position,
                showMetaFooter = !groupedWithNext,
                tightTopSpacing = groupedWithPrev,
                localError = current.localError,
                isLocalQueueItem = current.isLocalQueueItem
            )
        )
    }

    return items
}

private fun calendarDayKey(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val year = cal.get(Calendar.YEAR).toLong()
    val dayOfYear = cal.get(Calendar.DAY_OF_YEAR).toLong()
    return year * 1000L + dayOfYear
}

private fun formatChatDayLabel(timestamp: Long, nowMillis: Long): String {
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val yesterdayCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }

    val sameYear = msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
    return when {
        sameYear && msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "Today"
        msgCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            msgCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        sameYear -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
