package com.synfusion.attendance.notifications.registry

import com.synfusion.attendance.R
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationEvent
import com.synfusion.attendance.notifications.model.NotificationPayload
import com.synfusion.attendance.notifications.model.NotificationVisualStyle

object NotificationTemplateRegistry {

    // 12 Visual Family Definitions (Tango-inspired vibrant palettes)
    val LIVE_FAMILY = NotificationVisualStyle(
        categoryLabel = "LIVE STREAM",
        actionLabel = "Watch Live",
        primaryColorHex = 0xFFFF2A55, // Red/Pink
        secondaryColorHex = 0xFFFF7043, // Orange
        gradientColorsHex = listOf(0xFFFF1744, 0xFFFF5252, 0xFFFF7043),
        iconName = "video",
        illustrationResId = R.drawable.workdaily_coin
    )

    val SPIN_FAMILY = NotificationVisualStyle(
        categoryLabel = "BUILDER'S SPIN",
        actionLabel = "Spin Now",
        primaryColorHex = 0xFF9C27B0, // Purple
        secondaryColorHex = 0xFFFFD700, // Golden Yellow
        gradientColorsHex = listOf(0xFF8E24AA, 0xFFBA68C8, 0xFFFFD700),
        iconName = "refresh-cw",
        illustrationResId = R.drawable.reward_spin
    )

    val CHECK_IN_FAMILY = NotificationVisualStyle(
        categoryLabel = "DAILY REWARD",
        actionLabel = "Claim Reward",
        primaryColorHex = 0xFF0F9F7A, // Emerald Primary
        secondaryColorHex = 0xFFFFBE0B, // Gold Accent
        gradientColorsHex = listOf(0xFF0F9F7A, 0xFF10B981, 0xFFFFBE0B),
        iconName = "gift",
        illustrationResId = R.drawable.ic_wd_daily_checkin
    )

    val STREAK_FAMILY = NotificationVisualStyle(
        categoryLabel = "STREAK & MILESTONE",
        actionLabel = "View Streak",
        primaryColorHex = 0xFFFF6D00, // Vivid Orange
        secondaryColorHex = 0xFFFFD600, // Gold
        gradientColorsHex = listOf(0xFFDD2C00, 0xFFFF6D00, 0xFFFFD600),
        iconName = "flame",
        illustrationResId = R.drawable.checkin_streak
    )

    val ATTENDANCE_FAMILY = NotificationVisualStyle(
        categoryLabel = "ATTENDANCE",
        actionLabel = "Mark Attendance",
        primaryColorHex = 0xFF2979FF, // Royal Blue
        secondaryColorHex = 0xFF00E5FF, // Cyan
        gradientColorsHex = listOf(0xFF1565C0, 0xFF2979FF, 0xFF00E5FF),
        iconName = "calendar",
        illustrationResId = R.drawable.rewards_hub_worker
    )

    val SALARY_FAMILY = NotificationVisualStyle(
        categoryLabel = "SALARY & WAGES",
        actionLabel = "View Salary",
        primaryColorHex = 0xFF2E7D32, // Deep Green
        secondaryColorHex = 0xFFFFD700, // Gold
        gradientColorsHex = listOf(0xFF1B5E20, 0xFF2E7D32, 0xFFFFD700),
        iconName = "dollar-sign",
        illustrationResId = R.drawable.wallet_hero
    )

    val PASSBOOK_FAMILY = NotificationVisualStyle(
        categoryLabel = "PASSBOOK & PDF",
        actionLabel = "View Passbook",
        primaryColorHex = 0xFF00838F, // Teal
        secondaryColorHex = 0xFF00E5FF, // Cyan
        gradientColorsHex = listOf(0xFF006064, 0xFF00838F, 0xFF80DEEA),
        iconName = "file-text",
        illustrationResId = R.drawable.badge_first_report
    )

    val BACKUP_FAMILY = NotificationVisualStyle(
        categoryLabel = "BACKUP & RECORDS",
        actionLabel = "Backup Now",
        primaryColorHex = 0xFF4A148C, // Deep Violet
        secondaryColorHex = 0xFF2979FF, // Blue
        gradientColorsHex = listOf(0xFF4A148C, 0xFF7C4DFF, 0xFF2979FF),
        iconName = "shield",
        illustrationResId = R.drawable.file_000000004b2c82119a6ce8413df0e240
    )

