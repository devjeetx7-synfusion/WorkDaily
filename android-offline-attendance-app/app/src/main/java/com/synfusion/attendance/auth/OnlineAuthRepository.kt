package com.synfusion.attendance.auth

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean abstraction for online authentication operations.
 */
interface OnlineAuthRepository {
    val authState: StateFlow<OnlineAuthState>
    fun getCurrentUser(): OnlineAccount?
    suspend fun signInWithGoogle(context: Context, webClientId: String? = null): OnlineAuthState
    suspend fun signOut()
}
