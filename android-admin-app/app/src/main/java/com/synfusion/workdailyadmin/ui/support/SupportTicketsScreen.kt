package com.synfusion.workdailyadmin.ui.support

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.data.models.SupportTicket
import com.synfusion.workdailyadmin.ui.components.StatusChip
import com.synfusion.workdailyadmin.ui.components.TruncatedIdChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketsScreen(
    viewModel: SupportViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showReplyDialog by remember { mutableStateOf(false) }
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var replyText by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("OPEN") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Ticket Queue", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadTickets() }) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == "OPEN",
                    onClick = {
                        selectedStatus = "OPEN"
                        viewModel.loadTickets()
                    },
                    label = { Text("Open Tickets") }
                )
                FilterChip(
                    selected = selectedStatus == "RESOLVED",
                    onClick = {
                        selectedStatus = "RESOLVED"
                        viewModel.loadTickets()
                    },
                    label = { Text("Resolved") }
                )
                FilterChip(
                    selected = selectedStatus == "CLOSED",
                    onClick = {
                        selectedStatus = "CLOSED"
                        viewModel.loadTickets()
                    },
                    label = { Text("Closed") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is SupportUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SupportUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadTickets() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is SupportUiState.Success -> {
                    val filteredTickets = state.tickets.filter {
                        if (selectedStatus == "OPEN") it.status == "OPEN" else it.status == selectedStatus
                    }

                    if (filteredTickets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No support tickets currently found.", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTickets, key = { it.id }) { ticket ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ticket.subject,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            StatusChip(
                                                text = ticket.status,
                                                color = if (ticket.status == "OPEN") Color(0xFFEF4444) else Color(0xFF10B981)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = ticket.description, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TruncatedIdChip(id = ticket.userUid, prefix = "User: ", maxLength = 14)
                                            Button(
                                                onClick = {
                                                    selectedTicket = ticket
                                                    showReplyDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Respond")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showReplyDialog && selectedTicket != null) {
                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showReplyDialog = false },
                    title = { Text("Respond to Ticket #${selectedTicket!!.id}") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                label = { Text("Response message to user") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (replyText.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    viewModel.updateTicket(selectedTicket!!.id, "RESOLVED", replyText) { success, _ ->
                                        isSubmitting = false
                                        if (success) {
                                            showReplyDialog = false
                                            replyText = ""
                                        }
                                    }
                                }
                            },
                            enabled = replyText.isNotBlank() && !isSubmitting
                        ) {
                            Text("Send & Resolve")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showReplyDialog = false }, enabled = !isSubmitting) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
