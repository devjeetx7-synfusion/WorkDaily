package com.synfusion.attendance.backup

import android.content.Context
import androidx.room.Room
import com.synfusion.attendance.data.AttendanceDatabase
import java.io.File

object TempDatabaseManager {

    fun createTempDatabase(context: Context): AttendanceDatabase {
        val tempDbFile = File(context.cacheDir, "temp_restore_db")
        if (tempDbFile.exists()) {
            tempDbFile.delete()
        }
        File(tempDbFile.absolutePath + "-wal").delete()
        File(tempDbFile.absolutePath + "-shm").delete()

        return Room.databaseBuilder(
            context.applicationContext,
            AttendanceDatabase::class.java,
            tempDbFile.absolutePath
        )
        .allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .build()
    }

    fun swapToProduction(context: Context, tempDb: AttendanceDatabase) {
        tempDb.close()

        val prodDbFile = context.getDatabasePath("synfusion_offline_attendance_db")
        val prodWalFile = File(prodDbFile.absolutePath + "-wal")
        val prodShmFile = File(prodDbFile.absolutePath + "-shm")

        val tempDbFile = File(context.cacheDir, "temp_restore_db")
        val tempWalFile = File(tempDbFile.absolutePath + "-wal")
        val tempShmFile = File(tempDbFile.absolutePath + "-shm")

        // Create backup workspace for secure rollback
        val backupDir = File(context.cacheDir, "db_rollback_workspace").apply { mkdirs() }
        val backupProdDb = File(backupDir, "prod_db_backup")
        val backupProdWal = File(backupDir, "prod_db_backup-wal")
        val backupProdShm = File(backupDir, "prod_db_backup-shm")

        backupProdDb.delete()
        backupProdWal.delete()
        backupProdShm.delete()

        try {
            // Close active production instance and reset reference
            AttendanceDatabase.resetInstance()

            // 1. Back up current production files
            if (prodDbFile.exists()) {
                if (!prodDbFile.renameTo(backupProdDb)) {
                    throw Exception("Failed to backup main production database file.")
                }
            }
            if (prodWalFile.exists()) {
                prodWalFile.renameTo(backupProdWal)
            }
            if (prodShmFile.exists()) {
                prodShmFile.renameTo(backupProdShm)
            }

            // 2. Move temp files to production
            if (tempDbFile.exists()) {
                if (!tempDbFile.renameTo(prodDbFile)) {
                    throw Exception("Failed to swap temp database into production.")
                }
            }
            if (tempWalFile.exists()) {
                tempWalFile.renameTo(prodWalFile)
            }
            if (tempShmFile.exists()) {
                tempShmFile.renameTo(prodShmFile)
            }

            // Cleanup secure backups on successful transaction
            backupProdDb.delete()
            backupProdWal.delete()
            backupProdShm.delete()
            backupDir.deleteRecursively()

        } catch (e: Exception) {
            // Transaction Rollback logic: restore pristine production files
            if (backupProdDb.exists()) {
                prodDbFile.delete()
                backupProdDb.renameTo(prodDbFile)
            }
            if (backupProdWal.exists()) {
                prodWalFile.delete()
                backupProdWal.renameTo(prodWalFile)
            }
            if (backupProdShm.exists()) {
                prodShmFile.delete()
                backupProdShm.renameTo(prodShmFile)
            }
            throw e
        } finally {
            // Ensure temp leftovers are wiped
            tempDbFile.delete()
            tempWalFile.delete()
            tempShmFile.delete()
        }
    }
}
