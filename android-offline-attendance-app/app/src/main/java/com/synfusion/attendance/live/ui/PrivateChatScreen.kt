package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.auth.FirebaseAuth
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.FirestoreBlockRepository
import com.synfusion.attendance.online.FirestoreChatMessage
import com.synfusion.attendance.online.FirestoreChatRepository
import com.synfusion.attendance.online.FirestoreFollowRepository
import com.synfusion.attendance.online.FirestorePublicProfileRepository
import com.synfusion.attendance.online.MessageStatus
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val currentUserId = remember {
        try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    val followRepo = remember { FirestoreFollowRepository() }
    val publicProfileRepo = remember { FirestorePublicProfileRepository() }
    val blockRepo = remember { FirestoreBlockRepository() }
    val chatRepo = remember { FirestoreChatRepository() }
    val coroutineScope = rememberCoroutineScope()

    var isCheckingMutual by remember(friendId) { mutableStateOf(true) }
    var isMutualFriend by remember(friendId) { mutableStateOf(false) }
    var isBlocked by remember(friendId) { mutableStateOf(false) }
    var isBlockedByMe by remember(friendId) { mutableStateOf(false) }
    var isBlockActionBusy by remember(friendId) { mutableStateOf(false) }
    var showBlockConfirmDialog by remember(friendId) { mutableStateOf(false) }
    var friendProfile by remember(friendId) { mutableStateOf<PublicProfile?>(null) }

    val conversationId = remember(currentUserId, friendId) {
        if (currentUserId != null && friendId.isNotBlank()) chatRepo.getConversationId(currentUserId, friendId) else ""
    }

    val messagesState = produceState<List<FirestoreChatMessage>>(initialValue = emptyList(), key1 = conversationId) {
        if (conversationId.isNotBlank()) {
            chatRepo.observeMessages(conversationId).collect { value = it }
        }
    }
    val messages = messagesState.value

    suspend fun refreshChatRelationshipState(showInitialSpinner: Boolean) {
        if (showInitialSpinner && friendProfile == null) {
            isCheckingMutual = true
        }
        if (!currentUserId.isNullOrBlank() && friendId.isNotBlank()) {
            val blockedByCurrent = blockRepo.isBlocked(currentUserId, friendId)
            val blockedByOther = blockRepo.isBlocked(friendId, currentUserId)
            isBlockedByMe = blockedByCurrent
            isBlocked = blockedByCurrent || blockedByOther

            val mutual = if (!isBlocked) followRepo.isMutual(currentUserId, friendId) else false
            isMutualFriend = mutual

            val profRes = publicProfileRepo.getPublicProfile(friendId)
            if (profRes is ProfileOperationResult.Success) {
                friendProfile = profRes.data
            }

            if (mutual && !isBlocked && conversationId.isNotBlank()) {
                chatRepo.markMessagesAsRead(conversationId, currentUserId)
            }
        }
        isCheckingMutual = false
    }

    LaunchedEffect(currentUserId, friendId) {
        refreshChatRelationshipState(showInitialSpinner = true)
    }

    DisposableEffect(lifecycleOwner, currentUserId, friendId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    refreshChatRelationshipState(showInitialSpinner = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var messageInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto scroll to latest message & mark incoming messages read
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            val lastMsg = messages.lastOrNull()
            if (currentUserId != null &&
                isMutualFriend &&
                !isBlocked &&
                conversationId.isNotBlank() &&
                lastMsg != null &&
                lastMsg.senderUid != currentUserId &&
                lastMsg.status != MessageStatus.READ
            ) {
                chatRepo.markMessagesAsRead(conversationId, currentUserId)
            }
        }
    }

    val friendName = friendProfile?.displayName?.takeIf { it.isNotBlank() } ?: "WorkDaily Member"
    val friendUsername = friendProfile?.username?.takeIf { it.isNotBlank() }
    val friendAvatar = friendProfile?.photoUrl ?: ""
    val isVerified = friendProfile?.isVerified ?: false

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
                    text = "Blocking disables messaging and removes any follow relationship between you. You can unblock them anytime from their profile or Settings → Privacy & Safety → Blocked Users.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUserId.isNullOrBlank() || isBlockActionBusy) return@Button
                        isBlockActionBusy = true
                        coroutineScope.launch {
                            try {
                                when (val res = blockRepo.blockUser(currentUserId, friendId)) {
                                    is ProfileOperationResult.Success -> {
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
                    if (isBlockActionBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Block", fontWeight = FontWeight.Bold)
                    }
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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateProfile(friendId) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            WorkDailyLiveAvatar(
                                name = friendName,
                                avatarUrl = friendAvatar,
                                size = 40.dp,
                                isLive = false,
                                showBadge = false
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = friendName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isVerified) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = LivePrimaryEmerald,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = when {
                                        isBlocked -> "Messaging unavailable"
                                        isMutualFriend -> "Mutual Friend • Active"
                                        friendUsername != null -> "@$friendUsername"
                                        else -> "WorkDaily Member"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isMutualFriend && !isBlocked) {
                                        LivePrimaryEmerald
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = WorkDailyIcons.ArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = WorkDailyIcons.MoreVertical,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurface
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
                                            if (isBlockedByMe) {
                                                isBlockActionBusy = true
                                                coroutineScope.launch {
                                                    try {
                                                        when (val res = blockRepo.unblockUser(currentUserId, friendId)) {
                                                            is ProfileOperationResult.Success -> {
                                                                refreshChatRelationshipState(showInitialSpinner = false)
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isCheckingMutual) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LivePrimaryEmerald)
                }
            } else if (isBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Messaging Unavailable",
                                fontSize = 18.sp,
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
                                OutlinedButton(
                                    onClick = {
                                        if (isBlockActionBusy) return@OutlinedButton
                                        isBlockActionBusy = true
                                        coroutineScope.launch {
                                            try {
                                                when (val res = blockRepo.unblockUser(currentUserId, friendId)) {
                                                    is ProfileOperationResult.Success -> {
                                                        refreshChatRelationshipState(showInitialSpinner = false)
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
                                    Text("Unblock User", color = LivePrimaryEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (!isMutualFriend) {
                // FRIENDS-ONLY LOCKED SCREEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(LivePrimaryEmerald.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Friends Only",
                                    tint = LivePrimaryEmerald,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Text(
                                text = "Friends-Only Messaging",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Messaging is locked because you and $friendName are not mutual friends yet. Both of you must follow each other to start chatting.",
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
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("View Profile & Follow", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Messages Feed or Balanced Empty Conversation State
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.widthIn(max = 340.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WorkDailyLiveAvatar(
                                    name = friendName,
                                    avatarUrl = friendAvatar,
                                    size = 68.dp,
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
                                    text = "Send a message to $friendName",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Say hello to start your conversation. Messages are shared privately between mutual friends.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        items(messages, key = { it.id }) { msg ->
                            PrivateMessageBubble(message = msg, currentUserId = currentUserId)
                        }
                    }
                }

                // Bottom Composer Surface: Handles IME + Gesture Navigation Bar without overlap or duplicate padding
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        if (sendError != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sendError ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { sendError = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        val trimmedMessage = messageInput.trim()
                        val canSend = trimmedMessage.isNotEmpty() && !isSending && !currentUserId.isNullOrBlank()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Rounded Input Pill (48dp baseline height matching 48dp Send button)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp, max = 120.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (trimmedMessage.isNotEmpty()) {
                                        LivePrimaryEmerald.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                    }
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (messageInput.isEmpty()) {
                                        Text(
                                            text = "Type a message...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    BasicTextField(
                                        value = messageInput,
                                        onValueChange = {
                                            messageInput = it
                                            if (sendError != null) sendError = null
                                        },
                                        enabled = !isSending,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        ),
                                        cursorBrush = SolidColor(LivePrimaryEmerald),
                                        maxLines = 4
                                    )
                                }
                            }

                            // Aligned 48dp Circular Send Button
                            IconButton(
                                onClick = {
                                    val senderUid = currentUserId
                                    val textToSend = messageInput.trim()
                                    if (textToSend.isBlank() || senderUid.isNullOrBlank() || isSending) {
                                        return@IconButton
                                    }

                                    sendError = null
                                    isSending = true
                                    messageInput = ""

                                    coroutineScope.launch {
                                        try {
                                            when (val result = chatRepo.sendMessage(senderUid, friendId, textToSend)) {
                                                is ProfileOperationResult.Success -> {
                                                    sendError = null
                                                }
                                                is ProfileOperationResult.Error -> {
                                                    if (messageInput.isBlank()) {
                                                        messageInput = textToSend
                                                    }
                                                    sendError = result.message
                                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            if (messageInput.isBlank()) {
                                                messageInput = textToSend
                                            }
                                            val errMsg = e.message ?: "Failed to send message"
                                            sendError = errMsg
                                            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isSending = false
                                        }
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSending -> LivePrimaryEmerald.copy(alpha = 0.7f)
                                            canSend -> LivePrimaryEmerald
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Message",
                                        tint = if (canSend) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
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
 * Modern Chat Message Bubble with theme-aware styling.
 */
@Composable
fun PrivateMessageBubble(message: FirestoreChatMessage, currentUserId: String?) {
    val isMe = message.senderUid == currentUserId

    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    val bubbleBg = if (isMe) LivePrimaryEmerald else MaterialTheme.colorScheme.surface
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = bubbleBg,
            border = if (isMe) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 19.sp
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.75f)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val tickIcon = when (message.status) {
                            MessageStatus.PENDING -> Icons.Default.Schedule
                            MessageStatus.SENT -> Icons.Default.Done
                            MessageStatus.DELIVERED -> Icons.Default.DoneAll
                            MessageStatus.READ -> Icons.Default.DoneAll
                        }
                        val tickTint = if (message.status == MessageStatus.READ) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.85f)

                        Icon(
                            imageVector = tickIcon,
                            contentDescription = message.status.name,
                            tint = tickTint,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
