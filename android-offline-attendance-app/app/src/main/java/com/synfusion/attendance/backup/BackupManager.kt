package com.synfusion.attendance.backup

import android.content.Context
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.UUID

class BackupManager(private val context: Context) {

    private val tempDir = File(context.cacheDir, "backup_working_dir")
    private val verifyDir = File(context.cacheDir, "backup_verify_dir")
    private val logFile = File(context.cacheDir, "backup_op.log")
    private val logger = BackupLogger(logFile)

    suspend fun performBackup(outputZipFile: File, progressTracker: ProgressTracker) {
        val startTime = System.currentTimeMillis()
        val backupUuid = UUID.randomUUID().toString()

        withContext(Dispatchers.IO) {
            try {
                progressTracker.updateProgress(10f, "Starting Backup...")
                tempDir.deleteRecursively()
                tempDir.mkdirs()

                logger.info("Backup UUID: $backupUuid started.")
                progressTracker.updateProgress(30f, "Exporting WorkDaily data...")

                val combinedCounts = mutableMapOf<String, Int>()

                // Dynamically invoke modules registered in BackupRegistry
                val modules = BackupRegistry.getModules()
                modules.forEachIndexed { index, module ->
                    logger.info("Exporting module: ${module.name}")
                    val counts = module.exportModule(context, tempDir, logger)
                    combinedCounts.putAll(counts)
                    val currentProgress = 30f + ((index + 1).toFloat() / modules.size.toFloat() * 40f)
                    progressTracker.updateProgress(currentProgress, "Exported ${module.name}...")
                }

                progressTracker.updateProgress(75f, "Creating Manifest...")
                val totalFiles = countFilesRecursive(tempDir) + 1
                val uncompressedSize = calculateDirSize(tempDir)
                val duration = System.currentTimeMillis() - startTime

                // Generate Manifest with modular record counts
                val manifestFile = File(tempDir, "manifest.json")
                val manifestObj = ManifestManager.generateManifest(
                    backupUuid = backupUuid,
                    creationTimestamp = startTime,
                    totalBackupDurationMs = duration,
                    compressionRatio = 1.0,
                    appBuildNumber = 1L,
                    backupFormatVersion = 1,
                    backupVersion = 1,
                    appVersion = "1.0.0",
                    databaseVersion = 2,
                    androidVersion = android.os.Build.VERSION.SDK_INT,
                    deviceModel = android.os.Build.MODEL,
                    packageName = context.packageName,
                    recordCounts = combinedCounts,
                    totalFiles = totalFiles,
                    backupSize = uncompressedSize,
                    featureList = modules.map { it.name }
                )
                ManifestManager.writeManifest(manifestFile, manifestObj)

                progressTracker.updateProgress(85f, "Compressing backup archive...")
                if (outputZipFile.exists()) outputZipFile.delete()
                BackupProvider.compressFolder(tempDir, outputZipFile, logger)

                // Perform immediate Self-Validation Round-Trip Check
                progressTracker.updateProgress(95f, "Verifying created backup integrity...")
                verifyDir.deleteRecursively()
                verifyDir.mkdirs()
                try {
                    RestoreProvider.extractZip(outputZipFile, verifyDir, logger)
                    val validationRes = BackupValidator.validateBackupDirDetailed(verifyDir, logger)
                    if (!validationRes.isValid) {
                        outputZipFile.delete()
                        val err = "Backup created failed self-validation check: ${validationRes.code} (${validationRes.details})"
                        logger.error(err)
                        throw IllegalStateException(err)
                    }
                    logger.info("Backup round-trip self-validation succeeded.")
                } finally {
                    verifyDir.deleteRecursively()
                }

                progressTracker.updateProgress(100f, "Backup completed successfully!")
                logger.info("Backup generated cleanly: ${outputZipFile.name} (size: ${outputZipFile.length()} bytes, duration: $duration ms).")

            } catch (e: Exception) {
                logger.error("Backup operation aborted due to failure: ${e.message}", e)
                if (outputZipFile.exists()) outputZipFile.delete()
                throw e
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    private fun countFilesRecursive(dir: File): Int {
        var count = 0
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += countFilesRecursive(file)
            } else {
                count++
            }
        }
        return count
    }

    private fun calculateDirSize(dir: File): Long {
        var total: Long = 0
        dir.listFiles()?.forEach { file ->
            total += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }
        return total
    }
}
