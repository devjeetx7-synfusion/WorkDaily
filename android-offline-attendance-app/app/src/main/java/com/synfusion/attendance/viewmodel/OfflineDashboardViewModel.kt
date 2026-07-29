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
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OfflineDashboardState(
    val role: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

    // Contractor Stats
    val totalWorkers: String = "0",
    val todayPresent: String = "0",
    val todayAbsent: String = "0",
    val totalPaidMonth: String = "0",
    val pendingAmount: String = "0",

    // Personal Stats
    val todayEarned: String = "0",
    val monthEarned: String = "0",

    // Personal Attendance State
    val todayStatus: String? = null,
    val overtimeHours: Int = 0,
    val todayNote: String? = null,

    // Offline state placeholders
    val showOfflineBottomSheet: Boolean = false,
    val hasShownOfflineSheet: Boolean = false
)

class OfflineDashboardViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(OfflineDashboardState(isLoading = true))
    val dashboardState: StateFlow<OfflineDashboardState> = _dashboardState

    private var cachedDefaultWage: Double = 500.0

    init {
        viewModelScope.launch {
            preferencesRepository.userRoleFlow.collect { role ->
                if (role != null) {
                    _dashboardState.update { it.copy(role = role) }
                    loadLocalData(role)
                }
            }
        }
    }

    fun refresh() {
        if (_dashboardState.value.isRefreshing) return
        _dashboardState.update { it.copy(isRefreshing = true) }
        loadLocalData(_dashboardState.value.role)
    }

    private var loadJob: Job? = null

    private fun loadLocalData(role: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val user = localAttendanceRepository.getUser()
            val name = user?.name ?: "User"
            cachedDefaultWage = user?.dailyWage ?: 500.0

            _dashboardState.update { it.copy(name = name, isRefreshing = false, isLoading = false) }

            if (role == "contractor") {
                val workers = localAttendanceRepository.getAllWorkers()
                val totalWorkersCount = workers.size

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                val monthSummary = localAttendanceRepository.getSummaryFlow(currentMonthStr, "contractor")

                // Let's also dynamically calculate stats to be extremely accurate
                val todayRecords = localAttendanceRepository.getAttendanceByDate(todayStr)
                val todayPresentCount = todayRecords.count { it.status == "present" }
                val todayAbsentCount = todayRecords.count { it.status == "absent" }

                // Collect the summary flow or compute
                monthSummary.collect { summary ->
                    val totalPaid = summary?.totalAdvance ?: 0.0
                    val totalWages = summary?.totalWages ?: 0.0
                    val pending = totalWages - totalPaid

                    _dashboardState.update { it.copy(
                        totalWorkers = totalWorkersCount.toString(),
                        todayPresent = todayPresentCount.toString(),
                        todayAbsent = todayAbsentCount.toString(),
                        totalPaidMonth = totalPaid.toInt().toString(),
                        pendingAmount = pending.toInt().toString()
                    ) }
                }
            } else {
                // Personal role
                val personalAttendance = localAttendanceRepository.getPersonalAttendance()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                var todayEarned = 0.0
                var monthEarned = 0.0
                var currentTodayStatus: String? = null
                var currentOvertime = 0
                var currentNote: String? = null

                for (record in personalAttendance) {
                    val date = record.date
                    val status = record.status
                    val type = record.type
                    val otHours = record.overtimeHours
                    val noteStr = record.note

                    val dailyBase = if (status == "present") {
                        if (type == "half") cachedDefaultWage / 2 else cachedDefaultWage
                    } else 0.0
                    // Support overtime calculation
                    val dailyOT = if (status == "present") OvertimeCalculator.calculateOvertimeAmount(cachedDefaultWage, otHours, noteStr) else 0.0

                    if (date == todayStr) {
                        if (status != "advance") {
                            currentTodayStatus = status
                            currentOvertime = otHours
                            currentNote = OvertimeCalculator.cleanNote(noteStr)
                            todayEarned = dailyBase + dailyOT
                        }
                    }

                    if (date.startsWith(currentMonthStr)) {
                        monthEarned += (dailyBase + dailyOT)
                    }
                }

                _dashboardState.update { it.copy(
                    todayEarned = todayEarned.toInt().toString(),
                    monthEarned = monthEarned.toInt().toString(),
                    todayStatus = currentTodayStatus,
                    overtimeHours = currentOvertime,
                    todayNote = currentNote
                ) }
            }
        }
    }

    fun markAttendance(type: String, overtimeHours: Int, note: String) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val docId = "personal_$todayStr"

            val attendance = AttendanceEntity(
                id = docId,
                workerId = null,
                date = todayStr,
                status = "present",
                type = type,
                overtimeHours = overtimeHours,
                note = note,
                wage = cachedDefaultWage
            )

            _dashboardState.update { it.copy(
                todayStatus = "present",
                overtimeHours = overtimeHours,
                todayNote = note
            ) }

            localAttendanceRepository.saveAttendance(attendance)
            loadLocalData("personal")
        }
    }

    fun markAbsent(reason: String, note: String) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val docId = "personal_$todayStr"

            val attendance = AttendanceEntity(
                id = docId,
                workerId = null,
                date = todayStr,
                status = "absent",
                type = "full",
                reason = reason,
                note = note,
                wage = cachedDefaultWage
            )

            _dashboardState.update { it.copy(
                todayStatus = "absent",
                overtimeHours = 0,
                todayNote = note
            ) }

            localAttendanceRepository.saveAttendance(attendance)
            loadLocalData("personal")
        }
    }

    fun addAdvance(amount: Double, workerId: String? = null) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val docId = if (workerId != null) "${workerId}_${todayStr}_advance" else "personal_${todayStr}_advance"

            val attendance = AttendanceEntity(
                id = docId,
                workerId = workerId,
                date = todayStr,
                status = "advance",
                type = "full",
                advanceAmount = amount,
                wage = 0.0
            )

            localAttendanceRepository.saveAttendance(attendance)
            loadLocalData(_dashboardState.value.role)
        }
    }

    fun removeAttendance() {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val docId = "personal_$todayStr"

            _dashboardState.update { it.copy(
                todayStatus = null,
                overtimeHours = 0,
                todayNote = null
            ) }

            localAttendanceRepository.deleteAttendance(docId, todayStr)
            loadLocalData("personal")
        }
    }

    fun onNetworkStateChanged(isConnected: Boolean) {
        // Safe mock
    }

    fun dismissOfflineBottomSheet() {
        _dashboardState.update { it.copy(showOfflineBottomSheet = false) }
    }

    fun retryConnection(isConnected: Boolean) {
        // Safe mock
    }
}
