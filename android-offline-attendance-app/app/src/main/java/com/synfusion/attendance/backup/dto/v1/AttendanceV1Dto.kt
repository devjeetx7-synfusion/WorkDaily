package com.synfusion.attendance.backup.dto.v1

import com.google.gson.annotations.SerializedName

data class AttendanceV1Dto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("workerId") val workerId: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("overtimeHours") val overtimeHours: Int? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("advanceAmount") val advanceAmount: Double? = null,
    @SerializedName("wage") val wage: Double? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    @SerializedName("createdAt") val createdAt: Long? = null
)
