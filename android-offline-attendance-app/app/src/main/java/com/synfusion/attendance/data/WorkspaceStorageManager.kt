package com.synfusion.attendance.data

import android.content.Context
import com.synfusion.attendance.backup.DatabaseLifecycleCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

data class ArchivedWorkspaceMetadata(
    val workspaceKey: String,
    val localUserId: String? = null,
    val profilePhotoPath: String? = null,
    val googlePhotoUrl: String? = null,
    val countryCode: String = "IN",
    val currencyCode: String = "INR",
    val onlineAccountSetupCompleted: Boolean = false
)

interface WorkspaceStorageManager {
    suspend fun hasArchivedWorkspace(workspaceKey: String): Boolean
    suspend fun getArchivedMetadata(workspaceKey: String): ArchivedWorkspaceMetadata?
    suspend fun switchWorkspace(fromWorkspaceKey: String?, toWorkspaceKey: String)
}

class AndroidWorkspaceStorageManager(
    private val context: Context,
    private val preferencesRepository: OfflinePreferencesRepository
) : WorkspaceStorageManager {

    companion object {
        private const val DB_NAME = "synfusion_offline_attendance_db"
        private const val WORKSPACES_DIR = "workspaces"
        private const val REGISTRY_PREFS = "workdaily_workspace_registry"

        fun sanitizeKey(rawKey: String): String {
            return rawKey.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "default" }
        }
    }

    // Fallback metadata store when running under unit-test DummyContext(null)
    private val fallbackMetadataMap = mutableMapOf<String, ArchivedWorkspaceMetadata>()

    private fun resolveWorkspacesRoot(): File? {
        return try {
            val baseDir = context.filesDir ?: return null
            File(baseDir, WORKSPACES_DIR).apply { if (!exists()) mkdirs() }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun hasArchivedWorkspace(workspaceKey: String): Boolean = withContext(Dispatchers.IO) {
        if (workspaceKey.isBlank()) return@withContext false
        val safeKey = sanitizeKey(workspaceKey)
        val root = resolveWorkspacesRoot()
        if (root == null) {
            return@withContext fallbackMetadataMap.containsKey(safeKey)
        }
        val dbFile = File(File(root, safeKey), DB_NAME)
        dbFile.exists() && dbFile.length() > 0L
    }

    override suspend fun getArchivedMetadata(workspaceKey: String): ArchivedWorkspaceMetadata? = withContext(Dispatchers.IO) {
        if (workspaceKey.isBlank()) return@withContext null
        val safeKey = sanitizeKey(workspaceKey)
        val prefs = try {
            context.getSharedPreferences(REGISTRY_PREFS, Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        if (prefs == null) {
            return@withContext fallbackMetadataMap[safeKey]
        }
        if (!prefs.contains("${safeKey}_exists")) return@withContext null
        ArchivedWorkspaceMetadata(
            workspaceKey = workspaceKey,
            localUserId = prefs.getString("${safeKey}_local_user_id", null),
            profilePhotoPath = prefs.getString("${safeKey}_profile_photo_path", null),
            googlePhotoUrl = prefs.getString("${safeKey}_google_photo_url", null),
            countryCode = prefs.getString("${safeKey}_country_code", "IN") ?: "IN",
            currencyCode = prefs.getString("${safeKey}_currency_code", "INR") ?: "INR",
            onlineAccountSetupCompleted = prefs.getBoolean("${safeKey}_online_setup_completed", false)
        )
    }

    private fun saveMetadataToRegistry(safeKey: String, metadata: ArchivedWorkspaceMetadata) {
        val prefs = try {
            context.getSharedPreferences(REGISTRY_PREFS, Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        if (prefs == null) {
            fallbackMetadataMap[safeKey] = metadata
            return
        }
        prefs.edit()
            .putBoolean("${safeKey}_exists", true)
            .putString("${safeKey}_local_user_id", metadata.localUserId)
            .putString("${safeKey}_profile_photo_path", metadata.profilePhotoPath)
            .putString("${safeKey}_google_photo_url", metadata.googlePhotoUrl)
            .putString("${safeKey}_country_code", metadata.countryCode)
            .putString("${safeKey}_currency_code", metadata.currencyCode)
            .putBoolean("${safeKey}_online_setup_completed", metadata.onlineAccountSetupCompleted)
            .commit()
    }

    override suspend fun switchWorkspace(
        fromWorkspaceKey: String?,
        toWorkspaceKey: String
    ) = withContext(Dispatchers.IO) {
        val cleanTo = toWorkspaceKey.trim()
        require(cleanTo.isNotBlank()) { "Target workspace key cannot be blank." }
        val cleanFrom = fromWorkspaceKey?.trim()?.takeIf { it.isNotBlank() }

        if (cleanFrom != null && sanitizeKey(cleanFrom) == sanitizeKey(cleanTo)) {
            return@withContext
        }

        val currentPhotoPath = try { preferencesRepository.profilePhotoPathFlow.firstOrNull() } catch (_: Exception) { null }
        val currentGooglePhoto = try { preferencesRepository.googlePhotoUrlFlow.firstOrNull() } catch (_: Exception) { null }
        val currentCountry = try { preferencesRepository.countryFlow.firstOrNull() ?: "IN" } catch (_: Exception) { "IN" }
        val currentCurrency = try { preferencesRepository.currencyFlow.firstOrNull() ?: "INR" } catch (_: Exception) { "INR" }
        val currentSetupDone = try { preferencesRepository.isOnlineAccountSetupCompletedFlow.firstOrNull() ?: false } catch (_: Exception) { false }

        val root = resolveWorkspacesRoot()
        if (root == null) {
            // Unit-test environment without Android filesystem
            if (cleanFrom != null) {
                saveMetadataToRegistry(
                    sanitizeKey(cleanFrom),
                    ArchivedWorkspaceMetadata(
                        workspaceKey = cleanFrom,
                        profilePhotoPath = currentPhotoPath,
                        googlePhotoUrl = currentGooglePhoto,
                        countryCode = currentCountry,
                        currencyCode = currentCurrency,
                        onlineAccountSetupCompleted = currentSetupDone
                    )
                )
            }
            val targetMeta = getArchivedMetadata(cleanTo)
            if (targetMeta != null) {
                preferencesRepository.saveProfilePhotoPath(targetMeta.profilePhotoPath)
                preferencesRepository.saveGooglePhotoUrl(targetMeta.googlePhotoUrl)
                preferencesRepository.saveCountryAndCurrency(targetMeta.countryCode, targetMeta.currencyCode)
                preferencesRepository.saveOnlineAccountSetupCompleted(targetMeta.onlineAccountSetupCompleted)
            } else {
                preferencesRepository.saveProfilePhotoPath(null)
                preferencesRepository.saveGooglePhotoUrl(null)
                preferencesRepository.saveOnlineAccountSetupCompleted(false)
            }
            return@withContext
        }

        val currentLocalUserId = try {
            AttendanceDatabase.getInstance(context).userDao().getUser()?.id
        } catch (_: Exception) {
            null
        }

        DatabaseLifecycleCoordinator.stopAndCancelEverything()
        AttendanceDatabase.beginSwap()

        try {
            val prodDbFile = context.getDatabasePath(DB_NAME)
            val prodWalFile = File(prodDbFile.absolutePath + "-wal")
            val prodShmFile = File(prodDbFile.absolutePath + "-shm")

            // 1. Archive active workspace safely if fromWorkspaceKey is provided
            if (cleanFrom != null) {
                val safeFrom = sanitizeKey(cleanFrom)
                val fromDir = File(root, safeFrom).apply { if (!exists()) mkdirs() }
                val archiveDb = File(fromDir, DB_NAME)
                val archiveWal = File(fromDir, "$DB_NAME-wal")
                val archiveShm = File(fromDir, "$DB_NAME-shm")

                archiveWal.delete()
                archiveShm.delete()

                if (prodDbFile.exists()) {
                    prodDbFile.copyTo(archiveDb, overwrite = true)
                    if (!archiveDb.exists() || archiveDb.length() != prodDbFile.length()) {
                        throw IllegalStateException("Failed to archive current workspace database safely.")
                    }
                }
                if (prodWalFile.exists()) {
                    prodWalFile.copyTo(archiveWal, overwrite = true)
                }
                if (prodShmFile.exists()) {
                    prodShmFile.copyTo(archiveShm, overwrite = true)
                }

                saveMetadataToRegistry(
                    safeFrom,
                    ArchivedWorkspaceMetadata(
                        workspaceKey = cleanFrom,
                        localUserId = currentLocalUserId,
                        profilePhotoPath = currentPhotoPath,
                        googlePhotoUrl = currentGooglePhoto,
                        countryCode = currentCountry,
                        currencyCode = currentCurrency,
                        onlineAccountSetupCompleted = currentSetupDone
                    )
                )
            }

            // 2. Activate target workspace (restore archived DB if present, else start fresh isolated DB)
            val safeTo = sanitizeKey(cleanTo)
            val toDir = File(root, safeTo)
            val targetArchiveDb = File(toDir, DB_NAME)
            val targetArchiveWal = File(toDir, "$DB_NAME-wal")
            val targetArchiveShm = File(toDir, "$DB_NAME-shm")

            prodWalFile.delete()
            prodShmFile.delete()
            if (prodDbFile.exists()) {
                prodDbFile.delete()
            }

            if (targetArchiveDb.exists() && targetArchiveDb.length() > 0L) {
                if (prodDbFile.parentFile?.exists() == false) {
                    prodDbFile.parentFile?.mkdirs()
                }
                targetArchiveDb.copyTo(prodDbFile, overwrite = true)
                if (targetArchiveWal.exists()) {
                    targetArchiveWal.copyTo(prodWalFile, overwrite = true)
                }
                if (targetArchiveShm.exists()) {
                    targetArchiveShm.copyTo(prodShmFile, overwrite = true)
                }
            }
        } finally {
            AttendanceDatabase.endSwap()
        }

        // 3. Apply target workspace preferences outside DB lock
        val targetMeta = getArchivedMetadata(cleanTo)
        if (targetMeta != null) {
            val validPhotoPath = targetMeta.profilePhotoPath?.takeIf { File(it).exists() }
            preferencesRepository.saveProfilePhotoPath(validPhotoPath)
            preferencesRepository.saveGooglePhotoUrl(targetMeta.googlePhotoUrl)
            preferencesRepository.saveCountryAndCurrency(targetMeta.countryCode, targetMeta.currencyCode)
            preferencesRepository.saveOnlineAccountSetupCompleted(targetMeta.onlineAccountSetupCompleted)
        } else {
            preferencesRepository.saveProfilePhotoPath(null)
            preferencesRepository.saveGooglePhotoUrl(null)
            preferencesRepository.saveOnlineAccountSetupCompleted(false)
        }

        // Re-open fresh DB instance and notify registered ViewModels
        AttendanceDatabase.getInstance(context)
        DatabaseLifecycleCoordinator.refreshViewModels()
    }
}
