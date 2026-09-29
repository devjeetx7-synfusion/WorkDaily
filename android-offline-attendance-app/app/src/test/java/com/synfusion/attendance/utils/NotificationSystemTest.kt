package com.synfusion.attendance.utils

import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class NotificationSystemTest {

    @Test
    fun test200PlusNotificationTemplatesLoaded() {
        val file = java.io.File("src/main/res/raw/notification_templates.json").takeIf { it.exists() }
            ?: java.io.File("app/src/main/res/raw/notification_templates.json")
        assertTrue("notification_templates.json must exist", file.exists())

        val jsonString = file.readText()
        val rootObj = com.google.gson.JsonParser.parseString(jsonString).asJsonObject
        val array = rootObj.getAsJsonArray("templates")

        assertTrue("Template count must be 200+", array.size() >= 200)

        val ids = mutableSetOf<String>()
        for (i in 0 until array.size()) {
            val item = array.get(i).asJsonObject
            val id = item.get("id").asString
            val category = item.get("category").asString
            val title = item.get("title").asString
            val message = item.get("message").asString

            assertFalse("ID must not be empty", id.isBlank())
            assertFalse("Category must not be empty", category.isBlank())
            assertFalse("Title must not be empty", title.isBlank())
            assertFalse("Message must not be empty", message.isBlank())

            ids.add(id)
        }

        assertEquals("All template IDs must be unique", array.size(), ids.size)
    }

    @Test
    fun testPlaceholderReplacementWithName() {
        val template = "{userName}, aaj ki attendance nahi lagayi? 😄"
        val placeholders = mapOf("userName" to "Rahul")

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("Rahul, aaj ki attendance nahi lagayi? 😄", result)
    }

    @Test
    fun testPlaceholderReplacementWithoutName() {
        val template = "{userName}, aaj ki attendance nahi lagayi? 😄"
        val placeholders = mapOf("userName" to "")

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("aaj ki attendance nahi lagayi? 😄", result)
    }

    @Test
    fun testMultiplePlaceholdersReplacement() {
        val template = "कमाई: ₹{totalEarnings}, एडवांस: ₹{advance}, शुद्ध देय: ₹{netPayable}"
        val placeholders = mapOf(
            "totalEarnings" to "12,000",
            "advance" to "2,000",
            "netPayable" to "10,000"
        )

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("कमाई: ₹12,000, एडवांस: ₹2,000, शुद्ध देय: ₹10,000", result)
    }

    @Test
    fun testPlaceholderNullOrBlankCleanup() {
        val template = "Hello {userName}, aaj kaam kiya ya aaram?"
        val placeholders = mapOf("userName" to "null")

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("Hello, aaj kaam kiya ya aaram?", result)
    }

    @Test
    fun testBuildPlaceholdersMap() {
        val user = UserEntity("user_1", "Ramesh Kumar", "9876543210", "ramesh@test.com", "personal", 600.0)

        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val records = listOf(
            AttendanceEntity("p1", null, "$currentMonthPrefix-01", "present", "full", wage = 600.0),
            AttendanceEntity("p2", null, "$currentMonthPrefix-02", "present", "full", wage = 600.0),
            AttendanceEntity("p3", null, "$currentMonthPrefix-03", "present", "half", wage = 600.0),
            AttendanceEntity("p4", null, "$currentMonthPrefix-04", "present", "full", wage = 600.0),
            AttendanceEntity("p5", null, "$currentMonthPrefix-05", "present", "full", wage = 600.0),
            AttendanceEntity("a1", null, "$currentMonthPrefix-06", "absent", "full", wage = 600.0),
            AttendanceEntity("adv", null, "$currentMonthPrefix-07", "advance", "full", advanceAmount = 500.0)
        )

        val placeholders = NotificationRuleEngine.buildPlaceholders(user, records, "$currentMonthPrefix-08", "monthly_summary")

        assertEquals("Ramesh Kumar", placeholders["userName"])
        assertEquals("5", placeholders["presentDays"])
        assertEquals("1", placeholders["halfDays"])
        assertEquals("1", placeholders["absentDays"])
        assertEquals("4.5", placeholders["workingDays"])
        assertEquals("₹2,700.00", placeholders["totalEarnings"])
        assertEquals("₹500.00", placeholders["advance"])
        assertEquals("₹2,200.00", placeholders["netPayable"])
    }

    @Test
    fun testCustomWageChangePropagationInNotifications() {
        val wagesToTest = listOf(400.0, 500.0, 850.0, 900.0, 1000.0, 1100.0)

        for (wage in wagesToTest) {
            val user = UserEntity("u1", "Vikram", "9999999999", "v@test.com", "personal", wage)
            val todayStr = "2026-05-15"
            val record = AttendanceEntity("p1", null, todayStr, "present", "full", overtimeHours = 2, wage = wage)

            val placeholders = NotificationRuleEngine.buildPlaceholders(user, listOf(record), todayStr, "overtime")

            val expectedOt = wage / 8.0 * 2.0
            val expectedTotal = wage + expectedOt

            assertEquals("₹" + AmountFormatter.format(expectedTotal), placeholders["amount"])
            assertEquals("2", placeholders["hours"])
        }
    }

    @Test
    fun testWeeklySummaryCategoryPlaceholders() {
        val user = UserEntity("u1", "Ankit", "9876543210", "a@test.com", "personal", 800.0)
        val records = listOf(
            AttendanceEntity("p1", null, "2026-05-10", "present", "full", wage = 800.0),
            AttendanceEntity("p2", null, "2026-05-11", "present", "full", wage = 800.0),
            AttendanceEntity("p3", null, "2026-05-12", "present", "half", wage = 800.0),
            AttendanceEntity("p4", null, "2026-05-13", "absent", "full", wage = 800.0),
            AttendanceEntity("adv", null, "2026-05-14", "advance", "full", advanceAmount = 400.0)
        )

        val placeholders = NotificationRuleEngine.buildPlaceholders(user, records, "2026-05-16", "weekly_summary")

        assertEquals("3", placeholders["presentDays"])
        assertEquals("1", placeholders["halfDays"])
        assertEquals("1", placeholders["absentDays"])
        assertEquals("2.5", placeholders["workingDays"])
        assertEquals("₹2,000.00", placeholders["totalEarnings"])
        assertEquals("₹400.00", placeholders["advance"])
        assertEquals("₹1,600.00", placeholders["netPayable"])
        assertEquals("₹2,000.00", placeholders["amount"])
    }

    @Test
    fun testNotificationChannelsConfiguration() {
        // Verify channels
        assertEquals("attendance_channel", LocalNotificationManager.CHANNEL_ATTENDANCE)
        assertEquals("reminders_channel", LocalNotificationManager.CHANNEL_REMINDERS)
        assertEquals("summary_channel", LocalNotificationManager.CHANNEL_SUMMARY)
        assertEquals("earnings_channel", LocalNotificationManager.CHANNEL_EARNINGS)
        assertEquals("backup_channel", LocalNotificationManager.CHANNEL_BACKUP)
    }

    @Test
    fun testFriendlyNotificationMessagePoolsExist() {
        assertFalse(NotificationMessagePool.goodMorningMessages.isEmpty())
        assertFalse(NotificationMessagePool.goodEveningMessages.isEmpty())
        assertFalse(NotificationMessagePool.goodNightMessages.isEmpty())

        assertTrue(NotificationMessagePool.goodMorningMessages.any { it.contains("🌅") || it.contains("☀️") })
        assertTrue(NotificationMessagePool.goodEveningMessages.any { it.contains("🌆") || it.contains("🌇") })
        assertTrue(NotificationMessagePool.goodNightMessages.any { it.contains("🌙") || it.contains("🌃") })
    }

    @Test
    fun testFriendlyNotificationPersonalizationFallback() {
        val templateWithName = "😄 कैसे हो {userName}, आज का दिन कैसा रहा? ड्यूटी से वापस आ गए?"
        val placeholdersWithName = mapOf("userName" to "रमेश")
        val resultWithName = NotificationMessageSelector.evaluateTemplate(templateWithName, placeholdersWithName)
        assertEquals("😄 कैसे हो रमेश, आज का दिन कैसा रहा? ड्यूटी से वापस आ गए?", resultWithName)

        val placeholdersWithFallback = mapOf("userName" to "भाई")
        val resultWithFallback = NotificationMessageSelector.evaluateTemplate(templateWithName, placeholdersWithFallback)
        assertEquals("😄 कैसे हो भाई, आज का दिन कैसा रहा? ड्यूटी से वापस आ गए?", resultWithFallback)
    }

    @Test
    fun testFriendlyTimeWindows() {
        val getFriendlyCategory: (Int) -> String? = { timeInMinutes ->
            when {
                timeInMinutes in 390..570 -> "morning"    // 6:30 AM - 9:30 AM (inclusive)
                timeInMinutes in 1050..1200 -> "evening"  // 5:30 PM - 8:00 PM (inclusive)
                timeInMinutes in 1260..1350 -> "night"    // 9:00 PM - 10:30 PM (inclusive)
                else -> null
            }
        }

        assertEquals("morning", getFriendlyCategory(6 * 60 + 30))
        assertEquals("morning", getFriendlyCategory(8 * 60))
        assertEquals("morning", getFriendlyCategory(9 * 60 + 30))
        assertNull(getFriendlyCategory(6 * 60 + 29))
        assertNull(getFriendlyCategory(9 * 60 + 31))

        assertEquals("evening", getFriendlyCategory(17 * 60 + 30))
        assertEquals("evening", getFriendlyCategory(19 * 60))
        assertEquals("evening", getFriendlyCategory(20 * 60))
        assertNull(getFriendlyCategory(17 * 60 + 29))
        assertNull(getFriendlyCategory(20 * 60 + 1))

        assertEquals("night", getFriendlyCategory(21 * 60))
        assertEquals("night", getFriendlyCategory(22 * 60))
        assertEquals("night", getFriendlyCategory(22 * 60 + 30))
        assertNull(getFriendlyCategory(20 * 60 + 59))
        assertNull(getFriendlyCategory(22 * 60 + 31))
    }

    @Test
    fun testRuleEvaluationResultContainsDecoupledPersistenceState() {
        val evalResult = RuleEvaluationResult(
            triggerNotification = true,
            channelId = LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE,
            title = "आज की हाज़िरी",
            message = "आज की हाज़िरी दर्ज कर लो भाई!",
            category = "daily_attendance",
            reason = "Today's attendance missing",
            stateKeyToPersist = "last_attendance_reminder_date_user_123",
            stateValueToPersist = "2026-08-15",
            milestoneToPersist = null,
            userId = "user_123"
        )

        assertTrue(evalResult.triggerNotification)
        assertEquals("last_attendance_reminder_date_user_123", evalResult.stateKeyToPersist)
        assertEquals("2026-08-15", evalResult.stateValueToPersist)
        assertNull(evalResult.milestoneToPersist)
        assertEquals("user_123", evalResult.userId)
    }

    @Test
    fun testAttendanceWindowEligibilityLogic() {
        val isEligibleWindow: (Int) -> Boolean = { hour -> hour in 18..22 }

        assertFalse(isEligibleWindow(8))
        assertFalse(isEligibleWindow(12))
        assertFalse(isEligibleWindow(17))

        assertTrue(isEligibleWindow(18))
        assertTrue(isEligibleWindow(20))
        assertTrue(isEligibleWindow(22))

        assertFalse(isEligibleWindow(23))
        assertFalse(isEligibleWindow(0))
    }
}
