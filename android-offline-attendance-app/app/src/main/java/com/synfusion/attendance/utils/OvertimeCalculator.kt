package com.synfusion.attendance.utils

import java.math.BigDecimal
import java.math.RoundingMode

object OvertimeCalculator {
    private val otWageRegex = Regex("\\[OT_WAGE_([0-9.]+)\\]")

    /**
     * Extracts the custom overtime amount from the note if present.
     */
    fun extractCustomAmount(note: String?): Double? {
        return extractCustomAmountBigDecimal(note)?.toDouble()
    }

    /**
     * Extracts the custom overtime amount as BigDecimal with exact 2 decimal precision.
     */
    fun extractCustomAmountBigDecimal(note: String?): BigDecimal? {
        if (note.isNullOrBlank()) return null
        val match = otWageRegex.find(note)
        val strVal = match?.groupValues?.get(1) ?: return null
        return strVal.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Removes the tag from the note for UI display.
     */
    fun cleanNote(note: String?): String? {
        if (note == null) return null
        return note.replace(otWageRegex, "").trim()
    }

    /**
     * Calculates overtime amount based on daily wage and overtime hours using exact BigDecimal arithmetic.
     * Prioritizes custom amount entered in the note.
     * Fallbacks to standard calculation logic if no custom amount is found.
     */
    fun calculateOvertimeAmountBigDecimal(dailyWage: Double, overtimeHours: Int, note: String? = null): BigDecimal {
        val customAmount = extractCustomAmountBigDecimal(note)
        if (customAmount != null) {
            return customAmount
        }

        if (overtimeHours <= 0 || dailyWage <= 0.0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

        val wageBd = BigDecimal.valueOf(dailyWage)
        val hourlyRate = wageBd.divide(BigDecimal.valueOf(8), 10, RoundingMode.HALF_UP)
        return hourlyRate.multiply(BigDecimal.valueOf(overtimeHours.toLong())).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateOvertimeAmount(dailyWage: Double, overtimeHours: Int, note: String? = null): Double {
        return calculateOvertimeAmountBigDecimal(dailyWage, overtimeHours, note).toDouble()
    }
}
