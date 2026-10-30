package com.synfusion.attendance.rewards

import com.synfusion.attendance.rewards.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DailyCheckInAndSpinTest {

    @Test
    fun testSpinWheelSegments_has8ConstructionSectors() {
        val repository = FirebaseRewardsRepository(
            authSupplier = { throw IllegalStateException("Unit test auth uninitialized") },
            functionsSupplier = { throw IllegalStateException("Unit test functions uninitialized") }
        )

        val segments = repository.spinSegments
        assertEquals(8, segments.size)
        assertEquals("Safety Helmet", segments[0].name)
        assertEquals("Foreman's Pass", segments[7].name)
    }

    @Test
    fun testRedemptionOptions_containsValidOptions() {
        val repository = FirebaseRewardsRepository(
            authSupplier = { throw IllegalStateException("Unit test auth uninitialized") },
            functionsSupplier = { throw IllegalStateException("Unit test functions uninitialized") }
        )

        val options = repository.redemptionOptions.value
        assertEquals(3, options.size)
        assertEquals("rdm_opt_1", options[0].id)
        assertEquals(500, options[0].coinsCost)
    }
}
