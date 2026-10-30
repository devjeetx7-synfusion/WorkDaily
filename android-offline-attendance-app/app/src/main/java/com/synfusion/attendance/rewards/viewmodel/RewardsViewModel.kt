package com.synfusion.attendance.rewards.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.rewards.audio.RewardsAudioSettings
import com.synfusion.attendance.rewards.audio.RewardsSoundEvent
import com.synfusion.attendance.rewards.audio.RewardsSoundManager
import com.synfusion.attendance.rewards.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RewardsViewModel(
    application: Application,
    private val repository: FirebaseRewardsRepository = FirebaseRewardsRepository()
) : AndroidViewModel(application) {

    private val soundManager = RewardsSoundManager.getInstance(application)

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

    private val _audioSettings = MutableStateFlow(soundManager.settings)
    val audioSettings: StateFlow<RewardsAudioSettings> = _audioSettings.asStateFlow()

    private val _leaderboardTimeframe = MutableStateFlow(LeaderboardTimeframe.WEEKLY)
    val leaderboardTimeframe: StateFlow<LeaderboardTimeframe> = _leaderboardTimeframe.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<RewardTransaction?>(null)
    val selectedTransaction: StateFlow<RewardTransaction?> = _selectedTransaction.asStateFlow()

    private val _selectedTask = MutableStateFlow<RewardTask?>(null)
    val selectedTask: StateFlow<RewardTask?> = _selectedTask.asStateFlow()

    private val _selectedAchievement = MutableStateFlow<Achievement?>(null)
    val selectedAchievement: StateFlow<Achievement?> = _selectedAchievement.asStateFlow()

    fun updateAudioSettings(newSettings: RewardsAudioSettings) {
        soundManager.updateSettings(newSettings)
        _audioSettings.value = newSettings
    }

    fun playSound(event: RewardsSoundEvent) {
        soundManager.playSound(event)
    }

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
        repository.fetchLeaderboard(timeframe)
    }

    fun getLeaderboard(): StateFlow<List<LeaderboardUser>> {
        return if (_leaderboardTimeframe.value == LeaderboardTimeframe.WEEKLY) {
            repository.weeklyLeaderboard
        } else {
            repository.monthlyLeaderboard
        }
    }

    fun claimTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.claimTask(taskId)
            if (success) {
                playSound(RewardsSoundEvent.COIN_REWARD)
            } else {
                playSound(RewardsSoundEvent.ACTION_ERROR)
            }
        }
    }

    fun submitRedemption(option: RedemptionOption, onResult: (RedemptionRecord?) -> Unit = {}): RedemptionRecord? {
        var record: RedemptionRecord? = null
        viewModelScope.launch(Dispatchers.IO) {
            record = repository.submitRedemption(option)
            if (record != null) {
                playSound(RewardsSoundEvent.CONGRATULATIONS)
            } else {
                playSound(RewardsSoundEvent.ACTION_ERROR)
            }
            onResult(record)
        }
        return record
    }

    fun claimDailyCheckIn(onResult: (Boolean) -> Unit = {}): Boolean {
        var success = false
        viewModelScope.launch(Dispatchers.IO) {
            success = repository.claimDailyCheckIn()
            if (success) {
                playSound(RewardsSoundEvent.DAILY_CHECKIN_SUCCESS)
            } else {
                playSound(RewardsSoundEvent.ACTION_ERROR)
            }
            onResult(success)
        }
        return success
    }

    fun claimMilestone(milestoneId: String, onResult: (Boolean) -> Unit = {}): Boolean {
        var success = false
        viewModelScope.launch(Dispatchers.IO) {
            success = repository.claimMilestone(milestoneId)
            if (success) {
                playSound(RewardsSoundEvent.MILESTONE_CLAIM)
            } else {
                playSound(RewardsSoundEvent.ACTION_ERROR)
            }
            onResult(success)
        }
        return success
    }

    fun spinWheel(onResult: (SpinWheelSegment?) -> Unit = {}): SpinWheelSegment? {
        var segment: SpinWheelSegment? = null
        viewModelScope.launch(Dispatchers.IO) {
            segment = repository.spinWheel()
            onResult(segment)
        }
        return segment
    }

    fun submitReferralCode(code: String, onResult: (Result<Boolean>) -> Unit = {}): Result<Boolean> {
        var res = Result.failure<Boolean>(IllegalStateException("Processing"))
        viewModelScope.launch(Dispatchers.IO) {
            res = repository.submitReferralCode(code)
            onResult(res)
        }
        return res
    }

    fun updateDateState(dateStr: String) {
        repository.updateDateState(dateStr)
    }

    fun refreshState() {
        repository.refreshState()
    }
}

class RewardsViewModelFactory(
    private val application: Application,
    private val repository: FirebaseRewardsRepository = FirebaseRewardsRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RewardsViewModel::class.java)) {
            return RewardsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
