package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

data class FollowCounts(
    val followerCount: Int,
    val followingCount: Int
)

interface FollowRepository {
    suspend fun followUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean>
    suspend fun unfollowUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean>
    suspend fun isFollowing(followerUid: String, targetUid: String): Boolean
    suspend fun isMutual(uidA: String, uidB: String): Boolean
    suspend fun getFollowers(targetUid: String, limit: Int = 100): ProfileOperationResult<List<PublicProfile>>
    suspend fun getFollowing(followerUid: String, limit: Int = 100): ProfileOperationResult<List<PublicProfile>>

    suspend fun getFollowCounts(uid: String): ProfileOperationResult<FollowCounts> {
        val followersRes = getFollowers(uid)
        val followingRes = getFollowing(uid)
        return if (followersRes is ProfileOperationResult.Success && followingRes is ProfileOperationResult.Success) {
            ProfileOperationResult.Success(
                FollowCounts(
                    followerCount = followersRes.data.size,
                    followingCount = followingRes.data.size
                )
            )
        } else {
            val err = (followersRes as? ProfileOperationResult.Error)?.message
                ?: (followingRes as? ProfileOperationResult.Error)?.message
                ?: "Failed to load follow counts"
            ProfileOperationResult.Error(err)
        }
    }
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
            val blockForwardRef = firestore.collection("blocks").document("${followerUid}_${targetUid}")
            val blockReverseRef = firestore.collection("blocks").document("${targetUid}_${followerUid}")
            val followerProfileRef = firestore.collection("public_profiles").document(followerUid)
            val targetProfileRef = firestore.collection("public_profiles").document(targetUid)

            var createdRelationship = false

            firestore.runTransaction { transaction ->
                // Firestore transaction rule: ALL READS MUST BE EXECUTED BEFORE ANY WRITES!
                val followSnap = transaction.get(followRef)
                val blockForwardSnap = transaction.get(blockForwardRef)
                val blockReverseSnap = transaction.get(blockReverseRef)
                val targetSnap = transaction.get(targetProfileRef)
                val followerSnap = transaction.get(followerProfileRef)

                if (blockForwardSnap.exists() || blockReverseSnap.exists()) {
                    throw IllegalStateException("BLOCKED_RELATIONSHIP")
                }

                if (followSnap.exists()) {
                    // Already following - idempotent no-op
                    createdRelationship = false
                    return@runTransaction
                }

                val now = System.currentTimeMillis()
                val followData = mapOf(
                    "followerUid" to followerUid,
                    "targetUid" to targetUid,
                    "createdAt" to now
                )

                transaction.set(followRef, followData)

                if (targetSnap.exists()) {
                    val currentFollowers = (targetSnap.getLong("followerCount") ?: 0L)
                    transaction.update(targetProfileRef, "followerCount", currentFollowers + 1)
                }

                if (followerSnap.exists()) {
                    val currentFollowing = (followerSnap.getLong("followingCount") ?: 0L)
                    transaction.update(followerProfileRef, "followingCount", currentFollowing + 1)
                }
                createdRelationship = true
            }.await()

