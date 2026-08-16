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

data class PassbookCalculationResult(
    val present: Int,
    val absent: Int,
    val half: Int,
    val totalAdv: Double,
    val totalEarnings: Double,
    val netBalance: Double,
    val totalWorks: Double,
    val logs: List<OfflinePassbookLog>
)

object AttendanceCalculationEngine {

    val ROUNDING_MODE: RoundingMode = RoundingMode.HALF_UP

    private fun toMoney(value: Double): BigDecimal {
        return BigDecimal.valueOf(value).setScale(2, ROUNDING_MODE)
    }

    private fun toMoney(value: Long): BigDecimal {
        return BigDecimal.valueOf(value).setScale(2, ROUNDING_MODE)
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
                } else {
                    advanceTodayBd = advanceTodayBd.add(advanceAmtBd)
                }
            }

            if (date.startsWith(currentMonthStr)) {
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
        var allTimeDays = 0

        var totalEarningsBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)
        var present = 0
        var absent = 0
        var halfDays = 0
        var overtime = 0
        var advanceTotalBd = BigDecimal.ZERO.setScale(2, ROUNDING_MODE)

        val dailyRecordsMap = mutableMapOf<String, OfflineDailyRecord>()
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

            if (isPresent) {
                allTimeDays++
                allTimeEarningsBd = allTimeEarningsBd.add(totalCostValBd)
            }

            if (date.startsWith(currentMonthStr)) {
                if (isPresent) {
                    present++
                    totalEarningsBd = totalEarningsBd.add(totalCostValBd)
                    if (isHalf) halfDays++
                    overtime += ot
                } else if (rec.status == "absent") {
                    absent++
                }

                if (rec.status == "advance") {
                    advanceTotalBd = advanceTotalBd.add(advanceAmtBd)
                }

                if (rec.status != "advance") {
                    val attendanceVal = if (isPresent) (if (isHalf) 0.5 else 1.0) else 0.0
                    val existing = dailyRecordsMap[date]
                    if (existing != null) {
                        val newEarnings = toMoney(existing.earnings).add(totalCostValBd).toDouble()
                        dailyRecordsMap[date] = existing.copy(
                            earnings = newEarnings,
                            attendance = existing.attendance + attendanceVal
                        )
                    } else {
                        dailyRecordsMap[date] = OfflineDailyRecord(
                            dateStr = date,
                            earnings = totalCostValBd.toDouble(),
                            attendance = attendanceVal
                        )
                    }
                }
            }
        }

        val dailyRecordsList = dailyRecordsMap.values.toList().sortedBy { it.dateStr }

        return OfflinePersonalStatsData(
            present = present,
            absent = absent,
            halfDays = halfDays,
            overtime = overtime,
            advanceTotal = advanceTotalBd.toDouble(),
            totalEarnings = totalEarningsBd.toDouble(),
            dailyRecords = dailyRecordsList,
            allTimeDays = allTimeDays,
            allTimeEarnings = allTimeEarningsBd.toDouble()
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
                totalEarningsBd = totalEarningsBd.add(dailyBaseBd.add(dailyOtBd))
            } else if (status == "absent") {
                absent++
            }

            if (status == "advance") {
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

        return PassbookCalculationResult(
            present = present,
            absent = absent,
            half = half,
            totalAdv = totalAdvBd.toDouble(),
            totalEarnings = totalEarningsBd.toDouble(),
            netBalance = netBalanceBd.toDouble(),
            totalWorks = totalWorks,
            logs = finalLogs
        )
    }
}
