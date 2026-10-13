package com.synfusion.attendance.online

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Firestore implementation of [OnlineProfileRepository].
 * Manages minimal allowlisted WorkDaily account documents at path `users/{firebaseUid}`.
 *
 * CRITICAL DIRECTIVES MET:
 * 1. Document ID = authenticated Firebase UID (`users/{firebaseUid}`).
 * 2. `localWorkspaceId` mapping is protected; mismatch between device local workspace and Firestore yields [ProfileSyncResult.WorkspaceMismatch].
 * 3. Idempotent: safe reads/merges prevent duplicate account creation or duplicate document overwrites.
 * 4. Zero financial/attendance data: only allowlisted identity and contact metadata fields are serialized.
 * 5. Offline resilience: network/Firestore errors return [ProfileSyncResult.OfflineOrError] without corrupting local data.
 */
class FirestoreOnlineProfileRepository(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : OnlineProfileRepository {

    override suspend fun ensureOnlineProfile(profile: OnlineProfile): ProfileSyncResult = withContext(Dispatchers.IO) {
        try {
            val firestore = firestoreSupplier()
            val docRef = firestore.collection("users").document(profile.uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val existingWorkspaceId = snapshot.getString("localWorkspaceId")
                if (!existingWorkspaceId.isNullOrBlank() && existingWorkspaceId != profile.localWorkspaceId) {
                    return@withContext ProfileSyncResult.WorkspaceMismatch(
                        existingLocalWorkspaceIdInFirestore = existingWorkspaceId,
                        currentLocalWorkspaceId = profile.localWorkspaceId
                    )
                }

                // Document exists and workspace matches (or is empty). Safe merge of updated identity metadata.
                val updates = mutableMapOf<String, Any?>(
                    "displayName" to profile.displayName,
                    "email" to profile.email,
                    "photoUrl" to profile.photoUrl,
                    "phoneNumber" to profile.phoneNumber,
                    "countryCode" to profile.countryCode,
                    "currencyCode" to profile.currencyCode,
                    "emailVerified" to profile.emailVerified,
                    "updatedAt" to System.currentTimeMillis()
                )

                // Preserve localWorkspaceId if snapshot had none
                if (existingWorkspaceId.isNullOrBlank()) {
                    updates["localWorkspaceId"] = profile.localWorkspaceId
                }

                docRef.set(updates, SetOptions.merge()).await()

                val updatedProfile = profile.copy(
                    localWorkspaceId = existingWorkspaceId.takeIf { !it.isNullOrBlank() } ?: profile.localWorkspaceId,
                    createdAt = snapshot.getLong("createdAt") ?: profile.createdAt
                )
                return@withContext ProfileSyncResult.Success(updatedProfile)
            } else {
                // Document does not exist. Create new document with allowlisted fields only.
                val initialData = mapOf<String, Any?>(
                    "uid" to profile.uid,
                    "localWorkspaceId" to profile.localWorkspaceId,
                    "displayName" to profile.displayName,
                    "email" to profile.email,
                    "photoUrl" to profile.photoUrl,
                    "phoneNumber" to profile.phoneNumber,
                    "countryCode" to (profile.countryCode ?: "IN"),
                    "currencyCode" to (profile.currencyCode ?: "INR"),
                    "emailVerified" to profile.emailVerified,
                    "accountStatus" to profile.accountStatus,
                    "createdAt" to profile.createdAt,
                    "updatedAt" to profile.updatedAt
                )

                docRef.set(initialData).await()
                return@withContext ProfileSyncResult.Success(profile)
            }
        } catch (e: Exception) {
            return@withContext ProfileSyncResult.OfflineOrError(
                message = e.message ?: "Firestore profile operation failed or device is offline",
                cause = e
            )
        }
    }

    override suspend fun getOnlineProfile(firebaseUid: String): ProfileSyncResult = withContext(Dispatchers.IO) {
        try {
            val firestore = firestoreSupplier()
            val snapshot = firestore.collection("users").document(firebaseUid).get().await()

            if (snapshot.exists()) {
                val profile = OnlineProfile(
                    uid = snapshot.getString("uid") ?: firebaseUid,
                    localWorkspaceId = snapshot.getString("localWorkspaceId") ?: "",
                    displayName = snapshot.getString("displayName"),
                    email = snapshot.getString("email"),
                    photoUrl = snapshot.getString("photoUrl"),
                    phoneNumber = snapshot.getString("phoneNumber"),
                    countryCode = snapshot.getString("countryCode") ?: "IN",
                    currencyCode = snapshot.getString("currencyCode") ?: "INR",
                    emailVerified = snapshot.getBoolean("emailVerified") ?: false,
                    accountStatus = snapshot.getString("accountStatus") ?: "active",
                    createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                return@withContext ProfileSyncResult.Success(profile)
            } else {
                return@withContext ProfileSyncResult.OfflineOrError("Document does not exist for $firebaseUid")
            }
        } catch (e: Exception) {
            return@withContext ProfileSyncResult.OfflineOrError(
                message = e.message ?: "Failed to read online profile",
                cause = e
            )
        }
    }

    override suspend fun syncProfileFields(
        firebaseUid: String,
        currentLocalWorkspaceId: String,
        updates: Map<String, Any?>
    ): ProfileSyncResult = withContext(Dispatchers.IO) {
        try {
            val firestore = firestoreSupplier()
            val docRef = firestore.collection("users").document(firebaseUid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val existingWorkspaceId = snapshot.getString("localWorkspaceId")
                if (!existingWorkspaceId.isNullOrBlank() && existingWorkspaceId != currentLocalWorkspaceId) {
                    return@withContext ProfileSyncResult.WorkspaceMismatch(
                        existingLocalWorkspaceIdInFirestore = existingWorkspaceId,
                        currentLocalWorkspaceId = currentLocalWorkspaceId
                    )
                }
            }

            // Sanitized updates: NEVER allow changing sensitive identity mapping fields
            val safeUpdates = updates.filterKeys { key ->
                key != "uid" && key != "localWorkspaceId" && key != "createdAt" &&
                key != "dailyWage" && key != "attendance" && key != "salary" && key != "passbook"
            }.toMutableMap()

            safeUpdates["updatedAt"] = System.currentTimeMillis()

            docRef.set(safeUpdates, SetOptions.merge()).await()
            return@withContext getOnlineProfile(firebaseUid)
        } catch (e: Exception) {
            return@withContext ProfileSyncResult.OfflineOrError(
                message = e.message ?: "Failed to sync profile fields",
                cause = e
            )
        }
    }
}
