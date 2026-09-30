package com.synfusion.attendance.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.synfusion.attendance.data.OfflinePreferencesRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val stateRepo = BackupStateRepository(applicationContext)
            val isAutoBackupEnabled = stateRepo.isAutoBackupEnabledFlow.first()
            if (!isAutoBackupEnabled) {
                return Result.success()
            }

            val isDirty = stateRepo.isBackupDirtyFlow.first()
            if (!isDirty) {
                return Result.success()
            }

            val backupManager = BackupManager(applicationContext)
            val progressTracker = ProgressTracker()

            val backupsDir = File(applicationContext.filesDir, "backups").apply { mkdirs() }
            val tempBackupFile = File(applicationContext.cacheDir, "AutoBackup_temp.zip")

            backupManager.performBackup(tempBackupFile, progressTracker)

            if (!tempBackupFile.exists() || tempBackupFile.length() <= 0) {
                stateRepo.markBackupFailed("Generated backup file empty")
                return Result.failure()
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "WorkDaily_AutoBackup_$timestamp.zip"
            val destFile = File(backupsDir, fileName)

            tempBackupFile.copyTo(destFile, overwrite = true)
            tempBackupFile.delete()

            val prefsRepo = OfflinePreferencesRepository(applicationContext)
            prefsRepo.saveBackupMetadata(timestamp, destFile.length(), "Local Storage")

            // Check for SAF External Backup URI
            val externalUriStr = stateRepo.externalBackupUriFlow.first()
            var locationName = "Local Storage"

            if (!externalUriStr.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(externalUriStr)
                    val contentResolver = applicationContext.contentResolver
                    contentResolver.openOutputStream(uri, "wa")?.use { outputStream ->
                        destFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    locationName = "External Storage"
                } catch (e: Exception) {
                    Log.w("WorkDailyAutoBackup", "Could not write to external SAF URI: ${e.message}")
                }
            }

            stateRepo.markBackupSuccessful(timestamp, destFile.length(), locationName)

            // Retention Rotation: keep latest 7 daily backups
            rotateOldBackups(backupsDir, maxKeep = 7)

            Result.success()
        } catch (e: Exception) {
            Log.e("WorkDailyAutoBackup", "Auto backup failed: ${e.message}", e)
            try {
                BackupStateRepository(applicationContext).markBackupFailed(e.message ?: "Unknown error")
            } catch (_: Exception) {}
            Result.retry()
        }
    }

    private fun rotateOldBackups(dir: File, maxKeep: Int) {
        val files = dir.listFiles { _, name -> name.startsWith("WorkDaily_") && name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (files.size > maxKeep) {
            files.drop(maxKeep).forEach { oldFile ->
                oldFile.delete()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "workdaily_auto_backup_job"

        fun enqueueAutoBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
