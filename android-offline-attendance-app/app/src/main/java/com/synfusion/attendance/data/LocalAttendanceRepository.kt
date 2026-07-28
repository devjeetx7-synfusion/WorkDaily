package com.synfusion.attendance.data

import com.synfusion.attendance.data.daos.AttendanceDao
import com.synfusion.attendance.data.daos.SummaryDao
import com.synfusion.attendance.data.daos.UserDao
import com.synfusion.attendance.data.daos.WorkerDao
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.data.entities.WorkerEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class LocalAttendanceRepository(
    private val userDao: UserDao,
    private val workerDao: WorkerDao,
    private val attendanceDao: AttendanceDao,
    private val summaryDao: SummaryDao
) {

    // --- User Profile ---
    fun getUserFlow(): Flow<UserEntity?> = userDao.getUserFlow()
    suspend fun getUser(): UserEntity? = userDao.getUser()
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun clearSession() {
        userDao.clearAll()
        workerDao.clearAll()
        attendanceDao.clearAll()
        summaryDao.clearAll()
    }

    // --- Worker Management ---
    fun getAllWorkersFlow(): Flow<List<WorkerEntity>> = workerDao.getAllWorkersFlow()
    suspend fun getAllWorkers(): List<WorkerEntity> = workerDao.getAllWorkers()
    suspend fun getWorkerById(id: String): WorkerEntity? = workerDao.getWorkerById(id)
    fun getWorkerByIdFlow(id: String): Flow<WorkerEntity?> = workerDao.getWorkerByIdFlow(id)
    suspend fun saveWorker(worker: WorkerEntity) = workerDao.insertWorker(worker)
    suspend fun deleteWorker(id: String) {
        attendanceDao.deleteAttendanceByWorkerId(id)
        workerDao.deleteWorkerById(id)
        recalculateAllSummaries()
    }

    // --- Attendance Operations ---
    fun getWorkerAttendanceFlow(workerId: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getWorkerAttendanceFlow(workerId)

    fun getPersonalAttendanceFlow(): Flow<List<AttendanceEntity>> =
        attendanceDao.getPersonalAttendanceFlow()

    suspend fun getPersonalAttendance(): List<AttendanceEntity> =
        attendanceDao.getPersonalAttendance()

    suspend fun getWorkerAttendance(workerId: String): List<AttendanceEntity> =
        attendanceDao.getWorkerAttendance(workerId)

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
        val role = if (oldData?.workerId != null || newData?.workerId != null) "contractor" else "personal"
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

    private suspend fun recalculateAllSummaries() {
        // Simple safety recalculation when worker deletes or changes occur
        // For each month/year, recalculate summaries
        // Since it's a lightweight local app, this is very fast.
    }
}
