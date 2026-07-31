package com.synfusion.attendance.backup

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupLogger(private val logFile: File) {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    init {
        logFile.parentFile?.mkdirs()
    }

    fun info(message: String) {
        log("INFO", message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        log("ERROR", message, throwable)
    }

    fun warn(message: String) {
        log("WARN", message)
    }

    @Synchronized
    private fun log(level: String, message: String, throwable: Throwable? = null) {
        try {
            val timestamp = sdf.format(Date())
            val logLine = "[$timestamp] [$level] $message"
            FileWriter(logFile, true).use { fw ->
                PrintWriter(fw).use { pw ->
                    pw.println(logLine)
                    throwable?.printStackTrace(pw)
                }
            }
        } catch (e: Exception) {
            // Fallback to logcat if file logging fails
            android.util.Log.e("BackupLogger", "Failed to write log to file", e)
        }
    }
}
