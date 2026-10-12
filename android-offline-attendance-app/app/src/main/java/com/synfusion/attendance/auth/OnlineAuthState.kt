package com.synfusion.attendance.auth

/**
 * UI-ready sealed state representing online authentication status.
 */
sealed class OnlineAuthState {
    object Idle : OnlineAuthState()
    object Loading : OnlineAuthState()
    data class Authenticated(val account: OnlineAccount) : OnlineAuthState()
    object Unauthenticated : OnlineAuthState()
    object Cancelled : OnlineAuthState()
    object NetworkUnavailable : OnlineAuthState()
    data class Error(val message: String) : OnlineAuthState()
}
