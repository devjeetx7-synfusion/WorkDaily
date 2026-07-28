package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
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

class OfflineWorkerDetailViewModel(
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OfflinePassbookState(role = "contractor"))
    val state: StateFlow<OfflinePassbookState> = _state

    var workerId: String? = null
        private set

    fun initialize(id: String) {
        if (workerId == id) return
        workerId = id
        setupListeners()
    }

    fun refresh() {
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
        val wId = workerId ?: return

        viewModelScope.launch {
            val worker = localAttendanceRepository.getWorkerById(wId)
            if (worker != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                _state.update { it.copy(
                    name = worker.name,
                    workType = worker.workType,
                    dailyWage = worker.dailyWage,
                    joinedDate = sdf.format(Date(worker.createdAt)),
                    phone = worker.phone
                ) }

                val cal = Calendar.getInstance()
                cal.time = _state.value.selectedMonthDate
                val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val yearMonth = sdfMonth.format(cal.time)

                val attendance = localAttendanceRepository.getWorkerAttendance(wId)
                val filtered = attendance.filter { it.date.startsWith(yearMonth) }
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
        var totalOTAmount = 0.0
        var totalBaseEarnings = 0.0

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
                totalBaseEarnings += dailyBase
                totalOTAmount += dailyOT
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
        val grossEarned = totalBaseEarnings + totalOTAmount
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

    fun markAttendance(
        date: String,
        status: String,
        type: String,
        reason: String,
        overtimeHours: Int,
        note: String,
        advanceAmount: Double
    ) {
        viewModelScope.launch {
            val wId = workerId ?: return@launch
            val docId = "${wId}_$date"
            val wage = _state.value.dailyWage

            if (status == "present" || status == "absent") {
                val rec = AttendanceEntity(
                    id = docId,
                    workerId = wId,
                    date = date,
                    status = status,
                    type = type,
                    reason = reason,
                    overtimeHours = overtimeHours,
                    note = note,
                    wage = wage
                )
                localAttendanceRepository.saveAttendance(rec)
            }

            if (advanceAmount > 0) {
                val advance = AttendanceEntity(
                    id = "${docId}_advance",
                    workerId = wId,
                    date = date,
                    status = "advance",
                    type = "full",
                    advanceAmount = advanceAmount,
                    wage = 0.0
                )
                localAttendanceRepository.saveAttendance(advance)
            } else {
                localAttendanceRepository.deleteAttendance("${docId}_advance", date)
            }
            setupListeners()
        }
    }

    fun removeAttendance(date: String) {
        viewModelScope.launch {
            val wId = workerId ?: return@launch
            val docId = "${wId}_$date"
            localAttendanceRepository.deleteAttendance(docId, date)
            localAttendanceRepository.deleteAttendance("${docId}_advance", date)
            setupListeners()
        }
    }
}
