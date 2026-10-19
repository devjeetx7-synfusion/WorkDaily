package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.*
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.utils.AmountFormatter

enum class UserListType {
    FOLLOWERS,
    FOLLOWING,
    FRIENDS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveProfileScreen(
    viewModel: LiveViewModel,
    userId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit = {},
    onNavigateEarnings: () -> Unit = {},
    onNavigateStudio: () -> Unit = {},
    onNavigateLiveRoom: (String) -> Unit = {},
    onNavigatePrivateChat: (String) -> Unit = {},
    onNavigateDemoControls: () -> Unit = {}
) {
    val context = LocalContext.current
    val publicUsers by viewModel.publicUsers.collectAsState()
    val creatorEarning by viewModel.creatorEarning.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val creators by viewModel.creators.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    val targetUser = remember(publicUsers, userId) {
        if (userId != null && userId != "current_user" && userId != "user_me") {
            publicUsers.find { it.id == userId }
        } else null
    }

    val isSelf = targetUser == null || targetUser.id == "current_user" || targetUser.id == "user_me"

    val displayName = remember(targetUser, isSelf) {
        if (isSelf) "You" else targetUser?.name ?: "Worker Profile"
    }

    val profession = remember(targetUser, isSelf) {
        if (isSelf) "Site Engineer" else targetUser?.profession ?: "Site Worker"
    }

    val bio = remember(targetUser, isSelf) {
        if (isSelf) "WorkDaily Verified Member • Site Operations & Construction Specialist"
        else targetUser?.bio ?: ""
    }

    val avatarUrl = remember(targetUser, isSelf) {
        if (isSelf) "" else targetUser?.avatar
    }

    val isLive = targetUser?.isLiveNow == true

    var showMenu by remember { mutableStateOf(false) }
    var activeUserListType by remember { mutableStateOf<UserListType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelf) "My Profile" else "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSelf && targetUser != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share Profile") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        Toast.makeText(context, "Profile link copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block User", color = LiveRed) },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = LiveRed) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.blockUser(targetUser.id)
                                        Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report Profile") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        Toast.makeText(context, "Profile reported to moderators", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = onNavigateDemoControls) {
                            Icon(Icons.Default.BugReport, contentDescription = "Demo Controls", tint = LivePrimaryEmerald)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Compact Avatar & Identity Header
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WorkDailyLiveAvatar(
                            name = displayName,
                            avatarUrl = avatarUrl,
                            size = 80.dp,
                            isLive = isLive,
                            showBadge = isLive
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Member",
                                    tint = LivePrimaryEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LivePrimaryEmerald.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = profession,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LivePrimaryEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (bio.isNotBlank()) {
                            Text(
                                text = bio,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Compact Statistics Row (Followers, Following, Friends)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val followerCount = if (isSelf) followedIds.size + 142 else targetUser?.followerCount ?: 0
                            val followingCount = if (isSelf) 240 else targetUser?.followingCount ?: 0
                            val friendsCount = if (isSelf) 35 else targetUser?.friendsCount ?: 0

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeUserListType = UserListType.FOLLOWERS }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$followerCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeUserListType = UserListType.FOLLOWING }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$followingCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Following", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeUserListType = UserListType.FRIENDS }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$friendsCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Friends", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Section 2: Interactive Actions
            item {
                if (!isSelf && targetUser != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Follow / Following Action Button
                            Button(
                                onClick = { viewModel.toggleFollowCreator(targetUser.id) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (targetUser.isFollowing) MaterialTheme.colorScheme.surfaceVariant else LivePrimaryEmerald,
                                    contentColor = if (targetUser.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (targetUser.isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (targetUser.isFollowing) "Following" else "Follow",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Message Action Button
                            Button(
                                onClick = { onNavigatePrivateChat(targetUser.id) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            // Add / Manage Friend Button
                            IconButton(
                                onClick = {
                                    if (targetUser.isFriend) {
                                        viewModel.unfriendUser(targetUser.id)
                                        Toast.makeText(context, "Removed from friends", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val sent = viewModel.sendFriendRequest(targetUser.id)
                                        if (sent) {
                                            Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Request already sent or connected", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = if (targetUser.isFriend) Icons.Default.People else Icons.Default.GroupAdd,
                                    contentDescription = "Friend Action",
                                    tint = if (targetUser.isFriend) LivePrimaryEmerald else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Watch Live Banner Button (only displayed when user is broadcasting)
                        if (targetUser.isLiveNow && targetUser.activeRoomId != null) {
                            Button(
                                onClick = { onNavigateLiveRoom(targetUser.activeRoomId) },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Watch Live Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Self Creator & Wallet Navigation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateWallet,
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Diamond Wallet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onNavigateEarnings,
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LivePrimaryEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Creator Earnings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Section 3: TOTAL EARNED Summary Card (New Verified Section)
            item {
                TotalEarnedSummaryCard(
                    isSelf = isSelf,
                    creatorEarning = creatorEarning,
                    wallet = wallet,
                    onNavigateWallet = onNavigateWallet,
                    onNavigateEarnings = onNavigateEarnings
                )
            }
        }
    }

    // Modal Bottom Sheet for Followers / Following / Friends List
    activeUserListType?.let { listType ->
        ModalBottomSheet(
            onDismissRequest = { activeUserListType = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val title = when (listType) {
                UserListType.FOLLOWERS -> "Followers"
                UserListType.FOLLOWING -> "Following"
                UserListType.FRIENDS -> "Friends"
            }

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
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { activeUserListType = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                val listUsers = remember(publicUsers, listType) {
                    when (listType) {
                        UserListType.FRIENDS -> publicUsers.filter { it.isFriend }
                        UserListType.FOLLOWING -> publicUsers.filter { it.isFollowing }
                        UserListType.FOLLOWERS -> publicUsers
                    }
                }

                if (listUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No users found in $title", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(listUsers, key = { it.id }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        activeUserListType = null
                                        if (user.id != "current_user") {
                                            onNavigatePrivateChat(user.id)
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    WorkDailyLiveAvatar(
                                        name = user.name,
                                        avatarUrl = user.avatar,
                                        size = 40.dp,
                                        isLive = user.isLiveNow,
                                        showBadge = user.isLiveNow
                                    )
                                    Column {
                                        Text(user.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(user.profession, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                FollowButton(
                                    isFollowing = user.isFollowing,
                                    onToggle = { viewModel.toggleFollowCreator(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Polished Earnings Summary Card ("Total Earned").
 * Clearly displays platform rewards and creator earnings while keeping private salary/wage details protected.
 */
@Composable
fun TotalEarnedSummaryCard(
    isSelf: Boolean,
    creatorEarning: DemoCreatorEarning,
    wallet: DemoWallet,
    onNavigateWallet: () -> Unit,
    onNavigateEarnings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LivePrimaryEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = LivePrimaryEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Total Earned",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LivePrimaryEmerald.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isSelf) "Platform Rewards" else "Creator Verified",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LivePrimaryEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (isSelf) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Lifetime Earned Platform Value
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Lifetime Earned", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "₹${creatorEarning.totalRupeesEarned.toInt()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LivePrimaryEmerald
                            )
                            Text(
                                text = "${creatorEarning.totalDiamondsReceived} 💎 Gifts & Rewards",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card 2: Current Redeemable Balance
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Available Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${creatorEarning.availableDiamonds} 💎",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "≈ ₹${(creatorEarning.availableDiamonds * 0.5).toInt()} Redeemable",
                                fontSize = 10.sp,
                                color = LivePrimaryEmerald
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Private financial earnings visible only to you",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = onNavigateEarnings,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Details ›", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LivePrimaryEmerald)
                    }
                }
            } else {
                // Public View for other creators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Creator Live Gifts Earned", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${creatorEarning.totalDiamondsReceived} 💎 Diamonds", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(Icons.Default.Verified, contentDescription = "Verified Creator", tint = LivePrimaryEmerald, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
