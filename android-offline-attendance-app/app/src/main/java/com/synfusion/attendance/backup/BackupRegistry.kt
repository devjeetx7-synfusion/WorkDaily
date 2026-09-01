package com.synfusion.attendance.backup

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.data.offlineDataStore
import kotlinx.coroutines.flow.first
import java.io.File

interface BackupModule {
    val name: String
    val keyName: String
    suspend fun exportModule(context: Context, backupWorkingDir: File, logger: BackupLogger): Map<String, Int>
    suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int>
}

object BackupRegistry {
    private val modules = mutableListOf<BackupModule>()

    init {
        // Register core & extensible modules
        registerModule(DatabaseBackupModule())
        registerModule(PreferencesBackupModule())
        registerModule(MediaBackupModule())
        registerModule(LeaveBackupModule())
        registerModule(CalendarBackupModule())
        registerModule(NotificationBackupModule())
    }

    fun registerModule(module: BackupModule) {
        if (modules.none { it.name == module.name }) {
            modules.add(module)
        }
    }

    fun getModules(): List<BackupModule> = modules
}

class DatabaseBackupModule : BackupModule {
    override val name: String = "Database"
    override val keyName: String = "database"

    override suspend fun exportModule(context: Context, backupWorkingDir: File, logger: BackupLogger): Map<String, Int> {
        val db = AttendanceDatabase.getInstance(context)
        return JsonExporter.exportDatabase(db, backupWorkingDir, logger)
    }

    override suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> {
        val tempDb = TempDatabaseManager.createTempDatabase(context)
        try {
            val counts = JsonImporter.importDatabase(tempDb, backupWorkingDir, merge, logger)

            // Redesigned Restore Lifecycle Sequence:
            // 1. Stop active collectors & cancel restore-sensitive jobs
            DatabaseLifecycleCoordinator.stopAndCancelEverything()

            // 2. Wait for all database transactions/queries to finish/cancel
            kotlinx.coroutines.delay(300)

            // 3. Close the temporary database safely
            tempDb.close()

            // 4. Swap data to production database securely (this closes production db via resetInstance() and renames files)
            TempDatabaseManager.swapToProduction(context, tempDb)

            // 5. Recreate production database instance cleanly
            AttendanceDatabase.getInstance(context)

            // 6. Refresh ViewModels (re-query data and trigger new flow subscription collections)
            DatabaseLifecycleCoordinator.refreshViewModels()

            return counts
        } finally {
            tempDb.close()
        }
    }
}

class PreferencesBackupModule : BackupModule {
    override val name: String = "Preferences"
    override val keyName: String = "preferences"

    override suspend fun exportModule(context: Context, backupWorkingDir: File, logger: BackupLogger): Map<String, Int> {
        val prefDir = File(backupWorkingDir, "preferences")
        val dataStoreMap = readDataStoreValues(context)
        JsonExporter.exportPreferences(prefDir, dataStoreMap, emptyMap(), logger)
        return mapOf("datastore" to dataStoreMap.size)
    }

    override suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> {
        val (dsPrefs, _) = JsonImporter.importPreferences(backupWorkingDir, logger)
        applyDataStorePreferences(context, dsPrefs)
        return mapOf("datastore" to dsPrefs.size)
    }

    private suspend fun readDataStoreValues(context: Context): Map<String, Any> {
        val prefs = context.offlineDataStore.data.first()
        val map = mutableMapOf<String, Any>()
        prefs.asMap().forEach { (key, value) ->
            if (key.name != "auth_token") {
                map[key.name] = value
            }
        }
        return map
    }

    private suspend fun applyDataStorePreferences(context: Context, dsMap: Map<String, Any>) {
        context.offlineDataStore.edit { preferences ->
            dsMap.forEach { (key, value) ->
                if (key == "auth_token") return@forEach
                when (key) {
                    "last_backup_time" -> {
                        val num = (value as? Number)?.toLong() ?: return@forEach
                        preferences[androidx.datastore.preferences.core.longPreferencesKey(key)] = num
                    }
                    "last_backup_size" -> {
                        val num = (value as? Number)?.toLong() ?: return@forEach
                        preferences[androidx.datastore.preferences.core.longPreferencesKey(key)] = num
                    }
                    "dark_mode", "reminders_enabled", "onboarding_completed", "splash_enabled", "remember_last_screen" -> {
                        val bool = value as? Boolean ?: return@forEach
                        preferences[androidx.datastore.preferences.core.booleanPreferencesKey(key)] = bool
                    }
                    "user_role", "app_language", "profile_photo_path", "last_backup_loc",
                    "nav_left_1", "nav_left_2", "nav_right", "startup_screen", "last_active_screen" -> {
                        val str = value.toString()
                        preferences[androidx.datastore.preferences.core.stringPreferencesKey(key)] = str
                    }
                    else -> {
                        when (value) {
                            is Boolean -> preferences[androidx.datastore.preferences.core.booleanPreferencesKey(key)] = value
                            is String -> preferences[androidx.datastore.preferences.core.stringPreferencesKey(key)] = value
                            is Number -> preferences[androidx.datastore.preferences.core.longPreferencesKey(key)] = value.toLong()
                        }
                    }
                }
            }
        }
    }
}

class MediaBackupModule : BackupModule {
    override val name: String = "Media"
    override val keyName: String = "media"

    override suspend fun exportModule(context: Context, backupWorkingDir: File, logger: BackupLogger): Map<String, Int> {
        val mediaDir = File(backupWorkingDir, "media")
        val count = MediaExporter.exportMedia(context.filesDir, mediaDir, logger)
        return mapOf("media" to count)
    }

    override suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> {
        val mediaDir = File(backupWorkingDir, "media")
        val count = MediaImporter.importMedia(mediaDir, context.filesDir, logger)
        return mapOf("media" to count)
    }
}

class LeaveBackupModule : BackupModule {
    override val name: String = "Leave"
    override val keyName: String = "leave"
    override suspend fun exportModule(context: Context, backupWorkingDir: File, logger: BackupLogger): Map<String, Int> = mapOf("leave" to 0)
    override suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> = mapOf("leave" to 0)
}

class CalendarBackupModule : BackupModule {
    override val name: String = "Calendar"
    override val keyName: String = "calendar"
    override suspend fun exportModule(context: Context, backupWorkingDir: File, logger: BackupLogger): Map<String, Int> = mapOf("calendar" to 0)
    override suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> = mapOf("calendar" to 0)
}

class NotificationBackupModule : BackupModule {
    override val name: String = "Notification"
    override val keyName: String = "notification"
    override suspend fun exportModule(context: Context, backupWorkingDir: File, logger: BackupLogger): Map<String, Int> = mapOf("notification" to 0)
    override suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> = mapOf("notification" to 0)
}
