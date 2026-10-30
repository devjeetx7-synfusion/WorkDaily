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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Team & RBAC Provisioning", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadTeam() }) {
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
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is TeamUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is TeamUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadTeam() }) { Text("Retry") }
                    }
                }
                is TeamUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.team) { member ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = member.email, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(text = "UID: ${member.uid}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusChip(text = member.role.name, color = Color(0xFF3B82F6))
                                }
                            }
                        }
                    }
                }
            }

            if (showAssignDialog) {
                AlertDialog(
                    onDismissRequest = { showAssignDialog = false },
                    title = { Text("Provision Admin Role") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = uidInput,
                                onValueChange = { uidInput = it },
                                label = { Text("Target User UID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Admin Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select Role Level:")
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    label = { Text("Super Admin") }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (uidInput.isNotBlank() && emailInput.isNotBlank()) {
                                    showAssignDialog = false
                                    viewModel.assignRole(uidInput.trim(), emailInput.trim(), selectedRole) { _, _ -> }
                                    uidInput = ""
                                    emailInput = ""
                                }
                            },
                            enabled = uidInput.isNotBlank() && emailInput.isNotBlank()
                        ) {
                            Text("Assign Custom Claim")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showAssignDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
