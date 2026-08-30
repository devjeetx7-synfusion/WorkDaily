package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.DrillDownRepository
import com.synfusion.attendance.data.DrillDownType
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.utils.AttendanceCalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

data class AllTimeGroup(
    val year: String,
    val month: String, // e.g. "January"
    val monthNumeric: String, // e.g. "01"
    val totalEarnings: Double,
    val totalAdvance: Double,
    val totalOvertime: Double,
    val presentCount: Int,
    val absentCount: Int,
    val halfCount: Int,
    val logs: List<OfflinePassbookLog>
)

data class AllTimeYearGroup(
    val year: String,
    val months: List<AllTimeGroup>
)

data class OfflineAllTimePassbookState(
    val isLoading: Boolean = true,

    // Top Section Employment Summary
    val employmentSince: String = "",
    val totalWorkingDays: Double = 0.0,
    val totalPresent: Int = 0,
    val totalAbsent: Int = 0,
    val totalHalfDays: Int = 0,
    val totalSalaryEarned: Double = 0.0,
    val totalOtEarned: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val totalNetEarnings: Double = 0.0,
    val settlement: com.synfusion.attendance.utils.FinancialSettlementResult? = null,

    val searchQuery: String = "",

    val yearGroups: List<AllTimeYearGroup> = emptyList(),

    // Drill down states for all time
    val selectedDrillDownType: DrillDownType? = null,
    val selectedLogs: List<OfflinePassbookLog> = emptyList(),
    val isDrillDownLoading: Boolean = false,

    val errorMessage: String? = null
)

class OfflineAllTimePassbookViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    val localAttendanceRepository: LocalAttendanceRepository,
    private val drillDownRepository: DrillDownRepository
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _state = MutableStateFlow(OfflineAllTimePassbookState())
    val state: StateFlow<OfflineAllTimePassbookState> = _state

    private var cachedDefaultWage = 500.0

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.unregisterViewModel(this)
    }

    override fun cancelActiveJobs() {
        loadJob?.cancel()
    }

    override fun refreshOnRestore() {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        loadData()
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = withContext(Dispatchers.IO) { localAttendanceRepository.getUser() }
                cachedDefaultWage = user?.dailyWage ?: 500.0

                val joinedDate = user?.createdAt?.let {
                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                } ?: ""

                val records = withContext(Dispatchers.IO) { localAttendanceRepository.getPersonalAttendance() }

                val calcResult = withContext(Dispatchers.Default) {
                    AttendanceCalculationEngine.calculatePassbook(records, cachedDefaultWage)
                }

                // Filter logs based on search query
                val query = _state.value.searchQuery.trim()
                val filteredLogs = if (query.isBlank()) {
                    calcResult.logs
                } else {
                    calcResult.logs.filter { log ->
                        val dateMatch = log.date.contains(query, ignoreCase = true)
                        val noteMatch = log.note?.contains(query, ignoreCase = true) == true
                        val statusMatch = log.status.contains(query, ignoreCase = true)
                        val amtMatch = log.baseEarnings.toString().contains(query) || log.advanceAmount?.toString()?.contains(query) == true || log.overtimeAmount.toString().contains(query)

                        dateMatch || noteMatch || statusMatch || amtMatch
                    }
                }

                // Group Year -> Month -> Daily Records using exact BigDecimal precision
                val yearGroups = withContext(Dispatchers.Default) {
                    val grouped = filteredLogs.groupBy { it.date.substring(0, 4) } // Year
                    grouped.map { (year, yearLogs) ->
                        val monthGrouped = yearLogs.groupBy { it.date.substring(5, 7) } // Month (01-12)
                        val monthGroups = monthGrouped.map { (monthNum, monthLogs) ->
                            val monthName = getMonthName(monthNum.toInt())

                            var mEarningsBd = BigDecimal.ZERO.setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
                            var mAdvanceBd = BigDecimal.ZERO.setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
                            var mOvertimeBd = BigDecimal.ZERO.setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
                            var mPresent = 0
                            var mAbsent = 0
                            var mHalf = 0

                            monthLogs.forEach { log ->
                                mEarningsBd = mEarningsBd.add(BigDecimal.valueOf(log.baseEarnings))
                                mAdvanceBd = mAdvanceBd.add(BigDecimal.valueOf(log.advanceAmount ?: 0.0))
                                mOvertimeBd = mOvertimeBd.add(BigDecimal.valueOf(log.overtimeAmount))
                                if (log.status == "present") {
                                    mPresent++
                                    if (log.type == "half") mHalf++
                                } else if (log.status == "absent") {
                                    mAbsent++
                                }
                            }

                            AllTimeGroup(
                                year = year,
                                month = monthName,
                                monthNumeric = monthNum,
                                totalEarnings = mEarningsBd.toDouble(),
                                totalAdvance = mAdvanceBd.toDouble(),
                                totalOvertime = mOvertimeBd.toDouble(),
                                presentCount = mPresent,
                                absentCount = mAbsent,
                                halfCount = mHalf,
                                logs = monthLogs.sortedByDescending { it.date }
                            )
                        }.sortedByDescending { it.monthNumeric }

                        AllTimeYearGroup(year = year, months = monthGroups)
                    }.sortedByDescending { it.year }
                }

                val totalOt = calcResult.logs.fold(BigDecimal.ZERO.setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)) { acc, log ->
                    acc.add(BigDecimal.valueOf(log.overtimeAmount))
                }.toDouble()

                _state.update { current ->
                    current.copy(
                        employmentSince = joinedDate,
                        totalWorkingDays = calcResult.totalWorks,
                        totalPresent = calcResult.present,
                        totalAbsent = calcResult.absent,
                        totalHalfDays = calcResult.half,
                        totalSalaryEarned = calcResult.totalEarnings,
                        totalOtEarned = totalOt,
                        totalAdvance = calcResult.totalAdv,
                        totalNetEarnings = calcResult.netBalance,
                        settlement = calcResult.settlement,
                        yearGroups = yearGroups
                    )
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineAllTimeVM", "Error loading all time passbook", e)
                _state.update { it.copy(errorMessage = e.localizedMessage) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun openDrillDown(type: DrillDownType) {
        viewModelScope.launch {
            _state.update { it.copy(selectedDrillDownType = type, isDrillDownLoading = true, selectedLogs = emptyList()) }
            try {
                val records = drillDownRepository.getDrillDownRecords(type, null, true)
                val calcResult = AttendanceCalculationEngine.calculatePassbook(records, cachedDefaultWage)
                _state.update { it.copy(selectedLogs = calcResult.logs, isDrillDownLoading = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(isDrillDownLoading = false) }
            }
        }
    }

    fun closeDrillDown() {
        _state.update { it.copy(selectedDrillDownType = null, selectedLogs = emptyList()) }
    }

    private fun getMonthName(month: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.MONTH, month - 1)
        val sdf = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault())
        return sdf.format(cal.time)
    }
}
