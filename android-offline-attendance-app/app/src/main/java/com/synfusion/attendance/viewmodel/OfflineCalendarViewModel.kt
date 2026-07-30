package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class OfflineAttendanceRecord(
    val id: String,
    val date: String,
    val status: String,
    val type: String?,
    val reason: String?,
    val overtimeHours: Int?,
    val note: String?,
    val advanceAmount: Double?
)

data class OfflineCalendarState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,

    // Personal State
    val currentMonthDate: Date = Date(), // Determines the month/year displayed
    val personalAttendance: List<OfflineAttendanceRecord> = emptyList()
)

class OfflineCalendarViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _calendarState = MutableStateFlow(OfflineCalendarState())
    val calendarState: StateFlow<OfflineCalendarState> = _calendarState

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        viewModelScope.launch {
            loadData()
        }
    }

    fun refresh() {
        if (_calendarState.value.isRefreshing) return
        _calendarState.update { it.copy(isRefreshing = true) }
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _calendarState.update { it.copy(isLoading = true) }

            // Personal
            val currentMonthDate = _calendarState.value.currentMonthDate
            val cal = Calendar.getInstance()
            cal.time = currentMonthDate

            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val monthPrefix = sdfMonth.format(cal.time)

            val personal = localAttendanceRepository.getPersonalAttendance()
            val filtered = personal.filter { it.date.startsWith(monthPrefix) }
            val mappedRecords = filtered.map { rec ->
                OfflineAttendanceRecord(
                    id = rec.id,
                    date = rec.date,
                    status = rec.status,
                    type = rec.type,
                    reason = rec.reason,
                    overtimeHours = rec.overtimeHours,
                    note = rec.note,
                    advanceAmount = rec.advanceAmount
                )
            }

            _calendarState.update { it.copy(
                personalAttendance = mappedRecords,
                isLoading = false,
                isRefreshing = false
            ) }
        }
    }

    fun changePersonalMonth(offsetMonths: Int) {
        val cal = Calendar.getInstance()
        cal.time = _calendarState.value.currentMonthDate
        cal.add(Calendar.MONTH, offsetMonths)
        _calendarState.update { it.copy(
            currentMonthDate = cal.time,
            isLoading = true
        ) }
        loadData()
    }

    fun markPersonalAttendance(
        date: String,
        status: String,
        type: String,
        reason: String,
        overtimeHours: Int,
        note: String,
        advanceAmount: Double
    ) {
        viewModelScope.launch {
            val docId = "personal_$date"

            if (status == "present" || status == "absent") {
                val attendance = AttendanceEntity(
                    id = docId,
                    workerId = null,
                    date = date,
                    status = status,
                    type = type,
                    reason = reason,
                    overtimeHours = overtimeHours,
                    note = note
                )
                localAttendanceRepository.saveAttendance(attendance)
            }

            if (advanceAmount > 0) {
                val advance = AttendanceEntity(
                    id = "${docId}_advance",
                    workerId = null,
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

            loadData()
        }
    }

    fun removePersonalAttendance(date: String) {
        viewModelScope.launch {
            val docId = "personal_$date"
            localAttendanceRepository.deleteAttendance(docId, date)
            localAttendanceRepository.deleteAttendance("${docId}_advance", date)
            loadData()
        }
    }
}
