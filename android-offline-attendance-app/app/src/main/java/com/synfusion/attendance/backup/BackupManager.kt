package com.synfusion.attendance.backup

import android.content.Context
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.UUID
import com.google.gson.Gson

class BackupManager(private val context: Context) {

    private val tempDir = File(context.cacheDir, "backup_working_dir")
    private val logFile = File(tempDir, "logs/backup.log")
    private val logger = BackupLogger(logFile)

    suspend fun performBackup(outputZipFile: File, progressTracker: ProgressTracker) {
        val startTime = System.currentTimeMillis()
        val backupUuid = UUID.randomUUID().toString()

        withContext(Dispatchers.IO) {
            try {
                progressTracker.updateProgress(10f, "Starting Backup...")
                tempDir.deleteRecursively()
                tempDir.mkdirs()

                // Standard metadata & logs structures
                val metaDir = File(tempDir, "metadata")
                val logsDir = File(tempDir, "logs")
                metaDir.mkdirs()
                logsDir.mkdirs()

                logger.info("Backup UUID: $backupUuid started dynamically.")
                progressTracker.updateProgress(30f, "Invoking BackupRegistry modules...")

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

                progressTracker.updateProgress(75f, "Creating Metadata & Checksums...")
                val totalFiles = countFilesRecursive(tempDir) + 1

                // Compute checksums for integrity verification
                val checksumMap = mutableMapOf<String, String>()
                collectChecksumsRecursive(tempDir, tempDir, checksumMap)
                val checksumFile = File(metaDir, "checksums.json")
                checksumFile.writer().use { Gson().toJson(checksumMap, it) }

                val duration = System.currentTimeMillis() - startTime

                val uncompressedSize = calculateDirSize(tempDir)

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
                    backupSize = uncompressedSize, // Use actual uncompressed size for accurate validation during restore
                    featureList = modules.map { it.name }
                )
                ManifestManager.writeManifest(manifestFile, manifestObj)

                progressTracker.updateProgress(90f, "Compressing backup archive...")
                if (outputZipFile.exists()) outputZipFile.delete()
                BackupProvider.compressFolder(tempDir, outputZipFile, logger)

                progressTracker.updateProgress(100f, "Backup completed successfully!")
                logger.info("Backup generated: ${outputZipFile.name} duration: $duration ms.")

            } catch (e: Exception) {
                logger.error("Backup operation aborted due to failure: ${e.message}", e)
                throw e
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

    private fun collectChecksumsRecursive(root: File, current: File, map: MutableMap<String, String>) {
        current.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                collectChecksumsRecursive(root, file, map)
            } else {
                val relPath = file.absolutePath.substring(root.absolutePath.length + 1)
                if (!relPath.endsWith("backup.log") && !relPath.endsWith("manifest.json") && !relPath.endsWith("checksums.json")) {
                    map[relPath] = ChecksumManager.computeSha256(file)
                }
            }
        }
    }
}
