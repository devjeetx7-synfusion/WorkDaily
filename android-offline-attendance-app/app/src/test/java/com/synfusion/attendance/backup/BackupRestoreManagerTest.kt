package com.synfusion.attendance.backup

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets
import com.google.gson.Gson
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity

class BackupRestoreManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var workingDir: File
    private lateinit var backupZip: File
    private lateinit var logFile: File
    private lateinit var logger: BackupLogger

    @Before
    fun setUp() {
        workingDir = tempFolder.newFolder("backup_root")
        backupZip = tempFolder.newFile("AttendanceBackup.zip")
        logFile = tempFolder.newFile("backup.log")
        logger = BackupLogger(logFile)
    }

    @Test
    fun testChecksumVerification() {
        val testFile = File(workingDir, "test.json")
        testFile.writeText("{\"data\": \"hello world\"}")

        val checksum = ChecksumManager.computeSha256(testFile)
        assertTrue(checksum.isNotEmpty())

        assertTrue(ChecksumManager.verifyChecksum(testFile, checksum))
        assertFalse(ChecksumManager.verifyChecksum(testFile, "wrong_checksum"))
    }

    @Test
    fun testZipCompressionAndExtraction() {
        val dataFile = File(workingDir, "data.json").apply { writeText("{\"users\": [{\"id\": \"123\"}]}") }

        // Compress
        BackupProvider.compressFolder(workingDir, backupZip, logger)
        assertTrue(backupZip.exists())
        assertTrue(backupZip.length() > 0)

        // Decompress to another folder
        val restoreDir = tempFolder.newFolder("restore_dest")
        RestoreProvider.extractZip(backupZip, restoreDir, logger)

        val restoredDataFile = File(restoreDir, "data.json")
        assertTrue(restoredDataFile.exists())
        assertEquals("{\"users\": [{\"id\": \"123\"}]}", restoredDataFile.readText())
    }

    @Test
    fun testCleanDataJsonBackupStructure() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 2,
            androidVersion = 34,
            deviceModel = "Pixel Test",
            packageName = "com.synfusion.attendance",
            recordCounts = mapOf("attendance" to 1),
            totalFiles = 2,
            backupSize = 500L,
            featureList = listOf("Database", "Preferences")
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val user = UserEntity(id = "user_001", name = "राम कुमार (Ram Kumar)", phoneNumber = "9876543210", email = "ram@workdaily.in", role = "personal")
        val attendance = AttendanceEntity(id = "personal_2026-03-10", workerId = null, date = "2026-03-10", status = "present", type = "full", wage = 750.0, note = "ओवरटाइम काम किया")
        val payload = BackupDataPayload(
            users = listOf(user),
            attendance = listOf(attendance),
            summaries = emptyList(),
            preferences = mapOf("app_language" to "hi")
        )

        val dataFile = File(workingDir, "data.json")
        dataFile.writer(StandardCharsets.UTF_8).use { writer ->
            Gson().toJson(payload, writer)
        }

        BackupProvider.compressFolder(workingDir, backupZip, logger)
        assertTrue(backupZip.exists())

        // Extract and verify clean data.json content and Unicode preservation
        val extractDir = tempFolder.newFolder("clean_verify_extract")
        RestoreProvider.extractZip(backupZip, extractDir, logger)

        val extractedDataFile = File(extractDir, "data.json")
        assertTrue(extractedDataFile.exists())

        val readPayload = Gson().fromJson(extractedDataFile.readText(StandardCharsets.UTF_8), BackupDataPayload::class.java)
        assertNotNull(readPayload)
        assertEquals(1, readPayload.users.size)
        assertEquals("राम कुमार (Ram Kumar)", readPayload.users.first().name)
        assertEquals("ओवरटाइम काम किया", readPayload.attendance.first().note)
        assertNull(readPayload.attendance.first().workerId)

        // Ensure no junk files exist in ZIP
        val extractedFiles = extractDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(extractedFiles.contains("manifest.json"))
        assertTrue(extractedFiles.contains("data.json"))
        assertFalse(extractedFiles.contains("database"))
        assertFalse(extractedFiles.contains("logs"))
        assertFalse(extractedFiles.contains("metadata"))
    }

    @Test
    fun testManifestManagerWriteAndRead() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 2,
            androidVersion = 34,
            deviceModel = "Pixel Test",
            packageName = "com.synfusion.attendance",
            recordCounts = mapOf("attendance" to 42),
            totalFiles = 10,
            backupSize = 2048L,
            featureList = listOf("Profile", "Attendance")
        )

        ManifestManager.writeManifest(manifestFile, manifestObj)
        assertTrue(manifestFile.exists())

        val readObj = ManifestManager.readManifest(manifestFile)
        assertNotNull(readObj)
        assertEquals("test-uuid", readObj?.backupUuid)
        assertEquals(1, readObj?.backupVersion)
        assertEquals("1.0.0", readObj?.appVersion)
        assertEquals(2, readObj?.databaseVersion)
        assertEquals("Pixel Test", readObj?.deviceModel)
        assertEquals(42, readObj?.recordCounts?.get("attendance"))
    }

    @Test
    fun testProgressTracker() {
        val tracker = ProgressTracker()

        tracker.updateProgress(50f, "Halfway done")
        assertEquals(50f, tracker.progress.value)
        assertEquals("Halfway done", tracker.status.value)

        // Test cancellation exception
        tracker.cancel()
        try {
            tracker.checkCancelled()
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("Operation was cancelled by user.", e.message)
        }
    }

    @Test
    fun testVersionMigratorWithEqualOrHigherVersion() {
        val sourceVer = 1
        val targetVer = 1
        val (migratedUsers, migratedAttendances) = VersionMigrator.migrateData(
            users = emptyList(),
            attendances = emptyList(),
            sourceBackupVersion = sourceVer,
            targetBackupVersion = targetVer,
            logger = logger
        )
        assertTrue(migratedUsers.isEmpty())
        assertTrue(migratedAttendances.isEmpty())
    }

    @Test
    fun testBackupValidatorRejectsMissingManifest() {
        File(workingDir, "data.json").writeText("{}")

        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid)
    }

    @Test
    fun testBackupValidatorRejectsMissingDataFile() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 2,
            androidVersion = 34,
            deviceModel = "Test",
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(),
            totalFiles = 1,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid)
    }

    @Test
    fun testBackupValidatorValidatesSuccessfullyWithDataJson() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 2,
            androidVersion = 34,
            deviceModel = "Test",
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(),
            totalFiles = 2,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val payload = BackupDataPayload(users = emptyList(), attendance = emptyList(), summaries = emptyList())
        File(workingDir, "data.json").writeText(Gson().toJson(payload))

        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertTrue(isValid)
    }

    @Test
    fun testOlderAppGracefulRejection() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 3, // Newer database version code!
            androidVersion = 34,
            deviceModel = "Test",
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(),
            totalFiles = 2,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        File(workingDir, "data.json").writeText("{}")

        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid)
    }

    @Test
    fun testForeignKeyReferentialIntegrityCheckPassesForNullWorkerId() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 2,
            androidVersion = 34,
            deviceModel = "Test",
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(),
            totalFiles = 2,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val user = UserEntity(id = "user_001", name = "John Doe", phoneNumber = "9876543210", email = "j@j.com", role = "personal")
        val personalAttendance = AttendanceEntity(id = "personal_2026-03-10", workerId = null, date = "2026-03-10", status = "present", type = "full", wage = 500.0)

        val payload = BackupDataPayload(users = listOf(user), attendance = listOf(personalAttendance))
        File(workingDir, "data.json").writeText(Gson().toJson(payload))

        val result = BackupValidator.validateBackupDirDetailed(workingDir, logger)
        assertTrue(result.isValid)
    }

    @Test
    fun testForeignKeyReferentialIntegrityCheckFailsForInvalidWorkerId() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 2,
            androidVersion = 34,
            deviceModel = "Test",
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(),
            totalFiles = 2,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val user = UserEntity(id = "user_001", name = "John Doe", phoneNumber = "9876543210", email = "j@j.com", role = "personal")
        val badAttendance = AttendanceEntity(id = "att_01", workerId = "non_existent_worker_id", date = "2026-03-10", status = "present", type = "full", wage = 500.0)

        val payload = BackupDataPayload(users = listOf(user), attendance = listOf(badAttendance))
        File(workingDir, "data.json").writeText(Gson().toJson(payload))

        val result = BackupValidator.validateBackupDirDetailed(workingDir, logger)
        assertFalse(result.isValid)
        assertEquals(BackupValidationCode.INVALID_DATA, result.code)
    }

    @Test
    fun testMediaReferenceValidationFailsWhenMissing() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 1L,
            backupFormatVersion = 1,
            backupVersion = 1,
            appVersion = "1.0.0",
            databaseVersion = 2,
            androidVersion = 34,
            deviceModel = "Test",
            packageName = "com.synfusion.attendance",
            recordCounts = mapOf("media" to 1),
            totalFiles = 2,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val payload = BackupDataPayload(users = emptyList(), attendance = emptyList())
        File(workingDir, "data.json").writeText(Gson().toJson(payload))

        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid)
    }

    @Test
    fun testLargeDatasetStressPerformanceNoOom() {
        val gson = Gson()
        val list = ArrayList<AttendanceEntity>()
        for (i in 1..10000) {
            list.add(
                AttendanceEntity(
                    id = "personal_date_$i",
                    workerId = null,
                    date = "2024-03-10",
                    status = "present",
                    type = "full",
                    wage = 500.0,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        val testFile = File(workingDir, "large_attendance.json")

        testFile.writer(StandardCharsets.UTF_8).use { writer ->
            gson.toJson(list, writer)
        }

        assertTrue(testFile.exists())
        assertTrue(testFile.length() > 0)

        testFile.reader(StandardCharsets.UTF_8).use { reader ->
            val parsedList = gson.fromJson<List<AttendanceEntity>>(reader, object : com.google.gson.reflect.TypeToken<List<AttendanceEntity>>() {}.type)
            assertEquals(10000, parsedList.size)
        }
    }

    @Test
    fun testSanitizeBackupFileName() {
        assertEquals("My_Backup.zip", com.synfusion.attendance.ui.components.sanitizeBackupFileName("My_Backup"))
        assertEquals("हाजिरी_बैकअप.zip", com.synfusion.attendance.ui.components.sanitizeBackupFileName("हाजिरी_बैकअप"))
        assertEquals("WorkDaily_Backup_2026.zip", com.synfusion.attendance.ui.components.sanitizeBackupFileName("WorkDaily/Backup:2026"))
        assertEquals("my_file.zip", com.synfusion.attendance.ui.components.sanitizeBackupFileName("my_file.zip.zip"))
        assertEquals("my_file.zip", com.synfusion.attendance.ui.components.sanitizeBackupFileName("my_file.backup"))
        assertTrue(com.synfusion.attendance.ui.components.sanitizeBackupFileName("  ").startsWith("WorkDaily_Backup_"))
        assertTrue(com.synfusion.attendance.ui.components.sanitizeBackupFileName("  ").endsWith(".zip"))
    }
}
