package com.synfusion.attendance.backup

import android.content.Context
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
        // Register core offline modules only
        registerModule(DatabaseBackupModule())
        registerModule(PreferencesBackupModule())
        registerModule(MediaBackupModule())
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

            DatabaseLifecycleCoordinator.stopAndCancelEverything()
            kotlinx.coroutines.delay(100)
            tempDb.close()

            TempDatabaseManager.swapToProduction(context, tempDb)
            AttendanceDatabase.getInstance(context)
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
        val dataStoreMap = readDataStoreValues(context)
        JsonExporter.exportPreferences(backupWorkingDir, dataStoreMap, emptyMap(), logger)
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
            dsMap.forEach { (key, rawValue) ->
                if (key == "auth_token") return@forEach
                var value = rawValue

                // Remap historical profile photo paths to current internal storage path
                if (key == "profile_photo_path" && value is String) {
                    val rawPath = value as String
                    val fileName = File(rawPath).name
                    val candidates = listOf(
                        File(context.filesDir, "profile_photos/$fileName"),
                        File(context.filesDir, "profile/$fileName")
                    )
                    val matchingFile = candidates.firstOrNull { it.exists() }
                    if (matchingFile != null) {
                        value = matchingFile.absolutePath
                    }
                }

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
                    "nav_left_1", "nav_left_2", "nav_right", "startup_screen", "last_active_screen", "user_country", "user_currency" -> {
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
