package com.synfusion.attendance.backup.dto.canonical

import com.google.gson.annotations.SerializedName

data class CanonicalUser(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("dailyWage") val dailyWage: Double,
    @SerializedName("countryCode") val countryCode: String,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("createdAt") val createdAt: Long
)
