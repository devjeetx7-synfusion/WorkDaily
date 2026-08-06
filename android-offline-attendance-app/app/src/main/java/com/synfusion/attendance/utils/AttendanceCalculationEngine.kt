package com.synfusion.attendance.utils

import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.viewmodel.OfflinePassbookLog
import com.synfusion.attendance.viewmodel.OfflinePersonalStatsData
import com.synfusion.attendance.viewmodel.OfflineDailyRecord
import java.util.Calendar

data class DashboardCalculationResult(
    val todayEarned: Double,
    val monthEarned: Double,
    val currentTodayStatus: String?,
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

    fun calculateDashboardStats(
        records: List<AttendanceEntity>,
        todayStr: String,
        currentMonthStr: String,
        defaultWage: Double
    ): DashboardCalculationResult {
        var todayEarned = 0.0
        var monthEarned = 0.0
        var currentTodayStatus: String? = null
        var currentOvertime = 0
        var currentNote: String? = null
        var advanceToday = 0.0

        for (record in records) {
            val date = record.date
            val status = record.status
            val type = record.type
            val otHours = record.overtimeHours
            val noteStr = record.note
            val advanceAmt = record.advanceAmount

            val dailyBase = if (status == "present") {
                if (type == "half") defaultWage / 2.0 else defaultWage
            } else 0.0

            val dailyOT = if (status == "present") {
                OvertimeCalculator.calculateOvertimeAmount(defaultWage, otHours, noteStr)
            } else 0.0

            if (date == todayStr) {
                if (status != "advance") {
                    currentTodayStatus = status
                    currentOvertime = otHours
                    currentNote = OvertimeCalculator.cleanNote(noteStr)
                    todayEarned = dailyBase + dailyOT
                } else {
                    advanceToday += advanceAmt
                }
            }

            if (date.startsWith(currentMonthStr)) {
                monthEarned += (dailyBase + dailyOT)
            }
        }

        return DashboardCalculationResult(
            todayEarned = todayEarned,
            monthEarned = monthEarned,
            currentTodayStatus = currentTodayStatus,
            currentOvertime = currentOvertime,
            currentNote = currentNote,
            advanceToday = advanceToday
        )
    }

    fun calculateStats(
        records: List<AttendanceEntity>,
        currentMonthStr: String,
        defaultWage: Double
    ): OfflinePersonalStatsData {
        var allTimeEarnings = 0.0
        var allTimeDays = 0

        var totalEarnings = 0.0
        var present = 0
        var absent = 0
        var halfDays = 0
        var overtime = 0
        var advanceTotal = 0.0

        val dailyRecordsMap = mutableMapOf<String, OfflineDailyRecord>()

        for (rec in records) {
            val date = rec.date
            val isPresent = rec.status == "present"
            val isHalf = rec.type == "half"
            val ot = rec.overtimeHours
            val noteStr = rec.note
            val advanceAmt = rec.advanceAmount

            val baseCost = if (isPresent) (if (isHalf) defaultWage / 2.0 else defaultWage) else 0.0
            val otAmt = if (isPresent) OvertimeCalculator.calculateOvertimeAmount(defaultWage, ot, noteStr) else 0.0
            val totalCostVal = baseCost + otAmt

            if (isPresent) {
                allTimeDays++
                allTimeEarnings += totalCostVal
            }

            if (date.startsWith(currentMonthStr)) {
                if (isPresent) {
                    present++
                    totalEarnings += totalCostVal
                    if (isHalf) halfDays++
                } else if (rec.status == "absent") {
                    absent++
                }

                if (rec.status == "advance") {
                    advanceTotal += advanceAmt
                }
                overtime += ot

                if (rec.status != "advance") {
                    val attendanceVal = if (isPresent) (if (isHalf) 0.5 else 1.0) else 0.0
                    val existing = dailyRecordsMap[date]
                    if (existing != null) {
                        dailyRecordsMap[date] = existing.copy(
                            earnings = existing.earnings + totalCostVal,
                            attendance = existing.attendance + attendanceVal
                        )
                    } else {
                        dailyRecordsMap[date] = OfflineDailyRecord(
                            dateStr = date,
                            earnings = totalCostVal,
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
            advanceTotal = advanceTotal,
            totalEarnings = totalEarnings,
            dailyRecords = dailyRecordsList,
            allTimeDays = allTimeDays,
            allTimeEarnings = allTimeEarnings
        )
    }

    fun calculatePassbook(
        records: List<AttendanceEntity>,
        wage: Double
    ): PassbookCalculationResult {
        var present = 0
        var absent = 0
        var half = 0
        var totalAdv = 0.0
        var totalEarnings = 0.0

        val logMap = mutableMapOf<String, OfflinePassbookLog>()

        records.forEach { rec ->
            val date = rec.date
            val status = rec.status
            val type = rec.type
            val adv = rec.advanceAmount
            val note = rec.note
            val otHours = rec.overtimeHours

            var dailyBase = 0.0
            var dailyOT = 0.0

            if (status == "present") {
                present++
                dailyBase = if (type == "half") {
                    half++
                    wage / 2.0
                } else {
                    wage
                }
                dailyOT = OvertimeCalculator.calculateOvertimeAmount(wage, otHours, note)
                totalEarnings += (dailyBase + dailyOT)
            } else if (status == "absent") {
                absent++
            }

            if (status == "advance") {
                totalAdv += adv
            }

            val log = logMap[date]
            if (log != null) {
                val combinedAdv = (log.advanceAmount ?: 0.0) + adv
                val finalStatus = if (status == "advance") log.status else status
                val finalType = if (status == "advance") log.type else type
                logMap[date] = log.copy(
                    status = finalStatus,
                    type = finalType,
                    advanceAmount = if (combinedAdv > 0) combinedAdv else null,
                    baseEarnings = log.baseEarnings + dailyBase,
                    overtimeHours = log.overtimeHours + otHours,
                    overtimeAmount = log.overtimeAmount + dailyOT,
                    note = if (status != "advance") OvertimeCalculator.cleanNote(note) else log.note
                )
            } else {
                logMap[date] = OfflinePassbookLog(
                    date = date,
                    status = status,
                    type = type,
                    advanceAmount = if (adv > 0) adv else null,
                    baseEarnings = dailyBase,
                    overtimeHours = otHours,
                    overtimeAmount = dailyOT,
                    note = if (status != "advance") OvertimeCalculator.cleanNote(note) else null
                )
            }
        }

        val finalLogs = logMap.values.toList().sortedByDescending { it.date }
        val netBalance = totalEarnings - totalAdv
        val totalWorks = (present - half) + (half * 0.5)

        return PassbookCalculationResult(
            present = present,
            absent = absent,
            half = half,
            totalAdv = totalAdv,
            totalEarnings = totalEarnings,
            netBalance = netBalance,
            totalWorks = totalWorks,
            logs = finalLogs
        )
    }
}
