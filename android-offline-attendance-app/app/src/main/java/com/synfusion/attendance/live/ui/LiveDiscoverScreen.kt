package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDiscoverScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateStudio: () -> Unit,
    onNavigateLeaderboard: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateDemoControls: () -> Unit
) {
    val liveRooms by viewModel.liveRooms.collectAsState()
    val creators by viewModel.creators.collectAsState()
    val wallet by viewModel.wallet.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Video", "Audio Stage", "Site Safety", "Machinery", "Masonry", "Contractor")

    val filteredRooms = remember(liveRooms, searchQuery, selectedCategory) {
        liveRooms.filter { room ->
            val matchesSearch = searchQuery.isBlank() ||
                    room.title.contains(searchQuery, ignoreCase = true) ||
                    room.creatorName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Video" -> room.type == LiveRoomType.VIDEO
                "Audio Stage" -> room.type == LiveRoomType.AUDIO
                else -> room.category.equals(selectedCategory, ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("WorkDaily LIVE", fontWeight = FontWeight.Bold)
                        LiveBadge(text = "DEMO", bgColor = LivePrimaryEmerald)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    DiamondBalanceChip(
                        balance = wallet.diamondBalance,
                        onClick = onNavigateWallet
                    )
                    IconButton(onClick = onNavigateLeaderboard) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard", tint = LiveGold)
                    }
                    IconButton(onClick = onNavigateProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = onNavigateDemoControls) {
                        Icon(Icons.Default.BugReport, contentDescription = "Developer Controls", tint = LivePrimaryMint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiveDarkBg, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateStudio,
                containerColor = LivePrimaryEmerald,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                text = { Text("Go Live", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = LiveDarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search live site discussions & creators...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LivePrimaryMint,
                    unfocusedBorderColor = LivePrimaryEmerald.copy(alpha = 0.3f),
                    focusedContainerColor = LiveSurfaceDark,
                    unfocusedContainerColor = LiveSurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            // Category Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LivePrimaryEmerald,
                            selectedLabelColor = Color.White,
                            containerColor = LiveSurfaceDark,
                            labelColor = Color.LightGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = LivePrimaryEmerald.copy(alpha = 0.3f),
                            selectedBorderColor = LivePrimaryMint,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Featured Stream Banner / Quick Shortcuts Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateWallet() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, LiveGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Construction Gifts & Diamonds", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Support creators with 20+ site tools gifts", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LiveGold)
                }
            }

            // Live Rooms Section
            if (filteredRooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("No live streams found in this category", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredRooms) { room ->
                        LiveRoomGridCard(
                            room = room,
                            onClick = { onNavigateLiveRoom(room.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveRoomGridCard(
    room: LiveRoom,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveBadge(
                        text = if (room.type == LiveRoomType.AUDIO) "AUDIO" else "LIVE",
                        bgColor = if (room.type == LiveRoomType.AUDIO) LivePurple else LiveRed
                    )
                    ViewerCountChip(count = room.viewerCount)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulsingLiveAvatar(
                            name = room.creatorName,
                            size = 32.dp,
                            isLive = room.isLive
                        )
                        Text(
                            text = room.creatorName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LivePrimaryMint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = room.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoomCategoryChip(category = room.category)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(12.dp))
                            Text("${room.diamondEarnings}", fontSize = 10.sp, color = LiveDiamondColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
