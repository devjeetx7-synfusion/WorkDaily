package com.synfusion.attendance.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.data.DrillDownType
import com.synfusion.attendance.ui.components.DrillDownBottomSheet
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineAllTimePassbookViewModel
import com.synfusion.attendance.viewmodel.OfflineAllTimePassbookState
import com.synfusion.attendance.ui.components.DrillDownRecordItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTimePassbookScreen(
    viewModel: OfflineAllTimePassbookViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineAllTimePassbookState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Time Passbook", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        EmploymentSummarySection(state, viewModel)
                    }

                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search records...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    if (state.yearGroups.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(state.yearGroups) { yearGroup ->
                            YearGroupItem(yearGroup)
                        }
                    }
                }
            }

            state.selectedDrillDownType?.let { type ->
                DrillDownBottomSheet(
                    title = when(type) {
                        DrillDownType.EARNINGS -> "All Time Earnings"
                        DrillDownType.PRESENT -> "All Time Present"
                        DrillDownType.ABSENT -> "All Time Absent"
                        DrillDownType.HALF_DAY -> "All Time Half Day"
                        DrillDownType.OVERTIME -> "All Time Overtime"
                        DrillDownType.ADVANCE -> "All Time Advance"
                    },
                    logs = state.selectedLogs,
                    onDismiss = { viewModel.closeDrillDown() },
                    totalRecordsText = "${state.selectedLogs.size} records",
                    totalAmountText = if (type == DrillDownType.EARNINGS || type == DrillDownType.OVERTIME || type == DrillDownType.ADVANCE) {
                        val total = if (type == DrillDownType.ADVANCE) {
                            state.selectedLogs.sumOf { it.advanceAmount ?: 0.0 }
                        } else if (type == DrillDownType.OVERTIME) {
                            state.selectedLogs.sumOf { it.overtimeAmount }
                        } else {
                            state.selectedLogs.sumOf { it.baseEarnings + it.overtimeAmount }
                        }
                        AmountFormatter.format(total)
                    } else null
                )
            }
        }
    }
}

@Composable
fun EmploymentSummarySection(state: com.synfusion.attendance.viewmodel.OfflineAllTimePassbookState, viewModel: OfflineAllTimePassbookViewModel) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text("Employment Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (state.employmentSince.isNotEmpty()) {
            Text("Since ${state.employmentSince}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).clickable { viewModel.openDrillDown(DrillDownType.EARNINGS) }.padding(12.dp)) {
                Column {
                    Text("Total Net Earnings", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${AmountFormatter.format(state.totalNetEarnings)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).padding(12.dp)) {
                Column {
                    Text("Total Working Days", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalWorkingDays.toString().removeSuffix(".0"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).clickable { viewModel.openDrillDown(DrillDownType.PRESENT) }.padding(12.dp)) {
                Column {
                    Text("Total Present", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalPresent.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).clickable { viewModel.openDrillDown(DrillDownType.ABSENT) }.padding(12.dp)) {
                Column {
                    Text("Total Absent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalAbsent.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).clickable { viewModel.openDrillDown(DrillDownType.HALF_DAY) }.padding(12.dp)) {
                Column {
                    Text("Total Half Days", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalHalfDays.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).clickable { viewModel.openDrillDown(DrillDownType.OVERTIME) }.padding(12.dp)) {
                Column {
                    Text("Total OT Earned", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${AmountFormatter.format(state.totalOtEarned)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).clickable { viewModel.openDrillDown(DrillDownType.ADVANCE) }.padding(12.dp)) {
                Column {
                    Text("Total Advance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${AmountFormatter.format(state.totalAdvance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun YearGroupItem(yearGroup: com.synfusion.attendance.viewmodel.AllTimeYearGroup) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(yearGroup.year, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        yearGroup.months.forEach { monthGroup ->
            MonthGroupItem(monthGroup = monthGroup)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun MonthGroupItem(monthGroup: com.synfusion.attendance.viewmodel.AllTimeGroup) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(monthGroup.month, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Earnings: ₹${AmountFormatter.format(monthGroup.totalEarnings + monthGroup.totalOvertime)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(rotation))
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                monthGroup.logs.forEach { log ->
                    DrillDownRecordItem(log)
                }
            }
        }
    }
}
