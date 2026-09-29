package com.synfusion.attendance.utils

import androidx.compose.ui.text.AnnotatedString
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.viewmodel.OfflinePassbookLog
import org.junit.Assert.*
import org.junit.Test

class GlobalCurrencyFormattingTest {

    @Test
    fun test1_INR_IndianGrouping() {
        assertEquals("₹1,000.00", AmountFormatter.formatWithSymbol(1000.0, "INR"))
        assertEquals("₹12,000.00", AmountFormatter.formatWithSymbol(12000.0, "INR"))
        assertEquals("₹1,00,000.00", AmountFormatter.formatWithSymbol(100000.0, "INR"))
        assertEquals("₹12,00,000.00", AmountFormatter.formatWithSymbol(1200000.0, "INR"))
        assertEquals("₹1,00,00,000.00", AmountFormatter.formatWithSymbol(10000000.0, "INR"))
        assertEquals("₹12,34,56,789.12", AmountFormatter.formatWithSymbol(123456789.12, "INR"))
    }

    @Test
    fun test2_USD_WesternGrouping() {
        assertEquals("$1,000.00", AmountFormatter.formatWithSymbol(1000.0, "USD"))
        assertEquals("$12,000.00", AmountFormatter.formatWithSymbol(12000.0, "USD"))
        assertEquals("$100,000.00", AmountFormatter.formatWithSymbol(100000.0, "USD"))
        assertEquals("$1,000,000.00", AmountFormatter.formatWithSymbol(1000000.0, "USD"))
    }

    @Test
    fun test3_GBP_Formatting() {
        assertEquals("£1,000.00", AmountFormatter.formatWithSymbol(1000.0, "GBP"))
        assertEquals("£12,000.00", AmountFormatter.formatWithSymbol(12000.0, "GBP"))
        assertEquals("£100,000.00", AmountFormatter.formatWithSymbol(100000.0, "GBP"))
    }

    @Test
    fun test4_EUR_LocaleDifferences() {
        val eurFormatted = AmountFormatter.formatWithSymbol(1000.0, "EUR")
        assertTrue("EUR formatted should contain € and 1.000,00 or 1 000,00: '$eurFormatted'", eurFormatted.contains("€") && eurFormatted.contains("00"))
        val eurAmountFormatted = AmountFormatter.format(1000.0, "EUR")
        assertEquals("1.000,00", eurAmountFormatted)
    }

    @Test
    fun test5_6_7_ArabicGulfCurrencies() {
        val aedFormatted = AmountFormatter.formatWithSymbol(1000.0, "AED")
        assertTrue("AED should contain symbol 'د.إ': '$aedFormatted'", aedFormatted.contains("د.إ"))

        val sarFormatted = AmountFormatter.formatWithSymbol(1000.0, "SAR")
        assertTrue("SAR should contain symbol 'ر.س': '$sarFormatted'", sarFormatted.contains("ر.س"))

        val qarFormatted = AmountFormatter.formatWithSymbol(1000.0, "QAR")
        assertTrue("QAR should contain symbol 'ر.ق': '$qarFormatted'", qarFormatted.contains("ر.ق"))
    }

    @Test
    fun test8_9_10_ThreeDecimalCurrencies() {
        assertEquals("1.234", AmountFormatter.format(1.234, "KWD"))
        val kwdWithSym = AmountFormatter.formatWithSymbol(1.234, "KWD")
        assertTrue("KWD with symbol should contain '1.234': '$kwdWithSym'", kwdWithSym.contains("1.234"))

        assertEquals("12.500", AmountFormatter.format(12.5, "OMR"))
        val omrWithSym = AmountFormatter.formatWithSymbol(12.5, "OMR")
        assertTrue("OMR with symbol should contain '12.500': '$omrWithSym'", omrWithSym.contains("12.500"))

        assertEquals("100.000", AmountFormatter.format(100.0, "BHD"))
    }

    @Test
    fun test11_JPY_ZeroFractionDigits() {
        assertEquals("1,000", AmountFormatter.format(1000.0, "JPY"))
        assertEquals("¥1,000", AmountFormatter.formatWithSymbol(1000.0, "JPY"))
        assertEquals("¥12,500", AmountFormatter.formatWithSymbol(12500.0, "JPY"))
    }

    @Test
    fun test12_AllSupportedCurrencies() {
        for (country in CountryRepository.SUPPORTED_COUNTRIES) {
            val formatted = AmountFormatter.formatWithSymbol(1000.0, country.currencyCode)
            assertNotNull(formatted)
            assertTrue(formatted.isNotEmpty())
        }
    }

