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
    val totalAbsent: Int, // added
    val totalOvertimeHours: Int,
    val totalOvertimeEarnings: Double,
    val grossEarned: Double,
    val advanceDeducted: Double,
    val netPayable: Double,
    val periodStr: String, // added
    val generatedAtStr: String, // added
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

    private const val PAGE_WIDTH = 595 // A4 Portrait width at 72 PPI
    private const val PAGE_HEIGHT = 842 // A4 Portrait height at 72 PPI
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN // 523f

    // Typography constants
    private const val FONT_TITLE_SIZE = 18f
    private const val FONT_SUBTITLE_SIZE = 11f
    private const val FONT_SECTION_SIZE = 12f
    private const val FONT_BODY_BOLD_SIZE = 8.5f
    private const val FONT_BODY_NORMAL_SIZE = 8f
    private const val FONT_FOOTER_SIZE = 8f

    // Theme colors
    private val colorPrimary = Color.parseColor("#13B28A") // Emerald Green Primary
    private val colorTextPrimary = Color.parseColor("#0F172A") // Slate 900
    private val colorTextSecondary = Color.parseColor("#475569") // Slate 600
    private val colorBorder = Color.parseColor("#E2E8F0") // Slate 200
    private val colorLightBg = Color.parseColor("#F8FAFB") // Light clean bg
    private val colorWhite = Color.WHITE

    private val colorPresent = Color.parseColor("#0F9F7A")
    private val colorAbsent = Color.parseColor("#DC2626")
    private val colorHalfDay = Color.parseColor("#EA580C")
    private val colorOvertime = Color.parseColor("#2563EB")

    private val TABLE_COL_WIDTHS = floatArrayOf(65f, 75f, 65f, 75f, 65f, 178f) // Sums exactly to 523f

    private class PdfRenderContext(
        val context: Context,
        val document: PdfDocument,
        val title: String,
        val subtitle: String
    ) {
        var pageNumber = 1
        var yPos = 0f
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null

        val paintTextBold = Paint().apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        val paintTextNormal = Paint().apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }
        val paintRectBorder = Paint().apply {
            color = colorBorder
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        val paintRectFill = Paint().apply {
            style = Paint.Style.FILL
        }
        val textPaintNormal = TextPaint().apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
            textSize = FONT_BODY_NORMAL_SIZE
        }
        val textPaintBold = TextPaint().apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
            textSize = FONT_BODY_BOLD_SIZE
        }

        fun startNewPage() {
            if (page != null) {
                drawFooter()
                document.finishPage(page)
            }
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page!!.canvas
            yPos = MARGIN
            drawHeader()
            pageNumber++
        }

        fun checkPageBreak(requiredHeight: Float, onNewPageTableHeaders: (() -> Unit)? = null) {
            // Leaves safe space for footer and bottom margin
            if (yPos + requiredHeight > PAGE_HEIGHT - MARGIN - 30f) {
                startNewPage()
                onNewPageTableHeaders?.invoke()
            }
        }

        fun drawHeader() {
            val canvas = this.canvas ?: return

            if (pageNumber == 1) {
                val logoDrawable = try {
                    context.resources.getDrawable(R.mipmap.ic_launcher_foreground, null)
                } catch (e: Exception) {
                    null
                }

                val logoSize = 40f
                val textStartX = if (logoDrawable != null) {
                    logoDrawable.setBounds(MARGIN.toInt(), yPos.toInt(), (MARGIN + logoSize).toInt(), (yPos + logoSize).toInt())
                    logoDrawable.draw(canvas)
                    MARGIN + logoSize + 10f
                } else {
                    MARGIN
                }

                // Line 1: WorkDaily
                paintTextBold.color = colorPrimary
                paintTextBold.textSize = FONT_TITLE_SIZE
                paintTextBold.textAlign = Paint.Align.LEFT
                canvas.drawText("WorkDaily", textStartX, yPos + 18f, paintTextBold)

                // Line 2: User Name
                paintTextNormal.color = colorTextPrimary
                paintTextNormal.textSize = FONT_SUBTITLE_SIZE
                paintTextNormal.textAlign = Paint.Align.LEFT
                canvas.drawText(subtitle, textStartX, yPos + 34f, paintTextNormal)

                // Line 3: Report title
                paintTextBold.color = colorTextSecondary
                paintTextBold.textSize = FONT_SUBTITLE_SIZE
                paintTextBold.textAlign = Paint.Align.RIGHT
                canvas.drawText(title, PAGE_WIDTH - MARGIN, yPos + 24f, paintTextBold)

                // Separator
                yPos += 48f
                paintRectBorder.color = colorBorder
                canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paintRectBorder)
                yPos += 16f
            } else {
                // Continuation compact header
                paintTextBold.color = colorTextSecondary
                paintTextBold.textSize = FONT_BODY_BOLD_SIZE
                paintTextBold.textAlign = Paint.Align.LEFT
                canvas.drawText("WorkDaily - $title", MARGIN, yPos + 8f, paintTextBold)

                paintTextNormal.color = colorTextSecondary
                paintTextNormal.textSize = FONT_BODY_NORMAL_SIZE
                paintTextNormal.textAlign = Paint.Align.RIGHT
                canvas.drawText(subtitle, PAGE_WIDTH - MARGIN, yPos + 8f, paintTextNormal)

                yPos += 14f
                paintRectBorder.color = colorBorder
                canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paintRectBorder)
                yPos += 12f
            }
        }

        fun drawFooter() {
            val canvas = this.canvas ?: return
            val footerY = PAGE_HEIGHT - 25f

            paintRectBorder.color = colorBorder
            canvas.drawLine(MARGIN, footerY - 10f, PAGE_WIDTH - MARGIN, footerY - 10f, paintRectBorder)

            paintTextNormal.color = colorTextSecondary
            paintTextNormal.textSize = FONT_FOOTER_SIZE

            // Left
            paintTextNormal.textAlign = Paint.Align.LEFT
            canvas.drawText(context.getString(R.string.pdf_generated_by), MARGIN, footerY, paintTextNormal)

            // Right
            paintTextNormal.textAlign = Paint.Align.RIGHT
            val pageLabel = context.getString(R.string.pdf_page, pageNumber)
            canvas.drawText(pageLabel, PAGE_WIDTH - MARGIN, footerY, paintTextNormal)
        }

        fun finish() {
            if (page != null) {
                drawFooter()
                document.finishPage(page)
            }
        }
    }

    private fun fitTextToWidth(
        paint: Paint,
        text: String,
        maxWidth: Float,
        baseSize: Float,
        minSize: Float = 5.0f
    ): Float {
        paint.textSize = baseSize
        var size = baseSize
        while (size > minSize && paint.measureText(text) > maxWidth) {
            size -= 0.5f
            paint.textSize = size
        }
        return size
    }

    private fun drawTableHeaders(renderContext: PdfRenderContext, headers: Array<String>, colWidths: FloatArray) {
        val headerHeight = 22f
        renderContext.paintRectFill.color = colorPrimary
        renderContext.canvas?.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + headerHeight, renderContext.paintRectFill)

        renderContext.paintTextBold.color = colorWhite

        var currentX = MARGIN
        for (i in headers.indices) {
            val align = when (i) {
                0, 1 -> Paint.Align.CENTER
                2, 3, 4 -> Paint.Align.RIGHT
                else -> Paint.Align.LEFT
            }
            renderContext.paintTextBold.textAlign = align
            val textX = when (align) {
                Paint.Align.CENTER -> currentX + colWidths[i] / 2f
                Paint.Align.RIGHT -> currentX + colWidths[i] - 6f
                else -> currentX + 6f
            }

            val maxHeaderWidth = colWidths[i] - 4f
            val headerTextSize = fitTextToWidth(renderContext.paintTextBold, headers[i], maxHeaderWidth, 8.5f, 5f)
            renderContext.paintTextBold.textSize = headerTextSize

            renderContext.canvas?.drawText(headers[i], textX, renderContext.yPos + 14f, renderContext.paintTextBold)
            renderContext.canvas?.drawRect(currentX, renderContext.yPos, currentX + colWidths[i], renderContext.yPos + headerHeight, renderContext.paintRectBorder)
            currentX += colWidths[i]
        }
        renderContext.yPos += headerHeight
    }

    private fun drawTableRow(
        renderContext: PdfRenderContext,
        headers: Array<String>,
        colWidths: FloatArray,
        index: Int,
        vals: Array<String>,
        rawStatus: String,
        isHalfDay: Boolean,
        hasOvertime: Boolean,
        hasAdvance: Boolean,
        noteText: String,
        presentStr: String,
        halfDayStr: String,
        absentStr: String
    ) {
        val safeNote = if (noteText.isBlank()) "-" else noteText
        val noteColWidth = colWidths[5] - 10f

        val staticLayout = StaticLayout.Builder.obtain(safeNote, 0, safeNote.length, renderContext.textPaintNormal, noteColWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        val rowHeight = Math.max(20f, staticLayout.height + 8f)

        renderContext.checkPageBreak(rowHeight) {
            drawTableHeaders(renderContext, headers, colWidths)
        }

        val canvas = renderContext.canvas!!
        renderContext.paintRectFill.color = if (index % 2 == 0) colorWhite else colorLightBg
        canvas.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + rowHeight, renderContext.paintRectFill)

        var curX = MARGIN

        for (i in vals.indices) {
            val align = when (i) {
                0, 1 -> Paint.Align.CENTER
                2, 3, 4 -> Paint.Align.RIGHT
                else -> Paint.Align.LEFT
            }
            renderContext.paintTextNormal.textAlign = align

            val textX = when (align) {
                Paint.Align.CENTER -> curX + colWidths[i] / 2f
                Paint.Align.RIGHT -> curX + colWidths[i] - 6f
                else -> curX + 6f
            }

            when (i) {
                1 -> { // Status
                    if (rawStatus == "present" || vals[i].contains(presentStr) || vals[i].contains("Present")) {
                        renderContext.paintTextNormal.color = if (isHalfDay || vals[i].contains(halfDayStr) || vals[i].contains("Half")) colorHalfDay else colorPresent
                    } else if (rawStatus == "absent" || vals[i].contains(absentStr) || vals[i].contains("Absent")) {
                        renderContext.paintTextNormal.color = colorAbsent
                    } else {
                        renderContext.paintTextNormal.color = colorTextPrimary
                    }
                }
                3 -> { // Overtime
                    renderContext.paintTextNormal.color = if (hasOvertime) colorOvertime else colorTextPrimary
                }
                4 -> { // Advance
                    renderContext.paintTextNormal.color = if (hasAdvance) colorHalfDay else colorTextPrimary
                }
                else -> renderContext.paintTextNormal.color = colorTextPrimary
            }

            val maxTextWidth = colWidths[i] - 8f
            val fittedSize = fitTextToWidth(renderContext.paintTextNormal, vals[i], maxTextWidth, 8f, 5f)
            renderContext.paintTextNormal.textSize = fittedSize

            val textBaseline = renderContext.yPos + (rowHeight / 2f) - ((renderContext.paintTextNormal.descent() + renderContext.paintTextNormal.ascent()) / 2f)
            canvas.drawText(vals[i], textX, textBaseline, renderContext.paintTextNormal)
            canvas.drawRect(curX, renderContext.yPos, curX + colWidths[i], renderContext.yPos + rowHeight, renderContext.paintRectBorder)
            curX += colWidths[i]
        }

        // Draw notes
        renderContext.textPaintNormal.color = colorTextPrimary
        canvas.save()
        val noteYOffset = (rowHeight - staticLayout.height) / 2f
        canvas.translate(curX + 5f, renderContext.yPos + noteYOffset)
        staticLayout.draw(canvas)
        canvas.restore()
        canvas.drawRect(curX, renderContext.yPos, curX + colWidths[5], renderContext.yPos + rowHeight, renderContext.paintRectBorder)

        renderContext.yPos += rowHeight
    }

    private fun drawOverallSummary(renderContext: PdfRenderContext, items: List<Pair<String, String>>) {
        val canvas = renderContext.canvas ?: return
        val colWidth = CONTENT_WIDTH / 4f
        val summaryRowHeight = 36f

        for (row in 0 until 2) {
            val yT = renderContext.yPos + row * summaryRowHeight
            for (col in 0 until 4) {
                val idx = row * 4 + col
                if (idx >= items.size) break
                val item = items[idx]
                val xL = MARGIN + col * colWidth

                renderContext.paintRectFill.color = if (idx % 2 == 0) colorLightBg else colorWhite
                canvas.drawRect(xL, yT, xL + colWidth, yT + summaryRowHeight, renderContext.paintRectFill)

                renderContext.paintRectBorder.color = colorBorder
                canvas.drawRect(xL, yT, xL + colWidth, yT + summaryRowHeight, renderContext.paintRectBorder)

                // Label
                renderContext.paintTextNormal.color = colorTextSecondary
                val labelSize = fitTextToWidth(renderContext.paintTextNormal, item.first, colWidth - 8f, 7.5f, 5f)
                renderContext.paintTextNormal.textSize = labelSize
                renderContext.paintTextNormal.textAlign = Paint.Align.CENTER
                canvas.drawText(item.first, xL + colWidth / 2f, yT + 12f, renderContext.paintTextNormal)

                // Value
                renderContext.paintTextBold.color = when (idx) {
                    0 -> colorPresent
                    3 -> colorHalfDay
                    4 -> colorPresent
                    6 -> colorAbsent
                    else -> colorTextPrimary
                }
                val valSize = fitTextToWidth(renderContext.paintTextBold, item.second, colWidth - 8f, 9.5f, 5f)
                renderContext.paintTextBold.textSize = valSize
                renderContext.paintTextBold.textAlign = Paint.Align.CENTER
                canvas.drawText(item.second, xL + colWidth / 2f, yT + 27f, renderContext.paintTextBold)
            }
        }
        renderContext.yPos += summaryRowHeight * 2
    }

    private fun drawYearHeader(renderContext: PdfRenderContext, yearStr: String) {
        val canvas = renderContext.canvas ?: return
        renderContext.checkPageBreak(30f)
        renderContext.paintTextBold.color = colorTextPrimary
        renderContext.paintTextBold.textSize = FONT_SECTION_SIZE
        renderContext.paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText(yearStr, MARGIN, renderContext.yPos + 15f, renderContext.paintTextBold)
        renderContext.yPos += 22f
    }

    private fun drawMonthHeader(renderContext: PdfRenderContext, monthStr: String) {
        val canvas = renderContext.canvas ?: return
        renderContext.checkPageBreak(25f)
        renderContext.paintTextBold.color = colorPrimary
        renderContext.paintTextBold.textSize = 10f
        renderContext.paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText(monthStr, MARGIN, renderContext.yPos + 10f, renderContext.paintTextBold)
        renderContext.yPos += 16f
    }

    private fun drawMonthlySummaryStrip(
        renderContext: PdfRenderContext,
        earnings: Double,
        salary: Double,
        overtime: Double,
        advance: Double,
        present: Int,
        half: Int,
        absent: Int
    ) {
        val canvas = renderContext.canvas ?: return
        renderContext.checkPageBreak(30f)

        val colWidth = CONTENT_WIDTH / 7f
        val height = 24f

        val rsStr = renderContext.context.getString(R.string.pdf_rs)

        val items = listOf(
            Pair(renderContext.context.getString(R.string.pdf_earnings), "$rsStr${AmountFormatter.format(earnings)}"),
            Pair(renderContext.context.getString(R.string.salary), "$rsStr${AmountFormatter.format(salary)}"),
            Pair(renderContext.context.getString(R.string.overtime), "$rsStr${AmountFormatter.format(overtime)}"),
            Pair(renderContext.context.getString(R.string.pdf_advance), "$rsStr${AmountFormatter.format(advance)}"),
            Pair(renderContext.context.getString(R.string.present_1), present.toString()),
            Pair(renderContext.context.getString(R.string.half_day), half.toString()),
            Pair(renderContext.context.getString(R.string.absent_1), absent.toString())
        )

        renderContext.paintRectFill.color = Color.parseColor("#E6F4F1")
        canvas.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + height, renderContext.paintRectFill)

        renderContext.paintRectBorder.color = colorBorder
        canvas.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + height, renderContext.paintRectBorder)

        for (i in items.indices) {
            val item = items[i]
            val xL = MARGIN + i * colWidth

            if (i > 0) {
                canvas.drawLine(xL, renderContext.yPos, xL, renderContext.yPos + height, renderContext.paintRectBorder)
            }

            renderContext.paintTextNormal.color = colorTextSecondary
            val labelSize = fitTextToWidth(renderContext.paintTextNormal, item.first, colWidth - 4f, 6.5f, 5.0f)
            renderContext.paintTextNormal.textSize = labelSize
            renderContext.paintTextNormal.textAlign = Paint.Align.CENTER
            canvas.drawText(item.first, xL + colWidth / 2f, renderContext.yPos + 9f, renderContext.paintTextNormal)

            renderContext.paintTextBold.color = when (i) {
                0, 4 -> colorPresent
                3, 5 -> colorHalfDay
                6 -> colorAbsent
                else -> colorTextPrimary
            }
            val valSize = fitTextToWidth(renderContext.paintTextBold, item.second, colWidth - 4f, 8f, 5.0f)
            renderContext.paintTextBold.textSize = valSize
            renderContext.paintTextBold.textAlign = Paint.Align.CENTER
            canvas.drawText(item.second, xL + colWidth / 2f, renderContext.yPos + 20f, renderContext.paintTextBold)
        }

        renderContext.yPos += height + 8f
    }

    private fun drawClosingBranding(renderContext: PdfRenderContext) {
        val canvas = renderContext.canvas ?: return
        val brandingHeight = 80f

        renderContext.checkPageBreak(brandingHeight)

        renderContext.paintRectBorder.color = colorBorder
        canvas.drawLine(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos, renderContext.paintRectBorder)
        renderContext.yPos += 20f

        val logoDrawable = try {
            renderContext.context.resources.getDrawable(R.mipmap.ic_launcher_foreground, null)
        } catch (e: Exception) {
            null
        }

        if (logoDrawable != null) {
            val logoSize = 36f
            val logoLeft = (PAGE_WIDTH - logoSize) / 2f
            logoDrawable.setBounds(logoLeft.toInt(), renderContext.yPos.toInt(), (logoLeft + logoSize).toInt(), (renderContext.yPos + logoSize).toInt())
            logoDrawable.draw(canvas)
            renderContext.yPos += logoSize + 8f
        }

        renderContext.paintTextBold.color = colorPrimary
        renderContext.paintTextBold.textSize = 12f
        renderContext.paintTextBold.textAlign = Paint.Align.CENTER
        canvas.drawText("WorkDaily", PAGE_WIDTH / 2f, renderContext.yPos, renderContext.paintTextBold)
        renderContext.yPos += 14f
    }

    fun generatePdf(context: Context, data: PdfData): File? {
        val document = PdfDocument()
        val titleStr = context.getString(R.string.pdf_subtitle_passbook).uppercase()
        val renderContext = PdfRenderContext(context, document, titleStr, data.name)

        renderContext.startNewPage()
        val canvas = renderContext.canvas!!

        // Period & Metadata (page 1 only)
        renderContext.paintTextNormal.color = colorTextSecondary
        renderContext.paintTextNormal.textSize = FONT_BODY_NORMAL_SIZE
        renderContext.paintTextNormal.textAlign = Paint.Align.LEFT
        val periodLabel = context.getString(R.string.pdf_period, data.periodStr.substringBefore("—").trim(), data.periodStr.substringAfter("—").trim())
        canvas.drawText(periodLabel, MARGIN, renderContext.yPos, renderContext.paintTextNormal)

        renderContext.paintTextNormal.textAlign = Paint.Align.RIGHT
        canvas.drawText(data.generatedAtStr, PAGE_WIDTH - MARGIN, renderContext.yPos, renderContext.paintTextNormal)
        renderContext.yPos += 20f

        // Month Summary Section
        val summaryTitleStr = context.getString(R.string.pdf_summary_title)
        renderContext.paintTextBold.color = colorTextPrimary
        renderContext.paintTextBold.textSize = FONT_SECTION_SIZE
        renderContext.paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText(summaryTitleStr, MARGIN, renderContext.yPos, renderContext.paintTextBold)
        renderContext.yPos += 10f

        val baseSalary = data.grossEarned - data.totalOvertimeEarnings
        val rsStr = context.getString(R.string.pdf_rs)
        val items = listOf(
            Pair(context.getString(R.string.pdf_earnings), "$rsStr ${AmountFormatter.format(data.grossEarned)}"),
            Pair(context.getString(R.string.salary), "$rsStr ${AmountFormatter.format(baseSalary)}"),
            Pair(context.getString(R.string.overtime), "$rsStr ${AmountFormatter.format(data.totalOvertimeEarnings)}"),
            Pair(context.getString(R.string.pdf_advance), "$rsStr ${AmountFormatter.format(data.advanceDeducted)}"),
            Pair(context.getString(R.string.present_1), data.totalPresent.toString()),
            Pair(context.getString(R.string.half_day), data.totalHalfDays.toString()),
            Pair(context.getString(R.string.absent_1), data.totalAbsent.toString()),
            Pair(context.getString(R.string.pdf_working_days), AmountFormatter.format(data.totalManDays))
        )

        drawOverallSummary(renderContext, items)
        renderContext.yPos += 25f

        // Ledger Table Section
        val ledgerTitleStr = context.getString(R.string.pdf_ledger_title)
        renderContext.paintTextBold.color = colorTextPrimary
        renderContext.paintTextBold.textSize = FONT_SECTION_SIZE
        renderContext.paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText(ledgerTitleStr, MARGIN, renderContext.yPos, renderContext.paintTextBold)
        renderContext.yPos += 10f

        val logHeaders = arrayOf(
            context.getString(R.string.pdf_col_date),
            context.getString(R.string.pdf_col_status),
            context.getString(R.string.pdf_col_wage),
            context.getString(R.string.pdf_col_overtime),
            context.getString(R.string.pdf_col_advance),
            context.getString(R.string.pdf_col_notes)
        )

        drawTableHeaders(renderContext, logHeaders, TABLE_COL_WIDTHS)

        val presentStr = context.getString(R.string.present_1)
        val halfDayStr = context.getString(R.string.half_day)
        val absentStr = context.getString(R.string.absent_1)

        for ((index, log) in data.logs.withIndex()) {
            val isHalfDay = log.status.contains(halfDayStr) || log.status.contains("Half")
            val hasOvertime = log.overtime.isNotBlank() && log.overtime != "-"
            val hasAdvance = log.advanceAmount.isNotBlank() && log.advanceAmount != "-"

            val vals = arrayOf(
                log.date,
                log.status,
                log.dailyWage,
                log.overtime,
                log.advanceAmount
            )

            drawTableRow(
                renderContext = renderContext,
                headers = logHeaders,
                colWidths = TABLE_COL_WIDTHS,
                index = index,
                vals = vals,
                rawStatus = if (log.status.contains(presentStr)) "present" else if (log.status.contains(absentStr)) "absent" else "",
                isHalfDay = isHalfDay,
                hasOvertime = hasOvertime,
                hasAdvance = hasAdvance,
                noteText = log.note,
                presentStr = presentStr,
                halfDayStr = halfDayStr,
                absentStr = absentStr
            )
        }

        drawClosingBranding(renderContext)

        renderContext.finish()

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

    fun generateAllTimePdf(context: Context, data: AllTimePassbookPdfModel): File? {
        val document = PdfDocument()
        val titleStr = context.getString(R.string.pdf_all_time_passbook).uppercase()
        val renderContext = PdfRenderContext(context, document, titleStr, data.workerName)

        renderContext.startNewPage()
        val canvas = renderContext.canvas!!

        // Period & Metadata (page 1 only)
        renderContext.paintTextNormal.color = colorTextSecondary
        renderContext.paintTextNormal.textSize = FONT_BODY_NORMAL_SIZE
        renderContext.paintTextNormal.textAlign = Paint.Align.LEFT
        val periodLabel = context.getString(R.string.pdf_period, data.firstRecordDateStr, data.latestRecordDateStr)
        canvas.drawText(periodLabel, MARGIN, renderContext.yPos, renderContext.paintTextNormal)

        renderContext.paintTextNormal.textAlign = Paint.Align.RIGHT
        canvas.drawText(data.generatedAtStr, PAGE_WIDTH - MARGIN, renderContext.yPos, renderContext.paintTextNormal)
        renderContext.yPos += 20f

        // Overall Summary Section
        val summaryTitleStr = context.getString(R.string.pdf_overall_summary)
        renderContext.paintTextBold.color = colorTextPrimary
        renderContext.paintTextBold.textSize = FONT_SECTION_SIZE
        renderContext.paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText(summaryTitleStr, MARGIN, renderContext.yPos, renderContext.paintTextBold)
        renderContext.yPos += 10f

        val rsStr = context.getString(R.string.pdf_rs)
        val hrsStr = context.getString(R.string.pdf_hrs)

        val items = listOf(
            Pair(context.getString(R.string.pdf_earnings), "$rsStr ${AmountFormatter.format(data.totalEarnings)}"),
            Pair(context.getString(R.string.pdf_salary), "$rsStr ${AmountFormatter.format(data.totalSalary)}"),
            Pair(context.getString(R.string.pdf_overtime), "$rsStr ${AmountFormatter.format(data.totalOvertime)}"),
            Pair(context.getString(R.string.pdf_advance), "$rsStr ${AmountFormatter.format(data.totalAdvance)}"),
            Pair(context.getString(R.string.pdf_present), data.totalPresent.toString()),
            Pair(context.getString(R.string.pdf_half), data.totalHalfDays.toString()),
            Pair(context.getString(R.string.pdf_absent), data.totalAbsent.toString()),
            Pair(context.getString(R.string.pdf_working_days), AmountFormatter.format(data.totalWorkingDays))
        )

        drawOverallSummary(renderContext, items)
        renderContext.yPos += 25f

        val logHeaders = arrayOf(
            context.getString(R.string.pdf_col_date),
            context.getString(R.string.pdf_col_status),
            context.getString(R.string.pdf_col_wage),
            context.getString(R.string.pdf_col_overtime),
            context.getString(R.string.pdf_col_advance),
            context.getString(R.string.pdf_col_notes)
        )

        val presentStr = context.getString(R.string.present_1)
        val halfDayStr = context.getString(R.string.half_day)
        val absentStr = context.getString(R.string.absent_1)

        // Render Year Groups
        for (yearGroup in data.years) {
            val yearHeaderStr = context.getString(R.string.pdf_year, yearGroup.year)
            drawYearHeader(renderContext, yearHeaderStr)

            for (monthGroup in yearGroup.months) {
                // Month Title
                val monthHeaderStr = context.getString(R.string.pdf_month, monthGroup.monthName)
                drawMonthHeader(renderContext, monthHeaderStr)

                // Monthly Summary strip
                drawMonthlySummaryStrip(
                    renderContext = renderContext,
                    earnings = monthGroup.totalEarnings,
                    salary = monthGroup.totalSalary,
                    overtime = monthGroup.totalOvertime,
                    advance = monthGroup.totalAdvance,
                    present = monthGroup.presentCount,
                    half = monthGroup.halfCount,
                    absent = monthGroup.absentCount
                )

                // Table Headers
                drawTableHeaders(renderContext, logHeaders, TABLE_COL_WIDTHS)

                for ((index, record) in monthGroup.records.withIndex()) {
                    val otText = if (record.overtimeAmount > 0) {
                        if (record.overtimeHours > 0) "${record.overtimeHours} $hrsStr / $rsStr ${AmountFormatter.format(record.overtimeAmount)}"
                        else "$rsStr ${AmountFormatter.format(record.overtimeAmount)}"
                    } else "-"

                    val rVals = arrayOf(
                        record.dateStr,
                        record.statusStr,
                        "$rsStr ${AmountFormatter.format(record.baseEarnings)}",
                        otText,
                        if (record.advanceAmount > 0) "$rsStr ${AmountFormatter.format(record.advanceAmount)}" else "-"
                    )

                    drawTableRow(
                        renderContext = renderContext,
                        headers = logHeaders,
                        colWidths = TABLE_COL_WIDTHS,
                        index = index,
                        vals = rVals,
                        rawStatus = record.statusRaw,
                        isHalfDay = record.isHalfDay,
                        hasOvertime = record.overtimeAmount > 0,
                        hasAdvance = record.advanceAmount > 0,
                        noteText = record.note,
                        presentStr = presentStr,
                        halfDayStr = halfDayStr,
                        absentStr = absentStr
                    )
                }
                renderContext.yPos += 15f
            }
        }

        drawClosingBranding(renderContext)

        renderContext.finish()

        val fileName = "all_time_passbook_${data.workerName.replace(" ", "_")}.pdf"
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
