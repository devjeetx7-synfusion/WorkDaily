package com.synfusion.workdailyadmin.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.synfusion.workdailyadmin.data.models.AdminRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AdminAuthState {
    object Idle : AdminAuthState()
    object Loading : AdminAuthState()
    data class Authenticated(
        val uid: String,
        val email: String,
        val role: AdminRole
    ) : AdminAuthState()
    data class Unauthorized(val reason: String) : AdminAuthState()
    data class Error(val message: String) : AdminAuthState()
}

class AdminAuthViewModel(
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : ViewModel() {

    private val auth get() = authSupplier()
    private val firestore get() = firestoreSupplier()

    private val _authState = MutableStateFlow<AdminAuthState>(AdminAuthState.Idle)
    val authState: StateFlow<AdminAuthState> = _authState.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AdminAuthState.Idle
            return
        }

        viewModelScope.launch {
            _authState.value = AdminAuthState.Loading
            try {
                // Refresh token to get updated custom claims
                val tokenResult = user.getIdToken(true).await()
                val isAdminClaim = tokenResult.claims["admin"] as? Boolean ?: false

                if (isAdminClaim) {
                    val roleStr = tokenResult.claims["role"] as? String ?: "ANALYST"
                    val role = AdminRole.fromString(roleStr)
                    _authState.value = AdminAuthState.Authenticated(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = role
                    )
                } else {
                    // Fallback to checking admin_roles document
                    val adminDoc = firestore.collection("admin_roles").document(user.uid).get().await()
                    if (adminDoc.exists()) {
                        val roleStr = adminDoc.getString("role") ?: "ANALYST"
                        val role = AdminRole.fromString(roleStr)
                        _authState.value = AdminAuthState.Authenticated(
                            uid = user.uid,
                            email = user.email ?: "",
                            role = role
                        )
                    } else {
                        auth.signOut()
                        _authState.value = AdminAuthState.Unauthorized(
                            "Access Denied: Account is not authorized as WorkDaily Administrator."
                        )
                    }
                }
            } catch (e: Exception) {
                _authState.value = AdminAuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AdminAuthState.Error("Email and password are required.")
            return
        }

        viewModelScope.launch {
            _authState.value = AdminAuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
                val user = result.user
                if (user != null) {
                    checkCurrentSession()
                } else {
                    _authState.value = AdminAuthState.Error("Invalid login credentials.")
                }
            } catch (e: Exception) {
                _authState.value = AdminAuthState.Error(e.message ?: "Failed to sign in.")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AdminAuthState.Idle
    }
}
