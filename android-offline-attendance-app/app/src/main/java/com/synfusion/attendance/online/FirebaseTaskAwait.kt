package com.synfusion.attendance.online

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges a Play Services [Task] into a coroutine.
 *
 * The module intentionally does not depend on `kotlinx-coroutines-play-services`, so this is the
 * single await helper used by the Live backend instead of `Task.await()`. Cancelling the calling
 * coroutine simply stops waiting; Firebase keeps the operation itself running.
 */
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        val error = task.exception
        if (error != null) {
            continuation.resumeWithException(error)
        } else {
            @Suppress("UNCHECKED_CAST")
            continuation.resume(task.result as T)
        }
    }
}
