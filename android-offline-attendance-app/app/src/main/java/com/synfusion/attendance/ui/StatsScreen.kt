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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@Composable
fun AnimatedCounter(
    targetValue: Int,
    prefix: String = "",
    suffix: String = ""
) {
    var hasCompletedAnimation by rememberSaveable { mutableStateOf(false) }
    var animatedValue by remember { mutableStateOf(if (hasCompletedAnimation) targetValue else 0) }

    LaunchedEffect(targetValue) {
        animatedValue = targetValue
    }

    val count by animateIntAsState(
        targetValue = animatedValue,
        animationSpec = if (hasCompletedAnimation) tween(0) else tween(1500),
        label = "counter",
        finishedListener = {
            hasCompletedAnimation = true
        }
    )
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
    val tabMonthly = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.tab_monthly)
    val tabAllTime = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.tab_all_time)
    val tabs = listOf(tabMonthly, tabAllTime)

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
            val titleRes = when(type) {
                DrillDownType.EARNINGS -> com.synfusion.attendance.R.string.drilldown_earnings
                DrillDownType.PRESENT -> com.synfusion.attendance.R.string.drilldown_present
                DrillDownType.ABSENT -> com.synfusion.attendance.R.string.drilldown_absent
                DrillDownType.HALF_DAY -> com.synfusion.attendance.R.string.drilldown_half_day
                DrillDownType.OVERTIME -> com.synfusion.attendance.R.string.drilldown_overtime
                DrillDownType.ADVANCE -> com.synfusion.attendance.R.string.drilldown_advance
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
    val currentOvertime = if (isAllTime) state.personalStats.allTimeOvertime else state.personalStats.overtime
    val currentAdvance = if (isAllTime) state.personalStats.allTimeAdvanceTotal else state.personalStats.advanceTotal
    val currentPresent = if (isAllTime) state.personalStats.allTimePresent else state.personalStats.present
    val currentAbsent = if (isAllTime) state.personalStats.allTimeAbsent else state.personalStats.absent
    val currentHalfDays = if (isAllTime) state.personalStats.allTimeHalfDays else state.personalStats.halfDays
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
                                AnimatedCounter(targetValue = currentOvertime, suffix = " hrs")
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.ADVANCE, isAllTime) }.padding(16.dp)) {
                            Column {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_deductions), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedCounter(targetValue = currentAdvance.toInt(), prefix = "₹")
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

                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.attendance_breakdown), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            val purePresent = currentPresent - currentHalfDays
                            val total = (purePresent + currentAbsent + currentHalfDays).toFloat()
                            if (total > 0) {
                                val presentSweep = (purePresent / total) * 360f
                                val halfSweep = (currentHalfDays / total) * 360f
                                val absentSweep = (currentAbsent / total) * 360f

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
                                            val attendanceRate = if (total > 0) ((purePresent + currentHalfDays * 0.5) / total * 100).toInt() else 0
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
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_half_label, currentHalfDays), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.ABSENT, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(absentColor))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_absent_label, currentAbsent), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                                        AnimatedCounter(targetValue = currentOvertime, suffix = " hrs")
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable { viewModel.openDrillDown(DrillDownType.ADVANCE, isAllTime) }.padding(16.dp)) {
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_deductions), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AnimatedCounter(targetValue = currentAdvance.toInt(), prefix = "₹")
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

                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.attendance_breakdown), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))

                                    val purePresent = currentPresent - currentHalfDays
                                    val total = (purePresent + currentAbsent + currentHalfDays).toFloat()
                                    if (total > 0) {
                                        val presentSweep = (purePresent / total) * 360f
                                        val halfSweep = (currentHalfDays / total) * 360f
                                        val absentSweep = (currentAbsent / total) * 360f

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
                                                    val attendanceRate = if (total > 0) ((purePresent + currentHalfDays * 0.5) / total * 100).toInt() else 0
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
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_half_label, currentHalfDays), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                }
                                                Row(modifier = Modifier.clickable { viewModel.openDrillDown(DrillDownType.ABSENT, isAllTime) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(absentColor))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stats_absent_label, currentAbsent), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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

