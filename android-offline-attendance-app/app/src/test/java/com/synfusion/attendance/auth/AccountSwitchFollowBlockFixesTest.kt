package com.synfusion.attendance.auth

import com.synfusion.attendance.data.ArchivedWorkspaceMetadata
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.WorkspaceStorageManager
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.online.BlockRepository
import com.synfusion.attendance.online.FollowCounts
import com.synfusion.attendance.online.FollowRepository
import com.synfusion.attendance.online.OnlineProfile
import com.synfusion.attendance.online.OnlineProfileRepository
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.ProfileSyncResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.PublicProfileRepository
import com.synfusion.attendance.online.UsernameAvailabilityResult
import com.synfusion.attendance.online.UsernameValidationResult
import com.synfusion.attendance.online.UsernameValidator
import com.synfusion.attendance.viewmodel.AccountResolutionState
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineLoginState
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

private class MultiAccountDummyContext : android.content.ContextWrapper(null)

private class MultiAccountFakePrefsRepository : OfflinePreferencesRepository(MultiAccountDummyContext()) {
    var savedRole: String? = null
    var savedAuthToken: String? = null
    var savedCountryCode: String = "IN"
    var savedCurrencyCode: String = "INR"
    var savedOnboardingCompleted: Boolean = false
    var savedOnlineAccountSetupCompleted: Boolean = false
    var savedLinkedFirebaseUid: String? = null
    var savedGooglePhotoUrl: String? = null
    var savedProfilePhotoPath: String? = null

    private val _roleFlow = MutableStateFlow<String?>(null)
    override val userRoleFlow: StateFlow<String?> = _roleFlow.asStateFlow()

    private val _authTokenFlow = MutableStateFlow<String?>(null)
    override val authTokenFlow: StateFlow<String?> = _authTokenFlow.asStateFlow()

    private val _onlineSetupFlow = MutableStateFlow(false)
    override val isOnlineAccountSetupCompletedFlow: StateFlow<Boolean> = _onlineSetupFlow.asStateFlow()

    private val _linkedUidFlow = MutableStateFlow<String?>(null)
    override val linkedFirebaseUidFlow: StateFlow<String?> = _linkedUidFlow.asStateFlow()

    private val _photoPathFlow = MutableStateFlow<String?>(null)
    override val profilePhotoPathFlow: StateFlow<String?> = _photoPathFlow.asStateFlow()

    private val _googlePhotoFlow = MutableStateFlow<String?>(null)
    override val googlePhotoUrlFlow: StateFlow<String?> = _googlePhotoFlow.asStateFlow()

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
        _onlineSetupFlow.value = completed
    }

    override suspend fun saveLinkedFirebaseUid(uid: String?) {
        savedLinkedFirebaseUid = uid
        _linkedUidFlow.value = uid
    }

    override suspend fun saveGooglePhotoUrl(url: String?) {
        savedGooglePhotoUrl = url
        _googlePhotoFlow.value = url
    }

    override suspend fun saveProfilePhotoPath(path: String?) {
        savedProfilePhotoPath = path
        _photoPathFlow.value = path
    }

    override suspend fun clearAuthSession() {
        savedAuthToken = null
        savedRole = null
        _authTokenFlow.value = null
        _roleFlow.value = null
    }
}

private class MultiAccountFakeAttendanceRepository : LocalAttendanceRepository(MultiAccountDummyContext()) {
    var storedUser: UserEntity? = null
    val storedAttendance = mutableListOf<AttendanceEntity>()

    override suspend fun getUser(): UserEntity? = storedUser

    override suspend fun saveUser(user: UserEntity) {
        storedUser = user
    }

    override suspend fun getPersonalAttendance(): List<AttendanceEntity> = storedAttendance.toList()
}

