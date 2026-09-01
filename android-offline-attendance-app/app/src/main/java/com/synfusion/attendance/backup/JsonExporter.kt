package com.synfusion.attendance.backup

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity
import java.io.File
import java.nio.charset.StandardCharsets

object JsonExporter {

    suspend fun exportDatabase(db: AttendanceDatabase, outputDir: File, logger: BackupLogger): Map<String, Int> {
        val gson = Gson()
        val recordCounts = mutableMapOf<String, Int>()

        val users = db.userDao().getUser()?.let { listOf(it) } ?: emptyList()
        val attendances = db.attendanceDao().getPersonalAttendance()
        val summaries = db.summaryDao().getAllSummaries()

        recordCounts["users"] = users.size
        recordCounts["attendance"] = attendances.size
        recordCounts["summaries"] = summaries.size

        // Merge existing preferences if already exported into data.json
        val dataFile = File(outputDir, "data.json")
        var existingPrefs: Map<String, Any> = emptyMap()
        if (dataFile.exists()) {
            try {
                dataFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val payload = gson.fromJson(reader, BackupDataPayload::class.java)
                    if (payload?.preferences != null) {
                        existingPrefs = payload.preferences
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not read existing preferences for data.json: ${e.message}")
            }
        }

        val payload = BackupDataPayload(
            users = users,
            attendance = attendances,
            summaries = summaries,
            preferences = existingPrefs
        )

        dataFile.writer(StandardCharsets.UTF_8).use { writer ->
            gson.toJson(payload, writer)
        }

        logger.info("Exported database records to data.json (users: ${users.size}, attendance: ${attendances.size}, summaries: ${summaries.size}).")
        return recordCounts
    }

    fun exportPreferences(outputDir: File, dataStoreValues: Map<String, Any>, sharedPreferencesValues: Map<String, Any>, logger: BackupLogger) {
        val gson = Gson()
        val dataFile = File(outputDir, "data.json")

        var existingUsers = emptyList<UserEntity>()
        var existingAttendance = emptyList<AttendanceEntity>()
        var existingSummaries = emptyList<SummaryEntity>()

        if (dataFile.exists()) {
            try {
                dataFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val payload = gson.fromJson(reader, BackupDataPayload::class.java)
                    if (payload != null) {
                        existingUsers = payload.users
                        existingAttendance = payload.attendance
                        existingSummaries = payload.summaries
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not read existing DB records for data.json: ${e.message}")
            }
        }

        val payload = BackupDataPayload(
            users = existingUsers,
            attendance = existingAttendance,
            summaries = existingSummaries,
            preferences = dataStoreValues
        )

        dataFile.writer(StandardCharsets.UTF_8).use { writer ->
            gson.toJson(payload, writer)
        }

        logger.info("Exported preferences to data.json.")
    }
}
