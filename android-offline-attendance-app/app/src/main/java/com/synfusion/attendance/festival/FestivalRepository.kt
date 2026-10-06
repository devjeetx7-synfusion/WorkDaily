package com.synfusion.attendance.festival

import com.synfusion.attendance.festival.data.IndiaFestivalData
import java.time.LocalDate

object FestivalRepository {

    private val providers: Map<String, FestivalProvider> = mapOf(
        IndiaFestivalData.countryCode to IndiaFestivalData
    )

    fun getFestivalsForDate(countryCode: String, date: LocalDate): List<Festival> {
        val provider = providers[countryCode.uppercase()] ?: return emptyList()
        return provider.getFestivals().filter { it.date == date }
    }

    fun getFestivalsForMonth(countryCode: String, year: Int, month: Int): List<Festival> {
        val provider = providers[countryCode.uppercase()] ?: return emptyList()
        return provider.getFestivals()
            .filter { it.date.year == year && it.date.monthValue == month }
            .sortedBy { it.date }
    }

    fun getFestivalForDateRange(countryCode: String, startDate: LocalDate, endDate: LocalDate): List<Festival> {
        val provider = providers[countryCode.uppercase()] ?: return emptyList()
        return provider.getFestivals()
            .filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
            .sortedBy { it.date }
    }
}
