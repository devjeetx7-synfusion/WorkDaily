package com.synfusion.attendance.notifications.model

import androidx.compose.ui.graphics.Color

enum class NotificationCategory(
    val id: String,
    val displayName: String,
    val description: String
) {
    LIVE("live", "Live Streams", "Broadcasting alerts & live events"),
    SPIN("spin", "Builder's Spin", "Free spin availability & reminders"),
    CHECK_IN("check_in", "Daily Check-in", "Daily coin claim reminders"),
    STREAK("streak", "Streaks & Milestones", "Streak preservation & milestone achievements"),
    ATTENDANCE("attendance", "Attendance & Work", "Daily attendance reminders & summaries"),
    SALARY("salary", "Salary & Wages", "Monthly salary updates & estimates"),
    PASSBOOK("passbook", "Passbook & Reports", "Statement ready & PDF export updates"),
    BACKUP("backup", "Backup & Utilities", "Cloud/local backup reminders & status"),
    CHAT("chat", "Chat & Messages", "Direct messages & conversation alerts"),
    SOCIAL("social", "Followers & Friends", "New followers & social updates"),
    GIFTS("gifts", "Live Gifts & Rewards", "Gift received & stream celebrations"),
    REDEEM("redeem", "Redeem & Payouts", "Redemption status & payout confirmations"),
    SYSTEM("system", "System & Announcements", "Security alerts & WorkDaily announcements");

    companion object {
        fun fromId(id: String): NotificationCategory {
            return values().find { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}

data class NotificationEvent(
    val id: String,
    val type: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val entityId: String? = null,
    val deepLink: String? = null,
    val artworkUrl: String? = null,
    val artworkResId: Int? = null,
    val priority: Int = 0,
    val isRead: Boolean = false,
    val source: String = "local", // "local" or "online"
    val expiryTime: Long? = null,
    val isDemo: Boolean = false
)

data class NotificationVisualStyle(
    val categoryLabel: String,
    val actionLabel: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val gradientColorsHex: List<Long>,
    val iconName: String = "bell",
    val illustrationResId: Int? = null
) {
    val primaryColor: Color get() = Color(primaryColorHex)
    val secondaryColor: Color get() = Color(secondaryColorHex)
    val gradientColors: List<Color> get() = gradientColorsHex.map { Color(it) }
}

data class NotificationPayload(
    val event: NotificationEvent,
    val style: NotificationVisualStyle
)

data class NotificationPreferences(
    val checkInEnabled: Boolean = true,
    val spinEnabled: Boolean = true,
    val streakEnabled: Boolean = true,
    val attendanceEnabled: Boolean = true,
    val salaryEnabled: Boolean = true,
    val passbookEnabled: Boolean = true,
    val backupEnabled: Boolean = true,
    val festivalsEnabled: Boolean = true,
    val liveEnabled: Boolean = true,
    val chatEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    val giftsEnabled: Boolean = true,
    val redeemEnabled: Boolean = true,
    val announcementsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val attendanceReminderTime: String = "08:00"
)
