package com.synfusion.attendance.data.model

import java.util.Locale

enum class NumberingSystem {
    INDIAN,
    STANDARD_WESTERN
}

data class CountryConfig(
    val countryCode: String,
    val displayName: String,
    val currencyCode: String,
    val currencySymbol: String,
    val callingCode: String,
    val flagEmoji: String,
    val locale: Locale = Locale.US,
    val fractionDigits: Int = 2,
    val numberingSystem: NumberingSystem = NumberingSystem.STANDARD_WESTERN
) {
    val countryName: String get() = displayName
    val flag: String get() = flagEmoji
}

object CountryRepository {

    val DEFAULT_COUNTRY = CountryConfig(
        countryCode = "IN",
        displayName = "India",
        currencyCode = "INR",
        currencySymbol = "₹",
        callingCode = "+91",
        flagEmoji = "🇮🇳",
        locale = Locale("en", "IN"),
        fractionDigits = 2,
        numberingSystem = NumberingSystem.INDIAN
    )

    val SUPPORTED_COUNTRIES = listOf(
        DEFAULT_COUNTRY,
        CountryConfig("AE", "United Arab Emirates", "AED", "د.إ", "+971", "🇦🇪", Locale("en", "AE"), fractionDigits = 2),
        CountryConfig("SA", "Saudi Arabia", "SAR", "ر.س", "+966", "🇸🇦", Locale("en", "SA"), fractionDigits = 2),
        CountryConfig("QA", "Qatar", "QAR", "ر.ق", "+974", "🇶🇦", Locale("en", "QA"), fractionDigits = 2),
        CountryConfig("KW", "Kuwait", "KWD", "د.ك", "+965", "🇰🇼", Locale("en", "KW"), fractionDigits = 3),
        CountryConfig("OM", "Oman", "OMR", "ر.ع.", "+968", "🇴🇲", Locale("en", "OM"), fractionDigits = 3),
        CountryConfig("BH", "Bahrain", "BHD", ".د.ب", "+973", "🇧🇭", Locale("en", "BH"), fractionDigits = 3),
        CountryConfig("BD", "Bangladesh", "BDT", "৳", "+880", "🇧🇩", Locale("en", "BD"), fractionDigits = 2, numberingSystem = NumberingSystem.INDIAN),
        CountryConfig("NP", "Nepal", "NPR", "रू", "+977", "🇳🇵", Locale("en", "NP"), fractionDigits = 2, numberingSystem = NumberingSystem.INDIAN),
        CountryConfig("PK", "Pakistan", "PKR", "₨", "+92", "🇵🇰", Locale("en", "PK"), fractionDigits = 2),
        CountryConfig("LK", "Sri Lanka", "LKR", "Rs", "+94", "🇱🇰", Locale("en", "LK"), fractionDigits = 2),
        CountryConfig("ID", "Indonesia", "IDR", "Rp", "+62", "🇮🇩", Locale("in", "ID"), fractionDigits = 0),
        CountryConfig("MY", "Malaysia", "MYR", "RM", "+60", "🇲🇾", Locale("ms", "MY"), fractionDigits = 2),
        CountryConfig("PH", "Philippines", "PHP", "₱", "+63", "🇵🇭", Locale("en", "PH"), fractionDigits = 2),
        CountryConfig("VN", "Vietnam", "VND", "₫", "+84", "🇻🇳", Locale("vi", "VN"), fractionDigits = 0),
        CountryConfig("TH", "Thailand", "THB", "฿", "+66", "🇹🇭", Locale("en", "TH"), fractionDigits = 2),
        CountryConfig("ZA", "South Africa", "ZAR", "R", "+27", "🇿🇦", Locale("en", "ZA"), fractionDigits = 2),
        CountryConfig("KE", "Kenya", "KES", "KSh", "+254", "🇰🇪", Locale("en", "KE"), fractionDigits = 2),
        CountryConfig("NG", "Nigeria", "NGN", "₦", "+234", "🇳🇬", Locale("en", "NG"), fractionDigits = 2),
        CountryConfig("GH", "Ghana", "GHS", "GH₵", "+233", "🇬🇭", Locale("en", "GH"), fractionDigits = 2),
        CountryConfig("TZ", "Tanzania", "TZS", "TSh", "+255", "🇹🇿", Locale("en", "TZ"), fractionDigits = 2),
        CountryConfig("UG", "Uganda", "UGX", "USh", "+256", "🇺🇬", Locale("en", "UG"), fractionDigits = 0),
        CountryConfig("MX", "Mexico", "MXN", "$", "+52", "🇲🇽", Locale("es", "MX"), fractionDigits = 2),
        CountryConfig("BR", "Brazil", "BRL", "R$", "+55", "🇧🇷", Locale("pt", "BR"), fractionDigits = 2),
        CountryConfig("CO", "Colombia", "COP", "$", "+57", "🇨🇴", Locale("es", "CO"), fractionDigits = 2),
        CountryConfig("PE", "Peru", "PEN", "S/", "+51", "🇵🇪", Locale("es", "PE"), fractionDigits = 2),
        CountryConfig("US", "United States", "USD", "$", "+1", "🇺🇸", Locale.US, fractionDigits = 2),
        CountryConfig("GB", "United Kingdom", "GBP", "£", "+44", "🇬🇧", Locale.UK, fractionDigits = 2),
        CountryConfig("EU", "European Union", "EUR", "€", "+32", "🇪🇺", Locale.GERMANY, fractionDigits = 2),
        CountryConfig("JP", "Japan", "JPY", "¥", "+81", "🇯🇵", Locale.JAPAN, fractionDigits = 0)
    )

    private val countryMap: Map<String, CountryConfig> = SUPPORTED_COUNTRIES.associateBy { it.countryCode.uppercase() }
    private val currencyMap: Map<String, CountryConfig> = SUPPORTED_COUNTRIES.associateBy { it.currencyCode.uppercase() }
    private val currencySymbolMap: Map<String, String> = SUPPORTED_COUNTRIES.associate { it.currencyCode.uppercase() to it.currencySymbol }

    fun getCountry(countryCode: String?): CountryConfig {
        if (countryCode.isNullOrBlank()) return DEFAULT_COUNTRY
        return countryMap[countryCode.trim().uppercase()] ?: DEFAULT_COUNTRY
    }

    fun getCountryByCurrency(currencyCode: String?): CountryConfig {
        if (currencyCode.isNullOrBlank()) return DEFAULT_COUNTRY
        return currencyMap[currencyCode.trim().uppercase()] ?: DEFAULT_COUNTRY
    }

    fun getCurrencySymbol(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return DEFAULT_COUNTRY.currencySymbol
        val codeUpper = currencyCode.trim().uppercase()
        return currencySymbolMap[codeUpper] ?: try {
            java.util.Currency.getInstance(codeUpper).getSymbol(Locale.US)
        } catch (e: Exception) {
            codeUpper
        }
    }

    fun getDefaultCurrencyForCountry(countryCode: String?): String {
        return getCountry(countryCode).currencyCode
    }
}
