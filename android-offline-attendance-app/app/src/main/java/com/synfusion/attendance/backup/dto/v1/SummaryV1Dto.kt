package com.synfusion.attendance.backup.dto.v1

import com.google.gson.annotations.SerializedName

data class SummaryV1Dto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("monthId") val monthId: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("totalPresent") val totalPresent: Double? = null,
    @SerializedName("totalWages") val totalWages: Double? = null,
    @SerializedName("totalAdvance") val totalAdvance: Double? = null,
    @SerializedName("todayPresentCount") val todayPresentCount: Double? = null,
    @SerializedName("todayAbsentCount") val todayAbsentCount: Double? = null
)
