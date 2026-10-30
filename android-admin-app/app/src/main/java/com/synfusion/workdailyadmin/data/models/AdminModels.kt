package com.synfusion.workdailyadmin.data.models

enum class AdminRole(val rank: Int) {
    SUPER_ADMIN(5),
    ADMIN(4),
    MODERATOR(3),
    SUPPORT(2),
    ANALYST(1);

    companion object {
        fun fromString(value: String?): AdminRole = when (value?.uppercase()) {
            "SUPER_ADMIN" -> SUPER_ADMIN
            "ADMIN" -> ADMIN
            "MODERATOR" -> MODERATOR
            "SUPPORT" -> SUPPORT
            else -> ANALYST
        }
    }
}

data class UserRestrictions(
    val chatRestricted: Boolean = false,
    val liveRestricted: Boolean = false,
    val postingRestricted: Boolean = false
)

data class UserSummary(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val username: String = "",
    val accountStatus: String = "ACTIVE",
    val isVerified: Boolean = false,
    val countryCode: String = "IN",
    val createdAt: Long = 0,
    val restrictions: UserRestrictions = UserRestrictions()
)

data class UserNoteEntry(
    val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

data class UserDetails(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val username: String = "",
    val gender: String = "Male",
    val countryCode: String = "IN",
    val currencyCode: String = "INR",
    val accountStatus: String = "ACTIVE",
    val isVerified: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val restrictions: UserRestrictions = UserRestrictions(),
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val bio: String = "",
    val profession: String = "",
    val availableCoins: Int = 0,
    val lifetimeEarned: Int = 0,
    val spentCoins: Int = 0,
    val currentStreak: Int = 0,
    val referralCode: String = "",
    val notes: List<UserNoteEntry> = emptyList(),
    val reportsHistory: List<ReportItem> = emptyList(),
    val liveRoomsHistory: List<LiveRoomItem> = emptyList(),
    val auditHistory: List<AdminAuditLog> = emptyList()
)

data class DashboardStats(
    val totalUsers: Int = 0,
    val suspendedUsers: Int = 0,
    val bannedUsers: Int = 0,
    val verifiedUsers: Int = 0,
    val activeLiveRooms: Int = 0,
    val pendingReports: Int = 0,
    val totalCoinsIssued: Int = 0,
    val totalCoinsSpent: Int = 0,
    val pendingRedemptionsCount: Int = 0,
    val minSupportedVersionCode: Int = 1,
    val latestVersionCode: Int = 1,
    val maintenanceMode: Boolean = false,
    val systemStatus: String = "OPERATIONAL",
    val timestamp: Long = System.currentTimeMillis()
)

data class ReportItem(
    val id: String = "",
    val reporterUid: String = "",
    val targetUid: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: Long = 0,
    val status: String = "PENDING",
    val assignedAdminUid: String = "",
    val priority: String = "MEDIUM",
    val internalNotes: String = "",
    val resolutionReason: String = "",
    val resolvedAt: Long = 0
)

data class LiveRoomItem(
    val roomId: String = "",
    val hostUserId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val type: String = "VIDEO",
    val isLive: Boolean = false,
    val isPublic: Boolean = true,
    val statusText: String = "",
    val createdAtMillis: Long = 0,
    val endedAtMillis: Long? = null,
    val endedByAdminUid: String? = null,
    val endReason: String? = null
)

data class RedemptionRecord(
    val id: String = "",
    val targetUid: String = "",
    val optionTitle: String = "",
    val monetaryValue: String = "",
    val coinsSpent: Int = 0,
    val timestamp: String = "",
    val status: String = "PENDING",
    val referenceCode: String = "",
    val processedByAdminUid: String = "",
    val notes: String = ""
)

data class NotificationCampaign(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetAudience: String = "ALL",
    val status: String = "COMPLETED",
    val sentAt: Long = 0,
    val createdByAdminUid: String = "",
    val createdAt: Long = 0
)

data class AppConfigData(
    val minSupportedVersionCode: Int = 1,
    val latestVersionCode: Int = 1,
    val updateAnnouncementTitle: String = "Update Available",
    val updateAnnouncementMessage: String = "Please update WorkDaily.",
    val forceUpdate: Boolean = false,
    val maintenanceMode: Boolean = false,
    val supportWhatsappUrl: String = "",
    val supportEmail: String = "workdaily.synfusion@gmail.com",
    val shareMessage: String = "Download WorkDaily!",
    val announcementBannerText: String = "",
    val announcementBannerUrl: String = "",
    val isAnnouncementActive: Boolean = false,
    val liveEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    val rewardsEnabled: Boolean = true
)

data class AdminTeamMember(
    val uid: String = "",
    val email: String = "",
    val role: AdminRole = AdminRole.ANALYST,
    val assignedBy: String = "",
    val assignedAt: Long = 0
)

data class AdminAuditLog(
    val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reason: String = "",
    val detailsMap: Map<String, Any> = emptyMap(),
    val timestamp: Long = 0
)

data class SupportTicket(
    val id: String = "",
    val userUid: String = "",
    val subject: String = "",
    val description: String = "",
    val category: String = "GENERAL",
    val status: String = "OPEN",
    val priority: String = "MEDIUM",
    val assignedAdminUid: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class SystemHealthData(
    val firebaseFunctionsStatus: String = "HEALTHY",
    val fcmDeliveryRate: String = "99.8%",
    val firestoreStatus: String = "HEALTHY",
    val liveKitStatus: String = "HEALTHY",
    val errorTrends24h: Map<String, Int> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
