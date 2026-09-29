package com.synfusion.attendance.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.data.model.CountryConfig
import com.synfusion.attendance.data.model.CountryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

data class OfflineOnboardingState(
    val currentStep: Int = 0, // 0: Welcome, 1: Profile, 2: Country, 3: Wage, 4: Mobile, 5: Reveal
    val name: String = "",
    val profileImageUri: Uri? = null,
    val selectedCountry: CountryConfig = CountryRepository.DEFAULT_COUNTRY,
    val dailyWage: String = "700",
    val phoneNumber: String = "",
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class OfflineOnboardingViewModel(
    private val repository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OfflineOnboardingState())
    val state: StateFlow<OfflineOnboardingState> = _state

    init {
        autoDetectCountry()
    }

    private fun autoDetectCountry() {
        try {
            val systemCountryCode = Locale.getDefault().country
            if (!systemCountryCode.isNullOrBlank()) {
                val matchedCountry = CountryRepository.getCountry(systemCountryCode)
                _state.update { it.copy(selectedCountry = matchedCountry) }
            }
        } catch (e: Exception) {
            // Fallback to DEFAULT_COUNTRY
        }
    }

    fun onNextStep() {
        val next = (_state.value.currentStep + 1).coerceAtMost(5)
        _state.update { it.copy(currentStep = next) }
    }

    fun onPreviousStep() {
        val prev = (_state.value.currentStep - 1).coerceAtLeast(0)
        _state.update { it.copy(currentStep = prev) }
    }

    fun goToStep(step: Int) {
        _state.update { it.copy(currentStep = step.coerceIn(0, 5)) }
    }

    fun updateName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun updateProfilePhoto(uri: Uri?) {
        _state.update { it.copy(profileImageUri = uri) }
    }

    fun updateCountry(country: CountryConfig) {
        _state.update { it.copy(selectedCountry = country) }
    }

    fun updateWage(wage: String) {
        _state.update { it.copy(dailyWage = wage) }
    }

    fun updatePhone(phone: String) {
        _state.update { it.copy(phoneNumber = phone) }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val currentState = _state.value
                val userId = UUID.randomUUID().toString()

                // Save profile photo if URI is provided
                val photoUri = currentState.profileImageUri
                if (photoUri != null) {
                    val context = repository.context
                    val photoFileName = "${userId}_profile_photo.jpg"
                    val photosDir = java.io.File(context.filesDir, "profile_photos")
                    if (!photosDir.exists()) {
                        photosDir.mkdirs()
                    }
                    val destFile = java.io.File(photosDir, photoFileName)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                            java.io.FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    repository.saveProfilePhotoPath(destFile.absolutePath)
                }

                val wageDouble = currentState.dailyWage.toDoubleOrNull() ?: 500.0
                val user = UserEntity(
                    id = userId,
                    name = currentState.name.ifBlank { "User" },
                    phoneNumber = currentState.phoneNumber,
                    email = "",
                    role = "personal",
                    dailyWage = wageDouble,
                    countryCode = currentState.selectedCountry.countryCode,
                    currencyCode = currentState.selectedCountry.currencyCode
                )

                withContext(Dispatchers.IO) {
                    localAttendanceRepository.saveUser(user)
                }

                repository.saveCountryAndCurrency(user.countryCode, user.currencyCode)
                repository.saveAuthToken(userId)
                repository.saveUserRole("personal")
                repository.saveOnboardingCompleted(true)

                _state.update { it.copy(isSaving = false, isCompleted = true) }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save profile") }
            }
        }
    }
}
