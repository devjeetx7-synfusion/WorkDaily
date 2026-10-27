package com.synfusion.attendance.online

/**
 * Publicly visible user profile stored in Firestore collection `public_profiles/{firebaseUid}`.
 *
 * CRITICAL PRIVACY BOUNDARY:
 * Strictly contains user identity fields intended for public discovery, social interactions, and profile viewing.
 * ABSOLUTELY NEVER contains email, phone number, dailyWage, attendance, overtime, advance, earnings, salary,
 * settlements, passbook history, local workspace ID, or private account data.
 */
data class PublicProfile(
    val uid: String,
    val displayName: String = "",
    val username: String = "",
    val usernameLower: String = username.lowercase(),
    val displayNameLower: String = displayName.lowercase(),
    val bio: String = "",
    val profession: String = "Site Worker",
    val photoUrl: String? = null,
    val gender: String = "",
    val dateOfBirth: String? = null,
    val countryCode: String = "",
    val isPublic: Boolean = true,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
