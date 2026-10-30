package com.synfusion.workdailyadmin.crash

import android.content.Context
import android.util.Log
import com.synfusion.workdailyadmin.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Diagnostic wrapper that preserves Android's original uncaught-exception handler.
 * It records a sanitized summary and always delegates the original Throwable.
 */
class GlobalCrashHandler private constructor(
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private val isHandlingCrash = AtomicBoolean(false)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!isHandlingCrash.compareAndSet(false, true)) {
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        try {
            // Do not pass the original Throwable to Log: its message and nested causes
            // may contain user-provided data, credentials, or request payloads.
            Log.e(
                TAG,
                "FATAL EXCEPTION in thread [${thread.name}]: " +
                    "${throwable.javaClass.name}: ${sanitize(throwable.message)}"
            )
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Stack trace follows in debug-only diagnostics:")
                val writer = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(writer))
                Log.e(TAG, writer.toString())
            }
        } catch (handlerFailure: Throwable) {
            // Crash reporting must never replace the original application failure.
            try {
                Log.e(TAG, "Global crash diagnostics failed: ${handlerFailure.javaClass.simpleName}")
            } catch (_: Throwable) {
                // Nothing else is safe to do here.
            }
        } finally {
            // Delegate exactly once so Android performs its normal fatal-process handling.
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun sanitize(message: String?): String {
        if (message.isNullOrBlank()) return "No message"
        return SECRET_PATTERN.replace(message) { match ->
            "${match.groupValues[1]}=REDACTED"
        }.take(MAX_MESSAGE_LENGTH)
    }

    companion object {
        private const val TAG = "GlobalCrashHandler"
        private const val MAX_MESSAGE_LENGTH = 1000
        private val SECRET_PATTERN = Regex(
            "(?i)(password|token|bearer|secret|api[_-]?key)=([^&\\s]+)"
        )

        fun initialize(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is GlobalCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(current))
                Log.i(TAG, "Global crash diagnostics initialized.")
            }
        }
    }
}
