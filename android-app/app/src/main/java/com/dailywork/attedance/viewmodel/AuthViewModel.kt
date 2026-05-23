package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.dailywork.attedance.data.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel(
    val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    val userRoleFlow = repository.userRoleFlow
    val authTokenFlow = repository.authTokenFlow

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                repository.saveAuthToken(currentUser.uid)
                _loginState.value = LoginState.Success(currentUser.uid)
            }
        }
    }

    private fun canProceed(): Boolean {
        val state = _loginState.value
        return state !is LoginState.Loading
    }

    fun resetToIdle() {
        _loginState.value = LoginState.Idle
    }

    fun loginWithGoogleCredential(idToken: String) {
        if (!canProceed()) return
        viewModelScope.launch {
             _loginState.value = LoginState.Loading
             try {
                 val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                 val result = auth.signInWithCredential(credential).await()
                 val user = result.user
                 if (user != null) {
                     val uid = user.uid

                     // Upsert user document just like email registration to prevent crash on new Google user
                     val userData = mapOf(
                        "uid" to uid,
                        "name" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "createdAt" to System.currentTimeMillis()
                     )
                     com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(userData, com.google.firebase.firestore.SetOptions.merge())
                        .await()

                     fetchAndSaveUserRole(uid)
                     repository.saveAuthToken(uid)
                     _loginState.value = LoginState.Success(uid)
                 } else {
                     _loginState.value = LoginState.Error("Google Sign-In failed: Null user")
                 }
             } catch (e: Exception) {
                 _loginState.value = LoginState.Error(e.message ?: "Google Sign-In failed")
             }
        }
    }

    fun setLoginError(error: String) {
        _loginState.value = LoginState.Error(error)
    }

    fun savePreferences(role: String, language: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val updates = mapOf(
                        "role" to role,
                        "language" to language
                    )
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    repository.saveUserRole(role)
                    repository.saveLanguage(language)
                } catch (e: Exception) {
                    // Handle error if needed
                }
            }
        }
    }

    fun saveRole(role: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val updates = mapOf("role" to role)
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    repository.saveUserRole(role)
                } catch (e: Exception) {
                    // Handle error if needed
                }
            }
        }
    }

    private suspend fun fetchAndSaveUserRole(uid: String) {
        try {
            val document = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()

            val isBlocked = document.getBoolean("isBlocked") ?: false
            if (isBlocked) {
                _loginState.value = LoginState.Blocked
                return
            }

            val role = document.getString("role")
            if (role != null) {
                repository.saveUserRole(role)
            }
        } catch (e: Exception) {
            // Log error or handle gracefully
        }
    }

    fun logout() {
        _loginState.value = LoginState.Idle
        auth.signOut()
        viewModelScope.launch {
            repository.clearSession()
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    object Blocked : LoginState()
    data class Error(val message: String) : LoginState()
}
