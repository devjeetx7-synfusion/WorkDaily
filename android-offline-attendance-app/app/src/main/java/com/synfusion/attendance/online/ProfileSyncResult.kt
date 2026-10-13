package com.synfusion.attendance.online

/**
 * Result sealed hierarchy for online profile synchronization operations.
 */
sealed class ProfileSyncResult {
    data class Success(val profile: OnlineProfile) : ProfileSyncResult()
    data class WorkspaceMismatch(
        val existingLocalWorkspaceIdInFirestore: String,
        val currentLocalWorkspaceId: String
    ) : ProfileSyncResult()
    data class OfflineOrError(val message: String, val cause: Throwable? = null) : ProfileSyncResult()
}
