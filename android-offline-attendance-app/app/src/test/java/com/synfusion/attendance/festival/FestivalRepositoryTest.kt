package com.synfusion.attendance.festival

import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.utils.AttendanceCalculationEngine
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FestivalRepositoryTest {

    @Test
    fun testKnownIndiaFestivalLookup() {
        val date = LocalDate.of(2026, 1, 26) // Republic Day
        val festivals = FestivalRepository.getFestivalsForDate("IN", date)

        assertEquals(1, festivals.size)
        val republicDay = festivals.first()
        assertEquals("Republic Day", republicDay.name)
        assertEquals("गणतंत्र दिवस", republicDay.nameHi)
        assertEquals(FestivalType.NATIONAL_HOLIDAY, republicDay.type)
    }

    @Test
    fun testOctober2026IndiaFestivalsExactDates() {
        val festivals = FestivalRepository.getFestivalsForMonth("IN", 2026, 10)

        assertEquals(3, festivals.size)
        // Verify chronological order
        assertEquals(LocalDate.of(2026, 10, 2), festivals[0].date)
        assertEquals("Gandhi Jayanti", festivals[0].name)

        assertEquals(LocalDate.of(2026, 10, 11), festivals[1].date)
        assertTrue(festivals[1].name.contains("Navratri"))

        assertEquals(LocalDate.of(2026, 10, 20), festivals[2].date)
        assertTrue(festivals[2].name.contains("Dussehra"))

        // Verify October 7 has no festival
        val oct7 = FestivalRepository.getFestivalsForDate("IN", LocalDate.of(2026, 10, 7))
        assertTrue(oct7.isEmpty())
    }

    @Test
    fun testUnsupportedCountryReturnsEmptyList() {
        val date = LocalDate.of(2026, 1, 26)
        val festivalsUS = FestivalRepository.getFestivalsForDate("US", date)
        assertTrue(festivalsUS.isEmpty())

        val monthFestivalsAE = FestivalRepository.getFestivalsForMonth("AE", 2026, 10)
        assertTrue(monthFestivalsAE.isEmpty())
    }

    @Test
    fun testUnsupportedYearReturnsEmptyWithoutCrashing() {
        val date = LocalDate.of(2035, 1, 1)
        val festivals2035 = FestivalRepository.getFestivalsForDate("IN", date)
        assertTrue(festivals2035.isEmpty())

        val monthFestivals2035 = FestivalRepository.getFestivalsForMonth("IN", 2035, 5)
        assertTrue(monthFestivals2035.isEmpty())
    }

    @Test
    fun testCoexistenceWithAttendanceAndUnchangedCalculation() {
        val records = listOf(
            AttendanceEntity(
                id = "personal_2026-10-20",
                workerId = null,
                date = "2026-10-20",
                status = "present",
                type = "full",
                wage = 600.0,
                currencyCode = "INR"
            )
        )

        val calcResult = AttendanceCalculationEngine.calculateDashboardStats(
            records = records,
            todayStr = "2026-10-20",
            currentMonthStr = "2026-10",
            defaultWage = 600.0
        )

        assertEquals(600.0, calcResult.todayEarned, 0.001)
        assertEquals(600.0, calcResult.monthEarned, 0.001)

        // Confirm festival exists on the same date independently
        val fest = FestivalRepository.getFestivalsForDate("IN", LocalDate.of(2026, 10, 20))
        assertEquals(1, fest.size)
        assertTrue(fest.first().name.contains("Dussehra"))
    }
}