            ProfileOperationResult.Success(createdRelationship)
        } catch (e: Exception) {
            val msg = if (e.message == "BLOCKED_RELATIONSHIP" || e.cause?.message == "BLOCKED_RELATIONSHIP") {
                "Cannot follow this user due to block settings."
            } else {
                e.message ?: "Failed to follow user"
            }
            ProfileOperationResult.Error(msg, cause = e)
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

            var removedRelationship = false

            firestore.runTransaction { transaction ->
                // Firestore transaction rule: ALL READS MUST BE EXECUTED BEFORE ANY WRITES!
                val followSnap = transaction.get(followRef)
                val targetSnap = transaction.get(targetProfileRef)
                val followerSnap = transaction.get(followerProfileRef)

                if (!followSnap.exists()) {
                    // Not following - idempotent no-op
                    removedRelationship = false
                    return@runTransaction
                }

                transaction.delete(followRef)

                if (targetSnap.exists()) {
                    val currentFollowers = (targetSnap.getLong("followerCount") ?: 0L)
                    val newCount = (currentFollowers - 1).coerceAtLeast(0L)
                    transaction.update(targetProfileRef, "followerCount", newCount)
                }

                if (followerSnap.exists()) {
                    val currentFollowing = (followerSnap.getLong("followingCount") ?: 0L)
                    val newCount = (currentFollowing - 1).coerceAtLeast(0L)
                    transaction.update(followerProfileRef, "followingCount", newCount)
                }
                removedRelationship = true
            }.await()

            ProfileOperationResult.Success(removedRelationship)
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
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun isMutual(uidA: String, uidB: String): Boolean = withContext(Dispatchers.IO) {
        if (uidA.isBlank() || uidB.isBlank() || uidA == uidB) return@withContext false
        coroutineScope {
            val aFollowsBDeferred = async { isFollowing(uidA, uidB) }
            val bFollowsADeferred = async { isFollowing(uidB, uidA) }
            aFollowsBDeferred.await() && bFollowsADeferred.await()
        }
    }

    override suspend fun getFollowCounts(uid: String): ProfileOperationResult<FollowCounts> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) {
            return@withContext ProfileOperationResult.Error("UID must not be blank.")
        }

        try {
            val firestore = firestoreSupplier()
            val (followersAgg, followingAgg) = coroutineScope {
                val followersDeferred = async {
                    firestore.collection("follows")
                        .whereEqualTo("targetUid", uid)
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()
                }
                val followingDeferred = async {
                    firestore.collection("follows")
                        .whereEqualTo("followerUid", uid)
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()
                }
                followersDeferred.await() to followingDeferred.await()
            }

            val exactFollowers = followersAgg.count.coerceAtLeast(0L).toInt()
            val exactFollowing = followingAgg.count.coerceAtLeast(0L).toInt()

            // Self-heal stored counters on own public_profiles doc only when drifted (fast local cache check first)
            val currentAuthUid = try { authSupplier().currentUser?.uid } catch (_: Exception) { null }
            if (currentAuthUid == uid) {
                try {
                    val profileRef = firestore.collection("public_profiles").document(uid)
                    val cachedSnap = try { profileRef.get(Source.CACHE).await() } catch (_: Exception) { null }
                    val snap = cachedSnap ?: profileRef.get().await()
                    if (snap.exists()) {
                        val storedFollowers = (snap.getLong("followerCount") ?: 0L).toInt()
                        val storedFollowing = (snap.getLong("followingCount") ?: 0L).toInt()
                        if (storedFollowers != exactFollowers || storedFollowing != exactFollowing) {
                            profileRef.update(
                                mapOf(
                                    "followerCount" to exactFollowers,
                                    "followingCount" to exactFollowing
                                )
                            ).await()
                        }
                    }
                } catch (_: Exception) {
                    // Best-effort counter healing
                }
            }

            ProfileOperationResult.Success(
                FollowCounts(
                    followerCount = exactFollowers,
                    followingCount = exactFollowing
                )
            )
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to load follow counts", cause = e)
        }
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

            val followerUids = followsSnap.documents
                .sortedByDescending { it.getLong("createdAt") ?: 0L }
                .mapNotNull { it.getString("followerUid")?.takeIf { uid -> uid.isNotBlank() } }
                .distinct()

            if (followerUids.isEmpty()) {
                return@withContext ProfileOperationResult.Success(emptyList())
            }

            ProfileOperationResult.Success(fetchPublicProfilesPreservingOrder(firestore, followerUids))
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

            val targetUids = followsSnap.documents
                .sortedByDescending { it.getLong("createdAt") ?: 0L }
                .mapNotNull { it.getString("targetUid")?.takeIf { uid -> uid.isNotBlank() } }
                .distinct()

            if (targetUids.isEmpty()) {
                return@withContext ProfileOperationResult.Success(emptyList())
            }

            ProfileOperationResult.Success(fetchPublicProfilesPreservingOrder(firestore, targetUids))
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to fetch following users", cause = e)
        }
    }

    private suspend fun fetchPublicProfilesPreservingOrder(
        firestore: FirebaseFirestore,
        orderedUids: List<String>
    ): List<PublicProfile> = coroutineScope {
        val snapshots = orderedUids.chunked(10).map { chunk ->
            async {
                firestore.collection("public_profiles")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
            }
        }.awaitAll()

        val profileMap = mutableMapOf<String, PublicProfile>()
        for (snap in snapshots) {
            for (doc in snap.documents) {
                val data = doc.data ?: continue
                val uid = doc.id
                profileMap[uid] = PublicProfile(
                    uid = uid,
                    displayName = data["displayName"] as? String ?: "WorkDaily Member",
                    username = data["username"] as? String ?: "",
                    usernameLower = data["usernameLower"] as? String ?: "",
                    displayNameLower = data["displayNameLower"] as? String ?: "",
                    bio = data["bio"] as? String ?: "",
                    profession = data["profession"] as? String ?: "Site Worker",
                    photoUrl = data["photoUrl"] as? String,
                    isPublic = data["isPublic"] as? Boolean ?: true,
                    isVerified = data["isVerified"] as? Boolean ?: false,
                    followerCount = (data["followerCount"] as? Number)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                )
            }
        }
        orderedUids.map { uid ->
            profileMap[uid] ?: PublicProfile(
                uid = uid,
                displayName = "WorkDaily Member",
                username = uid.take(10).lowercase()
            )
        }
    }
}
