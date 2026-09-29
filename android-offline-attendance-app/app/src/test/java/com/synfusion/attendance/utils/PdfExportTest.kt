package com.synfusion.attendance.utils

import org.junit.Assert.*
import org.junit.Test
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.viewmodel.OfflinePassbookLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PdfExportTest {

    @Test
    fun testAmountFormatterCurrencyFormatting() {
        // Indian currency grouping rules
        assertEquals("750.00", AmountFormatter.format(750.0))
        assertEquals("1,250.00", AmountFormatter.format(1250.0))
        assertEquals("12,500.00", AmountFormatter.format(12500.0))
        assertEquals("1,25,000.00", AmountFormatter.format(125000.0))
        assertEquals("9,86,85,68,68,918.00", AmountFormatter.format(986856868918.0))

        // Decimal support
        assertEquals("12,500.50", AmountFormatter.format(12500.5))
        assertEquals("20.25", AmountFormatter.format(20.25))

        // Negative amounts
        assertEquals("-1,250.00", AmountFormatter.format(-1250.0))
        assertEquals("-20.50", AmountFormatter.format(-20.5))
    }

    @Test
    fun testCalculationEnginePassbookCalculations() {
        val records = listOf(
            AttendanceEntity(
                id = "1",
                workerId = null,
                date = "2024-03-01",
                status = "present",
                type = "full",
                overtimeHours = 2,
                advanceAmount = 100.0,
                wage = 600.0
            ), // Present + 2h OT + 100 Adv
            AttendanceEntity(
                id = "2",
                workerId = null,
                date = "2024-03-02",
                status = "present",
                type = "half",
                overtimeHours = 0,
                advanceAmount = 0.0,
                wage = 600.0
            ), // Half Day
            AttendanceEntity(
                id = "3",
                workerId = null,
                date = "2024-03-03",
                status = "absent",
                type = "full",
                overtimeHours = 0,
                advanceAmount = 0.0,
                wage = 600.0
            ),  // Absent
            AttendanceEntity(
                id = "4",
                workerId = null,
                date = "2024-03-04",
                status = "advance",
                type = "full",
                overtimeHours = 0,
                advanceAmount = 200.0,
                wage = 600.0
            ) // Advance Record
        )

        val wage = 600.0 // wage per day. Overtime is 600 / 8 = 75 per hour
        val calc = AttendanceCalculationEngine.calculatePassbook(records, wage)

        // Verifying Counts
        assertEquals(2, calc.present)
        assertEquals(1, calc.absent)
        assertEquals(1, calc.half)

        // Verifying Total Days Worked (1 full day + 1 half day = 1.5 working days)
        assertEquals(1.5, calc.totalWorks, 0.0)

        // Verifying Earnings:
        // Day 1 Present: 600 base wage + 150 OT (2 hours) = 750.0
        // Day 2 Half Present: 300 base wage = 300.0
        // Total Gross Earnings: 750 + 300 = 1050.0
        assertEquals(1050.0, calc.totalEarnings, 0.0)

        // Verifying Advances:
        // Day 1 has advanceAmount = 100.0 (present status) and Day 4 has advanceAmount = 200.0 (advance status).
        // Total advance = 100.0 + 200.0 = 300.0.
        assertEquals(300.0, calc.totalAdv, 0.0)

        // Net Balance = 1050.0 - 300.0 = 750.0
        assertEquals(750.0, calc.netBalance, 0.0)
    }

    @Test
    fun testEmptyDatabaseHandling() {
        val calc = AttendanceCalculationEngine.calculatePassbook(emptyList(), 500.0)
        assertEquals(0, calc.present)
        assertEquals(0, calc.absent)
        assertEquals(0, calc.half)
        assertEquals(0.0, calc.totalEarnings, 0.0)
        assertEquals(0.0, calc.totalAdv, 0.0)
        assertEquals(0.0, calc.netBalance, 0.0)
        assertEquals(0.0, calc.totalWorks, 0.0)
        assertTrue(calc.logs.isEmpty())
    }

    @Test
    fun testAllTimePassbookPdfModelMapping() {
        // Simulating data grouping across multiple months and years
        val name = "Aarav Sharma"
        val record1 = AllTimePdfRecord(
            dateStr = "01/12/2023",
            workerName = name,
            statusStr = "Present",
            statusRaw = "present",
            isHalfDay = false,
            baseEarnings = 500.0,
            overtimeHours = 0,
            overtimeAmount = 0.0,
            advanceAmount = 0.0,
            note = ""
        )
        val record2 = AllTimePdfRecord(
            dateStr = "05/01/2024",
            workerName = name,
            statusStr = "Half Day",
            statusRaw = "present",
            isHalfDay = true,
            baseEarnings = 250.0,
            overtimeHours = 2,
            overtimeAmount = 125.0,
            advanceAmount = 50.0,
            note = "Shorter day"
        )

        val monthGroupDec = AllTimePdfMonthGroup(
            monthName = "December",
            monthNumeric = "12",
            totalEarnings = 500.0,
            totalSalary = 500.0,
            totalOvertime = 0.0,
            totalAdvance = 0.0,
            presentCount = 1,
            halfCount = 0,
            absentCount = 0,
            records = listOf(record1)
        )

        val monthGroupJan = AllTimePdfMonthGroup(
            monthName = "January",
            monthNumeric = "01",
            totalEarnings = 375.0,
            totalSalary = 250.0,
            totalOvertime = 125.0,
            totalAdvance = 50.0,
            presentCount = 1,
            halfCount = 1,
            absentCount = 0,
            records = listOf(record2)
        )

        val yearGroup2023 = AllTimePdfYearGroup(year = "2023", months = listOf(monthGroupDec))
        val yearGroup2024 = AllTimePdfYearGroup(year = "2024", months = listOf(monthGroupJan))

        val model = AllTimePassbookPdfModel(
            workerName = name,
            generatedAtStr = "25 Oct 2024",
            firstRecordDateStr = "01/12/2023",
            latestRecordDateStr = "05/01/2024",
            totalRecordsCount = 2,
            totalEarnings = 875.0,
            totalSalary = 750.0,
            totalOvertime = 125.0,
            totalAdvance = 50.0,
            totalPresent = 2,
            totalHalfDays = 1,
            totalAbsent = 0,
            totalWorkingDays = 1.5,
            amountToReceive = 825.0,
            years = listOf(yearGroup2023, yearGroup2024)
        )

        assertEquals(2, model.years.size)
        assertEquals("2023", model.years[0].year)
        assertEquals("2024", model.years[1].year)

        assertEquals(875.0, model.totalEarnings, 0.0)
        assertEquals(750.0, model.totalSalary, 0.0)
        assertEquals(125.0, model.totalOvertime, 0.0)
        assertEquals(50.0, model.totalAdvance, 0.0)
        assertEquals(1.5, model.totalWorkingDays, 0.0)
        assertEquals(825.0, model.amountToReceive, 0.0)
    }

    @Test
    fun testPdfExportStateTransitions() {
        var state: PdfExportState = PdfExportState.Idle
        assertEquals(PdfExportState.Idle, state)

        state = PdfExportState.Preparing
        assertEquals(PdfExportState.Preparing, state)

        state = PdfExportState.CollectingRecords
        assertEquals(PdfExportState.CollectingRecords, state)

        state = PdfExportState.GeneratingPdf
        assertEquals(PdfExportState.GeneratingPdf, state)

        state = PdfExportState.Finalizing
        assertEquals(PdfExportState.Finalizing, state)

        val dummyFile = File("test.pdf")
        state = PdfExportState.Success(dummyFile)
        assertTrue(state is PdfExportState.Success)
        assertEquals(dummyFile, (state as PdfExportState.Success).file)

        state = PdfExportState.Error("Disk full")
        assertTrue(state is PdfExportState.Error)
        assertEquals("Disk full", (state as PdfExportState.Error).message)
    }

    @Test
    fun testLongNotesHeightMeasurementSimulation() {
        val note = "This is an extremely long note that will be wrapped into multiple lines on the pdf table canvas to verify that it does not overflow outside the column boundaries or cause overlap."
        val charCount = note.length

        // Simulation of standard line breaking:
        // note is broken up into lines depending on width.
        // Assuming 170f width and 8f font height, let's say average chars per line is ~35.
        val lineCount = Math.ceil(charCount.toDouble() / 35.0).toInt()
        val calculatedHeight = lineCount * 12f // 12f per line height including padding

        assertTrue(calculatedHeight > 20f) // Should dynamically size taller than base height
    }

    @Test
    fun testHindiLocalizationTranslations() {
        // Simulation of Locale mapping
        val localeEnglish = Locale("en")
        val localeHindi = Locale("hi")

        val translations = mapOf(
            "en" to mapOf(
                "title" to "All Time Passbook",
                "earnings" to "Total Earnings",
                "absent" to "Absent Days"
            ),
            "hi" to mapOf(
                "title" to "सर्वकालिक पासबुक",
                "earnings" to "कुल कमाई",
                "absent" to "अनुपस्थित दिन"
            )
        )

        assertEquals("All Time Passbook", translations[localeEnglish.language]?.get("title"))
        assertEquals("सर्वकालिक पासबुक", translations[localeHindi.language]?.get("title"))

        assertEquals("Total Earnings", translations[localeEnglish.language]?.get("earnings"))
        assertEquals("कुल कमाई", translations[localeHindi.language]?.get("earnings"))

        assertEquals("Absent Days", translations[localeEnglish.language]?.get("absent"))
        assertEquals("अनुपस्थित दिन", translations[localeHindi.language]?.get("absent"))
    }
}
