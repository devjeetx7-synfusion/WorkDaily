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
    version = 5,
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
                db.execSQL("ALTER TABLE users ADD COLUMN gender TEXT NOT NULL DEFAULT 'Male'")
                db.execSQL("ALTER TABLE users ADD COLUMN profession TEXT NOT NULL DEFAULT 'Site Worker'")
                db.execSQL("ALTER TABLE users ADD COLUMN bio TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN dateOfBirth INTEGER DEFAULT NULL")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
