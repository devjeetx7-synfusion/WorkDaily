package com.synfusion.attendance.utils

import android.content.Context
import com.synfusion.attendance.data.LocalAttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExportManager(
    private val context: Context,
    private val localAttendanceRepository: LocalAttendanceRepository
) {
    private val _exportState = MutableStateFlow<PdfExportState>(PdfExportState.Idle)
    val exportState: StateFlow<PdfExportState> = _exportState

    @Volatile
    private var isExporting = false

    fun resetState() {
        isExporting = false
        _exportState.value = PdfExportState.Idle
    }

    suspend fun exportMonthlyPdf(selectedMonthDate: Date, onShare: (File) -> Unit) {
        if (isExporting) return
        isExporting = true
        _exportState.value = PdfExportState.Preparing

        try {
            _exportState.value = PdfExportState.CollectingRecords
            val user = withContext(Dispatchers.IO) { localAttendanceRepository.getUser() }
            val cachedDefaultWage = user?.dailyWage ?: 500.0
            val name = user?.name ?: "User"
            val workType = user?.role ?: "Worker"

            val records = withContext(Dispatchers.IO) { localAttendanceRepository.getPersonalAttendance() }

            _exportState.value = PdfExportState.GeneratingPdf

            // Format dates
            val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val sdfMonthNumeric = SimpleDateFormat("MM", Locale.getDefault())
            val sdfYearNumeric = SimpleDateFormat("yyyy", Locale.getDefault())
            val monthYearStr = sdfMonth.format(selectedMonthDate)
            val monthNumericStr = sdfMonthNumeric.format(selectedMonthDate)
            val yearNumericStr = sdfYearNumeric.format(selectedMonthDate)

            // Select only records matching the selected month
            val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(selectedMonthDate)
            val monthlyRecords = records.filter { it.date.startsWith(currentMonthPrefix) }

            val calcResult = withContext(Dispatchers.Default) {
                AttendanceCalculationEngine.calculatePassbook(monthlyRecords, cachedDefaultWage)
            }

            // Map logs to PdfLog
            val rsStr = context.getString(com.synfusion.attendance.R.string.pdf_rs)
            val hrsStr = context.getString(com.synfusion.attendance.R.string.pdf_hrs)
            val halfDayStr = context.getString(com.synfusion.attendance.R.string.half_day)
            val presentStr = context.getString(com.synfusion.attendance.R.string.present_1)
            val absentStr = context.getString(com.synfusion.attendance.R.string.absent_1)

            val sortedLogs = calcResult.logs.sortedBy { it.date }.map { log ->
                val advanceAmt = log.advanceAmount ?: 0.0

                val otPdfText = if (log.overtimeAmount > 0) {
                    if (log.overtimeHours > 0) "${log.overtimeHours} $hrsStr / $rsStr ${AmountFormatter.format(log.overtimeAmount)}"
                    else "$rsStr ${AmountFormatter.format(log.overtimeAmount)}"
                } else "-"

                val statusText = if (log.status == "present") {
                    if (log.type == "half") halfDayStr else presentStr
                } else if (log.status == "absent") {
                    absentStr
                } else "-"

                PdfLog(
                    date = log.date.split("-").reversed().joinToString("/"),
                    status = statusText,
                    workType = workType,
                    dailyWage = "$rsStr ${AmountFormatter.format(cachedDefaultWage)}",
                    overtime = otPdfText,
                    advanceAmount = if (log.status == "advance" || advanceAmt > 0) "$rsStr ${AmountFormatter.format(advanceAmt)}" else "-",
                    runningBalance = "", // populated below
                    note = log.note ?: ""
                )
            }

            // populate running balance
            var currentRunningBalance = 0.0
            val sortedLogsWithBalance = calcResult.logs.sortedBy { it.date }.mapIndexed { idx, log ->
                val dailyEarned = log.baseEarnings
                val advanceAmt = log.advanceAmount ?: 0.0
                val otAmt = log.overtimeAmount
                currentRunningBalance += (dailyEarned + otAmt) - advanceAmt

                val origPdfLog = sortedLogs[idx]
                origPdfLog.copy(runningBalance = "$rsStr ${AmountFormatter.format(currentRunningBalance)}")
            }

            val calendar = java.util.Calendar.getInstance()
            calendar.time = selectedMonthDate
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val firstDay = calendar.time
            calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            val lastDay = calendar.time

            val sdfPeriod = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val periodStr = "${sdfPeriod.format(firstDay)} — ${sdfPeriod.format(lastDay)}"
            val generatedAtStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            val pdfData = PdfData(
                title = "WorkDaily",
                subtitle = context.getString(com.synfusion.attendance.R.string.pdf_subtitle_passbook),
                name = name,
                userId = "personal",
                role = workType,
                monthYearStr = monthYearStr,
                monthNumericStr = monthNumericStr,
                yearNumericStr = yearNumericStr,
                contractorName = "Admin",
                dailyWage = cachedDefaultWage,
                totalManDays = calcResult.totalWorks,
                totalPresent = calcResult.present,
                totalHalfDays = calcResult.half,
                totalAbsent = calcResult.absent,
                totalOvertimeHours = calcResult.logs.sumOf { it.overtimeHours },
                totalOvertimeEarnings = calcResult.logs.sumOf { it.overtimeAmount },
                grossEarned = calcResult.totalEarnings,
                advanceDeducted = calcResult.totalAdv,
                netPayable = calcResult.netBalance,
                periodStr = periodStr,
                generatedAtStr = generatedAtStr,
                logs = sortedLogsWithBalance
            )

            _exportState.value = PdfExportState.Finalizing
            val file = withContext(Dispatchers.IO) {
                PassbookPdfGenerator.generatePdf(context, pdfData)
            }

            if (file != null && validatePdfFile(file)) {
                _exportState.value = PdfExportState.Success(file)
                onShare(file)
            } else {
                if (file != null && file.exists()) {
                    file.delete()
                }
                _exportState.value = PdfExportState.Error("PDF validation failed or file generation failed.")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                _exportState.value = PdfExportState.Idle
                throw e
            }
            _exportState.value = PdfExportState.Error(e.localizedMessage ?: "Unknown error occurred")
        } finally {
            isExporting = false
        }
    }

    suspend fun exportAllTimePdf(onShare: (File) -> Unit) {
        if (isExporting) return
        isExporting = true
        _exportState.value = PdfExportState.Preparing

        try {
            _exportState.value = PdfExportState.CollectingRecords
            val user = withContext(Dispatchers.IO) { localAttendanceRepository.getUser() }
            val cachedDefaultWage = user?.dailyWage ?: 500.0
            val name = user?.name ?: "User"

            val records = withContext(Dispatchers.IO) { localAttendanceRepository.getPersonalAttendance() }

            if (records.isEmpty()) {
                _exportState.value = PdfExportState.Error(context.getString(com.synfusion.attendance.R.string.no_records_found_dots))
                isExporting = false
                return
            }

            _exportState.value = PdfExportState.GeneratingPdf

            val sortedRecords = records.sortedBy { it.date }
            val firstRecordDateStr = sortedRecords.firstOrNull()?.date?.split("-")?.reversed()?.joinToString("/") ?: "-"
            val latestRecordDateStr = sortedRecords.lastOrNull()?.date?.split("-")?.reversed()?.joinToString("/") ?: "-"

            val calcResult = withContext(Dispatchers.Default) {
                AttendanceCalculationEngine.calculatePassbook(records, cachedDefaultWage)
            }

            // Calculate overall totals
            val totalSalary = calcResult.logs.sumOf { it.baseEarnings }
            val totalOt = calcResult.logs.sumOf { it.overtimeAmount }
            val totalAdv = calcResult.totalAdv
            val totalEarnings = calcResult.totalEarnings // Salary + OT
            val totalPresent = calcResult.present
            val totalHalfDays = calcResult.half
            val totalAbsent = calcResult.absent
            val totalWorkingDays = calcResult.totalWorks

            // Build chronological Year -> Month -> Days
            val groupedByYear = calcResult.logs.groupBy { it.date.substring(0, 4) }
            val yearGroups = groupedByYear.map { (year, yearLogs) ->
                val groupedByMonth = yearLogs.groupBy { it.date.substring(5, 7) }
                val monthGroups = groupedByMonth.map { (monthNum, monthLogs) ->
                    val monthName = getMonthName(monthNum.toInt())

                    var mEarnings = 0.0
                    var mAdvance = 0.0
                    var mOvertime = 0.0
                    var mPresent = 0
                    var mAbsent = 0
                    var mHalf = 0

                    monthLogs.forEach { log ->
                        mEarnings += log.baseEarnings
                        mAdvance += (log.advanceAmount ?: 0.0)
                        mOvertime += log.overtimeAmount
                        if (log.status == "present") {
                            mPresent++
                            if (log.type == "half") mHalf++
                        } else if (log.status == "absent") {
                            mAbsent++
                        }
                    }

                    val monthRecordsMapped = monthLogs.sortedBy { it.date }.map { log ->
                        val statusText = if (log.status == "present") {
                            if (log.type == "half") context.getString(com.synfusion.attendance.R.string.half_day)
                            else context.getString(com.synfusion.attendance.R.string.present_1)
                        } else if (log.status == "absent") {
                            context.getString(com.synfusion.attendance.R.string.absent_1)
                        } else "-"

                        AllTimePdfRecord(
                            dateStr = log.date.split("-").reversed().joinToString("/"),
                            workerName = name,
                            statusStr = statusText,
                            statusRaw = log.status,
                            isHalfDay = log.type == "half",
                            baseEarnings = log.baseEarnings,
                            overtimeHours = log.overtimeHours,
                            overtimeAmount = log.overtimeAmount,
                            advanceAmount = log.advanceAmount ?: 0.0,
                            note = log.note ?: ""
                        )
                    }

                    AllTimePdfMonthGroup(
                        monthName = monthName,
                        monthNumeric = monthNum,
                        totalEarnings = mEarnings + mOvertime,
                        totalSalary = mEarnings,
                        totalOvertime = mOvertime,
                        totalAdvance = mAdvance,
                        presentCount = mPresent,
                        halfCount = mHalf,
                        absentCount = mAbsent,
                        records = monthRecordsMapped
                    )
                }.sortedBy { it.monthNumeric }

                AllTimePdfYearGroup(
                    year = year,
                    months = monthGroups
                )
            }.sortedBy { it.year }

            val generatedAtStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            val model = AllTimePassbookPdfModel(
                workerName = name,
                generatedAtStr = generatedAtStr,
                firstRecordDateStr = firstRecordDateStr,
                latestRecordDateStr = latestRecordDateStr,
                totalRecordsCount = records.size,
                totalEarnings = totalEarnings,
                totalSalary = totalSalary,
                totalOvertime = totalOt,
                totalAdvance = totalAdv,
                totalPresent = totalPresent,
                totalHalfDays = totalHalfDays,
                totalAbsent = totalAbsent,
                totalWorkingDays = totalWorkingDays,
                years = yearGroups
            )

            _exportState.value = PdfExportState.Finalizing

            val file = withContext(Dispatchers.IO) {
                PassbookPdfGenerator.generateAllTimePdf(context, model)
            }

            if (file != null && validatePdfFile(file)) {
                _exportState.value = PdfExportState.Success(file)
                onShare(file)
            } else {
                if (file != null && file.exists()) {
                    file.delete()
                }
                _exportState.value = PdfExportState.Error("PDF validation failed or file generation failed.")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                _exportState.value = PdfExportState.Idle
                throw e
            }
            _exportState.value = PdfExportState.Error(e.localizedMessage ?: "Unknown error occurred")
        } finally {
            isExporting = false
        }
    }

    private fun validatePdfFile(file: File): Boolean {
        if (!file.exists()) return false
        if (file.length() == 0L) return false

        return try {
            val fis = FileInputStream(file)
            val headerBytes = ByteArray(4)
            val bytesRead = fis.read(headerBytes)
            fis.close()
            if (bytesRead == 4) {
                val headerString = String(headerBytes)
                headerString == "%PDF"
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getMonthName(month: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.MONTH, month - 1)
        val sdf = SimpleDateFormat("MMMM", Locale.getDefault())
        return sdf.format(cal.time)
    }
}
