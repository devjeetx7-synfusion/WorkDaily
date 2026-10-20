package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
    onNavigateMainProfile: () -> Unit = {},
    onNavigateWallet: () -> Unit = {},
    onNavigateEarnings: () -> Unit = {},
    onNavigateStudio: () -> Unit = {},
    onNavigateLiveRoom: (String) -> Unit = {},
    onNavigatePrivateChat: (String) -> Unit = {},
    onNavigateDemoControls: () -> Unit = {}
) {
    val context = LocalContext.current
    val publicUsers by viewModel.publicUsers.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    val targetUser = remember(publicUsers, userId) {
        if (userId != null && userId != "current_user" && userId != "user_me") {
            publicUsers.find { it.id == userId }
        } else null
    }

    val isSelf = targetUser == null || userId == null || userId == "current_user" || userId == "user_me"

    LaunchedEffect(isSelf) {
        if (isSelf) {
            onNavigateMainProfile()
        }
    }

    if (isSelf || targetUser == null) {
        return
    }

    val displayName = targetUser.name
    val username = "@${targetUser.name.lowercase().replace(" ", "")}"
    val profession = targetUser.profession.ifBlank { "Site Specialist" }
    val bio = targetUser.bio.ifBlank { "WorkDaily Verified Member" }
    val avatarUrl = targetUser.avatar
    val isLive = targetUser.isLiveNow
    val isFollowing = followedIds.contains(targetUser.id)

    var showMenu by remember { mutableStateOf(false) }
    var activeUserListType by remember { mutableStateOf<UserListType?>(null) }

    val followerCount = targetUser.followerCount
    val followingCount = targetUser.followingCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                                text = { Text("Copy Link") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(context, "Profile URL copied", Toast.LENGTH_SHORT).show()
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 2. AVATAR, NAME, VERIFICATION, USERNAME, PROFESSION, BIO
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkDailyLiveAvatar(
                        name = displayName,
                        avatarUrl = avatarUrl,
                        size = 88.dp,
                        isLive = isLive,
                        showBadge = false
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (targetUser.isVerified) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Member",
                                    tint = LivePrimaryEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = username,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = LivePrimaryEmerald
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LivePrimaryEmerald.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = profession,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LivePrimaryEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        if (bio.isNotBlank()) {
                            Text(
                                text = bio,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. FOLLOWERS AND FOLLOWING STATISTICS
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeUserListType = UserListType.FOLLOWERS }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$followerCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeUserListType = UserListType.FOLLOWING }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$followingCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Following", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 4. FOLLOW / FOLLOWING AND MESSAGE BUTTONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleFollowCreator(targetUser.id) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else LivePrimaryEmerald,
                            contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigatePrivateChat(targetUser.id) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = LivePrimaryEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Message",
                            color = LivePrimaryEmerald,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. CONDITIONAL WATCH LIVE SECTION
            if (isLive && targetUser.activeRoomId != null) {
                item {
                    Button(
                        onClick = { onNavigateLiveRoom(targetUser.activeRoomId) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Live Stream", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

        }
    }

    // Followers & Following Bottom Sheet
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
                else -> "Users"
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
                        UserListType.FOLLOWING -> publicUsers.filter { it.isFollowing }
                        else -> publicUsers
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
                                        showBadge = false
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
