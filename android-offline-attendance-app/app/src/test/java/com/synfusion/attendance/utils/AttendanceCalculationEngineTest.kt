package com.synfusion.attendance.utils

import com.synfusion.attendance.data.entities.AttendanceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculationEngineTest {

    private val wage = 500.0

    @Test
    fun testSettlementScenarios() {
        // Scenario 1: Earned = 10000, Advance = 12000
        val s1 = AttendanceCalculationEngine.calculateSettlement(
            regularEarnings = 8000.0,
            overtimeEarnings = 2000.0,
            totalAdvanceReceived = 12000.0
        )
        assertEquals(-2000.0, s1.netSettlement, 0.0001)
        assertEquals(0.0, s1.amountToReceive, 0.0001)
        assertEquals(2000.0, s1.amountToPayBack, 0.0001)
        assertEquals(SettlementState.PAY_BACK, s1.settlementState)
        assertEquals("YOU NEED TO PAY BACK ₹2,000.00", s1.settlementMessage)

        // Scenario 2: Earned = 10000, Advance = 2000
        val s2 = AttendanceCalculationEngine.calculateSettlement(
            regularEarnings = 8000.0,
            overtimeEarnings = 2000.0,
            totalAdvanceReceived = 2000.0
        )
        assertEquals(8000.0, s2.netSettlement, 0.0001)
        assertEquals(8000.0, s2.amountToReceive, 0.0001)
        assertEquals(0.0, s2.amountToPayBack, 0.0001)
        assertEquals(SettlementState.RECEIVE, s2.settlementState)
        assertEquals("YOU WILL RECEIVE ₹8,000.00", s2.settlementMessage)

        // Scenario 3: Earned = 10000, Advance = 10000
        val s3 = AttendanceCalculationEngine.calculateSettlement(
            regularEarnings = 8000.0,
            overtimeEarnings = 2000.0,
            totalAdvanceReceived = 10000.0
        )
        assertEquals(0.0, s3.netSettlement, 0.0001)
        assertEquals(0.0, s3.amountToReceive, 0.0001)
        assertEquals(0.0, s3.amountToPayBack, 0.0001)
        assertEquals(SettlementState.SETTLED, s3.settlementState)
        assertEquals("SETTLED — ₹0.00 PENDING", s3.settlementMessage)
    }

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
    fun testAdvanceRecordedOnPresentAndAdvanceStatuses() {
        // Test case reflecting real bug case:
        // 10 July 2026 -> ₹2,500 advance recorded on attendance record (status = "present")
        // 16 August 2026 -> ₹200 advance recorded on separate advance record (status = "advance")
        val records = listOf(
            AttendanceEntity(id = "1", workerId = null, date = "2026-07-10", status = "present", type = "full", advanceAmount = 2500.0, wage = wage),
            AttendanceEntity(id = "2", workerId = null, date = "2026-08-16", status = "advance", type = "full", advanceAmount = 200.0, wage = wage)
        )

        // July stats
        val julyStats = AttendanceCalculationEngine.calculateStats(records, "2026-07", wage)
        assertEquals(2500.0, julyStats.advanceTotal, 0.0001)

        // August stats
        val augStats = AttendanceCalculationEngine.calculateStats(records, "2026-08", wage)
        assertEquals(200.0, augStats.advanceTotal, 0.0001)

        // DrillDown date range testing for August 2026
        val (augStart, augEnd) = com.synfusion.attendance.data.DrillDownRepository.getMonthDateRange("2026-08")
        assertEquals("2026-08-01", augStart)
        assertEquals("2026-09-01", augEnd)

        // Filter August records using ISO range date >= augStart AND date < augEnd
        val augRecords = records.filter { it.date >= augStart && it.date < augEnd }
        assertEquals(1, augRecords.size)
        assertEquals("2026-08-16", augRecords[0].date)
        assertEquals(200.0, augRecords[0].advanceAmount, 0.0001)
    }

    @Test
    fun testMonthDateRangeBoundaries() {
        val (start, end) = com.synfusion.attendance.data.DrillDownRepository.getMonthDateRange("2026-08")
        assertEquals("2026-08-01", start)
        assertEquals("2026-09-01", end)

        // Verify day 1, middle, and last day of month are included
        val aug1 = "2026-08-01"
        val aug16 = "2026-08-16"
        val aug31 = "2026-08-31"
        val sep1 = "2026-09-01"

        assertTrue(AttendanceCalculationEngine.isDateInMonth(aug1, "2026-08"))
        assertTrue(AttendanceCalculationEngine.isDateInMonth(aug16, "2026-08"))
        assertTrue(AttendanceCalculationEngine.isDateInMonth(aug31, "2026-08"))
        assertFalse(AttendanceCalculationEngine.isDateInMonth(sep1, "2026-08"))
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
