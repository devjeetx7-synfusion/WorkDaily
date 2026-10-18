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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.FriendRequestStatus
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

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
    val friendRequests by viewModel.friendRequests.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val creatorEarning by viewModel.creatorEarning.collectAsState()
    val creators by viewModel.creators.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    val targetUser = remember(publicUsers, userId) {
        if (userId != null && userId != "current_user") {
            publicUsers.find { it.id == userId }
        } else null
    }

    val isSelf = targetUser == null || targetUser.id == "current_user"

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelf) "My Public Profile" else targetUser?.name ?: "Worker Profile", fontWeight = FontWeight.Bold) },
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
                                    text = { Text("Block User", color = LiveRed) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.blockUser(targetUser.id)
                                        Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report Profile") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PulsingLiveAvatar(
                            name = if (isSelf) "You" else targetUser?.name ?: "Worker",
                            size = 64.dp,
                            isLive = targetUser?.isLiveNow == true
                        )

                        Text(
                            text = if (isSelf) "You (Site Engineer)" else targetUser?.name ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isSelf) "WorkDaily Verified Member" else targetUser?.profession ?: "Site Worker",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!isSelf && !targetUser?.bio.isNull_or_blank()) {
                            Text(
                                text = targetUser?.bio ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats Row (Followers, Following, Friends)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${if (isSelf) followedIds.size else targetUser?.followerCount ?: 0}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${if (isSelf) 240 else targetUser?.followingCount ?: 0}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Following", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${if (isSelf) 15 else targetUser?.friendsCount ?: 0}",
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

            // Actions Section for Other Users
            if (!isSelf && targetUser != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Follow / Following Button
                        Button(
                            onClick = { viewModel.toggleFollowCreator(targetUser.id) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (targetUser.isFollowing) MaterialTheme.colorScheme.surfaceVariant else LivePrimaryEmerald,
                                contentColor = if (targetUser.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (targetUser.isFollowing) "Following" else "Follow", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Message Action
                        Button(
                            onClick = { onNavigatePrivateChat(targetUser.id) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Message", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Add Friend Button
                        if (!targetUser.isFriend && targetUser.friendRequestStatus != FriendRequestStatus.PENDING) {
                            IconButton(
                                onClick = {
                                    viewModel.sendFriendRequest(targetUser.id)
                                    Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else if (targetUser.isFriend) {
                            IconButton(
                                onClick = { viewModel.unfriendUser(targetUser.id) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.People, contentDescription = "Friends", modifier = Modifier.size(20.dp), tint = LivePrimaryEmerald)
                            }
                        }
                    }
                }

                if (targetUser.isLiveNow && targetUser.activeRoomId != null) {
                    item {
                        Button(
                            onClick = { onNavigateLiveRoom(targetUser.activeRoomId) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch Live", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Self Quick Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateWallet,
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Wallet", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onNavigateEarnings,
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Earnings", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
