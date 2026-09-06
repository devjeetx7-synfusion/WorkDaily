package com.synfusion.attendance.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import com.synfusion.attendance.ui.components.ShimmerBox
import com.synfusion.attendance.ui.components.ShimmerCircle
import com.synfusion.attendance.ui.components.ShimmerTextLine
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.synfusion.attendance.utils.OvertimeCalculator
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.viewmodel.OfflineAttendanceRecord
import com.synfusion.attendance.viewmodel.OfflineCalendarViewModel
import com.synfusion.attendance.utils.IndianCurrencyVisualTransformation
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.ExperimentalMaterial3Api
import com.synfusion.attendance.ui.components.WorkDailyPullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: OfflineCalendarViewModel,
    scrollState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    val state by viewModel.calendarState.collectAsStateWithLifecycle(initialValue = com.synfusion.attendance.viewmodel.OfflineCalendarState(isLoading = true))

    WorkDailyPullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Crossfade(
            targetState = if (state.isLoading) "shimmer" else if (state.errorMessage != null) "error" else "content",
            animationSpec = tween(350),
            label = "calendar_crossfade"
        ) { pageState ->
            when (pageState) {
                "shimmer" -> {
                    CalendarShimmer()
                }
                "error" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                    PersonalCalendarView(viewModel = viewModel, state = state, scrollState = scrollState)
                }
            }
        }
    }
}