    @Test
    fun test13_ZeroFormatting() {
        assertEquals("₹0.00", AmountFormatter.formatWithSymbol(0.0, "INR"))
        assertEquals("$0.00", AmountFormatter.formatWithSymbol(0.0, "USD"))
        assertEquals("¥0", AmountFormatter.formatWithSymbol(0.0, "JPY"))
        val kwdZero = AmountFormatter.formatWithSymbol(0.0, "KWD")
        assertTrue(kwdZero.contains("0.000"))
    }

    @Test
    fun test14_LargeValues() {
        assertEquals("₹12,34,56,789.12", AmountFormatter.formatWithSymbol(123456789.12, "INR"))
        assertEquals("$123,456,789.12", AmountFormatter.formatWithSymbol(123456789.12, "USD"))
    }

    @Test
    fun test15_Decimals() {
        assertEquals("₹0.50", AmountFormatter.formatWithSymbol(0.5, "INR"))
        assertEquals("₹1.25", AmountFormatter.formatWithSymbol(1.25, "INR"))
        assertEquals("₹350.50", AmountFormatter.formatWithSymbol(350.5, "INR"))
        assertEquals("₹700.00", AmountFormatter.formatWithSymbol(700.0, "INR"))
    }

    @Test
    fun test16_NegativeValues() {
        assertEquals("-₹1,250.00", AmountFormatter.formatWithSymbol(-1250.0, "INR"))
        assertEquals("-$1,250.00", AmountFormatter.formatWithSymbol(-1250.0, "USD"))
        assertEquals("-1.250,00 €", AmountFormatter.formatWithSymbol(-1250.0, "EUR"))
    }

    @Test
    fun test17_ParsingFormattedInput() {
        assertEquals(120000.50, AmountFormatter.parseToDoubleOrNull("1,20,000.50", "INR")!!, 0.001)
        assertEquals(120000.50, AmountFormatter.parseToDoubleOrNull("₹1,20,000.50", "INR")!!, 0.001)
        assertEquals(120000.50, AmountFormatter.parseToDoubleOrNull("120,000.50", "USD")!!, 0.001)
        assertEquals(120000.50, AmountFormatter.parseToDoubleOrNull("$120,000.50", "USD")!!, 0.001)
        assertEquals(120000.50, AmountFormatter.parseToDoubleOrNull("120.000,50", "EUR")!!, 0.001)
    }

    @Test
    fun test18_CurrencyVisualTransformation() {
        val inrVt = CurrencyVisualTransformation("INR")
        val inrTransformed = inrVt.filter(AnnotatedString("100000"))
        assertEquals("1,00,000", inrTransformed.text.text)

        val usdVt = CurrencyVisualTransformation("USD")
        val usdTransformed = usdVt.filter(AnnotatedString("100000"))
        assertEquals("100,000", usdTransformed.text.text)

        val eurVt = CurrencyVisualTransformation("EUR")
        val eurTransformed = eurVt.filter(AnnotatedString("100000"))
        assertEquals("100.000", eurTransformed.text.text)
    }

    @Test
    fun test19_CountryPickerMetadata() {
        for (country in CountryRepository.SUPPORTED_COUNTRIES) {
            assertFalse("Country code must not be blank", country.countryCode.isBlank())
            assertFalse("Display name must not be blank", country.displayName.isBlank())
            assertFalse("Currency code must not be blank", country.currencyCode.isBlank())
            assertFalse("Currency symbol must not be blank", country.currencySymbol.isBlank())
            assertNotNull("Locale must not be null", country.locale)
            assertTrue("Fraction digits must be >= 0", country.fractionDigits >= 0)
        }
    }

    @Test
    fun test20_HistoricalRecordCurrencyPreservation() {
        val historicalEntityUsd = AttendanceEntity(
            id = "att1",
            workerId = null,
            date = "2024-01-01",
            status = "present",
            type = "full",
            wage = 100.0,
            currencyCode = "USD"
        )

        val formattedEntityUsd = AmountFormatter.formatWithSymbol(historicalEntityUsd.wage, historicalEntityUsd.currencyCode ?: "INR")
        assertEquals("$100.00", formattedEntityUsd)

        val formattedUserInr = AmountFormatter.formatWithSymbol(historicalEntityUsd.wage, "INR")
        assertEquals("₹100.00", formattedUserInr)

        assertNotEquals(formattedEntityUsd, formattedUserInr)
    }
}
