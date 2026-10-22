package com.synfusion.attendance.notifications.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.synfusion.attendance.notifications.engine.WorkDailyNotificationCoordinator
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationEvent

class WorkDailyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token refreshed - in production this registers with backend device_tokens
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "WorkDaily Update"
        val body = remoteMessage.notification?.body ?: data["body"] ?: ""
        val categoryId = data["category"] ?: "system"
        val type = data["type"] ?: "fcm_push"
        val deepLink = data["deep_link"] ?: "dashboard"
        val eventId = data["event_id"] ?: "fcm_${System.currentTimeMillis()}"

        val event = NotificationEvent(
            id = eventId,
            type = type,
            category = NotificationCategory.fromId(categoryId),
            title = title,
            body = body,
            deepLink = deepLink,
            source = "online"
        )

        val coordinator = WorkDailyNotificationCoordinator(applicationContext)
        coordinator.postNotification(event, triggerSystemAlert = true)
    }
}
