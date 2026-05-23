package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val role: String = "",
    val isLoading: Boolean = false,
    val totalWorkers: String = "0",
    val presentWorkers: String = "0",
    val pendingAmount: String = "₹0",
    val daysWorked: String = "0",
    val earnings: String = "₹0"
)

class DashboardViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    loadDashboardData(role)
                }
            }
        }
    }

    private fun loadDashboardData(role: String) {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, role = role)

            // Simulate network delay
            kotlinx.coroutines.delay(1000)

            if (role == "CONTRACTOR") {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    totalWorkers = "12",
                    presentWorkers = "10",
                    pendingAmount = "₹4500"
                )
            } else {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    daysWorked = "22",
                    earnings = "₹11000"
                )
            }
        }
    }
}
