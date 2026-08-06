package com.synfusion.attendance.data

import com.synfusion.attendance.data.daos.AttendanceDao
import com.synfusion.attendance.data.entities.AttendanceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DrillDownType {
    PRESENT, ABSENT, HALF_DAY, OVERTIME, ADVANCE, EARNINGS
}

class DrillDownRepository(
    private val attendanceDao: AttendanceDao
) {
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
            when (type) {
                DrillDownType.PRESENT -> attendanceDao.getPresentByMonth(monthPrefix)
                DrillDownType.ABSENT -> attendanceDao.getAbsentByMonth(monthPrefix)
                DrillDownType.HALF_DAY -> attendanceDao.getHalfDayByMonth(monthPrefix)
                DrillDownType.OVERTIME -> attendanceDao.getOvertimeByMonth(monthPrefix)
                DrillDownType.ADVANCE -> attendanceDao.getAdvanceByMonth(monthPrefix)
                DrillDownType.EARNINGS -> attendanceDao.getEarningsByMonth(monthPrefix)
            }
        }
    }
}
