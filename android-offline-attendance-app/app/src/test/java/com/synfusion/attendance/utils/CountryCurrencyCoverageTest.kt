package com.synfusion.attendance.utils

import com.synfusion.attendance.data.model.CountryRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryCurrencyCoverageTest {

    @Test
    fun testAllSupportedCountriesFormatWithSymbol() {
        val testAmount = 2700.0
        val supportedCountries = CountryRepository.SUPPORTED_COUNTRIES

        assertTrue("SUPPORTED_COUNTRIES should not be empty", supportedCountries.isNotEmpty())

        for (country in supportedCountries) {
            val formatted = AmountFormatter.formatWithSymbol(testAmount, country.currencyCode)
            val symbol = country.currencySymbol

            assertTrue(
                "Formatted amount '$formatted' for ${country.displayName} (${country.currencyCode}) should contain expected symbol '$symbol'",
                formatted.contains(symbol)
            )

            // Verify numeric value 2,700 or 2700 is present in output
            assertTrue(
                "Formatted amount '$formatted' for ${country.displayName} should contain numeric string '2,700' or '2700'",
                formatted.contains("2,700") || formatted.contains("2700")
            )

            // Verify currency code (e.g. INR, USD) is not appended as a plain string word unless the currency symbol itself is equal to the currency code (like AED)
            if (symbol != country.currencyCode) {
                assertFalse(
                    "Formatted amount '$formatted' should use symbol '$symbol' rather than text currency name/code '${country.currencyCode}'",
                    formatted.endsWith(" ${country.currencyCode}")
                )
            }
        }
    }

    @Test
    fun testZeroAndDecimalAmountFormattingAcrossCurrencies() {
        val zeroFormattedInr = AmountFormatter.formatWithSymbol(0.0, "INR")
        assertTrue("Zero formatted INR should contain symbol ₹", zeroFormattedInr.contains("₹"))

        val zeroFormattedUsd = AmountFormatter.formatWithSymbol(0.0, "USD")
        assertTrue("Zero formatted USD should contain symbol $", zeroFormattedUsd.contains("$"))

        val zeroFormattedAed = AmountFormatter.formatWithSymbol(0.0, "AED")
        assertTrue("Zero formatted AED should contain symbol AED", zeroFormattedAed.contains("AED"))
    }
}
