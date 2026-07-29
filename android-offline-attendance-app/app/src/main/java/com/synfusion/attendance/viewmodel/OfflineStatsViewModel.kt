package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.utils.OvertimeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class OfflineDailyRecord(
    val dateStr: String,
    val earnings: Double,
    val attendance: Double
)

data class OfflineWorkerStats(
    val name: String,
    val days: Double,
    val cost: Double
)

data class OfflineContractorStatsData(
    val totalCost: Double = 0.0,
    val totalDailyWorks: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val totalWorkers: Int = 0,
    val topWorkers: List<OfflineWorkerStats> = emptyList(),
    val dailyRecords: List<OfflineDailyRecord> = emptyList(),
    val allTimeCost: Double = 0.0,
    val allTimeWorks: Double = 0.0,
    val allTimeTopWorkers: List<OfflineWorkerStats> = emptyList()
)

data class OfflinePersonalStatsData(
    val present: Int = 0,
    val absent: Int = 0,
    val halfDays: Int = 0,
    val overtime: Int = 0,
    val advanceTotal: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val dailyRecords: List<OfflineDailyRecord> = emptyList(),
    val allTimeDays: Int = 0,
    val allTimeEarnings: Double = 0.0
)

data class OfflineStatsState(
    val role: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),
    val isPremium: Boolean = true, // Permanently unlocked for offline variant

    // Contractor
    val contractorStats: OfflineContractorStatsData = OfflineContractorStatsData(),

    // Personal
    val personalStats: OfflinePersonalStatsData = OfflinePersonalStatsData()
)

class OfflineStatsViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _statsState = MutableStateFlow(OfflineStatsState())
    val statsState: StateFlow<OfflineStatsState> = _statsState

    private var cachedDefaultWage: Double = 500.0

    init {
        viewModelScope.launch {
            preferencesRepository.userRoleFlow.collect { role ->
                if (role != null) {
                    _statsState.update { it.copy(role = role) }
                    loadStats(role)
                }
            }
        }
    }

    fun refresh() {
        if (_statsState.value.isRefreshing) return
        _statsState.update { it.copy(isRefreshing = true) }
        loadStats(_statsState.value.role)
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _statsState.update { it.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        ) }
        loadStats(_statsState.value.role)
    }

    fun setMonth(date: Date) {
        _statsState.update { it.copy(
            selectedMonthDate = date,
            isLoading = true
        ) }
        loadStats(_statsState.value.role)
    }

    private fun loadStats(role: String) {
        viewModelScope.launch {
            val user = localAttendanceRepository.getUser()
            cachedDefaultWage = user?.dailyWage ?: 500.0

            val cal = Calendar.getInstance()
            cal.time = _statsState.value.selectedMonthDate
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = sdfMonth.format(cal.time)

            if (role == "contractor") {
                val workers = localAttendanceRepository.getAllWorkers()
                val workersMap = workers.associateBy { it.id }

                val topWorkersMap = mutableMapOf<String, OfflineWorkerStats>()
                val allTimeTopWorkersMap = mutableMapOf<String, OfflineWorkerStats>()

                var totalCost = 0.0
                var totalDays = 0.0
                var totalAdvance = 0.0
                var todayPresent = 0
                var todayAbsent = 0
                var allTimeCost = 0.0
                var allTimeWorks = 0.0

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Aggregate from all worker attendance records
                for (worker in workers) {
                    val attList = localAttendanceRepository.getWorkerAttendance(worker.id)
                    var workerMonthDays = 0.0
                    var workerMonthCost = 0.0
                    var workerAllTimeDays = 0.0
                    var workerAllTimeCost = 0.0

                    for (rec in attList) {
                        val isPresent = rec.status == "present"
                        val isHalf = rec.type == "half"
                        val dayVal = if (isPresent) (if (isHalf) 0.5 else 1.0) else 0.0
                        val costVal = if (isPresent) (if (isHalf) rec.wage / 2.0 else rec.wage) else 0.0

                        workerAllTimeDays += dayVal
                        workerAllTimeCost += costVal

                        allTimeCost += costVal
                        allTimeWorks += dayVal

                        if (rec.date.startsWith(currentMonth)) {
                            workerMonthDays += dayVal
                            workerMonthCost += costVal

                            totalCost += costVal
                            totalDays += dayVal
                            if (rec.status == "advance") {
                                totalAdvance += rec.advanceAmount
                            }
                        }

                        if (rec.date == todayStr) {
                            if (rec.status == "present") todayPresent++
                            if (rec.status == "absent") todayAbsent++
                        }
                    }

                    if (workerMonthDays > 0 || workerMonthCost > 0) {
                        topWorkersMap[worker.id] = OfflineWorkerStats(worker.name, workerMonthDays, workerMonthCost)
                    }
                    if (workerAllTimeDays > 0 || workerAllTimeCost > 0) {
                        allTimeTopWorkersMap[worker.id] = OfflineWorkerStats(worker.name, workerAllTimeDays, workerAllTimeCost)
                    }
                }

                _statsState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        contractorStats = OfflineContractorStatsData(
                            totalCost = totalCost,
                            totalDailyWorks = totalDays,
                            totalAdvance = totalAdvance,
                            todayPresent = todayPresent,
                            todayAbsent = todayAbsent,
                            totalWorkers = workers.size,
                            topWorkers = topWorkersMap.values.sortedByDescending { it.days },
                            allTimeCost = allTimeCost,
                            allTimeWorks = allTimeWorks,
                            allTimeTopWorkers = allTimeTopWorkersMap.values.sortedByDescending { it.days }
                        )
                    )
                }
            } else {
                // Personal
                val personal = localAttendanceRepository.getPersonalAttendance()

                var allTimeEarnings = 0.0
                var allTimeDays = 0

                var totalEarnings = 0.0
                var present = 0
                var absent = 0
                var halfDays = 0
                var overtime = 0
                var advanceTotal = 0.0

                for (rec in personal) {
                    val isPresent = rec.status == "present"
                    val isHalf = rec.type == "half"
                    val ot = rec.overtimeHours
                    val noteStr = rec.note

                    val baseCost = if (isPresent) (if (isHalf) cachedDefaultWage / 2.0 else cachedDefaultWage) else 0.0
                    val otAmt = if (isPresent) OvertimeCalculator.calculateOvertimeAmount(cachedDefaultWage, ot, noteStr) else 0.0
                    val totalCostVal = baseCost + otAmt

                    if (isPresent) {
                        allTimeDays++
                        allTimeEarnings += totalCostVal
                    }

                    if (rec.date.startsWith(currentMonth)) {
                        if (isPresent) {
                            present++
                            if (isHalf) halfDays++
                            overtime += ot
                            totalEarnings += totalCostVal
                        } else if (rec.status == "absent") {
                            absent++
                        } else if (rec.status == "advance") {
                            advanceTotal += rec.advanceAmount
                        }
                    }
                }

                _statsState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        personalStats = OfflinePersonalStatsData(
                            present = present,
                            absent = absent,
                            halfDays = halfDays,
                            overtime = overtime,
                            advanceTotal = advanceTotal,
                            totalEarnings = totalEarnings,
                            allTimeDays = allTimeDays,
                            allTimeEarnings = allTimeEarnings
                        )
                    )
                }
            }
        }
    }
}
