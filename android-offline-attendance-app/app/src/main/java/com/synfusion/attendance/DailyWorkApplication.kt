package com.synfusion.attendance

import android.app.Application
import com.synfusion.attendance.data.AttendanceDatabase

class DailyWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize SQLite database eagerly
        AttendanceDatabase.getInstance(this)
    }
}
