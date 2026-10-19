package com.synfusion.attendance.live.ui.components

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

// WorkDaily Live Design System Tokens
val LivePrimaryEmerald = Color(0xFF0F9F7A)
val LivePrimaryMint = Color(0xFF10B981)
val LiveDarkBg = Color(0xFF0A0D14)
val LiveSurfaceDark = Color(0xFF161C28)
val LiveDiamondColor = Color(0xFF00E5FF)
val LiveGold = Color(0xFFECB159)
val LiveRed = Color(0xFFE53935)
val LivePurple = Color(0xFF8B5CF6)

val LiveEmeraldGradient = Brush.horizontalGradient(
    colors = listOf(LivePrimaryEmerald, LivePrimaryMint)
)

/**
 * Minimal Live Story Avatar with thin red ring and compact bottom badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveStoryAvatar(
    name: String,
    avatarUrl: String = "",
    size: Dp = 54.dp,
    isLive: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val initials = remember(name) {
        val parts = name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        else if (name.isNotBlank()) name.take(2).uppercase()
        else "WD"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(size + 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .size(size + 6.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            // Thin red ring container when Live
            Box(
                modifier = Modifier
                    .size(size + 6.dp)
                    .clip(CircleShape)
                    .then(
                        if (isLive) Modifier.border(1.5.dp, LiveRed, CircleShape)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(LivePrimaryEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = (size.value * 0.35f).sp,
                        fontWeight = FontWeight.Bold,
                        color = LivePrimaryEmerald
                    )
                }
            }

            // Small red Live Badge attached near the bottom edge
            if (isLive) {
                Box(
                    modifier = Modifier
                        .offset(y = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LiveRed)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        Text(
            text = name.split(" ").firstOrNull() ?: name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Legacy Pulsing Live Avatar maintained for backwards compatibility.
 */
@Composable
fun PulsingLiveAvatar(
    name: String,
    avatarUrl: String = "",
    size: Dp = 54.dp,
    isLive: Boolean = true,
    onClick: () -> Unit = {}
) {
    LiveStoryAvatar(
        name = name,
        avatarUrl = avatarUrl,
        size = size,
        isLive = isLive,
        onClick = onClick
    )
}

/**
 * Small Red Live Badge Indicator.
 */
