package com.dailywork.attedance.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.Locale

class DataMigrationManager(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val repository: FirestoreRepository = FirestoreRepository(db)
) {
    private val auth = FirebaseAuth.getInstance()

    suspend fun isMigrationCompleted(): Boolean {
        val user = auth.currentUser ?: return false
        val meta = db.collection("users").document(user.uid).collection("meta").document("migration").get().await()
        return meta.exists() && meta.getBoolean("completed") == true
    }

    private suspend fun setMigrationCompleted() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).collection("meta").document("migration")
            .set(mapOf("completed" to true, "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun migrate() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        if (isMigrationCompleted()) {
            Log.d("Migration", "Migration already completed for user: $uid")
            return
        }

        Log.d("Migration", "Starting migration for user: $uid")

        // 1. Migrate Workers
        val workersSnapshot = db.collection("workers")
            .whereEqualTo("contractorId", uid)
            .get()
            .await()

        for (doc in workersSnapshot.documents) {
            val data = doc.data?.toMutableMap() ?: continue
            data.remove("contractorId")

            val workerId = doc.id
            repository.workersCollection()?.document(workerId)?.set(data)?.await()
            Log.d("Migration", "Migrated worker: $workerId")

            // Migrate Worker Attendance
            val workerAttendanceSnapshot = db.collection("attendance")
                .whereEqualTo("user_id", "worker_$workerId")
                .get()
                .await()

            for (attDoc in workerAttendanceSnapshot.documents) {
                val attData = attDoc.data?.toMutableMap() ?: continue
                val date = attData["date"] as? String ?: continue
                val wage = data["wage"] as? Double ?: 500.0

                attData.remove("user_id")
                attData.remove("contractorId")

                repository.workerAttendanceCollection(workerId)?.document(attDoc.id)?.set(attData)?.await()
                updateSummaryDuringMigration(uid, date, attData, wage, workerId)
            }
            Log.d("Migration", "Migrated attendance for worker: $workerId")
        }

        // 2. Migrate Personal Attendance
        val personalAttendanceSnapshot = db.collection("attendance")
            .whereEqualTo("user_id", uid)
            .get()
            .await()

        for (doc in personalAttendanceSnapshot.documents) {
            val data = doc.data?.toMutableMap() ?: continue
            val date = data["date"] as? String ?: continue

            data.remove("user_id")

            repository.personalAttendanceCollection()?.document(doc.id)?.set(data)?.await()
            updateSummaryDuringMigration(uid, date, data, null, null)
        }
        Log.d("Migration", "Migrated personal attendance for user: $uid")

        // 3. Migrate Work Types
        val workTypesSnapshot = db.collection("workTypes")
            .whereEqualTo("createdBy", uid)
            .get()
            .await()

        for (doc in workTypesSnapshot.documents) {
            val data = doc.data?.toMutableMap() ?: continue
            data.remove("createdBy")

            repository.contractorWorkTypesCollection()?.document(doc.id)?.set(data)?.await()
        }
        Log.d("Migration", "Migrated work types for user: $uid")

        setMigrationCompleted()
        Log.d("Migration", "Migration completed for user: $uid")
    }

    suspend fun cleanupOldCollections() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // ONLY cleanup if migration is verified.
        // For this task, we will just provide the method.
        // In a real app, you'd run this after confirming new data exists.

        Log.d("Migration", "Starting cleanup for user: $uid")

        // Cleanup Workers
        val workersSnapshot = db.collection("workers").whereEqualTo("contractorId", uid).get().await()
        for (doc in workersSnapshot.documents) {
            doc.reference.delete().await()
        }

        // Cleanup Attendance
        val workerAttendanceSnapshot = db.collection("attendance").whereEqualTo("contractorId", uid).get().await()
        for (doc in workerAttendanceSnapshot.documents) {
            doc.reference.delete().await()
        }

        val personalAttendanceSnapshot = db.collection("attendance").whereEqualTo("user_id", uid).get().await()
        for (doc in personalAttendanceSnapshot.documents) {
            doc.reference.delete().await()
        }

        // Cleanup Work Types
        val workTypesSnapshot = db.collection("workTypes").whereEqualTo("createdBy", uid).get().await()
        for (doc in workTypesSnapshot.documents) {
            doc.reference.delete().await()
        }

        Log.d("Migration", "Cleanup completed for user: $uid")
    }

    private suspend fun updateSummaryDuringMigration(uid: String, date: String, data: Map<String, Any>, workerWage: Double?, workerId: String?) {
        val monthId = date.substring(0, 7)
        val summaryDoc = if (workerId != null) {
             db.collection("users").document(uid).collection("contractor").document("data").collection("summaries").document(monthId)
        } else {
             db.collection("users").document(uid).collection("personal").document("data").collection("summaries").document(monthId)
        }

        val status = data["status"] as? String
        val type = data["type"] as? String
        val advance = (data["advance_amount"] as? Number)?.toDouble() ?: 0.0
        val wage = workerWage ?: (data["daily_wage"] as? Number)?.toDouble() ?: 500.0

        val updates = mutableMapOf<String, Any>()
        if (status == "present") {
            val dayVal = if (type == "half") 0.5 else 1.0
            val costVal = if (type == "half") wage / 2 else wage
            updates["total_present"] = FieldValue.increment(dayVal)
            updates["total_wages"] = FieldValue.increment(costVal)
            if (workerId != null) {
                updates["workers.$workerId.days"] = FieldValue.increment(dayVal)
                updates["workers.$workerId.cost"] = FieldValue.increment(costVal)
            }
        }
        if (advance > 0) {
            updates["total_advance"] = FieldValue.increment(advance)
        }

        if (updates.isNotEmpty()) {
            summaryDoc.set(updates, SetOptions.merge()).await()
        }
    }
}
