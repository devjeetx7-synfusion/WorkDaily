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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.synfusion.attendance.utils.PdfExportManager
import com.synfusion.attendance.utils.PdfExportState
import com.synfusion.attendance.ui.components.PdfExportLoadingDialog
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.PictureAsPdf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTimePassbookScreen(
    viewModel: OfflineAllTimePassbookViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineAllTimePassbookState())
    val expandedMonths = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportManager = remember { PdfExportManager(context, viewModel.localAttendanceRepository) }
    val exportState by exportManager.exportState.collectAsStateWithLifecycle(initialValue = PdfExportState.Idle)

    DisposableEffect(Unit) {
        onDispose {
            exportManager.resetState()
        }
    }

    fun exportAllTimePDF() {
        scope.launch {
            exportManager.exportAllTimePdf { file ->
                scope.launch {
                    kotlinx.coroutines.delay(600)
                    exportManager.resetState()
                    val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Share All Time PDF"))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "No app found to share PDF", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_time_passbook_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportAllTimePDF() }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export All Time PDF")
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
                                placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.search_records)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    if (state.yearGroups.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.no_records_found_dots), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(state.yearGroups, key = { it.year }) { yearGroup ->
                            YearGroupItem(
                                yearGroup = yearGroup,
                                expandedMonths = expandedMonths,
                                onToggleMonth = { monthKey ->
                                    expandedMonths[monthKey] = !(expandedMonths[monthKey] ?: false)
                                }
                            )
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

    PdfExportLoadingDialog(
        state = exportState,
        onCancel = { exportManager.resetState() },
        onDismissError = { exportManager.resetState() }
    )
}

@Composable
fun EmploymentSummarySection(state: com.synfusion.attendance.viewmodel.OfflineAllTimePassbookState, viewModel: OfflineAllTimePassbookViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Employment Summary Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.employment_summary), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        if (state.employmentSince.isNotEmpty()) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.employment_since, state.employmentSince), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent().copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Active",
                            color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { viewModel.openDrillDown(DrillDownType.PRESENT) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_present), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.totalPresent.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { viewModel.openDrillDown(DrillDownType.ABSENT) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_absent), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.totalAbsent.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { viewModel.openDrillDown(DrillDownType.HALF_DAY) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_half_days), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.totalHalfDays.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.working_days), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.totalWorkingDays.toString().removeSuffix(".0"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }

        // Financial Summary Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.financial_summary), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.manage_your_daily_labor_earnings), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.salary), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("₹${AmountFormatter.format(state.totalSalaryEarned)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("₹${AmountFormatter.format(state.totalOtEarned)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getLeave())
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text("₹${AmountFormatter.format(state.totalAdvance)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.net_salary), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("₹${AmountFormatter.format(state.totalNetEarnings)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (state.totalNetEarnings >= 0) com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent() else com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
                }
            }
        }
    }
}

@Composable
fun YearGroupItem(
    yearGroup: com.synfusion.attendance.viewmodel.AllTimeYearGroup,
    expandedMonths: Map<String, Boolean>,
    onToggleMonth: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(yearGroup.year, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        yearGroup.months.forEach { monthGroup ->
            val monthKey = "${yearGroup.year}_${monthGroup.month}"
            MonthGroupItem(
                monthGroup = monthGroup,
                expanded = expandedMonths[monthKey] ?: false,
                onToggle = { onToggleMonth(monthKey) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun MonthGroupItem(
    monthGroup: com.synfusion.attendance.viewmodel.AllTimeGroup,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = monthGroup.month,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Earnings: ₹${AmountFormatter.format(monthGroup.totalEarnings + monthGroup.totalOvertime)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = "Present: ${monthGroup.presentCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Small monthly breakdown grid for a modern look
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.salary), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${AmountFormatter.format(monthGroup.totalEarnings)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${AmountFormatter.format(monthGroup.totalOvertime)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getLeave())
                        }
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_present), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(monthGroup.presentCount.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    monthGroup.logs.forEach { log ->
                        DrillDownRecordItem(log)
                    }
                }
            }
        }
    }
}
