package com.synfusion.attendance.data.daos

import androidx.room.*
import com.synfusion.attendance.data.entities.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE workerId IS NULL")
    fun getPersonalAttendanceFlow(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL")
    suspend fun getPersonalAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE id = :id")
    suspend fun getAttendanceById(id: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE date = :date")
    suspend fun getAttendanceByDate(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDateFlow(date: String): Flow<List<AttendanceEntity>>

    // DrillDown Queries
    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND status = 'present' AND date LIKE :monthPrefix || '%' ORDER BY date DESC")
    suspend fun getPresentByMonth(monthPrefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND status = 'absent' AND date LIKE :monthPrefix || '%' ORDER BY date DESC")
    suspend fun getAbsentByMonth(monthPrefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND status = 'present' AND type = 'half' AND date LIKE :monthPrefix || '%' ORDER BY date DESC")
    suspend fun getHalfDayByMonth(monthPrefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND overtimeHours > 0 AND date LIKE :monthPrefix || '%' ORDER BY date DESC")
    suspend fun getOvertimeByMonth(monthPrefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND advanceAmount > 0 AND date LIKE :monthPrefix || '%' ORDER BY date DESC")
    suspend fun getAdvanceByMonth(monthPrefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND status = 'present' ORDER BY date DESC")
    suspend fun getAllTimePresent(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND status = 'absent' ORDER BY date DESC")
    suspend fun getAllTimeAbsent(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND status = 'present' AND type = 'half' ORDER BY date DESC")
    suspend fun getAllTimeHalfDay(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND overtimeHours > 0 ORDER BY date DESC")
    suspend fun getAllTimeOvertime(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND advanceAmount > 0 ORDER BY date DESC")
    suspend fun getAllTimeAdvance(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND (status = 'present' OR overtimeHours > 0) AND date LIKE :monthPrefix || '%' ORDER BY date DESC")
    suspend fun getEarningsByMonth(monthPrefix: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId IS NULL AND (status = 'present' OR overtimeHours > 0) ORDER BY date DESC")
    suspend fun getAllTimeEarnings(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: String)

    @Query("DELETE FROM attendance")
    suspend fun clearAll()
}
