package com.synfusion.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.auth.OnlineAccount
import com.synfusion.attendance.backup.DatabaseLifecycleCoordinator
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.WorkspaceStorageManager
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.online.FirestoreOnlineProfileRepository
import com.synfusion.attendance.online.FirestorePublicProfileRepository
import com.synfusion.attendance.online.OnlineProfile
import com.synfusion.attendance.online.OnlineProfileRepository
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.ProfileSyncResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.PublicProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

sealed class AccountResolutionState {
    object Idle : AccountResolutionState()
    object Resolving : AccountResolutionState()
    data class SignedInExisting(val workspaceUserId: String) : AccountResolutionState()
    data class NeedsProfileSetup(
        val account: OnlineAccount,
        val prefillName: String = "",
        val prefillUsername: String = "",
        val prefillPhone: String = "",
        val prefillCountryCode: String = "IN",
        val prefillDailyWage: Double = 0.0,
        val prefillGender: String = "Male",
        val prefillProfession: String = "Site Worker",
        val prefillBio: String = "",
        val isExistingLocalUpgrade: Boolean = false
    ) : AccountResolutionState()
    data class WorkspaceConflict(
        val account: OnlineAccount,
        val message: String,
        val canSwitchToAccountWorkspace: Boolean
    ) : AccountResolutionState()
    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : AccountResolutionState()
}

