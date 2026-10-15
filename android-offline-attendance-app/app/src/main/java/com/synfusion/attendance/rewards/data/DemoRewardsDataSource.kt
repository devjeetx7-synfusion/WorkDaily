package com.synfusion.attendance.rewards.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.Random

class DemoRewardsDataSource(
    context: Context? = null,
    private val randomSupplier: () -> Double = { Random().nextDouble() }
) {

    private val prefs: SharedPreferences? = context?.applicationContext?.getSharedPreferences(
        "demo_rewards_persistent_store",
        Context.MODE_PRIVATE
    )

    // 8 Construction-Themed Spin Sectors according to specifications
    val spinSegments = listOf(
        SpinWheelSegment(1, "Safety Helmet", 2, "helmet", SpinRewardType.Coins(2)),
        SpinWheelSegment(2, "Hammer", 4, "hammer", SpinRewardType.Coins(4)),
        SpinWheelSegment(3, "Bricks", 5, "bricks", SpinRewardType.Coins(5)),
        SpinWheelSegment(4, "Toolbox", 8, "toolbox", SpinRewardType.Coins(8)),
        SpinWheelSegment(5, "Construction Crane", 10, "crane", SpinRewardType.Coins(10)),
        SpinWheelSegment(6, "Cement Truck", 12, "truck", SpinRewardType.Coins(12)),
        SpinWheelSegment(7, "Golden Blueprint", 20, "blueprint", SpinRewardType.Coins(20)),
        SpinWheelSegment(8, "Foreman's Pass", 0, "pass", SpinRewardType.BonusSpin(1))
    )

    private val _spinTokens = MutableStateFlow(
        SpinTokenBalance(availableSpins = prefs?.getInt("available_spins", 2) ?: 2)
    )
    val spinTokens: StateFlow<SpinTokenBalance> = _spinTokens.asStateFlow()

    private val initialClaimedDates = prefs?.getStringSet("claimed_dates", emptySet()) ?: emptySet()
    private val _dailyCheckInState = MutableStateFlow(
        DailyCheckInState(claimedDates = initialClaimedDates)
    )
    val dailyCheckInState: StateFlow<DailyCheckInState> = _dailyCheckInState.asStateFlow()

    private val _streakState = MutableStateFlow(DailyStreakState())
    val streakState: StateFlow<DailyStreakState> = _streakState.asStateFlow()

    private val _milestones = MutableStateFlow<List<MilestoneReward>>(emptyList())
    val milestones: StateFlow<List<MilestoneReward>> = _milestones.asStateFlow()

    private val initialClaimedMilestoneIds = prefs?.getStringSet("claimed_milestone_ids", emptySet()) ?: emptySet()
    private val _claimedMilestoneIds = MutableStateFlow<Set<String>>(initialClaimedMilestoneIds)
    val claimedMilestoneIds: StateFlow<Set<String>> = _claimedMilestoneIds.asStateFlow()

    private val _walletSummary = MutableStateFlow(
        WalletSummary(
            availableCoins = prefs?.getInt("available_coins", 2000) ?: 2000,
            pendingCoins = prefs?.getInt("pending_coins", 450) ?: 450,
            lifetimeEarned = prefs?.getInt("lifetime_earned", 7850) ?: 7850
        )
    )
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
            Achievement("ach_m7", "Safety Starter", "Claim 7-day check-in streak bonus", AchievementCategory.MILESTONES, false, 0, 7, "fire"),
            Achievement("ach_m30", "Consistent Builder", "Reach 30 unique check-in days", AchievementCategory.MILESTONES, false, 0, 30, "calendar"),
            Achievement("ach_m180", "Master Builder", "Reach 180 unique check-in days", AchievementCategory.MILESTONES, false, 0, 180, "badge"),
            Achievement("ach_m365", "365 Day Builder Legend", "Reach 365 unique check-in days", AchievementCategory.MILESTONES, false, 0, 365, "crown"),
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

    init {
        // Initialize check-in state for today safely
        val todayStr = try {
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Throwable) {
            "2026-10-15"
        }
        updateDateState(todayStr)
    }

    private fun persistState() {
        prefs?.edit()?.apply {
            putInt("available_spins", _spinTokens.value.availableSpins)
            putStringSet("claimed_dates", _dailyCheckInState.value.claimedDates)
            putStringSet("claimed_milestone_ids", _claimedMilestoneIds.value)
            putInt("available_coins", _walletSummary.value.availableCoins)
            putInt("pending_coins", _walletSummary.value.pendingCoins)
            putInt("lifetime_earned", _walletSummary.value.lifetimeEarned)
            apply()
        }
    }

    fun updateDateState(currentDateStr: String) {
        val claimed = _dailyCheckInState.value.claimedDates
        val isTodayClaimed = claimed.contains(currentDateStr)
        val status = if (isTodayClaimed) DailyCheckInStatus.CLAIMED else DailyCheckInStatus.AVAILABLE

        _dailyCheckInState.value = _dailyCheckInState.value.copy(
            isTodayClaimed = isTodayClaimed,
            todayDate = currentDateStr,
            status = status
        )

        recalculateStreaks(currentDateStr)
        recalculateMilestones()
    }

    private fun recalculateStreaks(referenceDateStr: String) {
        val claimedDates = _dailyCheckInState.value.claimedDates.mapNotNull {
            try { LocalDate.parse(it) } catch (e: Throwable) { null }
        }.sorted()

        if (claimedDates.isEmpty()) {
            _streakState.value = DailyStreakState(0, 0, 0)
            return
        }

        val totalDays = claimedDates.size
        var maxStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (d in claimedDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else if (ChronoUnit.DAYS.between(prevDate, d) == 1L) {
                tempStreak += 1
            } else {
                tempStreak = 1
            }
            if (tempStreak > maxStreak) {
                maxStreak = tempStreak
            }
            prevDate = d
        }

        val refDate = try { LocalDate.parse(referenceDateStr) } catch (e: Throwable) { LocalDate.of(2026, 10, 15) }
        var currentStreak = 0
        val lastClaimed = claimedDates.lastOrNull()

        if (lastClaimed != null) {
            val daysDiff = ChronoUnit.DAYS.between(lastClaimed, refDate)
            if (daysDiff == 0L || daysDiff == 1L) {
                var streakCount = 0
                val dateSet = claimedDates.toSet()

                var ptr: LocalDate? = lastClaimed
                while (ptr != null && dateSet.contains(ptr)) {
                    streakCount++
                    ptr = ptr.minusDays(1)
                }
                currentStreak = streakCount
            } else {
                currentStreak = 0
            }
        }

        _streakState.value = DailyStreakState(
            currentStreak = currentStreak,
            longestStreak = maxOf(maxStreak, currentStreak),
            totalCheckInDays = totalDays
        )
    }

    private fun recalculateMilestones() {
        val streak = _streakState.value.currentStreak
        val totalDays = _streakState.value.totalCheckInDays
        val claimedIds = _claimedMilestoneIds.value

        val cycle7Count = streak / 7
        val current7Progress = streak % 7
        val m7Id = "milestone_7_streak_${cycle7Count + 1}"
        val is7Claimed = claimedIds.contains(m7Id)
        val status7 = when {
            is7Claimed -> MilestoneStatus.CLAIMED
            streak >= 7 && (streak % 7 == 0 || cycle7Count > 0) && !is7Claimed -> MilestoneStatus.CLAIMABLE
            else -> MilestoneStatus.LOCKED
        }

        val m7 = MilestoneReward(
            id = m7Id,
            type = MilestoneType.SEVEN_DAY_STREAK,
            title = "7 Day Streak",
            description = "7 consecutive daily check-ins",
            requiredProgress = 7,
            currentProgress = if (status7 == MilestoneStatus.CLAIMABLE) 7 else current7Progress,
            coinReward = 20,
            bonusSpins = 1,
            badgeName = "Safety Starter",
            badgeIconName = "badge_safety_starter",
            status = status7
        )

        val m30Id = "milestone_30_days"
        val is30Claimed = claimedIds.contains(m30Id)
        val status30 = when {
            is30Claimed -> MilestoneStatus.CLAIMED
            totalDays >= 30 -> MilestoneStatus.CLAIMABLE
            else -> MilestoneStatus.LOCKED
        }
        val m30 = MilestoneReward(
            id = m30Id,
            type = MilestoneType.THIRTY_DAYS,
            title = "30 Check-in Days",
            description = "30 unique lifetime check-in days",
            requiredProgress = 30,
            currentProgress = minOf(totalDays, 30),
            coinReward = 75,
            bonusSpins = 0,
            badgeName = "Consistent Builder",
            badgeIconName = "badge_consistent_builder",
            status = status30
        )

        val m180Id = "milestone_180_days"
        val is180Claimed = claimedIds.contains(m180Id)
        val status180 = when {
            is180Claimed -> MilestoneStatus.CLAIMED
            totalDays >= 180 -> MilestoneStatus.CLAIMABLE
            else -> MilestoneStatus.LOCKED
        }
        val m180 = MilestoneReward(
            id = m180Id,
            type = MilestoneType.ONE_HUNDRED_EIGHTY_DAYS,
            title = "180 Check-in Days",
            description = "180 unique lifetime check-in days",
            requiredProgress = 180,
            currentProgress = minOf(totalDays, 180),
            coinReward = 350,
            bonusSpins = 0,
            badgeName = "Master Builder",
            badgeIconName = "badge_master_builder",
            status = status180
        )

        val m365Id = "milestone_365_days"
        val is365Claimed = claimedIds.contains(m365Id)
        val status365 = when {
            is365Claimed -> MilestoneStatus.CLAIMED
            totalDays >= 365 -> MilestoneStatus.CLAIMABLE
            else -> MilestoneStatus.LOCKED
        }
        val m365 = MilestoneReward(
            id = m365Id,
            type = MilestoneType.THREE_HUNDRED_SIXTY_FIVE_DAYS,
            title = "365 Check-in Days",
            description = "365 unique lifetime check-in days",
            requiredProgress = 365,
            currentProgress = minOf(totalDays, 365),
            coinReward = 750,
            bonusSpins = 3,
            badgeName = "365 Day Builder Legend",
            badgeIconName = "badge_builder_legend",
            status = status365
        )

        _milestones.value = listOf(m7, m30, m180, m365)

        _achievements.value = _achievements.value.map { ach ->
            when (ach.id) {
                "ach_m7" -> ach.copy(
                    isUnlocked = m7.status == MilestoneStatus.CLAIMED || m7.status == MilestoneStatus.CLAIMABLE,
                    currentProgress = m7.currentProgress,
                    maxProgress = m7.requiredProgress
                )
                "ach_m30" -> ach.copy(
                    isUnlocked = m30.status == MilestoneStatus.CLAIMED || m30.status == MilestoneStatus.CLAIMABLE,
                    currentProgress = m30.currentProgress,
                    maxProgress = m30.requiredProgress
                )
                "ach_m180" -> ach.copy(
                    isUnlocked = m180.status == MilestoneStatus.CLAIMED || m180.status == MilestoneStatus.CLAIMABLE,
                    currentProgress = m180.currentProgress,
                    maxProgress = m180.requiredProgress
                )
                "ach_m365" -> ach.copy(
                    isUnlocked = m365.status == MilestoneStatus.CLAIMED || m365.status == MilestoneStatus.CLAIMABLE,
                    currentProgress = m365.currentProgress,
                    maxProgress = m365.requiredProgress
                )
                else -> ach
            }
        }
    }

    fun claimDailyCheckIn(dateStr: String = try { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) } catch (e: Throwable) { "2026-10-15" }): Boolean {
        if (_dailyCheckInState.value.claimedDates.contains(dateStr)) {
            return false
        }

        val updatedDates = _dailyCheckInState.value.claimedDates + dateStr
        _dailyCheckInState.value = _dailyCheckInState.value.copy(
            isTodayClaimed = true,
            todayDate = dateStr,
            lastCheckInDate = dateStr,
            claimedDates = updatedDates,
            status = DailyCheckInStatus.CLAIMED
        )

        // Award +5 coins and +1 Spin Token
        creditDemoCoins(5, "Daily Check-in Reward", "Logged daily check-in for $dateStr", "Daily Check-in")
        _spinTokens.value = _spinTokens.value.copy(availableSpins = _spinTokens.value.availableSpins + 1)

        _tasks.value = _tasks.value.map { task ->
            if (task.id == "task_1") {
                task.copy(currentProgress = 1, status = TaskStatus.CLAIMED)
            } else {
                task
            }
        }

        recalculateStreaks(dateStr)
        recalculateMilestones()
        persistState()
        return true
    }

    fun claimMilestone(milestoneId: String): Boolean {
        val milestone = _milestones.value.find { it.id == milestoneId } ?: return false
        if (milestone.status != MilestoneStatus.CLAIMABLE) return false

        _claimedMilestoneIds.value = _claimedMilestoneIds.value + milestoneId

        if (milestone.coinReward > 0) {
            creditDemoCoins(milestone.coinReward, "${milestone.title} Bonus", "Claimed milestone: ${milestone.description}", "Milestone Reward")
        }
        if (milestone.bonusSpins > 0) {
            _spinTokens.value = _spinTokens.value.copy(availableSpins = _spinTokens.value.availableSpins + milestone.bonusSpins)
        }

        recalculateMilestones()
        persistState()
        return true
    }

    fun spinWheel(): SpinWheelSegment? {
        if (_spinTokens.value.availableSpins <= 0) {
            return null
        }

        // Deduct 1 Spin Token
        _spinTokens.value = _spinTokens.value.copy(availableSpins = _spinTokens.value.availableSpins - 1)

        // Equal probability 8-sector random selection
        val randVal = randomSupplier()
        val index = (randVal * spinSegments.size).toInt().coerceIn(0, spinSegments.size - 1)
        val selectedSegment = spinSegments[index]

        when (val reward = selectedSegment.rewardType) {
            is SpinRewardType.Coins -> {
                creditDemoCoins(
                    amount = reward.amount,
                    title = "Spin Wheel Prize",
                    description = "Won ${reward.amount} coins on ${selectedSegment.name}",
                    source = "Spin Wheel"
                )
            }
            is SpinRewardType.BonusSpin -> {
                _spinTokens.value = _spinTokens.value.copy(
                    availableSpins = _spinTokens.value.availableSpins + reward.count
                )
                addTransaction(
                    title = "Bonus Spin Won",
                    description = "+${reward.count} Free Spin Token on ${selectedSegment.name}",
                    amount = 0,
                    source = "Spin Wheel"
                )
            }
        }

        persistState()
        return selectedSegment
    }

    private fun creditDemoCoins(amount: Int, title: String, description: String, source: String) {
        val currentSummary = _walletSummary.value
        _walletSummary.value = currentSummary.copy(
            availableCoins = currentSummary.availableCoins + amount,
            lifetimeEarned = currentSummary.lifetimeEarned + amount
        )

        addTransaction(title, description, amount, source)
    }

    private fun addTransaction(title: String, description: String, amount: Int, source: String) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val newTx = RewardTransaction(
            id = "tx_${System.currentTimeMillis()}_${(100..999).random()}",
            title = title,
            description = description,
            timestamp = dateFormat.format(Date()),
            amount = amount,
            status = RewardTransactionStatus.CONFIRMED,
            type = if (amount >= 0) RewardTransactionType.EARNED else RewardTransactionType.SPENT,
            referenceCode = "RW-${(10000..99999).random()}",
            source = source
        )

        _transactions.value = listOf(newTx) + _transactions.value
    }

    fun claimTask(taskId: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId && task.status == TaskStatus.CLAIMABLE) {
                creditDemoCoins(task.coinReward, "Task Reward", "Completed task: ${task.title}", "Tasks")
                task.copy(status = TaskStatus.CLAIMED)
            } else {
                task
            }
        }
        persistState()
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

        // Deduct coins for redemption
        val currentSummary = _walletSummary.value
        if (currentSummary.availableCoins >= option.coinsCost) {
            _walletSummary.value = currentSummary.copy(
                availableCoins = currentSummary.availableCoins - option.coinsCost
            )
            addTransaction(
                title = "Reward Redemption",
                description = "Redeemed ${option.title} (${option.monetaryValue})",
                amount = -option.coinsCost,
                source = "Redemption Center"
            )
        }

        persistState()
        return newRecord
    }
}
