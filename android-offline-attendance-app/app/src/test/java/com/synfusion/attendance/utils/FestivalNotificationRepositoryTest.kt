package com.synfusion.attendance.utils

import org.junit.Assert.*
import org.junit.Test

class FestivalNotificationRepositoryTest {

    private val requiredFestivals = listOf(
        "diwali",
        "chhath_puja",
        "holi",
        "teachers_day",
        "eid_ul_fitr",
        "maha_shivratri",
        "buddha_purnima",
        "makar_sankranti",
        "republic_day",
        "independence_day",
        "janmashtami",
        "raksha_bandhan",
        "ganesh_chaturthi",
        "onam",
        "gandhi_jayanti",
        "dussehra",
        "childrens_day",
        "guru_nanak_jayanti",
        "christmas",
        "mahavir_jayanti",
        "ram_navami"
    )

    @Test
    fun testAll21FestivalsHaveTemplatePools() {
        val supportedIds = FestivalNotificationRepository.getAllSupportedFestivalIds()

        for (festId in requiredFestivals) {
            assertTrue("Supported festivals list must contain '$festId'", supportedIds.contains(festId))

            val dayTemplates = FestivalNotificationRepository.getTemplatesForFestival(festId, isUpcoming = false)
            assertTrue("Festival '$festId' must have >= 10 day templates", dayTemplates.size >= 10)

            for (tpl in dayTemplates) {
                assertFalse("Title for $festId (${tpl.id}) must not be blank", tpl.title.isBlank())
                assertFalse("Body for $festId (${tpl.id}) must not be blank", tpl.body.isBlank())
            }
        }
    }

    @Test
    fun testFestivalIdNormalization() {
        assertEquals("diwali", FestivalNotificationRepository.normalizeFestivalId("diwali_2024"))
        assertEquals("diwali", FestivalNotificationRepository.normalizeFestivalId("diwali_2026"))
        assertEquals("diwali", FestivalNotificationRepository.normalizeFestivalId("diwali"))
        assertEquals("chhath_puja", FestivalNotificationRepository.normalizeFestivalId("chhath_puja_2025"))
        assertEquals("maha_shivratri", FestivalNotificationRepository.normalizeFestivalId("maha_shivratri_2027"))
    }

    @Test
    fun testChhathPujaTemplatesContainNoFireworks() {
        val chhathTemplates = FestivalNotificationRepository.getTemplatesForFestival("chhath_puja", isUpcoming = false)
        assertTrue(chhathTemplates.isNotEmpty())

        val fireworkEmojis = listOf("🎇", "🎆", "💥")
        for (tpl in chhathTemplates) {
            for (emoji in fireworkEmojis) {
                assertFalse(
                    "Chhath Puja template (${tpl.id}) should not contain Diwali firework emoji '$emoji'",
                    tpl.title.contains(emoji) || tpl.body.contains(emoji)
                )
            }
            assertFalse("Chhath Puja template (${tpl.id}) should not mention fireworks", tpl.body.lowercase().contains("firework"))
        }
    }

    @Test
    fun testDiwaliTemplatesContainFestiveLightEmojis() {
        val diwaliTemplates = FestivalNotificationRepository.getTemplatesForFestival("diwali", isUpcoming = false)
        assertTrue(diwaliTemplates.isNotEmpty())

        val lightEmojis = listOf("🪔", "✨", "🌟", "🏮", "🎇")
        var containsLightEmojiCount = 0
        for (tpl in diwaliTemplates) {
            if (lightEmojis.any { tpl.title.contains(it) || tpl.body.contains(it) }) {
                containsLightEmojiCount++
            }
        }
        assertEquals("All Diwali templates should feature festive light emojis", diwaliTemplates.size, containsLightEmojiCount)
    }

    @Test
    fun testUpcomingAndDayTemplateFiltering() {
        val upcomingHoli = FestivalNotificationRepository.getTemplatesForFestival("holi", isUpcoming = true)
        val dayHoli = FestivalNotificationRepository.getTemplatesForFestival("holi", isUpcoming = false)

        assertTrue("Holi upcoming templates must not be empty", upcomingHoli.isNotEmpty())
        assertTrue("Holi day templates must not be empty", dayHoli.isNotEmpty())

        assertTrue(upcomingHoli.all { it.isUpcoming })
        assertTrue(dayHoli.all { !it.isUpcoming })
    }

    @Test
    fun testPlaceholderEvaluationInFestivalTemplates() {
        val template = FestivalNotificationTemplate("t1", "🪔 Happy Diwali {userName}!", "Roshni, khushiyan aur positive vibes for {userName} ✨")
        val placeholders = mapOf("userName" to "Aman")

        val title = template.title.replace("{userName}", placeholders["userName"] ?: "")
        val body = template.body.replace("{userName}", placeholders["userName"] ?: "")

        assertEquals("🪔 Happy Diwali Aman!", title)
        assertEquals("Roshni, khushiyan aur positive vibes for Aman ✨", body)
    }
}
