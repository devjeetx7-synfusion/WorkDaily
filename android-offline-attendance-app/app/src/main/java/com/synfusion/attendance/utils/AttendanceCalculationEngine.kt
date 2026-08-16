package com.synfusion.attendance.utils

import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.viewmodel.OfflinePassbookLog
import com.synfusion.attendance.viewmodel.OfflinePersonalStatsData
import com.synfusion.attendance.viewmodel.OfflineDailyRecord
import java.math.BigDecimal
import java.math.RoundingMode

data class DashboardCalculationResult(
    val todayEarned: Double,
    val monthEarned: Double,
    val currentTodayStatus: String?,
    val currentTodayType: String?,
    val currentOvertime: Int,
    val currentNote: String?,
    val advanceToday: Double
)

enum class SettlementState {
    RECEIVE,
    PAY_BACK,
    SETTLED
}

data class FinancialSettlementResult(
    val totalEarned: Double,
    val regularEarnings: Double,
    val overtimeEarnings: Double,
    val totalAdvanceReceived: Double,
    val totalDeductions: Double,
    val netSettlement: Double,
    val amountToReceive: Double,
    val amountToPayBack: Double,
    val settlementState: SettlementState,
    val settlementMessage: String
)

data class PassbookCalculationResult(
    val present: Int,
    val absent: Int,
    val half: Int,
    val totalAdv: Double,
    val totalEarnings: Double,
    val netBalance: Double,
    val totalWorks: Double,
    val settlement: FinancialSettlementResult,
    val logs: List<OfflinePassbookLog>
)

object AttendanceCalculationEngine {

    fun calculateSettlement(
        regularEarnings: Double,
        overtimeEarnings: Double,
        totalAdvanceReceived: Double,
        totalDeductions: Double = 0.0
    ): FinancialSettlementResult {
        val regBd = toMoney(regularEarnings)
        val otBd = toMoney(overtimeEarnings)
        val totalEarnedBd = regBd.add(otBd)
        val advBd = toMoney(totalAdvanceReceived)
        val dedBd = toMoney(totalDeductions)

        val netSettlementBd = totalEarnedBd.subtract(advBd).subtract(dedBd)
        val netVal = netSettlementBd.toDouble()

        val zeroBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

        return when {
            netSettlementBd.compareTo(zeroBd) > 0 -> {
                val amtReceive = netSettlementBd.toDouble()
                FinancialSettlementResult(
                    totalEarned = totalEarnedBd.toDouble(),
                    regularEarnings = regBd.toDouble(),
                    overtimeEarnings = otBd.toDouble(),
                    totalAdvanceReceived = advBd.toDouble(),
                    totalDeductions = dedBd.toDouble(),
                    netSettlement = netVal,
                    amountToReceive = amtReceive,
                    amountToPayBack = 0.0,
                    settlementState = SettlementState.RECEIVE,
                    settlementMessage = "YOU WILL RECEIVE ₹${AmountFormatter.format(amtReceive)}"
                )
            }
            netSettlementBd.compareTo(zeroBd) < 0 -> {
                val amtPayBack = netSettlementBd.abs().toDouble()
                FinancialSettlementResult(
                    totalEarned = totalEarnedBd.toDouble(),
                    regularEarnings = regBd.toDouble(),
                    overtimeEarnings = otBd.toDouble(),
                    totalAdvanceReceived = advBd.toDouble(),
                    totalDeductions = dedBd.toDouble(),
                    netSettlement = netVal,
                    amountToReceive = 0.0,
                    amountToPayBack = amtPayBack,
                    settlementState = SettlementState.PAY_BACK,
                    settlementMessage = "YOU NEED TO PAY BACK ₹${AmountFormatter.format(amtPayBack)}"
                )
            }
            else -> {
                FinancialSettlementResult(
                    totalEarned = totalEarnedBd.toDouble(),
                    regularEarnings = regBd.toDouble(),
                    overtimeEarnings = otBd.toDouble(),
                    totalAdvanceReceived = advBd.toDouble(),
                    totalDeductions = dedBd.toDouble(),
                    netSettlement = 0.0,
                    amountToReceive = 0.0,
                    amountToPayBack = 0.0,
                    settlementState = SettlementState.SETTLED,
                    settlementMessage = "SETTLED — ₹0 PENDING"
                )
            }
        }
    }

    val ROUNDING_MODE: RoundingMode = RoundingMode.HALF_UP

    private fun toMoney(value: Double): BigDecimal {
        return BigDecimal.valueOf(value).setScale(2, ROUNDING_MODE)
    }

    private fun toMoney(value: Long): BigDecimal {
        return BigDecimal.valueOf(value).setScale(2, ROUNDING_MODE)
    }

