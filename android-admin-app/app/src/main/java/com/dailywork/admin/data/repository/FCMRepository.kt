package com.dailywork.admin.data.repository

import android.util.Log

class FCMRepository {
    suspend fun sendNotification(target: String, title: String, message: String) {
        // In a real production app, this would call a Cloud Function or FCM HTTP v1 API.
        // For this project, we implement a placeholder as per "fire-and-forget" requirement.
        Log.d("FCMRepository", "Sending notification to $target: $title - $message")

        // Mocking successful delivery
        // If we had a backend URL, we would use Retrofit/Ktor here.
    }
}
