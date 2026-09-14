package com.synfusion.attendance.utils

data class AllTimePdfRecord(
    val dateStr: String, // e.g. "01/10/2024"
    val workerName: String,
    val statusStr: String, // Present, Half Day, Absent, or localized
    val statusRaw: String, // "present", "absent" etc.
    val isHalfDay: Boolean,
    val baseEarnings: Double,
    val overtimeHours: Int,
    val overtimeAmount: Double,
    val advanceAmount: Double,
    val note: String
)

data class AllTimePdfMonthGroup(
    val monthName: String,
    val monthNumeric: String,
    val totalEarnings: Double,
    val totalSalary: Double,
    val totalOvertime: Double,
    val totalAdvance: Double,
    val presentCount: Int,
    val halfCount: Int,
    val absentCount: Int,
    val records: List<AllTimePdfRecord>
)

data class AllTimePdfYearGroup(
    val year: String,
    val months: List<AllTimePdfMonthGroup>
)

data class AllTimePassbookPdfModel(
    val workerName: String,
    val generatedAtStr: String,
    val firstRecordDateStr: String,
    val latestRecordDateStr: String,
    val totalRecordsCount: Int,
    val totalEarnings: Double,
    val totalSalary: Double,
    val totalOvertime: Double,
    val totalAdvance: Double,
    val totalPresent: Int,
    val totalHalfDays: Int,
    val totalAbsent: Int,
    val totalWorkingDays: Double,
    val amountToReceive: Double,
    val years: List<AllTimePdfYearGroup>
)