class OfflineAuthViewModel(
    val repository: OfflinePreferencesRepository,
    private val localAttendanceRepository: LocalAttendanceRepository,
    private val onlineProfileRepository: OnlineProfileRepository = FirestoreOnlineProfileRepository(),
    private val publicProfileRepository: PublicProfileRepository = FirestorePublicProfileRepository(),
    private val workspaceStorageManager: WorkspaceStorageManager? = null
) : ViewModel(), com.synfusion.attendance.backup.RefreshableViewModel {

    private val _loginState = MutableStateFlow<OfflineLoginState>(OfflineLoginState.Idle)
    val loginState: StateFlow<OfflineLoginState> = _loginState

    private val _accountResolutionState = MutableStateFlow<AccountResolutionState>(AccountResolutionState.Idle)
    val accountResolutionState: StateFlow<AccountResolutionState> = _accountResolutionState.asStateFlow()

    val userRoleFlow = repository.userRoleFlow
    val authTokenFlow = repository.authTokenFlow
    val isOnlineAccountSetupCompletedFlow = repository.isOnlineAccountSetupCompletedFlow
    val linkedFirebaseUidFlow = repository.linkedFirebaseUidFlow

    private var checkJob: Job? = null
    private var resolutionJob: Job? = null

    init {
        DatabaseLifecycleCoordinator.registerViewModel(this)
        checkForUserAndLogin()
    }

    override fun onCleared() {
        super.onCleared()
        DatabaseLifecycleCoordinator.unregisterViewModel(this)
    }

    override fun cancelActiveJobs() {
        checkJob?.cancel()
    }

    override fun refreshOnRestore() {
        checkJob = viewModelScope.launch {
            checkForUserAndLogin()
        }
    }

    fun resetAccountResolutionState() {
        resolutionJob?.cancel()
        _accountResolutionState.value = AccountResolutionState.Idle
    }

    fun checkForUserAndLogin(): Job {
        checkJob?.cancel()
        val job = viewModelScope.launch {
            val user = localAttendanceRepository.getUser()
            if (user != null) {
                // Workspace detected: preload workspace config without auto-triggering login success
                repository.saveUserRole(user.role)
                repository.saveCountryAndCurrency(user.countryCode, user.currencyCode)
            }
        }
        checkJob = job
        return job
    }

    fun resumeExistingWorkspace(onComplete: ((Boolean) -> Unit)? = null): Job {
        return viewModelScope.launch {
            val existingUser = localAttendanceRepository.getUser()
            if (existingUser != null) {
                repository.saveOnboardingCompleted(true)
                repository.saveAuthToken(existingUser.id)
                repository.saveUserRole(existingUser.role)
                repository.saveCountryAndCurrency(existingUser.countryCode, existingUser.currencyCode)
                DatabaseLifecycleCoordinator.refreshViewModels()
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

    /**
     * Resolves an authenticated Google/Firebase account against Firestore and local workspace state:
     * - Already-registered users log straight into their own isolated workspace without re-registration.
     * - Switching from User A to User B archives User A's local workspace safely and loads/creates User B's workspace
     *   without leaking User A's data.
     * - New or incomplete users open profile setup prefilled strictly with their own authorized data.
     * - Genuine workspace ownership conflicts surface a clear conflict decision without corrupting data.
     */
    fun resolveAuthenticatedAccount(account: OnlineAccount): Job {
        if (_accountResolutionState.value is AccountResolutionState.Resolving) {
            return resolutionJob ?: Job().apply { complete() }
        }
        resolutionJob?.cancel()
        val job = viewModelScope.launch {
            if (account.uid.isBlank()) {
                _accountResolutionState.value = AccountResolutionState.Error(
                    message = "Authentication failed: missing Google account identifier."
                )
                return@launch
            }

            _accountResolutionState.value = AccountResolutionState.Resolving

            try {
                val linkedUid = repository.linkedFirebaseUidFlow.firstOrNull()
                val setupDone = repository.isOnlineAccountSetupCompletedFlow.firstOrNull() ?: false
                val currentLocalUser = localAttendanceRepository.getUser()
                val hasLocalAttendance = try {
                    localAttendanceRepository.getPersonalAttendance().isNotEmpty()
                } catch (_: Exception) {
                    false
                }

                val publicRes = publicProfileRepository.getPublicProfile(account.uid)
                val onlineRes = onlineProfileRepository.getOnlineProfile(account.uid)

                // 1. Handle network / Firestore read failure without corrupting local state
                if (publicRes is ProfileOperationResult.Error) {
                    if (linkedUid == account.uid && currentLocalUser != null && setupDone) {
                        activateWorkspaceSession(
                            user = currentLocalUser,
                            firebaseUid = account.uid,
                            photoUrl = account.photoUrl
                        )
                        return@launch
                    }

                    if (!linkedUid.isNullOrBlank() &&
                        linkedUid != account.uid &&
                        workspaceStorageManager != null &&
                        workspaceStorageManager.hasArchivedWorkspace(account.uid)
                    ) {
                        val archivedMeta = workspaceStorageManager.getArchivedMetadata(account.uid)
                        if (archivedMeta?.onlineAccountSetupCompleted == true) {
                            workspaceStorageManager.switchWorkspace(linkedUid, account.uid)
                            val archivedUser = localAttendanceRepository.getUser()
                            if (archivedUser != null) {
                                activateWorkspaceSession(
                                    user = archivedUser,
                                    firebaseUid = account.uid,
                                    photoUrl = archivedMeta.googlePhotoUrl ?: account.photoUrl
                                )
                                return@launch
                            }
                        }
                    }

                    _accountResolutionState.value = AccountResolutionState.Error(
                        message = "Unable to verify your WorkDaily account right now. Please check your connection or continue offline.",
                        isNetworkError = true
                    )
                    return@launch
                }

                val publicProfile = (publicRes as? ProfileOperationResult.Success)?.data
                val onlineProfile = (onlineRes as? ProfileSyncResult.Success)?.profile
                val isRegisteredInCloud = publicProfile != null && publicProfile.username.isNotBlank()

                // 2. Same account returning to its already-linked local workspace
                if (linkedUid == account.uid) {
                    if (onlineProfile != null &&
                        onlineProfile.localWorkspaceId.isNotBlank() &&
                        currentLocalUser != null &&
                        onlineProfile.localWorkspaceId != currentLocalUser.id
                    ) {
                        _accountResolutionState.value = AccountResolutionState.WorkspaceConflict(
                            account = account,
                            message = "This Google account is bound to a different WorkDaily workspace than the active local workspace.",
                            canSwitchToAccountWorkspace = false
                        )
                        return@launch
                    }

                    if (isRegisteredInCloud && publicProfile != null) {
                        val resolvedUser = mergeOrCreateUserForAccount(
                            existingUser = currentLocalUser,
                            account = account,
                            onlineProfile = onlineProfile,
                            publicProfile = publicProfile
                        )
                        localAttendanceRepository.saveUser(resolvedUser)
                        ensureOnlineBindingBestEffort(account, resolvedUser, onlineProfile)
                        activateWorkspaceSession(
                            user = resolvedUser,
                            firebaseUid = account.uid,
                            photoUrl = publicProfile.photoUrl ?: account.photoUrl
                        )
                    } else {
                        _accountResolutionState.value = buildSetupState(
                            account = account,
                            localUser = currentLocalUser,
                            onlineProfile = onlineProfile,
                            publicProfile = publicProfile,
                            isExistingLocalUpgrade = currentLocalUser != null
                        )
                    }
                    return@launch
                }

                // 3. Different Google account signing in after another account was linked
                if (!linkedUid.isNullOrBlank() && linkedUid != account.uid) {
                    if (workspaceStorageManager == null) {
                        _accountResolutionState.value = AccountResolutionState.WorkspaceConflict(
                            account = account,
                            message = "The signed-in Google account does not match the account linked to this workspace.",
                            canSwitchToAccountWorkspace = false
                        )
                        return@launch
                    }

                    val hasParkedWorkspace = workspaceStorageManager.hasArchivedWorkspace(account.uid)
                    if (hasParkedWorkspace || isRegisteredInCloud) {
                        workspaceStorageManager.switchWorkspace(
                            fromWorkspaceKey = linkedUid,
                            toWorkspaceKey = account.uid
                        )
                        val userB = localAttendanceRepository.getUser()

                        if (onlineProfile != null &&
                            onlineProfile.localWorkspaceId.isNotBlank() &&
                            userB != null &&
                            onlineProfile.localWorkspaceId != userB.id
                        ) {
                            _accountResolutionState.value = AccountResolutionState.WorkspaceConflict(
                                account = account,
                                message = "Workspace ownership conflict detected for ${account.email ?: "this account"}.",
                                canSwitchToAccountWorkspace = false
                            )
                            return@launch
                        }

                        val parkedSetupCompleted = workspaceStorageManager
                            .getArchivedMetadata(account.uid)
                            ?.onlineAccountSetupCompleted == true

                        if ((isRegisteredInCloud && publicProfile != null) || (userB != null && parkedSetupCompleted)) {
                            val resolvedUser = mergeOrCreateUserForAccount(
                                existingUser = userB,
                                account = account,
                                onlineProfile = onlineProfile,
                                publicProfile = publicProfile
                            )
                            localAttendanceRepository.saveUser(resolvedUser)
                            ensureOnlineBindingBestEffort(account, resolvedUser, onlineProfile)
                            activateWorkspaceSession(
                                user = resolvedUser,
                                firebaseUid = account.uid,
                                photoUrl = publicProfile?.photoUrl ?: account.photoUrl
                            )
                        } else {
                            repository.saveLinkedFirebaseUid(account.uid)
                            repository.saveOnlineAccountSetupCompleted(false)
                            _accountResolutionState.value = buildSetupState(
                                account = account,
                                localUser = userB,
                                onlineProfile = onlineProfile,
                                publicProfile = publicProfile,
                                isExistingLocalUpgrade = false
                            )
                        }
                    } else {
                        // Brand-new Account B: keep Account A untouched on disk until Account B submits registration,
                        // and NEVER prefill Account A's local data into Account B's setup form.
                        _accountResolutionState.value = buildSetupState(
                            account = account,
                            localUser = null,
                            onlineProfile = onlineProfile,
                            publicProfile = publicProfile,
                            isExistingLocalUpgrade = false
                        )
                    }
                    return@launch
                }

                // 4. No online account linked yet (fresh install or offline-only workspace)
                if (currentLocalUser == null) {
                    if (isRegisteredInCloud && publicProfile != null) {
                        val resolvedUser = mergeOrCreateUserForAccount(
                            existingUser = null,
                            account = account,
                            onlineProfile = onlineProfile,
                            publicProfile = publicProfile
                        )
                        localAttendanceRepository.saveUser(resolvedUser)
                        ensureOnlineBindingBestEffort(account, resolvedUser, onlineProfile)
                        activateWorkspaceSession(
                            user = resolvedUser,
                            firebaseUid = account.uid,
                            photoUrl = publicProfile.photoUrl ?: account.photoUrl
                        )
                    } else {
                        _accountResolutionState.value = buildSetupState(
                            account = account,
                            localUser = null,
                            onlineProfile = onlineProfile,
                            publicProfile = publicProfile,
                            isExistingLocalUpgrade = false
                        )
                    }
                    return@launch
                }

                // Unlinked local workspace exists
                if (!isRegisteredInCloud && (onlineProfile == null || onlineProfile.localWorkspaceId == currentLocalUser.id)) {
                    // Standard offline-to-online upgrade: link existing local workspace
                    _accountResolutionState.value = buildSetupState(
                        account = account,
                        localUser = currentLocalUser,
                        onlineProfile = onlineProfile,
                        publicProfile = publicProfile,
                        isExistingLocalUpgrade = true
                    )
                    return@launch
                }

                if (onlineProfile?.localWorkspaceId == currentLocalUser.id && publicProfile != null) {
                    val resolvedUser = mergeOrCreateUserForAccount(
                        existingUser = currentLocalUser,
                        account = account,
                        onlineProfile = onlineProfile,
                        publicProfile = publicProfile
                    )
                    localAttendanceRepository.saveUser(resolvedUser)
                    activateWorkspaceSession(
                        user = resolvedUser,
                        firebaseUid = account.uid,
                        photoUrl = publicProfile.photoUrl ?: account.photoUrl
                    )
                    return@launch
                }

                if (!hasLocalAttendance && workspaceStorageManager != null && publicProfile != null) {
                    workspaceStorageManager.switchWorkspace(
                        fromWorkspaceKey = "offline_${currentLocalUser.id}",
                        toWorkspaceKey = account.uid
                    )
                    val existingForB = localAttendanceRepository.getUser()
                    val resolvedUser = mergeOrCreateUserForAccount(
                        existingUser = existingForB,
                        account = account,
                        onlineProfile = onlineProfile,
                        publicProfile = publicProfile
                    )
                    localAttendanceRepository.saveUser(resolvedUser)
                    ensureOnlineBindingBestEffort(account, resolvedUser, onlineProfile)
                    activateWorkspaceSession(
                        user = resolvedUser,
                        firebaseUid = account.uid,
                        photoUrl = publicProfile.photoUrl ?: account.photoUrl
                    )
                    return@launch
                }

                _accountResolutionState.value = AccountResolutionState.WorkspaceConflict(
                    account = account,
                    message = "This device has unlinked offline attendance records, while ${account.email ?: "this Google account"} already has an existing WorkDaily account. You can safely archive this offline workspace and open your Google account, try a different Google account, or continue offline.",
                    canSwitchToAccountWorkspace = workspaceStorageManager != null && isRegisteredInCloud
                )
            } catch (e: Exception) {
                _accountResolutionState.value = AccountResolutionState.Error(
                    message = e.message ?: "Failed to resolve Google account."
                )
            }
        }
        resolutionJob = job
        return job
    }

    /**
     * Explicitly archives an unlinked local offline workspace (without deleting any data)
     * and signs into the already-registered Google account's isolated workspace.
     */
    fun switchFromUnlinkedToRegisteredAccount(account: OnlineAccount): Job {
        val manager = workspaceStorageManager ?: return Job().apply { complete() }
        resolutionJob?.cancel()
        val job = viewModelScope.launch {
            _accountResolutionState.value = AccountResolutionState.Resolving
            try {
                val currentLocalUser = localAttendanceRepository.getUser()
                val archiveKey = currentLocalUser?.let { "offline_${it.id}" }
                manager.switchWorkspace(
                    fromWorkspaceKey = archiveKey,
                    toWorkspaceKey = account.uid
                )

                val publicRes = publicProfileRepository.getPublicProfile(account.uid)
                val onlineRes = onlineProfileRepository.getOnlineProfile(account.uid)
                val publicProfile = (publicRes as? ProfileOperationResult.Success)?.data
                val onlineProfile = (onlineRes as? ProfileSyncResult.Success)?.profile
                val userB = localAttendanceRepository.getUser()

                val resolvedUser = mergeOrCreateUserForAccount(
                    existingUser = userB,
                    account = account,
                    onlineProfile = onlineProfile,
                    publicProfile = publicProfile
                )
                localAttendanceRepository.saveUser(resolvedUser)
                ensureOnlineBindingBestEffort(account, resolvedUser, onlineProfile)
                activateWorkspaceSession(
                    user = resolvedUser,
                    firebaseUid = account.uid,
                    photoUrl = publicProfile?.photoUrl ?: account.photoUrl
                )
            } catch (e: Exception) {
                _accountResolutionState.value = AccountResolutionState.Error(
                    message = e.message ?: "Failed to switch workspace."
                )
            }
        }
        resolutionJob = job
        return job
    }

    private fun buildSetupState(
        account: OnlineAccount,
        localUser: UserEntity?,
        onlineProfile: OnlineProfile?,
        publicProfile: PublicProfile?,
        isExistingLocalUpgrade: Boolean
    ): AccountResolutionState.NeedsProfileSetup {
        val prefillName = when {
            !localUser?.name.isNullOrBlank() && localUser?.name != "User" -> localUser!!.name
            !publicProfile?.displayName.isNullOrBlank() && publicProfile?.displayName != "WorkDaily Member" -> publicProfile!!.displayName
            !onlineProfile?.displayName.isNullOrBlank() -> onlineProfile!!.displayName!!
            !account.displayName.isNullOrBlank() -> account.displayName
            else -> ""
        }
        return AccountResolutionState.NeedsProfileSetup(
            account = account,
            prefillName = prefillName,
            prefillUsername = publicProfile?.username ?: "",
            prefillPhone = localUser?.phoneNumber?.ifBlank { null } ?: onlineProfile?.phoneNumber ?: "",
            prefillCountryCode = localUser?.countryCode?.ifBlank { null } ?: onlineProfile?.countryCode ?: "IN",
            prefillDailyWage = localUser?.dailyWage ?: 0.0,
            prefillGender = localUser?.gender?.ifBlank { null } ?: onlineProfile?.gender ?: "Male",
            prefillProfession = publicProfile?.profession?.ifBlank { null } ?: localUser?.profession ?: "Site Worker",
            prefillBio = publicProfile?.bio?.ifBlank { null } ?: localUser?.bio ?: "",
            isExistingLocalUpgrade = isExistingLocalUpgrade
        )
    }

    private fun mergeOrCreateUserForAccount(
        existingUser: UserEntity?,
        account: OnlineAccount,
        onlineProfile: OnlineProfile?,
        publicProfile: PublicProfile?
    ): UserEntity {
        val workspaceId = existingUser?.id
            ?: onlineProfile?.localWorkspaceId?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val createdAt = existingUser?.createdAt
            ?: onlineProfile?.createdAt?.takeIf { it > 0L }
            ?: publicProfile?.createdAt?.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val resolvedName = when {
            !publicProfile?.displayName.isNullOrBlank() -> publicProfile!!.displayName
            !existingUser?.name.isNullOrBlank() && existingUser?.name != "User" -> existingUser!!.name
            !onlineProfile?.displayName.isNullOrBlank() -> onlineProfile!!.displayName!!
            !account.displayName.isNullOrBlank() -> account.displayName
            else -> "User"
        }
        val resolvedEmail = account.email?.takeIf { it.isNotBlank() }
            ?: onlineProfile?.email?.takeIf { it.isNotBlank() }
            ?: existingUser?.email
            ?: ""
        val resolvedPhone = existingUser?.phoneNumber?.takeIf { it.isNotBlank() }
            ?: onlineProfile?.phoneNumber
            ?: ""
        val resolvedCountry = existingUser?.countryCode?.takeIf { it.isNotBlank() }
            ?: onlineProfile?.countryCode?.takeIf { it.isNotBlank() }
            ?: "IN"
        val resolvedCurrency = existingUser?.currencyCode?.takeIf { it.isNotBlank() }
            ?: onlineProfile?.currencyCode?.takeIf { it.isNotBlank() }
            ?: "INR"
        val resolvedGender = existingUser?.gender?.takeIf { it.isNotBlank() }
            ?: onlineProfile?.gender?.takeIf { it.isNotBlank() }
            ?: "Male"
        val resolvedProfession = publicProfile?.profession?.takeIf { it.isNotBlank() }
            ?: existingUser?.profession?.takeIf { it.isNotBlank() }
            ?: "Site Worker"
        val resolvedBio = publicProfile?.bio
            ?: existingUser?.bio
            ?: ""
        val resolvedWage = existingUser?.dailyWage?.takeIf { it > 0.0 } ?: 500.0

        return UserEntity(
            id = workspaceId,
            name = resolvedName,
            email = resolvedEmail,
            phoneNumber = resolvedPhone,
            role = existingUser?.role ?: "personal",
            dailyWage = resolvedWage,
            countryCode = resolvedCountry,
            currencyCode = resolvedCurrency,
            gender = resolvedGender,
            profession = resolvedProfession,
            bio = resolvedBio,
            createdAt = createdAt
        )
    }

    private suspend fun ensureOnlineBindingBestEffort(
        account: OnlineAccount,
        user: UserEntity,
        existingOnlineProfile: OnlineProfile?
    ) {
        if (existingOnlineProfile != null) return
        try {
            onlineProfileRepository.ensureOnlineProfile(
                OnlineProfile(
                    uid = account.uid,
                    localWorkspaceId = user.id,
                    displayName = user.name,
                    email = user.email.ifBlank { account.email },
                    phoneNumber = user.phoneNumber.ifBlank { null },
                    photoUrl = account.photoUrl,
                    countryCode = user.countryCode,
                    currencyCode = user.currencyCode,
                    gender = user.gender,
                    emailVerified = account.isEmailVerified
                )
            )
        } catch (_: Exception) {
            // Non-blocking best-effort sync for already-registered public accounts
        }
    }

    private suspend fun activateWorkspaceSession(
        user: UserEntity,
        firebaseUid: String,
        photoUrl: String?
    ) {
        repository.saveCountryAndCurrency(user.countryCode, user.currencyCode)
        repository.saveOnlineAccountSetupCompleted(true)
        repository.saveLinkedFirebaseUid(firebaseUid)
        repository.saveGooglePhotoUrl(photoUrl?.takeIf { it.isNotBlank() })
        repository.saveOnboardingCompleted(true)
        repository.saveAuthToken(user.id)
        repository.saveUserRole(user.role)
        DatabaseLifecycleCoordinator.refreshViewModels()
        _loginState.value = OfflineLoginState.Success(user.id)
        _accountResolutionState.value = AccountResolutionState.SignedInExisting(user.id)
    }

    fun completeProfile(
        googleAccount: OnlineAccount,
        enteredName: String,
        enteredUsername: String = "",
        enteredPhone: String,
        countryCode: String,
        currencyCode: String,
        dailyWage: Double,
        gender: String = "Male",
        profession: String = "Site Worker",
        bio: String = "",
        onComplete: (Boolean, String?) -> Unit
    ): Job {
        return viewModelScope.launch {
            _loginState.value = OfflineLoginState.Loading
            try {
                val linkedUid = repository.linkedFirebaseUidFlow.firstOrNull()
                if (!linkedUid.isNullOrBlank() && linkedUid != googleAccount.uid) {
                    if (workspaceStorageManager == null) {
                        val mismatchMsg = "Account mismatch: active workspace belongs to another account."
                        _loginState.value = OfflineLoginState.Error(mismatchMsg)
                        onComplete(false, mismatchMsg)
                        return@launch
                    }
                    // Archive previous user's workspace safely and open a clean/isolated workspace for the new account
                    workspaceStorageManager.switchWorkspace(
                        fromWorkspaceKey = linkedUid,
                        toWorkspaceKey = googleAccount.uid
                    )
                }

                val existingOnlineProfile = (onlineProfileRepository.getOnlineProfile(googleAccount.uid) as? ProfileSyncResult.Success)?.profile
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
                    // Brand-new user (or resumed setup on empty device): preserve online localWorkspaceId if already bound, else generate new local UUID
                    userId = existingOnlineProfile?.localWorkspaceId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                    createdAt = existingOnlineProfile?.createdAt?.takeIf { it > 0L } ?: System.currentTimeMillis()

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
                    gender = gender,
                    profession = profession,
                    bio = bio,
                    createdAt = createdAt
                )

                // Save local workspace first to protect local attendance data
                localAttendanceRepository.saveUser(user)
                repository.saveCountryAndCurrency(finalCountry, finalCurrency)
                repository.saveAuthToken(userId)
                repository.saveUserRole("personal")
                repository.saveOnboardingCompleted(true)

                // Try saving online profile & username reservation
                var publicProfileError: String? = null
                try {
                    val onlineProfile = OnlineProfile(
                        uid = googleAccount.uid,
                        localWorkspaceId = userId,
                        displayName = finalName,
                        email = finalEmail,
                        phoneNumber = finalPhone,
                        photoUrl = googleAccount.photoUrl,
                        countryCode = finalCountry,
                        currencyCode = finalCurrency,
                        gender = gender,
                        emailVerified = googleAccount.isEmailVerified
                    )
                    val onlineSyncResult = onlineProfileRepository.ensureOnlineProfile(onlineProfile)
                    if (onlineSyncResult is ProfileSyncResult.WorkspaceMismatch) {
                        publicProfileError =
                            "Workspace mismatch: this Google account is already linked to workspace ${onlineSyncResult.existingLocalWorkspaceIdInFirestore} (current workspace: ${onlineSyncResult.currentLocalWorkspaceId})."
                    } else {
                        val cleanUsername = enteredUsername.trim().removePrefix("@")
                        val pubResult = if (cleanUsername.isNotBlank()) {
                            publicProfileRepository.updatePublicProfile(
                                firebaseUid = googleAccount.uid,
                                newUsername = cleanUsername,
                                displayName = finalName,
                                bio = bio,
                                profession = profession,
                                photoUrl = googleAccount.photoUrl,
                                isPublic = true
                            )
                        } else {
                            publicProfileRepository.ensureOrCreatePublicProfile(
                                firebaseUid = googleAccount.uid,
                                displayName = finalName,
                                photoUrl = googleAccount.photoUrl
                            )
                        }

                        if (pubResult is ProfileOperationResult.Error) {
                            publicProfileError = pubResult.message
                        }
                    }
                } catch (e: Exception) {
                    publicProfileError = e.message ?: "Failed to save public profile"
                }

                if (publicProfileError != null) {
                    // Online setup failed (e.g. username taken or network issue)
                    // Do NOT mark setup completed! Propagate error so user can correct input or retry.
                    _loginState.value = OfflineLoginState.Error(publicProfileError)
                    onComplete(false, publicProfileError)
                    return@launch
                }

                // Online profile and username reservation saved successfully
                repository.saveOnlineAccountSetupCompleted(true)
                repository.saveLinkedFirebaseUid(googleAccount.uid)
                repository.saveGooglePhotoUrl(googleAccount.photoUrl?.takeIf { it.isNotBlank() })

                DatabaseLifecycleCoordinator.refreshViewModels()
                _loginState.value = OfflineLoginState.Success(userId)
                _accountResolutionState.value = AccountResolutionState.SignedInExisting(userId)
                onComplete(true, null)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to complete profile"
                _loginState.value = OfflineLoginState.Error(errorMsg)
                onComplete(false, errorMsg)
            }
        }
    }

    /**
     * Performs standard user sign-out by clearing active session tokens (auth_token and user_role)
     * and transient in-memory auth resolution state.
     *
     * IMPORTANT WORKDAILY OFFLINE CONTRACT:
     * - Standard sign-out NEVER deletes UserEntity, attendance records, summaries, passbook history, or local backup files.
     * - Standard sign-out preserves linked_firebase_uid and online_account_setup_completed so that
     *   returning or switching accounts can safely archive/restore isolated workspaces.
     * - Unlink Online Account, Delete Online Account, and Delete Local Data are separate domain operations.
     */
    fun logout(): Job {
        resolutionJob?.cancel()
        _loginState.value = OfflineLoginState.Idle
        _accountResolutionState.value = AccountResolutionState.Idle
        return viewModelScope.launch {
            repository.clearAuthSession()
        }
    }
}
