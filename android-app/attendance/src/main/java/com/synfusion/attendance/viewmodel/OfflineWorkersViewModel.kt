package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.WorkerEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class OfflineWorkerItem(
    val id: String,
    val name: String,
    val phone: String,
    val aadhar: String,
    val age: String,
    val workType: String,
    val wage: Double
)

data class OfflineWorkersState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val role: String = "",
    val workers: List<OfflineWorkerItem> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val hasAddWorkerGrant: Boolean = true // Permanently unlocked for offline version
)

class OfflineWorkersViewModel(
    private val repository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OfflineWorkersState())
    val state: StateFlow<OfflineWorkersState> = _state

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _state.value = _state.value.copy(role = role)
                    if (role == "contractor") {
                        setupListener()
                    }
                }
            }
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.value = _state.value.copy(isRefreshing = true)
        setupListener()
    }

    private fun setupListener() {
        viewModelScope.launch {
            localAttendanceRepository.getAllWorkersFlow().collect { list ->
                val mapped = list.map { doc ->
                    OfflineWorkerItem(
                        id = doc.id,
                        name = doc.name,
                        phone = doc.phone,
                        aadhar = doc.aadhaar,
                        age = doc.age.toString(),
                        workType = doc.workType,
                        wage = doc.dailyWage
                    )
                }
                _state.value = _state.value.copy(
                    workers = mapped,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun saveWorker(worker: OfflineWorkerItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            try {
                val finalId = if (worker.id.isEmpty()) UUID.randomUUID().toString() else worker.id
                val entity = WorkerEntity(
                    id = finalId,
                    name = worker.name,
                    phone = worker.phone,
                    aadhaar = worker.aadhar,
                    age = worker.age.toIntOrNull() ?: 18,
                    workType = worker.workType,
                    dailyWage = worker.wage
                )
                localAttendanceRepository.saveWorker(entity)
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = "Failed to save worker: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun deleteWorker(workerId: String) {
        viewModelScope.launch {
            try {
                localAttendanceRepository.deleteWorker(workerId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = "Failed to delete worker")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
