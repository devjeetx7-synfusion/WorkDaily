package com.synfusion.workdailyadmin.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.data.models.UserSummary
import com.synfusion.workdailyadmin.ui.components.AdminSearchBar
import com.synfusion.workdailyadmin.ui.components.StatusChip
import com.synfusion.workdailyadmin.ui.components.TruncatedIdChip
import com.synfusion.workdailyadmin.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: UserViewModel,
    onSelectUser: (String) -> Unit
) {
    val listState by viewModel.userListState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Directory", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Users")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AdminSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                placeholderText = "Search UID, name, or @username..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontally scrollable Filter Chips row ensuring clean fit on all screens
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.onStatusFilterChanged(null) },
                    label = { Text("All Users") }
                )
                FilterChip(
                    selected = statusFilter == "ACTIVE",
                    onClick = { viewModel.onStatusFilterChanged("ACTIVE") },
                    label = { Text("Active") },
                    leadingIcon = if (statusFilter == "ACTIVE") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = statusFilter == "SUSPENDED",
                    onClick = { viewModel.onStatusFilterChanged("SUSPENDED") },
                    label = { Text("Suspended") }
                )
                FilterChip(
                    selected = statusFilter == "BANNED",
                    onClick = { viewModel.onStatusFilterChanged("BANNED") },
                    label = { Text("Banned") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = listState) {
                is UserListState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading users...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is UserListState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadUsers() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is UserListState.Success -> {
                    if (state.users.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No users found matching query.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.users, key = { it.uid }) { user ->
                                UserListItemCard(user = user, onClick = { onSelectUser(user.uid) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItemCard(
    user: UserSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                photoUrl = user.photoUrl,
                name = user.displayName,
                size = 46
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = user.displayName.ifBlank { "Worker" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (user.username.isNotBlank()) {
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TruncatedIdChip(id = user.uid, prefix = "UID: ", maxLength = 14)
            }

            Spacer(modifier = Modifier.width(8.dp))

            val statusColor = when (user.accountStatus) {
                "ACTIVE" -> Color(0xFF10B981)
                "SUSPENDED" -> Color(0xFFF59E0B)
                "BANNED" -> Color(0xFFEF4444)
                else -> Color.Gray
            }

            StatusChip(text = user.accountStatus, color = statusColor)
        }
    }
}
