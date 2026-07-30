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

data class OfflinePassbookLog(
    val date: String,
    val status: String,
    val type: String?,
    val note: String?,
    val advanceAmount: Double?,
    val overtimeHours: Int = 0,
    val overtimeAmount: Double = 0.0,
    val baseEarnings: Double = 0.0
)

data class OfflinePassbookState(
    val role: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),
    val name: String = "",
    val workType: String = "Worker",
    val dailyWage: Double = 500.0,
    val joinedDate: String = "",

    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val halfDays: Int = 0,
    val totalDailyWorks: Double = 0.0,
    val passedDays: Int = 0,
    val attendanceRate: Int = 0,

    val grossEarned: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val finalBalance: Double = 0.0,
    val phone: String = "",

    val logs: List<OfflinePassbookLog> = emptyList(),
    val isPremium: Boolean = true // Permanently unlocked for offline version
)

class OfflinePassbookViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OfflinePassbookState())
    val state: StateFlow<OfflinePassbookState> = _state

    init {
        viewModelScope.launch {
            preferencesRepository.userRoleFlow.collect { role ->
                if (role != null) {
                    _state.update { it.copy(role = role) }
                    setupListeners()
                }
            }
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        setupListeners()
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _state.update { it.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        ) }
        setupListeners()
    }

    private fun setupListeners() {
        viewModelScope.launch {
            val user = localAttendanceRepository.getUser()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            if (user != null) {
                _state.update { it.copy(
                    name = user.name,
                    dailyWage = user.dailyWage,
                    joinedDate = sdf.format(Date(user.createdAt)),
                    phone = user.phoneNumber
                ) }

                val cal = Calendar.getInstance()
                cal.time = _state.value.selectedMonthDate
                val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val yearMonth = sdfMonth.format(cal.time)

                val personal = localAttendanceRepository.getPersonalAttendance()
                val filtered = personal.filter { it.date.startsWith(yearMonth) }
                calculatePassbook(filtered)
            }
        }
    }

    private fun calculatePassbook(list: List<AttendanceEntity>) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate

        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        val passedDays = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var present = 0
        var absent = 0
        var half = 0
        var totalAdv = 0.0
        var totalEarnings = 0.0

        val logMap = mutableMapOf<String, OfflinePassbookLog>()
        val wage = _state.value.dailyWage

        list.forEach { rec ->
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
            if (adv > 0) totalAdv += adv

            val existingLog = logMap[date]
            val cleanedNote = OvertimeCalculator.cleanNote(note) ?: existingLog?.note?.takeIf { note == null }

            val mergedOtHours = maxOf(otHours, existingLog?.overtimeHours ?: 0)
            val mergedOtAmount = dailyOT + (existingLog?.overtimeAmount ?: 0.0)
            val mergedBaseEarnings = dailyBase + (existingLog?.baseEarnings ?: 0.0)

            logMap[date] = OfflinePassbookLog(
                date = date,
                status = if (status != "advance") status else existingLog?.status ?: "advance",
                type = if (status != "advance") type else existingLog?.type,
                note = cleanedNote,
                advanceAmount = (existingLog?.advanceAmount ?: 0.0) + adv,
                overtimeHours = mergedOtHours,
                overtimeAmount = mergedOtAmount,
                baseEarnings = mergedBaseEarnings
            )
        }

        val effectiveDays = present - (half * 0.5)
        val grossEarned = totalEarnings
        val finalBalance = grossEarned - totalAdv

        val attRate = if (passedDays > 0) ((present.toDouble() / passedDays) * 100).toInt().coerceIn(0, 100) else 0

        val sortedLogs = logMap.values.toList().sortedByDescending { it.date }

        _state.update { current ->
            current.copy(
                isLoading = false,
                presentDays = present,
                absentDays = absent,
                halfDays = half,
                totalDailyWorks = effectiveDays,
                passedDays = passedDays,
                attendanceRate = attRate,
                grossEarned = grossEarned,
                totalAdvance = totalAdv,
                finalBalance = finalBalance,
                logs = sortedLogs,
                isRefreshing = false
            )
        }
    }
}
