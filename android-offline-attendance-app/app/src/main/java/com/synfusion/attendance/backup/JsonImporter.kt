package com.synfusion.attendance.backup

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synfusion.attendance.backup.dto.canonical.CanonicalBackupPayload
import com.synfusion.attendance.backup.dto.v1.*
import com.synfusion.attendance.backup.migration.MigrationRegistry
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity
import java.io.File
import java.nio.charset.StandardCharsets

object JsonImporter {

    suspend fun parseAndMigrateBackup(backupDir: File, logger: BackupLogger): CanonicalBackupPayload {
        val gson = Gson()
        val manifestFile = File(backupDir, "manifest.json")
        val manifest = ManifestManager.readManifest(manifestFile)
        val formatVersion = manifest?.backupFormatVersion ?: 1

        val dataFile = File(backupDir, "data.json")
        if (dataFile.exists()) {
            val rawPayload = dataFile.reader(StandardCharsets.UTF_8).use { reader ->
                gson.fromJson(reader, BackupV1Payload::class.java)
            } ?: BackupV1Payload()

            return MigrationRegistry.migrateToCanonical(rawPayload, formatVersion, manifest, logger)
        }

        // Legacy backup format fallback (database/ directory)
        val dbDir = File(backupDir, "database")
        var users: List<UserV1Dto> = emptyList()
        var attendances: List<AttendanceV1Dto> = emptyList()
        var summaries: List<SummaryV1Dto> = emptyList()

        val userFile = File(dbDir, "user.json")
        if (userFile.exists()) {
            userFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<UserV1Dto>>() {}.type
                users = gson.fromJson(reader, type) ?: emptyList()
            }
        }

        val attendanceFile = File(dbDir, "attendance.json")
        if (attendanceFile.exists()) {
            attendanceFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<AttendanceV1Dto>>() {}.type
                attendances = gson.fromJson(reader, type) ?: emptyList()
            }
        }

        val summaryFile = File(dbDir, "summary.json")
        if (summaryFile.exists()) {
            summaryFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<SummaryV1Dto>>() {}.type
                summaries = gson.fromJson(reader, type) ?: emptyList()
            }
        }

        val rawPayload = BackupV1Payload(
            users = users,
            attendance = attendances,
            summaries = summaries
        )

        return MigrationRegistry.migrateToCanonical(rawPayload, formatVersion, manifest, logger)
    }

    suspend fun importDatabase(db: AttendanceDatabase, backupDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> {
        val canonicalPayload = parseAndMigrateBackup(backupDir, logger)
        val counts = mutableMapOf<String, Int>()

        // Convert Canonical Users to Room UserEntity
        val userEntities = canonicalPayload.users.map { u ->
            UserEntity(
                id = u.id,
                name = u.name,
                phoneNumber = u.phoneNumber,
                email = u.email,
                role = u.role,
                dailyWage = u.dailyWage,
                countryCode = u.countryCode,
                currencyCode = u.currencyCode,
                createdAt = u.createdAt
            )
        }

        // Convert Canonical Attendance to Room AttendanceEntity
        val attendanceEntities = canonicalPayload.attendance.map { a ->
            AttendanceEntity(
                id = a.id,
                workerId = a.workerId,
                date = a.date,
                status = a.status,
                type = a.type,
                reason = a.reason,
                overtimeHours = a.overtimeHours,
                note = a.note,
                advanceAmount = a.advanceAmount,
                wage = a.wage,
                currencyCode = a.currencyCode,
                createdAt = a.createdAt
            )
        }

        // Convert Canonical Summaries to Room SummaryEntity
        val summaryEntities = canonicalPayload.summaries.map { s ->
            SummaryEntity(
                id = s.id,
                monthId = s.monthId,
                role = s.role,
                totalPresent = s.totalPresent,
                totalWages = s.totalWages,
                totalAdvance = s.totalAdvance,
                todayPresentCount = s.todayPresentCount,
                todayAbsentCount = s.todayAbsentCount
            )
        }

        // Import User Profile into temp/target DB
        if (userEntities.isNotEmpty()) {
            if (!merge) {
                db.userDao().clearAll()
            }
            db.userDao().insertUser(userEntities.first())
            counts["users"] = 1
        } else {
            counts["users"] = 0
        }

        // Import Attendance Records
        if (attendanceEntities.isNotEmpty()) {
            if (!merge) {
                db.attendanceDao().clearAll()
            }
            var imported = 0
            attendanceEntities.forEach { att ->
                db.attendanceDao().insertAttendance(att)
                imported++
            }
            counts["attendance"] = imported
        } else {
            counts["attendance"] = 0
        }

        // Import Summary Records
        if (summaryEntities.isNotEmpty()) {
            if (!merge) {
                db.summaryDao().clearAll()
            }
            db.summaryDao().insertSummaries(summaryEntities)
            counts["summaries"] = summaryEntities.size
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
                    val payload = gson.fromJson(reader, BackupV1Payload::class.java)
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
