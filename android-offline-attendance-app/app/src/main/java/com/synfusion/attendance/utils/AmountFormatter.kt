package com.synfusion.attendance.utils

import java.util.Locale

object AmountFormatter {
    fun format(amount: Double): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)

        val amountStr = String.format(Locale.US, "%.2f", absAmount)
        val parts = amountStr.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""

        val formattedInt = formatIntegerStr(intPart)

        val trimmedDec = decPart.trimEnd('0')
        val decimalSuffix = if (trimmedDec.isNotEmpty()) ".$trimmedDec" else ""

        val prefix = if (isNegative) "-" else ""
        return "$prefix$formattedInt$decimalSuffix"
    }

    fun format(amount: Float): String {
        return format(amount.toDouble())
    }

    fun format(amount: Long): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        val prefix = if (isNegative) "-" else ""
        return "$prefix${formatIntegerStr(absAmount.toString())}"
    }

    fun format(amount: Int): String {
        return format(amount.toLong())
    }

    fun format(amountStr: String): String {
        val cleanStr = amountStr.replace(",", "").trim()
        val dValue = cleanStr.toDoubleOrNull() ?: return amountStr
        return if (dValue % 1.0 == 0.0) {
            format(dValue.toLong())
        } else {
            format(dValue)
        }
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
