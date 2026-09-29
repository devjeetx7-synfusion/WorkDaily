package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.OfflinePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OfflineOnboardingState(
    val currentStep: Int = 0, // 0: Welcome, 1: Attendance, 2: Earnings, 3: Passbook, 4: Final
    val isCompleted: Boolean = false
)

class OfflineOnboardingViewModel(
    private val repository: OfflinePreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OfflineOnboardingState())
    val state: StateFlow<OfflineOnboardingState> = _state

    fun onNextStep() {
        val next = (_state.value.currentStep + 1).coerceAtMost(4)
        _state.update { it.copy(currentStep = next) }
    }

    fun onPreviousStep() {
        val prev = (_state.value.currentStep - 1).coerceAtLeast(0)
        _state.update { it.copy(currentStep = prev) }
    }

    fun goToStep(step: Int) {
        _state.update { it.copy(currentStep = step.coerceIn(0, 4)) }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveOnboardingCompleted(true)
            _state.update { it.copy(isCompleted = true) }
            onSuccess()
        }
    }
}
