package com.synfusion.attendance.backup.dto.v1

import com.google.gson.annotations.SerializedName

data class UserV1Dto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("dailyWage") val dailyWage: Double? = null,
    @SerializedName("countryCode") val countryCode: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    @SerializedName("createdAt") val createdAt: Long? = null
)
