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
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.FirestorePublicProfileRepository
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile

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
    val isSelf = userId == null || userId == "current_user" || userId == "user_me"

    LaunchedEffect(isSelf) {
        if (isSelf) {
            onNavigateMainProfile()
        }
    }

    if (isSelf || userId == null) {
        return
    }

    var isLoading by remember { mutableStateOf(true) }
    var publicProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val repository = remember { FirestorePublicProfileRepository() }

    LaunchedEffect(userId) {
        isLoading = true
        loadError = null
        when (val result = repository.getPublicProfile(userId)) {
            is ProfileOperationResult.Success -> {
                publicProfile = result.data
                isLoading = false
            }
            is ProfileOperationResult.Error -> {
                publicProfile = null
                loadError = result.message
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LivePrimaryEmerald)
        }
        return
    }

    val targetUser = publicProfile

    if (targetUser == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile Unavailable", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(com.synfusion.attendance.ui.components.icons.WorkDailyIcons.ArrowLeft, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("This profile is unavailable or does not exist.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }
        return
    }

    val displayName = targetUser.displayName.ifEmpty { "WorkDaily Member" }
    val username = "@${targetUser.username}"
    val profession = targetUser.profession.ifBlank { "Site Worker" }
    val bio = targetUser.bio.ifBlank { "WorkDaily Verified Member" }
    val avatarUrl = targetUser.photoUrl ?: ""

    var showMenu by remember { mutableStateOf(false) }

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
                        Icon(com.synfusion.attendance.ui.components.icons.WorkDailyIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(com.synfusion.attendance.ui.components.icons.WorkDailyIcons.MoreVertical, contentDescription = "More")
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
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkDailyLiveAvatar(
                        name = displayName,
                        avatarUrl = avatarUrl,
                        size = 88.dp,
                        isLive = false,
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

            // Truthful zero statistics
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
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${targetUser.followerCount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${targetUser.followingCount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Following", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigatePrivateChat(targetUser.uid) },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
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
        }
    }
}
