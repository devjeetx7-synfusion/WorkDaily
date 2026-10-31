package com.synfusion.workdailyadmin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminErrorType
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.DashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val stats: DashboardStats, val isFallback: Boolean = false) : DashboardUiState()
    data class Error(
        val message: String,
        val errorType: AdminErrorType = AdminErrorType.UNKNOWN_ERROR,
        val canRetry: Boolean = true
    ) : DashboardUiState()
}

class DashboardViewModel @JvmOverloads constructor(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var isSyncing: Boolean = false
    private var lastSyncAttemptMillis: Long = 0L

    init {
        loadDashboardStats()
    }

    fun loadDashboardStats(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (isSyncing) return
        if (!force && now - lastSyncAttemptMillis < 600L) {
            // Debounce rapid repeated clicks
            return
        }

        lastSyncAttemptMillis = now
        isSyncing = true
        _uiState.value = DashboardUiState.Loading

        viewModelScope.launch {
            try {
                when (val res = repository.getDashboardStats()) {
                    is Result.Success -> {
                        _uiState.value = DashboardUiState.Success(res.data, isFallback = res.isFallback)
                    }
                    is Result.Error -> {
                        _uiState.value = DashboardUiState.Error(
                            message = res.message,
                            errorType = res.errorType,
                            canRetry = res.canRetry
                        )
                    }
                }
            } finally {
                isSyncing = false
            }
        }
    }
}
