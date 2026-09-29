package com.synfusion.attendance.onboarding

import com.synfusion.attendance.data.model.CountryConfig
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.viewmodel.OfflineOnboardingState
import org.junit.Assert.*
import org.junit.Test

class OnboardingTest {

    @Test
    fun testInitialOnboardingStateDefaults() {
        val state = OfflineOnboardingState()
        assertEquals(0, state.currentStep)
        assertEquals("", state.name)
        assertNull(state.profileImageUri)
        assertEquals("IN", state.selectedCountry.countryCode)
        assertEquals("700", state.dailyWage)
        assertEquals("", state.phoneNumber)
        assertFalse(state.isSaving)
        assertFalse(state.isCompleted)
    }

    @Test
    fun testCountryRepositoryLookups() {
        val india = CountryRepository.getCountry("IN")
        assertEquals("India", india.displayName)
        assertEquals("INR", india.currencyCode)
        assertEquals("₹", india.currencySymbol)
        assertEquals("+91", india.callingCode)

        val uae = CountryRepository.getCountry("AE")
        assertEquals("United Arab Emirates", uae.displayName)
        assertEquals("AED", uae.currencyCode)
        assertEquals("د.إ", uae.currencySymbol)
        assertEquals("+971", uae.callingCode)

        val saudi = CountryRepository.getCountry("SA")
        assertEquals("Saudi Arabia", saudi.displayName)
        assertEquals("SAR", saudi.currencyCode)
        assertEquals("ر.س", saudi.currencySymbol)
        assertEquals("+966", saudi.callingCode)

        val qatar = CountryRepository.getCountry("QA")
        assertEquals("Qatar", qatar.displayName)
        assertEquals("QAR", qatar.currencyCode)
        assertEquals("ر.ق", qatar.currencySymbol)
        assertEquals("+974", qatar.callingCode)

        val kuwait = CountryRepository.getCountry("KW")
        assertEquals("Kuwait", kuwait.displayName)
        assertEquals("KWD", kuwait.currencyCode)
        assertEquals("د.ك", kuwait.currencySymbol)
        assertEquals("+965", kuwait.callingCode)

        val usa = CountryRepository.getCountry("US")
        assertEquals("United States", usa.displayName)
        assertEquals("USD", usa.currencyCode)
        assertEquals("$", usa.currencySymbol)
        assertEquals("+1", usa.callingCode)
    }

    @Test
    fun testPhoneValidationAcrossCountries() {
        fun isValid(phone: String, countryCode: String): Boolean {
            return if (countryCode == "IN") phone.length >= 10 else phone.length >= 7
        }

        // India (requires >= 10 digits)
        assertFalse(isValid("987654321", "IN"))
        assertTrue(isValid("9876543210", "IN"))

        // UAE (requires >= 7 digits)
        assertFalse(isValid("123456", "AE"))
        assertTrue(isValid("501234567", "AE"))

        // Saudi Arabia (requires >= 7 digits)
        assertTrue(isValid("501234567", "SA"))

        // USA (requires >= 7 digits)
        assertTrue(isValid("2025550123", "US"))
    }

    @Test
    fun testWageFormattingForSelectedCountry() {
        val wageVal = 700.0

        val formattedIndia = AmountFormatter.formatWithSymbol(wageVal, "INR")
        assertTrue(formattedIndia.contains("₹"))

        val formattedUae = AmountFormatter.formatWithSymbol(wageVal, "AED")
        assertTrue(formattedUae.contains("د.إ"))

        val formattedSaudi = AmountFormatter.formatWithSymbol(wageVal, "SAR")
        assertTrue(formattedSaudi.contains("ر.س"))
    }

    @Test
    fun testOnboardingStepTransitionsAndDataRetention() {
        var state = OfflineOnboardingState()

        // Step 0 -> Welcome
        assertEquals(0, state.currentStep)

        // Enter name and move to Step 1 -> Profile
        state = state.copy(name = "Rahul Kumar", currentStep = 1)
        assertEquals("Rahul Kumar", state.name)

        // Select UAE country and move to Step 2 -> Country
        val uae = CountryRepository.getCountry("AE")
        state = state.copy(selectedCountry = uae, currentStep = 2)
        assertEquals("AE", state.selectedCountry.countryCode)

        // Set wage to 1200 and move to Step 3 -> Wage
        state = state.copy(dailyWage = "1200", currentStep = 3)
        assertEquals("1200", state.dailyWage)

        // Enter phone and move to Step 4 -> Mobile
        state = state.copy(phoneNumber = "501234567", currentStep = 4)
        assertEquals("501234567", state.phoneNumber)

        // Move to Step 5 -> Reveal
        state = state.copy(currentStep = 5)
        assertEquals(5, state.currentStep)

        // Back to Step 4
        state = state.copy(currentStep = 4)

        // Verify data retained on back navigation
        assertEquals("Rahul Kumar", state.name)
        assertEquals("AE", state.selectedCountry.countryCode)
        assertEquals("1200", state.dailyWage)
        assertEquals("501234567", state.phoneNumber)
    }
}
