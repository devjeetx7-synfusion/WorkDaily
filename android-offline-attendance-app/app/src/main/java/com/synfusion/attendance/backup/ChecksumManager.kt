package com.synfusion.attendance.backup

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ChecksumManager {

    fun computeSha256(file: File): String {
        if (!file.exists() || file.isDirectory) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        if (expectedChecksum.isBlank()) return false
        val actual = computeSha256(file)
        return actual.equals(expectedChecksum, ignoreCase = true)
    }
}
