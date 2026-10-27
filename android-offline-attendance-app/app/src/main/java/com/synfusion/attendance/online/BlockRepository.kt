package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface BlockRepository {
    suspend fun blockUser(blockerUid: String, blockedUid: String): ProfileOperationResult<Boolean>
    suspend fun unblockUser(blockerUid: String, blockedUid: String): ProfileOperationResult<Boolean>
    suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean
    suspend fun isEitherBlocked(uidA: String, uidB: String): Boolean
    suspend fun getBlockedUserIds(blockerUid: String): ProfileOperationResult<List<String>>
    suspend fun removeFollower(targetUid: String, followerUid: String): ProfileOperationResult<Boolean>

    suspend fun getBlockedProfiles(blockerUid: String): ProfileOperationResult<List<PublicProfile>> {
        return when (val idsRes = getBlockedUserIds(blockerUid)) {
            is ProfileOperationResult.Success -> {
                ProfileOperationResult.Success(
                    idsRes.data.map { uid ->
                        PublicProfile(uid = uid, displayName = "Blocked User", username = uid.take(10))
                    }
                )
            }
            is ProfileOperationResult.Error -> idsRes
        }
    }
}

class FirestoreBlockRepository(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) : BlockRepository {

    override suspend fun blockUser(
        blockerUid: String,
        blockedUid: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) {
            return@withContext ProfileOperationResult.Error("Blocker UID and Blocked UID cannot be empty.")
        }
        if (blockerUid == blockedUid) {
            return@withContext ProfileOperationResult.Error("Self-blocking is not allowed.")
        }

        try {
            val firestore = firestoreSupplier()
            val blockDocId = "${blockerUid}_${blockedUid}"
            val blockRef = firestore.collection("blocks").document(blockDocId)

            val followRef1 = firestore.collection("follows").document("${blockerUid}_${blockedUid}")
            val followRef2 = firestore.collection("follows").document("${blockedUid}_${blockerUid}")

            val blockerProfileRef = firestore.collection("public_profiles").document(blockerUid)
            val blockedProfileRef = firestore.collection("public_profiles").document(blockedUid)

            firestore.runTransaction { transaction ->
                // READS FIRST
                val blockSnap = transaction.get(blockRef)
                val followSnap1 = transaction.get(followRef1)
                val followSnap2 = transaction.get(followRef2)
                val blockerProfileSnap = transaction.get(blockerProfileRef)
                val blockedProfileSnap = transaction.get(blockedProfileRef)

                if (!blockSnap.exists()) {
                    transaction.set(
                        blockRef,
                        mapOf(
                            "blockerUid" to blockerUid,
                            "blockedUid" to blockedUid,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                }

                // If blocker was following blocked user, remove follow relationship and update counters
                if (followSnap1.exists()) {
                    transaction.delete(followRef1)
                    if (blockerProfileSnap.exists()) {
                        val following = (blockerProfileSnap.getLong("followingCount") ?: 0L)
                        transaction.update(blockerProfileRef, "followingCount", (following - 1).coerceAtLeast(0L))
                    }
                    if (blockedProfileSnap.exists()) {
                        val followers = (blockedProfileSnap.getLong("followerCount") ?: 0L)
                        transaction.update(blockedProfileRef, "followerCount", (followers - 1).coerceAtLeast(0L))
                    }
                }

                // If blocked user was following blocker, remove follow relationship and update counters
                if (followSnap2.exists()) {
                    transaction.delete(followRef2)
                    if (blockedProfileSnap.exists()) {
                        val following = (blockedProfileSnap.getLong("followingCount") ?: 0L)
                        transaction.update(blockedProfileRef, "followingCount", (following - 1).coerceAtLeast(0L))
                    }
                    if (blockerProfileSnap.exists()) {
                        val followers = (blockerProfileSnap.getLong("followerCount") ?: 0L)
                        transaction.update(blockerProfileRef, "followerCount", (followers - 1).coerceAtLeast(0L))
                    }
                }
            }.await()

            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to block user", cause = e)
        }
    }

    override suspend fun unblockUser(
        blockerUid: String,
        blockedUid: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) {
            return@withContext ProfileOperationResult.Error("Blocker UID and Blocked UID cannot be empty.")
        }

        try {
            val firestore = firestoreSupplier()
            val blockDocId = "${blockerUid}_${blockedUid}"
            firestore.collection("blocks").document(blockDocId).delete().await()
            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to unblock user", cause = e)
        }
    }

