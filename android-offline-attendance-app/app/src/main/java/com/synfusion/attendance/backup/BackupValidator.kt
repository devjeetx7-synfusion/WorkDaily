package com.synfusion.attendance.backup

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synfusion.attendance.backup.dto.canonical.CanonicalBackupPayload
import com.synfusion.attendance.backup.dto.v1.*
import com.synfusion.attendance.backup.migration.MigrationRegistry
import com.synfusion.attendance.backup.migration.UnsupportedBackupVersionException
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

        // 2. Version validation using BackupVersionConstants
        val formatVer = manifest.backupFormatVersion
        val dbVer = manifest.databaseVersion

        if (formatVer > BackupVersionConstants.CURRENT_BACKUP_FORMAT_VERSION || dbVer > BackupVersionConstants.CURRENT_DATABASE_VERSION) {
            logger.error("Validation Failed: Backup format v$formatVer / DB v$dbVer is newer than app support (Format v${BackupVersionConstants.CURRENT_BACKUP_FORMAT_VERSION} / DB v${BackupVersionConstants.CURRENT_DATABASE_VERSION}). Rejection enforced.")
            return BackupValidationResult(false, BackupValidationCode.UNSUPPORTED_VERSION, "Backup format v$formatVer / DB v$dbVer is newer than supported app version.")
        }

        // 3. Locate data source (data.json OR legacy database/ directory)
        val dataFile = File(extractedDir, "data.json")
        val dbDir = File(extractedDir, "database")

        if (!dataFile.exists() && (!dbDir.exists() || !dbDir.isDirectory)) {
            logger.error("Validation Failed: Neither data.json nor database directory exists.")
            return BackupValidationResult(false, BackupValidationCode.MISSING_REQUIRED_DATA_FILE, "Backup data file (data.json) is missing.")
        }

        // 4. Read DTO & Migrate to Canonical Payload
        val gson = Gson()
        try {
            val canonicalPayload: CanonicalBackupPayload

            if (dataFile.exists()) {
                val rawPayload = dataFile.reader(StandardCharsets.UTF_8).use { reader ->
                    gson.fromJson(reader, BackupV1Payload::class.java)
                } ?: return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "data.json is empty or malformed.")

                canonicalPayload = MigrationRegistry.migrateToCanonical(rawPayload, formatVer, manifest, logger)
            } else {
                val userFile = File(dbDir, "user.json")
                val attendanceFile = File(dbDir, "attendance.json")
                val summaryFile = File(dbDir, "summary.json")

                if (!userFile.exists() || !attendanceFile.exists()) {
                    logger.error("Validation Failed: Required legacy database files missing.")
                    return BackupValidationResult(false, BackupValidationCode.MISSING_REQUIRED_DATA_FILE, "Legacy database files missing.")
                }

                val users = userFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<UserV1Dto>>() {}.type
                    gson.fromJson<List<UserV1Dto>>(reader, type) ?: emptyList()
                }

                val attendances = attendanceFile.reader(StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<AttendanceV1Dto>>() {}.type
                    gson.fromJson<List<AttendanceV1Dto>>(reader, type) ?: emptyList()
                }

                val summaries = if (summaryFile.exists()) {
                    summaryFile.reader(StandardCharsets.UTF_8).use { reader ->
                        val type = object : TypeToken<List<SummaryV1Dto>>() {}.type
                        gson.fromJson<List<SummaryV1Dto>>(reader, type) ?: emptyList()
                    }
                } else emptyList()

                val rawPayload = BackupV1Payload(users = users, attendance = attendances, summaries = summaries)
                canonicalPayload = MigrationRegistry.migrateToCanonical(rawPayload, formatVer, manifest, logger)
            }

            // 5. Referential & Semantic Integrity Check on Canonical Payload
            if (canonicalPayload.users.isNotEmpty()) {
                val primaryUser = canonicalPayload.users.first()
                for (att in canonicalPayload.attendance) {
                    if (att.workerId != null && att.workerId != primaryUser.id) {
                        logger.error("Validation Failed: Foreign key integrity violation. Attendance record ${att.id} references invalid user ${att.workerId}.")
                        return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Foreign key integrity violation in record ${att.id}.")
                    }
                    if (att.date.isBlank() || !att.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        logger.error("Validation Failed: Invalid date format in record ${att.id}.")
                        return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Invalid date format in record ${att.id}.")
                    }
                }
            }

            // 6. Media Reference Validation
            val mediaDir = File(extractedDir, "media")
            val profileMediaCount = manifest.recordCounts["media"] ?: 0
            if (profileMediaCount > 0) {
                if (!mediaDir.exists() || !mediaDir.isDirectory) {
                    logger.error("Validation Failed: Media files listed in manifest but media directory does not exist.")
                    return BackupValidationResult(false, BackupValidationCode.MISSING_MEDIA_DIR, "Media directory missing.")
                }
            }

        } catch (e: UnsupportedBackupVersionException) {
            logger.error("Validation Failed: Unsupported backup version - ${e.message}")
            return BackupValidationResult(false, BackupValidationCode.UNSUPPORTED_VERSION, e.message)
        } catch (e: Exception) {
            logger.error("Validation Failed: Syntax error or corrupt database JSON files.", e)
            return BackupValidationResult(false, BackupValidationCode.INVALID_DATA, "Syntax error or corrupt database JSON files: ${e.message}")
        }

        logger.info("Backup directory integrity and canonical schema validation succeeded.")
        return BackupValidationResult(true, BackupValidationCode.VALID)
    }
}
