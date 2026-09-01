package com.synfusion.attendance.backup

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import java.io.File
import java.nio.charset.StandardCharsets

object SchemaValidator {

    fun validateJsonSchema(jsonFile: File, requiredKeys: List<String>): Boolean {
        if (!jsonFile.exists()) return false
        val gson = Gson()
        return try {
            val content = jsonFile.readText(StandardCharsets.UTF_8).trim()
            if (content.startsWith("[")) {
                true
            } else if (content.startsWith("{")) {
                val map = gson.fromJson(content, Map::class.java)
                requiredKeys.all { map.containsKey(it) }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

enum class BackupValidationCode {
    VALID,
    MISSING_MANIFEST,
    INVALID_MANIFEST,
    UNSUPPORTED_VERSION,
    MISSING_REQUIRED_DATA_FILE,
    CHECKSUM_MISMATCH,
    INVALID_DATA,
    MISSING_MEDIA_DIR
}

data class BackupValidationResult(
    val isValid: Boolean,
    val code: BackupValidationCode,
    val details: String? = null
)

object BackupValidator {

    fun validateBackupDir(extractedDir: File, logger: BackupLogger): Boolean {
        return validateBackupDirDetailed(extractedDir, logger).isValid
    }

    fun validateBackupDirDetailed(extractedDir: File, logger: BackupLogger): BackupValidationResult {
        // 1. Validate manifest exists
        val manifestFile = File(extractedDir, "manifest.json")
        if (!manifestFile.exists()) {
            logger.error("Validation Failed: manifest.json is missing.")
            return BackupValidationResult(false, BackupValidationCode.MISSING_MANIFEST, "manifest.json is missing.")
        }

        val manifest = ManifestManager.readManifest(manifestFile)
        if (manifest == null) {
            logger.error("Validation Failed: manifest.json is invalid or corrupt.")
            return BackupValidationResult(false, BackupValidationCode.INVALID_MANIFEST, "manifest.json is invalid or corrupt.")
        }

        // 2. Older App Graceful Rejection (database version comparison)
        val currentDatabaseVersion = 2 // Current app database version code
        if (manifest.databaseVersion > currentDatabaseVersion) {
            logger.error("Validation Failed: Backup database version (v${manifest.databaseVersion}) is newer than current app database version (v$currentDatabaseVersion). Rejection enforced.")
            return BackupValidationResult(false, BackupValidationCode.UNSUPPORTED_VERSION, "Backup database version v${manifest.databaseVersion} is newer than current app database version v$currentDatabaseVersion.")
        }

        // 3. Locate data source (data.json OR legacy database/ directory)
        val dataFile = File(extractedDir, "data.json")
        val dbDir = File(extractedDir, "database")

        if (!dataFile.exists() && (!dbDir.exists() || !dbDir.isDirectory)) {
            logger.error("Validation Failed: Neither data.json nor database directory exists.")
            return BackupValidationResult(false, BackupValidationCode.MISSING_REQUIRED_DATA_FILE, "Backup data file (data.json) is missing.")
        }

        // 4. Read & Validate Data Schema + Records
        val gson = Gson()
        try {
            var users: List<UserEntity> = emptyList()
            var attendances: List<AttendanceEntity> = emptyList()

            if (dataFile.exists()) {
                dataFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val payload = gson.fromJson(reader, BackupDataPayload::class.java)
                        ?: return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "data.json is empty or malformed.")
                    users = payload.users
                    attendances = payload.attendance
                }
            } else {
                val userFile = File(dbDir, "user.json")
                val attendanceFile = File(dbDir, "attendance.json")
                if (!userFile.exists() || !attendanceFile.exists()) {
                    logger.error("Validation Failed: Required legacy database files missing.")
                    return BackupValidationResult(false, BackupValidationCode.MISSING_REQUIRED_DATA_FILE, "Legacy database files missing.")
                }
                users = userFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<UserEntity>>() {}.type
                    gson.fromJson(reader, type) ?: emptyList()
                }
                attendances = attendanceFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<AttendanceEntity>>() {}.type
                    gson.fromJson(reader, type) ?: emptyList()
                }
            }

            // Referential Integrity:
            // Since WorkDaily is an offline personal app, workerId in attendance is null for personal records.
            // If workerId is set (non-null), it must match a user's ID if user list is non-empty.
            if (users.isNotEmpty()) {
                val primaryUser = users.first()
                for (att in attendances) {
                    if (att.workerId != null && att.workerId != primaryUser.id) {
                        logger.error("Validation Failed: Foreign key integrity violation. Attendance record ${att.id} references invalid user ${att.workerId}.")
                        return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Foreign key integrity violation in record ${att.id}.")
                    }
                    // Validate basic attendance date format (YYYY-MM-DD)
                    if (att.date.isBlank() || !att.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        logger.error("Validation Failed: Invalid date format in record ${att.id}.")
                        return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Invalid date format in record ${att.id}.")
                    }
                }
            }

            // 5. Media Reference Validation
            val mediaDir = File(extractedDir, "media")
            val profileMediaCount = manifest.recordCounts["media"] ?: 0
            if (profileMediaCount > 0) {
                if (!mediaDir.exists() || !mediaDir.isDirectory) {
                    logger.error("Validation Failed: Media files listed in manifest but media directory does not exist.")
                    return BackupValidationResult(false, BackupValidationCode.MISSING_MEDIA_DIR, "Media directory missing.")
                }
            }

        } catch (e: Exception) {
            logger.error("Validation Failed: Syntax error or corrupt database JSON files.", e)
            return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Syntax error or corrupt database JSON files: ${e.message}")
        }

        logger.info("Backup directory integrity and schema validation succeeded.")
        return BackupValidationResult(true, BackupValidationCode.VALID)
    }
}
