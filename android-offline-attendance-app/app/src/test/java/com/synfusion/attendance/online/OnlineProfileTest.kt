package com.synfusion.attendance.online

import com.synfusion.attendance.auth.OnlineAccount
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineLoginState
import com.synfusion.attendance.viewmodel.OnlineProfileState
import com.synfusion.attendance.viewmodel.OnlineProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

private class DummyContext : android.content.ContextWrapper(null)

private class FakeTestPublicProfileRepository : PublicProfileRepository {
    val publicProfiles = mutableMapOf<String, PublicProfile>()
    val usernames = mutableSetOf<String>()

    override suspend fun getPublicProfile(firebaseUid: String): ProfileOperationResult<PublicProfile?> {
        return ProfileOperationResult.Success(publicProfiles[firebaseUid])
    }

    override suspend fun checkUsernameAvailability(username: String): UsernameAvailabilityResult {
        val validation = UsernameValidator.validate(username)
        if (validation is UsernameValidationResult.Invalid) {
            return UsernameAvailabilityResult.Invalid(validation.reason)
        }
        val normalized = UsernameValidator.normalize(username)
        return if (usernames.contains(normalized)) {
            UsernameAvailabilityResult.AlreadyTaken
        } else {
            UsernameAvailabilityResult.Available
        }
    }

    override suspend fun ensureOrCreatePublicProfile(
        firebaseUid: String,
        displayName: String?,
        photoUrl: String?
    ): ProfileOperationResult<PublicProfile> {
        val existing = publicProfiles[firebaseUid]
        if (existing != null) return ProfileOperationResult.Success(existing)
        val candidate = UsernameValidator.generateCandidate(displayName, firebaseUid)
        val profile = PublicProfile(
            uid = firebaseUid,
            displayName = displayName ?: "WorkDaily Member",
            username = candidate,
            photoUrl = photoUrl
        )
        publicProfiles[firebaseUid] = profile
        usernames.add(candidate)
        return ProfileOperationResult.Success(profile)
    }

    override suspend fun updatePublicProfile(
        firebaseUid: String,
        newUsername: String,
        displayName: String,
        bio: String,
        profession: String,
        photoUrl: String?,
        isPublic: Boolean
    ): ProfileOperationResult<PublicProfile> {
        val norm = UsernameValidator.normalize(newUsername)
        val existing = publicProfiles[firebaseUid]
        if (existing?.username != norm && usernames.contains(norm)) {
            return ProfileOperationResult.Error("Username @$norm is already taken by another user.")
        }
        if (existing != null) {
            usernames.remove(existing.username)
        }
        usernames.add(norm)
        val updated = PublicProfile(
            uid = firebaseUid,
            displayName = displayName,
            username = norm,
            bio = bio,
            profession = profession,
            photoUrl = photoUrl,
            isPublic = isPublic
        )
        publicProfiles[firebaseUid] = updated
        return ProfileOperationResult.Success(updated)
    }

    override suspend fun searchPublicProfiles(
        query: String,
        currentUserId: String?,
        limit: Int
    ): ProfileOperationResult<List<PublicProfile>> {
        val norm = UsernameValidator.normalize(query)
        val matches = publicProfiles.values.filter {
            (it.usernameLower.contains(norm) || it.displayNameLower.contains(norm)) && it.uid != currentUserId
        }.take(limit)
        return ProfileOperationResult.Success(matches)
    }
}

private class FakeTestOnlineProfileRepository : OnlineProfileRepository {
    val profilesInMemory = mutableMapOf<String, OnlineProfile>()
    var shouldFailWithNetworkError = false
    var ensureProfileCallCount = 0

    override suspend fun ensureOnlineProfile(profile: OnlineProfile): ProfileSyncResult {
        ensureProfileCallCount++
        if (shouldFailWithNetworkError) {
            return ProfileSyncResult.OfflineOrError("Network unavailable. Pending sync.")
        }

        val existing = profilesInMemory[profile.uid]
        if (existing != null) {
            if (existing.localWorkspaceId != profile.localWorkspaceId) {
                return ProfileSyncResult.WorkspaceMismatch(
                    existingLocalWorkspaceIdInFirestore = existing.localWorkspaceId,
                    currentLocalWorkspaceId = profile.localWorkspaceId
                )
            }
            // Merge permitted fields
            val merged = existing.copy(
                displayName = profile.displayName ?: existing.displayName,
                email = profile.email ?: existing.email,
                photoUrl = profile.photoUrl ?: existing.photoUrl,
                phoneNumber = profile.phoneNumber ?: existing.phoneNumber,
                countryCode = profile.countryCode ?: existing.countryCode,
                currencyCode = profile.currencyCode ?: existing.currencyCode,
                emailVerified = profile.emailVerified,
                updatedAt = System.currentTimeMillis()
            )
            profilesInMemory[profile.uid] = merged
            return ProfileSyncResult.Success(merged)
        } else {
            profilesInMemory[profile.uid] = profile
            return ProfileSyncResult.Success(profile)
        }
    }

