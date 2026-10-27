package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.FirestorePublicProfileRepository
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleAndFriendsScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateProfile: (String) -> Unit,
    onNavigatePrivateChat: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val preferences = remember { context.getSharedPreferences("workdaily_people_search", android.content.Context.MODE_PRIVATE) }
    val currentUserId = remember {
        try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
    }

    var selectedTabKey by remember { mutableStateOf("Discover") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var followersList by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var followingList by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var followedUids by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var recentQueries by remember {
        mutableStateOf(preferences.getString("recent_queries", "").orEmpty().split("\n").filter { it.isNotBlank() }.take(8))
    }
    var showAllRecent by remember { mutableStateOf(false) }
    var busyFollowUids by remember { mutableStateOf<Set<String>>(emptySet()) }
    val publicProfileRepo = remember { FirestorePublicProfileRepository() }
    val followRepo = remember { com.synfusion.attendance.online.FirestoreFollowRepository() }

    fun persistRecent(query: String) {
        val clean = query.trim().removePrefix("@").trim()
        if (clean.isBlank()) return
        recentQueries = (listOf(clean) + recentQueries.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
        preferences.edit().putString("recent_queries", recentQueries.joinToString("\n")).apply()
    }
    fun removeRecent(query: String) {
        recentQueries = recentQueries.filterNot { it == query }
        preferences.edit().putString("recent_queries", recentQueries.joinToString("\n")).apply()
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val followersRes = followRepo.getFollowers(currentUserId)
            if (followersRes is ProfileOperationResult.Success) followersList = followersRes.data
            val followingRes = followRepo.getFollowing(currentUserId)
            if (followingRes is ProfileOperationResult.Success) {
                followingList = followingRes.data
                followedUids = followingRes.data.map { it.uid }.toSet()
            }
        }
    }
    LaunchedEffect(searchQuery, selectedTabKey) {
        if (selectedTabKey != "Discover") return@LaunchedEffect
        val query = searchQuery.trim().removePrefix("@").trim()
        if (query.isBlank()) { searchResults = emptyList(); isSearching = false; searchError = null; return@LaunchedEffect }
        isSearching = true
        searchError = null
        delay(350)
        when (val result = publicProfileRepo.searchPublicProfiles(query, currentUserId = currentUserId)) {
            is ProfileOperationResult.Success -> { searchResults = result.data; isSearching = false }
            is ProfileOperationResult.Error -> { searchResults = emptyList(); searchError = result.message; isSearching = false }
        }
    }

    val tabs = listOf("Discover", "Followers (${followersList.size})", "Following (${followingList.size})")
    val displayedUsers = remember(selectedTabKey, searchResults, followersList, followingList, searchQuery) {
        when (selectedTabKey) {
            "Followers" -> if (searchQuery.isBlank()) followersList else followersList.filter { it.displayName.contains(searchQuery, true) || it.username.contains(searchQuery.removePrefix("@"), true) }
            "Following" -> if (searchQuery.isBlank()) followingList else followingList.filter { it.displayName.contains(searchQuery, true) || it.username.contains(searchQuery.removePrefix("@"), true) }
            else -> searchResults
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("People", fontWeight = FontWeight.Bold, fontSize = 22.sp) }, navigationIcon = {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)).padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Discover", "Followers", "Following").forEachIndexed { index, key ->
                    val selected = selectedTabKey == key
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (selected) LivePrimaryEmerald else Color.Transparent).clickable { selectedTabKey = key }.padding(horizontal = 2.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text(tabs[index], maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search people", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = { if (isSearching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear search") } },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { persistRecent(searchQuery); keyboardController?.hide() }))

            if (selectedTabKey == "Discover" && searchQuery.isBlank() && recentQueries.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent searches", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showAllRecent = !showAllRecent }) { Text(if (showAllRecent) "Show less" else "See all", color = LivePrimaryEmerald, fontSize = 12.sp) }
                        TextButton(onClick = { recentQueries = emptyList(); preferences.edit().remove("recent_queries").apply() }) { Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                    }
                }
                (if (showAllRecent) recentQueries else recentQueries.take(4)).forEach { recent ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { searchQuery = recent }.padding(vertical = 4.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(recent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Search again", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                        IconButton(onClick = { removeRecent(recent) }) { Icon(Icons.Default.Close, contentDescription = "Remove recent search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            if (selectedTabKey == "Discover" && searchQuery.isBlank()) {
                if (recentQueries.isEmpty()) Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(10.dp)); Text("Find your people", fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text("Search by name or @username", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else Spacer(Modifier.weight(1f))
            } else if (isSearching) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LivePrimaryEmerald) }
            else if (searchError != null) Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(8.dp)); Text("Couldn't load people", fontWeight = FontWeight.SemiBold); Text(searchError.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); TextButton(onClick = { searchQuery = searchQuery }) { Text("Try again", color = LivePrimaryEmerald) }
            } else if (displayedUsers.isEmpty()) Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(10.dp));
                val title = when { selectedTabKey == "Discover" -> "No people found"; selectedTabKey == "Followers" -> "No followers yet"; else -> "Not following anyone yet" }
                val subtitle = if (selectedTabKey == "Discover") "Try another name or username." else "People will appear here when this list has entries."
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface); Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(displayedUsers, key = { it.uid }) { user ->
                    val isFollowingUser = followedUids.contains(user.uid)
                    val scope = rememberCoroutineScope()
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { persistRecent(searchQuery); onNavigateProfile(user.uid) }.padding(vertical = 10.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        WorkDailyLiveAvatar(name = user.displayName, avatarUrl = user.photoUrl ?: "", size = 48.dp, isLive = false, showBadge = false, onClick = { persistRecent(searchQuery); onNavigateProfile(user.uid) })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text(user.displayName.ifEmpty { "WorkDaily Member" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis); if (user.isVerified) Icon(Icons.Default.Verified, contentDescription = "Verified", tint = LivePrimaryEmerald, modifier = Modifier.size(14.dp)) }
                            Text("@${user.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (user.profession.isNotBlank()) Text(user.profession, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        if (currentUserId != null && currentUserId != user.uid) {
                            val busy = user.uid in busyFollowUids
                            FollowButton(isFollowing = isFollowingUser, onToggle = {
                                if (busy) return@FollowButton
                                val wasFollowing = isFollowingUser
                                busyFollowUids = busyFollowUids + user.uid
                                scope.launch {
                                    val result = if (wasFollowing) followRepo.unfollowUser(currentUserId, user.uid) else followRepo.followUser(currentUserId, user.uid)
                                    busyFollowUids = busyFollowUids - user.uid
                                    if (result is ProfileOperationResult.Success) {
                                        followedUids = if (wasFollowing) followedUids - user.uid else followedUids + user.uid
                                        followingList = if (wasFollowing) followingList.filterNot { it.uid == user.uid } else (followingList + user).distinctBy { it.uid }
                                    } else if (result is ProfileOperationResult.Error) Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                        IconButton(onClick = { persistRecent(searchQuery); onNavigatePrivateChat(user.uid) }, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = LivePrimaryEmerald, modifier = Modifier.size(19.dp)) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}