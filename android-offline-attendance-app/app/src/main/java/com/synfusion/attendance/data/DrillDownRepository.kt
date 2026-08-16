package com.synfusion.attendance.data

import android.content.Context
import com.synfusion.attendance.data.daos.AttendanceDao
import com.synfusion.attendance.data.entities.AttendanceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DrillDownType {
    PRESENT, ABSENT, HALF_DAY, OVERTIME, ADVANCE, EARNINGS
}

class DrillDownRepository(
    private val context: Context
) {
    private val attendanceDao: AttendanceDao
        get() = AttendanceDatabase.getInstance(context).attendanceDao()

    suspend fun getDrillDownRecords(
        type: DrillDownType,
        monthPrefix: String?, // Format: "YYYY-MM", or null for All Time
        isAllTime: Boolean
    ): List<AttendanceEntity> = withContext(Dispatchers.IO) {
        if (isAllTime || monthPrefix == null) {
            when (type) {
                DrillDownType.PRESENT -> attendanceDao.getAllTimePresent()
                DrillDownType.ABSENT -> attendanceDao.getAllTimeAbsent()
                DrillDownType.HALF_DAY -> attendanceDao.getAllTimeHalfDay()
                DrillDownType.OVERTIME -> attendanceDao.getAllTimeOvertime()
                DrillDownType.ADVANCE -> attendanceDao.getAllTimeAdvance()
                DrillDownType.EARNINGS -> attendanceDao.getAllTimeEarnings()
            }
        } else {
            val (startDate, endDate) = getMonthDateRange(monthPrefix)
            when (type) {
                DrillDownType.PRESENT -> attendanceDao.getPresentByMonth(startDate, endDate)
                DrillDownType.ABSENT -> attendanceDao.getAbsentByMonth(startDate, endDate)
                DrillDownType.HALF_DAY -> attendanceDao.getHalfDayByMonth(startDate, endDate)
                DrillDownType.OVERTIME -> attendanceDao.getOvertimeByMonth(startDate, endDate)
                DrillDownType.ADVANCE -> attendanceDao.getAdvanceByMonth(startDate, endDate)
                DrillDownType.EARNINGS -> attendanceDao.getEarningsByMonth(startDate, endDate)
            }
        }
    }

    companion object {
        fun getMonthDateRange(monthPrefix: String): Pair<String, String> {
            val parts = monthPrefix.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val startDate = String.format(java.util.Locale.US, "%04d-%02d-01", year, month)
            val nextYear = if (month == 12) year + 1 else year
            val nextMonth = if (month == 12) 1 else month + 1
            val endDate = String.format(java.util.Locale.US, "%04d-%02d-01", nextYear, nextMonth)
            return Pair(startDate, endDate)
        }
    }
}
