package com.synfusion.attendance.data.model

data class CountryConfig(
    val countryCode: String,
    val displayName: String,
    val currencyCode: String,
    val currencySymbol: String,
    val callingCode: String,
    val flagEmoji: String
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
        flagEmoji = "🇮🇳"
    )

    val SUPPORTED_COUNTRIES = listOf(
        DEFAULT_COUNTRY,
        CountryConfig("AE", "United Arab Emirates", "AED", "د.إ", "+971", "🇦🇪"),
        CountryConfig("SA", "Saudi Arabia", "SAR", "ر.س", "+966", "🇸🇦"),
        CountryConfig("QA", "Qatar", "QAR", "ر.ق", "+974", "🇶🇦"),
        CountryConfig("KW", "Kuwait", "KWD", "د.ك", "+965", "🇰🇼"),
        CountryConfig("OM", "Oman", "OMR", "ر.ع.", "+968", "🇴🇲"),
        CountryConfig("BH", "Bahrain", "BHD", ".د.ب", "+973", "🇧🇭"),
        CountryConfig("BD", "Bangladesh", "BDT", "৳", "+880", "🇧🇩"),
        CountryConfig("NP", "Nepal", "NPR", "रू", "+977", "🇳🇵"),
        CountryConfig("PK", "Pakistan", "PKR", "₨", "+92", "🇵🇰"),
        CountryConfig("LK", "Sri Lanka", "LKR", "Rs", "+94", "🇱🇰"),
        CountryConfig("ID", "Indonesia", "IDR", "Rp", "+62", "🇮🇩"),
        CountryConfig("MY", "Malaysia", "MYR", "RM", "+60", "🇲🇾"),
        CountryConfig("PH", "Philippines", "PHP", "₱", "+63", "🇵🇭"),
        CountryConfig("VN", "Vietnam", "VND", "₫", "+84", "🇻🇳"),
        CountryConfig("TH", "Thailand", "THB", "฿", "+66", "🇹🇭"),
        CountryConfig("ZA", "South Africa", "ZAR", "R", "+27", "🇿🇦"),
        CountryConfig("KE", "Kenya", "KES", "KSh", "+254", "🇰🇪"),
        CountryConfig("NG", "Nigeria", "NGN", "₦", "+234", "🇳🇬"),
        CountryConfig("GH", "Ghana", "GHS", "GH₵", "+233", "🇬🇭"),
        CountryConfig("TZ", "Tanzania", "TZS", "TSh", "+255", "🇹🇿"),
        CountryConfig("UG", "Uganda", "UGX", "USh", "+256", "🇺🇬"),
        CountryConfig("MX", "Mexico", "MXN", "$", "+52", "🇲🇽"),
        CountryConfig("BR", "Brazil", "BRL", "R$", "+55", "🇧🇷"),
        CountryConfig("CO", "Colombia", "COP", "$", "+57", "🇨🇴"),
        CountryConfig("PE", "Peru", "PEN", "S/", "+51", "🇵🇪"),
        CountryConfig("US", "United States", "USD", "$", "+1", "🇺🇸"),
        CountryConfig("GB", "United Kingdom", "GBP", "£", "+44", "🇬🇧"),
        CountryConfig("EU", "European Union", "EUR", "€", "+32", "🇪🇺"),
        CountryConfig("JP", "Japan", "JPY", "¥", "+81", "🇯🇵")
    )

    private val countryMap: Map<String, CountryConfig> = SUPPORTED_COUNTRIES.associateBy { it.countryCode.uppercase() }
    private val currencySymbolMap: Map<String, String> = SUPPORTED_COUNTRIES.associate { it.currencyCode.uppercase() to it.currencySymbol }

    fun getCountry(countryCode: String?): CountryConfig {
        if (countryCode.isNullOrBlank()) return DEFAULT_COUNTRY
        return countryMap[countryCode.trim().uppercase()] ?: DEFAULT_COUNTRY
    }

    fun getCurrencySymbol(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return DEFAULT_COUNTRY.currencySymbol
        val codeUpper = currencyCode.trim().uppercase()
        return currencySymbolMap[codeUpper] ?: try {
            java.util.Currency.getInstance(codeUpper).getSymbol(java.util.Locale.US)
        } catch (e: Exception) {
            codeUpper
        }
    }

    fun getDefaultCurrencyForCountry(countryCode: String?): String {
        return getCountry(countryCode).currencyCode
    }
}