private class InMemoryTestWorkspaceStorageManager(
    private val attendanceRepo: MultiAccountFakeAttendanceRepository,
    private val prefsRepo: MultiAccountFakePrefsRepository
) : WorkspaceStorageManager {

    private data class Snapshot(
        val user: UserEntity?,
        val attendance: List<AttendanceEntity>,
        val metadata: ArchivedWorkspaceMetadata
    )

    private val archives = mutableMapOf<String, Snapshot>()

    override suspend fun hasArchivedWorkspace(workspaceKey: String): Boolean {
        return archives.containsKey(workspaceKey) && archives[workspaceKey]?.user != null
    }

    override suspend fun getArchivedMetadata(workspaceKey: String): ArchivedWorkspaceMetadata? {
        return archives[workspaceKey]?.metadata
    }

    override suspend fun switchWorkspace(fromWorkspaceKey: String?, toWorkspaceKey: String) {
        if (!fromWorkspaceKey.isNullOrBlank() && fromWorkspaceKey == toWorkspaceKey) return

        if (!fromWorkspaceKey.isNullOrBlank()) {
            archives[fromWorkspaceKey] = Snapshot(
                user = attendanceRepo.storedUser,
                attendance = attendanceRepo.storedAttendance.toList(),
                metadata = ArchivedWorkspaceMetadata(
                    workspaceKey = fromWorkspaceKey,
                    localUserId = attendanceRepo.storedUser?.id,
                    profilePhotoPath = prefsRepo.savedProfilePhotoPath,
                    googlePhotoUrl = prefsRepo.savedGooglePhotoUrl,
                    countryCode = prefsRepo.savedCountryCode,
                    currencyCode = prefsRepo.savedCurrencyCode,
                    onlineAccountSetupCompleted = prefsRepo.savedOnlineAccountSetupCompleted
                )
            )
        }

        val target = archives[toWorkspaceKey]
        attendanceRepo.storedAttendance.clear()
        if (target != null) {
            attendanceRepo.storedUser = target.user
            attendanceRepo.storedAttendance.addAll(target.attendance)
            prefsRepo.saveProfilePhotoPath(target.metadata.profilePhotoPath)
            prefsRepo.saveGooglePhotoUrl(target.metadata.googlePhotoUrl)
            prefsRepo.saveCountryAndCurrency(target.metadata.countryCode, target.metadata.currencyCode)
            prefsRepo.saveOnlineAccountSetupCompleted(target.metadata.onlineAccountSetupCompleted)
        } else {
            attendanceRepo.storedUser = null
            prefsRepo.saveProfilePhotoPath(null)
            prefsRepo.saveGooglePhotoUrl(null)
            prefsRepo.saveOnlineAccountSetupCompleted(false)
        }
    }

    fun getArchivedAttendanceCount(workspaceKey: String): Int {
        return archives[workspaceKey]?.attendance?.size ?: 0
    }
}

private class MultiAccountFakeOnlineProfileRepository : OnlineProfileRepository {
    val profiles = mutableMapOf<String, OnlineProfile>()
    var networkError: String? = null

    override suspend fun ensureOnlineProfile(profile: OnlineProfile): ProfileSyncResult {
        networkError?.let { return ProfileSyncResult.OfflineOrError(it) }
        val existing = profiles[profile.uid]
        if (existing != null && existing.localWorkspaceId != profile.localWorkspaceId) {
            return ProfileSyncResult.WorkspaceMismatch(existing.localWorkspaceId, profile.localWorkspaceId)
        }
        val saved = if (existing == null) profile else existing.copy(
            displayName = profile.displayName ?: existing.displayName,
            email = profile.email ?: existing.email,
            phoneNumber = profile.phoneNumber ?: existing.phoneNumber,
            photoUrl = profile.photoUrl ?: existing.photoUrl,
            updatedAt = System.currentTimeMillis()
        )
        profiles[profile.uid] = saved
        return ProfileSyncResult.Success(saved)
    }

    override suspend fun getOnlineProfile(firebaseUid: String): ProfileSyncResult {
        networkError?.let { return ProfileSyncResult.OfflineOrError(it) }
        val found = profiles[firebaseUid]
        return if (found != null) {
            ProfileSyncResult.Success(found)
        } else {
            ProfileSyncResult.OfflineOrError("Not found")
        }
    }

