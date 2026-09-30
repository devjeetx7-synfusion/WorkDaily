package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

sealed class OfflineLoginState {
    object Idle : OfflineLoginState()
    object Loading : OfflineLoginState()
    data class Success(val token: String) : OfflineLoginState()
    object Blocked : OfflineLoginState()
    data class Error(val message: String) : OfflineLoginState()
}

class OfflineAuthViewModel(
    val repository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _loginState = MutableStateFlow<OfflineLoginState>(OfflineLoginState.Idle)
    val loginState: StateFlow<OfflineLoginState> = _loginState

    val userRoleFlow = repository.userRoleFlow
    val authTokenFlow = repository.authTokenFlow

    private var checkJob: kotlinx.coroutines.Job? = null

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        checkForUserAndLogin()
    }

    override fun onCleared() {
        super.onCleared()
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.unregisterViewModel(this)
    }

    override fun cancelActiveJobs() {
        checkJob?.cancel()
    }

    override fun refreshOnRestore() {
        checkJob = viewModelScope.launch {
            checkForUserAndLogin()
        }
    }

    fun checkForUserAndLogin() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            val user = localAttendanceRepository.getUser()
            val currentToken = repository.authTokenFlow.first()
            if (user != null) {
                // Legacy user migration: preserve onboarding completed state for existing users
                repository.saveOnboardingCompleted(true)
                if (currentToken != null) {
                    repository.saveCountryAndCurrency(user.countryCode, user.currencyCode)
                    _loginState.value = OfflineLoginState.Success(user.id)
                }
            }
        }
    }

    fun login(
        name: String,
        email: String,
        phoneNumber: String,
        countryCode: String = "IN",
        currencyCode: String = "INR",
        dailyWage: Double = 500.0
    ) {
        viewModelScope.launch {
            _loginState.value = OfflineLoginState.Loading
            try {
                val existingUser = localAttendanceRepository.getUser()
                val userId = existingUser?.id ?: UUID.randomUUID().toString()
                val user = UserEntity(
                    id = userId,
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber,
                    role = "personal",
                    dailyWage = dailyWage,
                    countryCode = countryCode,
                    currencyCode = currencyCode
                )
                localAttendanceRepository.saveUser(user)
                repository.saveCountryAndCurrency(countryCode, currencyCode)
                repository.saveAuthToken(userId)
                repository.saveUserRole("personal")
                _loginState.value = OfflineLoginState.Success(userId)
            } catch (e: Exception) {
                _loginState.value = OfflineLoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        _loginState.value = OfflineLoginState.Idle
        viewModelScope.launch {
            repository.clearAuthSession()
        }
    }
}
