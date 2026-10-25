package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.online.OnlineProfile
import com.synfusion.attendance.online.OnlineProfileRepository
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.ProfileSyncResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.PublicProfileRepository
import com.synfusion.attendance.online.UsernameAvailabilityResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OnlineProfileState {
    object Idle : OnlineProfileState()
    object Syncing : OnlineProfileState()
    data class Synced(val profile: OnlineProfile) : OnlineProfileState()
    data class OfflinePending(val message: String) : OnlineProfileState()
    data class WorkspaceMismatch(
        val existingWorkspaceIdInCloud: String,
        val localWorkspaceId: String
    ) : OnlineProfileState()
    data class Error(val message: String) : OnlineProfileState()
}

sealed class UsernameCheckState {
    object Idle : UsernameCheckState()
    object Checking : UsernameCheckState()
    object Available : UsernameCheckState()
    object AlreadyTaken : UsernameCheckState()
    data class Invalid(val reason: String) : UsernameCheckState()
    data class Error(val message: String) : UsernameCheckState()
}

/**
 * ViewModel responsible for managing online profile state, public profile data, username reservation, and sync.
 *
 * PRIVACY & BOUNDARY DIRECTIVES:
 * Does NOT manage attendance, salary, overtime, advances, passbook, PDF, coins, or referrals.
 */
class OnlineProfileViewModel(
    private val onlineProfileRepository: OnlineProfileRepository,
    private val publicProfileRepository: PublicProfileRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _profileState = MutableStateFlow<OnlineProfileState>(OnlineProfileState.Idle)
    val profileState: StateFlow<OnlineProfileState> = _profileState.asStateFlow()

    private val _publicProfileState = MutableStateFlow<PublicProfile?>(null)
    val publicProfileState: StateFlow<PublicProfile?> = _publicProfileState.asStateFlow()

    private val _usernameCheckState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Idle)
    val usernameCheckState: StateFlow<UsernameCheckState> = _usernameCheckState.asStateFlow()

    private val _publicProfileSaveState = MutableStateFlow<ProfileOperationResult<PublicProfile>?>(null)
    val publicProfileSaveState: StateFlow<ProfileOperationResult<PublicProfile>?> = _publicProfileSaveState.asStateFlow()

    private var currentPendingProfile: OnlineProfile? = null
    private var checkUsernameJob: Job? = null

    fun ensureOnlineProfile(profile: OnlineProfile): Job {
        currentPendingProfile = profile
        _profileState.value = OnlineProfileState.Syncing

        return viewModelScope.launch(ioDispatcher) {
            when (val result = onlineProfileRepository.ensureOnlineProfile(profile)) {
                is ProfileSyncResult.Success -> {
                    _profileState.value = OnlineProfileState.Synced(result.profile)
                    // Also ensure public profile is created/loaded
                    publicProfileRepository?.ensureOrCreatePublicProfile(
                        firebaseUid = result.profile.uid,
                        displayName = result.profile.displayName,
                        photoUrl = result.profile.photoUrl
                    )?.let { pubResult ->
                        if (pubResult is ProfileOperationResult.Success) {
                            _publicProfileState.value = pubResult.data
                        }
                    }
                }
                is ProfileSyncResult.WorkspaceMismatch -> {
                    _profileState.value = OnlineProfileState.WorkspaceMismatch(
                        existingWorkspaceIdInCloud = result.existingLocalWorkspaceIdInFirestore,
                        localWorkspaceId = result.currentLocalWorkspaceId
                    )
                }
                is ProfileSyncResult.OfflineOrError -> {
                    _profileState.value = OnlineProfileState.OfflinePending(result.message)
                }
            }
        }
    }

    fun loadPublicProfile(firebaseUid: String): Job {
        return viewModelScope.launch(ioDispatcher) {
            val repository = publicProfileRepository ?: return@launch
            when (val result = repository.getPublicProfile(firebaseUid)) {
                is ProfileOperationResult.Success -> {
                    _publicProfileState.value = result.data
                }
                is ProfileOperationResult.Error -> {
                    // Retain null or log error
                }
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        checkUsernameJob?.cancel()
        val repository = publicProfileRepository ?: return

        if (username.isBlank()) {
            _usernameCheckState.value = UsernameCheckState.Idle
            return
        }

        _usernameCheckState.value = UsernameCheckState.Checking

        checkUsernameJob = viewModelScope.launch(ioDispatcher) {
            when (val result = repository.checkUsernameAvailability(username)) {
                is UsernameAvailabilityResult.Available -> {
                    val currentUsername = _publicProfileState.value?.username ?: ""
                    if (username.trim().equals(currentUsername, ignoreCase = true)) {
                        _usernameCheckState.value = UsernameCheckState.Available
                    } else {
                        _usernameCheckState.value = UsernameCheckState.Available
                    }
                }
                is UsernameAvailabilityResult.AlreadyTaken -> {
                    val currentUsername = _publicProfileState.value?.username ?: ""
                    if (username.trim().equals(currentUsername, ignoreCase = true)) {
                        _usernameCheckState.value = UsernameCheckState.Available
                    } else {
                        _usernameCheckState.value = UsernameCheckState.AlreadyTaken
                    }
                }
                is UsernameAvailabilityResult.Invalid -> {
                    _usernameCheckState.value = UsernameCheckState.Invalid(result.reason)
                }
                is UsernameAvailabilityResult.Error -> {
                    _usernameCheckState.value = UsernameCheckState.Error(result.message)
                }
            }
        }
    }

    fun updatePublicProfile(
        firebaseUid: String,
        newUsername: String,
        displayName: String,
        bio: String,
        profession: String,
        photoUrl: String?,
        isPublic: Boolean
    ): Job {
        return viewModelScope.launch(ioDispatcher) {
            val repository = publicProfileRepository ?: return@launch
            _publicProfileSaveState.value = null

            val result = repository.updatePublicProfile(
                firebaseUid = firebaseUid,
                newUsername = newUsername,
                displayName = displayName,
                bio = bio,
                profession = profession,
                photoUrl = photoUrl,
                isPublic = isPublic
            )

            if (result is ProfileOperationResult.Success) {
                _publicProfileState.value = result.data
            }
            _publicProfileSaveState.value = result
        }
    }

    fun retrySync(): Job? {
        val pending = currentPendingProfile ?: return null
        return ensureOnlineProfile(pending)
    }

    fun syncProfileFields(
        firebaseUid: String,
        currentLocalWorkspaceId: String,
        updates: Map<String, Any?>
    ): Job {
        _profileState.value = OnlineProfileState.Syncing

        return viewModelScope.launch(ioDispatcher) {
            when (val result = onlineProfileRepository.syncProfileFields(firebaseUid, currentLocalWorkspaceId, updates)) {
                is ProfileSyncResult.Success -> {
                    _profileState.value = OnlineProfileState.Synced(result.profile)
                }
                is ProfileSyncResult.WorkspaceMismatch -> {
                    _profileState.value = OnlineProfileState.WorkspaceMismatch(
                        existingWorkspaceIdInCloud = result.existingLocalWorkspaceIdInFirestore,
                        localWorkspaceId = result.currentLocalWorkspaceId
                    )
                }
                is ProfileSyncResult.OfflineOrError -> {
                    _profileState.value = OnlineProfileState.OfflinePending(result.message)
                }
            }
        }
    }

    fun clearSaveState() {
        _publicProfileSaveState.value = null
    }
}
