package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.dailywork.attedance.data.FirestoreRepository

data class WorkerItem(
    val id: String,
    val name: String,
    val phone: String,
    val aadhar: String,
    val age: String,
    val workType: String,
    val wage: Double
)

data class WorkersState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val role: String = "",
    val workers: List<WorkerItem> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isPremium: Boolean = false
)

class WorkersViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(WorkersState())
    val state: StateFlow<WorkersState> = _state

    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

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
        viewModelScope.launch {
            repository.isPremiumFlow.collect { isPremium ->
                _state.value = _state.value.copy(isPremium = isPremium)
            }
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.value = _state.value.copy(isRefreshing = true)
        setupListener()
    }

    private fun setupListener() {
        auth.currentUser ?: return

        workersListener?.remove()

        workersListener = firestoreRepository.getContractorWorkers()
            ?.limit(50) // Basic pagination limit
            ?.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _state.value = _state.value.copy(isLoading = false,
                    isRefreshing = false, errorMessage = error?.message)
                    return@addSnapshotListener
                }

                val workerList = snapshot.documents.mapNotNull { doc ->
                    WorkerItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        aadhar = doc.getString("aadhar") ?: "",
                        age = doc.getString("age") ?: "",
                        workType = doc.getString("workType") ?: "Labour",
                        wage = doc.getDouble("wage") ?: 500.0
                    )
                }

                _state.value = _state.value.copy(
                    workers = workerList,
                    isLoading = false,
                    isRefreshing = false
                )
            }
    }

    fun saveWorker(worker: WorkerItem) {
        viewModelScope.launch {
            auth.currentUser ?: return@launch

            if (!_state.value.isPremium && _state.value.workers.size >= 5 && worker.id.isEmpty()) {
                _state.value = _state.value.copy(errorMessage = "Worker limit reached. Please upgrade to Premium for unlimited workers.")
                return@launch
            }

            _state.value = _state.value.copy(isSaving = true, errorMessage = null)

            try {
                val data: MutableMap<String, Any> = mutableMapOf(
                    "name" to worker.name,
                    "phone" to worker.phone,
                    "aadhar" to worker.aadhar,
                    "age" to worker.age,
                    "workType" to worker.workType,
                    "wage" to worker.wage
                )

                if (worker.id.isEmpty()) {
                    data["created_at"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                    firestoreRepository.saveWorker(null, data)
                } else {
                    firestoreRepository.saveWorker(worker.id, data)
                }
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
                firestoreRepository.deleteWorker(workerId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = "Failed to delete worker")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        workersListener?.remove()
        userListener?.remove()
    }
}
