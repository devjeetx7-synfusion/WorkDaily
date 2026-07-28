package com.synfusion.attendance.data.daos

import androidx.room.*
import com.synfusion.attendance.data.entities.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE workerId = :workerId")
    fun getWorkerAttendanceFlow(workerId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL")
    fun getPersonalAttendanceFlow(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL")
    suspend fun getPersonalAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId")
    suspend fun getWorkerAttendance(workerId: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE id = :id")
    suspend fun getAttendanceById(id: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE date = :date")
    suspend fun getAttendanceByDate(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDateFlow(date: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: String)

    @Query("DELETE FROM attendance WHERE workerId = :workerId")
    suspend fun deleteAttendanceByWorkerId(workerId: String)

    @Query("DELETE FROM attendance")
    suspend fun clearAll()
}
