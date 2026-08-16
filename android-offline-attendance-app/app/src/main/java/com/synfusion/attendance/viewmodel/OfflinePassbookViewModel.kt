package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
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
    val settlement: com.synfusion.attendance.utils.FinancialSettlementResult? = null,
    val phone: String = "",

    val logs: List<OfflinePassbookLog> = emptyList(),
    val isPremium: Boolean = true, // Permanently unlocked for offline version
    val errorMessage: String? = null
)

class OfflinePassbookViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _state = MutableStateFlow(OfflinePassbookState())
    val state: StateFlow<OfflinePassbookState> = _state

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        viewModelScope.launch {
            setupListeners()
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
        setupListeners(showLoadingIndicator = true)
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        setupListeners(showLoadingIndicator = false)
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _state.update { it.copy(
            selectedMonthDate = cal.time
        ) }
        setupListeners(showLoadingIndicator = true)
    }

    private var loadJob: Job? = null

    private fun setupListeners(showLoadingIndicator: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isRefreshing = _state.value.isRefreshing
            val startTime = System.currentTimeMillis()
            try {
                if (showLoadingIndicator) {
                    _state.update { it.copy(isLoading = true) }
                }

                val user = withContext(Dispatchers.IO) {
                    localAttendanceRepository.getUser()
                }
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                if (user != null) {
                    _state.update { it.copy(
                        name = user.name,
                        dailyWage = user.dailyWage,
                        joinedDate = sdf.format(Date(user.createdAt)),
                        phone = user.phoneNumber,
                        errorMessage = null
                    ) }

                    val cal = Calendar.getInstance()
                    cal.time = _state.value.selectedMonthDate
                    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    val yearMonth = sdfMonth.format(cal.time)

                    val personal = withContext(Dispatchers.IO) {
                        localAttendanceRepository.getPersonalAttendance()
                    }
                    val filtered = personal.filter { AttendanceCalculationEngine.isDateInMonth(it.date, yearMonth) }
                    calculatePassbook(filtered)

                    if (isRefreshing) {
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed < 400) {
                            delay(400 - elapsed)
                        }
                    }
                    _state.update { it.copy(isRefreshing = false) }
                } else {
                    _state.update { it.copy(
                        isLoading = false,
                        isRefreshing = false
                    ) }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflinePassbookVM", "Error setting up listeners", e)
                _state.update { it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "Failed to load passbook summary: ${e.localizedMessage}"
                ) }
            }
        }
    }

    private fun calculatePassbook(list: List<AttendanceEntity>) {
        try {
            val cal = Calendar.getInstance()
            cal.time = _state.value.selectedMonthDate

            val today = Calendar.getInstance()
            val isCurrentMonth = today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
            val passedDays = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val wage = _state.value.dailyWage
            val calcResult = AttendanceCalculationEngine.calculatePassbook(list, wage)

            val attRate = if (passedDays > 0) ((calcResult.present.toDouble() / passedDays) * 100).toInt().coerceIn(0, 100) else 0

            _state.update { current ->
                current.copy(
                    isLoading = false,
                    presentDays = calcResult.present,
                    absentDays = calcResult.absent,
                    halfDays = calcResult.half,
                    totalDailyWorks = calcResult.totalWorks,
                    passedDays = passedDays,
                    attendanceRate = attRate,
                    grossEarned = calcResult.totalEarnings,
                    totalAdvance = calcResult.totalAdv,
                    finalBalance = calcResult.netBalance,
                    settlement = calcResult.settlement,
                    logs = calcResult.logs,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("OfflinePassbookVM", "Error calculating passbook", e)
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "Failed to calculate passbook details: ${e.localizedMessage}"
                )
            }
        }
    }
}
