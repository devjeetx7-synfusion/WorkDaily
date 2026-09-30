package com.synfusion.attendance.backup.migration

import com.synfusion.attendance.backup.BackupLogger
import com.synfusion.attendance.backup.BackupManifest
import com.synfusion.attendance.backup.BackupVersionConstants
import com.synfusion.attendance.backup.dto.canonical.CanonicalBackupPayload

object MigrationRegistry {

    private val migrations = mutableMapOf<Int, BackupMigration>()

    init {
        registerMigration(V1ToCanonicalMigration())
    }

    fun registerMigration(migration: BackupMigration) {
        migrations[migration.fromVersion] = migration
    }

    fun migrateToCanonical(
        rawPayload: Any,
        formatVersion: Int,
        manifest: BackupManifest?,
        logger: BackupLogger? = null
    ): CanonicalBackupPayload {
        if (formatVersion > BackupVersionConstants.CURRENT_BACKUP_FORMAT_VERSION) {
            throw UnsupportedBackupVersionException(
                formatVersion,
                "This backup was created by a newer version of WorkDaily (v$formatVersion). Update WorkDaily to restore it."
            )
        }

        val migration = migrations[formatVersion]
            ?: throw IllegalStateException("No backup migration registered for format version $formatVersion")

        logger?.info("Executing migration for backup format version $formatVersion...")
        return migration.migrate(rawPayload, manifest, logger)
    }
}
