package com.dailywork.admin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailywork.admin.data.model.User
import com.dailywork.admin.ui.utils.formatTimestamp
import com.dailywork.admin.viewmodel.SortOrder
import com.dailywork.admin.viewmodel.UsersViewModel
import java.util.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun UsersScreen(
    onUserClick: (String) -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val users by viewModel.filteredUsers.collectAsState()
    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(refreshing, {
        viewModel.refresh()
        refreshing = false
    })
    val searchQuery by viewModel.searchQuery.collectAsState()
    val roleFilter by viewModel.roleFilter.collectAsState()
    val premiumFilter by viewModel.premiumFilter.collectAsState()
    val blockedFilter by viewModel.blockedFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("User Management", fontWeight = FontWeight.Bold) },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Recent Activity") },
                            onClick = { viewModel.setSortOrder(SortOrder.RECENT_ACTIVITY); showSortMenu = false },
                            leadingIcon = { if (sortOrder == SortOrder.RECENT_ACTIVITY) Icon(Icons.Default.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Joined Date") },
                            onClick = { viewModel.setSortOrder(SortOrder.JOINED_DATE); showSortMenu = false },
                            leadingIcon = { if (sortOrder == SortOrder.JOINED_DATE) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name or email...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Clear, null) } }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var showRoleMenu by remember { mutableStateOf(false) }
                FilterChip(
                    selected = roleFilter != null,
                    onClick = { showRoleMenu = true },
                    label = { Text(roleFilter ?: "All Roles") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                )
                DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
                    DropdownMenuItem(text = { Text("All Roles") }, onClick = { viewModel.setRoleFilter(null); showRoleMenu = false })
                    DropdownMenuItem(text = { Text("Admin") }, onClick = { viewModel.setRoleFilter("admin"); showRoleMenu = false })
                    DropdownMenuItem(text = { Text("Contractor") }, onClick = { viewModel.setRoleFilter("contractor"); showRoleMenu = false })
                    DropdownMenuItem(text = { Text("Personal") }, onClick = { viewModel.setRoleFilter("personal"); showRoleMenu = false })
                }

                FilterChip(
                    selected = premiumFilter == true,
                    onClick = { viewModel.setPremiumFilter(if (premiumFilter == true) null else true) },
                    label = { Text("Premium") }
                )
                FilterChip(
                    selected = blockedFilter == true,
                    onClick = { viewModel.setBlockedFilter(if (blockedFilter == true) null else true) },
                    label = { Text("Blocked") }
                )
            }

            if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No users found" else "No results for \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onClick = { onUserClick(user.uid) }
                        )
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { viewModel.loadMore() }) {
                                Text("Load More")
                            }
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    val isOnline = System.currentTimeMillis() - user.lastActive < 5 * 60 * 1000

    val cardColor = if (user.isPremium) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorder = if (user.isPremium) {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
    } else null

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                if (user.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                user.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Online Indicator
                if (isOnline) {
                    Surface(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp),
                        shape = CircleShape,
                        color = Color.Green,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifEmpty { "Unknown User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Badge(
                        containerColor = when(user.role) {
                            "admin" -> MaterialTheme.colorScheme.errorContainer
                            "contractor" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(user.role.uppercase(), modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    if (user.isPremium) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary) {
                            val expiry = user.premiumInfo?.expiryDate
                            val text = if (expiry != null && expiry > 0) {
                                "PREMIUM UNTIL ${formatTimestamp(expiry)}"
                            } else "PREMIUM LIFETIME"
                            Text(text, modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp)
                        }
                    }
                    if (user.isBlocked) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("BLOCKED", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