@Composable
fun LiveBadge(
    text: String = "LIVE",
    bgColor: Color = LiveRed
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = text.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

/**
 * Translucent Viewer Counter Chip.
 */
@Composable
fun ViewerCountChip(
    count: Int,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Viewers",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Minimal Diamond Balance Chip.
 */
@Composable
fun DiamondBalanceChip(
    balance: Int,
    onClick: () -> Unit = {},
    fontSize: Int = 11,
    iconSize: Dp = 14.dp
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color.Black.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Diamonds",
                tint = LiveDiamondColor,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = "$balance",
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Lightweight Follow Button.
 */
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onToggle,
            modifier = modifier.height(28.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Text("Following", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = onToggle,
            modifier = modifier.height(28.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LiveRed,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text("Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Compact Floating Go Live Pill Button.
 */
@Composable
fun GoLiveFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(21.dp),
        color = LiveRed,
        contentColor = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Go Live",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Clean Floating Chat Message Bubble without thick dark rectangles.
 */
@Composable
fun LiveCommentItem(
    message: ChatMessage,
    onUserClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.85f),
        blurRadius = 4f
    )

    if (message.isGiftAnnouncement) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(LiveGold.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = LiveGold, modifier = Modifier.size(12.dp))
            Text(
                text = "${message.senderName} ${message.message}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LiveGold,
                style = TextStyle(shadow = textShadow)
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.padding(vertical = 2.dp)
        ) {
            val initials = remember(message.senderName) {
                val parts = message.senderName.trim().split(" ")
                if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                else if (message.senderName.isNotBlank()) message.senderName.take(2).uppercase()
                else "U"
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .then(if (onUserClick != null) Modifier.clickable { onUserClick(message.senderId) } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = message.senderName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LivePrimaryMint,
                style = TextStyle(shadow = textShadow),
                modifier = if (onUserClick != null) Modifier.clickable { onUserClick(message.senderId) } else Modifier
            )

            Text(
                text = message.message,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                style = TextStyle(shadow = textShadow),
                softWrap = true
            )
        }
    }
}

/**
 * Reusable User Profile Bottom Sheet for all Live interactions (Host, Viewers, Chat).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveProfileBottomSheet(
    userId: String,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onNavigatePrivateChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val publicUsers by viewModel.publicUsers.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    val targetUser = remember(publicUsers, userId) {
        publicUsers.find { it.id == userId || it.name == userId } ?: DemoPublicUser(
            id = userId,
            name = if (userId == "creator_me" || userId == "user_me") "You (Host)" else userId,
            profession = "Site Worker",
            bio = "Active WorkDaily live participant",
            followerCount = 420,
            followingCount = 180,
            friendsCount = 35
        )
    }

    val isSelf = targetUser.id == "current_user" || targetUser.id == "creator_me" || targetUser.id == "user_me"
    val isFollowing = followedIds.contains(targetUser.id)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LiveDarkBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Header Avatar & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveStoryAvatar(
                    name = targetUser.name,
                    size = 64.dp,
                    isLive = targetUser.isLiveNow
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = targetUser.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (targetUser.isVerified) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = LivePrimaryMint, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = targetUser.profession,
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                if (targetUser.bio.isNotBlank()) {
                    Text(
                        text = targetUser.bio,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LiveSurfaceDark)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${targetUser.followerCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Followers", fontSize = 11.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${targetUser.followingCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Following", fontSize = 11.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${targetUser.friendsCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Friends", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Interactive Actions
            if (!isSelf) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleFollowCreator(targetUser.id) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) LiveSurfaceDark else LivePrimaryEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(21.dp)
                    ) {
                        Text(if (isFollowing) "Following" else "Follow", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (targetUser.isFriend) {
                                onDismiss()
                                onNavigatePrivateChat(targetUser.id)
                            } else {
                                val sent = viewModel.sendFriendRequest(targetUser.id)
                                if (sent) {
                                    Toast.makeText(context, "Friend Request Sent!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Friend request pending or connected", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LiveSurfaceDark, contentColor = Color.White),
                        shape = RoundedCornerShape(21.dp)
                    ) {
                        Icon(
                            imageVector = if (targetUser.isFriend) Icons.Default.Chat else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (targetUser.isFriend) "Message" else "Add Friend", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Live Viewers Scrollable Bottom Sheet with search and profile integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveViewerListBottomSheet(
    room: LiveRoom,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onSelectViewerProfile: (String) -> Unit
) {
    val creators by viewModel.creators.collectAsState()
    val publicUsers by viewModel.publicUsers.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val allViewers = remember(creators, publicUsers) {
        val list = mutableListOf<DemoPublicUser>()
        creators.forEach { c ->
            list.add(DemoPublicUser(id = c.id, name = c.name, profession = c.profession, followerCount = c.followerCount, isLiveNow = c.isLiveNow))
        }
        publicUsers.forEach { p ->
            if (list.none { it.id == p.id }) list.add(p)
        }
        list
    }

    val filteredViewers = remember(allViewers, searchQuery) {
        if (searchQuery.isBlank()) allViewers
        else allViewers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.profession.contains(searchQuery, ignoreCase = true)
        }
    }

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = LivePrimaryMint)
                    Text("Live Viewers (${room.viewerCount})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search viewers by name...", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LiveSurfaceDark,
                    unfocusedContainerColor = LiveSurfaceDark,
                    focusedBorderColor = LivePrimaryEmerald,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp)
            )

            LazyColumn(
                modifier = Modifier.heightIn(min = 200.dp, max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredViewers, key = { it.id }) { viewer ->
                    val isFollowing = followedIds.contains(viewer.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiveSurfaceDark)
                            .clickable { onSelectViewerProfile(viewer.id) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            LiveStoryAvatar(name = viewer.name, size = 36.dp, isLive = viewer.isLiveNow)
                            Column {
                                Text(viewer.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(viewer.profession, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        FollowButton(
                            isFollowing = isFollowing,
                            onToggle = { viewModel.toggleFollowCreator(viewer.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomCategoryChip(category: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = LivePrimaryEmerald.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.3f))
    ) {
        Text(
            text = category,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = LivePrimaryMint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun getGiftIcon(giftName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        giftName.contains("Helmet", ignoreCase = true) || giftName.contains("Hardhat", ignoreCase = true) -> Icons.Default.Shield
        giftName.contains("Tape", ignoreCase = true) -> Icons.Default.Straighten
        giftName.contains("Brick", ignoreCase = true) -> Icons.Default.HomeWork
        giftName.contains("Trowel", ignoreCase = true) -> Icons.Default.Construction
        giftName.contains("Drill", ignoreCase = true) -> Icons.Default.Build
        giftName.contains("Scaffold", ignoreCase = true) -> Icons.Default.Apartment
        giftName.contains("Truck", ignoreCase = true) || giftName.contains("Hauler", ignoreCase = true) -> Icons.Default.LocalShipping
        giftName.contains("Mixer", ignoreCase = true) -> Icons.Default.Autorenew
        giftName.contains("Excavator", ignoreCase = true) || giftName.contains("Bulldozer", ignoreCase = true) -> Icons.Default.Agriculture
        giftName.contains("Crane", ignoreCase = true) -> Icons.Default.Architecture
        giftName.contains("Saw", ignoreCase = true) -> Icons.Default.ContentCut
        giftName.contains("Solar", ignoreCase = true) -> Icons.Default.WbSunny
        giftName.contains("Beam", ignoreCase = true) -> Icons.Default.HorizontalRule
        giftName.contains("Compass", ignoreCase = true) -> Icons.Default.Explore
        giftName.contains("Trophy", ignoreCase = true) -> Icons.Default.EmojiEvents
        else -> Icons.Default.Construction
    }
}
