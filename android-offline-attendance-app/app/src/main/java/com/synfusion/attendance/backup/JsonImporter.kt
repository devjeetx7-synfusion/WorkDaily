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
        val dbDir = File(backupDir, "database")
        val gson = Gson()
        val counts = mutableMapOf<String, Int>()

        // 1. Import User Profile
        val userFile = File(dbDir, "user.json")
        if (userFile.exists()) {
            userFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<UserEntity>>() {}.type
                val users = gson.fromJson<List<UserEntity>>(reader, type) ?: emptyList()
                if (users.isNotEmpty()) {
                    if (!merge) {
                        db.userDao().clearAll()
                    }
                    db.userDao().insertUser(users.first())
                    counts["users"] = 1
                }
            }
        }

        // 2. Import Attendance Records
        val attendanceFile = File(dbDir, "attendance.json")
        if (attendanceFile.exists()) {
            attendanceFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<AttendanceEntity>>() {}.type
                val attendances = gson.fromJson<List<AttendanceEntity>>(reader, type) ?: emptyList()
                if (!merge) {
                    db.attendanceDao().clearAll()
                }
                var imported = 0
                attendances.forEach { att ->
                    db.attendanceDao().insertAttendance(att)
                    imported++
                }
                counts["attendance"] = imported
            }
        }

        // 3. Import Summary Records
        val summaryFile = File(dbDir, "summary.json")
        if (summaryFile.exists()) {
            summaryFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<SummaryEntity>>() {}.type
                val summaries = gson.fromJson<List<SummaryEntity>>(reader, type) ?: emptyList()
                if (!merge) {
                    db.summaryDao().clearAll()
                }
                db.summaryDao().insertSummaries(summaries)
                counts["summaries"] = summaries.size
            }
        } else {
            counts["summaries"] = 0
        }

        return counts
    }

    fun importPreferences(backupDir: File, logger: BackupLogger): Pair<Map<String, Any>, Map<String, Any>> {
        val prefDir = File(backupDir, "preferences")
        val gson = Gson()

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
