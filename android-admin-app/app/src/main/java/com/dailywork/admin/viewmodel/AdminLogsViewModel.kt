package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.AdminLog
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminLogsViewModel @Inject constructor(
    private val repository: AdminFirestoreRepository
) : ViewModel() {

    private val _logs = MutableStateFlow<List<AdminLog>>(emptyList())
    val logs: StateFlow<List<AdminLog>> = _logs

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            repository.getAdminLogs().collectLatest {
                _logs.value = it
            }
        }
    }
}
