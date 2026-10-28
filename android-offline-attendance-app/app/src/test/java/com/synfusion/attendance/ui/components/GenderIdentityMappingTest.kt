package com.synfusion.attendance.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the mapping from a persisted gender string to a [GenderIdentity].
 *
 * Only the pure mapping is tested here; `genderIconFor` and `genderColor` need the Compose runtime
 * and are not unit-testable without Robolectric, which this project does not use.
 *
 * The mapping is the part that carries the two hard requirements: a user's stored gender value is
 * never reinterpreted, and no gender is ever assigned when there is none. Both are asserted below,
 * because the screen this replaced defaulted every unrecognised value -- including blank and
 * transgender -- to the male symbol.
 */
class GenderIdentityMappingTest {

    // ----------------------------------------------------------------------------------------
    // Persisted values round trip unchanged
    // ----------------------------------------------------------------------------------------

    @Test
    fun testStoredValuesAreTheExactStringsTheAppPersists() {
        // These three strings are what UserEntity.gender, PublicProfile.gender and the settings
        // selector already store. Changing them would silently reassign existing users.
        assertEquals("Male", GenderIdentity.MALE.storedValue)
        assertEquals("Female", GenderIdentity.FEMALE.storedValue)
        assertEquals("Transgender", GenderIdentity.TRANSGENDER.storedValue)
    }

    @Test
    fun testEveryStoredValueMapsBackToItsOwnIdentity() {
        GenderIdentity.values().forEach { identity ->
            assertEquals(identity, genderIdentityFrom(identity.storedValue))
        }
    }

    // ----------------------------------------------------------------------------------------
    // Case and whitespace tolerance
    // ----------------------------------------------------------------------------------------

    @Test
    fun testMappingToleratesCaseAndSurroundingWhitespace() {
        assertEquals(GenderIdentity.MALE, genderIdentityFrom("male"))
        assertEquals(GenderIdentity.MALE, genderIdentityFrom("  Male  "))
        assertEquals(GenderIdentity.FEMALE, genderIdentityFrom("FEMALE"))
        assertEquals(GenderIdentity.TRANSGENDER, genderIdentityFrom("transgender"))
        assertEquals(GenderIdentity.TRANSGENDER, genderIdentityFrom(" TransGender "))
    }

    @Test
    fun testLegacyShortFormsAreRecognised() {
        assertEquals(GenderIdentity.MALE, genderIdentityFrom("M"))
        assertEquals(GenderIdentity.FEMALE, genderIdentityFrom("f"))
        assertEquals(GenderIdentity.TRANSGENDER, genderIdentityFrom("t"))
        assertEquals(GenderIdentity.TRANSGENDER, genderIdentityFrom("Trans"))
    }

    // ----------------------------------------------------------------------------------------
    // No substring trap
    // ----------------------------------------------------------------------------------------

    @Test
    fun testFemaleIsNotMatchedByTheMaleBranch() {
        // "female" contains "male". A contains-based or startsWith-based mapping would classify
        // it as male, so the exact-match contract is asserted explicitly.
        assertEquals(GenderIdentity.FEMALE, genderIdentityFrom("female"))
        assertEquals(GenderIdentity.FEMALE, genderIdentityFrom("Female"))
    }

    // ----------------------------------------------------------------------------------------
    // Never assign a gender that was not chosen
    // ----------------------------------------------------------------------------------------

    @Test
    fun testMissingValueMapsToNullNotToMale() {
        // PublicProfile.gender defaults to "" and UserEntity.gender to "Male"; the display layer
        // must not turn an absent value into a statement about the user.
        assertNull(genderIdentityFrom(null))
        assertNull(genderIdentityFrom(""))
        assertNull(genderIdentityFrom("   "))
    }

    @Test
    fun testUnrecognisedValuesMapToNullRatherThanBeingCoerced() {
        // A value this app does not model must render as no symbol. Coercing "Other" into one of
        // the three would misreport the user's gender, which is worse than showing nothing.
        assertNull(genderIdentityFrom("Other"))
        assertNull(genderIdentityFrom("Prefer not to say"))
        assertNull(genderIdentityFrom("Non-binary"))
        assertNull(genderIdentityFrom("unknown"))
        assertNull(genderIdentityFrom("1"))
    }

    @Test
    fun testAllThreeIdentitiesAreDistinct() {
        assertEquals(3, GenderIdentity.values().size)
        assertEquals(
            3,
            GenderIdentity.values().map { it.storedValue }.distinct().size
        )
    }
}
