package com.synfusion.attendance.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.DateOfBirthUtils
import com.synfusion.attendance.utils.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class OfflineSettingsState(
    val name: String = "",
    val originalName: String = "",
    val username: String = "",
    val originalUsername: String = "",
    val usernameError: String? = null,
    val isCheckingUsername: Boolean = false,
    val usernameAvailability: com.synfusion.attendance.online.UsernameAvailabilityResult? = null,
    val dailyWage: Double = 500.0,
    val originalWage: Double = 500.0,
    val workType: String = "",
    val role: String = "personal",
    val originalRole: String = "personal",
    val phone: String = "",
    val originalPhone: String = "",
    val gender: String = "Male",
    val originalGender: String = "Male",
    val dateOfBirth: String? = null,
    val originalDateOfBirth: String? = null,
    val profession: String = "Site Worker",
    val originalProfession: String = "Site Worker",
    val bio: String = "",
    val originalBio: String = "",
    val countryCode: String = "IN",
    val originalCountryCode: String = "IN",
    val currencyCode: String = "INR",
    val originalCurrencyCode: String = "INR",
    val profileImageUrl: String = "",
    val originalProfileImageUrl: String = "",
    val googlePhotoUrl: String = "",
    val profileImageUri: Uri? = null,
    val language: String = "en",
    val isDarkMode: Boolean? = null,
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
    val lastBackupLoc: String? = null,
    val navLeft1: String = "dashboard",
    val navLeft2: String = "calendar",
    val navRight: String = "settings",
    val startupScreen: String = "dashboard",
    val rememberLastScreen: Boolean = false,
    val lastActiveScreen: String = "dashboard",
    val isLoaded: Boolean = false
)

