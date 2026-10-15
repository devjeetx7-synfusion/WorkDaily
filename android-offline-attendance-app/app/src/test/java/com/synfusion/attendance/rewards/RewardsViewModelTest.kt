package com.synfusion.attendance.rewards

import com.synfusion.attendance.rewards.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RewardsViewModelTest {

    private lateinit var repository: DemoRewardsRepository

    @Before
    fun setUp() {
        val dataSource = DemoRewardsDataSource(context = null, randomSupplier = { 0.5 })
        repository = DemoRewardsRepository(context = null, dataSource = dataSource)
    }

    @Test
    fun testInitialWalletSummary() {
        val summary = repository.walletSummary.value
        assertEquals(2000, summary.availableCoins)
        assertEquals(450, summary.pendingCoins)
        assertEquals(2450, summary.totalBalance)
        assertEquals(7850, summary.lifetimeEarned)
    }

    @Test
    fun testLeaderboardTimeframeData() {
        val weeklyUsers = repository.weeklyLeaderboard.value
        assertTrue(weeklyUsers.isNotEmpty())
        assertEquals(1, weeklyUsers.first().rank)
        assertEquals("Rahul", weeklyUsers.first().name)

        val monthlyUsers = repository.monthlyLeaderboard.value
        assertTrue(monthlyUsers.isNotEmpty())
        assertEquals(1, monthlyUsers.first().rank)
        assertEquals("Rahul", monthlyUsers.first().name)
    }

    @Test
    fun testClaimTaskDemo() {
        val taskToClaim = repository.tasks.value.find { it.status == TaskStatus.CLAIMABLE }
        assertNotNull(taskToClaim)

        repository.claimTaskDemo(taskToClaim!!.id)

        val updatedTask = repository.tasks.value.find { it.id == taskToClaim.id }
        assertNotNull(updatedTask)
        assertEquals(TaskStatus.CLAIMED, updatedTask!!.status)
    }

    @Test
    fun testDemoRedemption() {
        val option = repository.redemptionOptions.value.first()
        val record = repository.submitDemoRedemption(option)

        assertNotNull(record)
        assertEquals(option.title, record.optionTitle)
        assertEquals(RedemptionStatus.PENDING, record.status)
        assertTrue(repository.redemptionHistory.value.contains(record))
    }
}