data class ChartNiceScale(
    val maxVal: Double,
    val ticks: List<Double>
)

fun calculateNiceScale(maxDataValue: Double): ChartNiceScale {
    if (maxDataValue <= 0.0) {
        return ChartNiceScale(
            maxVal = 500.0,
            ticks = listOf(500.0, 375.0, 250.0, 125.0, 0.0)
        )
    }

    val maxTicks = 4
    val rawInterval = maxDataValue / maxTicks
    val magnitude = 10.0.pow(floor(log10(rawInterval)))
    val residual = rawInterval / magnitude

    val niceInterval = when {
        residual <= 1.0 -> 1.0 * magnitude
        residual <= 2.0 -> 2.0 * magnitude
        residual <= 2.5 -> 2.5 * magnitude
        residual <= 5.0 -> 5.0 * magnitude
        else -> 10.0 * magnitude
    }

    var niceMax = ceil(maxDataValue / niceInterval) * niceInterval
    if (niceMax <= maxDataValue) {
        niceMax += niceInterval
    }

    val ticks = mutableListOf<Double>()
    var curr = niceMax
    while (curr >= -0.0001) {
        ticks.add(curr.coerceAtLeast(0.0))
        curr -= niceInterval
    }

    return ChartNiceScale(maxVal = niceMax, ticks = ticks)
}

