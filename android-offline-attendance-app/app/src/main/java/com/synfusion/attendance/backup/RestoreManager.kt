package com.synfusion.attendance.backup

import android.content.Context
import android.content.Intent
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.AttendanceDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

object RestoreResultStore {
    private const val PREFS_NAME = "restore_result_prefs"
    private const val KEY_REPORT_JSON = "restore_report_json"

    fun saveReport(context: Context, report: RestoreSummaryReport, backupDate: Long, backupSize: Long, databaseVersion: Int, isMerge: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val map = mapOf(
            "attendanceCount" to report.attendanceCount,
            "totalAttendanceRecords" to report.totalAttendanceRecords,
            "profileRestored" to report.profileRestored,
            "mediaCount" to report.mediaCount,
            "totalMediaFiles" to report.totalMediaFiles,
            "settingsRestored" to report.settingsRestored,
            "backupDate" to backupDate,
            "backupSize" to backupSize,
            "databaseVersion" to databaseVersion,
            "isMerge" to isMerge,
            "restoreDate" to System.currentTimeMillis()
        )
        val json = Gson().toJson(map)
        prefs.edit().putString(KEY_REPORT_JSON, json).commit()
    }

    fun getAndClearReport(context: Context): Map<String, Any>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REPORT_JSON, null) ?: return null
        prefs.edit().remove(KEY_REPORT_JSON).commit()
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
}

class RestoreManager(private val context: Context) {

    private val tempExtractDir = File(context.cacheDir, "restore_working_dir")
    private val logFile = File(tempExtractDir, "logs/restore.log")
    private val logger = BackupLogger(logFile)

