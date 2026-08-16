package com.synfusion.attendance.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import com.synfusion.attendance.ui.components.ShimmerBox
import com.synfusion.attendance.ui.components.ShimmerCircle
import com.synfusion.attendance.ui.components.ShimmerTextLine
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.synfusion.attendance.utils.AmountFormatter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.ui.components.CustomToggleTab
import com.synfusion.attendance.viewmodel.OfflineStatsViewModel
import com.synfusion.attendance.viewmodel.OfflineStatsState
import com.synfusion.attendance.viewmodel.OfflineDailyRecord
import com.synfusion.attendance.data.DrillDownType
import com.synfusion.attendance.ui.components.DrillDownBottomSheet
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import java.text.SimpleDateFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.util.Locale

@Composable
fun AnimatedCounter(targetValue: Int, prefix: String = "", suffix: String = "") {
    var animatedValue by remember { mutableStateOf(0) }

    LaunchedEffect(targetValue) {
        animatedValue = targetValue
    }

    val count by animateIntAsState(targetValue = animatedValue, animationSpec = tween(1500), label = "counter")
    val formattedCount = if (prefix == "₹" || prefix == "Rs.") {
        AmountFormatter.format(count)
    } else {
        count.toString()
    }
    Text(
        text = if (prefix == "₹" || prefix == "Rs.") "₹$formattedCount$suffix" else "$prefix$count$suffix",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: OfflineStatsViewModel,
    onNavigatePassbook: () -> Unit = {},
    scrollState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    val state by viewModel.statsState.collectAsStateWithLifecycle(initialValue = OfflineStatsState(isLoading = true))

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Monthly", "All-Time")

    val pullRefreshState = rememberPullToRefreshState()
    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.refresh()
        }
    }
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            pullRefreshState.startRefresh()
        } else {
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.statistics), fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            Crossfade(
                targetState = if (state.isLoading) "shimmer" else if (state.errorMessage != null) "error" else "content",
                animationSpec = tween(350),
                label = "stats_crossfade"
            ) { pageState ->
                when (pageState) {
                    "shimmer" -> {
                        StatsShimmer()
                    }
                    "error" -> {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                                Text(state.errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Button(onClick = { viewModel.refresh() }) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.try_again))
                                }
                            }
                        }
                    }
                    "content" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                CustomToggleTab(
                                    tabs = tabs,
                                    selectedTabIndex = selectedTabIndex,
                                    onTabSelected = { selectedTabIndex = it }
                                )
                            }

                            PersonalStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1, onNavigatePassbook = onNavigatePassbook, scrollState = scrollState)
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }

        state.selectedDrillDownType?.let { type ->
            DrillDownBottomSheet(
                title = when(type) {
                    DrillDownType.EARNINGS -> "Earnings"
                    DrillDownType.PRESENT -> "Present"
                    DrillDownType.ABSENT -> "Absent"
                    DrillDownType.HALF_DAY -> "Half Day"
                    DrillDownType.OVERTIME -> "Overtime"
                    DrillDownType.ADVANCE -> "Advance"
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

@Composable
fun StatsShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle tab shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 48.dp, cornerRadius = 24.dp)
        Spacer(modifier = Modifier.height(8.dp))

        ShimmerTextLine(height = 24.dp, fraction = 0.4f)
        Spacer(modifier = Modifier.height(16.dp))

        // Month Selector Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 56.dp, cornerRadius = 16.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Grid cards shimmer
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 80.dp, cornerRadius = 16.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 80.dp, cornerRadius = 16.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 80.dp, cornerRadius = 16.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 80.dp, cornerRadius = 16.dp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Chart Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 200.dp, cornerRadius = 16.dp)
    }
}

