package com.synfusion.attendance.backup

import com.google.gson.Gson
import java.io.File
import java.nio.charset.StandardCharsets

object ManifestManager {

    fun generateManifest(
        backupUuid: String,
        creationTimestamp: Long,
        totalBackupDurationMs: Long,
        compressionRatio: Double,
        appBuildNumber: Long = 1L,
        backupFormatVersion: Int = BackupVersionConstants.CURRENT_BACKUP_FORMAT_VERSION,
        backupVersion: Int = 1,
        appVersion: String = "1.0.3",
        databaseVersion: Int = BackupVersionConstants.CURRENT_DATABASE_VERSION,
        androidVersion: Int = try { android.os.Build.VERSION.SDK_INT } catch (e: Throwable) { 34 },
        deviceModel: String = try { android.os.Build.MODEL ?: "Android Device" } catch (e: Throwable) { "Android Device" },
        packageName: String = "com.synfusion.attendance",
        recordCounts: Map<String, Int>,
        totalFiles: Int,
        backupSize: Long,
        featureList: List<String>
    ): BackupManifest {
        return BackupManifest(
            backupUuid = backupUuid,
            creationTimestamp = creationTimestamp,
            totalBackupDurationMs = totalBackupDurationMs,
            compressionRatio = compressionRatio,
            appBuildNumber = appBuildNumber,
            backupFormatVersion = backupFormatVersion,
            backupVersion = backupVersion,
            appVersion = appVersion,
            databaseVersion = databaseVersion,
            androidVersion = androidVersion,
            deviceModel = deviceModel,
            packageName = packageName,
            backupDate = System.currentTimeMillis(),
            recordCounts = recordCounts,
            totalFiles = totalFiles,
            backupSize = backupSize,
            featureList = featureList
        )
    }

    fun writeManifest(manifestFile: File, manifest: BackupManifest) {
        val gson = Gson()
        manifestFile.writer(StandardCharsets.UTF_8).use { writer ->
            gson.toJson(manifest, writer)
        }
    }

    fun readManifest(manifestFile: File): BackupManifest? {
        if (!manifestFile.exists()) return null
        val gson = Gson()
        return try {
            manifestFile.reader(StandardCharsets.UTF_8).use { reader ->
                gson.fromJson(reader, BackupManifest::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getOrSynthesizeManifest(extractedDir: File, logger: BackupLogger? = null): BackupManifest? {
        val manifestFile = File(extractedDir, "manifest.json")
        val existing = readManifest(manifestFile)
        if (existing != null) {
            return existing
        }

        // Fallback for historical legacy backups lacking manifest.json
        val dataFile = File(extractedDir, "data.json")
        val dbDir = File(extractedDir, "database")

        if (!dataFile.exists() && (!dbDir.exists() || !dbDir.isDirectory)) {
            return null
        }

        logger?.info("manifest.json missing or unparseable. Synthesizing fallback manifest for legacy backup...")

        var totalFiles = 0
        extractedDir.walkTopDown().forEach { if (it.isFile) totalFiles++ }

        return generateManifest(
            backupUuid = "legacy-synthetic-${System.currentTimeMillis()}",
            creationTimestamp = System.currentTimeMillis(),
            totalBackupDurationMs = 0L,
            compressionRatio = 1.0,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 1,
            androidVersion = try { android.os.Build.VERSION.SDK_INT } catch (e: Throwable) { 34 },
            deviceModel = try { android.os.Build.MODEL ?: "Android Device" } catch (e: Throwable) { "Android Device" },
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(), // Dynamic lookup fallback will be used
            totalFiles = totalFiles,
            backupSize = extractedDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum(),
            featureList = listOf("Database", "Preferences")
        )
    }
}
