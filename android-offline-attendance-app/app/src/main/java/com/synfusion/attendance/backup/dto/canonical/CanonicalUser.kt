package com.synfusion.attendance.backup.dto.canonical

data class CanonicalUser(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val role: String,
    val dailyWage: Double,
    val countryCode: String,
    val currencyCode: String,
    val createdAt: Long
)
