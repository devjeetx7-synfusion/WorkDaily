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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
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
 * Extracts the user's first name for compact Live headers and chips.
 * Prioritizes the first name (e.g. "Rajesh Sharma" -> "Rajesh", "Mohammed Abdul Rahman" -> "Mohammed").
 * Preserves unicode, single-word names, and non-Latin scripts cleanly.
 */
fun extractFirstName(fullName: String?): String {
    if (fullName.isNullOrBlank()) return "User"
    val trimmed = fullName.trim()
    val parts = trimmed.split(Regex("\\s+"))
    return parts.firstOrNull { it.isNotBlank() } ?: "User"
}

/**
 * Animated Live Ring: Continuous subtle pulse wave around live broadcasting avatars.
 */
@Composable
fun AnimatedLiveRing(
    modifier: Modifier = Modifier,
    isLive: Boolean = true
) {
    if (!isLive) return

    val infiniteTransition = rememberInfiniteTransition(label = "liveRingTransition")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Box(
        modifier = modifier
            .scale(waveScale)
            .border(
                width = 1.5.dp,
                color = LiveRed.copy(alpha = waveAlpha),
                shape = CircleShape
            )
    )
}

/**
 * Small, compact LIVE indicator badge (does not obstruct face).
 */
@Composable
fun CompactLiveBadge(
    text: String = "LIVE",
    bgColor: Color = LiveRed,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(16.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = bgColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = text.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

/**
 * WorkDaily Global User / Live Avatar component.
 * Displays photo with center-crop or fallback initials, subtle border,
 * subtle animated live ring when broadcasting.
 *
 * CRITICAL RULE: Animated Live avatar ring = NO extra LIVE text badge beside or below avatar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkDailyLiveAvatar(
    name: String,
    avatarUrl: String? = null,
    size: Dp = 56.dp,
    isLive: Boolean = false,
    showBadge: Boolean = false, // Universal Rule: Animated LIVE avatar ring = no extra LIVE text badge
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initials = remember(name) {
        val parts = name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        else if (name.isNotBlank()) name.take(2).uppercase()
        else "WD"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + 10.dp)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else Modifier
            )
    ) {
        // Subtle Pulse Ring when broadcasting
        if (isLive) {
            AnimatedLiveRing(
                modifier = Modifier.size(size + 8.dp),
                isLive = true
            )
        }

        // Outer static border ring
        Box(
            modifier = Modifier
                .size(size + 4.dp)
                .clip(CircleShape)
                .then(
                    if (isLive) Modifier.border(1.5.dp, LiveRed, CircleShape)
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Avatar Circle
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(LivePrimaryEmerald.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNull_or_blank()) {
                    val request = remember(avatarUrl) {
                        ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build()
                    }
                    SubcomposeAsyncImage(
                        model = request,
                        contentDescription = "$name Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = {
                            Text(
                                text = initials,
                                fontSize = (size.value * 0.36f).sp,
                                fontWeight = FontWeight.Bold,
                                color = LivePrimaryEmerald
                            )
                        }
                    )
                } else {
                    Text(
                        text = initials,
                        fontSize = (size.value * 0.36f).sp,
                        fontWeight = FontWeight.Bold,
                        color = LivePrimaryEmerald
                    )
                }
            }
        }

        // Only draw extra text badge if explicitly requested and NOT live
        if (!isLive && showBadge) {
            CompactLiveBadge(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 3.dp)
            )
        }
    }
}

/**
 * Minimal Live Story Avatar with label.
 */
@Composable
fun LiveStoryAvatar(
    name: String,
    avatarUrl: String = "",
    size: Dp = 54.dp,
    isLive: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(size + 12.dp)
    ) {
        WorkDailyLiveAvatar(
            name = name,
            avatarUrl = avatarUrl,
            size = size,
            isLive = isLive,
            showBadge = false,
            onClick = onClick,
            onLongClick = onLongClick
        )

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
 * Alias for PulsingLiveAvatar maintained for backwards compatibility.
 */
@Composable
fun PulsingLiveAvatar(
    name: String,
    avatarUrl: String = "",
    size: Dp = 54.dp,
    isLive: Boolean = true,
    onClick: () -> Unit = {}
) {
    WorkDailyLiveAvatar(
        name = name,
        avatarUrl = avatarUrl,
        size = size,
        isLive = isLive,
        showBadge = false,
        onClick = onClick
    )
}

/**
 * Legacy LiveBadge wrapper calling CompactLiveBadge.
 */
@Composable
fun LiveBadge(
    text: String = "LIVE",
    bgColor: Color = LiveRed
) {
    CompactLiveBadge(text = text, bgColor = bgColor)
}

/**
 * Translucent Viewer Counter Chip.
 */
@Composable
fun ViewerCountChip(
    count: Int,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                color = Color.White,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
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
        color = Color.Black.copy(alpha = 0.5f)
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
 * WorkDaily Branded Follow Button using LivePrimaryEmerald (`0xFF0F9F7A`).
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
            modifier = modifier
                .height(28.dp)
                .defaultMinSize(minWidth = 64.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.15f),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Following",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    } else {
        Button(
            onClick = onToggle,
            modifier = modifier
                .height(28.dp)
                .defaultMinSize(minWidth = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LivePrimaryEmerald,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Follow",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
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
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = LivePrimaryEmerald,
        contentColor = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Go Live",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Floating Chat Comment Item.
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
 * Reusable User Profile Bottom Sheet for Live stream overlays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveProfileBottomSheet(
    userId: String,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onNavigatePrivateChat: (String) -> Unit = {},
    onNavigateFullProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val publicUsers by viewModel.publicUsers.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    val targetUser = remember(publicUsers, userId) {
        publicUsers.find { it.id == userId || it.name == userId } ?: DemoPublicUser(
            id = userId,
            name = if (userId == "creator_me" || userId == "user_me" || userId == "current_user") "You (Host)" else userId,
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
        containerColor = Color(0xFF0B101A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        windowInsets = WindowInsets(0.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B101A))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Single Drag Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Header Avatar & Identity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WorkDailyLiveAvatar(
                    name = targetUser.name,
                    avatarUrl = targetUser.avatar,
                    size = 72.dp,
                    isLive = targetUser.isLiveNow,
                    showBadge = false
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = targetUser.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (targetUser.isVerified) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = LivePrimaryMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LivePrimaryEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = targetUser.profession,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LivePrimaryMint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }

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

            // Compact Statistics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LiveSurfaceDark)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${targetUser.followerCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Followers", fontSize = 11.sp, color = Color.Gray)
                }
                Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color.White.copy(alpha = 0.15f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${targetUser.followingCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Following", fontSize = 11.sp, color = Color.Gray)
                }
                Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color.White.copy(alpha = 0.15f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${targetUser.friendsCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Friends", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Interactive Action Buttons
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
                        shape = RoundedCornerShape(12.dp)
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (targetUser.isFriend) Icons.AutoMirrored.Filled.Chat else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (targetUser.isFriend) "Message" else "Add Friend", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (onNavigateFullProfile != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateFullProfile(targetUser.id)
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LivePrimaryMint),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View Full Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Live Viewers Scrollable Bottom Sheet with room-specific participant presence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveViewerListBottomSheet(
    room: LiveRoom,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onSelectViewerProfile: (String) -> Unit
) {
    val roomParticipantsMap by viewModel.roomParticipantsMap.collectAsState()
    val publicUsers by viewModel.publicUsers.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val roomParticipants = remember(roomParticipantsMap, room.id, publicUsers) {
        val specificList = roomParticipantsMap[room.id]
        if (!specificList.isNullOrEmpty()) {
            specificList
        } else {
            publicUsers.map { u ->
                RoomParticipant(
                    id = u.id,
                    name = u.name,
                    avatar = u.avatar,
                    profession = u.profession,
                    followerCount = u.followerCount,
                    isLiveNow = u.isLiveNow
                )
            }
        }
    }

    val filteredViewers = remember(roomParticipants, searchQuery) {
        if (searchQuery.isBlank()) roomParticipants
        else roomParticipants.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.profession.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B101A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        windowInsets = WindowInsets(0.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B101A))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = LivePrimaryMint)
                    Text("Live Viewers (${filteredViewers.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Search Bar Input Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(LiveSurfaceDark)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    if (searchQuery.isEmpty()) {
                        Text("Search room viewers...", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(start = 26.dp),
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                    singleLine = true
                )
            }

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
                            WorkDailyLiveAvatar(
                                name = viewer.name,
                                avatarUrl = viewer.avatar,
                                size = 36.dp,
                                isLive = viewer.isLiveNow,
                                showBadge = false
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(viewer.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (viewer.isHost) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = LivePrimaryEmerald
                                        ) {
                                            Text("HOST", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text(viewer.profession, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        if (!viewer.isHost && viewer.id != "current_user") {
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

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
