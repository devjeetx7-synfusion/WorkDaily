package com.synfusion.attendance.backup.dto.canonical

import com.google.gson.annotations.SerializedName

data class CanonicalSummary(
    @SerializedName("id") val id: String,
    @SerializedName("monthId") val monthId: String,
    @SerializedName("role") val role: String,
    @SerializedName("totalPresent") val totalPresent: Double,
    @SerializedName("totalWages") val totalWages: Double,
    @SerializedName("totalAdvance") val totalAdvance: Double,
    @SerializedName("todayPresentCount") val todayPresentCount: Double,
    @SerializedName("todayAbsentCount") val todayAbsentCount: Double
)
