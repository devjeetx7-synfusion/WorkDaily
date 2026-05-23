package com.dailywork.attedance.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailywork.attedance.data.local.AppDatabase
import com.dailywork.attedance.data.FirestoreRepository
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class PendingActionPayload(
    val workerId: String?,
    val attendanceId: String?,
    val date: String?,
    val data: Map<String, Any?>?
)

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val syncDao = database.syncActionDao()
        val firestoreRepository = FirestoreRepository()

        val unsyncedActions = syncDao.getUnsyncedActions()

        if (unsyncedActions.isEmpty()) {
            return Result.success()
        }

        var allSuccess = true

        for (action in unsyncedActions) {
            try {
                val gson = Gson()
                val payload: PendingActionPayload = gson.fromJson(action.payload, PendingActionPayload::class.java)

                when (action.actionType) {
                    "saveWorker" -> {
                        val workerId = payload.workerId
                        val dataMap = payload.data as? Map<String, Any>
                        if (dataMap != null) {
                            val correctedMap = dataMap.toMutableMap()
                            if (correctedMap["wage"] is Double) correctedMap["wage"] = (correctedMap["wage"] as Double)
                            firestoreRepository.saveWorker(workerId, correctedMap, true)
                        } else {
                            throw IllegalArgumentException("Missing data for saveWorker")
                        }
                    }
                    "deleteWorker" -> {
                        val workerId = payload.workerId
                        if (workerId != null) {
                            firestoreRepository.deleteWorker(workerId, true)
                        } else {
                            throw IllegalArgumentException("Missing workerId for deleteWorker")
                        }
                    }
                    "markWorkerAttendance" -> {
                        val workerId = payload.workerId
                        val attendanceId = payload.attendanceId
                        val dataMap = payload.data
                        if (workerId != null && attendanceId != null && dataMap != null) {
                            val correctedMap = dataMap.toMutableMap()
                            if (correctedMap["overtime_hours"] is Double) correctedMap["overtime_hours"] = (correctedMap["overtime_hours"] as Double).toInt()
                            if (correctedMap["advance_amount"] is Double) correctedMap["advance_amount"] = (correctedMap["advance_amount"] as Double)
                            firestoreRepository.markWorkerAttendance(workerId, attendanceId, correctedMap, true)
                        } else {
                            throw IllegalArgumentException("Missing fields for markWorkerAttendance")
                        }
                    }
                    "markPersonalAttendance" -> {
                        val attendanceId = payload.attendanceId
                        val dataMap = payload.data
                        if (attendanceId != null && dataMap != null) {
                            val correctedMap = dataMap.toMutableMap()
                            if (correctedMap["overtime_hours"] is Double) correctedMap["overtime_hours"] = (correctedMap["overtime_hours"] as Double).toInt()
                            if (correctedMap["advance_amount"] is Double) correctedMap["advance_amount"] = (correctedMap["advance_amount"] as Double)
                            firestoreRepository.markPersonalAttendance(attendanceId, correctedMap, true)
                        } else {
                            throw IllegalArgumentException("Missing fields for markPersonalAttendance")
                        }
                    }
                    "deletePersonalAttendance" -> {
                        val attendanceId = payload.attendanceId
                        val date = payload.date
                        if (attendanceId != null && date != null) {
                            firestoreRepository.deletePersonalAttendance(attendanceId, date, true)
                        } else {
                            throw IllegalArgumentException("Missing fields for deletePersonalAttendance")
                        }
                    }
                    "deleteWorkerAttendance" -> {
                        val workerId = payload.workerId
                        val attendanceId = payload.attendanceId
                        val date = payload.date
                        if (workerId != null && attendanceId != null && date != null) {
                            firestoreRepository.deleteWorkerAttendance(workerId, attendanceId, date, true)
                        } else {
                            throw IllegalArgumentException("Missing fields for deleteWorkerAttendance")
                        }
                    }
                }
                // Mark as synced if no exception
                val updatedAction = action.copy(isSynced = true)
                syncDao.update(updatedAction)
                // Or we can just delete it once done
                syncDao.deleteById(action.id)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                syncDao.deleteById(action.id) // Permanent failure, remove poison pill
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                syncDao.deleteById(action.id) // Malformed JSON, remove poison pill
            } catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }
}
