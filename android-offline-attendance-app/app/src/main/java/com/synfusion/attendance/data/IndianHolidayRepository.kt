package com.synfusion.attendance.data

enum class HolidayCategory {
    PUBLIC_HOLIDAY,
    FESTIVAL,
    OBSERVANCE
}

data class IndianHoliday(
    val day: Int,
    val month: Int, // 1-12 (1 = Jan, 12 = Dec)
    val year: Int? = null, // null for fixed annual date, or specific year for moveable festival
    val name: String,
    val category: HolidayCategory,
    val description: String,
    val emoji: String = "🚩"
)

object IndianHolidayRepository {

    // Centralized master list of Indian public holidays, festivals, and observances
    private val holidays: List<IndianHoliday> = listOf(
        // FIXED ANNUAL DATES (year = null)
        IndianHoliday(1, 1, null, "New Year's Day", HolidayCategory.OBSERVANCE, "Beginning of the solar New Year.", "🎆"),
        IndianHoliday(14, 1, null, "Makar Sankranti / Pongal", HolidayCategory.FESTIVAL, "Harvest festival dedicated to the Sun God Surya.", "🪁"),
        IndianHoliday(26, 1, null, "Republic Day", HolidayCategory.PUBLIC_HOLIDAY, "Honors the date on which the Constitution of India came into effect in 1950.", "🇮🇳"),
        IndianHoliday(8, 3, null, "International Women's Day", HolidayCategory.OBSERVANCE, "Celebrating the social, economic, cultural, and political achievements of women.", "🌺"),
        IndianHoliday(1, 5, null, "May Day / Labour Day", HolidayCategory.OBSERVANCE, "Celebration of laborers and working class.", "🛠️"),
        IndianHoliday(21, 6, null, "International Yoga Day", HolidayCategory.OBSERVANCE, "Promotes physical, mental, and spiritual well-being.", "🧘"),
        IndianHoliday(15, 8, null, "Independence Day", HolidayCategory.PUBLIC_HOLIDAY, "Commemorates the nation's independence in 1947.", "🇮🇳"),
        IndianHoliday(5, 9, null, "Teachers' Day", HolidayCategory.OBSERVANCE, "A day dedicated to celebrating teachers and educators, honoring Dr. S. Radhakrishnan.", "📚"),
        IndianHoliday(2, 10, null, "Gandhi Jayanti", HolidayCategory.PUBLIC_HOLIDAY, "Birth anniversary of Mahatma Gandhi, celebrated as International Day of Non-Violence.", "👓"),
        IndianHoliday(31, 10, null, "National Unity Day", HolidayCategory.OBSERVANCE, "Birth anniversary of Sardar Vallabhbhai Patel.", "🛡️"),
        IndianHoliday(14, 11, null, "Children's Day", HolidayCategory.OBSERVANCE, "Tribute to Pandit Jawaharlal Nehru, celebrating youth and innocence.", "🎈"),
        IndianHoliday(25, 12, null, "Christmas", HolidayCategory.PUBLIC_HOLIDAY, "Celebration of the birth of Jesus Christ.", "🎄"),

        // YEAR-SPECIFIC MOVEABLE FESTIVALS & HOLIDAYS

        // 2024
        IndianHoliday(8, 3, 2024, "Maha Shivratri", HolidayCategory.FESTIVAL, "Festival of reverence for Lord Shiva.", "🕉️"),
        IndianHoliday(25, 3, 2024, "Holi", HolidayCategory.FESTIVAL, "The vibrant festival of colors celebrating spring and love.", "🎨"),
        IndianHoliday(29, 3, 2024, "Good Friday", HolidayCategory.PUBLIC_HOLIDAY, "Commemorates the crucifixion of Jesus Christ.", "✝️"),
        IndianHoliday(11, 4, 2024, "Eid-ul-Fitr", HolidayCategory.PUBLIC_HOLIDAY, "Islamic festival marking the end of Ramadan fasting.", "🌙"),
        IndianHoliday(17, 4, 2024, "Ram Navami", HolidayCategory.FESTIVAL, "Celebrates the birth of Lord Rama.", "🏹"),
        IndianHoliday(21, 4, 2024, "Mahavir Jayanti", HolidayCategory.PUBLIC_HOLIDAY, "Birth anniversary of Lord Mahavira, founder of Jainism.", "☸️"),
        IndianHoliday(23, 5, 2024, "Buddha Purnima", HolidayCategory.PUBLIC_HOLIDAY, "Birth, enlightenment, and death of Gautama Buddha.", "🪷"),
        IndianHoliday(17, 6, 2024, "Eid-ul-Adha (Bakrid)", HolidayCategory.PUBLIC_HOLIDAY, "Feast of the Sacrifice observed by Muslims.", "🐑"),
        IndianHoliday(17, 7, 2024, "Muharram", HolidayCategory.PUBLIC_HOLIDAY, "First month of the Islamic calendar, observing Remembrance of Muharram.", "☪️"),
        IndianHoliday(19, 8, 2024, "Raksha Bandhan", HolidayCategory.FESTIVAL, "Celebration of the sacred bond between brothers and sisters.", "🧵"),
        IndianHoliday(26, 8, 2024, "Janmashtami", HolidayCategory.FESTIVAL, "Celebrates the birth of Lord Krishna.", "🪈"),
        IndianHoliday(7, 9, 2024, "Ganesh Chaturthi", HolidayCategory.FESTIVAL, "Ten-day festival celebrating the arrival of Lord Ganesha.", "🐘"),
        IndianHoliday(15, 9, 2024, "Onam", HolidayCategory.FESTIVAL, "Harvest festival of Kerala honoring King Mahabali.", "🌾"),
        IndianHoliday(12, 10, 2024, "Dussehra (Vijayadashami)", HolidayCategory.FESTIVAL, "Triumph of good over evil as Lord Rama defeated Ravana.", "🏹"),
        IndianHoliday(31, 10, 2024, "Diwali (Deepavali)", HolidayCategory.FESTIVAL, "Festival of lights symbolizing the victory of light over darkness.", "🪔"),
        IndianHoliday(15, 11, 2024, "Guru Nanak Jayanti", HolidayCategory.PUBLIC_HOLIDAY, "Birth anniversary of the founder of Sikhism, Guru Nanak Dev Ji.", "🪯"),

        // 2025
        IndianHoliday(26, 2, 2025, "Maha Shivratri", HolidayCategory.FESTIVAL, "Festival of reverence for Lord Shiva.", "🕉️"),
        IndianHoliday(14, 3, 2025, "Holi", HolidayCategory.FESTIVAL, "The vibrant festival of colors celebrating spring and love.", "🎨"),
        IndianHoliday(31, 3, 2025, "Eid-ul-Fitr", HolidayCategory.PUBLIC_HOLIDAY, "Islamic festival marking the end of Ramadan fasting.", "🌙"),
        IndianHoliday(6, 4, 2025, "Ram Navami", HolidayCategory.FESTIVAL, "Celebrates the birth of Lord Rama.", "🏹"),
        IndianHoliday(10, 4, 2025, "Mahavir Jayanti", HolidayCategory.PUBLIC_HOLIDAY, "Birth anniversary of Lord Mahavira.", "☸️"),
        IndianHoliday(18, 4, 2025, "Good Friday", HolidayCategory.PUBLIC_HOLIDAY, "Commemorates the crucifixion of Jesus Christ.", "✝️"),
        IndianHoliday(12, 5, 2025, "Buddha Purnima", HolidayCategory.PUBLIC_HOLIDAY, "Birth and enlightenment of Gautama Buddha.", "🪷"),
        IndianHoliday(7, 6, 2025, "Eid-ul-Adha (Bakrid)", HolidayCategory.PUBLIC_HOLIDAY, "Feast of the Sacrifice.", "🐑"),
        IndianHoliday(6, 7, 2025, "Muharram", HolidayCategory.PUBLIC_HOLIDAY, "Remembrance of Muharram.", "☪️"),
        IndianHoliday(9, 8, 2025, "Raksha Bandhan", HolidayCategory.FESTIVAL, "Sacred bond between brothers and sisters.", "🧵"),
        IndianHoliday(16, 8, 2025, "Janmashtami", HolidayCategory.FESTIVAL, "Birth of Lord Krishna.", "🪈"),
        IndianHoliday(27, 8, 2025, "Ganesh Chaturthi", HolidayCategory.FESTIVAL, "Celebration of Lord Ganesha.", "🐘"),
        IndianHoliday(5, 9, 2025, "Onam", HolidayCategory.FESTIVAL, "Harvest festival of Kerala.", "🌾"),
        IndianHoliday(2, 10, 2025, "Dussehra (Vijayadashami)", HolidayCategory.FESTIVAL, "Triumph of good over evil.", "🏹"),
        IndianHoliday(20, 10, 2025, "Diwali (Deepavali)", HolidayCategory.FESTIVAL, "Festival of lights.", "🪔"),
        IndianHoliday(5, 11, 2025, "Guru Nanak Jayanti", HolidayCategory.PUBLIC_HOLIDAY, "Birth anniversary of Guru Nanak Dev Ji.", "🪯"),

        // 2026
        IndianHoliday(15, 2, 2026, "Maha Shivratri", HolidayCategory.FESTIVAL, "Festival of reverence for Lord Shiva.", "🕉️"),
        IndianHoliday(4, 3, 2026, "Holi", HolidayCategory.FESTIVAL, "The festival of colors.", "🎨"),
        IndianHoliday(20, 3, 2026, "Eid-ul-Fitr", HolidayCategory.PUBLIC_HOLIDAY, "End of Ramadan fasting.", "🌙"),
        IndianHoliday(27, 3, 2026, "Ram Navami", HolidayCategory.FESTIVAL, "Birth of Lord Rama.", "🏹"),
        IndianHoliday(3, 4, 2026, "Good Friday", HolidayCategory.PUBLIC_HOLIDAY, "Commemorates the crucifixion of Jesus Christ.", "✝️"),
        IndianHoliday(31, 3, 2026, "Mahavir Jayanti", HolidayCategory.PUBLIC_HOLIDAY, "Birth anniversary of Lord Mahavira.", "☸️"),
        IndianHoliday(1, 5, 2026, "Buddha Purnima", HolidayCategory.PUBLIC_HOLIDAY, "Birth and enlightenment of Gautama Buddha.", "🪷"),
        IndianHoliday(27, 5, 2026, "Eid-ul-Adha (Bakrid)", HolidayCategory.PUBLIC_HOLIDAY, "Feast of the Sacrifice.", "🐑"),
        IndianHoliday(25, 6, 2026, "Muharram", HolidayCategory.PUBLIC_HOLIDAY, "Remembrance of Muharram.", "☪️"),
        IndianHoliday(28, 8, 2026, "Raksha Bandhan", HolidayCategory.FESTIVAL, "Bond between brothers and sisters.", "🧵"),
        IndianHoliday(4, 9, 2026, "Janmashtami", HolidayCategory.FESTIVAL, "Birth of Lord Krishna.", "🪈"),
        IndianHoliday(14, 9, 2026, "Ganesh Chaturthi", HolidayCategory.FESTIVAL, "Celebration of Lord Ganesha.", "🐘"),
        IndianHoliday(24, 8, 2026, "Onam", HolidayCategory.FESTIVAL, "Harvest festival of Kerala.", "🌾"),
        IndianHoliday(20, 10, 2026, "Dussehra (Vijayadashami)", HolidayCategory.FESTIVAL, "Victory of Lord Rama over Ravana.", "🏹"),
        IndianHoliday(8, 11, 2026, "Diwali (Deepavali)", HolidayCategory.FESTIVAL, "Festival of lights.", "🪔"),
        IndianHoliday(24, 11, 2026, "Guru Nanak Jayanti", HolidayCategory.PUBLIC_HOLIDAY, "Birth anniversary of Guru Nanak Dev Ji.", "🪯"),

        // 2027
        IndianHoliday(6, 3, 2027, "Maha Shivratri", HolidayCategory.FESTIVAL, "Festival of reverence for Lord Shiva.", "🕉️"),
        IndianHoliday(22, 3, 2027, "Holi", HolidayCategory.FESTIVAL, "Festival of colors.", "🎨"),
        IndianHoliday(10, 3, 2027, "Eid-ul-Fitr", HolidayCategory.PUBLIC_HOLIDAY, "End of Ramadan fasting.", "🌙"),
        IndianHoliday(15, 4, 2027, "Ram Navami", HolidayCategory.FESTIVAL, "Birth of Lord Rama.", "🏹"),
        IndianHoliday(26, 3, 2027, "Good Friday", HolidayCategory.PUBLIC_HOLIDAY, "Commemorates the crucifixion of Jesus Christ.", "✝️"),
        IndianHoliday(20, 5, 2027, "Buddha Purnima", HolidayCategory.PUBLIC_HOLIDAY, "Enlightenment of Gautama Buddha.", "🪷"),
        IndianHoliday(17, 8, 2027, "Raksha Bandhan", HolidayCategory.FESTIVAL, "Bond of brother and sister.", "🧵"),
        IndianHoliday(25, 8, 2027, "Janmashtami", HolidayCategory.FESTIVAL, "Birth of Lord Krishna.", "🪈"),
        IndianHoliday(4, 9, 2027, "Ganesh Chaturthi", HolidayCategory.FESTIVAL, "Arrival of Lord Ganesha.", "🐘"),
        IndianHoliday(9, 10, 2027, "Dussehra", HolidayCategory.FESTIVAL, "Vijayadashami celebration.", "🏹"),
        IndianHoliday(29, 10, 2027, "Diwali", HolidayCategory.FESTIVAL, "Festival of lights.", "🪔")
    )

    /**
     * Get list of holidays for a given day, month (1-12) and year.
     */
    fun getHolidaysForDate(day: Int, month: Int, year: Int): List<IndianHoliday> {
        return holidays.filter { holiday ->
            holiday.day == day && holiday.month == month && (holiday.year == null || holiday.year == year)
        }
    }

    /**
     * Get map of day -> List<IndianHoliday> for a specific month (1-12) and year.
     */
    fun getHolidaysForMonth(month: Int, year: Int): Map<Int, List<IndianHoliday>> {
        val map = mutableMapOf<Int, MutableList<IndianHoliday>>()
        holidays.forEach { holiday ->
            if (holiday.month == month && (holiday.year == null || holiday.year == year)) {
                map.getOrPut(holiday.day) { mutableListOf() }.add(holiday)
            }
        }
        return map
    }
}
