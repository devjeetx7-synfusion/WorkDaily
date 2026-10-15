package com.synfusion.attendance.rewards

import com.synfusion.attendance.rewards.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DailyCheckInAndSpinTest {

    private lateinit var dataSource: DemoRewardsDataSource

    @Before
    fun setUp() {
        // Deterministic random supplier returning fixed value 0.5 (4th sector out of 8)
        dataSource = DemoRewardsDataSource(randomSupplier = { 0.5 })
    }

    @Test
    fun testFirstDailyCheckIn_claimsSuccessfully() {
        val todayStr = "2026-10-15"
        dataSource.updateDateState(todayStr)

        val success = dataSource.claimDailyCheckIn(todayStr)
        assertTrue(success)

        val checkInState = dataSource.dailyCheckInState.value
        assertTrue(checkInState.isTodayClaimed)
        assertEquals(DailyCheckInStatus.CLAIMED, checkInState.status)

        // Wallet should increase by +5 coins
        assertEquals(2005, dataSource.walletSummary.value.availableCoins)

        // Spin tokens should increase by +1 (initial 2 + 1 = 3)
        assertEquals(3, dataSource.spinTokens.value.availableSpins)

        // Streak should be 1 day
        assertEquals(1, dataSource.streakState.value.currentStreak)
        assertEquals(1, dataSource.streakState.value.totalCheckInDays)
    }

    @Test
    fun testDuplicateDailyCheckIn_rejectedOnSameDay() {
        val todayStr = "2026-10-15"
        dataSource.updateDateState(todayStr)

        val firstClaim = dataSource.claimDailyCheckIn(todayStr)
        assertTrue(firstClaim)

        val secondClaim = dataSource.claimDailyCheckIn(todayStr)
        assertFalse(secondClaim)

        // Wallet balance should remain 2005 (not increased twice)
        assertEquals(2005, dataSource.walletSummary.value.availableCoins)
    }

    @Test
    fun testConsecutiveCheckIns_increasesStreak() {
        dataSource.claimDailyCheckIn("2026-10-10")
        dataSource.claimDailyCheckIn("2026-10-11")
        dataSource.claimDailyCheckIn("2026-10-12")
        dataSource.claimDailyCheckIn("2026-10-13")
        dataSource.claimDailyCheckIn("2026-10-14")
        dataSource.claimDailyCheckIn("2026-10-15")
        dataSource.claimDailyCheckIn("2026-10-16")

        dataSource.updateDateState("2026-10-16")

        val streakState = dataSource.streakState.value
        assertEquals(7, streakState.currentStreak)
        assertEquals(7, streakState.longestStreak)
        assertEquals(7, streakState.totalCheckInDays)

        // 7-day streak milestone should now be claimable
        val milestones = dataSource.milestones.value
        val m7 = milestones.find { it.type == MilestoneType.SEVEN_DAY_STREAK }
        assertNotNull(m7)
        assertEquals(MilestoneStatus.CLAIMABLE, m7?.status)
    }

    @Test
    fun testMissedDay_resetsCurrentStreak_butPreservesLongestStreakAndTotalDays() {
        dataSource.claimDailyCheckIn("2026-10-10")
        dataSource.claimDailyCheckIn("2026-10-11")
        dataSource.claimDailyCheckIn("2026-10-12")

        // Missed 13th and 14th, check-in on 15th
        dataSource.claimDailyCheckIn("2026-10-15")
        dataSource.updateDateState("2026-10-15")

        val streakState = dataSource.streakState.value
        assertEquals(1, streakState.currentStreak)
        assertEquals(3, streakState.longestStreak)
        assertEquals(4, streakState.totalCheckInDays)
    }

    @Test
    fun testMilestoneClaim_creditsCoinsAndSpins() {
        // Claim 7 consecutive days
        for (i in 1..7) {
            val date = String.format("2026-10-%02d", i)
            dataSource.claimDailyCheckIn(date)
        }
        dataSource.updateDateState("2026-10-07")

        val milestones = dataSource.milestones.value
        val m7 = milestones.find { it.type == MilestoneType.SEVEN_DAY_STREAK }
        assertNotNull(m7)

        val coinsBefore = dataSource.walletSummary.value.availableCoins
        val spinsBefore = dataSource.spinTokens.value.availableSpins

        val claimSuccess = dataSource.claimMilestone(m7!!.id)
        assertTrue(claimSuccess)

        // Wallet should gain +20 milestone bonus
        assertEquals(coinsBefore + 20, dataSource.walletSummary.value.availableCoins)

        // Spin tokens should gain +1 bonus spin
        assertEquals(spinsBefore + 1, dataSource.spinTokens.value.availableSpins)

        // Milestone status should now be CLAIMED
        val updatedM7 = dataSource.milestones.value.find { it.id == m7.id }
        assertEquals(MilestoneStatus.CLAIMED, updatedM7?.status)
    }

    @Test
    fun testSpinWheel_consumesTokenAndCreditsSectorReward() {
        val spinsBefore = dataSource.spinTokens.value.availableSpins
        val coinsBefore = dataSource.walletSummary.value.availableCoins

        val wonSegment = dataSource.spinWheel()
        assertNotNull(wonSegment)

        // Token deducted
        assertEquals(spinsBefore - 1, dataSource.spinTokens.value.availableSpins)

        // Won sector reward added to wallet (fixed random 0.5 -> index 4: Cement Truck with 6 coins)
        assertEquals(6, wonSegment!!.coinReward)
        assertEquals(coinsBefore + 6, dataSource.walletSummary.value.availableCoins)
    }

    @Test
    fun testSpinWheel_whenNoSpinsAvailable_returnsNull() {
        // Consume all available spins
        val initialSpins = dataSource.spinTokens.value.availableSpins
        for (i in 0 until initialSpins) {
            dataSource.spinWheel()
        }

        assertEquals(0, dataSource.spinTokens.value.availableSpins)

        // Attempt extra spin
        val wonSegment = dataSource.spinWheel()
        assertNull(wonSegment)
    }
}