    override suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean = withContext(Dispatchers.IO) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return@withContext false
        try {
            val firestore = firestoreSupplier()
            val snap = firestore.collection("blocks").document("${blockerUid}_${blockedUid}").get().await()
            snap.exists()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isEitherBlocked(uidA: String, uidB: String): Boolean = withContext(Dispatchers.IO) {
        if (uidA.isBlank() || uidB.isBlank()) return@withContext false
        coroutineScope {
            val ab = async { isBlocked(uidA, uidB) }
            val ba = async { isBlocked(uidB, uidA) }
            ab.await() || ba.await()
        }
    }

    override suspend fun getBlockedUserIds(blockerUid: String): ProfileOperationResult<List<String>> = withContext(Dispatchers.IO) {
        if (blockerUid.isBlank()) return@withContext ProfileOperationResult.Success(emptyList())
        try {
            val firestore = firestoreSupplier()
            val snap = firestore.collection("blocks")
                .whereEqualTo("blockerUid", blockerUid)
                .get()
                .await()
            val ids = snap.documents
                .sortedByDescending { it.getLong("timestamp") ?: 0L }
                .mapNotNull { it.getString("blockedUid")?.takeIf { uid -> uid.isNotBlank() } }
                .distinct()
            ProfileOperationResult.Success(ids)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to fetch blocked users", cause = e)
        }
    }

    override suspend fun getBlockedProfiles(blockerUid: String): ProfileOperationResult<List<PublicProfile>> = withContext(Dispatchers.IO) {
        if (blockerUid.isBlank()) return@withContext ProfileOperationResult.Success(emptyList())
        try {
            val firestore = firestoreSupplier()
            val snap = firestore.collection("blocks")
                .whereEqualTo("blockerUid", blockerUid)
                .get()
                .await()
            val blockedUids = snap.documents
                .sortedByDescending { it.getLong("timestamp") ?: 0L }
                .mapNotNull { it.getString("blockedUid")?.takeIf { uid -> uid.isNotBlank() } }
                .distinct()

            if (blockedUids.isEmpty()) {
                return@withContext ProfileOperationResult.Success(emptyList())
            }

            val snapshots = coroutineScope {
                blockedUids.chunked(10).map { chunk ->
                    async {
                        firestore.collection("public_profiles")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()
                    }
                }.awaitAll()
            }

            val profileMap = mutableMapOf<String, PublicProfile>()
            for (profilesSnap in snapshots) {
                for (doc in profilesSnap.documents) {
                    val data = doc.data ?: continue
                    profileMap[doc.id] = PublicProfile(
                        uid = doc.id,
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
                        followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0
                    )
                }
            }

            val orderedProfiles = blockedUids.map { uid ->
                profileMap[uid] ?: PublicProfile(
                    uid = uid,
                    displayName = "WorkDaily Member",
                    username = uid.take(10)
                )
            }
            ProfileOperationResult.Success(orderedProfiles)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to load blocked users", cause = e)
        }
    }

    override suspend fun removeFollower(
        targetUid: String,
        followerUid: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (targetUid.isBlank() || followerUid.isBlank()) {
            return@withContext ProfileOperationResult.Error("Target UID and Follower UID cannot be empty.")
        }

        try {
            val firestore = firestoreSupplier()
            val followDocId = "${followerUid}_${targetUid}"
            val followRef = firestore.collection("follows").document(followDocId)

            val followerProfileRef = firestore.collection("public_profiles").document(followerUid)
            val targetProfileRef = firestore.collection("public_profiles").document(targetUid)

            firestore.runTransaction { transaction ->
                val followSnap = transaction.get(followRef)
                val targetSnap = transaction.get(targetProfileRef)
                val followerSnap = transaction.get(followerProfileRef)

                if (!followSnap.exists()) return@runTransaction

                transaction.delete(followRef)

                if (targetSnap.exists()) {
                    val followers = (targetSnap.getLong("followerCount") ?: 0L)
                    transaction.update(targetProfileRef, "followerCount", (followers - 1).coerceAtLeast(0L))
                }

                if (followerSnap.exists()) {
                    val following = (followerSnap.getLong("followingCount") ?: 0L)
                    transaction.update(followerProfileRef, "followingCount", (following - 1).coerceAtLeast(0L))
                }
            }.await()

            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to remove follower", cause = e)
        }
    }
}
