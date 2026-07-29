package com.synfusion.attendance.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.ui.components.CustomToggleTab
import com.synfusion.attendance.ui.components.RewardedLockedContent
import com.synfusion.attendance.viewmodel.OfflineStatsViewModel
import com.synfusion.attendance.viewmodel.OfflineStatsState
import com.synfusion.attendance.viewmodel.OfflineDailyRecord
import com.synfusion.attendance.viewmodel.OfflineWorkerStats
import java.text.SimpleDateFormat
import java.util.Locale

private fun maskText(text: String, isUnlocked: Boolean, placeholder: String = "••••"): String {
    return if (isUnlocked) text else placeholder
}

@Composable
private fun LockedFeatureIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Locked",
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(16.dp)
    )
}

@Composable
fun AnimatedCounter(targetValue: Number, prefix: String = "", suffix: String = "") {
    var animatedValue by remember { mutableStateOf(0f) }

    LaunchedEffect(targetValue) {
        animatedValue = targetValue.toFloat()
    }

    val count by androidx.compose.animation.core.animateFloatAsState(targetValue = animatedValue, animationSpec = androidx.compose.animation.core.tween(1500), label = "counter")
    Text(
        text = "$prefix${com.synfusion.attendance.ui.components.CurrencyAmountVisualTransformation.formatIndianCurrency(count.toLong())}$suffix",
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: OfflineStatsViewModel,
    onNavigateToWorkerHistory: () -> Unit = {}
) {
    val state by viewModel.statsState.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Monthly", "All-Time")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.statistics), fontWeight = FontWeight.Bold) },
                actions = {
                    if (state.role == "contractor") {
                        IconButton(onClick = onNavigateToWorkerHistory) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    }
                },
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
        ) {
            if (state.isLoading && state.role.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
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

                    if (state.role == "contractor") {
                        ContractorStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1)
                    } else {
                        PersonalStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1)
                    }
                }
            }
        }
    }
}

@Composable
fun ContractorStatsView(viewModel: OfflineStatsViewModel, state: OfflineStatsState, isAllTime: Boolean) {
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)

    val currentTotalCost = if (isAllTime) state.contractorStats.allTimeCost else state.contractorStats.totalCost
    val currentTotalDays = if (isAllTime) state.contractorStats.allTimeWorks else state.contractorStats.totalDailyWorks
    val currentTopWorkers = if (isAllTime) state.contractorStats.allTimeTopWorkers else state.contractorStats.topWorkers
    val dailyRecords = if (isAllTime) emptyList() else state.contractorStats.dailyRecords

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.global_statistics), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.worker_performance_costs), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            if (!isAllTime) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                    Text(text = monthYearStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.system_overview), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_workforce), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AnimatedCounter(targetValue = state.contractorStats.totalWorkers)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_man_days), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AnimatedCounter(targetValue = currentTotalDays.toInt())
                        }
                    }
                }
            }
        }

        if (!isAllTime) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBox(
                        label = "TODAY PRESENT",
                        value = state.contractorStats.todayPresent,
                        color = Color(0xFF10B981),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "TODAY ABSENT",
                        value = state.contractorStats.todayAbsent,
                        color = Color(0xFFEF4444),
                        icon = Icons.Default.Cancel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(
                    label = "TOTAL COST",
                    value = currentTotalCost.toInt(),
                    prefix = "₹",
                    color = Color(0xFF3B82F6),
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "TOTAL ADVANCE",
                    value = state.contractorStats.totalAdvance.toInt(),
                    prefix = "₹",
                    color = Color(0xFFF97316),
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isAllTime && dailyRecords.isNotEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_costs), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box {
                            EarningsLineChart(dailyRecords = dailyRecords)
                        }
                    }
                }
            }
        }

        item {
            val maxCost = currentTopWorkers.maxOfOrNull { it.cost } ?: 1.0

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cost_breakdown), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentTopWorkers.isEmpty()) {
                        Text("No active workers ${if (isAllTime) "found" else "this month"}.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                    } else {
                        currentTopWorkers.take(5).forEach { worker ->
                            val progress = (worker.cost / maxCost).toFloat()
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(1000),
                                label = "progress"
                            )

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("₹${com.synfusion.attendance.ui.components.CurrencyAmountVisualTransformation.formatIndianCurrency(worker.cost)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${worker.days}d", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun StatBox(
    label: String,
    value: Int,
    prefix: String = "",
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedCounter(targetValue = value, prefix = prefix)
        }
    }
}

@Composable
fun PersonalStatsView(viewModel: OfflineStatsViewModel, state: OfflineStatsState, isAllTime: Boolean) {
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)

    val currentEarnings = if (isAllTime) state.personalStats.allTimeEarnings else state.personalStats.totalEarnings
    val currentDays = if (isAllTime) state.personalStats.allTimeDays else (state.personalStats.present + state.personalStats.halfDays)
    val dailyRecords = if (isAllTime) emptyList() else state.personalStats.dailyRecords

    LazyColumn(
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
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCounter(targetValue = currentEarnings.toInt(), prefix = "₹")
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
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
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_overtime), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCounter(targetValue = state.personalStats.overtime, suffix = " hrs")
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_deductions), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCounter(targetValue = state.personalStats.advanceTotal.toInt(), prefix = "₹")
                    }
                }
            }
        }

        if (!isAllTime && dailyRecords.isNotEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_earnings), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box {
                            EarningsLineChart(dailyRecords = dailyRecords)
                        }
                    }
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

                            val presentColor = Color(0xFF39b27d)
                            val halfColor = Color(0xFFF97316)
                            val absentColor = MaterialTheme.colorScheme.error

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(presentColor))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Present (${purePresent})", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(halfColor))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Half Day (${state.personalStats.halfDays})", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(absentColor))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Absent (${state.personalStats.absent})", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedCounter(targetValue = state.personalStats.allTimeEarnings.toInt(), prefix = "₹")
                        }
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
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
}

@Composable
fun EarningsLineChart(dailyRecords: List<OfflineDailyRecord>) {
    if (dailyRecords.isEmpty()) return

    val maxEarning = dailyRecords.maxOfOrNull { it.earnings } ?: 1.0
    val maxChartVal = if (maxEarning == 0.0) 1.0 else maxEarning * 1.2

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500),
        label = "chartAnim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(dailyRecords) {
                detectTapGestures { offset ->
                    val spacing = size.width / (dailyRecords.size.coerceAtLeast(2) - 1)
                    val index = (offset.x / spacing).toInt().coerceIn(0, dailyRecords.size - 1)
                    selectedIndex = index
                }
            }
        ) {
            val width = size.width
            val height = size.height

            val spacing = if (dailyRecords.size > 1) width / (dailyRecords.size - 1) else width

            val path = Path()
            val points = mutableListOf<Offset>()

            dailyRecords.forEachIndexed { index, record ->
                val x = index * spacing
                val y = height - ((record.earnings / maxChartVal) * height).toFloat()
                points.add(Offset(x, y))

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
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

            val visiblePoints = (points.size * animationProgress).toInt()
            points.take(visiblePoints).forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }
    }
}
