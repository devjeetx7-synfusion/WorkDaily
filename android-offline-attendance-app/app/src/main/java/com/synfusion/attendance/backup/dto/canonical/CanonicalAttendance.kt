package com.synfusion.attendance.backup.dto.canonical

import com.google.gson.annotations.SerializedName

data class CanonicalAttendance(
    @SerializedName("id") val id: String,
    @SerializedName("workerId") val workerId: String?,
    @SerializedName("date") val date: String,
    @SerializedName("status") val status: String,
    @SerializedName("type") val type: String,
    @SerializedName("reason") val reason: String?,
    @SerializedName("overtimeHours") val overtimeHours: Int,
    @SerializedName("note") val note: String,
    @SerializedName("advanceAmount") val advanceAmount: Double,
    @SerializedName("wage") val wage: Double,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("createdAt") val createdAt: Long
)
