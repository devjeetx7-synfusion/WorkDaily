package com.synfusion.workdailyadmin.ui.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketsScreen(
    viewModel: SupportViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showReplyDialog by remember { mutableStateOf(false) }
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var replyText by remember { mutableStateOf("") }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is SupportUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SupportUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadTickets() }) { Text("Retry") }
                    }
                }
                is SupportUiState.Success -> {
                    if (state.tickets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No support tickets in queue.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.tickets) { ticket ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = ticket.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            StatusChip(text = ticket.status, color = Color(0xFF3B82F6))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = ticket.description, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "User UID: ${ticket.userUid}", style = MaterialTheme.typography.labelSmall)

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                selectedTicket = ticket
                                                showReplyDialog = true
                                            }
                                        ) {
                                            Text("Reply & Resolve")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showReplyDialog && selectedTicket != null) {
                val t = selectedTicket!!
                AlertDialog(
                    onDismissRequest = { showReplyDialog = false },
                    title = { Text("Reply to Support Ticket") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                label = { Text("Support Response Message") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    showReplyDialog = false
                                    viewModel.updateTicket(t.id, "RESOLVED", replyText) { _, _ -> }
                                    replyText = ""
                                }
                            },
                            enabled = replyText.isNotBlank()
                        ) {
                            Text("Send & Resolve")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showReplyDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
