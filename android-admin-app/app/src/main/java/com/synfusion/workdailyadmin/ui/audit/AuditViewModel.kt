package com.synfusion.workdailyadmin.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.AdminAuditLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuditUiState {
    object Loading : AuditUiState()
    data class Success(val logs: List<AdminAuditLog>) : AuditUiState()
    data class Error(val message: String) : AuditUiState()
}

class AuditViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuditUiState>(AuditUiState.Loading)
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    init {
        loadAuditLogs()
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            _uiState.value = AuditUiState.Loading
            when (val res = repository.getAuditLogs(limit = 50)) {
                is Result.Success -> _uiState.value = AuditUiState.Success(res.data)
                is Result.Error -> _uiState.value = AuditUiState.Error(res.message)
            }
        }
    }
}
