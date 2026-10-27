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
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.BlockRepository
import com.synfusion.attendance.online.FirestoreReportRepository
import com.synfusion.attendance.online.FollowCounts
import com.synfusion.attendance.online.FollowRepository
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.SocialSessionCache
import com.synfusion.attendance.online.rememberCurrentFirebaseUid
import com.synfusion.attendance.ui.components.PublicProfileScreenSkeleton
import com.synfusion.attendance.ui.components.UserRowSkeletonList
import com.synfusion.attendance.ui.components.WorkDailyErrorRetryState
import com.synfusion.attendance.ui.components.WorkDailySubtleRefreshBar
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    val currentUserId = rememberCurrentFirebaseUid().value
    val socialSnapshot by SocialSessionCache.manager.snapshot.collectAsStateWithLifecycle(
        initialValue = SocialSessionCache.manager.snapshot.value
    )

    val isSelf = userId == null ||
        userId == "current_user" ||
        userId == "user_me" ||
        (currentUserId != null && userId == currentUserId)

    LaunchedEffect(isSelf) {
        if (isSelf) {
            onNavigateMainProfile()
        }
    }

    if (isSelf || userId.isNullOrBlank()) {
        return
    }

    val activeViewerUid: String? = currentUserId?.takeIf { it.isNotBlank() }

    val cachedProfile: PublicProfile? = remember(socialSnapshot, userId, activeViewerUid) {
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.profiles[userId]?.value
        } else {
            null
        }
    }
    val cachedCounts: FollowCounts? = remember(socialSnapshot, userId, activeViewerUid) {
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.followCounts[userId]?.value
        } else {
            null
        }
    }

    var isLoading by remember(userId) { mutableStateOf(cachedProfile == null) }
    var isRefreshing by remember(userId) { mutableStateOf(false) }
    var publicProfile by remember(userId) { mutableStateOf<PublicProfile?>(cachedProfile) }
    var followerCount by remember(userId) {
        mutableIntStateOf(
            cachedCounts?.followerCount
                ?: cachedProfile?.followerCount?.coerceAtLeast(0)
                ?: 0
        )
    }
    var followingCount by remember(userId) {
        mutableIntStateOf(
            cachedCounts?.followingCount
                ?: cachedProfile?.followingCount?.coerceAtLeast(0)
                ?: 0
        )
    }
    var isFollowingTarget by remember(userId, activeViewerUid) {
        mutableStateOf(
            activeViewerUid != null &&
                socialSnapshot.ownerUid == activeViewerUid &&
                socialSnapshot.myFollowedUids?.value?.contains(userId) == true
        )
    }
    var isBlocked by remember(userId, activeViewerUid) {
        mutableStateOf(
            activeViewerUid != null &&
                socialSnapshot.ownerUid == activeViewerUid &&
                socialSnapshot.myBlockedUids?.value?.contains(userId) == true
        )
    }
    var isBlockedByTarget by remember(userId) { mutableStateOf(false) }
    var isBlockActionBusy by remember(userId) { mutableStateOf(false) }
    var isFollowMutatingLocal by remember(userId) { mutableStateOf(false) }
    var loadError by remember(userId) { mutableStateOf<String?>(null) }
    var activeUserListType by remember(userId) { mutableStateOf<UserListType?>(null) }

    val followRepo = SocialSessionCache.followRepository
    val blockRepo = SocialSessionCache.blockRepository
    val reportRepo = remember { FirestoreReportRepository() }
    val coroutineScope = rememberCoroutineScope()

    val isFollowActionBusy = isFollowMutatingLocal || (
        activeViewerUid != null &&
            socialSnapshot.ownerUid == activeViewerUid &&
            socialSnapshot.activeMutations.contains("follow:$activeViewerUid:$userId")
        )

    // Keep displayed counts and follow state reactively synced with SocialSessionCache
    LaunchedEffect(cachedCounts) {
        if (cachedCounts != null) {
            followerCount = cachedCounts.followerCount
            followingCount = cachedCounts.followingCount
        }
    }
    LaunchedEffect(cachedProfile) {
        if (cachedProfile != null) {
            publicProfile = cachedProfile
        }
    }
    LaunchedEffect(socialSnapshot.myFollowedUids, socialSnapshot.ownerUid, activeViewerUid) {
        val followingSet = if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.myFollowedUids?.value
        } else {
            null
        }
        if (followingSet != null && !isBlocked && !isBlockedByTarget) {
            isFollowingTarget = followingSet.contains(userId)
        }
    }
    LaunchedEffect(socialSnapshot.myBlockedUids, socialSnapshot.ownerUid, activeViewerUid) {
        val blockedSet = if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.myBlockedUids?.value
        } else {
            null
        }
        if (blockedSet != null) {
            val nowBlocked = blockedSet.contains(userId)
            isBlocked = nowBlocked
            if (nowBlocked) {
                isFollowingTarget = false
            }
        }
    }

    suspend fun reconcileCounts(targetUid: String, forceRefresh: Boolean = true) {
        if (targetUid.isBlank()) return
        val viewerUid = currentUserId?.takeIf { it.isNotBlank() }
        val countsRes: ProfileOperationResult<FollowCounts> = if (viewerUid != null) {
            SocialSessionCache.manager.getFollowCounts(
                ownerUid = viewerUid,
                targetUid = targetUid,
                forceRefresh = forceRefresh
            )
        } else {
            followRepo.getFollowCounts(targetUid)
        }
        when (countsRes) {
            is ProfileOperationResult.Success -> {
                followerCount = countsRes.data.followerCount
                followingCount = countsRes.data.followingCount
            }
            is ProfileOperationResult.Error -> {
                // Keep current state rather than overwriting with stale pre-mutation profile counts.
            }
        }
    }

    suspend fun loadFullProfileState(forceRefresh: Boolean) {
        val hasExistingContent = publicProfile != null
        if (!hasExistingContent) {
            isLoading = true
        } else {
            isRefreshing = true
        }
        loadError = null

        try {
            coroutineScope {
                val viewerUid = currentUserId?.takeIf { it.isNotBlank() }

                val profileDeferred = async<ProfileOperationResult<PublicProfile?>> {
                    if (viewerUid != null) {
                        SocialSessionCache.manager.getPublicProfile(
                            ownerUid = viewerUid,
                            targetUid = userId,
                            forceRefresh = forceRefresh
                        )
                    } else {
                        SocialSessionCache.publicProfileRepository.getPublicProfile(userId)
                    }
                }
                val countsDeferred = async<ProfileOperationResult<FollowCounts>> {
                    if (viewerUid != null) {
                        SocialSessionCache.manager.getFollowCounts(
                            ownerUid = viewerUid,
                            targetUid = userId,
                            forceRefresh = forceRefresh
                        )
                    } else {
                        followRepo.getFollowCounts(userId)
                    }
                }

                val blockedByMeDeferred = if (viewerUid != null) {
                    async<Boolean> { blockRepo.isBlocked(viewerUid, userId) }
                } else null
                val blockedByTargetDeferred = if (viewerUid != null) {
                    async<Boolean> { blockRepo.isBlocked(userId, viewerUid) }
                } else null
                val followingDeferred = if (viewerUid != null) {
                    async<Boolean> { followRepo.isFollowing(viewerUid, userId) }
                } else null

                val profileRes: ProfileOperationResult<PublicProfile?> = profileDeferred.await()
                when (profileRes) {
                    is ProfileOperationResult.Success -> {
                        val loaded: PublicProfile? = profileRes.data
                        publicProfile = loaded
                        if (loaded != null && cachedCounts == null) {
                            followerCount = loaded.followerCount.coerceAtLeast(0)
                            followingCount = loaded.followingCount.coerceAtLeast(0)
                        }
                    }
                    is ProfileOperationResult.Error -> {
                        if (publicProfile == null) {
                            loadError = profileRes.message
                        }
                    }
                }

                val countsRes: ProfileOperationResult<FollowCounts> = countsDeferred.await()
                when (countsRes) {
                    is ProfileOperationResult.Success -> {
                        followerCount = countsRes.data.followerCount
                        followingCount = countsRes.data.followingCount
                    }
                    is ProfileOperationResult.Error -> {
                        // Preserve any existing/profile fallback counts on transient count-fetch errors.
                    }
                }

                if (blockedByMeDeferred != null && blockedByTargetDeferred != null && followingDeferred != null) {
                    val blockedByMe = blockedByMeDeferred.await()
                    val blockedByThem = blockedByTargetDeferred.await()
                    val followingNow = followingDeferred.await()
                    isBlocked = blockedByMe
                    isBlockedByTarget = blockedByThem
                    isFollowingTarget = if (blockedByMe || blockedByThem) false else followingNow
                } else {
                    isBlocked = false
                    isBlockedByTarget = false
                    isFollowingTarget = false
                }
            }
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(userId, currentUserId) {
        loadFullProfileState(forceRefresh = false)
    }

    DisposableEffect(lifecycleOwner, userId, currentUserId) {
        var handledInitialResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!handledInitialResume) {
                    handledInitialResume = true
                } else {
                    coroutineScope.launch {
                        loadFullProfileState(forceRefresh = false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isLoading && publicProfile == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            PublicProfileScreenSkeleton(modifier = Modifier.padding(padding))
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
                                    coroutineScope.launch { loadFullProfileState(forceRefresh = true) }
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
        val activeUid = currentUserId
        if (activeUid.isNullOrBlank()) {
            Toast.makeText(context, "Sign in with Google to manage blocked accounts.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isBlockActionBusy) return
        isBlockActionBusy = true
        coroutineScope.launch {
            try {
                when (val res = blockRepo.unblockUser(activeUid, targetUser.uid)) {
                    is ProfileOperationResult.Success -> {
                        SocialSessionCache.manager.recordUnblockSuccess(activeUid, targetUser.uid)
                        isBlocked = false
                        // Unblocking never auto-follows; verify actual follow state & counts
                        isFollowingTarget = followRepo.isFollowing(activeUid, targetUser.uid)
                        reconcileCounts(targetUser.uid, forceRefresh = true)
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
                        val activeUid = currentUserId
                        if (activeUid.isNullOrBlank() || isBlockActionBusy) return@Button
                        isBlockActionBusy = true
                        coroutineScope.launch {
                            try {
                                when (val res = blockRepo.blockUser(activeUid, targetUser.uid)) {
                                    is ProfileOperationResult.Success -> {
                                        SocialSessionCache.manager.recordBlockSuccess(activeUid, targetUser.uid)
                                        isBlocked = true
                                        isFollowingTarget = false
                                        showBlockConfirmDialog = false
                                        reconcileCounts(targetUser.uid, forceRefresh = true)
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
                        val activeUid = currentUserId
                        if (reportReason.isNotBlank() && !activeUid.isNullOrBlank()) {
                            isSubmittingReport = true
                            coroutineScope.launch {
                                val res = reportRepo.submitReport(activeUid, targetUser.uid, reportReason, reportDetails)
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

    val selectedListType = activeUserListType
    if (selectedListType != null && targetUser.uid.isNotBlank()) {
        UserRelationshipBottomSheet(
            targetUid = targetUser.uid,
            listType = selectedListType,
            initialCount = if (selectedListType == UserListType.FOLLOWERS) followerCount else followingCount,
            currentUserId = currentUserId,
            followRepo = followRepo,
            blockRepo = blockRepo,
            onDismiss = {
                activeUserListType = null
                val activeUid = currentUserId
                coroutineScope.launch {
                    reconcileCounts(targetUser.uid, forceRefresh = false)
                    if (!activeUid.isNullOrBlank() && !isBlocked && !isBlockedByTarget) {
                        isFollowingTarget = followRepo.isFollowing(activeUid, targetUser.uid)
                    }
                }
            },
            onRelationshipChanged = {
                coroutineScope.launch {
                    reconcileCounts(targetUser.uid, forceRefresh = true)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            WorkDailySubtleRefreshBar(visible = isRefreshing)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
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
                                val activeUid = currentUserId?.takeIf { it.isNotBlank() }
                                if (activeUid == null) {
                                    Toast.makeText(context, "Sign in with Google to follow members.", Toast.LENGTH_SHORT).show()
                                    return@FollowButton
                                }
                                if (targetUser.uid.isBlank() || activeUid == targetUser.uid) {
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

                                // Immediate UI response (also mirrored in SocialSessionCache)
                                isFollowMutatingLocal = true
                                isFollowingTarget = desiredStatus
                                followerCount = (previousFollowers + if (desiredStatus) 1 else -1).coerceAtLeast(0)

                                coroutineScope.launch {
                                    try {
                                        val result: ProfileOperationResult<Boolean>? = SocialSessionCache.manager.setFollowState(
                                            actorUid = activeUid,
                                            targetUid = targetUser.uid,
                                            wantFollow = desiredStatus,
                                            targetProfileHint = targetUser
                                        )
                                        when (result) {
                                            is ProfileOperationResult.Success -> {
                                                isFollowingTarget = desiredStatus
                                                reconcileCounts(targetUser.uid, forceRefresh = false)
                                            }
                                            is ProfileOperationResult.Error -> {
                                                isFollowingTarget = previousStatus
                                                followerCount = previousFollowers
                                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                            }
                                            null -> {
                                                // Duplicate tap while a follow/unfollow mutation was already active
                                                isFollowingTarget = previousStatus
                                                followerCount = previousFollowers
                                            }
                                        }
                                    } finally {
                                        isFollowMutatingLocal = false
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
    val socialSnapshot by SocialSessionCache.manager.snapshot.collectAsStateWithLifecycle(
        initialValue = SocialSessionCache.manager.snapshot.value
    )

    val pageStep = 40
    var currentLimit by rememberSaveable(targetUid, listType.name) { mutableIntStateOf(pageStep) }
    val activeViewerUid: String? = currentUserId?.takeIf { it.isNotBlank() }

    val cachedList: List<PublicProfile>? = remember(socialSnapshot, targetUid, listType, activeViewerUid) {
        if (activeViewerUid != null && targetUid.isNotBlank() && socialSnapshot.ownerUid == activeViewerUid) {
            when (listType) {
                UserListType.FOLLOWERS -> socialSnapshot.followersLists[targetUid]?.value?.users
                UserListType.FOLLOWING, UserListType.FRIENDS -> socialSnapshot.followingLists[targetUid]?.value?.users
            }
        } else {
            null
        }
    }

    var isLoading by remember(targetUid, listType) { mutableStateOf(cachedList == null) }
    var isRefreshing by remember(targetUid, listType) { mutableStateOf(false) }
    var isLoadingMore by remember(targetUid, listType) { mutableStateOf(false) }
    var errorMessage by remember(targetUid, listType) { mutableStateOf<String?>(null) }
    var users by remember(targetUid, listType) { mutableStateOf<List<PublicProfile>>(cachedList ?: emptyList()) }
    var myFollowingIds by remember(targetUid, listType, activeViewerUid) {
        mutableStateOf<Set<String>>(
            if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
                socialSnapshot.myFollowedUids?.value ?: emptySet()
            } else {
                emptySet()
            }
        )
    }
    var myFollowerIds by remember(targetUid, listType, activeViewerUid) {
        mutableStateOf<Set<String>>(
            if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
                socialSnapshot.myFollowerUids?.value ?: emptySet()
            } else {
                emptySet()
            }
        )
    }
    var blockedIds by remember(targetUid, listType, activeViewerUid) {
        mutableStateOf<Set<String>>(
            if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
                socialSnapshot.myBlockedUids?.value ?: emptySet()
            } else {
                emptySet()
            }
        )
    }
    var mutatingRowIds by remember(targetUid, listType) { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by rememberSaveable(targetUid, listType.name) { mutableStateOf("") }

    LaunchedEffect(
        socialSnapshot.myFollowedUids,
        socialSnapshot.myFollowerUids,
        socialSnapshot.myBlockedUids,
        socialSnapshot.ownerUid,
        activeViewerUid
    ) {
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.myFollowedUids?.value?.let { myFollowingIds = it }
            socialSnapshot.myFollowerUids?.value?.let { myFollowerIds = it }
            socialSnapshot.myBlockedUids?.value?.let { blockedIds = it }
        }
    }
    LaunchedEffect(cachedList) {
        if (cachedList != null) {
            users = cachedList
        }
    }

    suspend fun loadList(limitToFetch: Int = currentLimit, forceRefresh: Boolean = false, loadingMore: Boolean = false) {
        if (targetUid.isBlank()) {
            users = emptyList()
            isLoading = false
            isRefreshing = false
            isLoadingMore = false
            return
        }
        val hasContent = users.isNotEmpty()
        when {
            loadingMore -> isLoadingMore = true
            hasContent -> isRefreshing = true
            else -> isLoading = true
        }
        errorMessage = null

        try {
            coroutineScope {
                val viewerUid = currentUserId?.takeIf { it.isNotBlank() }

                val listDeferred = async<ProfileOperationResult<List<PublicProfile>>> {
                    if (viewerUid != null) {
                        when (listType) {
                            UserListType.FOLLOWERS -> SocialSessionCache.manager.getFollowersList(
                                ownerUid = viewerUid,
                                targetUid = targetUid,
                                limit = limitToFetch,
                                forceRefresh = forceRefresh
                            )
                            UserListType.FOLLOWING, UserListType.FRIENDS -> SocialSessionCache.manager.getFollowingList(
                                ownerUid = viewerUid,
                                targetUid = targetUid,
                                limit = limitToFetch,
                                forceRefresh = forceRefresh
                            )
                        }
                    } else {
                        when (listType) {
                            UserListType.FOLLOWERS -> followRepo.getFollowers(targetUid, limit = limitToFetch)
                            UserListType.FOLLOWING, UserListType.FRIENDS -> followRepo.getFollowing(targetUid, limit = limitToFetch)
                        }
                    }
                }

                val myFollowingDeferred = if (viewerUid != null) {
                    async<ProfileOperationResult<List<PublicProfile>>> {
                        SocialSessionCache.manager.getFollowingList(
                            ownerUid = viewerUid,
                            targetUid = viewerUid,
                            forceRefresh = false
                        )
                    }
                } else null

                val blockedDeferred = if (viewerUid != null) {
                    async<ProfileOperationResult<Set<String>>> {
                        SocialSessionCache.manager.getBlockedUserIds(
                            ownerUid = viewerUid,
                            forceRefresh = false
                        )
                    }
                } else null

                val countsDeferred = async<ProfileOperationResult<FollowCounts>> {
                    if (viewerUid != null) {
                        SocialSessionCache.manager.getFollowCounts(
                            ownerUid = viewerUid,
                            targetUid = targetUid,
                            forceRefresh = forceRefresh
                        )
                    } else {
                        followRepo.getFollowCounts(targetUid)
                    }
                }

                myFollowingDeferred?.await()?.let { res ->
                    if (res is ProfileOperationResult.Success) {
                        myFollowingIds = res.data.map { it.uid }.toSet()
                    }
                }
                blockedDeferred?.await()?.let { res ->
                    if (res is ProfileOperationResult.Success) {
                        blockedIds = res.data
                    }
                }
                countsDeferred.await()

                val listResult: ProfileOperationResult<List<PublicProfile>> = listDeferred.await()
                when (listResult) {
                    is ProfileOperationResult.Success -> {
                        users = listResult.data
                        errorMessage = null
                    }
                    is ProfileOperationResult.Error -> {
                        if (users.isEmpty()) {
                            errorMessage = listResult.message
                        } else {
                            Toast.makeText(context, listResult.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } finally {
            isLoading = false
            isRefreshing = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(targetUid, listType, currentUserId) {
        loadList(limitToFetch = currentLimit, forceRefresh = false)
    }

    val title = when (listType) {
        UserListType.FOLLOWERS -> "Followers"
        UserListType.FOLLOWING -> "Following"
        UserListType.FRIENDS -> "Friends"
    }

    val cachedTargetCounts = remember(socialSnapshot, targetUid, activeViewerUid) {
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.followCounts[targetUid]?.value
        } else {
            null
        }
    }

    val displayedCount = when (listType) {
        UserListType.FOLLOWERS -> cachedTargetCounts?.followerCount ?: initialCount
        UserListType.FOLLOWING, UserListType.FRIENDS -> cachedTargetCounts?.followingCount ?: initialCount
    }.coerceAtLeast(0)

    val canLoadMore = !isLoading && errorMessage == null && searchQuery.isBlank() &&
        users.size >= currentLimit && displayedCount > users.size

    val filteredUsers = remember(users, searchQuery, blockedIds) {
        val visible = users.filterNot { blockedIds.contains(it.uid) }
        val q = searchQuery.trim().removePrefix("@").lowercase()
        if (q.isBlank()) {
            visible
        } else {
            visible.filter { profile ->
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
            // Sheet Header with Title + Authoritative Count
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
            WorkDailySubtleRefreshBar(visible = isRefreshing)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading && users.isEmpty() -> {
                        UserRowSkeletonList(
                            rowCount = 5,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    errorMessage != null && users.isEmpty() -> {
                        WorkDailyErrorRetryState(
                            title = "Couldn't load $title",
                            message = errorMessage ?: "Failed to load $title.",
                            onRetry = {
                                coroutineScope.launch {
                                    loadList(limitToFetch = currentLimit, forceRefresh = true)
                                }
                            }
                        )
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
                                val isCurrentUserRow = activeViewerUid != null && member.uid == activeViewerUid
                                val isRowFollowing = myFollowingIds.contains(member.uid)
                                val followsMe = myFollowerIds.contains(member.uid)
                                val isRowBusy = mutatingRowIds.contains(member.uid) || (
                                    activeViewerUid != null &&
                                        socialSnapshot.ownerUid == activeViewerUid &&
                                        socialSnapshot.activeMutations.contains("follow:$activeViewerUid:${member.uid}")
                                    )
                                val isRowBlocked = blockedIds.contains(member.uid)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onUserSelected(member) }
                                        .padding(horizontal = 8.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WorkDailyLiveAvatar(
                                        name = memberName,
                                        avatarUrl = member.photoUrl ?: "",
                                        size = 44.dp,
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
                                                fontSize = 14.sp,
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
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = memberUsername,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (!isCurrentUserRow && followsMe && isRowFollowing) {
                                                Text(
                                                    text = "• Mutual",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = LivePrimaryEmerald,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    if (!isCurrentUserRow && !isRowBlocked) {
                                        FollowButton(
                                            isFollowing = isRowFollowing,
                                            isLoading = isRowBusy,
                                            height = 34.dp,
                                            minWidth = 88.dp,
                                            onToggle = {
                                                val activeUid = currentUserId?.takeIf { it.isNotBlank() }
                                                if (activeUid == null) {
                                                    Toast.makeText(context, "Sign in with Google to follow members.", Toast.LENGTH_SHORT).show()
                                                    return@FollowButton
                                                }
                                                if (member.uid.isBlank() || member.uid == activeUid || isRowBusy) {
                                                    return@FollowButton
                                                }
                                                val wasFollowing = isRowFollowing
                                                val wantFollow = !wasFollowing

                                                mutatingRowIds = mutatingRowIds + member.uid
                                                myFollowingIds = if (wantFollow) {
                                                    myFollowingIds + member.uid
                                                } else {
                                                    myFollowingIds - member.uid
                                                }

                                                coroutineScope.launch {
                                                    try {
                                                        val res: ProfileOperationResult<Boolean>? = SocialSessionCache.manager.setFollowState(
                                                            actorUid = activeUid,
                                                            targetUid = member.uid,
                                                            wantFollow = wantFollow,
                                                            targetProfileHint = member
                                                        )
                                                        when (res) {
                                                            is ProfileOperationResult.Success -> {
                                                                if (targetUid == activeUid && listType == UserListType.FOLLOWING && !wantFollow) {
                                                                    users = users.filterNot { it.uid == member.uid }
                                                                }
                                                                onRelationshipChanged()
                                                            }
                                                            is ProfileOperationResult.Error -> {
                                                                myFollowingIds = if (wasFollowing) {
                                                                    myFollowingIds + member.uid
                                                                } else {
                                                                    myFollowingIds - member.uid
                                                                }
                                                                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                                            }
                                                            null -> {
                                                                // Duplicate tap while mutation already active; restore pre-tap state
                                                                myFollowingIds = if (wasFollowing) {
                                                                    myFollowingIds + member.uid
                                                                } else {
                                                                    myFollowingIds - member.uid
                                                                }
                                                            }
                                                        }
                                                    } finally {
                                                        mutatingRowIds = mutatingRowIds - member.uid
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (canLoadMore) {
                                item(key = "load_more_relationships") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (!isLoadingMore) {
                                                    val nextLimit = currentLimit + pageStep
                                                    currentLimit = nextLimit
                                                    coroutineScope.launch {
                                                        loadList(limitToFetch = nextLimit, forceRefresh = true, loadingMore = true)
                                                    }
                                                }
                                            },
                                            enabled = !isLoadingMore,
                                            shape = RoundedCornerShape(18.dp),
                                            border = BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = if (isLoadingMore) "Loading…" else "Load more",
                                                color = LivePrimaryEmerald,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
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
}
