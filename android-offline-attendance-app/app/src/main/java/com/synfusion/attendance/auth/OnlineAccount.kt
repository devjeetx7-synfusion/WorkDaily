package com.synfusion.attendance.auth

/**
 * Clean domain representation of an authenticated online account.
 * Maintains strict separation from local WorkDaily workspace identity (UserEntity.id).
 */
data class OnlineAccount(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false
)
