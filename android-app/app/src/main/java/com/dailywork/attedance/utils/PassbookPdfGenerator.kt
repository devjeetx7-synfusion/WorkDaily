package com.dailywork.attedance.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import java.io.File
import java.io.FileOutputStream

data class PdfData(
    val title: String,
    val subtitle: String,
    val name: String,
    val userId: String,
    val role: String,
    val monthYearStr: String,
    val monthNumericStr: String, // e.g., "03"
    val yearNumericStr: String, // e.g., "2024"
    val contractorName: String,
    val dailyWage: Double,
    val totalManDays: Double,
    val totalPresent: Int,
    val totalHalfDays: Int,
    val totalOvertimeHours: Int,
    val totalOvertimeEarnings: Double,
    val grossEarned: Double,
    val advanceDeducted: Double,
    val netPayable: Double,
    val logs: List<PdfLog>
)

data class PdfLog(
    val date: String,
    val status: String,
    val workType: String,
    val dailyWage: String,
    val overtime: String,
    val advanceAmount: String,
    val runningBalance: String,
    val note: String
)

object PassbookPdfGenerator {

    private const val PAGE_WIDTH = 842 // A4 Landscape width at 72 PPI
    private const val PAGE_HEIGHT = 595 // A4 Landscape height at 72 PPI
    private const val MARGIN = 40f

    // Theme colors
    private val colorHeaderBg = Color.parseColor("#0f172a") // Deep slate
    private val colorHeaderText = Color.WHITE
    private val colorHeaderSubText = Color.parseColor("#94a3b8")
    private val colorSummaryTitle = Color.parseColor("#0f172a")
    private val colorTableHeadBg = Color.parseColor("#1e40af") // Blue
    private val colorPositive = Color.parseColor("#16a34a") // Green
    private val colorNegative = Color.parseColor("#dc2626") // Red
    private val colorStripedRow = Color.parseColor("#f8fafc")
    private val colorBorder = Color.parseColor("#cbd5e1")

    fun generatePdf(context: Context, data: PdfData): File? {
        val document = PdfDocument()
        var pageNumber = 1
        var yPos = 0f

        val paintHeaderBg = Paint().apply { color = colorHeaderBg; style = Paint.Style.FILL }
        val paintTextBold = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val paintTextNormal = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val paintRectBorder = Paint().apply { color = colorBorder; style = Paint.Style.STROKE; strokeWidth = 0.5f }
        val paintRectFill = Paint().apply { style = Paint.Style.FILL }

        val textPaint = TextPaint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true; textSize = 9f }

        fun drawHeader(canvas: Canvas) {
            val headerHeight = 100f
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paintHeaderBg)

            paintTextBold.color = colorHeaderText
            paintTextBold.textSize = 24f
            paintTextBold.textAlign = Paint.Align.LEFT
            canvas.drawText("DailyWork Pro", MARGIN, 40f, paintTextBold)

            paintTextNormal.color = colorHeaderSubText
            paintTextNormal.textSize = 10f
            canvas.drawText(data.subtitle, MARGIN, 58f, paintTextNormal)

            // Info Section
            paintTextBold.color = Color.WHITE
            paintTextBold.textSize = 11f
            canvas.drawText("Worker: ${data.name}", MARGIN, 85f, paintTextBold)
            canvas.drawText("Role: ${data.role}", MARGIN + 250f, 85f, paintTextBold)

            val rightAlign = PAGE_WIDTH - MARGIN
            paintTextBold.textAlign = Paint.Align.RIGHT
            canvas.drawText("Month: ${data.monthYearStr}", rightAlign, 40f, paintTextBold)

