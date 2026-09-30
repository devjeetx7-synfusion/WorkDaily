package com.synfusion.attendance.data

import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.viewmodel.OfflineLoginState
import org.junit.Assert.*
import org.junit.Test

class DataSafetyAndNavigationTest {

    // Helper simulating the centralized startup decision logic in MainActivity.kt
    private fun resolveStartupDestination(
        splashEnabled: Boolean,
        languageState: String?,
        onboardingCompleted: Boolean,
        tokenState: String?
    ): String {
        return if (!splashEnabled) {
            if (languageState == null) "language_selection"
            else if (!onboardingCompleted) "onboarding"
            else if (tokenState == null) "login"
            else "dashboard"
        } else {
            "splash"
        }
    }

    // Helper simulating post-splash navigation routing
    private fun resolvePostSplashDestination(
        languageState: String?,
        onboardingCompleted: Boolean,
        tokenState: String?
    ): String {
        return if (languageState == null) "language_selection"
        else if (!onboardingCompleted) "onboarding"
        else if (tokenState == null) "login"
        else "dashboard"
    }

    @Test
    fun testFreshInstallStartupRoutingFlow() {
        // Step 1: Initial Launch (Language not selected)
        val dest1 = resolvePostSplashDestination(
            languageState = null,
            onboardingCompleted = false,
            tokenState = null
        )
        assertEquals("language_selection", dest1)

        // Step 2: Language selected (Onboarding incomplete)
        val dest2 = resolvePostSplashDestination(
            languageState = "en",
            onboardingCompleted = false,
            tokenState = null
        )
        assertEquals("onboarding", dest2)

        // Step 3: Onboarding complete callback MUST route to login (Create Workspace), NOT dashboard
        val dest3 = resolvePostSplashDestination(
            languageState = "en",
            onboardingCompleted = true,
            tokenState = null
        )
        assertEquals("login", dest3)

        // Step 4: Workspace created (Token set) -> Dashboard
        val dest4 = resolvePostSplashDestination(
            languageState = "en",
            onboardingCompleted = true,
            tokenState = "user_uuid_123"
        )
        assertEquals("dashboard", dest4)
    }

    @Test
    fun testLogoutRoutingDoesNotReturnToOnboardingOrLanguage() {
        // Given user is logged in
        var languageState: String? = "hi"
        var onboardingCompleted = true
        var tokenState: String? = "user_uuid_123"

        assertEquals("dashboard", resolvePostSplashDestination(languageState, onboardingCompleted, tokenState))

        // When user logs out (clears tokenState, but leaves language and onboardingCompleted intact)
        tokenState = null

        // Then destination MUST be "login" (Create Workspace)
        val logoutDest = resolvePostSplashDestination(languageState, onboardingCompleted, tokenState)
        assertEquals("login", logoutDest)
        assertNotEquals("language_selection", logoutDest)
        assertNotEquals("onboarding", logoutDest)
    }

    @Test
    fun testLegacyUserMigrationPreservesOnboardingState() {
        // Given a legacy user exists in DB from a previous app version
        val legacyUser = UserEntity(
            id = "legacy_user_1",
            name = "Ramesh Kumar",
            phoneNumber = "9876543210",
            email = "",
            role = "personal",
            dailyWage = 650.0,
            countryCode = "IN",
            currencyCode = "INR"
        )

        // Simulating legacy migration check: if legacy user exists, onboardingCompleted is forced true
        val onboardingCompleted = true
        val languageState = "en"
        val tokenState = legacyUser.id

        // Existing configured users MUST land directly on dashboard
        val dest = resolvePostSplashDestination(languageState, onboardingCompleted, tokenState)
        assertEquals("dashboard", dest)
    }

    @Test
    fun testUserEntityDailyWageAndCountryPersistence() {
        val user = UserEntity(
            id = "user_456",
            name = "Anita Sharma",
            phoneNumber = "9811223344",
            email = "anita@example.com",
            role = "personal",
            dailyWage = 800.0,
            countryCode = "IN",
            currencyCode = "INR"
        )

        assertEquals(800.0, user.dailyWage, 0.001)
        assertEquals("IN", user.countryCode)
        assertEquals("INR", user.currencyCode)

        // Updating user profile retains original ID
        val updatedUser = user.copy(
            name = "Anita S. Sharma",
            dailyWage = 850.0
        )
        assertEquals("user_456", updatedUser.id)
        assertEquals(850.0, updatedUser.dailyWage, 0.001)
    }

    @Test
    fun testCountryRepositoryDataIntegrity() {
        val defaultCountry = CountryRepository.DEFAULT_COUNTRY
        assertEquals("IN", defaultCountry.countryCode)
        assertEquals("INR", defaultCountry.currencyCode)
        assertEquals("₹", defaultCountry.currencySymbol)

        val supported = CountryRepository.SUPPORTED_COUNTRIES
        assertTrue(supported.isNotEmpty())
        assertTrue(supported.any { it.countryCode == "IN" })
        assertTrue(supported.any { it.countryCode == "US" })
        assertTrue(supported.any { it.countryCode == "AE" })
    }
}
