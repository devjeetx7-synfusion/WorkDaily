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
    data class Authenticated(val uid: String, val email: String, val role: AdminRole) : AdminAuthState()
    data class Unauthorized(val reason: String) : AdminAuthState()
    data class Error(val message: String) : AdminAuthState()
}

class AdminAuthViewModel @JvmOverloads constructor(
    private val authSupplier: () -> FirebaseAuth? = {
        try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    },
    private val firestoreSupplier: () -> FirebaseFirestore? = {
        try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
    }
) : ViewModel() {
    private val auth get() = authSupplier()
    private val firestore get() = firestoreSupplier()
    private val _authState = MutableStateFlow<AdminAuthState>(AdminAuthState.Idle)
    val authState: StateFlow<AdminAuthState> = _authState.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        val currentAuth = auth
        if (currentAuth == null) {
            _authState.value = AdminAuthState.Error(
                "Firebase Authentication could not initialize. Check the Admin app Firebase configuration and rebuild the APK."
            )
            return
        }

        val user = currentAuth.currentUser
        if (user == null) {
            _authState.value = AdminAuthState.Idle
            return
        }

        viewModelScope.launch {
            _authState.value = AdminAuthState.Loading
            try {
                val tokenResult = user.getIdToken(true).await()
                val isAdminClaim = tokenResult.claims["admin"] as? Boolean ?: false
                if (isAdminClaim) {
                    val roleStr = tokenResult.claims["role"] as? String
                    if (roleStr.isNullOrBlank()) {
                        currentAuth.signOut()
                        _authState.value = AdminAuthState.Unauthorized(
                            "Administrator role is missing. Ask a super administrator to reassign your role."
                        )
                        return@launch
                    }
                    _authState.value = AdminAuthState.Authenticated(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = AdminRole.fromString(roleStr)
                    )
                } else {
                    val db = firestore
                    if (db == null) {
                        _authState.value = AdminAuthState.Error(
                            "Firebase Firestore is unavailable. Check Firebase configuration and network connection."
                        )
                        return@launch
                    }
                    val adminDoc = db.collection("admin_roles").document(user.uid).get().await()
                    if (!adminDoc.exists()) {
                        currentAuth.signOut()
                        _authState.value = AdminAuthState.Unauthorized(
                            "Access denied: this account is not registered as a WorkDaily administrator."
                        )
                        return@launch
                    }
                    val roleStr = adminDoc.getString("role")
                    if (roleStr.isNullOrBlank()) {
                        currentAuth.signOut()
                        _authState.value = AdminAuthState.Unauthorized(
                            "Administrator role is missing from the admin record."
                        )
                        return@launch
                    }
                    _authState.value = AdminAuthState.Authenticated(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = AdminRole.fromString(roleStr)
                    )
                }
            } catch (e: Exception) {
                _authState.value = AdminAuthState.Error(
                    e.localizedMessage ?: "Could not verify the administrator session. Check network access and try again."
                )
            }
        }
    }

    fun login(email: String, password: String) {
        val currentAuth = auth
        if (currentAuth == null) {
            _authState.value = AdminAuthState.Error(
                "Firebase Authentication could not initialize. Check the Admin app Firebase configuration and rebuild the APK."
            )
            return
        }
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AdminAuthState.Error("Email and password are required.")
            return
        }

        viewModelScope.launch {
            _authState.value = AdminAuthState.Loading
            try {
                // Normalize email only. Passwords must be passed exactly as entered.
                val result = currentAuth.signInWithEmailAndPassword(email.trim(), password).await()
                if (result.user != null) {
                    checkCurrentSession()
                } else {
                    _authState.value = AdminAuthState.Error("Firebase sign-in returned no user.")
                }
            } catch (e: Exception) {
                _authState.value = AdminAuthState.Error(
                    e.localizedMessage ?: "Sign-in failed. Verify credentials and Firebase Authentication settings."
                )
            }
        }
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (_: Exception) {
            // Clear local UI state even if SDK cleanup fails.
        }
        _authState.value = AdminAuthState.Idle
    }
}
