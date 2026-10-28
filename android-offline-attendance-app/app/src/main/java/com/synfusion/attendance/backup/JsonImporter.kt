package com.synfusion.attendance.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject
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

    fun getLenientGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Int::class.java, JsonDeserializer { json, _, _ ->
                if (json == null || json.isJsonNull) 0
                else if (json.isJsonPrimitive) {
                    val prim = json.asJsonPrimitive
                    if (prim.isNumber) prim.asNumber.toInt()
                    else if (prim.isString) prim.asString.toDoubleOrNull()?.toInt() ?: 0
                    else 0
                } else 0
            })
            .registerTypeAdapter(Double::class.java, JsonDeserializer { json, _, _ ->
                if (json == null || json.isJsonNull) 0.0
                else if (json.isJsonPrimitive) {
                    val prim = json.asJsonPrimitive
                    if (prim.isNumber) prim.asNumber.toDouble()
                    else if (prim.isString) prim.asString.toDoubleOrNull() ?: 0.0
                    else 0.0
                } else 0.0
            })
            .registerTypeAdapter(Long::class.java, JsonDeserializer { json, _, _ ->
                if (json == null || json.isJsonNull) 0L
                else if (json.isJsonPrimitive) {
                    val prim = json.asJsonPrimitive
                    if (prim.isNumber) prim.asNumber.toLong()
                    else if (prim.isString) prim.asString.toLongOrNull() ?: (prim.asString.toDoubleOrNull()?.toLong() ?: 0L)
                    else 0L
                } else 0L
            })
            .setLenient()
            .create()
    }

    suspend fun parseAndMigrateBackup(backupDir: File, logger: BackupLogger): CanonicalBackupPayload {
        val gson = getLenientGson()
        val manifest = ManifestManager.getOrSynthesizeManifest(backupDir, logger)
        val formatVersion = manifest?.backupFormatVersion ?: 1

        val dataFile = File(backupDir, "data.json")
        if (dataFile.exists()) {
            val content = dataFile.readText(StandardCharsets.UTF_8)
            var rawPayload = try {
                gson.fromJson(content, BackupV1Payload::class.java)
            } catch (e: Exception) {
                null
            }

            // Fallback check for singular or alias property names in JSON object
            if (rawPayload == null || (rawPayload.users.isEmpty() && rawPayload.attendance.isEmpty())) {
                try {
                    val jsonObj = gson.fromJson(content, JsonObject::class.java)
                    var users = rawPayload?.users ?: emptyList()
                    var attendance = rawPayload?.attendance ?: emptyList()
                    var summaries = rawPayload?.summaries ?: emptyList()

                    if (users.isEmpty() && jsonObj.has("user")) {
                        users = parseListOrSingle(jsonObj.get("user").toString(), gson, UserV1Dto::class.java)
                    }
                    if (attendance.isEmpty() && jsonObj.has("attendances")) {
                        attendance = parseListOrSingle(jsonObj.get("attendances").toString(), gson, AttendanceV1Dto::class.java)
                    }
                    if (summaries.isEmpty() && jsonObj.has("summary")) {
                        summaries = parseListOrSingle(jsonObj.get("summary").toString(), gson, SummaryV1Dto::class.java)
                    }

                    rawPayload = BackupV1Payload(
                        users = users,
                        attendance = attendance,
                        summaries = summaries,
                        preferences = rawPayload?.preferences ?: emptyMap()
                    )
                } catch (e: Exception) {
                    logger.error("Error parsing alias properties from data.json: ${e.message}")
                }
            }

            return MigrationRegistry.migrateToCanonical(rawPayload ?: BackupV1Payload(), formatVersion, manifest, logger)
        }

        // Legacy backup format fallback (database/ directory)
        val dbDir = File(backupDir, "database")
        var users: List<UserV1Dto> = emptyList()
        var attendances: List<AttendanceV1Dto> = emptyList()
        var summaries: List<SummaryV1Dto> = emptyList()

        val userFile = File(dbDir, "user.json")
        if (userFile.exists()) {
            users = parseListOrSingle(userFile.readText(StandardCharsets.UTF_8), gson, UserV1Dto::class.java)
        }

        val attendanceFile = File(dbDir, "attendance.json")
        if (attendanceFile.exists()) {
            attendances = parseListOrSingle(attendanceFile.readText(StandardCharsets.UTF_8), gson, AttendanceV1Dto::class.java)
        }

        val summaryFile = File(dbDir, "summary.json")
        if (summaryFile.exists()) {
            summaries = parseListOrSingle(summaryFile.readText(StandardCharsets.UTF_8), gson, SummaryV1Dto::class.java)
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
                gender = u.gender,
                profession = u.profession,
                bio = u.bio,
                dateOfBirth = u.dateOfBirth,
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

    private fun <T> parseListOrSingle(jsonContent: String, gson: Gson, clazz: Class<T>): List<T> {
        val trimmed = jsonContent.trim()
        if (trimmed.startsWith("[")) {
            val listType = TypeToken.getParameterized(List::class.java, clazz).type
            return gson.fromJson<List<T>>(trimmed, listType) ?: emptyList()
        } else if (trimmed.startsWith("{")) {
            val single = gson.fromJson(trimmed, clazz)
            return if (single != null) listOf(single) else emptyList()
        }
        return emptyList()
    }

    fun importPreferences(backupDir: File, logger: BackupLogger): Pair<Map<String, Any>, Map<String, Any>> {
        val gson = getLenientGson()
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
