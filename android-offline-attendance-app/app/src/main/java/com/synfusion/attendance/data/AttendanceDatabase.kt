package com.synfusion.attendance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.synfusion.attendance.data.daos.AttendanceDao
import com.synfusion.attendance.data.daos.SummaryDao
import com.synfusion.attendance.data.daos.UserDao
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.notifications.data.NotificationDao
import com.synfusion.attendance.notifications.data.NotificationEntity

@Database(
    entities = [
        UserEntity::class,
        AttendanceEntity::class,
        SummaryEntity::class,
        NotificationEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AttendanceDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun summaryDao(): SummaryDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        @Volatile
        private var isSwapping = false

        private val dbLock = Any()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS workers")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN countryCode TEXT NOT NULL DEFAULT 'IN'")
                db.execSQL("ALTER TABLE users ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'INR'")
                db.execSQL("ALTER TABLE attendance ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'INR'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notifications` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `userId` TEXT,
                        `entityId` TEXT,
                        `deepLink` TEXT,
                        `artworkUrl` TEXT,
                        `artworkResId` INTEGER,
                        `priority` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `expiryTime` INTEGER,
                        `isDemo` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Guarded: the exported 4.json already contains these three columns, while 3.json
                // does not. Depending on which build a device installed, it may reach this step
                // with the columns already present, and an unguarded ALTER TABLE would then fail
                // with "duplicate column name" and abort the whole upgrade.
                db.addColumnIfMissing("users", "gender", "TEXT NOT NULL DEFAULT 'Male'")
                db.addColumnIfMissing("users", "profession", "TEXT NOT NULL DEFAULT 'Site Worker'")
                db.addColumnIfMissing("users", "bio", "TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Adds [UserEntity.dateOfBirth].
         *
         * The column was introduced while the database was still declared as `version = 5`, so the
         * v5 schema shipped in two different shapes: builds before the change wrote identity hash
         * `9dbceadf8fdd0b6bd9ed4c0e670c8c0a`, builds after it wrote
         * `48d5cc40621697a7417a9fa65a3b1c43`. Because the version number was unchanged, Room never
         * ran an upgrade for those devices -- it went straight to `onOpen` -> `checkIdentity` and
         * threw "Room cannot verify the data integrity". Bumping to 6 gives the column a real
         * migration path so existing data is preserved instead of the app crash-looping.
         *
         * `dateOfBirth` is a nullable `Long?` with no default, so the column is added as a plain
         * nullable INTEGER. Column position is irrelevant: Room compares columns by name.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("users", "dateOfBirth", "INTEGER")
            }
        }

        fun getInstance(context: android.content.Context): AttendanceDatabase {
            synchronized(dbLock) {
                if (isSwapping) {
                    throw IllegalStateException("Cannot perform this operation because the connection pool is currently locked for restore database swap.")
                }
                return INSTANCE ?: run {
                    val instance = androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        AttendanceDatabase::class.java,
                        "synfusion_offline_attendance_db"
                    )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                }
            }
        }

        fun beginSwap() {
            synchronized(dbLock) {
                isSwapping = true
                resetInstance()
            }
        }

        fun endSwap() {
            synchronized(dbLock) {
                isSwapping = false
            }
        }

        fun resetInstance() {
            synchronized(dbLock) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    // ignore
                }
                INSTANCE = null
            }
        }
    }
}

/**
 * Adds `table.column` only when it is not already present.
 *
 * This guard is required rather than merely defensive. The exported schemas show that the
 * migration chain and the schema versions drifted apart: `3.json` has no `gender`/`profession`/
 * `bio` columns but `4.json` already does, while `MIGRATION_4_5` is the step that adds them.
 * A device therefore reaches `MIGRATION_4_5` either with those columns missing (it came up
 * through v3) or already present (it was created fresh at v4 by `onCreate`). An unguarded
 * `ALTER TABLE ... ADD COLUMN` aborts with "duplicate column name" in the second case, which
 * fails the whole upgrade and leaves the database unusable.
 *
 * Reading the live column set with `PRAGMA table_info` makes each step converge on the same
 * final schema regardless of the path a device took to get there.
 */
private fun SupportSQLiteDatabase.addColumnIfMissing(
    table: String,
    column: String,
    definition: String
) {
    if (!hasColumn(table, column)) {
        execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
    }
}

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    // Cursor implements Closeable, so `use` guarantees it is released even if reading throws.
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        if (nameIndex < 0) return false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}
