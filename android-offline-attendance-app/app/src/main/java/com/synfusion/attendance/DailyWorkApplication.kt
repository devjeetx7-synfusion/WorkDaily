package com.synfusion.attendance

import android.app.Application
import android.content.Intent
import android.os.Process
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.ui.CrashActivity
import kotlin.system.exitProcess

class DailyWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        // Initialize SQLite database eagerly
        AttendanceDatabase.getInstance(this)
    }

    private fun setupCrashHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                    putExtra(CrashActivity.EXTRA_ERROR_INFO, android.util.Log.getStackTraceString(throwable))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to default in case of launcher failure
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }

            // Kill the original process
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}
