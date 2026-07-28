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
import java.util.*

data class OfflineHistoryRecord(
    val id: String,
    val workerId: String,
    val workerName: String,
    val date: String,
    val type: String, // "Attendance", "Payment"
    val description: String,
    val amount: Double? = null,
    val status: String? = null, // "Present", "Absent", "Half Day"
    val timestamp: Long = 0
)

data class OfflineWorkerHistoryState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val records: List<OfflineHistoryRecord> = emptyList(),
    val filteredRecords: List<OfflineHistoryRecord> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val isPremium: Boolean = true // Permanently unlocked for offline version
)

class OfflineWorkerHistoryViewModel(
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OfflineWorkerHistoryState())
    val state: StateFlow<OfflineWorkerHistoryState> = _state

    private val workerNames = mutableMapOf<String, String>()

    init {
        loadHistory()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            localAttendanceRepository.getAllWorkersFlow().collect { workers ->
                workerNames.clear()
                workers.forEach { worker ->
                    workerNames[worker.id] = worker.name
                }

                val allRecords = mutableListOf<OfflineHistoryRecord>()
                workers.forEach { worker ->
                    val attendanceList = localAttendanceRepository.getWorkerAttendance(worker.id)
                    attendanceList.forEach { rec ->
                        val dateStr = formatDate(rec.date)
                        val cleanedNote = OvertimeCalculator.cleanNote(rec.note) ?: ""

                        if (rec.status == "present" || rec.status == "absent") {
                            allRecords.add(
                                OfflineHistoryRecord(
                                    id = "${worker.id}_${rec.id}_att",
                                    workerId = worker.id,
                                    workerName = worker.name,
                                    date = dateStr,
                                    type = "Attendance",
                                    description = if (rec.status == "present") "Daily Work" else "Absent: $cleanedNote".trimEnd(':', ' '),
                                    status = when {
                                        rec.status == "absent" -> "Absent"
                                        rec.type == "half" -> "Half Day"
                                        else -> "Present"
                                    },
                                    timestamp = rec.createdAt
                                )
                            )
                        }

                        if (rec.advanceAmount > 0) {
                            allRecords.add(
                                OfflineHistoryRecord(
                                    id = "${worker.id}_${rec.id}_pay",
                                    workerId = worker.id,
                                    workerName = worker.name,
                                    date = dateStr,
                                    type = "Payment",
                                    description = "Advance Payment",
                                    amount = rec.advanceAmount,
                                    timestamp = rec.createdAt
                                )
                            )
                        }
                    }
                }

                val sortedList = allRecords.sortedByDescending {
                    if (it.timestamp > 0) it.timestamp else parseDateToLong(it.date)
                }

                _state.update { current ->
                    current.copy(records = sortedList, isLoading = false, isRefreshing = false).also { applyFilters(it) }
                }
            }
        }
    }

    private fun applyFilters(currentState: OfflineWorkerHistoryState) {
        var filtered = currentState.records

        if (currentState.searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.workerName.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        if (currentState.selectedFilter != "All") {
            filtered = filtered.filter { it.type == currentState.selectedFilter }
        }

        _state.update { it.copy(filteredRecords = filtered) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilters(_state.value)
    }

    fun onFilterChange(filter: String) {
        _state.update { it.copy(selectedFilter = filter) }
        applyFilters(_state.value)
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val outputSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            date?.let { outputSdf.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseDateToLong(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
