package com.synfusion.workdailyadmin.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.SystemHealthData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HealthUiState {
    object Loading : HealthUiState()
    data class Success(val health: SystemHealthData) : HealthUiState()
    data class Error(val message: String) : HealthUiState()
}

class HealthViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        loadHealth()
    }

    fun loadHealth() {
        viewModelScope.launch {
            _uiState.value = HealthUiState.Loading
            when (val res = repository.getSystemHealth()) {
                is Result.Success -> _uiState.value = HealthUiState.Success(res.data)
                is Result.Error -> _uiState.value = HealthUiState.Error(res.message)
            }
        }
    }
}
