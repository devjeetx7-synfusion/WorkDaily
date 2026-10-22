package com.synfusion.attendance.notifications.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationEvent

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val categoryId: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val userId: String?,
    val entityId: String?,
    val deepLink: String?,
    val artworkUrl: String?,
    val artworkResId: Int?,
    val priority: Int,
    val isRead: Boolean,
    val source: String,
    val expiryTime: Long?,
    val isDemo: Boolean
) {
    fun toDomain(): NotificationEvent {
        return NotificationEvent(
            id = id,
            type = type,
            category = NotificationCategory.fromId(categoryId),
            title = title,
            body = body,
            timestamp = timestamp,
            userId = userId,
            entityId = entityId,
            deepLink = deepLink,
            artworkUrl = artworkUrl,
            artworkResId = artworkResId,
            priority = priority,
            isRead = isRead,
            source = source,
            expiryTime = expiryTime,
            isDemo = isDemo
        )
    }

    companion object {
        fun fromDomain(event: NotificationEvent): NotificationEntity {
            return NotificationEntity(
                id = event.id,
                type = event.type,
                categoryId = event.category.id,
                title = event.title,
                body = event.body,
                timestamp = event.timestamp,
                userId = event.userId,
                entityId = event.entityId,
                deepLink = event.deepLink,
                artworkUrl = event.artworkUrl,
                artworkResId = event.artworkResId,
                priority = event.priority,
                isRead = event.isRead,
                source = event.source,
                expiryTime = event.expiryTime,
                isDemo = event.isDemo
            )
        }
    }
}
