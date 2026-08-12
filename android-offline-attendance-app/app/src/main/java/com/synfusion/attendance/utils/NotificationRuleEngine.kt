package com.synfusion.attendance.utils

import android.content.Context
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import java.text.SimpleDateFormat
import java.util.*

object NotificationRuleEngine {

    private const val PREFS_SPAM = "notification_spam_prevention"
    private const val KEY_LAST_ATTENDANCE_DATE = "last_attendance_reminder_date"
    private const val KEY_LAST_WEEKLY_DATE = "last_weekly_report_date"
    private const val KEY_LAST_MONTH_DATE = "last_monthly_report_date"
    private const val KEY_LAST_YEAR_DATE = "last_yearly_report_date"
    private const val KEY_SENT_MILESTONES = "sent_milestone_days"

    fun evaluateAllRules(context: Context, repository: LocalAttendanceRepository, style: String): RuleEvaluationResult {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val pref = context.getSharedPreferences(PREFS_SPAM, Context.MODE_PRIVATE)

        // 1. Fetch User Data
        val user: UserEntity? = kotlinx.coroutines.runBlocking { repository.getUser() }

        if (user == null) {
            return RuleEvaluationResult(false, LocalNotificationManager.CHANNEL_ATTENDANCE, "", "", "", "No active user")
        }

        val userId = user.id

        // 2. Fetch Attendance Records
        val allRecords = kotlinx.coroutines.runBlocking { repository.getPersonalAttendance() }
        val todayRecord = allRecords.find { it.date == todayStr && it.status != "advance" }

        val placeholders = buildPlaceholders(user, allRecords, todayStr)

        // Rule evaluation logic
        // Rule A: Daily Attendance Reminder
        var triggerCategory: String? = null
        var message = ""
        var channel = LocalNotificationManager.CHANNEL_ATTENDANCE
        var reason = "No rules triggered"

        val canSendDaily = pref.getString("${KEY_LAST_ATTENDANCE_DATE}_$userId", "") != todayStr
        if (todayRecord == null && canSendDaily) {
            // Attendance not marked for today
            val consecutiveMissed = calculateConsecutiveMissedDays(allRecords, todayStr)
            val milestoneSet = pref.getStringSet("${KEY_SENT_MILESTONES}_$userId", emptySet()) ?: emptySet()

            when {
                consecutiveMissed >= 30 && !milestoneSet.contains("30") -> {
                    triggerCategory = "long_absence"
                    message = NotificationMessageSelector.selectMessage(
                        context, triggerCategory, NotificationMessagePool.longAbsenceMessages, placeholders
                    )
                    reason = "30 consecutive missed days milestone"
                    saveSentMilestone(pref, userId, "30")
                }
                consecutiveMissed >= 14 && !milestoneSet.contains("14") -> {
                    triggerCategory = "missed_seven_plus"
                    message = NotificationMessageSelector.selectMessage(
                        context, triggerCategory, NotificationMessagePool.missedSevenPlusDaysMessages, placeholders
                    )
                    reason = "14 consecutive missed days milestone"
                    saveSentMilestone(pref, userId, "14")
                }
                consecutiveMissed >= 10 && !milestoneSet.contains("10") -> {
                    triggerCategory = "missed_ten_days"
                    message = NotificationMessageSelector.selectMessage(
                        context, triggerCategory, NotificationMessagePool.missedTenPlusDaysMessages, placeholders
                    )
                    reason = "10 consecutive missed days milestone"
                    saveSentMilestone(pref, userId, "10")
                }
                consecutiveMissed >= 7 && !milestoneSet.contains("7") -> {
                    triggerCategory = "missed_seven_plus"
                    message = NotificationMessageSelector.selectMessage(
                        context, triggerCategory, NotificationMessagePool.missedSevenPlusDaysMessages, placeholders
                    )
                    reason = "7 consecutive missed days milestone"
                    saveSentMilestone(pref, userId, "7")
                }
                consecutiveMissed >= 3 && !milestoneSet.contains("3") -> {
                    triggerCategory = "missed_three_days"
                    message = NotificationMessageSelector.selectMessage(
                        context, triggerCategory, NotificationMessagePool.missedThreeDaysMessages, placeholders
                    )
                    reason = "3 consecutive missed days milestone"
                    saveSentMilestone(pref, userId, "3")
                }
                consecutiveMissed >= 2 && !milestoneSet.contains("2") -> {
                    triggerCategory = "missed_two_days"
                    message = NotificationMessageSelector.selectMessage(
                        context, triggerCategory, NotificationMessagePool.missedTwoDaysMessages, placeholders
                    )
                    reason = "2 consecutive missed days milestone"
                    saveSentMilestone(pref, userId, "2")
                }
                else -> {
                    // Normal daily reminder
                    triggerCategory = "daily_attendance"
                    message = NotificationMessageSelector.selectMessage(
                        context, triggerCategory, NotificationMessagePool.dailyAttendanceMessages, placeholders
                    )
                    reason = "Today's attendance missing"
                }
            }

            if (triggerCategory != null) {
                pref.edit().putString("${KEY_LAST_ATTENDANCE_DATE}_$userId", todayStr).apply()
                return RuleEvaluationResult(true, channel, "आज की Attendance", message, triggerCategory, reason)
            }
        }

        // Rule B: Weekly Report on Sundays
        val calendar = Calendar.getInstance()
        val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val currentWeekStr = "${calendar.get(Calendar.YEAR)}-W${calendar.get(Calendar.WEEK_OF_YEAR)}"
        if (isSunday && pref.getString("${KEY_LAST_WEEKLY_DATE}_$userId", "") != currentWeekStr) {
            triggerCategory = "weekly_report"
            message = NotificationMessageSelector.selectMessage(
                context, triggerCategory, NotificationMessagePool.weeklyReportMessages, placeholders
            )
            pref.edit().putString("${KEY_LAST_WEEKLY_DATE}_$userId", currentWeekStr).apply()
            return RuleEvaluationResult(
                true,
                LocalNotificationManager.CHANNEL_REPORTS,
                "Weekly Report 📊",
                message,
                triggerCategory,
                "Weekly report triggered on Sunday"
            )
        }

        // Rule C: Monthly Report on 1st of Month
        val isFirstOfMonth = calendar.get(Calendar.DAY_OF_MONTH) == 1
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        if (isFirstOfMonth && pref.getString("${KEY_LAST_MONTH_DATE}_$userId", "") != currentMonthStr) {
            triggerCategory = "monthly_report"
            message = NotificationMessageSelector.selectMessage(
                context, triggerCategory, NotificationMessagePool.monthlyReportMessages, placeholders
            )
            pref.edit().putString("${KEY_LAST_MONTH_DATE}_$userId", currentMonthStr).apply()
            return RuleEvaluationResult(
                true,
                LocalNotificationManager.CHANNEL_REPORTS,
                "Monthly Summary 📅",
                message,
                triggerCategory,
                "Monthly report triggered on 1st of month"
            )
        }

        // Rule D: Yearly Report on 1st January
        val isFirstOfYear = calendar.get(Calendar.DAY_OF_MONTH) == 1 && calendar.get(Calendar.MONTH) == Calendar.JANUARY
        val currentYearStr = calendar.get(Calendar.YEAR).toString()
        if (isFirstOfYear && pref.getString("${KEY_LAST_YEAR_DATE}_$userId", "") != currentYearStr) {
            triggerCategory = "yearly_report"
            message = NotificationMessageSelector.selectMessage(
                context, triggerCategory, NotificationMessagePool.yearlyReportMessages, placeholders
            )
            pref.edit().putString("${KEY_LAST_YEAR_DATE}_$userId", currentYearStr).apply()
            return RuleEvaluationResult(
                true,
                LocalNotificationManager.CHANNEL_REPORTS,
                "Yearly Recap 🏆",
                message,
                triggerCategory,
                "Yearly summary triggered on 1st Jan"
            )
        }

        return RuleEvaluationResult(false, channel, "", "", "", reason)
    }

