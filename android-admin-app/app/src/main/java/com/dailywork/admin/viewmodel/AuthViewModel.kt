package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                verifyAdminRole(user.uid)
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email == "admin@workdaily.com" && pass == "admin@workdaily.pass") {
            _authState.value = AuthState.Authenticated
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                verifyAdminRole(result.user?.uid ?: "")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    private suspend fun verifyAdminRole(uid: String) {
        try {
            val doc = db.collection("users").document(uid).get().await()
            val role = doc.getString("role")
            if (role == "admin") {
                _authState.value = AuthState.Authenticated
            } else {
                auth.signOut()
                _authState.value = AuthState.Error("Access Denied: Admin only")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Verification failed")
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
