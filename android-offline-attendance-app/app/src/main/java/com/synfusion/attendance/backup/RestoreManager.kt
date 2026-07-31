package com.synfusion.attendance.backup

import android.content.Context
import com.synfusion.attendance.data.entities.UserEntity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

class RestoreManager(private val context: Context) {

    private val tempExtractDir = File(context.cacheDir, "restore_working_dir")
    private val logFile = File(tempExtractDir, "logs/restore.log")
    private val logger = BackupLogger(logFile)

    suspend fun performRestore(zipFile: File, merge: Boolean, progressTracker: ProgressTracker): RestoreSummaryReport? {
        return withContext(Dispatchers.IO) {
            try {
                progressTracker.updateProgress(10f, "Extracting backup zip...")
                tempExtractDir.deleteRecursively()
                tempExtractDir.mkdirs()

                RestoreProvider.extractZip(zipFile, tempExtractDir, logger)

                // Read manifest
                val manifestFile = File(tempExtractDir, "manifest.json")
                val manifest = ManifestManager.readManifest(manifestFile)
                if (manifest == null) {
                    logger.error("Restore failed: Manifest not found.")
                    return@withContext null
                }

                progressTracker.updateProgress(30f, "Verifying backup integrity...")
                val isValid = BackupValidator.validateBackupDir(tempExtractDir, logger)
                if (!isValid) {
                    logger.error("Integrity check failed. Aborting restore.")
                    return@withContext null
                }

                // Check free space on device to handle low storage scenarios
                val freeSpace = context.filesDir.usableSpace
                val backupSize = manifest.backupSize
                if (freeSpace < backupSize * 2) {
                    logger.error("Restore failed: Insufficient device storage space (Required: ${backupSize * 2} bytes, Available: $freeSpace bytes).")
                    throw Exception("Insufficient storage space to perform restore.")
                }

                progressTracker.updateProgress(50f, "Importing database atomically...")
                val tempDb = TempDatabaseManager.createTempDatabase(context)

                try {
                    val dbCounts = JsonImporter.importDatabase(tempDb, tempExtractDir, merge, logger)
                    logger.info("Imported records into temporary database: $dbCounts")

                    // Perform integrity validations on temp database
                    val user = tempDb.userDao().getUser()
                    if (user == null && !merge) {
                        logger.error("Validation Failed: Imported database has no user details.")
                        tempDb.close()
                        return@withContext null
                    }

                    progressTracker.updateProgress(70f, "Applying atomic database swap...")
                    TempDatabaseManager.swapToProduction(context, tempDb)

                    progressTracker.updateProgress(80f, "Applying remaining registry modules dynamically...")

                    var dsPrefsRestoredCount = 0
                    var mediaRestoredCount = 0
                    var mediaExpectedCount = 0

                    // Execute other registry modules dynamically (preferences, media, notifications, calendar, leave)
                    val modules = BackupRegistry.getModules()
                    modules.forEach { module ->
                        if (module.name != "Database") {
                            logger.info("Restoring module dynamically: ${module.name}")
                            val resultCounts = module.importModule(context, tempExtractDir, merge, logger)
                            if (module.name == "Preferences") {
                                dsPrefsRestoredCount = resultCounts["datastore"] ?: 0
                            } else if (module.name == "Media") {
                                mediaRestoredCount = resultCounts["media"] ?: 0
                                mediaExpectedCount = manifest.recordCounts["media"] ?: mediaRestoredCount
                            }
                        }
                    }

                    val expectedAttendance = manifest.recordCounts["attendance"] ?: 0
                    val restoredAttendance = dbCounts["attendance"] ?: 0

                    progressTracker.updateProgress(100f, "Restore completed successfully!")

                    return@withContext RestoreSummaryReport(
                        attendanceCount = restoredAttendance,
                        totalAttendanceRecords = expectedAttendance,
                        profileRestored = user != null || merge,
                        mediaCount = mediaRestoredCount,
                        totalMediaFiles = mediaExpectedCount,
                        settingsRestored = dsPrefsRestoredCount > 0
                    )

                } catch (e: Exception) {
                    logger.error("Restore failed during DB operations: ${e.message}", e)
                    tempDb.close()
                    return@withContext null
                }

            } catch (e: Exception) {
                logger.error("Critical error during restore: ${e.message}", e)
                return@withContext null
            }
        }
    }

    suspend fun getBackupPreview(zipFile: File): BackupManifest? {
        return withContext(Dispatchers.IO) {
            try {
                val previewDir = File(context.cacheDir, "preview_extract_dir")
                previewDir.deleteRecursively()
                previewDir.mkdirs()

                // Just extract manifest.json to preview fast
                RestoreProvider.extractZip(zipFile, previewDir, logger)
                val manifestFile = File(previewDir, "manifest.json")
                val manifest = ManifestManager.readManifest(manifestFile)
                previewDir.deleteRecursively()
                manifest
            } catch (e: Exception) {
                null
            }
        }
    }
}