    override suspend fun getOnlineProfile(firebaseUid: String): ProfileSyncResult {
        if (shouldFailWithNetworkError) {
            return ProfileSyncResult.OfflineOrError("Network error")
        }
        val profile = profilesInMemory[firebaseUid]
        return if (profile != null) {
            ProfileSyncResult.Success(profile)
        } else {
            ProfileSyncResult.OfflineOrError("Document not found")
        }
    }

    override suspend fun syncProfileFields(
        firebaseUid: String,
        currentLocalWorkspaceId: String,
        updates: Map<String, Any?>
    ): ProfileSyncResult {
        if (shouldFailWithNetworkError) {
            return ProfileSyncResult.OfflineOrError("Network error")
        }
        val existing = profilesInMemory[firebaseUid]
            ?: return ProfileSyncResult.OfflineOrError("Document not found")

        if (existing.localWorkspaceId != currentLocalWorkspaceId) {
            return ProfileSyncResult.WorkspaceMismatch(
                existingLocalWorkspaceIdInFirestore = existing.localWorkspaceId,
                currentLocalWorkspaceId = currentLocalWorkspaceId
            )
        }

        val safeUpdates = updates.filterKeys {
            it != "uid" && it != "localWorkspaceId" && it != "createdAt" &&
            it != "dailyWage" && it != "attendance" && it != "salary" && it != "passbook"
        }

        val updated = existing.copy(
            displayName = safeUpdates["displayName"] as? String ?: existing.displayName,
            email = safeUpdates["email"] as? String ?: existing.email,
            phoneNumber = safeUpdates["phoneNumber"] as? String ?: existing.phoneNumber,
            photoUrl = safeUpdates["photoUrl"] as? String ?: existing.photoUrl,
            countryCode = safeUpdates["countryCode"] as? String ?: existing.countryCode,
            currencyCode = safeUpdates["currencyCode"] as? String ?: existing.currencyCode,
            updatedAt = System.currentTimeMillis()
        )
        profilesInMemory[firebaseUid] = updated
        return ProfileSyncResult.Success(updated)
    }
}

private class FakeTestPreferencesRepository : OfflinePreferencesRepository(DummyContext()) {
    var savedRole: String? = null
    var savedAuthToken: String? = null
    var savedCountryCode: String? = null
    var savedCurrencyCode: String? = null
    var savedOnboardingCompleted: Boolean? = null
    var savedOnlineAccountSetupCompleted: Boolean? = null
    var savedLinkedFirebaseUid: String? = null
    var savedGooglePhotoUrl: String? = null

    private val _roleFlow = MutableStateFlow<String?>(null)
    override val userRoleFlow: StateFlow<String?> = _roleFlow.asStateFlow()

    private val _authTokenFlow = MutableStateFlow<String?>(null)
    override val authTokenFlow: StateFlow<String?> = _authTokenFlow.asStateFlow()

    private val _onlineAccountSetupCompletedFlow = MutableStateFlow(false)
    override val isOnlineAccountSetupCompletedFlow: StateFlow<Boolean> = _onlineAccountSetupCompletedFlow.asStateFlow()

    private val _linkedFirebaseUidFlow = MutableStateFlow<String?>(null)
    override val linkedFirebaseUidFlow: StateFlow<String?> = _linkedFirebaseUidFlow.asStateFlow()

    override suspend fun saveCountryAndCurrency(countryCode: String, currencyCode: String) {
        savedCountryCode = countryCode
        savedCurrencyCode = currencyCode
    }

    override suspend fun saveUserRole(role: String) {
        savedRole = role
        _roleFlow.value = role
    }

    override suspend fun saveAuthToken(token: String) {
        savedAuthToken = token
        _authTokenFlow.value = token
    }

    override suspend fun saveOnboardingCompleted(completed: Boolean) {
        savedOnboardingCompleted = completed
    }

