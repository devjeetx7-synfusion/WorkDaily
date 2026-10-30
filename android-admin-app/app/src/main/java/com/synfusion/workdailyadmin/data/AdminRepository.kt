package com.synfusion.workdailyadmin.data

import com.google.firebase.functions.FirebaseFunctions
import com.synfusion.workdailyadmin.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}

class AdminRepository(
    private val functionsSupplier: () -> FirebaseFunctions = { FirebaseFunctions.getInstance() }
) {
    private val functions get() = functionsSupplier()

    @Suppress("UNCHECKED_CAST")
    suspend fun getDashboardStats(): Result<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            val res = functions.getHttpsCallable("adminGetDashboardStats").call().await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val stats = DashboardStats(
                totalUsers = (data["totalUsers"] as? Number)?.toInt() ?: 0,
                suspendedUsers = (data["suspendedUsers"] as? Number)?.toInt() ?: 0,
                bannedUsers = (data["bannedUsers"] as? Number)?.toInt() ?: 0,
                verifiedUsers = (data["verifiedUsers"] as? Number)?.toInt() ?: 0,
                activeLiveRooms = (data["activeLiveRooms"] as? Number)?.toInt() ?: 0,
                pendingReports = (data["pendingReports"] as? Number)?.toInt() ?: 0,
                totalCoinsIssued = (data["totalCoinsIssued"] as? Number)?.toInt() ?: 0,
                totalCoinsSpent = (data["totalCoinsSpent"] as? Number)?.toInt() ?: 0,
                pendingRedemptionsCount = (data["pendingRedemptionsCount"] as? Number)?.toInt() ?: 0,
                systemStatus = (data["systemStatus"] as? String) ?: "OPERATIONAL"
            )
            Result.Success(stats)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch dashboard stats", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getUsers(
        searchQuery: String = "",
        accountStatus: String? = null,
        limit: Int = 20
    ): Result<List<UserSummary>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf<String, Any>("limit" to limit)
            if (searchQuery.isNotBlank()) params["searchQuery"] = searchQuery
            if (!accountStatus.isNullOrBlank()) params["accountStatus"] = accountStatus

            val res = functions.getHttpsCallable("adminGetUsers").call(params).await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val rawList = data["users"] as? List<Map<String, Any>> ?: emptyList()

            val users = rawList.map { m ->
                val restrMap = m["restrictions"] as? Map<String, Any> ?: emptyMap()
                UserSummary(
                    uid = (m["uid"] as? String) ?: "",
                    displayName = (m["displayName"] as? String) ?: "Worker",
                    email = (m["email"] as? String) ?: "",
                    phoneNumber = (m["phoneNumber"] as? String) ?: "",
                    photoUrl = (m["photoUrl"] as? String) ?: "",
                    username = (m["username"] as? String) ?: "",
                    accountStatus = (m["accountStatus"] as? String) ?: "ACTIVE",
                    isVerified = (m["isVerified"] as? Boolean) ?: false,
                    countryCode = (m["countryCode"] as? String) ?: "IN",
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0,
                    restrictions = UserRestrictions(
                        chatRestricted = (restrMap["chatRestricted"] as? Boolean) ?: false,
                        liveRestricted = (restrMap["liveRestricted"] as? Boolean) ?: false,
                        postingRestricted = (restrMap["postingRestricted"] as? Boolean) ?: false
                    )
                )
            }
            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch users", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getUserDetails(targetUid: String): Result<UserDetails> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid)
            val res = functions.getHttpsCallable("adminGetUserDetails").call(params).await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()

            val ov = data["overview"] as? Map<String, Any> ?: emptyMap()
            val soc = data["socialStats"] as? Map<String, Any> ?: emptyMap()
            val rwd = data["rewardsSummary"] as? Map<String, Any> ?: emptyMap()

            val details = UserDetails(
                uid = (ov["uid"] as? String) ?: targetUid,
                displayName = (ov["displayName"] as? String) ?: "Worker",
                email = (ov["email"] as? String) ?: "",
                phoneNumber = (ov["phoneNumber"] as? String) ?: "",
                photoUrl = (ov["photoUrl"] as? String) ?: "",
                username = (ov["username"] as? String) ?: "",
                gender = (ov["gender"] as? String) ?: "Male",
                countryCode = (ov["countryCode"] as? String) ?: "IN",
                currencyCode = (ov["currencyCode"] as? String) ?: "INR",
                accountStatus = (ov["accountStatus"] as? String) ?: "ACTIVE",
                isVerified = (ov["isVerified"] as? Boolean) ?: false,
                createdAt = (ov["createdAt"] as? Number)?.toLong() ?: 0,
                updatedAt = (ov["updatedAt"] as? Number)?.toLong() ?: 0,
                followerCount = (soc["followerCount"] as? Number)?.toInt() ?: 0,
                followingCount = (soc["followingCount"] as? Number)?.toInt() ?: 0,
                bio = (soc["bio"] as? String) ?: "",
                profession = (soc["profession"] as? String) ?: "",
                availableCoins = (rwd["availableCoins"] as? Number)?.toInt() ?: 0,
                lifetimeEarned = (rwd["lifetimeEarned"] as? Number)?.toInt() ?: 0,
                spentCoins = (rwd["spentCoins"] as? Number)?.toInt() ?: 0,
                currentStreak = (rwd["currentStreak"] as? Number)?.toInt() ?: 0,
                referralCode = (rwd["referralCode"] as? String) ?: ""
            )
            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch user details", e)
        }
    }

    suspend fun updateUserStatus(
        targetUid: String,
        accountStatus: String?,
        isVerified: Boolean?,
        restrictions: UserRestrictions?,
        reason: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf<String, Any>(
                "targetUid" to targetUid,
                "reason" to reason
            )
            if (accountStatus != null) params["accountStatus"] = accountStatus
            if (isVerified != null) params["isVerified"] = isVerified
            if (restrictions != null) {
                params["restrictions"] = mapOf(
                    "chatRestricted" to restrictions.chatRestricted,
                    "liveRestricted" to restrictions.liveRestricted,
                    "postingRestricted" to restrictions.postingRestricted
                )
            }
            functions.getHttpsCallable("adminUpdateUserStatus").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update user status", e)
        }
    }

    suspend fun revokeSessions(targetUid: String, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "reason" to reason)
            functions.getHttpsCallable("adminRevokeSessions").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to revoke sessions", e)
        }
    }

    suspend fun addUserNote(targetUid: String, note: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "note" to note)
            functions.getHttpsCallable("adminAddUserNote").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to add user note", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getReports(status: String = "PENDING"): Result<List<ReportItem>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("status" to status)
            val res = functions.getHttpsCallable("adminGetReports").call(params).await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val rawList = data["reports"] as? List<Map<String, Any>> ?: emptyList()

            val reports = rawList.map { m ->
                ReportItem(
                    id = (m["id"] as? String) ?: "",
                    reporterUid = (m["reporterUid"] as? String) ?: "",
                    targetUid = (m["targetUid"] as? String) ?: "",
                    reason = (m["reason"] as? String) ?: "",
                    details = (m["details"] as? String) ?: "",
                    timestamp = (m["timestamp"] as? Number)?.toLong() ?: 0,
                    status = (m["status"] as? String) ?: "PENDING",
                    assignedAdminUid = (m["assignedAdminUid"] as? String) ?: "",
                    priority = (m["priority"] as? String) ?: "MEDIUM",
                    internalNotes = (m["internalNotes"] as? String) ?: "",
                    resolutionReason = (m["resolutionReason"] as? String) ?: "",
                    resolvedAt = (m["resolvedAt"] as? Number)?.toLong() ?: 0
                )
            }
            Result.Success(reports)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch reports", e)
        }
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: String,
        resolutionReason: String,
        priority: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf<String, Any>(
                "reportId" to reportId,
                "status" to status,
                "resolutionReason" to resolutionReason
            )
            if (priority != null) params["priority"] = priority
            functions.getHttpsCallable("adminUpdateReportStatus").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update report status", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getLiveRooms(isLive: Boolean = true): Result<List<LiveRoomItem>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("isLive" to isLive)
            val res = functions.getHttpsCallable("adminGetLiveRooms").call(params).await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val rawList = data["liveRooms"] as? List<Map<String, Any>> ?: emptyList()

            val rooms = rawList.map { m ->
                LiveRoomItem(
                    roomId = (m["roomId"] as? String) ?: "",
                    hostUserId = (m["hostUserId"] as? String) ?: "",
                    creatorId = (m["creatorId"] as? String) ?: "",
                    title = (m["title"] as? String) ?: "Live Stream",
                    type = (m["type"] as? String) ?: "VIDEO",
                    isLive = (m["isLive"] as? Boolean) ?: false,
                    isPublic = (m["isPublic"] as? Boolean) ?: true,
                    statusText = (m["statusText"] as? String) ?: "",
                    createdAtMillis = (m["createdAtMillis"] as? Number)?.toLong() ?: 0
                )
            }
            Result.Success(rooms)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch live rooms", e)
        }
    }

    suspend fun endLiveRoom(roomId: String, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("roomId" to roomId, "reason" to reason)
            functions.getHttpsCallable("adminEndLiveRoom").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to terminate live room", e)
        }
    }

    suspend fun adjustUserCoins(targetUid: String, amount: Int, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "amount" to amount, "reason" to reason)
            functions.getHttpsCallable("adminAdjustUserCoins").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to adjust user coins", e)
        }
    }

    suspend fun processRedemption(targetUid: String, redemptionId: String, status: String, notes: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "redemptionId" to redemptionId, "status" to status, "notes" to notes)
            functions.getHttpsCallable("adminProcessRedemption").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to process redemption", e)
        }
    }

    suspend fun sendNotificationCampaign(title: String, body: String, targetAudience: String = "ALL"): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("title" to title, "body" to body, "targetAudience" to targetAudience)
            functions.getHttpsCallable("adminSendNotificationCampaign").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to send notification campaign", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getNotificationCampaigns(): Result<List<NotificationCampaign>> = withContext(Dispatchers.IO) {
        try {
            val res = functions.getHttpsCallable("adminGetNotificationCampaigns").call().await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val rawList = data["campaigns"] as? List<Map<String, Any>> ?: emptyList()

            val campaigns = rawList.map { m ->
                NotificationCampaign(
                    id = (m["id"] as? String) ?: "",
                    title = (m["title"] as? String) ?: "",
                    body = (m["body"] as? String) ?: "",
                    targetAudience = (m["targetAudience"] as? String) ?: "ALL",
                    status = (m["status"] as? String) ?: "COMPLETED",
                    sentAt = (m["sentAt"] as? Number)?.toLong() ?: 0,
                    createdByAdminUid = (m["createdByAdminUid"] as? String) ?: "",
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0
                )
            }
            Result.Success(campaigns)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch notification campaigns", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getAppConfig(): Result<AppConfigData> = withContext(Dispatchers.IO) {
        try {
            val res = functions.getHttpsCallable("adminGetAppConfig").call().await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val flags = data["featureFlags"] as? Map<String, Any> ?: emptyMap()

            val config = AppConfigData(
                minSupportedVersionCode = (data["minSupportedVersionCode"] as? Number)?.toInt() ?: 1,
                latestVersionCode = (data["latestVersionCode"] as? Number)?.toInt() ?: 1,
                updateAnnouncementTitle = (data["updateAnnouncementTitle"] as? String) ?: "Update Available",
                updateAnnouncementMessage = (data["updateAnnouncementMessage"] as? String) ?: "Please update WorkDaily.",
                forceUpdate = (data["forceUpdate"] as? Boolean) ?: false,
                maintenanceMode = (data["maintenanceMode"] as? Boolean) ?: false,
                supportWhatsappUrl = (data["supportWhatsappUrl"] as? String) ?: "",
                supportEmail = (data["supportEmail"] as? String) ?: "workdaily.synfusion@gmail.com",
                shareMessage = (data["shareMessage"] as? String) ?: "Download WorkDaily!",
                announcementBannerText = (data["announcementBannerText"] as? String) ?: "",
                announcementBannerUrl = (data["announcementBannerUrl"] as? String) ?: "",
                isAnnouncementActive = (data["isAnnouncementActive"] as? Boolean) ?: false,
                liveEnabled = (flags["liveEnabled"] as? Boolean) ?: true,
                socialEnabled = (flags["socialEnabled"] as? Boolean) ?: true,
                rewardsEnabled = (flags["rewardsEnabled"] as? Boolean) ?: true
            )
            Result.Success(config)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch app configuration", e)
        }
    }

    suspend fun updateAppConfig(config: AppConfigData): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "minSupportedVersionCode" to config.minSupportedVersionCode,
                "latestVersionCode" to config.latestVersionCode,
                "updateAnnouncementTitle" to config.updateAnnouncementTitle,
                "updateAnnouncementMessage" to config.updateAnnouncementMessage,
                "forceUpdate" to config.forceUpdate,
                "maintenanceMode" to config.maintenanceMode,
                "supportWhatsappUrl" to config.supportWhatsappUrl,
                "supportEmail" to config.supportEmail,
                "shareMessage" to config.shareMessage,
                "announcementBannerText" to config.announcementBannerText,
                "announcementBannerUrl" to config.announcementBannerUrl,
                "isAnnouncementActive" to config.isAnnouncementActive,
                "featureFlags" to mapOf(
                    "liveEnabled" to config.liveEnabled,
                    "socialEnabled" to config.socialEnabled,
                    "rewardsEnabled" to config.rewardsEnabled
                )
            )
            functions.getHttpsCallable("adminUpdateAppConfig").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update app configuration", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getAdminTeam(): Result<List<AdminTeamMember>> = withContext(Dispatchers.IO) {
        try {
            val res = functions.getHttpsCallable("adminGetAdminTeam").call().await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val rawList = data["team"] as? List<Map<String, Any>> ?: emptyList()

            val team = rawList.map { m ->
                AdminTeamMember(
                    uid = (m["uid"] as? String) ?: "",
                    email = (m["email"] as? String) ?: "",
                    role = AdminRole.fromString(m["role"] as? String),
                    assignedBy = (m["assignedBy"] as? String) ?: "",
                    assignedAt = (m["assignedAt"] as? Number)?.toLong() ?: 0
                )
            }
            Result.Success(team)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch admin team", e)
        }
    }

    suspend fun assignAdminRole(targetUid: String, email: String, role: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "email" to email, "role" to role)
            functions.getHttpsCallable("adminAssignRole").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to assign admin role", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getAuditLogs(limit: Int = 50): Result<List<AdminAuditLog>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("limit" to limit)
            val res = functions.getHttpsCallable("adminGetAuditLogs").call(params).await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val rawList = data["logs"] as? List<Map<String, Any>> ?: emptyList()

            val logs = rawList.map { m ->
                AdminAuditLog(
                    id = (m["id"] as? String) ?: "",
                    adminUid = (m["adminUid"] as? String) ?: "",
                    adminEmail = (m["adminEmail"] as? String) ?: "",
                    action = (m["action"] as? String) ?: "",
                    targetType = (m["targetType"] as? String) ?: "",
                    targetId = (m["targetId"] as? String) ?: "",
                    reason = (m["reason"] as? String) ?: "",
                    timestamp = (m["timestamp"] as? Number)?.toLong() ?: 0
                )
            }
            Result.Success(logs)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch audit logs", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getSupportTickets(status: String = "OPEN"): Result<List<SupportTicket>> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("status" to status)
            val res = functions.getHttpsCallable("adminGetSupportTickets").call(params).await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()
            val rawList = data["tickets"] as? List<Map<String, Any>> ?: emptyList()

            val tickets = rawList.map { m ->
                SupportTicket(
                    id = (m["id"] as? String) ?: "",
                    userUid = (m["userUid"] as? String) ?: "",
                    subject = (m["subject"] as? String) ?: "",
                    description = (m["description"] as? String) ?: "",
                    category = (m["category"] as? String) ?: "GENERAL",
                    status = (m["status"] as? String) ?: "OPEN",
                    priority = (m["priority"] as? String) ?: "MEDIUM",
                    assignedAdminUid = (m["assignedAdminUid"] as? String) ?: "",
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0
                )
            }
            Result.Success(tickets)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch support tickets", e)
        }
    }

    suspend fun updateSupportTicket(ticketId: String, status: String?, replyText: String?, priority: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf<String, Any>("ticketId" to ticketId)
            if (status != null) params["status"] = status
            if (replyText != null) params["replyText"] = replyText
            if (priority != null) params["priority"] = priority

            functions.getHttpsCallable("adminUpdateSupportTicket").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update support ticket", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getSystemHealth(): Result<SystemHealthData> = withContext(Dispatchers.IO) {
        try {
            val res = functions.getHttpsCallable("adminGetSystemHealth").call().await()
            val data = res.getData() as? Map<String, Any> ?: emptyMap()

            val health = SystemHealthData(
                firebaseFunctionsStatus = (data["firebaseFunctionsStatus"] as? String) ?: "HEALTHY",
                fcmDeliveryRate = (data["fcmDeliveryRate"] as? String) ?: "99.8%",
                firestoreStatus = (data["firestoreStatus"] as? String) ?: "HEALTHY",
                liveKitStatus = (data["liveKitStatus"] as? String) ?: "HEALTHY",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
            Result.Success(health)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch system health", e)
        }
    }
}
