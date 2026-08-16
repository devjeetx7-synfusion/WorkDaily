package com.synfusion.attendance.ui

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.synfusion.attendance.ui.components.ShimmerBox
import com.synfusion.attendance.ui.components.ShimmerCircle
import com.synfusion.attendance.ui.components.ShimmerTextLine
import com.synfusion.attendance.ui.components.ExpandableNoteText
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
import com.synfusion.attendance.viewmodel.OfflinePassbookState
import com.synfusion.attendance.utils.PassbookPdfGenerator
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.utils.PdfData
import com.synfusion.attendance.utils.PdfLog
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.PdfExportManager
import com.synfusion.attendance.utils.PdfExportState
import com.synfusion.attendance.ui.components.PdfExportLoadingDialog
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassbookScreen(
    viewModel: OfflinePassbookViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflinePassbookState(isLoading = true))
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearStr = sdfMonth.format(state.selectedMonthDate)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportManager = remember { PdfExportManager(context, viewModel.localAttendanceRepository) }
    val exportState by exportManager.exportState.collectAsStateWithLifecycle(initialValue = PdfExportState.Idle)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    DisposableEffect(Unit) {
        onDispose {
            exportManager.resetState()
        }
    }

    val isExporting = exportState != PdfExportState.Idle && exportState !is PdfExportState.Success && exportState !is PdfExportState.Error

    fun exportPDF() {
        if (isExporting) return
        scope.launch {
            exportManager.exportMonthlyPdf(state.selectedMonthDate) { file ->
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
                        context.startActivity(Intent.createChooser(intent, "Share PDF"))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "No app found to share PDF", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun shareViaWhatsApp() {
        if (isExporting) return
        scope.launch {
            exportManager.exportMonthlyPdf(state.selectedMonthDate) { file ->
                scope.launch {
                    kotlinx.coroutines.delay(600)
                    exportManager.resetState()

                    val officialPassbookLabel = context.getString(com.synfusion.attendance.R.string.pdf_subtitle_passbook)
                    val nameLabel = context.getString(com.synfusion.attendance.R.string.full_name)
                    val monthLabel = context.getString(com.synfusion.attendance.R.string.passbook_month)
                    val attendanceSummaryLabel = context.getString(com.synfusion.attendance.R.string.attendance_breakdown)
                    val presentLabel = context.getString(com.synfusion.attendance.R.string.present_1)
                    val halfDayLabel = context.getString(com.synfusion.attendance.R.string.half_day)
                    val absentLabel = context.getString(com.synfusion.attendance.R.string.absent_1)
                    val totalDaysWorkedLabel = context.getString(com.synfusion.attendance.R.string.total_man_days)
                    val financialSummaryLabel = context.getString(com.synfusion.attendance.R.string.financial_summary)
                    val dailyWageLabel = context.getString(com.synfusion.attendance.R.string.daily_wage)
                    val grossEarnedLabel = context.getString(com.synfusion.attendance.R.string.gross_earned)
                    val advanceDeductedLabel = context.getString(com.synfusion.attendance.R.string.total_advance)
                    val netPayableLabel = context.getString(com.synfusion.attendance.R.string.net_payable)
                    val appName = context.getString(com.synfusion.attendance.R.string.app_name)

                    val settlementMsg = state.settlement?.settlementMessage ?: if (state.finalBalance >= 0) "YOU WILL RECEIVE ₹${AmountFormatter.format(state.finalBalance)}" else "YOU NEED TO PAY BACK ₹${AmountFormatter.format(kotlin.math.abs(state.finalBalance))}"

                    val text = """
                        👷 *$officialPassbookLabel*

                        👤 *$nameLabel:* ${state.name}
                        📅 *$monthLabel:* $monthYearStr
                        📊 *$attendanceSummaryLabel:*
                        ✅ $presentLabel: ${state.presentDays}
                        🕒 $halfDayLabel: ${state.halfDays}
                        ❌ $absentLabel: ${state.absentDays}
                        📈 *$totalDaysWorkedLabel:* ${state.totalDailyWorks}

                        💰 *$financialSummaryLabel:*
                        💵 $dailyWageLabel: ₹${AmountFormatter.format(state.dailyWage)}
                        💸 $grossEarnedLabel: ₹${AmountFormatter.format(state.grossEarned)}
                        📉 $advanceDeductedLabel: ₹${AmountFormatter.format(state.totalAdvance)}
                        ━━━━━━━━━━━━━━━━━━
                        🏆 *$settlementMsg*
                        ━━━━━━━━━━━━━━━━━━

                        _Generated automatically via $appName App_ 📱
                    """.trimIndent()

                    val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, text)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, text)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        try {
                            context.startActivity(Intent.createChooser(fallbackIntent, "Share PDF"))
                        } catch (ex: Exception) {
                            android.widget.Toast.makeText(context, "No app found to share PDF", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

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
    val windowSizeInfo = LocalWindowSizeInfo.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        Crossfade(
            targetState = if (state.isLoading) "shimmer" else if (state.errorMessage != null) "error" else "content",
            animationSpec = tween(350),
            label = "passbook_crossfade"
        ) { pageState ->
            when (pageState) {
                "shimmer" -> {
                    PassbookShimmer()
                }
                "error" -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                            Text(state.errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                "content" -> {
                    ResponsiveContainer {
                        if (windowSizeInfo.isCompact) {
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
                                                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("₹${AmountFormatter.format(state.dailyWage)} / " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.day_singular), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                                                }
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.joined_date_label, state.joinedDate), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.mark_today), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay(), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        OutlinedButton(
                                            onClick = { exportPDF() },
                                            enabled = !isExporting,
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isExporting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.export_pdf), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Button(
                                            onClick = { shareViaWhatsApp() },
                                            enabled = !isExporting,
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isExporting) MaterialTheme.colorScheme.outline else Color(0xFF25D366))
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
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.worked_ratio_label, AmountFormatter.format(state.totalDailyWorks), state.passedDays), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("${state.presentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.present), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("${state.absentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("${state.halfDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
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
                                    val settlement = state.settlement
                                    val settlementState = settlement?.settlementState ?: if (state.finalBalance > 0) com.synfusion.attendance.utils.SettlementState.RECEIVE else if (state.finalBalance < 0) com.synfusion.attendance.utils.SettlementState.PAY_BACK else com.synfusion.attendance.utils.SettlementState.SETTLED

                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.financial_summary), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.gross_earned), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                                Text("₹${AmountFormatter.format(state.grossEarned)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_advance), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                                Text("- ₹${AmountFormatter.format(state.totalAdvance)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(12.dp))

                                            val statusColor = when (settlementState) {
                                                com.synfusion.attendance.utils.SettlementState.RECEIVE -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                                                com.synfusion.attendance.utils.SettlementState.PAY_BACK -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
                                                com.synfusion.attendance.utils.SettlementState.SETTLED -> MaterialTheme.colorScheme.primary
                                            }

                                            when (settlementState) {
                                                com.synfusion.attendance.utils.SettlementState.RECEIVE -> {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Amount To Receive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                        Text("₹${AmountFormatter.format(settlement?.amountToReceive ?: state.finalBalance)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = statusColor)
                                                    }
                                                }
                                                com.synfusion.attendance.utils.SettlementState.PAY_BACK -> {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Amount To Pay Back", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
                                                        Text("₹${AmountFormatter.format(settlement?.amountToPayBack ?: kotlin.math.abs(state.finalBalance))}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = statusColor)
                                                    }
                                                }
                                                com.synfusion.attendance.utils.SettlementState.SETTLED -> {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Settlement Status", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                        Text("SETTLED — ₹0 PENDING", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            val msg = settlement?.settlementMessage ?: if (state.finalBalance > 0) "YOU WILL RECEIVE ₹${AmountFormatter.format(state.finalBalance)}" else if (state.finalBalance < 0) "YOU NEED TO PAY BACK ₹${AmountFormatter.format(kotlin.math.abs(state.finalBalance))}" else "SETTLED — ₹0 PENDING"
                                            Text(
                                                text = msg,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
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
                                                        Text(otText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getLeave())
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.Top) {
                                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (log.status == "present" || log.status == "absent") {
                                                            val textColor = if (log.status == "present") {
                                                                if (log.type == "half") com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay() else com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                                                            } else {
                                                                com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
                                                            }
                                                            val text = if (log.status == "present") { if (log.type == "half") "Half Day" else "Present" } else "Absent"
                                                            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                        }

                                                        if (log.advanceAmount != null && log.advanceAmount > 0) {
                                                            Text("₹${AmountFormatter.format(log.advanceAmount)} " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
                                                        }
                                                    }
                                                }
                                            }

                                            if (!log.note.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                ExpandableNoteText(
                                                    text = "Note: ${log.note}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Left Column: User details, Actions & Month selector
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(), modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("₹${AmountFormatter.format(state.dailyWage)} / " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.day_singular), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                                                        }
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.joined_date_label, state.joinedDate), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.mark_today), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay(), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                OutlinedButton(
                                                    onClick = { exportPDF() },
                                                    enabled = !isExporting,
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isExporting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.export_pdf), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Button(
                                                    onClick = { shareViaWhatsApp() },
                                                    enabled = !isExporting,
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (isExporting) MaterialTheme.colorScheme.outline else Color(0xFF25D366))
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.whatsapp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

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

                                        // Right Column: Summary & Counts
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
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
                                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.worked_ratio_label, AmountFormatter.format(state.totalDailyWorks), state.passedDays), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("${state.presentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.present), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("${state.absentDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.absent), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("${state.halfDays}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
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

                                            val settlement = state.settlement
                                            val settlementState = settlement?.settlementState ?: if (state.finalBalance > 0) com.synfusion.attendance.utils.SettlementState.RECEIVE else if (state.finalBalance < 0) com.synfusion.attendance.utils.SettlementState.PAY_BACK else com.synfusion.attendance.utils.SettlementState.SETTLED

                                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)) {
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.financial_summary), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.gross_earned), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                                        Text("₹${AmountFormatter.format(state.grossEarned)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.total_advance), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                                        Text("- ₹${AmountFormatter.format(state.totalAdvance)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    val statusColor = when (settlementState) {
                                                        com.synfusion.attendance.utils.SettlementState.RECEIVE -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                                                        com.synfusion.attendance.utils.SettlementState.PAY_BACK -> com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
                                                        com.synfusion.attendance.utils.SettlementState.SETTLED -> MaterialTheme.colorScheme.primary
                                                    }

                                                    when (settlementState) {
                                                        com.synfusion.attendance.utils.SettlementState.RECEIVE -> {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                                Text("Amount To Receive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                                Text("₹${AmountFormatter.format(settlement?.amountToReceive ?: state.finalBalance)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = statusColor)
                                                            }
                                                        }
                                                        com.synfusion.attendance.utils.SettlementState.PAY_BACK -> {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                                Text("Amount To Pay Back", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
                                                                Text("₹${AmountFormatter.format(settlement?.amountToPayBack ?: kotlin.math.abs(state.finalBalance))}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = statusColor)
                                                            }
                                                        }
                                                        com.synfusion.attendance.utils.SettlementState.SETTLED -> {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                                Text("Settlement Status", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                                Text("SETTLED — ₹0 PENDING", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    val msg = settlement?.settlementMessage ?: if (state.finalBalance > 0) "YOU WILL RECEIVE ₹${AmountFormatter.format(state.finalBalance)}" else if (state.finalBalance < 0) "YOU NEED TO PAY BACK ₹${AmountFormatter.format(kotlin.math.abs(state.finalBalance))}" else "SETTLED — ₹0 PENDING"
                                                    Text(
                                                        text = msg,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
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
                                                        Text(otText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getLeave())
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.Top) {
                                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (log.status == "present" || log.status == "absent") {
                                                            val textColor = if (log.status == "present") {
                                                                if (log.type == "half") com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay() else com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                                                            } else {
                                                                com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
                                                            }
                                                            val text = if (log.status == "present") { if (log.type == "half") "Half Day" else "Present" } else "Absent"
                                                            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                        }

                                                        if (log.advanceAmount != null && log.advanceAmount > 0) {
                                                            Text("₹${AmountFormatter.format(log.advanceAmount)} " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay())
                                                        }
                                                    }
                                                }
                                            }

                                            if (!log.note.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                ExpandableNoteText(
                                                    text = "Note: ${log.note}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.fillMaxWidth()
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
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }

    PdfExportLoadingDialog(
        state = exportState,
        onCancel = { exportManager.resetState() },
        onDismissError = { exportManager.resetState() }
    )
}

@Composable
fun PassbookShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back and title row shimmer
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ShimmerCircle(size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            ShimmerTextLine(height = 24.dp, fraction = 0.5f)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Wage / Joined Card Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 64.dp, cornerRadius = 16.dp)

        // Buttons row Shimmer
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp, cornerRadius = 12.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp, cornerRadius = 12.dp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp, cornerRadius = 12.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp, cornerRadius = 12.dp)
        }

        // Month selector Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 56.dp, cornerRadius = 12.dp)

        // Attendance rate Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 110.dp, cornerRadius = 16.dp)

        // Statistics breakdown cards Shimmer
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 64.dp, cornerRadius = 12.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 64.dp, cornerRadius = 12.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 64.dp, cornerRadius = 12.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 64.dp, cornerRadius = 12.dp)
        }

        // Financial summary card Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 180.dp, cornerRadius = 16.dp)
    }
}
