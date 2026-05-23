package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.viewmodel.WorkerDetailViewModel
import com.dailywork.attedance.viewmodel.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkerCalendarScreen(
    workerId: String,
    viewModel: WorkerDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    val currentMonthDate = state.selectedMonthDate
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

    // Group attendance by day using state.logs
    val dayMap = remember(state.logs) {
        val map = mutableMapOf<Int, AttendanceRecord>()
        val advances = mutableMapOf<Int, Double>()

        state.logs.forEach { log ->
            val recordParts = log.date.split("-")
            if (recordParts.size == 3) {
                val rYear = recordParts[0].toIntOrNull() ?: return@forEach
                val rMonth = (recordParts[1].toIntOrNull() ?: return@forEach) - 1
                val rDay = recordParts[2].toIntOrNull() ?: return@forEach

                if (rYear == year && rMonth == month) {
                    val adv = log.advanceAmount ?: 0.0
                    if (adv > 0) {
                        advances[rDay] = adv
                    }
                    if (log.status != "advance") {
                        map[rDay] = AttendanceRecord(
                            id = log.date,
                            date = log.date,
                            status = log.status,
                            type = log.type,
                            reason = null,
                            overtimeHours = log.overtimeHours,
                            note = log.note,
                            advanceAmount = adv
                        )
                    }
                }
            }
        }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.calendar), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(state.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeMonth(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = monthYearStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.changeMonth(1) }) {
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
                viewModel.markAttendance(dateStr, status, type, reason, overtime, note, advance)
                showDialog = false
            },
            onDelete = {
                viewModel.removeAttendance(dateStr)
                showDialog = false
            }
        )
    }
}
