package com.synfusion.workdailyadmin.ui.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.UserSummary
import com.synfusion.workdailyadmin.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialManagementScreen(
    viewModel: SocialViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showVerifyDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserSummary?>(null) }
    var reasonInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Social & Profile Governance", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Public profiles, creator badge verification & discovery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadSocialProfiles() },
                        enabled = uiState !is SocialUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Profiles")
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
                onQueryChange = {
                    viewModel.searchQuery.value = it
                    viewModel.loadSocialProfiles()
                },
                placeholderText = "Search creator by username, UID, or display name..."
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (val state = uiState) {
                is SocialUiState.Loading -> {
                    AdminLoadingState(message = "Retrieving creator public profiles...")
                }
                is SocialUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadSocialProfiles() }
                    )
                }
                is SocialUiState.Success -> {
                    if (state.users.isEmpty()) {
                        AdminEmptyState(
                            title = "No Creator Profiles Found",
                            description = if (searchQuery.isNotBlank()) "No social profiles matching \"$searchQuery\"." else "No public profiles currently registered.",
                            icon = Icons.Default.Public,
                            actionText = "Refresh",
                            onActionClick = { viewModel.loadSocialProfiles() }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.users, key = { it.uid }) { user ->
                                SocialProfileCard(
                                    user = user,
                                    onToggleVerification = {
                                        selectedUser = user
                                        reasonInput = ""
                                        showVerifyDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showVerifyDialog && selectedUser != null) {
                val target = selectedUser!!
                val willVerify = !target.isVerified

                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showVerifyDialog = false },
                    title = {
                        Text(
                            text = if (willVerify) "Grant Verification Badge" else "Revoke Verification Badge",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Please enter an administrative audit reason to ${if (willVerify) "grant verified status to" else "revoke verified status from"} ${target.displayName}:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = reasonInput,
                                onValueChange = { reasonInput = it },
                                placeholder = { Text("Audit reason...") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (reasonInput.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.toggleVerification(target.uid, target.isVerified, reasonInput)
                                    isSubmitting = false
                                    showVerifyDialog = false
                                }
                            },
                            enabled = reasonInput.isNotBlank() && !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text(if (willVerify) "Grant Badge" else "Revoke Badge")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showVerifyDialog = false },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun SocialProfileCard(
    user: UserSummary,
    onToggleVerification: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
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
                size = 50
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
                            contentDescription = "Verified Badge",
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

                Spacer(modifier = Modifier.height(4.dp))

                TruncatedIdChip(id = user.uid, prefix = "UID: ", maxLength = 14)
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onToggleVerification,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (user.isVerified) Icons.Default.VerifiedUser else Icons.Default.AddModerator,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (user.isVerified) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (user.isVerified) "Verified" else "Verify",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
