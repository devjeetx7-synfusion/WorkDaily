package com.synfusion.attendance.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object AmountFormatter {
    fun format(amount: Double): String {
        val bd = BigDecimal.valueOf(amount).setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
        return formatBigDecimal(bd)
    }

    fun format(amount: Float): String {
        return format(amount.toDouble())
    }

    fun format(amount: Long): String {
        val bd = BigDecimal.valueOf(amount).setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
        return formatBigDecimal(bd)
    }

    fun format(amount: Int): String {
        return format(amount.toLong())
    }

    fun format(amountStr: String): String {
        val cleanStr = amountStr.replace(",", "").trim()
        val bdValue = cleanStr.toBigDecimalOrNull() ?: return amountStr
        val scaled = bdValue.setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
        return formatBigDecimal(scaled)
    }

    private fun formatBigDecimal(bd: BigDecimal): String {
        val isNegative = bd.compareTo(BigDecimal.ZERO) < 0
        val absBd = bd.abs()

        val plainStr = absBd.toPlainString()
        val parts = plainStr.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else "00"

        val formattedInt = formatIntegerStr(intPart)

        val trimmedDec = decPart.trimEnd('0')
        val decimalSuffix = if (trimmedDec.isNotEmpty()) ".$trimmedDec" else ""

        val prefix = if (isNegative) "-" else ""
        return "$prefix$formattedInt$decimalSuffix"
    }

    private fun formatIntegerStr(intPart: String): String {
        val len = intPart.length
        if (len <= 3) return intPart

        val lastThree = intPart.substring(len - 3)
        val remaining = intPart.substring(0, len - 3)

        val remainingGroups = mutableListOf<String>()
        var i = remaining.length
        while (i > 0) {
            val start = Math.max(0, i - 2)
            remainingGroups.add(0, remaining.substring(start, i))
            i -= 2
        }
        return remainingGroups.joinToString(",") + "," + lastThree
    }
}
