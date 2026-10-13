package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.online.OnlineProfile
import com.synfusion.attendance.online.OnlineProfileRepository
import com.synfusion.attendance.online.ProfileSyncResult
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

/**
 * ViewModel responsible strictly for managing online profile state and safe sync/retry operations.
 *
 * PRIVACY & BOUNDARY DIRECTIVES:
 * Does NOT manage attendance, salary, overtime, advances, passbook, PDF, coins, or referrals.
 */
class OnlineProfileViewModel(
    private val onlineProfileRepository: OnlineProfileRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _profileState = MutableStateFlow<OnlineProfileState>(OnlineProfileState.Idle)
    val profileState: StateFlow<OnlineProfileState> = _profileState.asStateFlow()

    private var currentPendingProfile: OnlineProfile? = null

    fun ensureOnlineProfile(profile: OnlineProfile): Job {
        currentPendingProfile = profile
        _profileState.value = OnlineProfileState.Syncing

        return viewModelScope.launch(ioDispatcher) {
            when (val result = onlineProfileRepository.ensureOnlineProfile(profile)) {
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
                    // Safe offline failure handling: local operations continue, sync marked pending
                    _profileState.value = OnlineProfileState.OfflinePending(result.message)
                }
            }
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
}
