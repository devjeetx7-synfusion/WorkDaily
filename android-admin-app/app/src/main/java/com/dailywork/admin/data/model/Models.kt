package com.dailywork.admin.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "", // admin, contractor, personal
    val isBlocked: Boolean = false,
    val isPremium: Boolean = false,
    val lastActive: Long = 0L,
    val createdAt: Long = 0L,
    val blockInfo: BlockInfo? = null,
    val premiumInfo: PremiumInfo? = null,
    val fcmToken: String = ""
)

data class BlockInfo(
    val reason: String = "",
    val blockedAt: Long = 0L,
    val blockedBy: String = ""
)

data class PremiumInfo(
    val expiryDate: Long = 0L,
    val activatedAt: Long = 0L,
    val activatedBy: String = ""
)

data class AppConfig(
    val languageEnabled: Boolean = true,
    val languageActivityEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val roleUiEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val registrationEnabled: Boolean = true,
    val premiumPurchaseEnabled: Boolean = true
)

data class AdminStats(
    val totalUsers: Int = 0,
    val contractorCount: Int = 0,
    val personalCount: Int = 0,
    val blockedCount: Int = 0,
    val premiumCount: Int = 0,
    val onlineCount: Int = 0,
    val totalJobs: Int = 0
)

data class Report(
    val id: String = "",
    val reportedUserId: String = "",
    val reportedBy: String = "",
    val reason: String = "",
    val timestamp: Long = 0L,
    val status: String = "pending" // pending, ignored, resolved
)

data class AdminLog(
    val id: String = "",
    val adminId: String = "",
    val action: String = "",
    val targetId: String = "",
    val timestamp: Long = 0L,
    val details: String = ""
)
