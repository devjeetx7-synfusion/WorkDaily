package com.synfusion.workdailyadmin.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.synfusion.workdailyadmin.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException

enum class AdminErrorType {
    NETWORK_UNAVAILABLE,
    AUTHENTICATION_EXPIRED,
    PERMISSION_DENIED,
    BACKEND_FUNCTION_NOT_FOUND,
    RESOURCE_NOT_FOUND,
    SERVER_ERROR,
    UNKNOWN_ERROR
}

sealed class Result<out T> {
    data class Success<out T>(val data: T, val isFallback: Boolean = false) : Result<T>()
    data class Error(
        val message: String,
        val errorType: AdminErrorType = AdminErrorType.UNKNOWN_ERROR,
        val cause: Throwable? = null,
        val canRetry: Boolean = true
    ) : Result<Nothing>()
}

open class AdminRepository(
    private val functionsSupplier: () -> FirebaseFunctions = { FirebaseFunctions.getInstance() },
    private val firestoreSupplier: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }
) {
    private val functions get() = functionsSupplier()
    private val firestore get() = firestoreSupplier()

    companion object {
        fun classifyError(e: Throwable, defaultMessage: String = "Operation failed"): Result.Error {
            return when (e) {
                is UnknownHostException, is SocketException, is FirebaseNetworkException -> {
                    Result.Error(
                        message = "Network unavailable. Please verify your internet connection.",
                        errorType = AdminErrorType.NETWORK_UNAVAILABLE,
                        cause = e,
                        canRetry = true
                    )
                }
                is FirebaseAuthException -> {
                    Result.Error(
                        message = "Admin session has expired. Please sign in again.",
                        errorType = AdminErrorType.AUTHENTICATION_EXPIRED,
                        cause = e,
                        canRetry = false
                    )
                }
                is FirebaseFunctionsException -> {
                    when (e.code) {
                        FirebaseFunctionsException.Code.NOT_FOUND -> {
                            Result.Error(
                                message = "Backend Cloud Function endpoint is not deployed or unavailable.",
                                errorType = AdminErrorType.BACKEND_FUNCTION_NOT_FOUND,
                                cause = e,
                                canRetry = true
                            )
                        }
                        FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                            Result.Error(
                                message = "Authentication required. Please sign in again.",
                                errorType = AdminErrorType.AUTHENTICATION_EXPIRED,
                                cause = e,
                                canRetry = false
                            )
                        }
                        FirebaseFunctionsException.Code.PERMISSION_DENIED -> {
                            Result.Error(
                                message = "Access denied: insufficient administrator permissions for this operation.",
                                errorType = AdminErrorType.PERMISSION_DENIED,
                                cause = e,
                                canRetry = false
                            )
                        }
                        FirebaseFunctionsException.Code.UNAVAILABLE -> {
                            Result.Error(
                                message = "Backend service temporarily unavailable. Please retry in a moment.",
                                errorType = AdminErrorType.NETWORK_UNAVAILABLE,
                                cause = e,
                                canRetry = true
                            )
                        }
                        FirebaseFunctionsException.Code.INTERNAL -> {
                            Result.Error(
                                message = "Internal backend error occurred. Please contact system support.",
                                errorType = AdminErrorType.SERVER_ERROR,
                                cause = e,
                                canRetry = true
                            )
                        }
                        else -> {
                            val msg = e.localizedMessage?.takeIf { it.isNotBlank() && it != "NOT_FOUND" }
                                ?: "Cloud function error: ${e.code.name}"
                            Result.Error(
                                message = msg,
                                errorType = AdminErrorType.SERVER_ERROR,
                                cause = e,
                                canRetry = true
                            )
                        }
                    }
                }
                is FirebaseFirestoreException -> {
                    when (e.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                            Result.Error(
                                message = "Access denied by Firestore security rules. Verify your admin role assignment.",
                                errorType = AdminErrorType.PERMISSION_DENIED,
                                cause = e,
                                canRetry = false
                            )
                        }
                        FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                            Result.Error(
                                message = "User session expired. Please sign in again.",
                                errorType = AdminErrorType.AUTHENTICATION_EXPIRED,
                                cause = e,
                                canRetry = false
                            )
                        }
                        FirebaseFirestoreException.Code.UNAVAILABLE -> {
                            Result.Error(
                                message = "Firestore service is currently unreachable.",
                                errorType = AdminErrorType.NETWORK_UNAVAILABLE,
                                cause = e,
                                canRetry = true
                            )
                        }
                        FirebaseFirestoreException.Code.NOT_FOUND -> {
                            Result.Error(
                                message = "Requested Firestore document or collection does not exist.",
                                errorType = AdminErrorType.RESOURCE_NOT_FOUND,
                                cause = e,
                                canRetry = false
                            )
                        }
                        else -> {
                            Result.Error(
                                message = e.localizedMessage ?: "Database operation failed.",
                                errorType = AdminErrorType.SERVER_ERROR,
                                cause = e,
                                canRetry = true
                            )
                        }
                    }
                }
                is IOException -> {
                    Result.Error(
                        message = "Network communication error. Please check your connection.",
                        errorType = AdminErrorType.NETWORK_UNAVAILABLE,
                        cause = e,
                        canRetry = true
                    )
                }
                else -> {
                    val msg = e.localizedMessage?.takeIf { it.isNotBlank() && it != "NOT_FOUND" }
                        ?: defaultMessage
                    Result.Error(
                        message = msg,
                        errorType = AdminErrorType.UNKNOWN_ERROR,
                        cause = e,
                        canRetry = true
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    open suspend fun getDashboardStats(): Result<DashboardStats> = withContext(Dispatchers.IO) {
        // Attempt primary authoritative Callable Cloud Function
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
            return@withContext Result.Success(stats, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            // If the Cloud Function is genuinely not deployed or not found, fallback to authorized Firestore reads
            if (isNotFound) {
                val db = firestore
                if (db != null) {
                    try {
                        val stats = fetchDashboardStatsFromFirestore(db)
                        return@withContext Result.Success(stats, isFallback = true)
                    } catch (fsErr: Exception) {
                        return@withContext classifyError(fsErr, "Failed to load dashboard statistics from database.")
                    }
                }
            }

            classifyError(e, "Failed to fetch dashboard stats")
        }
    }

    /**
     * Resilient fallback reading real production collections authorized under Firestore Security Rules.
     * Uses efficient Firestore Count Aggregations where available without downloading entire collections.
     * Only supported, authorized fields are queried; unavailable metrics are safely omitted or marked
     * rather than fabricating arbitrary values.
     */
    private suspend fun fetchDashboardStatsFromFirestore(db: FirebaseFirestore): DashboardStats {
        val totalUsersCount = try {
            db.collection("public_profiles").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count.toInt()
        } catch (_: Exception) {
            db.collection("public_profiles").limit(1000).get().await().size()
        }

        val verifiedUsersCount = try {
            db.collection("public_profiles")
                .whereEqualTo("isVerified", true)
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
                .count
                .toInt()
        } catch (_: Exception) {
            0
        }

        val activeLiveRoomsCount = try {
            db.collection("live_rooms")
                .whereEqualTo("isLive", true)
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
                .count
                .toInt()
        } catch (_: Exception) {
            0
        }

        val configDoc = try {
            db.collection("app_config").document("global").get().await()
        } catch (_: Exception) {
            null
        }
        val maintenanceMode = configDoc?.getBoolean("maintenanceMode") ?: false
        val systemStatus = if (maintenanceMode) "MAINTENANCE" else "OPERATIONAL (DIRECT FIRESTORE SYNC)"

        return DashboardStats(
            totalUsers = totalUsersCount,
            suspendedUsers = 0,
            bannedUsers = 0,
            verifiedUsers = verifiedUsersCount,
            activeLiveRooms = activeLiveRoomsCount,
            pendingReports = 0,
            totalCoinsIssued = 0,
            totalCoinsSpent = 0,
            pendingRedemptionsCount = 0,
            systemStatus = systemStatus
        )
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
            Result.Success(users, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            if (isNotFound) {
                val db = firestore
                if (db != null) {
                    try {
                        val users = fetchUsersFromFirestore(db, searchQuery, limit)
                        return@withContext Result.Success(users, isFallback = true)
                    } catch (fsErr: Exception) {
                        return@withContext classifyError(fsErr, "Failed to load users from database.")
                    }
                }
            }
            classifyError(e, "Failed to fetch users")
        }
    }

    private suspend fun fetchUsersFromFirestore(
        db: FirebaseFirestore,
        searchQuery: String,
        limit: Int
    ): List<UserSummary> {
        val snap = db.collection("public_profiles").limit(limit.coerceAtMost(50).toLong()).get().await()
        var list = snap.documents.map { doc ->
            UserSummary(
                uid = doc.id,
                displayName = doc.getString("displayName") ?: "Worker",
                email = "",
                phoneNumber = "",
                photoUrl = doc.getString("photoUrl") ?: "",
                username = doc.getString("username") ?: "",
                accountStatus = "ACTIVE",
                isVerified = doc.getBoolean("isVerified") ?: false,
                countryCode = doc.getString("countryCode") ?: "IN",
                createdAt = doc.getLong("createdAt") ?: 0L,
                restrictions = UserRestrictions()
            )
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase().trim()
            list = list.filter {
                it.uid.lowercase().contains(q) ||
                        it.displayName.lowercase().contains(q) ||
                        it.username.lowercase().contains(q)
            }
        }
        return list
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
            Result.Success(details, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            if (isNotFound) {
                val db = firestore
                if (db != null) {
                    try {
                        val pubDoc = db.collection("public_profiles").document(targetUid).get().await()
                        if (pubDoc.exists()) {
                            val details = UserDetails(
                                uid = targetUid,
                                displayName = pubDoc.getString("displayName") ?: "Worker",
                                email = "",
                                phoneNumber = "",
                                photoUrl = pubDoc.getString("photoUrl") ?: "",
                                username = pubDoc.getString("username") ?: "",
                                gender = pubDoc.getString("gender") ?: "Male",
                                countryCode = pubDoc.getString("countryCode") ?: "IN",
                                currencyCode = "INR",
                                accountStatus = "ACTIVE",
                                isVerified = pubDoc.getBoolean("isVerified") ?: false,
                                createdAt = pubDoc.getLong("createdAt") ?: 0L,
                                updatedAt = pubDoc.getLong("updatedAt") ?: 0L,
                                followerCount = pubDoc.getLong("followerCount")?.toInt() ?: 0,
                                followingCount = pubDoc.getLong("followingCount")?.toInt() ?: 0,
                                bio = pubDoc.getString("bio") ?: "",
                                profession = pubDoc.getString("profession") ?: "",
                                availableCoins = 0,
                                lifetimeEarned = 0,
                                spentCoins = 0,
                                currentStreak = 0,
                                referralCode = ""
                            )
                            return@withContext Result.Success(details, isFallback = true)
                        }
                    } catch (fsErr: Exception) {
                        return@withContext classifyError(fsErr, "Failed to load user details from database.")
                    }
                }
            }
            classifyError(e, "Failed to fetch user details")
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
            classifyError(e, "Failed to update user status")
        }
    }

    suspend fun revokeSessions(targetUid: String, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "reason" to reason)
            functions.getHttpsCallable("adminRevokeSessions").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            classifyError(e, "Failed to revoke sessions")
        }
    }

    suspend fun addUserNote(targetUid: String, note: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "note" to note)
            functions.getHttpsCallable("adminAddUserNote").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            classifyError(e, "Failed to add user note")
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
            classifyError(e, "Failed to fetch reports")
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
            classifyError(e, "Failed to update report status")
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
            Result.Success(rooms, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            if (isNotFound) {
                val db = firestore
                if (db != null) {
                    try {
                        val snap = db.collection("live_rooms")
                            .whereEqualTo("isLive", isLive)
                            .limit(50)
                            .get()
                            .await()
                        val rooms = snap.documents.map { doc ->
                            LiveRoomItem(
                                roomId = doc.id,
                                hostUserId = doc.getString("hostUserId") ?: "",
                                creatorId = doc.getString("creatorId") ?: "",
                                title = doc.getString("title") ?: "Live Stream",
                                type = doc.getString("type") ?: "VIDEO",
                                isLive = doc.getBoolean("isLive") ?: false,
                                isPublic = doc.getBoolean("isPublic") ?: true,
                                statusText = doc.getString("statusText") ?: "",
                                createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
                            )
                        }
                        return@withContext Result.Success(rooms, isFallback = true)
                    } catch (fsErr: Exception) {
                        return@withContext classifyError(fsErr, "Failed to load live rooms from database.")
                    }
                }
            }
            classifyError(e, "Failed to fetch live rooms")
        }
    }

    suspend fun endLiveRoom(roomId: String, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("roomId" to roomId, "reason" to reason)
            functions.getHttpsCallable("adminEndLiveRoom").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            classifyError(e, "Failed to terminate live room")
        }
    }

    suspend fun adjustUserCoins(targetUid: String, amount: Int, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "amount" to amount, "reason" to reason)
            functions.getHttpsCallable("adminAdjustUserCoins").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            classifyError(e, "Failed to adjust user coins")
        }
    }

    suspend fun processRedemption(targetUid: String, redemptionId: String, status: String, notes: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "redemptionId" to redemptionId, "status" to status, "notes" to notes)
            functions.getHttpsCallable("adminProcessRedemption").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            classifyError(e, "Failed to process redemption")
        }
    }

    suspend fun sendNotificationCampaign(title: String, body: String, targetAudience: String = "ALL"): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("title" to title, "body" to body, "targetAudience" to targetAudience)
            functions.getHttpsCallable("adminSendNotificationCampaign").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            classifyError(e, "Failed to send notification campaign")
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
            Result.Success(campaigns, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            if (isNotFound) {
                val db = firestore
                if (db != null) {
                    try {
                        val snap = db.collection("notification_campaigns").limit(30).get().await()
                        val campaigns = snap.documents.map { doc ->
                            NotificationCampaign(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                body = doc.getString("body") ?: "",
                                targetAudience = doc.getString("targetAudience") ?: "ALL",
                                status = doc.getString("status") ?: "COMPLETED",
                                sentAt = doc.getLong("sentAt") ?: 0L,
                                createdByAdminUid = doc.getString("createdByAdminUid") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        }
                        return@withContext Result.Success(campaigns, isFallback = true)
                    } catch (fsErr: Exception) {
                        return@withContext classifyError(fsErr, "Failed to load notification campaigns.")
                    }
                }
            }
            classifyError(e, "Failed to fetch notification campaigns")
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
            Result.Success(config, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            if (isNotFound) {
                val db = firestore
                if (db != null) {
                    try {
                        val doc = db.collection("app_config").document("global").get().await()
                        if (doc.exists()) {
                            val flags = doc.get("featureFlags") as? Map<*, *> ?: emptyMap<String, Any>()
                            val config = AppConfigData(
                                minSupportedVersionCode = doc.getLong("minSupportedVersionCode")?.toInt() ?: 1,
                                latestVersionCode = doc.getLong("latestVersionCode")?.toInt() ?: 1,
                                updateAnnouncementTitle = doc.getString("updateAnnouncementTitle") ?: "Update Available",
                                updateAnnouncementMessage = doc.getString("updateAnnouncementMessage") ?: "Please update WorkDaily.",
                                forceUpdate = doc.getBoolean("forceUpdate") ?: false,
                                maintenanceMode = doc.getBoolean("maintenanceMode") ?: false,
                                supportWhatsappUrl = doc.getString("supportWhatsappUrl") ?: "",
                                supportEmail = doc.getString("supportEmail") ?: "workdaily.synfusion@gmail.com",
                                shareMessage = doc.getString("shareMessage") ?: "Download WorkDaily!",
                                announcementBannerText = doc.getString("announcementBannerText") ?: "",
                                announcementBannerUrl = doc.getString("announcementBannerUrl") ?: "",
                                isAnnouncementActive = doc.getBoolean("isAnnouncementActive") ?: false,
                                liveEnabled = (flags["liveEnabled"] as? Boolean) ?: true,
                                socialEnabled = (flags["socialEnabled"] as? Boolean) ?: true,
                                rewardsEnabled = (flags["rewardsEnabled"] as? Boolean) ?: true
                            )
                            return@withContext Result.Success(config, isFallback = true)
                        }
                    } catch (fsErr: Exception) {
                        return@withContext classifyError(fsErr, "Failed to load app config from database.")
                    }
                }
            }
            classifyError(e, "Failed to fetch app configuration")
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
            classifyError(e, "Failed to update app configuration")
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
            classifyError(e, "Failed to fetch admin team")
        }
    }

    suspend fun assignAdminRole(targetUid: String, email: String, role: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("targetUid" to targetUid, "email" to email, "role" to role)
            functions.getHttpsCallable("adminAssignRole").call(params).await()
            Result.Success(true)
        } catch (e: Exception) {
            classifyError(e, "Failed to assign admin role")
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
            Result.Success(logs, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            if (isNotFound) {
                val db = firestore
                if (db != null) {
                    try {
                        val snap = db.collection("admin_audit_logs").limit(limit.toLong()).get().await()
                        val logs = snap.documents.map { doc ->
                            AdminAuditLog(
                                id = doc.id,
                                adminUid = doc.getString("adminUid") ?: "",
                                adminEmail = doc.getString("adminEmail") ?: "",
                                action = doc.getString("action") ?: "",
                                targetType = doc.getString("targetType") ?: "",
                                targetId = doc.getString("targetId") ?: "",
                                reason = doc.getString("reason") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        }
                        return@withContext Result.Success(logs, isFallback = true)
                    } catch (fsErr: Exception) {
                        return@withContext classifyError(fsErr, "Failed to load audit logs from database.")
                    }
                }
            }
            classifyError(e, "Failed to fetch audit logs")
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
            classifyError(e, "Failed to fetch support tickets")
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
            classifyError(e, "Failed to update support ticket")
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getSystemHealth(): Result<SystemHealthData> = withContext(Dispatchers.IO) {
        try {
            val res = functions.getHttpsCallable("adminGetSystemHealth").call().await()
            val data = res.getData() as? Map<String, Any>
                ?: throw IllegalStateException("System health response was empty or malformed.")

            val health = SystemHealthData(
                firebaseFunctionsStatus = (data["firebaseFunctionsStatus"] as? String) ?: "UNKNOWN",
                fcmDeliveryRate = (data["fcmDeliveryRate"] as? String) ?: "N/A",
                firestoreStatus = (data["firestoreStatus"] as? String) ?: "UNKNOWN",
                liveKitStatus = (data["liveKitStatus"] as? String) ?: "UNKNOWN",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
            )
            Result.Success(health, isFallback = false)
        } catch (e: Exception) {
            val isNotFound = (e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.NOT_FOUND) ||
                    (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)

            if (isNotFound) {
                val db = firestore
                val firestoreAvailable = db != null
                val health = SystemHealthData(
                    firebaseFunctionsStatus = "UNAVAILABLE (NOT DEPLOYED)",
                    fcmDeliveryRate = "N/A",
                    firestoreStatus = if (firestoreAvailable) "OPERATIONAL (DIRECT)" else "UNAVAILABLE",
                    liveKitStatus = "UNKNOWN",
                    timestamp = System.currentTimeMillis()
                )
                return@withContext Result.Success(health, isFallback = true)
            }

            classifyError(e, "Failed to fetch live system health")
        }
    }
}
