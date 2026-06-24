package com.dailywork.admin.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FCMRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun sendNotification(target: String, title: String, message: String) {
        // TODO: In a real production app, this would call a Cloud Function or FCM HTTP v1 API.
        // For this project, we implement a placeholder as per "fire-and-forget" requirement.
        Log.d("FCMRepository", "Sending notification to $target: $title - $message")

        val notificationLog = mapOf(
            "target" to target,
            "title" to title,
            "message" to message,
            "sentAt" to System.currentTimeMillis(),
            "sentBy" to (auth.currentUser?.uid ?: "unknown")
        )

        try {
            db.collection("admin_notifications").add(notificationLog).await()

            // Also log as an admin action
            val adminLog = mapOf(
                "action" to "SEND_NOTIFICATION",
                "targetId" to target,
                "timestamp" to System.currentTimeMillis(),
                "details" to "Title: $title",
                "adminId" to (auth.currentUser?.uid ?: "unknown")
            )
            db.collection("admin_logs").add(adminLog).await()
        } catch (e: Exception) {
            Log.e("FCMRepository", "Failed to log notification", e)
        }
    }
}
