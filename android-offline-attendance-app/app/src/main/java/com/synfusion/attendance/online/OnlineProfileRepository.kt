package com.synfusion.attendance.online

/**
 * Interface defining online profile repository operations for WorkDaily online accounts.
 */
interface OnlineProfileRepository {
    /**
     * Idempotently ensures an online profile exists in Firestore under `users/{profile.uid}`.
     * Detects workspace mismatches if `users/{profile.uid}.localWorkspaceId` already exists and differs.
     */
    suspend fun ensureOnlineProfile(profile: OnlineProfile): ProfileSyncResult

    /**
     * Retrieves the online profile document for the specified authenticated Firebase UID.
     */
    suspend fun getOnlineProfile(firebaseUid: String): ProfileSyncResult

    /**
     * Updates permitted profile fields (displayName, photoUrl, phoneNumber, countryCode, currencyCode, etc.)
     * after validating identity mapping.
     */
    suspend fun syncProfileFields(
        firebaseUid: String,
        currentLocalWorkspaceId: String,
        updates: Map<String, Any?>
    ): ProfileSyncResult
}