fun formatYAxisLabel(value: Double): String {
    if (value == 0.0) return "₹0"
    return if (value >= 1000) {
        val kVal = value / 1000.0
        if (kVal % 1.0 == 0.0) "₹${kVal.toInt()}K" else "₹${"%.1f".format(Locale.US, kVal)}K"
    } else {
        "₹${value.toInt()}"
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
    val niceScale = remember(maxEarning) { calculateNiceScale(maxEarning) }

    var selectedIndex by remember(dailyRecords) { mutableStateOf<Int?>(null) }
    if (selectedIndex != null && selectedIndex!! >= dailyRecords.size) {
        selectedIndex = if (dailyRecords.isNotEmpty()) dailyRecords.size - 1 else null
    }

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000),
        label = "chartAnim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

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

    val pointWidthDp = 44.dp
    val minChartWidthDp = pointWidthDp * dailyRecords.size.coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Selected Data Point Compact Tooltip Card
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
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.earning_label, AmountFormatter.format(record.earnings)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryColor
                            )
                            if (record.baseEarnings > 0 && record.baseEarnings != record.earnings) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.regular_label, AmountFormatter.format(record.baseEarnings)),
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor
                                )
                            } else if (record.earnings > 0 && record.overtimeAmount > 0) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.regular_label, AmountFormatter.format(record.baseEarnings)),
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor
                                )
                            }
                            if (record.overtimeAmount > 0 || record.overtimeHours > 0) {
                                val otText = if (record.overtimeHours > 0) {
                                    androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime_label_hrs, AmountFormatter.format(record.overtimeAmount), record.overtimeHours)
                                } else {
                                    androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime_label, AmountFormatter.format(record.overtimeAmount))
                                }
                                Text(
                                    text = otText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onSurfaceVariantColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            // Dynamic Nice Scale Y-Axis Labels
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .padding(top = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                niceScale.ticks.forEach { tickVal ->
                    Text(
                        text = formatYAxisLabel(tickVal),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelTextColor,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Scrollable Chart Canvas area
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val viewportWidthPx = with(density) { maxWidth.toPx() }
                val minChartWidthPx = with(density) { minChartWidthDp.toPx() }
                val chartWidthPx = maxOf(viewportWidthPx, minChartWidthPx)
                val chartWidthDp = with(density) { chartWidthPx.toDp() }

                val count = dailyRecords.size
                val spacing = if (count > 1) chartWidthPx / (count - 1) else chartWidthPx / 2f

                // Auto-scroll logic when selectedIndex changes
                LaunchedEffect(selectedIndex, dailyRecords) {
                    selectedIndex?.let { idx ->
                        if (idx in dailyRecords.indices) {
                            val targetX = if (count > 1) idx * spacing else chartWidthPx / 2f
                            val maxScroll = scrollState.maxValue
                            if (maxScroll > 0) {
                                val targetScroll = (targetX - viewportWidthPx / 2f).roundToInt().coerceIn(0, maxScroll)
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(targetScroll)
                                }
                            }
                        }
                    }
                }

                fun findNearestIndex(touchX: Float): Int {
                    if (count <= 0) return -1
                    if (count == 1) return 0
                    return (touchX / spacing).roundToInt().coerceIn(0, count - 1)
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(chartWidthDp)
                            .fillMaxHeight()
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(dailyRecords, chartWidthPx) {
                                    detectTapGestures { offset ->
                                        val idx = findNearestIndex(offset.x)
                                        if (idx != -1) selectedIndex = idx
                                    }
                                }
                                .pointerInput(dailyRecords, chartWidthPx) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            val idx = findNearestIndex(offset.x)
                                            if (idx != -1) selectedIndex = idx
                                        },
                                        onHorizontalDrag = { change, _ ->
                                            val idx = findNearestIndex(change.position.x)
                                            if (idx != -1) selectedIndex = idx
                                        }
                                    )
                                }
                        ) {
                            val width = size.width
                            val height = size.height
                            val topPadding = 20.dp.toPx()
                            val bottomPadding = 30.dp.toPx()
                            val usableHeight = height - topPadding - bottomPadding

                            // Background Horizontal Grid Lines matching Y-Axis Ticks
                            niceScale.ticks.forEach { tickVal ->
                                val ratio = (tickVal / niceScale.maxVal).coerceIn(0.0, 1.0).toFloat()
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
                                val ratio = (record.earnings / niceScale.maxVal).coerceIn(0.0, 1.0).toFloat()
                                val y = topPadding + usableHeight * (1f - ratio)
                                points.add(Offset(x, y))
                            }

                            if (points.isNotEmpty()) {
                                // Draw Line Path
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

                                // Vertical Guide Line for Selected Point
                                selectedIndex?.let { selIdx ->
                                    if (selIdx in points.indices) {
                                        val selPt = points[selIdx]
                                        drawLine(
                                            color = primaryColor.copy(alpha = 0.45f),
                                            start = Offset(selPt.x, topPadding),
                                            end = Offset(selPt.x, height - bottomPadding),
                                            strokeWidth = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    }
                                }

                                // Draw Data Points & Outer Pulse Highlight Ring
                                val visibleCount = (points.size * animationProgress).toInt().coerceIn(0, points.size)
                                for (i in 0 until visibleCount) {
                                    val pt = points[i]
                                    val isSelected = selectedIndex == i

                                    if (isSelected) {
                                        // Outer pulse highlight ring
                                        drawCircle(
                                            color = primaryColor.copy(alpha = 0.2f),
                                            radius = 14.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = primaryColor.copy(alpha = 0.4f),
                                            radius = 10.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = primaryColor,
                                            radius = 7.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 3.5.dp.toPx(),
                                            center = pt
                                        )
                                    } else {
                                        drawCircle(
                                            color = primaryColor.copy(alpha = 0.85f),
                                            radius = 4.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 2.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                }
                            }
                        }

                        // Adaptive X-Axis Labels Overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(28.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val step = when {
                                count <= 10 -> 1
                                count <= 20 -> 2
                                count <= 31 -> 3
                                else -> (count / 10).coerceAtLeast(1)
                            }

                            dailyRecords.forEachIndexed { index, record ->
                                val isSelected = selectedIndex == index
                                val recordDateLabel = formatDateForXAxis(record.dateStr)
                                val accessibleText = "${formatDateForTooltip(record.dateStr)}, earning ₹${AmountFormatter.format(record.earnings)}${if (isSelected) ", selected" else ""}"

                                if (index % step == 0 || index == count - 1 || isSelected) {
                                    Text(
                                        text = recordDateLabel,
                                        fontSize = 9.sp,
                                        color = if (isSelected) primaryColor else labelTextColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        modifier = Modifier.semantics {
                                            contentDescription = accessibleText
                                        }
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
}