    fun isDateInMonth(date: String, monthPrefix: String): Boolean {
        return try {
            val (startDate, endDate) = com.synfusion.attendance.data.DrillDownRepository.getMonthDateRange(monthPrefix)
            date >= startDate && date < endDate
        } catch (e: Exception) {
            date.startsWith(monthPrefix)
        }
    }

    fun calculateDashboardStats(
        records: List<AttendanceEntity>,
        todayStr: String,
        currentMonthStr: String,
        defaultWage: Double
    ): DashboardCalculationResult {
        var todayEarnedBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var monthEarnedBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var currentTodayStatus: String? = null
        var currentTodayType: String? = null
        var currentOvertime = 0
        var currentNote: String? = null
        var advanceTodayBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

        val wageBd = toMoney(defaultWage)
        val halfWageBd = wageBd.divide(BigDecimal.valueOf(2), 2, ROUNDING_MODE)

        for (record in records) {
            val date = record.date
            val status = record.status
            val type = record.type
            val otHours = record.overtimeHours
            val noteStr = record.note
            val advanceAmtBd = toMoney(record.advanceAmount)

            val dailyBaseBd = if (status == "present") {
                if (type == "half") halfWageBd else wageBd
            } else BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

            val dailyOtBd = if (status == "present") {
                OvertimeCalculator.calculateOvertimeAmountBigDecimal(defaultWage, otHours, noteStr)
            } else BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

            val dailyEarnedBd = dailyBaseBd.add(dailyOtBd)

            if (date == todayStr) {
                if (status != "advance") {
                    currentTodayStatus = status
                    currentTodayType = type
                    currentOvertime = otHours
                    currentNote = OvertimeCalculator.cleanNote(noteStr)
                    todayEarnedBd = dailyEarnedBd
                }
                if (advanceAmtBd.compareTo(BigDecimal.ZERO) > 0) {
                    advanceTodayBd = advanceTodayBd.add(advanceAmtBd)
                }
            }

            if (isDateInMonth(date, currentMonthStr)) {
                monthEarnedBd = monthEarnedBd.add(dailyEarnedBd)
            }
        }

        return DashboardCalculationResult(
            todayEarned = todayEarnedBd.toDouble(),
            monthEarned = monthEarnedBd.toDouble(),
            currentTodayStatus = currentTodayStatus,
            currentTodayType = currentTodayType,
            currentOvertime = currentOvertime,
            currentNote = currentNote,
            advanceToday = advanceTodayBd.toDouble()
        )
    }

