package com.synfusion.workdailyadmin

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.synfusion.workdailyadmin.crash.GlobalCrashHandler

class AdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.initialize(this)
        try {
            val existingApps = FirebaseApp.getApps(this)
            if (existingApps.isEmpty()) {
                val initializedApp = FirebaseApp.initializeApp(this)
                if (initializedApp == null) {
                    Log.e(TAG, "Firebase default app was not initialized. Verify the Google Services plugin is applied and app/google-services.json matches com.synfusion.workdailyadmin.")
                } else {
                    Log.i(TAG, "Firebase default app initialized.")
                }
            } else {
                Log.i(TAG, "Firebase app already initialized.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed (" + e.javaClass.simpleName + "). Check app configuration.")
        }
    }

    companion object {
        private const val TAG = "AdminApplication"
    }
}
