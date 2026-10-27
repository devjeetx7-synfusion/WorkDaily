package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.auth.FirebaseAuth
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.BlockRepository
import com.synfusion.attendance.online.FirestoreBlockRepository
import com.synfusion.attendance.online.FirestoreFollowRepository
import com.synfusion.attendance.online.FirestorePublicProfileRepository
import com.synfusion.attendance.online.FirestoreReportRepository
import com.synfusion.attendance.online.FollowRepository
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
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
    onNavigatePeople: () -> Unit = {},
    onNavigateUserProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentUserId = remember {
        try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    val isSelf = userId == null ||
        userId == "current_user" ||
        userId == "user_me" ||
        (currentUserId != null && userId == currentUserId)

    LaunchedEffect(isSelf) {
        if (isSelf) {
            onNavigateMainProfile()
        }
    }

    if (isSelf || userId == null) {
        return
    }

    var isLoading by remember(userId) { mutableStateOf(true) }
    var publicProfile by remember(userId) { mutableStateOf<PublicProfile?>(null) }
    var followerCount by remember(userId) { mutableIntStateOf(0) }
    var followingCount by remember(userId) { mutableIntStateOf(0) }
    var isFollowingTarget by remember(userId) { mutableStateOf(false) }
    var isFollowActionBusy by remember(userId) { mutableStateOf(false) }
    var isBlocked by remember(userId) { mutableStateOf(false) }
    var isBlockedByTarget by remember(userId) { mutableStateOf(false) }
    var isBlockActionBusy by remember(userId) { mutableStateOf(false) }
    var loadError by remember(userId) { mutableStateOf<String?>(null) }
    var activeUserListType by remember(userId) { mutableStateOf<UserListType?>(null) }

    val repository = remember { FirestorePublicProfileRepository() }
    val followRepo = remember { FirestoreFollowRepository() }
    val blockRepo = remember { FirestoreBlockRepository() }
    val reportRepo = remember { FirestoreReportRepository() }
    val coroutineScope = rememberCoroutineScope()

    suspend fun reconcileCounts(targetUid: String, fallbackProfile: PublicProfile? = publicProfile) {
        when (val countsRes = followRepo.getFollowCounts(targetUid)) {
            is ProfileOperationResult.Success -> {
                followerCount = countsRes.data.followerCount
                followingCount = countsRes.data.followingCount
                if (fallbackProfile != null) {
                    publicProfile = fallbackProfile.copy(
                        followerCount = countsRes.data.followerCount,
                        followingCount = countsRes.data.followingCount
                    )
                }
            }
            is ProfileOperationResult.Error -> {
                if (fallbackProfile != null) {
                    followerCount = fallbackProfile.followerCount.coerceAtLeast(0)
                    followingCount = fallbackProfile.followingCount.coerceAtLeast(0)
                }
            }
        }
    }

    suspend fun loadFullProfileState(showSpinner: Boolean) {
        if (showSpinner && publicProfile == null) {
            isLoading = true
        }
        loadError = null

        when (val result = repository.getPublicProfile(userId)) {
            is ProfileOperationResult.Success -> {
                val loaded = result.data
                publicProfile = loaded
                if (loaded != null) {
                    followerCount = loaded.followerCount.coerceAtLeast(0)
                    followingCount = loaded.followingCount.coerceAtLeast(0)
                    reconcileCounts(userId, loaded)
                }
            }
            is ProfileOperationResult.Error -> {
                if (publicProfile == null) {
                    loadError = result.message
                }
            }
        }

        if (!currentUserId.isNullOrBlank()) {
            isBlocked = blockRepo.isBlocked(currentUserId, userId)
            isBlockedByTarget = blockRepo.isBlocked(userId, currentUserId)
            isFollowingTarget = if (isBlocked || isBlockedByTarget) {
                false
            } else {
                followRepo.isFollowing(currentUserId, userId)
            }
        } else {
            isBlocked = false
            isBlockedByTarget = false
            isFollowingTarget = false
        }

        isLoading = false
    }

    LaunchedEffect(userId, currentUserId) {
        loadFullProfileState(showSpinner = true)
    }

    DisposableEffect(lifecycleOwner, userId, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    loadFullProfileState(showSpinner = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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
                            Icon(WorkDailyIcons.ArrowLeft, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = loadError ?: "This profile is unavailable or does not exist.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (loadError != null) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch { loadFullProfileState(showSpinner = true) }
                                },
                                border = BorderStroke(1.dp, LivePrimaryEmerald)
                            ) {
                                Text("Retry", color = LivePrimaryEmerald)
                            }
                        }
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                        ) {
                            Text("Go Back", color = Color.White)
                        }
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
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportDetails by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }

    fun performUnblock() {
        if (currentUserId.isNullOrBlank()) {
            Toast.makeText(context, "Sign in with Google to manage blocked accounts.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isBlockActionBusy) return
        isBlockActionBusy = true
        coroutineScope.launch {
            try {
                when (val res = blockRepo.unblockUser(currentUserId, targetUser.uid)) {
                    is ProfileOperationResult.Success -> {
                        isBlocked = false
                        // Unblocking never auto-follows; verify actual follow state & counts
                        isFollowingTarget = followRepo.isFollowing(currentUserId, targetUser.uid)
                        reconcileCounts(targetUser.uid)
                        Toast.makeText(context, "Unblocked @$displayName", Toast.LENGTH_SHORT).show()
                    }
                    is ProfileOperationResult.Error -> {
                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Failed to unblock user", Toast.LENGTH_SHORT).show()
            } finally {
                isBlockActionBusy = false
            }
        }
    }

    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBlockActionBusy) showBlockConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Block @${targetUser.username}?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "They won't be able to follow or message you on WorkDaily, and any existing follow relationship between you will be removed. You can unblock them anytime in Settings → Privacy & Safety → Blocked Users.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUserId.isNullOrBlank() || isBlockActionBusy) return@Button
                        isBlockActionBusy = true
                        coroutineScope.launch {
                            try {
                                when (val res = blockRepo.blockUser(currentUserId, targetUser.uid)) {
                                    is ProfileOperationResult.Success -> {
                                        isBlocked = true
                                        isFollowingTarget = false
                                        showBlockConfirmDialog = false
                                        reconcileCounts(targetUser.uid)
                                        Toast.makeText(context, "Blocked @${targetUser.username}", Toast.LENGTH_SHORT).show()
                                    }
                                    is ProfileOperationResult.Error -> {
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Failed to block user", Toast.LENGTH_SHORT).show()
                            } finally {
                                isBlockActionBusy = false
                            }
                        }
                    },
                    enabled = !isBlockActionBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    if (isBlockActionBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Block", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockConfirmDialog = false },
                    enabled = !isBlockActionBusy
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingReport) showReportDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
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
                TextButton(
                    onClick = { showReportDialog = false },
                    enabled = !isSubmittingReport
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (activeUserListType != null) {
        UserRelationshipBottomSheet(
            targetUid = targetUser.uid,
            listType = activeUserListType!!,
            initialCount = if (activeUserListType == UserListType.FOLLOWERS) followerCount else followingCount,
            currentUserId = currentUserId,
            followRepo = followRepo,
            blockRepo = blockRepo,
            onDismiss = {
                activeUserListType = null
                coroutineScope.launch {
                    reconcileCounts(targetUser.uid)
                    if (!currentUserId.isNullOrBlank() && !isBlocked && !isBlockedByTarget) {
                        isFollowingTarget = followRepo.isFollowing(currentUserId, targetUser.uid)
                    }
                }
            },
            onRelationshipChanged = {
                coroutineScope.launch {
                    reconcileCounts(targetUser.uid)
                }
            },
            onUserSelected = { selectedProfile ->
                activeUserListType = null
                if (currentUserId != null && selectedProfile.uid == currentUserId) {
                    onNavigateMainProfile()
                } else {
                    onNavigateUserProfile(selectedProfile.uid)
                }
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
                        Icon(WorkDailyIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(WorkDailyIcons.MoreVertical, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
                                text = {
                                    Text(
                                        text = if (isBlocked) "Unblock User" else "Block User",
                                        color = if (isBlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                    )
                                },
                                enabled = !isBlockActionBusy,
                                onClick = {
                                    showMenu = false
                                    if (currentUserId.isNullOrBlank()) {
                                        Toast.makeText(context, "Sign in with Google to block users.", Toast.LENGTH_SHORT).show()
                                    } else if (isBlocked) {
                                        performUnblock()
                                    } else {
                                        showBlockConfirmDialog = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Profile") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (currentUserId.isNullOrBlank()) {
                                        Toast.makeText(context, "Sign in with Google to report profiles.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showReportDialog = true
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
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
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Statistics Card: Followers and Following side-by-side (opens expandable bottom sheet)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeUserListType = UserListType.FOLLOWERS }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$followerCount",
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
                                .clickable { activeUserListType = UserListType.FOLLOWING }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$followingCount",
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
                    }
                }
            }

            // Blocked status notice when current user blocked this profile
            if (isBlocked) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "You have blocked @${targetUser.username}. Unblock to follow or message.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Action Buttons: Balanced equal-width 48dp Follow/Following (or Unblock) and Message
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBlocked) {
                        OutlinedButton(
                            onClick = { performUnblock() },
                            enabled = !isBlockActionBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            if (isBlockActionBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Unblock",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }
                    } else {
                        FollowButton(
                            isFollowing = isFollowingTarget,
                            onToggle = {
                                if (currentUserId.isNullOrBlank()) {
                                    Toast.makeText(context, "Sign in with Google to follow members.", Toast.LENGTH_SHORT).show()
                                    return@FollowButton
                                }
                                if (currentUserId == targetUser.uid) {
                                    return@FollowButton
                                }
                                if (isBlocked || isBlockedByTarget) {
                                    Toast.makeText(context, "Cannot follow this user due to block settings.", Toast.LENGTH_SHORT).show()
                                    return@FollowButton
                                }
                                if (isFollowActionBusy) return@FollowButton

                                val previousStatus = isFollowingTarget
                                val previousFollowers = followerCount
                                val desiredStatus = !previousStatus

                                isFollowActionBusy = true
                                coroutineScope.launch {
                                    try {
                                        val result = if (desiredStatus) {
                                            followRepo.followUser(currentUserId, targetUser.uid)
                                        } else {
                                            followRepo.unfollowUser(currentUserId, targetUser.uid)
                                        }
                                        when (result) {
                                            is ProfileOperationResult.Success -> {
                                                isFollowingTarget = desiredStatus
                                                reconcileCounts(targetUser.uid)
                                            }
                                            is ProfileOperationResult.Error -> {
                                                isFollowingTarget = previousStatus
                                                followerCount = previousFollowers
                                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isFollowingTarget = previousStatus
                                        followerCount = previousFollowers
                                        Toast.makeText(context, e.message ?: "Operation failed", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isFollowActionBusy = false
                                    }
                                }
                            },
                            isLoading = isFollowActionBusy,
                            enabled = !isBlockedByTarget,
                            showIcon = true,
                            height = 48.dp,
                            minWidth = 0.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val messageEnabled = !isBlocked && !isBlockedByTarget
                    OutlinedButton(
                        onClick = {
                            if (currentUserId.isNullOrBlank()) {
                                Toast.makeText(context, "Sign in with Google to message members.", Toast.LENGTH_SHORT).show()
                            } else if (messageEnabled) {
                                onNavigatePrivateChat(targetUser.uid)
                            }
                        },
                        enabled = messageEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (messageEnabled) LivePrimaryEmerald else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = LivePrimaryEmerald,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Message",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Message",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRelationshipBottomSheet(
    targetUid: String,
    listType: UserListType,
    initialCount: Int,
    currentUserId: String?,
    followRepo: FollowRepository,
    blockRepo: BlockRepository,
    onDismiss: () -> Unit,
    onRelationshipChanged: () -> Unit = {},
    onUserSelected: (PublicProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var isLoading by remember(targetUid, listType) { mutableStateOf(true) }
    var errorMessage by remember(targetUid, listType) { mutableStateOf<String?>(null) }
    var users by remember(targetUid, listType) { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var myFollowingIds by remember(targetUid, listType) { mutableStateOf<Set<String>>(emptySet()) }
    var blockedIds by remember(targetUid, listType) { mutableStateOf<Set<String>>(emptySet()) }
    var busyUids by remember(targetUid, listType) { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by rememberSaveable(targetUid, listType.name) { mutableStateOf("") }

    suspend fun loadList() {
        isLoading = true
        errorMessage = null

        val listResult = when (listType) {
            UserListType.FOLLOWERS -> followRepo.getFollowers(targetUid)
            UserListType.FOLLOWING, UserListType.FRIENDS -> followRepo.getFollowing(targetUid)
        }

        if (!currentUserId.isNullOrBlank()) {
            val myFollowingRes = followRepo.getFollowing(currentUserId)
            if (myFollowingRes is ProfileOperationResult.Success) {
                myFollowingIds = myFollowingRes.data.map { it.uid }.toSet()
            }
            val blockedRes = blockRepo.getBlockedUserIds(currentUserId)
            if (blockedRes is ProfileOperationResult.Success) {
                blockedIds = blockedRes.data.toSet()
            }
        }

        when (listResult) {
            is ProfileOperationResult.Success -> {
                users = listResult.data
                isLoading = false
            }
            is ProfileOperationResult.Error -> {
                errorMessage = listResult.message
                isLoading = false
            }
        }
    }

    LaunchedEffect(targetUid, listType, currentUserId) {
        loadList()
    }

    val title = when (listType) {
        UserListType.FOLLOWERS -> "Followers"
        UserListType.FOLLOWING -> "Following"
        UserListType.FRIENDS -> "Friends"
    }

    val displayedCount = if (!isLoading && errorMessage == null) {
        if (users.size < 100) users.size else maxOf(initialCount, users.size)
    } else {
        initialCount
    }

    val filteredUsers = remember(users, searchQuery) {
        val q = searchQuery.trim().removePrefix("@").lowercase()
        if (q.isBlank()) {
            users
        } else {
            users.filter { profile ->
                profile.displayName.lowercase().contains(q) ||
                    profile.username.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Sheet Header with Title + Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LivePrimaryEmerald.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$displayedCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LivePrimaryEmerald,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Search Field
            if (users.isNotEmpty() || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search $title",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LivePrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LivePrimaryEmerald)
                        }
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage ?: "Failed to load $title.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { coroutineScope.launch { loadList() } },
                                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                            ) {
                                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    filteredUsers.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "No matching users found for \"$searchQuery\"."
                                } else if (listType == UserListType.FOLLOWERS) {
                                    "No followers yet."
                                } else {
                                    "Not following anyone yet."
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredUsers, key = { it.uid }) { member ->
                                val memberName = member.displayName.ifBlank { "WorkDaily Member" }
                                val memberUsername = if (member.username.isNotBlank()) "@${member.username}" else "@member"
                                val isCurrentUserRow = currentUserId != null && member.uid == currentUserId
                                val isRowFollowing = myFollowingIds.contains(member.uid)
                                val isRowBusy = busyUids.contains(member.uid)
                                val isRowBlocked = blockedIds.contains(member.uid)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onUserSelected(member) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WorkDailyLiveAvatar(
                                        name = memberName,
                                        avatarUrl = member.photoUrl ?: "",
                                        size = 46.dp,
                                        isLive = false,
                                        showBadge = false
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = memberName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (member.isVerified) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = LivePrimaryEmerald,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = memberUsername,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!isCurrentUserRow && !isRowBlocked) {
                                        FollowButton(
                                            isFollowing = isRowFollowing,
                                            isLoading = isRowBusy,
                                            height = 36.dp,
                                            minWidth = 92.dp,
                                            onToggle = {
                                                if (currentUserId.isNullOrBlank()) {
                                                    Toast.makeText(context, "Sign in with Google to follow members.", Toast.LENGTH_SHORT).show()
                                                    return@FollowButton
                                                }
                                                if (member.uid == currentUserId || isRowBusy) {
                                                    return@FollowButton
                                                }
                                                val wasFollowing = isRowFollowing
                                                val wantFollow = !wasFollowing
                                                busyUids = busyUids + member.uid

                                                coroutineScope.launch {
                                                    try {
                                                        val res = if (wantFollow) {
                                                            followRepo.followUser(currentUserId, member.uid)
                                                        } else {
                                                            followRepo.unfollowUser(currentUserId, member.uid)
                                                        }
                                                        when (res) {
                                                            is ProfileOperationResult.Success -> {
                                                                myFollowingIds = if (wantFollow) {
                                                                    myFollowingIds + member.uid
                                                                } else {
                                                                    myFollowingIds - member.uid
                                                                }
                                                                if (targetUid == currentUserId && listType == UserListType.FOLLOWING && !wantFollow) {
                                                                    users = users.filterNot { it.uid == member.uid }
                                                                }
                                                                onRelationshipChanged()
                                                            }
                                                            is ProfileOperationResult.Error -> {
                                                                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, e.message ?: "Action failed", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        busyUids = busyUids - member.uid
                                                    }
                                                }
                                            }
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
