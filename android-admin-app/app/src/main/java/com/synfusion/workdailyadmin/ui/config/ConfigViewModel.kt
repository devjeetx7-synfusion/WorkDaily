package com.synfusion.workdailyadmin.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.AppConfigData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConfigUiState {
    object Loading : ConfigUiState()
    data class Success(val config: AppConfigData) : ConfigUiState()
    data class Error(val message: String) : ConfigUiState()
}

class ConfigViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConfigUiState>(ConfigUiState.Loading)
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = ConfigUiState.Loading
            when (val res = repository.getAppConfig()) {
                is Result.Success -> _uiState.value = ConfigUiState.Success(res.data)
                is Result.Error -> _uiState.value = ConfigUiState.Error(res.message)
            }
        }
    }

    fun updateConfig(newConfig: AppConfigData, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.updateAppConfig(newConfig)) {
                is Result.Success -> {
                    _uiState.value = ConfigUiState.Success(newConfig)
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