            paintTextNormal.color = colorHeaderSubText
            paintTextNormal.textAlign = Paint.Align.RIGHT
            canvas.drawText("Contractor: ${data.contractorName}", rightAlign, 58f, paintTextNormal)
        }

        fun drawTableHeaders(canvas: Canvas, y: Float, colWidths: FloatArray, headers: Array<String>) {
            val headerHeight = 25f
            paintRectFill.color = colorTableHeadBg
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerHeight, paintRectFill)

            paintTextBold.color = Color.WHITE
            paintTextBold.textSize = 9f
            paintTextBold.textAlign = Paint.Align.CENTER

            var currentX = MARGIN
            for (i in headers.indices) {
                canvas.drawText(headers[i], currentX + colWidths[i] / 2f, y + 16f, paintTextBold)
                canvas.drawRect(currentX, y, currentX + colWidths[i], y + headerHeight, paintRectBorder)
                currentX += colWidths[i]
            }
        }

        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        drawHeader(canvas)
        yPos = 120f

        // Financial Summary Section
        paintTextBold.color = colorSummaryTitle
        paintTextBold.textSize = 14f
        paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText("Financial Summary Breakdown", MARGIN, yPos, paintTextBold)
        yPos += 10f

        val summaryTableWidth = PAGE_WIDTH - (MARGIN * 2)
        val summaryCols = floatArrayOf(summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.15f, summaryTableWidth * 0.15f)
        val summaryHeaders = arrayOf("Daily Wage", "Total Days", "Present/Half", "OT Hours", "OT Earned", "Gross Total", "Net Payable")

        drawTableHeaders(canvas, yPos, summaryCols, summaryHeaders)
        yPos += 25f

        // Summary row
        paintTextNormal.color = Color.BLACK
        paintTextNormal.textSize = 10f
        paintTextNormal.textAlign = Paint.Align.CENTER
        val summaryRowHeight = 30f

        val summaryValues = arrayOf(
            "Rs. ${data.dailyWage.toInt()}",
            "${data.totalManDays}",
            "${data.totalPresent}P / ${data.totalHalfDays}H",
            "${data.totalOvertimeHours} hrs",
            "Rs. ${data.totalOvertimeEarnings.toInt()}",
            "Rs. ${data.grossEarned.toInt()}",
            "Rs. ${data.netPayable.toInt()}"
        )

        var curX = MARGIN
        for (i in summaryValues.indices) {
            if (i == 6) {
                paintTextBold.color = if (data.netPayable >= 0) colorPositive else colorNegative
                paintTextBold.textAlign = Paint.Align.CENTER
                paintTextBold.textSize = 11f
                canvas.drawText(summaryValues[i], curX + summaryCols[i] / 2f, yPos + 18f, paintTextBold)
            } else {
                canvas.drawText(summaryValues[i], curX + summaryCols[i] / 2f, yPos + 18f, paintTextNormal)
            }
            canvas.drawRect(curX, yPos, curX + summaryCols[i], yPos + summaryRowHeight, paintRectBorder)
            curX += summaryCols[i]
        }

        yPos += summaryRowHeight + 30f

        // Detailed Logs Section
        paintTextBold.color = Color.BLACK
        paintTextBold.textAlign = Paint.Align.LEFT
        paintTextBold.textSize = 14f
        canvas.drawText("Detailed Attendance & Financial Ledger", MARGIN, yPos, paintTextBold)
        yPos += 10f

        val logCols = floatArrayOf(
            summaryTableWidth * 0.10f, // Date
            summaryTableWidth * 0.10f, // Status
            summaryTableWidth * 0.12f, // Work Type
            summaryTableWidth * 0.10f, // Wage
            summaryTableWidth * 0.10f, // Overtime
            summaryTableWidth * 0.10f, // Advance
            summaryTableWidth * 0.10f, // Balance
            summaryTableWidth * 0.28f  // Notes
        )
        val logHeaders = arrayOf("Date", "Status", "Work Type", "Wage", "Overtime", "Advance", "Balance", "Notes")

        drawTableHeaders(canvas, yPos, logCols, logHeaders)
        yPos += 25f

        // Draw logs with multi-page handling and text wrapping for notes
        for ((index, log) in data.logs.withIndex()) {
            // Measure note height
            val noteWidth = logCols[7] - 10f
            val staticLayout = StaticLayout.Builder.obtain(log.note, 0, log.note.length, textPaint, noteWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            val rowHeight = Math.max(22f, staticLayout.height + 10f)

            if (yPos + rowHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawHeader(canvas)
                yPos = 120f
                drawTableHeaders(canvas, yPos, logCols, logHeaders)
                yPos += 25f
            }

            paintRectFill.color = if (index % 2 == 0) Color.WHITE else colorStripedRow
            canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + rowHeight, paintRectFill)

            val vals = arrayOf(log.date, log.status, log.workType, log.dailyWage, log.overtime, log.advanceAmount, log.runningBalance)
            curX = MARGIN
            paintTextNormal.textSize = 9f
            paintTextNormal.textAlign = Paint.Align.CENTER

            for (i in vals.indices) {
                canvas.drawText(vals[i], curX + logCols[i] / 2f, yPos + 14f, paintTextNormal)
                canvas.drawRect(curX, yPos, curX + logCols[i], yPos + rowHeight, paintRectBorder)
                curX += logCols[i]
            }

            // Draw Wrapped Note
            canvas.save()
            canvas.translate(curX + 5f, yPos + 5f)
            staticLayout.draw(canvas)
            canvas.restore()
            canvas.drawRect(curX, yPos, curX + logCols[7], yPos + rowHeight, paintRectBorder)

            yPos += rowHeight
        }

        // Footer
        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 8f; isAntiAlias = true }
        canvas.drawText("Generated by DailyWork Pro | Digital Passbook System", MARGIN, PAGE_HEIGHT - 20f, footerPaint)

        document.finishPage(page)

        val fileName = "passbook_${data.userId}_${data.yearNumericStr}-${data.monthNumericStr}.pdf"
        val cachePath = File(context.cacheDir, "docs")
        cachePath.mkdirs()
        val file = File(cachePath, fileName)

        return try {
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }
}
