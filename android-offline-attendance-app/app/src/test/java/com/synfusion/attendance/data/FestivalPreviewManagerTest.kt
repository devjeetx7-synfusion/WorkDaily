package com.synfusion.attendance.data

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FestivalPreviewManagerTest {

    @Before
    fun setUp() {
        FestivalPreviewManager.clearPreview()
    }

    @After
    fun tearDown() {
        FestivalPreviewManager.clearPreview()
    }

    @Test
    fun testPreviewActivationAndClearing() {
        assertFalse(FestivalPreviewManager.isPreviewActive())
        assertNull(FestivalPreviewManager.activePreviewEvent.value)

        val diwali = FestivalRepository.getSpecialHomeEventsForPreview().first { it.name == "Diwali" }
        FestivalPreviewManager.setPreviewFestival(diwali)

        assertTrue(FestivalPreviewManager.isPreviewActive())
        assertEquals("Diwali", FestivalPreviewManager.activePreviewEvent.value?.name)

        FestivalPreviewManager.clearPreview()
        assertFalse(FestivalPreviewManager.isPreviewActive())
        assertNull(FestivalPreviewManager.activePreviewEvent.value)
    }

    @Test
    fun testPriorityResolution() {
        // Normal day (10th May 2026) with no preview override
        val normalDayEvent = FestivalPreviewManager.getEffectiveSpecialHomeEvent(10, 5, 2026)
        assertNull(normalDayEvent)

        // Real festival day (26th Jan 2026 - Republic Day) with no preview override
        val republicDayEvent = FestivalPreviewManager.getEffectiveSpecialHomeEvent(26, 1, 2026)
        assertNotNull(republicDayEvent)
        assertEquals("republic_day", republicDayEvent?.id)

        // Set preview override to Holi
        val holi = FestivalRepository.getSpecialHomeEventsForPreview().first { it.name == "Holi" }
        FestivalPreviewManager.setPreviewFestival(holi)

        // On normal day -> returns Holi
        val normalDayOverride = FestivalPreviewManager.getEffectiveSpecialHomeEvent(10, 5, 2026)
        assertNotNull(normalDayOverride)
        assertEquals("Holi", normalDayOverride?.name)

        // On Republic Day -> returns Holi (Preview > Real Special Festival)
        val republicDayOverride = FestivalPreviewManager.getEffectiveSpecialHomeEvent(26, 1, 2026)
        assertNotNull(republicDayOverride)
        assertEquals("Holi", republicDayOverride?.name)
    }

    @Test
    fun testSwitchingBetweenFestivals() {
        val festivals = FestivalRepository.getSpecialHomeEventsForPreview()
        val diwali = festivals.first { it.name == "Diwali" }
        val chhath = festivals.first { it.name == "Chhath Puja" }
        val holi = festivals.first { it.name == "Holi" }

        FestivalPreviewManager.setPreviewFestival(diwali)
        assertEquals("Diwali", FestivalPreviewManager.activePreviewEvent.value?.name)

        // Immediate switch to Chhath Puja
        FestivalPreviewManager.setPreviewFestival(chhath)
        assertEquals("Chhath Puja", FestivalPreviewManager.activePreviewEvent.value?.name)

        // Immediate switch to Holi
        FestivalPreviewManager.setPreviewFestival(holi)
        assertEquals("Holi", FestivalPreviewManager.activePreviewEvent.value?.name)

        FestivalPreviewManager.clearPreview()
        assertNull(FestivalPreviewManager.activePreviewEvent.value)
    }

    @Test
    fun testDynamicPreviewList() {
        val previewFestivals = FestivalRepository.getSpecialHomeEventsForPreview()
        assertTrue(previewFestivals.isNotEmpty())

        // All items must be SPECIAL_HOME_EVENT
        assertTrue(previewFestivals.all { it.category == FestivalCategory.SPECIAL_HOME_EVENT })

        // Names must be distinct
        val names = previewFestivals.map { it.name }
        assertEquals(names.size, names.distinct().size)

        // Chhath Puja, Diwali, Teachers' Day, Holi must be present
        assertTrue(names.contains("Chhath Puja"))
        assertTrue(names.contains("Diwali"))
        assertTrue(names.contains("Teachers' Day"))
        assertTrue(names.contains("Holi"))
    }
}
