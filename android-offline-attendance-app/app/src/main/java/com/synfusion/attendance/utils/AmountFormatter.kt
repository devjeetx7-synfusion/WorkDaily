package com.synfusion.attendance.utils

import com.synfusion.attendance.data.model.CountryRepository
import java.math.BigDecimal

object AmountFormatter {
    fun format(amount: Double, currencyCode: String = "INR"): String {
        val bd = BigDecimal.valueOf(amount).setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
        return formatBigDecimal(bd, currencyCode)
    }

    fun format(amount: Float, currencyCode: String = "INR"): String {
        return format(amount.toDouble(), currencyCode)
    }

    fun format(amount: Long, currencyCode: String = "INR"): String {
        val bd = BigDecimal.valueOf(amount).setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
        return formatBigDecimal(bd, currencyCode)
    }

    fun format(amount: Int, currencyCode: String = "INR"): String {
        return format(amount.toLong(), currencyCode)
    }

    fun format(amountStr: String, currencyCode: String = "INR"): String {
        val cleanStr = amountStr.replace(",", "").trim()
        val bdValue = cleanStr.toBigDecimalOrNull() ?: return amountStr
        val scaled = bdValue.setScale(2, AttendanceCalculationEngine.ROUNDING_MODE)
        return formatBigDecimal(scaled, currencyCode)
    }

    fun formatWithSymbol(amount: Double, currencyCode: String = "INR"): String {
        val symbol = CountryRepository.getCurrencySymbol(currencyCode)
        val formattedNum = format(amount, currencyCode)
        val needsSpace = symbol.length > 1 && !symbol.startsWith("$") && !symbol.startsWith("₹")
        return if (needsSpace) "$symbol $formattedNum" else "$symbol$formattedNum"
    }

    fun formatWithSymbol(amountStr: String, currencyCode: String = "INR"): String {
        val cleanStr = amountStr.replace(",", "").trim()
        val bdValue = cleanStr.toBigDecimalOrNull()
        return if (bdValue != null) {
            formatWithSymbol(bdValue.toDouble(), currencyCode)
        } else {
            val symbol = CountryRepository.getCurrencySymbol(currencyCode)
            "$symbol $amountStr"
        }
    }

    private fun formatBigDecimal(bd: BigDecimal, currencyCode: String): String {
        val isNegative = bd.compareTo(BigDecimal.ZERO) < 0
        val absBd = bd.abs()

        val plainStr = absBd.toPlainString()
        val parts = plainStr.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else "00"

        val formattedInt = if (currencyCode.equals("INR", ignoreCase = true)) {
            formatIndianIntegerStr(intPart)
        } else {
            formatStandardIntegerStr(intPart)
        }

        val trimmedDec = decPart.trimEnd('0')
        val decimalSuffix = if (trimmedDec.isNotEmpty()) ".$trimmedDec" else ""

        val prefix = if (isNegative) "-" else ""
        return "$prefix$formattedInt$decimalSuffix"
    }

    private fun formatIndianIntegerStr(intPart: String): String {
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

    private fun formatStandardIntegerStr(intPart: String): String {
        val len = intPart.length
        if (len <= 3) return intPart

        val groups = mutableListOf<String>()
        var i = len
        while (i > 0) {
            val start = Math.max(0, i - 3)
            groups.add(0, intPart.substring(start, i))
            i -= 3
        }
        return groups.joinToString(",")
    }
}
