package com.synfusion.attendance.utils

import com.synfusion.attendance.data.entities.AttendanceEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class FinancialReconciliationTest {

    private val defaultWage = 600.0

    @Test
    fun testMandatoryReconciliationRules() {
        val records = listOf(
            AttendanceEntity(id = "1", workerId = null, date = "2025-05-01", status = "present", type = "full", wage = defaultWage),
            AttendanceEntity(id = "1_adv", workerId = null, date = "2025-05-01", status = "advance", type = "full", advanceAmount = 200.0),
            AttendanceEntity(id = "2", workerId = null, date = "2025-05-02", status = "present", type = "half", wage = defaultWage),
            AttendanceEntity(id = "3", workerId = null, date = "2025-05-03", status = "present", type = "full", overtimeHours = 4, note = "Urgent shift [OT_WAGE_350.0]", wage = defaultWage),
            AttendanceEntity(id = "4", workerId = null, date = "2025-05-04", status = "absent", type = "full", wage = defaultWage),
            AttendanceEntity(id = "5_adv", workerId = null, date = "2025-05-05", status = "advance", type = "full", advanceAmount = 150.25)
        )

        val monthStr = "2025-05"
        val passbook = AttendanceCalculationEngine.calculatePassbook(records, defaultWage)
        val stats = AttendanceCalculationEngine.calculateStats(records, monthStr, defaultWage)

        // Mandatory Reconciliation Rule 1: Sum(Daily Earnings) == Period Earnings
        val sumDailyEarnings = passbook.logs.sumOf { it.baseEarnings + it.overtimeAmount }
        assertEquals("Sum(Daily Earnings) must equal Total Earnings", passbook.totalEarnings, sumDailyEarnings, 0.0001)

        // Mandatory Reconciliation Rule 2: Sum(Passbook Earnings) == Statistics Earnings
        assertEquals("Sum(Passbook Earnings) must equal Statistics Earnings", passbook.totalEarnings, stats.totalEarnings, 0.0001)

        // Mandatory Reconciliation Rule 3: Sum(Valid Advances) == Total Advances
        val sumAdvances = records.filter { it.status == "advance" }.sumOf { it.advanceAmount }
        assertEquals("Sum(Valid Advances) must equal Total Advances in Passbook", passbook.totalAdv, sumAdvances, 0.0001)
        assertEquals("Sum(Valid Advances) must equal Total Advances in Stats", stats.advanceTotal, sumAdvances, 0.0001)

        // Mandatory Reconciliation Rule 4: Net Earnings == Gross Earnings - Advances - Deductions
        val expectedNet = passbook.totalEarnings - passbook.totalAdv
        assertEquals("Net Earnings must equal Gross Earnings - Advances", passbook.netBalance, expectedNet, 0.0001)

        // Mandatory Reconciliation Rule 5: Displayed Amount == Central Calculation Amount
        val formattedGross = AmountFormatter.format(passbook.totalEarnings)
        val formattedNet = AmountFormatter.format(passbook.netBalance)
        assertEquals("Formatted Gross Earnings mismatch", "1,850", formattedGross)
        assertEquals("Formatted Net Earnings mismatch", "1,499.75", formattedNet)
    }

    @Test
    fun testRealWorldCoreRequirementScenario() {
        // Test setup matching real-world scenario specified in prompt:
        // July: 10 July 2026 -> Advance ₹2,500; 10 OT hours = ₹1,000 (at daily wage 800 -> hourly rate 100)
        // August: 16 August 2026 -> Advance ₹200; 12 OT hours = ₹1,200
        val testWage = 800.0 // 800/8 = 100 per OT hour

        val records = listOf(
            AttendanceEntity(id = "july_att", workerId = null, date = "2026-07-10", status = "present", type = "full", overtimeHours = 10, advanceAmount = 2500.0, wage = testWage),
            AttendanceEntity(id = "aug_adv", workerId = null, date = "2026-08-16", status = "advance", type = "full", advanceAmount = 200.0, wage = testWage),
            AttendanceEntity(id = "aug_att", workerId = null, date = "2026-08-16_att", status = "present", type = "full", overtimeHours = 12, wage = testWage)
        )

        val julyStats = AttendanceCalculationEngine.calculateStats(records, "2026-07", testWage)
        val augStats = AttendanceCalculationEngine.calculateStats(records, "2026-08", testWage)

        // July Monthly Checks
        assertEquals(2500.0, julyStats.advanceTotal, 0.0001)
        assertEquals(10, julyStats.overtime)
        assertEquals(1800.0, julyStats.totalEarnings, 0.0001) // 800 base + 1000 OT

        // August Monthly Checks
        assertEquals(200.0, augStats.advanceTotal, 0.0001)
        assertEquals(12, augStats.overtime)
        assertEquals(2000.0, augStats.totalEarnings, 0.0001) // 800 base + 1200 OT

        // All-Time Checks (Consistent across any month view selection)
        assertEquals(2700.0, julyStats.allTimeAdvanceTotal, 0.0001)
        assertEquals(22, julyStats.allTimeOvertime)
        assertEquals(3800.0, julyStats.allTimeEarnings, 0.0001)
        assertEquals(2, julyStats.allTimePresent)
        assertEquals(0, julyStats.allTimeAbsent)
        assertEquals(0, julyStats.allTimeHalfDays)

        assertEquals(2700.0, augStats.allTimeAdvanceTotal, 0.0001)
        assertEquals(22, augStats.allTimeOvertime)
        assertEquals(3800.0, augStats.allTimeEarnings, 0.0001)
        assertEquals(2, augStats.allTimePresent)
        assertEquals(0, augStats.allTimeAbsent)
        assertEquals(0, augStats.allTimeHalfDays)

        // Summary vs Drill-Down Reconciliation Test for All-Time Advances
        val drillDownAllTimeAdvRecords = records.filter { it.advanceAmount > 0 }
        val drillDownAllTimeAdvSum = drillDownAllTimeAdvRecords.sumOf { it.advanceAmount }
        assertEquals("Summary card All-Time advance must equal sum of drill-down records", augStats.allTimeAdvanceTotal, drillDownAllTimeAdvSum, 0.0001)

        // Summary vs Drill-Down Reconciliation Test for August Monthly Advances
        val (augStart, augEnd) = com.synfusion.attendance.data.DrillDownRepository.getMonthDateRange("2026-08")
        val drillDownAugAdvRecords = records.filter { it.date >= augStart && it.date < augEnd && it.advanceAmount > 0 }
        val drillDownAugAdvSum = drillDownAugAdvRecords.sumOf { it.advanceAmount }
        assertEquals("Summary card August advance must equal sum of August drill-down records", augStats.advanceTotal, drillDownAugAdvSum, 0.0001)
    }

    @Test
    fun testAmountFormatterPrecision() {
        assertEquals("0", AmountFormatter.format(0.0))
        assertEquals("1", AmountFormatter.format(1.0))
        assertEquals("1.01", AmountFormatter.format(1.01))
        assertEquals("999.99", AmountFormatter.format(999.99))
        assertEquals("1,000", AmountFormatter.format(1000.0))
        assertEquals("1,00,000", AmountFormatter.format(100000.0))
        assertEquals("-150.25", AmountFormatter.format(-150.25))
    }
}
