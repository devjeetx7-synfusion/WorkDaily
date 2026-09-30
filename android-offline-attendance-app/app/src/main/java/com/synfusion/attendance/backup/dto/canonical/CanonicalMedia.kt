package com.synfusion.attendance.backup.dto.canonical

import com.google.gson.annotations.SerializedName

data class CanonicalMedia(
    @SerializedName("relativePath") val relativePath: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("category") val category: String, // "profile", "profile_photos", "attachments", etc.
    @SerializedName("fileSize") val fileSize: Long,
    @SerializedName("checksumSha256") val checksumSha256: String
)
