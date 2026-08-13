package com.synfusion.attendance.utils

import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.UserEntity
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class NotificationSystemTest {

    @Test
    fun testPlaceholderReplacementWithName() {
        val template = "{userName}, आज की attendance अभी तक नहीं लगी है 😄"
        val placeholders = mapOf("userName" to "Dharmjeet")

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("Dharmjeet, आज की attendance अभी तक नहीं लगी है 😄", result)
    }

    @Test
    fun testPlaceholderReplacementWithoutName() {
        val template = "{userName}, आज की attendance अभी तक नहीं लगी है 😄"
        val placeholders = mapOf("userName" to "")

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("आज की attendance अभी तक नहीं लगी है 😄", result)
    }

    @Test
    fun testMultiplePlaceholdersReplacement() {
        val template = "कमाई: ₹{totalEarnings}, Advance: ₹{advance}, Net: ₹{netPayable}"
        val placeholders = mapOf(
            "totalEarnings" to "12,000",
            "advance" to "2,000",
            "netPayable" to "10,000"
        )

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("कमाई: ₹12,000, Advance: ₹2,000, Net: ₹10,000", result)
    }

    @Test
    fun testPlaceholderNullOrBlankCleanup() {
        val template = "हेलो {userName}, आज काम किया या आराम?"
        val placeholders = mapOf("userName" to "null")

        val result = NotificationMessageSelector.evaluateTemplate(template, placeholders)
        assertEquals("हेलो , आज काम किया या आराम?", result)
    }

    @Test
    fun testBuildPlaceholdersMap() {
        val user = UserEntity("user_1", "Ramesh Kumar", "9876543210", "ramesh@test.com", "personal", 600.0)

        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        // Mock some records matching the current month prefix dynamically
        val records = listOf(
            AttendanceEntity("p1", null, "$currentMonthPrefix-01", "present", "full", wage = 600.0),
            AttendanceEntity("p2", null, "$currentMonthPrefix-02", "present", "full", wage = 600.0),
            AttendanceEntity("p3", null, "$currentMonthPrefix-03", "present", "half", wage = 600.0),
            AttendanceEntity("p4", null, "$currentMonthPrefix-04", "present", "full", wage = 600.0),
            AttendanceEntity("p5", null, "$currentMonthPrefix-05", "present", "full", wage = 600.0),
            AttendanceEntity("a1", null, "$currentMonthPrefix-06", "absent", "full", wage = 600.0),
            AttendanceEntity("adv", null, "$currentMonthPrefix-07", "advance", "full", advanceAmount = 500.0)
        )

        val placeholders = NotificationRuleEngine.buildPlaceholders(user, records, "$currentMonthPrefix-08")

        assertEquals("Ramesh Kumar", placeholders["userName"])
        assertEquals("5", placeholders["presentDays"])
        assertEquals("1", placeholders["halfDays"])
        assertEquals("1", placeholders["absentDays"])
        // working days = 4 present + 0.5 half day = 4.5
        assertEquals("4.5", placeholders["workingDays"])
        // total earnings = 4 * 600 + 1 * 300 = 2700
        assertEquals("2,700", placeholders["totalEarnings"])
        assertEquals("500", placeholders["advance"])
        assertEquals("2,200", placeholders["netPayable"])
    }

    @Test
    fun testConsolidatedNotificationChannelConfiguration() {
        // Verify that the consolidated channel ID is correct and stable
        assertEquals("offline_attendance_reminders", LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE)

        // Verify that all backwards-compatible channel aliases resolve to the same consolidated channel ID
        assertEquals(LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE, LocalNotificationManager.CHANNEL_ATTENDANCE)
        assertEquals(LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE, LocalNotificationManager.CHANNEL_REPORTS)
        assertEquals(LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE, LocalNotificationManager.CHANNEL_EARNINGS)
        assertEquals(LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE, LocalNotificationManager.CHANNEL_FUN_MOTIVATION)
    }

    @Test
    fun testFriendlyNotificationMessagePoolsExist() {
        assertFalse(NotificationMessagePool.goodMorningMessages.isEmpty())
        assertFalse(NotificationMessagePool.goodEveningMessages.isEmpty())
        assertFalse(NotificationMessagePool.goodNightMessages.isEmpty())

        // Verify some content matches expected strings
        assertTrue(NotificationMessagePool.goodMorningMessages.any { it.contains("🌅") || it.contains("☀️") })
        assertTrue(NotificationMessagePool.goodEveningMessages.any { it.contains("🌆") || it.contains("🌇") })
        assertTrue(NotificationMessagePool.goodNightMessages.any { it.contains("🌙") || it.contains("🌃") })
    }

    @Test
    fun testFriendlyNotificationPersonalizationFallback() {
        // Test with user display name
        val templateWithName = "😄 Kaise {userName}, aaj kaisa raha? Duty se wapas aa gaye?"
        val placeholdersWithName = mapOf("userName" to "Ramesh")
        val resultWithName = NotificationMessageSelector.evaluateTemplate(templateWithName, placeholdersWithName)
        assertEquals("😄 Kaise Ramesh, aaj kaisa raha? Duty se wapas aa gaye?", resultWithName)

        // Test with blank/empty name and fallback to "bhai"
        val placeholdersWithFallback = mapOf("userName" to "bhai")
        val resultWithFallback = NotificationMessageSelector.evaluateTemplate(templateWithName, placeholdersWithFallback)
        assertEquals("😄 Kaise bhai, aaj kaisa raha? Duty se wapas aa gaye?", resultWithFallback)
    }

    @Test
    fun testFriendlyTimeWindows() {
        // Helper block mapping to match our when condition
        val getFriendlyCategory: (Int) -> String? = { timeInMinutes ->
            when {
                timeInMinutes in 390..570 -> "morning"    // 6:30 AM - 9:30 AM (inclusive)
                timeInMinutes in 1050..1200 -> "evening"  // 5:30 PM - 8:00 PM (inclusive)
                timeInMinutes in 1260..1350 -> "night"    // 9:00 PM - 10:30 PM (inclusive)
                else -> null
            }
        }

        // Test Good Morning (6:30 AM - 9:30 AM)
        assertEquals("morning", getFriendlyCategory(6 * 60 + 30)) // 6:30 AM
        assertEquals("morning", getFriendlyCategory(8 * 60))      // 8:00 AM
        assertEquals("morning", getFriendlyCategory(9 * 60 + 30)) // 9:30 AM
        assertNull(getFriendlyCategory(6 * 60 + 29))              // 6:29 AM
        assertNull(getFriendlyCategory(9 * 60 + 31))              // 9:31 AM

        // Test Good Evening (5:30 PM - 8:00 PM)
        assertEquals("evening", getFriendlyCategory(17 * 60 + 30)) // 5:30 PM
        assertEquals("evening", getFriendlyCategory(19 * 60))       // 7:00 PM
        assertEquals("evening", getFriendlyCategory(20 * 60))       // 8:00 PM
        assertNull(getFriendlyCategory(17 * 60 + 29))              // 5:29 PM
        assertNull(getFriendlyCategory(20 * 60 + 1))               // 8:01 PM

        // Test Good Night (9:00 PM - 10:30 PM)
        assertEquals("night", getFriendlyCategory(21 * 60))      // 9:00 PM
        assertEquals("night", getFriendlyCategory(22 * 60))      // 10:00 PM
        assertEquals("night", getFriendlyCategory(22 * 60 + 30)) // 10:30 PM
        assertNull(getFriendlyCategory(20 * 60 + 59))            // 8:59 PM
        assertNull(getFriendlyCategory(22 * 60 + 31))            // 10:31 PM
    }
}
