package com.synfusion.workdailyadmin.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.data.models.UserSummary
import com.synfusion.workdailyadmin.ui.components.AdminSearchBar
import com.synfusion.workdailyadmin.ui.components.StatusChip

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
                title = { Text("User Management", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            AdminSearchBar(
                query = searchQuery,
                onQueryChange = {
                    viewModel.searchQuery.value = it
                    viewModel.loadUsers()
                },
                placeholderText = "Search UID, name, @username, or email..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = {
                        viewModel.statusFilter.value = null
                        viewModel.loadUsers()
                    },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = statusFilter == "ACTIVE",
                    onClick = {
                        viewModel.statusFilter.value = "ACTIVE"
                        viewModel.loadUsers()
                    },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = statusFilter == "SUSPENDED",
                    onClick = {
                        viewModel.statusFilter.value = "SUSPENDED"
                        viewModel.loadUsers()
                    },
                    label = { Text("Suspended") }
                )
                FilterChip(
                    selected = statusFilter == "BANNED",
                    onClick = {
                        viewModel.statusFilter.value = "BANNED"
                        viewModel.loadUsers()
                    },
                    label = { Text("Banned") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = listState) {
                is UserListState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UserListState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadUsers() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is UserListState.Success -> {
                    if (state.users.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No users found matching query.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.users) { user ->
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "UID: ${user.uid}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

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
