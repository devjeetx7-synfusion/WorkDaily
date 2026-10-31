package com.synfusion.workdailyadmin.ui.support

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.workdailyadmin.data.models.SupportTicket
import com.synfusion.workdailyadmin.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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
                title = {
                    Column {
                        Text("Support Ticket Queue", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Customer service queries, worker assistance & escalation tickets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadTickets() },
                        enabled = uiState !is SupportUiState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Tickets")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (val state = uiState) {
                is SupportUiState.Loading -> {
                    AdminLoadingState(message = "Retrieving support ticket requests...")
                }
                is SupportUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadTickets() }
                    )
                }
                is SupportUiState.Success -> {
                    if (state.tickets.isEmpty()) {
                        AdminEmptyState(
                            title = "Support Desk Clear",
                            description = "No customer support inquiries or tickets require attention.",
                            icon = Icons.AutoMirrored.Filled.HelpCenter,
                            actionText = "Refresh Queue",
                            onActionClick = { viewModel.loadTickets() }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.tickets, key = { it.id }) { ticket ->
                                SupportTicketCard(
                                    ticket = ticket,
                                    onRespond = {
                                        selectedTicket = ticket
                                        selectedStatus = ticket.status
                                        replyText = ""
                                        showReplyDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showReplyDialog && selectedTicket != null) {
                val ticket = selectedTicket!!

                AlertDialog(
                    onDismissRequest = { if (!isSubmitting) showReplyDialog = false },
                    title = { Text("Respond to Ticket #${ticket.id.take(8)}", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Subject: ${ticket.subject}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                label = { Text("Official Response Notes") },
                                placeholder = { Text("Type reply or resolution details...") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Update Ticket Status:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("OPEN", "IN_PROGRESS", "CLOSED").forEach { st ->
                                    FilterChip(
                                        selected = selectedStatus == st,
                                        onClick = { selectedStatus = st },
                                        label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isSubmitting = true
                                viewModel.updateTicket(
                                    ticketId = ticket.id,
                                    status = selectedStatus,
                                    replyText = replyText.trim().ifBlank { null }
                                ) { success, _ ->
                                    isSubmitting = false
                                    if (success) {
                                        showReplyDialog = false
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Update Ticket")
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showReplyDialog = false },
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
fun SupportTicketCard(
    ticket: SupportTicket,
    onRespond: () -> Unit
) {
    val dateStr = remember(ticket.createdAt) {
        if (ticket.createdAt > 0) {
            SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(ticket.createdAt))
        } else "Recent"
    }

    val statusColor = when (ticket.status.uppercase()) {
        "OPEN" -> Color(0xFFEF4444)
        "IN_PROGRESS" -> Color(0xFFF59E0B)
        "CLOSED" -> Color(0xFF10B981)
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(text = ticket.status, color = statusColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ticket.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TruncatedIdChip(id = ticket.userUid, prefix = "User UID: ")

                Button(
                    onClick = onRespond,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Respond", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
