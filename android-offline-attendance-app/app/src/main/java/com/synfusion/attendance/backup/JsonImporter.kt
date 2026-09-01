package com.synfusion.attendance.backup

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity
import java.io.File
import java.nio.charset.StandardCharsets

object JsonImporter {

    suspend fun importDatabase(db: AttendanceDatabase, backupDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> {
        val gson = Gson()
        val counts = mutableMapOf<String, Int>()

        var users: List<UserEntity> = emptyList()
        var attendances: List<AttendanceEntity> = emptyList()
        var summaries: List<SummaryEntity> = emptyList()

        val dataFile = File(backupDir, "data.json")
        if (dataFile.exists()) {
            dataFile.reader(StandardCharsets.UTF_8).use { reader ->
                val payload = gson.fromJson(reader, BackupDataPayload::class.java)
                if (payload != null) {
                    users = payload.users
                    attendances = payload.attendance
                    summaries = payload.summaries
                }
            }
        } else {
            // Legacy backup format fallback (database/ directory)
            val dbDir = File(backupDir, "database")
            val userFile = File(dbDir, "user.json")
            if (userFile.exists()) {
                userFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<UserEntity>>() {}.type
                    users = gson.fromJson(reader, type) ?: emptyList()
                }
            }

            val attendanceFile = File(dbDir, "attendance.json")
            if (attendanceFile.exists()) {
                attendanceFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<AttendanceEntity>>() {}.type
                    attendances = gson.fromJson(reader, type) ?: emptyList()
                }
            }

            val summaryFile = File(dbDir, "summary.json")
            if (summaryFile.exists()) {
                summaryFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<SummaryEntity>>() {}.type
                    summaries = gson.fromJson(reader, type) ?: emptyList()
                }
            }
        }

        // Import User Profile
        if (users.isNotEmpty()) {
            if (!merge) {
                db.userDao().clearAll()
            }
            db.userDao().insertUser(users.first())
            counts["users"] = 1
        } else {
            counts["users"] = 0
        }

        // Import Attendance Records
        if (attendances.isNotEmpty()) {
            if (!merge) {
                db.attendanceDao().clearAll()
            }
            var imported = 0
            attendances.forEach { att ->
                db.attendanceDao().insertAttendance(att)
                imported++
            }
            counts["attendance"] = imported
        } else {
            counts["attendance"] = 0
        }

        // Import Summary Records
        if (summaries.isNotEmpty()) {
            if (!merge) {
                db.summaryDao().clearAll()
            }
            db.summaryDao().insertSummaries(summaries)
            counts["summaries"] = summaries.size
        } else {
            counts["summaries"] = 0
        }

        return counts
    }

    fun importPreferences(backupDir: File, logger: BackupLogger): Pair<Map<String, Any>, Map<String, Any>> {
        val gson = Gson()
        val dataFile = File(backupDir, "data.json")

        if (dataFile.exists()) {
            val dsMap = try {
                dataFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val payload = gson.fromJson(reader, BackupDataPayload::class.java)
                    payload?.preferences ?: emptyMap()
                }
            } catch (e: Exception) {
                emptyMap()
            }
            return Pair(dsMap, emptyMap())
        }

        // Legacy backup format fallback (preferences/ directory)
        val prefDir = File(backupDir, "preferences")
        val dsFile = File(prefDir, "datastore.json")
        val dataStoreMap = if (dsFile.exists()) {
            dsFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(reader, type) ?: emptyMap()
            }
        } else {
            emptyMap()
        }

        val spFile = File(prefDir, "sharedpreferences.json")
        val spMap = if (spFile.exists()) {
            spFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(reader, type) ?: emptyMap()
            }
        } else {
            emptyMap()
        }

        return Pair(dataStoreMap, spMap)
    }
}
