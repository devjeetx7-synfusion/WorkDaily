package com.synfusion.attendance.data

object FestivalRepository {

    private val festivalList: List<FestivalEvent> = listOf(
        // ==========================================
        // FIXED ANNUAL SPECIAL HOME EVENTS (year = null)
        // ==========================================
        FestivalEvent(
            id = "makar_sankranti",
            name = "Makar Sankranti",
            day = 14, month = 1, year = null,
            category = FestivalCategory.SPECIAL_HOME_EVENT,
            isPublicHoliday = false,
            priority = 90,
            greeting = "Happy Makar Sankranti!",
            description = "Harvest festival dedicated to the Sun God Surya.",
            emoji = "🪁",
            animationFamily = FestivalAnimationFamily.KITE_FLY,
            intensity = FestivalIntensity.MEDIUM,
            primaryColorHex = 0xFFFF9800,   // Sun Gold
            secondaryColorHex = 0xFF03A9F4, // Sky Blue
            accentColorHex = 0xFFFFEB3B,    // Sunlight Yellow
            avoidText = "Generic confetti, heavy fireworks"
        , heroBgGradientTopHex = 0xFF0C1A2B, heroBgGradientBottomHex = 0xFF1E3A8A),
        FestivalEvent(
            id = "republic_day",
            name = "Republic Day",
            day = 26, month = 1, year = null,
            category = FestivalCategory.SPECIAL_HOME_EVENT,
            isPublicHoliday = true,
            priority = 100,
            greeting = "Happy Republic Day!",
            description = "Honors the day the Constitution of India came into effect in 1950.",
            emoji = "🇮🇳",
            animationFamily = FestivalAnimationFamily.PATRIOTIC_RIBBON,
            intensity = FestivalIntensity.MEDIUM,
            primaryColorHex = 0xFFFF671F,   // India Saffron
            secondaryColorHex = 0xFF046A38, // India Green
            accentColorHex = 0xFF06038D,    // Ashoka Navy
            avoidText = "Party confetti, cartoonish party effects"
        , heroBgGradientTopHex = 0xFF081C15, heroBgGradientBottomHex = 0xFF1B2A4A),
        FestivalEvent(
            id = "teachers_day",
            name = "Teachers' Day",
            day = 5, month = 9, year = null,
            category = FestivalCategory.SPECIAL_HOME_EVENT,
            isPublicHoliday = false,
            priority = 85,
            greeting = "Happy Teachers' Day!",
            description = "Celebrating teachers and educators in honor of Dr. S. Radhakrishnan.",
            emoji = "📚",
            animationFamily = FestivalAnimationFamily.EDUCATION_FLOAT,
            intensity = FestivalIntensity.MEDIUM,
            primaryColorHex = 0xFF1E88E5,   // Educational Blue
            secondaryColorHex = 0xFF43A047, // Knowledge Emerald
            accentColorHex = 0xFFFFB300,    // Wisdom Gold
            avoidText = "Religious imagery, fireworks, generic party confetti"
        , heroBgGradientTopHex = 0xFF0F172A, heroBgGradientBottomHex = 0xFF1E1B4B),
        FestivalEvent(
            id = "gandhi_jayanti",
            name = "Gandhi Jayanti",
            day = 2, month = 10, year = null,
            category = FestivalCategory.SPECIAL_HOME_EVENT,
            isPublicHoliday = true,
            priority = 95,
            greeting = "Happy Gandhi Jayanti!",
            description = "Birth anniversary of Mahatma Gandhi, promoting non-violence and peace.",
            emoji = "👓",
            animationFamily = FestivalAnimationFamily.PEACE_LIGHT,
            intensity = FestivalIntensity.SUBTLE,
            primaryColorHex = 0xFF8D6E63,   // Khadi Earth Tone
            secondaryColorHex = 0xFF5D4037, // Warm Brown
            accentColorHex = 0xFFFFF8E1,    // Peaceful Warm Cream
            avoidText = "Loud celebration, fireworks, colorful confetti"
        , heroBgGradientTopHex = 0xFF1C1917, heroBgGradientBottomHex = 0xFF3D2C24),
        FestivalEvent(
            id = "childrens_day",
            name = "Children's Day",
            day = 14, month = 11, year = null,
            category = FestivalCategory.SPECIAL_HOME_EVENT,
            isPublicHoliday = false,
            priority = 80,
            greeting = "Happy Children's Day!",
            description = "Celebrating youth, innocence, and the birth of Pandit Jawaharlal Nehru.",
            emoji = "🎈",
            animationFamily = FestivalAnimationFamily.YOUTHFUL_PLAY,
            intensity = FestivalIntensity.MEDIUM,
            primaryColorHex = 0xFFEC407A,   // Joyful Pink
            secondaryColorHex = 0xFF29B6F6, // Bright Sky
            accentColorHex = 0xFFFFCA28,    // Cheerful Amber
            avoidText = "Religious festival styling"
        , heroBgGradientTopHex = 0xFF180E29, heroBgGradientBottomHex = 0xFF4A1245),
        FestivalEvent(
            id = "christmas",
            name = "Christmas",
            day = 25, month = 12, year = null,
            category = FestivalCategory.SPECIAL_HOME_EVENT,
            isPublicHoliday = true,
            priority = 95,
            greeting = "Merry Christmas!",
            description = "Celebrating peace, joy, and the birth of Jesus Christ.",
            emoji = "🎄",
            animationFamily = FestivalAnimationFamily.SNOW_TWINKLE,
            intensity = FestivalIntensity.STRONG,
            primaryColorHex = 0xFFD32F2F,   // Christmas Red
            secondaryColorHex = 0xFF388E3C, // Pine Green
            accentColorHex = 0xFFFFD700,    // Star Gold
            avoidText = "Diwali-style golden fireworks"
        , heroBgGradientTopHex = 0xFF1A0505, heroBgGradientBottomHex = 0xFF450A0A),
        FestivalEvent(
            id = "independence_day",
            name = "Independence Day",
            day = 15, month = 8, year = null,
            category = FestivalCategory.SPECIAL_HOME_EVENT,
            isPublicHoliday = true,
            priority = 100,
            greeting = "Happy Independence Day!",
            description = "Commemorates the nation's independence in 1947.",
            emoji = "🇮🇳",
            animationFamily = FestivalAnimationFamily.PATRIOTIC_RIBBON,
            intensity = FestivalIntensity.MEDIUM,
            primaryColorHex = 0xFFFF671F,   // Saffron
            secondaryColorHex = 0xFF046A38, // India Green
            accentColorHex = 0xFFFFFFFF,    // Pure White
            avoidText = "Overusing flag imagery, generic party animation"
        , heroBgGradientTopHex = 0xFF081C15, heroBgGradientBottomHex = 0xFF1B2A4A),

        // ==========================================
        // MOVEABLE SPECIAL HOME EVENTS (Year-Aware)
        // ==========================================

        // 2024 Moveable
        FestivalEvent("maha_shivratri_2024", "Maha Shivratri", 8, 3, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Maha Shivratri!", "Reverence for Lord Shiva.", "🕉️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF3F51B5, 0xFF7E57C2, 0xFFE040FB, "Colorful party effects, fireworks", heroBgGradientTopHex = 0xFF09090B, heroBgGradientBottomHex = 0xFF1E1B4B),
        FestivalEvent("holi_2024", "Holi", 25, 3, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Holi!", "Festival of colors celebrating spring and joy.", "🎨", FestivalAnimationFamily.COLOR_BURST, FestivalIntensity.STRONG, 0xFFE91E63, 0xFF00BCD4, 0xFFFFEB3B, "Dark religious treatment, excessive screen obstruction", heroBgGradientTopHex = 0xFF1E0B2B, heroBgGradientBottomHex = 0xFF5B21B6),
        FestivalEvent("eid_ul_fitr_2024", "Eid-ul-Fitr", 11, 4, 2024, FestivalCategory.SPECIAL_HOME_EVENT, true, 95, "Eid Mubarak!", "Marking the end of Ramadan fasting.", "🌙", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.MEDIUM, 0xFF00897B, 0xFF0288D1, 0xFFFFD54F, "Generic rainbow gradients, loud fireworks", heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("ram_navami_2024", "Ram Navami", 17, 4, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Ram Navami!", "Celebrating the birth of Lord Rama.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.MEDIUM, 0xFFFF9800, 0xFFE65100, 0xFFFFD54F, "Party-style confetti", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("mahavir_jayanti_2024", "Mahavir Jayanti", 21, 4, 2024, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Mahavir Jayanti!", "Birth anniversary of Lord Mahavira.", "☸️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFFB8C00, 0xFFF57C00, 0xFFFFF8E1, "Aggressive celebration effects, fireworks", heroBgGradientTopHex = 0xFF1C1005, heroBgGradientBottomHex = 0xFF422006),
        FestivalEvent("buddha_purnima_2024", "Buddha Purnima", 23, 5, 2024, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Buddha Purnima!", "Enlightenment and peace of Gautama Buddha.", "🪷", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFAB47BC, 0xFF7B1FA2, 0xFFE1BEE7, "Energetic confetti", heroBgGradientTopHex = 0xFF180E29, heroBgGradientBottomHex = 0xFF4A1245),
        FestivalEvent("raksha_bandhan_2024", "Raksha Bandhan", 19, 8, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Raksha Bandhan!", "Sacred bond between brothers and sisters.", "🧵", FestivalAnimationFamily.CIRCULAR_THREAD, FestivalIntensity.MEDIUM, 0xFFE91E63, 0xFFC2185B, 0xFFFFD54F, "Generic confetti overload", heroBgGradientTopHex = 0xFF220A1A, heroBgGradientBottomHex = 0xFF701A75),
        FestivalEvent("janmashtami_2024", "Janmashtami", 26, 8, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Janmashtami!", "Celebrating the birth of Lord Krishna.", "🪈", FestivalAnimationFamily.PLAYFUL_DIVINE, FestivalIntensity.MEDIUM, 0xFF1976D2, 0xFF0D47A1, 0xFFFFD54F, "Generic fireworks", heroBgGradientTopHex = 0xFF0B1026, heroBgGradientBottomHex = 0xFF1E2B58),
        FestivalEvent("ganesh_chaturthi_2024", "Ganesh Chaturthi", 7, 9, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Ganesh Chaturthi!", "Arrival of Lord Ganesha.", "🐘", FestivalAnimationFamily.FESTIVE_AUGMENT, FestivalIntensity.STRONG, 0xFFFF6F00, 0xFFE65100, 0xFFFFD54F, "Excessive loud fireworks", heroBgGradientTopHex = 0xFF2B0A00, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("onam_2024", "Onam", 15, 9, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Onam!", "Harvest festival of Kerala.", "🌾", FestivalAnimationFamily.FLORAL_PETALS, FestivalIntensity.MEDIUM, 0xFF43A047, 0xFF2E7D32, 0xFFFFD54F, "Diwali-style gold effects", heroBgGradientTopHex = 0xFF052E16, heroBgGradientBottomHex = 0xFF14532D),
        FestivalEvent("dussehra_2024", "Dussehra", 12, 10, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Dussehra!", "Triumph of good over evil.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFD84315, 0xFFFFD54F, "Unsafe fire imitation", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("diwali_2024", "Diwali", 31, 10, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 100, "Happy Diwali!", "Festival of lights and prosperity.", "🪔", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFFF6F00, 0xFFFFD54F, "Excessive fireworks covering UI", heroBgGradientTopHex = 0xFF1A0B2E, heroBgGradientBottomHex = 0xFF4C1D95),
        FestivalEvent("chhath_puja_2024", "Chhath Puja", 7, 11, 2024, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Chhath Puja!", "Devotion to Sun God Surya and holy river banks.", "🌅", FestivalAnimationFamily.SUNRISE_RIVER, FestivalIntensity.MEDIUM, 0xFFFF6F00, 0xFFE65100, 0xFF29B6F6, "Diwali fireworks, colorful party confetti", heroBgGradientTopHex = 0xFF2A0800, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("guru_nanak_jayanti_2024", "Guru Nanak Jayanti", 15, 11, 2024, FestivalCategory.SPECIAL_HOME_EVENT, true, 85, "Happy Guru Nanak Jayanti!", "Birth anniversary of Guru Nanak Dev Ji.", "🪯", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFFFA000, 0xFFFF6F00, 0xFFFFF8E1, "Party confetti", heroBgGradientTopHex = 0xFF1C1300, heroBgGradientBottomHex = 0xFF422C00),

        // 2025 Moveable
        FestivalEvent("maha_shivratri_2025", "Maha Shivratri", 26, 2, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Maha Shivratri!", "Reverence for Lord Shiva.", "🕉️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF3F51B5, 0xFF7E57C2, 0xFFE040FB, "Colorful party effects", heroBgGradientTopHex = 0xFF09090B, heroBgGradientBottomHex = 0xFF1E1B4B),
        FestivalEvent("holi_2025", "Holi", 14, 3, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Holi!", "Festival of colors.", "🎨", FestivalAnimationFamily.COLOR_BURST, FestivalIntensity.STRONG, 0xFFE91E63, 0xFF00BCD4, 0xFFFFEB3B, "Dark religious treatment", heroBgGradientTopHex = 0xFF1E0B2B, heroBgGradientBottomHex = 0xFF5B21B6),
        FestivalEvent("eid_ul_fitr_2025", "Eid-ul-Fitr", 31, 3, 2025, FestivalCategory.SPECIAL_HOME_EVENT, true, 95, "Eid Mubarak!", "Marking the end of Ramadan.", "🌙", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.MEDIUM, 0xFF00897B, 0xFF0288D1, 0xFFFFD54F, "Loud fireworks", heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("ram_navami_2025", "Ram Navami", 6, 4, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Ram Navami!", "Birth of Lord Rama.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.MEDIUM, 0xFFFF9800, 0xFFE65100, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("mahavir_jayanti_2025", "Mahavir Jayanti", 10, 4, 2025, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Mahavir Jayanti!", "Birth anniversary of Lord Mahavira.", "☸️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFFB8C00, 0xFFF57C00, 0xFFFFF8E1, "Fireworks", heroBgGradientTopHex = 0xFF1C1005, heroBgGradientBottomHex = 0xFF422006),
        FestivalEvent("buddha_purnima_2025", "Buddha Purnima", 12, 5, 2025, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Buddha Purnima!", "Enlightenment of Gautama Buddha.", "🪷", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFAB47BC, 0xFF7B1FA2, 0xFFE1BEE7, "Confetti", heroBgGradientTopHex = 0xFF180E29, heroBgGradientBottomHex = 0xFF4A1245),
        FestivalEvent("raksha_bandhan_2025", "Raksha Bandhan", 9, 8, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Raksha Bandhan!", "Sacred bond of brother and sister.", "🧵", FestivalAnimationFamily.CIRCULAR_THREAD, FestivalIntensity.MEDIUM, 0xFFE91E63, 0xFFC2185B, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF220A1A, heroBgGradientBottomHex = 0xFF701A75),
        FestivalEvent("janmashtami_2025", "Janmashtami", 16, 8, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Janmashtami!", "Birth of Lord Krishna.", "🪈", FestivalAnimationFamily.PLAYFUL_DIVINE, FestivalIntensity.MEDIUM, 0xFF1976D2, 0xFF0D47A1, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF0B1026, heroBgGradientBottomHex = 0xFF1E2B58),
        FestivalEvent("ganesh_chaturthi_2025", "Ganesh Chaturthi", 27, 8, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Ganesh Chaturthi!", "Arrival of Lord Ganesha.", "🐘", FestivalAnimationFamily.FESTIVE_AUGMENT, FestivalIntensity.STRONG, 0xFFFF6F00, 0xFFE65100, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF2B0A00, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("onam_2025", "Onam", 5, 9, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Onam!", "Harvest festival of Kerala.", "🌾", FestivalAnimationFamily.FLORAL_PETALS, FestivalIntensity.MEDIUM, 0xFF43A047, 0xFF2E7D32, 0xFFFFD54F, "Diwali effects", heroBgGradientTopHex = 0xFF052E16, heroBgGradientBottomHex = 0xFF14532D),
        FestivalEvent("dussehra_2025", "Dussehra", 2, 10, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Dussehra!", "Triumph of good over evil.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFD84315, 0xFFFFD54F, "Fire imitation", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("diwali_2025", "Diwali", 20, 10, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 100, "Happy Diwali!", "Festival of lights.", "🪔", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFFF6F00, 0xFFFFD54F, "Fireworks overlay", heroBgGradientTopHex = 0xFF1A0B2E, heroBgGradientBottomHex = 0xFF4C1D95),
        FestivalEvent("chhath_puja_2025", "Chhath Puja", 27, 10, 2025, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Chhath Puja!", "Holy devotion to Sun God Surya.", "🌅", FestivalAnimationFamily.SUNRISE_RIVER, FestivalIntensity.MEDIUM, 0xFFFF6F00, 0xFFE65100, 0xFF29B6F6, "Diwali fireworks", heroBgGradientTopHex = 0xFF2A0800, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("guru_nanak_jayanti_2025", "Guru Nanak Jayanti", 5, 11, 2025, FestivalCategory.SPECIAL_HOME_EVENT, true, 85, "Happy Guru Nanak Jayanti!", "Birth anniversary of Guru Nanak Dev Ji.", "🪯", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFFFA000, 0xFFFF6F00, 0xFFFFF8E1, "Party confetti", heroBgGradientTopHex = 0xFF1C1300, heroBgGradientBottomHex = 0xFF422C00),

        // 2026 Moveable
        FestivalEvent("maha_shivratri_2026", "Maha Shivratri", 15, 2, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Maha Shivratri!", "Reverence for Lord Shiva.", "🕉️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF3F51B5, 0xFF7E57C2, 0xFFE040FB, "Party effects", heroBgGradientTopHex = 0xFF09090B, heroBgGradientBottomHex = 0xFF1E1B4B),
        FestivalEvent("holi_2026", "Holi", 4, 3, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Holi!", "Festival of colors.", "🎨", FestivalAnimationFamily.COLOR_BURST, FestivalIntensity.STRONG, 0xFFE91E63, 0xFF00BCD4, 0xFFFFEB3B, "Dark treatment", heroBgGradientTopHex = 0xFF1E0B2B, heroBgGradientBottomHex = 0xFF5B21B6),
        FestivalEvent("eid_ul_fitr_2026", "Eid-ul-Fitr", 20, 3, 2026, FestivalCategory.SPECIAL_HOME_EVENT, true, 95, "Eid Mubarak!", "End of Ramadan fasting.", "🌙", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.MEDIUM, 0xFF00897B, 0xFF0288D1, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("ram_navami_2026", "Ram Navami", 27, 3, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Ram Navami!", "Birth of Lord Rama.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.MEDIUM, 0xFFFF9800, 0xFFE65100, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("mahavir_jayanti_2026", "Mahavir Jayanti", 31, 3, 2026, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Mahavir Jayanti!", "Birth anniversary of Lord Mahavira.", "☸️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFFB8C00, 0xFFF57C00, 0xFFFFF8E1, "Fireworks"),
        FestivalEvent("buddha_purnima_2026", "Buddha Purnima", 1, 5, 2026, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Buddha Purnima!", "Enlightenment of Gautama Buddha.", "🪷", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFAB47BC, 0xFF7B1FA2, 0xFFE1BEE7, "Confetti"),
        FestivalEvent("onam_2026", "Onam", 24, 8, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Onam!", "Harvest festival of Kerala.", "🌾", FestivalAnimationFamily.FLORAL_PETALS, FestivalIntensity.MEDIUM, 0xFF43A047, 0xFF2E7D32, 0xFFFFD54F, "Diwali effects", heroBgGradientTopHex = 0xFF052E16, heroBgGradientBottomHex = 0xFF14532D),
        FestivalEvent("raksha_bandhan_2026", "Raksha Bandhan", 28, 8, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Raksha Bandhan!", "Bond between brothers and sisters.", "🧵", FestivalAnimationFamily.CIRCULAR_THREAD, FestivalIntensity.MEDIUM, 0xFFE91E63, 0xFFC2185B, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF220A1A, heroBgGradientBottomHex = 0xFF701A75),
        FestivalEvent("janmashtami_2026", "Janmashtami", 4, 9, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Janmashtami!", "Birth of Lord Krishna.", "🪈", FestivalAnimationFamily.PLAYFUL_DIVINE, FestivalIntensity.MEDIUM, 0xFF1976D2, 0xFF0D47A1, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF0B1026, heroBgGradientBottomHex = 0xFF1E2B58),
        FestivalEvent("ganesh_chaturthi_2026", "Ganesh Chaturthi", 14, 9, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Ganesh Chaturthi!", "Celebration of Lord Ganesha.", "🐘", FestivalAnimationFamily.FESTIVE_AUGMENT, FestivalIntensity.STRONG, 0xFFFF6F00, 0xFFE65100, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF2B0A00, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("dussehra_2026", "Dussehra", 20, 10, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Dussehra!", "Vijayadashami celebration.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFD84315, 0xFFFFD54F, "Fire imitation", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("diwali_2026", "Diwali", 8, 11, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 100, "Happy Diwali!", "Festival of lights.", "🪔", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFFF6F00, 0xFFFFD54F, "Fireworks overlay", heroBgGradientTopHex = 0xFF1A0B2E, heroBgGradientBottomHex = 0xFF4C1D95),
        FestivalEvent("chhath_puja_2026", "Chhath Puja", 15, 11, 2026, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Chhath Puja!", "Sun worship at river banks.", "🌅", FestivalAnimationFamily.SUNRISE_RIVER, FestivalIntensity.MEDIUM, 0xFFFF6F00, 0xFFE65100, 0xFF29B6F6, "Diwali fireworks", heroBgGradientTopHex = 0xFF2A0800, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("guru_nanak_jayanti_2026", "Guru Nanak Jayanti", 24, 11, 2026, FestivalCategory.SPECIAL_HOME_EVENT, true, 85, "Happy Guru Nanak Jayanti!", "Birth anniversary of Guru Nanak Dev Ji.", "🪯", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFFFA000, 0xFFFF6F00, 0xFFFFF8E1, "Party confetti"),

        // 2027 Moveable
        FestivalEvent("maha_shivratri_2027", "Maha Shivratri", 6, 3, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Maha Shivratri!", "Reverence for Lord Shiva.", "🕉️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF3F51B5, 0xFF7E57C2, 0xFFE040FB, "Party effects", heroBgGradientTopHex = 0xFF09090B, heroBgGradientBottomHex = 0xFF1E1B4B),
        FestivalEvent("holi_2027", "Holi", 22, 3, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Holi!", "Festival of colors.", "🎨", FestivalAnimationFamily.COLOR_BURST, FestivalIntensity.STRONG, 0xFFE91E63, 0xFF00BCD4, 0xFFFFEB3B, "Dark treatment", heroBgGradientTopHex = 0xFF1E0B2B, heroBgGradientBottomHex = 0xFF5B21B6),
        FestivalEvent("eid_ul_fitr_2027", "Eid-ul-Fitr", 10, 3, 2027, FestivalCategory.SPECIAL_HOME_EVENT, true, 95, "Eid Mubarak!", "End of Ramadan fasting.", "🌙", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.MEDIUM, 0xFF00897B, 0xFF0288D1, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("ram_navami_2027", "Ram Navami", 15, 4, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Ram Navami!", "Birth of Lord Rama.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.MEDIUM, 0xFFFF9800, 0xFFE65100, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("buddha_purnima_2027", "Buddha Purnima", 20, 5, 2027, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Buddha Purnima!", "Enlightenment of Buddha.", "🪷", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFAB47BC, 0xFF7B1FA2, 0xFFE1BEE7, "Confetti"),
        FestivalEvent("raksha_bandhan_2027", "Raksha Bandhan", 17, 8, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Raksha Bandhan!", "Brother sister bond.", "🧵", FestivalAnimationFamily.CIRCULAR_THREAD, FestivalIntensity.MEDIUM, 0xFFE91E63, 0xFFC2185B, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF220A1A, heroBgGradientBottomHex = 0xFF701A75),
        FestivalEvent("janmashtami_2027", "Janmashtami", 25, 8, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Janmashtami!", "Birth of Lord Krishna.", "🪈", FestivalAnimationFamily.PLAYFUL_DIVINE, FestivalIntensity.MEDIUM, 0xFF1976D2, 0xFF0D47A1, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF0B1026, heroBgGradientBottomHex = 0xFF1E2B58),
        FestivalEvent("ganesh_chaturthi_2027", "Ganesh Chaturthi", 4, 9, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Ganesh Chaturthi!", "Arrival of Lord Ganesha.", "🐘", FestivalAnimationFamily.FESTIVE_AUGMENT, FestivalIntensity.STRONG, 0xFFFF6F00, 0xFFE65100, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF2B0A00, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("dussehra_2027", "Dussehra", 9, 10, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Dussehra!", "Vijayadashami celebration.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFD84315, 0xFFFFD54F, "Fire imitation", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("diwali_2027", "Diwali", 29, 10, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 100, "Happy Diwali!", "Festival of lights.", "🪔", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFFF6F00, 0xFFFFD54F, "Fireworks overlay", heroBgGradientTopHex = 0xFF1A0B2E, heroBgGradientBottomHex = 0xFF4C1D95),
        FestivalEvent("chhath_puja_2027", "Chhath Puja", 4, 11, 2027, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Chhath Puja!", "Sun worship.", "🌅", FestivalAnimationFamily.SUNRISE_RIVER, FestivalIntensity.MEDIUM, 0xFFFF6F00, 0xFFE65100, 0xFF29B6F6, "Fireworks", heroBgGradientTopHex = 0xFF2A0800, heroBgGradientBottomHex = 0xFF7C2D12),

        // 2028 Moveable
        FestivalEvent("maha_shivratri_2028", "Maha Shivratri", 25, 2, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Maha Shivratri!", "Reverence for Lord Shiva.", "🕉️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF3F51B5, 0xFF7E57C2, 0xFFE040FB, "Party effects", heroBgGradientTopHex = 0xFF09090B, heroBgGradientBottomHex = 0xFF1E1B4B),
        FestivalEvent("holi_2028", "Holi", 11, 3, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Holi!", "Festival of colors.", "🎨", FestivalAnimationFamily.COLOR_BURST, FestivalIntensity.STRONG, 0xFFE91E63, 0xFF00BCD4, 0xFFFFEB3B, "Dark treatment", heroBgGradientTopHex = 0xFF1E0B2B, heroBgGradientBottomHex = 0xFF5B21B6),
        FestivalEvent("eid_ul_fitr_2028", "Eid-ul-Fitr", 28, 2, 2028, FestivalCategory.SPECIAL_HOME_EVENT, true, 95, "Eid Mubarak!", "End of Ramadan fasting.", "🌙", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.MEDIUM, 0xFF00897B, 0xFF0288D1, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("ram_navami_2028", "Ram Navami", 3, 4, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Ram Navami!", "Birth of Lord Rama.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.MEDIUM, 0xFFFF9800, 0xFFE65100, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("mahavir_jayanti_2028", "Mahavir Jayanti", 7, 4, 2028, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Mahavir Jayanti!", "Birth anniversary of Lord Mahavira.", "☸️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFFB8C00, 0xFFF57C00, 0xFFFFF8E1, "Fireworks"),
        FestivalEvent("buddha_purnima_2028", "Buddha Purnima", 8, 5, 2028, FestivalCategory.SPECIAL_HOME_EVENT, true, 80, "Happy Buddha Purnima!", "Enlightenment of Buddha.", "🪷", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFAB47BC, 0xFF7B1FA2, 0xFFE1BEE7, "Confetti"),
        FestivalEvent("raksha_bandhan_2028", "Raksha Bandhan", 5, 8, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Raksha Bandhan!", "Sacred bond of brother and sister.", "🧵", FestivalAnimationFamily.CIRCULAR_THREAD, FestivalIntensity.MEDIUM, 0xFFE91E63, 0xFFC2185B, 0xFFFFD54F, "Confetti", heroBgGradientTopHex = 0xFF220A1A, heroBgGradientBottomHex = 0xFF701A75),
        FestivalEvent("janmashtami_2028", "Janmashtami", 13, 8, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 90, "Happy Janmashtami!", "Birth of Lord Krishna.", "🪈", FestivalAnimationFamily.PLAYFUL_DIVINE, FestivalIntensity.MEDIUM, 0xFF1976D2, 0xFF0D47A1, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF0B1026, heroBgGradientBottomHex = 0xFF1E2B58),
        FestivalEvent("ganesh_chaturthi_2028", "Ganesh Chaturthi", 23, 8, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Ganesh Chaturthi!", "Arrival of Lord Ganesha.", "🐘", FestivalAnimationFamily.FESTIVE_AUGMENT, FestivalIntensity.STRONG, 0xFFFF6F00, 0xFFE65100, 0xFFFFD54F, "Fireworks", heroBgGradientTopHex = 0xFF2B0A00, heroBgGradientBottomHex = 0xFF7C2D12),
        FestivalEvent("onam_2028", "Onam", 1, 9, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 85, "Happy Onam!", "Harvest festival of Kerala.", "🌾", FestivalAnimationFamily.FLORAL_PETALS, FestivalIntensity.MEDIUM, 0xFF43A047, 0xFF2E7D32, 0xFFFFD54F, "Diwali effects", heroBgGradientTopHex = 0xFF052E16, heroBgGradientBottomHex = 0xFF14532D),
        FestivalEvent("dussehra_2028", "Dussehra", 28, 9, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Dussehra!", "Vijayadashami celebration.", "🏹", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFD84315, 0xFFFFD54F, "Fire imitation", heroBgGradientTopHex = 0xFF280D00, heroBgGradientBottomHex = 0xFF9A3412),
        FestivalEvent("diwali_2028", "Diwali", 17, 10, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 100, "Happy Diwali!", "Festival of lights.", "🪔", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.STRONG, 0xFFFF8F00, 0xFFFF6F00, 0xFFFFD54F, "Fireworks overlay", heroBgGradientTopHex = 0xFF1A0B2E, heroBgGradientBottomHex = 0xFF4C1D95),
        FestivalEvent("chhath_puja_2028", "Chhath Puja", 23, 10, 2028, FestivalCategory.SPECIAL_HOME_EVENT, false, 95, "Happy Chhath Puja!", "Sun worship.", "🌅", FestivalAnimationFamily.SUNRISE_RIVER, FestivalIntensity.MEDIUM, 0xFFFF6F00, 0xFFE65100, 0xFF29B6F6, "Fireworks", heroBgGradientTopHex = 0xFF2A0800, heroBgGradientBottomHex = 0xFF7C2D12),

        // ==========================================
        // OTHER CALENDAR EVENTS & OBSERVANCES
        // ==========================================
        FestivalEvent("new_year", "New Year's Day", 1, 1, null, FestivalCategory.OBSERVANCE, false, 50, "Happy New Year!", "Beginning of the solar New Year.", "🎆", FestivalAnimationFamily.LIGHT_GLOW, FestivalIntensity.SUBTLE, 0xFF3F51B5, 0xFF1A237E, 0xFFFFD54F),
        FestivalEvent("womens_day", "International Women's Day", 8, 3, null, FestivalCategory.OBSERVANCE, false, 50, "Happy Women's Day!", "Celebrating social, economic, cultural, and political achievements of women.", "🌺", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFFE91E63, 0xFF880E4F, 0xFFF8BBD0),
        FestivalEvent("labour_day", "Labour Day / May Day", 1, 5, null, FestivalCategory.OBSERVANCE, false, 50, "Happy Labour Day!", "Celebration of laborers and working class.", "🛠️", FestivalAnimationFamily.PEACE_LIGHT, FestivalIntensity.SUBTLE, 0xFFFF5722, 0xFFBF360C, 0xFFFFCC80),
        FestivalEvent("yoga_day", "International Yoga Day", 21, 6, null, FestivalCategory.OBSERVANCE, false, 50, "Happy Yoga Day!", "Promoting physical, mental, and spiritual well-being.", "🧘", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF009688, 0xFF004D40, 0xFFB2DFDB),
        FestivalEvent("national_unity_day", "National Unity Day", 31, 10, null, FestivalCategory.OBSERVANCE, false, 50, "National Unity Day", "Birth anniversary of Sardar Vallabhbhai Patel.", "🛡️", FestivalAnimationFamily.PATRIOTIC_RIBBON, FestivalIntensity.SUBTLE, 0xFFFF9800, 0xFFE65100, 0xFFFFF3E0),

        // Moveable Public Holidays (Calendar events)
        FestivalEvent("good_friday_2024", "Good Friday", 29, 3, 2024, FestivalCategory.CALENDAR_EVENT, true, 60, "Good Friday", "Commemorating the crucifixion of Jesus Christ.", "✝️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF5D4037, 0xFF3E2723, 0xFFD7CCC8),
        FestivalEvent("eid_ul_adha_2024", "Eid-ul-Adha (Bakrid)", 17, 6, 2024, FestivalCategory.CALENDAR_EVENT, true, 60, "Eid Mubarak!", "Feast of the Sacrifice.", "🐑", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.SUBTLE, 0xFF00796B, 0xFF004D40, 0xFFE0F2F1, heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("muharram_2024", "Muharram", 17, 7, 2024, FestivalCategory.CALENDAR_EVENT, true, 60, "Muharram", "Remembrance of Muharram.", "☪️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF37474F, 0xFF212121, 0xFFCFD8DC),

        FestivalEvent("good_friday_2025", "Good Friday", 18, 4, 2025, FestivalCategory.CALENDAR_EVENT, true, 60, "Good Friday", "Commemorating the crucifixion of Jesus Christ.", "✝️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF5D4037, 0xFF3E2723, 0xFFD7CCC8),
        FestivalEvent("eid_ul_adha_2025", "Eid-ul-Adha (Bakrid)", 7, 6, 2025, FestivalCategory.CALENDAR_EVENT, true, 60, "Eid Mubarak!", "Feast of the Sacrifice.", "🐑", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.SUBTLE, 0xFF00796B, 0xFF004D40, 0xFFE0F2F1, heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("muharram_2025", "Muharram", 6, 7, 2025, FestivalCategory.CALENDAR_EVENT, true, 60, "Muharram", "Remembrance of Muharram.", "☪️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF37474F, 0xFF212121, 0xFFCFD8DC),

        FestivalEvent("good_friday_2026", "Good Friday", 3, 4, 2026, FestivalCategory.CALENDAR_EVENT, true, 60, "Good Friday", "Commemorating the crucifixion of Jesus Christ.", "✝️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF5D4037, 0xFF3E2723, 0xFFD7CCC8),
        FestivalEvent("eid_ul_adha_2026", "Eid-ul-Adha (Bakrid)", 27, 5, 2026, FestivalCategory.CALENDAR_EVENT, true, 60, "Eid Mubarak!", "Feast of the Sacrifice.", "🐑", FestivalAnimationFamily.MOON_STAR_TWINKLE, FestivalIntensity.SUBTLE, 0xFF00796B, 0xFF004D40, 0xFFE0F2F1, heroBgGradientTopHex = 0xFF022C22, heroBgGradientBottomHex = 0xFF065F46),
        FestivalEvent("muharram_2026", "Muharram", 25, 6, 2026, FestivalCategory.CALENDAR_EVENT, true, 60, "Muharram", "Remembrance of Muharram.", "☪️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF37474F, 0xFF212121, 0xFFCFD8DC),

        FestivalEvent("good_friday_2027", "Good Friday", 26, 3, 2027, FestivalCategory.CALENDAR_EVENT, true, 60, "Good Friday", "Commemorating the crucifixion of Jesus Christ.", "✝️", FestivalAnimationFamily.SPIRITUAL_GLOW, FestivalIntensity.SUBTLE, 0xFF5D4037, 0xFF3E2723, 0xFFD7CCC8)
    )

    /**
     * Get all events occurring on a specific date (day, month 1-12, year).
     * Sorted by priority descending.
     */
    fun getEventsForDate(day: Int, month: Int, year: Int): List<FestivalEvent> {
        return festivalList
            .filter { event ->
                event.day == day && event.month == month && (event.year == null || event.year == year)
            }
            .sortedByDescending { it.priority }
    }

    /**
     * Get today's highest-priority Special Home Event if any exists.
     * Returns null if no SPECIAL_HOME_EVENT is active for today.
     */
    fun getTodaySpecialHomeEvent(day: Int, month: Int, year: Int): FestivalEvent? {
        return getEventsForDate(day, month, year)
            .firstOrNull { it.category == FestivalCategory.SPECIAL_HOME_EVENT }
    }

    /**
     * Get map of day -> List<FestivalEvent> for a specific month (1-12) and year.
     */
    fun getEventsForMonth(month: Int, year: Int): Map<Int, List<FestivalEvent>> {
        val map = mutableMapOf<Int, MutableList<FestivalEvent>>()
        festivalList.forEach { event ->
            if (event.month == month && (event.year == null || event.year == year)) {
                map.getOrPut(event.day) { mutableListOf() }.add(event)
            }
        }
        // Sort each day's events by priority descending
        map.forEach { (_, list) -> list.sortByDescending { it.priority } }
        return map
    }

    /**
     * Get master list of all configured festivals.
     */
    fun getAllEvents(): List<FestivalEvent> = festivalList

    /**
     * Get distinct SPECIAL_HOME_EVENT items for preview listing.
     * Groups by festival name and picks the current year's instance (or fixed annual instance),
     * sorted chronologically by month and day.
     */
    fun getSpecialHomeEventsForPreview(currentYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)): List<FestivalEvent> {
        val specialEvents = festivalList.filter { it.category == FestivalCategory.SPECIAL_HOME_EVENT }
        val groupedByName = specialEvents.groupBy { it.name }
        val distinctEvents = groupedByName.values.map { list ->
            list.firstOrNull { it.year == currentYear }
                ?: list.firstOrNull { it.year == null }
                ?: list.first()
        }
        return distinctEvents.sortedWith(compareBy({ it.month }, { it.day }))
    }
}
