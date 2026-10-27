package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
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
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.FirestoreConversation
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.SocialSessionCache
import com.synfusion.attendance.online.rememberCurrentFirebaseUid
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf

/**
 * Chat list screen showing recent conversations.
 * Ordered by last message timestamp (newest first).
 * Supports opening an existing conversation or starting a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePrivateChat: (String) -> Unit,
    onNavigateUserProfile: (String) -> Unit
) {
    val currentUserId = rememberCurrentFirebaseUid().value
    val activeViewerUid: String? = currentUserId?.takeIf { it.isNotBlank() }

    // chatRepository lives on the SocialSessionCache singleton (SocialSessionManager exposes the
    // social snapshot, not the repositories) - same accessor PrivateChatScreen uses.
    val chatRepository = SocialSessionCache.chatRepository

    val conversations by remember(activeViewerUid) {
        activeViewerUid?.let { chatRepository.observeConversations(it) }
            ?: flowOf(emptyList<FirestoreConversation>())
    }.collectAsStateWithLifecycle(initialValue = emptyList<FirestoreConversation>())

    val socialSnapshot by SocialSessionCache.manager.snapshot.collectAsStateWithLifecycle(
        initialValue = SocialSessionCache.manager.snapshot.value
    )

    // Helper to get the other participant's UID
    fun otherParticipantUid(conv: FirestoreConversation): String? =
        conv.participants.firstOrNull { it.isNotBlank() && it != activeViewerUid }

    val participantUids = remember(conversations, activeViewerUid) {
        conversations
            .mapNotNull { otherParticipantUid(it) }
            .distinct()
    }

    // Resolve real public profiles for every conversation participant through the existing
    // SocialSessionCache manager. It caches into the snapshot StateFlow this screen already
    // observes, so no second profile store or messaging repository is introduced.
    LaunchedEffect(participantUids, activeViewerUid) {
        val viewerUid = activeViewerUid ?: return@LaunchedEffect
        participantUids.forEach { uid ->
            val alreadyCached = SocialSessionCache.manager.snapshot.value.profiles[uid]?.value != null
            if (!alreadyCached) {
                SocialSessionCache.manager.getPublicProfile(ownerUid = viewerUid, targetUid = uid)
            }
        }
    }

    // Resolves a participant profile from the shared social snapshot, honouring the same
    // owner-UID isolation guard PrivateChatScreen applies so a stale cache from a previously
    // signed-in account can never be displayed.
    fun resolveProfile(uid: String): PublicProfile? =
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.profiles[uid]?.value
        } else {
            null
        }

    val unreadTotal = conversations.sumOf { conv ->
        conv.unreadCounts[activeViewerUid ?: ""] ?: 0
    }

    Column(modifier = Modifier.fillMaxSize()) {
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (conversations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                    items = conversations.sortedByDescending { it.lastTimestamp },
                    key = { it.id }
                ) { conv ->
                    val otherUid = otherParticipantUid(conv) ?: ""
                    val otherProfile = resolveProfile(otherUid)
                    val otherName = otherProfile?.displayName?.takeIf { it.isNotBlank() }
                        ?: otherProfile?.username?.takeIf { it.isNotBlank() }
                        ?: "Unknown User"
                    val otherPhotoUrl = otherProfile?.photoUrl
                    val unreadCount = conv.unreadCounts[activeViewerUid ?: ""] ?: 0
                    val isUnread = unreadCount > 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable(enabled = otherUid.isNotBlank()) {
                                onNavigatePrivateChat(otherUid)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkDailyLiveAvatar(
                            name = otherName,
                            avatarUrl = otherPhotoUrl.orEmpty(),
                            size = 48.dp,
                            isLive = false,
                            showBadge = false
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = otherName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isUnread) {
                                    Text(
                                        text = kotlin.math.min(unreadCount, 99).toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.lastMessage.ifEmpty { "Say hello!" },
                                    fontSize = 14.sp,
                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatTimestamp(conv.lastTimestamp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isUnread) {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(start = 72.dp))
                }
            }
        }

        // Start new conversation button
        if (conversations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { /* Open user picker to start new conversation */ },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start New Conversation", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
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
