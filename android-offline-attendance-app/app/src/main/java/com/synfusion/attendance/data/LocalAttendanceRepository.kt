package com.synfusion.attendance.data

import android.content.Context
import com.synfusion.attendance.data.daos.AttendanceDao
import com.synfusion.attendance.data.daos.SummaryDao
import com.synfusion.attendance.data.daos.UserDao
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalAttendanceRepository(
    private val context: Context
) {
    private val db: AttendanceDatabase
        get() = AttendanceDatabase.getInstance(context)

    private val userDao: UserDao
        get() = db.userDao()

    private val attendanceDao: AttendanceDao
        get() = db.attendanceDao()

    private val summaryDao: SummaryDao
        get() = db.summaryDao()

    // --- User Profile ---
    fun getUserFlow(): Flow<UserEntity?> = userDao.getUserFlow()
    suspend fun getUser(): UserEntity? = userDao.getUser()
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun clearSession() {
        // Intentionally no-op to preserve user attendance, profile, and summary records during session reset.
    }

    // --- Attendance Operations ---
    fun getPersonalAttendanceFlow(): Flow<List<AttendanceEntity>> =
        attendanceDao.getPersonalAttendanceFlow()

    suspend fun getPersonalAttendance(): List<AttendanceEntity> =
        attendanceDao.getPersonalAttendance()

    fun getAttendanceByDateFlow(date: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceByDateFlow(date)

    suspend fun getAttendanceByDate(date: String): List<AttendanceEntity> =
        attendanceDao.getAttendanceByDate(date)

    suspend fun saveAttendance(attendance: AttendanceEntity) {
        val oldAttendance = attendanceDao.getAttendanceById(attendance.id)
        attendanceDao.insertAttendance(attendance)
        updateSummary(attendance.date, oldAttendance, attendance)
    }

    suspend fun deleteAttendance(attendanceId: String, date: String) {
        val oldAttendance = attendanceDao.getAttendanceById(attendanceId)
        if (oldAttendance != null) {
            attendanceDao.deleteAttendanceById(attendanceId)
            updateSummary(date, oldAttendance, null)
        }
    }

    // --- Summary Management ---
    fun getSummaryFlow(monthId: String, role: String): Flow<SummaryEntity?> {
        val id = "${role}_$monthId"
        return summaryDao.getSummaryByIdFlow(id)
    }

    private suspend fun updateSummary(
        date: String,
        oldData: AttendanceEntity?,
        newData: AttendanceEntity?
    ) {
        val monthId = date.substring(0, 7) // YYYY-MM
        val role = "personal"
        val summaryId = "${role}_$monthId"

        val currentSummary = summaryDao.getSummaryById(summaryId) ?: SummaryEntity(
            id = summaryId,
            monthId = monthId,
            role = role
        )

        fun getStats(data: AttendanceEntity?): Triple<Double, Double, Double> {
            if (data == null) return Triple(0.0, 0.0, 0.0)
            val isPresent = data.status == "present"
            val isHalf = data.type == "half"
            val wage = data.wage

            var dayVal = 0.0
            var costVal = 0.0
            if (isPresent) {
                dayVal = if (isHalf) 0.5 else 1.0
                costVal = if (isHalf) wage / 2.0 else wage
            }
            return Triple(dayVal, costVal, data.advanceAmount)
        }

        val (oldDay, oldCost, oldAdv) = getStats(oldData)
        val (newDay, newCost, newAdv) = getStats(newData)

        val diffDay = newDay - oldDay
        val diffCost = newCost - oldCost
        val diffAdv = newAdv - oldAdv

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var diffTodayPresent = 0.0
        var diffTodayAbsent = 0.0

        if (date == today) {
            val oldStatus = oldData?.status
            val newStatus = newData?.status
            if (oldStatus != newStatus) {
                if (oldStatus == "present") diffTodayPresent -= 1.0
                if (oldStatus == "absent") diffTodayAbsent -= 1.0

                if (newStatus == "present") diffTodayPresent += 1.0
                if (newStatus == "absent") diffTodayAbsent += 1.0
            }
        }

        val updatedSummary = currentSummary.copy(
            totalPresent = currentSummary.totalPresent + diffDay,
            totalWages = currentSummary.totalWages + diffCost,
            totalAdvance = currentSummary.totalAdvance + diffAdv,
            todayPresentCount = currentSummary.todayPresentCount + diffTodayPresent,
            todayAbsentCount = currentSummary.todayAbsentCount + diffTodayAbsent
        )

        summaryDao.insertSummary(updatedSummary)
    }
}
