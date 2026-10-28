package com.synfusion.attendance.live.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.live.ui.components.FollowButton
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.SocialSessionCache
import com.synfusion.attendance.online.rememberCurrentFirebaseUid
import com.synfusion.attendance.ui.components.UserRowSkeletonList
import com.synfusion.attendance.ui.components.WorkDailyErrorRetryState
import com.synfusion.attendance.ui.components.WorkDailyPulseDots
import com.synfusion.attendance.ui.components.WorkDailySubtleRefreshBar
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PEOPLE_PAGE_STEP = 40

/**
 * People / Discover screen.
 *
 * The Discover (search) experience is shown directly -- there are no Followers / Following tabs.
 * Follower and Following records, repository methods and profile-level follow functionality are
 * untouched; only the tab-based navigation on this screen is removed.
 *
 * Blocked users are filtered out of search results and recent-search entries are never used to
 * surface a blocked account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleAndFriendsScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateProfile: (String) -> Unit,
    onNavigatePrivateChat: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val preferences = remember {
        context.getSharedPreferences("workdaily_people_search", Context.MODE_PRIVATE)
    }

    val currentUserId = rememberCurrentFirebaseUid().value
    val socialSnapshot by SocialSessionCache.manager.snapshot.collectAsStateWithLifecycle(
        initialValue = SocialSessionCache.manager.snapshot.value
    )

    // UID-scoped recent search history key so accounts never share search history
    val recentQueriesKey = remember(currentUserId) { "recent_queries_${currentUserId ?: "guest"}" }

    fun loadRecentForKey(key: String): List<String> {
        return preferences.getString(key, "")
            .orEmpty()
            .split("\n")
            .map { it.trim().removePrefix("@") }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(8)
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val activeViewerUid: String? = currentUserId?.takeIf { it.isNotBlank() }

    // Blocked users for the current account. Kept in sync with SocialSessionCache so blocked
    // accounts never surface in search results, recent searches or follow/message rows.
    var blockedUids by remember(currentUserId) {
        mutableStateOf<Set<String>>(
            if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
                socialSnapshot.myBlockedUids?.value ?: emptySet()
            } else {
                emptySet()
            }
        )
    }
    // Users the current account follows -- used only to render the Follow button's correct state.
    var followedUids by remember(currentUserId) {
        mutableStateOf<Set<String>>(
            if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
                socialSnapshot.myFollowedUids?.value ?: emptySet()
            } else {
                emptySet()
            }
        )
    }

    LaunchedEffect(
        socialSnapshot.myBlockedUids,
        socialSnapshot.myFollowedUids,
        socialSnapshot.ownerUid,
        activeViewerUid
    ) {
        if (activeViewerUid != null && socialSnapshot.ownerUid == activeViewerUid) {
            socialSnapshot.myBlockedUids?.value?.let { blockedUids = it }
            socialSnapshot.myFollowedUids?.value?.let { followedUids = it }
        }
    }

    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchRetryNonce by remember { mutableIntStateOf(0) }

    var recentQueries by remember(recentQueriesKey) {
        mutableStateOf(loadRecentForKey(recentQueriesKey))
    }
    var showAllRecent by rememberSaveable { mutableStateOf(false) }

    fun persistRecent(query: String) {
        val clean = query.trim().removePrefix("@").trim()
        if (clean.isBlank()) return
        val updated = (listOf(clean) + recentQueries.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
        recentQueries = updated
        preferences.edit().putString(recentQueriesKey, updated.joinToString("\n")).apply()
    }

    fun removeRecent(query: String) {
        val updated = recentQueries.filterNot { it.equals(query, ignoreCase = true) }
        recentQueries = updated
        preferences.edit().putString(recentQueriesKey, updated.joinToString("\n")).apply()
    }

    fun clearAllRecent() {
        recentQueries = emptyList()
        preferences.edit().remove(recentQueriesKey).apply()
    }

    /**
     * Removes every recent-search entry whose text matches a blocked user's display name or
     * username, so a blocked account cannot be re-opened via recent searches.
     */
    fun purgeBlockedFromRecents() {
        if (blockedUids.isEmpty()) return
        val snapshot = SocialSessionCache.manager.snapshot.value
        val blockedLabels = blockedUids.flatMap { uid ->
            snapshot.profiles[uid]?.value?.let { profile ->
                listOf(profile.username, profile.displayName).filter { it.isNotBlank() }
            } ?: emptyList()
        }
        if (blockedLabels.isEmpty()) return
        val updated = recentQueries.filter { recent ->
            blockedLabels.none { label -> recent.equals(label, ignoreCase = true) }
        }
        if (updated.size != recentQueries.size) {
            recentQueries = updated
            preferences.edit().putString(recentQueriesKey, updated.joinToString("\n")).apply()
        }
    }

    // Purge blocked users from recent searches whenever the blocked set changes.
    LaunchedEffect(blockedUids) {
        purgeBlockedFromRecents()
    }

    // Debounced, cancellable search using SocialSessionCache (immediate hit on cached queries)
    LaunchedEffect(searchQuery, currentUserId, searchRetryNonce) {
        val normalized = searchQuery.trim().removePrefix("@").trim()
        if (normalized.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            searchError = null
            return@LaunchedEffect
        }

        val warmCached = SocialSessionCache.manager
            .currentSnapshotFor(currentUserId)
            ?.searchResults
            ?.get(normalized.lowercase())
            ?.value

        if (warmCached != null && searchRetryNonce == 0) {
            searchResults = warmCached
            searchError = null
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        searchError = null
        delay(280)

        when (
            val result = SocialSessionCache.manager.searchProfiles(
                ownerUid = currentUserId,
                rawQuery = normalized,
                forceRefresh = searchRetryNonce > 0
            )
        ) {
            is ProfileOperationResult.Success -> {
                searchResults = result.data
                searchError = null
                isSearching = false
            }
            is ProfileOperationResult.Error -> {
                if (searchResults.isEmpty()) {
                    searchError = result.message
                }
                isSearching = false
            }
        }
    }

    val cleanFilterQuery = remember(searchQuery) { searchQuery.trim().removePrefix("@").trim() }

    // Search results filtered client-side: blocklisted accounts are never shown.
    val displayedUsers = remember(searchResults, cleanFilterQuery, blockedUids) {
        searchResults
            .filter { it.displayName.contains(cleanFilterQuery, ignoreCase = true) || it.username.contains(cleanFilterQuery, ignoreCase = true) }
            .filterNot { blockedUids.contains(it.uid) }
    }

    val screenBackground = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 1. Compact Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(screenBackground)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = WorkDailyIcons.ArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "People",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }

            // 2. Compact Rounded Search Bar
            val searchBorderColor = when {
                isSearchFocused -> LivePrimaryEmerald.copy(alpha = 0.7f)
                searchQuery.isNotEmpty() -> LivePrimaryEmerald.copy(alpha = 0.42f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, searchBorderColor, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSearchFocused) LivePrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val fieldTextStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search name or @username",
                            style = fieldTextStyle.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        textStyle = fieldTextStyle,
                        cursorBrush = SolidColor(LivePrimaryEmerald),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                persistRecent(searchQuery)
                                keyboardController?.hide()
                            }
                        )
                    )
                }

                when {
                    isSearching -> {
                        WorkDailyPulseDots(
                            color = LivePrimaryEmerald,
                            dotSize = 4.5.dp,
                            spacing = 3.dp
                        )
                    }
                    searchQuery.isNotEmpty() -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
        }

        // 3. Main Content Area (Recent Searches / Empty / Retry / User Rows)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
        ) {
            when {
                // Idle state: Recent Searches (scrollable) or clean prompt
                cleanFilterQuery.isEmpty() -> {
                    if (recentQueries.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(LivePrimaryEmerald.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = LivePrimaryEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Find people on WorkDaily",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Search by display name or @username to follow coworkers and message mutual friends.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        val visibleRecents = if (showAllRecent) recentQueries else recentQueries.take(5)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            item(key = "recent_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Recent searches",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (recentQueries.size > 5) {
                                            Text(
                                                text = if (showAllRecent) "Show less" else "See all",
                                                color = LivePrimaryEmerald,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { showAllRecent = !showAllRecent }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "Clear all",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { clearAllRecent() }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            items(visibleRecents, key = { "recent_$it" }) { recent ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            searchQuery = recent
                                            keyboardController?.hide()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "@$recent",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { removeRecent(recent) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove recent search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cold initial skeleton (only when data is not yet available)
                isSearching && searchResults.isEmpty() -> {
                    UserRowSkeletonList(
                        rowCount = 6,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Clean retry state on failure
                searchError != null && searchResults.isEmpty() -> {
                    WorkDailyErrorRetryState(
                        title = "Couldn't load people",
                        message = searchError,
                        onRetry = {
                            searchRetryNonce += 1
                        }
                    )
                }

                // Clean empty state
                displayedUsers.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching accounts",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Check the spelling or search by @username.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Populated Compact User List
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        items(displayedUsers, key = { it.uid }) { user ->
                            val activeUid = currentUserId?.takeIf { it.isNotBlank() }
                            val isSelfRow = activeUid != null && activeUid == user.uid
                            val isFollowingUser = followedUids.contains(user.uid)

                            val displayName = user.displayName.ifBlank { "WorkDaily Member" }
                            val handle = if (user.username.isNotBlank()) "@${user.username}" else "@member"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        persistRecent(user.username.ifBlank { user.displayName })
                                        onNavigateProfile(user.uid)
                                    }
                                    .padding(vertical = 9.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WorkDailyLiveAvatar(
                                    name = displayName,
                                    avatarUrl = user.photoUrl ?: "",
                                    size = 44.dp,
                                    isLive = false,
                                    showBadge = false,
                                    onClick = {
                                        persistRecent(user.username.ifBlank { user.displayName })
                                        onNavigateProfile(user.uid)
                                    }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                        )
                                        if (user.isVerified) {
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
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = handle,
                                            fontSize = 12.sp,
                                            color = LivePrimaryEmerald,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                        )
                                    }
                                }

                                if (activeUid != null && !isSelfRow && !blockedUids.contains(user.uid)) {
                                    Spacer(modifier = Modifier.width(8.dp))

                                    FollowButton(
                                        isFollowing = isFollowingUser,
                                        height = 34.dp,
                                        minWidth = 86.dp,
                                        onToggle = {
                                            if (user.uid.isBlank()) return@FollowButton
                                            val wantFollow = !isFollowingUser
                                            scope.launch {
                                                val result: ProfileOperationResult<Boolean>? =
                                                    SocialSessionCache.manager.setFollowState(
                                                        actorUid = activeUid,
                                                        targetUid = user.uid,
                                                        wantFollow = wantFollow,
                                                        targetProfileHint = user
                                                    )
                                                when (result) {
                                                    is ProfileOperationResult.Success -> Unit
                                                    is ProfileOperationResult.Error -> {
                                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                    }
                                                    null -> Unit
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            persistRecent(user.username.ifBlank { user.displayName })
                                            onNavigatePrivateChat(user.uid)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Message",
                                            tint = if (isFollowingUser) {
                                                LivePrimaryEmerald
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
