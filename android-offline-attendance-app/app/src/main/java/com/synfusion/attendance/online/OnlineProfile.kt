package com.synfusion.attendance.online

/**
 * Allowlisted domain model for WorkDaily online account profile stored in Firestore (`users/{firebaseUid}`).
 *
 * CRITICAL PRIVACY BOUNDARY:
 * Strictly contains user identity and metadata required for online WorkDaily account management.
 * NEVER contains dailyWage, attendance, overtime, advance, earnings, salary, settlements, passbook history,
 * notes, or work history.
 */
data class OnlineProfile(
    val uid: String,
    val localWorkspaceId: String,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val countryCode: String? = "IN",
    val currencyCode: String? = "INR",
    val emailVerified: Boolean = false,
    val accountStatus: String = "active",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
