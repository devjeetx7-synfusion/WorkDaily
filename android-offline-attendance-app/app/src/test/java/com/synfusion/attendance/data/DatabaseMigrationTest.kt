package com.synfusion.attendance.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.JsonParser
import com.synfusion.attendance.backup.BackupVersionConstants
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    private fun createFakeDatabase(executedSql: MutableList<String>): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase
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

    @Test
    fun testBackupVersionConstantsMatchesCurrentDatabaseVersion() {
        assertEquals(5, BackupVersionConstants.CURRENT_DATABASE_VERSION)
    }

    @Test
    fun testSchemaJsonV5ContainsDateOfBirthAndIdentityHash() {
        val schemaFile = File("schemas/com.synfusion.attendance.data.AttendanceDatabase/5.json")
        val jsonFile = if (schemaFile.exists()) schemaFile else File("app/schemas/com.synfusion.attendance.data.AttendanceDatabase/5.json")
        assertTrue("Schema file 5.json must exist", jsonFile.exists())

        val root = JsonParser.parseString(jsonFile.readText()).asJsonObject
        val databaseObj = root.getAsJsonObject("database")
        assertEquals(5, databaseObj.get("version").asInt)
        assertEquals("48d5cc40621697a7417a9fa65a3b1c43", databaseObj.get("identityHash").asString)

        val entities = databaseObj.getAsJsonArray("entities")
        val usersEntity = entities.map { it.asJsonObject }.first { it.get("tableName").asString == "users" }
        val fields = usersEntity.getAsJsonArray("fields").map { it.asJsonObject.get("columnName").asString }

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
}
