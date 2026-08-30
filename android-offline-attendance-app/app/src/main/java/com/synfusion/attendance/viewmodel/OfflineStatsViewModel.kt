package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.DrillDownRepository
import com.synfusion.attendance.data.DrillDownType
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class OfflineDailyRecord(
    val dateStr: String,
    val earnings: Double,
    val baseEarnings: Double = 0.0,
    val overtimeAmount: Double = 0.0,
    val overtimeHours: Int = 0,
    val attendance: Double = 0.0
)

data class OfflinePersonalStatsData(
    val present: Int = 0,
    val absent: Int = 0,
    val halfDays: Int = 0,
    val overtime: Int = 0,
    val advanceTotal: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val regularEarnings: Double = 0.0,
    val overtimeEarnings: Double = 0.0,
    val monthlySettlement: com.synfusion.attendance.utils.FinancialSettlementResult? = null,
    val dailyRecords: List<OfflineDailyRecord> = emptyList(),
    val allTimeDailyRecords: List<OfflineDailyRecord> = emptyList(),
    val allTimeDays: Int = 0,
    val allTimeEarnings: Double = 0.0,
    val allTimeRegularEarnings: Double = 0.0,
    val allTimeOvertimeEarnings: Double = 0.0,
    val allTimeOvertime: Int = 0,
    val allTimeAdvanceTotal: Double = 0.0,
    val allTimePresent: Int = 0,
    val allTimeAbsent: Int = 0,
    val allTimeHalfDays: Int = 0,
    val allTimeSettlement: com.synfusion.attendance.utils.FinancialSettlementResult? = null,
    val hasRecordsInPeriod: Boolean = false,
    val hasAllTimeRecords: Boolean = false
)

data class OfflineStatsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),
    val isPremium: Boolean = true, // Permanently unlocked for offline variant

    // Personal
    val personalStats: OfflinePersonalStatsData = OfflinePersonalStatsData(),
    val errorMessage: String? = null,

    val selectedDrillDownType: DrillDownType? = null,
    val selectedLogs: List<OfflinePassbookLog> = emptyList(),
    val isDrillDownLoading: Boolean = false
)

class OfflineStatsViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository,
    private val drillDownRepository: DrillDownRepository
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _statsState = MutableStateFlow(OfflineStatsState())
    val statsState: StateFlow<OfflineStatsState> = _statsState

    private var cachedDefaultWage: Double = 500.0

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        viewModelScope.launch {
            loadStats()
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
        loadStats(showLoadingIndicator = true)
    }

    fun refresh() {
        if (_statsState.value.isRefreshing) return
        _statsState.update { it.copy(isRefreshing = true) }
        loadStats(showLoadingIndicator = false)
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _statsState.update { it.copy(
            selectedMonthDate = cal.time
        ) }
        loadStats(showLoadingIndicator = true)
    }

    fun setMonth(date: Date) {
        _statsState.update { it.copy(
            selectedMonthDate = date
        ) }
        loadStats(showLoadingIndicator = true)
    }

    private var loadJob: Job? = null

    private fun loadStats(showLoadingIndicator: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isRefreshing = _statsState.value.isRefreshing
            val startTime = System.currentTimeMillis()
            try {
                if (showLoadingIndicator) {
                    _statsState.update { it.copy(isLoading = true) }
                }

                val user = withContext(Dispatchers.IO) {
                    localAttendanceRepository.getUser()
                }
                cachedDefaultWage = user?.dailyWage ?: 500.0

                val cal = Calendar.getInstance()
                cal.time = _statsState.value.selectedMonthDate
                val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonth = sdfMonth.format(cal.time)

                // Personal
                val personal = withContext(Dispatchers.IO) {
                    localAttendanceRepository.getPersonalAttendance()
                }

                val statsData = withContext(Dispatchers.Default) {
                    AttendanceCalculationEngine.calculateStats(
                        records = personal,
                        currentMonthStr = currentMonth,
                        defaultWage = cachedDefaultWage
                    )
                }

                if (isRefreshing) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 400) {
                        delay(400 - elapsed)
                    }
                }

                _statsState.update { current ->
                    current.copy(
                        errorMessage = null,
                        personalStats = statsData
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineStatsVM", "Error loading stats", e)
                _statsState.update { current ->
                    current.copy(
                        errorMessage = "Failed to load stats: ${e.localizedMessage}"
                    )
                }
            } finally {
                _statsState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun openDrillDown(type: DrillDownType, isAllTime: Boolean) {
        viewModelScope.launch {
            _statsState.update { it.copy(selectedDrillDownType = type, isDrillDownLoading = true, selectedLogs = emptyList()) }
            try {
                val cal = Calendar.getInstance()
                cal.time = _statsState.value.selectedMonthDate
                val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthPrefix = if (isAllTime) null else sdfMonth.format(cal.time)

                val records = drillDownRepository.getDrillDownRecords(type, monthPrefix, isAllTime)
                val calcResult = AttendanceCalculationEngine.calculatePassbook(records, cachedDefaultWage)

                _statsState.update { it.copy(selectedLogs = calcResult.logs, isDrillDownLoading = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineStatsVM", "Error loading drill down", e)
                _statsState.update { it.copy(isDrillDownLoading = false) }
            }
        }
    }

    fun closeDrillDown() {
        _statsState.update { it.copy(selectedDrillDownType = null, selectedLogs = emptyList()) }
    }
}
