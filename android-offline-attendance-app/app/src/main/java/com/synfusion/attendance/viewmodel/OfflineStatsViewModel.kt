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
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),
    val isPremium: Boolean = true, // Permanently unlocked for offline variant

    // Personal
    val personalStats: OfflinePersonalStatsData = OfflinePersonalStatsData(),
    val errorMessage: String? = null
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
            _statsState.update { it.copy(isLoading = true) }
            loadStats()
        }
    }

    fun refresh() {
        if (_statsState.value.isRefreshing) return
        _statsState.update { it.copy(isRefreshing = true) }
        loadStats()
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _statsState.update { it.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        ) }
        loadStats()
    }

    fun setMonth(date: Date) {
        _statsState.update { it.copy(
            selectedMonthDate = date,
            isLoading = true
        ) }
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val user = localAttendanceRepository.getUser()
                cachedDefaultWage = user?.dailyWage ?: 500.0

                val cal = Calendar.getInstance()
                cal.time = _statsState.value.selectedMonthDate
                val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonth = sdfMonth.format(cal.time)

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
                        errorMessage = null,
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
            } catch (e: Exception) {
                android.util.Log.e("OfflineStatsVM", "Error loading stats", e)
                _statsState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Failed to load stats: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
