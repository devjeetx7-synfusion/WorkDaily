package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSearchSheet(
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit,
    onNavigateProfile: () -> Unit
) {
    val liveRooms by viewModel.liveRooms.collectAsState()
    val creators by viewModel.creators.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filters = listOf("All", "Live", "Users", "Audio", "Video", "Following")

    // Filter creators and rooms based on searchQuery and selectedFilter
    val matchingCreators = remember(creators, searchQuery, selectedFilter, followedCreatorIds) {
        if (selectedFilter == "Audio" || selectedFilter == "Video") emptyList()
        else {
            creators.filter { creator ->
                val matchesQuery = searchQuery.isBlank() ||
                        creator.name.contains(searchQuery, ignoreCase = true) ||
                        creator.profession.contains(searchQuery, ignoreCase = true) ||
                        creator.bio.contains(searchQuery, ignoreCase = true) ||
                        creator.roomCategory.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    "Following" -> followedCreatorIds.contains(creator.id)
                    "Live" -> creator.isLiveNow
                    else -> true
                }

                matchesQuery && matchesFilter
            }
        }
    }

    val matchingRooms = remember(liveRooms, searchQuery, selectedFilter, followedCreatorIds) {
        if (selectedFilter == "Users") emptyList()
        else {
            liveRooms.filter { room ->
                val matchesQuery = searchQuery.isBlank() ||
                        room.title.contains(searchQuery, ignoreCase = true) ||
                        room.creatorName.contains(searchQuery, ignoreCase = true) ||
                        room.category.contains(searchQuery, ignoreCase = true) ||
                        room.description.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    "Live" -> room.isLive
                    "Audio" -> room.type == LiveRoomType.AUDIO
                    "Video" -> room.type == LiveRoomType.VIDEO
                    "Following" -> followedCreatorIds.contains(room.creatorId)
                    else -> true
                }

                matchesQuery && matchesFilter
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = LivePrimaryEmerald)
                    Text(
                        text = "Search Live & Creators",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Search Bar Input Field - Theme-Aware
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search by name, stream title, category, profession...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LivePrimaryEmerald) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LivePrimaryEmerald,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { f ->
                    val isSelected = selectedFilter == f
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = f },
                        label = { Text(f, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LivePrimaryEmerald,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = LivePrimaryMint,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Results List
            if (matchingCreators.isEmpty() && matchingRooms.isEmpty()) {
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
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Text("No matching creators or live streams found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Live Streams Matching Results
                    if (matchingRooms.isNotEmpty()) {
                        item {
                            Text(
                                "ACTIVE LIVE STREAMS (${matchingRooms.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LivePrimaryEmerald
                            )
                        }

                        items(matchingRooms) { room ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onNavigateLiveRoom(room.id)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PulsingLiveAvatar(
                                            name = room.creatorName,
                                            size = 38.dp,
                                            isLive = room.isLive
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = room.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${room.creatorName} • ${room.category}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onNavigateLiveRoom(room.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Watch Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Creators Matching Results
                    if (matchingCreators.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "CREATORS (${matchingCreators.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LivePrimaryEmerald
                            )
                        }

                        items(matchingCreators) { creator ->
                            val isFollowing = followedCreatorIds.contains(creator.id)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PulsingLiveAvatar(
                                            name = creator.name,
                                            size = 38.dp,
                                            isLive = creator.isLiveNow
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = creator.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (creator.isLiveNow) {
                                                    LiveBadge(text = "LIVE", bgColor = LiveRed)
                                                }
                                            }
                                            Text(
                                                text = "${creator.profession} • ${creator.followerCount} followers",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (creator.id == "creator_me") {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onNavigateProfile()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("My Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (creator.isLiveNow && creator.activeRoomId != null) {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onNavigateLiveRoom(creator.activeRoomId)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Watch Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        FollowButton(
                                            isFollowing = isFollowing,
                                            onToggle = { viewModel.toggleFollowCreator(creator.id) }
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
}
