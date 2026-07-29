package com.synfusion.attendance.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

class CurrencyAmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = formatIndianCurrency(originalText)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (originalText.isEmpty()) return 0
                var transformedOffset = 0
                var originalOffset = 0
                while (originalOffset < offset && transformedOffset < formattedText.length) {
                    if (formattedText[transformedOffset] == ',') {
                        transformedOffset++
                    } else {
                        originalOffset++
                        transformedOffset++
                    }
                }
                while (transformedOffset < formattedText.length && formattedText[transformedOffset] == ',') {
                    transformedOffset++
                }
                return transformedOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (formattedText.isEmpty()) return 0
                var transformedOffset = 0
                var originalOffset = 0
                while (transformedOffset < offset && originalOffset < originalText.length) {
                    if (formattedText[transformedOffset] == ',') {
                        transformedOffset++
                    } else {
                        originalOffset++
                        transformedOffset++
                    }
                }
                return originalOffset
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }

    companion object {
        private val indianLocale = Locale("en", "IN")
        private val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            maximumFractionDigits = 2
            isGroupingUsed = true
        }

        fun formatIndianCurrency(amountStr: String): String {
            val cleanStr = amountStr.replace(",", "")
            if (cleanStr.isEmpty()) return ""
            return try {
                val hasDecimal = cleanStr.contains(".")
                if (hasDecimal) {
                    val parts = cleanStr.split(".")
                    val integerPart = parts[0].toLongOrNull() ?: 0L
                    val decimalPart = if (parts.size > 1) parts[1] else ""
                    "${formatter.format(integerPart)}.$decimalPart"
                } else {
                    val parsed = cleanStr.toLongOrNull() ?: return amountStr
                    formatter.format(parsed)
                }
            } catch (e: Exception) {
                amountStr
            }
        }

        fun formatIndianCurrency(amount: Number?): String {
            if (amount == null) return "0"
            return try {
                formatter.format(amount)
            } catch (e: Exception) {
                amount.toString()
            }
        }
    }
}
