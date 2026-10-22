package com.synfusion.attendance.notifications.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.synfusion.attendance.R
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationEvent
import com.synfusion.attendance.notifications.registry.NotificationTemplateRegistry
import com.synfusion.attendance.ui.MainActivity

class LocalNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_GENERAL = "wd_channel_general"
        const val CHANNEL_REMINDERS = "wd_channel_reminders"
        const val CHANNEL_REWARDS = "wd_channel_rewards"
        const val CHANNEL_LIVE = "wd_channel_live"
        const val CHANNEL_TEST = "wd_channel_test"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "WorkDaily Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "General updates and system alerts" }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Work & Attendance Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Daily attendance, salary, and backup alerts" }

            val rewardsChannel = NotificationChannel(
                CHANNEL_REWARDS,
                "Rewards & Spin Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily check-in and Builder's Spin reminders" }

            val liveChannel = NotificationChannel(
                CHANNEL_LIVE,
                "Live Streams & Social",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Live broadcast and chat notifications" }

            val testChannel = NotificationChannel(
                CHANNEL_TEST,
                "Notification Studio Test",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Local testing channel for Notification Studio" }

            manager.createNotificationChannels(
                listOf(generalChannel, reminderChannel, rewardsChannel, liveChannel, testChannel)
            )
        }
    }

    fun sendSystemNotification(event: NotificationEvent) {
        val channelId = if (event.isDemo) {
            CHANNEL_TEST
        } else {
            when (event.category) {
                NotificationCategory.ATTENDANCE, NotificationCategory.SALARY, NotificationCategory.PASSBOOK, NotificationCategory.BACKUP -> CHANNEL_REMINDERS
                NotificationCategory.CHECK_IN, NotificationCategory.SPIN, NotificationCategory.STREAK -> CHANNEL_REWARDS
                NotificationCategory.LIVE, NotificationCategory.CHAT, NotificationCategory.SOCIAL, NotificationCategory.GIFTS -> CHANNEL_LIVE
                else -> CHANNEL_GENERAL
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link", event.deepLink)
            putExtra("notification_id", event.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val style = NotificationTemplateRegistry.getVisualStyleForCategory(event.category)
        val bannerBitmap = generateBannerBitmap(event.title, event.body, style.categoryLabel, style.primaryColorHex)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo_transparent)
            .setContentTitle(event.title)
            .setContentText(event.body)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .setSummaryText("${style.categoryLabel} • WorkDaily")
            )
            .setPriority(if (event.priority > 0) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(event.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Permission denied on Android 13+
        }
    }

    private fun generateBannerBitmap(
        title: String,
        body: String,
        category: String,
        colorHex: Long
    ): Bitmap {
        val width = 600
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Gradient Surface
        val bgPaint = Paint().apply {
            color = (colorHex or 0xFF000000).toInt()
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Dark Overlay Card
        val cardPaint = Paint().apply {
            color = Color.parseColor("#1A1A2E")
            isAntiAlias = true
        }
        canvas.drawRoundRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), 24f, 24f, cardPaint)

        // Text rendering
        val categoryPaint = Paint().apply {
            color = (colorHex or 0xFF000000).toInt()
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(category.uppercase(), 50f, 75f, categoryPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val displayTitle = if (title.length > 32) title.take(30) + "..." else title
        canvas.drawText(displayTitle, 50f, 130f, titlePaint)

        val bodyPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 20f
            isAntiAlias = true
        }
        val displayBody = if (body.length > 45) body.take(43) + "..." else body
        canvas.drawText(displayBody, 50f, 185f, bodyPaint)

        val brandPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("WORKDAILY • SMART NOTIFICATIONS", 50f, 245f, brandPaint)

        return bitmap
    }
}
