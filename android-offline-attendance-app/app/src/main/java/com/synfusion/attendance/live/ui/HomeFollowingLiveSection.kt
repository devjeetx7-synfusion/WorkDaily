package com.synfusion.attendance.live.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveCreator
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.LivePrimaryMint
import com.synfusion.attendance.live.ui.components.LiveRed

@Composable
fun HomeFollowingLiveSection(
    liveRooms: List<LiveRoom>,
    creators: List<LiveCreator> = emptyList(),
    followedCreatorIds: Set<String> = emptySet(),
    onNavigateLiveDiscover: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit,
    onCreatorLongPress: (LiveCreator) -> Unit = {}
) {
    // Filter active live rooms
    val activeRooms = remember(liveRooms) { liveRooms.filter { it.isLive } }

    // Sort live rooms: 1) Followed live creators first, 2) Other recommended live creators
    val sortedRooms = remember(activeRooms, followedCreatorIds) {
        activeRooms.sortedByDescending { followedCreatorIds.contains(it.creatorId) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp, max = 98.dp),
        color = Color.Transparent
    ) {
        if (sortedRooms.isEmpty()) {
            // Compact Discover Live entry if no active streams
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateLiveDiscover() },
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LivePrimaryEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = "WorkDaily LIVE",
                                tint = LivePrimaryMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "WorkDaily LIVE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to explore site streams & discussions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Discover",
                        tint = LivePrimaryEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // Horizontal scrolling row of compact story avatars with names below
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(sortedRooms) { room ->
                    val creator = creators.find { it.id == room.creatorId } ?: LiveCreator(
                        id = room.creatorId,
                        name = room.creatorName,
                        avatar = room.creatorAvatar,
                        bio = room.description
                    )

                    LiveStoryAvatarItem(
                        room = room,
                        creator = creator,
                        onClick = { onNavigateLiveRoom(room.id) },
                        onLongClick = { onCreatorLongPress(creator) }
                    )
                }

                // Compact See All / Discover Icon item at the end
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.width(56.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    LivePrimaryEmerald.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .clickable { onNavigateLiveDiscover() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "See All LIVE",
                                tint = LivePrimaryEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "See All",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveStoryAvatarItem(
    room: LiveRoom,
    creator: LiveCreator,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Breathing/Pulsing LIVE Ring Animation
    val infiniteTransition = rememberInfiniteTransition(label = "liveRing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val initials = remember(creator.name) {
        val parts = creator.name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        else if (creator.name.isNotEmpty()) creator.name.take(2).uppercase()
        else "WD"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(62.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Outer Breathing Gradient Live Ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                LiveRed,
                                Color(0xFFFF2D55),
                                LiveRed,
                                Color(0xFFFF5252),
                                LiveRed
                            )
                        )
                    )
                    .padding(2.5.dp)
            ) {
                // Inner gap background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                ) {
                    // Profile Avatar Photo / Fallback Initials
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(LivePrimaryEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LivePrimaryEmerald
                        )
                    }
                }
            }
        }

        // Creator Name Text Below Avatar
        Text(
            text = creator.name.split(" ").firstOrNull() ?: creator.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
