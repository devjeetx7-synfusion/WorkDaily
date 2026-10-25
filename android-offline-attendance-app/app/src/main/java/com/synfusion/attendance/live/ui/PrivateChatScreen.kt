package com.synfusion.attendance.live.ui

import android.widget.Toast
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.FirestoreBlockRepository
import com.synfusion.attendance.online.FirestoreChatRepository
import com.synfusion.attendance.online.FirestoreFollowRepository
import com.synfusion.attendance.online.FirestorePublicProfileRepository
import com.synfusion.attendance.online.FirestoreChatMessage
import com.synfusion.attendance.online.MessageStatus
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
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
    val currentUserId = remember {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    val followRepo = remember { FirestoreFollowRepository() }
    val publicProfileRepo = remember { FirestorePublicProfileRepository() }
    val blockRepo = remember { FirestoreBlockRepository() }
    val chatRepo = remember { FirestoreChatRepository() }
    val coroutineScope = rememberCoroutineScope()

    var isCheckingMutual by remember { mutableStateOf(true) }
    var isMutualFriend by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var friendProfile by remember { mutableStateOf<PublicProfile?>(null) }

    val conversationId = remember(currentUserId, friendId) {
        if (currentUserId != null && friendId.isNotBlank()) chatRepo.getConversationId(currentUserId, friendId) else ""
    }

    val messagesState = produceState<List<FirestoreChatMessage>>(initialValue = emptyList(), key1 = conversationId) {
        if (conversationId.isNotBlank()) {
            chatRepo.observeMessages(conversationId).collect { value = it }
        }
    }
    val messages = messagesState.value

    // Check mutual friendship and block status on Firestore
    LaunchedEffect(currentUserId, friendId) {
        isCheckingMutual = true
        if (currentUserId != null && friendId.isNotBlank()) {
            isBlocked = blockRepo.isEitherBlocked(currentUserId, friendId)
            val mutual = followRepo.isMutual(currentUserId, friendId)
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

    var messageInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto scroll to latest message when messages list updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val friendName = friendProfile?.displayName ?: "WorkDaily Member"
    val friendAvatar = friendProfile?.photoUrl ?: ""
    val isVerified = friendProfile?.isVerified ?: false

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateProfile(friendId) }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        WorkDailyLiveAvatar(
                            name = friendName,
                            avatarUrl = friendAvatar,
                            size = 38.dp,
                            isLive = false,
                            showBadge = false
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isVerified) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = LivePrimaryEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isMutualFriend) "Mutual Friend • Active" else "Follower",
                                fontSize = 11.sp,
                                color = if (isMutualFriend) LivePrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(com.synfusion.attendance.ui.components.icons.WorkDailyIcons.ArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(com.synfusion.attendance.ui.components.icons.WorkDailyIcons.MoreVertical, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Profile") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateProfile(friendId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isCheckingMutual) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LivePrimaryEmerald)
                }
            } else if (isBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Text(text = "Messaging Unavailable", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "You cannot send or receive messages from this account due to block restrictions.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else if (!isMutualFriend) {
                // FRIENDS-ONLY LOCKED SCREEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Friends Only",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
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
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { onNavigateProfile(friendId) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Profile & Follow")
                        }
                    }
                }
            } else {
                // Messages Feed
                if (messages.isEmpty()) {
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
                            WorkDailyLiveAvatar(
                                name = friendName,
                                avatarUrl = friendAvatar,
                                size = 56.dp,
                                isLive = false
                            )
                            Text(
                                text = "Send a message to $friendName",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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

                // Theme-Aware Compact Composer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Rounded Input Field
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 110.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (messageInput.isEmpty()) {
                                Text(
                                    text = "Message...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(LivePrimaryEmerald),
                                maxLines = 4
                            )
                        }
                    }

                    // Active Circular Send Button
                    IconButton(
                        onClick = {
                            val textToSend = messageInput.trim()
                            if (textToSend.isNotBlank() && currentUserId != null) {
                                messageInput = ""
                                coroutineScope.launch {
                                    when (val result = chatRepo.sendMessage(currentUserId, friendId, textToSend)) {
                                        is ProfileOperationResult.Error -> {
                                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                        }
                                        is ProfileOperationResult.Success -> {}
                                    }
                                }
                            }
                        },
                        enabled = messageInput.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageInput.isNotBlank()) LivePrimaryEmerald
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (messageInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
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

    val bubbleBg = if (isMe) LivePrimaryEmerald else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

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
