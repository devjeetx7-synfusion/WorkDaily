package com.synfusion.attendance.backup.dto.canonical

data class CanonicalMedia(
    val relativePath: String,
    val fileName: String,
    val category: String, // "profile", "profile_photos", "attachments", etc.
    val fileSize: Long,
    val checksumSha256: String
)
