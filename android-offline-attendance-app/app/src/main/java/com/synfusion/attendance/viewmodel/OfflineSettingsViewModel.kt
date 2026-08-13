package com.synfusion.attendance.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class OfflineSettingsState(
    val name: String = "",
    val originalName: String = "",
    val dailyWage: Double = 500.0,
    val originalWage: Double = 500.0,
    val workType: String = "",
    val role: String = "personal",
    val originalRole: String = "personal",
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
    val isPremium: Boolean = true, // Permanently unlocked for offline version
    val isSplashEnabled: Boolean = true,
    val triggerRestart: Boolean = false,
    val isLanguageEnabled: Boolean = true,
    val isLanguageActivityEnabled: Boolean = true,
    val isRoleUiEnabled: Boolean = false,
    val lastBackupTime: Long? = null,
    val lastBackupSize: Long? = null,
    val lastBackupLoc: String? = null
)

class OfflineSettingsViewModel(
    private val repository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _state = MutableStateFlow(OfflineSettingsState())
    val state: StateFlow<OfflineSettingsState> = _state

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        loadSettings()
        viewModelScope.launch {
            repository.darkModeFlow.collect { isDark ->
                _state.update { it.copy(isDarkMode = isDark ?: false) }
            }
        }
        viewModelScope.launch {
            repository.remindersFlow.collect { enabled ->
                _state.update { it.copy(isRemindersEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            repository.splashEnabledFlow.collect { enabled ->
                _state.update { it.copy(isSplashEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            repository.languageFlow.collect { lang ->
                _state.update { it.copy(language = lang ?: "en") }
            }
        }
        viewModelScope.launch {
            repository.profilePhotoPathFlow.collect { path ->
                _state.update { it.copy(
                    profileImageUrl = path ?: "",
                    originalProfileImageUrl = path ?: ""
                ) }
            }
        }
        viewModelScope.launch {
            repository.lastBackupTimeFlow.collect { time ->
                _state.update { it.copy(lastBackupTime = time) }
            }
        }
        viewModelScope.launch {
            repository.lastBackupSizeFlow.collect { size ->
                _state.update { it.copy(lastBackupSize = size) }
            }
        }
        viewModelScope.launch {
            repository.lastBackupLocFlow.collect { loc ->
                _state.update { it.copy(lastBackupLoc = loc) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.unregisterViewModel(this)
    }

    override fun cancelActiveJobs() {
        loadJob?.cancel()
    }

    override fun refreshOnRestore() {
        loadSettings()
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadSettings() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                localAttendanceRepository.getUser()
            }
            if (user != null) {
                _state.update { it.copy(
                    name = user.name,
                    originalName = user.name,
                    dailyWage = user.dailyWage,
                    originalWage = user.dailyWage,
                    role = "personal",
                    originalRole = "personal",
                    phone = user.phoneNumber,
                    originalPhone = user.phoneNumber
                ) }
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

    fun onNameChange(newName: String) {
        _state.update { it.copy(name = newName) }
    }

    fun onWageChange(newWage: String) {
        val wageDouble = newWage.toDoubleOrNull() ?: 0.0
        _state.update { it.copy(dailyWage = wageDouble) }
    }

    fun onPhoneChange(newPhone: String) {
        _state.update { it.copy(phone = newPhone) }
    }

    fun onProfileImageSelected(uri: Uri?) {
        _state.update { it.copy(profileImageUri = uri) }
    }

    fun onRemoveProfilePhoto() {
        _state.update { it.copy(profileImageUrl = "") }
    }

    fun discardChanges() {
        _state.update { it.copy(
            name = it.originalName,
            dailyWage = it.originalWage,
            phone = it.originalPhone,
            profileImageUrl = it.originalProfileImageUrl,
            profileImageUri = null
        ) }
    }

    fun saveChanges() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isSaving = true) }

            try {
                val user = withContext(Dispatchers.IO) {
                    localAttendanceRepository.getUser()
                }
                val userId = user?.id ?: "default_user"
                val photoFileName = "${userId}_profile_photo.jpg"

                // Photo selection/removal handling
                val selectedUri = currentState.profileImageUri
                if (selectedUri != null) {
                    val context = repository.context
                    val photosDir = java.io.File(context.filesDir, "profile_photos")
                    if (!photosDir.exists()) {
                        photosDir.mkdirs()
                    }
                    val destFile = java.io.File(photosDir, photoFileName)
                    if (destFile.exists()) {
                        destFile.delete()
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                            java.io.FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    repository.saveProfilePhotoPath(destFile.absolutePath)
                } else if (currentState.profileImageUrl.isEmpty() && currentState.originalProfileImageUrl.isNotEmpty()) {
                    // Image was removed
                    val context = repository.context
                    val photosDir = java.io.File(context.filesDir, "profile_photos")
                    val destFile = java.io.File(photosDir, photoFileName)
                    if (destFile.exists()) {
                        destFile.delete()
                    }
                    repository.saveProfilePhotoPath(null)
                }
                if (user != null) {
                    val updated = user.copy(
                        name = currentState.name,
                        dailyWage = currentState.dailyWage,
                        role = "personal",
                        phoneNumber = currentState.phone
                    )
                    withContext(Dispatchers.IO) {
                        localAttendanceRepository.saveUser(updated)
                    }

                    _state.update { it.copy(
                        originalName = it.name,
                        originalWage = it.dailyWage,
                        originalRole = "personal",
                        originalPhone = it.phone,
                        profileImageUri = null,
                        savedMessage = "Profile updated successfully!",
                        isSaving = false
                    ) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    savedMessage = "Failed to update profile: ${e.localizedMessage}",
                    isSaving = false
                ) }
            }
        }
    }

    fun saveLanguage(newLang: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                repository.saveLanguage(newLang)
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(newLang)
                )
                _state.update { it.copy(
                    language = newLang,
                    savedMessage = "Language saved successfully!",
                    isSaving = false
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    savedMessage = "Failed to save language.",
                    isSaving = false
                ) }
            }
        }
    }

    fun saveWorkType(newWorkType: String) {
        viewModelScope.launch {
            _state.update { it.copy(savedMessage = "Work type saved successfully!") }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(savedMessage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                localAttendanceRepository.clearSession()
            }
            repository.clearSession()
        }
    }
}
