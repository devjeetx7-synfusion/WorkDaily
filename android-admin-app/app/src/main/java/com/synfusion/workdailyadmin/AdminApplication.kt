package com.synfusion.workdailyadmin

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.synfusion.workdailyadmin.crash.GlobalCrashHandler

class AdminApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Centralized Global Crash Handler
        GlobalCrashHandler.initialize(this)

        // 2. Safe Firebase Initialization
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.i(TAG, "FirebaseApp initialized successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseApp initialization deferred or configuration missing: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "AdminApplication"
    }
}
