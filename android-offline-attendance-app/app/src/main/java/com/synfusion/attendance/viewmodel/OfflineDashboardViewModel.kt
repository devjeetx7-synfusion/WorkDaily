package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.DrillDownRepository
import com.synfusion.attendance.data.DrillDownType
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.utils.OvertimeCalculator
import com.synfusion.attendance.utils.AttendanceCalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OfflineDashboardState(
    val name: String = "",
    val photoUrl: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

    // Personal Stats
    val todayEarned: String = "0",
    val monthEarned: String = "0",

    // Personal Attendance State
    val todayStatus: String? = null,
    val overtimeHours: Int = 0,
    val todayNote: String? = null,

    // Offline state placeholders
    val showOfflineBottomSheet: Boolean = false,
    val hasShownOfflineSheet: Boolean = false,

    val errorMessage: String? = null,

    val selectedDrillDownType: DrillDownType? = null,
    val selectedLogs: List<OfflinePassbookLog> = emptyList(),
    val isDrillDownLoading: Boolean = false
)

class OfflineDashboardViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository,
    private val drillDownRepository: DrillDownRepository
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _dashboardState = MutableStateFlow(OfflineDashboardState(isLoading = true))
    val dashboardState: StateFlow<OfflineDashboardState> = _dashboardState

    private var cachedDefaultWage: Double = 500.0

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true) }
            loadLocalData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.unregisterViewModel(this)
    }

    override fun cancelActiveJobs() {
        loadJob?.cancel()
    }

    override fun refreshOnRestore() {
        loadLocalData(showLoadingIndicator = true)
    }

    fun refresh() {
        if (_dashboardState.value.isRefreshing) return
        _dashboardState.update { it.copy(isRefreshing = true) }
        loadLocalData(showLoadingIndicator = false)
    }

    private var loadJob: Job? = null

    private fun loadLocalData(showLoadingIndicator: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isRefreshing = _dashboardState.value.isRefreshing
            val startTime = System.currentTimeMillis()
            try {
                if (showLoadingIndicator) {
                    _dashboardState.update { it.copy(isLoading = true) }
                }

                val user = withContext(Dispatchers.IO) { localAttendanceRepository.getUser() }
                val name = user?.name ?: "User"
                cachedDefaultWage = user?.dailyWage ?: 500.0

                _dashboardState.update { it.copy(name = name) }

                // Personal role
                val personalAttendance = withContext(Dispatchers.IO) { localAttendanceRepository.getPersonalAttendance() }
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                val calcResult = withContext(Dispatchers.Default) {
                    AttendanceCalculationEngine.calculateDashboardStats(
                        records = personalAttendance,
                        todayStr = todayStr,
                        currentMonthStr = currentMonthStr,
                        defaultWage = cachedDefaultWage
                    )
                }

                if (isRefreshing) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 400) {
                        delay(400 - elapsed)
                    }
                }

                _dashboardState.update { it.copy(
                    todayEarned = calcResult.todayEarned.toString().removeSuffix(".0"),
                    monthEarned = calcResult.monthEarned.toString().removeSuffix(".0"),
                    todayStatus = calcResult.currentTodayStatus,
                    overtimeHours = calcResult.currentOvertime,
                    todayNote = calcResult.currentNote,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                ) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineDashboardVM", "Error loading local data", e)
                _dashboardState.update { it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "Failed to load records: ${e.localizedMessage}"
                ) }
            }
        }
    }

    fun markAttendance(type: String, overtimeHours: Int, note: String) {
        viewModelScope.launch {
            try {
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

                withContext(Dispatchers.IO) {
                    localAttendanceRepository.saveAttendance(attendance)
                }
                loadLocalData(showLoadingIndicator = false)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineDashboardVM", "Error marking attendance", e)
                _dashboardState.update { it.copy(errorMessage = "Failed to mark attendance.") }
            }
        }
    }

    fun markAbsent(reason: String, note: String) {
        viewModelScope.launch {
            try {
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

                withContext(Dispatchers.IO) {
                    localAttendanceRepository.saveAttendance(attendance)
                }
                loadLocalData(showLoadingIndicator = false)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineDashboardVM", "Error marking absent", e)
                _dashboardState.update { it.copy(errorMessage = "Failed to record absence.") }
            }
        }
    }

    fun addAdvance(amount: Double) {
        viewModelScope.launch {
            try {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val docId = "personal_${todayStr}_advance"

                val attendance = AttendanceEntity(
                    id = docId,
                    workerId = null,
                    date = todayStr,
                    status = "advance",
                    type = "full",
                    advanceAmount = amount,
                    wage = 0.0
                )

                withContext(Dispatchers.IO) {
                    localAttendanceRepository.saveAttendance(attendance)
                }
                loadLocalData(showLoadingIndicator = false)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineDashboardVM", "Error adding advance", e)
                _dashboardState.update { it.copy(errorMessage = "Failed to record advance.") }
            }
        }
    }

    fun removeAttendance() {
        viewModelScope.launch {
            try {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val docId = "personal_$todayStr"

                _dashboardState.update { it.copy(
                    todayStatus = null,
                    overtimeHours = 0,
                    todayNote = null
                ) }

                withContext(Dispatchers.IO) {
                    localAttendanceRepository.deleteAttendance(docId, todayStr)
                }
                loadLocalData(showLoadingIndicator = false)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineDashboardVM", "Error removing attendance", e)
                _dashboardState.update { it.copy(errorMessage = "Failed to remove attendance.") }
            }
        }
    }

    fun openDrillDown(type: DrillDownType, isAllTime: Boolean = false) {
        viewModelScope.launch {
            _dashboardState.update { it.copy(selectedDrillDownType = type, isDrillDownLoading = true, selectedLogs = emptyList()) }
            try {
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val records = drillDownRepository.getDrillDownRecords(type, currentMonthStr, isAllTime)
                val calcResult = AttendanceCalculationEngine.calculatePassbook(records, cachedDefaultWage)
                _dashboardState.update { it.copy(selectedLogs = calcResult.logs, isDrillDownLoading = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineDashboardVM", "Error loading drill down", e)
                _dashboardState.update { it.copy(isDrillDownLoading = false) }
            }
        }
    }

    fun closeDrillDown() {
        _dashboardState.update { it.copy(selectedDrillDownType = null, selectedLogs = emptyList()) }
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
