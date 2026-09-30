package com.synfusion.attendance.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object MediaImporter {

    fun importMedia(backupMediaDir: File, appFilesDir: File, logger: BackupLogger): Int {
        var restoredCount = 0
        if (!backupMediaDir.exists() || !backupMediaDir.isDirectory) {
            return 0
        }

        // 1. Copy files directly inside backupMediaDir (e.g. root media/ avatar/photos)
        val profilePhotosDest = File(appFilesDir, "profile_photos").apply { mkdirs() }
        backupMediaDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val destFile = File(profilePhotosDest, file.name)
                copyFile(file, destFile)
                restoredCount++
            }
        }

        // 2. Copy files inside subdirectories
        val subDirs = listOf("profile", "profile_photos", "attachments", "reports")
        subDirs.forEach { dirName ->
            val srcSubDir = File(backupMediaDir, dirName)
            if (srcSubDir.exists() && srcSubDir.isDirectory) {
                val destSubDir = File(appFilesDir, if (dirName == "profile") "profile_photos" else dirName)
                destSubDir.mkdirs()

                srcSubDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val destFile = File(destSubDir, file.name)
                        copyFile(file, destFile)
                        restoredCount++
                    }
                }
            }
        }
        logger.info("Restored $restoredCount media files successfully.")
        return restoredCount
    }

    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { inStream ->
            FileOutputStream(destination).use { outStream ->
                val buffer = ByteArray(8192)
                var length: Int
                while (inStream.read(buffer).also { length = it } > 0) {
                    outStream.write(buffer, 0, length)
                }
            }
        }
    }
}
