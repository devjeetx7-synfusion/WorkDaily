package com.synfusion.workdailyadmin.ui.team

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.AdminRole
import com.synfusion.workdailyadmin.data.models.AdminTeamMember
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeamScreen(
    viewModel: TeamViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAssignDialog by remember { mutableStateOf(false) }
    var uidInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("MODERATOR") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Team & RBAC Provisioning", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Role assignments, administrative authorization & access tokens", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadTeam() },
                        enabled = uiState !is TeamUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Team")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAssignDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Provision Admin")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (val state = uiState) {
                is TeamUiState.Loading -> {
                    AdminLoadingState(message = "Querying authorized administrative accounts...")
                }
                is TeamUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadTeam() }
                    )
                }
                is TeamUiState.Success -> {
                    if (state.team.isEmpty()) {
                        AdminEmptyState(
                            title = "No Staff Members Provisioned",
                            description = "Add administrators, moderators, or support agents using the + button.",
                            icon = Icons.Default.SupervisorAccount,
                            actionText = "Provision Member",
                            onActionClick = { showAssignDialog = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.team, key = { it.uid }) { member ->
                                AdminTeamCard(
                                    member = member,
                                    onEditRole = {
                                        uidInput = member.uid
                                        emailInput = member.email
                                        selectedRole = member.role.name
                                        showAssignDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showAssignDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showAssignDialog = false },
                    title = { Text("Assign Administrative Role", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Grant role-based administrative claims on Firebase Authentication:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uidInput,
                                onValueChange = { uidInput = it },
                                label = { Text("Firebase User UID") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Official Email") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Select RBAC Role:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("ADMIN", "MODERATOR", "SUPPORT").forEach { r ->
                                    FilterChip(
                                        selected = selectedRole == r,
                                        onClick = { selectedRole = r },
                                        label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (uidInput.isNotBlank() && emailInput.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.assignRole(uidInput.trim(), emailInput.trim(), selectedRole) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showAssignDialog = false
                                            uidInput = ""
                                            emailInput = ""
                                            selectedRole = "MODERATOR"
                                        }
                                    }
                                }
                            },
                            enabled = uidInput.isNotBlank() && emailInput.isNotBlank() && !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Assign Role")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showAssignDialog = false },
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
fun AdminTeamCard(
    member: AdminTeamMember,
    onEditRole: () -> Unit
) {
    val assignedDateStr = remember(member.assignedAt) {
        if (member.assignedAt > 0) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(member.assignedAt))
        } else "Initial"
    }

    val roleColor = when (member.role) {
        AdminRole.SUPER_ADMIN -> Color(0xFFEF4444)
        AdminRole.ADMIN -> Color(0xFF10B981)
        AdminRole.MODERATOR -> Color(0xFF3B82F6)
        AdminRole.SUPPORT -> Color(0xFFF59E0B)
        AdminRole.ANALYST -> Color(0xFF8B5CF6)
    }

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
                photoUrl = null,
                name = member.email,
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
                        text = member.email,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                TruncatedIdChip(id = member.uid, prefix = "UID: ", maxLength = 14)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assigned: $assignedDateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                StatusChip(text = member.role.name, color = roleColor)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onEditRole,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Edit Role", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