@Composable
fun PersonalStatsView(
    viewModel: OfflineStatsViewModel,
    state: OfflineStatsState,
    isAllTime: Boolean,
    onNavigatePassbook: () -> Unit = {},
    scrollState: androidx.compose.foundation.lazy.LazyListState
) {
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)

    val currentEarnings = if (isAllTime) state.personalStats.allTimeEarnings else state.personalStats.totalEarnings
    val currentDays = if (isAllTime) state.personalStats.allTimeDays else (state.personalStats.present + state.personalStats.halfDays)
    val dailyRecords = if (isAllTime) emptyList() else state.personalStats.dailyRecords
    val windowSizeInfo = LocalWindowSizeInfo.current

    ResponsiveContainer {
        if (windowSizeInfo.isCompact) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isAllTime) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                            Text(text = monthYearStr, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.EARNINGS, isAllTime) }.padding(16.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedCounter(targetValue = currentEarnings.toInt(), prefix = "₹")
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.PRESENT, isAllTime) }.padding(16.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_days), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedCounter(targetValue = currentDays.toInt())
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.OVERTIME, isAllTime) }.padding(16.dp)) {
                            Column {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_overtime), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedCounter(targetValue = state.personalStats.overtime, suffix = " hrs")
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.ADVANCE, isAllTime) }.padding(16.dp)) {
                            Column {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_deductions), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedCounter(targetValue = state.personalStats.advanceTotal.toInt(), prefix = "₹")
                            }
                        }
                    }
                }

                item {
                    val currentDailyRecords = if (isAllTime) state.personalStats.allTimeDailyRecords else state.personalStats.dailyRecords
                    val currentHasRecords = if (isAllTime) state.personalStats.hasAllTimeRecords else state.personalStats.hasRecordsInPeriod
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_earnings), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            EarningsLineChart(
                                dailyRecords = currentDailyRecords,
                                hasRecords = currentHasRecords,
                                isAllTime = isAllTime
                            )
                        }
                    }
                }

                if (!isAllTime) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.attendance_breakdown), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                val purePresent = state.personalStats.present - state.personalStats.halfDays
                                val total = (purePresent + state.personalStats.absent + state.personalStats.halfDays).toFloat()
                                if (total > 0) {
                                    val presentSweep = (purePresent / total) * 360f
                                    val halfSweep = (state.personalStats.halfDays / total) * 360f
                                    val absentSweep = (state.personalStats.absent / total) * 360f

                                    val animatedPresentSweep by animateFloatAsState(targetValue = presentSweep, animationSpec = tween(1000), label = "present")
                                    val animatedHalfSweep by animateFloatAsState(targetValue = halfSweep, animationSpec = tween(1000), label = "half")
                                    val animatedAbsentSweep by animateFloatAsState(targetValue = absentSweep, animationSpec = tween(1000), label = "absent")

                                    val presentColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                                    val halfColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()
                                    val absentColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()

                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.size(100.dp)) {
                                                val strokeWidth = 24.dp.toPx()

                                                drawArc(
                                                    color = Color.LightGray.copy(alpha = 0.3f),
                                                    startAngle = 0f,
                                                    sweepAngle = 360f,
                                                    useCenter = false,
                                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                                )

                                                var currentStartAngle = -90f

                                                drawArc(
                                                    color = presentColor,
                                                    startAngle = currentStartAngle,
                                                    sweepAngle = animatedPresentSweep,
                                                    useCenter = false,
                                                    style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                                                )
                                                currentStartAngle += animatedPresentSweep

                                                drawArc(
                                                    color = halfColor,
                                                    startAngle = currentStartAngle,
                                                    sweepAngle = animatedHalfSweep,
                                                    useCenter = false,
                                                    style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                                                )
                                                currentStartAngle += animatedHalfSweep

                                                drawArc(
                                                    color = absentColor,
                                                    startAngle = currentStartAngle,
                                                    sweepAngle = animatedAbsentSweep,
                                                    useCenter = false,
                                                    style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                val attendanceRate = if (total > 0) ((purePresent + state.personalStats.halfDays * 0.5) / total * 100).toInt() else 0
                                                Text("${attendanceRate}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.attendance), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(24.dp))

                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.PRESENT, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(presentColor))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_present_label, purePresent), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            }
                                            Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.HALF_DAY, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(halfColor))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_half_label, state.personalStats.halfDays), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            }
                                            Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.ABSENT, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(absentColor))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_absent_label, state.personalStats.absent), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.no_data), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isAllTime) {
                    item {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_time_stats), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.EARNINGS, true) }.padding(16.dp)) {
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AnimatedCounter(targetValue = state.personalStats.allTimeEarnings.toInt(), prefix = "₹")
                                }
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.PRESENT, true) }.padding(16.dp)) {
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_days), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AnimatedCounter(targetValue = state.personalStats.allTimeDays)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: Month selector & summary cards
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!isAllTime) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                                    Text(text = monthYearStr, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.EARNINGS, isAllTime) }.padding(16.dp)) {
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AnimatedCounter(targetValue = currentEarnings.toInt(), prefix = "₹")
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.PRESENT, isAllTime) }.padding(16.dp)) {
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_days), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AnimatedCounter(targetValue = currentDays.toInt())
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.OVERTIME, isAllTime) }.padding(16.dp)) {
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_overtime), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AnimatedCounter(targetValue = state.personalStats.overtime, suffix = " hrs")
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.ADVANCE, isAllTime) }.padding(16.dp)) {
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_deductions), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AnimatedCounter(targetValue = state.personalStats.advanceTotal.toInt(), prefix = "₹")
                                    }
                                }
                            }

                            if (!isAllTime) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_time_stats), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.EARNINGS, true) }.padding(16.dp)) {
                                        Column {
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            AnimatedCounter(targetValue = state.personalStats.allTimeEarnings.toInt(), prefix = "₹")
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.PRESENT, true) }.padding(16.dp)) {
                                        Column {
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_days), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            AnimatedCounter(targetValue = state.personalStats.allTimeDays)
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column: Charts
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val currentDailyRecords = if (isAllTime) state.personalStats.allTimeDailyRecords else state.personalStats.dailyRecords
                            val currentHasRecords = if (isAllTime) state.personalStats.hasAllTimeRecords else state.personalStats.hasRecordsInPeriod
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_earnings), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    EarningsLineChart(
                                        dailyRecords = currentDailyRecords,
                                        hasRecords = currentHasRecords,
                                        isAllTime = isAllTime
                                    )
                                }
                            }

                            if (!isAllTime) {
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.attendance_breakdown), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))

                                        val purePresent = state.personalStats.present - state.personalStats.halfDays
                                        val total = (purePresent + state.personalStats.absent + state.personalStats.halfDays).toFloat()
                                        if (total > 0) {
                                            val presentSweep = (purePresent / total) * 360f
                                            val halfSweep = (state.personalStats.halfDays / total) * 360f
                                            val absentSweep = (state.personalStats.absent / total) * 360f

                                            val animatedPresentSweep by animateFloatAsState(targetValue = presentSweep, animationSpec = tween(1000), label = "present")
                                            val animatedHalfSweep by animateFloatAsState(targetValue = halfSweep, animationSpec = tween(1000), label = "half")
                                            val animatedAbsentSweep by animateFloatAsState(targetValue = absentSweep, animationSpec = tween(1000), label = "absent")

                                            val presentColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                                            val halfColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()
                                            val absentColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()

                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                                    Canvas(modifier = Modifier.size(100.dp)) {
                                                        val strokeWidth = 24.dp.toPx()

                                                        drawArc(
                                                            color = Color.LightGray.copy(alpha = 0.3f),
                                                            startAngle = 0f,
                                                            sweepAngle = 360f,
                                                            useCenter = false,
                                                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                                        )

                                                        var currentStartAngle = -90f

                                                        drawArc(
                                                            color = presentColor,
                                                            startAngle = currentStartAngle,
                                                            sweepAngle = animatedPresentSweep,
                                                            useCenter = false,
                                                            style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                                                        )
                                                        currentStartAngle += animatedPresentSweep

                                                        drawArc(
                                                            color = halfColor,
                                                            startAngle = currentStartAngle,
                                                            sweepAngle = animatedHalfSweep,
                                                            useCenter = false,
                                                            style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                                                        )
                                                        currentStartAngle += animatedHalfSweep

                                                        drawArc(
                                                            color = absentColor,
                                                            startAngle = currentStartAngle,
                                                            sweepAngle = animatedAbsentSweep,
                                                            useCenter = false,
                                                            style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        val attendanceRate = if (total > 0) ((purePresent + state.personalStats.halfDays * 0.5) / total * 100).toInt() else 0
                                                        Text("${attendanceRate}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.attendance), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(24.dp))

                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.PRESENT, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(presentColor))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_present_label, purePresent), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                    Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.HALF_DAY, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(halfColor))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_half_label, state.personalStats.halfDays), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                    Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.ABSENT, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(absentColor))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_absent_label, state.personalStats.absent), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.no_data), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsLineChart(
    dailyRecords: List<OfflineDailyRecord>,
    hasRecords: Boolean = true,
    isAllTime: Boolean = false
) {
    if (!hasRecords || dailyRecords.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.no_data),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val maxEarning = remember(dailyRecords) { dailyRecords.maxOfOrNull { it.earnings } ?: 0.0 }
    val maxChartVal = remember(maxEarning) {
        if (maxEarning == 0.0) 100.0 else maxEarning * 1.2
    }

    var selectedIndex by remember(dailyRecords) { mutableStateOf<Int?>(null) }

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200),
        label = "chartAnim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Date Formatters
    val inSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthDaySdf = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val fullDateSdf = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    fun formatDateForXAxis(dateStr: String): String {
        return try {
            val date = inSdf.parse(dateStr) ?: return dateStr
            if (isAllTime) monthDaySdf.format(date) else dateStr.split("-").last().toInt().toString()
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatDateForTooltip(dateStr: String): String {
        return try {
            val date = inSdf.parse(dateStr) ?: return dateStr
            fullDateSdf.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    val pointWidthDp = if (dailyRecords.size > 31) 36.dp else if (dailyRecords.size > 15) 28.dp else 40.dp
    val minChartWidthDp = pointWidthDp * dailyRecords.size.coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Selected Data Point Tooltip Card
        selectedIndex?.let { idx ->
            if (idx in dailyRecords.indices) {
                val record = dailyRecords[idx]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceVariantColor)
                        .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDateForTooltip(record.dateStr),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceVariantColor
                            )
                            IconButton(
                                onClick = { selectedIndex = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = onSurfaceVariantColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Earning: ₹${AmountFormatter.format(record.earnings)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor
                                )
                                Text(
                                    text = "Regular: ₹${AmountFormatter.format(record.baseEarnings)}",
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor
                                )
                            }
                            if (record.overtimeAmount > 0 || record.overtimeHours > 0) {
                                Column {
                                    Text(
                                        text = "Overtime: ₹${AmountFormatter.format(record.overtimeAmount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onSurfaceVariantColor
                                    )
                                    if (record.overtimeHours > 0) {
                                        Text(
                                            text = "${record.overtimeHours} hrs OT",
                                            fontSize = 12.sp,
                                            color = onSurfaceVariantColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val y1 = maxChartVal
                val y2 = maxChartVal * 0.66
                val y3 = maxChartVal * 0.33
                val y4 = 0.0

                Text(text = "₹${y1.toInt()}", fontSize = 9.sp, color = labelTextColor, maxLines = 1)
                Text(text = "₹${y2.toInt()}", fontSize = 9.sp, color = labelTextColor, maxLines = 1)
                Text(text = "₹${y3.toInt()}", fontSize = 9.sp, color = labelTextColor, maxLines = 1)
                Text(text = "₹${y4.toInt()}", fontSize = 9.sp, color = labelTextColor, maxLines = 1)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Scrollable Chart Canvas area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = minChartWidthDp)
                        .fillMaxHeight()
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(dailyRecords) {
                                detectTapGestures { offset ->
                                    val topPadding = 20.dp.toPx()
                                    val bottomPadding = 30.dp.toPx()
                                    val chartHeight = size.height - topPadding - bottomPadding

                                    val count = dailyRecords.size
                                    val spacing = if (count > 1) size.width / (count - 1) else size.width
                                    val index = if (count > 1) {
                                        (offset.x / spacing).toInt().coerceIn(0, count - 1)
                                    } else 0
                                    selectedIndex = index
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val topPadding = 20.dp.toPx()
                        val bottomPadding = 30.dp.toPx()
                        val usableHeight = height - topPadding - bottomPadding

                        val count = dailyRecords.size
                        val spacing = if (count > 1) width / (count - 1) else width / 2f

                        // Draw background horizontal grid lines
                        val gridLines = listOf(0f, 0.33f, 0.66f, 1f)
                        gridLines.forEach { ratio ->
                            val gridY = topPadding + usableHeight * (1f - ratio)
                            drawLine(
                                color = outlineColor,
                                start = Offset(0f, gridY),
                                end = Offset(width, gridY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val points = mutableListOf<Offset>()
                        dailyRecords.forEachIndexed { index, record ->
                            val x = if (count > 1) index * spacing else width / 2f
                            val ratio = (record.earnings / maxChartVal).coerceIn(0.0, 1.0).toFloat()
                            val y = topPadding + usableHeight * (1f - ratio)
                            points.add(Offset(x, y))
                        }

                        // Path calculation
                        if (points.isNotEmpty()) {
                            val path = Path()
                            points.forEachIndexed { idx, pt ->
                                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                            }

                            val animPath = Path()
                            val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
                            val length = pathMeasure.length
                            val dst = android.graphics.Path()
                            pathMeasure.getSegment(0f, length * animationProgress, dst, true)
                            animPath.addPath(dst.asComposePath())

                            drawPath(
                                path = animPath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            val visiblePointsCount = (points.size * animationProgress).toInt().coerceIn(0, points.size)
                            for (i in 0 until visiblePointsCount) {
                                val pt = points[i]
                                val isSelected = selectedIndex == i

                                drawCircle(
                                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.9f),
                                    radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = androidx.compose.ui.graphics.Color.White,
                                    radius = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }

                    // Adaptive X-Axis Labels Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val count = dailyRecords.size
                        val step = when {
                            count <= 10 -> 1
                            count <= 20 -> 2
                            count <= 31 -> 3
                            else -> (count / 10).coerceAtLeast(1)
                        }

                        dailyRecords.forEachIndexed { index, record ->
                            if (index % step == 0 || index == count - 1) {
                                Text(
                                    text = formatDateForXAxis(record.dateStr),
                                    fontSize = 9.sp,
                                    color = if (selectedIndex == index) primaryColor else labelTextColor,
                                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
