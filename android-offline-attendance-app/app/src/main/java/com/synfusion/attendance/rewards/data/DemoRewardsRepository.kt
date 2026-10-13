package com.synfusion.attendance.rewards.data

import kotlinx.coroutines.flow.StateFlow

class DemoRewardsRepository(
    private val dataSource: DemoRewardsDataSource = DemoRewardsDataSource()
) {
    val walletSummary: StateFlow<WalletSummary> = dataSource.walletSummary
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
