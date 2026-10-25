package com.synfusion.attendance.online

import org.junit.Assert.*
import org.junit.Test

class UsernameValidatorTest {

    @Test
    fun validate_validUsernames_returnsValid() {
        val validUsernames = listOf(
            "rahul_builder",
            "mohit_123",
            "siteworker07",
            "john_doe",
            "abc"
        )

        for (username in validUsernames) {
            val result = UsernameValidator.validate(username)
            assertTrue("Expected $username to be valid", result is UsernameValidationResult.Valid)
        }
    }

    @Test
    fun validate_tooShortUsername_returnsInvalid() {
        val result = UsernameValidator.validate("ab")
        assertTrue(result is UsernameValidationResult.Invalid)
        assertEquals("Username must be at least 3 characters", (result as UsernameValidationResult.Invalid).reason)
    }

    @Test
    fun validate_tooLongUsername_returnsInvalid() {
        val result = UsernameValidator.validate("a_very_long_username_exceeding_20_chars")
        assertTrue(result is UsernameValidationResult.Invalid)
        assertEquals("Username cannot exceed 20 characters", (result as UsernameValidationResult.Invalid).reason)
    }

    @Test
    fun validate_invalidCharacters_returnsInvalid() {
        val invalidUsernames = listOf(
            "rahul builder",
            "user@123",
            "worker-07",
            "user#123",
            "user!name"
        )

        for (username in invalidUsernames) {
            val result = UsernameValidator.validate(username)
            assertTrue("Expected $username to be invalid", result is UsernameValidationResult.Invalid)
        }
    }

    @Test
    fun validate_usernameWithPeriod_returnsValid() {
        val result = UsernameValidator.validate("user.name")
        assertTrue(result is UsernameValidationResult.Valid)
    }

    @Test
    fun validate_reservedUsernames_returnsInvalid() {
        val reserved = listOf("admin", "workdaily", "support", "official", "privacy")

        for (username in reserved) {
            val result = UsernameValidator.validate(username)
            assertTrue("Expected reserved username $username to be invalid", result is UsernameValidationResult.Invalid)
            assertEquals("This username is reserved by the system", (result as UsernameValidationResult.Invalid).reason)
        }
    }

    @Test
    fun generateCandidate_validDisplayName_generatesCleanCandidate() {
        val candidate = UsernameValidator.generateCandidate("Rahul Sharma", "uid123456789")
        assertTrue("Candidate should be valid: $candidate", UsernameValidator.validate(candidate) is UsernameValidationResult.Valid)
        assertTrue("Candidate should contain base", candidate.contains("rahulsharma"))
    }

    @Test
    fun generateCandidate_emptyDisplayName_generatesFallbackCandidate() {
        val candidate = UsernameValidator.generateCandidate("", "uid987654321")
        assertTrue("Candidate should be valid: $candidate", UsernameValidator.validate(candidate) is UsernameValidationResult.Valid)
    }
}
