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
        appBuildNumber: Long,
        backupFormatVersion: Int,
        backupVersion: Int,
        appVersion: String,
        databaseVersion: Int,
        androidVersion: Int,
        deviceModel: String,
        packageName: String,
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
}
