package com.synfusion.attendance.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.data.model.NumberingSystem
import java.text.DecimalFormatSymbols

class CurrencyVisualTransformation(val currencyCode: String = "INR") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val country = CountryRepository.getCountryByCurrency(currencyCode)
        val symbols = DecimalFormatSymbols(country.locale)
        val groupingSepStr = symbols.groupingSeparator.toString()
        val decimalSepStr = symbols.decimalSeparator.toString()

        val parts = originalText.split(".", ",")
        val intPart = parts[0]

        val transformed = StringBuilder()

        val origToTrans = IntArray(originalText.length + 1)
        val transToOrig = ArrayList<Int>()

        val intLen = intPart.length
        val isIndian = country.numberingSystem == NumberingSystem.INDIAN

        for (i in 0 until intLen) {
            val distanceFromRight = intLen - i
            val needGroup = if (isIndian) {
                i > 0 && distanceFromRight >= 3 && (distanceFromRight - 3) % 2 == 0
            } else {
                i > 0 && distanceFromRight % 3 == 0
            }

            if (needGroup) {
                transformed.append(groupingSepStr)
                transToOrig.add(i)
            }
            origToTrans[i] = transformed.length
            transformed.append(intPart[i])
            transToOrig.add(i)
        }
        origToTrans[intLen] = transformed.length

        if (parts.size > 1) {
            origToTrans[intLen] = transformed.length
            transformed.append(decimalSepStr)
            transToOrig.add(intLen)

            val decPartStr = parts[1]
            for (i in 0 until decPartStr.length) {
                origToTrans[intLen + 1 + i] = transformed.length
                transformed.append(decPartStr[i])
                transToOrig.add(intLen + 1 + i)
            }
            origToTrans[originalText.length] = transformed.length
        }
        transToOrig.add(originalText.length)

        val transformedTextStr = transformed.toString()
        val transformedText = AnnotatedString(transformedTextStr)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, originalText.length)
                return origToTrans[safeOffset]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, transformedTextStr.length)
                return transToOrig[safeOffset]
            }
        }

        return TransformedText(transformedText, offsetMapping)
    }
}

class IndianCurrencyVisualTransformation : VisualTransformation {
    private val delegate = CurrencyVisualTransformation("INR")
    override fun filter(text: AnnotatedString): TransformedText {
        return delegate.filter(text)
    }
}
