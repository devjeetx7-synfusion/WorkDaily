package com.synfusion.attendance.utils

import com.synfusion.attendance.data.model.CountryConfig
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.data.model.NumberingSystem
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

object AmountFormatter {

    fun format(amount: Double, currencyCode: String = "INR"): String {
        val country = CountryRepository.getCountryByCurrency(currencyCode)
        return formatAmount(BigDecimal.valueOf(amount), country)
    }

    fun format(amount: Float, currencyCode: String = "INR"): String {
        return format(amount.toDouble(), currencyCode)
    }

    fun format(amount: Long, currencyCode: String = "INR"): String {
        val country = CountryRepository.getCountryByCurrency(currencyCode)
        return formatAmount(BigDecimal.valueOf(amount), country)
    }

    fun format(amount: Int, currencyCode: String = "INR"): String {
        return format(amount.toLong(), currencyCode)
    }

    fun format(amountStr: String, currencyCode: String = "INR"): String {
        val parsed = parseToDoubleOrNull(amountStr, currencyCode) ?: return amountStr
        return format(parsed, currencyCode)
    }

    fun formatWithSymbol(amount: Double, currencyCode: String = "INR"): String {
        val country = CountryRepository.getCountryByCurrency(currencyCode)
        val formattedNum = format(amount, currencyCode)
        return formatWithSymbolAndNum(formattedNum, country)
    }

    fun formatWithSymbol(amountStr: String, currencyCode: String = "INR"): String {
        val parsed = parseToDoubleOrNull(amountStr, currencyCode)
        return if (parsed != null) {
            formatWithSymbol(parsed, currencyCode)
        } else {
            val country = CountryRepository.getCountryByCurrency(currencyCode)
            formatWithSymbolAndNum(amountStr, country)
        }
    }

    fun parseToDoubleOrNull(input: String?, currencyCode: String = "INR"): Double? {
        if (input.isNullOrBlank()) return null
        val country = CountryRepository.getCountryByCurrency(currencyCode)
        val symbols = DecimalFormatSymbols(country.locale)
        val decimalSep = symbols.decimalSeparator

        // Remove symbol and currency code
        var clean = input.trim()
            .replace(country.currencySymbol, "")
            .replace(currencyCode, "", ignoreCase = true)
            .trim()

        if (clean.isEmpty()) return null

        // Replace grouping separators / spaces and normalize decimal separator to '.'
        if (decimalSep == ',') {
            clean = clean.replace(".", "").replace(" ", "").replace("\u00A0", "").replace(",", ".")
        } else {
            clean = clean.replace(",", "").replace(" ", "").replace("\u00A0", "")
        }

        return clean.toDoubleOrNull()
    }

    private fun formatAmount(bd: BigDecimal, country: CountryConfig): String {
        val fractionDigits = country.fractionDigits
        val scaled = bd.setScale(fractionDigits, AttendanceCalculationEngine.ROUNDING_MODE)
        val isNegative = scaled.compareTo(BigDecimal.ZERO) < 0
        val absBd = scaled.abs()

        val plainStr = absBd.toPlainString()
        val parts = plainStr.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""

        val symbols = DecimalFormatSymbols(country.locale)
        val groupingSep = symbols.groupingSeparator
        val decimalSep = symbols.decimalSeparator

        val formattedInt = if (country.numberingSystem == NumberingSystem.INDIAN) {
            formatIndianIntegerStr(intPart, groupingSep)
        } else {
            formatStandardIntegerStr(intPart, groupingSep)
        }

        val decimalSuffix = if (fractionDigits > 0) {
            val paddedDec = decPart.padEnd(fractionDigits, '0').take(fractionDigits)
            "$decimalSep$paddedDec"
        } else {
            ""
        }

        val prefix = if (isNegative) "-" else ""
        return "$prefix$formattedInt$decimalSuffix"
    }

    private fun formatIndianIntegerStr(intPart: String, groupingSep: Char): String {
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
        return remainingGroups.joinToString(groupingSep.toString()) + groupingSep + lastThree
    }

    private fun formatStandardIntegerStr(intPart: String, groupingSep: Char): String {
        val len = intPart.length
        if (len <= 3) return intPart

        val groups = mutableListOf<String>()
        var i = len
        while (i > 0) {
            val start = Math.max(0, i - 3)
            groups.add(0, intPart.substring(start, i))
            i -= 3
        }
        return groups.joinToString(groupingSep.toString())
    }

    private fun formatWithSymbolAndNum(formattedNum: String, country: CountryConfig): String {
        val symbol = country.currencySymbol
        val locale = country.locale

        val isNegative = formattedNum.startsWith("-")
        val absNum = if (isNegative) formattedNum.substring(1) else formattedNum

        val isSuffix = try {
            val nf = NumberFormat.getCurrencyInstance(locale)
            if (nf is DecimalFormat) {
                val pattern = nf.toPattern()
                pattern.trim().endsWith("¤")
            } else false
        } catch (e: Exception) {
            false
        }

        val isArabicLocale = locale.language == "ar" || locale.language == "ur" || locale.language == "fa"

        val signedResult = if (isSuffix) {
            "$absNum $symbol"
        } else {
            val needsSpace = isArabicLocale || (symbol.length > 1 && !symbol.startsWith("$") && !symbol.startsWith("₹") && !symbol.startsWith("£") && !symbol.startsWith("¥"))
            if (needsSpace) "$symbol $absNum" else "$symbol$absNum"
        }

        return if (isNegative) "-$signedResult" else signedResult
    }
}
