package com.synfusion.attendance.rewards

import com.synfusion.attendance.rewards.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RewardsViewModelTest {

    private lateinit var repository: FirebaseRewardsRepository

    @Before
    fun setUp() {
        repository = FirebaseRewardsRepository(
            authSupplier = { throw IllegalStateException("Unit test auth uninitialized") },
            functionsSupplier = { throw IllegalStateException("Unit test functions uninitialized") }
        )
    }

    @Test
    fun testInitialWalletSummary() {
        val summary = repository.walletSummary.value
        assertEquals(0, summary.availableCoins)
        assertEquals(0, summary.pendingCoins)
        assertEquals(0, summary.totalBalance)
        assertEquals(0, summary.lifetimeEarned)
    }

    @Test
    fun testInitialSpinTokens() {
        val spinBalance = repository.spinTokens.value
        assertEquals(0, spinBalance.availableSpins)
    }
}
