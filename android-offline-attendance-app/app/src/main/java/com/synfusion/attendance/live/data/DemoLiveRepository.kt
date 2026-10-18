package com.synfusion.attendance.live.data

import android.content.Context
import android.content.SharedPreferences
import com.synfusion.attendance.live.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DemoLiveRepository(context: Context? = null) {

    private val inMemoryPrefs = HashMap<String, Any>()

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _gifts = MutableStateFlow(buildDefaultGifts())
    val gifts: StateFlow<List<LiveGift>> = _gifts.asStateFlow()

    private val _wallet = MutableStateFlow(loadWallet())
    val wallet: StateFlow<DemoWallet> = _wallet.asStateFlow()

    private val _creatorEarning = MutableStateFlow(loadCreatorEarning())
    val creatorEarning: StateFlow<DemoCreatorEarning> = _creatorEarning.asStateFlow()

    private val _followedCreatorIds = MutableStateFlow(loadFollowedCreatorIds())
    val followedCreatorIds: StateFlow<Set<String>> = _followedCreatorIds.asStateFlow()

    private val _creators = MutableStateFlow(buildInitialCreators())
    val creators: StateFlow<List<LiveCreator>> = _creators.asStateFlow()

    private val _liveRooms = MutableStateFlow(buildInitialLiveRooms())
    val liveRooms: StateFlow<List<LiveRoom>> = _liveRooms.asStateFlow()

    private val _activeRoom = MutableStateFlow<LiveRoom?>(null)
    val activeRoom: StateFlow<LiveRoom?> = _activeRoom.asStateFlow()

    private val _chatMessagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(buildInitialChatMessages())
    val chatMessagesMap: StateFlow<Map<String, List<ChatMessage>>> = _chatMessagesMap.asStateFlow()

    private val _giftTransactions = MutableStateFlow<List<GiftTransaction>>(emptyList())
    val giftTransactions: StateFlow<List<GiftTransaction>> = _giftTransactions.asStateFlow()

    init {
        syncFollowStateToCreators()
    }

    private fun syncFollowStateToCreators() {
        val followed = _followedCreatorIds.value
        _creators.value = _creators.value.map { creator ->
            creator.copy(isFollowing = followed.contains(creator.id))
        }
    }

    private fun loadWallet(): DemoWallet {
        val balance = prefs?.getInt(KEY_DIAMOND_BALANCE, 500) ?: (inMemoryPrefs[KEY_DIAMOND_BALANCE] as? Int ?: 500)
        val purchased = prefs?.getInt(KEY_TOTAL_PURCHASED, 500) ?: (inMemoryPrefs[KEY_TOTAL_PURCHASED] as? Int ?: 500)
        val spent = prefs?.getInt(KEY_TOTAL_SPENT, 0) ?: (inMemoryPrefs[KEY_TOTAL_SPENT] as? Int ?: 0)
        return DemoWallet(diamondBalance = balance, totalPurchased = purchased, totalSpent = spent)
    }

    private fun saveWallet(wallet: DemoWallet) {
        _wallet.value = wallet
        prefs?.edit()
            ?.putInt(KEY_DIAMOND_BALANCE, wallet.diamondBalance)
            ?.putInt(KEY_TOTAL_PURCHASED, wallet.totalPurchased)
            ?.putInt(KEY_TOTAL_SPENT, wallet.totalSpent)
            ?.apply()
        inMemoryPrefs[KEY_DIAMOND_BALANCE] = wallet.diamondBalance
        inMemoryPrefs[KEY_TOTAL_PURCHASED] = wallet.totalPurchased
        inMemoryPrefs[KEY_TOTAL_SPENT] = wallet.totalSpent
    }

    private fun loadCreatorEarning(): DemoCreatorEarning {
        val diamonds = prefs?.getInt(KEY_CREATOR_DIAMONDS, 1200) ?: (inMemoryPrefs[KEY_CREATOR_DIAMONDS] as? Int ?: 1200)
        val rupees = diamonds * RUPEES_PER_DIAMOND
        return DemoCreatorEarning(totalDiamondsReceived = diamonds, totalRupeesEarned = rupees)
    }

    private fun saveCreatorEarning(earning: DemoCreatorEarning) {
        _creatorEarning.value = earning
        prefs?.edit()
            ?.putInt(KEY_CREATOR_DIAMONDS, earning.totalDiamondsReceived)
            ?.apply()
        inMemoryPrefs[KEY_CREATOR_DIAMONDS] = earning.totalDiamondsReceived
    }

    private fun loadFollowedCreatorIds(): Set<String> {
        val set = prefs?.getStringSet(KEY_FOLLOWED_CREATORS, null)
        @Suppress("UNCHECKED_CAST")
        return set ?: (inMemoryPrefs[KEY_FOLLOWED_CREATORS] as? Set<String>) ?: setOf("creator_1", "creator_2")
    }

    private fun saveFollowedCreatorIds(followed: Set<String>) {
        _followedCreatorIds.value = followed
        prefs?.edit()?.putStringSet(KEY_FOLLOWED_CREATORS, followed)?.apply()
        inMemoryPrefs[KEY_FOLLOWED_CREATORS] = followed
        syncFollowStateToCreators()
    }

    fun toggleFollowCreator(creatorId: String) {
        val current = _followedCreatorIds.value.toMutableSet()
        if (current.contains(creatorId)) {
            current.remove(creatorId)
        } else {
            current.add(creatorId)
        }
        saveFollowedCreatorIds(current)
    }

    fun topUpDiamonds(amount: Int) {
        val current = _wallet.value
        val updated = current.copy(
            diamondBalance = current.diamondBalance + amount,
            totalPurchased = current.totalPurchased + amount
        )
        saveWallet(updated)
    }

    fun addFreeDiamonds(amount: Int = 1000) {
        topUpDiamonds(amount)
    }

    fun sendGift(roomId: String, gift: LiveGift): Boolean {
        val currentWallet = _wallet.value
        if (currentWallet.diamondBalance < gift.diamondCost) {
            return false
        }

        // Deduct diamonds from wallet
        val updatedWallet = currentWallet.copy(
            diamondBalance = currentWallet.diamondBalance - gift.diamondCost,
            totalSpent = currentWallet.totalSpent + gift.diamondCost
        )
        saveWallet(updatedWallet)

        // Record Gift Transaction
        val tx = GiftTransaction(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderId = "current_user",
            senderName = "You",
            giftId = gift.id,
            giftName = gift.name,
            diamondCost = gift.diamondCost
        )
        _giftTransactions.value = listOf(tx) + _giftTransactions.value

        // Update Room Diamond Earnings
        _liveRooms.value = _liveRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(diamondEarnings = room.diamondEarnings + gift.diamondCost)
            } else room
        }

        if (_activeRoom.value?.id == roomId) {
            val currentActive = _activeRoom.value
            if (currentActive != null) {
                _activeRoom.value = currentActive.copy(diamondEarnings = currentActive.diamondEarnings + gift.diamondCost)
            }
        }

        // Credit Creator Earnings (if stream is by creator)
        val room = _liveRooms.value.find { it.id == roomId }
        if (room != null) {
            val currentEarning = _creatorEarning.value
            val newTotalDiamonds = currentEarning.totalDiamondsReceived + gift.diamondCost
            val newTotalRupees = newTotalDiamonds * RUPEES_PER_DIAMOND
            saveCreatorEarning(currentEarning.copy(
                totalDiamondsReceived = newTotalDiamonds,
                totalRupeesEarned = newTotalRupees
            ))
        }

        // Post chat gift announcement
        val announcement = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderName = "You",
            message = "sent ${gift.name} (${gift.diamondCost} 💎)",
            isGiftAnnouncement = true,
            giftName = gift.name,
            giftDiamondCost = gift.diamondCost
        )
        addChatMessageInternal(roomId, announcement)

        return true
    }

    fun sendChatMessage(roomId: String, messageText: String, senderName: String = "You") {
        if (messageText.isBlank()) return
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderName = senderName,
            message = messageText.trim()
        )
        addChatMessageInternal(roomId, msg)
    }

    private fun addChatMessageInternal(roomId: String, msg: ChatMessage) {
        val currentMap = _chatMessagesMap.value.toMutableMap()
        val list = currentMap[roomId]?.toMutableList() ?: mutableListOf()
        list.add(msg)
        currentMap[roomId] = list
        _chatMessagesMap.value = currentMap
    }

    fun setActiveRoom(roomId: String?) {
        if (roomId == null) {
            _activeRoom.value = null
            return
        }
        val room = _liveRooms.value.find { it.id == roomId }
        _activeRoom.value = room
    }

    fun startLiveStream(
        title: String,
        category: String,
        type: LiveRoomType,
        tags: List<String>
    ): LiveRoom {
        val newRoom = LiveRoom(
            id = "room_" + UUID.randomUUID().toString().take(8),
            title = title.ifBlank { "Live Construction Workday" },
            creatorId = "creator_me",
            creatorName = "You (Host)",
            creatorAvatar = "",
            category = category,
            type = type,
            viewerCount = 1,
            diamondEarnings = 0,
            isLive = true,
            tags = tags.ifEmpty { listOf("LiveWork", "Construction") }
        )
        _liveRooms.value = listOf(newRoom) + _liveRooms.value
        _activeRoom.value = newRoom

        // Initial welcome chat message
        sendChatMessage(newRoom.id, "Welcome to the live stream! Ask questions in chat.", "System")
        return newRoom
    }

    fun endLiveStream(roomId: String) {
        _liveRooms.value = _liveRooms.value.map { room ->
            if (room.id == roomId) room.copy(isLive = false) else room
        }
        if (_activeRoom.value?.id == roomId) {
            _activeRoom.value = null
        }
    }

    fun cashoutEarnings(diamondsToCashout: Int): Boolean {
        val currentEarning = _creatorEarning.value
        if (diamondsToCashout <= 0 || currentEarning.totalDiamondsReceived < diamondsToCashout) {
            return false
        }

        val rupees = diamondsToCashout * RUPEES_PER_DIAMOND
        val newRecord = CashoutRecord(
            id = "cashout_" + UUID.randomUUID().toString().take(6),
            amountRupees = rupees,
            diamondsConverted = diamondsToCashout,
            timestamp = System.currentTimeMillis()
        )

        val updated = currentEarning.copy(
            totalDiamondsReceived = currentEarning.totalDiamondsReceived - diamondsToCashout,
            totalRupeesEarned = (currentEarning.totalDiamondsReceived - diamondsToCashout) * RUPEES_PER_DIAMOND,
            cashoutHistory = listOf(newRecord) + currentEarning.cashoutHistory
        )
        saveCreatorEarning(updated)
        return true
    }

    fun resetDemoData() {
        prefs?.edit()?.clear()?.apply()
        inMemoryPrefs.clear()
        _wallet.value = DemoWallet()
        _creatorEarning.value = DemoCreatorEarning()
        _followedCreatorIds.value = setOf("creator_1", "creator_2")
        _creators.value = buildInitialCreators()
        _liveRooms.value = buildInitialLiveRooms()
        _chatMessagesMap.value = buildInitialChatMessages()
        _giftTransactions.value = emptyList()
        _activeRoom.value = null
        syncFollowStateToCreators()
    }

    companion object {
        const val PREFS_NAME = "workdaily_live_demo_prefs"
        const val KEY_DIAMOND_BALANCE = "diamond_balance"
        const val KEY_TOTAL_PURCHASED = "total_purchased"
        const val KEY_TOTAL_SPENT = "total_spent"
        const val KEY_CREATOR_DIAMONDS = "creator_diamonds"
        const val KEY_FOLLOWED_CREATORS = "followed_creators"
        const val RUPEES_PER_DIAMOND = 0.50

        @Volatile
        private var INSTANCE: DemoLiveRepository? = null

        fun getInstance(context: Context): DemoLiveRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DemoLiveRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDefaultGifts(): List<LiveGift> = listOf(
            LiveGift("gift_1", "Safety Helmet", 1, "ic_helmet", "Site Tools", "Basic safety headgear"),
            LiveGift("gift_2", "Measuring Tape", 5, "ic_tape", "Site Tools", "Precision measurement tool"),
            LiveGift("gift_3", "Brick Stack", 10, "ic_brick", "Materials", "Stack of red building bricks"),
            LiveGift("gift_4", "Hard Hat", 20, "ic_hardhat", "Site Tools", "Pro worker hard hat"),
            LiveGift("gift_5", "Golden Trowel", 50, "ic_trowel", "Masonry", "Shining golden trowel"),
            LiveGift("gift_6", "Power Drill", 100, "ic_drill", "Tools", "Heavy duty power drill"),
            LiveGift("gift_7", "Scaffold", 150, "ic_scaffold", "Structures", "Steel scaffold frame"),
            LiveGift("gift_8", "Concrete Truck", 250, "ic_mixer_truck", "Machinery", "Ready-mix transit truck"),
            LiveGift("gift_9", "Cement Mixer", 300, "ic_mixer", "Machinery", "Heavy motor cement mixer"),
            LiveGift("gift_10", "Excavator", 500, "ic_excavator", "Machinery", "Hydraulic excavator digger"),
            LiveGift("gift_11", "Bulldozer", 750, "ic_dozer", "Machinery", "Crawler earthmover bulldozer"),
            LiveGift("gift_12", "Crane", 1000, "ic_crane", "Heavy Equipment", "Mobile telescopic crane"),
            LiveGift("gift_13", "Tower Crane", 1500, "ic_tower_crane", "Heavy Equipment", "Skyscraper tower crane"),
            LiveGift("gift_14", "Diamond Saw", 2000, "ic_saw", "Power Tools", "Diamond blade cutter"),
            LiveGift("gift_15", "Solar Panel", 2500, "ic_solar", "Green Energy", "Rooftop solar module"),
            LiveGift("gift_16", "Steel Beam", 3000, "ic_beam", "Structures", "Structural steel girder"),
            LiveGift("gift_17", "Architect Compass", 3500, "ic_compass", "Design", "Drafting precision compass"),
            LiveGift("gift_18", "Golden Hardhat", 4000, "ic_gold_hardhat", "VIP Gifts", "Pure gold engineer hardhat"),
            LiveGift("gift_19", "Heavy Hauler", 4500, "ic_hauler", "Machinery", "Multi-axle dump truck"),
            LiveGift("gift_20", "Master Builder Trophy", 5000, "ic_trophy", "Trophies", "Ultimate construction award")
        )

        private fun buildInitialCreators(): List<LiveCreator> = listOf(
            LiveCreator("creator_1", "Rajesh Sharma", "", "Senior Civil Site Engineer | 12 yrs site experience", 1420, true, 3400, "Site Safety"),
            LiveCreator("creator_2", "Amit Kumar", "", "Heavy Equipment Specialist & Operator", 2890, true, 5800, "Machinery"),
            LiveCreator("creator_3", "Vikram Singh", "", "Master Mason & Structural Concrete Artisan", 950, false, 1900, "Masonry"),
            LiveCreator("creator_4", "Pooja Verma", "", "Safety Auditor & Site Compliance Inspector", 3100, false, 8200, "Engineering"),
            LiveCreator("creator_5", "Suresh Patel", "", "Labor Contractor & Payroll Lead", 1850, false, 2600, "Contractor")
        )

        private fun buildInitialLiveRooms(): List<LiveRoom> = listOf(
            LiveRoom(
                id = "room_1",
                title = "Civil Site Foundation & Concrete Pouring",
                creatorId = "creator_1",
                creatorName = "Rajesh Sharma",
                creatorAvatar = "",
                category = "Site Safety",
                type = LiveRoomType.VIDEO,
                viewerCount = 348,
                diamondEarnings = 1250,
                isLive = true,
                tags = listOf("Foundation", "Concrete", "CivilSite")
            ),
            LiveRoom(
                id = "room_2",
                title = "Excavator & Heavy Machinery Safety Demo",
                creatorId = "creator_2",
                creatorName = "Amit Kumar",
                creatorAvatar = "",
                category = "Machinery",
                type = LiveRoomType.VIDEO,
                viewerCount = 512,
                diamondEarnings = 2400,
                isLive = true,
                tags = listOf("Excavator", "Machinery", "Operator")
            ),
            LiveRoom(
                id = "room_3",
                title = "Masonry Q&A: Plastering, Bricks & Tile Fitting",
                creatorId = "creator_3",
                creatorName = "Vikram Singh",
                creatorAvatar = "",
                category = "Masonry",
                type = LiveRoomType.AUDIO,
                viewerCount = 184,
                diamondEarnings = 680,
                isLive = true,
                tags = listOf("Masonry", "Plaster", "AudioStage")
            ),
            LiveRoom(
                id = "room_4",
                title = "Worker Safety Guidelines & Helmet Standards",
                creatorId = "creator_4",
                creatorName = "Pooja Verma",
                creatorAvatar = "",
                category = "Engineering",
                type = LiveRoomType.VIDEO,
                viewerCount = 420,
                diamondEarnings = 3100,
                isLive = true,
                tags = listOf("SafetyFirst", "PPE", "Inspection")
            ),
            LiveRoom(
                id = "room_5",
                title = "Daily Wage Rates & Labor Attendance Discussion",
                creatorId = "creator_5",
                creatorName = "Suresh Patel",
                creatorAvatar = "",
                category = "Contractor",
                type = LiveRoomType.AUDIO,
                viewerCount = 275,
                diamondEarnings = 1450,
                isLive = true,
                tags = listOf("WageRate", "Contractor", "AudioDiscussion")
            )
        )

        private fun buildInitialChatMessages(): Map<String, List<ChatMessage>> = mapOf(
            "room_1" to listOf(
                ChatMessage("c1", "room_1", "Sunil (Mason)", "", "How many bags of cement per cubic meter for M25?"),
                ChatMessage("c2", "room_1", "Rajesh Sharma", "", "For M25, use 1:1:2 ratio, about 11 bags per cum."),
                ChatMessage("c3", "room_1", "Ramesh", "", "Great live session sir!")
            ),
            "room_2" to listOf(
                ChatMessage("c4", "room_2", "Deepak", "", "What is the hourly hydraulic pressure check interval?"),
                ChatMessage("c5", "room_2", "Amit Kumar", "", "Check oil level daily before start, pressure monthly.")
            ),
            "room_3" to listOf(
                ChatMessage("c6", "room_3", "Rohan", "", "Audio is clear! Voice stage working perfectly.")
            )
        )
    }
}
