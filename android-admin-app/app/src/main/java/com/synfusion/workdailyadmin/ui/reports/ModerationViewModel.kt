package com.synfusion.workdailyadmin.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.ReportItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportsUiState {
    object Loading : ReportsUiState()
    data class Success(val reports: List<ReportItem>) : ReportsUiState()
    data class Error(val message: String) : ReportsUiState()
}

class ModerationViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    var statusFilter = MutableStateFlow("PENDING")

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading
            when (val res = repository.getReports(status = statusFilter.value)) {
                is Result.Success -> _uiState.value = ReportsUiState.Success(res.data)
                is Result.Error -> _uiState.value = ReportsUiState.Error(res.message)
            }
        }
    }

    fun updateReportStatus(
        reportId: String,
        status: String,
        resolutionReason: String,
        priority: String? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.updateReportStatus(reportId, status, resolutionReason, priority)) {
                is Result.Success -> {
                    loadReports()
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
