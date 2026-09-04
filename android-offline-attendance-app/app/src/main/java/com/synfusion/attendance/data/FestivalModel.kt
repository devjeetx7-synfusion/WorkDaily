package com.synfusion.attendance.data

enum class FestivalCategory {
    SPECIAL_HOME_EVENT,
    CALENDAR_EVENT,
    OBSERVANCE
}

enum class FestivalAnimationFamily {
    LIGHT_GLOW,         // Diwali, Vijayadashami, Ram Navami
    COLOR_BURST,        // Holi
    SNOW_TWINKLE,       // Christmas
    EDUCATION_FLOAT,    // Teachers' Day
    SUNRISE_RIVER,      // Chhath Puja (sunrise rays, river reflection, golden light)
    KITE_FLY,           // Makar Sankranti (sky drift, kites)
    SPIRITUAL_GLOW,     // Maha Shivratri, Buddha Purnima, Mahavir Jayanti, Guru Nanak Jayanti
    MOON_STAR_TWINKLE,  // Eid-ul-Fitr
    PATRIOTIC_RIBBON,   // Republic Day, Independence Day (flowing tricolor ribbons, light)
    PLAYFUL_DIVINE,     // Janmashtami (peacock feather movement, divine aura)
    CIRCULAR_THREAD,    // Raksha Bandhan (rakhi thread motifs, soft sparkles)
    FESTIVE_AUGMENT,    // Ganesh Chaturthi (Ganesha contour aura, festive sparkles)
    FLORAL_PETALS,      // Onam (Pookalam petal movement)
    PEACE_LIGHT,        // Gandhi Jayanti (serene circular aura, soft warm light)
    YOUTHFUL_PLAY       // Children's Day (floating paper planes, cheerful stars)
}

enum class FestivalIntensity {
    SUBTLE,
    MEDIUM,
    STRONG
}

data class FestivalEvent(
    val id: String,
    val name: String,
    val day: Int,
    val month: Int, // 1-12
    val year: Int? = null, // null = fixed annual date, non-null = year-specific moveable date
    val category: FestivalCategory,
    val isPublicHoliday: Boolean = false,
    val priority: Int = 10,
    val greeting: String,
    val description: String,
    val emoji: String,
    val animationFamily: FestivalAnimationFamily,
    val intensity: FestivalIntensity = FestivalIntensity.MEDIUM,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val avoidText: String = "",
    val heroBgGradientTopHex: Long? = null,
    val heroBgGradientBottomHex: Long? = null,
    val useLightStatusBarIcons: Boolean = true,
    val customBaseKey: String? = null,
    val calendarTextColorHex: Long = 0xFFFFFFFF
) {
    val baseKey: String
        get() {
            if (customBaseKey != null) return customBaseKey
            val k = id.replace(Regex("_20\\d\\d$"), "")
            if (k.startsWith("eid_ul_fitr") || k.startsWith("eid_ul_adha")) return "eid"
            return k
        }

    val heroAssetPath: String
        get() = "festivals/$baseKey/hero.png"

    val calendarAssetPath: String
        get() = "festivals/$baseKey/calendar.png"

    val backgroundDrawableName: String
        get() = "festival_${baseKey}_background"

    val heroDrawableName: String
        get() = "festival_${baseKey}_hero"

    val calendarDrawableName: String
        get() = "festival_${baseKey}_calendar"

    fun getBackgroundDrawableRes(context: android.content.Context): Int {
        return context.resources.getIdentifier(backgroundDrawableName, "drawable", context.packageName)
    }

    fun getHeroDrawableRes(context: android.content.Context): Int {
        return context.resources.getIdentifier(heroDrawableName, "drawable", context.packageName)
    }

    fun getCalendarDrawableRes(context: android.content.Context): Int {
        return context.resources.getIdentifier(calendarDrawableName, "drawable", context.packageName)
    }
}
