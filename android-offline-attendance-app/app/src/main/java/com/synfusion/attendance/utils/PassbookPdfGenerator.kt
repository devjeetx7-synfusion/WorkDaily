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
    val settlement: FinancialSettlementResult? = null,
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹",
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

    private class PdfRenderContext(
        val context: Context,
        val document: PdfDocument,
        val title: String,
        val subtitle: String
    ) {
        var pageNumber = 0
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
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page!!.canvas
            yPos = MARGIN

            if (pageNumber == 1) {
                drawFirstPageHeader()
            } else {
                drawContinuationHeader()
            }
        }

        fun checkPageBreak(requiredHeight: Float, onNewPageTableHeaders: (() -> Unit)? = null) {
            // Leaves safe space for footer and bottom margin
            if (yPos + requiredHeight > PAGE_HEIGHT - MARGIN - 30f) {
                startNewPage()
                onNewPageTableHeaders?.invoke()
            }
        }

        private fun drawFirstPageHeader() {
            val canvas = this.canvas ?: return

            val logoDrawable = try {
                context.resources.getDrawable(R.drawable.logo_transparent, null)
            } catch (e: Exception) {
                null
            }

            val textStartX = if (logoDrawable != null) {
                val logoSize = 44f
                logoDrawable.setBounds(MARGIN.toInt(), yPos.toInt(), (MARGIN + logoSize).toInt(), (yPos + logoSize).toInt())
                logoDrawable.draw(canvas)
                MARGIN + logoSize + 12f
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
            yPos += 52f
            paintRectBorder.color = colorBorder
            canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paintRectBorder)
            yPos += 16f
        }

        private fun drawContinuationHeader() {
            val canvas = this.canvas ?: return

            // Compact header for page 2+
            paintTextNormal.color = colorTextPrimary
            paintTextNormal.textSize = 10f
            paintTextNormal.textAlign = Paint.Align.LEFT
            canvas.drawText("WorkDaily - " + subtitle, MARGIN, yPos + 10f, paintTextNormal)

            paintTextNormal.textAlign = Paint.Align.RIGHT
            canvas.drawText(title, PAGE_WIDTH - MARGIN, yPos + 10f, paintTextNormal)

            // Separator
            yPos += 20f
            paintRectBorder.color = colorBorder
            canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paintRectBorder)
            yPos += 16f
        }

        private fun drawFooter() {
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

    private fun drawTableHeaders(renderContext: PdfRenderContext, headers: Array<String>, colWidths: FloatArray) {
        val headerHeight = 22f
        renderContext.paintRectFill.color = colorPrimary
        renderContext.canvas?.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + headerHeight, renderContext.paintRectFill)

        renderContext.paintTextBold.color = colorWhite
        renderContext.paintTextBold.textSize = 8.5f

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
            renderContext.canvas?.drawText(headers[i], textX, renderContext.yPos + 14f, renderContext.paintTextBold)
            renderContext.canvas?.drawRect(currentX, renderContext.yPos, currentX + colWidths[i], renderContext.yPos + headerHeight, renderContext.paintRectBorder)
            currentX += colWidths[i]
        }
        renderContext.yPos += headerHeight
    }

    private fun scaleTextToFit(paint: Paint, text: String, maxWidth: Float, minSize: Float, defaultSize: Float) {
        paint.textSize = defaultSize
        var width = paint.measureText(text)
        while (width > maxWidth && paint.textSize > minSize) {
            paint.textSize -= 0.5f
            width = paint.measureText(text)
        }
    }

    private fun drawClosingBranding(renderContext: PdfRenderContext, name: String) {
        val brandingHeight = 100f

        renderContext.checkPageBreak(brandingHeight)

        val canvas = renderContext.canvas ?: return

        // Separator line
        renderContext.paintRectBorder.color = colorBorder
        canvas.drawLine(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos, renderContext.paintRectBorder)
        renderContext.yPos += 20f

        val logoDrawable = try {
            renderContext.context.resources.getDrawable(R.drawable.logo_transparent, null)
        } catch (e: Exception) {
            null
        }

        if (logoDrawable != null) {
            val logoSize = 40f
            val logoLeft = (PAGE_WIDTH - logoSize) / 2f
            logoDrawable.setBounds(logoLeft.toInt(), renderContext.yPos.toInt(), (logoLeft + logoSize).toInt(), (renderContext.yPos + logoSize).toInt())
            logoDrawable.draw(canvas)
            renderContext.yPos += logoSize + 10f
        }

        renderContext.paintTextBold.color = colorPrimary
        renderContext.paintTextBold.textSize = 12f
        renderContext.paintTextBold.textAlign = Paint.Align.CENTER
        canvas.drawText("WorkDaily", PAGE_WIDTH / 2f, renderContext.yPos, renderContext.paintTextBold)
        renderContext.yPos += 14f
    }

    private fun drawOverallSummary(renderContext: PdfRenderContext, items: List<Pair<String, String>>) {
        val canvas = renderContext.canvas ?: return
        val summaryTitleStr = renderContext.context.getString(R.string.pdf_overall_summary)
        renderContext.paintTextBold.color = colorTextPrimary
        renderContext.paintTextBold.textSize = FONT_SECTION_SIZE
        renderContext.paintTextBold.textAlign = Paint.Align.LEFT
        canvas.drawText(summaryTitleStr, MARGIN, renderContext.yPos, renderContext.paintTextBold)
        renderContext.yPos += 10f

        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 4f
        val summaryRowHeight = 36f

        for (row in 0 until 2) {
            val yT = renderContext.yPos + row * summaryRowHeight
            for (col in 0 until 4) {
                val idx = row * 4 + col
                if (idx >= items.size) continue
                val item = items[idx]
                val xL = MARGIN + col * colWidth

                renderContext.paintRectFill.color = if (idx % 2 == 0) colorLightBg else colorWhite
                canvas.drawRect(xL, yT, xL + colWidth, yT + summaryRowHeight, renderContext.paintRectFill)
                canvas.drawRect(xL, yT, xL + colWidth, yT + summaryRowHeight, renderContext.paintRectBorder)

                // Label
                renderContext.paintTextNormal.color = colorTextSecondary
                renderContext.paintTextNormal.textAlign = Paint.Align.CENTER
                scaleTextToFit(renderContext.paintTextNormal, item.first, colWidth - 8f, 5f, 7f)
                canvas.drawText(item.first, xL + colWidth / 2f, yT + 12f, renderContext.paintTextNormal)

                // Value
                renderContext.paintTextBold.color = when (idx) {
                    0 -> colorPresent
                    3 -> colorHalfDay
                    4 -> colorPresent
                    6 -> colorAbsent
                    else -> colorTextPrimary
                }
                renderContext.paintTextBold.textAlign = Paint.Align.CENTER
                scaleTextToFit(renderContext.paintTextBold, item.second, colWidth - 8f, 6f, 9f)
                canvas.drawText(item.second, xL + colWidth / 2f, yT + 26f, renderContext.paintTextBold)
            }
        }
        renderContext.yPos += summaryRowHeight * 2 + 25f
    }

    private fun drawMonthlySummaryStrip(renderContext: PdfRenderContext, mSumStr: String) {
        val currentCanvas = renderContext.canvas ?: return
        val mSummaryHeight = 22f
        renderContext.paintRectFill.color = Color.parseColor("#E6F4F1")
        currentCanvas.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + mSummaryHeight, renderContext.paintRectFill)
        currentCanvas.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + mSummaryHeight, renderContext.paintRectBorder)

        renderContext.paintTextNormal.color = colorTextPrimary
        renderContext.paintTextNormal.textAlign = Paint.Align.CENTER
        scaleTextToFit(renderContext.paintTextNormal, mSumStr, (PAGE_WIDTH - 2 * MARGIN) - 16f, 5f, 7.5f)
        currentCanvas.drawText(mSumStr, PAGE_WIDTH / 2f, renderContext.yPos + 14f, renderContext.paintTextNormal)
        renderContext.yPos += mSummaryHeight + 10f
    }

    fun generatePdf(context: Context, data: PdfData): File? {
        val document = PdfDocument()
        val titleStr = context.getString(R.string.pdf_subtitle_passbook).uppercase()
        val renderContext = PdfRenderContext(context, document, titleStr, data.name)

        renderContext.startNewPage()
        val canvas = renderContext.canvas!!

        // Period & Metadata
        renderContext.paintTextNormal.color = colorTextSecondary
        renderContext.paintTextNormal.textSize = FONT_BODY_NORMAL_SIZE
        renderContext.paintTextNormal.textAlign = Paint.Align.LEFT
        val periodLabel = context.getString(R.string.pdf_period, data.periodStr.substringBefore("—").trim(), data.periodStr.substringAfter("—").trim())
        canvas.drawText(periodLabel, MARGIN, renderContext.yPos, renderContext.paintTextNormal)

        renderContext.paintTextNormal.textAlign = Paint.Align.RIGHT
        canvas.drawText(data.generatedAtStr, PAGE_WIDTH - MARGIN, renderContext.yPos, renderContext.paintTextNormal)
        renderContext.yPos += 20f

        // Month Summary Section
        val rsStr = data.currencySymbol
        val baseSalary = data.grossEarned - data.totalOvertimeEarnings
        val settlementMsg = data.settlement?.settlementMessage ?: if (data.netPayable > 0) "YOU WILL RECEIVE " + AmountFormatter.formatWithSymbol(data.netPayable, data.currencyCode) else if (data.netPayable < 0) "YOU NEED TO PAY BACK " + AmountFormatter.formatWithSymbol(kotlin.math.abs(data.netPayable), data.currencyCode) else context.getString(R.string.settled_zero_pending, rsStr)

        val settlementLabel = when (data.settlement?.settlementState) {
            SettlementState.PAY_BACK -> context.getString(R.string.pdf_amount_to_pay_back)
            SettlementState.RECEIVE -> context.getString(R.string.pdf_amount_to_receive)
            else -> if (data.netPayable < 0) context.getString(R.string.pdf_amount_to_pay_back) else context.getString(R.string.pdf_amount_to_receive)
        }

        val settlementVal = when (data.settlement?.settlementState) {
            SettlementState.PAY_BACK -> AmountFormatter.formatWithSymbol(data.settlement.amountToPayBack, data.currencyCode)
            SettlementState.RECEIVE -> AmountFormatter.formatWithSymbol(data.settlement.amountToReceive, data.currencyCode)
            SettlementState.SETTLED -> "$rsStr 0"
            else -> if (data.netPayable < 0) AmountFormatter.formatWithSymbol(kotlin.math.abs(data.netPayable), data.currencyCode) else AmountFormatter.formatWithSymbol(data.netPayable, data.currencyCode)
        }

        val summaryItems = listOf(
            Pair(context.getString(R.string.pdf_earnings), AmountFormatter.formatWithSymbol(data.grossEarned, data.currencyCode)),
            Pair(context.getString(R.string.salary), AmountFormatter.formatWithSymbol(baseSalary, data.currencyCode)),
            Pair(context.getString(R.string.overtime), AmountFormatter.formatWithSymbol(data.totalOvertimeEarnings, data.currencyCode)),
            Pair(context.getString(R.string.pdf_advance), AmountFormatter.formatWithSymbol(data.advanceDeducted, data.currencyCode)),
            Pair(settlementLabel, settlementVal),
            Pair(context.getString(R.string.present_1), data.totalPresent.toString()),
            Pair(context.getString(R.string.half_day), data.totalHalfDays.toString()),
            Pair(context.getString(R.string.pdf_working_days), AmountFormatter.format(data.totalManDays))
        )

        drawOverallSummary(renderContext, summaryItems)

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

        val colWidths = floatArrayOf(65f, 75f, 65f, 75f, 65f, 178f)

        drawTableHeaders(renderContext, logHeaders, colWidths)

        val presentStr = context.getString(R.string.present_1)
        val halfDayStr = context.getString(R.string.half_day)
        val absentStr = context.getString(R.string.absent_1)

        for ((index, log) in data.logs.withIndex()) {
            val safeNote = if (log.note.isBlank()) "-" else log.note
            val noteColWidth = colWidths[5] - 10f
            val staticLayout = StaticLayout.Builder.obtain(safeNote, 0, safeNote.length, renderContext.textPaintNormal, noteColWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            val rowHeight = Math.max(20f, staticLayout.height + 8f)

            renderContext.checkPageBreak(rowHeight) {
                drawTableHeaders(renderContext, logHeaders, colWidths)
            }

            val currentCanvas = renderContext.canvas!!
            renderContext.paintRectFill.color = if (index % 2 == 0) colorWhite else colorLightBg
            currentCanvas.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + rowHeight, renderContext.paintRectFill)

            val vals = arrayOf(
                log.date,
                log.status,
                log.dailyWage,
                log.overtime,
                log.advanceAmount
            )

            var curX = MARGIN
            renderContext.paintTextNormal.textSize = 8f

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
                        if (vals[i].contains("Present") || vals[i].contains(presentStr)) {
                            renderContext.paintTextNormal.color = colorPresent
                        } else if (vals[i].contains("Half Day") || vals[i].contains(halfDayStr)) {
                            renderContext.paintTextNormal.color = colorHalfDay
                        } else if (vals[i].contains("Absent") || vals[i].contains(absentStr)) {
                            renderContext.paintTextNormal.color = colorAbsent
                        } else {
                            renderContext.paintTextNormal.color = colorTextPrimary
                        }
                    }
                    3 -> { // Overtime
                        if (vals[i].isNotBlank() && vals[i] != "-") {
                            renderContext.paintTextNormal.color = colorOvertime
                        } else {
                            renderContext.paintTextNormal.color = colorTextPrimary
                        }
                    }
                    4 -> { // Advance
                        if (vals[i].isNotBlank() && vals[i] != "-") {
                            renderContext.paintTextNormal.color = colorHalfDay
                        } else {
                            renderContext.paintTextNormal.color = colorTextPrimary
                        }
                    }
                    else -> renderContext.paintTextNormal.color = colorTextPrimary
                }

                val textBaseline = renderContext.yPos + (rowHeight / 2f) - ((renderContext.paintTextNormal.descent() + renderContext.paintTextNormal.ascent()) / 2f)
                scaleTextToFit(renderContext.paintTextNormal, vals[i], colWidths[i] - 8f, 5f, 8f)
                currentCanvas.drawText(vals[i], textX, textBaseline, renderContext.paintTextNormal)
                currentCanvas.drawRect(curX, renderContext.yPos, curX + colWidths[i], renderContext.yPos + rowHeight, renderContext.paintRectBorder)
                curX += colWidths[i]
            }

            // Notes
            renderContext.textPaintNormal.color = colorTextPrimary
            currentCanvas.save()
            val noteYOffset = (rowHeight - staticLayout.height) / 2f
            currentCanvas.translate(curX + 5f, renderContext.yPos + noteYOffset)
            staticLayout.draw(currentCanvas)
            currentCanvas.restore()
            currentCanvas.drawRect(curX, renderContext.yPos, curX + colWidths[5], renderContext.yPos + rowHeight, renderContext.paintRectBorder)

            renderContext.yPos += rowHeight
        }

        drawClosingBranding(renderContext, data.name)

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

        // Period & Metadata
        renderContext.paintTextNormal.color = colorTextSecondary
        renderContext.paintTextNormal.textSize = FONT_BODY_NORMAL_SIZE
        renderContext.paintTextNormal.textAlign = Paint.Align.LEFT
        val periodLabel = context.getString(R.string.pdf_period, data.firstRecordDateStr, data.latestRecordDateStr)
        canvas.drawText(periodLabel, MARGIN, renderContext.yPos, renderContext.paintTextNormal)

        renderContext.paintTextNormal.textAlign = Paint.Align.RIGHT
        canvas.drawText(data.generatedAtStr, PAGE_WIDTH - MARGIN, renderContext.yPos, renderContext.paintTextNormal)
        renderContext.yPos += 20f

        // Overall Summary Section
        val rsStr = data.currencySymbol
        val hrsStr = context.getString(R.string.pdf_hrs)

        val summaryItems = listOf(
            Pair(context.getString(R.string.pdf_earnings), AmountFormatter.formatWithSymbol(data.totalEarnings, data.currencyCode)),
            Pair(context.getString(R.string.pdf_salary), AmountFormatter.formatWithSymbol(data.totalSalary, data.currencyCode)),
            Pair(context.getString(R.string.pdf_overtime), AmountFormatter.formatWithSymbol(data.totalOvertime, data.currencyCode)),
            Pair(context.getString(R.string.pdf_advance), AmountFormatter.formatWithSymbol(data.totalAdvance, data.currencyCode)),
            Pair(context.getString(R.string.pdf_present), data.totalPresent.toString()),
            Pair(context.getString(R.string.pdf_half), data.totalHalfDays.toString()),
            Pair(context.getString(R.string.pdf_absent), data.totalAbsent.toString()),
            Pair(context.getString(R.string.pdf_amount_to_receive), AmountFormatter.formatWithSymbol(data.amountToReceive, data.currencyCode))
        )

        drawOverallSummary(renderContext, summaryItems)

        val logHeaders = arrayOf(
            context.getString(R.string.pdf_col_date),
            context.getString(R.string.pdf_col_status),
            context.getString(R.string.pdf_col_wage),
            context.getString(R.string.pdf_col_overtime),
            context.getString(R.string.pdf_col_advance),
            context.getString(R.string.pdf_col_notes)
        )
        val colWidths = floatArrayOf(65f, 75f, 65f, 75f, 65f, 178f)

        // Render Year Groups
        for (yearGroup in data.years) {
            renderContext.checkPageBreak(30f)
            renderContext.paintTextBold.color = colorTextPrimary
            renderContext.paintTextBold.textSize = FONT_SECTION_SIZE
            renderContext.paintTextBold.textAlign = Paint.Align.LEFT
            val yearHeaderStr = context.getString(R.string.pdf_year, yearGroup.year)
            renderContext.canvas?.drawText(yearHeaderStr, MARGIN, renderContext.yPos + 15f, renderContext.paintTextBold)
            renderContext.yPos += 25f

            for (monthGroup in yearGroup.months) {
                renderContext.checkPageBreak(80f)

                val currentCanvas = renderContext.canvas!!

                // Month Title
                renderContext.paintTextBold.color = colorPrimary
                renderContext.paintTextBold.textSize = 10f
                renderContext.paintTextBold.textAlign = Paint.Align.LEFT
                val monthHeaderStr = context.getString(R.string.pdf_month, monthGroup.monthName)
                currentCanvas.drawText(monthHeaderStr, MARGIN, renderContext.yPos + 12f, renderContext.paintTextBold)
                renderContext.yPos += 18f

                // Monthly Summary bar
                val mSumStr = context.getString(
                    R.string.pdf_month_summary_strip,
                    AmountFormatter.formatWithSymbol(monthGroup.totalEarnings, data.currencyCode),
                    AmountFormatter.formatWithSymbol(monthGroup.totalSalary, data.currencyCode),
                    AmountFormatter.formatWithSymbol(monthGroup.totalOvertime, data.currencyCode),
                    AmountFormatter.formatWithSymbol(monthGroup.totalAdvance, data.currencyCode),
                    monthGroup.presentCount,
                    monthGroup.halfCount,
                    monthGroup.absentCount
                )
                drawMonthlySummaryStrip(renderContext, mSumStr)

                // Table Headers
                drawTableHeaders(renderContext, logHeaders, colWidths)

                for ((index, record) in monthGroup.records.withIndex()) {
                    val safeNote = if (record.note.isBlank()) "-" else record.note
                    val noteColWidth = colWidths[5] - 10f
                    val staticLayout = StaticLayout.Builder.obtain(safeNote, 0, safeNote.length, renderContext.textPaintNormal, noteColWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()

                    val rowHeight = Math.max(18f, staticLayout.height + 8f)

                    renderContext.checkPageBreak(rowHeight) {
                        drawTableHeaders(renderContext, logHeaders, colWidths)
                    }

                    val rowCanvas = renderContext.canvas!!
                    renderContext.paintRectFill.color = if (index % 2 == 0) colorWhite else colorLightBg
                    rowCanvas.drawRect(MARGIN, renderContext.yPos, PAGE_WIDTH - MARGIN, renderContext.yPos + rowHeight, renderContext.paintRectFill)

                    val otText = if (record.overtimeAmount > 0) {
                        if (record.overtimeHours > 0) "${record.overtimeHours} $hrsStr / " + AmountFormatter.formatWithSymbol(record.overtimeAmount, data.currencyCode)
                        else AmountFormatter.formatWithSymbol(record.overtimeAmount, data.currencyCode)
                    } else "-"

                    val rVals = arrayOf(
                        record.dateStr,
                        record.statusStr,
                        AmountFormatter.formatWithSymbol(record.baseEarnings, data.currencyCode),
                        otText,
                        if (record.advanceAmount > 0) AmountFormatter.formatWithSymbol(record.advanceAmount, data.currencyCode) else "-"
                    )

                    var curX = MARGIN
                    renderContext.paintTextNormal.textSize = 8f

                    for (i in rVals.indices) {
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
                                if (record.statusRaw == "present") {
                                    renderContext.paintTextNormal.color = if (record.isHalfDay) colorHalfDay else colorPresent
                                } else if (record.statusRaw == "absent") {
                                    renderContext.paintTextNormal.color = colorAbsent
                                } else {
                                    renderContext.paintTextNormal.color = colorTextPrimary
                                }
                            }
                            3 -> { // OT
                                renderContext.paintTextNormal.color = if (record.overtimeAmount > 0) colorOvertime else colorTextPrimary
                            }
                            4 -> { // Advance
                                renderContext.paintTextNormal.color = if (record.advanceAmount > 0) colorHalfDay else colorTextPrimary
                            }
                            else -> renderContext.paintTextNormal.color = colorTextPrimary
                        }

                        val textBaseline = renderContext.yPos + (rowHeight / 2f) - ((renderContext.paintTextNormal.descent() + renderContext.paintTextNormal.ascent()) / 2f)
                        scaleTextToFit(renderContext.paintTextNormal, rVals[i], colWidths[i] - 8f, 5f, 8f)
                        rowCanvas.drawText(rVals[i], textX, textBaseline, renderContext.paintTextNormal)
                        rowCanvas.drawRect(curX, renderContext.yPos, curX + colWidths[i], renderContext.yPos + rowHeight, renderContext.paintRectBorder)
                        curX += colWidths[i]
                    }

                    // Notes
                    renderContext.textPaintNormal.color = colorTextPrimary
                    rowCanvas.save()
                    val noteYOffset = (rowHeight - staticLayout.height) / 2f
                    rowCanvas.translate(curX + 5f, renderContext.yPos + noteYOffset)
                    staticLayout.draw(rowCanvas)
                    rowCanvas.restore()
                    rowCanvas.drawRect(curX, renderContext.yPos, curX + colWidths[5], renderContext.yPos + rowHeight, renderContext.paintRectBorder)

                    renderContext.yPos += rowHeight
                }
                renderContext.yPos += 15f
            }
        }

        drawClosingBranding(renderContext, data.workerName)

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