class OfflineSettingsViewModel(
    private val repository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository,
    private val publicProfileRepository: com.synfusion.attendance.online.PublicProfileRepository = com.synfusion.attendance.online.FirestorePublicProfileRepository()
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _state = MutableStateFlow(OfflineSettingsState())
    val state: StateFlow<OfflineSettingsState> = _state

    init {
        com.synfusion.attendance.backup.DatabaseLifecycleCoordinator.registerViewModel(this)
        loadSettings()
        viewModelScope.launch {
            repository.darkModeFlow.collect { isDark ->
                _state.update { it.copy(isDarkMode = isDark) }
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
            repository.countryFlow.collect { country ->
                _state.update { it.copy(countryCode = country) }
            }
        }
        viewModelScope.launch {
            repository.currencyFlow.collect { currency ->
                _state.update { it.copy(currencyCode = currency) }
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
            repository.googlePhotoUrlFlow.collect { url ->
                _state.update { it.copy(
                    googlePhotoUrl = url ?: ""
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
        viewModelScope.launch {
            repository.navLeft1Flow.collect { item ->
                _state.update { it.copy(navLeft1 = item) }
            }
        }
        viewModelScope.launch {
            repository.navLeft2Flow.collect { item ->
                _state.update { it.copy(navLeft2 = item) }
            }
        }
        viewModelScope.launch {
            repository.navRightFlow.collect { item ->
                _state.update { it.copy(navRight = item) }
            }
        }
        viewModelScope.launch {
            repository.startupScreenFlow.collect { screen ->
                _state.update { it.copy(startupScreen = screen) }
            }
        }
        viewModelScope.launch {
            repository.rememberLastScreenFlow.collect { remember ->
                _state.update { it.copy(rememberLastScreen = remember) }
            }
        }
        viewModelScope.launch {
            repository.lastActiveScreenFlow.collect { screen ->
                _state.update { it.copy(lastActiveScreen = screen, isLoaded = true) }
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

            var fetchedUsername = ""
            var fetchedDateOfBirth: String? = null
            val firebaseUid = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }

            if (firebaseUid != null) {
                val profileRes = publicProfileRepository.getPublicProfile(firebaseUid)
                if (profileRes is com.synfusion.attendance.online.ProfileOperationResult.Success && profileRes.data != null) {
                    fetchedUsername = profileRes.data.username
                    fetchedDateOfBirth = profileRes.data.dateOfBirth
                }
            }

            // The local Room column is authoritative for this offline-first screen; the Firestore
            // public profile is the fallback so a DOB saved through Edit Profile (or on another
            // device) is still shown. Both representations round-trip through DateOfBirthUtils.
            val localDateOfBirth = DateOfBirthUtils.fromEpochMillis(user?.dateOfBirth)
            val resolvedDateOfBirth = localDateOfBirth ?: fetchedDateOfBirth

            if (user != null) {
                _state.update { it.copy(
                    name = user.name,
                    originalName = user.name,
                    username = fetchedUsername.ifEmpty { user.name.lowercase().replace(" ", "") },
                    originalUsername = fetchedUsername.ifEmpty { user.name.lowercase().replace(" ", "") },
                    dailyWage = user.dailyWage,
                    originalWage = user.dailyWage,
                    role = "personal",
                    originalRole = "personal",
                    phone = user.phoneNumber,
                    originalPhone = user.phoneNumber,
                    gender = user.gender,
                    originalGender = user.gender,
                    dateOfBirth = resolvedDateOfBirth,
                    originalDateOfBirth = resolvedDateOfBirth,
                    profession = user.profession,
                    originalProfession = user.profession,
                    bio = user.bio,
                    originalBio = user.bio,
                    countryCode = user.countryCode,
                    originalCountryCode = user.countryCode,
                    currencyCode = user.currencyCode,
                    originalCurrencyCode = user.currencyCode
                ) }
            } else {
                _state.update { it.copy(
                    name = "",
                    originalName = "",
                    username = "",
                    originalUsername = "",
                    usernameError = null,
                    isCheckingUsername = false,
                    usernameAvailability = null,
                    dailyWage = 500.0,
                    originalWage = 500.0,
                    phone = "",
                    originalPhone = "",
                    gender = "Male",
                    originalGender = "Male",
                    dateOfBirth = resolvedDateOfBirth,
                    originalDateOfBirth = resolvedDateOfBirth,
                    profession = "Site Worker",
                    originalProfession = "Site Worker",
                    bio = "",
                    originalBio = ""
                ) }
            }
        }
    }

    fun onUsernameChange(newUsername: String) {
        val cleaned = newUsername.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20)
        _state.update { it.copy(username = cleaned, usernameError = null) }

        if (cleaned.isBlank()) {
            _state.update { it.copy(usernameAvailability = null, isCheckingUsername = false) }
            return
        }

        // An unchanged username (case-insensitive) belongs to the authenticated user and is always valid.
        val original = _state.value.originalUsername
        if (cleaned.equals(original, ignoreCase = true)) {
            _state.update { it.copy(usernameAvailability = com.synfusion.attendance.online.UsernameAvailabilityResult.Available, isCheckingUsername = false) }
            return
        }

        val syntaxCheck = com.synfusion.attendance.online.UsernameValidator.validate(cleaned)
        if (syntaxCheck is com.synfusion.attendance.online.UsernameValidationResult.Invalid) {
            _state.update { it.copy(usernameError = syntaxCheck.reason, usernameAvailability = null, isCheckingUsername = false) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCheckingUsername = true, usernameError = null) }
            val currentUid = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
            val avail = publicProfileRepository.checkUsernameAvailability(cleaned, currentUid)
            _state.update { it.copy(usernameAvailability = avail, isCheckingUsername = false) }
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
            val context = repository.context
            if (enabled) {
                NotificationScheduler.scheduleAllSchedules(context)
            } else {
                NotificationScheduler.cancelAllSchedules(context)
            }
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
        val wageDouble = AmountFormatter.parseToDoubleOrNull(newWage, _state.value.currencyCode) ?: 0.0
        _state.update { it.copy(dailyWage = wageDouble) }
    }

    fun onPhoneChange(newPhone: String) {
        _state.update { it.copy(phone = newPhone) }
    }

    fun onGenderChange(newGender: String) {
        _state.update { it.copy(gender = newGender) }
    }

    fun onDateOfBirthChange(newDob: String?) {
        _state.update { it.copy(dateOfBirth = newDob) }
    }

    fun onProfessionChange(newProfession: String) {
        _state.update { it.copy(profession = newProfession) }
    }

    fun onBioChange(newBio: String) {
        _state.update { it.copy(bio = newBio) }
    }

    fun saveProfileInfo(name: String, username: String, profession: String, bio: String) {
        viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isSaving = true) }

            val cleanUsername = username.trim().lowercase().removePrefix("@")
            val uid = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }

            if (uid != null && cleanUsername.isNotBlank()) {
                // Defense in depth: re-verify username availability at the save boundary,
                // excluding the current authenticated user so an unchanged username is always accepted.
                val availability = publicProfileRepository.checkUsernameAvailability(cleanUsername, uid)
                when (availability) {
                    is com.synfusion.attendance.online.UsernameAvailabilityResult.AlreadyTaken -> {
                        _state.update { it.copy(isSaving = false, usernameError = "Username @$cleanUsername is already taken by another user", savedMessage = "Username @$cleanUsername is already taken by another user") }
                        return@launch
                    }
                    is com.synfusion.attendance.online.UsernameAvailabilityResult.Invalid -> {
                        _state.update { it.copy(isSaving = false, usernameError = availability.reason, savedMessage = availability.reason) }
                        return@launch
                    }
                    is com.synfusion.attendance.online.UsernameAvailabilityResult.Error -> {
                        _state.update { it.copy(isSaving = false, usernameError = availability.message, savedMessage = availability.message) }
                        return@launch
                    }
                    is com.synfusion.attendance.online.UsernameAvailabilityResult.Available -> {
                        // Proceed to save
                    }
                }

                val validation = com.synfusion.attendance.online.UsernameValidator.validate(cleanUsername)
                if (validation is com.synfusion.attendance.online.UsernameValidationResult.Invalid) {
                    _state.update { it.copy(isSaving = false, usernameError = validation.reason, savedMessage = validation.reason) }
                    return@launch
                }

                val existingPhotoUrl = com.synfusion.attendance.online.SocialSessionCache.manager
                    .currentSnapshotFor(uid)
                    ?.profiles
                    ?.get(uid)
                    ?.value
                    ?.photoUrl

                val pubResult = publicProfileRepository.updatePublicProfile(
                    firebaseUid = uid,
                    newUsername = cleanUsername,
                    displayName = name,
                    bio = bio,
                    profession = profession,
                    photoUrl = currentState.googlePhotoUrl.ifBlank { existingPhotoUrl },
                    isPublic = true,
                    gender = currentState.gender,
                    dateOfBirth = currentState.dateOfBirth,
                    countryCode = currentState.countryCode
                )

                when (pubResult) {
                    is com.synfusion.attendance.online.ProfileOperationResult.Error -> {
                        _state.update { it.copy(isSaving = false, usernameError = pubResult.message, savedMessage = pubResult.message) }
                        return@launch
                    }
                    is com.synfusion.attendance.online.ProfileOperationResult.Success -> {
                        com.synfusion.attendance.online.SocialSessionCache.manager.seedProfile(uid, pubResult.data)
                    }
                }
            }

            _state.update { it.copy(name = name, username = cleanUsername, originalUsername = cleanUsername, profession = profession, bio = bio, originalBio = bio) }
            saveChanges()
        }
    }

    fun onCountryChange(newCountryCode: String) {
        val defaultCurrency = CountryRepository.getDefaultCurrencyForCountry(newCountryCode)
        _state.update { it.copy(
            countryCode = newCountryCode,
            currencyCode = defaultCurrency
        ) }
    }

    fun onCurrencyChange(newCurrencyCode: String) {
        _state.update { it.copy(currencyCode = newCurrencyCode) }
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
            countryCode = it.originalCountryCode,
            currencyCode = it.originalCurrencyCode,
            dateOfBirth = it.originalDateOfBirth,
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
                        phoneNumber = currentState.phone,
                        countryCode = currentState.countryCode,
                        currencyCode = currentState.currencyCode,
                        gender = currentState.gender,
                        // Persist the Date of Birth into the existing UserEntity.dateOfBirth column
                        // (epoch millis). An explicit clear stores null; an incomplete or invalid
                        // entry keeps the previously stored value so a typo can never erase data.
                        dateOfBirth = if (currentState.dateOfBirth.isNullOrBlank()) {
                            null
                        } else {
                            DateOfBirthUtils.toEpochMillis(currentState.dateOfBirth) ?: user.dateOfBirth
                        },
                        profession = currentState.profession,
                        bio = currentState.bio
                    )
                    withContext(Dispatchers.IO) {
                        localAttendanceRepository.saveUser(updated)
                    }
                    repository.saveCountryAndCurrency(currentState.countryCode, currentState.currencyCode)

                    _state.update { it.copy(
                        originalName = it.name,
                        originalWage = it.dailyWage,
                        originalRole = "personal",
                        originalPhone = it.phone,
                        originalCountryCode = it.countryCode,
                        originalCurrencyCode = it.currencyCode,
                        originalDateOfBirth = it.dateOfBirth,
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

    fun updateNavigationConfig(left1: String, left2: String, right: String) {
        viewModelScope.launch {
            repository.saveNavigationConfig(left1, left2, right)
        }
    }

    fun updateStartupScreen(screen: String) {
        viewModelScope.launch {
            repository.saveStartupScreen(screen)
        }
    }

    fun updateRememberLastScreen(remember: Boolean) {
        viewModelScope.launch {
            repository.saveRememberLastScreen(remember)
        }
    }

    fun updateLastActiveScreen(screen: String) {
        viewModelScope.launch {
            repository.saveLastActiveScreen(screen)
        }
    }

    fun clearMessage() {
        _state.update { it.copy(savedMessage = null) }
    }

    fun clearSessionState() {
        loadJob?.cancel()
        _state.update {
            it.copy(
                name = "",
                originalName = "",
                username = "",
                originalUsername = "",
                usernameError = null,
                isCheckingUsername = false,
                usernameAvailability = null,
                dailyWage = 500.0,
                originalWage = 500.0,
                phone = "",
                originalPhone = "",
                gender = "Male",
                originalGender = "Male",
                profession = "Site Worker",
                originalProfession = "Site Worker",
                bio = "",
                originalBio = "",
                profileImageUrl = "",
                originalProfileImageUrl = "",
                googlePhotoUrl = "",
                profileImageUri = null,
                isSaving = false,
                savedMessage = null
            )
        }
    }

    fun logout() {
        clearSessionState()
        viewModelScope.launch {
            repository.clearAuthSession()
            NotificationScheduler.cancelAllSchedules(repository.context)
        }
    }
}
