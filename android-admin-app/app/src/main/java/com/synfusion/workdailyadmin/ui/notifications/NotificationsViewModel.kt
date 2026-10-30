package com.synfusion.workdailyadmin.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.NotificationCampaign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(val campaigns: List<NotificationCampaign>) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadCampaigns()
    }

    fun loadCampaigns() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            when (val res = repository.getNotificationCampaigns()) {
                is Result.Success -> _uiState.value = NotificationsUiState.Success(res.data)
                is Result.Error -> _uiState.value = NotificationsUiState.Error(res.message)
            }
        }
    }

    fun sendCampaign(title: String, body: String, audience: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.sendNotificationCampaign(title, body, audience)) {
                is Result.Success -> {
                    loadCampaigns()
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
