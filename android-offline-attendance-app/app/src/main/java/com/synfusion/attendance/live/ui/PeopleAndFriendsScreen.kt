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

    var selectedTabKey by rememberSaveable { mutableStateOf("Discover") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val cachedFollowers = remember(socialSnapshot, currentUserId) {
        if (!currentUserId.isNullOrBlank() && socialSnapshot.ownerUid == currentUserId) {
            socialSnapshot.followersList?.value
        } else null
    }
    val cachedFollowing = remember(socialSnapshot, currentUserId) {
        if (!currentUserId.isNullOrBlank() && socialSnapshot.ownerUid == currentUserId) {
            socialSnapshot.followingList?.value
        } else null
    }
    val cachedCounts = remember(socialSnapshot, currentUserId) {
        val uid = currentUserId
        if (!uid.isNullOrBlank() && socialSnapshot.ownerUid == uid) {
            socialSnapshot.followCounts[uid]?.value
        } else null
    }

    var followersLimit by rememberSaveable(currentUserId) { mutableIntStateOf(PEOPLE_PAGE_STEP) }
    var followingLimit by rememberSaveable(currentUserId) { mutableIntStateOf(PEOPLE_PAGE_STEP) }

    var followersList by remember(currentUserId) { mutableStateOf(cachedFollowers ?: emptyList()) }
    var followingList by remember(currentUserId) { mutableStateOf(cachedFollowing ?: emptyList()) }
    var followedUids by remember(currentUserId) {
        mutableStateOf(
            if (!currentUserId.isNullOrBlank() && socialSnapshot.ownerUid == currentUserId) {
                socialSnapshot.followingIds?.value ?: emptySet()
            } else emptySet()
        )
    }
    var followerUids by remember(currentUserId) {
        mutableStateOf(
            if (!currentUserId.isNullOrBlank() && socialSnapshot.ownerUid == currentUserId) {
                socialSnapshot.followerIds?.value ?: emptySet()
            } else emptySet()
        )
    }
    var blockedUids by remember(currentUserId) {
        mutableStateOf(
            if (!currentUserId.isNullOrBlank() && socialSnapshot.ownerUid == currentUserId) {
                socialSnapshot.blockedIds?.value ?: emptySet()
            } else emptySet()
        )
    }

    var isRelationshipsInitialLoading by remember(currentUserId) {
        mutableStateOf(!currentUserId.isNullOrBlank() && (cachedFollowers == null || cachedFollowing == null))
    }
    var isRelationshipsRefreshing by remember(currentUserId) { mutableStateOf(false) }
    var isLoadingMoreRelationships by remember(currentUserId) { mutableStateOf(false) }
    var relationshipsError by remember(currentUserId) { mutableStateOf<String?>(null) }

    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchRetryNonce by remember { mutableIntStateOf(0) }

    var recentQueries by remember(recentQueriesKey) {
        mutableStateOf(loadRecentForKey(recentQueriesKey))
    }
    var showAllRecent by rememberSaveable { mutableStateOf(false) }

    // Sync local state immediately whenever SocialSessionCache updates (e.g. optimistic Follow/Unfollow/Block)
    LaunchedEffect(cachedFollowers) {
        if (cachedFollowers != null) followersList = cachedFollowers
    }
    LaunchedEffect(cachedFollowing) {
        if (cachedFollowing != null) followingList = cachedFollowing
    }
    LaunchedEffect(
        socialSnapshot.followingIds,
        socialSnapshot.followerIds,
        socialSnapshot.blockedIds,
        socialSnapshot.ownerUid,
        currentUserId
    ) {
        if (!currentUserId.isNullOrBlank() && socialSnapshot.ownerUid == currentUserId) {
            socialSnapshot.followingIds?.value?.let { followedUids = it }
            socialSnapshot.followerIds?.value?.let { followerUids = it }
            socialSnapshot.blockedIds?.value?.let { blockedUids = it }
        }
    }

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

    suspend fun loadRelationships(
        targetUid: String? = currentUserId,
        forceRefresh: Boolean = false,
        loadingMore: Boolean = false
    ) {
        if (targetUid.isNullOrBlank()) {
            followersList = emptyList()
            followingList = emptyList()
            followedUids = emptySet()
            followerUids = emptySet()
            blockedUids = emptySet()
            isRelationshipsInitialLoading = false
            isRelationshipsRefreshing = false
            isLoadingMoreRelationships = false
            relationshipsError = null
            return
        }

        val hasWarmLists = cachedFollowers != null && cachedFollowing != null
        when {
            loadingMore -> isLoadingMoreRelationships = true
            hasWarmLists -> isRelationshipsRefreshing = true
            else -> isRelationshipsInitialLoading = true
        }
        relationshipsError = null

        try {
            coroutineScope {
                val blockedDeferred = async {
                    SocialSessionCache.manager.getBlockedIds(targetUid, forceRefresh = forceRefresh)
                }
                val followersDeferred = async {
                    SocialSessionCache.manager.getFollowersList(
                        ownerUid = targetUid,
                        limit = followersLimit,
                        forceRefresh = forceRefresh
                    )
                }
                val followingDeferred = async {
                    SocialSessionCache.manager.getFollowingList(
                        ownerUid = targetUid,
                        limit = followingLimit,
                        forceRefresh = forceRefresh
                    )
                }
                val countsDeferred = async {
                    SocialSessionCache.manager.getFollowCounts(
                        ownerUid = targetUid,
                        targetUid = targetUid,
                        forceRefresh = forceRefresh
                    )
                }

                val blockedRes = blockedDeferred.await()
                if (blockedRes is ProfileOperationResult.Success) {
                    blockedUids = blockedRes.data
                }

                val followersRes = followersDeferred.await()
                val followingRes = followingDeferred.await()
                countsDeferred.await()

                var firstError: String? = null
                when (followersRes) {
                    is ProfileOperationResult.Success -> followersList = followersRes.data
                    is ProfileOperationResult.Error -> firstError = followersRes.message
                }
                when (followingRes) {
                    is ProfileOperationResult.Success -> {
                        followingList = followingRes.data
                        followedUids = followingRes.data.map { it.uid }.toSet()
                    }
                    is ProfileOperationResult.Error -> {
                        if (firstError == null) firstError = followingRes.message
                    }
                }

                if (firstError != null && followersList.isEmpty() && followingList.isEmpty()) {
                    relationshipsError = firstError
                }
            }
        } finally {
            isRelationshipsInitialLoading = false
            isRelationshipsRefreshing = false
            isLoadingMoreRelationships = false
        }
    }

    LaunchedEffect(currentUserId) {
        loadRelationships(currentUserId, forceRefresh = false)
    }

    DisposableEffect(lifecycleOwner, currentUserId) {
        var handledInitialResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!handledInitialResume) {
                    handledInitialResume = true
                } else if (!currentUserId.isNullOrBlank()) {
                    scope.launch {
                        loadRelationships(currentUserId, forceRefresh = false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Debounced, cancellable search using SocialSessionCache (immediate hit on cached queries)
    LaunchedEffect(searchQuery, selectedTabKey, currentUserId, searchRetryNonce) {
        if (selectedTabKey != "Discover") {
            isSearching = false
            return@LaunchedEffect
        }
        val normalized = searchQuery.trim().removePrefix("@").trim()
        if (normalized.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            searchError = null
            return@LaunchedEffect
        }

        val warmCached = SocialSessionCache.manager
            .currentSnapshotFor(currentUserId)
            ?.searches
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

    val followersCountDisplay: Int? = cachedCounts?.followerCount
    val followingCountDisplay: Int? = cachedCounts?.followingCount

    val followersTabTitle = if (followersCountDisplay != null) {
        "Followers ($followersCountDisplay)"
    } else if (currentUserId.isNullOrBlank()) {
        "Followers (0)"
    } else {
        "Followers"
    }

    val followingTabTitle = if (followingCountDisplay != null) {
        "Following ($followingCountDisplay)"
    } else if (currentUserId.isNullOrBlank()) {
        "Following (0)"
    } else {
        "Following"
    }

    val tabEntries = listOf(
        "Discover" to "Discover",
        "Followers" to followersTabTitle,
        "Following" to followingTabTitle
    )

    val cleanFilterQuery = remember(searchQuery) { searchQuery.trim().removePrefix("@").trim() }

    val displayedUsers = remember(
        selectedTabKey,
        searchResults,
        followersList,
        followingList,
        cleanFilterQuery,
        blockedUids
    ) {
        val rawList = when (selectedTabKey) {
            "Followers" -> if (cleanFilterQuery.isBlank()) followersList else followersList.filter {
                it.displayName.contains(cleanFilterQuery, ignoreCase = true) ||
                    it.username.contains(cleanFilterQuery, ignoreCase = true)
            }
            "Following" -> if (cleanFilterQuery.isBlank()) followingList else followingList.filter {
                it.displayName.contains(cleanFilterQuery, ignoreCase = true) ||
                    it.username.contains(cleanFilterQuery, ignoreCase = true)
            }
            else -> searchResults
        }
        rawList.filterNot { blockedUids.contains(it.uid) }
    }

    val canLoadMoreFollowers = selectedTabKey == "Followers" &&
        cleanFilterQuery.isBlank() &&
        followersCountDisplay != null &&
        followersList.size >= followersLimit &&
        followersCountDisplay > followersList.size

    val canLoadMoreFollowing = selectedTabKey == "Following" &&
        cleanFilterQuery.isBlank() &&
        followingCountDisplay != null &&
        followingList.size >= followingLimit &&
        followingCountDisplay > followingList.size

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

            // 2. Compact Segmented Tabs (Discover / Followers / Following)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                tabEntries.forEach { (key, label) ->
                    val selected = selectedTabKey == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) LivePrimaryEmerald else Color.Transparent)
                            .clickable { selectedTabKey = key }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }

            // 3. Compact Rounded Search Bar
            val searchPlaceholder = when (selectedTabKey) {
                "Followers" -> "Filter followers…"
                "Following" -> "Filter following…"
                else -> "Search name or @username"
            }
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
                            text = searchPlaceholder,
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
                                if (selectedTabKey == "Discover") {
                                    persistRecent(searchQuery)
                                }
                                keyboardController?.hide()
                            }
                        )
                    )
                }

                when {
                    selectedTabKey == "Discover" && isSearching -> {
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
            WorkDailySubtleRefreshBar(
                visible = isRelationshipsRefreshing || (selectedTabKey != "Discover" && isLoadingMoreRelationships)
            )
        }

        // 4. Main Content Area (Initial Skeleton / Recent Searches / Empty / Retry / User Rows)
        val isActiveRelationshipTabInitialLoad =
            selectedTabKey != "Discover" && isRelationshipsInitialLoading && displayedUsers.isEmpty()

        val isDiscoverColdSearching =
            selectedTabKey == "Discover" && cleanFilterQuery.isNotEmpty() && isSearching && searchResults.isEmpty()

        val activeError: String? = when (selectedTabKey) {
            "Discover" -> if (cleanFilterQuery.isNotEmpty() && searchResults.isEmpty()) searchError else null
            else -> if (displayedUsers.isEmpty()) relationshipsError else null
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
        ) {
            when {
                // Discover idle state: Recent Searches (scrollable) or clean prompt
                selectedTabKey == "Discover" && cleanFilterQuery.isEmpty() -> {
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
                isDiscoverColdSearching || isActiveRelationshipTabInitialLoad -> {
                    UserRowSkeletonList(
                        rowCount = 6,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Clean retry state on failure
                activeError != null -> {
                    WorkDailyErrorRetryState(
                        title = "Couldn't load people",
                        message = activeError,
                        onRetry = {
                            if (selectedTabKey == "Discover") {
                                searchRetryNonce += 1
                            } else {
                                scope.launch {
                                    loadRelationships(currentUserId, forceRefresh = true)
                                }
                            }
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
                        val emptyTitle = when {
                            selectedTabKey == "Discover" -> "No matching accounts"
                            cleanFilterQuery.isNotEmpty() -> "No matches in $selectedTabKey"
                            selectedTabKey == "Followers" -> "No followers yet"
                            else -> "Not following anyone yet"
                        }
                        val emptySubtitle = when {
                            selectedTabKey == "Discover" -> "Check the spelling or search by @username."
                            cleanFilterQuery.isNotEmpty() -> "Try clearing the filter above."
                            selectedTabKey == "Followers" -> "Members who follow you will appear here."
                            else -> "Use Discover to find and follow people on WorkDaily."
                        }
                        Text(
                            text = emptyTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = emptySubtitle,
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
                            val activeUid = currentUserId
                            val isSelfRow = !activeUid.isNullOrBlank() && activeUid == user.uid
                            val isFollowingUser = followedUids.contains(user.uid)
                            val followsMe = followerUids.contains(user.uid)
                            val isMutual = isFollowingUser && followsMe
                            val isRowBusy = !activeUid.isNullOrBlank() &&
                                socialSnapshot.ownerUid == activeUid &&
                                socialSnapshot.inFlightFollowMutations.contains(user.uid)

                            val displayName = user.displayName.ifBlank { "WorkDaily Member" }
                            val handle = if (user.username.isNotBlank()) "@${user.username}" else "@member"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (selectedTabKey == "Discover") {
                                            persistRecent(user.username.ifBlank { user.displayName })
                                        }
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
                                        if (selectedTabKey == "Discover") {
                                            persistRecent(user.username.ifBlank { user.displayName })
                                        }
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

                                        val relationHint = when {
                                            isSelfRow -> null
                                            isMutual -> "• Friends"
                                            followsMe -> "• Follows you"
                                            user.profession.isNotBlank() -> "• ${user.profession}"
                                            else -> null
                                        }
                                        if (relationHint != null) {
                                            Text(
                                                text = relationHint,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                            )
                                        }
                                    }
                                }

                                if (!activeUid.isNullOrBlank() && !isSelfRow) {
                                    Spacer(modifier = Modifier.width(8.dp))

                                    FollowButton(
                                        isFollowing = isFollowingUser,
                                        isLoading = isRowBusy,
                                        height = 34.dp,
                                        minWidth = 86.dp,
                                        onToggle = {
                                            if (isRowBusy) return@FollowButton
                                            val wasFollowing = isFollowingUser
                                            val wantFollow = !wasFollowing

                                            // Row-scoped optimistic update (synced via SocialSessionCache)
                                            followedUids = if (wantFollow) {
                                                followedUids + user.uid
                                            } else {
                                                followedUids - user.uid
                                            }

                                            scope.launch {
                                                when (
                                                    val result = SocialSessionCache.manager.setFollowState(
                                                        ownerUid = activeUid,
                                                        targetProfile = user,
                                                        shouldFollow = wantFollow
                                                    )
                                                ) {
                                                    is ProfileOperationResult.Success -> Unit
                                                    is ProfileOperationResult.Error -> {
                                                        followedUids = if (wasFollowing) {
                                                            followedUids + user.uid
                                                        } else {
                                                            followedUids - user.uid
                                                        }
                                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            if (selectedTabKey == "Discover") {
                                                persistRecent(user.username.ifBlank { user.displayName })
                                            }
                                            onNavigatePrivateChat(user.uid)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Message",
                                            tint = if (isMutual) {
                                                LivePrimaryEmerald
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }

                        if (canLoadMoreFollowers || canLoadMoreFollowing) {
                            item(key = "load_more_people") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (!isLoadingMoreRelationships) {
                                                if (selectedTabKey == "Followers") {
                                                    followersLimit += PEOPLE_PAGE_STEP
                                                } else if (selectedTabKey == "Following") {
                                                    followingLimit += PEOPLE_PAGE_STEP
                                                }
                                                scope.launch {
                                                    loadRelationships(
                                                        targetUid = currentUserId,
                                                        forceRefresh = true,
                                                        loadingMore = true
                                                    )
                                                }
                                            }
                                        },
                                        enabled = !isLoadingMoreRelationships,
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (isLoadingMoreRelationships) "Loading…" else "Load more",
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
