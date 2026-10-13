package com.synfusion.attendance.rewards.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DemoRewardsDataSource {

    private val _walletSummary = MutableStateFlow(WalletSummary())
    val walletSummary: StateFlow<WalletSummary> = _walletSummary.asStateFlow()

    private val _referralStats = MutableStateFlow(ReferralStats())
    val referralStats: StateFlow<ReferralStats> = _referralStats.asStateFlow()

    private val _transactions = MutableStateFlow(
        listOf(
            RewardTransaction(
                id = "tx_101",
                title = "Referral Bonus",
                description = "Invited friend Ramesh joined WorkDaily",
                timestamp = "Today, 10:30 AM",
                amount = 100,
                status = RewardTransactionStatus.CONFIRMED,
                type = RewardTransactionType.EARNED,
                referenceCode = "REF-88912",
                source = "Referral Program"
            ),
            RewardTransaction(
                id = "tx_102",
                title = "Weekly Task Reward",
                description = "Completed 'Stay Consistent' 5-day attendance task",
                timestamp = "Yesterday, 06:15 PM",
                amount = 25,
                status = RewardTransactionStatus.CONFIRMED,
                type = RewardTransactionType.EARNED,
                referenceCode = "TSK-44210",
                source = "Tasks"
            ),
            RewardTransaction(
                id = "tx_103",
                title = "Achievement Milestone",
                description = "Unlocked 'Work Pro' milestone badge",
                timestamp = "12 Oct 2026, 02:45 PM",
                amount = 50,
                status = RewardTransactionStatus.PENDING,
                type = RewardTransactionType.PENDING,
                referenceCode = "ACH-90021",
                source = "Achievements"
            ),
            RewardTransaction(
                id = "tx_104",
                title = "Gift Card Redemption",
                description = "₹50 Shopping Voucher Redemption",
                timestamp = "10 Oct 2026, 11:20 AM",
                amount = -50,
                status = RewardTransactionStatus.REVERSED,
                type = RewardTransactionType.SPENT,
                referenceCode = "RDM-10023",
                source = "Redemption Center"
            ),
            RewardTransaction(
                id = "tx_105",
                title = "Daily Check-in",
                description = "Logged in and marked daily attendance",
                timestamp = "09 Oct 2026, 09:00 AM",
                amount = 5,
                status = RewardTransactionStatus.COMPLETED,
                type = RewardTransactionType.EARNED,
                referenceCode = "CHK-11002",
                source = "Daily Check-in"
            )
        )
    )
    val transactions: StateFlow<List<RewardTransaction>> = _transactions.asStateFlow()

    private val _referrals = MutableStateFlow(
        listOf(
            ReferralItem("ref_1", "Ramesh Kumar", ReferralStatus.REWARDED, 100, "Today, 10:30 AM"),
            ReferralItem("ref_2", "Suresh Verma", ReferralStatus.PENDING, 100, "Yesterday"),
            ReferralItem("ref_3", "Pankaj Sharma", ReferralStatus.REWARDED, 100, "11 Oct 2026"),
            ReferralItem("ref_4", "Amit Singh", ReferralStatus.NOT_QUALIFIED, 0, "08 Oct 2026"),
            ReferralItem("ref_5", "Deepak Gupta", ReferralStatus.REWARDED, 100, "05 Oct 2026"),
            ReferralItem("ref_6", "Vikram Patel", ReferralStatus.PENDING, 100, "02 Oct 2026")
        )
    )
    val referrals: StateFlow<List<ReferralItem>> = _referrals.asStateFlow()

    private val _tasks = MutableStateFlow(
        listOf(
            RewardTask(
                id = "task_1",
                title = "Daily Check-in",
                description = "Open WorkDaily and check in daily",
                category = TaskCategory.DAILY,
                currentProgress = 1,
                maxProgress = 1,
                coinReward = 5,
                status = TaskStatus.CLAIMED,
                requirements = listOf("Open app", "Mark attendance or view logs")
            ),
            RewardTask(
                id = "task_2",
                title = "Stay Consistent",
                description = "Mark attendance for 5 consecutive days",
                category = TaskCategory.WEEKLY,
                currentProgress = 4,
                maxProgress = 5,
                coinReward = 25,
                status = TaskStatus.IN_PROGRESS,
                requirements = listOf("Log attendance 5 days this week")
            ),
            RewardTask(
                id = "task_3",
                title = "Add Profile Photo",
                description = "Upload your profile avatar image",
                category = TaskCategory.SPECIAL,
                currentProgress = 0,
                maxProgress = 1,
                coinReward = 20,
                status = TaskStatus.IN_PROGRESS,
                requirements = listOf("Go to Profile", "Upload custom avatar photo")
            ),
            RewardTask(
                id = "task_4",
                title = "Complete Profile",
                description = "Fill in your full name, phone number, and wage details",
                category = TaskCategory.SPECIAL,
                currentProgress = 1,
                maxProgress = 1,
                coinReward = 50,
                status = TaskStatus.CLAIMABLE,
                requirements = listOf("Complete 100% of workspace profile")
            )
        )
    )
    val tasks: StateFlow<List<RewardTask>> = _tasks.asStateFlow()

    private val _achievements = MutableStateFlow(
        listOf(
            Achievement("ach_1", "First Step", "Logged your first attendance entry", AchievementCategory.ATTENDANCE, true, 1, 1, "flag", "01 Sep 2026"),
            Achievement("ach_2", "7 Day Streak", "Logged attendance for 7 consecutive days", AchievementCategory.ATTENDANCE, true, 7, 7, "fire", "15 Sep 2026"),
            Achievement("ach_3", "30 Day Master", "Logged attendance for 30 consecutive days", AchievementCategory.ATTENDANCE, false, 18, 30, "calendar"),
            Achievement("ach_4", "First Report", "Generated and exported your first PDF Passbook", AchievementCategory.MILESTONES, true, 1, 1, "description", "20 Sep 2026"),
            Achievement("ach_5", "Work Pro", "Completed 10 regular shift logs with overtime", AchievementCategory.WORK, false, 8, 10, "badge"),
            Achievement("ach_6", "WorkDaily Legend", "Reach 5,000 lifetime WorkDaily XP points", AchievementCategory.MILESTONES, false, 860, 5000, "crown")
        )
    )
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _weeklyLeaderboard = MutableStateFlow(
        listOf(
            LeaderboardUser("user_1", 1, "Rahul", 4850),
            LeaderboardUser("user_2", 2, "Aman", 4120),
            LeaderboardUser("user_3", 3, "Suraj", 3940),
            LeaderboardUser("user_4", 4, "Vikash", 3620),
            LeaderboardUser("user_5", 5, "Arjun", 3410),
            LeaderboardUser("user_6", 6, "Rohit", 3150),
            LeaderboardUser("user_me", 128, "You (Dharmjeet)", 860, isCurrentUser = true)
        )
    )
    val weeklyLeaderboard: StateFlow<List<LeaderboardUser>> = _weeklyLeaderboard.asStateFlow()

    private val _monthlyLeaderboard = MutableStateFlow(
        listOf(
            LeaderboardUser("user_1", 1, "Rahul", 18450),
            LeaderboardUser("user_2", 2, "Aman", 16200),
            LeaderboardUser("user_3", 3, "Suraj", 15800),
            LeaderboardUser("user_4", 4, "Vikash", 14100),
            LeaderboardUser("user_5", 5, "Arjun", 13500),
            LeaderboardUser("user_6", 6, "Rohit", 12900),
            LeaderboardUser("user_me", 128, "You (Dharmjeet)", 3450, isCurrentUser = true)
        )
    )
    val monthlyLeaderboard: StateFlow<List<LeaderboardUser>> = _monthlyLeaderboard.asStateFlow()

    private val _redemptionOptions = MutableStateFlow(
        listOf(
            RedemptionOption("rdm_opt_1", "₹50 Gift Card", "Shopping voucher valid across popular stores", 500, "₹50", "giftcard"),
            RedemptionOption("rdm_opt_2", "₹100 Gift Card", "Shopping voucher valid across popular stores", 1000, "₹100", "giftcard"),
            RedemptionOption("rdm_opt_3", "₹200 Gift Card", "Shopping voucher valid across popular stores", 2000, "₹200", "giftcard")
        )
    )
    val redemptionOptions: StateFlow<List<RedemptionOption>> = _redemptionOptions.asStateFlow()

    private val _redemptionHistory = MutableStateFlow(
        listOf(
            RedemptionRecord("rdm_rec_1", "₹50 Gift Card", "₹50", 500, "Today, 11:00 AM", RedemptionStatus.PENDING, "RDM-99182"),
            RedemptionRecord("rdm_rec_2", "₹100 Gift Card", "₹100", 1000, "02 Oct 2026", RedemptionStatus.COMPLETED, "RDM-88120"),
            RedemptionRecord("rdm_rec_3", "₹50 Gift Card", "₹50", 500, "20 Sep 2026", RedemptionStatus.FAILED, "RDM-77109")
        )
    )
    val redemptionHistory: StateFlow<List<RedemptionRecord>> = _redemptionHistory.asStateFlow()

    fun claimTask(taskId: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId && task.status == TaskStatus.CLAIMABLE) {
                task.copy(status = TaskStatus.CLAIMED)
            } else {
                task
            }
        }
    }

    fun addDemoRedemptionRecord(option: RedemptionOption): RedemptionRecord {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val newRecord = RedemptionRecord(
            id = "rdm_rec_${System.currentTimeMillis()}",
            optionTitle = option.title,
            monetaryValue = option.monetaryValue,
            coinsSpent = option.coinsCost,
            timestamp = dateFormat.format(Date()),
            status = RedemptionStatus.PENDING,
            referenceCode = "RDM-${(10000..99999).random()}"
        )
        _redemptionHistory.value = listOf(newRecord) + _redemptionHistory.value
        return newRecord
    }
}
