package com.synfusion.attendance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.synfusion.attendance.data.daos.AttendanceDao
import com.synfusion.attendance.data.daos.SummaryDao
import com.synfusion.attendance.data.daos.UserDao
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AttendanceEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AttendanceDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getInstance(context: android.content.Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "synfusion_offline_attendance_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
