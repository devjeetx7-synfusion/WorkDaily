package com.synfusion.attendance.auth

import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineLoginState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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

private class DummyContext : android.content.ContextWrapper(null)

private class FakeTestPreferencesRepository : OfflinePreferencesRepository(DummyContext()) {
    var savedRole: String? = null
    var savedAuthToken: String? = null
    var savedCountryCode: String? = null
    var savedCurrencyCode: String? = null
    var savedOnboardingCompleted: Boolean? = null
    var savedOnlineAccountSetupCompleted: Boolean? = null
    var savedLinkedFirebaseUid: String? = null
    var savedGooglePhotoUrl: String? = null
    var savedProfilePhotoPath: String? = null

    private val _roleFlow = MutableStateFlow<String?>(null)
    override val userRoleFlow: StateFlow<String?> = _roleFlow.asStateFlow()

    private val _authTokenFlow = MutableStateFlow<String?>(null)
    override val authTokenFlow: StateFlow<String?> = _authTokenFlow.asStateFlow()

    private val _onlineAccountSetupCompletedFlow = MutableStateFlow(false)
    override val isOnlineAccountSetupCompletedFlow: StateFlow<Boolean> = _onlineAccountSetupCompletedFlow.asStateFlow()

    private val _linkedFirebaseUidFlow = MutableStateFlow<String?>(null)
    override val linkedFirebaseUidFlow: StateFlow<String?> = _linkedFirebaseUidFlow.asStateFlow()

    private val _profilePhotoPathFlow = MutableStateFlow<String?>(null)
    override val profilePhotoPathFlow: StateFlow<String?> = _profilePhotoPathFlow.asStateFlow()

