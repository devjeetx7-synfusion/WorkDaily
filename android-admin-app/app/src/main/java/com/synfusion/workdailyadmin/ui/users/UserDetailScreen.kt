package com.synfusion.workdailyadmin.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.ui.components.ConfirmationDialog
import com.synfusion.workdailyadmin.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    targetUid: String,
    viewModel: UserViewModel,
    onBack: () -> Unit
) {
    val detailState by viewModel.userDetailState.collectAsState()

    var showActionDialog by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    LaunchedEffect(targetUid) {
        viewModel.loadUserDetails(targetUid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = detailState) {
                is UserDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UserDetailState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadUserDetails(targetUid) }) {
                            Text("Retry")
                        }
                    }
                }
                is UserDetailState.Success -> {
                    val u = state.details
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // User Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = u.displayName,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (u.isVerified) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = Color(0xFF3B82F6),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        if (u.username.isNotBlank()) {
                                            Text(
                                                text = "@${u.username}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    val statusColor = when (u.accountStatus) {
                                        "ACTIVE" -> Color(0xFF10B981)
                                        "SUSPENDED" -> Color(0xFFF59E0B)
                                        "BANNED" -> Color(0xFFEF4444)
                                        else -> Color.Gray
                                    }
                                    StatusChip(text = u.accountStatus, color = statusColor)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(text = "UID: ${u.uid}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Email: ${u.email.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Phone: ${u.phoneNumber.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Country: ${u.countryCode} (${u.currencyCode})", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Admin Action Controls
                        Text(
                            text = "Moderation Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (u.accountStatus != "SUSPENDED") {
                                Button(
                                    onClick = {
                                        actionType = "SUSPEND"
                                        showActionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Suspend")
                                }
                            }
                            if (u.accountStatus != "BANNED") {
                                Button(
                                    onClick = {
                                        actionType = "BAN"
                                        showActionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ban")
                                }
                            }
                            if (u.accountStatus != "ACTIVE") {
                                Button(
                                    onClick = {
                                        actionType = "RESTORE"
                                        showActionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Restore")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    actionType = "REVOKE_SESSIONS"
                                    showActionDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Revoke Sessions")
                            }
                            OutlinedButton(
                                onClick = {
                                    actionType = "TOGGLE_VERIFY"
                                    showActionDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (u.isVerified) "Remove Verify" else "Verify User")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Social & Rewards Summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Account Context & Activity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Followers: ${u.followerCount} | Following: ${u.followingCount}")
                                Text(text = "Available Coins: ${u.availableCoins} | Lifetime Earned: ${u.lifetimeEarned}")
                                Text(text = "Check-in Streak: ${u.currentStreak} Days")
                                if (u.referralCode.isNotBlank()) {
                                    Text(text = "Referral Code: ${u.referralCode}")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Internal Admin Notes Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Internal Admin Notes (${u.notes.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = noteInput,
                                    onValueChange = { noteInput = it },
                                    placeholder = { Text("Add administrative note...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (noteInput.isNotBlank()) {
                                            viewModel.addUserNote(u.uid, noteInput) { success, _ ->
                                                if (success) noteInput = ""
                                            }
                                        }
                                    },
                                    enabled = noteInput.isNotBlank()
                                ) {
                                    Text("Add Note")
                                }

                                if (u.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    u.notes.forEach { note ->
                                        Text(
                                            text = "• ${note.text} (${note.adminEmail})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }

            if (showActionDialog) {
                AlertDialog(
                    onDismissRequest = { showActionDialog = false },
                    title = { Text("Confirm Action: $actionType") },
                    text = {
                        Column {
                            Text("Please enter mandatory administrative reason:")
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
                                    showActionDialog = false
                                    when (actionType) {
                                        "SUSPEND" -> viewModel.updateUserStatus(targetUid, "SUSPENDED", null, null, reasonInput) { _, _ -> }
                                        "BAN" -> viewModel.updateUserStatus(targetUid, "BANNED", null, null, reasonInput) { _, _ -> }
                                        "RESTORE" -> viewModel.updateUserStatus(targetUid, "ACTIVE", null, null, reasonInput) { _, _ -> }
                                        "REVOKE_SESSIONS" -> viewModel.revokeSessions(targetUid, reasonInput) { _, _ -> }
                                        "TOGGLE_VERIFY" -> {
                                            val isVer = (detailState as? UserDetailState.Success)?.details?.isVerified ?: false
                                            viewModel.updateUserStatus(targetUid, null, !isVer, null, reasonInput) { _, _ -> }
                                        }
                                    }
                                    reasonInput = ""
                                }
                            },
                            enabled = reasonInput.isNotBlank()
                        ) {
                            Text("Execute")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showActionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
