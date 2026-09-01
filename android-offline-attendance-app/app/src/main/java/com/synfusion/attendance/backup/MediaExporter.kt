package com.synfusion.attendance.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object MediaExporter {

    fun exportMedia(srcDir: File, destDir: File, logger: BackupLogger): Int {
        destDir.mkdirs()
        var copiedFilesCount = 0

        val mediaDirs = listOf("profile", "profile_photos", "attachments", "reports")
        mediaDirs.forEach { dirName ->
            val targetFolder = File(destDir, dirName)
            // Look for any media files on target path.
            // E.g. search in the srcDir (App context filesDir)
            val appSpecificMediaDir = File(srcDir, dirName)
            if (appSpecificMediaDir.exists() && appSpecificMediaDir.isDirectory) {
                targetFolder.mkdirs()
                appSpecificMediaDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val outFile = File(targetFolder, file.name)
                        copyFile(file, outFile)
                        copiedFilesCount++
                    }
                }
            }
        }
        logger.info("Exported $copiedFilesCount media files.")
        return copiedFilesCount
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
