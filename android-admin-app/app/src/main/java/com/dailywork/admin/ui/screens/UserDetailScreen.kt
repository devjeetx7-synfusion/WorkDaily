package com.dailywork.admin.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dailywork.admin.data.model.User
import com.dailywork.admin.ui.utils.formatTimestamp
import com.dailywork.admin.viewmodel.UsersViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: String,
    onBack: () -> Unit,
    onNotifyUser: (String) -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val user by viewModel.getUser(userId).collectAsState(initial = null)
    val isPerformingAction by viewModel.isPerformingAction.collectAsState()
    var showBlockDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        user?.let { u ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                UserHeaderDetail(u)

                AdminActionsGrid(
                    user = u,
                    isPerformingAction = isPerformingAction,
                    onNotify = { onNotifyUser(u.uid) },
                    onBlockClick = { showBlockDialog = true },
                    onPremiumClick = { showPremiumDialog = true },
                    onRoleClick = { showRoleDialog = true },
                    onDeleteClick = { showDeleteDialog = true }
                )

                DetailedInfoCard(u)
            }

            if (showBlockDialog) {
                BlockUserDialog(
                    isBlocked = u.isBlocked,
                    onDismiss = { showBlockDialog = false },
                    onConfirm = { reason ->
                        viewModel.toggleBlockStatus(u.uid, !u.isBlocked, reason)
                        showBlockDialog = false
                    }
                )
            }

            if (showPremiumDialog) {
                PremiumDialog(
                    isPremium = u.isPremium,
                    onDismiss = { showPremiumDialog = false },
                    onConfirm = { days ->
                        viewModel.togglePremiumStatus(u.uid, !u.isPremium, days)
                        showPremiumDialog = false
                    }
                )
            }

            if (showRoleDialog) {
                RoleDialog(
                    currentRole = u.role,
                    onDismiss = { showRoleDialog = false },
                    onConfirm = { role ->
                        viewModel.changeRole(u.uid, role)
                        showRoleDialog = false
                    }
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete User") },
                    text = { Text("Are you sure you want to permanently delete this user? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteUser(u.uid)
                                showDeleteDialog = false
                                onBack()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun UserHeaderDetail(user: User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user.photoUrl.isNotEmpty()) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.name.ifEmpty { "Unknown User" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(user.role.uppercase(), modifier = Modifier.padding(4.dp))
            }
            if (user.isPremium) {
                Badge(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary) {
                    Text("PREMIUM", modifier = Modifier.padding(4.dp))
                }
            }
            if (user.isBlocked) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("BLOCKED", modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Composable
fun AdminActionsGrid(
    user: User,
    isPerformingAction: Boolean,
    onNotify: () -> Unit,
    onBlockClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onRoleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Administrative Actions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    icon = Icons.Default.Notifications,
                    label = "Notify",
                    onClick = onNotify,
                    enabled = !isPerformingAction,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    label = if (user.isBlocked) "Unblock" else "Block",
                    onClick = onBlockClick,
                    enabled = !isPerformingAction,
                    color = if (user.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    icon = if (user.isPremium) Icons.Default.StarOutline else Icons.Default.Star,
                    label = if (user.isPremium) "Set Free" else "Set Premium",
                    onClick = onPremiumClick,
                    enabled = !isPerformingAction,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.AdminPanelSettings,
                    label = "Change Role",
                    onClick = onRoleClick,
                    enabled = !isPerformingAction,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPerformingAction,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete User")
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = color.copy(alpha = 0.1f), contentColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailedInfoCard(user: User) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Account Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            DetailRow(Icons.Default.Badge, "User UID", user.uid)
            DetailRow(Icons.Default.Schedule, "Joined Date", formatTimestamp(user.createdAt))
            DetailRow(Icons.Default.Visibility, "Last Activity", formatTimestamp(user.lastActive))

            if (user.isBlocked && user.blockInfo != null) {
                HorizontalDivider()
                Text("Block Information", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                DetailRow(Icons.Default.Info, "Reason", user.blockInfo.reason)
                DetailRow(Icons.Default.History, "Blocked At", formatTimestamp(user.blockInfo.blockedAt))
            }

            if (user.isPremium && user.premiumInfo != null) {
                HorizontalDivider()
                Text("Premium Information", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                DetailRow(Icons.Default.Event, "Expiry Date", if (user.premiumInfo.expiryDate > 0) formatTimestamp(user.premiumInfo.expiryDate) else "Lifetime")
                DetailRow(Icons.Default.History, "Activated At", formatTimestamp(user.premiumInfo.activatedAt))
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BlockUserDialog(isBlocked: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBlocked) "Unblock User" else "Block User") },
        text = {
            if (!isBlocked) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for blocking") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Are you sure you want to unblock this user?")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason) }) {
                Text(if (isBlocked) "Unblock" else "Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PremiumDialog(isPremium: Boolean, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var days by remember { mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPremium) "Disable Premium" else "Enable Premium") },
        text = {
            if (!isPremium) {
                Column {
                    Text("Select duration (days):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = days,
                        onValueChange = { days = it.filter { char -> char.isDigit() } },
                        label = { Text("Days (0 for Lifetime)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text("Are you sure you want to disable premium for this user?")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(days.toIntOrNull() ?: 0) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RoleDialog(currentRole: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val roles = listOf("admin", "contractor", "personal")
    var selectedRole by remember { mutableStateOf(currentRole) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change User Role") },
        text = {
            Column {
                roles.forEach { role ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { selectedRole = role }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedRole == role, onClick = { selectedRole = role })
                        Text(role.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedRole) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
