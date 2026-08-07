package com.synfusion.attendance.utils

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
import com.synfusion.attendance.R

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
    private val colorHeaderBg = Color.parseColor("#13B28A") // Green Primary
    private val colorHeaderText = Color.WHITE
    private val colorHeaderSubText = Color.parseColor("#F8FAFB") // Light for contrast
    private val colorSummaryTitle = Color.parseColor("#1E293B") // TextPrimary
    private val colorTableHeadBg = Color.parseColor("#13B28A") // Green Primary
    private val colorPositive = Color.parseColor("#16a34a") // Green
    private val colorNegative = Color.parseColor("#dc2626") // Red
    private val colorHalfDay = Color.parseColor("#f97316") // Orange
    private val colorAdvance = Color.parseColor("#f97316") // Orange
    private val colorOvertime = Color.parseColor("#3b82f6") // Blue
    private val colorStripedRow = Color.parseColor("#F8FAFB") // Background theme color
    private val colorBorder = Color.parseColor("#F1F5F9") // Border theme color

    fun generatePdf(context: Context, data: PdfData): File? {
        val document = PdfDocument()
        var pageNumber = 1
        var yPos = 0f

        val paintHeaderBg = Paint().apply { color = colorHeaderBg; style = Paint.Style.FILL }
        // Using "sans-serif" typeface to ensure maximum compatibility and Devanagari character support across all Android devices.
        val paintTextBold = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); isAntiAlias = true }
        val paintTextNormal = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); isAntiAlias = true }
        val paintRectBorder = Paint().apply { color = colorBorder; style = Paint.Style.STROKE; strokeWidth = 0.5f }
        val paintRectFill = Paint().apply { style = Paint.Style.FILL }

        val textPaint = TextPaint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); isAntiAlias = true; textSize = 9f }

        // Localized string values
        val appNameStr = context.getString(R.string.app_name)
        val monthLabelFormat = context.getString(R.string.pdf_month_label)
        val generatedByStr = context.getString(R.string.pdf_generated_by)
        val summaryTitleStr = context.getString(R.string.pdf_summary_title)
        val ledgerTitleStr = context.getString(R.string.pdf_ledger_title)
        val rsStr = context.getString(R.string.pdf_rs)
        val hrsStr = context.getString(R.string.pdf_hrs)
        val presentStr = context.getString(R.string.present_1)
        val halfDayStr = context.getString(R.string.half_day)
        val absentStr = context.getString(R.string.absent_1)

        val summaryHeaders = arrayOf(
            context.getString(R.string.pdf_daily_wage),
            context.getString(R.string.pdf_total_days),
            context.getString(R.string.pdf_present_half),
            context.getString(R.string.pdf_ot_hours),
            context.getString(R.string.pdf_ot_earned),
            context.getString(R.string.pdf_gross_total),
            context.getString(R.string.pdf_net_payable)
        )

        val logHeaders = arrayOf(
            context.getString(R.string.pdf_col_date),
            context.getString(R.string.pdf_col_status),
            context.getString(R.string.pdf_col_wage),
            context.getString(R.string.pdf_col_overtime),
            context.getString(R.string.pdf_col_advance),
            context.getString(R.string.pdf_col_balance),
            context.getString(R.string.pdf_col_notes)
        )

        fun drawHeader(canvas: Canvas) {
            val headerHeight = 80f
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paintHeaderBg)

            paintTextBold.color = colorHeaderText
            paintTextBold.textSize = 24f
            paintTextBold.textAlign = Paint.Align.LEFT
            canvas.drawText(appNameStr, MARGIN, 45f, paintTextBold)

            paintTextNormal.color = colorHeaderSubText
            paintTextNormal.textSize = 10f
            canvas.drawText(data.subtitle, MARGIN, 63f, paintTextNormal)

            val rightAlign = PAGE_WIDTH - MARGIN
            paintTextBold.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(monthLabelFormat, data.monthYearStr), rightAlign, 45f, paintTextBold)
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

        fun drawFooter(c: Canvas) {
            val footerPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); color = Color.GRAY; textSize = 8f; isAntiAlias = true }
            c.drawText(generatedByStr, MARGIN, PAGE_HEIGHT - 20f, footerPaint)
        }

        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        drawHeader(canvas)
        yPos = 100f

        // Financial Summary Section
        paintTextBold.color = colorSummaryTitle
        paintTextBold.textSize = 14f
        paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText(summaryTitleStr, MARGIN, yPos, paintTextBold)
        yPos += 10f

        val summaryTableWidth = PAGE_WIDTH - (MARGIN * 2)
        val summaryCols = floatArrayOf(summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.14f, summaryTableWidth * 0.15f, summaryTableWidth * 0.15f)

        drawTableHeaders(canvas, yPos, summaryCols, summaryHeaders)
        yPos += 25f

        // Summary row
        paintTextNormal.color = Color.BLACK
        paintTextNormal.textSize = 10f
        paintTextNormal.textAlign = Paint.Align.CENTER
        val summaryRowHeight = 30f

        val summaryValues = arrayOf(
            "$rsStr ${AmountFormatter.format(data.dailyWage)}",
            "${data.totalManDays}",
            context.getString(R.string.pdf_present_half_value, data.totalPresent, data.totalHalfDays),
            "${data.totalOvertimeHours} $hrsStr",
            "$rsStr ${AmountFormatter.format(data.totalOvertimeEarnings)}",
            "$rsStr ${AmountFormatter.format(data.grossEarned)}",
            "$rsStr ${AmountFormatter.format(data.netPayable)}"
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
        canvas.drawText(ledgerTitleStr, MARGIN, yPos, paintTextBold)
        yPos += 10f

        val logCols = floatArrayOf(
            summaryTableWidth * 0.10f, // Date
            summaryTableWidth * 0.10f, // Status
            summaryTableWidth * 0.10f, // Wage
            summaryTableWidth * 0.10f, // Overtime
            summaryTableWidth * 0.10f, // Advance
            summaryTableWidth * 0.10f, // Balance
            summaryTableWidth * 0.40f  // Notes
        )

        drawTableHeaders(canvas, yPos, logCols, logHeaders)
        yPos += 25f

        // Draw logs with multi-page handling and text wrapping for notes
        for ((index, log) in data.logs.withIndex()) {
            // Measure note height
            val safeNote = if (log.note.isBlank()) "-" else log.note
            val noteWidth = logCols[6] - 10f
            val staticLayout = StaticLayout.Builder.obtain(safeNote, 0, safeNote.length, textPaint, noteWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            val rowHeight = Math.max(22f, staticLayout.height + 10f)

            if (yPos + rowHeight > PAGE_HEIGHT - MARGIN - 30f) {
                drawFooter(canvas)
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                drawHeader(canvas)
                paintTextNormal.color = Color.BLACK
                paintTextBold.color = Color.BLACK
                yPos = 100f
                drawTableHeaders(canvas, yPos, logCols, logHeaders)
                yPos += 25f
            }

            paintRectFill.color = if (index % 2 == 0) Color.WHITE else colorStripedRow
            canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + rowHeight, paintRectFill)

            val vals = arrayOf(log.date, log.status, log.dailyWage, log.overtime, log.advanceAmount, log.runningBalance)
            curX = MARGIN
            paintTextNormal.textSize = 9f
            paintTextNormal.textAlign = Paint.Align.CENTER

            for (i in vals.indices) {
                when (i) {
                    1 -> { // Status
                        if (vals[i].contains("Present") || vals[i].contains(presentStr)) {
                            paintTextNormal.color = colorPositive
                        } else if (vals[i].contains("Half Day") || vals[i].contains(halfDayStr)) {
                            paintTextNormal.color = colorHalfDay
                        } else if (vals[i].contains("Absent") || vals[i].contains(absentStr)) {
                            paintTextNormal.color = colorNegative
                        } else {
                            paintTextNormal.color = Color.BLACK
                        }
                    }
                    3 -> { // Overtime
                        if (vals[i].isNotBlank() && vals[i] != "-" && vals[i] != "0" && !vals[i].startsWith("$rsStr 0")) {
                            paintTextNormal.color = colorOvertime
                        } else {
                            paintTextNormal.color = Color.BLACK
                        }
                    }
                    4 -> { // Advance
                        if (vals[i].isNotBlank() && vals[i] != "-" && vals[i] != "0" && !vals[i].startsWith("$rsStr 0")) {
                            paintTextNormal.color = colorAdvance
                        } else {
                            paintTextNormal.color = Color.BLACK
                        }
                    }
                    5 -> { // Balance
                        paintTextNormal.color = colorPositive
                    }
                    else -> paintTextNormal.color = Color.BLACK
                }

                val textBaseline = yPos + (rowHeight / 2f) - ((paintTextNormal.descent() + paintTextNormal.ascent()) / 2f)
                canvas.drawText(vals[i], curX + logCols[i] / 2f, textBaseline, paintTextNormal)
                canvas.drawRect(curX, yPos, curX + logCols[i], yPos + rowHeight, paintRectBorder)
                curX += logCols[i]
            }

            // Draw Wrapped Note
            textPaint.color = Color.BLACK
            canvas.save()
            val noteYOffset = (rowHeight - staticLayout.height) / 2f
            canvas.translate(curX + 5f, yPos + noteYOffset)
            staticLayout.draw(canvas)
            canvas.restore()
            canvas.drawRect(curX, yPos, curX + logCols[6], yPos + rowHeight, paintRectBorder)

            yPos += rowHeight
        }

        drawFooter(canvas)
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