    val CHAT_FAMILY = NotificationVisualStyle(
        categoryLabel = "PRIVATE CHAT",
        actionLabel = "Open Chat",
        primaryColorHex = 0xFF651FFF, // Purple Lavender
        secondaryColorHex = 0xFFB388FF,
        gradientColorsHex = listOf(0xFF4A148C, 0xFF651FFF, 0xFFB388FF),
        iconName = "message-square",
        illustrationResId = R.drawable.rewards_hub_worker
    )

    val SOCIAL_FAMILY = NotificationVisualStyle(
        categoryLabel = "SOCIAL & FOLLOWERS",
        actionLabel = "View Profile",
        primaryColorHex = 0xFFC2185B, // Rose
        secondaryColorHex = 0xFFFF80AB,
        gradientColorsHex = listOf(0xFF880E4F, 0xFFC2185B, 0xFFFF80AB),
        iconName = "user-plus",
        illustrationResId = R.drawable.refer_workers
    )

    val GIFTS_FAMILY = NotificationVisualStyle(
        categoryLabel = "LIVE GIFTS",
        actionLabel = "View Gift",
        primaryColorHex = 0xFFFF8F00, // Amber
        secondaryColorHex = 0xFFFFD54F,
        gradientColorsHex = listOf(0xFFFF6F00, 0xFFFF8F00, 0xFFFFD54F),
        iconName = "award",
        illustrationResId = R.drawable.workdaily_coin
    )

    val REDEEM_FAMILY = NotificationVisualStyle(
        categoryLabel = "REDEEM & PAYOUT",
        actionLabel = "View Wallet",
        primaryColorHex = 0xFF00C853, // Verified Green
        secondaryColorHex = 0xFFFFD600,
        gradientColorsHex = listOf(0xFF1B5E20, 0xFF00C853, 0xFFFFD600),
        iconName = "credit-card",
        illustrationResId = R.drawable.redeem_rewards
    )

    val SYSTEM_FAMILY = NotificationVisualStyle(
        categoryLabel = "ANNOUNCEMENT",
        actionLabel = "View Details",
        primaryColorHex = 0xFF37474F, // Slate
        secondaryColorHex = 0xFF90A4AE,
        gradientColorsHex = listOf(0xFF263238, 0xFF37474F, 0xFF78909C),
        iconName = "bell",
        illustrationResId = R.drawable.logo_transparent
    )

