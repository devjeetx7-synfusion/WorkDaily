package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.AdminStats
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AdminFirestoreRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(AdminStats())
    val stats: StateFlow<AdminStats> = _stats

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            repository.getAdminStats().collectLatest { stats ->
                val jobsCount = repository.getTotalJobsCount()
                _stats.value = stats.copy(totalJobs = jobsCount)
            }
        }
    }
}
