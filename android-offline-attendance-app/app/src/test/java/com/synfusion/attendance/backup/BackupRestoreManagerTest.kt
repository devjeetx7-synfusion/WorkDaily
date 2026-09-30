package com.synfusion.attendance.backup

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets
import com.google.gson.Gson
import com.synfusion.attendance.backup.dto.canonical.*
import com.synfusion.attendance.backup.dto.v1.*
import com.synfusion.attendance.backup.migration.MigrationRegistry
import com.synfusion.attendance.backup.migration.UnsupportedBackupVersionException
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
    fun testHistoricalV1BackupRestore() {
        val historicalDir = File("/tmp/file_attachments/My WorkDaily Backup (3)")
        val targetDir = if (historicalDir.exists() && File(historicalDir, "data.json").exists()) {
            historicalDir
        } else {
            // Fallback fixture creation if file_attachments not present
            val fixtureDir = tempFolder.newFolder("historical_fixture")
            val manifestFile = File(fixtureDir, "manifest.json")
            val manifestObj = ManifestManager.generateManifest(
                backupUuid = "ded325f3-a383-481f-9a0c-557a7c12bdc8",
                creationTimestamp = 1790258793955L,
                totalBackupDurationMs = 67L,
                compressionRatio = 1.0,
                appBuildNumber = 1L,
                backupFormatVersion = 1,
                backupVersion = 1,
                appVersion = "1.0.0",
                databaseVersion = 2,
                androidVersion = 36,
                deviceModel = "V2334",
                packageName = "com.synfusion.attendance",
                recordCounts = mapOf("users" to 1, "attendance" to 50, "summaries" to 2, "media" to 1),
                totalFiles = 4,
                backupSize = 71069L,
                featureList = listOf("Database", "Preferences", "Media")
            )
            ManifestManager.writeManifest(manifestFile, manifestObj)

            val rawUser = UserV1Dto(id = "72d4a90d-9eab-471f-94b5-5884a1e12662", name = "Dharmjeet Singh", phoneNumber = "9334603912", role = "personal", dailyWage = 400.0)
            val rawAtt = (1..50).map { i -> AttendanceV1Dto(id = "personal_2026-08-$i", date = "2026-08-${if (i < 10) "0$i" else "$i"}", status = "present", type = "full", wage = 400.0) }
            val rawSum = listOf(
                SummaryV1Dto(id = "personal_2026-08", monthId = "2026-08", role = "personal"),
                SummaryV1Dto(id = "personal_2026-09", monthId = "2026-09", role = "personal")
            )
            val rawPayload = BackupV1Payload(users = listOf(rawUser), attendance = rawAtt, summaries = rawSum, preferences = mapOf("app_language" to "hi"))

            File(fixtureDir, "data.json").writeText(Gson().toJson(rawPayload))
            val mediaDir = File(fixtureDir, "media/profile_photos").apply { mkdirs() }
            File(mediaDir, "avatar.png").writeText("fake image content")
            fixtureDir
        }

        val canonicalPayload = kotlinx.coroutines.runBlocking {
            JsonImporter.parseAndMigrateBackup(targetDir, logger)
        }

        assertEquals(1, canonicalPayload.users.size)
        val user = canonicalPayload.users.first()
        assertEquals("Dharmjeet Singh", user.name)
        assertEquals("9334603912", user.phoneNumber)
        assertEquals("IN", user.countryCode)
        assertEquals("INR", user.currencyCode)

        assertEquals(50, canonicalPayload.attendance.size)
        val firstAtt = canonicalPayload.attendance.first()
        assertEquals("INR", firstAtt.currencyCode)
        assertEquals(400.0, firstAtt.wage, 0.01)

        assertEquals(2, canonicalPayload.summaries.size)
    }

    @Test
    fun testLegacyBackupWithoutManifest() {
        val legacyDir = tempFolder.newFolder("legacy_no_manifest")
        val rawUser = UserV1Dto(id = "u1", name = "Legacy User", dailyWage = 500.0)
        val rawAtt = AttendanceV1Dto(id = "personal_2026-01-01", date = "2026-01-01", status = "present", wage = 500.0)
        val rawPayload = BackupV1Payload(users = listOf(rawUser), attendance = listOf(rawAtt))

        File(legacyDir, "data.json").writeText(Gson().toJson(rawPayload))

        val result = BackupValidator.validateBackupDirDetailed(legacyDir, logger)
        assertTrue(result.isValid)

        val canonical = kotlinx.coroutines.runBlocking {
            JsonImporter.parseAndMigrateBackup(legacyDir, logger)
        }
        assertEquals(1, canonical.users.size)
        assertEquals("Legacy User", canonical.users.first().name)
        assertEquals("IN", canonical.users.first().countryCode)
        assertEquals("INR", canonical.users.first().currencyCode)
    }

    @Test
    fun testLenientTypeParsingInLegacyJson() {
        val jsonWithStringsAndDoubles = """
        {
            "users": [{ "id": "u1", "name": "Lenient User", "dailyWage": "600.0", "createdAt": "1700000000000" }],
            "attendance": [{ "id": "personal_2026-01-01", "date": "2026-01-01", "status": "present", "overtimeHours": "2.0", "wage": "600.0", "advanceAmount": "100" }]
        }
        """.trimIndent()

        val jsonDir = tempFolder.newFolder("lenient_json_dir")
        File(jsonDir, "data.json").writeText(jsonWithStringsAndDoubles)

        val canonical = kotlinx.coroutines.runBlocking {
            JsonImporter.parseAndMigrateBackup(jsonDir, logger)
        }

        assertEquals(1, canonical.users.size)
        assertEquals(600.0, canonical.users.first().dailyWage, 0.01)
        assertEquals(1700000000000L, canonical.users.first().createdAt)

        assertEquals(1, canonical.attendance.size)
        assertEquals(2, canonical.attendance.first().overtimeHours)
        assertEquals(600.0, canonical.attendance.first().wage, 0.01)
        assertEquals(100.0, canonical.attendance.first().advanceAmount, 0.01)
    }

    @Test
    fun testCanonicalBackupRoundTrip() {
        val canonicalUser = CanonicalUser(
            id = "user_100",
            name = "Test Worker",
            phoneNumber = "9998887770",
            email = "test@workdaily.in",
            role = "personal",
            dailyWage = 600.0,
            countryCode = "IN",
            currencyCode = "INR",
            createdAt = 1000000L
        )

        val canonicalAttendance = CanonicalAttendance(
            id = "personal_2026-09-01",
            workerId = null,
            date = "2026-09-01",
            status = "present",
            type = "full",
            reason = null,
            overtimeHours = 2,
            note = "Night shift",
            advanceAmount = 500.0,
            wage = 600.0,
            currencyCode = "INR",
            createdAt = 1000005L
        )

        val payload = CanonicalBackupPayload(
            manifest = null,
            users = listOf(canonicalUser),
            attendance = listOf(canonicalAttendance),
            summaries = emptyList()
        )

        val json = Gson().toJson(payload)
        val deserialized = Gson().fromJson(json, CanonicalBackupPayload::class.java)

        assertEquals(1, deserialized.users.size)
        assertEquals("Test Worker", deserialized.users.first().name)
        assertEquals(1, deserialized.attendance.size)
        assertEquals("Night shift", deserialized.attendance.first().note)
        assertEquals(500.0, deserialized.attendance.first().advanceAmount, 0.01)
    }

    @Test
    fun testRestoreEditBackupRestoreLifecycle() {
        // 1. Initial V1 DTO
        val rawUser = UserV1Dto(id = "user_test", name = "Original Name", dailyWage = 500.0)
        val rawAtt = AttendanceV1Dto(id = "personal_2026-08-01", date = "2026-08-01", status = "present", type = "full", wage = 500.0, note = "Old note")
        val initialV1 = BackupV1Payload(users = listOf(rawUser), attendance = listOf(rawAtt))

        // 2. Migrate to canonical
        val migrated1 = MigrationRegistry.migrateToCanonical(initialV1, 1, null, logger)
        assertEquals(1, migrated1.attendance.size)
        assertEquals("Old note", migrated1.attendance.first().note)

        // 3. User edits historical record & adds new record
        val editedAtt = migrated1.attendance.first().copy(note = "Edited note", wage = 600.0)
        val newAtt = CanonicalAttendance(
            id = "personal_2026-10-01",
            workerId = null,
            date = "2026-10-01",
            status = "present",
            type = "full",
            reason = null,
            overtimeHours = 1,
            note = "New October record",
            advanceAmount = 0.0,
            wage = 600.0,
            currencyCode = "INR",
            createdAt = System.currentTimeMillis()
        )

        val secondPayload = migrated1.copy(attendance = listOf(editedAtt, newAtt))

        // 4. Round trip second payload
        val json = Gson().toJson(secondPayload)
        val restoredSecond = Gson().fromJson(json, CanonicalBackupPayload::class.java)

        assertEquals(2, restoredSecond.attendance.size)
        val restoredEdited = restoredSecond.attendance.first { it.id == "personal_2026-08-01" }
        assertEquals("Edited note", restoredEdited.note)
        assertEquals(600.0, restoredEdited.wage, 0.01)

        val restoredNew = restoredSecond.attendance.first { it.id == "personal_2026-10-01" }
        assertEquals("New October record", restoredNew.note)
    }

    @Test
    fun testCorruptAndFutureVersionBackupSafety() {
        val manifestFile = File(workingDir, "manifest.json")
        val manifestObj = ManifestManager.generateManifest(
            backupUuid = "test-future-uuid",
            creationTimestamp = 12345L,
            totalBackupDurationMs = 200L,
            compressionRatio = 0.5,
            appBuildNumber = 999L,
            backupFormatVersion = 99, // Unknown future version!
            backupVersion = 99,
            appVersion = "99.0.0",
            databaseVersion = 99,
            androidVersion = 36,
            deviceModel = "Future Phone",
            packageName = "com.synfusion.attendance",
            recordCounts = emptyMap(),
            totalFiles = 1,
            backupSize = 100L,
            featureList = emptyList()
        )
        ManifestManager.writeManifest(manifestFile, manifestObj)
        File(workingDir, "data.json").writeText("{}")

        val result = BackupValidator.validateBackupDirDetailed(workingDir, logger)
        assertFalse(result.isValid)
        assertEquals(BackupValidationCode.UNSUPPORTED_VERSION, result.code)
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
            databaseVersion = 3,
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
        assertEquals(3, readObj?.databaseVersion)
        assertEquals("Pixel Test", readObj?.deviceModel)
        assertEquals(42, readObj?.recordCounts?.get("attendance"))
    }

    @Test
    fun testProgressTracker() {
        val tracker = ProgressTracker()

        tracker.updateProgress(50f, "Halfway done")
        assertEquals(50f, tracker.progress.value)
        assertEquals("Halfway done", tracker.status.value)

        tracker.cancel()
        try {
            tracker.checkCancelled()
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("Operation was cancelled by user.", e.message)
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
