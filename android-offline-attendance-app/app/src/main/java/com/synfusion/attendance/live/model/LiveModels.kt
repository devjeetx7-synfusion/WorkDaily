package com.synfusion.attendance.live.model

enum class LiveRoomType {
    AUDIO,
    VIDEO
}

data class LiveRoom(
    val id: String,
    val title: String,
    val creatorId: String,
    val creatorName: String,
    val creatorAvatar: String,
    val category: String,
    val type: LiveRoomType,
    val viewerCount: Int = 0,
    val diamondEarnings: Int = 0,
    val isLive: Boolean = true,
    val streamUrl: String? = null,
    val tags: List<String> = emptyList(),
    val statusText: String = "LIVE"
)

data class LiveCreator(
    val id: String,
    val name: String,
    val avatar: String,
    val bio: String,
    val followerCount: Int = 0,
    val isFollowing: Boolean = false,
    val totalEarnings: Int = 0,
    val roomCategory: String = "Site Safety",
    val isVerified: Boolean = true
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
    val cashoutHistory: List<CashoutRecord> = emptyList()
)

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val message: String,
    val isGiftAnnouncement: Boolean = false,
    val giftName: String? = null,
    val giftDiamondCost: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
