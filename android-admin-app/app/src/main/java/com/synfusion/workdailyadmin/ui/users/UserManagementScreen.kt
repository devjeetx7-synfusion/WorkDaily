package com.synfusion.workdailyadmin.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.UserSummary
import com.synfusion.workdailyadmin.ui.components.*

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
                title = {
                    Column {
                        Text("User Directory", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Identity management & account status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Users")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AdminSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                placeholderText = "Search UID, full name, or @username..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontally scrollable Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.onStatusFilterChanged(null) },
                    label = { Text("All Accounts") }
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

            Spacer(modifier = Modifier.height(14.dp))

            when (val state = listState) {
                is UserListState.Loading -> {
                    AdminLoadingState(message = "Retrieving user directory...")
                }
                is UserListState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadUsers() }
                    )
                }
                is UserListState.Success -> {
                    if (state.users.isEmpty()) {
                        AdminEmptyState(
                            title = "No Users Found",
                            description = if (searchQuery.isNotBlank()) "No users matching \"$searchQuery\"." else "No users currently registered under this filter.",
                            icon = Icons.Default.PersonSearch
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
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
                        text = user.displayName.ifBlank { "WorkDaily Member" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Account",
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
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TruncatedIdChip(id = user.uid, prefix = "UID: ", maxLength = 14)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val statusColor = when (user.accountStatus.uppercase()) {
                    "ACTIVE" -> Color(0xFF10B981)
                    "SUSPENDED" -> Color(0xFFF59E0B)
                    "BANNED" -> Color(0xFFEF4444)
                    else -> Color(0xFF6B7280)
                }

                StatusChip(text = user.accountStatus, color = statusColor)
                Spacer(modifier = Modifier.height(10.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
