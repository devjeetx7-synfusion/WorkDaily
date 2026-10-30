package com.synfusion.workdailyadmin.ui.social

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social & Profile Moderation", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadSocialProfiles() }) {
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
                    viewModel.loadSocialProfiles()
                },
                placeholderText = "Search profiles by @username, name, or UID..."
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadSocialProfiles() }) { Text("Retry") }
                        }
                    }
                }
                is SocialUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.users) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
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
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedUser = user
                                            showVerifyDialog = true
                                        }
                                    ) {
                                        Text(if (user.isVerified) "Unverify" else "Verify")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showVerifyDialog && selectedUser != null) {
                val u = selectedUser!!
                AlertDialog(
                    onDismissRequest = { showVerifyDialog = false },
                    title = { Text(if (u.isVerified) "Revoke Verification" else "Grant Verification") },
                    text = {
                        Column {
                            Text("Provide mandatory verification update reason:")
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
                                if (reasonInput.isNotBlank()) {
                                    showVerifyDialog = false
                                    viewModel.toggleVerification(u.uid, u.isVerified, reasonInput)
                                    reasonInput = ""
                                }
                            },
                            enabled = reasonInput.isNotBlank()
                        ) {
                            Text("Confirm")
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
