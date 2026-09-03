package com.synfusion.attendance.data

import org.junit.Assert.*
import org.junit.Test

class FestivalRepositoryTest {

    @Test
    fun testFixedAnnualFestivalDetection() {
        // Teachers' Day is 5th September (fixed year = null)
        val events2024 = FestivalRepository.getEventsForDate(5, 9, 2024)
        val events2026 = FestivalRepository.getEventsForDate(5, 9, 2026)

        assertTrue(events2024.any { it.id == "teachers_day" })
        assertTrue(events2026.any { it.id == "teachers_day" })

        val special2026 = FestivalRepository.getTodaySpecialHomeEvent(5, 9, 2026)
        assertNotNull(special2026)
        assertEquals("teachers_day", special2026?.id)
        assertEquals("Happy Teachers' Day!", special2026?.greeting)
        assertEquals(FestivalCategory.SPECIAL_HOME_EVENT, special2026?.category)
    }

    @Test
    fun testMoveableFestivalYearAwareness() {
        // Diwali 2024: 31st October
        val diwali2024 = FestivalRepository.getTodaySpecialHomeEvent(31, 10, 2024)
        assertNotNull(diwali2024)
        assertEquals("diwali_2024", diwali2024?.id)

        // Diwali 2026: 8th November
        val diwali2026 = FestivalRepository.getTodaySpecialHomeEvent(8, 11, 2026)
        assertNotNull(diwali2026)
        assertEquals("diwali_2026", diwali2026?.id)

        // 31st Oct 2026 should NOT be Diwali 2026
        val notDiwali2026 = FestivalRepository.getTodaySpecialHomeEvent(31, 10, 2026)
        assertNotEquals("diwali_2026", notDiwali2026?.id)
    }

    @Test
    fun testPriorityResolution() {
        // Test date with multiple events
        val events = FestivalRepository.getEventsForDate(15, 8, 2026) // Independence Day
        assertTrue(events.isNotEmpty())
        assertEquals("independence_day", events.first().id)
    }

    @Test
    fun testChhathPujaVisualIdentity() {
        // Chhath Puja 2026 is Nov 15, 2026
        val chhath = FestivalRepository.getTodaySpecialHomeEvent(15, 11, 2026)
        assertNotNull(chhath)
        assertEquals("chhath_puja_2026", chhath?.id)
        assertEquals(FestivalAnimationFamily.SUNRISE_RIVER, chhath?.animationFamily)
        assertEquals("Happy Chhath Puja!", chhath?.greeting)
    }

    @Test
    fun testNormalDayReturnsNoSpecialEvent() {
        // Random normal date (e.g. 10th May 2026)
        val normalEvent = FestivalRepository.getTodaySpecialHomeEvent(10, 5, 2026)
        assertNull(normalEvent)
    }
}
