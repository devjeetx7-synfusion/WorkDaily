package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailywork.attedance.viewmodel.StatsViewModel
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.dailywork.attedance.ui.components.CustomToggleTab
import com.dailywork.attedance.ui.components.PremiumLockedIcon
import com.dailywork.attedance.ui.components.InlinePremiumCard
import com.dailywork.attedance.ui.components.maskText
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateIntAsState

@Composable
fun AnimatedCounter(targetValue: Int, prefix: String = "", suffix: String = "") {
    var animatedValue by remember { mutableStateOf(0) }

    LaunchedEffect(targetValue) {
        animatedValue = targetValue
    }

    val count by animateIntAsState(targetValue = animatedValue, animationSpec = tween(1500))
    Text(
        text = "$prefix$count$suffix",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreenContent(
    viewModel: StatsViewModel,
    onNavigateToPremium: () -> Unit = {},
    onNavigateToWorkerHistory: () -> Unit = {}
) {
    val state by viewModel.statsState.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
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

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Monthly", "All-Time")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.statistics), fontWeight = FontWeight.Bold) },
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
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
                if (state.isLoading && state.role.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val statsContent = @Composable {
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
                            ContractorStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1, onNavigateToPremium = onNavigateToPremium)
                        } else {
                            PersonalStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1, onNavigateToPremium = onNavigateToPremium)
                        }
                    }

                    // Both views already use LazyColumn internally
                    Column(modifier = Modifier.fillMaxSize()) {
                        statsContent()
                    }
                }
                PullToRefreshContainer(
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

@Composable
fun ContractorStatsView(viewModel: StatsViewModel, state: com.dailywork.attedance.viewmodel.StatsState, isAllTime: Boolean, onNavigateToPremium: () -> Unit = {}) {
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
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.global_statistics), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.worker_performance_costs), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Main Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.system_overview), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        if (!state.isPremium) {
                            Spacer(modifier = Modifier.width(8.dp))
                            PremiumLockedIcon()
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.total_workforce), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.isPremium) {
                                AnimatedCounter(targetValue = state.contractorStats.totalWorkers)
                            } else {
                                Text("--", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.total_man_days), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.isPremium) {
                                AnimatedCounter(targetValue = currentTotalDays.toInt())
                            } else {
                                Text("--", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
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
                    modifier = Modifier.weight(1f),
                    isPremium = state.isPremium
                )
                StatBox(
                    label = "TOTAL ADVANCE",
                    value = state.contractorStats.totalAdvance.toInt(),
                    prefix = "₹",
                    color = Color(0xFFF97316),
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f),
                    isPremium = state.isPremium
                )
            }
        }

        if (!isAllTime && dailyRecords.isNotEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.daily_costs), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            if (!state.isPremium) {
                                Spacer(modifier = Modifier.width(8.dp))
                                PremiumLockedIcon()
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = if (!state.isPremium) Modifier.alpha(0.3f) else Modifier) {
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
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.cost_breakdown), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        if (!state.isPremium) {
                            Spacer(modifier = Modifier.width(8.dp))
                            PremiumLockedIcon()
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentTopWorkers.isEmpty()) {
                        Text("No active workers ${if (isAllTime) "found" else "this month"}.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                    } else {
                        currentTopWorkers.take(5).forEach { worker ->
                            val progress = (worker.cost / maxCost).toFloat()
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(1000)
                            )

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(maskText(worker.name, state.isPremium), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("₹${maskText(worker.cost.toInt().toString(), state.isPremium, "--")}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(if (state.isPremium) animatedProgress else 0.3f).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${if (state.isPremium) worker.days else "--"}d", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!state.isPremium) {
            item {
                InlinePremiumCard(onUpgrade = onNavigateToPremium)
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
    modifier: Modifier = Modifier,
    isPremium: Boolean = true
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
                if (!isPremium) {
                    Spacer(modifier = Modifier.width(4.dp))
                    PremiumLockedIcon(modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (isPremium) {
                AnimatedCounter(targetValue = value, prefix = prefix)
            } else {
                Text("$prefix --", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PersonalStatsView(viewModel: StatsViewModel, state: com.dailywork.attedance.viewmodel.StatsState, isAllTime: Boolean, onNavigateToPremium: () -> Unit = {}) {
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
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.stats), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!state.isPremium) {
                                Spacer(modifier = Modifier.width(4.dp))
                                PremiumLockedIcon(modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (state.isPremium) {
                            AnimatedCounter(targetValue = currentEarnings.toInt(), prefix = "₹")
                        } else {
                            Text("₹ --", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.total_days), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!state.isPremium) {
                                Spacer(modifier = Modifier.width(4.dp))
                                PremiumLockedIcon(modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (state.isPremium) {
                            AnimatedCounter(targetValue = currentDays.toInt())
                        } else {
                            Text("--", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.total_overtime), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCounter(targetValue = state.personalStats.overtime, suffix = " hrs")
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.advance_deductions), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.daily_earnings), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            if (!state.isPremium) {
                                Spacer(modifier = Modifier.width(8.dp))
                                PremiumLockedIcon()
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = if (!state.isPremium) Modifier.alpha(0.3f) else Modifier) {
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
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.attendance_breakdown), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            if (!state.isPremium) {
                                Spacer(modifier = Modifier.width(8.dp))
                                PremiumLockedIcon()
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Present includes half days in the count, so subtract them for purely full-day slices
                        val purePresent = state.personalStats.present - state.personalStats.halfDays
                        val total = (purePresent + state.personalStats.absent + state.personalStats.halfDays).toFloat()
                        if (total > 0) {
                            val presentSweep = (purePresent / total) * 360f
                            val halfSweep = (state.personalStats.halfDays / total) * 360f
                            val absentSweep = (state.personalStats.absent / total) * 360f

                            val animatedPresentSweep by animateFloatAsState(targetValue = presentSweep, animationSpec = tween(1000))
                            val animatedHalfSweep by animateFloatAsState(targetValue = halfSweep, animationSpec = tween(1000))
                            val animatedAbsentSweep by animateFloatAsState(targetValue = absentSweep, animationSpec = tween(1000))

                            val presentColor = Color(0xFF39b27d)
                            val halfColor = Color(0xFFF97316)
                            val absentColor = MaterialTheme.colorScheme.error

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(120.dp).alpha(if (state.isPremium) 1f else 0.3f), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.size(100.dp)) {
                                        val strokeWidth = 24.dp.toPx()

                                        // Draw base circle
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
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.attendance), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.no_data), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (!isAllTime) {
            if (!state.isPremium) {
                item {
                    InlinePremiumCard(onUpgrade = onNavigateToPremium)
                }
            }

            item {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.all_time_stats), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.total_earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedCounter(targetValue = state.personalStats.allTimeEarnings.toInt(), prefix = "₹")
                        }
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.total_days), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun EarningsLineChart(dailyRecords: List<com.dailywork.attedance.viewmodel.DailyRecord>) {
    if (dailyRecords.isEmpty()) return

    val maxEarning = dailyRecords.maxOfOrNull { it.earnings } ?: 1.0
    val maxChartVal = if (maxEarning == 0.0) 1.0 else maxEarning * 1.2

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500)
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

            // Draw points
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

            // Tooltip
            selectedIndex?.let { index ->
                if (index < points.size) {
                    val point = points[index]
                    val record = dailyRecords[index]

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        isAntiAlias = true
                    }

                    val text = "₹${record.earnings.toInt()} (${record.dateStr.takeLast(2)})"
                    val textBounds = android.graphics.Rect()
                    textPaint.getTextBounds(text, 0, text.length, textBounds)

                    val tooltipWidth = textBounds.width() + 32f
                    val tooltipHeight = textBounds.height() + 24f

                    val tooltipX = point.x.coerceIn(tooltipWidth / 2, width - tooltipWidth / 2)
                    val tooltipY = (point.y - tooltipHeight - 16f).coerceAtLeast(0f)

                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        tooltipX - tooltipWidth / 2,
                        tooltipY,
                        tooltipX + tooltipWidth / 2,
                        tooltipY + tooltipHeight,
                        8f, 8f, bgPaint
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        tooltipX,
                        tooltipY + tooltipHeight / 2 + textBounds.height() / 2,
                        textPaint
                    )
                }
            }
        }
    }
}
