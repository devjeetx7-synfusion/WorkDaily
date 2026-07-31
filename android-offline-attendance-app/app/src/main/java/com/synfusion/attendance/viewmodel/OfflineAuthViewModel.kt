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
) : ViewModel() {

    private val _loginState = MutableStateFlow<OfflineLoginState>(OfflineLoginState.Idle)
    val loginState: StateFlow<OfflineLoginState> = _loginState

    val userRoleFlow = repository.userRoleFlow
    val authTokenFlow = repository.authTokenFlow

    init {
        viewModelScope.launch {
            val user = localAttendanceRepository.getUser()
            if (user != null) {
                repository.saveAuthToken(user.id)
                _loginState.value = OfflineLoginState.Success(user.id)
            }
        }
    }

    fun checkForUserAndLogin() {
        viewModelScope.launch {
            val user = localAttendanceRepository.getUser()
            if (user != null) {
                repository.saveAuthToken(user.id)
                _loginState.value = OfflineLoginState.Success(user.id)
            }
        }
    }

    fun login(name: String, email: String, phoneNumber: String) {
        viewModelScope.launch {
            _loginState.value = OfflineLoginState.Loading
            try {
                val userId = UUID.randomUUID().toString()
                val user = UserEntity(
                    id = userId,
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber,
                    role = "personal"
                )
                localAttendanceRepository.saveUser(user)
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
            localAttendanceRepository.clearSession()
            repository.clearSession()
        }
    }
}
