package com.synfusion.attendance.rewards

import com.synfusion.attendance.rewards.data.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RewardsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RewardsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RewardsViewModel(DemoRewardsRepository(DemoRewardsDataSource()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialWalletSummary() {
        val summary = viewModel.walletSummary.value
        assertEquals(2000, summary.availableCoins)
        assertEquals(450, summary.pendingCoins)
        assertEquals(2450, summary.totalBalance)
        assertEquals(7850, summary.lifetimeEarned)
    }

    @Test
    fun testLeaderboardTimeframeSwitch() {
        assertEquals(LeaderboardTimeframe.WEEKLY, viewModel.leaderboardTimeframe.value)

        viewModel.setLeaderboardTimeframe(LeaderboardTimeframe.MONTHLY)
        assertEquals(LeaderboardTimeframe.MONTHLY, viewModel.leaderboardTimeframe.value)

        val monthlyUsers = viewModel.getLeaderboard().value
        assertTrue(monthlyUsers.isNotEmpty())
        assertEquals(1, monthlyUsers.first().rank)
        assertEquals("Rahul", monthlyUsers.first().name)
    }

    @Test
    fun testClaimTaskDemo() {
        val taskToClaim = viewModel.tasks.value.find { it.status == TaskStatus.CLAIMABLE }
        assertNotNull(taskToClaim)

        viewModel.claimTask(taskToClaim!!.id)

        val updatedTask = viewModel.tasks.value.find { it.id == taskToClaim.id }
        assertNotNull(updatedTask)
        assertEquals(TaskStatus.CLAIMED, updatedTask!!.status)
    }

    @Test
    fun testDemoRedemption() {
        val option = viewModel.redemptionOptions.value.first()
        val record = viewModel.submitRedemption(option)

        assertNotNull(record)
        assertEquals(option.title, record.optionTitle)
        assertEquals(RedemptionStatus.PENDING, record.status)
        assertTrue(viewModel.redemptionHistory.value.contains(record))
    }
}
