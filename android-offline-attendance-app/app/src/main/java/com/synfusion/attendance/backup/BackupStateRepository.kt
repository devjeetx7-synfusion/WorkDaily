package com.synfusion.attendance.backup

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.synfusion.attendance.data.offlineDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BackupStateRepository(private val context: Context) {

    companion object {
        val BACKUP_DIRTY_KEY = booleanPreferencesKey("backup_dirty")
        val LAST_DATA_MUTATION_AT_KEY = longPreferencesKey("last_data_mutation_at")
        val LAST_SUCCESSFUL_BACKUP_AT_KEY = longPreferencesKey("last_successful_backup_at")
        val LAST_BACKUP_STATUS_KEY = stringPreferencesKey("last_backup_status")
        val EXTERNAL_BACKUP_URI_KEY = stringPreferencesKey("external_backup_uri")
        val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("auto_backup_enabled")

        const val STATUS_PROTECTED = "Protected"
        const val STATUS_BACKUP_PENDING = "Backup Pending"
        const val STATUS_LOCAL_ONLY = "Local Only"
        const val STATUS_NEEDS_ATTENTION = "Needs Attention"
        const val STATUS_FAILED = "Failed"
    }

    val isBackupDirtyFlow: Flow<Boolean> = context.offlineDataStore.data.map { prefs ->
        prefs[BACKUP_DIRTY_KEY] ?: false
    }

    val lastDataMutationAtFlow: Flow<Long?> = context.offlineDataStore.data.map { prefs ->
        prefs[LAST_DATA_MUTATION_AT_KEY]
    }

    val lastSuccessfulBackupAtFlow: Flow<Long?> = context.offlineDataStore.data.map { prefs ->
        prefs[LAST_SUCCESSFUL_BACKUP_AT_KEY]
    }

    val backupStatusFlow: Flow<String> = context.offlineDataStore.data.map { prefs ->
        prefs[LAST_BACKUP_STATUS_KEY] ?: STATUS_LOCAL_ONLY
    }

    val externalBackupUriFlow: Flow<String?> = context.offlineDataStore.data.map { prefs ->
        prefs[EXTERNAL_BACKUP_URI_KEY]
    }

    val isAutoBackupEnabledFlow: Flow<Boolean> = context.offlineDataStore.data.map { prefs ->
        prefs[AUTO_BACKUP_ENABLED_KEY] ?: true
    }

    suspend fun markDataMutated() {
        val now = System.currentTimeMillis()
        context.offlineDataStore.edit { prefs ->
            prefs[BACKUP_DIRTY_KEY] = true
            prefs[LAST_DATA_MUTATION_AT_KEY] = now
            prefs[LAST_BACKUP_STATUS_KEY] = STATUS_BACKUP_PENDING
        }
        AutoBackupWorker.enqueueAutoBackup(context)
    }

    suspend fun markBackupSuccessful(timestamp: Long, size: Long, location: String) {
        context.offlineDataStore.edit { prefs ->
            prefs[BACKUP_DIRTY_KEY] = false
            prefs[LAST_SUCCESSFUL_BACKUP_AT_KEY] = timestamp
            prefs[LAST_BACKUP_STATUS_KEY] = if (location.contains("External")) STATUS_PROTECTED else STATUS_LOCAL_ONLY
        }
    }

    suspend fun markBackupFailed(reason: String) {
        context.offlineDataStore.edit { prefs ->
            prefs[LAST_BACKUP_STATUS_KEY] = STATUS_FAILED
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.offlineDataStore.edit { prefs ->
            prefs[AUTO_BACKUP_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveExternalBackupUri(uriStr: String?) {
        context.offlineDataStore.edit { prefs ->
            if (uriStr != null) {
                prefs[EXTERNAL_BACKUP_URI_KEY] = uriStr
            } else {
                prefs.remove(EXTERNAL_BACKUP_URI_KEY)
            }
        }
    }
}
