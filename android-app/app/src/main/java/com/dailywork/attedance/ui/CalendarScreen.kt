package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.dailywork.attedance.utils.OvertimeCalculator
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailywork.attedance.viewmodel.AttendanceRecord
import com.dailywork.attedance.viewmodel.CalendarViewModel
import com.dailywork.attedance.viewmodel.Worker
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel
) {
    val state by viewModel.calendarState.collectAsState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        if (state.isLoading && state.role.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            if (state.role == "contractor") {
                ContractorCalendarView(viewModel = viewModel, state = state)
            } else {
                PersonalCalendarView(viewModel = viewModel, state = state)
            }
        }
        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ContractorCalendarView(viewModel: CalendarViewModel, state: com.dailywork.attedance.viewmodel.CalendarState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.mark_attendance), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.daily_attendance_for_all_workers), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Date Picker/Selector Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.changeContractorDate(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.selectedDate,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { viewModel.changeContractorDate(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                }
            }
        }

        if (state.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.workers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.no_workers_found_add_workers_first), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.workers) { worker ->
                WorkerAttendanceCard(
                    worker = worker,
                    attendance = state.contractorAttendance.find { it.id == "${worker.id}_${state.selectedDate}" && it.status != "advance" },
                    onMarkAttendance = { status, type ->
                        viewModel.markContractorAttendance(worker.id, status, type)
                    }
                )
            }
        }
    }
}

@Composable
fun WorkerAttendanceCard(
    worker: Worker,
    attendance: AttendanceRecord?,
    onMarkAttendance: (String, String) -> Unit
) {
    val currentStatus = attendance?.status
    val currentType = attendance?.type

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = worker.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = worker.workType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Present (Full Day)
                val isPresentFull = currentStatus == "present" && currentType == "full"
                OutlinedButton(
                    onClick = { onMarkAttendance("present", "full") },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isPresentFull) Color(0xFF22C55E) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isPresentFull) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPresentFull) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.present_1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Half Day
                val isPresentHalf = currentStatus == "present" && currentType == "half"
                OutlinedButton(
                    onClick = { onMarkAttendance("present", "half") },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isPresentHalf) Color(0xFF86EFAC) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isPresentHalf) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPresentHalf) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.half_day), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Absent
                val isAbsent = currentStatus == "absent"
                OutlinedButton(
                    onClick = { onMarkAttendance("absent", "full") },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isAbsent) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.absent_1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalCalendarView(viewModel: CalendarViewModel, state: com.dailywork.attedance.viewmodel.CalendarState) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

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

    // Group attendance by day
    val dayMap = remember(state.personalAttendance) {
        val map = mutableMapOf<Int, AttendanceRecord>()
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

        // Merge advances into the main records for display purposes
        val mergedMap = mutableMapOf<Int, AttendanceRecord>()
        for (day in 1..daysInMonth) {
            val record = map[day]
            val adv = advances[day]
            if (record != null || adv != null) {
                mergedMap[day] = record?.copy(advanceAmount = adv) ?: AttendanceRecord(
                    id = "", date = String.format("%04d-%02d-%02d", year, month + 1, day),
                    status = "advance", type = null, reason = null, overtimeHours = null, note = null, advanceAmount = adv
                )
            }
        }
        mergedMap
    }

    val presentCount = dayMap.values.count { it.status == "present" }
    val absentCount = dayMap.values.count { it.status == "absent" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.calendar), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Month Selector
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
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.changePersonalMonth(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Month stats
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
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.present_1), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.absent_1), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.tap_any_day_to_edit_attendance_or_add_no_msg),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            // Day Labels
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
            // Calendar Grid
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
                                    val isPresent = data?.status == "present"
                                    val isAbsent = data?.status == "absent"
                                    val isHalf = data?.type == "half"
                                    val hasAdvance = data?.advanceAmount != null && data.advanceAmount > 0
                                    val isToday = isCurrentMonth && day == todayDay

                                    val bgColor = when {
                                        isPresent && isHalf -> Color(0xFFF97316)
                                        isPresent -> MaterialTheme.colorScheme.primary
                                        isAbsent -> MaterialTheme.colorScheme.error
                                        else -> Color.Transparent
                                    }

                                    val textColor = when {
                                        isPresent || isAbsent -> Color.White
                                        isToday -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    }

                                    val hasOT = data?.overtimeHours != null && data.overtimeHours > 0

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .clickable {
                                                selectedDay = day
                                                showDialog = true
                                            }
                                            .border(
                                                width = if (hasOT) 2.dp else if (isToday && !isPresent && !isAbsent) 2.dp else 0.dp,
                                                color = if (hasOT) Color(0xFF8B5CF6) else if (isToday && !isPresent && !isAbsent) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        if (isHalf) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onPrimary)
                                            )
                                        }
                                        if (hasAdvance) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 6.dp)
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF97316)) // Orange
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.full_day), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFF97316)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.half_day), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.absent_1), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFF97316)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.advance), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showDialog && selectedDay != null) {
        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, selectedDay!!)
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
    existingRecord: AttendanceRecord?,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(displayDate, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.tap_to_edit_attendance_or_add_noteadvanc_msg), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { editStatus = "present" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editStatus == "present") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (editStatus == "present") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.present_1), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { editStatus = "absent" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editStatus == "absent") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (editStatus == "absent") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.absent_1), fontWeight = FontWeight.Bold)
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
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.full_day), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.half_day), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.overtime_hours), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                        onValueChange = { editOTAmount = it },
                        placeholder = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.ot_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else if (editStatus == "absent") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(100.dp) // Approximate height for 2 rows
                    ) {
                        items(absenceReasons) { reason ->
                            val displayReason = reason.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                            Button(
                                onClick = { editReason = reason },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editReason == reason) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (editReason == reason) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(displayReason, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.advance_payment), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editAdvance,
                    onValueChange = { editAdvance = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.add_note)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
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
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.save))
            }
        },
        dismissButton = {
            if (existingRecord != null) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.remove))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.cancel))
                }
            }
        }
    )
}
