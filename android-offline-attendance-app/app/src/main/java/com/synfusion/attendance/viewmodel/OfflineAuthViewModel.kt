package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            if (user != null) {
                // Recover local session safely for existing user/workspace
                repository.saveOnboardingCompleted(true)
                repository.saveAuthToken(user.id)
                repository.saveUserRole(user.role)
                repository.saveCountryAndCurrency(user.countryCode, user.currencyCode)
                _loginState.value = OfflineLoginState.Success(user.id)
            }
        }
    }

    fun resumeExistingWorkspace(): Boolean {
        var resumed = false
        viewModelScope.launch {
            val existingUser = localAttendanceRepository.getUser()
            if (existingUser != null) {
                repository.saveOnboardingCompleted(true)
                repository.saveAuthToken(existingUser.id)
                repository.saveUserRole(existingUser.role)
                repository.saveCountryAndCurrency(existingUser.countryCode, existingUser.currencyCode)
                _loginState.value = OfflineLoginState.Success(existingUser.id)
                resumed = true
            }
        }
        return resumed
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
                val finalName = name.ifBlank { existingUser?.name ?: "User" }
                val finalEmail = email.ifBlank { existingUser?.email ?: "" }
                val finalPhone = phoneNumber.ifBlank { existingUser?.phoneNumber ?: "" }
                val finalDailyWage = if (dailyWage > 0.0) dailyWage else (existingUser?.dailyWage ?: 500.0)
                val finalCreatedAt = existingUser?.createdAt ?: System.currentTimeMillis()

                val user = UserEntity(
                    id = userId,
                    name = finalName,
                    email = finalEmail,
                    phoneNumber = finalPhone,
                    role = "personal",
                    dailyWage = finalDailyWage,
                    countryCode = countryCode,
                    currencyCode = currencyCode,
                    createdAt = finalCreatedAt
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
