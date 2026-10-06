package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.festival.Festival
import com.synfusion.attendance.festival.FestivalRepository
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
    val personalAttendance: List<OfflineAttendanceRecord> = emptyList(),
    val currencyCode: String = "INR",
    val errorMessage: String? = null,

    // Festival State
    val festivalsEnabled: Boolean = true,
    val countryCode: String = "IN",
    val festivals: List<Festival> = emptyList()
)

class OfflineCalendarViewModel(
    private val preferencesRepository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _calendarState = MutableStateFlow(OfflineCalendarState())
    val calendarState: StateFlow<OfflineCalendarState> = _calendarState

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        viewModelScope.launch {
            preferencesRepository.festivalsEnabledFlow.collect { enabled ->
                _calendarState.update { it.copy(festivalsEnabled = enabled) }
                loadData(showLoadingIndicator = false)
            }
        }
        viewModelScope.launch {
            preferencesRepository.countryFlow.collect { country ->
                _calendarState.update { it.copy(countryCode = country) }
                loadData(showLoadingIndicator = false)
            }
        }
        viewModelScope.launch {
            loadData()
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
        loadData(showLoadingIndicator = true)
    }

    fun refresh() {
        if (_calendarState.value.isRefreshing) return
        _calendarState.update { it.copy(isRefreshing = true) }
        loadData(showLoadingIndicator = false)
    }

    private var loadJob: Job? = null

    private fun loadData(showLoadingIndicator: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isRefreshing = _calendarState.value.isRefreshing
            val startTime = System.currentTimeMillis()
            try {
                if (showLoadingIndicator) {
                    _calendarState.update { it.copy(isLoading = true) }
                }

                // Personal
                val currentMonthDate = _calendarState.value.currentMonthDate
                val cal = Calendar.getInstance()
                cal.time = currentMonthDate

                val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthPrefix = sdfMonth.format(cal.time)

                val user = withContext(Dispatchers.IO) { localAttendanceRepository.getUser() }
                val currencyCode = user?.currencyCode ?: "INR"
                val countryCode = user?.countryCode ?: _calendarState.value.countryCode

                val personal = withContext(Dispatchers.IO) {
                    localAttendanceRepository.getPersonalAttendance()
                }
                val filtered = personal.filter { com.synfusion.attendance.utils.AttendanceCalculationEngine.isDateInMonth(it.date, monthPrefix) }
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

                val isFestivalsEnabled = _calendarState.value.festivalsEnabled
                val year = cal.get(Calendar.YEAR)
                val month1Based = cal.get(Calendar.MONTH) + 1
                val monthlyFestivals = if (isFestivalsEnabled) {
                    FestivalRepository.getFestivalsForMonth(countryCode, year, month1Based)
                } else {
                    emptyList()
                }

                if (isRefreshing) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 400) {
                        delay(400 - elapsed)
                    }
                }

                _calendarState.update { it.copy(
                    personalAttendance = mappedRecords,
                    currencyCode = currencyCode,
                    countryCode = countryCode,
                    festivals = monthlyFestivals,
                    errorMessage = null
                ) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineCalendarVM", "Error loading data", e)
                _calendarState.update { it.copy(
                    errorMessage = "Failed to load calendar records: ${e.localizedMessage}"
                ) }
            } finally {
                _calendarState.update { it.copy(
                    isLoading = false,
                    isRefreshing = false
                ) }
            }
        }
    }

    fun changePersonalMonth(offsetMonths: Int) {
        val cal = Calendar.getInstance()
        cal.time = _calendarState.value.currentMonthDate
        cal.add(Calendar.MONTH, offsetMonths)
        _calendarState.update { it.copy(
            currentMonthDate = cal.time
        ) }
        loadData(showLoadingIndicator = true)
    }

    fun setPersonalMonth(date: Date) {
        _calendarState.update { it.copy(
            currentMonthDate = date
        ) }
        loadData(showLoadingIndicator = true)
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
            try {
                val docId = "personal_$date"

                val user = withContext(Dispatchers.IO) { localAttendanceRepository.getUser() }
                val userCurrency = user?.currencyCode ?: "INR"

                if (status == "present" || status == "absent") {
                    val currentWage = user?.dailyWage ?: 500.0
                    val attendance = AttendanceEntity(
                        id = docId,
                        workerId = null,
                        date = date,
                        status = status,
                        type = type,
                        reason = reason,
                        overtimeHours = overtimeHours,
                        note = note,
                        wage = currentWage,
                        currencyCode = userCurrency
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
                        wage = 0.0,
                        currencyCode = userCurrency
                    )
                    localAttendanceRepository.saveAttendance(advance)
                } else {
                    localAttendanceRepository.deleteAttendance("${docId}_advance", date)
                }

                loadData()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineCalendarVM", "Error marking calendar attendance", e)
                _calendarState.update { it.copy(errorMessage = "Failed to mark attendance.") }
            }
        }
    }

    fun removePersonalAttendance(date: String) {
        viewModelScope.launch {
            try {
                val docId = "personal_$date"
                localAttendanceRepository.deleteAttendance(docId, date)
                localAttendanceRepository.deleteAttendance("${docId}_advance", date)
                loadData()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("OfflineCalendarVM", "Error removing attendance", e)
                _calendarState.update { it.copy(errorMessage = "Failed to remove attendance.") }
            }
        }
    }
}
