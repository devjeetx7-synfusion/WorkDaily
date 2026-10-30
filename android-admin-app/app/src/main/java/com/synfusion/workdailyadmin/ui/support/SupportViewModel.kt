package com.synfusion.workdailyadmin.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.SupportTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SupportUiState {
    object Loading : SupportUiState()
    data class Success(val tickets: List<SupportTicket>) : SupportUiState()
    data class Error(val message: String) : SupportUiState()
}

class SupportViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SupportUiState>(SupportUiState.Loading)
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    init {
        loadTickets()
    }

    fun loadTickets() {
        viewModelScope.launch {
            _uiState.value = SupportUiState.Loading
            when (val res = repository.getSupportTickets()) {
                is Result.Success -> _uiState.value = SupportUiState.Success(res.data)
                is Result.Error -> _uiState.value = SupportUiState.Error(res.message)
            }
        }
    }

    fun updateTicket(ticketId: String, status: String?, replyText: String?, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.updateSupportTicket(ticketId, status, replyText)) {
                is Result.Success -> {
                    loadTickets()
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
