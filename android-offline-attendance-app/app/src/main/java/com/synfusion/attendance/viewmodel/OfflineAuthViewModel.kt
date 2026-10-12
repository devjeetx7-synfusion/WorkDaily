package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val isOnlineAccountSetupCompletedFlow = repository.isOnlineAccountSetupCompletedFlow
    val linkedFirebaseUidFlow = repository.linkedFirebaseUidFlow

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
                // Workspace detected: preload workspace config without auto-triggering login success
                repository.saveUserRole(user.role)
                repository.saveCountryAndCurrency(user.countryCode, user.currencyCode)
            }
        }
    }

    fun resumeExistingWorkspace(onComplete: ((Boolean) -> Unit)? = null): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            val existingUser = localAttendanceRepository.getUser()
            if (existingUser != null) {
                repository.saveOnboardingCompleted(true)
                repository.saveAuthToken(existingUser.id)
                repository.saveUserRole(existingUser.role)
                repository.saveCountryAndCurrency(existingUser.countryCode, existingUser.currencyCode)
                _loginState.value = OfflineLoginState.Success(existingUser.id)
                onComplete?.invoke(true)
            } else {
                onComplete?.invoke(false)
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

    suspend fun getExistingUser(): UserEntity? {
        return localAttendanceRepository.getUser()
    }

    suspend fun checkAccountMismatch(authenticatedUid: String): Boolean {
        val linkedUid = repository.linkedFirebaseUidFlow.firstOrNull()
        return !linkedUid.isNullOrBlank() && linkedUid != authenticatedUid
    }

    fun completeProfile(
        googleAccount: com.synfusion.attendance.auth.OnlineAccount,
        enteredName: String,
        enteredPhone: String,
        countryCode: String,
        currencyCode: String,
        dailyWage: Double,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _loginState.value = OfflineLoginState.Loading
            try {
                val existingUser = localAttendanceRepository.getUser()
                val userId: String
                val createdAt: Long
                val finalName: String
                val finalEmail: String
                val finalPhone: String
                val finalWage: Double
                val finalCountry: String
                val finalCurrency: String

                if (existingUser != null) {
                    // Existing User Upgrade: KEEP SAME LOCAL UUID & createdAt
                    userId = existingUser.id
                    createdAt = existingUser.createdAt

                    // Name merge rule: keep existing custom name if present, else use Google/entered name
                    finalName = if (existingUser.name.isNotBlank() && existingUser.name != "User") {
                        existingUser.name
                    } else if (enteredName.isNotBlank()) {
                        enteredName
                    } else {
                        googleAccount.displayName ?: existingUser.name
                    }

                    finalEmail = googleAccount.email?.takeIf { it.isNotBlank() } ?: existingUser.email
                    finalPhone = enteredPhone.ifBlank { existingUser.phoneNumber }
                    finalWage = if (dailyWage > 0.0) dailyWage else existingUser.dailyWage
                    finalCountry = countryCode.ifBlank { existingUser.countryCode }
                    finalCurrency = currencyCode.ifBlank { existingUser.currencyCode }
                } else {
                    // Brand-new user: generate new local UUID (distinct from Firebase UID)
                    userId = UUID.randomUUID().toString()
                    createdAt = System.currentTimeMillis()

                    finalName = if (enteredName.isNotBlank()) enteredName else (googleAccount.displayName ?: "User")
                    finalEmail = googleAccount.email ?: ""
                    finalPhone = enteredPhone
                    finalWage = if (dailyWage > 0.0) dailyWage else 500.0
                    finalCountry = if (countryCode.isNotBlank()) countryCode else "IN"
                    finalCurrency = if (currencyCode.isNotBlank()) currencyCode else "INR"
                }

                val user = UserEntity(
                    id = userId,
                    name = finalName,
                    email = finalEmail,
                    phoneNumber = finalPhone,
                    role = "personal",
                    dailyWage = finalWage,
                    countryCode = finalCountry,
                    currencyCode = finalCurrency,
                    createdAt = createdAt
                )

                localAttendanceRepository.saveUser(user)
                repository.saveCountryAndCurrency(finalCountry, finalCurrency)
                repository.saveAuthToken(userId)
                repository.saveUserRole("personal")
                repository.saveOnboardingCompleted(true)
                repository.saveOnlineAccountSetupCompleted(true)
                repository.saveLinkedFirebaseUid(googleAccount.uid)

                // Save Google photo URL to its own separate account key (leaving custom local photo path untouched)
                if (!googleAccount.photoUrl.isNullOrBlank()) {
                    repository.saveGooglePhotoUrl(googleAccount.photoUrl)
                }

                _loginState.value = OfflineLoginState.Success(userId)
                onComplete(true, null)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to complete profile"
                _loginState.value = OfflineLoginState.Error(errorMsg)
                onComplete(false, errorMsg)
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