    override suspend fun syncProfileFields(
        firebaseUid: String,
        currentLocalWorkspaceId: String,
        updates: Map<String, Any?>
    ): ProfileSyncResult {
        val found = profiles[firebaseUid] ?: return ProfileSyncResult.OfflineOrError("Not found")
        if (found.localWorkspaceId != currentLocalWorkspaceId) {
            return ProfileSyncResult.WorkspaceMismatch(found.localWorkspaceId, currentLocalWorkspaceId)
        }
        return ProfileSyncResult.Success(found)
    }
}

private class MultiAccountFakePublicProfileRepository : PublicProfileRepository {
    val publicProfiles = mutableMapOf<String, PublicProfile>()
    val usernameOwners = mutableMapOf<String, String>() // normalizedUsername -> ownerUid
    var networkError: String? = null

    override suspend fun getPublicProfile(firebaseUid: String): ProfileOperationResult<PublicProfile?> {
        networkError?.let { return ProfileOperationResult.Error(it) }
        return ProfileOperationResult.Success(publicProfiles[firebaseUid])
    }

    override suspend fun checkUsernameAvailability(username: String): UsernameAvailabilityResult {
        return checkUsernameAvailability(username, currentUserId = null)
    }

    override suspend fun checkUsernameAvailability(
        username: String,
        currentUserId: String?
    ): UsernameAvailabilityResult {
        networkError?.let { return UsernameAvailabilityResult.Error(it) }
        val validation = UsernameValidator.validate(username)
        if (validation is UsernameValidationResult.Invalid) {
            return UsernameAvailabilityResult.Invalid(validation.reason)
        }
        val normalized = UsernameValidator.normalize(username)
        val owner = usernameOwners[normalized]
        return when {
            owner == null -> UsernameAvailabilityResult.Available
            !currentUserId.isNullOrBlank() && owner == currentUserId -> UsernameAvailabilityResult.Available
            else -> UsernameAvailabilityResult.AlreadyTaken
        }
    }

    override suspend fun ensureOrCreatePublicProfile(
        firebaseUid: String,
        displayName: String?,
        photoUrl: String?
    ): ProfileOperationResult<PublicProfile> {
        networkError?.let { return ProfileOperationResult.Error(it) }
        val existing = publicProfiles[firebaseUid]
        if (existing != null) return ProfileOperationResult.Success(existing)
        val candidate = UsernameValidator.generateCandidate(displayName, firebaseUid)
        val created = PublicProfile(
            uid = firebaseUid,
            displayName = displayName ?: "WorkDaily Member",
            username = candidate,
            usernameLower = candidate,
            photoUrl = photoUrl
        )
        publicProfiles[firebaseUid] = created
        usernameOwners[candidate] = firebaseUid
        return ProfileOperationResult.Success(created)
    }

    override suspend fun updatePublicProfile(
        firebaseUid: String,
        newUsername: String,
        displayName: String,
        bio: String,
        profession: String,
        photoUrl: String?,
        isPublic: Boolean,
        gender: String,
        dateOfBirth: String?,
        countryCode: String
    ): ProfileOperationResult<PublicProfile> {
        networkError?.let { return ProfileOperationResult.Error(it) }
        val validation = UsernameValidator.validate(newUsername)
        if (validation is UsernameValidationResult.Invalid) {
            return ProfileOperationResult.Error(validation.reason)
        }
        val normalized = UsernameValidator.normalize(newUsername)
        val owner = usernameOwners[normalized]
        if (owner != null && owner != firebaseUid) {
            return ProfileOperationResult.Error("Username @$normalized is already taken by another user.")
        }

        val current = publicProfiles[firebaseUid]
        if (current != null && current.usernameLower.isNotBlank() && current.usernameLower != normalized) {
            usernameOwners.remove(current.usernameLower)
        }
        usernameOwners[normalized] = firebaseUid

        // Preserves existing followerCount, followingCount, isVerified, createdAt
        val updated = PublicProfile(
            uid = firebaseUid,
            displayName = displayName.ifBlank { "WorkDaily Member" },
            username = normalized,
            usernameLower = normalized,
            displayNameLower = displayName.lowercase(),
            bio = bio,
            profession = profession.ifBlank { "Site Worker" },
            photoUrl = photoUrl,
            gender = gender.ifBlank { current?.gender ?: "" },
            dateOfBirth = dateOfBirth?.takeIf { it.isNotBlank() } ?: current?.dateOfBirth,
            countryCode = countryCode.ifBlank { current?.countryCode ?: "" },
            isPublic = isPublic,
            isVerified = current?.isVerified ?: false,
            followerCount = current?.followerCount ?: 0,
            followingCount = current?.followingCount ?: 0,
            createdAt = current?.createdAt ?: 1000L,
            updatedAt = System.currentTimeMillis()
        )
        publicProfiles[firebaseUid] = updated
        return ProfileOperationResult.Success(updated)
    }