@Composable
fun PersonalCalendarView(
    viewModel: OfflineCalendarViewModel,
    state: com.synfusion.attendance.viewmodel.OfflineCalendarState,
    scrollState: androidx.compose.foundation.lazy.LazyListState
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }

    val currentMonthDate = state.currentMonthDate
    val cal = Calendar.getInstance()
    cal.time = currentMonthDate
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(currentMonthDate)

    val todayCal = Calendar.getInstance()
    val isCurrentMonth = todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) == month
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val dayMap = remember(state.personalAttendance) {
        val map = mutableMapOf<Int, OfflineAttendanceRecord>()
        val advances = mutableMapOf<Int, Double>()

        state.personalAttendance.forEach { record ->
            val recordParts = record.date.split("-")
            if (recordParts.size == 3) {
                val rYear = recordParts[0].toIntOrNull() ?: return@forEach
                val rMonth = (recordParts[1].toIntOrNull() ?: return@forEach) - 1
                val rDay = recordParts[2].toIntOrNull() ?: return@forEach

                if (rYear == year && rMonth == month) {
                    if (record.status == "advance") {
                        advances[rDay] = (advances[rDay] ?: 0.0) + (record.advanceAmount ?: 0.0)
                    } else {
                        map[rDay] = record
                    }
                }
            }
        }

        val mergedMap = mutableMapOf<Int, OfflineAttendanceRecord>()
        for (day in 1..daysInMonth) {
            val record = map[day]
            val adv = advances[day]
            if (record != null || adv != null) {
                mergedMap[day] = record?.copy(advanceAmount = adv) ?: OfflineAttendanceRecord(
                    id = "", date = String.format("%04d-%02d-%02d", year, month + 1, day),
                    status = "advance", type = null, reason = null, overtimeHours = null, note = null, advanceAmount = adv
                )
            }
        }
        mergedMap
    }

    val presentCount = dayMap.values.count { it.status == "present" }
    val absentCount = dayMap.values.count { it.status == "absent" }
    val windowSizeInfo = LocalWindowSizeInfo.current

    ResponsiveContainer {
        if (windowSizeInfo.isCompact) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, end = 16.dp, bottom = 96.dp)
            ) {
                item {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.calendar), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    var showCalPicker by remember { mutableStateOf(false) }
                    if (showCalPicker) {
                        com.synfusion.attendance.ui.components.MonthYearPickerDialog(
                            initialDate = currentMonthDate,
                            onMonthYearSelected = { newDate ->
                                viewModel.setPersonalMonth(newDate)
                            },
                            onDismiss = { showCalPicker = false }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.changePersonalMonth(-1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }
                        Text(
                            text = monthYearStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showCalPicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        IconButton(onClick = { viewModel.changePersonalMonth(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$presentCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.present_1), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$absentCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent_1), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.tap_any_day_to_edit_attendance_or_add_no_msg),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        dayLabels.forEach { d ->
                            Text(
                                text = d,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    val totalCells = firstDayOfWeek + daysInMonth
                    val rows = Math.ceil(totalCells / 7.0).toInt()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    val day = cellIndex - firstDayOfWeek + 1

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (day in 1..daysInMonth) {
                                            val data = dayMap[day]
                                            val isToday = isCurrentMonth && day == todayDay
                                            CalendarDateCell(
                                                day = day,
                                                data = data,
                                                isToday = isToday,
                                                onClick = {
                                                    selectedDay = day
                                                    showDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_day), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.half_day), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent_1), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.calendar), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: Calendar Grid & Month selector
                        Card(
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                var showCalPickerWide by remember { mutableStateOf(false) }
                                if (showCalPickerWide) {
                                    com.synfusion.attendance.ui.components.MonthYearPickerDialog(
                                        initialDate = currentMonthDate,
                                        onMonthYearSelected = { newDate ->
                                            viewModel.setPersonalMonth(newDate)
                                        },
                                        onDismiss = { showCalPickerWide = false }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.changePersonalMonth(-1) }) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                                    }
                                    Text(
                                        text = monthYearStr,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showCalPickerWide = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    IconButton(onClick = { viewModel.changePersonalMonth(1) }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    dayLabels.forEach { d ->
                                        Text(
                                            text = d,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                val totalCells = firstDayOfWeek + daysInMonth
                                val rows = Math.ceil(totalCells / 7.0).toInt()

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    for (row in 0 until rows) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            for (col in 0..6) {
                                                val cellIndex = row * 7 + col
                                                val day = cellIndex - firstDayOfWeek + 1

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (day in 1..daysInMonth) {
                                                        val data = dayMap[day]
                                                        val isToday = isCurrentMonth && day == todayDay
                                                        CalendarDateCell(
                                                day = day,
                                                data = data,
                                                isToday = isToday,
                                                onClick = {
                                                                selectedDay = day
                                                                showDialog = true
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column: Counters, Details & Legend
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Monthly Overview",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$presentCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.present_1), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$absentCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent_1), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Text(
                                        androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.tap_any_day_to_edit_attendance_or_add_no_msg),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.legend), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_day), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.half_day), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent_1), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedDay != null) {
        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, selectedDay!!)
        val existingRecord = dayMap[selectedDay!!]
        PersonalAttendanceDialog(
            displayDate = "$selectedDay ${SimpleDateFormat("MMMM", Locale.getDefault()).format(currentMonthDate)} $year",
            existingRecord = existingRecord,

            onDismiss = { showDialog = false },
            onSave = { status, type, reason, overtime, note, advance ->
                viewModel.markPersonalAttendance(dateStr, status, type, reason, overtime, note, advance)
                showDialog = false
            },
            onDelete = {
                viewModel.removePersonalAttendance(dateStr)
                showDialog = false
            }
        )
    }
}

@Composable
fun PersonalAttendanceDialog(
    displayDate: String,
    existingRecord: OfflineAttendanceRecord?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, Double) -> Unit,
    onDelete: () -> Unit
) {
    var editStatus by remember { mutableStateOf(if (existingRecord?.status == "advance" || existingRecord == null) "present" else existingRecord.status) }
    var editType by remember { mutableStateOf(existingRecord?.type ?: "full") }
    var editReason by remember { mutableStateOf(existingRecord?.reason ?: "sick") }
    var editOT by remember { mutableStateOf(existingRecord?.overtimeHours ?: 0) }
    var editOTAmount by remember {
        mutableStateOf(existingRecord?.note?.let { OvertimeCalculator.extractCustomAmount(it)?.toInt()?.toString() } ?: "")
    }
    var editNote by remember {
        mutableStateOf(existingRecord?.note?.let { OvertimeCalculator.cleanNote(it) } ?: "")
    }
    var editAdvance by remember { mutableStateOf((existingRecord?.advanceAmount ?: 0.0).toInt().toString()) }

    val absenceReasons = listOf("sick", "family", "travel", "other")

    // Smooth Entrance Sequence (250-400ms)
    var isDialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(displayDate) {
        isDialogVisible = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(displayDate, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.tap_to_edit_attendance_or_add_noteadvanc_msg),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            AnimatedVisibility(
                visible = isDialogVisible,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 280.dp)
                        .heightIn(max = 480.dp)
                        .verticalScroll(scrollState)
                ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { editStatus = "present" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editStatus == "present") com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent() else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (editStatus == "present") Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.present_1), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { editStatus = "absent" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editStatus == "absent") com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent() else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (editStatus == "absent") Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent_1), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (editStatus == "present") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { editType = "full" },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editType == "full") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (editType == "full") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_day), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { editType = "half" },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editType == "half") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (editType == "half") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.half_day), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime_hours), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { if (editOT > 0) editOT-- },
                                contentAlignment = Alignment.Center
                            ) { Text("-", fontWeight = FontWeight.Bold) }
                            Text(editOT.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { editOT++ },
                                contentAlignment = Alignment.Center
                            ) { Text("+", fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editOTAmount,
                        onValueChange = { editOTAmount = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.ot_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        visualTransformation = IndianCurrencyVisualTransformation()
                    )
                } else if (editStatus == "absent") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        items(absenceReasons) { reason ->
                            val displayReason = reason.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                            Button(
                                onClick = { editReason = reason },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editReason == reason) com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent() else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (editReason == reason) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(displayReason, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_payment), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editAdvance,
                    onValueChange = { editAdvance = it.filter { ch -> ch.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    visualTransformation = IndianCurrencyVisualTransformation()
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.add_note)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val advanceVal = editAdvance.toDoubleOrNull() ?: 0.0
                    val finalNote = if (editOTAmount.isNotBlank() && editStatus == "present") {
                        val otStr = "[OT_WAGE_${editOTAmount.trim()}]"
                        if (editNote.isNotBlank()) "$otStr $editNote" else otStr
                    } else {
                        editNote
                    }
                    onSave(editStatus, editType, editReason, editOT, finalNote, advanceVal)
                }
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.save))
            }
        },
        dismissButton = {
            if (existingRecord != null) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.remove))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel))
                }
            }
        }
    )
}

