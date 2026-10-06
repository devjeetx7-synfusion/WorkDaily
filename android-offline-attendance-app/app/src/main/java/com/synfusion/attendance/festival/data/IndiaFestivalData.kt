package com.synfusion.attendance.festival.data

import com.synfusion.attendance.festival.Festival
import com.synfusion.attendance.festival.FestivalProvider
import com.synfusion.attendance.festival.FestivalType
import java.time.LocalDate

object IndiaFestivalData : FestivalProvider {
    override val countryCode: String = "IN"

    private fun createFixedAnnualFestivals(year: Int): List<Festival> {
        return listOf(
            Festival("in_new_year_$year", LocalDate.of(year, 1, 1), "New Year's Day", "नया साल", "New Year", "IN", FestivalType.OBSERVANCE),
            Festival("in_sankranti_$year", LocalDate.of(year, 1, 14), "Makar Sankranti / Pongal", "मकर संक्रांति / पोंगल", "Makar Sankranti", "IN", FestivalType.FESTIVAL),
            Festival("in_republic_$year", LocalDate.of(year, 1, 26), "Republic Day", "गणतंत्र दिवस", "Republic Day", "IN", FestivalType.NATIONAL_HOLIDAY),
            Festival("in_labour_$year", LocalDate.of(year, 5, 1), "May Day / Labour Day", "मज़दूर दिवस", "Labour Day", "IN", FestivalType.OBSERVANCE),
            Festival("in_independence_$year", LocalDate.of(year, 8, 15), "Independence Day", "स्वतंत्रता दिवस", "Independence Day", "IN", FestivalType.NATIONAL_HOLIDAY),
            Festival("in_gandhi_$year", LocalDate.of(year, 10, 2), "Gandhi Jayanti", "गांधी जयंती", "Gandhi Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY),
            Festival("in_children_$year", LocalDate.of(year, 11, 14), "Children's Day", "बाल दिवस", "Children's Day", "IN", FestivalType.OBSERVANCE),
            Festival("in_christmas_$year", LocalDate.of(year, 12, 25), "Christmas", "क्रिसमस", "Christmas", "IN", FestivalType.NATIONAL_HOLIDAY)
        )
    }

    private val moveableFestivals: List<Festival> = listOf(
        // 2024
        Festival("in_shivratri_2024", LocalDate.of(2024, 3, 8), "Maha Shivratri", "महाशिवरात्रि", "Shivratri", "IN", FestivalType.FESTIVAL),
        Festival("in_holi_2024", LocalDate.of(2024, 3, 25), "Holi", "होली", "Holi", "IN", FestivalType.FESTIVAL),
        Festival("in_good_friday_2024", LocalDate.of(2024, 3, 29), "Good Friday", "गुड फ्राइडे", "Good Friday", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_eid_fitr_2024", LocalDate.of(2024, 4, 11), "Eid-ul-Fitr", "ईद-उल-फ़ित्र", "Eid", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_ram_navami_2024", LocalDate.of(2024, 4, 17), "Ram Navami", "राम नवमी", "Ram Navami", "IN", FestivalType.FESTIVAL),
        Festival("in_mahavir_2024", LocalDate.of(2024, 4, 21), "Mahavir Jayanti", "महावीर जयंती", "Mahavir Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_buddha_2024", LocalDate.of(2024, 5, 23), "Buddha Purnima", "बुद्ध पूर्णिमा", "Buddha Purnima", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_eid_adha_2024", LocalDate.of(2024, 6, 17), "Eid-ul-Adha (Bakrid)", "ईद-उल-अजहा (बकरीद)", "Bakrid", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_muharram_2024", LocalDate.of(2024, 7, 17), "Muharram", "मुहर्रम", "Muharram", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_rakhi_2024", LocalDate.of(2024, 8, 19), "Raksha Bandhan", "रक्षाबंधन", "Rakhi", "IN", FestivalType.FESTIVAL),
        Festival("in_janmashtami_2024", LocalDate.of(2024, 8, 26), "Janmashtami", "जन्माष्टमी", "Janmashtami", "IN", FestivalType.FESTIVAL),
        Festival("in_ganesh_2024", LocalDate.of(2024, 9, 7), "Ganesh Chaturthi", "गणेश चतुर्थी", "Ganesh Chaturthi", "IN", FestivalType.FESTIVAL),
        Festival("in_onam_2024", LocalDate.of(2024, 9, 15), "Onam", "ओणम", "Onam", "IN", FestivalType.FESTIVAL),
        Festival("in_navratri_2024", LocalDate.of(2024, 10, 3), "Navratri Begins", "नवरात्र प्रारंभ", "Navratri", "IN", FestivalType.FESTIVAL),
        Festival("in_dussehra_2024", LocalDate.of(2024, 10, 12), "Dussehra (Vijayadashami)", "दशहरा (विजयादशमी)", "Dussehra", "IN", FestivalType.FESTIVAL),
        Festival("in_diwali_2024", LocalDate.of(2024, 10, 31), "Diwali (Deepavali)", "दीपावली", "Diwali", "IN", FestivalType.FESTIVAL),
        Festival("in_govardhan_2024", LocalDate.of(2024, 11, 2), "Govardhan Puja / Bhai Dooj", "गोवर्धन पूजा / भाई दूज", "Govardhan Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_chhath_2024", LocalDate.of(2024, 11, 7), "Chhath Puja", "छठ पूजा", "Chhath Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_guru_nanak_2024", LocalDate.of(2024, 11, 15), "Guru Nanak Jayanti", "गुरु नानक जयंती", "Guru Nanak Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY),

        // 2025
        Festival("in_shivratri_2025", LocalDate.of(2025, 2, 26), "Maha Shivratri", "महाशिवरात्रि", "Shivratri", "IN", FestivalType.FESTIVAL),
        Festival("in_holi_2025", LocalDate.of(2025, 3, 14), "Holi", "होली", "Holi", "IN", FestivalType.FESTIVAL),
        Festival("in_eid_fitr_2025", LocalDate.of(2025, 3, 31), "Eid-ul-Fitr", "ईद-उल-फ़ित्र", "Eid", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_ram_navami_2025", LocalDate.of(2025, 4, 6), "Ram Navami", "राम नवमी", "Ram Navami", "IN", FestivalType.FESTIVAL),
        Festival("in_mahavir_2025", LocalDate.of(2025, 4, 10), "Mahavir Jayanti", "महावीर जयंती", "Mahavir Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_good_friday_2025", LocalDate.of(2025, 4, 18), "Good Friday", "गुड फ्राइडे", "Good Friday", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_buddha_2025", LocalDate.of(2025, 5, 12), "Buddha Purnima", "बुद्ध पूर्णिमा", "Buddha Purnima", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_eid_adha_2025", LocalDate.of(2025, 6, 7), "Eid-ul-Adha (Bakrid)", "ईद-उल-अजहा (बकरीद)", "Bakrid", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_muharram_2025", LocalDate.of(2025, 7, 6), "Muharram", "मुहर्रम", "Muharram", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_rakhi_2025", LocalDate.of(2025, 8, 9), "Raksha Bandhan", "रक्षाबंधन", "Rakhi", "IN", FestivalType.FESTIVAL),
        Festival("in_janmashtami_2025", LocalDate.of(2025, 8, 16), "Janmashtami", "जन्माष्टमी", "Janmashtami", "IN", FestivalType.FESTIVAL),
        Festival("in_ganesh_2025", LocalDate.of(2025, 8, 27), "Ganesh Chaturthi", "गणेश चतुर्थी", "Ganesh Chaturthi", "IN", FestivalType.FESTIVAL),
        Festival("in_onam_2025", LocalDate.of(2025, 9, 5), "Onam", "ओणम", "Onam", "IN", FestivalType.FESTIVAL),
        Festival("in_navratri_2025", LocalDate.of(2025, 9, 22), "Navratri Begins", "नवरात्र प्रारंभ", "Navratri", "IN", FestivalType.FESTIVAL),
        Festival("in_dussehra_2025", LocalDate.of(2025, 10, 2), "Dussehra (Vijayadashami)", "दशहरा (विजयादशमी)", "Dussehra", "IN", FestivalType.FESTIVAL),
        Festival("in_diwali_2025", LocalDate.of(2025, 10, 20), "Diwali (Deepavali)", "दीपावली", "Diwali", "IN", FestivalType.FESTIVAL),
        Festival("in_govardhan_2025", LocalDate.of(2025, 10, 22), "Govardhan Puja / Bhai Dooj", "गोवर्धन पूजा / भाई दूज", "Govardhan Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_chhath_2025", LocalDate.of(2025, 10, 28), "Chhath Puja", "छठ पूजा", "Chhath Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_guru_nanak_2025", LocalDate.of(2025, 11, 5), "Guru Nanak Jayanti", "गुरु नानक जयंती", "Guru Nanak Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY),

        // 2026
        Festival("in_shivratri_2026", LocalDate.of(2026, 2, 15), "Maha Shivratri", "महाशिवरात्रि", "Shivratri", "IN", FestivalType.FESTIVAL),
        Festival("in_holi_2026", LocalDate.of(2026, 3, 4), "Holi", "होली", "Holi", "IN", FestivalType.FESTIVAL),
        Festival("in_eid_fitr_2026", LocalDate.of(2026, 3, 20), "Eid-ul-Fitr", "ईद-उल-फ़ित्र", "Eid", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_ram_navami_2026", LocalDate.of(2026, 3, 27), "Ram Navami", "राम नवमी", "Ram Navami", "IN", FestivalType.FESTIVAL),
        Festival("in_mahavir_2026", LocalDate.of(2026, 3, 31), "Mahavir Jayanti", "महावीर जयंती", "Mahavir Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_good_friday_2026", LocalDate.of(2026, 4, 3), "Good Friday", "गुड फ्राइडे", "Good Friday", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_buddha_2026", LocalDate.of(2026, 5, 1), "Buddha Purnima", "बुद्ध पूर्णिमा", "Buddha Purnima", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_eid_adha_2026", LocalDate.of(2026, 5, 27), "Eid-ul-Adha (Bakrid)", "ईद-उल-अजहा (बकरीद)", "Bakrid", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_muharram_2026", LocalDate.of(2026, 6, 25), "Muharram", "मुहर्रम", "Muharram", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_onam_2026", LocalDate.of(2026, 8, 24), "Onam", "ओणम", "Onam", "IN", FestivalType.FESTIVAL),
        Festival("in_rakhi_2026", LocalDate.of(2026, 8, 28), "Raksha Bandhan", "रक्षाबंधन", "Rakhi", "IN", FestivalType.FESTIVAL),
        Festival("in_janmashtami_2026", LocalDate.of(2026, 9, 4), "Janmashtami", "जन्माष्टमी", "Janmashtami", "IN", FestivalType.FESTIVAL),
        Festival("in_ganesh_2026", LocalDate.of(2026, 9, 14), "Ganesh Chaturthi", "गणेश चतुर्थी", "Ganesh Chaturthi", "IN", FestivalType.FESTIVAL),
        Festival("in_navratri_2026", LocalDate.of(2026, 10, 11), "Navratri Begins", "नवरात्र प्रारंभ", "Navratri", "IN", FestivalType.FESTIVAL),
        Festival("in_dussehra_2026", LocalDate.of(2026, 10, 20), "Dussehra (Vijayadashami)", "दशहरा (विजयादशमी)", "Dussehra", "IN", FestivalType.FESTIVAL),
        Festival("in_diwali_2026", LocalDate.of(2026, 11, 8), "Diwali (Deepavali)", "दीपावली", "Diwali", "IN", FestivalType.FESTIVAL),
        Festival("in_govardhan_2026", LocalDate.of(2026, 11, 10), "Govardhan Puja / Bhai Dooj", "गोवर्धन पूजा / भाई दूज", "Govardhan Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_chhath_2026", LocalDate.of(2026, 11, 15), "Chhath Puja", "छठ पूजा", "Chhath Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_guru_nanak_2026", LocalDate.of(2026, 11, 24), "Guru Nanak Jayanti", "गुरु नानक जयंती", "Guru Nanak Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY),

        // 2027
        Festival("in_shivratri_2027", LocalDate.of(2027, 3, 6), "Maha Shivratri", "महाशिवरात्रि", "Shivratri", "IN", FestivalType.FESTIVAL),
        Festival("in_eid_fitr_2027", LocalDate.of(2027, 3, 10), "Eid-ul-Fitr", "ईद-उल-फ़ित्र", "Eid", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_holi_2027", LocalDate.of(2027, 3, 22), "Holi", "होली", "Holi", "IN", FestivalType.FESTIVAL),
        Festival("in_good_friday_2027", LocalDate.of(2027, 3, 26), "Good Friday", "गुड फ्राइडे", "Good Friday", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_ram_navami_2027", LocalDate.of(2027, 4, 15), "Ram Navami", "राम नवमी", "Ram Navami", "IN", FestivalType.FESTIVAL),
        Festival("in_buddha_2027", LocalDate.of(2027, 5, 20), "Buddha Purnima", "बुद्ध पूर्णिमा", "Buddha Purnima", "IN", FestivalType.NATIONAL_HOLIDAY),
        Festival("in_rakhi_2027", LocalDate.of(2027, 8, 17), "Raksha Bandhan", "रक्षाबंधन", "Rakhi", "IN", FestivalType.FESTIVAL),
        Festival("in_janmashtami_2027", LocalDate.of(2027, 8, 25), "Janmashtami", "जन्माष्टमी", "Janmashtami", "IN", FestivalType.FESTIVAL),
        Festival("in_ganesh_2027", LocalDate.of(2027, 9, 4), "Ganesh Chaturthi", "गणेश चतुर्थी", "Ganesh Chaturthi", "IN", FestivalType.FESTIVAL),
        Festival("in_onam_2027", LocalDate.of(2027, 9, 12), "Onam", "ओणम", "Onam", "IN", FestivalType.FESTIVAL),
        Festival("in_navratri_2027", LocalDate.of(2027, 9, 30), "Navratri Begins", "नवरात्र प्रारंभ", "Navratri", "IN", FestivalType.FESTIVAL),
        Festival("in_dussehra_2027", LocalDate.of(2027, 10, 9), "Dussehra (Vijayadashami)", "दशहरा (विजयादशमी)", "Dussehra", "IN", FestivalType.FESTIVAL),
        Festival("in_diwali_2027", LocalDate.of(2027, 10, 29), "Diwali (Deepavali)", "दीपावली", "Diwali", "IN", FestivalType.FESTIVAL),
        Festival("in_govardhan_2027", LocalDate.of(2027, 10, 31), "Govardhan Puja / Bhai Dooj", "गोवर्धन पूजा / भाई दूज", "Govardhan Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_chhath_2027", LocalDate.of(2027, 11, 4), "Chhath Puja", "छठ पूजा", "Chhath Puja", "IN", FestivalType.FESTIVAL),
        Festival("in_guru_nanak_2027", LocalDate.of(2027, 11, 13), "Guru Nanak Jayanti", "गुरु नानक जयंती", "Guru Nanak Jayanti", "IN", FestivalType.NATIONAL_HOLIDAY)
    )

    override fun getFestivals(): List<Festival> {
        val supportedYears = listOf(2024, 2025, 2026, 2027)
        val fixed = supportedYears.flatMap { createFixedAnnualFestivals(it) }
        return (fixed + moveableFestivals).sortedBy { it.date }
    }
}
