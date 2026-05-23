package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.Report
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: AdminFirestoreRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    private val _isPerformingAction = MutableStateFlow(false)
    val isPerformingAction: StateFlow<Boolean> = _isPerformingAction

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            repository.getReports().collectLatest {
                _reports.value = it
            }
        }
    }

    fun ignoreReport(reportId: String) {
        viewModelScope.launch {
            _isPerformingAction.value = true
            repository.updateReportStatus(reportId, "ignored")
            _isPerformingAction.value = false
        }
    }

    fun resolveReport(reportId: String) {
        viewModelScope.launch {
            _isPerformingAction.value = true
            repository.updateReportStatus(reportId, "resolved")
            _isPerformingAction.value = false
        }
    }
}
