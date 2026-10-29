package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.ChatListState
import com.synfusion.attendance.online.FirestoreConversation
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.SocialSessionCache
import com.synfusion.attendance.online.otherParticipant
import com.synfusion.attendance.online.rememberCurrentFirebaseUid
import com.synfusion.attendance.online.unreadTotalFor
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf

/**
 * Chat list screen showing the signed-in user's real conversations.
 *
 * Data flow: [rememberCurrentFirebaseUid] supplies the authenticated UID, which is the only key
 * used to query `conversations` -- so the list can never contain another account's threads.
 * Ordering is newest-first by `lastTimestamp`, applied client-side because the server-side
 * `orderBy` that used to do it required a Firestore composite index this project does not declare.
 *
 * Loading, empty and error are three distinct states. They were previously collapsed into one:
 * a failed query emitted an empty list, so a backend error rendered as "No messages yet" and
 * looked like a user with no chats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePrivateChat: (String) -> Unit,
    onNavigateUserProfile: (String) -> Unit,
    onNavigatePeople: () -> Unit = {}
) {
    val currentUserId = rememberCurrentFirebaseUid().value
    val chatRepository = SocialSessionCache.chatRepository

    // Bumped by the error state's Retry action. It is a remember key so incrementing it rebuilds
    // the flow and re-subscribes the Firestore listener, giving a genuine retry rather than a
    // button that only recomposes the same failed state.
    var retryToken by remember { mutableStateOf(0) }

    // The flow is rebuilt whenever the authenticated UID changes, which is what keeps a previous
    // account's conversations off screen after a switch or sign-out. Building it inside `remember`
    // and collecting unconditionally also avoids calling a @Composable inside an `if`, which
    // destabilises the slot table when the UID transitions between null and non-null.
    val conversationsFlow = remember(currentUserId, chatRepository, retryToken) {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            flowOf(ChatListState.Loaded(emptyList<FirestoreConversation>()))
        } else {
            chatRepository.observeConversationsState(uid)
        }
    }
    val listState by conversationsFlow.collectAsStateWithLifecycle(
        initialValue = ChatListState.Loading
    )

    val conversations = (listState as? ChatListState.Loaded)?.conversations ?: emptyList()

    val socialSnapshot by SocialSessionCache.manager.snapshot.collectAsStateWithLifecycle(
        initialValue = SocialSessionCache.manager.snapshot.value
    )

    // Peer resolution lives on FirestoreConversation.otherParticipant so the filtering rule is
    // shared with, and covered by, ChatListMappingTest rather than duplicated inline here.
    fun otherParticipantUid(conv: FirestoreConversation): String? = conv.otherParticipant(currentUserId)

    /**
     * Resolves a participant's public profile from the shared, UID-scoped session cache.
     *
     * This reads only from [socialSnapshot] and refuses to serve entries whose `ownerUid` is not
     * the current account. The screen previously kept its own `remember { mutableMapOf() }` memo
     * on top of the snapshot; because that map was not keyed on the user, it survived an account
     * switch and could label a new account's rows with the previous account's cached names and
     * avatars. Dropping the local memo removes that leak by construction -- the snapshot is
     * already cleared atomically on sign-out by SocialSessionManager.
     */
    fun resolveProfile(uid: String): PublicProfile? {
        if (uid.isBlank()) return null
        val ownerUid = socialSnapshot.ownerUid
        if (ownerUid == null || ownerUid != currentUserId) return null
        return socialSnapshot.profiles[uid]?.value
    }

    // Populate the shared snapshot cache for each conversation participant. Without this the
    // snapshot holds no entry for them and every row falls through resolveProfile() to
    // "Unknown User". This reuses the existing SocialSessionCache manager, which writes into the
    // same snapshot StateFlow observed above, so no second profile store or chat system is added.
    val participantUids = remember(conversations, currentUserId) {
        conversations
            .mapNotNull { otherParticipantUid(it) }
            .distinct()
    }

    LaunchedEffect(participantUids, currentUserId) {
        val viewerUid = currentUserId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        participantUids.forEach { uid ->
            val alreadyCached = SocialSessionCache.manager.snapshot.value.profiles[uid]?.value != null
            if (!alreadyCached) {
                SocialSessionCache.manager.getPublicProfile(ownerUid = viewerUid, targetUid = uid)
            }
        }
    }

    // Per-participant counters: summing the maps directly would also credit the viewer with the
    // peer's unread messages.
    val unreadTotal = conversations.unreadTotalFor(currentUserId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        TopAppBar(
            title = { Text("Messages", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(WorkDailyIcons.ArrowLeft, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        if (unreadTotal > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = WorkDailyIcons.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$unreadTotal unread message${if (unreadTotal != 1) "s" else ""}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        when (listState) {
            is ChatListState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading your conversations…",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ChatListState.Error -> {
                // A failed query used to be indistinguishable from an empty inbox. Show the actual
                // failure and offer a retry that re-subscribes the listener.
                val message = (listState as ChatListState.Error).message
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = WorkDailyIcons.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Couldn't load messages",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { retryToken++ }) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is ChatListState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (conversations.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = WorkDailyIcons.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No messages yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start a conversation with someone you follow.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(
                            items = conversations,
                            key = { it.id }
                        ) { conv ->
                            val otherUid = otherParticipantUid(conv) ?: ""
                            val otherProfile = resolveProfile(otherUid)
                            val otherName = when {
                                otherProfile == null -> "Unknown User"
                                otherProfile.displayName.isNotBlank() -> otherProfile.displayName
                                otherProfile.username.isNotBlank() -> otherProfile.username
                                else -> "Unknown User"
                            }
                            val otherPhotoUrl = otherProfile?.photoUrl
                            val unreadCount = conv.unreadCounts[currentUserId ?: ""] ?: 0
                            val isUnread = unreadCount > 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable {
                                        if (otherUid.isNotBlank()) onNavigatePrivateChat(otherUid)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tapping the avatar opens the participant's existing profile;
                                // tapping anywhere else on the row opens the conversation.
                                Box(
                                    modifier = Modifier.clickable {
                                        if (otherUid.isNotBlank()) onNavigateUserProfile(otherUid)
                                    }
                                ) {
                                    WorkDailyLiveAvatar(
                                        name = otherName,
                                        avatarUrl = otherPhotoUrl.orEmpty(),
                                        size = 48.dp,
                                        isLive = false,
                                        showBadge = false
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = otherName,
                                            fontSize = 15.sp,
                                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formatTimestamp(conv.lastTimestamp),
                                            fontSize = 12.sp,
                                            color = if (isUnread) LivePrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = conv.lastMessage.ifEmpty { "Say hello!" },
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isUnread) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            UnreadBadge(count = unreadCount)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth().padding(start = 72.dp)
                            )
                        }
                    }
                }

                // Routes to the existing people directory, which already starts real conversations
                // via getOrCreateConversation. Conversation IDs are deterministic
                // (min(uid)_max(uid)), so re-opening a thread never creates a duplicate.
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigatePeople,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LivePrimaryEmerald,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "New message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Message",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Reusable compact unread count badge.
 * Uses a circle shape for single-digit counts (1-9) and a rounded pill for multi-digit counts (10+).
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    val text = if (count > 99) "99+" else count.toString()
    val isSingleDigit = count in 1..9
    val badgeShape = if (isSingleDigit) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier,
        shape = badgeShape,
        color = MaterialTheme.colorScheme.error,
        contentColor = Color.White
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = if (isSingleDigit) 20.dp else 24.dp, minHeight = 20.dp)
                .padding(horizontal = if (isSingleDigit) 4.dp else 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val msgDate = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    return if (msgDate == today) {
        java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            .toString().substring(0, 5) // HH:mm
    } else if (msgDate == today.minusDays(1)) {
        "Yesterday"
    } else {
        msgDate.toString()
    }
}
