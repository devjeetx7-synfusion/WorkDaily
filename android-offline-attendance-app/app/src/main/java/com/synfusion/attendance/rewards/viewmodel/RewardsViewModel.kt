package com.synfusion.attendance.rewards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synfusion.attendance.rewards.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RewardsViewModel(
    private val repository: DemoRewardsRepository = DemoRewardsRepository()
) : ViewModel() {

    val walletSummary: StateFlow<WalletSummary> = repository.walletSummary
    val referralStats: StateFlow<ReferralStats> = repository.referralStats
    val transactions: StateFlow<List<RewardTransaction>> = repository.transactions
    val referrals: StateFlow<List<ReferralItem>> = repository.referrals
    val tasks: StateFlow<List<RewardTask>> = repository.tasks
    val achievements: StateFlow<List<Achievement>> = repository.achievements
    val redemptionOptions: StateFlow<List<RedemptionOption>> = repository.redemptionOptions
    val redemptionHistory: StateFlow<List<RedemptionRecord>> = repository.redemptionHistory

    val spinTokens: StateFlow<SpinTokenBalance> = repository.spinTokens
    val dailyCheckInState: StateFlow<DailyCheckInState> = repository.dailyCheckInState
    val streakState: StateFlow<DailyStreakState> = repository.streakState
    val milestones: StateFlow<List<MilestoneReward>> = repository.milestones
    val spinSegments: List<SpinWheelSegment> get() = repository.spinSegments

    private val _leaderboardTimeframe = MutableStateFlow(LeaderboardTimeframe.WEEKLY)
    val leaderboardTimeframe: StateFlow<LeaderboardTimeframe> = _leaderboardTimeframe.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<RewardTransaction?>(null)
    val selectedTransaction: StateFlow<RewardTransaction?> = _selectedTransaction.asStateFlow()

    private val _selectedTask = MutableStateFlow<RewardTask?>(null)
    val selectedTask: StateFlow<RewardTask?> = _selectedTask.asStateFlow()

    private val _selectedAchievement = MutableStateFlow<Achievement?>(null)
    val selectedAchievement: StateFlow<Achievement?> = _selectedAchievement.asStateFlow()

    fun selectTransaction(transaction: RewardTransaction?) {
        _selectedTransaction.value = transaction
    }

    fun selectTask(task: RewardTask?) {
        _selectedTask.value = task
    }

    fun selectAchievement(achievement: Achievement?) {
        _selectedAchievement.value = achievement
    }

    fun setLeaderboardTimeframe(timeframe: LeaderboardTimeframe) {
        _leaderboardTimeframe.value = timeframe
    }

    fun getLeaderboard(): StateFlow<List<LeaderboardUser>> {
        return if (_leaderboardTimeframe.value == LeaderboardTimeframe.WEEKLY) {
            repository.weeklyLeaderboard
        } else {
            repository.monthlyLeaderboard
        }
    }

    fun claimTask(taskId: String) {
        repository.claimTaskDemo(taskId)
    }

    fun submitRedemption(option: RedemptionOption): RedemptionRecord {
        return repository.submitDemoRedemption(option)
    }

    fun claimDailyCheckIn(): Boolean {
        return repository.claimDailyCheckIn()
    }

    fun claimMilestone(milestoneId: String): Boolean {
        return repository.claimMilestone(milestoneId)
    }

    fun spinWheel(): SpinWheelSegment? {
        return repository.spinWheel()
    }

    fun updateDateState(dateStr: String) {
        repository.updateDateState(dateStr)
    }
}

class RewardsViewModelFactory(
    private val repository: DemoRewardsRepository = DemoRewardsRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RewardsViewModel::class.java)) {
            return RewardsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