    override suspend fun saveOnlineAccountSetupCompleted(completed: Boolean) {
        savedOnlineAccountSetupCompleted = completed
        _onlineAccountSetupCompletedFlow.value = completed
    }

    override suspend fun saveLinkedFirebaseUid(uid: String?) {
        savedLinkedFirebaseUid = uid
        _linkedFirebaseUidFlow.value = uid
    }

    override suspend fun saveGooglePhotoUrl(url: String?) {
        savedGooglePhotoUrl = url
    }

    override suspend fun clearAuthSession() {
        savedAuthToken = null
        savedRole = null
        _authTokenFlow.value = null
        _roleFlow.value = null
    }
}

private class FakeTestAttendanceRepository : LocalAttendanceRepository(DummyContext()) {
    var storedUser: UserEntity? = null
    val storedAttendance = mutableListOf<AttendanceEntity>()

    override suspend fun getUser(): UserEntity? = storedUser

    override suspend fun saveUser(user: UserEntity) {
        storedUser = user
    }

    override suspend fun getPersonalAttendance(): List<AttendanceEntity> = storedAttendance
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineProfileTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeProfileRepo: FakeTestOnlineProfileRepository
    private lateinit var fakePublicProfileRepo: FakeTestPublicProfileRepository
    private lateinit var fakePrefsRepo: FakeTestPreferencesRepository
    private lateinit var fakeAttendanceRepo: FakeTestAttendanceRepository
    private lateinit var profileViewModel: OnlineProfileViewModel
    private lateinit var authViewModel: OfflineAuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepo = FakeTestOnlineProfileRepository()
        fakePublicProfileRepo = FakeTestPublicProfileRepository()
        fakePrefsRepo = FakeTestPreferencesRepository()
        fakeAttendanceRepo = FakeTestAttendanceRepository()

