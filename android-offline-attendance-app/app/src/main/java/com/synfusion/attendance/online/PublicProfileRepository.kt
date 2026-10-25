package com.synfusion.attendance.online

sealed class UsernameAvailabilityResult {
    object Available : UsernameAvailabilityResult()
    object AlreadyTaken : UsernameAvailabilityResult()
    data class Invalid(val reason: String) : UsernameAvailabilityResult()
    data class Error(val message: String) : UsernameAvailabilityResult()
}

sealed class ProfileOperationResult<out T> {
    data class Success<out T>(val data: T) : ProfileOperationResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : ProfileOperationResult<Nothing>()
}

interface PublicProfileRepository {
    suspend fun getPublicProfile(firebaseUid: String): ProfileOperationResult<PublicProfile?>

    suspend fun checkUsernameAvailability(username: String): UsernameAvailabilityResult

    suspend fun ensureOrCreatePublicProfile(
        firebaseUid: String,
        displayName: String?,
        photoUrl: String?
    ): ProfileOperationResult<PublicProfile>

    suspend fun updatePublicProfile(
        firebaseUid: String,
        newUsername: String,
        displayName: String,
        bio: String,
        profession: String,
        photoUrl: String?,
        isPublic: Boolean
    ): ProfileOperationResult<PublicProfile>

    suspend fun searchPublicProfiles(
        query: String,
        currentUserId: String?,
        limit: Int = 20
    ): ProfileOperationResult<List<PublicProfile>>
}
