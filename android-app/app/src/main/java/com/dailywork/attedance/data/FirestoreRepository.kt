package com.dailywork.attedance.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import com.dailywork.attedance.data.local.SyncActionDao
import com.dailywork.attedance.data.local.SyncActionEntity
import java.util.UUID
import com.google.gson.Gson

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val syncActionDao: SyncActionDao? = null
) {

    private val auth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

    // --- Path Helpers ---

    private fun userDoc(): DocumentReference? = userId?.let { db.collection("users").document(it) }

    fun contractorDoc(): DocumentReference? = userDoc()?.collection("contractor")?.document("data")
    fun personalDoc(): DocumentReference? = userDoc()?.collection("personal")?.document("data")
    fun workersCollection(): CollectionReference? = contractorDoc()?.collection("workers")

    fun workerAttendanceCollection(workerId: String): CollectionReference? =
        workersCollection()?.document(workerId)?.collection("attendance")

    fun contractorWorkTypesCollection(): CollectionReference? = contractorDoc()?.collection("workTypes")

    fun contractorSummariesCollection(): CollectionReference? = contractorDoc()?.collection("summaries")

    // --- Personal Paths ---
    fun personalAttendanceCollection(): CollectionReference? = personalDoc()?.collection("attendance")

    fun personalSummariesCollection(): CollectionReference? = personalDoc()?.collection("summaries")

    // --- Explicit Role-Based Methods ---

    fun getContractorWorkers() = workersCollection()
    fun getContractorAttendance(workerId: String) = workerAttendanceCollection(workerId)
    fun getPersonalAttendance() = personalAttendanceCollection()

    // --- Worker Operations ---

    suspend fun saveWorker(workerId: String?, data: Map<String, Any>, fromSyncWorker: Boolean = false) {
        val workers = workersCollection() ?: return
        val finalWorkerId = workerId ?: workers.document().id

        if (!fromSyncWorker) {
            val gson = Gson()
            val payloadMap = mapOf("workerId" to finalWorkerId, "data" to data)
            val action = SyncActionEntity(
                id = UUID.randomUUID().toString(),
                actionType = "saveWorker",
                payload = gson.toJson(payloadMap),
                isSynced = false,
                createdAt = System.currentTimeMillis()
            )
            syncActionDao?.insert(action)
        }

        try {
            workers.document(finalWorkerId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteWorker(workerId: String, fromSyncWorker: Boolean = false) {
        if (!fromSyncWorker) {
            val gson = Gson()
            val payloadMap = mapOf("workerId" to workerId)
            val action = SyncActionEntity(
                id = UUID.randomUUID().toString(),
                actionType = "deleteWorker",
                payload = gson.toJson(payloadMap),
                isSynced = false,
                createdAt = System.currentTimeMillis()
            )
            syncActionDao?.insert(action)
        }

        try {
            val workerDoc = workersCollection()?.document(workerId) ?: return
            val attendance = workerAttendanceCollection(workerId)?.get()?.await()
            attendance?.documents?.forEach { doc ->
                doc.reference.delete().await()
            }
            workerDoc.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Attendance Operations ---

    suspend fun markWorkerAttendance(workerId: String, attendanceId: String, data: Map<String, Any?>, fromSyncWorker: Boolean = false) {
        if (!fromSyncWorker) {
            val gson = Gson()
            val payloadMap = mapOf("workerId" to workerId, "attendanceId" to attendanceId, "data" to data)
            val action = SyncActionEntity(
                id = UUID.randomUUID().toString(),
                actionType = "markWorkerAttendance",
                payload = gson.toJson(payloadMap),
                isSynced = false,
                createdAt = System.currentTimeMillis()
            )
            syncActionDao?.insert(action)
        }

        try {
            val collection = workerAttendanceCollection(workerId) ?: return
            val docRef = collection.document(attendanceId)
            val oldDoc = docRef.get().await()

            val date = data["date"] as String
            val oldData = if (oldDoc.exists()) oldDoc.data else null

            docRef.set(data, SetOptions.merge()).await()
            updateContractorSummary(date, oldData, data, workerId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markPersonalAttendance(attendanceId: String, data: Map<String, Any?>, fromSyncWorker: Boolean = false) {
        if (!fromSyncWorker) {
            val gson = Gson()
            val payloadMap = mapOf("attendanceId" to attendanceId, "data" to data)
            val action = SyncActionEntity(
                id = UUID.randomUUID().toString(),
                actionType = "markPersonalAttendance",
                payload = gson.toJson(payloadMap),
                isSynced = false,
                createdAt = System.currentTimeMillis()
            )
            syncActionDao?.insert(action)
        }

        try {
            val collection = personalAttendanceCollection() ?: return
            val docRef = collection.document(attendanceId)
            val oldDoc = docRef.get().await()

            val date = data["date"] as String
            val oldData = if (oldDoc.exists()) oldDoc.data else null

            docRef.set(data, SetOptions.merge()).await()
            updatePersonalSummary(date, oldData, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePersonalAttendance(attendanceId: String, date: String, fromSyncWorker: Boolean = false) {
        if (!fromSyncWorker) {
            val gson = Gson()
            val payloadMap = mapOf("attendanceId" to attendanceId, "date" to date)
            val action = SyncActionEntity(
                id = UUID.randomUUID().toString(),
                actionType = "deletePersonalAttendance",
                payload = gson.toJson(payloadMap),
                isSynced = false,
                createdAt = System.currentTimeMillis()
            )
            syncActionDao?.insert(action)
        }

        try {
            val docRef = personalAttendanceCollection()?.document(attendanceId) ?: return
            val oldDoc = docRef.get().await()
            if (oldDoc.exists()) {
                val oldData = oldDoc.data
                docRef.delete().await()
                updatePersonalSummary(date, oldData, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteWorkerAttendance(workerId: String, attendanceId: String, date: String, fromSyncWorker: Boolean = false) {
        if (!fromSyncWorker) {
            val gson = Gson()
            val payloadMap = mapOf("workerId" to workerId, "attendanceId" to attendanceId, "date" to date)
            val action = SyncActionEntity(
                id = UUID.randomUUID().toString(),
                actionType = "deleteWorkerAttendance",
                payload = gson.toJson(payloadMap),
                isSynced = false,
                createdAt = System.currentTimeMillis()
            )
            syncActionDao?.insert(action)
        }

        try {
            val docRef = workerAttendanceCollection(workerId)?.document(attendanceId) ?: return
            val oldDoc = docRef.get().await()
            if (oldDoc.exists()) {
                val oldData = oldDoc.data
                docRef.delete().await()
                updateContractorSummary(date, oldData, null, workerId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Summary Optimization ---

    private suspend fun updateContractorSummary(date: String, oldData: Map<String, Any?>?, newData: Map<String, Any?>?, workerId: String?) {
        val monthId = date.substring(0, 7) // YYYY-MM
        val summaryDoc = contractorSummariesCollection()?.document(monthId) ?: return

        val updates = mutableMapOf<String, Any>()

        // Helper to get values
        fun getStats(data: Map<String, Any?>?): Triple<Double, Double, Double> {
            if (data == null) return Triple(0.0, 0.0, 0.0)
            val status = data["status"] as? String
            val type = data["type"] as? String
            val advance = (data["advance_amount"] as? Number)?.toDouble() ?: 0.0
            val wage = (data["wage"] as? Number)?.toDouble() ?: (data["daily_wage"] as? Number)?.toDouble() ?: 500.0

            var dayVal = 0.0
            var costVal = 0.0
            if (status == "present") {
                dayVal = if (type == "half") 0.5 else 1.0
                costVal = if (type == "half") wage / 2 else wage
            }
            return Triple(dayVal, costVal, advance)
        }

        val (oldDay, oldCost, oldAdv) = getStats(oldData)
        val (newDay, newCost, newAdv) = getStats(newData)

        val diffDay = newDay - oldDay
        val diffCost = newCost - oldCost
        val diffAdv = newAdv - oldAdv

        if (diffDay != 0.0) updates["total_present"] = FieldValue.increment(diffDay)
        if (diffCost != 0.0) updates["total_wages"] = FieldValue.increment(diffCost)
        if (diffAdv != 0.0) updates["total_advance"] = FieldValue.increment(diffAdv)

        // Per-worker stats in monthly summary
        if (workerId != null && (diffDay != 0.0 || diffCost != 0.0)) {
            updates["workers.$workerId.days"] = FieldValue.increment(diffDay)
            updates["workers.$workerId.cost"] = FieldValue.increment(diffCost)
        }

        // Today's stats
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        if (date == today) {
            val oldStatus = oldData?.get("status") as? String
            val newStatus = newData?.get("status") as? String

            if (oldStatus != newStatus) {
                if (oldStatus == "present") updates["today.present_count"] = FieldValue.increment(-1.0)
                if (oldStatus == "absent") updates["today.absent_count"] = FieldValue.increment(-1.0)

                if (newStatus == "present") updates["today.present_count"] = FieldValue.increment(1.0)
                if (newStatus == "absent") updates["today.absent_count"] = FieldValue.increment(1.0)
            }
        }

        if (updates.isNotEmpty()) {
            summaryDoc.set(updates, SetOptions.merge()).await()
        }
    }

    private suspend fun updatePersonalSummary(date: String, oldData: Map<String, Any?>?, newData: Map<String, Any?>?) {
        val monthId = date.substring(0, 7) // YYYY-MM
        val summaryDoc = personalSummariesCollection()?.document(monthId) ?: return

        val updates = mutableMapOf<String, Any>()

        fun getStats(data: Map<String, Any?>?): Triple<Double, Double, Double> {
            if (data == null) return Triple(0.0, 0.0, 0.0)
            val status = data["status"] as? String
            val type = data["type"] as? String
            val advance = (data["advance_amount"] as? Number)?.toDouble() ?: 0.0
            val wage = (data["daily_wage"] as? Number)?.toDouble() ?: 500.0

            var dayVal = 0.0
            var costVal = 0.0
            if (status == "present") {
                dayVal = if (type == "half") 0.5 else 1.0
                costVal = if (type == "half") wage / 2 else wage
            }
            return Triple(dayVal, costVal, advance)
        }

        val (oldDay, oldCost, oldAdv) = getStats(oldData)
        val (newDay, newCost, newAdv) = getStats(newData)

        val diffDay = newDay - oldDay
        val diffCost = newCost - oldCost
        val diffAdv = newAdv - oldAdv

        if (diffDay != 0.0) updates["total_present"] = FieldValue.increment(diffDay)
        if (diffCost != 0.0) updates["total_wages"] = FieldValue.increment(diffCost)
        if (diffAdv != 0.0) updates["total_advance"] = FieldValue.increment(diffAdv)

        if (updates.isNotEmpty()) {
            summaryDoc.set(updates, SetOptions.merge()).await()
        }
    }
}