    override suspend fun searchPublicProfiles(
        query: String,
        currentUserId: String?,
        limit: Int
    ): ProfileOperationResult<List<PublicProfile>> {
        val q = query.trim().lowercase()
        val list = publicProfiles.values.filter {
            it.uid != currentUserId &&
                (it.usernameLower.startsWith(q) || it.displayNameLower.startsWith(q))
        }.take(limit)
        return ProfileOperationResult.Success(list)
    }
}

private class SocialBlockAndFollowHarness(
    private val profileRepo: MultiAccountFakePublicProfileRepository
) : FollowRepository, BlockRepository {

    private val follows = mutableSetOf<Pair<String, String>>() // followerUid to targetUid
    private val blocks = mutableSetOf<Pair<String, String>>() // blockerUid to blockedUid

    override suspend fun followUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean> {
        if (followerUid.isBlank() || targetUid.isBlank()) return ProfileOperationResult.Error("Invalid UID")
        if (followerUid == targetUid) return ProfileOperationResult.Error("Self-follow prohibited")
        if (isEitherBlocked(followerUid, targetUid)) {
            return ProfileOperationResult.Error("Cannot follow this user due to block settings.")
        }
        if (follows.add(followerUid to targetUid)) {
            profileRepo.publicProfiles[targetUid]?.let {
                profileRepo.publicProfiles[targetUid] = it.copy(followerCount = it.followerCount + 1)
            }
            profileRepo.publicProfiles[followerUid]?.let {
                profileRepo.publicProfiles[followerUid] = it.copy(followingCount = it.followingCount + 1)
            }
        }
        return ProfileOperationResult.Success(true)
    }

    override suspend fun unfollowUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean> {
        if (followerUid.isBlank() || targetUid.isBlank()) return ProfileOperationResult.Error("Invalid UID")
        if (follows.remove(followerUid to targetUid)) {
            profileRepo.publicProfiles[targetUid]?.let {
                profileRepo.publicProfiles[targetUid] = it.copy(followerCount = (it.followerCount - 1).coerceAtLeast(0))
            }
            profileRepo.publicProfiles[followerUid]?.let {
                profileRepo.publicProfiles[followerUid] = it.copy(followingCount = (it.followingCount - 1).coerceAtLeast(0))
            }
        }
        return ProfileOperationResult.Success(true)
    }

    override suspend fun isFollowing(followerUid: String, targetUid: String): Boolean {
        return follows.contains(followerUid to targetUid)
    }

    override suspend fun isMutual(uidA: String, uidB: String): Boolean {
        return isFollowing(uidA, uidB) && isFollowing(uidB, uidA)
    }

    override suspend fun getFollowers(targetUid: String, limit: Int): ProfileOperationResult<List<PublicProfile>> {
        val list = follows.filter { it.second == targetUid }.mapNotNull { profileRepo.publicProfiles[it.first] }.take(limit)
        return ProfileOperationResult.Success(list)
    }

    override suspend fun getFollowing(followerUid: String, limit: Int): ProfileOperationResult<List<PublicProfile>> {
        val list = follows.filter { it.first == followerUid }.mapNotNull { profileRepo.publicProfiles[it.second] }.take(limit)
        return ProfileOperationResult.Success(list)
    }

    override suspend fun getFollowCounts(uid: String): ProfileOperationResult<FollowCounts> {
        val followersCount = follows.count { it.second == uid }
        val followingCount = follows.count { it.first == uid }
        profileRepo.publicProfiles[uid]?.let {
            profileRepo.publicProfiles[uid] = it.copy(
                followerCount = followersCount,
                followingCount = followingCount
            )
        }
        return ProfileOperationResult.Success(FollowCounts(followersCount, followingCount))
    }

    override suspend fun blockUser(blockerUid: String, blockedUid: String): ProfileOperationResult<Boolean> {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return ProfileOperationResult.Error("Invalid UID")
        if (blockerUid == blockedUid) return ProfileOperationResult.Error("Self-block prohibited")
        blocks.add(blockerUid to blockedUid)
        unfollowUser(blockerUid, blockedUid)
        unfollowUser(blockedUid, blockerUid)
        return ProfileOperationResult.Success(true)
    }

    override suspend fun unblockUser(blockerUid: String, blockedUid: String): ProfileOperationResult<Boolean> {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return ProfileOperationResult.Error("Invalid UID")
        blocks.remove(blockerUid to blockedUid)
        return ProfileOperationResult.Success(true)
    }

    override suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean {
        return blocks.contains(blockerUid to blockedUid)
    }

    override suspend fun isEitherBlocked(uidA: String, uidB: String): Boolean {
        return isBlocked(uidA, uidB) || isBlocked(uidB, uidA)
    }

    override suspend fun getBlockedUserIds(blockerUid: String): ProfileOperationResult<List<String>> {
        return ProfileOperationResult.Success(blocks.filter { it.first == blockerUid }.map { it.second })
    }

    override suspend fun getBlockedProfiles(blockerUid: String): ProfileOperationResult<List<PublicProfile>> {
        val profiles = blocks.filter { it.first == blockerUid }.map { (_, blockedUid) ->
            profileRepo.publicProfiles[blockedUid] ?: PublicProfile(uid = blockedUid, displayName = "WorkDaily Member")
        }
        return ProfileOperationResult.Success(profiles)
    }

    override suspend fun removeFollower(targetUid: String, followerUid: String): ProfileOperationResult<Boolean> {
        return unfollowUser(followerUid, targetUid)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSwitchFollowBlockFixesTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var prefsRepo: MultiAccountFakePrefsRepository
    private lateinit var attendanceRepo: MultiAccountFakeAttendanceRepository
    private lateinit var workspaceManager: InMemoryTestWorkspaceStorageManager
    private lateinit var onlineProfileRepo: MultiAccountFakeOnlineProfileRepository
    private lateinit var publicProfileRepo: MultiAccountFakePublicProfileRepository
    private lateinit var viewModel: OfflineAuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        prefsRepo = MultiAccountFakePrefsRepository()
        attendanceRepo = MultiAccountFakeAttendanceRepository()
        workspaceManager = InMemoryTestWorkspaceStorageManager(attendanceRepo, prefsRepo)
        onlineProfileRepo = MultiAccountFakeOnlineProfileRepository()
        publicProfileRepo = MultiAccountFakePublicProfileRepository()
        viewModel = OfflineAuthViewModel(
            repository = prefsRepo,
            localAttendanceRepository = attendanceRepo,
            onlineProfileRepository = onlineProfileRepo,
            publicProfileRepository = publicProfileRepo,
            workspaceStorageManager = workspaceManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `already registered Google account logs in directly without repeating registration`() = runBlocking {
        onlineProfileRepo.profiles["uid-existing-1"] = OnlineProfile(
            uid = "uid-existing-1",
            localWorkspaceId = "ws-existing-1",
            displayName = "Anita Verma",
            email = "anita@example.com",
            phoneNumber = "9876543210",
            countryCode = "IN",
            currencyCode = "INR"
        )
        publicProfileRepo.publicProfiles["uid-existing-1"] = PublicProfile(
            uid = "uid-existing-1",
            displayName = "Anita Verma",
            username = "anita_verma",
            usernameLower = "anita_verma",
            profession = "Civil Supervisor",
            bio = "Bridge construction"
        )
        publicProfileRepo.usernameOwners["anita_verma"] = "uid-existing-1"

        val account = OnlineAccount(
            uid = "uid-existing-1",
            displayName = "Anita Verma",
            email = "anita@example.com",
            photoUrl = "https://example.com/anita.jpg",
            isEmailVerified = true
        )

        viewModel.resolveAuthenticatedAccount(account).join()

        val resState = viewModel.accountResolutionState.value
        assertTrue("Expected direct login without setup screen", resState is AccountResolutionState.SignedInExisting)
        assertEquals("ws-existing-1", (resState as AccountResolutionState.SignedInExisting).workspaceUserId)
        assertEquals(OfflineLoginState.Success("ws-existing-1"), viewModel.loginState.value)
        assertEquals("uid-existing-1", prefsRepo.savedLinkedFirebaseUid)
        assertTrue(prefsRepo.savedOnlineAccountSetupCompleted)
        assertEquals("ws-existing-1", attendanceRepo.storedUser?.id)
        assertEquals("9876543210", attendanceRepo.storedUser?.phoneNumber)
    }

    @Test
    fun `switching from User A to registered User B and back isolates workspaces and preserves attendance`() = runBlocking {
        // 1. Register User A and add local attendance + custom ₹900 wage
        val accountA = OnlineAccount(
            uid = "uid-user-a",
            displayName = "Rajesh Kumar",
            email = "rajesh@example.com",
            photoUrl = "https://example.com/a.jpg",
            isEmailVerified = true
        )
        viewModel.completeProfile(
            googleAccount = accountA,
            enteredName = "Rajesh Kumar",
            enteredUsername = "rajesh_kumar",
            enteredPhone = "9111111111",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 900.0
        ) { _, _ -> }.join()

        val userAWorkspaceId = attendanceRepo.storedUser!!.id
        attendanceRepo.storedAttendance.add(
            AttendanceEntity(
                id = "att-a-1",
                workerId = userAWorkspaceId,
                date = "2026-10-25",
                status = "present",
                type = "full"
            )
        )

        // 2. Pre-populate registered User B in Firestore
        onlineProfileRepo.profiles["uid-user-b"] = OnlineProfile(
            uid = "uid-user-b",
            localWorkspaceId = "ws-user-b",
            displayName = "Sunil Patel",
            email = "sunil@example.com",
            phoneNumber = "9222222222",
            countryCode = "IN",
            currencyCode = "INR"
        )
        publicProfileRepo.publicProfiles["uid-user-b"] = PublicProfile(
            uid = "uid-user-b",
            displayName = "Sunil Patel",
            username = "sunil_patel",
            usernameLower = "sunil_patel"
        )
        publicProfileRepo.usernameOwners["sunil_patel"] = "uid-user-b"

        // 3. User A logs out, then User B signs in with Google
        viewModel.logout().join()
        assertNull("Auth token cleared on logout", prefsRepo.savedAuthToken)

        val accountB = OnlineAccount(
            uid = "uid-user-b",
            displayName = "Sunil Patel",
            email = "sunil@example.com",
            photoUrl = "https://example.com/b.jpg",
            isEmailVerified = true
        )
        viewModel.resolveAuthenticatedAccount(accountB).join()

        // Verify User B logged straight in with ZERO leakage of User A's attendance, wage, or phone
        val resB = viewModel.accountResolutionState.value
        assertTrue("Expected User B to sign straight in without mismatch block", resB is AccountResolutionState.SignedInExisting)
        assertEquals("ws-user-b", attendanceRepo.storedUser?.id)
        assertEquals("Sunil Patel", attendanceRepo.storedUser?.name)
        assertEquals("9222222222", attendanceRepo.storedUser?.phoneNumber)
        assertEquals("User B should not inherit User A's ₹900 wage", 500.0, attendanceRepo.storedUser?.dailyWage ?: 0.0, 0.01)
        assertTrue("User B should have zero of User A's attendance records", attendanceRepo.storedAttendance.isEmpty())
        assertEquals("uid-user-b", prefsRepo.savedLinkedFirebaseUid)

        // 4. User B logs out and User A logs back in -> User A's ₹900 wage & attendance return intact!
        viewModel.logout().join()
        viewModel.resolveAuthenticatedAccount(accountA).join()

        val resA = viewModel.accountResolutionState.value
        assertTrue(resA is AccountResolutionState.SignedInExisting)
        assertEquals(userAWorkspaceId, attendanceRepo.storedUser?.id)
        assertEquals("9111111111", attendanceRepo.storedUser?.phoneNumber)
        assertEquals(900.0, attendanceRepo.storedUser?.dailyWage ?: 0.0, 0.01)
        assertEquals(1, attendanceRepo.storedAttendance.size)
        assertEquals("att-a-1", attendanceRepo.storedAttendance.first().id)
        assertEquals("uid-user-a", prefsRepo.savedLinkedFirebaseUid)
    }

    @Test
    fun `switching from User A to brand new User C never leaks User A profile fields into setup`() = runBlocking {
        val accountA = OnlineAccount(
            uid = "uid-user-a",
            displayName = "Rajesh Kumar",
            email = "rajesh@example.com",
            photoUrl = null,
            isEmailVerified = true
        )
        viewModel.completeProfile(
            googleAccount = accountA,
            enteredName = "Rajesh Kumar",
            enteredUsername = "rajesh_kumar",
            enteredPhone = "9111111111",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 950.0
        ) { _, _ -> }.join()
        val userAWorkspaceId = attendanceRepo.storedUser!!.id

        viewModel.logout().join()

        val accountC = OnlineAccount(
            uid = "uid-new-c",
            displayName = "Meena Devi",
            email = "meena@example.com",
            photoUrl = null,
            isEmailVerified = true
        )
        viewModel.resolveAuthenticatedAccount(accountC).join()

        val setupState = viewModel.accountResolutionState.value
        assertTrue("New User C must enter profile setup", setupState is AccountResolutionState.NeedsProfileSetup)
        setupState as AccountResolutionState.NeedsProfileSetup
        assertEquals("Meena Devi", setupState.prefillName)
        assertEquals("User A's phone must never leak into User C's setup", "", setupState.prefillPhone)
        assertEquals("User A's daily wage must never leak into User C's setup", 0.0, setupState.prefillDailyWage, 0.01)
        assertFalse(setupState.isExistingLocalUpgrade)

        // Complete setup for User C and verify isolated workspace
        viewModel.completeProfile(
            googleAccount = accountC,
            enteredName = "Meena Devi",
            enteredUsername = "meena_devi",
            enteredPhone = "9333333333",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 650.0
        ) { _, _ -> }.join()

        assertNotEquals(userAWorkspaceId, attendanceRepo.storedUser?.id)
        assertEquals("Meena Devi", attendanceRepo.storedUser?.name)
        assertEquals("9333333333", attendanceRepo.storedUser?.phoneNumber)
        assertEquals(650.0, attendanceRepo.storedUser?.dailyWage ?: 0.0, 0.01)
        assertTrue("User A's workspace remains archived safely", workspaceManager.hasArchivedWorkspace("uid-user-a"))
    }

    @Test
    fun `unlinked offline workspace with attendance surfaces conflict and archives safely if switched`() = runBlocking {
        // Offline-only user with attendance records (no linked_firebase_uid)
        val offlineUser = UserEntity(
            id = "offline-ws-001",
            name = "Offline Worker",
            email = "",
            phoneNumber = "9000000000",
            role = "personal",
            dailyWage = 700.0
        )
        attendanceRepo.saveUser(offlineUser)
        attendanceRepo.storedAttendance.add(
            AttendanceEntity(id = "att-off-1", workerId = "offline-ws-001", date = "2026-10-26", status = "present", type = "full")
        )

        // Registered cloud account bound to a different workspace
        onlineProfileRepo.profiles["uid-cloud-existing"] = OnlineProfile(
            uid = "uid-cloud-existing",
            localWorkspaceId = "cloud-ws-999",
            displayName = "Cloud User",
            email = "cloud@example.com"
        )
        publicProfileRepo.publicProfiles["uid-cloud-existing"] = PublicProfile(
            uid = "uid-cloud-existing",
            displayName = "Cloud User",
            username = "cloud_user",
            usernameLower = "cloud_user"
        )
        publicProfileRepo.usernameOwners["cloud_user"] = "uid-cloud-existing"

        val cloudAccount = OnlineAccount(
            uid = "uid-cloud-existing",
            displayName = "Cloud User",
            email = "cloud@example.com",
            photoUrl = null,
            isEmailVerified = true
        )

        viewModel.resolveAuthenticatedAccount(cloudAccount).join()

        val conflict = viewModel.accountResolutionState.value
        assertTrue("Must surface conflict instead of overwriting offline data", conflict is AccountResolutionState.WorkspaceConflict)
        conflict as AccountResolutionState.WorkspaceConflict
        assertTrue(conflict.canSwitchToAccountWorkspace)
        assertEquals("offline-ws-001", attendanceRepo.storedUser?.id)

        // Explicitly switch: archives offline workspace without deleting attendance
        viewModel.switchFromUnlinkedToRegisteredAccount(cloudAccount).join()
        assertEquals("cloud-ws-999", attendanceRepo.storedUser?.id)
        assertEquals(1, workspaceManager.getArchivedAttendanceCount("offline_offline-ws-001"))
    }

    @Test
    fun `profile update preserves follower and following counts and username ownership is respected`() = runBlocking {
        val harness = SocialBlockAndFollowHarness(publicProfileRepo)

        publicProfileRepo.ensureOrCreatePublicProfile("uid-1", "User One", null)
        publicProfileRepo.ensureOrCreatePublicProfile("uid-2", "User Two", null)
        publicProfileRepo.updatePublicProfile("uid-1", "user_one", "User One", "", "Mason", null, true, "", null, "")
        publicProfileRepo.updatePublicProfile("uid-2", "user_two", "User Two", "", "Carpenter", null, true, "", null, "")

        // User 1 follows User 2
        assertTrue(harness.followUser("uid-1", "uid-2") is ProfileOperationResult.Success)
        assertEquals(1, publicProfileRepo.publicProfiles["uid-1"]?.followingCount)
        assertEquals(1, publicProfileRepo.publicProfiles["uid-2"]?.followerCount)

        // Editing User 1's profile MUST preserve followingCount = 1 (root cause of Profile 0 vs People 1 mismatch!)
        val updatedRes = publicProfileRepo.updatePublicProfile(
            firebaseUid = "uid-1",
            newUsername = "user_one",
            displayName = "User One Updated",
            bio = "Updated bio",
            profession = "Lead Mason",
            photoUrl = null,
            isPublic = true,
            gender = "Male",
            dateOfBirth = "1994-05-21",
            countryCode = "IN"
        )
        assertTrue(updatedRes is ProfileOperationResult.Success)
        assertEquals("Profile edit must not reset followingCount to 0", 1, publicProfileRepo.publicProfiles["uid-1"]?.followingCount)

        // Own username check returns Available, other user checking same username gets AlreadyTaken
        assertEquals(
            UsernameAvailabilityResult.Available,
            publicProfileRepo.checkUsernameAvailability("user_one", currentUserId = "uid-1")
        )
        assertEquals(
            UsernameAvailabilityResult.AlreadyTaken,
            publicProfileRepo.checkUsernameAvailability("user_one", currentUserId = "uid-2")
        )

        // Blocking removes follow relationship, prevents follow while blocked, and unblocking does not auto-follow
        assertTrue(harness.blockUser("uid-1", "uid-2") is ProfileOperationResult.Success)
        assertTrue(harness.isBlocked("uid-1", "uid-2"))
        assertFalse(harness.isFollowing("uid-1", "uid-2"))
        assertEquals(0, (harness.getFollowCounts("uid-1") as ProfileOperationResult.Success).data.followingCount)
        assertEquals(0, (harness.getFollowCounts("uid-2") as ProfileOperationResult.Success).data.followerCount)
        assertTrue(harness.followUser("uid-2", "uid-1") is ProfileOperationResult.Error)

        val blockedProfiles = (harness.getBlockedProfiles("uid-1") as ProfileOperationResult.Success).data
        assertEquals(1, blockedProfiles.size)
        assertEquals("uid-2", blockedProfiles.first().uid)

        assertTrue(harness.unblockUser("uid-1", "uid-2") is ProfileOperationResult.Success)
        assertFalse(harness.isBlocked("uid-1", "uid-2"))
        assertFalse("Unblocking must never auto-follow", harness.isFollowing("uid-1", "uid-2"))
        assertTrue((harness.getBlockedProfiles("uid-1") as ProfileOperationResult.Success).data.isEmpty())
    }
}
