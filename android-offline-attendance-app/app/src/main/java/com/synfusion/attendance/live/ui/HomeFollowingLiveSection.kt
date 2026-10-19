package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveCreator
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.LivePrimaryMint
import com.synfusion.attendance.live.ui.components.LiveStoryAvatar

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
            // Horizontal scrolling row sharing the unified LiveStoryAvatar composable
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(sortedRooms, key = { it.id }) { room ->
                    val creator = creators.find { it.id == room.creatorId } ?: LiveCreator(
                        id = room.creatorId,
                        name = room.creatorName,
                        avatar = room.creatorAvatar,
                        bio = room.description
                    )

                    LiveStoryAvatar(
                        name = room.creatorName,
                        avatarUrl = room.creatorAvatar,
                        size = 56.dp,
                        isLive = room.isLive,
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
                                .size(56.dp)
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
