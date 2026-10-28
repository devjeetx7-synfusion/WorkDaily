package com.synfusion.attendance.data

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.JsonParser
import com.synfusion.attendance.backup.BackupVersionConstants
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

/**
 * Verifies the SQL each [androidx.room.migration.Migration] emits, and that the exported schema
 * files agree with [BackupVersionConstants].
 *
 * The migrations guard every `ADD COLUMN` behind a `PRAGMA table_info` lookup (see
 * `addColumnIfMissing` in [AttendanceDatabase]), so the fake database here answers `query` with a
 * [Cursor] over a caller-supplied column list. That lets a test drive both branches: columns
 * absent (the ALTER must be emitted) and columns already present (it must be skipped).
 */
class DatabaseMigrationTest {

    /**
     * @param existingColumns what `PRAGMA table_info(users)` should report. Empty means the table
     *   has none of the columns a migration wants to add, so every ALTER fires.
     */
    private fun createFakeDatabase(
        executedSql: MutableList<String>,
        existingColumns: List<String> = emptyList()
    ): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.isNotEmpty()) {
                        executedSql.add(args[0] as String)
                    }
                    null
                }
                // The addColumnIfMissing guard inspects the live schema before altering it.
                "query" -> createFakeCursor(existingColumns)
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                "toString" -> "FakeSupportSQLiteDatabase"
                else -> null
            }
        } as SupportSQLiteDatabase
    }

    /** A [Cursor] over `PRAGMA table_info`, whose `name` column yields [columns]. */
    private fun createFakeCursor(columns: List<String>): Cursor {
        val position = intArrayOf(-1)
        return Proxy.newProxyInstance(
            Cursor::class.java.classLoader,
            arrayOf(Cursor::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "getColumnIndex" -> if (args?.getOrNull(0) == "name") 0 else -1
                "moveToNext" -> {
                    position[0]++
                    position[0] < columns.size
                }
                "getString" -> columns.getOrElse(position[0]) { "" }
                "close" -> null
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                "toString" -> "FakeCursor"
                else -> null
            }
        } as Cursor
    }

    @Test
    fun testMigration1To2ExecutesCorrectSql() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(executedSql)
        AttendanceDatabase.MIGRATION_1_2.migrate(fakeDb)
        assertEquals(1, executedSql.size)
        assertTrue(executedSql[0].contains("DROP TABLE IF EXISTS workers"))
    }

    @Test
    fun testMigration2To3ExecutesCorrectSql() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(executedSql)
        AttendanceDatabase.MIGRATION_2_3.migrate(fakeDb)
        assertEquals(3, executedSql.size)
        assertTrue(executedSql[0].contains("ALTER TABLE users ADD COLUMN countryCode"))
        assertTrue(executedSql[1].contains("ALTER TABLE users ADD COLUMN currencyCode"))
        assertTrue(executedSql[2].contains("ALTER TABLE attendance ADD COLUMN currencyCode"))
    }

    @Test
    fun testMigration3To4ExecutesCorrectSql() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(executedSql)
        AttendanceDatabase.MIGRATION_3_4.migrate(fakeDb)
        assertEquals(1, executedSql.size)
        assertTrue(executedSql[0].contains("CREATE TABLE IF NOT EXISTS `notifications`"))
    }

    @Test
    fun testMigration4To5ExecutesCorrectSql() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(executedSql)
        AttendanceDatabase.MIGRATION_4_5.migrate(fakeDb)
        assertEquals(4, executedSql.size)
        assertTrue(executedSql[0].contains("ALTER TABLE users ADD COLUMN gender"))
        assertTrue(executedSql[1].contains("ALTER TABLE users ADD COLUMN profession"))
        assertTrue(executedSql[2].contains("ALTER TABLE users ADD COLUMN bio"))
        assertTrue(executedSql[3].contains("ALTER TABLE users ADD COLUMN dateOfBirth"))
    }

    /**
     * A device created fresh at v4 by `onCreate` already has these columns -- the exported 4.json
     * declares them. Without the guard this migration aborts with "duplicate column name" and the
     * upgrade fails, so the guard must suppress every ALTER here.
     */
    @Test
    fun testMigration4To5IsIdempotentWhenColumnsAlreadyExist() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(
            executedSql,
            existingColumns = listOf("gender", "profession", "bio", "dateOfBirth")
        )
        AttendanceDatabase.MIGRATION_4_5.migrate(fakeDb)
        assertTrue("no ALTER should be emitted, but got: $executedSql", executedSql.isEmpty())
    }

    /** A partially upgraded device must get only the columns it is still missing. */
    @Test
    fun testMigration4To5AddsOnlyMissingColumns() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(executedSql, existingColumns = listOf("gender", "bio"))
        AttendanceDatabase.MIGRATION_4_5.migrate(fakeDb)
        assertEquals(2, executedSql.size)
        assertTrue(executedSql[0].contains("ADD COLUMN profession"))
        assertTrue(executedSql[1].contains("ADD COLUMN dateOfBirth"))
    }

    /**
     * The migration that actually fixes the reported crash: it is what runs for a device already
     * sitting on version 5, which is the population whose stored identity hash no longer matches
     * the compiled schema.
     */
    @Test
    fun testMigration5To6ExecutesCorrectSql() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(executedSql)
        AttendanceDatabase.MIGRATION_5_6.migrate(fakeDb)
        assertEquals(1, executedSql.size)
        assertTrue(executedSql[0].contains("ALTER TABLE users ADD COLUMN dateOfBirth"))
        // Nullable Long? with no default: adding "DEFAULT NULL" would record a dflt_value of
        // 'NULL' and make a migrated column differ from one created by Room's onCreate.
        assertFalse(executedSql[0].contains("NOT NULL"))
        assertFalse(executedSql[0].contains("DEFAULT"))
    }

    /**
     * A device that installed the later v5 build already has `dateOfBirth`. It still runs this
     * migration when moving to v6, so the guard must make it a no-op rather than abort.
     */
    @Test
    fun testMigration5To6IsIdempotentWhenDateOfBirthExists() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeDatabase(executedSql, existingColumns = listOf("dateOfBirth"))
        AttendanceDatabase.MIGRATION_5_6.migrate(fakeDb)
        assertTrue("no ALTER should be emitted, but got: $executedSql", executedSql.isEmpty())
    }

    @Test
    fun testBackupVersionConstantsMatchesCurrentDatabaseVersion() {
        assertEquals(6, BackupVersionConstants.CURRENT_DATABASE_VERSION)
    }

    private fun readSchema(version: Int) = File(
        "schemas/com.synfusion.attendance.data.AttendanceDatabase/$version.json"
    ).let { if (it.exists()) it else File("app/schemas/com.synfusion.attendance.data.AttendanceDatabase/$version.json") }

    private fun userColumns(schemaVersion: Int): List<String> {
        val jsonFile = readSchema(schemaVersion)
        assertTrue("Schema file $schemaVersion.json must exist", jsonFile.exists())

        val root = JsonParser.parseString(jsonFile.readText()).asJsonObject
        val databaseObj = root.getAsJsonObject("database")
        assertEquals(schemaVersion, databaseObj.get("version").asInt)

        val entities = databaseObj.getAsJsonArray("entities")
        val usersEntity = entities.map { it.asJsonObject }.first { it.get("tableName").asString == "users" }
        return usersEntity.getAsJsonArray("fields").map { it.asJsonObject.get("columnName").asString }
    }

    @Test
    fun testSchemaJsonV5ContainsDateOfBirthAndIdentityHash() {
        val jsonFile = readSchema(5)
        assertTrue("Schema file 5.json must exist", jsonFile.exists())

        val root = JsonParser.parseString(jsonFile.readText()).asJsonObject
        val databaseObj = root.getAsJsonObject("database")
        assertEquals(5, databaseObj.get("version").asInt)
        assertEquals("48d5cc40621697a7417a9fa65a3b1c43", databaseObj.get("identityHash").asString)

        val fields = userColumns(5)
        assertTrue(fields.contains("id"))
        assertTrue(fields.contains("name"))
        assertTrue(fields.contains("phoneNumber"))
        assertTrue(fields.contains("email"))
        assertTrue(fields.contains("role"))
        assertTrue(fields.contains("dailyWage"))
        assertTrue(fields.contains("countryCode"))
        assertTrue(fields.contains("currencyCode"))
        assertTrue(fields.contains("gender"))
        assertTrue(fields.contains("profession"))
        assertTrue(fields.contains("bio"))
        assertTrue(fields.contains("dateOfBirth"))
        assertTrue(fields.contains("createdAt"))
    }

    /**
     * v6 is a version bump with no entity change: it exists purely to give devices already on v5 a
     * migration path. Room's identity hash is derived from the schema structure and not the version
     * number, so v6 must carry v5's hash and column set exactly.
     */
    @Test
    fun testSchemaJsonV6IsStructurallyIdenticalToV5() {
        val v6 = readSchema(6)
        assertTrue("Schema file 6.json must exist", v6.exists())

        val databaseObj = JsonParser.parseString(v6.readText()).asJsonObject.getAsJsonObject("database")
        assertEquals(6, databaseObj.get("version").asInt)
        assertEquals(
            "v6 changes no entity, so its identity hash must equal v5's",
            "48d5cc40621697a7417a9fa65a3b1c43",
            databaseObj.get("identityHash").asString
        )
        assertEquals(userColumns(5), userColumns(6))
    }
}