    fun calculateStats(
        records: List<AttendanceEntity>,
        currentMonthStr: String,
        defaultWage: Double
    ): OfflinePersonalStatsData {
        var allTimeEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var allTimeBaseEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var allTimeOtEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var allTimeDays = 0
        var allTimeOvertime = 0
        var allTimeAdvanceTotalBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var allTimePresent = 0
        var allTimeAbsent = 0
        var allTimeHalfDays = 0

        var totalEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var baseEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var otEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var present = 0
        var absent = 0
        var halfDays = 0
        var overtime = 0
        var advanceTotalBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

        val monthDailyMap = mutableMapOf<String, OfflineDailyRecord>()
        val allTimeDailyMap = mutableMapOf<String, OfflineDailyRecord>()

        var hasRecordsInPeriod = false
        var hasAllTimeRecords = false

        val wageBd = toMoney(defaultWage)
        val halfWageBd = wageBd.divide(BigDecimal.valueOf(2), 2, ROUNDING_MODE)

        for (rec in records) {
            val date = rec.date
            val isPresent = rec.status == "present"
            val isHalf = rec.type == "half"
            val ot = rec.overtimeHours
            val noteStr = rec.note
            val advanceAmtBd = toMoney(rec.advanceAmount)

            val baseCostBd = if (isPresent) (if (isHalf) halfWageBd else wageBd) else BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
            val otAmtBd = if (isPresent) OvertimeCalculator.calculateOvertimeAmountBigDecimal(defaultWage, ot, noteStr) else BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
            val totalCostValBd = baseCostBd.add(otAmtBd)

            if (rec.status != "advance") {
                hasAllTimeRecords = true
                val attendanceVal = if (isPresent) (if (isHalf) 0.5 else 1.0) else 0.0
                val existing = allTimeDailyMap[date]
                if (existing != null) {
                    val newEarningsBd = toMoney(existing.earnings).add(totalCostValBd)
                    val newBaseBd = toMoney(existing.baseEarnings).add(baseCostBd)
                    val newOtBd = toMoney(existing.overtimeAmount).add(otAmtBd)
                    allTimeDailyMap[date] = existing.copy(
                        earnings = newEarningsBd.toDouble(),
                        baseEarnings = newBaseBd.toDouble(),
                        overtimeAmount = newOtBd.toDouble(),
                        overtimeHours = existing.overtimeHours + ot,
                        attendance = existing.attendance + attendanceVal
                    )
                } else {
                    allTimeDailyMap[date] = OfflineDailyRecord(
                        dateStr = date,
                        earnings = totalCostValBd.toDouble(),
                        baseEarnings = baseCostBd.toDouble(),
                        overtimeAmount = otAmtBd.toDouble(),
                        overtimeHours = ot,
                        attendance = attendanceVal
                    )
                }
            }

            if (isPresent) {
                allTimeDays++
                allTimePresent++
                if (isHalf) allTimeHalfDays++
                allTimeBaseEarningsBd = allTimeBaseEarningsBd.add(baseCostBd)
                allTimeOtEarningsBd = allTimeOtEarningsBd.add(otAmtBd)
                allTimeEarningsBd = allTimeEarningsBd.add(totalCostValBd)
                allTimeOvertime += ot
            } else if (rec.status == "absent") {
                allTimeAbsent++
            }

            if (advanceAmtBd.compareTo(BigDecimal.ZERO) > 0) {
                allTimeAdvanceTotalBd = allTimeAdvanceTotalBd.add(advanceAmtBd)
            }

            if (isDateInMonth(date, currentMonthStr)) {
                if (rec.status != "advance") {
                    hasRecordsInPeriod = true
                }
                if (isPresent) {
                    present++
                    baseEarningsBd = baseEarningsBd.add(baseCostBd)
                    otEarningsBd = otEarningsBd.add(otAmtBd)
                    totalEarningsBd = totalEarningsBd.add(totalCostValBd)
                    if (isHalf) halfDays++
                    overtime += ot
                } else if (rec.status == "absent") {
                    absent++
                }

                if (advanceAmtBd.compareTo(BigDecimal.ZERO) > 0) {
                    advanceTotalBd = advanceTotalBd.add(advanceAmtBd)
                }

                if (rec.status != "advance") {
                    val attendanceVal = if (isPresent) (if (isHalf) 0.5 else 1.0) else 0.0
                    val existing = monthDailyMap[date]
                    if (existing != null) {
                        val newEarningsBd = toMoney(existing.earnings).add(totalCostValBd)
                        val newBaseBd = toMoney(existing.baseEarnings).add(baseCostBd)
                        val newOtBd = toMoney(existing.overtimeAmount).add(otAmtBd)
                        monthDailyMap[date] = existing.copy(
                            earnings = newEarningsBd.toDouble(),
                            baseEarnings = newBaseBd.toDouble(),
                            overtimeAmount = newOtBd.toDouble(),
                            overtimeHours = existing.overtimeHours + ot,
                            attendance = existing.attendance + attendanceVal
                        )
                    } else {
                        monthDailyMap[date] = OfflineDailyRecord(
                            dateStr = date,
                            earnings = totalCostValBd.toDouble(),
                            baseEarnings = baseCostBd.toDouble(),
                            overtimeAmount = otAmtBd.toDouble(),
                            overtimeHours = ot,
                            attendance = attendanceVal
                        )
                    }
                }
            }
        }

        // Fill all calendar days for currentMonthStr
        val filledMonthRecords = mutableListOf<OfflineDailyRecord>()
        try {
            val parts = currentMonthStr.split("-")
            if (parts.size == 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt() - 1 // 0-based month in Calendar
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.YEAR, year)
                cal.set(java.util.Calendar.MONTH, month)
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

                for (day in 1..maxDays) {
                    val dayStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    val record = monthDailyMap[dayStr] ?: OfflineDailyRecord(
                        dateStr = dayStr,
                        earnings = 0.0,
                        baseEarnings = 0.0,
                        overtimeAmount = 0.0,
                        overtimeHours = 0,
                        attendance = 0.0
                    )
                    filledMonthRecords.add(record)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AttendanceCalcEngine", "Error generating calendar days for $currentMonthStr", e)
        }

        val dailyRecordsList = if (filledMonthRecords.isNotEmpty()) filledMonthRecords else monthDailyMap.values.toList().sortedBy { it.dateStr }
        val allTimeDailyList = allTimeDailyMap.values.toList().sortedBy { it.dateStr }

        val monthlySettlement = calculateSettlement(
            regularEarnings = baseEarningsBd.toDouble(),
            overtimeEarnings = otEarningsBd.toDouble(),
            totalAdvanceReceived = advanceTotalBd.toDouble()
        )

        val allTimeSettlement = calculateSettlement(
            regularEarnings = allTimeBaseEarningsBd.toDouble(),
            overtimeEarnings = allTimeOtEarningsBd.toDouble(),
            totalAdvanceReceived = allTimeAdvanceTotalBd.toDouble()
        )

        return OfflinePersonalStatsData(
            present = present,
            absent = absent,
            halfDays = halfDays,
            overtime = overtime,
            advanceTotal = advanceTotalBd.toDouble(),
            totalEarnings = totalEarningsBd.toDouble(),
            regularEarnings = baseEarningsBd.toDouble(),
            overtimeEarnings = otEarningsBd.toDouble(),
            monthlySettlement = monthlySettlement,
            dailyRecords = dailyRecordsList,
            allTimeDailyRecords = allTimeDailyList,
            allTimeDays = allTimeDays,
            allTimeEarnings = allTimeEarningsBd.toDouble(),
            allTimeRegularEarnings = allTimeBaseEarningsBd.toDouble(),
            allTimeOvertimeEarnings = allTimeOtEarningsBd.toDouble(),
            allTimeOvertime = allTimeOvertime,
            allTimeAdvanceTotal = allTimeAdvanceTotalBd.toDouble(),
            allTimePresent = allTimePresent,
            allTimeAbsent = allTimeAbsent,
            allTimeHalfDays = allTimeHalfDays,
            allTimeSettlement = allTimeSettlement,
            hasRecordsInPeriod = hasRecordsInPeriod,
            hasAllTimeRecords = hasAllTimeRecords
        )
    }

    fun calculatePassbook(
        records: List<AttendanceEntity>,
        wage: Double
    ): PassbookCalculationResult {
        var present = 0
        var absent = 0
        var half = 0
        var totalAdvBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var totalEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var totalBaseBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var totalOtBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

        val wageBd = toMoney(wage)
        val halfWageBd = wageBd.divide(BigDecimal.valueOf(2), 2, ROUNDING_MODE)

        val logMap = mutableMapOf<String, OfflinePassbookLog>()

        records.forEach { rec ->
            val date = rec.date
            val status = rec.status
            val type = rec.type
            val advBd = toMoney(rec.advanceAmount)
            val note = rec.note
            val otHours = rec.overtimeHours

            var dailyBaseBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
            var dailyOtBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

            if (status == "present") {
                present++
                if (type == "half") {
                    half++
                    dailyBaseBd = halfWageBd
                } else {
                    dailyBaseBd = wageBd
                }
                dailyOtBd = OvertimeCalculator.calculateOvertimeAmountBigDecimal(wage, otHours, note)
                totalBaseBd = totalBaseBd.add(dailyBaseBd)
                totalOtBd = totalOtBd.add(dailyOtBd)
                totalEarningsBd = totalEarningsBd.add(dailyBaseBd.add(dailyOtBd))
            } else if (status == "absent") {
                absent++
            }

            if (advBd.compareTo(BigDecimal.ZERO) > 0) {
                totalAdvBd = totalAdvBd.add(advBd)
            }

            val existingLog = logMap[date]
            val cleanedNote = if (status != "advance") OvertimeCalculator.cleanNote(note) else existingLog?.note

            val mergedOtHours = maxOf(otHours, existingLog?.overtimeHours ?: 0)
            val existingOtBd = toMoney(existingLog?.overtimeAmount ?: 0.0)
            val mergedOtBd = dailyOtBd.add(existingOtBd)

            val existingBaseBd = toMoney(existingLog?.baseEarnings ?: 0.0)
            val mergedBaseBd = dailyBaseBd.add(existingBaseBd)

            val existingAdvBd = toMoney(existingLog?.advanceAmount ?: 0.0)
            val mergedAdvBd = existingAdvBd.add(advBd)

            val finalStatus = if (status != "advance") status else existingLog?.status ?: "advance"
            val finalType = if (status != "advance") type else existingLog?.type

            logMap[date] = OfflinePassbookLog(
                date = date,
                status = finalStatus,
                type = finalType,
                note = cleanedNote,
                advanceAmount = if (mergedAdvBd.compareTo(BigDecimal.ZERO) > 0) mergedAdvBd.toDouble() else null,
                overtimeHours = mergedOtHours,
                overtimeAmount = mergedOtBd.toDouble(),
                baseEarnings = mergedBaseBd.toDouble()
            )
        }

        val finalLogs = logMap.values.toList().sortedByDescending { it.date }
        val netBalanceBd = totalEarningsBd.subtract(totalAdvBd)
        val totalWorks = (present - half) + (half * 0.5)

        val settlement = calculateSettlement(
            regularEarnings = totalBaseBd.toDouble(),
            overtimeEarnings = totalOtBd.toDouble(),
            totalAdvanceReceived = totalAdvBd.toDouble()
        )

        return PassbookCalculationResult(
            present = present,
            absent = absent,
            half = half,
            totalAdv = totalAdvBd.toDouble(),
            totalEarnings = totalEarningsBd.toDouble(),
            netBalance = netBalanceBd.toDouble(),
            totalWorks = totalWorks,
            settlement = settlement,
            logs = finalLogs
        )
    }
}
