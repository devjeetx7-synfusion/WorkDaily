package com.synfusion.attendance.utils

import android.content.Context
import android.util.Log
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import java.text.SimpleDateFormat
import java.util.*

object NotificationRuleEngine {

    private const val TAG = "WorkDailyNotification"
    private const val PREFS_SPAM = "notification_spam_prevention"
    private const val KEY_LAST_ATTENDANCE_DATE = "last_attendance_reminder_date"
    private const val KEY_LAST_DAILY_SUMMARY_DATE = "last_daily_summary_date"
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
            Log.d(TAG, "evaluateAllRules: No active user profile found in database.")
            return RuleEvaluationResult(false, LocalNotificationManager.CHANNEL_ATTENDANCE, "", "", "", "No active user")
        }

        val userId = user.id

        // 2. Fetch Attendance Records
        val allRecords = kotlinx.coroutines.runBlocking { repository.getPersonalAttendance() }
        val todayRecord = allRecords.find { it.date == todayStr && it.status != "advance" }

        val placeholders = buildPlaceholders(user, allRecords, todayStr)

        // Rule A: Daily Attendance Reminder
        var triggerCategory: String? = null
        var message = ""
        var title = "WorkDaily"
        var reason = "No rules triggered"

        val canSendDaily = pref.getString("${KEY_LAST_ATTENDANCE_DATE}_$userId", "") != todayStr
        if (todayRecord == null && canSendDaily) {
            val consecutiveMissed = calculateConsecutiveMissedDays(allRecords, todayStr)
            val milestoneSet = pref.getStringSet("${KEY_SENT_MILESTONES}_$userId", emptySet()) ?: emptySet()

            var milestoneToSave: String? = null

            when {
                consecutiveMissed >= 30 && !milestoneSet.contains("30") -> {
                    triggerCategory = "missing_attendance"
                    reason = "30 consecutive missed days milestone"
                    milestoneToSave = "30"
                }
                consecutiveMissed >= 14 && !milestoneSet.contains("14") -> {
                    triggerCategory = "missing_attendance"
                    reason = "14 consecutive missed days milestone"
                    milestoneToSave = "14"
                }
                consecutiveMissed >= 10 && !milestoneSet.contains("10") -> {
                    triggerCategory = "missing_attendance"
                    reason = "10 consecutive missed days milestone"
                    milestoneToSave = "10"
                }
                consecutiveMissed >= 7 && !milestoneSet.contains("7") -> {
                    triggerCategory = "missing_attendance"
                    reason = "7 consecutive missed days milestone"
                    milestoneToSave = "7"
                }
                consecutiveMissed >= 3 && !milestoneSet.contains("3") -> {
                    triggerCategory = "missing_attendance"
                    reason = "3 consecutive missed days milestone"
                    milestoneToSave = "3"
                }
                consecutiveMissed >= 2 && !milestoneSet.contains("2") -> {
                    triggerCategory = "missing_attendance"
                    reason = "2 consecutive missed days milestone"
                    milestoneToSave = "2"
                }
                else -> {
                    triggerCategory = "attendance_reminder"
                    reason = "Today's attendance missing"
                }
            }

            val templatePair = NotificationTemplateRepository.selectTemplate(context, triggerCategory, placeholders)
            if (templatePair != null) {
                val (template, formattedMsg) = templatePair
                title = template.title
                message = formattedMsg
            } else {
                title = "Aaj attendance nahi lagayi? 😂"
                message = NotificationTemplateRepository.formatMessage(
                    "{userName}, aaj ki attendance pending hai bhai! Jaldi se laga lo. 😂",
                    placeholders
                )
            }

            val channel = LocalNotificationManager.mapCategoryToChannel(triggerCategory)

            Log.i(TAG, "Daily Attendance Rule triggered! Category: $triggerCategory, Title: $title, Reason: $reason")

            return RuleEvaluationResult(
                triggerNotification = true,
                channelId = channel,
                title = title,
                message = message,
                category = triggerCategory,
                reason = reason,
                stateKeyToPersist = "${KEY_LAST_ATTENDANCE_DATE}_$userId",
                stateValueToPersist = todayStr,
                milestoneToPersist = milestoneToSave,
                userId = userId
            )
        } else if (todayRecord != null) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val canSendDailySummary = pref.getString("${KEY_LAST_DAILY_SUMMARY_DATE}_$userId", "") != todayStr

            if (canSendDailySummary && hour >= 18) {
                val cat = if (todayRecord.overtimeHours > 0) "overtime" else "daily_summary"
                val templatePair = NotificationTemplateRepository.selectTemplate(context, cat, placeholders)
                if (templatePair != null) {
                    val (template, formattedMsg) = templatePair
                    title = template.title
                    message = formattedMsg
                } else {
                    title = "Aaj ka hisaab ready hai! 📊"
                    message = NotificationTemplateRepository.formatMessage(
                        "{userName}, aaj ki shift poori. Passbook me hisaab dekh lo bhai! 💰",
                        placeholders
                    )
                }

                val channel = LocalNotificationManager.mapCategoryToChannel(cat)

                return RuleEvaluationResult(
                    triggerNotification = true,
                    channelId = channel,
                    title = title,
                    message = message,
                    category = cat,
                    reason = "Daily summary for marked attendance",
                    stateKeyToPersist = "${KEY_LAST_DAILY_SUMMARY_DATE}_$userId",
                    stateValueToPersist = todayStr,
                    userId = userId
                )
            }
        }

        // Rule B: Weekly Report on Sundays
        val calendar = Calendar.getInstance()
        val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val currentWeekStr = "${calendar.get(Calendar.YEAR)}-W${calendar.get(Calendar.WEEK_OF_YEAR)}"
        if (isSunday && pref.getString("${KEY_LAST_WEEKLY_DATE}_$userId", "") != currentWeekStr) {
            triggerCategory = "weekly_summary"
            val templatePair = NotificationTemplateRepository.selectTemplate(context, triggerCategory, placeholders)
            if (templatePair != null) {
                title = templatePair.first.title
                message = templatePair.second
            } else {
                title = "Hafte ka hisaab ready hai! 📊"
                message = NotificationTemplateRepository.formatMessage(
                    "{userName}, hafte ki report ready hai! Passbook khol ke dekh le.",
                    placeholders
                )
            }

            Log.i(TAG, "Weekly Report Rule triggered on Sunday! Week: $currentWeekStr")

            return RuleEvaluationResult(
                triggerNotification = true,
                channelId = LocalNotificationManager.CHANNEL_SUMMARY,
                title = title,
                message = message,
                category = triggerCategory,
                reason = "Weekly report triggered on Sunday",
                stateKeyToPersist = "${KEY_LAST_WEEKLY_DATE}_$userId",
                stateValueToPersist = currentWeekStr,
                userId = userId
            )
        }

        // Rule C: Monthly Report on 1st of Month
        val isFirstOfMonth = calendar.get(Calendar.DAY_OF_MONTH) == 1
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        if (isFirstOfMonth && pref.getString("${KEY_LAST_MONTH_DATE}_$userId", "") != currentMonthStr) {
            triggerCategory = "monthly_summary"
            val templatePair = NotificationTemplateRepository.selectTemplate(context, triggerCategory, placeholders)
            if (templatePair != null) {
                title = templatePair.first.title
                message = templatePair.second
            } else {
                title = "Mahine ka report card! 📅"
                message = NotificationTemplateRepository.formatMessage(
                    "{userName}, mahina poora ho gaya! Passbook me dekh kitni kamai hui. 💰",
                    placeholders
                )
            }

            Log.i(TAG, "Monthly Report Rule triggered on 1st of Month! Month: $currentMonthStr")

            return RuleEvaluationResult(
                triggerNotification = true,
                channelId = LocalNotificationManager.CHANNEL_SUMMARY,
                title = title,
                message = message,
                category = triggerCategory,
                reason = "Monthly report triggered on 1st of month",
                stateKeyToPersist = "${KEY_LAST_MONTH_DATE}_$userId",
                stateValueToPersist = currentMonthStr,
                userId = userId
            )
        }

        // Rule D: Yearly Report on 1st January
        val isFirstOfYear = calendar.get(Calendar.DAY_OF_MONTH) == 1 && calendar.get(Calendar.MONTH) == Calendar.JANUARY
        val currentYearStr = calendar.get(Calendar.YEAR).toString()
        if (isFirstOfYear && pref.getString("${KEY_LAST_YEAR_DATE}_$userId", "") != currentYearStr) {
            triggerCategory = "yearly_report"
            val templatePair = NotificationTemplateRepository.selectTemplate(context, triggerCategory, placeholders)
            if (templatePair != null) {
                title = templatePair.first.title
                message = templatePair.second
            } else {
                title = "Saal bhar ka maha-hisaab! 🏆"
                message = NotificationTemplateRepository.formatMessage(
                    "{userName}, poore saal ki mehnat ka hisaab ready hai bhai! Khol ke dekh le.",
                    placeholders
                )
            }

            Log.i(TAG, "Yearly Report Rule triggered on 1st January! Year: $currentYearStr")

            return RuleEvaluationResult(
                triggerNotification = true,
                channelId = LocalNotificationManager.CHANNEL_SUMMARY,
                title = title,
                message = message,
                category = triggerCategory,
                reason = "Yearly summary triggered on 1st Jan",
                stateKeyToPersist = "${KEY_LAST_YEAR_DATE}_$userId",
                stateValueToPersist = currentYearStr,
                userId = userId
            )
        }

        return RuleEvaluationResult(false, LocalNotificationManager.CHANNEL_ATTENDANCE, "", "", "", reason)
    }

    fun markRuleStatePersisted(context: Context, evalResult: RuleEvaluationResult) {
        val pref = context.getSharedPreferences(PREFS_SPAM, Context.MODE_PRIVATE)
        val editor = pref.edit()
        if (!evalResult.stateKeyToPersist.isNullOrBlank() && !evalResult.stateValueToPersist.isNullOrEmpty()) {
            editor.putString(evalResult.stateKeyToPersist, evalResult.stateValueToPersist)
        }
        if (!evalResult.milestoneToPersist.isNullOrEmpty() && !evalResult.userId.isNullOrEmpty()) {
            saveSentMilestone(pref, evalResult.userId, evalResult.milestoneToPersist)
        }
        editor.apply()
        Log.d(TAG, "Persisted notification rule state for key: ${evalResult.stateKeyToPersist}")
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

        val dayStr = calendar.get(Calendar.DAY_OF_MONTH).toString()
        val monthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(Date())
        val yearStr = calendar.get(Calendar.YEAR).toString()

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
            "streak" to streak.toString(),
            "amount" to AmountFormatter.format(if (totalEarnings > 0) totalEarnings else dailyWage),
            "hours" to if (overtimeHours > 0) overtimeHours.toString() else "8",
            "time" to SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
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
                if (streak == 0 && dateToCheck == sdf.format(Date())) {
                    // skip today if not marked yet
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
    val reason: String,
    val stateKeyToPersist: String? = null,
    val stateValueToPersist: String? = null,
    val milestoneToPersist: String? = null,
    val userId: String? = null
)
