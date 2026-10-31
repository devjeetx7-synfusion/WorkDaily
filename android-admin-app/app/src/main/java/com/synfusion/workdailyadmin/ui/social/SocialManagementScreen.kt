package com.synfusion.workdailyadmin.ui.social

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
import com.synfusion.workdailyadmin.data.models.UserSummary
import com.synfusion.workdailyadmin.ui.components.AdminSearchBar
import com.synfusion.workdailyadmin.ui.components.TruncatedIdChip
import com.synfusion.workdailyadmin.ui.components.UserAvatar

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
                title = { Text("Social & Profile Governance", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadSocialProfiles() },
                        enabled = uiState !is SocialUiState.Loading
                    ) {
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AdminSearchBar(
                query = searchQuery,
                onQueryChange = {
                    viewModel.searchQuery.value = it
                    viewModel.loadSocialProfiles()
                },
                placeholderText = "Search public profiles..."
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is SocialUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SocialUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadSocialProfiles() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is SocialUiState.Success -> {
                    if (state.users.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No profiles match criteria.", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.users, key = { it.uid }) { user ->
                                SocialProfileCard(
                                    user = user,
                                    onToggleVerify = {
                                        selectedUser = user
                                        showVerifyDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showVerifyDialog && selectedUser != null) {
                val isVer = selectedUser!!.isVerified
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showVerifyDialog = false },
                    title = { Text(if (isVer) "Remove Verification Badge" else "Grant Verification Badge") },
                    text = {
                        Column {
                            Text("Provide administrative justification:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = reasonInput,
                                onValueChange = { reasonInput = it },
                                placeholder = { Text("Reason...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (reasonInput.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    viewModel.toggleVerification(selectedUser!!.uid, isVer, reasonInput)
                                    isSubmitting = false
                                    showVerifyDialog = false
                                    reasonInput = ""
                                }
                            },
                            enabled = reasonInput.isNotBlank() && !isSubmitting
                        ) {
                            Text(if (isVer) "Remove Badge" else "Grant Badge")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showVerifyDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SocialProfileCard(
    user: UserSummary,
    onToggleVerify: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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

            OutlinedButton(
                onClick = onToggleVerify,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (user.isVerified) "Unverify" else "Verify",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
