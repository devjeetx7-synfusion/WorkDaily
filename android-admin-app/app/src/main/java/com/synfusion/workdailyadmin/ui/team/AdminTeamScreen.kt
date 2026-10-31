package com.synfusion.workdailyadmin.ui.team

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
import com.synfusion.workdailyadmin.ui.components.StatusChip
import com.synfusion.workdailyadmin.ui.components.TruncatedIdChip

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
                title = { Text("Admin Team & RBAC Provisioning", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadTeam() },
                        enabled = uiState !is TeamUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAssignDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Provision Admin")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (val state = uiState) {
                is TeamUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TeamUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadTeam() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is TeamUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.team, key = { it.uid }) { member ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.email,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TruncatedIdChip(id = member.uid, prefix = "UID: ", maxLength = 16)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusChip(text = member.role.name, color = Color(0xFF3B82F6))
                                }
                            }
                        }
                    }
                }
            }

            if (showAssignDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showAssignDialog = false },
                    title = { Text("Provision Administrator") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = uidInput,
                                onValueChange = { uidInput = it },
                                label = { Text("Target User UID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Admin Email") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Role Level:", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = selectedRole == "MODERATOR",
                                    onClick = { selectedRole = "MODERATOR" },
                                    label = { Text("Moderator") }
                                )
                                FilterChip(
                                    selected = selectedRole == "ADMIN",
                                    onClick = { selectedRole = "ADMIN" },
                                    label = { Text("Admin") }
                                )
                                FilterChip(
                                    selected = selectedRole == "SUPER_ADMIN",
                                    onClick = { selectedRole = "SUPER_ADMIN" },
                                    label = { Text("Super") }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (uidInput.isNotBlank() && emailInput.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    viewModel.assignRole(uidInput.trim(), emailInput.trim(), selectedRole) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showAssignDialog = false
                                            uidInput = ""
                                            emailInput = ""
                                        }
                                    }
                                }
                            },
                            enabled = uidInput.isNotBlank() && emailInput.isNotBlank() && !isSubmitting
                        ) {
                            Text("Assign Role")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showAssignDialog = false }, enabled = !isSubmitting) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