    // Full 57 Catalog Entries (40 Offline + 17 Online)
    val CATALOG_TEMPLATES: List<NotificationPayload> = listOf(
        // --- 40 OFFLINE SCENARIOS ---
        // A. DAILY REWARDS (1-12)
        createTemplate("off_1", "checkin_ready", NotificationCategory.CHECK_IN, "Your Daily Reward is Waiting!", "Claim 5 coins + 1 spin token now for your check-in.", CHECK_IN_FAMILY, "rewards_daily_checkin"),
        createTemplate("off_2", "checkin_pending", NotificationCategory.CHECK_IN, "Check-in Reminder", "Don't break your daily streak! Claim your reward today.", CHECK_IN_FAMILY, "rewards_daily_checkin"),
        createTemplate("off_3", "streak_risk", NotificationCategory.STREAK, "Keep Your Streak Alive! 🔥", "Your current streak is at risk of resetting tonight.", STREAK_FAMILY, "rewards_daily_checkin"),
        createTemplate("off_4", "streak_7day", NotificationCategory.STREAK, "7-Day Streak Builder!", "Congratulations! You unlocked the Safety Starter badge.", STREAK_FAMILY, "rewards_achievements"),
        createTemplate("off_5", "streak_30day", NotificationCategory.STREAK, "30-Day Streak Master! 🏆", "Amazing consistency! 30 days active in WorkDaily.", STREAK_FAMILY, "rewards_achievements"),
        createTemplate("off_6", "streak_180day", NotificationCategory.STREAK, "180-Day Construction Veteran! 🛠️", "Half a year of continuous active work logging!", STREAK_FAMILY, "rewards_achievements"),
        createTemplate("off_7", "streak_365day", NotificationCategory.STREAK, "365-Day Builder Legend! 👑", "One full year! You are a true WorkDaily Legend.", STREAK_FAMILY, "rewards_achievements"),
        createTemplate("off_8", "spin_ready", NotificationCategory.SPIN, "Your Free Spin is Ready! 🎰", "You have an available wheel spin waiting. Spin now!", SPIN_FAMILY, "rewards_spin"),
        createTemplate("off_9", "spin_reminder", NotificationCategory.SPIN, "Unused Spin Reminder", "Spin the Construction Wheel for extra coin prizes.", SPIN_FAMILY, "rewards_spin"),
        createTemplate("off_10", "spin_expiry", NotificationCategory.SPIN, "Spin Token Status", "Use your available spin token today.", SPIN_FAMILY, "rewards_spin"),
        createTemplate("off_11", "badge_unlocked", NotificationCategory.STREAK, "New Badge Unlocked! 🎉", "You earned a new achievement badge in WorkDaily.", STREAK_FAMILY, "rewards_achievements"),
        createTemplate("off_12", "reward_claimed", NotificationCategory.CHECK_IN, "Reward Claimed!", "5 coins added to your WorkDaily Wallet.", CHECK_IN_FAMILY, "rewards_wallet"),

        // B. ATTENDANCE & WORK (13-24)
        createTemplate("off_13", "att_morning_reminder", NotificationCategory.ATTENDANCE, "Morning Attendance Check", "Good morning! Don't forget to mark today's work.", ATTENDANCE_FAMILY, "dashboard"),
        createTemplate("off_14", "att_today_missing", NotificationCategory.ATTENDANCE, "Today's Attendance Missing", "You haven't marked attendance for today yet.", ATTENDANCE_FAMILY, "dashboard"),
        createTemplate("off_15", "att_yesterday_missing", NotificationCategory.ATTENDANCE, "Yesterday's Attendance Unmarked", "Update yesterday's attendance in your calendar.", ATTENDANCE_FAMILY, "calendar"),
        createTemplate("off_16", "att_present_saved", NotificationCategory.ATTENDANCE, "Attendance Saved: Present", "Today's full day attendance recorded successfully.", ATTENDANCE_FAMILY, "dashboard"),
        createTemplate("off_17", "att_absent_saved", NotificationCategory.ATTENDANCE, "Attendance Saved: Absent", "Today's leave/absent recorded.", ATTENDANCE_FAMILY, "dashboard"),
        createTemplate("off_18", "att_halfday_saved", NotificationCategory.ATTENDANCE, "Attendance Saved: Half-Day", "Today's half-day attendance saved.", ATTENDANCE_FAMILY, "dashboard"),
        createTemplate("off_19", "att_overtime_added", NotificationCategory.ATTENDANCE, "Overtime Hours Recorded", "Overtime hours added to your daily work log.", ATTENDANCE_FAMILY, "dashboard"),
        createTemplate("off_20", "att_weekly_summary", NotificationCategory.ATTENDANCE, "Weekly Work Summary", "Review your attendance and overtime stats for this week.", ATTENDANCE_FAMILY, "stats"),
        createTemplate("off_21", "att_monthly_summary", NotificationCategory.ATTENDANCE, "Monthly Attendance Complete", "Check your total present days for this month.", ATTENDANCE_FAMILY, "stats"),
        createTemplate("off_22", "att_work_streak", NotificationCategory.ATTENDANCE, "Work Consistency Streak", "10 consecutive days marked in WorkDaily!", ATTENDANCE_FAMILY, "stats"),
        createTemplate("off_23", "att_tomorrow_holiday", NotificationCategory.ATTENDANCE, "Holiday Tomorrow! 🎆", "Tomorrow is a scheduled holiday/festival on site.", ATTENDANCE_FAMILY, "calendar"),
        createTemplate("off_24", "att_planned_leave", NotificationCategory.ATTENDANCE, "Planned Leave Reminder", "Your upcoming planned leave starts tomorrow.", ATTENDANCE_FAMILY, "calendar"),

        // C. SALARY, ADVANCE & PASSBOOK (25-33)
        createTemplate("off_25", "sal_monthly_summary", NotificationCategory.SALARY, "Monthly Wage Summary Ready", "Your estimated net payable salary is updated.", SALARY_FAMILY, "passbook"),
        createTemplate("off_26", "sal_estimate_updated", NotificationCategory.SALARY, "Salary Target Calculation", "Check your updated estimated earnings for this month.", SALARY_FAMILY, "stats"),
        createTemplate("off_27", "sal_target_achieved", NotificationCategory.SALARY, "Earnings Goal Milestone", "You reached your monthly earning target!", SALARY_FAMILY, "stats"),
        createTemplate("off_28", "sal_advance_added", NotificationCategory.SALARY, "Advance Entry Recorded", "New advance payment entry recorded in daily log.", SALARY_FAMILY, "dashboard"),
        createTemplate("off_29", "sal_advance_reminder", NotificationCategory.SALARY, "Advance Payment Review", "Check your recent advance deductions in passbook.", SALARY_FAMILY, "passbook"),
        createTemplate("off_30", "sal_overtime_summary", NotificationCategory.SALARY, "Overtime Earnings Summary", "Your total overtime earnings for this month calculated.", SALARY_FAMILY, "passbook"),
        createTemplate("off_31", "sal_passbook_ready", NotificationCategory.PASSBOOK, "Monthly Passbook Statement Ready", "View your complete monthly wage and advance breakdown.", PASSBOOK_FAMILY, "passbook"),
        createTemplate("off_32", "sal_pdf_completed", NotificationCategory.PASSBOOK, "PDF Statement Exported 📄", "Your passbook PDF file has been saved to device.", PASSBOOK_FAMILY, "passbook"),
        createTemplate("off_33", "sal_pdf_reminder", NotificationCategory.PASSBOOK, "Passbook Backup Export", "Export your monthly work records as PDF.", PASSBOOK_FAMILY, "passbook"),

        // D. BACKUP & UTILITIES (34-40)
        createTemplate("off_34", "util_backup_reminder", NotificationCategory.BACKUP, "Protect Your Work Records 🛡️", "Create a local JSON/ZIP backup of your attendance data.", BACKUP_FAMILY, "settings"),
        createTemplate("off_35", "util_backup_completed", NotificationCategory.BACKUP, "Backup Successful", "Your work records have been backed up safely.", BACKUP_FAMILY, "settings"),
        createTemplate("off_36", "util_backup_failed", NotificationCategory.BACKUP, "Backup Issue", "Could not complete automatic data backup.", BACKUP_FAMILY, "settings"),
        createTemplate("off_37", "util_restore_completed", NotificationCategory.BACKUP, "Data Restore Complete", "Attendance records and preferences restored.", BACKUP_FAMILY, "settings"),
        createTemplate("off_38", "util_import_completed", NotificationCategory.BACKUP, "Records Imported Successfully", "Work records merged into active workspace.", BACKUP_FAMILY, "settings"),
        createTemplate("off_39", "util_festival_tomorrow", NotificationCategory.BACKUP, "Upcoming Festival Tomorrow 🪔", "Enjoy the upcoming festival celebration!", BACKUP_FAMILY, "calendar"),
        createTemplate("off_40", "util_monthly_review", NotificationCategory.BACKUP, "Monthly Records Review", "Review and audit your month's records.", BACKUP_FAMILY, "all_time_passbook"),

        // --- 17 ONLINE SCENARIOS ---
        createTemplate("on_41", "live_started", NotificationCategory.LIVE, "Rajesh Sharma is LIVE! 🔴", "Civil Site Foundation & Concrete Tips broadcast active.", LIVE_FAMILY, "live_discover"),
        createTemplate("on_42", "friend_live_started", NotificationCategory.LIVE, "Your Friend Started Live", "Join the live stream conversation now.", LIVE_FAMILY, "live_discover"),
        createTemplate("on_43", "live_invitation", NotificationCategory.LIVE, "Live Stream Guest Invite", "You've been invited to join as guest broadcaster.", LIVE_FAMILY, "live_discover"),
        createTemplate("on_44", "live_scheduled", NotificationCategory.LIVE, "Scheduled Stream Starting Soon", "Civil Machinery Q&A starts in 15 minutes.", LIVE_FAMILY, "live_discover"),
        createTemplate("on_45", "chat_message", NotificationCategory.CHAT, "Pooja sent you a message 💬", "Hey! Have you reviewed the site drawing?", CHAT_FAMILY, "live_people"),
        createTemplate("on_46", "chat_request", NotificationCategory.CHAT, "New Message Request", "You received a chat request from a site worker.", CHAT_FAMILY, "live_people"),
        createTemplate("on_47", "social_follower", NotificationCategory.SOCIAL, "Anu Started Following You 👤", "View public profile and follow back.", SOCIAL_FAMILY, "live_people"),
        createTemplate("on_48", "social_followback", NotificationCategory.SOCIAL, "Follow Back Confirmation", "Ramesh followed you back on WorkDaily.", SOCIAL_FAMILY, "live_people"),
        createTemplate("on_49", "gift_received", NotificationCategory.GIFTS, "You Received a Power Drill! 🎁", "50 diamonds reward added from live stream.", GIFTS_FAMILY, "live_wallet"),
        createTemplate("on_50", "gift_combo", NotificationCategory.GIFTS, "5x Gift Combo Streak! 🔥", "Viewer sent 5 Safety Helmets during stream!", GIFTS_FAMILY, "live_wallet"),
        createTemplate("on_51", "social_friend_request", NotificationCategory.SOCIAL, "Friend Connection Request", "Connect with verified site contractors.", SOCIAL_FAMILY, "live_people"),
        createTemplate("on_52", "redeem_submitted", NotificationCategory.REDEEM, "Redemption Request Submitted", "Your reward withdrawal request is being processed.", REDEEM_FAMILY, "rewards_wallet"),
        createTemplate("on_53", "redeem_processing", NotificationCategory.REDEEM, "Redeem Request In Progress", "Verification under process by WorkDaily support.", REDEEM_FAMILY, "rewards_wallet"),
        createTemplate("on_54", "redeem_paid", NotificationCategory.REDEEM, "Payout Completed Successfully ✅", "₹500 payout transferred to your UPI account.", REDEEM_FAMILY, "rewards_wallet"),
        createTemplate("on_55", "redeem_failed", NotificationCategory.REDEEM, "Redeem Request Returned", "Details could not be verified. Coins refunded.", REDEEM_FAMILY, "rewards_wallet"),
        createTemplate("on_56", "sys_security_alert", NotificationCategory.SYSTEM, "Account Security Update", "New device login detected for your profile.", SYSTEM_FAMILY, "settings"),
        createTemplate("on_57", "sys_announcement", NotificationCategory.SYSTEM, "WorkDaily V3 Update Live! 🚀", "Explore brand-new Smart Rich Notifications.", SYSTEM_FAMILY, "dashboard")
    )

