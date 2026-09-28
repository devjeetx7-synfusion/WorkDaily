package com.synfusion.attendance.utils

import android.content.Context
import android.util.Log
import com.synfusion.attendance.data.HolidayCategory
import com.synfusion.attendance.data.IndianHolidayRepository
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
    private const val KEY_FESTIVAL_SENT_DATE = "last_festival_sent_date"
    private const val KEY_FESTIVAL_UPCOMING_SENT_DATE = "last_festival_upcoming_sent_date"

    fun evaluateAllRules(context: Context, repository: LocalAttendanceRepository, style: String = "Friendly"): RuleEvaluationResult {
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

        // 3. Evaluate Festival Notifications (Top Priority on Festival Days / Upcoming Festival Days)
        val festivalResult = evaluateFestivalRules(context, user, allRecords, todayStr)
        if (festivalResult.triggerNotification) {
            Log.i(TAG, "Festival Rule triggered! Title: ${festivalResult.title}, Reason: ${festivalResult.reason}")
            return festivalResult
        }

        val todayRecord = allRecords.find { it.date == todayStr && it.status != "advance" }

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

            val placeholders = buildPlaceholders(user, allRecords, todayStr, triggerCategory)
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
                val placeholders = buildPlaceholders(user, allRecords, todayStr, cat)
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
            val placeholders = buildPlaceholders(user, allRecords, todayStr, triggerCategory)
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

        // Rule C: Monthly Report on 1st of Month (Calculates completed previous month)
        val isFirstOfMonth = calendar.get(Calendar.DAY_OF_MONTH) == 1
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        if (isFirstOfMonth && pref.getString("${KEY_LAST_MONTH_DATE}_$userId", "") != currentMonthStr) {
            triggerCategory = "monthly_summary"
            val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            val targetMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(prevCal.time)
            val placeholders = buildPlaceholders(user, allRecords, todayStr, triggerCategory, targetMonthStr)
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

            Log.i(TAG, "Monthly Report Rule triggered on 1st of Month! Target Month: $targetMonthStr")

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

        // Rule D: Yearly Report on 1st January (Calculates completed previous year)
        val isFirstOfYear = calendar.get(Calendar.DAY_OF_MONTH) == 1 && calendar.get(Calendar.MONTH) == Calendar.JANUARY
        val currentYearStr = calendar.get(Calendar.YEAR).toString()
        if (isFirstOfYear && pref.getString("${KEY_LAST_YEAR_DATE}_$userId", "") != currentYearStr) {
            triggerCategory = "yearly_report"
            val targetYearStr = (calendar.get(Calendar.YEAR) - 1).toString()
            val placeholders = buildPlaceholders(user, allRecords, todayStr, triggerCategory, targetYearStr)
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

            Log.i(TAG, "Yearly Report Rule triggered on 1st January! Target Year: $targetYearStr")

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

    private fun evaluateFestivalRules(
        context: Context,
        user: UserEntity,
        allRecords: List<AttendanceEntity>,
        todayStr: String
    ): RuleEvaluationResult {
        val pref = context.getSharedPreferences(PREFS_SPAM, Context.MODE_PRIVATE)
        val userId = user.id

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        try {
            val parsed = sdfDate.parse(todayStr)
            if (parsed != null) todayCal.time = parsed
        } catch (e: Exception) {
            // fallback
        }

        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val todayMonth = todayCal.get(Calendar.MONTH) + 1 // 1-12
        val todayYear = todayCal.get(Calendar.YEAR)

        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowStr = sdfDate.format(tomorrowCal.time)
        val tomorrowDay = tomorrowCal.get(Calendar.DAY_OF_MONTH)
        val tomorrowMonth = tomorrowCal.get(Calendar.MONTH) + 1
        val tomorrowYear = tomorrowCal.get(Calendar.YEAR)

        // 1. Check Today's Festival
        val todayEvent = IndianHolidayRepository.getHolidaysForDate(todayDay, todayMonth, todayYear)
            .firstOrNull { it.category == HolidayCategory.FESTIVAL }

        if (todayEvent != null) {
            val normId = FestivalNotificationRepository.normalizeFestivalId(todayEvent.name)
            val stateKey = "${KEY_FESTIVAL_SENT_DATE}_${normId}_$userId"
            val lastSentDate = pref.getString(stateKey, "")

            if (lastSentDate != todayStr) {
                val placeholders = buildPlaceholders(user, allRecords, todayStr, "festival")
                val templatePair = FestivalNotificationRepository.selectTemplateForFestival(
                    context = context,
                    festivalId = normId,
                    isUpcoming = false,
                    placeholders = placeholders
                )

                if (templatePair != null) {
                    val (template, formattedMsg) = templatePair
                    return RuleEvaluationResult(
                        triggerNotification = true,
                        channelId = LocalNotificationManager.mapCategoryToChannel("festival"),
                        title = template.title,
                        message = formattedMsg,
                        category = "festival",
                        reason = "Festival day notification for ${todayEvent.name}",
                        stateKeyToPersist = stateKey,
                        stateValueToPersist = todayStr,
                        userId = userId
                    )
                }
            }
        }

        // 2. Check Tomorrow's Festival (Upcoming Festival Reminder)
        val tomorrowEvent = IndianHolidayRepository.getHolidaysForDate(tomorrowDay, tomorrowMonth, tomorrowYear)
            .firstOrNull { it.category == HolidayCategory.FESTIVAL }

        if (tomorrowEvent != null) {
            val normId = FestivalNotificationRepository.normalizeFestivalId(tomorrowEvent.name)
            val upcomingStateKey = "${KEY_FESTIVAL_UPCOMING_SENT_DATE}_${normId}_$userId"
            val lastUpcomingSentDate = pref.getString(upcomingStateKey, "")

            if (lastUpcomingSentDate != tomorrowStr) {
                val placeholders = buildPlaceholders(user, allRecords, todayStr, "festival")
                val templatePair = FestivalNotificationRepository.selectTemplateForFestival(
                    context = context,
                    festivalId = normId,
                    isUpcoming = true,
                    placeholders = placeholders
                )

                if (templatePair != null) {
                    val (template, formattedMsg) = templatePair
                    return RuleEvaluationResult(
                        triggerNotification = true,
                        channelId = LocalNotificationManager.mapCategoryToChannel("festival"),
                        title = template.title,
                        message = formattedMsg,
                        category = "festival",
                        reason = "Upcoming festival notification for ${tomorrowEvent.name}",
                        stateKeyToPersist = upcomingStateKey,
                        stateValueToPersist = tomorrowStr,
                        userId = userId
                    )
                }
            }
        }

        return RuleEvaluationResult(false, LocalNotificationManager.CHANNEL_REMINDERS, "", "", "", "No festival active today or tomorrow")
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

    fun buildPlaceholders(
        user: UserEntity?,
        allRecords: List<AttendanceEntity>,
        todayStr: String,
        category: String? = null,
        targetPeriodStr: String? = null
    ): Map<String, String> {
        val userName = user?.name ?: ""
        val dailyWage = user?.dailyWage ?: 500.0

        val calendar = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val parsed = sdfDate.parse(todayStr)
            if (parsed != null) calendar.time = parsed
        } catch (e: Exception) {
            // fallback to current time
        }

        val dayStr = calendar.get(Calendar.DAY_OF_MONTH).toString()
        val monthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
        val yearStr = calendar.get(Calendar.YEAR).toString()

        val periodRecords: List<AttendanceEntity>
        val totalPeriodDays: Int

        when (category) {
            "weekly_summary" -> {
                // Last 7 days ending on todayStr
                val endCal = calendar.clone() as Calendar
                val startCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
                val startDateStr = sdfDate.format(startCal.time)
                val endDateStr = sdfDate.format(endCal.time)

                periodRecords = allRecords.filter { it.date >= startDateStr && it.date <= endDateStr }
                totalPeriodDays = 7
            }
            "monthly_summary" -> {
                val targetMonthPrefix = targetPeriodStr ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
                periodRecords = allRecords.filter { AttendanceCalculationEngine.isDateInMonth(it.date, targetMonthPrefix) }

                val targetCal = Calendar.getInstance()
                try {
                    val parts = targetMonthPrefix.split("-")
                    targetCal.set(Calendar.YEAR, parts[0].toInt())
                    targetCal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    targetCal.set(Calendar.DAY_OF_MONTH, 1)
                } catch (e: Exception) {
                    // default
                }
                totalPeriodDays = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            "yearly_report" -> {
                val targetYear = targetPeriodStr ?: calendar.get(Calendar.YEAR).toString()
                periodRecords = allRecords.filter { it.date.startsWith(targetYear) }
                totalPeriodDays = if (targetYear == calendar.get(Calendar.YEAR).toString()) {
                    calendar.get(Calendar.DAY_OF_YEAR)
                } else 365
            }
            else -> {
                // Default: current month
                val targetMonthPrefix = targetPeriodStr ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
                periodRecords = allRecords.filter { AttendanceCalculationEngine.isDateInMonth(it.date, targetMonthPrefix) }
                totalPeriodDays = calendar.get(Calendar.DAY_OF_MONTH)
            }
        }

        var presentCount = 0
        var absentCount = 0
        var halfDaysCount = 0
        var overtimeHours = 0
        var advanceAmount = 0.0

        periodRecords.forEach { rec ->
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

        val totalEarnings = periodRecords.filter { it.status == "present" }.sumOf { rec ->
            val effectiveWage = if (rec.wage > 0) rec.wage else dailyWage
            val isHalf = rec.type == "half"
            val base = if (isHalf) effectiveWage / 2.0 else effectiveWage
            val ot = OvertimeCalculator.calculateOvertimeAmount(effectiveWage, rec.overtimeHours, rec.note)
            base + ot
        }

        val netPayable = totalEarnings - advanceAmount
        val workingDays = (presentCount - halfDaysCount) + (halfDaysCount * 0.5)

        val attendancePercentage = if (totalPeriodDays > 0) {
            String.format(Locale.getDefault(), "%.0f", (workingDays / totalPeriodDays) * 100.0)
        } else "0"

        val missedDays = calculateConsecutiveMissedDays(allRecords, todayStr)
        val streak = calculateAttendanceStreak(allRecords)

        // Calculate today specific details for daily/overtime/attendance notifications
        val todayRecords = allRecords.filter { it.date == todayStr }
        val todayAttendance = todayRecords.find { it.status != "advance" }
        val todayEarned = if (todayAttendance != null && todayAttendance.status == "present") {
            val effectiveWage = if (todayAttendance.wage > 0) todayAttendance.wage else dailyWage
            val isHalf = todayAttendance.type == "half"
            val base = if (isHalf) effectiveWage / 2.0 else effectiveWage
            val ot = OvertimeCalculator.calculateOvertimeAmount(effectiveWage, todayAttendance.overtimeHours, todayAttendance.note)
            base + ot
        } else 0.0

        val todayOtHours = todayAttendance?.overtimeHours ?: 0

        // Determination of amount placeholder:
        // For summary categories (weekly_summary, monthly_summary, yearly_report), use totalEarnings of that period.
        // For daily/attendance/overtime notifications, use todayEarned if > 0, else dailyWage.
        val amountVal = when (category) {
            "weekly_summary", "monthly_summary", "yearly_report" -> totalEarnings
            else -> if (todayEarned > 0.0) todayEarned else dailyWage
        }

        val hoursVal = when (category) {
            "weekly_summary", "monthly_summary", "yearly_report" -> overtimeHours.toString()
            else -> if (todayOtHours > 0) todayOtHours.toString() else "8"
        }

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
            "totalEarnings" to AmountFormatter.formatWithSymbol(totalEarnings, user?.currencyCode ?: "INR"),
            "overtime" to overtimeHours.toString(),
            "advance" to AmountFormatter.formatWithSymbol(advanceAmount, user?.currencyCode ?: "INR"),
            "netPayable" to AmountFormatter.formatWithSymbol(netPayable, user?.currencyCode ?: "INR"),
            "attendancePercentage" to attendancePercentage,
            "streak" to streak.toString(),
            "amount" to AmountFormatter.formatWithSymbol(amountVal, user?.currencyCode ?: "INR"),
            "hours" to hoursVal,
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
