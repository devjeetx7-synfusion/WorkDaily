package com.dailywork.admin.data.repository

import com.dailywork.admin.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun getAllUsers(limit: Long = 50): Flow<List<User>> = callbackFlow {
        val subscription = db.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { subscription.remove() }
    }

    fun getUser(userId: String): Flow<User?> = callbackFlow {
        val subscription = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)?.copy(uid = snapshot.id)
                trySend(user)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun setUserBlockedStatus(userId: String, isBlocked: Boolean, reason: String = "") {
        val adminId = auth.currentUser?.uid ?: "unknown"
        val blockInfo = if (isBlocked) {
            BlockInfo(reason = reason, blockedAt = System.currentTimeMillis(), blockedBy = adminId)
        } else null

        db.collection("users").document(userId)
            .update(
                mapOf(
                    "isBlocked" to isBlocked,
                    "blockInfo" to blockInfo
                )
            ).await()

        logAdminAction(
            action = if (isBlocked) "BLOCK_USER" else "UNBLOCK_USER",
            targetId = userId,
            details = if (isBlocked) "Reason: $reason" else ""
        )
    }

    suspend fun setUserRole(userId: String, role: String) {
        db.collection("users").document(userId).update("role", role).await()
        logAdminAction("CHANGE_ROLE", userId, "New role: $role")
    }

    suspend fun deleteUser(userId: String) {
        db.collection("users").document(userId).delete().await()
        logAdminAction("DELETE_USER", userId)
    }

    fun getAdminStats(): Flow<AdminStats> = callbackFlow {
        val subscription = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()

                val stats = AdminStats(
                    totalUsers = users.size,
                    contractorCount = users.count { it.role == "contractor" },
                    personalCount = users.count { it.role == "personal" },
                    blockedCount = users.count { it.isBlocked },
                    premiumCount = 0,
                    onlineCount = users.count { System.currentTimeMillis() - it.lastActive < 5 * 60 * 1000 },
                    totalJobs = 0
                )
                trySend(stats)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getTotalJobsCount(): Int {
        return try {
            val query = db.collectionGroup("attendance")
            val snapshot = query.count().get(AggregateSource.SERVER).await()
            snapshot.count.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        val subscription = db.collection("config").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val config = snapshot?.toObject(AppConfig::class.java) ?: AppConfig()
                trySend(config)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateAppConfig(config: AppConfig) {
        db.collection("config").document("settings").set(config).await()
        logAdminAction("UPDATE_CONFIG", "settings", config.toString())
    }

    fun getReports(): Flow<List<Report>> = callbackFlow {
        val subscription = db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Report::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(reports)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateReportStatus(reportId: String, status: String) {
        db.collection("reports").document(reportId).update("status", status).await()
        logAdminAction("UPDATE_REPORT", reportId, "New status: $status")
    }

    fun getAdminLogs(): Flow<List<AdminLog>> = callbackFlow {
        val subscription = db.collection("admin_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AdminLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { subscription.remove() }
    }

    private suspend fun logAdminAction(action: String, targetId: String, details: String = "") {
        val adminId = auth.currentUser?.uid ?: "unknown"
        val log = AdminLog(
            adminId = adminId,
            action = action,
            targetId = targetId,
            timestamp = System.currentTimeMillis(),
            details = details
        )
        db.collection("admin_logs").add(log).await()
    }
}
