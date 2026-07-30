package com.synfusion.attendance.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
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
    onNavigatePassbook: () -> Unit = {}
) {
    val state by viewModel.statsState.collectAsStateWithLifecycle(initialValue = OfflineStatsState(isLoading = true))

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Monthly", "All-Time")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.statistics), fontWeight = FontWeight.Bold) },
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
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        Text(state.errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
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

                    PersonalStatsView(viewModel = viewModel, state = state, isAllTime = selectedTabIndex == 1, onNavigatePassbook = onNavigatePassbook)
                }
            }
        }
    }
}

@Composable
fun PersonalStatsView(viewModel: OfflineStatsViewModel, state: OfflineStatsState, isAllTime: Boolean, onNavigatePassbook: () -> Unit = {}) {
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
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCounter(targetValue = currentEarnings.toInt(), prefix = "₹")
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp)) {
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
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp)) {
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_overtime), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedCounter(targetValue = state.personalStats.overtime, suffix = " hrs")
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp)) {
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
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp)) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_earnings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedCounter(targetValue = state.personalStats.allTimeEarnings.toInt(), prefix = "₹")
                        }
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp)) {
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
