package com.synfusion.workdailyadmin

import com.synfusion.workdailyadmin.data.AdminErrorType
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.DashboardStats
import com.synfusion.workdailyadmin.ui.dashboard.DashboardUiState
import com.synfusion.workdailyadmin.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAdminRepository(
        var returnResult: Result<DashboardStats>
    ) : AdminRepository({ throw IllegalStateException("Not used") }, { null }) {
        var callCount = 0
            private set

        override suspend fun getDashboardStats(): Result<DashboardStats> {
            callCount++
            return returnResult
        }
    }

    @Test
    fun testSuccessfulDashboardStatsLoad() = runTest {
        val stats = DashboardStats(
            totalUsers = 42,
            suspendedUsers = 1,
            bannedUsers = 0,
            verifiedUsers = 15,
            activeLiveRooms = 3,
            pendingReports = 2,
            totalCoinsIssued = 5000,
            totalCoinsSpent = 1200,
            pendingRedemptionsCount = 1,
            systemStatus = "OPERATIONAL"
        )
        val fakeRepo = FakeAdminRepository(Result.Success(stats))
        val viewModel = DashboardViewModel(fakeRepo)

        assertTrue(viewModel.uiState.value is DashboardUiState.Loading)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertEquals(42, success.stats.totalUsers)
        assertEquals(3, success.stats.activeLiveRooms)
        assertFalse(success.isFallback)
        assertEquals(1, fakeRepo.callCount)
    }

    @Test
    fun testErrorDashboardStatsLoad() = runTest {
        val fakeRepo = FakeAdminRepository(
            Result.Error(
                message = "Network unavailable",
                errorType = AdminErrorType.NETWORK_UNAVAILABLE,
                canRetry = true
            )
        )
        val viewModel = DashboardViewModel(fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        val error = state as DashboardUiState.Error
        assertEquals("Network unavailable", error.message)
        assertEquals(AdminErrorType.NETWORK_UNAVAILABLE, error.errorType)
        assertTrue(error.canRetry)
    }

    @Test
    fun testRetryNetworkSyncLoadsSuccess() = runTest {
        val fakeRepo = FakeAdminRepository(
            Result.Error(
                message = "Network unavailable",
                errorType = AdminErrorType.NETWORK_UNAVAILABLE,
                canRetry = true
            )
        )
        val viewModel = DashboardViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DashboardUiState.Error)

        val updatedStats = DashboardStats(
            totalUsers = 10,
            suspendedUsers = 0,
            bannedUsers = 0,
            verifiedUsers = 5,
            activeLiveRooms = 0,
            pendingReports = 0,
            totalCoinsIssued = 100,
            totalCoinsSpent = 0,
            pendingRedemptionsCount = 0,
            systemStatus = "OPERATIONAL"
        )
        fakeRepo.returnResult = Result.Success(updatedStats, isFallback = true)

        viewModel.loadDashboardStats(force = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertEquals(10, success.stats.totalUsers)
        assertTrue(success.isFallback)
        assertEquals(2, fakeRepo.callCount)
    }
}
