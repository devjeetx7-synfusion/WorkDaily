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
}
