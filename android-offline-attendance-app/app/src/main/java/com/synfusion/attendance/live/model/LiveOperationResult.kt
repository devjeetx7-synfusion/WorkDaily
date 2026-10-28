package com.synfusion.attendance.live.model

/**
 * Result of a Live backend mutation.
 *
 * [isAuthError] marks "not signed in / not allowed", [isConflict] marks a stale revision or a
 * duplicate playlist entry, and [isTransient] marks network-style failures that can be retried.
 */
sealed class LiveOperationResult<out T> {
    data class Success<out T>(val data: T) : LiveOperationResult<T>()

    data class Error(
        val message: String,
        val isAuthError: Boolean = false,
        val isConflict: Boolean = false,
        val isTransient: Boolean = false,
        val cause: Throwable? = null
    ) : LiveOperationResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
}
