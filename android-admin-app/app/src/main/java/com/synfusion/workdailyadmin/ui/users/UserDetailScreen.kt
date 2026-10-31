package com.synfusion.workdailyadmin.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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
    var isAddingNote by remember { mutableStateOf(false) }

    LaunchedEffect(targetUid) {
        viewModel.loadUserDetails(targetUid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("User Details & Governance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(targetUid, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadUserDetails(targetUid) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh User")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = detailState) {
                is UserDetailState.Idle -> {
                    AdminLoadingState(message = "Initializing...")
                }
                is UserDetailState.Loading -> {
                    AdminLoadingState(message = "Retrieving complete profile and moderation records...")
                }
                is UserDetailState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadUserDetails(targetUid) }
                    )
                }
                is UserDetailState.Success -> {
                    val u = state.details
                    val createdDateStr = remember(u.createdAt) {
                        if (u.createdAt > 0) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(u.createdAt)) else "Unknown"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // User Profile Hero Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(
                                        photoUrl = u.photoUrl,
                                        name = u.displayName,
                                        size = 64
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = u.displayName.ifBlank { "WorkDaily Member" },
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                            if (u.isVerified) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified Account",
                                                    tint = Color(0xFF3B82F6),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (u.username.isNotBlank()) {
                                            Text(
                                                text = "@${u.username}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (u.profession.isNotBlank()) {
                                            Text(
                                                text = u.profession,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    val statusColor = when (u.accountStatus.uppercase()) {
                                        "ACTIVE" -> Color(0xFF10B981)
                                        "SUSPENDED" -> Color(0xFFF59E0B)
                                        "BANNED" -> Color(0xFFEF4444)
                                        else -> Color(0xFF6B7280)
                                    }
                                    StatusChip(text = u.accountStatus, color = statusColor)
                                }

                                if (u.bio.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = u.bio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TruncatedIdChip(id = u.uid, prefix = "UID: ", maxLength = 16)
                                    Text(
                                        text = "Joined: $createdDateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Account Metadata Grid
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Identity & Communication",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                MetaInfoRow(label = "Email Address", value = u.email.ifBlank { "Not configured" })
                                MetaInfoRow(label = "Phone Number", value = u.phoneNumber.ifBlank { "Not configured" })
                                MetaInfoRow(label = "Country & Currency", value = "${u.countryCode} • ${u.currencyCode}")
                                MetaInfoRow(label = "Gender", value = u.gender)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Social & Rewards Summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Platform Activity & Economy",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    MetricPill(
                                        label = "Followers",
                                        value = u.followerCount.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    MetricPill(
                                        label = "Following",
                                        value = u.followingCount.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    MetricPill(
                                        label = "Available Coins",
                                        value = u.availableCoins.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    MetricPill(
                                        label = "Current Streak",
                                        value = "${u.currentStreak} Days",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (u.referralCode.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    MetaInfoRow(label = "Referral Code", value = u.referralCode)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Moderation Action Controls
                        Text(
                            text = "Administrative Moderation",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

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
                                    shape = RoundedCornerShape(10.dp),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ban Account")
                                }
                            }
                            if (u.accountStatus != "ACTIVE") {
                                Button(
                                    onClick = {
                                        actionType = "RESTORE"
                                        showActionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Restore Active")
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Revoke Sessions")
                            }
                            OutlinedButton(
                                onClick = {
                                    actionType = "TOGGLE_VERIFY"
                                    showActionDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (u.isVerified) "Remove Verified" else "Verify Account")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Internal Admin Notes Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Internal Admin Notes",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    StatusChip(text = "${u.notes.size} Notes", color = MaterialTheme.colorScheme.primary)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = noteInput,
                                    onValueChange = { noteInput = it },
                                    placeholder = { Text("Add administrative note...") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (noteInput.isNotBlank()) {
                                            isAddingNote = true
                                            viewModel.addUserNote(u.uid, noteInput) { success, _ ->
                                                isAddingNote = false
                                                if (success) noteInput = ""
                                            }
                                        }
                                    },
                                    enabled = noteInput.isNotBlank() && !isAddingNote,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    if (isAddingNote) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                    } else {
                                        Text("Save Note")
                                    }
                                }

                                if (u.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        u.notes.forEach { note ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = note.text,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "By ${note.adminEmail}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            if (showActionDialog) {
                AlertDialog(
                    onDismissRequest = { showActionDialog = false },
                    title = { Text("Execute: $actionType", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Please enter an administrative audit reason to execute this action:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = reasonInput,
                                onValueChange = { reasonInput = it },
                                placeholder = { Text("Reason for audit trail...") },
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
                            enabled = reasonInput.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Confirm & Execute")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showActionDialog = false },
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
private fun MetaInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
