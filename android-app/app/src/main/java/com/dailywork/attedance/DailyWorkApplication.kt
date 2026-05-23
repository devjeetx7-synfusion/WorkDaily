package com.dailywork.attedance

import android.app.Application
import android.content.Intent
import android.os.Process
import com.dailywork.attedance.ui.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess
import android.app.ActivityManager
import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.dailywork.attedance.worker.SyncWorker
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy


class DailyWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        setupCrashHandler()
        setupWorkManager()
    }

    private fun setupCrashHandler() {
        // Prevent infinite loops if the error handler itself crashes
        if (isErrorHandlerProcess()) {
            return
        }

        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_ERROR_INFO, android.util.Log.getStackTraceString(throwable))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)

            // Allow Crashlytics to report the crash
            defaultExceptionHandler?.uncaughtException(thread, throwable)

            // Kill the original process
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    private fun isErrorHandlerProcess(): Boolean {
        val myPid = Process.myPid()
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses ?: emptyList()) {
            if (processInfo.pid == myPid) {
                return processInfo.processName.endsWith(":error_handler")
            }
        }
        return false
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

}