        profileViewModel = OnlineProfileViewModel(
            onlineProfileRepository = fakeProfileRepo,
            publicProfileRepository = fakePublicProfileRepo,
            ioDispatcher = testDispatcher
        )
        authViewModel = OfflineAuthViewModel(
            repository = fakePrefsRepo,
            localAttendanceRepository = fakeAttendanceRepo,
            onlineProfileRepository = fakeProfileRepo,
            publicProfileRepository = fakePublicProfileRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNewUserMapping_FirebaseUidNotEqualsLocalWorkspaceId() = runBlocking {
        fakeAttendanceRepo.storedUser = null

        val googleAccount = OnlineAccount(
            uid = "firebase_user_abc123",
            displayName = "Brand New Worker",
            email = "newworker@example.com",
            photoUrl = "https://example.com/avatar.jpg",
            isEmailVerified = true
        )

        authViewModel.completeProfile(
            googleAccount = googleAccount,
            enteredName = "Brand New Worker",
            enteredPhone = "9876543210",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 600.0,
            onComplete = { _, _ -> }
        ).join()

        // Local workspace user entity saved
        val savedUser = fakeAttendanceRepo.getUser()
        assertNotNull(savedUser)

        // Verifications
        assertNotEquals("firebase_user_abc123", savedUser!!.id) // Firebase UID != local workspace UUID!
        assertTrue(savedUser.id.length >= 32)

        // Firestore online profile document path = users/firebase_user_abc123
        val createdOnlineProfile = fakeProfileRepo.profilesInMemory["firebase_user_abc123"]
        assertNotNull(createdOnlineProfile)
        assertEquals("firebase_user_abc123", createdOnlineProfile!!.uid)
        assertEquals(savedUser.id, createdOnlineProfile.localWorkspaceId) // localWorkspaceId = local UUID
        assertEquals("Brand New Worker", createdOnlineProfile.displayName)
        assertEquals("newworker@example.com", createdOnlineProfile.email)
        assertEquals("9876543210", createdOnlineProfile.phoneNumber)
        assertEquals("IN", createdOnlineProfile.countryCode)
        assertEquals("INR", createdOnlineProfile.currencyCode)
    }

    @Test
    fun testExistingUserMapping_PreservesLocalUUIDAndCreatedAt() = runBlocking {
        val existingLocalUuid = "local_existing_uuid_777"
        val existingCreatedAt = 1600000000000L
        val existingUser = UserEntity(
            id = existingLocalUuid,
            name = "Existing Master Craftsman",
            phoneNumber = "9988776655",
            email = "craftsman@example.com",
            role = "personal",
            dailyWage = 1200.0,
            countryCode = "IN",
            currencyCode = "INR",
            createdAt = existingCreatedAt
        )
        fakeAttendanceRepo.storedUser = existingUser

        val googleAccount = OnlineAccount(
            uid = "firebase_existing_google_uid",
            displayName = "Google Craftsman Name",
            email = "craftsman@gmail.com",
            photoUrl = "https://lh3.googleusercontent.com/photo.png"
        )

        authViewModel.completeProfile(
            googleAccount = googleAccount,
            enteredName = "Existing Master Craftsman",
            enteredPhone = "9988776655",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 1200.0,
            onComplete = { _, _ -> }
        ).join()

        // Verifications
        val updatedLocalUser = fakeAttendanceRepo.getUser()
        assertNotNull(updatedLocalUser)
        assertEquals(existingLocalUuid, updatedLocalUser!!.id) // Existing local UUID preserved!
        assertEquals(existingCreatedAt, updatedLocalUser.createdAt) // Existing createdAt preserved!

        val onlineProfile = fakeProfileRepo.profilesInMemory["firebase_existing_google_uid"]
        assertNotNull(onlineProfile)
        assertEquals("firebase_existing_google_uid", onlineProfile!!.uid)
        assertEquals(existingLocalUuid, onlineProfile.localWorkspaceId) // Maps online account to existing local UUID
    }

    @Test
    fun testIdempotentProfileCreation_RunningTwiceDoesNotCreateDuplicateOrChangeIdentity() = runBlocking {
        val firebaseUid = "firebase_idempotent_999"
        val localWorkspaceId = UUID.randomUUID().toString()

        val initialProfile = OnlineProfile(
            uid = firebaseUid,
            localWorkspaceId = localWorkspaceId,
            displayName = "Worker One",
            email = "worker@example.com"
        )

        // First execution
        profileViewModel.ensureOnlineProfile(initialProfile).join()
        assertTrue(profileViewModel.profileState.value is OnlineProfileState.Synced)
        assertEquals(1, fakeProfileRepo.ensureProfileCallCount)

        val storedFirst = fakeProfileRepo.profilesInMemory[firebaseUid]
        assertNotNull(storedFirst)
        assertEquals(localWorkspaceId, storedFirst!!.localWorkspaceId)

        // Second execution with updated metadata
        val updatedProfile = initialProfile.copy(displayName = "Worker One Updated")
        profileViewModel.ensureOnlineProfile(updatedProfile).join()
        assertTrue(profileViewModel.profileState.value is OnlineProfileState.Synced)
        assertEquals(2, fakeProfileRepo.ensureProfileCallCount)

        // Verify still only 1 document exists, identity unchanged, name updated
        assertEquals(1, fakeProfileRepo.profilesInMemory.size)
        val storedSecond = fakeProfileRepo.profilesInMemory[firebaseUid]
        assertEquals(localWorkspaceId, storedSecond!!.localWorkspaceId)
        assertEquals("Worker One Updated", storedSecond.displayName)
    }

    @Test
    fun testWorkspaceMismatchDetection_PreventsOverwritingDifferentLocalWorkspaceId() = runBlocking {
        val firebaseUid = "firebase_shared_uid"
        val workspaceA = "workspace_A_111"
        val workspaceB = "workspace_B_222"

        // Online document already linked to workspace A
        fakeProfileRepo.profilesInMemory[firebaseUid] = OnlineProfile(
            uid = firebaseUid,
            localWorkspaceId = workspaceA,
            displayName = "Workspace A User"
        )

        // Device with local workspace B attempts to link to same Firebase UID
        val profileAttemptB = OnlineProfile(
            uid = firebaseUid,
            localWorkspaceId = workspaceB,
            displayName = "Workspace B User"
        )

        profileViewModel.ensureOnlineProfile(profileAttemptB).join()

        // State reflects WorkspaceMismatch
        val state = profileViewModel.profileState.value
        assertTrue(state is OnlineProfileState.WorkspaceMismatch)
        val mismatchState = state as OnlineProfileState.WorkspaceMismatch
        assertEquals(workspaceA, mismatchState.existingWorkspaceIdInCloud)
        assertEquals(workspaceB, mismatchState.localWorkspaceId)

        // Online document in Firestore was NOT overwritten!
        assertEquals(workspaceA, fakeProfileRepo.profilesInMemory[firebaseUid]?.localWorkspaceId)
        assertEquals("Workspace A User", fakeProfileRepo.profilesInMemory[firebaseUid]?.displayName)
    }

    @Test
    fun testPrivateDataBoundary_OnlineProfileModelHasZeroFinancialOrAttendanceFields() {
        val fields = OnlineProfile::class.java.declaredFields.map { it.name }

        // Must NOT contain any financial or attendance fields
        assertFalse(fields.contains("dailyWage"))
        assertFalse(fields.contains("attendance"))
        assertFalse(fields.contains("overtime"))
        assertFalse(fields.contains("advance"))
        assertFalse(fields.contains("earnings"))
        assertFalse(fields.contains("salary"))
        assertFalse(fields.contains("passbook"))
        assertFalse(fields.contains("settlement"))
        assertFalse(fields.contains("notes"))

        // Must contain explicitly allowlisted identity fields only
        assertTrue(fields.contains("uid"))
        assertTrue(fields.contains("localWorkspaceId"))
        assertTrue(fields.contains("displayName"))
        assertTrue(fields.contains("email"))
        assertTrue(fields.contains("phoneNumber"))
        assertTrue(fields.contains("countryCode"))
        assertTrue(fields.contains("currencyCode"))
    }

    @Test
    fun testOfflineFailureResilience_NetworkFailureDoesNotBlockLocalWorkspaceOrDeleteData() = runBlocking {
        fakeAttendanceRepo.storedUser = null
        fakeProfileRepo.shouldFailWithNetworkError = true

        val googleAccount = OnlineAccount(
            uid = "firebase_offline_user",
            displayName = "Offline Worker"
        )

        authViewModel.completeProfile(
            googleAccount = googleAccount,
            enteredName = "Offline Worker",
            enteredPhone = "9876543210",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 500.0,
            onComplete = { success, _ ->
                assertTrue(success)
            }
        ).join()

        // Local workspace creation succeeded despite Firestore network error!
        assertTrue(authViewModel.loginState.value is OfflineLoginState.Success)
        val localUser = fakeAttendanceRepo.getUser()
        assertNotNull(localUser)
        assertEquals("Offline Worker", localUser!!.name)
        assertEquals(500.0, localUser.dailyWage, 0.001)

        // Retry via OnlineProfileViewModel succeeds when network recovers
        fakeProfileRepo.shouldFailWithNetworkError = false
        val pendingProfile = OnlineProfile(
            uid = googleAccount.uid,
            localWorkspaceId = localUser.id,
            displayName = "Offline Worker"
        )
        profileViewModel.ensureOnlineProfile(pendingProfile).join()

        assertTrue(profileViewModel.profileState.value is OnlineProfileState.Synced)
        assertNotNull(fakeProfileRepo.profilesInMemory["firebase_offline_user"])
    }

    @Test
    fun testSignOutSemantics_OrdinarySignOutDoesNotDeleteOnlineProfileOrLocalRecords() = runBlocking {
        val firebaseUid = "firebase_signout_test"
        val localUuid = "local_signout_uuid"

        val user = UserEntity(
            id = localUuid,
            name = "Active Worker",
            phoneNumber = "9876543210",
            email = "active@example.com",
            role = "personal",
            dailyWage = 900.0
        )
        fakeAttendanceRepo.storedUser = user
        fakeAttendanceRepo.storedAttendance.add(
            AttendanceEntity(id = "att_1", workerId = null, date = "2026-03-01", status = "present", type = "full", wage = 900.0)
        )

        fakeProfileRepo.profilesInMemory[firebaseUid] = OnlineProfile(
            uid = firebaseUid,
            localWorkspaceId = localUuid,
            displayName = "Active Worker"
        )

        fakePrefsRepo.saveAuthToken(localUuid)
        fakePrefsRepo.saveUserRole("personal")
        fakePrefsRepo.saveLinkedFirebaseUid(firebaseUid)
        fakePrefsRepo.saveOnlineAccountSetupCompleted(true)

        // Perform Logout
        authViewModel.logout().join()

        // Active tokens cleared
        assertNull(fakePrefsRepo.savedAuthToken)
        assertNull(fakePrefsRepo.savedRole)

        // Local workspace database records preserved
        assertNotNull(fakeAttendanceRepo.getUser())
        assertEquals(1, fakeAttendanceRepo.getPersonalAttendance().size)

        // Online Firestore profile preserved
        assertNotNull(fakeProfileRepo.profilesInMemory[firebaseUid])
        assertEquals(localUuid, fakeProfileRepo.profilesInMemory[firebaseUid]?.localWorkspaceId)
    }
}
