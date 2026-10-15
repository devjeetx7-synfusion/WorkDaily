package com.synfusion.attendance.rewards.data

import kotlinx.coroutines.flow.StateFlow

class DemoRewardsRepository(
    private val dataSource: DemoRewardsDataSource = DemoRewardsDataSource()
) {
    val walletSummary: StateFlow<WalletSummary> = dataSource.walletSummary
    val spinTokens: StateFlow<SpinTokenBalance> = dataSource.spinTokens
    val dailyCheckInState: StateFlow<DailyCheckInState> = dataSource.dailyCheckInState
    val streakState: StateFlow<DailyStreakState> = dataSource.streakState
    val milestones: StateFlow<List<MilestoneReward>> = dataSource.milestones

    val spinSegments: List<SpinWheelSegment> get() = dataSource.spinSegments

    fun claimDailyCheckIn(dateStr: String? = null): Boolean {
        return if (dateStr != null) {
            dataSource.claimDailyCheckIn(dateStr)
        } else {
            dataSource.claimDailyCheckIn()
        }
    }

    fun claimMilestone(milestoneId: String): Boolean {
        return dataSource.claimMilestone(milestoneId)
    }

    fun spinWheel(): SpinWheelSegment? {
        return dataSource.spinWheel()
    }

    fun updateDateState(currentDateStr: String) {
        dataSource.updateDateState(currentDateStr)
    }
    val referralStats: StateFlow<ReferralStats> = dataSource.referralStats
    val transactions: StateFlow<List<RewardTransaction>> = dataSource.transactions
    val referrals: StateFlow<List<ReferralItem>> = dataSource.referrals
    val tasks: StateFlow<List<RewardTask>> = dataSource.tasks
    val achievements: StateFlow<List<Achievement>> = dataSource.achievements
    val weeklyLeaderboard: StateFlow<List<LeaderboardUser>> = dataSource.weeklyLeaderboard
    val monthlyLeaderboard: StateFlow<List<LeaderboardUser>> = dataSource.monthlyLeaderboard
    val redemptionOptions: StateFlow<List<RedemptionOption>> = dataSource.redemptionOptions
    val redemptionHistory: StateFlow<List<RedemptionRecord>> = dataSource.redemptionHistory

    fun claimTaskDemo(taskId: String) {
        dataSource.claimTask(taskId)
    }

    fun submitDemoRedemption(option: RedemptionOption): RedemptionRecord {
        return dataSource.addDemoRedemptionRecord(option)
    }
}