    suspend fun performRestore(zipFile: File, merge: Boolean, progressTracker: ProgressTracker): RestoreSummaryReport? {
        return withContext(Dispatchers.IO) {
            try {
                progressTracker.updateState(RestoreState.READING_BACKUP)
                progressTracker.updateProgress(10f, "Extracting backup zip...")
                tempExtractDir.deleteRecursively()
                tempExtractDir.mkdirs()

                RestoreProvider.extractZip(zipFile, tempExtractDir, logger)

                // Read manifest
                val manifestFile = File(tempExtractDir, "manifest.json")
                val manifest = ManifestManager.readManifest(manifestFile)
                if (manifest == null) {
                    logger.error("Restore failed: Manifest not found.")
                    progressTracker.updateState(RestoreState.RESTORE_FAILED)
                    return@withContext null
                }

                progressTracker.updateState(RestoreState.VALIDATING)
                progressTracker.updateProgress(30f, "Verifying backup integrity...")
                val isValid = BackupValidator.validateBackupDir(tempExtractDir, logger)
                if (!isValid) {
                    logger.error("Integrity check failed. Aborting restore.")
                    progressTracker.updateState(RestoreState.RESTORE_FAILED)
                    return@withContext null
                }

                // Check free space on device to handle low storage scenarios
                val freeSpace = context.filesDir.usableSpace
                val backupSize = manifest.backupSize
                if (freeSpace < backupSize * 2) {
                    logger.error("Restore failed: Insufficient device storage space (Required: ${backupSize * 2} bytes, Available: $freeSpace bytes).")
                    progressTracker.updateState(RestoreState.RESTORE_FAILED)
                    throw Exception("Insufficient storage space to perform restore.")
                }

                progressTracker.updateState(RestoreState.IMPORTING)
                progressTracker.updateProgress(50f, "Importing database atomically...")
                val tempDb = TempDatabaseManager.createTempDatabase(context)

                try {
                    val dbCounts = JsonImporter.importDatabase(tempDb, tempExtractDir, merge = false, logger)
                    logger.info("Imported records into temporary database: $dbCounts")

                    progressTracker.updateState(RestoreState.VERIFYING)
                    progressTracker.updateProgress(55f, "Verifying record-level consistency...")

                    // 100% record-level validation against manifest counts
                    val expectedUsers = manifest.recordCounts["users"] ?: 0
                    val expectedAttendance = manifest.recordCounts["attendance"] ?: 0
                    val expectedSummaries = manifest.recordCounts["summaries"] ?: 0

                    val tempUserCount = if (tempDb.userDao().getUser() != null) 1 else 0
                    val tempAttendanceCount = tempDb.attendanceDao().getPersonalAttendance().size
                    val tempSummariesCount = tempDb.summaryDao().getAllSummaries().size

                    if (tempUserCount != expectedUsers || tempAttendanceCount != expectedAttendance || tempSummariesCount != expectedSummaries) {
                        logger.error("Validation Failed: Record count mismatch. Expected: {users=$expectedUsers, attendance=$expectedAttendance, summaries=$expectedSummaries}, Actual: {users=$tempUserCount, attendance=$tempAttendanceCount, summaries=$tempSummariesCount}")
                        tempDb.close()
                        progressTracker.updateState(RestoreState.RESTORE_FAILED)
                        return@withContext null
                    }

                    if (merge) {
                        progressTracker.updateProgress(58f, "Merging records deterministically...")
                        // Load existing production database records safely
                        val prodDb = AttendanceDatabase.getInstance(context)
                        val prodUser = prodDb.userDao().getUser()
                        val prodAttendance = prodDb.attendanceDao().getPersonalAttendance()
                        val prodSummaries = prodDb.summaryDao().getAllSummaries()

                        // Merge user details: prefer backup user details if they exist, else keep production user details
                        val tempUser = tempDb.userDao().getUser()
                        if (tempUser == null && prodUser != null) {
                            tempDb.userDao().insertUser(prodUser)
                        }

                        // Merge attendance: insert unique production attendance records that are not in tempDb
                        val tempAttendanceIds = tempDb.attendanceDao().getPersonalAttendance().map { it.id }.toSet()
                        val uniqueProdAttendance = prodAttendance.filter { it.id !in tempAttendanceIds }
                        for (att in uniqueProdAttendance) {
                            tempDb.attendanceDao().insertAttendance(att)
                        }

                        // Merge summaries: insert unique production summary records that are not in tempDb
                        val tempSummaryIds = tempDb.summaryDao().getAllSummaries().map { it.id }.toSet()
                        val uniqueProdSummaries = prodSummaries.filter { it.id !in tempSummaryIds }
                        tempDb.summaryDao().insertSummaries(uniqueProdSummaries)

                        logger.info("Deterministic merge complete. Combined with ${uniqueProdAttendance.size} production attendance and ${uniqueProdSummaries.size} production summary records.")
                    }

                    progressTracker.updateState(RestoreState.PREPARING_REPLACEMENT)
                    progressTracker.updateProgress(65f, "Preparing database replacement...")

                    // 1. Stop active collectors & cancel restore-sensitive jobs
                    DatabaseLifecycleCoordinator.stopAndCancelEverything()

                    // 2. Lock and close production database completely to prevent any queries from racing
                    AttendanceDatabase.beginSwap()

                    // 3. Close the temporary database safely before swapping
                    tempDb.close()

                    // 4. Swap data to production database securely
                    progressTracker.updateState(RestoreState.REPLACING_DATABASE)
                    progressTracker.updateProgress(75f, "Applying atomic database swap...")
                    TempDatabaseManager.swapToProduction(context, tempDb)

                    progressTracker.updateState(RestoreState.RESTORING_PREFERENCES)
                    progressTracker.updateProgress(85f, "Restoring preferences and media files...")

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

                    // 5. Unlock database swap state
                    AttendanceDatabase.endSwap()

                    // 6. Ensure final record counts in the restored prod database
                    val finalProdDb = AttendanceDatabase.getInstance(context)

                    // 7. Refresh ViewModels so screens reload seamlessly without forcing a restart
                    DatabaseLifecycleCoordinator.refreshViewModels()

                    val finalUser = finalProdDb.userDao().getUser()
                    val finalAttendance = finalProdDb.attendanceDao().getPersonalAttendance()

                    progressTracker.updateState(RestoreState.RESTORE_SUCCESS)
                    progressTracker.updateProgress(95f, "Restore completed successfully!")

                    val report = RestoreSummaryReport(
                        attendanceCount = finalAttendance.size,
                        totalAttendanceRecords = finalAttendance.size,
                        profileRestored = finalUser != null,
                        mediaCount = mediaRestoredCount,
                        totalMediaFiles = mediaExpectedCount,
                        settingsRestored = dsPrefsRestoredCount > 0
                    )

                    // Persist report to SharedPreferences synchronously for safe load upon startup
                    RestoreResultStore.saveReport(
                        context = context,
                        report = report,
                        backupDate = manifest.backupDate,
                        backupSize = manifest.backupSize,
                        databaseVersion = manifest.databaseVersion,
                        isMerge = merge
                    )

                    progressTracker.updateState(RestoreState.RESTARTING_APP)
                    progressTracker.updateProgress(100f, "Restarting application...")

                    return@withContext report

                } catch (e: Exception) {
                    logger.error("Restore failed during DB operations: ${e.message}", e)
                    tempDb.close()
                    AttendanceDatabase.endSwap()
                    progressTracker.updateState(RestoreState.RESTORE_FAILED)
                    return@withContext null
                }

            } catch (e: Exception) {
                logger.error("Critical error during restore: ${e.message}", e)
                AttendanceDatabase.endSwap()
                progressTracker.updateState(RestoreState.RESTORE_FAILED)
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

    fun triggerCleanRestart() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            val restartIntent = Intent.makeRestartActivityTask(intent.component)
            context.startActivity(restartIntent)
        }
        // Safely exit current process to guarantee complete Room singleton and ViewModel recreation
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }
}
