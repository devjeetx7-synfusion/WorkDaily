package com.synfusion.attendance.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Covers the canonical date-of-birth rules shared by the picker, the validator and the profile.
 *
 * Every date-relative assertion pins [LocalDate] explicitly instead of reading the clock, so the
 * suite cannot start failing on someone's birthday or at a year boundary. The same parameter is
 * what makes "age updates as time passes" testable at all.
 */
class DateOfBirthUtilsTest {

    // ----------------------------------------------------------------------------------------
    // Parsing and validation
    // ----------------------------------------------------------------------------------------

    @Test
    fun testParseAcceptsCanonicalDate() {
        assertEquals(LocalDate.of(1994, 5, 21), DateOfBirthUtils.parse("1994-05-21"))
    }

    @Test
    fun testParseRejectsMalformedAndMissingValues() {
        assertNull(DateOfBirthUtils.parse(null))
        assertNull(DateOfBirthUtils.parse(""))
        assertNull(DateOfBirthUtils.parse("   "))
        assertNull(DateOfBirthUtils.parse("1994/05/21"))
        assertNull(DateOfBirthUtils.parse("1994-05"))
        assertNull(DateOfBirthUtils.parse("not-a-date"))
    }

    @Test
    fun testParseRejectsImpossibleCalendarDates() {
        assertNull(DateOfBirthUtils.parse("1994-02-30"))
        assertNull(DateOfBirthUtils.parse("1994-13-01"))
        assertNull(DateOfBirthUtils.parse("1994-00-10"))
    }

