package com.synfusion.attendance.online

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Firestore implementation of [PublicProfileRepository].
 * Manages public profiles at `public_profiles/{firebaseUid}` and unique username reservations at `usernames/{normalizedUsername}`.
 *
 * CRITICAL DIRECTIVES MET:
 * 1. Strictly public data: ZERO financial, attendance, or private phone/email fields serialized.
 * 2. Atomic username reservation: uses Firestore transactions to prevent race conditions and duplicate claims.
 * 3. Prefix search: bounded query prefix matching on `usernameLower` and `displayNameLower`.
 * 4. Case-insensitive username validation & system reserved word checking via [UsernameValidator].
 */
class FirestorePublicProfileRepository(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : PublicProfileRepository {

    override suspend fun getPublicProfile(firebaseUid: String): ProfileOperationResult<PublicProfile?> = withContext(Dispatchers.IO) {
        try {
            val firestore = firestoreSupplier()
            val snapshot = firestore.collection("public_profiles").document(firebaseUid).get().await()

            if (snapshot.exists()) {
                val profile = mapSnapshotToProfile(snapshot.data, firebaseUid)
                ProfileOperationResult.Success(profile)
            } else {
                ProfileOperationResult.Success(null)
            }
        } catch (e: Exception) {
            ProfileOperationResult.Error(
                message = e.message ?: "Failed to retrieve public profile for $firebaseUid",
                cause = e
            )
        }
    }

    override suspend fun checkUsernameAvailability(username: String): UsernameAvailabilityResult {
        return checkUsernameAvailability(username, currentUserId = null)
    }

    override suspend fun checkUsernameAvailability(
        username: String,
        currentUserId: String?
    ): UsernameAvailabilityResult = withContext(Dispatchers.IO) {
        val validation = UsernameValidator.validate(username)
        if (validation is UsernameValidationResult.Invalid) {
            return@withContext UsernameAvailabilityResult.Invalid(validation.reason)
        }

        val normalized = UsernameValidator.normalize(username)

        try {
            val firestore = firestoreSupplier()
            val snapshot = firestore.collection("usernames").document(normalized).get().await()

            if (snapshot.exists()) {
                val ownerUid = snapshot.getString("uid")
                if (!currentUserId.isNullOrBlank() && ownerUid == currentUserId) {
                    UsernameAvailabilityResult.Available
                } else {
                    UsernameAvailabilityResult.AlreadyTaken
                }
            } else {
                UsernameAvailabilityResult.Available
            }
        } catch (e: Exception) {
            UsernameAvailabilityResult.Error(e.message ?: "Failed to verify username availability")
        }
    }

    override suspend fun ensureOrCreatePublicProfile(
        firebaseUid: String,
        displayName: String?,
        photoUrl: String?
    ): ProfileOperationResult<PublicProfile> = withContext(Dispatchers.IO) {
        try {
            val firestore = firestoreSupplier()
            val existing = firestore.collection("public_profiles").document(firebaseUid).get().await()

            if (existing.exists()) {
                val profile = mapSnapshotToProfile(existing.data, firebaseUid)
                return@withContext ProfileOperationResult.Success(profile)
            }

            // Public profile does not exist yet. Safe candidate username generation and transaction reservation.
            var candidate = UsernameValidator.generateCandidate(displayName, firebaseUid)
            var attempt = 0
            var finalProfile: PublicProfile? = null

            while (attempt < 5 && finalProfile == null) {
                val candidateNormalized = UsernameValidator.normalize(candidate)
                val profileRef = firestore.collection("public_profiles").document(firebaseUid)
                val usernameRef = firestore.collection("usernames").document(candidateNormalized)

                try {
                    firestore.runTransaction { transaction ->
                        val usernameSnap = transaction.get(usernameRef)
                        if (usernameSnap.exists()) {
                            val ownerUid = usernameSnap.getString("uid")
                            if (ownerUid != firebaseUid) {
                                throw IllegalStateException("USERNAME_TAKEN")
                            }
                        }

                        val now = System.currentTimeMillis()
                        val initialDisplayName = displayName?.takeIf { it.isNotBlank() } ?: "WorkDaily Member"
                        val newProfile = PublicProfile(
                            uid = firebaseUid,
                            displayName = initialDisplayName,
                            username = candidateNormalized,
                            usernameLower = candidateNormalized,
                            displayNameLower = initialDisplayName.lowercase(Locale.US),
                            bio = "",
                            profession = "Site Worker",
                            photoUrl = photoUrl,
                            isPublic = true,
                            createdAt = now,
                            updatedAt = now
                        )

                        transaction.set(usernameRef, mapOf("uid" to firebaseUid, "createdAt" to now))
                        transaction.set(profileRef, mapToFirestoreMap(newProfile))
                        finalProfile = newProfile
                    }.await()
                } catch (e: Exception) {
                    if (e.message == "USERNAME_TAKEN" || e.cause?.message == "USERNAME_TAKEN") {
                        attempt++
                        candidate = UsernameValidator.generateCandidate("$displayName$attempt", firebaseUid)
                    } else {
                        throw e
                    }
                }
            }

            if (finalProfile != null) {
                ProfileOperationResult.Success(finalProfile!!)
            } else {
                ProfileOperationResult.Error("Could not reserve a unique username after multiple attempts.")
            }
        } catch (e: Exception) {
            ProfileOperationResult.Error(
                message = e.message ?: "Failed to create public profile",
                cause = e
            )
        }
    }

    override suspend fun updatePublicProfile(
        firebaseUid: String,
        newUsername: String,
        displayName: String,
        bio: String,
        profession: String,
        photoUrl: String?,
        isPublic: Boolean
    ): ProfileOperationResult<PublicProfile> = withContext(Dispatchers.IO) {
        val validation = UsernameValidator.validate(newUsername)
        if (validation is UsernameValidationResult.Invalid) {
            return@withContext ProfileOperationResult.Error("Invalid username: ${validation.reason}")
        }

        val newNormalized = UsernameValidator.normalize(newUsername)
        val firestore = firestoreSupplier()

        try {
            val profileRef = firestore.collection("public_profiles").document(firebaseUid)
            val currentSnap = profileRef.get().await()

            val oldUsernameLower = currentSnap.getString("usernameLower") ?: ""
            val createdAt = currentSnap.getLong("createdAt") ?: System.currentTimeMillis()

            var updatedProfileResult: PublicProfile? = null

            firestore.runTransaction { transaction ->
                val now = System.currentTimeMillis()
                val txProfileSnap = transaction.get(profileRef)
                val txOldUsernameLower = txProfileSnap.getString("usernameLower") ?: oldUsernameLower
                val existingFollowerCount = (txProfileSnap.getLong("followerCount") ?: currentSnap.getLong("followerCount") ?: 0L).toInt()
                val existingFollowingCount = (txProfileSnap.getLong("followingCount") ?: currentSnap.getLong("followingCount") ?: 0L).toInt()
                val existingIsVerified = txProfileSnap.getBoolean("isVerified") ?: currentSnap.getBoolean("isVerified") ?: false
                val existingCreatedAt = txProfileSnap.getLong("createdAt") ?: createdAt

                val newUsernameRef = firestore.collection("usernames").document(newNormalized)
                val newUsernameSnap = if (txOldUsernameLower != newNormalized) {
                    transaction.get(newUsernameRef)
                } else {
                    null
                }

                if (txOldUsernameLower != newNormalized) {
                    if (newUsernameSnap != null && newUsernameSnap.exists()) {
                        val ownerUid = newUsernameSnap.getString("uid")
                        if (ownerUid != firebaseUid) {
                            throw IllegalStateException("USERNAME_TAKEN")
                        }
                    }

                    // Reserve new username
                    transaction.set(newUsernameRef, mapOf("uid" to firebaseUid, "createdAt" to now))

                    // Delete old reservation if it existed
                    if (txOldUsernameLower.isNotBlank()) {
                        val oldUsernameRef = firestore.collection("usernames").document(txOldUsernameLower)
                        transaction.delete(oldUsernameRef)
                    }
                }

                val cleanDisplayName = displayName.trim().ifEmpty { "WorkDaily Member" }
                val updated = PublicProfile(
                    uid = firebaseUid,
                    displayName = cleanDisplayName,
                    username = newNormalized,
                    usernameLower = newNormalized,
                    displayNameLower = cleanDisplayName.lowercase(Locale.US),
                    bio = bio.trim(),
                    profession = profession.trim().ifEmpty { "Site Worker" },
                    photoUrl = photoUrl,
                    isPublic = isPublic,
                    isVerified = existingIsVerified,
                    followerCount = existingFollowerCount,
                    followingCount = existingFollowingCount,
                    createdAt = existingCreatedAt,
                    updatedAt = now
                )

                transaction.set(profileRef, mapToFirestoreMap(updated), SetOptions.merge())
                updatedProfileResult = updated
            }.await()

            if (updatedProfileResult != null) {
                ProfileOperationResult.Success(updatedProfileResult!!)
            } else {
                ProfileOperationResult.Error("Failed to update profile: transaction returned null result")
            }
        } catch (e: Exception) {
            val msg = if (e.message == "USERNAME_TAKEN" || e.cause?.message == "USERNAME_TAKEN") {
                "Username @$newNormalized is already taken by another user."
            } else {
                e.message ?: "Failed to update public profile"
            }
            ProfileOperationResult.Error(message = msg, cause = e)
        }
    }

    override suspend fun searchPublicProfiles(
        query: String,
        currentUserId: String?,
        limit: Int
    ): ProfileOperationResult<List<PublicProfile>> = withContext(Dispatchers.IO) {
        val normalizedQuery = UsernameValidator.normalize(query)
        if (normalizedQuery.isBlank()) {
            return@withContext ProfileOperationResult.Success(emptyList())
        }

        try {
            val firestore = firestoreSupplier()
            val endQuery = normalizedQuery + "\uf8ff"

            // Prefix search by username
            val usernameQuerySnap = firestore.collection("public_profiles")
                .whereGreaterThanOrEqualTo("usernameLower", normalizedQuery)
                .whereLessThanOrEqualTo("usernameLower", endQuery)
                .limit(limit.toLong())
                .get()
                .await()

            // Prefix search by display name
            val displayNameQuerySnap = firestore.collection("public_profiles")
                .whereGreaterThanOrEqualTo("displayNameLower", normalizedQuery)
                .whereLessThanOrEqualTo("displayNameLower", endQuery)
                .limit(limit.toLong())
                .get()
                .await()

            val combined = mutableMapOf<String, PublicProfile>()

            for (doc in usernameQuerySnap.documents) {
                val profile = mapSnapshotToProfile(doc.data, doc.id)
                if (profile.isPublic && profile.uid != currentUserId) {
                    combined[profile.uid] = profile
                }
            }

            for (doc in displayNameQuerySnap.documents) {
                val profile = mapSnapshotToProfile(doc.data, doc.id)
                if (profile.isPublic && profile.uid != currentUserId) {
                    combined[profile.uid] = profile
                }
            }

            val results = combined.values.take(limit)
            ProfileOperationResult.Success(results)
        } catch (e: Exception) {
            ProfileOperationResult.Error(
                message = e.message ?: "Failed to execute user search",
                cause = e
            )
        }
    }

    private fun mapSnapshotToProfile(data: Map<String, Any?>?, docId: String): PublicProfile {
        if (data == null) return PublicProfile(uid = docId)

        return PublicProfile(
            uid = data["uid"] as? String ?: docId,
            displayName = data["displayName"] as? String ?: "",
            username = data["username"] as? String ?: "",
            usernameLower = data["usernameLower"] as? String ?: (data["username"] as? String ?: "").lowercase(Locale.US),
            displayNameLower = data["displayNameLower"] as? String ?: (data["displayName"] as? String ?: "").lowercase(Locale.US),
            bio = data["bio"] as? String ?: "",
            profession = data["profession"] as? String ?: "Site Worker",
            photoUrl = data["photoUrl"] as? String,
            isPublic = data["isPublic"] as? Boolean ?: true,
            followerCount = (data["followerCount"] as? Number)?.toInt() ?: 0,
            followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
            isVerified = data["isVerified"] as? Boolean ?: false,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun mapToFirestoreMap(profile: PublicProfile): Map<String, Any?> {
        return mapOf(
            "uid" to profile.uid,
            "displayName" to profile.displayName,
            "username" to profile.username,
            "usernameLower" to profile.usernameLower,
            "displayNameLower" to profile.displayNameLower,
            "bio" to profile.bio,
            "profession" to profile.profession,
            "photoUrl" to profile.photoUrl,
            "isPublic" to profile.isPublic,
            "followerCount" to profile.followerCount,
            "followingCount" to profile.followingCount,
            "isVerified" to profile.isVerified,
            "createdAt" to profile.createdAt,
            "updatedAt" to profile.updatedAt
        )
    }
}
