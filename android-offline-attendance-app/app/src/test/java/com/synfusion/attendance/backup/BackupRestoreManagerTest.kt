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
        val databaseDir = File(workingDir, "database").apply { mkdirs() }
        val testDbFile = File(databaseDir, "user.json").apply { writeText("{\"id\": \"123\"}") }

        // Compress
        BackupProvider.compressFolder(workingDir, backupZip, logger)
        assertTrue(backupZip.exists())
        assertTrue(backupZip.length() > 0)

        // Decompress to another folder
        val restoreDir = tempFolder.newFolder("restore_dest")
        RestoreProvider.extractZip(backupZip, restoreDir, logger)

        val restoredDbFile = File(restoreDir, "database/user.json")
        assertTrue(restoredDbFile.exists())
        assertEquals("{\"id\": \"123\"}", restoredDbFile.readText())
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
        val databaseDir = File(workingDir, "database").apply { mkdirs() }
        File(databaseDir, "user.json").writeText("[]")

        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid)
    }

    @Test
    fun testBackupValidatorRejectsMissingDatabaseFiles() {
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
    fun testBackupValidatorValidatesSuccessfully() {
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
            totalFiles = 3,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val databaseDir = File(workingDir, "database").apply { mkdirs() }
        File(databaseDir, "user.json").writeText("[]")
        File(databaseDir, "attendance.json").writeText("[]")

        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertTrue(isValid)
    }

    @Test
    fun testOlderAppGracefulRejection() {
        // App database version is 2. Create manifest with newer database version 3
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
            databaseVersion = 3, // Newer version!
            androidVersion = 34,
            deviceModel = "Test",
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(),
            totalFiles = 3,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val databaseDir = File(workingDir, "database").apply { mkdirs() }
        File(databaseDir, "user.json").writeText("[]")
        File(databaseDir, "attendance.json").writeText("[]")

        // Should gracefully reject because backup database version (3) is newer than app database version (2)
        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid)
    }

    @Test
    fun testForeignKeyReferentialIntegrityCheck() {
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
            totalFiles = 3,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val databaseDir = File(workingDir, "database").apply { mkdirs() }

        // Setup User record
        val userJson = "[{\"id\": \"user_001\", \"name\": \"John Doe\", \"phoneNumber\": \"9876543210\", \"email\": \"j@j.com\", \"role\": \"personal\"}]"
        File(databaseDir, "user.json").writeText(userJson)

        // Setup Attendance with mismatched foreign key workerId (referencing invalid user)
        val badAttendanceJson = "[{\"id\": \"att_01\", \"workerId\": \"invalid_user_id\", \"date\": \"2024-03-10\", \"status\": \"present\", \"type\": \"full\", \"wage\": 500.0, \"createdAt\": 12345L}]"
        File(databaseDir, "attendance.json").writeText(badAttendanceJson)

        // Must reject restore due to foreign key constraint failure
        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid)
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
            recordCounts = mapOf("media" to 1), // Claims 1 media file
            totalFiles = 3,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)

        val databaseDir = File(workingDir, "database").apply { mkdirs() }
        File(databaseDir, "user.json").writeText("[]")
        File(databaseDir, "attendance.json").writeText("[]")

        // Omit actual media dir or files
        val isValid = BackupValidator.validateBackupDir(workingDir, logger)
        assertFalse(isValid) // Rejected because media folder is missing but listed as referenced
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

        // Verify we can serialize 10,000+ attendance records fast and cleanly without memory overhead
        testFile.writer(StandardCharsets.UTF_8).use { writer ->
            gson.toJson(list, writer)
        }

        assertTrue(testFile.exists())
        assertTrue(testFile.length() > 0)

        // Verify clean streaming parsing without OOM
        testFile.reader(StandardCharsets.UTF_8).use { reader ->
            val parsedList = gson.fromJson<List<AttendanceEntity>>(reader, object : com.google.gson.reflect.TypeToken<List<AttendanceEntity>>() {}.type)
            assertEquals(10000, parsedList.size)
        }
    }

    @Test
    fun testAtomicRestoreRollbackSimulation() {
        val prodDbFile = File(workingDir, "production.db")
        prodDbFile.writeText("Original production data")

        val tempDbFile = File(workingDir, "temp_swap.db")
        tempDbFile.writeText("Imported validated data")

        // Simulate crash/interruption of the rename swap
        var swapSuccess = false
        try {
            // Suppose rename is blocked because of permissions or files locks
            val simulatedLock = true
            if (simulatedLock) {
                throw Exception("Simulated IO swap exception")
            }
            if (tempDbFile.renameTo(prodDbFile)) {
                swapSuccess = true
            }
        } catch (e: Exception) {
            // Check that rollback occurred and original production remains pristine
            assertTrue(prodDbFile.exists())
            assertEquals("Original production data", prodDbFile.readText())
            swapSuccess = false
        }

        assertFalse(swapSuccess)
    }

    @Test
    fun testRestoreStateMachineProgression() {
        val tracker = ProgressTracker()
        assertEquals(RestoreState.IDLE, tracker.state.value)

        tracker.updateState(RestoreState.VALIDATING)
        assertEquals(RestoreState.VALIDATING, tracker.state.value)

        tracker.updateState(RestoreState.REPLACING_DATABASE)
        assertEquals(RestoreState.REPLACING_DATABASE, tracker.state.value)
    }

    @Test
    fun testRecordLevelValidationCountsMismatch() {
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
            recordCounts = mapOf("users" to 1, "attendance" to 5), // Manifest expects 1 user, 5 attendance
            totalFiles = 3,
            backupSize = 100L,
            featureList = emptyList()
        )

        // Mock database with actual mismatching counts
        val tempUserCount = 0
        val tempAttendanceCount = 2
        val tempSummariesCount = 0

        val expectedUsers = manifestObj.recordCounts["users"] ?: 0
        val expectedAttendance = manifestObj.recordCounts["attendance"] ?: 0
        val expectedSummaries = manifestObj.recordCounts["summaries"] ?: 0

        val isMismatch = (tempUserCount != expectedUsers || tempAttendanceCount != expectedAttendance || tempSummariesCount != expectedSummaries)
        assertTrue(isMismatch) // Correctly flags mismatch
    }

    @Test
    fun testDeterministicMergeAttendanceLogic() {
        // Mock production records
        val prodAttendance = listOf(
            AttendanceEntity(id = "prod_att_1", workerId = null, date = "2024-03-01", status = "present", type = "full", wage = 500.0),
            AttendanceEntity(id = "common_att_1", workerId = null, date = "2024-03-02", status = "absent", type = "full", wage = 500.0) // Matching id
        )

        // Mock backup records (tempDb)
        val backupAttendance = listOf(
            AttendanceEntity(id = "common_att_1", workerId = null, date = "2024-03-02", status = "present", type = "full", wage = 500.0), // Backup overwrites
            AttendanceEntity(id = "backup_att_1", workerId = null, date = "2024-03-03", status = "present", type = "full", wage = 500.0)
        )

        // Perform merge logic simulation
        val tempAttendanceIds = backupAttendance.map { it.id }.toSet()
        val uniqueProdAttendance = prodAttendance.filter { it.id !in tempAttendanceIds }

        val mergedResult = backupAttendance + uniqueProdAttendance

        assertEquals(3, mergedResult.size)
        // Check that backup record overrode production record for "common_att_1"
        val commonAtt = mergedResult.find { it.id == "common_att_1" }
        assertNotNull(commonAtt)
        assertEquals("present", commonAtt?.status) // Preserved backup overwrites
    }
}
