package com.synfusion.attendance.backup

import com.synfusion.attendance.backup.dto.canonical.CanonicalBackupPayload
import com.synfusion.attendance.backup.dto.v1.BackupV1Payload
import com.synfusion.attendance.backup.migration.MigrationRegistry

object VersionMigrator {

    fun migratePayload(
        rawPayload: Any,
        sourceFormatVersion: Int,
        manifest: BackupManifest?,
        logger: BackupLogger
    ): CanonicalBackupPayload {
        return MigrationRegistry.migrateToCanonical(
            rawPayload = rawPayload,
            formatVersion = sourceFormatVersion,
            manifest = manifest,
            logger = logger
        )
    }
}
