package com.synfusion.attendance.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.viewmodel.OfflinePassbookViewModel
import com.synfusion.attendance.viewmodel.OfflinePassbookLog
import com.synfusion.attendance.utils.PassbookPdfGenerator
import com.synfusion.attendance.utils.PdfData
import com.synfusion.attendance.utils.PdfLog
import com.synfusion.attendance.utils.AmountFormatter
import androidx.core.content.FileProvider
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassbookScreen(
    viewModel: OfflinePassbookViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val sdfMonthNumeric = SimpleDateFormat("MM", Locale.getDefault())
    val sdfYearNumeric = SimpleDateFormat("yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)
    val monthNumericStr = sdfMonthNumeric.format(state.selectedMonthDate)
    val yearNumericStr = sdfYearNumeric.format(state.selectedMonthDate)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    fun generatePdfData(): PdfData {
        var currentRunningBalance = 0.0
        var totalOTHours = 0
        var totalOTEarnings = 0.0

        val sortedLogs = state.logs.sortedBy { it.date }.map { log ->
            val dailyEarned = log.baseEarnings
            val advanceAmt = log.advanceAmount ?: 0.0
            val otAmt = log.overtimeAmount

            totalOTHours += log.overtimeHours
            totalOTEarnings += otAmt

            currentRunningBalance += (dailyEarned + otAmt) - advanceAmt

            val otPdfText = if (log.overtimeAmount > 0) {
                if (log.overtimeHours > 0) "${log.overtimeHours} hrs / Rs.${AmountFormatter.format(log.overtimeAmount)}"
                else "Rs.${AmountFormatter.format(log.overtimeAmount)}"
            } else "-"

            PdfLog(
                date = log.date.split("-").reversed().joinToString("/"),
                status = if (log.status == "present") if (log.type == "half") "Half Day" else "Present" else if (log.status == "absent") "Absent" else "-",
                workType = state.workType,
                dailyWage = "Rs. ${AmountFormatter.format(state.dailyWage)}",
                overtime = otPdfText,
                advanceAmount = if (log.status == "advance" || advanceAmt > 0) "Rs. ${AmountFormatter.format(advanceAmt)}" else "-",
                runningBalance = "Rs. ${AmountFormatter.format(currentRunningBalance)}",
                note = log.note ?: ""
            )
        }

        return PdfData(
            title = "WorkDaily",
            subtitle = "Official Worker Passbook",
            name = state.name,
            userId = "personal",
            role = state.workType,
            monthYearStr = monthYearStr,
            monthNumericStr = monthNumericStr,
            yearNumericStr = yearNumericStr,
            contractorName = "Admin",
            dailyWage = state.dailyWage,
            totalManDays = state.totalDailyWorks,
            totalPresent = state.presentDays,
            totalHalfDays = state.halfDays,
            totalOvertimeHours = totalOTHours,
            totalOvertimeEarnings = totalOTEarnings,
            grossEarned = state.grossEarned,
            advanceDeducted = state.totalAdvance,
            netPayable = state.finalBalance,
            logs = sortedLogs
        )
    }

    fun exportPDF() {
        val file = PassbookPdfGenerator.generatePdf(context, generatePdfData())
        if (file != null) {
            val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Open PDF"))
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "No PDF viewer found", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareViaWhatsApp() {
        val text = """
            👷 *Official Worker Passbook*

            👤 *Name:* ${state.name}
            📅 *Month:* $monthYearStr
            📊 *Attendance Summary:*
            ✅ Present: ${state.presentDays} days
            🕒 Half Days: ${state.halfDays} days
            ❌ Absent: ${state.absentDays} days
            📈 *Total Man Days:* ${state.totalDailyWorks}

            💰 *Financial Summary:*
            💵 Daily Wage: ₹${AmountFormatter.format(state.dailyWage)}
            💸 Gross Earned: ₹${AmountFormatter.format(state.grossEarned)}
            📉 Advance Deducted: ₹${AmountFormatter.format(state.totalAdvance)}
            ━━━━━━━━━━━━━━━━━━
            🏆 *Net Payable: ₹${AmountFormatter.format(state.finalBalance)}*
            ━━━━━━━━━━━━━━━━━━

            _Generated automatically via WorkDaily App_ 📱
        """.trimIndent()

        val file = PassbookPdfGenerator.generatePdf(context, generatePdfData())
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (file != null) {
                val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (file != null) {
                    val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", file)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                putExtra(Intent.EXTRA_TEXT, text)
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, "Share PDF"))
            } catch (ex: Exception) {
                android.widget.Toast.makeText(context, "No app found to share PDF", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(state.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("₹${AmountFormatter.format(state.dailyWage)} / day", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF16A34A))
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Joined ${state.joinedDate}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.mark_today), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { exportPDF() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.export_pdf), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { shareViaWhatsApp() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.whatsapp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.passbook_month), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", modifier = Modifier.size(20.dp)) }
                            Text(text = monthYearStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(20.dp)) }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.attendance_rate), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text("${state.attendanceRate}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(progress = state.attendanceRate / 100f, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Worked ${state.totalDailyWorks} out of ${state.passedDays} passed days this month", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${state.presentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.present), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${state.absentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${state.halfDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.half), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${state.totalDailyWorks}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.man_days), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.financial_summary), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.gross_earned), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text("₹${AmountFormatter.format(state.grossEarned)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_advance), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text("- ₹${AmountFormatter.format(state.totalAdvance)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.net_payable), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("₹${AmountFormatter.format(state.finalBalance)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (state.finalBalance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFDC2626))
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.detailed_ledger), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (state.logs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.no_records_found_for_this_month), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(state.logs) { log ->
                        val dateParts = log.date.split("-").reversed()
                        val displayDate = if (dateParts.size == 3) "${dateParts[0]}/${dateParts[1]}/${dateParts[2]}" else log.date
                        var expanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayDate, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                                    if (log.overtimeAmount > 0) {
                                        val otText = if (log.overtimeHours > 0) "+ ₹${AmountFormatter.format(log.overtimeAmount)} (${log.overtimeHours} hrs OT)" else "+ ₹${AmountFormatter.format(log.overtimeAmount)} (OT)"
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(otText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                    }
                                }

                                Row(verticalAlignment = Alignment.Top) {
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (log.status == "present" || log.status == "absent") {
                                            val textColor = if (log.status == "present") Color(0xFF16A34A) else Color(0xFFDC2626)
                                            val text = if (log.status == "present") { if (log.type == "half") "Half Day" else "Present" } else "Absent"
                                            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                        }

                                        if (log.advanceAmount != null && log.advanceAmount > 0) {
                                            Text("₹${AmountFormatter.format(log.advanceAmount)} Advance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                                        }
                                    }

                                    if (!log.note.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { expanded = !expanded },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand note",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!log.note.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: ${log.note}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
