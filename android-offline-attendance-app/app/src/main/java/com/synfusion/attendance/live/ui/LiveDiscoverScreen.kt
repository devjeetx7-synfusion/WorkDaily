package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDiscoverScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit,
    onNavigateWallet: () -> Unit = {},
    onNavigateStudio: () -> Unit = {},
    onNavigateLeaderboard: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigatePeople: () -> Unit = {},
    onNavigateMessages: () -> Unit = {},
    onNavigateDemoControls: () -> Unit = {}
) {
    val liveRooms by viewModel.liveRooms.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var showSearchSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val activeLiveRooms = remember(liveRooms) { liveRooms.filter { it.isLive } }
    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Live Streams",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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

                    IconButton(onClick = onNavigatePeople) {
                        Icon(Icons.Default.People, contentDescription = "People & Friends")
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            GoLiveFloatingButton(
                onClick = onNavigateStudio,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 84.dp, end = 12.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Horizontal Creator Stories Row with thin red ring
            if (activeLiveRooms.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(activeLiveRooms, key = { it.id }) { room ->
                        LiveStoryAvatar(
                            name = room.creatorName,
                            avatarUrl = room.creatorAvatar,
                            size = 52.dp,
                            isLive = room.isLive,
                            onClick = { onNavigateLiveRoom(room.id) }
                        )
                    }
                }
            }

            // 2-Column Edge-to-Edge Tall Portrait Cards Grid with Trade Visual Previews
            if (activeLiveRooms.isEmpty()) {
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
                        Text("No live broadcasts currently active", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    items(activeLiveRooms, key = { it.id }) { room ->
                        CleanLiveRoomGridCard(
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
        containerColor = MaterialTheme.colorScheme.surface,
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
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = LivePrimaryEmerald)
                    Text("Live Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notifications yet", color = Color.Gray, fontSize = 13.sp)
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LivePrimaryEmerald)
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
fun CleanLiveRoomGridCard(
    room: LiveRoom,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.80f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // High Quality Thumbnail Preview or Trade Backdrop
            if (!room.thumbnailUrl.isNull_or_blank()) {
                val request = remember(room.thumbnailUrl) {
                    ImageRequest.Builder(context)
                        .data(room.thumbnailUrl)
                        .crossfade(true)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = room.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        TradeBackdrop(room = room)
                    }
                )
            } else {
                TradeBackdrop(room = room)
            }

            // Top Overlay: Single Red Live Badge + Translucent Viewer Counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactLiveBadge(text = "LIVE")
                ViewerCountChip(count = room.viewerCount)
            }

            // Bottom Overlay: Subtle Scrim + Creator Identity & Title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WorkDailyLiveAvatar(
                        name = room.creatorName,
                        avatarUrl = room.creatorAvatar,
                        size = 22.dp,
                        isLive = false,
                        showBadge = false
                    )

                    Text(
                        text = room.creatorName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = room.title,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TradeBackdrop(room: LiveRoom) {
    val tradeIcon = rememberTradeIcon(room.category, room.title)
    val gradientColors = rememberTradeGradient(room.category)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tradeIcon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.18f),
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun rememberTradeIcon(category: String, title: String): ImageVector {
    return remember(category, title) {
        when {
            category.contains("Safety", ignoreCase = true) || title.contains("Safety", ignoreCase = true) -> Icons.Default.Shield
            category.contains("Machinery", ignoreCase = true) || title.contains("Excavator", ignoreCase = true) -> Icons.Default.Agriculture
            category.contains("Masonry", ignoreCase = true) || title.contains("Mason", ignoreCase = true) -> Icons.Default.Construction
            category.contains("Engineering", ignoreCase = true) || title.contains("Civil", ignoreCase = true) -> Icons.Default.Architecture
            category.contains("Contractor", ignoreCase = true) || title.contains("Labor", ignoreCase = true) -> Icons.Default.Groups
            else -> Icons.Default.Engineering
        }
    }
}

@Composable
private fun rememberTradeGradient(category: String): List<Color> {
    return remember(category) {
        when {
            category.contains("Safety", ignoreCase = true) -> listOf(Color(0xFF0F392B), Color(0xFF071B14), Color(0xFF020907))
            category.contains("Machinery", ignoreCase = true) -> listOf(Color(0xFF3B2506), Color(0xFF1F1202), Color(0xFF0D0701))
            category.contains("Masonry", ignoreCase = true) -> listOf(Color(0xFF381216), Color(0xFF1E080A), Color(0xFF0A0203))
            category.contains("Engineering", ignoreCase = true) -> listOf(Color(0xFF10283B), Color(0xFF07141F), Color(0xFF02070D))
            else -> listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617))
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
