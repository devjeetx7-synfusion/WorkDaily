package com.synfusion.attendance.live.model

enum class LiveRoomType {
    AUDIO,
    VIDEO
}

enum class GuestRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

enum class NotificationType {
    LIVE_START,
    NEW_FOLLOWER,
    GIFT_RECEIVED,
    GUEST_APPROVED,
    LIVE_ENDED
}

data class LiveRoom(
    val id: String,
    val title: String,
    val creatorId: String,
    val creatorName: String,
    val creatorAvatar: String = "",
    val creatorProfession: String = "Site Engineer",
    val category: String = "Site Safety",
    val type: LiveRoomType = LiveRoomType.VIDEO,
    val viewerCount: Int = 0,
    val peakViewerCount: Int = 0,
    val diamondEarnings: Int = 0,
    val likeCount: Int = 0,
    val isLive: Boolean = true,
    val isHost: Boolean = false,
    val hostUserId: String = "creator_1",
    val streamUrl: String? = null,
    val tags: List<String> = emptyList(),
    val statusText: String = "LIVE",
    val description: String = "",
    val isPublic: Boolean = true,
    val allowGuestRequests: Boolean = true,
    val showGifts: Boolean = true,
    val isChatEnabled: Boolean = true,
    val pinnedMessage: ChatMessage? = null,
    val startTimeMillis: Long = System.currentTimeMillis()
)

data class LiveCreator(
    val id: String,
    val name: String,
    val avatar: String = "",
    val bio: String = "",
    val profession: String = "Site Pro",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val totalEarnings: Int = 0,
    val roomCategory: String = "Site Safety",
    val isVerified: Boolean = true,
    val isLiveNow: Boolean = false,
    val activeRoomId: String? = null
)

data class LiveGift(
    val id: String,
    val name: String,
    val diamondCost: Int,
    val iconName: String,
    val category: String = "Site Tools",
    val description: String = ""
)

data class GiftTransaction(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val giftId: String,
    val giftName: String,
    val diamondCost: Int,
    val quantity: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class DemoWallet(
    val diamondBalance: Int = 500,
    val totalPurchased: Int = 500,
    val totalSpent: Int = 0
)

data class CashoutRecord(
    val id: String,
    val amountRupees: Double,
    val diamondsConverted: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED"
)

data class DemoCreatorEarning(
    val totalDiamondsReceived: Int = 1200,
    val totalRupeesEarned: Double = 600.0, // 1 Diamond = ₹0.50
    val pendingDiamonds: Int = 300,
    val availableDiamonds: Int = 900,
    val cashoutHistory: List<CashoutRecord> = emptyList()
)

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderId: String = "user",
    val senderName: String,
    val senderAvatar: String = "",
    val message: String,
    val isGiftAnnouncement: Boolean = false,
    val giftName: String? = null,
    val giftDiamondCost: Int? = null,
    val isSystemMessage: Boolean = false,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class GuestRequest(
    val id: String,
    val roomId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String = "",
    val userProfession: String = "Site Worker",
    val status: GuestRequestStatus = GuestRequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveEndSummary(
    val roomId: String,
    val title: String,
    val creatorName: String,
    val durationSeconds: Long,
    val peakViewers: Int,
    val totalViewers: Int,
    val heartsReceived: Int,
    val newFollowersCount: Int,
    val giftsReceivedCount: Int,
    val diamondsEarned: Int,
    val rupeesEarned: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class DemoNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetRoomId: String? = null,
    val targetCreatorId: String? = null
)

data class LiveGoal(
    val id: String,
    val title: String,
    val target: Int,
    val current: Int,
    val unitText: String,
    val isCompleted: Boolean = false
)

data class LiveGiftOverlayEvent(
    val id: String,
    val roomId: String,
    val giftName: String,
    val senderName: String,
    val quantity: Int = 1,
    val comboCount: Int = 1,
    val diamondCost: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class AudioSeat(
    val seatIndex: Int,
    val userId: String? = null,
    val userName: String? = null,
    val userAvatar: String = "",
    val isHostSeat: Boolean = false,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false
)