    private fun createTemplate(
        id: String,
        type: String,
        category: NotificationCategory,
        title: String,
        body: String,
        style: NotificationVisualStyle,
        deepLink: String
    ): NotificationPayload {
        return NotificationPayload(
            event = NotificationEvent(
                id = id,
                type = type,
                category = category,
                title = title,
                body = body,
                deepLink = deepLink,
                isDemo = true
            ),
            style = style
        )
    }

    fun getTemplateForType(type: String): NotificationPayload? {
        return CATALOG_TEMPLATES.find { it.event.type.equals(type, ignoreCase = true) }
    }

    fun getVisualStyleForCategory(category: NotificationCategory): NotificationVisualStyle {
        return when (category) {
            NotificationCategory.LIVE -> LIVE_FAMILY
            NotificationCategory.SPIN -> SPIN_FAMILY
            NotificationCategory.CHECK_IN -> CHECK_IN_FAMILY
            NotificationCategory.STREAK -> STREAK_FAMILY
            NotificationCategory.ATTENDANCE -> ATTENDANCE_FAMILY
            NotificationCategory.SALARY -> SALARY_FAMILY
            NotificationCategory.PASSBOOK -> PASSBOOK_FAMILY
            NotificationCategory.BACKUP -> BACKUP_FAMILY
            NotificationCategory.CHAT -> CHAT_FAMILY
            NotificationCategory.SOCIAL -> SOCIAL_FAMILY
            NotificationCategory.GIFTS -> GIFTS_FAMILY
            NotificationCategory.REDEEM -> REDEEM_FAMILY
            NotificationCategory.SYSTEM -> SYSTEM_FAMILY
        }
    }
}