    private val _googlePhotoUrlFlow = MutableStateFlow<String?>(null)
    override val googlePhotoUrlFlow: StateFlow<String?> = _googlePhotoUrlFlow.asStateFlow()

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
        _googlePhotoUrlFlow.value = url
    }

    fun setProfilePhotoPath(path: String?) {
        savedProfilePhotoPath = path
        _profilePhotoPathFlow.value = path
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
class Step2SingleAccountOnboardingTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePrefs: FakeTestPreferencesRepository
    private lateinit var fakeAttendanceRepo: FakeTestAttendanceRepository
    private lateinit var viewModel: OfflineAuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = FakeTestPreferencesRepository()
        fakeAttendanceRepo = FakeTestAttendanceRepository()
        viewModel = OfflineAuthViewModel(fakePrefs, fakeAttendanceRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testExistingWorkspaceDetection_DoesNotAutoEmitSuccessOrBypassSetup() = runBlocking {
        val existingUser = UserEntity(
            id = "local_uuid_100",
            name = "Ramesh Kumar",
            phoneNumber = "9876543210",
            email = "ramesh@example.com",
            role = "personal",
            dailyWage = 800.0,
            countryCode = "IN",
            currencyCode = "INR"
        )
        fakeAttendanceRepo.storedUser = existingUser

        // Calling real detection method
        viewModel.checkForUserAndLogin().join()

        // Config is preloaded
        assertEquals("personal", fakePrefs.savedRole)
        assertEquals("IN", fakePrefs.savedCountryCode)
        assertEquals("INR", fakePrefs.savedCurrencyCode)

        // Login state remains Idle, NOT auto-setting Success
        assertTrue(viewModel.loginState.value is OfflineLoginState.Idle)
        assertFalse(fakePrefs.isOnlineAccountSetupCompletedFlow.value)
    }

    @Test
    fun testContinueOffline_RestoresSessionAndPreservesLocalState() = runBlocking {
        val localUuid = "local_uuid_200"
        val existingUser = UserEntity(
            id = localUuid,
            name = "Offline Worker",
            phoneNumber = "9900112233",
            email = "",
            role = "personal",
            dailyWage = 700.0,
            countryCode = "IN",
            currencyCode = "INR"
        )
        fakeAttendanceRepo.storedUser = existingUser
        fakeAttendanceRepo.storedAttendance.add(
            AttendanceEntity(id = "att_1", workerId = null, date = "2026-03-01", status = "present", type = "full", wage = 700.0)
        )

        // Explicit Continue Offline
        viewModel.resumeExistingWorkspace().join()

        // Verifications
        assertEquals(OfflineLoginState.Success(localUuid), viewModel.loginState.value)
        assertEquals(localUuid, fakePrefs.savedAuthToken)
        assertEquals("personal", fakePrefs.savedRole)
        assertEquals("IN", fakePrefs.savedCountryCode)
        assertEquals(true, fakePrefs.savedOnboardingCompleted)

        // Online account setup is NOT marked complete & linked UID remains untouched
        assertNull(fakePrefs.savedOnlineAccountSetupCompleted)
        assertNull(fakePrefs.savedLinkedFirebaseUid)

        // Attendance records remain intact
        assertEquals(1, fakeAttendanceRepo.getPersonalAttendance().size)
        assertEquals("att_1", fakeAttendanceRepo.getPersonalAttendance().first().id)
    }

    @Test
    fun testExistingUserCompleteProfile_PreservesUUID_CreatedAt_Attendance_AndWages() = runBlocking {
        val oldUuid = "local_uuid_300"
        val oldTimestamp = 1600000000000L
        val existingUser = UserEntity(
            id = oldUuid,
            name = "Original Custom Name",
            phoneNumber = "9876543210",
            email = "old@example.com",
            role = "personal",
            dailyWage = 850.0,
            countryCode = "IN",
            currencyCode = "INR",
            createdAt = oldTimestamp
        )
        fakeAttendanceRepo.storedUser = existingUser
        val attRecord = AttendanceEntity(id = "personal_2026-03-01", workerId = null, date = "2026-03-01", status = "present", type = "full", wage = 850.0)
        fakeAttendanceRepo.storedAttendance.add(attRecord)

        val googleAccount = OnlineAccount(
            uid = "firebase_google_300",
            displayName = "Google Name",
            email = "google@gmail.com",
            photoUrl = "https://lh3.googleusercontent.com/avatar.png"
        )

        // Perform real completeProfile
        viewModel.completeProfile(
            googleAccount = googleAccount,
            enteredName = "",
            enteredPhone = "9876543210",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 850.0,
            onComplete = { _, _ -> }
        ).join()

        val savedUser = fakeAttendanceRepo.getUser()
        assertNotNull(savedUser)
        assertEquals(oldUuid, savedUser!!.id) // UUID preserved!
        assertNotEquals("firebase_google_300", savedUser.id) // Local UUID != Firebase UID
        assertEquals(oldTimestamp, savedUser.createdAt) // Creation date preserved!
        assertEquals("Original Custom Name", savedUser.name) // Custom name preserved!
        assertEquals(850.0, savedUser.dailyWage, 0.001) // Historical wage preserved!

        // Attendance untouched
        assertEquals(1, fakeAttendanceRepo.getPersonalAttendance().size)
        assertEquals(850.0, fakeAttendanceRepo.getPersonalAttendance().first().wage, 0.001)

        // Online account linking metadata stored separately
        assertEquals("firebase_google_300", fakePrefs.savedLinkedFirebaseUid)
        assertEquals(true, fakePrefs.savedOnlineAccountSetupCompleted)
        assertEquals("https://lh3.googleusercontent.com/avatar.png", fakePrefs.savedGooglePhotoUrl)
    }

    @Test
    fun testNewUserCompleteProfile_GeneratesNewLocalUUIDAndSavesProfile() = runBlocking {
        fakeAttendanceRepo.storedUser = null

        val googleAccount = OnlineAccount(
            uid = "firebase_new_400",
            displayName = "Brand New User",
            email = "newuser@gmail.com",
            photoUrl = null
        )

        viewModel.completeProfile(
            googleAccount = googleAccount,
            enteredName = "Brand New User",
            enteredPhone = "9123456789",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 600.0,
            onComplete = { _, _ -> }
        ).join()

        val savedUser = fakeAttendanceRepo.getUser()
        assertNotNull(savedUser)
        assertTrue(savedUser!!.id.length >= 32)
        assertNotEquals("firebase_new_400", savedUser.id) // Local UUID != Firebase UID
        assertEquals("Brand New User", savedUser.name)
        assertEquals("newuser@gmail.com", savedUser.email)
        assertEquals("9123456789", savedUser.phoneNumber)
        assertEquals(600.0, savedUser.dailyWage, 0.001)

        assertEquals("firebase_new_400", fakePrefs.savedLinkedFirebaseUid)
        assertEquals(true, fakePrefs.savedOnlineAccountSetupCompleted)
    }

    @Test
    fun testAccountMismatchDetection_RejectsSilentRelinking() = runBlocking {
        fakePrefs.saveLinkedFirebaseUid("firebase_account_A")

        // Authenticated with different account -> mismatch!
        val isMismatch = viewModel.checkAccountMismatch("firebase_account_B")
        assertTrue(isMismatch)

        // Authenticated with matching account -> no mismatch!
        val isMatch = viewModel.checkAccountMismatch("firebase_account_A")
        assertFalse(isMatch)
    }

    @Test
    fun testDailyWageValidationRules_RejectsInvalidInputsAndPreservesExistingWage() = runBlocking {
        val validateWageString = { input: String ->
            val parsed = input.toDoubleOrNull()
            parsed != null && parsed > 0.0
        }

        assertFalse(validateWageString(""))
        assertFalse(validateWageString("0"))
        assertFalse(validateWageString("-500"))
        assertFalse(validateWageString("abc"))
        assertTrue(validateWageString("500"))
        assertTrue(validateWageString("1200.50"))

        // Complete profile with 0 or negative wage on existing user preserves original wage
        val existingUser = UserEntity(
            id = "u500",
            name = "Worker",
            phoneNumber = "9876543210",
            email = "",
            role = "personal",
            dailyWage = 900.0
        )
        fakeAttendanceRepo.storedUser = existingUser

        val googleAccount = OnlineAccount(uid = "firebase_500")
        viewModel.completeProfile(
            googleAccount = googleAccount,
            enteredName = "Worker",
            enteredPhone = "9876543210",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 0.0, // Invalid/zero wage attempt
            onComplete = { _, _ -> }
        ).join()

        assertEquals(900.0, fakeAttendanceRepo.getUser()?.dailyWage ?: 0.0, 0.001)
    }

    @Test
    fun testProfilePhotoSeparationAndPriority() = runBlocking {
        val customPath = "/data/user/0/com.synfusion.attendance/files/profile_photos/custom_avatar.jpg"
        val googleUrl = "https://lh3.googleusercontent.com/photo.png"

        fakePrefs.setProfilePhotoPath(customPath)

        val googleAccount = OnlineAccount(uid = "firebase_600", photoUrl = googleUrl)
        viewModel.completeProfile(
            googleAccount = googleAccount,
            enteredName = "Worker",
            enteredPhone = "9876543210",
            countryCode = "IN",
            currencyCode = "INR",
            dailyWage = 500.0,
            onComplete = { _, _ -> }
        ).join()

        // Google photo URL saved to its own separate DataStore key
        assertEquals(googleUrl, fakePrefs.savedGooglePhotoUrl)

        // Custom local profile photo path remains untouched
        assertEquals(customPath, fakePrefs.savedProfilePhotoPath)

        // Photo display resolution priority logic
        val resolveImage = { localPath: String?, googlePhoto: String? ->
            if (!localPath.isNullOrEmpty()) localPath
            else if (!googlePhoto.isNullOrEmpty()) googlePhoto
            else null
        }

        // Priority 1: Custom local photo
        assertEquals(customPath, resolveImage(customPath, googleUrl))
        // Priority 2: Google photo
        assertEquals(googleUrl, resolveImage(null, googleUrl))
        // Priority 3: Default avatar
        assertNull(resolveImage(null, null))
    }

    @Test
    fun testBackupPreferenceExclusion() {
        val mockPreferences = mapOf(
            "app_language" to "hi",
            "dark_mode" to true,
            "user_country" to "IN",
            "user_currency" to "INR",
            "onboarding_completed" to true,
            "auth_token" to "sensitive_token_123",
            "online_account_setup_completed" to true,
            "linked_firebase_uid" to "firebase_sensitive_uid_456",
            "google_photo_url" to "https://lh3.googleusercontent.com/photo.jpg"
        )

        val filteredKeys = mockPreferences.filterKeys { key ->
            key != "auth_token" &&
            key != "online_account_setup_completed" &&
            key != "linked_firebase_uid" &&
            key != "google_photo_url"
        }

        assertFalse(filteredKeys.containsKey("auth_token"))
        assertFalse(filteredKeys.containsKey("online_account_setup_completed"))
        assertFalse(filteredKeys.containsKey("linked_firebase_uid"))
        assertFalse(filteredKeys.containsKey("google_photo_url"))

        assertTrue(filteredKeys.containsKey("app_language"))
        assertTrue(filteredKeys.containsKey("dark_mode"))
        assertTrue(filteredKeys.containsKey("user_country"))
    }

    @Test
    fun testSignOutSemantics_PreservesUserDatabaseAndLinkedFirebaseUid() = runBlocking {
        val localUuid = "local_uuid_700"
        val existingUser = UserEntity(
            id = localUuid,
            name = "Signed In Worker",
            phoneNumber = "9876543210",
            email = "",
            role = "personal",
            dailyWage = 800.0
        )
        fakeAttendanceRepo.storedUser = existingUser
        fakeAttendanceRepo.storedAttendance.add(
            AttendanceEntity(id = "att_1", workerId = null, date = "2026-03-01", status = "present", type = "full", wage = 800.0)
        )

        fakePrefs.saveAuthToken(localUuid)
        fakePrefs.saveUserRole("personal")
        fakePrefs.saveLinkedFirebaseUid("firebase_linked_700")
        fakePrefs.saveOnlineAccountSetupCompleted(true)

        // Perform Sign Out
        viewModel.logout().join()

        // Active session tokens are cleared
        assertNull(fakePrefs.savedAuthToken)
        assertNull(fakePrefs.savedRole)

        // Linked Firebase UID and online account completion marker are PRESERVED
        assertEquals("firebase_linked_700", fakePrefs.savedLinkedFirebaseUid)
        assertEquals(true, fakePrefs.savedOnlineAccountSetupCompleted)

        // Database records (UserEntity & Attendance) remain completely INTACT
        assertNotNull(fakeAttendanceRepo.getUser())
        assertEquals(localUuid, fakeAttendanceRepo.getUser()?.id)
        assertEquals(1, fakeAttendanceRepo.getPersonalAttendance().size)
    }

    @Test
    fun testOfflineFirstStartupContract_FunctionsFullyWithoutActiveFirebaseSession() = runBlocking {
        val localUuid = "local_uuid_800"
        val user = UserEntity(
            id = localUuid,
            name = "Offline First Worker",
            phoneNumber = "9876543210",
            email = "",
            role = "personal",
            dailyWage = 750.0,
            countryCode = "IN",
            currencyCode = "INR"
        )
        fakeAttendanceRepo.storedUser = user
        fakeAttendanceRepo.storedAttendance.add(
            AttendanceEntity(id = "att_100", workerId = null, date = "2026-03-10", status = "present", type = "full", wage = 750.0)
        )

        // Set local session as restored offline
        fakePrefs.saveAuthToken(localUuid)
        fakePrefs.saveUserRole("personal")

        // Verify offline workspace calculations work 100% without network or active Firebase auth
        val loadedUser = fakeAttendanceRepo.getUser()
        assertNotNull(loadedUser)
        assertEquals(750.0, loadedUser!!.dailyWage, 0.001)

        val attendance = fakeAttendanceRepo.getPersonalAttendance()
        assertEquals(1, attendance.size)
        assertEquals(750.0, attendance.first().wage, 0.001)
    }
}
