package com.synfusion.attendance.backup.dto.canonical

import com.google.gson.annotations.SerializedName
import com.synfusion.attendance.backup.BackupManifest

data class CanonicalBackupPayload(
    @SerializedName("manifest") val manifest: BackupManifest?,
    @SerializedName("users") val users: List<CanonicalUser> = emptyList(),
    @SerializedName("attendance") val attendance: List<CanonicalAttendance> = emptyList(),
    @SerializedName("summaries") val summaries: List<CanonicalSummary> = emptyList(),
    @SerializedName("preferences") val preferences: Map<String, Any> = emptyMap(),
    @SerializedName("mediaFiles") val mediaFiles: List<CanonicalMedia> = emptyList()
)
