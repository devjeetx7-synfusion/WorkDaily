package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.dailywork.attedance.data.FirestoreRepository

data class SettingsState(
    val name: String = "",
    val originalName: String = "",
    val dailyWage: Double = 500.0,
    val originalWage: Double = 500.0,
    val workType: String = "",
    val role: String = "",
    val originalRole: String = "",
    val phone: String = "",
    val originalPhone: String = "",
    val profileImageUrl: String = "",
    val originalProfileImageUrl: String = "",
    val profileImageUri: Uri? = null,
    val language: String = "en",
    val isDarkMode: Boolean = false,
    val isRemindersEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val savedMessage: String? = null,
    val isPremium: Boolean = false,
    val isSplashEnabled: Boolean = true,
    val triggerRestart: Boolean = false,
    val isLanguageEnabled: Boolean = true,
    val isLanguageActivityEnabled: Boolean = true,
    val isRoleUiEnabled: Boolean = true
)

class SettingsViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var configListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        setupListener()
        setupConfigListener()
        viewModelScope.launch {
            repository.darkModeFlow.collect { isDark ->
                _state.value = _state.value.copy(isDarkMode = isDark ?: false)
            }
        }
        viewModelScope.launch {
            repository.remindersFlow.collect { enabled ->
                _state.value = _state.value.copy(isRemindersEnabled = enabled)
            }
        }
        viewModelScope.launch {
            repository.isPremiumFlow.collect { isPremium ->
                _state.value = _state.value.copy(isPremium = isPremium)
            }
        }
        viewModelScope.launch {
            repository.splashEnabledFlow.collect { enabled ->
                _state.value = _state.value.copy(isSplashEnabled = enabled)
            }
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            repository.saveThemePreference(isDark)
        }
    }

    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveRemindersPreference(enabled)
        }
    }

    fun toggleSplash(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSplashEnabledPreference(enabled)
        }
    }

    private fun setupConfigListener() {
        configListener = db.collection("config").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val languageEnabled = snapshot.getBoolean("languageEnabled") ?: true
                    val languageActivityEnabled = snapshot.getBoolean("languageActivityEnabled") ?: true
                    val roleUiEnabled = snapshot.getBoolean("roleUiEnabled") ?: true
                    _state.value = _state.value.copy(
                        isLanguageEnabled = languageEnabled,
                        isLanguageActivityEnabled = languageActivityEnabled,
                        isRoleUiEnabled = roleUiEnabled
                    )
                }
            }
    }

    private fun setupListener() {
        val user = auth.currentUser ?: return

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val currentName = snapshot.getString("name") ?: user.displayName ?: ""
                    val currentWage = snapshot.getDouble("daily_wage") ?: 500.0
                    val currentRole = snapshot.getString("role") ?: ""
                    val currentPhone = snapshot.getString("phone") ?: ""
                    val currentProfileImageUrl = snapshot.getString("profileImageUrl") ?: ""

                    _state.value = _state.value.copy(
                        name = currentName,
                        originalName = currentName,
                        dailyWage = currentWage,
                        originalWage = currentWage,
                        workType = snapshot.getString("workType") ?: "",
                        role = currentRole,
                        originalRole = currentRole,
                        phone = currentPhone,
                        originalPhone = currentPhone,
                        profileImageUrl = currentProfileImageUrl,
                        originalProfileImageUrl = currentProfileImageUrl,
                        language = snapshot.getString("language") ?: "en"
                    )
                }
            }
    }

    fun onNameChange(newName: String) {
        _state.value = _state.value.copy(name = newName)
    }

    fun onWageChange(newWage: String) {
        val wageDouble = newWage.toDoubleOrNull() ?: 0.0
        _state.value = _state.value.copy(dailyWage = wageDouble)
    }

    fun onRoleChange(newRole: String) {
        _state.value = _state.value.copy(role = newRole)
    }

    fun onPhoneChange(newPhone: String) {
        _state.value = _state.value.copy(phone = newPhone)
    }

    fun onProfileImageSelected(uri: Uri?) {
        _state.value = _state.value.copy(profileImageUri = uri)
    }

    fun saveChanges() {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch

            val currentState = _state.value
            val roleChanged = currentState.role != currentState.originalRole
            val newRole = currentState.role

            _state.update { it.copy(isSaving = true) }

            try {
                var finalImageUrl = currentState.profileImageUrl
                val imageUri = currentState.profileImageUri

                if (imageUri != null) {
                    val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/${user.uid}.jpg")
                    storageRef.putFile(imageUri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }

                val updates = mutableMapOf<String, Any>(
                    "name" to currentState.name,
                    "daily_wage" to currentState.dailyWage,
                    "role" to currentState.role,
                    "phone" to currentState.phone,
                    "profileImageUrl" to finalImageUrl
                )

                db.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()

                if (roleChanged) {
                    repository.saveUserRole(newRole)
                }

                _state.update { it.copy(
                    originalName = it.name,
                    originalWage = it.dailyWage,
                    originalRole = it.role,
                    originalPhone = it.phone,
                    profileImageUrl = finalImageUrl,
                    originalProfileImageUrl = finalImageUrl,
                    profileImageUri = null,
                    savedMessage = "Profile updated successfully!",
                    isSaving = false,
                    triggerRestart = roleChanged
                ) }

            } catch (e: Exception) {
                _state.update { it.copy(
                    savedMessage = "Failed to update profile.",
                    isSaving = false
                ) }
            }
        }
    }

    fun saveLanguage(newLang: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true)
            try {
                db.collection("users").document(user.uid).set(mapOf("language" to newLang), SetOptions.merge()).await()
                _state.value = _state.value.copy(savedMessage = "Language saved successfully!")
            } catch (e: Exception) {
                _state.value = _state.value.copy(savedMessage = "Failed to save language.")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun saveWorkType(newWorkType: String) {
        viewModelScope.launch {
            auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true)
            try {
                val data = mapOf("name" to newWorkType)
                firestoreRepository.contractorWorkTypesCollection()?.add(data)?.await()
                _state.value = _state.value.copy(savedMessage = "Work type saved successfully!")
            } catch (e: Exception) {
                _state.value = _state.value.copy(savedMessage = "Failed to save work type.")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(savedMessage = null)
    }

    fun logout() {
        auth.signOut()
        viewModelScope.launch {
            repository.clearSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        configListener?.remove()
    }
}
