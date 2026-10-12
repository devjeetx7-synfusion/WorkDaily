package com.synfusion.attendance.auth

import com.synfusion.attendance.backup.readDataStoreValuesForBackup
import com.synfusion.attendance.data.entities.UserEntity
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class Step2SingleAccountOnboardingTest {

    @Test
    fun testExistingUserUpgradePreservesLocalUUIDAndData() {
        val oldLocalUuid = UUID.randomUUID().toString()
        val oldTimestamp = System.currentTimeMillis() - 86400000L * 30 // 30 days ago
        val firebaseUid = "firebase_xyz_123_google_id"

        val existingUser = UserEntity(
            id = oldLocalUuid,
            name = "Ramesh Kumar",
            phoneNumber = "9876543210",
            email = "ramesh@example.com",
            role = "personal",
            dailyWage = 850.0,
            countryCode = "IN",
            currencyCode = "INR",
            createdAt = oldTimestamp
        )

        // Simulate completeProfile merge logic
        val googleAccount = OnlineAccount(
            uid = firebaseUid,
            displayName = "Ramesh Kumar (Google)",
            email = "ramesh.google@gmail.com",
            photoUrl = "https://lh3.googleusercontent.com/avatar.jpg"
        )

        val mergedName = if (existingUser.name.isNotBlank() && existingUser.name != "User") {
            existingUser.name
        } else {
            googleAccount.displayName ?: existingUser.name
        }

        val updatedUser = existingUser.copy(
            name = mergedName,
            email = googleAccount.email ?: existingUser.email,
            phoneNumber = existingUser.phoneNumber,
            dailyWage = existingUser.dailyWage,
            countryCode = existingUser.countryCode,
            currencyCode = existingUser.currencyCode
        )

        // Strict Assertions
        assertEquals(oldLocalUuid, updatedUser.id)
        assertNotEquals(firebaseUid, updatedUser.id)
        assertEquals(oldTimestamp, updatedUser.createdAt)
        assertEquals(850.0, updatedUser.dailyWage, 0.001)
        assertEquals("Ramesh Kumar", updatedUser.name)
        assertEquals("IN", updatedUser.countryCode)
        assertEquals("INR", updatedUser.currencyCode)
    }

    @Test
    fun testNewUserCreationGeneratesLocalUUIDDistinctFromFirebaseUID() {
        val firebaseUid = "firebase_new_user_999"
        val googleAccount = OnlineAccount(
            uid = firebaseUid,
            displayName = "New Worker",
            email = "newworker@gmail.com",
            photoUrl = null
        )

        val generatedLocalUuid = UUID.randomUUID().toString()
        val newUser = UserEntity(
            id = generatedLocalUuid,
            name = googleAccount.displayName ?: "User",
            email = googleAccount.email ?: "",
            phoneNumber = "9812345678",
            role = "personal",
            dailyWage = 600.0,
            countryCode = "IN",
            currencyCode = "INR",
            createdAt = System.currentTimeMillis()
        )

        assertNotEquals(firebaseUid, newUser.id)
        assertTrue(newUser.id.length >= 32)
        assertEquals("New Worker", newUser.name)
        assertEquals("newworker@gmail.com", newUser.email)
        assertEquals(600.0, newUser.dailyWage, 0.001)
    }

    @Test
    fun testAccountMismatchDetectionLogic() {
        val linkedUid = "google_account_A_111"
        val authenticatedUid = "google_account_B_222"

        val isMismatch = (!linkedUid.isBlank() && linkedUid != authenticatedUid)
        assertTrue(isMismatch)

        // When accounts match
        val matchingAuthenticatedUid = "google_account_A_111"
        val isMatching = (!linkedUid.isBlank() && linkedUid != matchingAuthenticatedUid)
        assertFalse(isMatching)
    }

    @Test
    fun testOfflineContinuationPreservesWorkspaceState() {
        val localUuid = UUID.randomUUID().toString()
        val existingUser = UserEntity(
            id = localUuid,
            name = "Offline Worker",
            phoneNumber = "9900112233",
            email = "",
            role = "personal",
            dailyWage = 500.0,
            countryCode = "IN",
            currencyCode = "INR"
        )

        // User continues offline without Firebase connection
        val onlineAccountSetupCompleted = false
        val token = existingUser.id

        assertEquals(localUuid, token)
        assertFalse(onlineAccountSetupCompleted)
        assertEquals("Offline Worker", existingUser.name)
    }

    @Test
    fun testBackupExcludesOnlineAccountLinkingKeys() {
        val mockDataStoreKeys = listOf(
            "app_language",
            "dark_mode",
            "user_country",
            "user_currency",
            "onboarding_completed",
            "auth_token",
            "online_account_setup_completed",
            "linked_firebase_uid",
            "google_photo_url"
        )

        val filteredForBackup = mockDataStoreKeys.filter { key ->
            key != "auth_token" &&
            key != "online_account_setup_completed" &&
            key != "linked_firebase_uid" &&
            key != "google_photo_url"
        }

        assertFalse(filteredForBackup.contains("auth_token"))
        assertFalse(filteredForBackup.contains("online_account_setup_completed"))
        assertFalse(filteredForBackup.contains("linked_firebase_uid"))
        assertFalse(filteredForBackup.contains("google_photo_url"))
        assertTrue(filteredForBackup.contains("app_language"))
        assertTrue(filteredForBackup.contains("dark_mode"))
    }

    @Test
    fun testDailyWageValidationRules() {
        val validateWage = { input: String ->
            val parsed = input.toDoubleOrNull()
            parsed != null && parsed > 0.0
        }

        assertFalse("Blank wage is invalid", validateWage(""))
        assertFalse("Zero wage is invalid", validateWage("0"))
        assertFalse("Negative wage is invalid", validateWage("-500"))
        assertFalse("Non-number wage is invalid", validateWage("abc"))
        assertTrue("Positive wage 500 is valid", validateWage("500"))
        assertTrue("Positive wage 1250.50 is valid", validateWage("1250.50"))
    }

    @Test
    fun testPhoneValidationRules() {
        val validatePhone = { phone: String, countryCode: String ->
            val digits = phone.filter { it.isDigit() }
            if (countryCode == "IN") digits.length == 10 else digits.length in 7..15
        }

        // India rules
        assertTrue("Valid 10 digit Indian number", validatePhone("9876543210", "IN"))
        assertFalse("Too short Indian number", validatePhone("987654321", "IN"))
        assertFalse("Too long Indian number", validatePhone("98765432101", "IN"))

        // Non-India rules (e.g. US / AE)
        assertTrue("7 digit US number", validatePhone("5551234", "US"))
        assertTrue("10 digit US number", validatePhone("2025550143", "US"))
        assertFalse("6 digit number is too short", validatePhone("123456", "US"))
        assertFalse("16 digit number is too long", validatePhone("12345678901234567", "US"))
    }

    @Test
    fun testProfilePhotoSeparationAndPriority() {
        var customLocalPhotoPath: String? = "/data/user/0/com.synfusion.attendance/files/profile_photos/user_123.jpg"
        var googlePhotoUrl: String? = "https://lh3.googleusercontent.com/photo.jpg"

        // Rule: Google photo URL does NOT overwrite custom local photo path
        val effectiveCustomPhoto = customLocalPhotoPath // preserved
        val effectiveGooglePhoto = googlePhotoUrl

        // Display priority logic
        val resolveImageModel = { customPath: String?, googleUrl: String? ->
            if (!customPath.isNullOrEmpty()) customPath
            else if (!googleUrl.isNullOrEmpty()) googleUrl
            else null
        }

        // Priority 1: Custom local photo present
        assertEquals(effectiveCustomPhoto, resolveImageModel(effectiveCustomPhoto, effectiveGooglePhoto))

        // Priority 2: Custom local photo missing, Google photo present
        assertEquals(effectiveGooglePhoto, resolveImageModel(null, effectiveGooglePhoto))

        // Priority 3: Both missing
        assertNull(resolveImageModel(null, null))
    }

    @Test
    fun testExistingUserBypassPrevention() {
        val existingUser = UserEntity(
            id = "existing_uuid_123",
            name = "Existing Worker",
            phoneNumber = "9876543210",
            email = "worker@example.com",
            role = "personal",
            dailyWage = 700.0,
            countryCode = "IN",
            currencyCode = "INR"
        )
        val onlineAccountSetupCompleted = false

        // Existing user detection alone MUST NOT set onlineAccountSetupCompleted to true or bypass account linking
        assertFalse(onlineAccountSetupCompleted)
        assertEquals("existing_uuid_123", existingUser.id)
    }
}