    private fun saveSentMilestone(pref: android.content.SharedPreferences, userId: String, day: String) {
        val milestoneSet = pref.getStringSet("${KEY_SENT_MILESTONES}_$userId", emptySet())?.toMutableSet() ?: mutableSetOf()
        milestoneSet.add(day)
        pref.edit().putStringSet("${KEY_SENT_MILESTONES}_$userId", milestoneSet).apply()
    }

    fun buildPlaceholders(user: UserEntity?, allRecords: List<AttendanceEntity>, todayStr: String): Map<String, String> {
        val userName = user?.name ?: ""
        val dailyWage = user?.dailyWage ?: 500.0

        val calendar = Calendar.getInstance()
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val currentYearPrefix = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

        val dayStr = calendar.get(Calendar.DAY_OF_MONTH).toString()
        val monthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(Date())
        val yearStr = calendar.get(Calendar.YEAR).toString()

        // Filter current month records
        val monthRecords = allRecords.filter { it.date.startsWith(currentMonthPrefix) }

        var presentCount = 0
        var absentCount = 0
        var halfDaysCount = 0
        var overtimeHours = 0
        var advanceAmount = 0.0

        monthRecords.forEach { rec ->
            if (rec.status == "present") {
                presentCount++
                if (rec.type == "half") {
                    halfDaysCount++
                }
                overtimeHours += rec.overtimeHours
            } else if (rec.status == "absent") {
                absentCount++
            } else if (rec.status == "advance") {
                advanceAmount += rec.advanceAmount
            }
        }

        val totalEarnings = monthRecords.filter { it.status == "present" }.sumOf { rec ->
            val isHalf = rec.type == "half"
            val base = if (isHalf) rec.wage / 2.0 else rec.wage
            val ot = OvertimeCalculator.calculateOvertimeAmount(rec.wage, rec.overtimeHours, rec.note)
            base + ot
        }

        val netPayable = totalEarnings - advanceAmount
        val workingDays = (presentCount - halfDaysCount) + (halfDaysCount * 0.5)

        val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val passedDays = calendar.get(Calendar.DAY_OF_MONTH)
        val attendancePercentage = if (passedDays > 0) {
            String.format(Locale.getDefault(), "%.0f", (workingDays / passedDays) * 100.0)
        } else "0"

        val missedDays = calculateConsecutiveMissedDays(allRecords, todayStr)
        val streak = calculateAttendanceStreak(allRecords)

        return mapOf(
            "userName" to userName,
            "date" to todayStr,
            "day" to dayStr,
            "month" to monthStr,
            "year" to yearStr,
            "missedDays" to missedDays.toString(),
            "presentDays" to presentCount.toString(),
            "halfDays" to halfDaysCount.toString(),
            "absentDays" to absentCount.toString(),
            "workingDays" to AmountFormatter.format(workingDays),
            "totalEarnings" to AmountFormatter.format(totalEarnings),
            "overtime" to overtimeHours.toString(),
            "advance" to AmountFormatter.format(advanceAmount),
            "netPayable" to AmountFormatter.format(netPayable),
            "attendancePercentage" to attendancePercentage,
            "streak" to streak.toString()
        )
    }

