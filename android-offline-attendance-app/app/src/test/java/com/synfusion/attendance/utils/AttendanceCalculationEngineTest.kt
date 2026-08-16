package com.synfusion.attendance.utils

import com.synfusion.attendance.data.entities.AttendanceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AttendanceCalculationEngineTest {

    private val wage = 500.0

    @Test
    fun testZeroAndSmallAmounts() {
        val records = listOf(
            AttendanceEntity(id = "1", workerId = null, date = "2025-02-01", status = "present", type = "full", wage = wage),
            AttendanceEntity(id = "2", workerId = null, date = "2025-02-01_adv", status = "advance", type = "full", advanceAmount = 0.0),
            AttendanceEntity(id = "3", workerId = null, date = "2025-02-02_adv", status = "advance", type = "full", advanceAmount = 1.01)
        )

        val passbook = AttendanceCalculationEngine.calculatePassbook(records, wage)

        assertEquals(500.0, passbook.totalEarnings, 0.0001)
        assertEquals(1.01, passbook.totalAdv, 0.0001)
        assertEquals(498.99, passbook.netBalance, 0.0001)
    }

    @Test
    fun testHalfDayAndOvertimeCustomWage() {
        val records = listOf(
            // Half day present: base 250.0
            AttendanceEntity(id = "1", workerId = null, date = "2025-02-10", status = "present", type = "half", wage = wage),
            // Present with OT and custom wage note: base 500.0, OT 999.99
            AttendanceEntity(id = "2", workerId = null, date = "2025-02-11", status = "present", type = "full", overtimeHours = 2, note = "Worked extra [OT_WAGE_999.99]", wage = wage)
        )

        val passbook = AttendanceCalculationEngine.calculatePassbook(records, wage)

        assertEquals(2, passbook.present)
        assertEquals(1, passbook.half)
        // Earnings = 250.0 + 500.0 + 999.99 = 1749.99
        assertEquals(1749.99, passbook.totalEarnings, 0.0001)
    }

    @Test
    fun testOvertimeStandardRateCalculation() {
        // Daily wage 500 -> Hourly rate = 500 / 8 = 62.5
        // 3 hours OT = 187.5
        val otAmount = OvertimeCalculator.calculateOvertimeAmount(500.0, 3)
        assertEquals(187.5, otAmount, 0.0001)
    }

    @Test
    fun testLeapYearAndMonthBoundaries() {
        val records = listOf(
            AttendanceEntity(id = "1", workerId = null, date = "2024-02-28", status = "present", type = "full", wage = wage),
            AttendanceEntity(id = "2", workerId = null, date = "2024-02-29", status = "present", type = "full", wage = wage), // Leap day 2024
            AttendanceEntity(id = "3", workerId = null, date = "2024-03-01", status = "present", type = "half", wage = wage)
        )

        val febStats = AttendanceCalculationEngine.calculateStats(records, "2024-02", wage)
        assertEquals(2, febStats.present)
        assertEquals(1000.0, febStats.totalEarnings, 0.0001)

        val marStats = AttendanceCalculationEngine.calculateStats(records, "2024-03", wage)
        assertEquals(1, marStats.present)
        assertEquals(1, marStats.halfDays)
        assertEquals(250.0, marStats.totalEarnings, 0.0001)
    }

    @Test
    fun testMultipleAdvancesOnSameAndDifferentDays() {
        val records = listOf(
            AttendanceEntity(id = "1", workerId = null, date = "2025-02-15", status = "present", type = "full", wage = wage),
            AttendanceEntity(id = "1_adv1", workerId = null, date = "2025-02-15", status = "advance", type = "full", advanceAmount = 100.0),
            AttendanceEntity(id = "1_adv2", workerId = null, date = "2025-02-15", status = "advance", type = "full", advanceAmount = 50.50),
            AttendanceEntity(id = "2_adv1", workerId = null, date = "2025-02-16", status = "advance", type = "full", advanceAmount = 200.0)
        )

        val passbook = AttendanceCalculationEngine.calculatePassbook(records, wage)

        assertEquals(350.50, passbook.totalAdv, 0.0001)
        assertEquals(500.0, passbook.totalEarnings, 0.0001)
        assertEquals(149.50, passbook.netBalance, 0.0001)

        val feb15Log = passbook.logs.find { it.date == "2025-02-15" }
        assertNotNull(feb15Log)
        assertEquals(150.50, feb15Log!!.advanceAmount!!, 0.0001)
    }

    @Test
    fun testMonthlyCalendarDaysAndExactFinancialTotals() {
        val records = listOf(
            AttendanceEntity(id = "1", workerId = null, date = "2024-02-01", status = "present", type = "full", wage = wage), // 500.0
            AttendanceEntity(id = "2", workerId = null, date = "2024-02-15", status = "present", type = "half", wage = wage), // 250.0
            AttendanceEntity(id = "3", workerId = null, date = "2024-02-29", status = "present", type = "full", overtimeHours = 2, wage = wage) // 500.0 + 125.0 = 625.0
        )

        val stats = AttendanceCalculationEngine.calculateStats(records, "2024-02", wage)

        // Feb 2024 is leap year -> 29 days
        assertEquals(29, stats.dailyRecords.size)
        assertEquals(1375.0, stats.totalEarnings, 0.0001)

        val dailySum = stats.dailyRecords.sumOf { it.earnings }
        assertEquals(stats.totalEarnings, dailySum, 0.0001)

        val passbook = AttendanceCalculationEngine.calculatePassbook(records, wage)
        assertEquals(passbook.totalEarnings, stats.totalEarnings, 0.0001)

        val feb29Record = stats.dailyRecords.find { it.dateStr == "2024-02-29" }
        assertNotNull(feb29Record)
        assertEquals(625.0, feb29Record!!.earnings, 0.0001)
        assertEquals(500.0, feb29Record.baseEarnings, 0.0001)
        assertEquals(125.0, feb29Record.overtimeAmount, 0.0001)
        assertEquals(2, feb29Record.overtimeHours)
    }

    @Test
    fun testAllTimeDailyRecordsExactTotals() {
        val records = listOf(
            AttendanceEntity(id = "1", workerId = null, date = "2023-12-31", status = "present", type = "full", wage = wage), // 500.0
            AttendanceEntity(id = "2", workerId = null, date = "2024-01-15", status = "present", type = "half", wage = wage), // 250.0
            AttendanceEntity(id = "3", workerId = null, date = "2024-02-10", status = "present", type = "full", overtimeHours = 4, wage = wage) // 500.0 + 250.0 = 750.0
        )

        val stats = AttendanceCalculationEngine.calculateStats(records, "2024-01", wage)

        assertEquals(1500.0, stats.allTimeEarnings, 0.0001)
        val allTimeDailySum = stats.allTimeDailyRecords.sumOf { it.earnings }
        assertEquals(stats.allTimeEarnings, allTimeDailySum, 0.0001)

        val passbook = AttendanceCalculationEngine.calculatePassbook(records, wage)
        assertEquals(passbook.totalEarnings, stats.allTimeEarnings, 0.0001)
    }
}
