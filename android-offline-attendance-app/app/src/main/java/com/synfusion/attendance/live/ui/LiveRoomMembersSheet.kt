package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveRoomMember
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.LivePrimaryMint
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar

/**
 * Real viewer list backed by persisted room membership.
 *
 * Counting convention (matching the badge on the room header): the displayed count excludes the
 * host, and the host is pinned first in the list with a HOST badge. Follow buttons use the existing
 * WorkDaily social graph through the ViewModel; host-only moderation is enforced again in Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomMembersSheet(
    members: List<LiveRoomMember>,
    isHost: Boolean,
    currentUid: String?,
    followingUids: Set<String>,
    followBusyUids: Set<String>,
    onFollowToggle: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(members, searchQuery) {
        if (searchQuery.isBlank()) {
            members
        } else {
            members.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val viewerCount = members.count { !it.isHost }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = LivePrimaryMint)
                    Text(
                        "Viewers ($viewerCount)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search viewers", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            if (members.isEmpty()) "No one has joined yet." else "No viewer matches that search.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(items = filtered, key = { it.uid }) { member ->
                        MemberRow(
                            member = member,
                            isSelf = member.uid == currentUid,
                            isFollowing = followingUids.contains(member.uid),
                            followBusy = followBusyUids.contains(member.uid),
                            canModerate = isHost && !member.isHost,
                            onFollowToggle = { onFollowToggle(member.uid) },
                            onOpenProfile = { onOpenProfile(member.uid) },
                            onRemove = { onRemoveMember(member.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: LiveRoomMember,
    isSelf: Boolean,
    isFollowing: Boolean,
    followBusy: Boolean,
    canModerate: Boolean,
    onFollowToggle: () -> Unit,
    onOpenProfile: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(enabled = !isSelf, onClick = onOpenProfile)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WorkDailyLiveAvatar(
            name = member.displayName,
            avatarUrl = member.photoUrl,
            size = 38.dp,
            isLive = false,
            showBadge = false
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = member.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (member.isVerified) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = LivePrimaryMint,
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (member.isHost) {
                    Surface(shape = RoundedCornerShape(4.dp), color = LivePrimaryEmerald) {
                        Text(
                            "HOST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (isSelf) {
                    Text("(you)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = buildString {
                    if (member.username.isNotBlank()) append("@").append(member.username)
                    if (member.profession.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(member.profession)
                    }
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isSelf && !member.isHost) {
            TextButton(onClick = onFollowToggle, enabled = !followBusy) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else LivePrimaryEmerald
                )
            }
        }

        if (canModerate) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Moderation", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove from room", fontSize = 12.sp) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}
