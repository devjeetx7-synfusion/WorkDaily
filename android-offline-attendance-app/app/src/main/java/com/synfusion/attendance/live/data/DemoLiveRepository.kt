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

    private val _guestRequestsMap = MutableStateFlow<Map<String, List<GuestRequest>>>(buildInitialGuestRequests())
    val guestRequestsMap: StateFlow<Map<String, List<GuestRequest>>> = _guestRequestsMap.asStateFlow()

    private val _audioSeatsMap = MutableStateFlow<Map<String, List<AudioSeat>>>(buildInitialAudioSeats())
    val audioSeatsMap: StateFlow<Map<String, List<AudioSeat>>> = _audioSeatsMap.asStateFlow()

    private val _roomParticipantsMap = MutableStateFlow<Map<String, List<RoomParticipant>>>(buildInitialRoomParticipants())
    val roomParticipantsMap: StateFlow<Map<String, List<RoomParticipant>>> = _roomParticipantsMap.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _mutedUserIdsMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val mutedUserIdsMap: StateFlow<Map<String, Set<String>>> = _mutedUserIdsMap.asStateFlow()

    private val _notifications = MutableStateFlow(buildInitialNotifications())
    val notifications: StateFlow<List<DemoNotification>> = _notifications.asStateFlow()

    private val _liveGoals = MutableStateFlow(buildInitialLiveGoals())
    val liveGoals: StateFlow<List<LiveGoal>> = _liveGoals.asStateFlow()

    private val _giftOverlayEvents = MutableStateFlow<List<LiveGiftOverlayEvent>>(emptyList())
    val giftOverlayEvents: StateFlow<List<LiveGiftOverlayEvent>> = _giftOverlayEvents.asStateFlow()

    private val _lastEndSummary = MutableStateFlow<LiveEndSummary?>(null)
    val lastEndSummary: StateFlow<LiveEndSummary?> = _lastEndSummary.asStateFlow()

    // Sound preferences
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    // Social Community State
    private val _publicUsers = MutableStateFlow(buildInitialPublicUsers())
    val publicUsers: StateFlow<List<DemoPublicUser>> = _publicUsers.asStateFlow()

    private val _friendRequests = MutableStateFlow(buildInitialFriendRequests())
    val friendRequests: StateFlow<List<DemoFriendRequest>> = _friendRequests.asStateFlow()

    private val _friendships = MutableStateFlow(buildInitialFriendships())
    val friendships: StateFlow<List<DemoFriendship>> = _friendships.asStateFlow()

    private val _conversations = MutableStateFlow(buildInitialConversations())
    val conversations: StateFlow<List<DemoConversation>> = _conversations.asStateFlow()

    private val _privateMessagesMap = MutableStateFlow<Map<String, List<DemoPrivateMessage>>>(buildInitialPrivateMessages())
    val privateMessagesMap: StateFlow<Map<String, List<DemoPrivateMessage>>> = _privateMessagesMap.asStateFlow()

    private val _socialNotifications = MutableStateFlow(buildInitialSocialNotifications())
    val socialNotifications: StateFlow<List<DemoSocialNotification>> = _socialNotifications.asStateFlow()

    init {
        syncFollowStateToCreators()
        syncActiveRoomsToCreators()
        syncSocialStateToUsers()
    }

    private fun syncFollowStateToCreators() {
        val followed = _followedCreatorIds.value
        _creators.value = _creators.value.map { creator ->
            creator.copy(isFollowing = followed.contains(creator.id))
        }
    }

    private fun syncActiveRoomsToCreators() {
        val activeRooms = _liveRooms.value.filter { it.isLive }
        _creators.value = _creators.value.map { creator ->
            val room = activeRooms.find { it.creatorId == creator.id }
            creator.copy(
                isLiveNow = room != null,
                activeRoomId = room?.id
            )
        }
    }

    private fun syncSocialStateToUsers() {
        val followed = _followedCreatorIds.value
        val friends = getFriendUserIds("current_user")
        val requests = _friendRequests.value

        _publicUsers.value = _publicUsers.value.map { user ->
            val isFollow = followed.contains(user.id)
            val isFr = friends.contains(user.id)
            val req = requests.find {
                (it.senderId == "current_user" && it.receiverId == user.id) ||
                (it.senderId == user.id && it.receiverId == "current_user")
            }
            user.copy(
                isFollowing = isFollow,
                isFriend = isFr,
                friendRequestStatus = req?.status
            )
        }
    }

    private fun getFriendUserIds(userId: String): Set<String> {
        return _friendships.value.flatMap {
            when {
                it.userId1 == userId -> listOf(it.userId2)
                it.userId2 == userId -> listOf(it.userId1)
                else -> emptyList()
            }
        }.toSet()
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
        val pending = (diamonds * 0.25).toInt()
        val available = diamonds - pending
        return DemoCreatorEarning(
            totalDiamondsReceived = diamonds,
            totalRupeesEarned = rupees,
            pendingDiamonds = pending,
            availableDiamonds = available
        )
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
        syncSocialStateToUsers()
    }

    fun toggleFollowCreator(creatorId: String, roomId: String? = null) {
        if (creatorId == "current_user") return
        val current = _followedCreatorIds.value.toMutableSet()
        val isNowFollowing = !current.contains(creatorId)
        if (isNowFollowing) {
            current.add(creatorId)
            addNotification(
                title = "New Follow",
                message = "You are now following creator",
                type = NotificationType.NEW_FOLLOWER,
                targetCreatorId = creatorId
            )
            if (roomId != null) {
                val creator = _creators.value.find { it.id == creatorId }
                val creatorName = creator?.name ?: "Host"
                val followMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    roomId = roomId,
                    senderName = "You",
                    message = "You started following $creatorName",
                    eventType = "USER_FOLLOWED"
                )
                addChatMessageInternal(roomId, followMsg)
            }
        } else {
            current.remove(creatorId)
        }
        saveFollowedCreatorIds(current)
    }

    fun removeGiftOverlayEvent(eventId: String) {
        _giftOverlayEvents.value = _giftOverlayEvents.value.filterNot { it.id == eventId }
    }

    fun onUserJoinedRoom(roomId: String, userName: String = "Dev") {
        val existingChats = _chatMessagesMap.value[roomId] ?: emptyList()
        val hasRecentJoin = existingChats.takeLast(10).any { it.eventType == "USER_JOINED" && it.senderName == userName }
        if (!hasRecentJoin) {
            val joinMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                senderName = userName,
                message = "joined the live",
                eventType = "USER_JOINED"
            )
            addChatMessageInternal(roomId, joinMsg)
        }
    }

    fun onShareLiveStream(roomId: String, senderName: String = "You") {
        val shareMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderName = senderName,
            message = "shared this live",
            eventType = "LIVE_SHARED"
        )
        addChatMessageInternal(roomId, shareMsg)
    }

    fun triggerHostFollowInvitation(roomId: String, hostName: String) {
        val inviteMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderName = hostName,
            message = "Follow $hostName for more lives!",
            eventType = "FOLLOW_INVITATION"
        )
        addChatMessageInternal(roomId, inviteMsg)
    }

    // Friend Requests & Relationships
    fun sendFriendRequest(targetUserId: String): Boolean {
        if (targetUserId == "current_user" || _blockedUserIds.value.contains(targetUserId)) return false
        val friends = getFriendUserIds("current_user")
        if (friends.contains(targetUserId)) return false

        val existing = _friendRequests.value.find {
            (it.senderId == "current_user" && it.receiverId == targetUserId) ||
            (it.senderId == targetUserId && it.receiverId == "current_user")
        }
        if (existing != null && existing.status == FriendRequestStatus.PENDING) return false

        val user = _publicUsers.value.find { it.id == targetUserId } ?: return false
        val req = DemoFriendRequest(
            id = UUID.randomUUID().toString(),
            senderId = "current_user",
            senderName = "You",
            senderProfession = "Site Engineer",
            receiverId = targetUserId,
            receiverName = user.name,
            status = FriendRequestStatus.PENDING
        )
        _friendRequests.value = listOf(req) + _friendRequests.value
        syncSocialStateToUsers()
        return true
    }

    fun acceptFriendRequest(requestId: String): Boolean {
        val req = _friendRequests.value.find { it.id == requestId } ?: return false
        if (req.status != FriendRequestStatus.PENDING) return false

        val otherUserId = if (req.senderId == "current_user") req.receiverId else req.senderId
        if (_blockedUserIds.value.contains(otherUserId)) return false

        _friendRequests.value = _friendRequests.value.map {
            if (it.id == requestId) it.copy(status = FriendRequestStatus.ACCEPTED) else it
        }

        val friendship = DemoFriendship(
            id = UUID.randomUUID().toString(),
            userId1 = req.senderId,
            userId2 = req.receiverId
        )
        _friendships.value = listOf(friendship) + _friendships.value

        // Create or get conversation
        ensureConversation(otherUserId)

        syncSocialStateToUsers()
        return true
    }

    fun declineFriendRequest(requestId: String) {
        _friendRequests.value = _friendRequests.value.map {
            if (it.id == requestId) it.copy(status = FriendRequestStatus.DECLINED) else it
        }
        syncSocialStateToUsers()
    }

    fun cancelFriendRequest(requestId: String) {
        _friendRequests.value = _friendRequests.value.filterNot { it.id == requestId }
        syncSocialStateToUsers()
    }

    fun unfriendUser(targetUserId: String) {
        _friendships.value = _friendships.value.filterNot {
            (it.userId1 == "current_user" && it.userId2 == targetUserId) ||
            (it.userId1 == targetUserId && it.userId2 == "current_user")
        }
        _friendRequests.value = _friendRequests.value.filterNot {
            (it.senderId == "current_user" && it.receiverId == targetUserId) ||
            (it.senderId == targetUserId && it.receiverId == "current_user")
        }
        syncSocialStateToUsers()
    }

    private fun ensureConversation(friendId: String): DemoConversation {
        val friend = _publicUsers.value.find { it.id == friendId }
        val existing = _conversations.value.find { it.friendId == friendId }
        if (existing != null) return existing

        val newConv = DemoConversation(
            id = "conv_" + UUID.randomUUID().toString().take(8),
            friendId = friendId,
            friendName = friend?.name ?: "Worker Friend",
            friendAvatar = friend?.avatar ?: "",
            friendProfession = friend?.profession ?: "Site Pro",
            lastMessage = "You are now connected as friends!",
            lastMessageTime = System.currentTimeMillis()
        )
        _conversations.value = listOf(newConv) + _conversations.value
        return newConv
    }

    fun sendPrivateMessage(friendId: String, text: String): Boolean {
        if (text.isBlank()) return false
        val friends = getFriendUserIds("current_user")
        if (!friends.contains(friendId) || _blockedUserIds.value.contains(friendId)) {
            return false
        }

        val conv = ensureConversation(friendId)
        val msg = DemoPrivateMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conv.id,
            senderId = "current_user",
            senderName = "You",
            receiverId = friendId,
            message = text.trim(),
            timestamp = System.currentTimeMillis(),
            isRead = true
        )

        val currentMap = _privateMessagesMap.value.toMutableMap()
        val list = currentMap[conv.id]?.toMutableList() ?: mutableListOf()
        list.add(msg)
        while (list.size > MAX_CHAT_MESSAGES) {
            list.removeAt(0)
        }
        currentMap[conv.id] = list
        _privateMessagesMap.value = currentMap

        // Update conversation summary
        _conversations.value = _conversations.value.map {
            if (it.id == conv.id) {
                it.copy(
                    lastMessage = msg.message,
                    lastMessageTime = msg.timestamp
                )
            } else it
        }

        return true
    }

    fun deletePrivateMessage(conversationId: String, messageId: String) {
        val currentMap = _privateMessagesMap.value.toMutableMap()
        val list = currentMap[conversationId]?.filterNot { it.id == messageId } ?: emptyList()
        currentMap[conversationId] = list
        _privateMessagesMap.value = currentMap
    }

    fun clearConversation(conversationId: String) {
        val currentMap = _privateMessagesMap.value.toMutableMap()
        currentMap[conversationId] = emptyList()
        _privateMessagesMap.value = currentMap
    }

    fun topUpDiamonds(amount: Int) {
        if (amount <= 0) return
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

    fun sendGift(roomId: String, gift: LiveGift, quantity: Int = 1): Boolean {
        if (quantity <= 0) return false
        val totalCost = gift.diamondCost * quantity
        val currentWallet = _wallet.value
        if (currentWallet.diamondBalance < totalCost) {
            return false
        }

        // Deduct diamonds
        val updatedWallet = currentWallet.copy(
            diamondBalance = currentWallet.diamondBalance - totalCost,
            totalSpent = currentWallet.totalSpent + totalCost
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
            diamondCost = totalCost,
            quantity = quantity
        )
        _giftTransactions.value = listOf(tx) + _giftTransactions.value

        // Update Room Diamond Earnings
        _liveRooms.value = _liveRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(diamondEarnings = room.diamondEarnings + totalCost)
            } else room
        }

        if (_activeRoom.value?.id == roomId) {
            val currentActive = _activeRoom.value
            if (currentActive != null) {
                _activeRoom.value = currentActive.copy(diamondEarnings = currentActive.diamondEarnings + totalCost)
            }
        }

        // Credit Creator Earnings
        val room = _liveRooms.value.find { it.id == roomId }
        if (room != null) {
            val currentEarning = _creatorEarning.value
            val newTotalDiamonds = currentEarning.totalDiamondsReceived + totalCost
            val newTotalRupees = newTotalDiamonds * RUPEES_PER_DIAMOND
            val newPending = (newTotalDiamonds * 0.25).toInt()
            saveCreatorEarning(currentEarning.copy(
                totalDiamondsReceived = newTotalDiamonds,
                totalRupeesEarned = newTotalRupees,
                pendingDiamonds = newPending,
                availableDiamonds = newTotalDiamonds - newPending
            ))
        }

        // Post Chat Announcement
        val quantityText = if (quantity > 1) " x$quantity" else ""
        val announcement = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderName = "You",
            message = "sent ${gift.name}$quantityText ($totalCost 💎)",
            isGiftAnnouncement = true,
            giftName = gift.name,
            giftDiamondCost = totalCost
        )
        addChatMessageInternal(roomId, announcement)

        // Trigger Gift Overlay Event
        val overlayEvent = LiveGiftOverlayEvent(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            giftName = gift.name,
            senderName = "You",
            quantity = quantity,
            comboCount = if (quantity >= 5) quantity else 1,
            diamondCost = totalCost
        )
        _giftOverlayEvents.value = listOf(overlayEvent) + _giftOverlayEvents.value

        // Update Goals
        updateGoalProgress(typeText = "GIFTS", amount = quantity)

        return true
    }

    fun sendHearts(roomId: String, count: Int = 1) {
        if (count <= 0) return
        _liveRooms.value = _liveRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(likeCount = room.likeCount + count)
            } else room
        }
        if (_activeRoom.value?.id == roomId) {
            val active = _activeRoom.value
            if (active != null) {
                _activeRoom.value = active.copy(likeCount = active.likeCount + count)
            }
        }
        updateGoalProgress(typeText = "HEARTS", amount = count)
    }

    fun sendChatMessage(roomId: String, messageText: String, senderName: String = "You") {
        if (messageText.isBlank()) return
        val room = _liveRooms.value.find { it.id == roomId }
        if (room != null && !room.isChatEnabled) return

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
        while (list.size > MAX_CHAT_MESSAGES) {
            list.removeAt(0)
        }
        currentMap[roomId] = list
        _chatMessagesMap.value = currentMap
    }

    fun pinChatMessage(roomId: String, messageId: String) {
        val currentMap = _chatMessagesMap.value.toMutableMap()
        val list = currentMap[roomId]?.map { msg ->
            if (msg.id == messageId) msg.copy(isPinned = true) else msg.copy(isPinned = false)
        } ?: emptyList()
        currentMap[roomId] = list
        _chatMessagesMap.value = currentMap

        val pinnedMsg = list.find { it.id == messageId }
        _liveRooms.value = _liveRooms.value.map { room ->
            if (room.id == roomId) room.copy(pinnedMessage = pinnedMsg) else room
        }
        if (_activeRoom.value?.id == roomId) {
            val active = _activeRoom.value
            if (active != null) {
                _activeRoom.value = active.copy(pinnedMessage = pinnedMsg)
            }
        }
    }

    fun unpinChatMessage(roomId: String) {
        val currentMap = _chatMessagesMap.value.toMutableMap()
        val list = currentMap[roomId]?.map { msg -> msg.copy(isPinned = false) } ?: emptyList()
        currentMap[roomId] = list
        _chatMessagesMap.value = currentMap

        _liveRooms.value = _liveRooms.value.map { room ->
            if (room.id == roomId) room.copy(pinnedMessage = null) else room
        }
        if (_activeRoom.value?.id == roomId) {
            val active = _activeRoom.value
            if (active != null) {
                _activeRoom.value = active.copy(pinnedMessage = null)
            }
        }
    }

    fun deleteChatMessage(roomId: String, messageId: String) {
        val currentMap = _chatMessagesMap.value.toMutableMap()
        val list = currentMap[roomId]?.filterNot { it.id == messageId } ?: emptyList()
        currentMap[roomId] = list
        _chatMessagesMap.value = currentMap
    }

    fun toggleChatEnabled(roomId: String) {
        _liveRooms.value = _liveRooms.value.map { room ->
            if (room.id == roomId) room.copy(isChatEnabled = !room.isChatEnabled) else room
        }
        if (_activeRoom.value?.id == roomId) {
            val active = _activeRoom.value
            if (active != null) {
                _activeRoom.value = active.copy(isChatEnabled = !active.isChatEnabled)
            }
        }
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
        tags: List<String>,
        description: String = "",
        isPublic: Boolean = true,
        allowGuestRequests: Boolean = true,
        showGifts: Boolean = true
    ): LiveRoom {
        val newRoom = LiveRoom(
            id = "room_" + UUID.randomUUID().toString().take(8),
            title = title.ifBlank { "Live Site Workday Discussion" },
            creatorId = "creator_me",
            creatorName = "You (Host)",
            creatorAvatar = "",
            creatorProfession = "Site Engineer Lead",
            category = category.ifBlank { "Site Safety" },
            type = type,
            viewerCount = 1,
            peakViewerCount = 1,
            diamondEarnings = 0,
            likeCount = 0,
            isLive = true,
            isHost = true,
            hostUserId = "creator_me",
            tags = tags.ifEmpty { listOf("LiveWork", "SiteSafety") },
            description = description,
            isPublic = isPublic,
            allowGuestRequests = allowGuestRequests,
            showGifts = showGifts
        )
        _liveRooms.value = listOf(newRoom) + _liveRooms.value
        _activeRoom.value = newRoom

        // Initialize Audio seats if audio
        if (type == LiveRoomType.AUDIO) {
            _audioSeatsMap.value = _audioSeatsMap.value.toMutableMap().apply {
                put(newRoom.id, buildDefaultAudioSeats(hostName = "You (Host)"))
            }
        }

        // Initial welcome chat message
        sendChatMessage(newRoom.id, "Welcome to your live stream! Attendees can comment and send gifts.", "System")

        // Notification
        addNotification(
            title = "Live Stream Started",
            message = "Your stream '${newRoom.title}' is now live!",
            type = NotificationType.LIVE_START,
            targetRoomId = newRoom.id
        )

        syncActiveRoomsToCreators()
        return newRoom
    }

    fun endLiveStream(roomId: String): LiveEndSummary? {
        val room = _liveRooms.value.find { it.id == roomId } ?: return null
        val durationSeconds = ((System.currentTimeMillis() - room.startTimeMillis) / 1000).coerceAtLeast(10)
        val rupeesEarned = room.diamondEarnings * RUPEES_PER_DIAMOND

        val summary = LiveEndSummary(
            roomId = room.id,
            title = room.title,
            creatorName = room.creatorName,
            durationSeconds = durationSeconds,
            peakViewers = (room.viewerCount * 1.5).toInt().coerceAtLeast(room.viewerCount),
            totalViewers = (room.viewerCount * 2.5).toInt().coerceAtLeast(room.viewerCount + 10),
            heartsReceived = room.likeCount,
            newFollowersCount = 12,
            giftsReceivedCount = (room.diamondEarnings / 20).coerceAtLeast(2),
            diamondsEarned = room.diamondEarnings,
            rupeesEarned = rupeesEarned
        )
        _lastEndSummary.value = summary

        _liveRooms.value = _liveRooms.value.map { r ->
            if (r.id == roomId) r.copy(isLive = false) else r
        }
        if (_activeRoom.value?.id == roomId) {
            _activeRoom.value = null
        }

        addNotification(
            title = "Live Session Ended",
            message = "Your stream ended. Total Diamonds earned: ${summary.diamondsEarned} 💎",
            type = NotificationType.LIVE_ENDED,
            targetRoomId = roomId
        )

        syncActiveRoomsToCreators()
        return summary
    }

    fun requestToJoinGuest(roomId: String, userName: String = "You"): GuestRequest {
        val req = GuestRequest(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            userId = "current_user",
            userName = userName,
            status = GuestRequestStatus.PENDING
        )
        val currentMap = _guestRequestsMap.value.toMutableMap()
        val list = currentMap[roomId]?.toMutableList() ?: mutableListOf()
        list.add(req)
        currentMap[roomId] = list
        _guestRequestsMap.value = currentMap
        return req
    }

    fun approveGuestRequest(roomId: String, requestId: String) {
        updateGuestRequestStatus(roomId, requestId, GuestRequestStatus.APPROVED)
    }

    fun rejectGuestRequest(roomId: String, requestId: String) {
        updateGuestRequestStatus(roomId, requestId, GuestRequestStatus.REJECTED)
    }

    private fun updateGuestRequestStatus(roomId: String, requestId: String, status: GuestRequestStatus) {
        val currentMap = _guestRequestsMap.value.toMutableMap()
        val list = currentMap[roomId]?.map { req ->
            if (req.id == requestId) req.copy(status = status) else req
        } ?: emptyList()
        currentMap[roomId] = list
        _guestRequestsMap.value = currentMap
    }

    fun occupyAudioSeat(roomId: String, seatIndex: Int, userId: String, userName: String) {
        val currentMap = _audioSeatsMap.value.toMutableMap()
        val seats = currentMap[roomId]?.toMutableList() ?: buildDefaultAudioSeats().toMutableList()
        if (seatIndex in seats.indices) {
            seats[seatIndex] = seats[seatIndex].copy(userId = userId, userName = userName, isSpeaking = true)
        }
        currentMap[roomId] = seats
        _audioSeatsMap.value = currentMap
    }

    fun leaveAudioSeat(roomId: String, seatIndex: Int) {
        val currentMap = _audioSeatsMap.value.toMutableMap()
        val seats = currentMap[roomId]?.toMutableList() ?: return
        if (seatIndex in seats.indices && !seats[seatIndex].isHostSeat) {
            seats[seatIndex] = seats[seatIndex].copy(userId = null, userName = null, isSpeaking = false)
        }
        currentMap[roomId] = seats
        _audioSeatsMap.value = currentMap
    }

    fun toggleAudioSeatMute(roomId: String, seatIndex: Int) {
        val currentMap = _audioSeatsMap.value.toMutableMap()
        val seats = currentMap[roomId]?.toMutableList() ?: return
        if (seatIndex in seats.indices) {
            seats[seatIndex] = seats[seatIndex].copy(isMuted = !seats[seatIndex].isMuted)
        }
        currentMap[roomId] = seats
        _audioSeatsMap.value = currentMap
    }

    fun toggleLockSeat(roomId: String, seatIndex: Int) {
        val currentMap = _audioSeatsMap.value.toMutableMap()
        val seats = currentMap[roomId]?.toMutableList() ?: return
        if (seatIndex in seats.indices) {
            seats[seatIndex] = seats[seatIndex].copy(isLocked = !seats[seatIndex].isLocked)
        }
        currentMap[roomId] = seats
        _audioSeatsMap.value = currentMap
    }

    fun blockUser(userId: String) {
        if (userId == "current_user") return
        _blockedUserIds.value = _blockedUserIds.value + userId
        unfriendUser(userId)
    }

    fun unblockUser(userId: String) {
        _blockedUserIds.value = _blockedUserIds.value - userId
    }

    fun muteUserInRoom(roomId: String, userId: String) {
        val map = _mutedUserIdsMap.value.toMutableMap()
        val currentMuted = map[roomId]?.toMutableSet() ?: mutableSetOf()
        currentMuted.add(userId)
        map[roomId] = currentMuted
        _mutedUserIdsMap.value = map
    }

    fun addNotification(title: String, message: String, type: NotificationType, targetRoomId: String? = null, targetCreatorId: String? = null) {
        val notif = DemoNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            targetRoomId = targetRoomId,
            targetCreatorId = targetCreatorId
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map { notif ->
            if (notif.id == id) notif.copy(isRead = true) else notif
        }
    }

    fun toggleSoundEnabled() {
        _soundEnabled.value = !_soundEnabled.value
    }

    fun toggleHapticsEnabled() {
        _hapticsEnabled.value = !_hapticsEnabled.value
    }

    private fun updateGoalProgress(typeText: String, amount: Int) {
        _liveGoals.value = _liveGoals.value.map { goal ->
            if (goal.unitText.contains(typeText, ignoreCase = true) && !goal.isCompleted) {
                val newCurrent = goal.current + amount
                goal.copy(
                    current = newCurrent,
                    isCompleted = newCurrent >= goal.target
                )
            } else goal
        }
    }

    fun cashoutEarnings(diamondsToCashout: Int): Boolean {
        val currentEarning = _creatorEarning.value
        if (diamondsToCashout <= 0 || currentEarning.availableDiamonds < diamondsToCashout) {
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
            availableDiamonds = currentEarning.availableDiamonds - diamondsToCashout,
            cashoutHistory = listOf(newRecord) + currentEarning.cashoutHistory
        )
        saveCreatorEarning(updated)
        return true
    }

    fun updateDemoDataOverrides(
        diamonds: Int? = null,
        creatorDiamonds: Int? = null,
        followersCount: Int? = null,
        viewerCount: Int? = null,
        likeCount: Int? = null
    ) {
        diamonds?.let {
            saveWallet(_wallet.value.copy(diamondBalance = it))
        }
        creatorDiamonds?.let {
            saveCreatorEarning(_creatorEarning.value.copy(
                totalDiamondsReceived = it,
                totalRupeesEarned = it * RUPEES_PER_DIAMOND,
                availableDiamonds = (it * 0.75).toInt(),
                pendingDiamonds = (it * 0.25).toInt()
            ))
        }
        followersCount?.let { count ->
            _creators.value = _creators.value.mapIndexed { idx, creator ->
                if (idx == 0) creator.copy(followerCount = count) else creator
            }
        }
        viewerCount?.let { count ->
            _liveRooms.value = _liveRooms.value.map { room ->
                room.copy(viewerCount = count)
            }
        }
        likeCount?.let { count ->
            _liveRooms.value = _liveRooms.value.map { room ->
                room.copy(likeCount = count)
            }
        }
    }

    fun resetSocialDemoData() {
        _friendRequests.value = buildInitialFriendRequests()
        _friendships.value = buildInitialFriendships()
        _conversations.value = buildInitialConversations()
        _privateMessagesMap.value = buildInitialPrivateMessages()
        _blockedUserIds.value = emptySet()
        _socialNotifications.value = buildInitialSocialNotifications()
        syncSocialStateToUsers()
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
        _guestRequestsMap.value = buildInitialGuestRequests()
        _audioSeatsMap.value = buildInitialAudioSeats()
        _notifications.value = buildInitialNotifications()
        _liveGoals.value = buildInitialLiveGoals()
        _lastEndSummary.value = null
        resetSocialDemoData()
        syncFollowStateToCreators()
        syncActiveRoomsToCreators()
    }

    companion object {
        const val PREFS_NAME = "workdaily_live_demo_prefs"
        const val KEY_DIAMOND_BALANCE = "diamond_balance"
        const val KEY_TOTAL_PURCHASED = "total_purchased"
        const val KEY_TOTAL_SPENT = "total_spent"
        const val KEY_CREATOR_DIAMONDS = "creator_diamonds"
        const val KEY_FOLLOWED_CREATORS = "followed_creators"
        const val RUPEES_PER_DIAMOND = 0.50
        const val MAX_CHAT_MESSAGES = 10

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
            LiveCreator("creator_1", "Rajesh Sharma", "", "Senior Civil Site Engineer | 12 yrs site experience", "Civil Engineer", 1420, 240, true, 3400, "Site Safety", true, true, "room_1"),
            LiveCreator("creator_2", "Amit Kumar", "", "Heavy Equipment Specialist & Operator", "Machinery Operator", 2890, 180, true, 5800, "Machinery", true, true, "room_2"),
            LiveCreator("creator_3", "Vikram Singh", "", "Master Mason & Structural Concrete Artisan", "Master Mason", 950, 95, false, 1900, "Masonry", true, true, "room_3"),
            LiveCreator("creator_4", "Pooja Verma", "", "Safety Auditor & Site Compliance Inspector", "Safety Auditor", 3100, 410, false, 8200, "Engineering", true, true, "room_4"),
            LiveCreator("creator_5", "Suresh Patel", "", "Labor Contractor & Payroll Lead", "Contractor", 1850, 310, false, 2600, "Contractor", true, true, "room_5")
        )

        private fun buildInitialPublicUsers(): List<DemoPublicUser> = listOf(
            DemoPublicUser("creator_1", "Rajesh Sharma", "", "Senior Civil Site Engineer | 12 yrs site experience", "Civil Engineer", 1420, 240, 85, isFollowing = true, isFriend = true, isVerified = true, isLiveNow = true, activeRoomId = "room_1"),
            DemoPublicUser("creator_2", "Amit Kumar", "", "Heavy Equipment Specialist & Operator", "Machinery Operator", 2890, 180, 110, isFollowing = true, isFriend = false, isVerified = true, isLiveNow = true, activeRoomId = "room_2"),
            DemoPublicUser("creator_3", "Vikram Singh", "", "Master Mason & Structural Concrete Artisan", "Master Mason", 950, 95, 42, isFollowing = false, isFriend = false, isVerified = true, isLiveNow = true, activeRoomId = "room_3"),
            DemoPublicUser("creator_4", "Pooja Verma", "", "Safety Auditor & Site Compliance Inspector", "Safety Auditor", 3100, 410, 150, isFollowing = false, isFriend = false, isVerified = true, isLiveNow = true, activeRoomId = "room_4"),
            DemoPublicUser("creator_5", "Suresh Patel", "", "Labor Contractor & Payroll Lead", "Contractor", 1850, 310, 90, isFollowing = false, isFriend = false, isVerified = true, isLiveNow = true, activeRoomId = "room_5"),
            DemoPublicUser("user_101", "Sunil Mason", "", "Plaster & Brickwork Expert", "Site Mason", 340, 50, 20, isFollowing = false, isFriend = false, friendRequestStatus = FriendRequestStatus.PENDING),
            DemoPublicUser("user_102", "Deepak Plumber", "", "Commercial Plumbing Specialist", "Plumbing Lead", 510, 80, 35, isFollowing = false, isFriend = false)
        )

        private fun buildInitialRoomParticipants(): Map<String, List<RoomParticipant>> = mapOf(
            "room_1" to listOf(
                RoomParticipant("creator_1", "Rajesh Sharma (Host)", "", "Civil Site Engineer", 1420, isHost = true, isLiveNow = true),
                RoomParticipant("user_101", "Sunil Mason", "", "Site Mason", 340),
                RoomParticipant("user_102", "Deepak Plumber", "", "Plumbing Lead", 510),
                RoomParticipant("user_103", "Rohan Electrician", "", "Electrical Lead", 280),
                RoomParticipant("user_104", "Anil Contractor", "", "Project Contractor", 890)
            ),
            "room_2" to listOf(
                RoomParticipant("creator_2", "Amit Kumar (Host)", "", "Machinery Operator", 2890, isHost = true, isLiveNow = true),
                RoomParticipant("user_105", "Suresh Crane Operator", "", "Heavy Equipment", 420),
                RoomParticipant("user_106", "Pankaj Inspector", "", "Safety Auditor", 670)
            ),
            "room_3" to listOf(
                RoomParticipant("creator_3", "Vikram Singh (Host)", "", "Master Mason", 950, isHost = true, isLiveNow = true),
                RoomParticipant("user_101", "Sunil Mason", "", "Site Mason", 340),
                RoomParticipant("user_107", "Dinesh Concrete Lead", "", "Concrete Worker", 190)
            ),
            "room_4" to listOf(
                RoomParticipant("creator_4", "Pooja Verma (Host)", "", "Safety Auditor", 3100, isHost = true, isLiveNow = true),
                RoomParticipant("user_108", "Karan Site Supervisor", "", "Site Supervisor", 530),
                RoomParticipant("user_109", "Vijay Surveyor", "", "Civil Surveyor", 310)
            ),
            "room_5" to listOf(
                RoomParticipant("creator_5", "Suresh Patel (Host)", "", "Contractor Lead", 1850, isHost = true, isLiveNow = true),
                RoomParticipant("user_110", "Ganesh Builder", "", "Sub Contractor", 720),
                RoomParticipant("user_111", "Manoj Transport Manager", "", "Logistics Lead", 410)
            )
        )

        private fun buildInitialFriendRequests(): List<DemoFriendRequest> = listOf(
            DemoFriendRequest("freq_1", "user_101", "Sunil Mason", "", "Site Mason", "current_user", "You", FriendRequestStatus.PENDING)
        )

        private fun buildInitialFriendships(): List<DemoFriendship> = listOf(
            DemoFriendship("fship_1", "current_user", "creator_1")
        )

        private fun buildInitialConversations(): List<DemoConversation> = listOf(
            DemoConversation("conv_1", "creator_1", "Rajesh Sharma", "", "Civil Engineer", "Thanks for joining the civil foundation live stream!", System.currentTimeMillis() - 1800000, 0)
        )

        private fun buildInitialPrivateMessages(): Map<String, List<DemoPrivateMessage>> = mapOf(
            "conv_1" to listOf(
                DemoPrivateMessage("pm_1", "conv_1", "creator_1", "Rajesh Sharma", "current_user", "Hello! Great to connect with fellow site engineers.", System.currentTimeMillis() - 3600000, true),
                DemoPrivateMessage("pm_2", "conv_1", "current_user", "You", "creator_1", "Thanks Rajesh sir! Your concrete mix ratio tip was very helpful.", System.currentTimeMillis() - 2700000, true),
                DemoPrivateMessage("pm_3", "conv_1", "creator_1", "Rajesh Sharma", "current_user", "Thanks for joining the civil foundation live stream!", System.currentTimeMillis() - 1800000, true)
            )
        )

        private fun buildInitialSocialNotifications(): List<DemoSocialNotification> = listOf(
            DemoSocialNotification("snotif_1", "New Friend Request", "Sunil Mason sent you a friend request.", NotificationType.FRIEND_REQUEST, System.currentTimeMillis() - 360000, false, targetUserId = "user_101")
        )

        private fun buildInitialLiveRooms(): List<LiveRoom> = listOf(
            LiveRoom(
                id = "room_1",
                title = "Civil Site Foundation & Concrete Pouring",
                creatorId = "creator_1",
                creatorName = "Rajesh Sharma",
                creatorAvatar = "",
                creatorProfession = "Senior Site Engineer",
                category = "Site Safety",
                type = LiveRoomType.VIDEO,
                viewerCount = 348,
                peakViewerCount = 420,
                diamondEarnings = 1250,
                likeCount = 890,
                isLive = true,
                tags = listOf("Foundation", "Concrete", "CivilSite"),
                description = "Live inspection of concrete mix ratio and slab foundation pouring."
            ),
            LiveRoom(
                id = "room_2",
                title = "Excavator & Heavy Machinery Safety Demo",
                creatorId = "creator_2",
                creatorName = "Amit Kumar",
                creatorAvatar = "",
                creatorProfession = "Machinery Specialist",
                category = "Machinery",
                type = LiveRoomType.VIDEO,
                viewerCount = 512,
                peakViewerCount = 610,
                diamondEarnings = 2400,
                likeCount = 1420,
                isLive = true,
                tags = listOf("Excavator", "Machinery", "Operator"),
                description = "Demonstration of hydraulic earthmoving controls and safety perimeters."
            ),
            LiveRoom(
                id = "room_3",
                title = "Masonry Q&A: Plastering, Bricks & Tile Fitting",
                creatorId = "creator_3",
                creatorName = "Vikram Singh",
                creatorAvatar = "",
                creatorProfession = "Master Mason",
                category = "Masonry",
                type = LiveRoomType.AUDIO,
                viewerCount = 184,
                peakViewerCount = 210,
                diamondEarnings = 680,
                likeCount = 450,
                isLive = true,
                tags = listOf("Masonry", "Plaster", "AudioStage"),
                description = "Open voice Q&A on finishing, mortar ratios, and tile alignment."
            ),
            LiveRoom(
                id = "room_4",
                title = "Worker Safety Guidelines & Helmet Standards",
                creatorId = "creator_4",
                creatorName = "Pooja Verma",
                creatorAvatar = "",
                creatorProfession = "Safety Auditor",
                category = "Engineering",
                type = LiveRoomType.VIDEO,
                viewerCount = 420,
                peakViewerCount = 500,
                diamondEarnings = 3100,
                likeCount = 2100,
                isLive = true,
                tags = listOf("SafetyFirst", "PPE", "Inspection"),
                description = "Comprehensive walk-through of PPE equipment and site safety compliance."
            ),
            LiveRoom(
                id = "room_5",
                title = "Daily Wage Rates & Labor Attendance Discussion",
                creatorId = "creator_5",
                creatorName = "Suresh Patel",
                creatorAvatar = "",
                creatorProfession = "Contractor Lead",
                category = "Contractor",
                type = LiveRoomType.AUDIO,
                viewerCount = 275,
                peakViewerCount = 310,
                diamondEarnings = 1450,
                likeCount = 680,
                isLive = true,
                tags = listOf("WageRate", "Contractor", "AudioDiscussion"),
                description = "Group voice panel discussing daily wage calculations and attendance logs."
            )
        )

        private fun buildInitialChatMessages(): Map<String, List<ChatMessage>> = mapOf(
            "room_1" to listOf(
                ChatMessage("c1", "room_1", "u101", "Sunil (Mason)", "", "How many bags of cement per cubic meter for M25?"),
                ChatMessage("c2", "room_1", "creator_1", "Rajesh Sharma", "", "For M25, use 1:1:2 ratio, about 11 bags per cum."),
                ChatMessage("c3", "room_1", "u102", "Ramesh", "", "Great live session sir!")
            ),
            "room_2" to listOf(
                ChatMessage("c4", "room_2", "u103", "Deepak", "", "What is the hourly hydraulic pressure check interval?"),
                ChatMessage("c5", "room_2", "creator_2", "Amit Kumar", "", "Check oil level daily before start, pressure monthly.")
            ),
            "room_3" to listOf(
                ChatMessage("c6", "room_3", "u104", "Rohan", "", "Audio is clear! Voice stage working perfectly.")
            )
        )

        private fun buildInitialGuestRequests(): Map<String, List<GuestRequest>> = mapOf(
            "room_3" to listOf(
                GuestRequest("g1", "room_3", "u201", "Sunil Plumber", "", "Plumbing Master", GuestRequestStatus.PENDING),
                GuestRequest("g2", "room_3", "u202", "Dinesh Mason", "", "Site Mason", GuestRequestStatus.APPROVED)
            )
        )

        private fun buildInitialAudioSeats(): Map<String, List<AudioSeat>> = mapOf(
            "room_3" to listOf(
                AudioSeat(0, "creator_3", "Vikram Singh (Host)", "", isHostSeat = true, isSpeaking = true),
                AudioSeat(1, "u202", "Dinesh Mason", "", isHostSeat = false, isSpeaking = false),
                AudioSeat(2, null, null, "", isHostSeat = false),
                AudioSeat(3, null, null, "", isHostSeat = false),
                AudioSeat(4, null, null, "", isHostSeat = false),
                AudioSeat(5, null, null, "", isHostSeat = false)
            ),
            "room_5" to buildDefaultAudioSeats(hostName = "Suresh Patel (Host)")
        )

        private fun buildDefaultAudioSeats(hostName: String = "Host"): List<AudioSeat> = listOf(
            AudioSeat(0, "host", hostName, "", isHostSeat = true, isSpeaking = true),
            AudioSeat(1, null, null, "", isHostSeat = false),
            AudioSeat(2, null, null, "", isHostSeat = false),
            AudioSeat(3, null, null, "", isHostSeat = false),
            AudioSeat(4, null, null, "", isHostSeat = false),
            AudioSeat(5, null, null, "", isHostSeat = false)
        )

        private fun buildInitialNotifications(): List<DemoNotification> = listOf(
            DemoNotification("n1", "Rajesh Sharma started LIVE", "Civil Site Foundation & Concrete Pouring is live now!", NotificationType.LIVE_START, System.currentTimeMillis() - 300000, false, "room_1", "creator_1"),
            DemoNotification("n2", "New Supporter", "Amit Kumar sent you a Power Drill gift!", NotificationType.GIFT_RECEIVED, System.currentTimeMillis() - 1200000, true)
        )

        private fun buildInitialLiveGoals(): List<LiveGoal> = listOf(
            LiveGoal("goal_1", "Heart Goal", 1000, 890, "HEARTS"),
            LiveGoal("goal_2", "Gifts Goal", 20, 12, "GIFTS"),
            LiveGoal("goal_3", "Followers Goal", 20, 14, "FOLLOWERS")
        )
    }
}
