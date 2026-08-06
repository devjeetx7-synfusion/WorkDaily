package com.synfusion.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.viewmodel.OfflinePassbookLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillDownBottomSheet(
    title: String,
    logs: List<OfflinePassbookLog>,
    onDismiss: () -> Unit,
    totalRecordsText: String,
    totalAmountText: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(false) }

    val filteredLogs = remember(searchQuery, sortAscending, logs) {
        val filtered = if (searchQuery.isBlank()) {
            logs
        } else {
            logs.filter {
                it.date.contains(searchQuery, ignoreCase = true) ||
                (it.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        if (sortAscending) {
            filtered.sortedBy { it.date }
        } else {
            filtered.sortedByDescending { it.date }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search by date or note...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { sortAscending = !sortAscending }) {
                    Text(if (sortAscending) "Sort: Oldest First" else "Sort: Newest First")
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredLogs, key = { it.date }) { log ->
                        DrillDownRecordItem(log)
                    }
                }
            }

            // Sticky Summary Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Records", fontWeight = FontWeight.Bold)
                        Text(totalRecordsText, fontWeight = FontWeight.Bold)
                    }
                    if (totalAmountText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("₹$totalAmountText", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrillDownRecordItem(log: OfflinePassbookLog) {
    val dateParts = log.date.split("-")
    val formattedDate = if (dateParts.size == 3) "${dateParts[2]} ${getMonthName(dateParts[1].toInt())} ${dateParts[0]}" else log.date

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formattedDate, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (!log.note.isNullOrBlank()) {
                Text(log.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (log.overtimeHours > 0) {
                Text("OT: ${log.overtimeHours} hrs (₹${AmountFormatter.format(log.overtimeAmount)})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val amount = if (log.status == "advance" && log.advanceAmount != null) {
                "-₹${AmountFormatter.format(log.advanceAmount)}"
            } else if (log.baseEarnings > 0 || log.overtimeAmount > 0) {
                "+₹${AmountFormatter.format(log.baseEarnings + log.overtimeAmount)}"
            } else ""

            val color = if (log.status == "advance") MaterialTheme.colorScheme.error else Color(0xFF10B981)

            Text(
                text = log.status.replaceFirstChar { it.uppercase() } + (if (log.type == "half") " (Half)" else ""),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            if (amount.isNotEmpty()) {
                Text(amount, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

private fun getMonthName(month: Int): String {
    return when(month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> ""
    }
}
