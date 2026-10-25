package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface ReportRepository {
    suspend fun submitReport(
        reporterUid: String,
        targetUid: String,
        reason: String,
        details: String
    ): ProfileOperationResult<Boolean>
}

class FirestoreReportRepository(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) : ReportRepository {

    override suspend fun submitReport(
        reporterUid: String,
        targetUid: String,
        reason: String,
        details: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (reporterUid.isBlank() || targetUid.isBlank() || reason.isBlank()) {
            return@withContext ProfileOperationResult.Error("Reporter UID, Target UID and Reason are required.")
        }

        try {
            val firestore = firestoreSupplier()
            val reportRef = firestore.collection("reports").document()
            val reportData = mapOf(
                "id" to reportRef.id,
                "reporterUid" to reporterUid,
                "targetUid" to targetUid,
                "reason" to reason.trim(),
                "details" to details.trim(),
                "timestamp" to System.currentTimeMillis()
            )

            reportRef.set(reportData).await()
            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to submit report", cause = e)
        }
    }
}
