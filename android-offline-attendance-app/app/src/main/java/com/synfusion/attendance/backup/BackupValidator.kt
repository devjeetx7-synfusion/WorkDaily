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
    MISSING_DATABASE_DIR,
    MISSING_REQUIRED_DB_FILES,
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

        // 3. Validate JSON files in database sub-directory
        val dbDir = File(extractedDir, "database")
        if (!dbDir.exists() || !dbDir.isDirectory) {
            logger.error("Validation Failed: database directory is missing.")
            return BackupValidationResult(false, BackupValidationCode.MISSING_DATABASE_DIR, "database directory is missing.")
        }

        val requiredJsonFiles = listOf("user.json", "attendance.json")
        for (fileName in requiredJsonFiles) {
            val file = File(dbDir, fileName)
            if (!file.exists()) {
                logger.error("Validation Failed: database/$fileName is missing.")
                return BackupValidationResult(false, BackupValidationCode.MISSING_REQUIRED_DB_FILES, "database/$fileName is missing.")
            }
        }

        // 4. Validate SHA-256 Checksums
        val checksumsFile = File(extractedDir, "metadata/checksums.json")
        if (checksumsFile.exists()) {
            val gson = Gson()
            try {
                val checksumMap: Map<String, String> = checksumsFile.reader(StandardCharsets.UTF_8).use { reader ->
                    @Suppress("UNCHECKED_CAST")
                    gson.fromJson(reader, Map::class.java) as? Map<String, String> ?: emptyMap()
                }
                for ((relPath, expectedHash) in checksumMap) {
                    val actualFile = File(extractedDir, relPath)
                    if (!ChecksumManager.verifyChecksum(actualFile, expectedHash)) {
                        logger.error("Validation Failed: File $relPath is corrupt (Checksum mismatch).")
                        return BackupValidationResult(false, BackupValidationCode.CHECKSUM_MISMATCH, "File $relPath checksum mismatch.")
                    }
                }
            } catch (e: Exception) {
                logger.error("Validation Failed: Failed to verify SHA-256 checksums", e)
                return BackupValidationResult(false, BackupValidationCode.CHECKSUM_MISMATCH, "Failed to verify SHA-256 checksums: ${e.message}")
            }
        }

        // 5. Referential Integrity & Data Validation
        val gson = Gson()
        try {
            val userFile = File(dbDir, "user.json")
            val users: List<UserEntity> = userFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<UserEntity>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }

            val attendanceFile = File(dbDir, "attendance.json")
            val attendances: List<AttendanceEntity> = attendanceFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<AttendanceEntity>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }

            // Referential Integrity: attendance must not reference non-existing users
            // Since this is single-user personal app, workerId must be null or match the logged in user ID if set
            if (users.isNotEmpty()) {
                val primaryUser = users.first()
                for (att in attendances) {
                    if (att.workerId != null && att.workerId != primaryUser.id) {
                        logger.error("Validation Failed: Foreign key integrity violation. Attendance record ${att.id} references invalid user ${att.workerId}.")
                        return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Foreign key integrity violation in record ${att.id}.")
                    }
                    // Validate basic attendance attributes
                    if (att.date.isBlank() || !att.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        logger.error("Validation Failed: Invalid date format in record ${att.id}.")
                        return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Invalid date format in record ${att.id}.")
                    }
                }
            }

            // 6. Media Reference Validation
            // If any file has profile or signature reference, we verify that the corresponding media file is present in media folder
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
