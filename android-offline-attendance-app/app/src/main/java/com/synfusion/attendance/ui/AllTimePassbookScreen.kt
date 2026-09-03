package com.synfusion.attendance.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Close
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
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

    val isExporting = exportState != PdfExportState.Idle && exportState !is PdfExportState.Success && exportState !is PdfExportState.Error

    fun exportAllTimePDF() {
        if (isExporting) return
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

    var isSearchActive by remember { mutableStateOf(false) }

    if (isSearchActive) {
        BackHandler {
            isSearchActive = false
            viewModel.updateSearchQuery("")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(280), initialOffsetX = { it / 3 }) + fadeIn(animationSpec = tween(280)) + scaleIn(animationSpec = tween(280), initialScale = 0.95f))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { -it / 3 }) + fadeOut(animationSpec = tween(220)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(280), initialOffsetX = { -it / 3 }) + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it / 3 }) + fadeOut(animationSpec = tween(220)) + scaleOut(animationSpec = tween(220), targetScale = 0.95f))
                    }
                },
                label = "topAppBarSearchTransition"
            ) { searchActive ->
                if (searchActive) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.search_records), fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                            }
                        },
                        windowInsets = WindowInsets(0.dp),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                } else {
                    TopAppBar(
                        title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_time_passbook_title), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(
                                onClick = { exportAllTimePDF() },
                                enabled = !isExporting
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export All Time PDF")
                            }
                        },
                        windowInsets = WindowInsets(0.dp),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ResponsiveContainer {
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
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isSearchActive) {
                            item {
                                EmploymentSummarySection(state, viewModel)
                            }
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
            }

            state.selectedDrillDownType?.let { type ->
                val titleRes = when(type) {
                    DrillDownType.EARNINGS -> com.synfusion.attendance.R.string.drilldown_all_time_earnings
                    DrillDownType.PRESENT -> com.synfusion.attendance.R.string.drilldown_all_time_present
                    DrillDownType.ABSENT -> com.synfusion.attendance.R.string.drilldown_all_time_absent
                    DrillDownType.HALF_DAY -> com.synfusion.attendance.R.string.drilldown_all_time_half_day
                    DrillDownType.OVERTIME -> com.synfusion.attendance.R.string.drilldown_all_time_overtime
                    DrillDownType.ADVANCE -> com.synfusion.attendance.R.string.drilldown_all_time_advance
                }
                DrillDownBottomSheet(
                    title = androidx.compose.ui.res.stringResource(titleRes),
                    logs = state.selectedLogs,
                    onDismiss = { viewModel.closeDrillDown() },
                    totalRecordsText = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.records_count, state.selectedLogs.size),
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
    val windowSizeInfo = LocalWindowSizeInfo.current

    val employmentCard = @Composable {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.employment_summary),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.employmentSince.isNotEmpty()) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.employment_since, state.employmentSince),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent().copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent().copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.active_status),
                            color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.openDrillDown(DrillDownType.PRESENT) },
                        shape = RoundedCornerShape(14.dp),
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent().copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent().copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_present),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.totalPresent.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.openDrillDown(DrillDownType.ABSENT) },
                        shape = RoundedCornerShape(14.dp),
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent().copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent().copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_absent),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.totalAbsent.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.openDrillDown(DrillDownType.HALF_DAY) },
                        shape = RoundedCornerShape(14.dp),
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay().copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay().copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_half_days),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.totalHalfDays.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.working_days),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.totalWorkingDays.toString().removeSuffix(".0"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }

    val financialCard = @Composable {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.financial_summary),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.manage_your_daily_labor_earnings),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.salary),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${AmountFormatter.format(state.totalSalaryEarned)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${AmountFormatter.format(state.totalOtEarned)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getLeave()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${AmountFormatter.format(state.totalAdvance)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(14.dp))

                val settlement = state.settlement
                val settlementState = settlement?.settlementState ?: if (state.totalNetEarnings > 0) com.synfusion.attendance.utils.SettlementState.RECEIVE else if (state.totalNetEarnings < 0) com.synfusion.attendance.utils.SettlementState.PAY_BACK else com.synfusion.attendance.utils.SettlementState.SETTLED
                val statusColor = when (settlementState) {
                    com.synfusion.attendance.utils.SettlementState.RECEIVE -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                    com.synfusion.attendance.utils.SettlementState.PAY_BACK -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
                    com.synfusion.attendance.utils.SettlementState.SETTLED -> MaterialTheme.colorScheme.primary
                }

                when (settlementState) {
                    com.synfusion.attendance.utils.SettlementState.RECEIVE -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.amount_to_receive), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("₹${AmountFormatter.format(settlement?.amountToReceive ?: state.totalNetEarnings)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = statusColor)
                        }
                    }
                    com.synfusion.attendance.utils.SettlementState.PAY_BACK -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.amount_to_pay_back), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
                            Text("₹${AmountFormatter.format(settlement?.amountToPayBack ?: kotlin.math.abs(state.totalNetEarnings))}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = statusColor)
                        }
                    }
                    com.synfusion.attendance.utils.SettlementState.SETTLED -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.settlement_status), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.settled_zero_pending), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                val msg = when (settlementState) {
                    com.synfusion.attendance.utils.SettlementState.RECEIVE -> androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.you_will_receive_msg, AmountFormatter.format(settlement?.amountToReceive ?: state.totalNetEarnings))
                    com.synfusion.attendance.utils.SettlementState.PAY_BACK -> androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.you_need_to_pay_back_msg, AmountFormatter.format(settlement?.amountToPayBack ?: kotlin.math.abs(state.totalNetEarnings)))
                    com.synfusion.attendance.utils.SettlementState.SETTLED -> androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.settled_zero_pending)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    if (windowSizeInfo.isCompact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            employmentCard()
            financialCard()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(1f)) { employmentCard() }
            Box(modifier = Modifier.weight(1f)) { financialCard() }
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
