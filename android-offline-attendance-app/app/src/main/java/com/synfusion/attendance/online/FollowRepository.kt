package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Data class representing a Follow relationship record in Firestore `follows/{followerUid}_${targetUid}`.
 */
data class FollowRecord(
    val followerUid: String = "",
    val targetUid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

interface FollowRepository {
    suspend fun followUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean>
    suspend fun unfollowUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean>
    suspend fun isFollowing(followerUid: String, targetUid: String): Boolean
    suspend fun isMutual(uidA: String, uidB: String): Boolean
    suspend fun getFollowers(targetUid: String, limit: Int = 50): ProfileOperationResult<List<PublicProfile>>
    suspend fun getFollowing(followerUid: String, limit: Int = 50): ProfileOperationResult<List<PublicProfile>>
}

class FirestoreFollowRepository(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) : FollowRepository {

    override suspend fun followUser(
        followerUid: String,
        targetUid: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (followerUid.isBlank() || targetUid.isBlank()) {
            return@withContext ProfileOperationResult.Error("Follower UID and Target UID must not be empty.")
        }

        if (followerUid == targetUid) {
            return@withContext ProfileOperationResult.Error("Self-follow is prohibited.")
        }

        try {
            val firestore = firestoreSupplier()
            val followDocId = "${followerUid}_${targetUid}"
            val followRef = firestore.collection("follows").document(followDocId)
            val followerProfileRef = firestore.collection("public_profiles").document(followerUid)
            val targetProfileRef = firestore.collection("public_profiles").document(targetUid)

            firestore.runTransaction { transaction ->
                val followSnap = transaction.get(followRef)
                if (followSnap.exists()) {
                    // Already following - idempotent success
                    return@runTransaction
                }

                val now = System.currentTimeMillis()
                val followData = mapOf(
                    "followerUid" to followerUid,
                    "targetUid" to targetUid,
                    "createdAt" to now
                )

                transaction.set(followRef, followData)

                // Update counts on public profiles if documents exist
                val targetSnap = transaction.get(targetProfileRef)
                if (targetSnap.exists()) {
                    val currentFollowers = (targetSnap.getLong("followerCount") ?: 0L)
                    transaction.update(targetProfileRef, "followerCount", currentFollowers + 1)
                }

                val followerSnap = transaction.get(followerProfileRef)
                if (followerSnap.exists()) {
                    val currentFollowing = (followerSnap.getLong("followingCount") ?: 0L)
                    transaction.update(followerProfileRef, "followingCount", currentFollowing + 1)
                }
            }.await()

            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to follow user", cause = e)
        }
    }

    override suspend fun unfollowUser(
        followerUid: String,
        targetUid: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (followerUid.isBlank() || targetUid.isBlank()) {
            return@withContext ProfileOperationResult.Error("Follower UID and Target UID must not be empty.")
        }

        try {
            val firestore = firestoreSupplier()
            val followDocId = "${followerUid}_${targetUid}"
            val followRef = firestore.collection("follows").document(followDocId)
            val followerProfileRef = firestore.collection("public_profiles").document(followerUid)
            val targetProfileRef = firestore.collection("public_profiles").document(targetUid)

            firestore.runTransaction { transaction ->
                val followSnap = transaction.get(followRef)
                if (!followSnap.exists()) {
                    // Not following - idempotent success
                    return@runTransaction
                }

                transaction.delete(followRef)

                val targetSnap = transaction.get(targetProfileRef)
                if (targetSnap.exists()) {
                    val currentFollowers = (targetSnap.getLong("followerCount") ?: 0L)
                    val newCount = (currentFollowers - 1).coerceAtLeast(0L)
                    transaction.update(targetProfileRef, "followerCount", newCount)
                }

                val followerSnap = transaction.get(followerProfileRef)
                if (followerSnap.exists()) {
                    val currentFollowing = (followerSnap.getLong("followingCount") ?: 0L)
                    val newCount = (currentFollowing - 1).coerceAtLeast(0L)
                    transaction.update(followerProfileRef, "followingCount", newCount)
                }
            }.await()

            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to unfollow user", cause = e)
        }
    }

    override suspend fun isFollowing(followerUid: String, targetUid: String): Boolean = withContext(Dispatchers.IO) {
        if (followerUid.isBlank() || targetUid.isBlank()) return@withContext false
        try {
            val firestore = firestoreSupplier()
            val followDocId = "${followerUid}_${targetUid}"
            val snap = firestore.collection("follows").document(followDocId).get().await()
            snap.exists()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isMutual(uidA: String, uidB: String): Boolean = withContext(Dispatchers.IO) {
        if (uidA.isBlank() || uidB.isBlank() || uidA == uidB) return@withContext false
        val aFollowsB = isFollowing(uidA, uidB)
        if (!aFollowsB) return@withContext false
        val bFollowsA = isFollowing(uidB, uidA)
        aFollowsB && bFollowsA
    }

    override suspend fun getFollowers(
        targetUid: String,
        limit: Int
    ): ProfileOperationResult<List<PublicProfile>> = withContext(Dispatchers.IO) {
        if (targetUid.isBlank()) return@withContext ProfileOperationResult.Success(emptyList())

        try {
            val firestore = firestoreSupplier()
            val followsSnap = firestore.collection("follows")
                .whereEqualTo("targetUid", targetUid)
                .limit(limit.toLong())
                .get()
                .await()

            val followerUids = followsSnap.documents.mapNotNull { it.getString("followerUid") }
            if (followerUids.isEmpty()) {
                return@withContext ProfileOperationResult.Success(emptyList())
            }

            val profiles = mutableListOf<PublicProfile>()
            for (uid in followerUids) {
                val profileSnap = firestore.collection("public_profiles").document(uid).get().await()
                if (profileSnap.exists()) {
                    val data = profileSnap.data
                    if (data != null) {
                        profiles.add(
                            PublicProfile(
                                uid = uid,
                                displayName = data["displayName"] as? String ?: "WorkDaily Member",
                                username = data["username"] as? String ?: "",
                                bio = data["bio"] as? String ?: "",
                                profession = data["profession"] as? String ?: "Site Worker",
                                photoUrl = data["photoUrl"] as? String,
                                isVerified = data["isVerified"] as? Boolean ?: false,
                                followerCount = (data["followerCount"] as? Number)?.toInt() ?: 0,
                                followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0
                            )
                        )
                    }
                }
            }

            ProfileOperationResult.Success(profiles)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to fetch followers", cause = e)
        }
    }

    override suspend fun getFollowing(
        followerUid: String,
        limit: Int
    ): ProfileOperationResult<List<PublicProfile>> = withContext(Dispatchers.IO) {
        if (followerUid.isBlank()) return@withContext ProfileOperationResult.Success(emptyList())

        try {
            val firestore = firestoreSupplier()
            val followsSnap = firestore.collection("follows")
                .whereEqualTo("followerUid", followerUid)
                .limit(limit.toLong())
                .get()
                .await()

            val targetUids = followsSnap.documents.mapNotNull { it.getString("targetUid") }
            if (targetUids.isEmpty()) {
                return@withContext ProfileOperationResult.Success(emptyList())
            }

            val profiles = mutableListOf<PublicProfile>()
            for (uid in targetUids) {
                val profileSnap = firestore.collection("public_profiles").document(uid).get().await()
                if (profileSnap.exists()) {
                    val data = profileSnap.data
                    if (data != null) {
                        profiles.add(
                            PublicProfile(
                                uid = uid,
                                displayName = data["displayName"] as? String ?: "WorkDaily Member",
                                username = data["username"] as? String ?: "",
                                bio = data["bio"] as? String ?: "",
                                profession = data["profession"] as? String ?: "Site Worker",
                                photoUrl = data["photoUrl"] as? String,
                                isVerified = data["isVerified"] as? Boolean ?: false,
                                followerCount = (data["followerCount"] as? Number)?.toInt() ?: 0,
                                followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0
                            )
                        )
                    }
                }
            }

            ProfileOperationResult.Success(profiles)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to fetch following users", cause = e)
        }
    }
}
