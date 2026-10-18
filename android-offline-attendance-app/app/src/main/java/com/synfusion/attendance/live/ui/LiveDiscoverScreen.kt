package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    val wallet by viewModel.wallet.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var selectedTab by remember { mutableStateOf("For You") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val tabs = listOf("For You", "Following", "Audio", "Video")
    val categories = listOf("All", "Site Safety", "Machinery", "Masonry", "Engineering", "Contractor")

    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

    val filteredRooms = remember(liveRooms, selectedTab, selectedCategory, followedCreatorIds) {
        liveRooms.filter { room ->
            val matchesTab = when (selectedTab) {
                "For You" -> true
                "Following" -> followedCreatorIds.contains(room.creatorId)
                "Audio" -> room.type == LiveRoomType.AUDIO
                "Video" -> room.type == LiveRoomType.VIDEO
                else -> true
            }

            val matchesCategory = if (selectedCategory == "All") true
            else room.category.equals(selectedCategory, ignoreCase = true)

            matchesTab && matchesCategory && room.isLive
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("WorkDaily Live", fontWeight = FontWeight.Bold)
                        LiveBadge(text = "DEMO", bgColor = LivePrimaryEmerald)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Live")
                    }

                    Box {
                        IconButton(onClick = { showNotificationsSheet = true }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                        }
                    }

                    DiamondBalanceChip(
                        balance = wallet.diamondBalance,
                        onClick = onNavigateWallet
                    )

                    IconButton(onClick = onNavigateStudio) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Creator Studio", tint = LivePrimaryMint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateStudio,
                containerColor = LivePrimaryEmerald,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Videocam, contentDescription = "Go Live") },
                text = { Text("Go Live", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(24.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // WorkDaily Custom Pill Tabs (For You, Following, Audio, Video)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LivePrimaryEmerald else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Filter Scrollable Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LivePrimaryEmerald.copy(alpha = 0.15f),
                            selectedLabelColor = LivePrimaryEmerald,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            selectedBorderColor = LivePrimaryEmerald,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Featured Quick Banner
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
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(22.dp))
                        Column {
                            Text("Construction Gifts & Diamonds", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Support site engineers & workers with 20+ gifts", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LiveGold)
                }
            }

            // Responsive 2-Column Grid
            if (filteredRooms.isEmpty()) {
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
                        Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("No live streams found in '$selectedTab'", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
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

        // Global Search Bottom Sheet
        if (showSearchSheet) {
            LiveSearchSheet(
                viewModel = viewModel,
                onDismiss = { showSearchSheet = false },
                onNavigateLiveRoom = onNavigateLiveRoom,
                onNavigateProfile = onNavigateProfile
            )
        }

        // Demo Notifications Sheet
        if (showNotificationsSheet) {
            DemoNotificationsSheet(
                viewModel = viewModel,
                onDismiss = { showNotificationsSheet = false },
                onNavigateLiveRoom = onNavigateLiveRoom
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoNotificationsSheet(
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LiveDarkBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = LivePrimaryMint)
                    Text("Live Demo Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No live notifications yet", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                notifications.forEach { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markNotificationRead(notif.id)
                                notif.targetRoomId?.let {
                                    onDismiss()
                                    onNavigateLiveRoom(it)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(notif.message, fontSize = 11.sp, color = Color.LightGray)
                            }
                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LivePrimaryMint)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
            .height(175.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
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

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PulsingLiveAvatar(
                            name = room.creatorName,
                            size = 28.dp,
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoomCategoryChip(category = room.category)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(11.dp))
                            Text("${room.diamondEarnings}", fontSize = 10.sp, color = LiveDiamondColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
