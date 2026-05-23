package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    val userRoleFlow = repository.userRoleFlow
    val authTokenFlow = repository.authTokenFlow

    fun login(phoneNumber: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            // Simulate API Call
            kotlinx.coroutines.delay(1000)
            if (phoneNumber.isNotEmpty() && pass.isNotEmpty()) {
                val mockToken = "mock_jwt_token_12345"
                repository.saveAuthToken(mockToken)
                _loginState.value = LoginState.Success(mockToken)
            } else {
                _loginState.value = LoginState.Error("Invalid credentials")
            }
        }
    }

    fun saveRole(role: String) {
        viewModelScope.launch {
            repository.saveUserRole(role)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearSession()
            _loginState.value = LoginState.Idle
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