    @Test
    fun testValidateRejectsFutureDates() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        assertEquals(
            "Date of birth cannot be in the future",
            DateOfBirthUtils.validate(tomorrow)
        )
    }

    @Test
    fun testValidateAllowsIncompleteInputSoTypingIsNeverBlocked() {
        // A partially entered value is not an error; only a complete-but-impossible one is.
        assertNull(DateOfBirthUtils.validate(null))
        assertNull(DateOfBirthUtils.validate(""))
        assertNull(DateOfBirthUtils.validate("1994"))
        assertNull(DateOfBirthUtils.validate("1994-05"))
    }

    @Test
    fun testIsValidRequiresACompleteRealDate() {
        assertTrue(DateOfBirthUtils.isValid("1994-05-21"))
        assertFalse(DateOfBirthUtils.isValid("1994-5-21"))
        assertFalse(DateOfBirthUtils.isValid("1994-02-30"))
        assertFalse(DateOfBirthUtils.isValid(null))
    }

    // ----------------------------------------------------------------------------------------
    // Leap years
    // ----------------------------------------------------------------------------------------

    @Test
    fun testDaysInMonthHonoursLeapYears() {
        assertEquals(29, DateOfBirthUtils.daysInMonth(2024, 2)) // divisible by 4
        assertEquals(28, DateOfBirthUtils.daysInMonth(2023, 2))
        assertEquals(28, DateOfBirthUtils.daysInMonth(1900, 2)) // divisible by 100, not 400
        assertEquals(29, DateOfBirthUtils.daysInMonth(2000, 2)) // divisible by 400
    }

    @Test
    fun testDaysInMonthMatchesCalendarLengths() {
        assertEquals(31, DateOfBirthUtils.daysInMonth(2023, 1))
        assertEquals(30, DateOfBirthUtils.daysInMonth(2023, 4))
        assertEquals(31, DateOfBirthUtils.daysInMonth(2023, 12))
    }

    @Test
    fun testDaysInMonthIsZeroForAnImpossibleMonth() {
        assertEquals(0, DateOfBirthUtils.daysInMonth(2023, 13))
    }

    @Test
    fun testLeapDayIsAValidDateOfBirth() {
        assertTrue(DateOfBirthUtils.isValid("2024-02-29"))
        assertFalse(DateOfBirthUtils.isValid("2023-02-29"))
    }

    // ----------------------------------------------------------------------------------------
    // Age derivation
    // ----------------------------------------------------------------------------------------

    @Test
    fun testAgeInYearsUsesTheSuppliedCurrentDate() {
        val dob = "2000-06-15"
        assertEquals(25, DateOfBirthUtils.ageInYears(dob, LocalDate.of(2026, 6, 14)))
        assertEquals(26, DateOfBirthUtils.ageInYears(dob, LocalDate.of(2026, 6, 15)))
        assertEquals(26, DateOfBirthUtils.ageInYears(dob, LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun testAgeIsNullRatherThanZeroForUnusableInput() {
        // Returning 0 would be indistinguishable from a newborn, which is why the helper is
        // nullable. This is the specific defect the removed private calculateAge had.
        assertNull(DateOfBirthUtils.ageInYears(null))
        assertNull(DateOfBirthUtils.ageInYears(""))
        assertNull(DateOfBirthUtils.ageInYears("garbage"))
        assertNull(DateOfBirthUtils.ageInYears("1994-02-30"))
    }

    @Test
    fun testAgeIsNullForAFutureDateOfBirth() {
        assertNull(
            DateOfBirthUtils.ageInYears(
                LocalDate.now().plusYears(1).toString(),
                LocalDate.now()
            )
        )
    }

    @Test
    fun testAgeLabelIsHumanReadableAndSingularAtOne() {
        assertEquals("22 years old", DateOfBirthUtils.ageLabel("2004-01-10", LocalDate.of(2026, 6, 1)))
        assertEquals("1 year old", DateOfBirthUtils.ageLabel("2025-01-10", LocalDate.of(2026, 6, 1)))
        assertEquals("0 years old", DateOfBirthUtils.ageLabel("2026-01-10", LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun testAgeLabelIsNullWhenThereIsNoDateOfBirth() {
        assertNull(DateOfBirthUtils.ageLabel(null))
        assertNull(DateOfBirthUtils.ageLabel(""))
        assertNull(DateOfBirthUtils.ageLabel("1994-02-30"))
    }

    // ----------------------------------------------------------------------------------------
    // Storage round trip
    // ----------------------------------------------------------------------------------------

    @Test
    fun testEpochMillisRoundTripPreservesTheCalendarDate() {
        val millis = DateOfBirthUtils.toEpochMillis("1994-05-21")
        assertNotNull(millis)
        assertEquals("1994-05-21", DateOfBirthUtils.fromEpochMillis(millis))
    }

    @Test
    fun testToEpochMillisRefusesInvalidInput() {
        // Bad input must never be persisted, otherwise a malformed date reaches the Room column.
        assertNull(DateOfBirthUtils.toEpochMillis(null))
        assertNull(DateOfBirthUtils.toEpochMillis("1994-05"))
        assertNull(DateOfBirthUtils.toEpochMillis("1994-02-30"))
        assertNull(DateOfBirthUtils.toEpochMillis(LocalDate.now().plusDays(1).toString()))
    }

    @Test
    fun testFromEpochMillisIsNullForMissingValue() {
        assertNull(DateOfBirthUtils.fromEpochMillis(null))
    }

    // ----------------------------------------------------------------------------------------
    // Picker bounds
    // ----------------------------------------------------------------------------------------

    @Test
    fun testSelectableYearsNeverOffersAFutureYear() {
        val now = LocalDate.of(2026, 6, 1)
        val years = DateOfBirthUtils.selectableYears(now)
        assertEquals(2026, years.first())
        assertTrue("no offered year may be in the future", years.none { it > now.year })
    }

    @Test
    fun testSelectableYearsIsNewestFirstAndBounded() {
        val years = DateOfBirthUtils.selectableYears(LocalDate.of(2026, 6, 1))
        assertEquals(years.sortedDescending(), years)
        // MAX_AGE_YEARS is a plausibility guard, so the list is inclusive of both ends.
        assertEquals(121, years.size)
        assertEquals(1906, years.last())
    }

    @Test
    fun testFormatInputIsIdempotentAndDigitsOnly() {
        assertEquals("1994-05-21", DateOfBirthUtils.formatInput("19940521"))
        assertEquals("1994-05-21", DateOfBirthUtils.formatInput("1994-05-21"))
        assertEquals("1994-05-21", DateOfBirthUtils.formatInput("19a94/05?21"))
        assertEquals("", DateOfBirthUtils.formatInput(""))
    }
}
