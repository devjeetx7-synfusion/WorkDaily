package com.synfusion.attendance.rewards.data

enum class RewardTransactionStatus {
    CONFIRMED,
    PENDING,
    COMPLETED,
    REVERSED,
    REJECTED
}

enum class RewardTransactionType {
    EARNED,
    PENDING,
    SPENT
}

data class WalletSummary(
    val availableCoins: Int = 2000,
    val pendingCoins: Int = 450,
    val lifetimeEarned: Int = 7850
) {
    val totalBalance: Int get() = availableCoins + pendingCoins
}

data class RewardTransaction(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val amount: Int,
    val status: RewardTransactionStatus,
    val type: RewardTransactionType,
    val referenceCode: String? = null,
    val source: String
)

enum class ReferralStatus {
    PENDING,
    REWARDED,
    NOT_QUALIFIED
}

data class ReferralItem(
    val id: String,
    val name: String,
    val status: ReferralStatus,
    val rewardCoins: Int,
    val date: String
)

data class ReferralStats(
    val invitedCount: Int = 12,
    val rewardedCount: Int = 7,
    val pendingCount: Int = 3,
    val totalCoinsEarned: Int = 500,
    val referralCode: String = "WD7X29"
)

enum class TaskCategory {
    DAILY,
    WEEKLY,
    SPECIAL
}

enum class TaskStatus {
    IN_PROGRESS,
    CLAIMABLE,
    CLAIMED
}

data class RewardTask(
    val id: String,
    val title: String,
    val description: String,
    val category: TaskCategory,
    val currentProgress: Int,
    val maxProgress: Int,
    val coinReward: Int,
    val status: TaskStatus,
    val requirements: List<String>
)

enum class AchievementCategory {
    ATTENDANCE,
    WORK,
    MILESTONES
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val maxProgress: Int,
    val iconName: String,
    val unlockedDate: String? = null
)

data class LeaderboardUser(
    val id: String,
    val rank: Int,
    val name: String,
    val xp: Int,
    val isCurrentUser: Boolean = false,
    val avatarUrl: String? = null
)

enum class LeaderboardTimeframe {
    WEEKLY,
    MONTHLY
}

data class RedemptionOption(
    val id: String,
    val title: String,
    val description: String,
    val coinsCost: Int,
    val monetaryValue: String,
    val iconType: String
)

enum class RedemptionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REVERSED
}

data class RedemptionRecord(
    val id: String,
    val optionTitle: String,
    val monetaryValue: String,
    val coinsSpent: Int,
    val timestamp: String,
    val status: RedemptionStatus,
    val referenceCode: String
)

enum class DailyCheckInStatus {
    AVAILABLE,
    CLAIMING,
    CLAIMED
}

data class DailyCheckInState(
    val isTodayClaimed: Boolean = false,
    val todayDate: String = "",
    val lastCheckInDate: String? = null,
    val claimedDates: Set<String> = emptySet(),
    val status: DailyCheckInStatus = DailyCheckInStatus.AVAILABLE
)

data class DailyStreakState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCheckInDays: Int = 0
)

enum class MilestoneStatus {
    LOCKED,
    CLAIMABLE,
    CLAIMED
}

enum class MilestoneType {
    SEVEN_DAY_STREAK,
    THIRTY_DAYS,
    ONE_HUNDRED_EIGHTY_DAYS,
    THREE_HUNDRED_SIXTY_FIVE_DAYS
}

data class MilestoneReward(
    val id: String,
    val type: MilestoneType,
    val title: String,
    val description: String,
    val requiredProgress: Int,
    val currentProgress: Int,
    val coinReward: Int,
    val bonusSpins: Int,
    val badgeName: String,
    val badgeIconName: String,
    val status: MilestoneStatus
)

data class SpinWheelSegment(
    val id: Int,
    val name: String,
    val coinReward: Int,
    val iconName: String
)

data class SpinTokenBalance(
    val availableSpins: Int = 1
)

enum class SpinStatus {
    READY,
    SPINNING,
    RESULT_AVAILABLE,
    NO_SPINS
}

data class SpinRewardRecord(
    val id: String,
    val segment: SpinWheelSegment,
    val coinsEarned: Int,
    val timestamp: String
)
