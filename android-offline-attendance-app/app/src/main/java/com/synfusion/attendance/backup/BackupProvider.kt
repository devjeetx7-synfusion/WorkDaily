package com.synfusion.attendance.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupProvider {

    fun compressFolder(sourceDir: File, zipFile: File, logger: BackupLogger) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zipDir(sourceDir, sourceDir, zos, logger)
        }
    }

    private fun zipDir(root: File, current: File, zos: ZipOutputStream, logger: BackupLogger) {
        val files = current.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipDir(root, file, zos, logger)
            } else {
                val relativePath = file.absolutePath.substring(root.absolutePath.length + 1)
                val entry = ZipEntry(relativePath)
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (fis.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                    }
                }
                zos.closeEntry()
            }
        }
    }
}

object RestoreProvider {

    fun extractZip(zipFile: File, destDir: File, logger: BackupLogger) {
        destDir.deleteRecursively()
        destDir.mkdirs()

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var count: Int
                        while (zis.read(buffer).also { count = it } != -1) {
                            fos.write(buffer, 0, count)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
