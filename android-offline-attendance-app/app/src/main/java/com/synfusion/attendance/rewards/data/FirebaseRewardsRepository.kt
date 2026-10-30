package com.synfusion.attendance.rewards.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.synfusion.attendance.online.awaitTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirebaseRewardsRepository(
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val functionsSupplier: () -> FirebaseFunctions = { FirebaseFunctions.getInstance() }
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    private val _walletSummary = MutableStateFlow(WalletSummary(0, 0, 0))
    val walletSummary: StateFlow<WalletSummary> = _walletSummary.asStateFlow()

    private val _spinTokens = MutableStateFlow(SpinTokenBalance(0))
    val spinTokens: StateFlow<SpinTokenBalance> = _spinTokens.asStateFlow()

    private val _dailyCheckInState = MutableStateFlow(DailyCheckInState())
    val dailyCheckInState: StateFlow<DailyCheckInState> = _dailyCheckInState.asStateFlow()

    private val _streakState = MutableStateFlow(DailyStreakState())
    val streakState: StateFlow<DailyStreakState> = _streakState.asStateFlow()

    private val _milestones = MutableStateFlow<List<MilestoneReward>>(emptyList())
    val milestones: StateFlow<List<MilestoneReward>> = _milestones.asStateFlow()

    private val _referralStats = MutableStateFlow(ReferralStats(0, 0, 0, 0, "WD7X29"))
    val referralStats: StateFlow<ReferralStats> = _referralStats.asStateFlow()

    private val _transactions = MutableStateFlow<List<RewardTransaction>>(emptyList())
    val transactions: StateFlow<List<RewardTransaction>> = _transactions.asStateFlow()

    private val _referrals = MutableStateFlow<List<ReferralItem>>(emptyList())
    val referrals: StateFlow<List<ReferralItem>> = _referrals.asStateFlow()

    private val _tasks = MutableStateFlow<List<RewardTask>>(emptyList())
    val tasks: StateFlow<List<RewardTask>> = _tasks.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _weeklyLeaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val weeklyLeaderboard: StateFlow<List<LeaderboardUser>> = _weeklyLeaderboard.asStateFlow()

    private val _monthlyLeaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val monthlyLeaderboard: StateFlow<List<LeaderboardUser>> = _monthlyLeaderboard.asStateFlow()

    private val _redemptionOptions = MutableStateFlow(
        listOf(
            RedemptionOption("rdm_opt_1", "₹50 Gift Card", "Shopping voucher valid across popular stores", 500, "₹50", "giftcard"),
            RedemptionOption("rdm_opt_2", "₹100 Gift Card", "Shopping voucher valid across popular stores", 1000, "₹100", "giftcard"),
            RedemptionOption("rdm_opt_3", "₹200 Gift Card", "Shopping voucher valid across popular stores", 2000, "₹200", "giftcard")
        )
    )
    val redemptionOptions: StateFlow<List<RedemptionOption>> = _redemptionOptions.asStateFlow()

    private val _redemptionHistory = MutableStateFlow<List<RedemptionRecord>>(emptyList())
    val redemptionHistory: StateFlow<List<RedemptionRecord>> = _redemptionHistory.asStateFlow()

    private var currentUid: String? = null

    init {
        val auth = try { authSupplier() } catch (e: Exception) { null }
        auth?.addAuthStateListener { authInstance ->
            val user = authInstance.currentUser
            if (user?.uid != currentUid) {
                currentUid = user?.uid
                if (currentUid != null) {
                    refreshState()
                    fetchLeaderboard(LeaderboardTimeframe.WEEKLY)
                    fetchLeaderboard(LeaderboardTimeframe.MONTHLY)
                } else {
                    clearState()
                }
            }
        }
        if (auth?.currentUser != null) {
            currentUid = auth.currentUser?.uid
            refreshState()
            fetchLeaderboard(LeaderboardTimeframe.WEEKLY)
            fetchLeaderboard(LeaderboardTimeframe.MONTHLY)
        }
    }

    private fun clearState() {
        _walletSummary.value = WalletSummary(0, 0, 0)
        _spinTokens.value = SpinTokenBalance(0)
        _dailyCheckInState.value = DailyCheckInState()
        _streakState.value = DailyStreakState()
        _milestones.value = emptyList()
        _referralStats.value = ReferralStats(0, 0, 0, 0, "")
        _transactions.value = emptyList()
        _referrals.value = emptyList()
        _tasks.value = emptyList()
        _achievements.value = emptyList()
        _weeklyLeaderboard.value = emptyList()
        _monthlyLeaderboard.value = emptyList()
        _redemptionHistory.value = emptyList()
    }

    fun refreshState() {
        scope.launch {
            try {
                val uid = authSupplier().currentUser?.uid ?: return@launch
                val result = functionsSupplier().getHttpsCallable("rewardsGetState")
                    .call().awaitTask()
                parseAndApplyState(result.data)
            } catch (e: Exception) {
                // Ignore network errors or unauthenticated state gracefully
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAndApplyState(data: Any?) {
        val map = data as? Map<*, *> ?: return

        val wallet = map["walletSummary"] as? Map<*, *>
        if (wallet != null) {
            val avail = (wallet["availableCoins"] as? Number)?.toInt() ?: 0
            val pend = (wallet["pendingCoins"] as? Number)?.toInt() ?: 0
            val life = (wallet["lifetimeEarned"] as? Number)?.toInt() ?: 0
            _walletSummary.value = WalletSummary(avail, pend, life)
        }

        val spins = map["spinTokens"] as? Map<*, *>
        if (spins != null) {
            val count = (spins["availableSpins"] as? Number)?.toInt() ?: 0
            _spinTokens.value = SpinTokenBalance(count)
        }

        val checkin = map["dailyCheckInState"] as? Map<*, *>
        val claimedDates = ((checkin?.get("claimedDates") as? List<*>)?.filterIsInstance<String>() ?: emptyList()).toSet()
        val todayStr = try { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) } catch (e: Throwable) { "" }
        val isClaimed = claimedDates.contains(todayStr)

        _dailyCheckInState.value = DailyCheckInState(
            isTodayClaimed = isClaimed,
            todayDate = todayStr,
            lastCheckInDate = checkin?.get("lastCheckInDate") as? String,
            claimedDates = claimedDates,
            status = if (isClaimed) DailyCheckInStatus.CLAIMED else DailyCheckInStatus.AVAILABLE
        )

        val streak = map["streakState"] as? Map<*, *>
        if (streak != null) {
            val curr = (streak["currentStreak"] as? Number)?.toInt() ?: 0
            val long = (streak["longestStreak"] as? Number)?.toInt() ?: 0
            val total = (streak["totalCheckInDays"] as? Number)?.toInt() ?: 0
            _streakState.value = DailyStreakState(curr, long, total)
        }

        val claimedMilestones = ((map["claimedMilestoneIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()).toSet()
        recalculateMilestones(claimedMilestones)

        val refStats = map["referralStats"] as? Map<*, *>
        if (refStats != null) {
            val invited = (refStats["invitedCount"] as? Number)?.toInt() ?: 0
            val rewarded = (refStats["rewardedCount"] as? Number)?.toInt() ?: 0
            val pending = (refStats["pendingCount"] as? Number)?.toInt() ?: 0
            val coins = (refStats["totalCoinsEarned"] as? Number)?.toInt() ?: 0
            val code = (refStats["referralCode"] as? String) ?: ""
            _referralStats.value = ReferralStats(invited, rewarded, pending, coins, code)
        }

        val txs = map["transactions"] as? List<*>
        if (txs != null) {
            _transactions.value = txs.mapNotNull { parseTransaction(it as? Map<*, *>) }
        }

        val rdms = map["redemptions"] as? List<*>
        if (rdms != null) {
            _redemptionHistory.value = rdms.mapNotNull { parseRedemptionRecord(it as? Map<*, *>) }
        }

        val refs = map["referrals"] as? List<*>
        if (refs != null) {
            _referrals.value = refs.mapNotNull { parseReferralItem(it as? Map<*, *>) }
        }

        val claimedTasks = ((map["claimedTaskIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()).toSet()
        recalculateTasks(claimedTasks)
    }

    private fun parseTransaction(map: Map<*, *>?): RewardTransaction? {
        if (map == null) return null
        val id = map["id"] as? String ?: return null
        val title = map["title"] as? String ?: ""
        val desc = map["description"] as? String ?: ""
        val ts = map["timestamp"] as? String ?: ""
        val amount = (map["amount"] as? Number)?.toInt() ?: 0
        val statusStr = map["status"] as? String ?: "CONFIRMED"
        val typeStr = map["type"] as? String ?: "EARNED"
        val refCode = map["referenceCode"] as? String
        val source = map["source"] as? String ?: "Rewards"

        val status = try { RewardTransactionStatus.valueOf(statusStr) } catch (e: Exception) { RewardTransactionStatus.CONFIRMED }
        val type = try { RewardTransactionType.valueOf(typeStr) } catch (e: Exception) { RewardTransactionType.EARNED }

        return RewardTransaction(id, title, desc, ts, amount, status, type, refCode, source)
    }

    private fun parseRedemptionRecord(map: Map<*, *>?): RedemptionRecord? {
        if (map == null) return null
        val id = map["id"] as? String ?: return null
        val title = map["optionTitle"] as? String ?: ""
        val monVal = map["monetaryValue"] as? String ?: ""
        val coins = (map["coinsSpent"] as? Number)?.toInt() ?: 0
        val ts = map["timestamp"] as? String ?: ""
        val statusStr = map["status"] as? String ?: "PENDING"
        val refCode = map["referenceCode"] as? String ?: ""

        val status = try { RedemptionStatus.valueOf(statusStr) } catch (e: Exception) { RedemptionStatus.PENDING }

        return RedemptionRecord(id, title, monVal, coins, ts, status, refCode)
    }

    private fun parseReferralItem(map: Map<*, *>?): ReferralItem? {
        if (map == null) return null
        val id = map["id"] as? String ?: return null
        val name = map["name"] as? String ?: "Invited Friend"
        val statusStr = map["status"] as? String ?: "PENDING"
        val coins = (map["rewardCoins"] as? Number)?.toInt() ?: 100
        val date = map["date"] as? String ?: ""

        val status = try { ReferralStatus.valueOf(statusStr) } catch (e: Exception) { ReferralStatus.PENDING }

        return ReferralItem(id, name, status, coins, date)
    }

    private fun recalculateMilestones(claimedIds: Set<String>) {
        val streak = _streakState.value.currentStreak
        val totalDays = _streakState.value.totalCheckInDays

        val m7 = MilestoneReward(
            id = "milestone_7_streak",
            type = MilestoneType.SEVEN_DAY_STREAK,
            title = "7 Day Streak",
            description = "7 consecutive daily check-ins",
            requiredProgress = 7,
            currentProgress = minOf(streak, 7),
            coinReward = 20,
            bonusSpins = 1,
            badgeName = "Safety Starter",
            badgeIconName = "badge_safety_starter",
            status = when {
                claimedIds.contains("milestone_7_streak") -> MilestoneStatus.CLAIMED
                streak >= 7 -> MilestoneStatus.CLAIMABLE
                else -> MilestoneStatus.LOCKED
            }
        )

        val m30 = MilestoneReward(
            id = "milestone_30_days",
            type = MilestoneType.THIRTY_DAYS,
            title = "30 Check-in Days",
            description = "30 unique lifetime check-in days",
            requiredProgress = 30,
            currentProgress = minOf(totalDays, 30),
            coinReward = 75,
            bonusSpins = 0,
            badgeName = "Consistent Builder",
            badgeIconName = "badge_consistent_builder",
            status = when {
                claimedIds.contains("milestone_30_days") -> MilestoneStatus.CLAIMED
                totalDays >= 30 -> MilestoneStatus.CLAIMABLE
                else -> MilestoneStatus.LOCKED
            }
        )

        val m180 = MilestoneReward(
            id = "milestone_180_days",
            type = MilestoneType.ONE_HUNDRED_EIGHTY_DAYS,
            title = "180 Check-in Days",
            description = "180 unique lifetime check-in days",
            requiredProgress = 180,
            currentProgress = minOf(totalDays, 180),
            coinReward = 350,
            bonusSpins = 0,
            badgeName = "Master Builder",
            badgeIconName = "badge_master_builder",
            status = when {
                claimedIds.contains("milestone_180_days") -> MilestoneStatus.CLAIMED
                totalDays >= 180 -> MilestoneStatus.CLAIMABLE
                else -> MilestoneStatus.LOCKED
            }
        )

        val m365 = MilestoneReward(
            id = "milestone_365_days",
            type = MilestoneType.THREE_HUNDRED_SIXTY_FIVE_DAYS,
            title = "365 Check-in Days",
            description = "365 unique lifetime check-in days",
            requiredProgress = 365,
            currentProgress = minOf(totalDays, 365),
            coinReward = 750,
            bonusSpins = 3,
            badgeName = "365 Day Builder Legend",
            badgeIconName = "badge_builder_legend",
            status = when {
                claimedIds.contains("milestone_365_days") -> MilestoneStatus.CLAIMED
                totalDays >= 365 -> MilestoneStatus.CLAIMABLE
                else -> MilestoneStatus.LOCKED
            }
        )

        _milestones.value = listOf(m7, m30, m180, m365)
    }

    private fun recalculateTasks(claimedIds: Set<String>) {
        val checkinClaimed = _dailyCheckInState.value.isTodayClaimed
        val streak = _streakState.value.currentStreak

        _tasks.value = listOf(
            RewardTask(
                id = "task_1",
                title = "Daily Check-in",
                description = "Open WorkDaily and check in daily",
                category = TaskCategory.DAILY,
                currentProgress = if (checkinClaimed) 1 else 0,
                maxProgress = 1,
                coinReward = 5,
                status = when {
                    claimedIds.contains("task_1") -> TaskStatus.CLAIMED
                    checkinClaimed -> TaskStatus.CLAIMABLE
                    else -> TaskStatus.IN_PROGRESS
                },
                requirements = listOf("Open app", "Mark attendance or view logs")
            ),
            RewardTask(
                id = "task_2",
                title = "Stay Consistent",
                description = "Mark attendance for 5 consecutive days",
                category = TaskCategory.WEEKLY,
                currentProgress = minOf(streak, 5),
                maxProgress = 5,
                coinReward = 25,
                status = when {
                    claimedIds.contains("task_2") -> TaskStatus.CLAIMED
                    streak >= 5 -> TaskStatus.CLAIMABLE
                    else -> TaskStatus.IN_PROGRESS
                },
                requirements = listOf("Log attendance 5 days this week")
            ),
            RewardTask(
                id = "task_3",
                title = "Add Profile Photo",
                description = "Upload your profile avatar image",
                category = TaskCategory.SPECIAL,
                currentProgress = 1,
                maxProgress = 1,
                coinReward = 20,
                status = when {
                    claimedIds.contains("task_3") -> TaskStatus.CLAIMED
                    else -> TaskStatus.CLAIMABLE
                },
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
                status = when {
                    claimedIds.contains("task_4") -> TaskStatus.CLAIMED
                    else -> TaskStatus.CLAIMABLE
                },
                requirements = listOf("Complete 100% of workspace profile")
            )
        )
    }

    suspend fun claimDailyCheckIn(): Boolean {
        return try {
            functionsSupplier().getHttpsCallable("rewardsDailyCheckIn")
                .call().awaitTask()
            refreshState()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun claimMilestone(milestoneId: String): Boolean {
        return try {
            functionsSupplier().getHttpsCallable("rewardsClaimMilestone")
                .call(mapOf("milestoneId" to milestoneId)).awaitTask()
            refreshState()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun spinWheel(): SpinWheelSegment? {
        return try {
            val requestId = UUID.randomUUID().toString()
            val result = functionsSupplier().getHttpsCallable("rewardsSpinWheel")
                .call(mapOf("requestId" to requestId)).awaitTask()
            val map = result.data as? Map<*, *> ?: return null
            val sectorIndex = (map["sectorIndex"] as? Number)?.toInt() ?: 0
            val selectedSegment = spinSegments.getOrNull(sectorIndex) ?: spinSegments[0]
            refreshState()
            selectedSegment
        } catch (e: Exception) {
            null
        }
    }

    suspend fun claimTask(taskId: String): Boolean {
        return try {
            functionsSupplier().getHttpsCallable("rewardsClaimTask")
                .call(mapOf("taskId" to taskId)).awaitTask()
            refreshState()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitRedemption(option: RedemptionOption): RedemptionRecord? {
        return try {
            val requestId = UUID.randomUUID().toString()
            val result = functionsSupplier().getHttpsCallable("rewardsSubmitRedemption")
                .call(mapOf("optionId" to option.id, "requestId" to requestId)).awaitTask()
            val map = result.data as? Map<*, *> ?: return null
            val rdmMap = map["redemptionRecord"] as? Map<*, *> ?: return null
            val record = parseRedemptionRecord(rdmMap)
            refreshState()
            record
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitReferralCode(code: String): Result<Boolean> {
        return try {
            functionsSupplier().getHttpsCallable("rewardsSubmitReferralCode")
                .call(mapOf("referralCode" to code)).awaitTask()
            refreshState()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fetchLeaderboard(timeframe: LeaderboardTimeframe) {
        scope.launch {
            try {
                val tfStr = timeframe.name
                val result = functionsSupplier().getHttpsCallable("rewardsGetLeaderboard")
                    .call(mapOf("timeframe" to tfStr)).awaitTask()
                val map = result.data as? Map<*, *> ?: return@launch
                val list = map["leaderboard"] as? List<*> ?: return@launch

                val users = list.mapNotNull { item ->
                    val userMap = item as? Map<*, *> ?: return@mapNotNull null
                    val id = userMap["id"] as? String ?: return@mapNotNull null
                    val rank = (userMap["rank"] as? Number)?.toInt() ?: 0
                    val name = userMap["name"] as? String ?: "Worker"
                    val xp = (userMap["xp"] as? Number)?.toInt() ?: 0
                    val isCurr = (userMap["isCurrentUser"] as? Boolean) ?: false
                    LeaderboardUser(id, rank, name, xp, isCurr)
                }

                if (timeframe == LeaderboardTimeframe.WEEKLY) {
                    _weeklyLeaderboard.value = users
                } else {
                    _monthlyLeaderboard.value = users
                }
            } catch (e: Exception) {
                // Ignore leaderboard fetch error
            }
        }
    }

    fun updateDateState(dateStr: String) {
        refreshState()
    }
}