@Composable
fun CalendarDateCell(
    day: Int,
    data: OfflineAttendanceRecord?,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPresent = data?.status == "present"
    val isAbsent = data?.status == "absent"
    val isHalf = data?.type == "half"
    val hasAdvance = data?.advanceAmount != null && data.advanceAmount > 0
    val hasOT = data?.overtimeHours != null && data.overtimeHours > 0

    val defaultBgColor = when {
        isPresent && isHalf -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()
        isPresent -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
        isAbsent -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
        else -> Color.Transparent
    }

    val textColor = when {
        isPresent || isAbsent -> Color.White
        isToday -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    // Border hierarchy for attendance rings and today indicators
    val borderStrokeWidth = when {
        isPresent || isAbsent || isHalf -> 2.5.dp
        hasOT -> 2.dp
        isToday -> 2.dp
        else -> 0.dp
    }

    val borderStrokeColor = when {
        isPresent && isHalf -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay()
        isPresent -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
        isAbsent -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
        hasOT -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getLeave()
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .then(
                Modifier.background(defaultBgColor)
            )
            .clickable(onClick = onClick)
            .border(
                width = borderStrokeWidth,
                color = borderStrokeColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // CENTERED DATE NUMBER
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            style = LocalTextStyle.current
        )

        // Subtle attendance indicator badges (Half-day / Advance)
        if (isHalf) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
            )
        }

        if (hasAdvance) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
            )
        }
    }
}

@Composable
fun CalendarShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerTextLine(height = 24.dp, fraction = 0.3f)
        Spacer(modifier = Modifier.height(16.dp))

        // Month selector Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerCircle(size = 40.dp)
            ShimmerTextLine(height = 20.dp, fraction = 0.4f)
            ShimmerCircle(size = 40.dp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Preset / Absent counts Shimmer
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp, cornerRadius = 12.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp, cornerRadius = 12.dp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Grid Shimmer
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 5) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (j in 0..6) {
                        ShimmerBox(modifier = Modifier.weight(1f).aspectRatio(1f), height = 40.dp, cornerRadius = 100.dp)
                    }
                }
            }
        }
    }
}