    private fun calculateConsecutiveMissedDays(allRecords: List<AttendanceEntity>, todayStr: String): Int {
        if (allRecords.isEmpty()) {
            return 0
        }
        val markedDates = allRecords.filter { it.status != "advance" }.map { it.date }.toSet()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var currentCal = Calendar.getInstance().apply {
            time = sdf.parse(todayStr) ?: Date()
        }

        var missedCount = 0
        // check backward
        while (true) {
            val dateToCheck = sdf.format(currentCal.time)
            if (markedDates.contains(dateToCheck)) {
                val record = allRecords.find { it.date == dateToCheck && it.status != "advance" }
                if (record?.status == "absent") {
                    missedCount++
                } else {
                    break
                }
            } else {
                missedCount++
            }
            currentCal.add(Calendar.DAY_OF_YEAR, -1)
            // Limit checks to prevent infinite loops
            if (missedCount > 365) break
        }
        return missedCount
    }

    private fun calculateAttendanceStreak(allRecords: List<AttendanceEntity>): Int {
        val presentDates = allRecords.filter { it.status == "present" }.map { it.date }.toSet()
        if (presentDates.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentCal = Calendar.getInstance()

        var streak = 0
        while (true) {
            val dateToCheck = sdf.format(currentCal.time)
            if (presentDates.contains(dateToCheck)) {
                streak++
            } else {
                // If checking today and it's not marked yet, don't break yet, check yesterday
                if (streak == 0 && dateToCheck == sdf.format(Date())) {
                    // skip today and continue checking
                } else {
                    break
                }
            }
            currentCal.add(Calendar.DAY_OF_YEAR, -1)
            if (streak > 365) break
        }
        return streak
    }
}

data class RuleEvaluationResult(
    val triggerNotification: Boolean,
    val channelId: String,
    val title: String,
    val message: String,
    val category: String,
    val reason: String
)
