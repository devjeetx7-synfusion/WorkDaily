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
import kotlinx.coroutines.launch

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
    onNavigateDemoControls: () -> Unit = {},
    onNavigatePeople: () -> Unit = {}
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

    val currentUserId = remember {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    var isLoading by remember { mutableStateOf(true) }
    var publicProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var isFollowingTarget by remember { mutableStateOf(false) }
    var isFollowActionBusy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val repository = remember { FirestorePublicProfileRepository() }
    val followRepo = remember { com.synfusion.attendance.online.FirestoreFollowRepository() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId, currentUserId) {
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

        if (currentUserId != null && userId != null) {
            isFollowingTarget = followRepo.isFollowing(currentUserId, userId)
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
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                    ) {
                        Text("Go Back", color = Color.White)
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
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportDetails by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }

    val blockRepo = remember { com.synfusion.attendance.online.FirestoreBlockRepository() }
    val reportRepo = remember { com.synfusion.attendance.online.FirestoreReportRepository() }
    var isBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId, targetUser.uid) {
        if (currentUserId != null) {
            isBlocked = blockRepo.isBlocked(currentUserId, targetUser.uid)
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingReport) showReportDialog = false },
            title = { Text("Report User", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a reason for reporting @${targetUser.username}:", fontSize = 13.sp)
                    val reasons = listOf("Spam or Scam", "Inappropriate Content", "Harassment or Bullying", "Fake Profile")
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = r }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = reportReason == r, onClick = { reportReason = r })
                            Text(r, fontSize = 14.sp)
                        }
                    }
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        placeholder = { Text("Additional details (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotBlank() && currentUserId != null) {
                            isSubmittingReport = true
                            coroutineScope.launch {
                                val res = reportRepo.submitReport(currentUserId, targetUser.uid, reportReason, reportDetails)
                                isSubmittingReport = false
                                showReportDialog = false
                                if (res is ProfileOperationResult.Success) {
                                    Toast.makeText(context, "Report submitted. Thank you.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = reportReason.isNotBlank() && !isSubmittingReport,
                    colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                ) {
                    Text("Submit Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

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
                                text = { Text(if (isBlocked) "Unblock User" else "Block User") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (currentUserId != null) {
                                        coroutineScope.launch {
                                            if (isBlocked) {
                                                blockRepo.unblockUser(currentUserId, targetUser.uid)
                                                isBlocked = false
                                                Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                                            } else {
                                                blockRepo.blockUser(currentUserId, targetUser.uid)
                                                isBlocked = true
                                                isFollowingTarget = false
                                                Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Profile") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = LivePrimaryEmerald
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LivePrimaryEmerald.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = profession,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LivePrimaryEmerald,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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

            // Statistics Card: Followers and Following side-by-side
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var activeTargetUserListType by remember { mutableStateOf<UserListType?>(null) }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeTargetUserListType = UserListType.FOLLOWERS }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${targetUser.followerCount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Followers",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeTargetUserListType = UserListType.FOLLOWING }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${targetUser.followingCount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Following",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (activeTargetUserListType != null) {
                            val listType = activeTargetUserListType!!
                            var sheetUsers by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
                            var isSheetLoading by remember { mutableStateOf(true) }
                            var followedSet by remember { mutableStateOf<Set<String>>(emptySet()) }
                            var busyUids by remember { mutableStateOf<Set<String>>(emptySet()) }

                            LaunchedEffect(listType, targetUser.uid, currentUserId) {
                                isSheetLoading = true
                                val res = if (listType == UserListType.FOLLOWERS) {
                                    followRepo.getFollowers(targetUser.uid)
                                } else {
                                    followRepo.getFollowing(targetUser.uid)
                                }
                                if (res is ProfileOperationResult.Success) {
                                    sheetUsers = res.data
                                }
                                if (currentUserId != null) {
                                    val myFollowingRes = followRepo.getFollowing(currentUserId)
                                    if (myFollowingRes is ProfileOperationResult.Success) {
                                        followedSet = myFollowingRes.data.map { it.uid }.toSet()
                                    }
                                }
                                isSheetLoading = false
                            }

                            ModalBottomSheet(
                                onDismissRequest = { activeTargetUserListType = null },
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                windowInsets = WindowInsets(0.dp),
                                dragHandle = { BottomSheetDefaults.DragHandle() }
                            ) {
                                val sheetTitle = when (listType) {
                                    UserListType.FOLLOWERS -> "Followers (${sheetUsers.size})"
                                    UserListType.FOLLOWING -> "Following (${sheetUsers.size})"
                                    UserListType.FRIENDS -> "Followers (${sheetUsers.size})"
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sheetTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        IconButton(onClick = { activeTargetUserListType = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }

                                    if (isSheetLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = LivePrimaryEmerald)
                                        }
                                    } else if (sheetUsers.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No users found in $sheetTitle", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(sheetUsers, key = { it.uid }) { user ->
                                                val isFollowingUser = followedSet.contains(user.uid)
                                                val busy = user.uid in busyUids

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            activeTargetUserListType = null
                                                            onNavigatePeople()
                                                        }
                                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        WorkDailyLiveAvatar(
                                                            name = user.displayName,
                                                            avatarUrl = user.photoUrl ?: "",
                                                            size = 44.dp,
                                                            isLive = false,
                                                            showBadge = false
                                                        )
                                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                Text(user.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                                if (user.isVerified) {
                                                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = LivePrimaryEmerald, modifier = Modifier.size(14.dp))
                                                                }
                                                            }
                                                            Text("@${user.username}", fontSize = 12.sp, color = LivePrimaryEmerald)
                                                            if (user.profession.isNotBlank()) {
                                                                Text(user.profession, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                    }

                                                    if (currentUserId != null && currentUserId != user.uid) {
                                                        FollowButton(
                                                            isFollowing = isFollowingUser,
                                                            onToggle = {
                                                                if (busy) return@FollowButton
                                                                val wasFollowing = isFollowingUser
                                                                busyUids = busyUids + user.uid
                                                                coroutineScope.launch {
                                                                    try {
                                                                        val res = if (wasFollowing) followRepo.unfollowUser(currentUserId, user.uid) else followRepo.followUser(currentUserId, user.uid)
                                                                        if (res is ProfileOperationResult.Success) {
                                                                            followedSet = if (wasFollowing) followedSet - user.uid else followedSet + user.uid
                                                                        }
                                                                    } catch (_: Exception) {
                                                                    } finally {
                                                                        busyUids = busyUids - user.uid
                                                                    }
                                                                }
                                                            },
                                                            isLoading = busy,
                                                            height = 32.dp,
                                                            minWidth = 72.dp
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Balanced side-by-side Follow/Following and Message
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUserId != null && currentUserId != targetUser.uid) {
                        FollowButton(
                            isFollowing = isFollowingTarget,
                            onToggle = {
                                if (isFollowActionBusy) return@FollowButton
                                val previousStatus = isFollowingTarget
                                val newStatus = !previousStatus
                                isFollowActionBusy = true
                                coroutineScope.launch {
                                    try {
                                        val result = if (newStatus) {
                                            followRepo.followUser(currentUserId, targetUser.uid)
                                        } else {
                                            followRepo.unfollowUser(currentUserId, targetUser.uid)
                                        }
                                        if (result is ProfileOperationResult.Success) {
                                            isFollowingTarget = newStatus
                                            val latestProfile = publicProfile
                                            if (latestProfile != null) {
                                                val updatedFollowerCount = if (newStatus) {
                                                    latestProfile.followerCount + 1
                                                } else {
                                                    (latestProfile.followerCount - 1).coerceAtLeast(0)
                                                }
                                                publicProfile = latestProfile.copy(followerCount = updatedFollowerCount)
                                            }
                                        } else if (result is ProfileOperationResult.Error) {
                                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message ?: "Operation failed", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isFollowActionBusy = false
                                    }
                                }
                            },
                            isLoading = isFollowActionBusy,
                            height = 48.dp,
                            minWidth = 0.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigatePrivateChat(targetUser.uid) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, LivePrimaryEmerald),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = LivePrimaryEmerald
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Message",
                            tint = LivePrimaryEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
