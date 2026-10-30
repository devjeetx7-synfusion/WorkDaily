package com.synfusion.workdailyadmin.crash

import android.content.Context
import android.util.Log
import com.synfusion.workdailyadmin.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

class GlobalCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private val isHandlingCrash = AtomicBoolean(false)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Prevent recursive crash handling loops
        if (!isHandlingCrash.compareAndSet(false, true)) {
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        try {
            val sanitizedMessage = sanitizeStackTrace(throwable)
            Log.e(TAG, "FATAL EXCEPTION in thread [${thread.name}]: $sanitizedMessage", throwable)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Uncaught exception diagnostic: ${throwable.javaClass.simpleName} - ${throwable.localizedMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in GlobalCrashHandler execution", e)
        } finally {
            // Respect Android normal process termination behavior
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun sanitizeStackTrace(throwable: Throwable): String {
        val msg = throwable.message ?: "No message"
        // Strip sensitive authorization/token patterns if present
        return msg.replace(Regex("(?i)(password|token|bearer|secret|apiKey)=[^&\\s]+"), "$1=REDACTED")
    }

    companion object {
        private const val TAG = "GlobalCrashHandler"

        fun initialize(context: Context) {
            val currentDefault = Thread.getDefaultUncaughtExceptionHandler()
            if (currentDefault !is GlobalCrashHandler) {
                val handler = GlobalCrashHandler(context.applicationContext, currentDefault)
                Thread.setDefaultUncaughtExceptionHandler(handler)
                Log.i(TAG, "GlobalCrashHandler successfully initialized.")
            }
        }
    }
}
