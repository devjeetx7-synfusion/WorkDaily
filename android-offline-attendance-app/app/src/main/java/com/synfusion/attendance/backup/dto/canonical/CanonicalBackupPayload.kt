package com.synfusion.attendance.backup.dto.canonical

import com.synfusion.attendance.backup.BackupManifest

data class CanonicalBackupPayload(
    val manifest: BackupManifest?,
    val users: List<CanonicalUser> = emptyList(),
    val attendance: List<CanonicalAttendance> = emptyList(),
    val summaries: List<CanonicalSummary> = emptyList(),
    val preferences: Map<String, Any> = emptyMap(),
    val mediaFiles: List<CanonicalMedia> = emptyList()
)
