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
