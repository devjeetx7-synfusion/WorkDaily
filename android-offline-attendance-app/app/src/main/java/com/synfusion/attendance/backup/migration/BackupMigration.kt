package com.synfusion.attendance.backup.migration

import com.synfusion.attendance.backup.BackupManifest
import com.synfusion.attendance.backup.dto.canonical.CanonicalBackupPayload

class UnsupportedBackupVersionException(
    val formatVersion: Int,
    message: String
) : Exception(message)

interface BackupMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(rawPayload: Any, manifest: BackupManifest?, logger: com.synfusion.attendance.backup.BackupLogger? = null): CanonicalBackupPayload
}
