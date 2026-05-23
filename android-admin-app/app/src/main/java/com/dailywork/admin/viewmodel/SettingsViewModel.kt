package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.AppConfig
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AdminFirestoreRepository
) : ViewModel() {

    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config

    init {
        viewModelScope.launch {
            repository.getAppConfig().collectLatest {
                _config.value = it
            }
        }
    }

    fun updateConfig(newConfig: AppConfig) {
        viewModelScope.launch {
            repository.updateAppConfig(newConfig)
        }
    }
}
